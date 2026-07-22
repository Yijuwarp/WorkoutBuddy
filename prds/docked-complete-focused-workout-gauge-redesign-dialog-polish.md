# Docked Complete button, focused workout mode, gauge redesign, and dialog polish

## Problem Statement

1. The "Complete Workout" button floats over the exercise list with visible gaps on all sides, so list content (exercise cards, checkmarks) shows behind/around it and the last items are partially hidden even with the current 100dp spacer hack (`WorkoutScreen.kt:380-384, 424-474`).
2. During an active workout the bottom nav stays visible, taking vertical space and inviting mid-workout tab-wandering; there's no "focused" workout mode.
3. Under the dials there is a "Strength target / Burn target / Sets left" row (`WorkoutScreen.kt:302-324`) that largely duplicates what the gauges should communicate. Note the row's "Strength target" (`startingStrengthScore + 2`, a target user strength score) is a related-but-distinct metric from the performance dial's target (`startingStrengthScore × totalSets`, workout performance points) — removing the row intentionally drops the strength-score target from this screen rather than relocating it.
4. Gauge coloring fights the numbers: the performance dial is always red (`PerformanceRed`) and the burn dial always amber, regardless of progress. Red reads as "bad" even when the user is doing fine.
5. The Record Broken celebration dialog blocks the screen until manually dismissed — hostile mid-workout (user may be holding weights).
6. Onboarding pre-selects "Male" as gender (`OnboardingScreen.kt:63`), a biased default; users can page past without ever making a real choice.
7. In the Workout Completed dialog, "Records Broken" — the emotional payoff — renders at the same size as every other stat tile.

## Solution

1. Replace the floating Complete Workout button with a docked bottom bar: full-width button inside a solid surface-colored container, flush against whatever is below it (bottom nav when paused, system nav when running). No content visible behind it.
2. Hide the bottom nav bar while the workout timer is running ("focused mode"). Pausing the timer brings the nav back; resuming hides it again. The docked Complete button is visible in both states, sitting directly on top of the nav bar when it's shown.
3. Delete the "Strength target / Burn target / Sets left" row entirely. Represent the target on each gauge itself as a tick mark on the arc (no target number text).
4. Recolor both gauges identically: the arc sweeps through a smooth white → green → gold gradient as progress toward target increases. The "PERFORMANCE" label keeps red, the "BURN" label keeps gold/amber, preserving each dial's identity.
5. Auto-dismiss the Record Broken dialog after 3 seconds. Same visuals; tapping outside or "Awesome!" still dismisses early.
6. No gender pre-selected in onboarding; Next on the gender step is disabled until one is chosen.
7. When records were broken, render "Records Broken" as a full-width hero row at the top of the completed-workout stats; when zero records, keep the current equal-tile layout (the records tile is already hidden at zero).

## User Stories

1. As a user scrolling my exercise list mid-workout, I never want cards half-hidden behind a floating button.
2. As a user who started a workout, I want the app in a focused mode without the tab bar, so the workout is the whole screen.
3. As a user who pauses my workout, I want the tab bar back so I can check my Log or Body screen, and I want it gone again when I resume.
4. As a user glancing at the dials, I want the arc color itself to tell me how close I am to target — white when I've barely started, green when solidly underway, gold when I'm near/at target.
5. As a user, I want to see where the target sits on the dial arc, without a second row of numbers repeating it.
6. As a user who breaks a record mid-set, I want the celebration to show itself and get out of the way on its own.
7. As a new user in onboarding, I shouldn't see a gender pre-chosen for me; the app should wait for my answer.
8. As a user finishing a workout with PRs, I want "Records Broken" celebrated as the headline stat.

## Implementation Decisions

### 1. Docked Complete Workout bar (`WorkoutScreen.kt:424-474`)
- Replace the `align(BottomCenter)` floating `Button` (both Start and Complete variants) with a docked container: `Surface`/`Column` anchored to the bottom, full width, `MaterialTheme.colorScheme.surface` background, top border or elevation for separation, containing the existing green button with 16dp inner padding.
- No vertical margin below the container — flush with the bottom edge of the content area (Scaffold `innerPadding` already accounts for nav bar / system insets).
- The exercise `LazyColumn` gets `contentPadding` bottom equal to the docked bar height (~88dp) instead of the current 100dp spacer item (`WorkoutScreen.kt:380-384` — remove that item).
- The minimized cooldown banner (`WorkoutScreen.kt:399-422`) keeps floating above the docked bar; its `padding(bottom=…)` updates to the bar height.

### 2. Focused mode — hide bottom nav while running (`Navigation.kt:62-108`)
- `MainNavigation` collects two existing ViewModel flows: workout-started state (same source as `isStarted` in WorkoutScreen) and `isTimerPaused`.
- Bottom bar visibility: `show = !(isStarted && !isTimerPaused)` — i.e. hidden only while the workout is actively running. Wrap `NavigationBar` in `AnimatedVisibility` (slide/shrink vertically) so the transition isn't a jarring pop.
- When the nav hides while the user is on another tab (edge case: they paused, switched to Log, resumed via notification/other path), force `currentTab = WORKOUT` so they're never stranded on a tab with no nav. In practice resume only happens from the Workout screen's timer chip, so this is a safety net.
- The docked Complete bar lives inside WorkoutScreen, so with nav hidden it sits against the system nav area (Scaffold insets keep it above gesture/3-button nav); with nav shown (paused) it sits directly on top of the NavigationBar — no gap in either state.

