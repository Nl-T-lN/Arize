package com.example.arize

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

// ---------- Data ----------

enum class WorkoutStatus { COMPLETED, MISSED, REST }

private const val KEY_CALENDAR_DATA = "calendar_workout_data"

// ---------- Colors ----------

private val CalCompleted = Color(0xFFF59E0B) // Amber/Yellow-orange
private val CalStreak = Color(0xFF34D399)     // Green
private val CalMissed = Color(0xFFEF4444)     // Red
private val CalToday = Color(0xFF60A5FA)      // Blue
private val CalDefault = Color.Transparent
private val CalCardBg = Color(0xFF131A2B)
private val CalHeaderDay = Color(0xFF64748B)

// ---------- Main composable ----------

@Composable
fun FitnessCalendar(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val workoutMap = remember { mutableStateMapOf<String, WorkoutStatus>().apply { putAll(loadCalendarData(prefs)) } }
    var displayMonth by remember { mutableStateOf(YearMonth.now()) }
    val today = LocalDate.now()

    val currentStreak = computeStreak(workoutMap, today)

    // Weekly progress: how many of the last 7 days have workouts
    val weeklyCount = (0L until 7L).count { i ->
        val d = today.minusDays(i).toString()
        workoutMap[d] == WorkoutStatus.COMPLETED
    }

    Column(modifier = modifier) {
        // ---------- Streak banner ----------
        Card(
            colors = CardDefaults.cardColors(containerColor = CalCardBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = Color(0xFFFF6B35),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (currentStreak > 0) "$currentStreak-day streak!" else "No active streak",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        if (currentStreak > 0) "Keep going! Don't break the chain."
                        else "Tap a date to log your workout.",
                        color = CalHeaderDay,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ---------- Calendar card ----------
        Card(
            colors = CardDefaults.cardColors(containerColor = CalCardBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Month nav
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { displayMonth = displayMonth.minusMonths(1) }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous", tint = Color.White)
                    }
                    Text(
                        text = "${displayMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${displayMonth.year}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    IconButton(onClick = { displayMonth = displayMonth.plusMonths(1) }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Day-of-week headers
                val dayHeaders = listOf("S", "M", "T", "W", "T", "F", "S")
                Row(modifier = Modifier.fillMaxWidth()) {
                    dayHeaders.forEach { d ->
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text(d, color = CalHeaderDay, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Build calendar grid
                val firstOfMonth = displayMonth.atDay(1)
                // Sunday = 0 offset
                val startOffset = (firstOfMonth.dayOfWeek.value % 7) // Monday=1..Sunday=7 -> Sun=0
                val daysInMonth = displayMonth.lengthOfMonth()

                val totalCells = startOffset + daysInMonth
                val rows = (totalCells + 6) / 7

                for (row in 0 until rows) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (col in 0..6) {
                            val cellIndex = row * 7 + col
                            val dayNum = cellIndex - startOffset + 1

                            if (dayNum < 1 || dayNum > daysInMonth) {
                                // Empty / overflow cell
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Show overflow dates faintly
                                    if (dayNum < 1) {
                                        val prevMonth = displayMonth.minusMonths(1)
                                        val overflowDay = prevMonth.lengthOfMonth() + dayNum
                                        Text(
                                            overflowDay.toString(),
                                            color = Color(0xFF2D3748),
                                            fontSize = 14.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    } else {
                                        val overflowDay = dayNum - daysInMonth
                                        Text(
                                            overflowDay.toString(),
                                            color = Color(0xFF2D3748),
                                            fontSize = 14.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                val date = displayMonth.atDay(dayNum)
                                val dateKey = date.toString()
                                val status = workoutMap[dateKey]
                                val isToday = date == today

                                // Determine streak membership for pill shapes
                                val isInStreak = isPartOfStreak(dateKey, workoutMap)
                                val prevInStreak = dayNum > 1 && isPartOfStreak(displayMonth.atDay(dayNum - 1).toString(), workoutMap)
                                val nextInStreak = dayNum < daysInMonth && isPartOfStreak(displayMonth.atDay(dayNum + 1).toString(), workoutMap)

                                // Also check cross-column for pill: don't connect across rows
                                val isStreakStart = isInStreak && (col == 0 || !prevInStreak)
                                val isStreakEnd = isInStreak && (col == 6 || !nextInStreak)
                                val isStreakMid = isInStreak && !isStreakStart && !isStreakEnd

                                val bgColor = when {
                                    status == WorkoutStatus.MISSED -> CalMissed
                                    isInStreak && streakLength(dateKey, workoutMap) >= 3 -> CalStreak
                                    status == WorkoutStatus.COMPLETED -> CalCompleted
                                    isToday -> CalToday
                                    else -> CalDefault
                                }

                                val animatedBg by animateColorAsState(
                                    targetValue = bgColor,
                                    animationSpec = tween(250),
                                    label = "dayBg"
                                )

                                val shape = when {
                                    isInStreak && isStreakStart && isStreakEnd -> RoundedCornerShape(50)
                                    isInStreak && isStreakStart -> RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50, topEndPercent = 0, bottomEndPercent = 0)
                                    isInStreak && isStreakEnd -> RoundedCornerShape(topStartPercent = 0, bottomStartPercent = 0, topEndPercent = 50, bottomEndPercent = 50)
                                    isInStreak && isStreakMid -> RoundedCornerShape(0.dp)
                                    isToday && bgColor == CalToday -> CircleShape
                                    status == WorkoutStatus.MISSED -> CircleShape
                                    status == WorkoutStatus.COMPLETED -> CircleShape
                                    else -> CircleShape
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(vertical = 2.dp)
                                        .clip(shape)
                                        .background(animatedBg)
                                        .clickable {
                                            if (date.isAfter(today)) return@clickable // Don't allow future
                                            val current = workoutMap[dateKey]
                                            val next = when (current) {
                                                null, WorkoutStatus.REST -> WorkoutStatus.COMPLETED
                                                WorkoutStatus.COMPLETED -> WorkoutStatus.MISSED
                                                WorkoutStatus.MISSED -> WorkoutStatus.REST
                                            }
                                            if (next == WorkoutStatus.REST) {
                                                workoutMap.remove(dateKey)
                                            } else {
                                                workoutMap[dateKey] = next
                                            }
                                            saveCalendarData(prefs, workoutMap)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        dayNum.toString(),
                                        color = if (bgColor != CalDefault) Color.White else Color(0xFFD4D4D8),
                                        fontSize = 14.sp,
                                        fontWeight = if (isToday || status != null) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ---------- Weekly progress ----------
        Card(
            colors = CardDefaults.cardColors(containerColor = CalCardBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Weekly Progress", color = Color.White, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("$weeklyCount / 7 days", color = CalHeaderDay, fontSize = 13.sp)
                    Text(
                        when {
                            weeklyCount >= 6 -> "🏆 Amazing!"
                            weeklyCount >= 4 -> "💪 Great job!"
                            weeklyCount >= 2 -> "👍 Keep going!"
                            else -> "🔥 Start today!"
                        },
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { (weeklyCount / 7f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = when {
                        weeklyCount >= 5 -> CalStreak
                        weeklyCount >= 3 -> CalCompleted
                        else -> CalToday
                    },
                    trackColor = Color(0xFF1E293B),
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Day pills for the week
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (i in 6 downTo 0) {
                        val d = today.minusDays(i.toLong())
                        val dayLabel = d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2)
                        val status = workoutMap[d.toString()]
                        val dotColor = when (status) {
                            WorkoutStatus.COMPLETED -> CalCompleted
                            WorkoutStatus.MISSED -> CalMissed
                            else -> Color(0xFF1E293B)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(dayLabel, color = CalHeaderDay, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ---------- Legend ----------
        Card(
            colors = CardDefaults.cardColors(containerColor = CalCardBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendDot("Today", CalToday)
                LegendDot("Done", CalCompleted)
                LegendDot("Streak", CalStreak)
                LegendDot("Missed", CalMissed)
            }
        }
    }
}

// ---------- Small helpers ----------

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, color = CalHeaderDay, fontSize = 11.sp)
    }
}

fun computeStreak(map: Map<String, WorkoutStatus>, today: LocalDate): Int {
    var count = 0
    var date = today
    while (true) {
        val s = map[date.toString()]
        if (s == WorkoutStatus.COMPLETED) {
            count++
            date = date.minusDays(1)
        } else {
            break
        }
    }
    return count
}

private fun isPartOfStreak(dateKey: String, map: Map<String, WorkoutStatus>): Boolean {
    return map[dateKey] == WorkoutStatus.COMPLETED
}

private fun streakLength(dateKey: String, map: Map<String, WorkoutStatus>): Int {
    val date = LocalDate.parse(dateKey)
    var count = 1
    // count backwards
    var d = date.minusDays(1)
    while (map[d.toString()] == WorkoutStatus.COMPLETED) { count++; d = d.minusDays(1) }
    // count forwards
    d = date.plusDays(1)
    while (map[d.toString()] == WorkoutStatus.COMPLETED) { count++; d = d.plusDays(1) }
    return count
}

// ---------- Persistence ----------

private fun saveCalendarData(prefs: SharedPreferences, map: Map<String, WorkoutStatus>) {
    val json = JSONObject()
    map.forEach { (k, v) -> json.put(k, v.name) }
    prefs.edit().putString(KEY_CALENDAR_DATA, json.toString()).apply()
}

private fun loadCalendarData(prefs: SharedPreferences): Map<String, WorkoutStatus> {
    val raw = prefs.getString(KEY_CALENDAR_DATA, null) ?: return emptyMap()
    return runCatching {
        val json = JSONObject(raw)
        buildMap {
            json.keys().forEach { key ->
                val status = runCatching { WorkoutStatus.valueOf(json.getString(key)) }.getOrNull()
                if (status != null) put(key, status)
            }
        }
    }.getOrDefault(emptyMap())
}

fun loadCalendarDataPublic(prefs: SharedPreferences): Map<String, WorkoutStatus> {
    return loadCalendarData(prefs)
}

fun saveCalendarDataPublic(prefs: SharedPreferences, map: Map<String, WorkoutStatus>) {
    saveCalendarData(prefs, map)
}
