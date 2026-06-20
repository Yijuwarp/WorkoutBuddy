package com.example.workoutbuddy.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String, // "PUSH", "PULL", "LOWER_BODY"
    val type: String, // "LIFT", "CARDIO", "HOLD"
    val bodyPart: String,
    val calorieBurnRate: Double,
    val description: String,
    val howToSteps: String, // Newline separated steps
    val impactLevel: String // "HIGH", "MEDIUM", "LOW"
)
