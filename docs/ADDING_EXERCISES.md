# Adding New Exercises

A repeatable runbook for expanding the exercise library. Each exercise needs five
things: a **seed entry**, a **thumbnail image**, a **YouTube how-to link**, **how-to
steps**, and the right **scoring fields**. The scoring "just works" once the fields are
set correctly — there's no per-exercise scoring code to write.

All thumbnails come from [wger.de](https://wger.de/api/v2/) (an open-source workout
manager, exercise images under open/AGPL licenses), converted to the app's house style:
**200×200 grayscale baseline JPEG, ~6–9 KB**, named `ic_ex_<sanitized_name>.jpg`.

---

## TL;DR

```bash
cd tools/exercise_import
pip install pillow                       # once
python wger_import.py fetch              # cache the wger catalog (re-run to refresh)
python wger_import.py search "upright row"   # find the wger id (only image-backed hits shown)
python wger_import.py convert 694="Dumbbell Upright Row"   # download + grayscale + drop into drawable/
```

Then, by hand:
1. Add an `ExerciseEntity(...)` to `DatabaseInitializer.kt` (name must match the `convert` name exactly).
2. Get a YouTube link via web search and paste it as `youtubeUrl`.
3. Bump the DB version in `WorkoutDatabase.kt` so the seed re-runs.
4. Build, install, verify.

---

## Step 1 — Pick the exercises

The existing set already covers most basics. Before adding, skim
`DatabaseInitializer.kt` so you don't create near-duplicates (e.g. there's already a
generic "Shrugs", "Walking Lunges", "Cable Chest Fly", etc.). Good additions are
genuinely distinct movements or the missing barbell/dumbbell counterpart of an existing
one.

## Step 2 — Find each exercise's wger image id

```bash
python wger_import.py fetch                 # only needed once per session
python wger_import.py search "<keywords>"
```

`search` only lists wger entries **that have an image**, so anything it returns is safe
to use. Note the numeric id. The wger *name* shown may be in another language (their
catalog is multilingual) — that's fine, you control the app-facing name yourself.

## Step 3 — Download + convert the thumbnails

```bash
python wger_import.py convert <id>="<App Exercise Name>" [<id>="<Name>" ...]
```

This downloads the 200×200 thumbnail, flattens transparency on white, converts to
grayscale, recompresses to land in the existing size band, and writes
`app/src/main/res/drawable/ic_ex_<sanitized_name>.jpg`. The sanitization
(`lowercase`, non-alphanumerics → `_`) **must** match the `name` you use in
`DatabaseInitializer.kt` — that's how `ExerciseThumbnail` resolves the drawable at
runtime (see `getExerciseDrawableResourceName` in `UIComponents.kt`). If they don't
match, the app silently falls back to the generic dumbbell icon.

## Step 4 — Get a YouTube how-to link

**For compound/functional lifts, check the official CrossFit channel first** — it has a
clean "CrossFit Foundational Movement" series (back squat, front squat, air squat,
deadlift, sumo deadlift, shoulder press, push press, push jerk, good morning) plus
standalone videos for push-ups, pull-ups, and the walking lunge. These are well-produced,
stable (HQ-published, unlikely to be taken down), and consistent in style across
exercises. Verify it's actually the official channel before using a link — search titles
often surface third-party "CrossFit [Gym Name]" affiliate channels, which are a different
source and shouldn't be attributed as the official one. The most reliable check:

```bash
curl -s "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=<id>&format=json"
```

This returns the real `author_name` for the video — only treat it as a CrossFit-channel
link if that field is exactly `CrossFit`. CrossFit's channel does **not** cover
machine/isolation work (leg extension, preacher curl, pec deck, cable flies, concentration
curl, rows, etc.) — those aren't CrossFit movements, so don't force a fit.

For everything else, search the web for `"<exercise> how to form tutorial youtube"` and
pick a real, current `https://www.youtube.com/watch?v=...` URL. **Don't fabricate IDs** —
dead links look broken in the app. Verify by opening the link if unsure.

## Step 5 — Write the seed entry

Add to the matching category block in `DatabaseInitializer.kt`:

