package com.example.workoutbuddy.viewmodel

import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.workoutbuddy.TimerExpiredReceiver
import com.example.workoutbuddy.audio.AppSound
import com.example.workoutbuddy.audio.Haptics
import com.example.workoutbuddy.audio.SoundPlayer
import com.example.workoutbuddy.data.database.ExerciseEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

class WorkoutTimerManager(
    private val application: Application,
    private val soundPlayer: SoundPlayer
) {
    val isWorkoutStarted = MutableStateFlow(false)
    val isTimerPaused = MutableStateFlow(false)
    val workoutDuration = MutableStateFlow(0L)

    val cooldownRemaining = MutableStateFlow(0)
    val cooldownDuration = MutableStateFlow(0)
    val cooldownExerciseName = MutableStateFlow<String?>(null)

    val countdownRemaining = MutableStateFlow(0)
    val countdownDuration = MutableStateFlow(0)
    val countdownExerciseName = MutableStateFlow<String?>(null)
    val isCountdownActive = MutableStateFlow(false)
    val isCountdownPaused = MutableStateFlow(false)
    val countdownSetId = MutableStateFlow<Long?>(null)

    private var timerJob: Job? = null
    private var cooldownJob: Job? = null
    private var countdownJob: Job? = null
    private var backgroundTimestamp: Long = 0L

    fun startWorkout(scope: CoroutineScope, onDurationUpdated: (Long) -> Unit) {
        if (!isWorkoutStarted.value) {
            isWorkoutStarted.value = true
            isTimerPaused.value = false
            startWorkoutTimer(scope, onDurationUpdated)
        }
    }

    fun toggleWorkoutTimer(scope: CoroutineScope, onDurationUpdated: (Long) -> Unit) {
        if (isTimerPaused.value) {
            isTimerPaused.value = false
            startWorkoutTimer(scope, onDurationUpdated)
        } else {
            isTimerPaused.value = true
            timerJob?.cancel()
            timerJob = null
        }
    }

    fun startWorkoutTimer(scope: CoroutineScope, onDurationUpdated: (Long) -> Unit) {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                delay(1000)
                if (!isTimerPaused.value) {
                    workoutDuration.value += 1
                    onDurationUpdated(workoutDuration.value)
                }
            }
        }
    }

    fun triggerCooldown(scope: CoroutineScope, exercise: ExerciseEntity, restTimerEnabled: Boolean) {
        if (!restTimerEnabled) return

        val duration = when (exercise.impactLevel) {
            "HEAVY" -> 120
            "HIGH" -> 90
            "MEDIUM" -> 60
            "LOW" -> 45
            else -> 60
        }

        cooldownDuration.value = duration
        cooldownRemaining.value = duration
        cooldownExerciseName.value = exercise.name

        startCooldownJob(scope)
    }

    private fun startCooldownJob(scope: CoroutineScope) {
        cooldownJob?.cancel()
        cooldownJob = scope.launch {
            while (cooldownRemaining.value > 0 && isActive) {
                delay(1000)
                cooldownRemaining.value -= 1
            }
            if (cooldownRemaining.value <= 0) {
                cooldownExerciseName.value = null
                playBeep(isRestTimer = true)
            }
        }
    }

    fun skipCooldown() {
        cooldownJob?.cancel()
        cooldownJob = null
        cooldownRemaining.value = 0
        cooldownExerciseName.value = null
        cancelNotification(isRest = true)
    }

    fun startCountdown(
        scope: CoroutineScope,
        setId: Long,
        exerciseName: String,
        durationSeconds: Int,
        onComplete: (Long?) -> Unit
    ) {
        countdownSetId.value = setId
        countdownExerciseName.value = exerciseName
        countdownDuration.value = durationSeconds
        countdownRemaining.value = durationSeconds
        isCountdownActive.value = true
        isCountdownPaused.value = false

        startCountdownJob(scope, onComplete)
    }

    private fun startCountdownJob(scope: CoroutineScope, onComplete: (Long?) -> Unit) {
        countdownJob?.cancel()
        countdownJob = scope.launch {
            while (countdownRemaining.value > 0 && isActive) {
                delay(1000)
                if (!isCountdownPaused.value) {
                    countdownRemaining.value -= 1
                }
            }
            if (countdownRemaining.value <= 0 && !isCountdownPaused.value) {
                val completedId = countdownSetId.value
                isCountdownActive.value = false
                countdownSetId.value = null
                playBeep(isRestTimer = false)
                onComplete(completedId)
            }
        }
    }

    fun toggleCountdownPause() {
        isCountdownPaused.value = !isCountdownPaused.value
    }

    fun completeCountdownEarly(): Long? {
        val id = countdownSetId.value
        countdownJob?.cancel()
        countdownJob = null
        isCountdownActive.value = false
        countdownSetId.value = null
        cancelNotification(isRest = false)
        return id
    }

    fun getElapsedCountdownTime(): Int {
        val total = countdownDuration.value
        val rem = countdownRemaining.value
        return (total - rem).coerceAtLeast(1)
    }

    fun cancelCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        isCountdownActive.value = false
        countdownSetId.value = null
        countdownRemaining.value = 0
        cancelNotification(isRest = false)
    }

    fun stopAllWorkoutTimers() {
        timerJob?.cancel()
        timerJob = null
        cooldownJob?.cancel()
        cooldownJob = null
        countdownJob?.cancel()
        countdownJob = null
        isTimerPaused.value = false
        isWorkoutStarted.value = false
        cooldownRemaining.value = 0
        cooldownExerciseName.value = null
        isCountdownActive.value = false
        countdownSetId.value = null
        cancelNotification(isRest = true)
        cancelNotification(isRest = false)
    }

    private fun playBeep(isRestTimer: Boolean) {
        if (isRestTimer) {
            soundPlayer.play(AppSound.REST_TIMER_END)
            Haptics.success(application)
        } else {
            soundPlayer.play(AppSound.TIMER_END)
            Haptics.success(application)
        }
    }

    fun onAppBackgrounded() {
        backgroundTimestamp = System.currentTimeMillis()

        if (cooldownRemaining.value > 0 && cooldownExerciseName.value != null) {
            scheduleNotification(
                title = "Rest Complete!",
                message = "Time for your next set of ${cooldownExerciseName.value}!",
                delaySeconds = cooldownRemaining.value,
                isRest = true
            )
        }

        if (isCountdownActive.value && !isCountdownPaused.value && countdownRemaining.value > 0) {
            scheduleNotification(
                title = "Timer Complete!",
                message = "${countdownExerciseName.value ?: "Cardio"} set complete!",
                delaySeconds = countdownRemaining.value,
                isRest = false
            )
        }

        timerJob?.cancel()
        timerJob = null
        cooldownJob?.cancel()
        cooldownJob = null
        countdownJob?.cancel()
        countdownJob = null
    }

    fun onAppForegrounded(
        scope: CoroutineScope,
        onDurationUpdated: (Long) -> Unit,
        onCountdownComplete: (Long?) -> Unit
    ) {
        if (backgroundTimestamp == 0L) return
        val elapsedMs = System.currentTimeMillis() - backgroundTimestamp
        val elapsedSeconds = elapsedMs / 1000
        backgroundTimestamp = 0L

        cancelNotification(isRest = true)
        cancelNotification(isRest = false)

        if (isWorkoutStarted.value && !isTimerPaused.value) {
            workoutDuration.value += elapsedSeconds
            onDurationUpdated(workoutDuration.value)
            startWorkoutTimer(scope, onDurationUpdated)
        }

        if (cooldownRemaining.value > 0 && cooldownExerciseName.value != null) {
            val remaining = (cooldownRemaining.value - elapsedSeconds).toInt()
            if (remaining > 0) {
                cooldownRemaining.value = remaining
                startCooldownJob(scope)
            } else {
                cooldownRemaining.value = 0
                cooldownExerciseName.value = null
                playBeep(isRestTimer = true)
            }
        }

        if (isCountdownActive.value && countdownRemaining.value > 0) {
            if (!isCountdownPaused.value) {
                val remaining = (countdownRemaining.value - elapsedSeconds).toInt()
                if (remaining > 0) {
                    countdownRemaining.value = remaining
                    startCountdownJob(scope, onCountdownComplete)
                } else {
                    countdownRemaining.value = 0
                    val completedId = countdownSetId.value
                    isCountdownActive.value = false
                    countdownSetId.value = null
                    playBeep(isRestTimer = false)
                    onCountdownComplete(completedId)
                }
            }
        }
    }

    private fun scheduleNotification(title: String, message: String, delaySeconds: Int, isRest: Boolean) {
        if (delaySeconds <= 0) return
        val intent = Intent(application, TimerExpiredReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("message", message)
            putExtra("isRest", isRest)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            application,
            if (isRest) 1001 else 1002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = application.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerTime = System.currentTimeMillis() + delaySeconds * 1000

        try {
            val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }
            if (canScheduleExact) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            }
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        }
    }

    private fun cancelNotification(isRest: Boolean) {
        val intent = Intent(application, TimerExpiredReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            application,
            if (isRest) 1001 else 1002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = application.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)

        val notificationManager = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(if (isRest) 1001 else 1002)
    }
}
