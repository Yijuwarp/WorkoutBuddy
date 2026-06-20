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

---

## 🏗️ Architecture

WorkoutBuddy follows **MVVM (Model-View-ViewModel)** with a clean unidirectional data flow.

```
┌──────────────────────────────────────────────────────────┐
│                     UI Layer (Compose)                    │
│  WorkoutScreen  │  LogScreen  │  UIComponents             │
└────────────────────────┬─────────────────────────────────┘
                         │ StateFlow / collectAsState
┌────────────────────────▼─────────────────────────────────┐
│                  ViewModel Layer                          │
│                WorkoutViewModel                           │
│  (Timers, PR detection, workout generation, summaries)    │
└────────────────────────┬─────────────────────────────────┘
                         │ suspend functions / Flow
┌────────────────────────▼─────────────────────────────────┐
│                  Repository Layer                         │
│        WorkoutRepository  │  DataRepository              │
└────────────────────────┬─────────────────────────────────┘
                         │ Room DAO
┌────────────────────────▼─────────────────────────────────┐
│                  Database Layer (Room)                    │
│   WorkoutDatabase  │  WorkoutDao                         │
│   ExerciseEntity   │  WorkoutEntity  │  WorkoutSetEntity  │
└──────────────────────────────────────────────────────────┘
```

---

## 📁 Project Structure

```
app/src/main/java/com/example/workoutbuddy/
│
├── MainActivity.kt               # Entry point, sets up theme & ViewModel
├── WorkoutApplication.kt         # Application class (DI setup)
├── Navigation.kt                 # Bottom nav tabs: Workout / Log
├── NavigationKeys.kt             # Navigation route definitions
│
├── data/
│   ├── WorkoutRepository.kt      # Main repo: abstracts all DB operations
│   ├── DataRepository.kt         # Exercise seeding & static data
│   └── database/
│       ├── WorkoutDatabase.kt    # Room DB singleton (v1)
│       ├── WorkoutDao.kt         # All SQL queries via Room annotations
│       ├── ExerciseEntity.kt     # Exercise table (name, type, body part, etc.)
│       ├── WorkoutEntity.kt      # Workout session table
│       ├── WorkoutSetEntity.kt   # Individual set table (weight, reps, time, etc.)
│       └── DatabaseInitializer.kt# Pre-seeds exercises on first launch
│
├── viewmodel/
│   └── WorkoutViewModel.kt       # Core business logic, timers, state management
│
├── ui/
│   ├── screens/
│   │   ├── WorkoutScreen.kt      # Active workout UI
│   │   └── LogScreen.kt          # Workout history / calendar UI
│   └── components/
│       └── UIComponents.kt       # Shared composables (cards, dialogs, timers)
│
└── theme/
    ├── Color.kt                  # Color palette
    ├── Theme.kt                  # MaterialTheme configuration
    └── Type.kt                   # Typography
```

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

### `workouts`
| Column | Type | Description |
|---|---|---|
| `id` | Long (PK) | Auto-generated |
| `category` | String | `PUSH`, `PULL`, or `LOWER_BODY` |
| `date` | Long | Epoch timestamp of completion |
| `isCompleted` | Boolean | `false` = draft/active, `true` = finished |
| `durationInSeconds` | Long | Total session time |
| `totalCalories` | Double | Estimated calories burned |
| `totalSteps` | Int | Estimated step count |
| `prCount` | Int | Number of PRs set in this session |

### `workout_sets`
| Column | Type | Description |
|---|---|---|
| `id` | Long (PK) | Auto-generated |
| `workoutId` | Long (FK) | Parent workout |
| `exerciseId` | Int (FK) | Exercise reference |
| `setNumber` | Int | Set order (1, 2, 3…) |
| `recommendedWeight` | Double? | AI-suggested weight |
| `recommendedReps` | Int? | AI-suggested reps |
| `recommendedTime` | Int? | AI-suggested duration (seconds) |
| `recommendedDistance` | Double? | AI-suggested distance (km) |
| `weight` | Double? | Actual weight logged |
| `reps` | Int? | Actual reps logged |
| `time` | Int? | Actual time logged |
| `distance` | Double? | Actual distance logged |
| `isCompleted` | Boolean | Whether the set was completed |
| `isPR` | Boolean | Whether this set was a PR |

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
