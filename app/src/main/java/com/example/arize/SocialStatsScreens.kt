package com.example.arize

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate
import kotlin.math.min

private const val BODY_VIEWBOX_WIDTH = 200f
private const val BODY_VIEWBOX_HEIGHT = 450f

private data class PolygonFacet(
    val p: String,
    val g: String,
    val s: String,
)

private data class MuscleChip(
    val label: String,
    val group: String,
)

private data class RankedPlayer(
    val name: String,
    val workout: Int,
    val food: Int,
    val icon: ImageVector,
    val avatarColor: Color,
)

private data class ParsedFacet(
    val path: Path,
    val minX: Float,
    val maxX: Float,
    val minY: Float,
    val maxY: Float,
)

@Composable
fun SocialPage(modifier: Modifier = Modifier) {
    val leaderboard = remember {
        listOf(
            RankedPlayer("Aarav", 820, 91, Icons.Default.AutoAwesome, Color(0xFF6D6BFF)),
            RankedPlayer("Priya", 760, 95, Icons.Default.Star, Color(0xFF00A6A6)),
            RankedPlayer("Riya", 730, 89, Icons.Default.LocalFireDepartment, Color(0xFFE8664A)),
            RankedPlayer("Vikram", 670, 84, Icons.Default.Bolt, Color(0xFF3F8CFF)),
            RankedPlayer("Nisha", 610, 86, Icons.Default.SportsEsports, Color(0xFF9B6BFF))
        ).sortedByDescending { it.workout + it.food }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(AppDark),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Community Rank", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Compete in league points: workout + nutrition.", color = MutedText, modifier = Modifier.padding(top = 6.dp))
        }

        item {
            val top3 = leaderboard.take(3)
            if (top3.size == 3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TopRankCard(rank = 2, player = top3[1], modifier = Modifier.weight(1f))
                    TopRankCard(rank = 1, player = top3[0], modifier = Modifier.weight(1f))
                    TopRankCard(rank = 3, player = top3[2], modifier = Modifier.weight(1f))
                }
            }
        }

        itemsIndexed(leaderboard) { index, player ->
            if (index < 3) return@itemsIndexed
            Card(colors = CardDefaults.cardColors(containerColor = CardDark), shape = RoundedCornerShape(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).background(Color(0xFF2E3445), CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Text((index + 1).toString(), color = Color.White, fontWeight = FontWeight.Bold) }

                    Surface(
                        shape = CircleShape,
                        color = player.avatarColor,
                        modifier = Modifier.padding(start = 10.dp).size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(player.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }

                    Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                        Text(player.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("Workout ${player.workout} • Food ${player.food}", color = MutedText, style = MaterialTheme.typography.bodySmall)
                    }
                    Text((player.workout + player.food).toString(), color = AccentBlue, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TopRankCard(
    rank: Int,
    player: RankedPlayer,
    modifier: Modifier = Modifier,
) {
    val rankLabel = when (rank) {
        1 -> "#1"
        2 -> "#2"
        else -> "#3"
    }
    val glow = when (rank) {
        1 -> Color(0xFFEAB308)
        2 -> Color(0xFF94A3B8)
        else -> Color(0xFFCD7F32)
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF171D2B)),
        border = BorderStroke(1.dp, glow.copy(alpha = 0.55f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(rankLabel, color = glow, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(8.dp))
            Surface(shape = CircleShape, color = player.avatarColor, modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(player.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(player.name, color = Color.White, fontWeight = FontWeight.Bold)
            Text("${player.workout + player.food} pts", color = AccentBlue, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatsPage(touchedBodyParts: List<String>, modifier: Modifier = Modifier) {
    StatsPage(touchedBodyParts = touchedBodyParts, modifier = modifier, profile = UserProfile())
}

private enum class StatsSection { HUB, CALENDAR, BODY_COVERAGE, CALORIES }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatsPage(touchedBodyParts: List<String>, modifier: Modifier = Modifier, profile: UserProfile) {
    var activeSectionName by rememberSaveable { mutableStateOf(StatsSection.HUB.name) }
    val activeSection = StatsSection.valueOf(activeSectionName)

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val calendarMap = remember { loadCalendarDataPublic(prefs) }
    val currentStreak = computeStreak(calendarMap, LocalDate.now())
    val caloriesToday = remember { mutableStateOf(getCaloriesToday(prefs)) }

    // Refresh calories when returning to hub
    if (activeSection == StatsSection.HUB) {
        caloriesToday.value = getCaloriesToday(prefs)
    }

    when (activeSection) {
        StatsSection.HUB -> StatsHub(
            modifier = modifier,
            currentStreak = currentStreak,
            caloriesToday = caloriesToday.value,
            touchedBodyParts = touchedBodyParts,
            onSectionSelected = { activeSectionName = it.name }
        )
        StatsSection.CALENDAR -> StatsDetailWrapper(
            title = "Workout Calendar",
            modifier = modifier,
            onBack = { activeSectionName = StatsSection.HUB.name }
        ) {
            FitnessCalendar()
        }
        StatsSection.BODY_COVERAGE -> BodyCoverageDetail(
            touchedBodyParts = touchedBodyParts,
            modifier = modifier,
            onBack = { activeSectionName = StatsSection.HUB.name }
        )
        StatsSection.CALORIES -> CalorieTrackerDetail(
            prefs = prefs,
            modifier = modifier,
            onBack = { activeSectionName = StatsSection.HUB.name }
        )
    }
}

// ---------- Hub view ----------

@Composable
private fun StatsHub(
    modifier: Modifier,
    currentStreak: Int,
    caloriesToday: Int,
    touchedBodyParts: List<String>,
    onSectionSelected: (StatsSection) -> Unit
) {
    val context = LocalContext.current
    val todayLog = remember { getCalorieLogToday(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)) }
    val loggedBodyParts = todayLog.map { it.bodyPart }.filter { it.isNotBlank() }
    val coverageCount = (touchedBodyParts + loggedBodyParts)
        .map(::normalizeTouchedGroup)
        .filter { it != "None" }
        .distinct()
        .size

    LazyColumn(
        modifier = modifier.fillMaxSize().background(AppDark),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                "YOUR STATS",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text("Tap a card to view details", color = MutedText, modifier = Modifier.padding(top = 4.dp))
        }

        item {
            StatsHubCard(
                icon = Icons.Default.CalendarMonth,
                title = "Workout Calendar",
                subtitle = if (currentStreak > 0) "🔥 $currentStreak-day streak" else "Start tracking today",
                accentColor = Color(0xFF60A5FA),
                onClick = { onSectionSelected(StatsSection.CALENDAR) }
            )
        }

        item {
            StatsHubCard(
                icon = Icons.Default.FitnessCenter,
                title = "Body Coverage",
                subtitle = "$coverageCount muscle groups trained",
                accentColor = Color(0xFF3B82F6),
                onClick = { onSectionSelected(StatsSection.BODY_COVERAGE) }
            )
        }

        item {
            StatsHubCard(
                icon = Icons.Default.LocalFireDepartment,
                title = "Calorie Tracker",
                subtitle = "$caloriesToday kcal burned today",
                accentColor = Color(0xFFF59E0B),
                onClick = { onSectionSelected(StatsSection.CALORIES) }
            )
        }
    }
}

@Composable
private fun StatsHubCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(18.dp),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = accentColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(26.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(subtitle, color = MutedText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
            }
            Text("→", color = accentColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ---------- Detail wrapper ----------

@Composable
private fun StatsDetailWrapper(
    title: String,
    modifier: Modifier,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().background(AppDark),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(12.dp),
                    onClick = onBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text("←", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
        }
        item {
            content()
        }
    }
}

// ---------- Body Coverage Detail ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BodyCoverageDetail(
    touchedBodyParts: List<String>,
    modifier: Modifier,
    onBack: () -> Unit
) {
    val chips = remember {
        listOf(
            MuscleChip("Chest", "Chest"),
            MuscleChip("Abs", "Abs"),
            MuscleChip("Back", "Back"),
            MuscleChip("Shoulder", "Shoulder"),
            MuscleChip("Arms", "Arm"),
            MuscleChip("Legs", "Leg"),
            MuscleChip("Butt", "Butt")
        )
    }
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val todayLog = remember { getCalorieLogToday(prefs) }
    val loggedBodyParts = todayLog.map { it.bodyPart }.filter { it.isNotBlank() }
    val calendarMap = remember { loadCalendarDataPublic(prefs) }
    val currentStreak = computeStreak(calendarMap, LocalDate.now())

    val selectedMuscles = remember {
        mutableStateListOf<String>().apply {
            (touchedBodyParts + loggedBodyParts)
                .map(::normalizeTouchedGroup)
                .filter { it != "None" }
                .distinct()
                .forEach { add(it) }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(AppDark),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(12.dp),
                    onClick = onBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text("←", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("Body Coverage", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
        }

        item {
            Text("Tap chips to highlight low-poly muscle groups.", color = MutedText)
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = CardDark), shape = RoundedCornerShape(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BodyMapCard(title = "Front", facets = frontPolygons, selectedMuscles = selectedMuscles, modifier = Modifier.weight(1f))
                    BodyMapCard(title = "Back", facets = backPolygons, selectedMuscles = selectedMuscles, modifier = Modifier.weight(1f))
                }
            }
        }

        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                chips.forEach { chip ->
                    val selected = selectedMuscles.contains(chip.group)
                    val chipColor by animateColorAsState(
                        targetValue = if (selected) Color(0xFF3B82F6) else Color(0xFF27272A),
                        animationSpec = tween(durationMillis = 220), label = "chipColor"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (selected) Color.White else Color(0xFFD4D4D8),
                        animationSpec = tween(durationMillis = 220), label = "chipTextColor"
                    )
                    Surface(
                        color = chipColor,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (selected) Color(0xFF60A5FA) else Color(0xFF3F3F46)),
                        onClick = { if (selected) selectedMuscles.remove(chip.group) else selectedMuscles.add(chip.group) }
                    ) {
                        Text(chip.label, color = textColor, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        item {
            val workoutsThisWeek = remember(calendarMap) {
                val today = LocalDate.now()
                (0L..6L).count { i -> calendarMap[today.minusDays(i).toString()] == WorkoutStatus.COMPLETED }
            }
            Card(colors = CardDefaults.cardColors(containerColor = CardDark), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("This Week", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricPill("Workouts", workoutsThisWeek.toString())
                        MetricPill("Streak", if (currentStreak > 0) "$currentStreak days" else "0")
                        MetricPill("Coverage", "${if (chips.isNotEmpty()) (selectedMuscles.size * 100) / chips.size else 0}%")
                    }
                }
            }
        }
    }
}

// ---------- Calorie Tracker Detail ----------

@Composable
private fun CalorieTrackerDetail(
    prefs: android.content.SharedPreferences,
    modifier: Modifier,
    onBack: () -> Unit
) {
    val today = LocalDate.now()
    val caloriesToday = getCaloriesToday(prefs)
    val todayLog = remember { getCalorieLogToday(prefs) }
    val last7Days = remember {
        (6 downTo 0).map { i ->
            val d = today.minusDays(i.toLong())
            d to getCaloriesForDate(prefs, d)
        }
    }
    val maxCal = (last7Days.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)

    LazyColumn(
        modifier = modifier.fillMaxSize().background(AppDark),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color(0xFF1E293B), shape = RoundedCornerShape(12.dp),
                    onClick = onBack, modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text("←", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("Calorie Tracker", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
        }

        // Big calorie display
        item {
            Card(colors = CardDefaults.cardColors(containerColor = CardDark), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔥", fontSize = 40.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("$caloriesToday", color = Color(0xFFF59E0B), fontSize = 56.sp, fontWeight = FontWeight.ExtraBold)
                    Text("kcal burned today", color = MutedText, fontSize = 14.sp)
                }
            }
        }

        // 7-day bar chart
        item {
            Card(colors = CardDefaults.cardColors(containerColor = CardDark), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Last 7 Days", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        last7Days.forEach { (date, cals) ->
                            val isToday = date == today
                            val barColor = if (isToday) Color(0xFFF59E0B) else Color(0xFF3B82F6)
                            val ratio = if (maxCal > 0) (cals.toFloat() / maxCal) else 0f

                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text(if (cals > 0) "$cals" else "", color = MutedText, fontSize = 9.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(24.dp)
                                        .height((ratio * 80).dp.coerceAtLeast(4.dp))
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(barColor)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault()).take(2),
                                    color = if (isToday) Color.White else MutedText,
                                    fontSize = 11.sp,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        // Today's exercise log
        if (todayLog.isNotEmpty()) {
            item {
                Text("Today's Breakdown", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
            items(todayLog.size) { index ->
                val entry = todayLog[index]
                Card(colors = CardDefaults.cardColors(containerColor = CardDark), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = Color(0xFFF59E0B).copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp), modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Text("💪", fontSize = 18.sp) }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.exerciseName, color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text(entry.bodyPart, color = MutedText, fontSize = 12.sp)
                        }
                        Text("${entry.calories} kcal", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = CardDark), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No exercises logged today", color = MutedText)
                        Text("Complete a workout to see calorie data here!", color = MutedText, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun BodyMapCard(
    title: String,
    facets: List<PolygonFacet>,
    selectedMuscles: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(Color(0xFF121827), RoundedCornerShape(14.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = MutedText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Aspect ratio ensures the canvas matches the 200x450 proportion of the raw data
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(200f / 450f) 
        ) {
            val originalWidth = 200f
            val originalHeight = 450f

            val scaleFactor = min(size.width / originalWidth, size.height / originalHeight)
            val xOffset = (size.width - (originalWidth * scaleFactor)) / 2f
            val yOffset = (size.height - (originalHeight * scaleFactor)) / 2f

            withTransform({
                translate(left = xOffset, top = yOffset)
                scale(scaleX = scaleFactor, scaleY = scaleFactor, pivot = Offset.Zero)
            }) {
                facets.forEach { facet ->
                    val path = parsePolygonStringToPath(facet.p)
                    val targetColor = getPolygonColor(
                        group = facet.g, 
                        shade = facet.s, 
                        isSelected = selectedMuscles.contains(facet.g)
                    )

                    drawPath(
                        path = path,
                        color = targetColor
                    )

                    drawPath(
                        path = path,
                        color = Color(0xFF09090B),
                        style = Stroke(
                            width = 2.5f / scaleFactor, 
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }
    }
}

private fun parsePolygonStringToPath(polygonString: String): Path {
    val path = Path()
    val points = polygonString.trim().split(" ")
    if (points.isEmpty()) return path

    points.forEachIndexed { index, pointStr ->
        val coords = pointStr.split(",")
        if (coords.size == 2) {
            val x = coords[0].toFloatOrNull() ?: 0f
            val y = coords[1].toFloatOrNull() ?: 0f
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
    }
    path.close()
    return path
}

private fun getPolygonColor(group: String, shade: String, isSelected: Boolean): Color {
    if (group == "None") {
        return if (shade == "dark") Color(0xFF18181B) else Color(0xFF27272A)
    }

    return if (isSelected) {
        when (shade) {
            "light" -> Color(0xFF60A5FA)
            "dark" -> Color(0xFF1D4ED8)
            else -> Color(0xFF3B82F6)
        }
    } else {
        when (shade) {
            "light" -> Color(0xFF3F3F46)
            "dark" -> Color(0xFF18181B)
            else -> Color(0xFF27272A)
        }
    }
}

private fun normalizeTouchedGroup(group: String): String {
    return when (group.trim().lowercase()) {
        "arms", "arm" -> "Arm"
        "legs", "leg" -> "Leg"
        "shoulders", "shoulder" -> "Shoulder"
        "abs", "ab" -> "Abs"
        "chest" -> "Chest"
        "back" -> "Back"
        "butt", "glutes", "glute" -> "Butt"
        else -> "None"
    }
}

@Composable
private fun MetricPill(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color(0xFF22293A), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .widthIn(min = 82.dp)
    ) {
        Text(value, color = Color.White, fontWeight = FontWeight.Bold)
        Text(label, color = MutedText, style = MaterialTheme.typography.bodySmall)
    }
}

private val frontPolygons = listOf(
    PolygonFacet("100,20 85,20 90,40 100,60", "None", "base"),
    PolygonFacet("100,20 115,20 110,40 100,60", "None", "base"),
    PolygonFacet("100,60 90,60 95,75 100,75", "None", "dark"),
    PolygonFacet("100,60 110,60 105,75 100,75", "None", "dark"),
    PolygonFacet("95,75 60,90 50,115 65,125 75,85", "Shoulder", "base"),
    PolygonFacet("105,75 140,90 150,115 135,125 125,85", "Shoulder", "base"),
    PolygonFacet("100,75 95,75 75,85 70,110 100,110", "Chest", "light"),
    PolygonFacet("100,75 105,75 125,85 130,110 100,110", "Chest", "light"),
    PolygonFacet("70,110 75,135 100,135 100,110", "Chest", "base"),
    PolygonFacet("130,110 125,135 100,135 100,110", "Chest", "base"),
    PolygonFacet("100,135 75,135 80,160 100,165", "Abs", "light"),
    PolygonFacet("100,135 125,135 120,160 100,165", "Abs", "light"),
    PolygonFacet("100,165 80,160 82,185 100,190", "Abs", "base"),
    PolygonFacet("100,165 120,160 118,185 100,190", "Abs", "base"),
    PolygonFacet("100,190 82,185 85,210 100,215", "Abs", "dark"),
    PolygonFacet("100,190 118,185 115,210 100,215", "Abs", "dark"),
    PolygonFacet("75,135 65,165 75,200 82,185 80,160", "Abs", "dark"),
    PolygonFacet("125,135 135,165 125,200 118,185 120,160", "Abs", "dark"),
    PolygonFacet("65,125 50,115 40,155 55,165", "Arm", "base"),
    PolygonFacet("135,125 150,115 160,155 145,165", "Arm", "base"),
    PolygonFacet("55,165 40,155 30,210 45,215", "Arm", "light"),
    PolygonFacet("145,165 160,155 170,210 155,215", "Arm", "light"),
    PolygonFacet("45,215 30,210 25,235 35,240", "Arm", "dark"),
    PolygonFacet("155,215 170,210 175,235 165,240", "Arm", "dark"),
    PolygonFacet("100,215 85,210 75,225 100,250", "None", "base"),
    PolygonFacet("100,215 115,210 125,225 100,250", "None", "base"),
    PolygonFacet("75,225 55,280 65,320 80,315 85,250", "Leg", "base"),
    PolygonFacet("125,225 145,280 135,320 120,315 115,250", "Leg", "base"),
    PolygonFacet("85,250 80,315 95,320 100,250", "Leg", "light"),
    PolygonFacet("115,250 120,315 105,320 100,250", "Leg", "light"),
    PolygonFacet("65,320 95,320 90,340 70,340", "Leg", "dark"),
    PolygonFacet("135,320 105,320 110,340 130,340", "Leg", "dark"),
    PolygonFacet("70,340 60,390 75,430 85,425 90,340", "Leg", "base"),
    PolygonFacet("130,340 140,390 125,430 115,425 110,340", "Leg", "base"),
    PolygonFacet("75,430 85,425 95,445 70,445", "Leg", "dark"),
    PolygonFacet("125,430 115,425 105,445 130,445", "Leg", "dark"),
)

private val backPolygons = listOf(
    PolygonFacet("100,20 85,20 90,40 100,60", "None", "base"),
    PolygonFacet("100,20 115,20 110,40 100,60", "None", "base"),
    PolygonFacet("100,60 90,60 75,85 100,105", "Back", "light"),
    PolygonFacet("100,60 110,60 125,85 100,105", "Back", "light"),
    PolygonFacet("100,105 75,85 60,120 75,170 100,190", "Back", "base"),
    PolygonFacet("100,105 125,85 140,120 125,170 100,190", "Back", "base"),
    PolygonFacet("100,190 75,170 85,210 100,220", "Back", "dark"),
    PolygonFacet("100,190 125,170 115,210 100,220", "Back", "dark"),
    PolygonFacet("75,85 50,105 60,120", "Shoulder", "dark"),
    PolygonFacet("125,85 150,105 140,120", "Shoulder", "dark"),
    PolygonFacet("60,120 50,105 35,150 55,160", "Arm", "base"),
    PolygonFacet("140,120 150,105 165,150 145,160", "Arm", "base"),
    PolygonFacet("55,160 35,150 25,200 45,210", "Arm", "light"),
    PolygonFacet("145,160 165,150 175,200 155,210", "Arm", "light"),
    PolygonFacet("45,210 25,200 20,225 35,230", "Arm", "dark"),
    PolygonFacet("155,210 175,200 180,225 165,230", "Arm", "dark"),
    PolygonFacet("100,220 85,210 70,240 75,270 100,280", "Butt", "base"),
    PolygonFacet("100,220 115,210 130,240 125,270 100,280", "Butt", "base"),
    PolygonFacet("100,280 75,270 65,330 95,330", "Leg", "base"),
    PolygonFacet("100,280 125,270 135,330 105,330", "Leg", "base"),
    PolygonFacet("65,330 95,330 90,350 70,350", "Leg", "dark"),
    PolygonFacet("135,330 105,330 110,350 130,350", "Leg", "dark"),
    PolygonFacet("70,350 90,350 85,420 75,420", "Leg", "light"),
    PolygonFacet("130,350 110,350 115,420 125,420", "Leg", "light"),
    PolygonFacet("75,420 85,420 90,440 70,440", "Leg", "dark"),
    PolygonFacet("125,420 115,420 110,440 130,440", "Leg", "dark"),
)
