# 💪 WorkoutBuddy

A smart, adaptive Android workout tracker built with **Jetpack Compose**, **Room**, and **MVVM** architecture. WorkoutBuddy automatically plans your next workout based on your training history, tracks progressive overload, and records personal records (PRs) — all in a clean, modern UI.

---

## ✨ Features

| Feature | Description |
|---|---|
| **Smart Workout Planning** | Automatically rotates Push → Pull → Lower Body splits based on your history |
| **Progressive Overload** | Recommends weight/reps each session based on what you last lifted |
| **Personal Records (PRs)** | Detects and flags new PRs in real time as you log sets |
| **Rest Timer** | Auto-starts a cooldown timer after each completed set (based on exercise intensity) |
| **Cardio & Hold Timers** | Countdown timer for timed exercises (running, planks, etc.) with pause/resume |
| **Workout Timer** | Full-session stopwatch with pause/resume support |
| **Workout Summary** | Post-workout overlay showing calories burned, steps, PRs, and duration |
| **Workout Log** | Calendar-based history view with detailed per-workout breakdowns |
| **Muscle Impact Map** | Visual breakdown of which muscle groups were worked in each session |
| **Calorie Estimation** | Per-set calorie calculations for lifts, cardio, and holds |
| **User Profiling & Onboarding** | First-run setup (nickname, age, height, weight, gender, gym experience) drives adaptive weight recommendations |
| **Strength/Stamina Scoring** | Tracks a running strength and stamina score per user, updated after every completed workout |
| **Timer Notifications** | Posts a system notification + chime when the rest/countdown timer expires, even if the app isn't in the foreground |

---

## 🏗️ Architecture

WorkoutBuddy follows **MVVM (Model-View-ViewModel)** with a clean unidirectional data flow.

```
┌──────────────────────────────────────────────────────────┐
│                     UI Layer (Compose)                    │
│  WorkoutScreen │ LogScreen │ OnboardingScreen │           │
│  ProfileScreen │ UIComponents │ WorkoutIntensityDial      │
└────────────────────────┬─────────────────────────────────┘
                         │ StateFlow / collectAsState
┌────────────────────────▼─────────────────────────────────┐
│                  ViewModel Layer                          │
│                WorkoutViewModel                           │
│  (Timers, PR detection, workout generation, summaries,    │
│   profile management, adaptive weight recommendations)    │
└────────────────────────┬─────────────────────────────────┘
                         │ suspend functions / Flow
┌────────────────────────▼─────────────────────────────────┐
│                  Repository Layer                         │
│                  WorkoutRepository                        │
└────────────────────────┬─────────────────────────────────┘
                         │ Room DAO
┌────────────────────────▼─────────────────────────────────┐
│                  Database Layer (Room, v10)                │
│   WorkoutDatabase  │  WorkoutDao                          │
│   ExerciseEntity │ WorkoutEntity │ WorkoutSetEntity │      │
│   UserProfileEntity                                        │
└──────────────────────────────────────────────────────────┘
```

---

## 📁 Project Structure

```
app/src/main/java/com/example/workoutbuddy/
│
├── MainActivity.kt               # Entry point; sets up theme, ViewModel, requests
│                                  # POST_NOTIFICATIONS permission on Android 13+
├── WorkoutApplication.kt         # Application class (DI setup, notification channel)
├── Navigation.kt                 # Bottom nav: Workout / Log / Profile tabs
├── NavigationKeys.kt             # Navigation route definitions
├── TimerExpiredReceiver.kt       # BroadcastReceiver: posts notification + chime
│                                  # when a rest/countdown timer expires
│
├── data/
│   ├── WorkoutRepository.kt      # Main repo: abstracts all DB operations (Dispatchers.IO)
│   └── database/
│       ├── WorkoutDatabase.kt    # Room DB singleton (v10, destructive fallback migration)
│       ├── WorkoutDao.kt         # 40+ SQL queries via Room annotations
│       ├── ExerciseEntity.kt     # Exercise table (name, type, body part, etc.)
│       ├── WorkoutEntity.kt      # Workout session table
│       ├── WorkoutSetEntity.kt   # Individual set table (weight, reps, time, etc.)
│       ├── UserProfileEntity.kt  # Single-row user profile (stats, scores)
│       └── DatabaseInitializer.kt# Pre-seeds 60+ exercises on first launch
│
├── viewmodel/
│   └── WorkoutViewModel.kt       # Core business logic: timers, state, PR detection,
│                                  # profile management, adaptive weight recommendations
│
├── ui/
│   ├── screens/
│   │   ├── WorkoutScreen.kt      # Active workout UI
│   │   ├── LogScreen.kt          # Workout history / calendar UI
│   │   ├── OnboardingScreen.kt   # First-run user profile setup
│   │   └── ProfileScreen.kt      # Profile management & lifetime stats
│   └── components/
│       ├── UIComponents.kt       # Shared composables (cards, dialogs, timers, PR badges)
│       ├── WavyFloatingNumbers.kt# Animated celebratory number effect
│       └── WorkoutIntensityDial.kt# Visual intensity gauge
│
└── theme/
    ├── Color.kt                  # Color palette
    ├── Theme.kt                  # MaterialTheme configuration
    └── Type.kt                   # Typography
```

### Assets (`app/src/main/res/`)
- `drawable/ic_ex_*.jpg` – 63 exercise photos, one per seeded exercise
- `drawable/ic_launcher_foreground.xml`, `ic_launcher_background.xml` – adaptive icon layers
- `mipmap-*dpi/ic_launcher*.png` – legacy launcher icons (incl. round variants)
- `raw/chime.ogg` – sound played when a timer expires
- `xml/backup_rules.xml`, `xml/data_extraction_rules.xml` – Android backup/data-extraction policy

