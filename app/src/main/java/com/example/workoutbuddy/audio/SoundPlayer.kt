package com.example.workoutbuddy.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.workoutbuddy.R

enum class AppSound {
    TICK,
    POP,
    WHOOSH,
    BUTTON_TAP,
    SUCCESS_DING,
    CHIME,
    TIMER_END,
    REST_TIMER_END
}

// Short, low-latency UI sound effects. Backed by SoundPool rather than MediaPlayer/ToneGenerator
// since these are all <1s one-shot clips that need to fire with minimal delay on taps/state changes.
class SoundPlayer(context: Context) {

    private val appContext = context.applicationContext

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val soundIds: Map<AppSound, Int> = mapOf(
        AppSound.TICK to soundPool.load(appContext, R.raw.tick, 1),
        AppSound.POP to soundPool.load(appContext, R.raw.pop, 1),
        AppSound.WHOOSH to soundPool.load(appContext, R.raw.whoosh, 1),
        AppSound.BUTTON_TAP to soundPool.load(appContext, R.raw.button_tap, 1),
        AppSound.SUCCESS_DING to soundPool.load(appContext, R.raw.success_ding, 1),
        AppSound.CHIME to soundPool.load(appContext, R.raw.chime, 1),
        AppSound.TIMER_END to soundPool.load(appContext, R.raw.timer_end, 1),
        AppSound.REST_TIMER_END to soundPool.load(appContext, R.raw.rest_timer_end, 1)
    )

    fun play(sound: AppSound, volume: Float = 1f) {
        val id = soundIds[sound] ?: return
        // id is 0 if load() failed (e.g. missing/corrupt asset) - SoundPool ignores plays on id 0,
        // but guard explicitly so a bad asset can never throw mid-workout.
        if (id == 0) return
        soundPool.play(id, volume, volume, 1, 0, 1f)
    }

    fun release() {
        soundPool.release()
    }
}
