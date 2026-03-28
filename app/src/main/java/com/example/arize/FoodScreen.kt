package com.example.arize

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Calendar

// Standard default daily goals (You can later connect this to UserProfile)
private const val GOAL_CALORIES = 2000
private const val GOAL_PROTEIN = 150.0
private const val GOAL_CARBS = 250.0
private const val GOAL_FATS = 70.0

data class FoodAnalysisResult(
    val itemName: String,
    val calories: Int,
    val protein: String,
    val fat: String,
    val carbs: String,
    val fiber: String,
    val score: Int,
    val shouldEat: Boolean,
    val recommendation: String
)

private data class LoggedMeal(
    val id: Long,
    val photoPath: String,
    val result: FoodAnalysisResult,
)

private data class PendingMeal(
    val photo: Bitmap,
    val result: FoodAnalysisResult,
)

private const val KEY_FOOD_HISTORY = "food_history_json"

// Helper to extract numbers from "15.5g" strings
private fun String.extractMacro(): Double {
    return this.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
}

// Helper to check if a meal was logged today
private fun isToday(timestamp: Long): Boolean {
    val calendar = Calendar.getInstance()
    val todayYear = calendar.get(Calendar.YEAR)
    val todayDay = calendar.get(Calendar.DAY_OF_YEAR)
    calendar.timeInMillis = timestamp
    return calendar.get(Calendar.YEAR) == todayYear && calendar.get(Calendar.DAY_OF_YEAR) == todayDay
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodPage(profile: UserProfile, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val coroutineScope = rememberCoroutineScope()
    val loggedMeals = remember { mutableStateListOf<LoggedMeal>().apply { addAll(loadFoodHistory(prefs)) } }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }

    var isAnalyzing by remember { mutableStateOf(false) }
    var loadingPhoto by remember { mutableStateOf<Bitmap?>(null) }
    var analysisError by remember { mutableStateOf<String?>(null) }
    var pendingMeal by remember { mutableStateOf<PendingMeal?>(null) }
    var selectedMeal by remember { mutableStateOf<LoggedMeal?>(null) }
    val runtimeApiKey = if (profile.geminiApiKey.isNotBlank()) profile.geminiApiKey else BuildConfig.GEMINI_API_KEY

    // Calculate Today's Totals
    val todayMeals = loggedMeals.filter { isToday(it.id) }
    val consumedCalories = todayMeals.sumOf { it.result.calories }
    val consumedProtein = todayMeals.sumOf { it.result.protein.extractMacro() }
    val consumedCarbs = todayMeals.sumOf { it.result.carbs.extractMacro() }
    val consumedFats = todayMeals.sumOf { it.result.fat.extractMacro() }

    val remainingCals = (GOAL_CALORIES - consumedCalories).coerceAtLeast(0)
    val remainingProtein = (GOAL_PROTEIN - consumedProtein).coerceAtLeast(0.0)
    val remainingCarbs = (GOAL_CARBS - consumedCarbs).coerceAtLeast(0.0)
    val remainingFats = (GOAL_FATS - consumedFats).coerceAtLeast(0.0)

    fun runGemini(bitmap: Bitmap) {
        if (runtimeApiKey.isBlank()) {
            analysisError = "API key missing. Add GEMINI_API_KEY in local.properties or Profile."
            return
        }
        analysisError = null
        pendingMeal = null
        loadingPhoto = bitmap
        isAnalyzing = true

        coroutineScope.launch {
            val result = analyzeFoodBitmapWithGeminiSdk(
                apiKey = runtimeApiKey,
                bitmap = bitmap,
                profile = profile,
                remCals = remainingCals,
                remPro = remainingProtein,
                remCarbs = remainingCarbs,
                remFat = remainingFats
            )
            isAnalyzing = false
            loadingPhoto = null
            result.onSuccess { data ->
                pendingMeal = PendingMeal(photo = bitmap, result = data)
            }
            result.onFailure {
                analysisError = it.message ?: "Failed to analyze image"
            }
        }
    }

    val takePhotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap == null) {
            analysisError = "No photo captured"
            return@rememberLauncherForActivityResult
        }
        runGemini(bitmap)
    }

    val uploadPhotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val bitmap = decodeUriToBitmap(context, uri)
        if (bitmap == null) {
            analysisError = "Could not read selected photo"
            return@rememberLauncherForActivityResult
        }
        runGemini(bitmap)
    }

    Box(modifier = modifier.fillMaxSize().background(AppDark)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // --- NEW: Daily Nutrition Plan Dashboard ---
            item {
                Text("Daily Nutrition Plan", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Your macro targets for today", color = MutedText, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Calories
                        val calProgress = (consumedCalories.toFloat() / GOAL_CALORIES).coerceIn(0f, 1f)
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Calories", color = Color.White, fontWeight = FontWeight.Bold)
                                Text("$consumedCalories / $GOAL_CALORIES kcal", color = MutedText)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = calProgress,
                                modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
                                color = AccentBlue,
                                trackColor = Color(0xFF2A3142)
                            )
                        }

                        // Macros
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                MacroProgressBar("Protein", consumedProtein, GOAL_PROTEIN, Color(0xFF60A5FA)) // Blue
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                MacroProgressBar("Carbs", consumedCarbs, GOAL_CARBS, Color(0xFF34D399)) // Green
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                MacroProgressBar("Fats", consumedFats, GOAL_FATS, Color(0xFFFBBF24)) // Yellow
                            }
                        }
                    }
                }
            }
            // -------------------------------------------

            item {
                Text("Food Scan", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(170.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(enabled = !isAnalyzing) {
                                    if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
                                    else takePhotoLauncher.launch(null)
                                },
                            color = Color(0xFF121827),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A3142))
                        ) {
                            Box {
                                AsyncImage(
                                    model = "https://images.pexels.com/photos/1640777/pexels-photo-1640777.jpeg",
                                    contentDescription = "Take photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(modifier = Modifier.fillMaxSize().background(Color(0xB8000000)))
                                Column(
                                    modifier = Modifier.align(Alignment.Center),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Take Photo", color = Color.White, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = { uploadPhotoLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A3142))
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Upload Photo", color = Color.White)
                        }
                    }
                }
            }

            if (analysisError != null) {
                item {
                    Text(
                        analysisError!!,
                        color = Color(0xFFFFCACA),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF4B1E27), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    )
                }
            }

            if (pendingMeal != null) {
                item {
                    PendingReportCard(
                        pending = pendingMeal!!,
                        onSave = {
                            val id = System.currentTimeMillis()
                            val photoPath = persistFoodBitmap(context, id, pendingMeal!!.photo)
                            if (photoPath == null) {
                                analysisError = "Failed to save meal image"
                                return@PendingReportCard
                            }
                            val meal = LoggedMeal(id = id, photoPath = photoPath, result = pendingMeal!!.result)
                            loggedMeals.add(0, meal)
                            saveFoodHistory(prefs, loggedMeals)
                            pendingMeal = null
                        },
                        onDiscard = { pendingMeal = null }
                    )
                }
            }

            if (loggedMeals.isNotEmpty()) {
                item {
                    Text("Saved Reports", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(loggedMeals, key = { it.id }) { meal ->
                            SavedMealCard(
                                meal = meal,
                                onOpen = { selectedMeal = meal }
                            )
                        }
                    }
                }
            }
        }

        if (isAnalyzing && loadingPhoto != null) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xAA000000)),
                color = Color.Transparent
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardDark),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            androidx.compose.foundation.Image(
                                bitmap = loadingPhoto!!.asImageBitmap(),
                                contentDescription = "Captured food",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            CircularProgressIndicator(color = AccentBlue)
                            Text("Analyzing limits and macros...", color = Color.White)
                        }
                    }
                }
            }
        }

        if (selectedMeal != null) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { selectedMeal = null },
                sheetState = sheetState,
                containerColor = CardDark
            ) {
                SavedReportDetails(meal = selectedMeal!!, onClose = { selectedMeal = null })
            }
        }
    }
}