### 3. Remove targets row; tick mark on gauges (`WorkoutScreen.kt:300-324`, `WorkoutIntensityDial.kt`)
- Delete the `Spacer` + targets `Row` (lines 300-324). Card now contains only the two dials. (Per the Problem note: the row's strength-score target is a different metric from the dial's performance target and is deliberately dropped, not migrated onto the gauge — the tick marks represent the dials' own targets only.)
- Both dials currently treat target = 100% of the 260° sweep, which makes a target tick meaningless. Rescale: dial max = `target × 1.25`, so target sits at 80% of the sweep (208°). Progress arc still `coerceIn(0f, 1f)` against the new max, letting the arc visibly pass the tick when the user exceeds target.
- Tick mark: short radial line (~stroke 2.5dp, extending ~4dp beyond each edge of the track) drawn at the target angle in `TextMuted`/`BorderLight`-derived color so it's visible over both empty track and filled arc. No target number text anywhere.
- Zone label thresholds (`LOW/MEDIUM/HIGH/EXTREME`, `WorkoutIntensityDial.kt:26-28, 154-162`) stay ratio-of-target based — unchanged semantics.
- The two dial composables are near-duplicates; fold the drawing into one shared private composable parameterized by label text/color, called by both `WorkoutIntensityDial` and `WorkoutBurnDial`.

### 4. Gauge gradient coloring (`WorkoutIntensityDial.kt`)
- Replace the flat `dialColor` arc fill on both dials with a sweep gradient white → green (`GreenSuccess`) → gold (`GoldPR`) along the arc, revealed progressively: use `Brush.sweepGradient` anchored to the dial's angular range so the arc tip's color corresponds to current progress (white near 0, green around mid-progress, gold approaching/beyond the target tick).
- The pointer dot at the arc tip takes the color at the tip instead of the flat dial color (approximate by lerping white→green→gold with the same progress stops).
- Center number stays `TextDark`. Zone text + "PERFORMANCE" stay `PerformanceRed`; zone text + "BURN" stay `BurnAmber` (per decision: labels keep red/gold identities).
- Gradient stops: 0.0 white, 0.5 green, 0.8 gold (0.8 = the target tick position from §3), so "gold" visually means "at target".

### 5. Auto-dismiss Record Broken dialog (`WorkoutScreen.kt:548-693`)
- In the existing `LaunchedEffect(celeb)` (line 566), after the entrance animations, `delay(3000)` then `viewModel.dismissRecordCelebration()`.
- Manual dismissal unchanged ("Awesome!" button and outside-tap both still call dismiss). Because the effect is keyed on `celeb`, a second record queued behind the first restarts the 3s window.
- Timer counts from when the dialog actually appears (the dialog is already deferred until the exercise detail screen closes, so no early-expiry issue).

### 6. Gender required in onboarding (`OnboardingScreen.kt:63, 229-243, 559+`)
- `var gender by remember { mutableStateOf("Male") }` → `mutableStateOf("")` (empty = unselected). `OnboardingGenderStep`'s `isSelected` comparison already handles this — nothing highlights.
- The `LaunchedEffect(gender)` height/weight defaulting (lines 71-80) must ignore the empty value: only apply defaults `when (gender)` matches a real option; on empty, leave the generic defaults.
- Next gating: extend the existing step-1 pattern (`nextEnabled = step != 1 || nickname.isNotBlank()`, line 239) to `nextEnabled = when (step) { 1 -> nickname.isNotBlank(); 2 -> gender.isNotEmpty(); else -> true }`. Disabled style already exists (`disabledContainerColor`, line 242).
- Step 7 summary and `saveUserProfile` are unreachable without a gender, so no downstream change.

### 7. Records Broken hero row (`WorkoutScreen.kt:1032-1053`)
- When `summary.prCount > 0`: move the PR stat out of the shared row into a full-width hero `StatBox` rendered first (above kcal/steps), visually amplified — larger value type, gold trophy icon (`Icons.Default.EmojiEvents`), gold border/tint consistent with the Record Broken dialog's `GoldPR` styling. The kcal/steps and Lifted rows follow at current size.
- When `summary.prCount == 0`: current layout unchanged (PR tile already hidden; "Lifted" fills its row).
- Reuse/extend `StatBox` with a `hero: Boolean` (or a separate small `HeroStatBox`) rather than duplicating tile styling.

## Out of Scope

- Any theming/dark-mode unification, exercise thumbnail changes, or other items from the broader UX review not listed above.
- Changing how targets are computed (`targetScore = startingStrengthScore × totalSets`, `targetCalories = totalSets × 20 + 100`) — display only.
- Play Store screenshot refresh (worth doing after these land, tracked separately).
