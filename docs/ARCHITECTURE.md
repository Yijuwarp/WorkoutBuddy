# WorkoutBuddy – Architecture & Design

## Overview

WorkoutBuddy is an Android application that helps users log, track, and plan gym workouts. It uses a local-first approach with Room (SQLite) as its only data store — no backend, no cloud sync required.

---

## Layer Responsibilities

### 1. UI Layer (`ui/`)

Built entirely with **Jetpack Compose** + **Material 3**.

| File | Responsibility |
|---|---|
| `WorkoutScreen.kt` | Displays the active workout: exercise cards, set rows, timers, and completion CTA |
| `LogScreen.kt` | Calendar view for browsing workout history; shows workout detail on tap |
| `OnboardingScreen.kt` | First-run profile setup (nickname, age, height, weight, gender, gym experience) |
| `ProfileScreen.kt` | View/edit profile, lifetime stats (total workouts, volume, PRs) |
| `UIComponents.kt` | Shared composables: rest timer overlay, countdown overlay, summary dialog, PR badge |
| `WavyFloatingNumbers.kt` | Animated floating-number celebration effect (e.g. on PR) |
| `WorkoutIntensityDial.kt` | Visual gauge for session intensity |
| `Navigation.kt` | Bottom navigation bar with `Workout`, `Log`, and `Profile` tabs |

**State model:** The UI observes `StateFlow`s exposed by `WorkoutViewModel` via `collectAsStateWithLifecycle()`. No business logic lives in composables.

---

### 2. ViewModel Layer (`viewmodel/WorkoutViewModel.kt`)

Single ViewModel for the entire app. Responsibilities:

- **Workout generation** – determines next PPL category, selects exercises, seeds recommended weights/reps
- **Active session state** – tracks which sets are done, current exercise states
- **PR detection** – re-evaluates all sets after each update to flag new personal records
- **Timers** – three independent coroutine jobs:
  - `timerJob`: global session stopwatch
  - `cooldownJob`: per-set rest timer (auto-starts on set completion)
  - `countdownJob`: per-set countdown for CARDIO/HOLD exercises
- **Calorie & step estimation** – calculated at workout completion
- **History/Log** – manages selected date, fetches workouts by date range, loads workout detail
- **User profile management** – onboarding, profile edits, strength/stamina score updates after each session
- **Adaptive weight recommendations** – scales baseline weight ratios (e.g. Squat 0.75× bodyweight, Deadlift 0.90×, Bench 0.55×) by `strengthScore`, rounded to the nearest 2.5kg plate

**Key StateFlows:**

```kotlin
val activeWorkout: StateFlow<WorkoutEntity?>
val isWorkoutStarted: StateFlow<Boolean>
val workoutDuration: StateFlow<Long>
val activeExerciseStates: StateFlow<List<ActiveExerciseState>>
val cooldownRemaining: StateFlow<Int>
val countdownRemaining: StateFlow<Int>
val workoutSummary: StateFlow<WorkoutSummaryState?>
val allCompletedWorkouts: StateFlow<List<WorkoutEntity>>
val selectedWorkoutDetail: StateFlow<WorkoutDetailState?>
```

---

### 3. Repository Layer (`data/`)

| File | Responsibility |
|---|---|
| `WorkoutRepository.kt` | Single repository wrapping all `WorkoutDao` calls; runs DB operations on `Dispatchers.IO` |

The repository is the single source of truth. The ViewModel never touches the DAO directly. `DatabaseInitializer.kt` (in `data/database/`) seeds the default 60+ exercise library on first launch instead of a separate repository class.

---

### 4. Database Layer (`data/database/`)

**Room** database (schema version 10, destructive-fallback migration) with a single `WorkoutDao` exposing 40+ queries.

#### Entity Relationships

```
ExerciseEntity (1) ──< WorkoutSetEntity >── (1) WorkoutEntity

UserProfileEntity (single row, id = 1) — independent of the above,
read by the ViewModel to drive adaptive weight recommendations
and updated after each completed workout.
```

- An `ExerciseEntity` is a static definition (name, type, muscle group, etc.)
- A `WorkoutEntity` is one gym session
- A `WorkoutSetEntity` is a single set within a session, linked to both a workout and an exercise (cascade-deleted with its parent workout)
- A `UserProfileEntity` is the single user's profile and running strength/stamina scores

#### Key Design Decisions

