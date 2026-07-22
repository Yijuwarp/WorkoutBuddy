# Concise settings, auto-progression, first-open tour, summary cleanup, difficulty slider redesign, and Body tab

## Problem Statement

Several rough edges have accumulated across the app:

1. Settings descriptions on `ProfileScreen` are wordy.
2. There is no auto-progression: each new week the user gets a workout pre-filled with last time's best set, but the app never nudges reps/distance/time upward on its own — progression is entirely manual.
3. New users land on `WorkoutScreen` after onboarding with no explanation of the muscle-group selector, refresh button, or the two performance dials.
4. The Workout Completed summary always shows a step-count tile (even at 0 steps), labels total weight moved as "Volume" instead of plain language, and counts a first-time exercise's very first logged set as a "record broken," which inflates the PR tile and can make it appear even when nothing was actually beaten.
5. The difficulty slider (Profile → Settings) is a plain Material3 `Slider` with labels below it, visually inconsistent with the custom Canvas-based gym-experience slider used in onboarding.
6. There's no view of overall progress (PRs/trends per exercise) or per-muscle-group recovery; the bottom nav only has Workout, Log, and Profile.

## Solution

1. Rewrite the four settings descriptions on `ProfileScreen` to be one short clause each.
2. Add auto-progression for both lift and cardio exercises: each time an exercise is selected into a new workout, its recommended values step up from the last logged set according to a fixed rule (reps for lifts up to a cap then weight increases and reps reset; distance/time for cardio increase at constant pace).
3. Add a 4-step coach-mark tour shown once on first arrival at `WorkoutScreen`, pointing at the muscle-group selector, the refresh button, and the two dials, ending on a "We've set up your first workout — hit Start Workout" message.
4. On the Workout Completed dialog: hide the step tile when steps are 0, rename the "Volume" tile to "Lifted", stop flagging a first-ever logged set on a new exercise as a PR, and hide the "Records broken" tile entirely when the count is 0.
5. Rebuild `DifficultySlider` using the same custom Canvas-track + snap-notch + Layout-aligned-label approach as `OnboardingGymExperienceStep`'s gym-experience slider, applied to 3 stops (Easy/Medium/Hard) instead of 3 experience levels.
6. Add a new "Body" tab in the bottom nav at position 1 (Workout, **Body**, Log, Profile), with two sub-sections — Results (reuses the ProfileScreen top section: name, strength, stamina, plus a new per-exercise PR + trend list) and Recovery (a percentage bar per muscle group that drains with training and refills over time).

## User Stories

1. As a user reading Settings, I want each option's description to be a single short line, so I can scan them quickly.
2. As a user who hit 8 reps @ 7.5kg on Bicep Curl last week, I want this week's recommended Bicep Curl set to start at 9 reps @ 7.5kg, so my plan keeps me progressing without me doing math.
3. As a user who's plateaued at 12 reps on a given weight, I want the app to bump the weight and reset to a lower starting rep count next time, so I keep progressing past a rep ceiling.
4. As a user logging cardio, I want each new cardio session to ask for slightly more distance and time than last time at the same pace, so my cardio also progresses automatically.
5. As a brand-new user landing on the workout screen for the first time, I want a quick guided tour of the muscle-group selector, refresh button, and dials, so I understand the screen before I start.
6. As a new user finishing the tour, I want to be told my first workout is ready and prompted to start it, so I know exactly what to do next.
7. As a user who didn't hit any step count this workout (e.g. all-upper-body lifting), I don't want to see a "0 steps" tile cluttering my summary.
8. As a user, I want the weight-times-reps tile to just say "Lifted" instead of the more technical "Volume", so the summary reads more naturally.
9. As a user doing an exercise for the very first time, I don't want my first-ever set on it counted as a "record broken" — there was nothing to break.
10. As a user whose workout broke zero real records, I don't want an empty/zero "Records broken" tile shown at all.
11. As a user adjusting my difficulty setting, I want the slider to look and feel like the gym-experience slider I used during onboarding, so the app feels visually consistent.
12. As a user, I want a "Body" tab where I can see my name, strength, stamina, and a list of every exercise I've done with its PR and recent trend, so I have one place to check overall progress.
13. As a user, I want a "Recovery" view showing how rested each muscle group is, so I know which muscle groups are safe to train again versus still recovering.

## Implementation Decisions

### 1. Concise settings descriptions
- Rewrite in place in `ProfileScreen.kt` (lines ~366-473), no data/model changes:
  - Rest Timers: "Auto-start a rest timer after each set"
  - Equipment: "Only show workouts using gear you own"
  - Difficulty: "Higher tiers include all tiers below"
  - Manage Exercises: "Show more or less of specific exercises"
- No localization infrastructure exists (strings are hardcoded in Compose, not `strings.xml`); keep doing it the same way.

