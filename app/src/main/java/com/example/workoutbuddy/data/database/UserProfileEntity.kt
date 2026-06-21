package com.example.workoutbuddy.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

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
    val gymExperience: String = "Beginner"
)
