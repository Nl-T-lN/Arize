package com.example.arize

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import coil.compose.AsyncImage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.util.ArrayDeque

private enum class GymTab(val label: String) {
    PLAN("PLAN"),
    EXERCISES("EXERCISES")
}

private enum class CoachMode {
    BICEP_CURL,
    SQUAT
}

private data class WorkoutDay(
    val dayIndex: Int,
    val focus: String,
)

private data class PoseOverlay(
    val points: Map<String, Offset> = emptyMap(),
    val mirrored: Boolean = true,
)

private data class LivePoseState(
    val reps: Int = 0,
    val stage: String = "--",
    val angle: Int = 0,
    val feedback: String = "Stand in frame to begin",
    val overlay: PoseOverlay = PoseOverlay(),
)

private val overlayConnections = listOf(
    "left_shoulder" to "left_elbow",
    "left_elbow" to "left_wrist",
    "right_shoulder" to "right_elbow",
    "right_elbow" to "right_wrist",
    "left_shoulder" to "right_shoulder",
    "left_hip" to "right_hip",
    "left_shoulder" to "left_hip",
    "right_shoulder" to "right_hip",
    "left_hip" to "left_knee",
    "left_knee" to "left_ankle",
    "right_hip" to "right_knee",
    "right_knee" to "right_ankle"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymPage(
    profile: UserProfile,
    modifier: Modifier = Modifier,
    onExerciseTouched: (String) -> Unit,
) {
    val allExercises = remember { buildExerciseCatalog() }
    val planDays = remember(profile, allExercises) { buildWorkoutPlan(profile, allExercises) }
    var selectedTab by rememberSaveable { mutableStateOf(GymTab.PLAN) }
    var selectedPart by rememberSaveable { mutableStateOf("All") }
    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }
    var activeCoachExercise by remember { mutableStateOf<Exercise?>(null) }
    val completedExercises = remember { mutableStateListOf<String>() }

    if (activeCoachExercise != null) {
        LiveWorkoutCoachFullscreen(
            exercise = activeCoachExercise!!,
            modifier = modifier,
            onBack = { activeCoachExercise = null },
            onTargetComplete = {
                val title = activeCoachExercise!!.title
                if (!completedExercises.contains(title)) {
                    completedExercises.add(title)
                }
                activeCoachExercise = null
            }
        )
        return
    }

    val visibleExercises = allExercises.filter { selectedPart == "All" || it.bodyPart == selectedPart }

    Box(modifier = modifier.fillMaxSize().background(AppDark)) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("EXERCISES", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.White)
                }
                Spacer(modifier = Modifier.height(10.dp))
                GymTopTabs(selected = selectedTab, onSelect = { selectedTab = it })
            }

            if (selectedTab == GymTab.PLAN) {
                item { PlanHeroCard(profile = profile) }
                items(planDays) { day ->
                    PlanDayCard(
                        day = day,
                        unlocked = day.dayIndex == 1,
                        onStart = {
                            selectedTab = GymTab.EXERCISES
                            selectedPart = day.focus
                        }
                    )
                }
            } else {
                item {
                    val parts = listOf("All", "Arms", "Chest", "Back", "Shoulder", "Legs", "Abs")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        parts.forEach { part ->
                            FilterChip(
                                selected = selectedPart == part,
                                onClick = { selectedPart = part },
                                label = { Text(part) }
                            )
                        }
                    }
                }

                items(visibleExercises) { exercise ->
                    ExerciseCard(
                        exercise = exercise,
                        completed = completedExercises.contains(exercise.title),
                        onOpenDetails = {
                            onExerciseTouched(exercise.bodyPart)
                            selectedExercise = exercise
                        }
                    )
                }
            }
        }

        if (selectedExercise != null) {
            val detailsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { selectedExercise = null },
                sheetState = detailsSheetState,
                containerColor = CardDark
            ) {
                ExerciseDetailsSheet(
                    exercise = selectedExercise!!,
                    onStart = {
                        activeCoachExercise = selectedExercise
                        selectedExercise = null
                    },
                    onClose = { selectedExercise = null }
                )
            }
        }
    }
}

