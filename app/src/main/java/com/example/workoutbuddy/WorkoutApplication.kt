package com.example.workoutbuddy

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import com.example.workoutbuddy.data.WorkoutRepository
import com.example.workoutbuddy.data.database.WorkoutDatabase
import com.example.workoutbuddy.notification.DailyReminderReceiver

const val TIMER_CHANNEL_ID = "timer_channel_v2"

class WorkoutApplication : Application() {
    val database by lazy { WorkoutDatabase.getDatabase(this) }
    val repository by lazy { WorkoutRepository(database.workoutDao()) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            // Channel settings are frozen at first creation, so the original channels (created
            // without an explicit sound) can never be made audible programmatically. The v2
            // channels declare the sound up front; the old ones are deleted so users don't see
            // stale duplicates in system settings.
            manager.deleteNotificationChannel("timer_channel")
            manager.deleteNotificationChannel("daily_reminder")

            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val soundAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            manager.createNotificationChannel(
                NotificationChannel(
                    TIMER_CHANNEL_ID,
                    "Workout Timers",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for rest and exercise timers completion"
                    enableLights(true)
                    enableVibration(true)
                    setSound(soundUri, soundAttributes)
                }
            )
            manager.createNotificationChannel(
                NotificationChannel(
                    DailyReminderReceiver.CHANNEL_ID,
                    "Workout Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Daily reminder to start your workout"
                    enableVibration(true)
                    setSound(soundUri, soundAttributes)
                }
            )
        }
    }
}
