package com.example.workoutbuddy.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long, // timestamp
    val category: String, // "PUSH", "PULL", "LOWER_BODY"
    val isCompleted: Boolean,
    val totalCalories: Double = 0.0,
    val totalSteps: Int = 0,
    val prCount: Int = 0,
    val durationInSeconds: Long = 0L
)