---

## 🗄️ Database Schema

### `exercises`
| Column | Type | Description |
|---|---|---|
| `id` | Int (PK) | Auto-generated |
| `name` | String | Exercise name |
| `type` | String | `LIFT`, `CARDIO`, or `HOLD` |
| `category` | String | `PUSH`, `PULL`, or `LOWER_BODY` |
| `bodyPart` | String | Primary muscle group |
| `impactLevel` | String | `LOW`, `MEDIUM`, or `HIGH` (drives rest timer) |
| `calorieBurnRate` | Double | kcal/min for cardio exercises |
| `description` | String | Short exercise description |
| `howToSteps` | String | Newline-separated instructional steps |
| `youtubeUrl` | String | Tutorial video link |

### `workouts`
| Column | Type | Description |
|---|---|---|
| `id` | Long (PK) | Auto-generated |
| `category` | String | `PUSH`, `PULL`, or `LOWER_BODY` |
| `date` | Long | Epoch timestamp of completion |
| `isCompleted` | Boolean | `false` = draft/active, `true` = finished |
| `isStarted` | Boolean | Whether the session timer was started |
| `durationInSeconds` | Long | Total session time |
| `totalCalories` | Double | Estimated calories burned |
| `totalSteps` | Int | Estimated step count |
| `totalVolumeKg` | Double | Sum of all weight lifted in the session |
| `prCount` | Int | Number of PRs set in this session |
| `intensityScore` | Double | Calculated session intensity |
| `startingStrengthScore` | Double | User's strength score at session start |
| `startingStaminaScore` | Double | User's stamina score at session start |
| `strengthGain` | Double | Strength score gain from this session |
| `staminaGain` | Double | Stamina score gain from this session |

### `workout_sets`
| Column | Type | Description |
|---|---|---|
| `id` | Long (PK) | Auto-generated |
| `workoutId` | Long (FK, CASCADE) | Parent workout |
| `exerciseId` | Int (FK) | Exercise reference |
| `setNumber` | Int | Set order (1, 2, 3…) |
| `recommendedWeight` | Double? | AI-suggested weight |
| `recommendedReps` | Int? | AI-suggested reps |
| `recommendedTime` | Int? | AI-suggested duration (seconds) |
| `recommendedDistance` | Double? | AI-suggested distance (km) |
| `weight` | Double? | Actual weight logged |
| `reps` | Int? | Actual reps logged |
| `time` | Int? | Actual time logged (seconds) |
| `distance` | Double? | Actual distance logged (km) |
| `inclinePct` | Double? | Incline % for treadmill/bike exercises |
| `isCompleted` | Boolean | Whether the set was completed |
| `isPR` | Boolean | Recalculated dynamically against exercise history |

### `user_profile`
| Column | Type | Description |
|---|---|---|
| `id` | Int (PK) | Always `1` — single-row table |
| `nickname` | String | Display name |
| `age` | Int | User age |
| `height` | Double | Height in cm |
| `weight` | Double | Weight in kg |
| `gender` | String | Male/Female/Other |
| `gymExperience` | String | Beginner/Intermediate/Advanced |
| `strengthScore` | Double | Running strength metric, drives weight recommendations |
| `staminaScore` | Double | Running stamina metric (default 100.0) |

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- Android SDK 36
- Java 17 (configured via `jvmToolchain(17)`)
- Min SDK: Android 7.0 (API 24)

### Build & Run

```bash
# Clone the repo
git clone <your-repo-url>
cd WorkoutBuddy

# Open in Android Studio and sync Gradle, or build via CLI:
./gradlew assembleDebug

# Install on a connected device/emulator
./gradlew installDebug
```

---

## 🛠️ Tech Stack

| Technology | Purpose |
|---|---|
| **Kotlin** | Primary language |
| **Jetpack Compose** | UI framework |
| **Material 3** | Design system |
| **Room** | Local SQLite database |
| **ViewModel + StateFlow** | State management |
| **Kotlin Coroutines** | Async operations & timers |
| **Navigation3** | Compose navigation |
| **KSP** | Kotlin Symbol Processing for Room code-gen |

---

## Permissions

| Permission | Purpose |
|---|---|
| `POST_NOTIFICATIONS` | Required on Android 13+ to show timer-expiry notifications |
| `VIBRATE` | Vibration feedback on timer expiry |
| `SCHEDULE_EXACT_ALARM` | Precise timer/notification scheduling |
| `INTERNET` | Reserved for future use (e.g. loading YouTube tutorial links) |

---

## Testing

| File | Covers |
|---|---|
| `app/src/test/.../WorkoutCalculationsTest.kt` | Unit tests for calorie formulas (lift/hold/cardio), step estimation, and progressive overload logic |
| `app/src/androidTest/.../ui/main/MainScreenTest.kt` | Compose UI test for the main screen |

```bash
./gradlew test               # unit tests
./gradlew connectedAndroidTest  # instrumented UI tests
```

---

## 📋 Workout Rotation Logic

WorkoutBuddy uses a simple **PPL (Push/Pull/Legs)** rotation:

```
PUSH → PULL → LOWER_BODY → PUSH → ...
```

On each new session:
1. The app checks the last 5 completed workouts to determine what category comes next.
2. It finds the most recent workout of that same category and **re-uses the same exercises**.
3. It recommends weights/reps based on **progressive overload rules**:
   - If you hit ≥12 reps last time → weight increases, reps drop to 8.
   - Otherwise → same weight, aim for 10 reps.
4. A **random cardio exercise** is always appended at the end.
5. If no history exists, a default starter set is used.

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/my-feature`)
3. Commit your changes (`git commit -m 'Add my feature'`)
4. Push to the branch (`git push origin feature/my-feature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License.
