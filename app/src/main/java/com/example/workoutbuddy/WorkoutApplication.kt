package com.example.workoutbuddy

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.workoutbuddy.data.WorkoutRepository
import com.example.workoutbuddy.data.database.WorkoutDatabase
import com.example.workoutbuddy.notification.DailyReminderReceiver

class WorkoutApplication : Application() {
    val database by lazy { WorkoutDatabase.getDatabase(this) }
    val repository by lazy { WorkoutRepository(database.workoutDao()) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    "timer_channel",
                    "Workout Timers",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for rest and exercise timers completion"
                    enableLights(true)
                    enableVibration(true)
                }
            )
            manager.createNotificationChannel(
                NotificationChannel(
                    DailyReminderReceiver.CHANNEL_ID,
                    "Workout Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Daily reminder to start your workout"
                }
            )
        }
    }
}
