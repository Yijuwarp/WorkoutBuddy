package com.example.workoutbuddy.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

// Short haptic presets shared by timer completion, set-completion, and PR/record-broken events.
// Mirrors the VibratorManager/Vibrator branch already used in TimerExpiredReceiver.
object Haptics {

    private fun vibrator(context: Context): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun vibrate(context: Context, durationMs: Long, amplitude: Int = VibrationEffect.DEFAULT_AMPLITUDE) {
        try {
            val v = vibrator(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(durationMs)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Light tap - set completion, button taps
    fun tick(context: Context) = vibrate(context, 25)

    // Stronger pulse - timer completion, exercise finish
    fun success(context: Context) = vibrate(context, 80)

    // Celebratory double-pulse - PR / record broken
    fun celebrate(context: Context) {
        try {
            val v = vibrator(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(longArrayOf(0, 60, 80, 120), -1)
                v.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(longArrayOf(0, 60, 80, 120), -1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
