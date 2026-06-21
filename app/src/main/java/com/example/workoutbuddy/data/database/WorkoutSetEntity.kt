package com.example.workoutbuddy.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["workoutId"])]
)
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val exerciseId: Int,
    val setNumber: Int,
    val weight: Double? = null,
    val reps: Int? = null,
    val time: Int? = null,
    val distance: Double? = null,
    val inclinePct: Double? = null,  // % incline for walking/running/cycling
    val isCompleted: Boolean = false,
    val isPR: Boolean = false,
    val recommendedWeight: Double? = null,
    val recommendedReps: Int? = null,
    val recommendedTime: Int? = null,
    val recommendedDistance: Double? = null
)