@Composable
private fun GymTopTabs(selected: GymTab, onSelect: (GymTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF161C2A), RoundedCornerShape(18.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GymTab.entries.forEach { tab ->
            val selectedTab = tab == selected
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelect(tab) },
                color = if (selectedTab) AccentBlue else Color.Transparent
            ) {
                Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                    Text(tab.label, color = if (selectedTab) Color.White else MutedText, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PlanHeroCard(profile: UserProfile) {
    Card(colors = CardDefaults.cardColors(containerColor = CardDark), shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = "https://images.pexels.com/photos/949130/pexels-photo-949130.jpeg",
                contentDescription = "Strength plan",
                modifier = Modifier.fillMaxWidth().height(160.dp),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(14.dp)) {
                Text("STRENGTH TRAINING", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text("${profile.workoutDays.size * 4} days • Goal ${profile.targetWeightKg.format1()}kg", color = MutedText)
            }
        }
    }
}

@Composable
private fun PlanDayCard(day: WorkoutDay, unlocked: Boolean, onStart: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (unlocked) AccentBlue else CardDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Day ${day.dayIndex}", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(day.focus, color = if (unlocked) Color.White else MutedText)
            }
            if (unlocked) {
                OutlinedButton(onClick = onStart) {
                    Text("Start", fontWeight = FontWeight.Bold)
                }
            } else {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF8A90A0))
            }
        }
    }
}

@Composable
private fun ExerciseCard(exercise: Exercise, completed: Boolean, onOpenDetails: () -> Unit) {
    Card(
        modifier = Modifier.clickable { onOpenDetails() },
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = exercise.imageUrl,
                contentDescription = exercise.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2D3342))
            )
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(exercise.title, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text("${exercise.bodyPart} • ${exercise.sets} sets x ${exercise.reps} reps", color = MutedText, style = MaterialTheme.typography.bodySmall)
                if (completed) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF7EE787), modifier = Modifier.size(14.dp))
                        Text("Completed", color = Color(0xFF7EE787), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = AccentBlue)
        }
    }
}

