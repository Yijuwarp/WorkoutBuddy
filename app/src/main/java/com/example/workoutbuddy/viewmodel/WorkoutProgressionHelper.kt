package com.example.workoutbuddy.viewmodel

import com.example.workoutbuddy.R
import com.example.workoutbuddy.data.database.MuscleGroupRecoveryEntity

// Canonical individual muscles the Body tab's Recovery view tracks/displays, in display order.
val RECOVERY_MUSCLE_GROUPS = listOf(
    "Chest", "Back", "Shoulders", "Biceps", "Triceps", "Quads", "Hamstrings", "Glutes", "Calves", "Core"
)

object WorkoutProgressionHelper {

    const val LIFT_REP_CAP = 12
    const val LIFT_REPS_RESET = 8
    const val CARDIO_PROGRESSION_FACTOR = 1.05
    const val BASE_FATIGUE_BUMP = 25.0
    const val FATIGUE_INTENSITY_MIN = 0.2
    const val FATIGUE_INTENSITY_MAX = 2.0
    const val RECOVERY_PCT_PER_DAY = 20.0
    const val MS_PER_DAY = 86400000.0

    fun workoutSizing(lengthMinutes: Int): Pair<Int, Int> = when {
        lengthMinutes <= 15 -> 2 to 0
        lengthMinutes <= 30 -> 3 to 0
        lengthMinutes <= 45 -> 4 to 1
        lengthMinutes <= 60 -> 5 to 1
        else -> 7 to 1
    }

    fun cardioWorkoutSizing(lengthMinutes: Int): Pair<Int, Int> = when {
        lengthMinutes <= 15 -> 0 to 1
        lengthMinutes <= 30 -> 2 to 1
        lengthMinutes <= 45 -> 2 to 2
        lengthMinutes <= 60 -> 3 to 2
        else -> 3 to 3
    }

    fun progressLift(lastWeight: Double, lastReps: Int): Pair<Double, Int> {
        return if (lastReps >= LIFT_REP_CAP) {
            (lastWeight + 2.5) to LIFT_REPS_RESET
        } else {
            lastWeight to (lastReps + 1)
        }
    }

    fun progressCardio(lastDistance: Double?, lastTime: Int): Pair<Double?, Int> {
        val nextDist = lastDistance?.let { it * CARDIO_PROGRESSION_FACTOR }
        val nextTime = (lastTime * CARDIO_PROGRESSION_FACTOR).toInt()
        return nextDist to nextTime
    }

    fun getAdaptiveStartWeight(name: String, strengthScore: Double, bodyWeight: Double): Double {
        val ratio = when {
            name.contains("Barbell Squat", ignoreCase = true) -> 0.75
            name.contains("Bench Press", ignoreCase = true) -> 0.60
            name.contains("Deadlift", ignoreCase = true) -> 0.40
            name.contains("Overhead Press", ignoreCase = true) -> 0.35
            name.contains("Barbell Row", ignoreCase = true) -> 0.45
            name.contains("Dumbbell", ignoreCase = true) -> 0.25
            name.contains("Push-ups", ignoreCase = true) -> 0.0
            name.contains("Pull-ups", ignoreCase = true) -> 0.0
            else -> 0.30
        }
        if (ratio == 0.0) return 0.0
        val strengthFactor = strengthScore / 100.0
        val rawWeight = bodyWeight * ratio * strengthFactor
        return (Math.round(rawWeight / 2.5) * 2.5).coerceAtLeast(2.5)
    }

    fun getStandardStartDuration(name: String, staminaScore: Double): Int {
        val baseSeconds = when {
            name.contains("Jump Rope", ignoreCase = true) -> 300
            name.contains("Running", ignoreCase = true) -> 1800
            name.contains("Cycling", ignoreCase = true) -> 2400
            name.contains("Walking", ignoreCase = true) -> 1800
            else -> 1200
        }
        val factor = staminaScore / 100.0
        return (baseSeconds * factor).toInt().coerceAtLeast(300)
    }

