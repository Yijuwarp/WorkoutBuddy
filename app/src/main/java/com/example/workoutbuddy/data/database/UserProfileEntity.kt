package com.example.workoutbuddy.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.workoutbuddy.data.Equipment

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1, // Single row constraint
    val nickname: String,
    val age: Int,
    val height: Double, // in cm
    val weight: Double, // in kg
    val gender: String, // "Male", "Female", "Other"
    val strengthScore: Double,
    val staminaScore: Double = 100.0,
    val gymExperience: String = "Beginner",
    val restTimerEnabled: Boolean = true,
    val equipmentOwned: String = Equipment.allIdsCsv, // CSV of Equipment ids the user has access to
    val difficultyCeiling: String? = null, // "EASY"/"MEDIUM"/"HARD"; null = unset, shows first-launch tuning overlay
    val hasSeenWorkoutTour: Boolean = false, // true once the first-open WorkoutScreen coach-mark tour has been completed/skipped
    val workoutLengthMinutes: Int = 45 // target length of auto-generated workouts: 15/30/45/60/90
)
