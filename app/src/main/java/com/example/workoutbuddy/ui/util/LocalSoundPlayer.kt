package com.example.workoutbuddy.ui.util

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.workoutbuddy.audio.SoundPlayer

// Lets pure-UI tap interactions (skip/minimize buttons, list taps, onboarding controls) play a
// sound without plumbing the ViewModel through every Composable. State-driven sounds (set
// completion, PR/record-broken, timer completion) still fire from WorkoutViewModel directly.
val LocalSoundPlayer = staticCompositionLocalOf<SoundPlayer> {
    error("LocalSoundPlayer not provided - wrap the Compose tree with CompositionLocalProvider")
}