    fun getStandardStartDistance(name: String, staminaScore: Double): Double {
        val baseKm = when {
            name.contains("Running", ignoreCase = true) -> 5.0
            name.contains("Cycling", ignoreCase = true) -> 15.0
            name.contains("Walking", ignoreCase = true) -> 3.0
            else -> 5.0
        }
        val factor = staminaScore / 100.0
        return (Math.round(baseKm * factor * 10.0) / 10.0).coerceAtLeast(1.0)
    }

    fun currentRecoveryPct(entity: MuscleGroupRecoveryEntity?, now: Long = System.currentTimeMillis()): Double {
        if (entity == null) return 100.0
        val elapsedDays = (now - entity.lastUpdatedAt) / MS_PER_DAY
        val decayedFatigue = (entity.fatiguePct - RECOVERY_PCT_PER_DAY * elapsedDays).coerceIn(0.0, 100.0)
        return 100.0 - decayedFatigue
    }

    fun deriveRankTier(strengthScore: Double, staminaScore: Double): String {
        val avg = (strengthScore + staminaScore) / 2.0
        return when {
            avg < 60.0 -> "Bronze"
            avg < 80.0 -> "Silver"
            avg < 100.0 -> "Gold"
            avg < 125.0 -> "Platinum"
            avg < 160.0 -> "Diamond"
            avg < 200.0 -> "Master"
            avg < 260.0 -> "Grandmaster"
            else -> "Legend"
        }
    }

    fun rankBadgeRes(tier: String): Int = when (tier) {
        "Bronze" -> R.drawable.badge_bronze
        "Silver" -> R.drawable.badge_silver
        "Gold" -> R.drawable.badge_gold
        "Platinum" -> R.drawable.badge_platinum
        "Diamond" -> R.drawable.badge_diamond
        "Master" -> R.drawable.badge_master
        "Grandmaster" -> R.drawable.badge_grandmaster
        else -> R.drawable.badge_legend
    }

    private fun ageMultStrengthFor(age: Int): Double = when {
        age < 18 -> 0.8
        age in 18..35 -> 1.0
        else -> (1.0 - (age - 35) * 0.01).coerceAtLeast(0.6)
    }

    private fun ageMultStaminaFor(age: Int): Double = when {
        age < 18 -> 0.85
        age in 18..35 -> 1.0
        else -> (1.0 - (age - 35) * 0.015).coerceAtLeast(0.5)
    }

    private fun heightMultFor(height: Double): Double = when {
        height > 180.0 -> 1.05
        height < 160.0 -> 0.95
        else -> 1.0
    }

    private fun weightMultStrengthFor(weight: Double): Double = when {
        weight > 90.0 -> 1.05
        weight < 60.0 -> 0.95
        else -> 1.0
    }

    private fun gymMultFor(gymExperience: String): Double = when (gymExperience) {
        "Beginner" -> 0.7
        "Intermediate" -> 1.0
        "Expert" -> 1.3
        else -> 0.7
    }

    fun calculateInitialStrengthScore(age: Int, height: Double, weight: Double, gender: String, gymExperience: String): Double {
        val genderMult = when (gender) {
            "Male" -> 1.2
            "Female" -> 0.9
            else -> 1.05
        }
        val base = height * 0.45 * genderMult
        return (base * ageMultStrengthFor(age) * weightMultStrengthFor(weight) * gymMultFor(gymExperience)).coerceIn(30.0, 999.0)
    }

    fun calculateInitialStaminaScore(age: Int, height: Double, weight: Double, gender: String, gymExperience: String): Double {
        val weightFactorStamina = (70.0 / weight).coerceIn(0.7, 1.3)
        val genderStam = when (gender) {
            "Male" -> 0.95
            "Female" -> 1.05
            else -> 1.0
        }
        return (100.0 * weightFactorStamina * genderStam * ageMultStaminaFor(age) * heightMultFor(height) * gymMultFor(gymExperience)).coerceIn(30.0, 999.0)
    }
}
