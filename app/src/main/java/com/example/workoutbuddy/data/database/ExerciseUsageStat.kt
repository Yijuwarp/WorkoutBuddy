package com.example.workoutbuddy.data.database

data class ExerciseUsageStat(
    val exerciseId: Int,
    val logCount: Int,
    val lastUsedDate: Long?
)
