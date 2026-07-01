package com.example.workoutbuddy.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks accumulated training fatigue per muscle group (keyed by ExerciseEntity.category:
 * "PUSH"/"PULL"/"LOWER_BODY"). fatiguePct is 0 (fully recovered) to 100 (fully fatigued) as of
 * lastUpdatedAt; recovery is computed on read by decaying fatiguePct for elapsed time since
 * lastUpdatedAt, rather than stored as a running value, so no background job is needed.
 */
@Entity(tableName = "muscle_group_recovery")
data class MuscleGroupRecoveryEntity(
    @PrimaryKey val muscleGroup: String,
    val fatiguePct: Double,
    val lastUpdatedAt: Long
)
