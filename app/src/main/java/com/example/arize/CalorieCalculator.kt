package com.example.arize

import android.content.SharedPreferences
import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

// ---------- MET lookup ----------

private val metMap = mapOf(
    "Chest" to 5.0,
    "Back" to 5.5,
    "Shoulder" to 4.0,
    "Arms" to 3.5,
    "Legs" to 6.0,
    "Abs" to 3.8
)

fun getMetForBodyPart(bodyPart: String): Double {
    return metMap[bodyPart] ?: 4.0
}

// ---------- Core calculation ----------

fun calculateCalories(weightKg: Double, durationMin: Int, met: Double): Int {
    return ((met * weightKg * durationMin) / 60.0).roundToInt()
}

fun estimateDuration(sets: Int, reps: Int): Int {
    // ~3 seconds per rep + 60s rest between sets
    val workSeconds = sets * reps * 3
    val restSeconds = (sets - 1).coerceAtLeast(0) * 60
    return ((workSeconds + restSeconds) / 60.0).roundToInt().coerceAtLeast(1)
}

// ---------- Persistence ----------

private const val KEY_DAILY_CALORIES_PREFIX = "daily_calories_"
private const val KEY_CALORIE_LOG = "calorie_exercise_log_"

data class CalorieLogEntry(
    val exerciseName: String,
    val bodyPart: String,
    val calories: Int,
    val timestamp: Long
)

fun addCaloriesToday(prefs: SharedPreferences, calories: Int, exerciseName: String = "", bodyPart: String = "") {
    val dateKey = LocalDate.now().toString()
    val totalKey = KEY_DAILY_CALORIES_PREFIX + dateKey
    val current = prefs.getInt(totalKey, 0)
    prefs.edit().putInt(totalKey, current + calories).apply()

    // Also log the individual exercise entry
    if (exerciseName.isNotBlank()) {
        val logKey = KEY_CALORIE_LOG + dateKey
        val rawLog = prefs.getString(logKey, "[]") ?: "[]"
        val array = runCatching { JSONArray(rawLog) }.getOrDefault(JSONArray())
        array.put(
            JSONObject()
                .put("name", exerciseName)
                .put("bodyPart", bodyPart)
                .put("calories", calories)
                .put("timestamp", System.currentTimeMillis())
        )
        prefs.edit().putString(logKey, array.toString()).apply()
    }
}

fun getCaloriesToday(prefs: SharedPreferences): Int {
    val dateKey = LocalDate.now().toString()
    return prefs.getInt(KEY_DAILY_CALORIES_PREFIX + dateKey, 0)
}

fun getCaloriesForDate(prefs: SharedPreferences, date: LocalDate): Int {
    return prefs.getInt(KEY_DAILY_CALORIES_PREFIX + date.toString(), 0)
}

fun getCalorieLogToday(prefs: SharedPreferences): List<CalorieLogEntry> {
    val dateKey = LocalDate.now().toString()
    val raw = prefs.getString(KEY_CALORIE_LOG + dateKey, "[]") ?: "[]"
    return runCatching {
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                add(
                    CalorieLogEntry(
                        exerciseName = obj.optString("name", "Exercise"),
                        bodyPart = obj.optString("bodyPart", ""),
                        calories = obj.optInt("calories", 0),
                        timestamp = obj.optLong("timestamp", 0)
                    )
                )
            }
        }
    }.getOrDefault(emptyList())
}