### 2. Auto-progression
- **Trigger point:** when a workout is generated and an exercise is selected, instead of (or in addition to) seeding `recommendedWeight/Reps/Time/Distance` from the prior best set verbatim (`WorkoutViewModel.kt:256-286`), apply a progression step on top of the most recent logged set for that exercise (not the best-ever set — progression should compound from last time, not jump back to an old peak).
- **Lift progression rule:** given last logged set `(weight, reps)`:
  - If `reps < 12`: recommend `(weight, reps + 1)`.
  - If `reps >= 12`: recommend `(nextWeightIncrement(weight), startingReps)`, where `nextWeightIncrement` uses the existing equipment-aware weight-rounding step already used elsewhere for plate/dumbbell increments, and `startingReps` is a fixed reset value (8) rather than recomputing from `getAdaptiveStartWeight()`.
  - Applies per exercise independently — progression state is derived from that exercise's own most recent `WorkoutSetEntity`, not a separate progression table.
- **Cardio progression rule:** given last logged set `(distance, time)`, implying pace `time/distance`:
  - Recommend `(distance * 1.05, time * 1.05)` — a flat 5% increase to both distance and time each time the exercise is logged again, which holds pace (speed) constant since both scale by the same factor.
- **No new entity required** — progression is computed at recommendation time from existing `WorkoutSetEntity` history (most recent set per exercise), the same way current recommendation logic already reads history; only the formula in `WorkoutViewModel.kt:256-327` changes from "copy last/best set" to "step last set forward."
- **First time on an exercise:** falls back to the existing `getAdaptiveStartWeight()` / stamina-based cardio start, unchanged — progression only applies once there's a prior logged set to step from.
- **Edge case — exercise not done for several weeks:** still steps from the single most recent logged set, regardless of gap length (no decay/regression for time elapsed, to keep the rule simple and match what was requested).

### 3. First-open tour
- **Trigger:** a new boolean flag, e.g. `hasSeenWorkoutTour` on `UserProfileEntity` (default `false`), checked on `WorkoutScreen` entry. Distinct from `difficultyCeiling == null` (onboarding-complete) — the tour runs once, immediately after onboarding finishes and the first workout is generated, not on every cold start.
- **Steps (4), each a coach-mark overlay anchored to its target with a dimmed backdrop and a "Next" affordance:**
  1. Muscle-group selector → "Switch muscle group here."
  2. Refresh button → "Don't like this set? Refresh for a new one."
  3. The two dials → "These track your performance and calories burned during the workout."
  4. Centered, no anchor → "We've set up your first workout — hit Start Workout when you're ready." (final step has a "Got it" affordance instead of "Next")
- On completing or dismissing the last step, set `hasSeenWorkoutTour = true` and persist immediately so it never reappears, including after process death mid-tour (write the flag on tour completion, not on `WorkoutScreen` entry).
- Skippable: an "X"/"Skip" affordance available at every step exits the whole tour and sets the flag immediately, same as completing it.

