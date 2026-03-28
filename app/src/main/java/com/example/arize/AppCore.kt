package com.example.arize

import android.content.SharedPreferences
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.math.pow

const val PREFS_NAME = "arize_prefs"
const val KEY_ONBOARDING_DONE = "onboarding_done"
const val KEY_GENDER = "gender"
const val KEY_HEIGHT = "height"
const val KEY_CURRENT_WEIGHT = "current_weight"
const val KEY_TARGET_WEIGHT = "target_weight"
const val KEY_WORKOUT_DAYS = "workout_days"
const val KEY_REMINDER_HOUR = "reminder_hour"
const val KEY_REMINDER_MINUTE = "reminder_minute"
const val KEY_GEMINI_API_KEY = "gemini_api_key"

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    GYM("Gym", Icons.Default.FitnessCenter),
    FOOD("Food", Icons.Default.Restaurant),
    SOCIAL("Social", Icons.Default.Groups),
    STATS("Stats", Icons.Default.Analytics),
    PROFILE("Profile", Icons.Default.Person),
}

enum class GenderType(val label: String) {
    MALE("Male"),
    FEMALE("Female")
}

data class UserProfile(
    val gender: GenderType = GenderType.MALE,
    val heightCm: Float = 172f,
    val currentWeightKg: Float = 72f,
    val targetWeightKg: Float = 70f,
    val workoutDays: Set<String> = setOf("Mon", "Wed", "Fri"),
    val reminderHour: Int = 7,
    val reminderMinute: Int = 0,
    val geminiApiKey: String = ""
)

data class Exercise(
    val title: String,
    val bodyPart: String,
    val sets: Int,
    val reps: Int,
    val imageUrl: String,
    val equipment: String,
    val focusAreas: List<String>,
    val preparation: List<String>,
    val execution: List<String>,
    val keyTips: List<String>,
    val videoUrl: String,
    val sourceUrl: String,
)

val AppDark = Color(0xFF090B12)
val CardDark = Color(0xFF1A1F2A)
val AccentBlue = Color(0xFF3B57F0)
val MutedText = Color(0xFF9CA3B0)
val LightSurface = Color(0xFFF4F6F8)
val LightCard = Color(0xFFFFFFFF)

fun bmiTargetRange(heightCm: Float): Pair<Float, Float> {
    val meters = heightCm / 100f
    val min = 18.5f * meters.pow(2)
    val max = 24.9f * meters.pow(2)
    return min to max
}