```kotlin
ExerciseEntity(
    name = "Dumbbell Upright Row",     // MUST match the convert name (drives the image lookup)
    category = "PULL",                  // PUSH | PULL | LOWER_BODY (drives the PPL rotation)
    type = "LIFT",                      // LIFT | CARDIO | HOLD (drives all scoring, see below)
    bodyPart = "Traps & Shoulders",    // display only
    calorieBurnRate = 0.18,            // see field guide below
    description = "One-sentence summary shown in the How-To sheet.",
    howToSteps = "Step 1.\nStep 2.\nStep 3.",   // newline-separated
    impactLevel = "MEDIUM",            // LOW | MEDIUM | HIGH -> rest timer 30 / 60 / 120 s
    youtubeUrl = "https://www.youtube.com/watch?v=K0dYqPCaO14"
),
```

## Step 6 — Make the new exercises actually appear

Seeding only runs when the exercises table is **empty** (`seedDatabaseIfEmpty()` in
`WorkoutRepository.kt`). On an existing install the table isn't empty, so new seed
entries won't show up until the DB is rebuilt. The DB uses
`fallbackToDestructiveMigration()`, so **bump the version** in `WorkoutDatabase.kt`:

```kotlin
version = 12,   // was 11
```

This wipes and re-seeds on next launch (user re-onboards — fine for dev). If you ever
need to add exercises *without* wiping user data, you'd instead change
`seedDatabaseIfEmpty()` to insert-missing-by-name rather than only-when-empty.

## Step 7 — Build, install, verify

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.example.workoutbuddy/.MainActivity
```

Open an exercise — confirm the thumbnail renders (not the fallback icon), the How-To
sheet shows your steps, and the YouTube link opens.

---

## Field guide: how each field drives scoring

There is **no per-exercise scoring code** — these four behaviours are derived entirely
from the fields above (see `WorkoutViewModel.calculateSetPerformance`,
`calculateSetCalories`, the strength/stamina block in `toggleSetCompletion`, and
`getAdaptiveStartWeight`).

| Field | Drives |
|---|---|
| `type = "LIFT"` | Performance = `weight × (1 + reps/30)`; bodyweight (weight 0) uses `bodyWeight × 0.6`. Calories = `weight×reps×0.05 + 3` (or `reps×0.2 + 3` bodyweight). **Strength** rises only on a genuine PR; **stamina** is barely touched. Use for all resistance moves. |
| `type = "CARDIO"` | Performance from distance/speed/incline; calories from distance/duration. Drives **stamina** (not strength). Distance/time defaults scale off the user's stamina score. |
| `type = "HOLD"` | Performance = `time × 0.6`; calories = `time × 0.15`. Drives **stamina**. PR = longest hold. |
| `category` | PPL rotation bucket (PUSH → PULL → LOWER_BODY). Pick the day it belongs to. |
| `impactLevel` | Post-set rest timer: `LOW`=30 s, `MEDIUM`=60 s, `HIGH`=120 s. Heavy compounds → HIGH, isolation → LOW. |
| `calorieBurnRate` | Only used as `kcal/min` for **CARDIO** (the fallback branch). For LIFT/HOLD it's nominal — match nearby entries (~0.15 isolation, ~0.30 compound, ~0.40+ big compound). |
| `name` | Resolves the thumbnail (Step 3) **and** the easy starting weight via keyword match in `getAdaptiveStartWeight` (e.g. names containing "Barbell Bench Press" → 0.40×BW, "Dumbbell" → 0.15×BW, "Push-ups/Dips/Pull-ups/Chin-ups" → bodyweight/0 kg, everything else → 0.10×BW). If a new lift should start heavier/lighter, either name it to hit an existing bucket or add a branch to `getAdaptiveStartWeight`. |

Per-workout gains are capped globally (max +3 regular, +1 high-intensity bonus = +4 per
stat) in `WorkoutViewModel` — new exercises inherit this automatically.

---

## Licensing

wger exercise images are community-contributed under open licenses; wger itself is AGPL.
A couple of thumbnails (Decline Dumbbell Press, Dumbbell Skull Crushers) came from
Wikimedia Commons instead, since wger has no image for them — these are Everkinetic
illustrations under **CC BY-SA 3.0**, which requires attribution if redistributed.
Keep attribution intact for both sources if you redistribute. The `wger_import.py` cache
files (`wger_*.json`) are gitignored — regenerate with `fetch`.

Avoid sourcing images from `free-exercise-db` (or similar "public domain" claims without
an actual LICENSE file) — its own GitHub issues raise unresolved questions about whether
the images are genuinely rights-cleared, despite the public-domain label in its README.