@Composable
fun MacroProgressBar(label: String, consumed: Double, goal: Double, color: Color) {
    val progress = (consumed / goal).toFloat().coerceIn(0f, 1f)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White, fontSize = 12.sp)
            Text("${consumed.toInt()}/${goal.toInt()}g", color = MutedText, fontSize = 11.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = Color(0xFF2A3142)
        )
    }
}

@Composable
private fun PendingReportCard(
    pending: PendingMeal,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("AI Dietitian Report", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            androidx.compose.foundation.Image(
                bitmap = pending.photo.asImageBitmap(),
                contentDescription = pending.result.itemName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            ReportFacts(result = pending.result)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onDiscard, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Discard")
                }
                Button(onClick = onSave, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Save Log")
                }
            }
        }
    }
}

@Composable
private fun SavedMealCard(
    meal: LoggedMeal,
    onOpen: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .size(width = 250.dp, height = 280.dp)
            .clickable { onOpen() }
            .border(1.dp, Color(0xFF2A3142), RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            AsyncImage(
                model = File(meal.photoPath),
                contentDescription = meal.result.itemName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(meal.result.itemName, color = Color.White, maxLines = 2, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("${meal.result.calories} kcal", color = MutedText)
        }
    }
}

@Composable
private fun SavedReportDetails(
    meal: LoggedMeal,
    onClose: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Saved Report", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        item {
            AsyncImage(
                model = File(meal.photoPath),
                contentDescription = meal.result.itemName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(14.dp))
            )
        }
        item { ReportFacts(result = meal.result) }
        item {
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                Text("Close")
            }
        }
    }
}

