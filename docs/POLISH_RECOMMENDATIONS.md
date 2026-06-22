# Sound & Animation Polish Recommendations

Survey of the current Compose UI to find spots where sound effects and animation would add polish, without duplicating what already exists.

## What's already there

**Sound:** `WorkoutViewModel.playBeep()` (`app/src/main/java/com/example/workoutbuddy/viewmodel/WorkoutViewModel.kt:1014`) uses `ToneGenerator` DTMF tones on rest-timer and exercise-timer completion, falling back to the system notification sound. No other sound effects exist in the app — no SoundPool, no UI click/feedback sounds.

**Animation:** Onboarding step transitions (`OnboardingScreen.kt:138`) use `AnimatedContent` with slide+fade — a good baseline to match in tone elsewhere. `WorkoutIntensityDial`/`WorkoutBurnDial` (`WorkoutIntensityDial.kt:39,143`) animate their arc sweep with `animateFloatAsState`. `WavyFloatingNumbers.kt` already does a custom 3.8s wavy rise-and-fade for floating score numbers. The calendar (`UIComponents.kt:106`) has swipe gesture navigation. None of these currently have accompanying sound, and most button/state-change interactions across the app have zero animation or sound feedback.

## Recommendations

### 1. Set completion (highest traffic interaction)
`SetRowItem` completion checkbox — `UIComponents.kt:1282-1319`
- **Sound:** short, light "tick" SFX on toggle-to-complete (distinct from the timer beep).
- **Animation:** icon morph circle → checkmark with a quick scale-bounce (`Modifier.scale` via `animateFloatAsState`, ~150ms spring). If the set is a PR, escalate to a star icon with a brighter bounce.

### 2. Record broken celebration
`RecordBrokenCelebration` dialog — `WorkoutScreen.kt:429-533`
- **Sound:** a celebratory chime/fanfare distinct from the DTMF beeps — this is the app's biggest "win" moment and currently has no audio at all.
- **Animation:** entrance bounce-scale on the trophy icon (overshoot easing) rather than a plain fade/pop; consider a brief confetti burst using the existing `WavyFloatingNumbersContainer` pattern as a template.

### 3. Workout start / complete
Start/Complete Workout FAB — `WorkoutScreen.kt:352-383`
- **Sound:** soft start "whoosh"/tap on Start; a slightly more rewarding completion sound on Complete (separate from the rest-timer beep family).
- **Animation:** press scale-down (Material `Modifier.scale` on press state) plus a brief success checkmark/pulse on the button itself when a workout completes, before the summary dialog appears.

### 4. Rest / countdown timer completion
`startCooldownJob`/`startCountdownJob` → `playBeep()` — `WorkoutViewModel.kt:895-940, 1014-1043`
- **Sound:** already covered; consider only a softer "tick" in the final 3 seconds (countdown urgency) layered on top of the existing completion tone.
- **Animation:** `RestTimerModal`/`CountdownTimerDialog` (`UIComponents.kt:1502-1610, 1613-1702`) circular progress could pulse/scale on the final 3-second mark for visual urgency, and the dialogs should animate in (scale+fade) rather than appear instantly.
- **Haptic (cheap win bundled with sound work):** add a vibration pulse alongside `playBeep()` — useful when the phone is muted or pocketed mid-set.

### 5. Skip / minimize buttons
`RestTimerModal` skip — `UIComponents.kt:1682-1696`; `CooldownBanner`/`CountdownBanner` expand-tap — `UIComponents.kt:1706-1839`
- **Sound:** light tap SFX on skip (currently silent).
- **Animation:** banner expand/collapse should animate height+fade (`AnimatedVisibility` with `expandVertically`/`shrinkVertically`) instead of an instant swap.

### 6. Set add/remove
Add set, and swipe-to-remove via `SwipeToDismissBox` — `UIComponents.kt:666-701`
- **Sound:** subtle "pop" on add, subtle "whoosh" on swipe-dismiss completion.
- **Animation:** wrap the set list in `AnimatedVisibility`/`animateItemPlacement()` (in a `LazyColumn`) so added/removed rows animate in/out and reflow smoothly instead of jump-cutting.

### 7. Exercise list & detail sheet
`ExerciseListItem` — `UIComponents.kt:326-442`; `ExerciseDetailBottomSheet` — `UIComponents.kt:447-841`
- **Animation:** bottom sheet already slides up via `ModalBottomSheet` defaults — fine as-is. Add a subtle press-scale on `ExerciseListItem` tap for tactile feedback, and crossfade the exercise thumbnail when it loads (`Crossfade` is unused elsewhere in the app, would fit here).

### 8. Workout summary dialog
`WorkoutSummaryDialog` — `WorkoutScreen.kt:417-425`
- **Sound:** a closing "ding" reusing the win-chime asset from recommendation #2 at lower intensity.
- **Animation:** scale+fade entrance; animate the stat numbers counting up rather than appearing static (ties naturally into the existing floating-numbers system for consistency).

### 9. Calendar day selection
`CalendarWidget` — `UIComponents.kt:54-269`
- **Animation:** selected-day indicator should animate position (`animateContentSize` or offset animation) when moving between days, rather than snapping; workout indicator dots could fade in on month-swipe rather than appearing instantly.

### 10. Onboarding micro-interactions
`OnboardingScreen.kt` steps 1–4 (`:392-635`)
- Screen-level transitions are already good. Add small touches: age +/- buttons (`:507-565`) with press-scale, slider thumb (height/weight, gym-experience) with a subtle scale-up while dragging for tactile confirmation.

## Suggested implementation order

1. Set completion tick + record-broken chime (#1, #2) — highest visibility, currently silent.
2. Haptics bundled into `playBeep()` (#4) — near-zero cost, reuses existing call sites.
3. List animations for set add/remove and `LazyColumn` item placement (#6) — fixes visible jump-cuts.
4. Button press-scale as a shared `Modifier` extension, applied across Start/Complete/Skip/list items (#3, #5, #7) — one reusable utility, many call sites.
5. Everything else (banners, calendar, summary dialog, onboarding micro-interactions) as incremental follow-ups.

## Notes on assets
No sound assets currently exist in the project beyond system tones. Adding distinct SFX (tick, pop, chime, whoosh) will require new audio files under `res/raw/` and a lightweight `SoundPool`-based player (preferred over `MediaPlayer` for these short, low-latency UI sounds) rather than extending `ToneGenerator`, which is suited only to tone sequences.