- **Draft pattern**: A workout starts as `isCompleted = false` (a draft). It is marked `isCompleted = true` only when the user taps "Complete Workout". This allows mid-session state to survive app restarts.
- **Recommended vs actual values**: Each set stores both `recommendedWeight/Reps/Time/Distance` (pre-calculated suggestions) and the actual logged values. This supports displaying "suggested" alongside logged values.
- **PR flag**: `isPR` on `WorkoutSetEntity` is recalculated dynamically every time a set is updated, by comparing against all historical sets for that exercise.
- **Session snapshotting**: `WorkoutEntity` stores `startingStrengthScore`/`startingStaminaScore` (the user's scores at session start) alongside `strengthGain`/`staminaGain`, so historical sessions retain the score context they were generated under even as the live profile score changes.

---

## Data Flow: Completing a Set

```
User taps "Done" on a set
        │
        ▼
WorkoutViewModel.toggleSetCompletion(setId, true)
        │
        ├─► Fills in recommended values if user left fields blank
        ├─► repository.updateWorkoutSet(updatedSet)
        ├─► reevaluatePRsForExercise(workoutId, exerciseId)  ← checks all history
        ├─► loadExerciseStatesForWorkout(workoutId)          ← refreshes UI state
        └─► triggerCooldown(exercise)                        ← starts rest timer
```

---

## Workout Generation Flow

```
App launch / workout complete
        │
        ▼
loadOrGenerateActiveWorkout()
        │
        ├─ Draft exists? ──YES──► Load draft + its sets → display to user
        │
        └─ No draft ──────────► determineNextCategory()
                                        │
                                        ▼
                               Check last 5 completed workouts
                               → Pick: PUSH → PULL → LOWER_BODY → repeat
                                        │
                                        ▼
                               generateExercisesForWorkout()
                                        │
                               ├─ Find last workout of same category
                               ├─ Re-use same exercise IDs
                               ├─ Calculate recommended weight/reps (progressive overload)
                               └─ Append a random CARDIO exercise
                                        │
                                        ▼
                               Insert WorkoutSetEntities → display
```

---

## Timer Architecture

All three timers are `Job`s launched in `viewModelScope` using `delay(1000)` loops. They are cancelled in `onCleared()` automatically via the ViewModel scope.

```
viewModelScope
    ├── timerJob         (session stopwatch: increments every second)
    ├── cooldownJob      (rest countdown: decrements from exercise.impactLevel duration)
    └── countdownJob     (exercise countdown: decrements from set.recommendedTime)
```

Rest timer durations by impact level:
- `HIGH` → 120 seconds (2 min)
- `MEDIUM` → 60 seconds (1 min)
- `LOW` → 30 seconds

On timer expiry, `TimerExpiredReceiver` (a `BroadcastReceiver` registered by `WorkoutApplication`) fires a system notification through the channel created at app startup and plays `res/raw/chime.ogg`. `MainActivity` requests the `POST_NOTIFICATIONS` runtime permission on Android 13+ (API 33) so this still works in the background.

---

## Assets (`app/src/main/res/`)

- **`drawable/ic_ex_*.jpg`** — 63 exercise photos, one per seeded exercise, referenced by `ExerciseEntity` rows
- **`drawable/ic_launcher_foreground.xml` / `ic_launcher_background.xml`** — adaptive icon layers (API 26+)
- **`mipmap-*dpi/ic_launcher*.png`** — legacy/round launcher icons across densities
- **`raw/chime.ogg`** — sound played by `TimerExpiredReceiver` on timer expiry
- **`xml/backup_rules.xml`, `xml/data_extraction_rules.xml`** — Android backup and data-extraction policy (the Room DB is local-only, so these mainly govern what survives auto-backup)

---

## Calorie Estimation Model

Calories are estimated at workout completion using simplified metabolic equivalents:

| Type | Formula |
|---|---|
| **Lift** (weighted) | `weight × reps × 0.05 + 3.0` kcal/set |
| **Lift** (bodyweight) | `reps × 0.2 + 3.0` kcal/set |
| **Running** | `75 × distance_km` kcal |
| **Walking** | `40 × distance_km` kcal |
| **Cycling** | `30 × distance_km` kcal |
| **Elliptical** | `7 × duration_min` kcal |
| **Hold** (plank, etc.) | `0.15 × duration_sec` kcal (~9 kcal/min) |

---

## Build Configuration

| Property | Value |
|---|---|
| `compileSdk` | 36 |
| `minSdk` | 24 (Android 7.0) |
| `targetSdk` | 36 |
| `jvmToolchain` | 17 |
| `versionCode` | 1 |
| `versionName` | 1.0 |

Key Gradle plugins:
- `kotlin.serialization` – for navigation type-safe routes
- `ksp` – Room annotation processing
- `compose.compiler` – Compose compiler plugin
