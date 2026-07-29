package com.example.workoutbuddy.viewmodel

import com.example.workoutbuddy.data.database.ExerciseEntity
import com.example.workoutbuddy.data.database.WorkoutSetEntity
import kotlin.math.abs

private const val PERFORMANCE_MULTIPLIER = 1.5
private const val ON_PAR_RATIO_LOW = 0.95
private const val ON_PAR_RATIO_HIGH = 1.05
private const val EXCEED_FULL_RATIO = 1.20
private const val ON_PAR_SCORE = 70.0

object WorkoutPerformanceCalculator {

    private val TIME_ONLY_CARDIO = listOf(
        "Jump Rope", "Jumping Jacks", "High Knees", "Burpees",
        "Mountain Climbers", "Butt Kicks", "Skater Jumps", "Shadow Boxing"
    )

    fun isTimeOnlyCardio(name: String): Boolean =
        TIME_ONLY_CARDIO.any { name.contains(it, ignoreCase = true) }

    fun muscleGroupsForBodyPart(bodyPart: String): List<String> {
        val parts = bodyPart.split("&").map { it.trim() }
        return parts.mapNotNull { part ->
            when {
                part.contains("Chest", ignoreCase = true) -> "Chest"
                part.equals("Back", ignoreCase = true) ||
                    part.equals("Lats", ignoreCase = true) ||
                    part.contains("Traps", ignoreCase = true) -> "Back"
                part.contains("Deltoid", ignoreCase = true) ||
                    part.contains("Shoulders", ignoreCase = true) -> "Shoulders"
                part.contains("Biceps", ignoreCase = true) ||
                    part.contains("Brachialis", ignoreCase = true) -> "Biceps"
                part.contains("Triceps", ignoreCase = true) -> "Triceps"
                part.contains("Quads", ignoreCase = true) -> "Quads"
                part.contains("Hamstrings", ignoreCase = true) -> "Hamstrings"
                part.contains("Glutes", ignoreCase = true) -> "Glutes"
                part.contains("Calves", ignoreCase = true) -> "Calves"
                part.equals("Core", ignoreCase = true) -> "Core"
                else -> null
            }
        }.distinct()
    }

    fun calculateSetPerformance(
        exerciseName: String,
        weight: Double?,
        reps: Int?,
        time: Int?,
        distance: Double?,
        exerciseType: String,
        userBodyWeight: Double,
        inclinePct: Double? = null
    ): Double {
        return when (exerciseType) {
            "LIFT" -> {
                val w = if (abs(weight ?: 0.0) != 0.0) weight ?: 0.0 else userBodyWeight
                val r = reps ?: 0
                (w * r) * PERFORMANCE_MULTIPLIER
            }
            "CARDIO" -> {
                if (isTimeOnlyCardio(exerciseName)) {
                    ((time ?: 0) * 0.2) * PERFORMANCE_MULTIPLIER
                } else {
                    val dist = distance ?: 0.0
                    val timeSec = time ?: 0
                    val incl = inclinePct ?: 0.0
                    val speedKmh = if (timeSec > 0) dist / (timeSec / 3600.0) else 0.0
                    val paceFactor = 1.0 + (speedKmh / 20.0)
                    val inclineFactor = 1.0 + (incl / 100.0)
                    (dist * 10.0 * paceFactor * inclineFactor) * PERFORMANCE_MULTIPLIER
                }
            }
            "HOLD" -> {
                ((time ?: 0) * 0.6) * PERFORMANCE_MULTIPLIER
            }
            else -> 0.0
        }
    }

    fun calculateSetCalories(set: WorkoutSetEntity, exercise: ExerciseEntity): Double {
        return when (exercise.type) {
            "LIFT" -> {
                val weight = set.weight ?: set.recommendedWeight ?: 0.0
                val reps = set.reps ?: set.recommendedReps ?: 0
                val volume = weight * reps
                val base = volume * 0.05
                val rate = exercise.calorieBurnRate.takeIf { it > 0 } ?: 3.0
                (base + rate).coerceAtLeast(1.0)
            }
            "CARDIO" -> {
                val distance = set.distance ?: set.recommendedDistance ?: 0.0
                val timeSec = set.time ?: set.recommendedTime ?: 0
                val rate = exercise.calorieBurnRate.takeIf { it > 0 } ?: 8.0
                if (isTimeOnlyCardio(exercise.name)) {
                    val minutes = timeSec / 60.0
                    (minutes * rate).coerceAtLeast(1.0)
                } else if (distance > 0.0) {
                    val calPerKm = 75.0
                    (distance * calPerKm).coerceAtLeast(1.0)
                } else {
                    val minutes = timeSec / 60.0
                    (minutes * rate).coerceAtLeast(1.0)
                }
            }
            "HOLD" -> {
                val timeSec = set.time ?: set.recommendedTime ?: 0
                val minutes = timeSec / 60.0
                val rate = exercise.calorieBurnRate.takeIf { it > 0 } ?: 4.0
                (minutes * rate).coerceAtLeast(1.0)
            }
            else -> 5.0
        }
    }

    fun scoreCompletedSetAgainstExpectation(
        set: WorkoutSetEntity,
        exercise: ExerciseEntity,
        bodyWeight: Double
    ): Double {
        val actual = calculateSetPerformance(
            exerciseName = exercise.name,
            weight = set.weight,
            reps = set.reps,
            time = set.time,
            distance = set.distance,
            exerciseType = exercise.type,
            userBodyWeight = bodyWeight,
            inclinePct = set.inclinePct
        )
        val expected = calculateSetPerformance(
            exerciseName = exercise.name,
            weight = set.recommendedWeight,
            reps = set.recommendedReps,
            time = set.recommendedTime,
            distance = set.recommendedDistance,
            exerciseType = exercise.type,
            userBodyWeight = bodyWeight,
            inclinePct = set.inclinePct
        )
        if (expected <= 0.0) return ON_PAR_SCORE
        val ratio = actual / expected
        return when {
            ratio >= EXCEED_FULL_RATIO -> 100.0
            ratio <= ON_PAR_RATIO_LOW -> (ratio / ON_PAR_RATIO_LOW * ON_PAR_SCORE).coerceIn(0.0, ON_PAR_SCORE)
            ratio <= ON_PAR_RATIO_HIGH -> ON_PAR_SCORE
            else -> ON_PAR_SCORE + ((ratio - ON_PAR_RATIO_HIGH) / (EXCEED_FULL_RATIO - ON_PAR_RATIO_HIGH) * 30.0)
        }
    }
}