@Composable
private fun ReportFacts(result: FoodAnalysisResult) {
    Surface(
        color = Color(0xFF111827),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A3142)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(result.itemName, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Calories: ${result.calories} kcal", color = Color(0xFFD1D5DB))
            Text("Protein: ${result.protein}  Fat: ${result.fat}", color = Color(0xFFD1D5DB))
            Text("Carbs: ${result.carbs}  Fiber: ${result.fiber}", color = Color(0xFFD1D5DB))

            Spacer(modifier = Modifier.height(4.dp))

            // Display the AI Recommendation
            if (result.recommendation.isNotBlank()) {
                val statusColor = if (result.shouldEat) Color(0xFF86EFAC) else Color(0xFFFCA5A5)
                val statusText = if (result.shouldEat) "✓ Fits Your Macros" else "✗ Exceeds Remaining Goals"

                Text(statusText, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(result.recommendation, color = Color(0xFF9CA3AF), style = MaterialTheme.typography.bodyMedium)
            }

            Text("Overall Nutrition Score: ${result.score}/10", color = Color(0xFF93C5FD), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

private suspend fun analyzeFoodBitmapWithGeminiSdk(
    apiKey: String,
    bitmap: Bitmap,
    profile: UserProfile,
    remCals: Int,
    remPro: Double,
    remCarbs: Double,
    remFat: Double
): Result<FoodAnalysisResult> {
    return withContext(Dispatchers.IO) {
        runCatching {
            val responseText = analyzeObject(bitmap, apiKey, profile, remCals, remPro, remCarbs, remFat)
                ?: throw IllegalStateException("Gemini returned empty response")

            parseGeminiNutritionResponse(responseText)
                ?: throw IllegalStateException("Could not parse Gemini response")
        }
    }
}

private suspend fun analyzeObject(
    bitmap: Bitmap,
    apiKey: String,
    profile: UserProfile,
    remCals: Int,
    remPro: Double,
    remCarbs: Double,
    remFat: Double
): String? {
    val prompt = """
Identify the primary food in this image. Return ONLY valid JSON.
Schema:
{
  "name": string,
  "calories": number,
  "protein": number,
  "carbs": number,
  "fats": number,
  "fiber": number,
  "score": number,
  "shouldEat": boolean,
  "recommendation": string
}
Rules:
- Use nutrition for a realistic single serving seen in photo.
- If branded package text is visible, prefer package nutrition values.
- Keep score in 1..10 based on general healthiness.
- "shouldEat": Analyze the remaining daily targets below. Return true if this serving reasonably fits into their remaining goals without heavily exceeding them, false otherwise.
- "recommendation": Give a short 1-2 sentence tailored explanation to the user on why they should or should not eat this based strictly on their remaining targets below.

User's REMAINING Daily Targets:
Calories left: $remCals kcal
Protein left: ${remPro}g
Carbs left: ${remCarbs}g
Fats left: ${remFat}g

Fitness context:
${buildFitnessContext(profile)}
""".trimIndent()

    val input = content {
        image(bitmap)
        text(prompt)
    }

    val strictModel = GenerativeModel(
        modelName = "gemini-3-flash-preview",
        apiKey = apiKey,
        generationConfig = generationConfig { responseMimeType = "application/json" }
    )

    val relaxedModel = GenerativeModel(
        modelName = "gemini-3-flash-preview",
        apiKey = apiKey
    )

    val strictText = runCatching {
        strictModel.generateContent(input).text
    }.getOrNull()?.takeIf { it.isNotBlank() }

    if (strictText != null) return strictText
    return relaxedModel.generateContent(input).text
}

private fun parseGeminiNutritionResponse(responseText: String): FoodAnalysisResult? {
    val strictJson = parseShoppingJson(responseText) ?: return null
    val food = strictJson.optString("food", strictJson.optString("itemName", strictJson.optString("name", "Food Item"))).ifBlank { "Food Item" }

    val calories = strictJson.optDouble("calories", 120.0).toInt().coerceIn(20, 2500)
    val protein = strictJson.optDouble("protein", 3.0).coerceIn(0.0, 120.0)
    val carbs = strictJson.optDouble("carbs", 16.0).coerceIn(0.0, 250.0)
    val fats = strictJson.optDouble("fats", strictJson.optDouble("fat", 4.0)).coerceIn(0.0, 120.0)
    val fiber = strictJson.optDouble("fiber", 2.0).coerceIn(0.0, 80.0)
    val score = strictJson.optInt("score", 5).coerceIn(1, 10)
    val shouldEat = strictJson.optBoolean("shouldEat", true)
    val recommendation = strictJson.optString("recommendation", "Fits reasonably well into a standard diet.")

    return FoodAnalysisResult(
        itemName = food,
        calories = calories,
        protein = "${protein.format1()}g",
        fat = "${fats.format1()}g",
        carbs = "${carbs.format1()}g",
        fiber = "${fiber.format1()}g",
        score = score,
        shouldEat = shouldEat,
        recommendation = recommendation
    )
}

private fun parseShoppingJson(text: String): JSONObject? {
    return runCatching {
        val clean = text.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        try {
            JSONObject(clean)
        } catch (_: Throwable) {
            val start = clean.indexOf('{')
            val end = clean.lastIndexOf('}')
            if (start >= 0 && end > start) JSONObject(clean.substring(start, end + 1)) else throw IllegalStateException("No JSON object found")
        }
    }.getOrNull()
}

private fun decodeUriToBitmap(context: Context, uri: Uri): Bitmap? {
    return runCatching {
        context.contentResolver.openInputStream(uri).use { stream ->
            if (stream == null) null else BitmapFactory.decodeStream(stream)
        }
    }.getOrNull()
}

private fun persistFoodBitmap(context: Context, id: Long, bitmap: Bitmap): String? {
    return runCatching {
        val dir = File(context.filesDir, "food_history").apply { mkdirs() }
        val file = File(dir, "meal_${id}.jpg")
        file.outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
        }
        file.absolutePath
    }.getOrNull()
}

private fun saveFoodHistory(prefs: android.content.SharedPreferences, meals: List<LoggedMeal>) {
    val array = JSONArray()
    meals.forEach { meal ->
        array.put(
            JSONObject()
                .put("id", meal.id)
                .put("photoPath", meal.photoPath)
                .put("itemName", meal.result.itemName)
                .put("calories", meal.result.calories)
                .put("protein", meal.result.protein)
                .put("fat", meal.result.fat)
                .put("carbs", meal.result.carbs)
                .put("fiber", meal.result.fiber)
                .put("score", meal.result.score)
                .put("shouldEat", meal.result.shouldEat)
                .put("recommendation", meal.result.recommendation)
        )
    }
    prefs.edit().putString(KEY_FOOD_HISTORY, array.toString()).apply()
}

private fun loadFoodHistory(prefs: android.content.SharedPreferences): List<LoggedMeal> {
    val raw = prefs.getString(KEY_FOOD_HISTORY, null) ?: return emptyList()
    return runCatching {
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val photoPath = obj.optString("photoPath")
                if (photoPath.isBlank() || !File(photoPath).exists()) continue
                add(
                    LoggedMeal(
                        id = obj.optLong("id"),
                        photoPath = photoPath,
                        result = FoodAnalysisResult(
                            itemName = obj.optString("itemName", "Food Item"),
                            calories = obj.optInt("calories", 120),
                            protein = obj.optString("protein", "3.0g"),
                            fat = obj.optString("fat", "4.0g"),
                            carbs = obj.optString("carbs", "16.0g"),
                            fiber = obj.optString("fiber", "2.0g"),
                            score = obj.optInt("score", 5).coerceIn(1, 10),
                            shouldEat = obj.optBoolean("shouldEat", true),
                            recommendation = obj.optString("recommendation", "")
                        )
                    )
                )
            }
        }
    }.getOrDefault(emptyList())
}