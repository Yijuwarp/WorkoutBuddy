package com.example.workoutbuddy.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

// Per-exercise frequency tag. Only exercises the user has explicitly tagged get a row here;
// absence of a row means "neutral" - same uniform-random weight as before this feature existed.
@Entity(tableName = "exercise_preferences")
data class ExercisePreferenceEntity(
    @PrimaryKey val exerciseId: Int,
    val frequency: String // "ALWAYS", "OFTEN", "LESS", "NEVER" - see Frequency enum
)