fun buildExerciseCatalog(): List<Exercise> {
    return listOf(
        Exercise(
            title = "Incline Bench Press",
            bodyPart = "Chest",
            sets = 4,
            reps = 10,
            imageUrl = "https://images.pexels.com/photos/1552106/pexels-photo-1552106.jpeg",
            equipment = "Barbell + Incline Bench",
            focusAreas = listOf("Chest", "Shoulders", "Triceps"),
            preparation = listOf("Set incline bench to 30-45 degrees", "Retract shoulder blades and keep feet planted"),
            execution = listOf("Lower bar to upper chest with control", "Press up while keeping elbows under wrists"),
            keyTips = listOf("Avoid bouncing bar", "Keep lower back naturally arched"),
            videoUrl = "https://www.youtube.com/results?search_query=incline+bench+press+proper+form",
            sourceUrl = "https://en.wikipedia.org/wiki/Bench_press"
        ),
        Exercise(
            title = "Cable Fly",
            bodyPart = "Chest",
            sets = 3,
            reps = 12,
            imageUrl = "https://images.pexels.com/photos/1229356/pexels-photo-1229356.jpeg",
            equipment = "Cable Machine",
            focusAreas = listOf("Chest", "Front Deltoids"),
            preparation = listOf("Set pulleys just above shoulder height", "Stand staggered for balance"),
            execution = listOf("Bring handles together in an arc", "Return slowly to stretch chest"),
            keyTips = listOf("Keep a slight bend in elbows", "Control tempo in both directions"),
            videoUrl = "https://www.youtube.com/results?search_query=cable+chest+fly+form",
            sourceUrl = "https://en.wikipedia.org/wiki/Fly_(exercise)"
        ),
        Exercise(
            title = "Lat Pulldown",
            bodyPart = "Back",
            sets = 4,
            reps = 10,
            imageUrl = "https://images.pexels.com/photos/2261485/pexels-photo-2261485.jpeg",
            equipment = "Lat Pulldown Machine",
            focusAreas = listOf("Lats", "Biceps", "Upper Back"),
            preparation = listOf("Grip bar slightly wider than shoulders", "Sit tall with core braced"),
            execution = listOf("Pull bar to upper chest", "Pause briefly and control the return"),
            keyTips = listOf("Do not swing torso", "Lead with elbows down"),
            videoUrl = "https://www.youtube.com/results?search_query=lat+pulldown+proper+form",
            sourceUrl = "https://en.wikipedia.org/wiki/Lat_pulldown"
        ),
        Exercise(
            title = "Deadlift",
            bodyPart = "Back",
            sets = 4,
            reps = 6,
            imageUrl = "https://images.pexels.com/photos/3837757/pexels-photo-3837757.jpeg",
            equipment = "Barbell",
            focusAreas = listOf("Posterior Chain", "Back", "Glutes"),
            preparation = listOf("Midfoot under bar", "Hinge hips and set neutral spine"),
            execution = listOf("Push floor away and stand tall", "Lower by hinging first then bending knees"),
            keyTips = listOf("Keep bar close to shins", "Do not round lower back"),
            videoUrl = "https://www.youtube.com/results?search_query=barbell+deadlift+form",
            sourceUrl = "https://en.wikipedia.org/wiki/Deadlift"
        ),
        Exercise(
            title = "Shoulder Press",
            bodyPart = "Shoulder",
            sets = 4,
            reps = 8,
            imageUrl = "https://images.pexels.com/photos/3838331/pexels-photo-3838331.jpeg",
            equipment = "Dumbbells",
            focusAreas = listOf("Deltoids", "Triceps"),
            preparation = listOf("Sit upright and brace core", "Start dumbbells at shoulder level"),
            execution = listOf("Press overhead without shrugging", "Lower under control"),
            keyTips = listOf("Keep wrists stacked", "Avoid excessive lower-back arch"),
            videoUrl = "android.resource://com.example.arize/raw/shoulder_press",
            sourceUrl = "https://en.wikipedia.org/wiki/Overhead_press"
        ),
        Exercise(
            title = "Lateral Raise",
            bodyPart = "Shoulder",
            sets = 3,
            reps = 15,
            imageUrl = "https://images.pexels.com/photos/4761779/pexels-photo-4761779.jpeg",
            equipment = "Dumbbells",
            focusAreas = listOf("Lateral Deltoids"),
            preparation = listOf("Stand tall with slight elbow bend", "Keep dumbbells by sides"),
            execution = listOf("Raise to shoulder height", "Lower slowly to start"),
            keyTips = listOf("Do not swing weight", "Lead with elbows, not hands"),
            videoUrl = "android.resource://com.example.arize/raw/lateral_raise",
            sourceUrl = "https://en.wikipedia.org/wiki/Lateral_raise"
        ),
        Exercise(
            title = "Barbell Curl",
            bodyPart = "Arms",
            sets = 4,
            reps = 10,
            imageUrl = "https://images.pexels.com/photos/6456140/pexels-photo-6456140.jpeg",
            equipment = "Barbell",
            focusAreas = listOf("Biceps", "Forearms"),
            preparation = listOf("Stand with elbows near torso", "Use shoulder-width grip"),
            execution = listOf("Curl bar toward shoulders", "Lower until arms are nearly straight"),
            keyTips = listOf("Keep upper arms fixed", "Avoid leaning back"),
            videoUrl = "android.resource://com.example.arize/raw/barbell_curl",
            sourceUrl = "https://en.wikipedia.org/wiki/Biceps_curl"
        ),
        Exercise(
            title = "Triceps Pushdown",
            bodyPart = "Arms",
            sets = 3,
            reps = 12,
            imageUrl = "https://images.pexels.com/photos/1552249/pexels-photo-1552249.jpeg",
            equipment = "Cable Machine",
            focusAreas = listOf("Triceps"),
            preparation = listOf("Grip rope with elbows tucked", "Lean slightly forward"),
            execution = listOf("Extend elbows fully", "Return slowly until forearms are parallel"),
            keyTips = listOf("Keep elbows close to ribs", "Control both phases"),
            videoUrl = "android.resource://com.example.arize/raw/tricep_pushdown",
            sourceUrl = "https://en.wikipedia.org/wiki/Triceps_extension"
        ),
        Exercise(
            title = "Back Squat",
            bodyPart = "Legs",
            sets = 4,
            reps = 8,
            imageUrl = "https://images.pexels.com/photos/949129/pexels-photo-949129.jpeg",
            equipment = "Barbell + Rack",
            focusAreas = listOf("Quads", "Glutes", "Core"),
            preparation = listOf("Set bar on upper traps", "Brace core and set feet shoulder-width"),
            execution = listOf("Sit down and back", "Drive up through midfoot"),
            keyTips = listOf("Keep knees tracking toes", "Maintain neutral spine"),
            videoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
            sourceUrl = "https://en.wikipedia.org/wiki/Squat_(exercise)"
        ),
        Exercise(
            title = "Romanian Deadlift",
            bodyPart = "Legs",
            sets = 4,
            reps = 10,
            imageUrl = "https://images.pexels.com/photos/3757376/pexels-photo-3757376.jpeg",
            equipment = "Barbell or Dumbbells",
            focusAreas = listOf("Hamstrings", "Glutes", "Lower Back"),
            preparation = listOf("Start standing tall with weight in hands", "Unlock knees slightly"),
            execution = listOf("Hinge hips backward until hamstring stretch", "Stand up by squeezing glutes"),
            keyTips = listOf("Keep bar close to legs", "Avoid rounding shoulders"),
            videoUrl = "https://www.youtube.com/results?search_query=romanian+deadlift+form",
            sourceUrl = "https://en.wikipedia.org/wiki/Deadlift#Romanian_deadlift"
        ),
        Exercise(
            title = "Cable Crunch",
            bodyPart = "Abs",
            sets = 3,
            reps = 15,
            imageUrl = "https://images.pexels.com/photos/136405/pexels-photo-136405.jpeg",
            equipment = "Cable Machine + Rope",
            focusAreas = listOf("Rectus Abdominis"),
            preparation = listOf("Kneel facing cable stack", "Hold rope by temples"),
            execution = listOf("Crunch by flexing spine", "Return slowly without losing tension"),
            keyTips = listOf("Do not pull with arms", "Keep hips relatively fixed"),
            videoUrl = "https://www.youtube.com/results?search_query=cable+crunch+form",
            sourceUrl = "https://en.wikipedia.org/wiki/Crunch_(exercise)"
        ),
        Exercise(
            title = "Hanging Knee Raise",
            bodyPart = "Abs",
            sets = 3,
            reps = 12,
            imageUrl = "https://images.pexels.com/photos/7674497/pexels-photo-7674497.jpeg",
            equipment = "Pull-up Bar",
            focusAreas = listOf("Abs", "Hip Flexors"),
            preparation = listOf("Hang from bar with controlled grip", "Slightly posteriorly tilt pelvis"),
            execution = listOf("Raise knees toward chest", "Lower with control"),
            keyTips = listOf("Avoid swinging", "Exhale on lift"),
            videoUrl = "https://www.youtube.com/results?search_query=hanging+knee+raise+form",
            sourceUrl = "https://en.wikipedia.org/wiki/Leg_raise"
        )
    )
}

