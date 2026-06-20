package com.example.workoutbuddy

import android.app.Application
import com.example.workoutbuddy.data.WorkoutRepository
import com.example.workoutbuddy.data.database.WorkoutDatabase

class WorkoutApplication : Application() {
    val database by lazy { WorkoutDatabase.getDatabase(this) }
    val repository by lazy { WorkoutRepository(database.workoutDao()) }
}