@Composable
private fun ExerciseDetailsSheet(
    exercise: Exercise,
    onStart: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("INSTRUCTIONS", color = Color.White, fontWeight = FontWeight.Bold)
                Text("RECORDS", color = MutedText)
            }
        }

        item {
            ExerciseVideoPlayer(videoUrl = exercise.videoUrl)
        }

        item {
            Text(exercise.title, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("FOCUS AREA  ${exercise.focusAreas.joinToString()}", color = Color(0xFFDCE2F3))
            Text("EQUIPMENT  ${exercise.equipment}", color = Color(0xFFDCE2F3))
        }

        item {
            Text("PREPARATION", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            exercise.preparation.forEachIndexed { index, step ->
                Text("${index + 1}. $step", color = Color(0xFFD0D6E8), modifier = Modifier.padding(top = 6.dp))
            }
        }

        item {
            Text("EXECUTION", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            exercise.execution.forEachIndexed { index, step ->
                Text("${index + 1}. $step", color = Color(0xFFD0D6E8), modifier = Modifier.padding(top = 6.dp))
            }
        }

        item {
            Text("KEY TIPS", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            exercise.keyTips.forEachIndexed { index, step ->
                Text("${index + 1}. $step", color = Color(0xFFD0D6E8), modifier = Modifier.padding(top = 6.dp))
            }
        }

        item {
            OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(exercise.sourceUrl))) }, modifier = Modifier.fillMaxWidth()) {
                Text("Read Source")
                Icon(Icons.Default.ArrowOutward, contentDescription = null, modifier = Modifier.padding(start = 6.dp))
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(12.dp)) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
                Button(onClick = onStart, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(12.dp)) {
                    Text("START", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ExerciseVideoPlayer(videoUrl: String) {
    val context = LocalContext.current

    if (videoUrl.startsWith("android.resource://")) {
        AndroidView(
            factory = {
                android.widget.VideoView(context).apply {
                    setVideoURI(Uri.parse(videoUrl))
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                        start()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black)
        )
    } else {
        AndroidView(
            factory = {
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true // Crucial for modern YouTube mobile layout
                    settings.mediaPlaybackRequiresUserGesture = false

                    // Keep navigation contained within the WebView itself
                    webViewClient = WebViewClient()

                    loadUrl(videoUrl)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp) // Taller height to comfortably view search results
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black)
        )
    }
}

@Composable
private fun LiveWorkoutCoachFullscreen(
    exercise: Exercise,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onTargetComplete: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageAnalysis = remember {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
    }
    val previewView = remember { PreviewView(context) }

    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }

    var poseState by remember { mutableStateOf(LivePoseState()) }
    var completedMarked by remember { mutableStateOf(false) }
    val targetReps = exercise.sets * exercise.reps
    val mode = if (exercise.title.contains("curl", ignoreCase = true) || exercise.bodyPart == "Arms") {
        CoachMode.BICEP_CURL
    } else {
        CoachMode.SQUAT
    }

    DisposableEffect(Unit) {
        onDispose {
            imageAnalysis.clearAnalyzer()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(hasPermission, exercise.title) {
        if (!hasPermission) return@LaunchedEffect
        bindCoachCamera(
            context = context,
            lifecycleOwner = lifecycleOwner,
            previewView = previewView,
            imageAnalysis = imageAnalysis,
            mode = mode,
            onPoseUpdate = { poseState = it }
        )
    }

    LaunchedEffect(poseState.reps) {
        if (!completedMarked && poseState.reps >= targetReps) {
            completedMarked = true
            onTargetComplete()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        if (hasPermission) {
            AndroidView(
                factory = {
                    previewView.apply { scaleX = -1f }
                },
                modifier = Modifier.fillMaxSize()
            )
            PoseOverlayCanvas(overlay = poseState.overlay, modifier = Modifier.fillMaxSize())
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Grant Camera Permission")
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xAA0F172A),
                modifier = Modifier.clickable { onBack() }
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                    Text("Back", color = Color.White, modifier = Modifier.padding(start = 4.dp))
                }
            }
            Surface(shape = RoundedCornerShape(20.dp), color = Color(0xAA0F172A)) {
                Text(exercise.title, color = Color.White, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontWeight = FontWeight.SemiBold)
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
            color = Color(0xEE0F1422)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    CoachMetricTile("Reps", "${poseState.reps} / $targetReps", Modifier.weight(1f))
                    CoachMetricTile("Stage", poseState.stage, Modifier.weight(1f))
                    CoachMetricTile("Angle", if (poseState.angle == 0) "--" else "${poseState.angle}°", Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(10.dp))
                Surface(color = Color(0xFF1C2230), shape = RoundedCornerShape(12.dp)) {
                    Text(
                        text = poseState.feedback,
                        color = Color(0xFFDCE2F3),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Text("Stop Session", modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun PoseOverlayCanvas(overlay: PoseOverlay, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        fun mapPoint(p: Offset): Offset {
            val x = if (overlay.mirrored) size.width * (1f - p.x) else size.width * p.x
            val y = size.height * p.y
            return Offset(x, y)
        }

        overlayConnections.forEach { (a, b) ->
            val p1 = overlay.points[a]
            val p2 = overlay.points[b]
            if (p1 != null && p2 != null) {
                drawLine(color = Color(0xFF3B82F6), start = mapPoint(p1), end = mapPoint(p2), strokeWidth = 6f)
            }
        }

        overlay.points.values.forEach { p ->
            drawCircle(color = Color(0xFF10B981), radius = 8f, center = mapPoint(p))
            drawCircle(color = Color(0xAA0F172A), radius = 10f, center = mapPoint(p), style = Stroke(width = 3f))
        }
    }
}

@Composable
private fun CoachMetricTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = Color(0xFF151A28), shape = RoundedCornerShape(12.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 10.dp)) {
            Text(label, color = MutedText, style = MaterialTheme.typography.labelSmall)
            Text(value, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun bindCoachCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    imageAnalysis: ImageAnalysis,
    mode: CoachMode,
    onPoseUpdate: (LivePoseState) -> Unit,
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
        // FIX: Use explicit setSurfaceProvider instead of property access
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val options = PoseDetectorOptions.Builder().setDetectorMode(PoseDetectorOptions.STREAM_MODE).build()
        val detector = PoseDetection.getClient(options)

        val repCounter = RepCounter(mode)
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
            processPoseFrame(imageProxy, detector, repCounter, onPoseUpdate)
        }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageAnalysis)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }, ContextCompat.getMainExecutor(context)) // FIX: Run asynchronously on the main executor
}

private fun processPoseFrame(
    imageProxy: ImageProxy,
    detector: com.google.mlkit.vision.pose.PoseDetector,
    repCounter: RepCounter,
    onPoseUpdate: (LivePoseState) -> Unit,
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        return
    }

    val rotation = imageProxy.imageInfo.rotationDegrees
    val width = if (rotation == 0 || rotation == 180) imageProxy.width.toFloat() else imageProxy.height.toFloat()
    val height = if (rotation == 0 || rotation == 180) imageProxy.height.toFloat() else imageProxy.width.toFloat()

    val image = InputImage.fromMediaImage(mediaImage, rotation)
    detector.process(image)
        .addOnSuccessListener { pose ->
            onPoseUpdate(repCounter.analyzePose(pose, width, height))
        }
        .addOnFailureListener {
            onPoseUpdate(LivePoseState(feedback = "Pose detection failed, adjust camera"))
        }
        .addOnCompleteListener {
            imageProxy.close()
        }
}

private class RepCounter(private val mode: CoachMode) {
    private var reps = 0
    private var stage = "--"
    private val angleWindow = ArrayDeque<Float>()

    fun analyzePose(pose: Pose, frameWidth: Float, frameHeight: Float): LivePoseState {
        return when (mode) {
            CoachMode.BICEP_CURL -> analyzeCurl(pose, frameWidth, frameHeight)
            CoachMode.SQUAT -> analyzeSquat(pose, frameWidth, frameHeight)
        }
    }

    private fun analyzeCurl(pose: Pose, frameWidth: Float, frameHeight: Float): LivePoseState {
        val ls = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val le = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val lw = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val rs = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val re = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
        val rw = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)

        val leftConfidence = (ls?.inFrameLikelihood ?: 0f) + (le?.inFrameLikelihood ?: 0f) + (lw?.inFrameLikelihood ?: 0f)
        val rightConfidence = (rs?.inFrameLikelihood ?: 0f) + (re?.inFrameLikelihood ?: 0f) + (rw?.inFrameLikelihood ?: 0f)

        val shoulder = if (leftConfidence >= rightConfidence) ls else rs
        val elbow = if (leftConfidence >= rightConfidence) le else re
        val wrist = if (leftConfidence >= rightConfidence) lw else rw
        val otherShoulder = if (leftConfidence >= rightConfidence) rs else ls

        if (shoulder == null || elbow == null || wrist == null) {
            return LivePoseState(reps = reps, stage = stage, feedback = "Ensure full arm is visible")
        }

        val angle = smooth(calculateAngle(shoulder.position.x, shoulder.position.y, elbow.position.x, elbow.position.y, wrist.position.x, wrist.position.y))
        if (angle > 150f) stage = "down"
        if (angle < 45f && stage == "down") {
            stage = "up"
            reps++
        }

        var feedback = "Good curl form. Keep elbows stable"
        if (otherShoulder != null && kotlin.math.abs(shoulder.position.y - otherShoulder.position.y) > 40f) {
            feedback = "Keep shoulders level"
        }

        return LivePoseState(
            reps = reps,
            stage = stage,
            angle = angle.toInt(),
            feedback = feedback,
            overlay = PoseOverlay(points = mapNormalizedPoints(pose, frameWidth, frameHeight), mirrored = true)
        )
    }

    private fun analyzeSquat(pose: Pose, frameWidth: Float, frameHeight: Float): LivePoseState {
        val lh = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val lk = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val la = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rh = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val rk = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val ra = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

        val leftConfidence = (lh?.inFrameLikelihood ?: 0f) + (lk?.inFrameLikelihood ?: 0f) + (la?.inFrameLikelihood ?: 0f)
        val rightConfidence = (rh?.inFrameLikelihood ?: 0f) + (rk?.inFrameLikelihood ?: 0f) + (ra?.inFrameLikelihood ?: 0f)

        val hip = if (leftConfidence >= rightConfidence) lh else rh
        val knee = if (leftConfidence >= rightConfidence) lk else rk
        val ankle = if (leftConfidence >= rightConfidence) la else ra

        if (hip == null || knee == null || ankle == null) {
            return LivePoseState(reps = reps, stage = stage, feedback = "Ensure full lower body is visible")
        }

        val angle = smooth(calculateAngle(hip.position.x, hip.position.y, knee.position.x, knee.position.y, ankle.position.x, ankle.position.y))
        if (angle > 160f) stage = "up"
        if (angle < 95f && stage == "up") {
            stage = "down"
            reps++
        }

        val feedback = if (angle < 70f) "Go slightly higher, avoid over-compressing knees" else "Great squat form"
        return LivePoseState(
            reps = reps,
            stage = stage,
            angle = angle.toInt(),
            feedback = feedback,
            overlay = PoseOverlay(points = mapNormalizedPoints(pose, frameWidth, frameHeight), mirrored = true)
        )
    }

    private fun mapNormalizedPoints(pose: Pose, frameWidth: Float, frameHeight: Float): Map<String, Offset> {
        fun landmarkOffset(type: Int): Offset? {
            val landmark = pose.getPoseLandmark(type) ?: return null
            return Offset(
                x = (landmark.position.x / frameWidth).coerceIn(0f, 1f),
                y = (landmark.position.y / frameHeight).coerceIn(0f, 1f)
            )
        }

        val map = mutableMapOf<String, Offset>()
        landmarkOffset(PoseLandmark.LEFT_SHOULDER)?.let { map["left_shoulder"] = it }
        landmarkOffset(PoseLandmark.LEFT_ELBOW)?.let { map["left_elbow"] = it }
        landmarkOffset(PoseLandmark.LEFT_WRIST)?.let { map["left_wrist"] = it }
        landmarkOffset(PoseLandmark.RIGHT_SHOULDER)?.let { map["right_shoulder"] = it }
        landmarkOffset(PoseLandmark.RIGHT_ELBOW)?.let { map["right_elbow"] = it }
        landmarkOffset(PoseLandmark.RIGHT_WRIST)?.let { map["right_wrist"] = it }
        landmarkOffset(PoseLandmark.LEFT_HIP)?.let { map["left_hip"] = it }
        landmarkOffset(PoseLandmark.RIGHT_HIP)?.let { map["right_hip"] = it }
        landmarkOffset(PoseLandmark.LEFT_KNEE)?.let { map["left_knee"] = it }
        landmarkOffset(PoseLandmark.RIGHT_KNEE)?.let { map["right_knee"] = it }
        landmarkOffset(PoseLandmark.LEFT_ANKLE)?.let { map["left_ankle"] = it }
        landmarkOffset(PoseLandmark.RIGHT_ANKLE)?.let { map["right_ankle"] = it }
        return map
    }

    private fun smooth(value: Float): Float {
        angleWindow.addLast(value)
        if (angleWindow.size > 5) angleWindow.removeFirst()
        return angleWindow.average().toFloat()
    }

    private fun calculateAngle(ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float): Float {
        val radians = kotlin.math.atan2(cy - by, cx - bx) - kotlin.math.atan2(ay - by, ax - bx)
        var angle = kotlin.math.abs((radians * 180f / Math.PI).toFloat())
        if (angle > 180f) angle = 360f - angle
        return angle
    }
}

private fun buildWorkoutPlan(profile: UserProfile, allExercises: List<Exercise>): List<WorkoutDay> {
    val split = when {
        profile.targetWeightKg < profile.currentWeightKg -> listOf("Arms", "Chest", "Back", "Legs", "Shoulder", "Abs")
        profile.targetWeightKg > profile.currentWeightKg -> listOf("Arms", "Chest", "Back", "Legs", "Shoulder")
        else -> listOf("Arms", "Chest", "Back", "Legs", "Shoulder")
    }

    return split.take(6).mapIndexed { index, focus -> WorkoutDay(dayIndex = index + 1, focus = focus) }
}