fun loadProfile(prefs: SharedPreferences): UserProfile {
    val daysRaw = prefs.getString(KEY_WORKOUT_DAYS, "Mon,Wed,Fri") ?: "Mon,Wed,Fri"
    val daySet = daysRaw.split(",").filter { it.isNotBlank() }.toSet()
    return UserProfile(
        gender = GenderType.valueOf(prefs.getString(KEY_GENDER, GenderType.MALE.name) ?: GenderType.MALE.name),
        heightCm = prefs.getFloat(KEY_HEIGHT, 172f),
        currentWeightKg = prefs.getFloat(KEY_CURRENT_WEIGHT, 72f),
        targetWeightKg = prefs.getFloat(KEY_TARGET_WEIGHT, 70f),
        workoutDays = daySet,
        reminderHour = prefs.getInt(KEY_REMINDER_HOUR, 7),
        reminderMinute = prefs.getInt(KEY_REMINDER_MINUTE, 0),
        geminiApiKey = prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
    )
}

fun saveProfile(prefs: SharedPreferences, profile: UserProfile) {
    prefs.edit()
        .putString(KEY_GENDER, profile.gender.name)
        .putFloat(KEY_HEIGHT, profile.heightCm)
        .putFloat(KEY_CURRENT_WEIGHT, profile.currentWeightKg)
        .putFloat(KEY_TARGET_WEIGHT, profile.targetWeightKg)
        .putString(KEY_WORKOUT_DAYS, profile.workoutDays.joinToString(","))
        .putInt(KEY_REMINDER_HOUR, profile.reminderHour)
        .putInt(KEY_REMINDER_MINUTE, profile.reminderMinute)
        .putString(KEY_GEMINI_API_KEY, profile.geminiApiKey)
        .apply()
}

fun Double.format1(): String = String.format("%.1f", this)
fun Float.format1(): String = String.format("%.1f", this)

fun buildFitnessContext(userProfile: UserProfile): String {
    val contextParts = mutableListOf<String>()
    val goal = when {
        userProfile.targetWeightKg > userProfile.currentWeightKg -> "muscle_gain"
        userProfile.targetWeightKg < userProfile.currentWeightKg -> "weight_loss"
        else -> "maintenance"
    }
    contextParts.add("User Goal: $goal")
    contextParts.add("Gender: ${userProfile.gender.label}")
    contextParts.add("Current Weight: ${userProfile.currentWeightKg.format1()} kg")
    contextParts.add("Target Weight: ${userProfile.targetWeightKg.format1()} kg")
    contextParts.add("Workout Days/Week: ${userProfile.workoutDays.size}")
    contextParts.add("Reminder Time: ${"%02d:%02d".format(userProfile.reminderHour, userProfile.reminderMinute)}")
    return contextParts.joinToString("\n")
}