### 4. Workout Completed summary cleanup
- File: `WorkoutScreen.kt` (`WorkoutSummaryDialog`, lines ~827-924).
- **Steps tile:** wrap existing `StatBox` for steps in a conditional — render only if `animatedSteps > 0` (final value, not the in-flight animated counter, so the tile doesn't flicker in/out as the count animates from 0).
- **"Volume" → "Lifted":** rename the label only; no change to the underlying calculation (still total weight × reps for the session).
- **First-time PR exclusion:** in PR-detection logic (`WorkoutViewModel`, set-completion path that sets `WorkoutSetEntity.isPR`), only mark `isPR = true` when a prior logged set exists for that exercise and the new set beats it. A set logged on an exercise with zero prior history sets `isPR = false` instead of defaulting true. This changes both the PR-count tile and the existing PR celebration dialog (lines ~550-665), which should also stop firing on an exercise's first-ever set.
- **Records-broken tile visibility:** conditionally render the existing PR-count `StatBox` only when `animatedPrCount > 0` (final value), same gating pattern as steps.

### 5. Difficulty slider redesign
- Port the implementation pattern from `OnboardingGymExperienceStep` (`OnboardingScreen.kt:355-504`) into `DifficultySlider` (`UIComponents.kt:2864-2904`): custom Canvas-drawn track with 3 snap notches, animated progress arc, `BluePrimary` active / `Color.White.copy(alpha=0.1f)` inactive track, `BlueSecondary` notches and thumb, and the custom `Layout` that aligns "Easy"/"Medium"/"Hard" labels exactly under their notches.
- Keep the existing snap-to-3-discrete-values behavior and existing `DIFFICULTY_SLIDER_STEPS`-driven value semantics — this is a visual/component swap, not a behavior change to what difficulty selection does.
- Extract the shared Canvas/Layout drawing logic into a small reusable composable if practical (both sliders are now visually identical, 3-stop snap sliders differing only in labels/values), rather than duplicating the Canvas code a second time.

### 6. Body tab (Results + Recovery)
- **Bottom nav:** insert a new `BODY` destination into `Navigation.kt` (currently `WORKOUT`(0) / `LOG`(1) / `PROFILE`(2)) at index 1, shifting Log to 2 and Profile to 3: `WORKOUT`(0) / `BODY`(1) / `LOG`(2) / `PROFILE`(3). Icon: a body/fitness glyph distinct from the existing three (e.g. `Icons.Default.Favorite` or `Icons.Default.MonitorHeart` — finalize during implementation against available icon set).
- **Screen structure:** `BodyScreen` with an internal two-tab or segmented control for "Results" and "Recovery" (Results is the default/first tab).
- **Results tab:**
  - Top: reuse the existing `ProfileScreen` top-section composable (rank badge, name, strength/stamina circles, `ProfileScreen.kt:103-212`) — extract it into a shared composable rather than duplicating, since it now renders on two screens.
  - Below: a list of every exercise the user has logged at least once, each row showing exercise name, current PR (best weight/reps for lifts, best distance/time for cardio), and a trend indicator (e.g. up/flat/down arrow comparing the last 2-3 sessions of that exercise). Exercises never logged are omitted entirely (no empty/placeholder rows), per the requirement to only list exercises "that have been done."
  - Trend computation: derive from existing `WorkoutSetEntity` history per exercise at render/query time — no new entity needed for "current PR", since `isPR`-flagged sets already exist; trend direction needs a simple comparison across the last few sessions' best set per exercise (exact window, e.g. last 3 sessions, finalized during implementation).
- **Recovery tab:**
  - One row per individual muscle (Chest, Back, Shoulders, Biceps, Triceps, Quads, Hamstrings, Glutes, Calves, Core — not the coarse PUSH/PULL/LOWER_BODY category), each with a percentage bar that depletes when that muscle is trained and refills over elapsed time. Each exercise's free-text `bodyPart` (e.g. "Chest & Lats", "Upper Chest", "Front Deltoids") is mapped onto this canonical list; a set can bump multiple muscles if its bodyPart names more than one. "Cardio" and "Full Body" bodyParts don't map to any muscle and contribute no fatigue.
  - **New persisted state required:** a per-muscle-group "last trained timestamp" and/or "current fatigue %" — add a new Room entity (e.g. `MuscleGroupRecoveryEntity(muscleGroup, lastTrainedAt, fatiguePct)`) since nothing today tracks recovery/fatigue at the muscle-group level. Requires a real Room `Migration`, consistent with how the existing `ExercisePreferenceEntity` addition was handled (see [exercise-frequency-and-difficulty-preferences.md](exercise-frequency-and-difficulty-preferences.md)) — not the destructive-wipe fallback.
  - **Recovery formula:** recovery is accumulative and time-based, computed on read (no background job/ticker):
    - Each muscle group has a stored `fatiguePct` (0% = fully recovered, 100% = fully fatigued) and `lastUpdatedAt` timestamp.
    - On completing a set for an exercise, each muscle group it targets first has its `fatiguePct` decayed for elapsed time since `lastUpdatedAt` (20%/day, see below), then gets a fixed fatigue bump added on top (e.g. +25%, tune in implementation) and clamped to 100% — this is what makes fatigue accumulative: training a partially-recovered muscle group again pushes it higher rather than resetting to 100%, so back-to-back days on the same muscle group compound.
    - Recovery rate is fixed at 20% per day (linear), i.e. full recovery from 100% fatigue takes 5 days. Recovery % shown to the user = `100 - fatiguePct`, recomputed at render time from `lastUpdatedAt` rather than stored as a separate field.
    - `lastUpdatedAt` is updated to "now" whenever `fatiguePct` is recomputed (on a new set completion for that muscle group), so the decay calculation always measures from the last touch point rather than re-decaying from an old timestamp.
  - Exercises that hit multiple muscle groups affect all of them; exact per-exercise-to-muscle-group mapping reuses whatever categorization `ExerciseEntity` already exposes today.

## Testing Decisions

- **Auto-progression:** unit tests on the new lift-progression formula covering: reps < 12 increments reps only; reps == 12 increments weight and resets reps to the fixed starting value; first-ever logged set on an exercise still uses the existing adaptive-start path, not the progression formula. Cardio: unit test that distance increases by the fixed step and time increases proportionally to hold pace constant.
- **PR/summary logic:** unit tests on set-completion PR detection confirming a first-ever set on a new exercise never sets `isPR = true`, and that a second-or-later set still triggers `isPR = true` correctly when it beats prior history. UI-level check (manual or snapshot) that the steps and records-broken tiles don't render at zero.
- **Tour:** manual check that `hasSeenWorkoutTour` gates the tour correctly across onboarding completion, skip, and full completion, and that it survives process death once set.
- **Recovery migration:** since this adds a new Room entity, follow the same migration-testing bar as prior schema changes — an explicit `Migration` test, not reliance on `fallbackToDestructiveMigration()`.

## Open Questions

- Cardio progression rate (5% per session) and recovery rate (20%/day, accumulative) are now fixed per product decision — see Implementation Decisions §2 and §6.
- Remaining unspecified tuning constants (trend-comparison window for the Results list, and the fixed per-set fatigue bump added on top of decay in Recovery) are flagged above as "finalize during implementation," since they're product-feel decisions better tuned against real workout data than guessed up front.
