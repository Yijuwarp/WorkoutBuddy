package com.example.workoutbuddy.viewmodel

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.workoutbuddy.R
import com.example.workoutbuddy.audio.AppSound
import com.example.workoutbuddy.audio.Haptics
import com.example.workoutbuddy.audio.SoundPlayer
import com.example.workoutbuddy.data.WorkoutRepository
import com.example.workoutbuddy.data.database.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.Calendar

private const val PERFORMANCE_MULTIPLIER = 1.5

// Strength score gains are driven solely by genuine PRs on LIFT exercises, scaled by
// how much the new PR beats the previous best by (in calculateSetPerformance units).
private const val STRENGTH_PR_GAIN_FACTOR = 0.15
private const val MAX_STRENGTH_GAIN_PER_PR = 15.0

// Per-workout caps: regular (PR-driven) strength/stamina gains are capped at 3 per workout,
// plus up to +1 bonus each if the workout hits "high intensity" performance/calories at
// completion — so the absolute max either stat can move in a single workout is 4.
private const val MAX_REGULAR_GAIN_PER_WORKOUT = 3.0
private const val HIGH_INTENSITY_BONUS = 1.0
private const val HIGH_INTENSITY_RATIO_THRESHOLD = 0.65

// New-workout exercise selection: how many non-cardio exercises to pick, and how many
// of them may share the same bodyPart before the rest of the pool is preferred instead.
private const val MAIN_EXERCISES_PER_WORKOUT = 4
private const val MAX_EXERCISES_PER_BODY_PART = 2

// Auto-progression: lift reps climb to a cap, then weight steps up and reps reset.
private const val LIFT_REP_CAP = 12
private const val LIFT_REPS_RESET = 8
// Auto-progression: cardio distance/time both scale by this factor each session, holding pace constant.
private const val CARDIO_PROGRESSION_FACTOR = 1.05

// Muscle group recovery: fatigue per set scales with how hard the set was relative to the
// user's strength/stamina score. BASE_FATIGUE_BUMP is the bump applied when performance
// exactly equals the user's score (i.e. a "normal" set). Sets below that score cost less;
// sets above cost more, clamped between FATIGUE_INTENSITY_MIN and FATIGUE_INTENSITY_MAX
// so a very easy set still costs something and one extreme set can't fully drain the bar.
// Recovery drains fatigue at a fixed rate; a fully fatigued muscle takes 5 days to recover.
private const val BASE_FATIGUE_BUMP = 25.0
private const val FATIGUE_INTENSITY_MIN = 0.2   // floor: ≥5% bar per set
private const val FATIGUE_INTENSITY_MAX = 2.0   // ceiling: ≤50% bar per set
private const val RECOVERY_PCT_PER_DAY = 20.0
private const val MS_PER_DAY = 86_400_000.0

// Canonical individual muscles the Body tab's Recovery view tracks/displays, in display order.
val RECOVERY_MUSCLE_GROUPS = listOf(
    "Chest", "Back", "Shoulders", "Biceps", "Triceps", "Quads", "Hamstrings", "Glutes", "Calves", "Core"
)

/**
 * Maps a free-text ExerciseEntity.bodyPart (seed data uses combos like "Chest & Lats" and
 * qualifiers like "Upper Chest"/"Front Deltoids") onto the canonical muscle list above.
 * "Cardio" and "Full Body" intentionally map to nothing - they aren't muscle-specific enough
 * to attribute fatigue to a single group without guessing.
 */
private fun muscleGroupsForBodyPart(bodyPart: String): List<String> {
    val parts = bodyPart.split("&").map { it.trim() }
    return parts.mapNotNull { part ->
        when {
            part.contains("Chest", ignoreCase = true) -> "Chest"
            part.equals("Back", ignoreCase = true) ||
                part.equals("Lats", ignoreCase = true) ||
                part.contains("Traps", ignoreCase = true) -> "Back"
            part.contains("Deltoid", ignoreCase = true) ||
                part.equals("Shoulders", ignoreCase = true) -> "Shoulders"
            part.contains("Biceps", ignoreCase = true) ||
                part.contains("Brachialis", ignoreCase = true) -> "Biceps"
            part.contains("Triceps", ignoreCase = true) -> "Triceps"
            part.contains("Quads", ignoreCase = true) -> "Quads"
            part.contains("Hamstrings", ignoreCase = true) -> "Hamstrings"
            part.contains("Glutes", ignoreCase = true) -> "Glutes"
            part.contains("Calves", ignoreCase = true) -> "Calves"
            part.equals("Core", ignoreCase = true) -> "Core"
            else -> null // "Legs" (too ambiguous), "Cardio", "Full Body", or unrecognized
        }
    }.distinct()
}

class WorkoutViewModel(
    application: Application,
    private val repository: WorkoutRepository
) : AndroidViewModel(application) {

    val soundPlayer = SoundPlayer(application)

    val isExerciseScreenOpen = MutableStateFlow(false)

    // Active Workout State
    val activeWorkout = MutableStateFlow<WorkoutEntity?>(null)
    val isWorkoutStarted = MutableStateFlow(false)
    val isTimerPaused = MutableStateFlow(false)
    val workoutDuration = MutableStateFlow(0L)
    val activeExerciseStates = MutableStateFlow<List<ActiveExerciseState>>(emptyList())

    // User Profile Flow
    private val _userProfile = MutableStateFlow<UserProfileEntity?>(null)
    val userProfile: StateFlow<UserProfileEntity?> = _userProfile.asStateFlow()
    val isProfileLoaded = MutableStateFlow(false)

    // Displayed/Animated Dial Values
    val displayedIntensity = MutableStateFlow(0.0)
    val displayedCalories = MutableStateFlow(0.0)



    fun onExerciseScreenOpened() {
        isExerciseScreenOpen.value = true
    }

    // Floating Numbers State
    val floatingNumbers = MutableStateFlow<List<FloatingNumber>>(emptyList())
    private val newlyCompletedSetIds = mutableSetOf<Long>()
    val recordBrokenCelebration = MutableStateFlow<RecordBrokenState?>(null)

    // Caches the pre-session "old record" text per (workoutId:exerciseId) the first time a PR
    // is broken during this exercise's session, so that breaking the record again with an even
    // better set later in the same session ratchets the celebration's "new record" up against
    // the original baseline, instead of chaining off the intermediate PR and firing repeatedly.
    private val celebrationBaselineForExercise = mutableMapOf<String, String>()

    // Exercise completion order (exerciseId -> order index, lower = completed earlier)
    private val exerciseCompletionOrder = MutableStateFlow<Map<Int, Int>>(emptyMap())

    // Timers State
    private var timerJob: Job? = null
    private var cooldownJob: Job? = null
    val cooldownRemaining = MutableStateFlow(0) // seconds remaining
    val cooldownDuration = MutableStateFlow(0) // total cooldown time
    val cooldownExerciseName = MutableStateFlow<String?>(null)

    // Cardio/Hold Countdown Timer State
    val countdownRemaining = MutableStateFlow(0)
    val countdownDuration = MutableStateFlow(0)
    val countdownExerciseName = MutableStateFlow<String?>(null)
    val isCountdownActive = MutableStateFlow(false)
    val isCountdownPaused = MutableStateFlow(false)
    val countdownSetId = MutableStateFlow<Long?>(null)
    private var countdownJob: Job? = null

    // History and Selection State
    val allCompletedWorkouts = repository.getAllCompletedWorkouts().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val allExercises = repository.getAllExercises().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val exerciseUsageStats = MutableStateFlow<Map<Int, ExerciseUsageStat>>(emptyMap())

    // Per-exercise frequency tags (exerciseId -> Frequency). Absent entry = neutral.
    val exercisePreferences = repository.getAllPreferences()
        .map { list -> list.associate { it.exerciseId to com.example.workoutbuddy.data.Frequency.fromName(it.frequency) } }
        .map { it.filterValues { freq -> freq != null }.mapValues { (_, freq) -> freq!! } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Raw per-muscle-group fatigue rows (muscleGroup -> entity); the Body tab's Recovery view
    // computes the live, decayed percentage from these at render time.
    val muscleGroupRecovery = repository.getAllRecoveryFlow()
        .map { list -> list.associateBy { it.muscleGroup } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Body tab "Results" list: only exercises logged at least once, refreshed on demand
    // when the Body screen is opened rather than continuously observed.
    val exerciseResultSummaries = MutableStateFlow<List<ExerciseResultSummary>>(emptyList())

    fun refreshExerciseResultSummaries() {
        viewModelScope.launch {
            val loggedIds = repository.getLoggedExerciseIds()
            val summaries = loggedIds.mapNotNull { exerciseId ->
                val exercise = repository.getExerciseById(exerciseId) ?: return@mapNotNull null
                val sets = repository.getCompletedSetsForExerciseOrdered(exerciseId)
                if (sets.isEmpty()) return@mapNotNull null

                // Per-session "best" metric: weight*reps for lifts, distance for cardio,
                // time for hold/cardio-by-time, grouped in workout order (oldest first).
                fun metricFor(set: WorkoutSetEntity): Double = when (exercise.type) {
                    "LIFT" -> (set.weight ?: 0.0) * (set.reps ?: 0)
                    "CARDIO" -> set.distance ?: (set.time?.toDouble() ?: 0.0)
                    else -> set.time?.toDouble() ?: 0.0
                }
                val bestPerSession = sets.groupBy { it.workoutId }
                    .map { (_, sessionSets) -> sessionSets.maxOf { metricFor(it) } }

                val prLabel = when (exercise.type) {
                    "LIFT" -> {
                        val best = sets.maxWithOrNull(compareBy<WorkoutSetEntity> { it.weight ?: 0.0 }.thenBy { it.reps ?: 0 })
                        "${formatDecimal(best?.weight ?: 0.0)} kg x ${best?.reps ?: 0}"
                    }
                    "CARDIO" -> {
                        val best = sets.maxByOrNull { it.distance ?: 0.0 }
                        if ((best?.distance ?: 0.0) > 0.0) "${formatDecimal(best?.distance ?: 0.0)} km"
                        else formatTime(sets.maxOf { it.time ?: 0 })
                    }
                    else -> formatTime(sets.maxOf { it.time ?: 0 })
                }

                val recent = bestPerSession.takeLast(3)
                val trend = if (recent.size < 2) {
                    ExerciseTrend.FLAT
                } else if (recent.last() > recent.first()) {
                    ExerciseTrend.UP
                } else if (recent.last() < recent.first()) {
                    ExerciseTrend.DOWN
                } else {
                    ExerciseTrend.FLAT
                }

                ExerciseResultSummary(exerciseId, exercise.name, prLabel, trend)
            }
            exerciseResultSummaries.value = summaries
        }
    }

    private fun refreshExerciseUsageStats() {
        viewModelScope.launch {
            exerciseUsageStats.value = repository.getExerciseUsageStats().associateBy { it.exerciseId }
        }
    }
    val selectedDate = MutableStateFlow<Long?>(null) // date timestamp
    val workoutsForSelectedDate = MutableStateFlow<List<WorkoutEntity>>(emptyList())
    val selectedWorkoutDetail = MutableStateFlow<WorkoutDetailState?>(null)

    // Completion Dialog Summary
    val workoutSummary = MutableStateFlow<WorkoutSummaryState?>(null)

    init {
        viewModelScope.launch {
            repository.getUserProfileFlow().collect { profile ->
                _userProfile.value = profile
                isProfileLoaded.value = true
            }
        }
        viewModelScope.launch {
            repository.seedDatabaseIfEmpty()
            loadOrGenerateActiveWorkout()
        }
        refreshExerciseUsageStats()
        
        // Listen for selected date changes to update history list
        viewModelScope.launch {
            selectedDate.collect { timestamp ->
                updateWorkoutsForSelectedDate(timestamp)
            }
        }
    }

    // --- Active Workout Logic ---

    fun loadOrGenerateActiveWorkout(forceCategory: String? = null) {
        viewModelScope.launch {
            var draft = repository.getActiveWorkoutDraft()
            
            // If user forces a category, delete current draft and start fresh
            if (forceCategory != null && draft != null && draft.category != forceCategory) {
                repository.deleteWorkout(draft.id)
                draft = null
            }

            if (draft != null) {
                activeWorkout.value = draft
                displayedIntensity.value = draft.intensityScore
                displayedCalories.value = draft.totalCalories
                workoutDuration.value = draft.durationInSeconds
                isTimerPaused.value = false
                isWorkoutStarted.value = draft.isStarted
                if (draft.isStarted) {
                    startWorkoutTimer()
                }
                loadExerciseStatesForWorkout(draft.id)
            } else {
                // Generate next workout
                val nextCategory = forceCategory ?: determineNextCategory()
                val profile = repository.getUserProfile()
                val startingStrength = profile?.strengthScore ?: 100.0
                val startingStamina = profile?.staminaScore ?: 100.0
                val newDraft = repository.createActiveWorkoutDraft(nextCategory, startingStrength, startingStamina)
                generateExercisesForWorkout(newDraft.id, nextCategory)
                
                activeWorkout.value = newDraft
                displayedIntensity.value = newDraft.intensityScore
                displayedCalories.value = newDraft.totalCalories
                workoutDuration.value = 0L
                isWorkoutStarted.value = false
                isTimerPaused.value = false
                loadExerciseStatesForWorkout(newDraft.id)
            }
        }
    }

    private suspend fun determineNextCategory(): String {
        val last5 = repository.getLast5CompletedWorkouts()
        if (last5.isEmpty()) return "PUSH"
        
        return when (last5.first().category) {
            "PUSH" -> "PULL"
            "PULL" -> "LOWER_BODY"
            "LOWER_BODY" -> "PUSH"
            else -> "PUSH"
        }
    }

    private suspend fun generateExercisesForWorkout(workoutId: Long, category: String) {
        // Fetch user profile for adaptive weight recommendations, and to know what equipment
        // is available so generation never picks an exercise the user has no way to do.
        val profile = repository.getUserProfile()
        val strengthScore = profile?.strengthScore ?: 100.0
        val staminaScore = profile?.staminaScore ?: 100.0
        val bodyWeight = profile?.weight ?: 75.0
        val ownedEquipment = com.example.workoutbuddy.data.Equipment.parseCsv(
            profile?.equipmentOwned ?: com.example.workoutbuddy.data.Equipment.allIdsCsv
        )
        // Ceiling is set at profile creation (proxied from gym experience) or by migration, so
        // this should always resolve - MEDIUM is just a defensive fallback if it's ever unset.
        val difficultyCeiling = com.example.workoutbuddy.data.Difficulty.fromName(profile?.difficultyCeiling)
            ?: com.example.workoutbuddy.data.Difficulty.MEDIUM
        val preferences = repository.getAllPreferencesOnce()
            .associate { it.exerciseId to com.example.workoutbuddy.data.Frequency.fromName(it.frequency) }
            .filterValues { it != null }
            .mapValues { it.value!! }

        // Selection (gates + Always/Often/Less/Never weighting, applied identically to the
        // main category pool and the cardio pool) lives in selectWorkoutExercises - kept as a
        // pure function so it's unit-testable without Room/coroutines.
        val categoryExercises = repository.getExercisesByCategory(category).filter { it.type != "CARDIO" }
        val allExs = repository.getAllExercises().filter { it.isNotEmpty() }.first()
        val cardioExercises = allExs.filter { it.type == "CARDIO" }

        val exerciseIdsToUse = selectWorkoutExercises(
            categoryPool = categoryExercises,
            cardioPool = cardioExercises,
            ownedEquipment = ownedEquipment,
            difficultyCeiling = difficultyCeiling,
            preferences = preferences,
            count = MAIN_EXERCISES_PER_WORKOUT,
            maxPerBodyPart = MAX_EXERCISES_PER_BODY_PART
        ).toMutableList()

        val setsToInsert = mutableListOf<WorkoutSetEntity>()
        for (exerciseId in exerciseIdsToUse) {
            val exercise = repository.getExerciseById(exerciseId) ?: continue
            val prevSets = repository.getPreviousWorkoutSetsForExercise(exerciseId)
            val bestWeight = repository.getBestWeightForExercise(exerciseId)
            val bestTime = repository.getBestTimeForExercise(exerciseId)
            val bestDistance = repository.getBestDistanceForExercise(exerciseId)

            val targetSets = if (exercise.type == "CARDIO") 1 else 3
            for (setNum in 1..targetSets) {
                var recWeight: Double? = null
                var recReps: Int? = null
                var recTime: Int? = null
                var recDistance: Double? = null

                when (exercise.type) {
                    "LIFT" -> {
                        // Progress from the most recently logged set, not the all-time best,
                        // so the recommendation compounds week over week instead of reverting
                        // to an old peak.
                        val lastSet = prevSets.maxWithOrNull(
                            compareBy<WorkoutSetEntity> { it.weight ?: 0.0 }.thenBy { it.reps ?: 0 }
                        )
                        if (lastSet?.weight != null && lastSet.reps != null) {
                            val (nextWeight, nextReps) = progressLift(lastSet.weight, lastSet.reps)
                            recWeight = nextWeight
                            recReps = nextReps
                        } else {
                            recWeight = getAdaptiveStartWeight(exercise.name, strengthScore, bodyWeight)
                            recReps = 10
                        }
                    }
                    "CARDIO" -> {
                        val lastSet = prevSets.firstOrNull()
                        if (lastSet != null) {
                            val (nextDistance, nextTime) = progressCardio(lastSet.distance, lastSet.time ?: 0)
                            recTime = nextTime
                            recDistance = if (exercise.name.contains("Jump Rope", ignoreCase = true)) null else nextDistance
                        } else {
                            recTime = getStandardStartDuration(exercise.name, staminaScore)
                            recDistance = if (exercise.name.contains("Jump Rope", ignoreCase = true)) null else getStandardStartDistance(exercise.name, staminaScore)
                        }
                    }
                    "HOLD" -> {
                        recTime = prevSets.firstOrNull()?.time ?: 60
                    }
                }

                setsToInsert.add(
                    WorkoutSetEntity(
                        workoutId = workoutId,
                        exerciseId = exerciseId,
                        setNumber = setNum,
                        recommendedWeight = recWeight,
                        recommendedReps = recReps,
                        recommendedTime = recTime,
                        recommendedDistance = recDistance
                    )
                )
            }
        }
        repository.insertWorkoutSets(setsToInsert)
    }

    /**
     * Steps a lift exercise's recommendation forward from its most recently logged set:
     * reps increase by 1 each session up to a cap of 12, then the weight steps up by one
     * plate increment (2.5kg) and reps reset to a fixed starting value (8).
     */
    private fun progressLift(lastWeight: Double, lastReps: Int): Pair<Double, Int> {
        return if (lastReps < LIFT_REP_CAP) {
            lastWeight to (lastReps + 1)
        } else {
            nextWeightIncrement(lastWeight) to LIFT_REPS_RESET
        }
    }

    private fun nextWeightIncrement(weight: Double): Double = weight + 2.5

    /**
     * Steps a cardio exercise's recommendation forward by a flat 5% increase to both
     * distance and time, which holds pace constant since both scale by the same factor.
     */
    private fun progressCardio(lastDistance: Double?, lastTime: Int): Pair<Double?, Int> {
        val nextDistance = lastDistance?.let { it * CARDIO_PROGRESSION_FACTOR }
        val nextTime = Math.round(lastTime * CARDIO_PROGRESSION_FACTOR).toInt()
        return nextDistance to nextTime
    }

    /**
     * Returns an adaptive starting weight based on the user's strength score and body weight.
     * Used only when no PR exists for the exercise. Scales proportionally so stronger
     * users get heavier defaults without the original hardcoded values.
     *
     * Rationale fractions (of body weight) are calibrated to be an easy starting point:
     *   Squat 0.75x, Deadlift 0.40x, Bench 0.40x, OHP 0.15x, Row 0.30x, Dumbbell 0.15x
     * These are then blended with the strength score (S) as a secondary scaling factor
     * so that a user with a higher S gets nudged toward heavier weights.
     */
    private fun getAdaptiveStartWeight(name: String, strengthScore: Double, bodyWeight: Double): Double {
        // Strength score modifier: S=100 is baseline, scales linearly
        val sFactor = (strengthScore / 100.0).coerceIn(0.5, 3.0)
        val raw = when {
            // Bodyweight/unloaded exercises must be checked first - matched before the
            // "Squat" check below so "Bodyweight Squats" doesn't pick up a barbell ratio.
            name.contains("Bodyweight", ignoreCase = true) ||
                name.contains("Push-ups", ignoreCase = true) ||
                name.contains("Dips", ignoreCase = true) ||
                name.contains("Pull-ups", ignoreCase = true) ||
                name.contains("Chin-ups", ignoreCase = true) ||
                name.contains("Pike Push-ups", ignoreCase = true) ||
                name.contains("Inverted Row", ignoreCase = true)  -> 0.0  // bodyweight exercises
            // Matches "Barbell Back Squat" (the actual seeded name) without also matching
            // Front/Goblet/Hack Squat, which intentionally use the generic ratio below.
            name.contains("Barbell", ignoreCase = true) && name.contains("Squat", ignoreCase = true) -> bodyWeight * 0.75
            name.contains("Deadlift", ignoreCase = true)          -> bodyWeight * 0.40
            name.contains("Barbell Bench Press", ignoreCase = true) -> bodyWeight * 0.40
            name.contains("Barbell Overhead Press", ignoreCase = true) -> bodyWeight * 0.15
            name.contains("Barbell Row", ignoreCase = true)       -> bodyWeight * 0.30
            name.contains("Dumbbell", ignoreCase = true)          -> bodyWeight * 0.15
            else -> bodyWeight * 0.10
        }
        // Round to nearest 2.5kg plate increment
        val scaled = raw * sFactor
        return (Math.round(scaled / 2.5) * 2.5).coerceAtLeast(0.0)
    }

    /**
     * Default starting duration, driven by stamina score so the user always has a
     * sensible amount of time to complete their recommended distance.
     * - Distance-tracked cardio (Running/Walking/Cycling/Elliptical/etc.): derived from
     *   the recommended distance at an easy default pace, so the time actually matches
     *   the distance instead of being picked independently.
     * - Pure time-based cardio (Jump Rope): anchored directly off stamina, so a stamina
     *   score of 50 maps to a 1-minute default.
     */
    private fun getStandardStartDuration(name: String, staminaScore: Double): Int {
        if (name.contains("Jump Rope", ignoreCase = true)) {
            val staminaFactor = staminaScore / 50.0
            val seconds = staminaFactor * 60.0
            return (Math.round(seconds / 15.0) * 15).toInt().coerceAtLeast(15)
        }
        val distanceKm = getStandardStartDistance(name, staminaScore)
        val paceKmh = when {
            name.contains("Running", ignoreCase = true) -> 8.0
            name.contains("Walking", ignoreCase = true) -> 5.0
            name.contains("Cycling", ignoreCase = true) -> 15.0
            name.contains("Elliptical", ignoreCase = true) -> 7.0
            name.contains("Rowing", ignoreCase = true) -> 12.0
            name.contains("Stair Climber", ignoreCase = true) -> 4.0
            name.contains("Battle Ropes", ignoreCase = true) -> 3.0
            else -> 5.0
        }
        val seconds = (distanceKm / paceKmh) * 3600.0
        return (Math.round(seconds / 30.0) * 30).toInt().coerceAtLeast(60)
    }

    /**
     * Default starting distance, driven entirely by stamina score. Anchored so a
     * stamina score of 50 maps to a 1km running default; other cardio exercises are
     * scaled relative to running. Always rounded to the nearest 0.5km.
     */
    private fun getStandardStartDistance(name: String, staminaScore: Double): Double {
        val staminaFactor = staminaScore / 50.0
        val relativeFactor = when {
            name.contains("Running", ignoreCase = true) -> 1.0
            name.contains("Walking", ignoreCase = true) -> 0.8
            name.contains("Cycling", ignoreCase = true) -> 2.0
            name.contains("Elliptical", ignoreCase = true) -> 1.2
            name.contains("Rowing", ignoreCase = true) -> 2.5
            name.contains("Stair Climber", ignoreCase = true) -> 0.6
            name.contains("Battle Ropes", ignoreCase = true) -> 0.5
            else -> 0.4
        }
        val raw = staminaFactor * relativeFactor
        return (Math.round(raw / 0.5) * 0.5).coerceAtLeast(0.5)
    }

    private suspend fun loadExerciseStatesForWorkout(workoutId: Long) {
        val sets = repository.getSetsForWorkout(workoutId)
        val exerciseIds = sets.map { it.exerciseId }.distinct()
        
        val states = mutableListOf<ActiveExerciseState>()
        for (exerciseId in exerciseIds) {
            val exercise = repository.getExerciseById(exerciseId) ?: continue
            val exerciseSets = sets.filter { it.exerciseId == exerciseId }
            
            // Best lift representation
            val bestSet = repository.getBestSetForExercise(exerciseId)
            val bestDistanceSet = repository.getBestDistanceSetForExercise(exerciseId)
            val bestTimeSet = repository.getBestTimeSetForExercise(exerciseId)
            
            val bestText = when (exercise.type) {
                "LIFT" -> {
                    if (bestSet != null) {
                        val reps = bestSet.reps ?: 0
                        val weight = bestSet.weight ?: 0.0
                        "BEST ${reps}x${formatDecimal(weight)}kg"
                    } else {
                        "BEST None"
                    }
                }
                "CARDIO" -> {
                    if (exercise.name.contains("Jump Rope", ignoreCase = true)) {
                        if (bestTimeSet != null) {
                            "BEST ${formatTime(bestTimeSet.time ?: 0)}"
                        } else {
                            "BEST None"
                        }
                    } else {
                        if (bestDistanceSet != null) {
                            val dist = bestDistanceSet.distance ?: 0.0
                            val incl = (bestDistanceSet.inclinePct ?: 0.0).toInt()
                            "BEST ${formatDecimal(dist)}km at $incl%"
                        } else {
                            "BEST None"
                        }
                    }
                }
                "HOLD" -> {
                    if (bestTimeSet != null) {
                        "BEST ${formatTime(bestTimeSet.time ?: 0)}"
                    } else {
                        "BEST None"
                    }
                }
                else -> ""
            }

            // Previous lift representation
            val prevSets = repository.getPreviousWorkoutSetsForExercise(exerciseId)
            val lastCompletedSet = prevSets.filter { it.isCompleted }.maxByOrNull { 
                if (exercise.type == "LIFT") it.weight ?: 0.0 
                else if (exercise.type == "CARDIO") it.distance ?: 0.0 
                else (it.time ?: 0).toDouble()
            } ?: prevSets.firstOrNull()

            val prevText = if (lastCompletedSet != null) {
                when (exercise.type) {
                    "LIFT" -> {
                        val reps = lastCompletedSet.reps ?: lastCompletedSet.recommendedReps ?: 0
                        val weight = lastCompletedSet.weight ?: lastCompletedSet.recommendedWeight ?: 0.0
                        "LAST ${reps}x${formatDecimal(weight)}kg"
                    }
                    "CARDIO" -> {
                        val time = lastCompletedSet.time ?: lastCompletedSet.recommendedTime ?: 0
                        val dist = lastCompletedSet.distance ?: lastCompletedSet.recommendedDistance ?: 0.0
                        val incl = (lastCompletedSet.inclinePct ?: 0.0).toInt()
                        if (exercise.name.contains("Jump Rope", ignoreCase = true)) {
                            "LAST ${formatTime(time)}"
                        } else {
                            "LAST ${formatDecimal(dist)}km at $incl%"
                        }
                    }
                    "HOLD" -> {
                        val time = lastCompletedSet.time ?: lastCompletedSet.recommendedTime ?: 0
                        "LAST ${formatTime(time)}"
                    }
                    else -> ""
                }
            } else {
                "LAST None"
            }

            states.add(
                ActiveExerciseState(
                    exercise = exercise,
                    sets = exerciseSets,
                    bestLiftText = bestText,
                    prevLiftText = prevText
                )
            )
        }

        // Sort: completed exercises (in completion order) first, then uncompleted in original order
        val completionOrder = exerciseCompletionOrder.value
        val sorted = states.sortedWith(Comparator { a, b ->
            val aCompleted = a.sets.isNotEmpty() && a.sets.all { it.isCompleted }
            val bCompleted = b.sets.isNotEmpty() && b.sets.all { it.isCompleted }
            when {
                aCompleted && bCompleted -> {
                    val aOrder = completionOrder[a.exercise.id] ?: Int.MAX_VALUE
                    val bOrder = completionOrder[b.exercise.id] ?: Int.MAX_VALUE
                    aOrder.compareTo(bOrder)
                }
                aCompleted -> -1
                bCompleted -> 1
                else -> 0
            }
        })
        activeExerciseStates.value = sorted
    }

    // --- Workout Ticker Timer ---

    fun startWorkout() {
        isWorkoutStarted.value = true
        isTimerPaused.value = false
        startWorkoutTimer()
        activeWorkout.value?.let {
            viewModelScope.launch {
                val updated = it.copy(isStarted = true)
                repository.updateWorkout(updated)
                activeWorkout.value = updated
            }
        }
    }

    fun toggleWorkoutTimer() {
        if (!isWorkoutStarted.value) return
        if (isTimerPaused.value) {
            isTimerPaused.value = false
            startWorkoutTimer()
        } else {
            isTimerPaused.value = true
            timerJob?.cancel()
            timerJob = null
        }
    }

    private fun startWorkoutTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                workoutDuration.value += 1
                // Periodically save duration in draft
                activeWorkout.value?.let {
                    repository.updateWorkout(it.copy(durationInSeconds = workoutDuration.value))
                }
            }
        }
    }

    // --- Logging & Set Updates ---

    fun updateSetValues(
        setId: Long,
        weight: Double?,
        reps: Int?,
        time: Int?,
        distance: Double?,
        inclinePct: Double? = null
    ) {
        viewModelScope.launch {
            val states = activeExerciseStates.value
            for (state in states) {
                val match = state.sets.find { it.id == setId }
                if (match != null) {
                    val updatedSet = match.copy(
                        weight = weight,
                        reps = reps,
                        time = time,
                        distance = distance,
                        inclinePct = inclinePct
                    )
                    repository.updateWorkoutSet(updatedSet)

                    // Cascade values to subsequent sets that haven't been completed yet —
                    // completed sets reflect what the user actually did and shouldn't be overwritten.
                    val setsToUpdate = state.sets.filter { s ->
                        s.setNumber > match.setNumber && !s.isCompleted
                    }
                    for (cascadeSet in setsToUpdate) {
                        repository.updateWorkoutSet(cascadeSet.copy(
                            weight = weight,
                            reps = reps,
                            time = time,
                            distance = distance,
                            inclinePct = inclinePct
                        ))
                    }

                    val activeId = activeWorkout.value?.id ?: return@launch
                    reevaluatePRsForExercise(activeId, match.exerciseId)
                    loadExerciseStatesForWorkout(activeId)
                    recalculateActiveWorkoutIntensity()
                    break
                }
            }
        }
    }

    fun toggleSetCompletion(
        setId: Long,
        isCompleted: Boolean,
        weight: Double? = null,
        reps: Int? = null,
        time: Int? = null,
        distance: Double? = null,
        inclinePct: Double? = null
    ) {
        viewModelScope.launch {
            val activeId = activeWorkout.value?.id ?: return@launch
            val states = activeExerciseStates.value
            
            for (state in states) {
                val match = state.sets.find { it.id == setId }
                if (match != null) {
                    if (isCompleted) {
                        newlyCompletedSetIds.add(setId)
                        soundPlayer.play(AppSound.TICK)
                        Haptics.tick(getApplication())
                    } else {
                        newlyCompletedSetIds.remove(setId)
                    }

                    var finalWeight = weight ?: match.weight
                    var finalReps = reps ?: match.reps
                    var finalTime = time ?: match.time
                    var finalDistance = distance ?: match.distance
                    val finalIncline = inclinePct ?: match.inclinePct

                    if (isCompleted) {
                        if (finalWeight == null) finalWeight = match.recommendedWeight
                        if (finalReps == null) finalReps = match.recommendedReps
                        if (finalTime == null) finalTime = match.recommendedTime
                        if (finalDistance == null) finalDistance = match.recommendedDistance
                    }

                    val updatedSet = match.copy(
                        isCompleted = isCompleted, 
                        weight = finalWeight,
                        reps = finalReps,
                        time = finalTime,
                        distance = finalDistance,
                        inclinePct = match.inclinePct
                    )
                    repository.updateWorkoutSet(updatedSet)

                    if (isCompleted) {
                        val subsequentSets = repository.getSetsForWorkout(activeId)
                            .filter { it.exerciseId == match.exerciseId && it.setNumber > match.setNumber && !it.isCompleted }
                        for (s in subsequentSets) {
                            var changed = false
                            var newRecWeight = s.recommendedWeight
                            var newWeight = s.weight
                            var newRecReps = s.recommendedReps
                            var newReps = s.reps
                            var newRecTime = s.recommendedTime
                            var newTime = s.time
                            var newRecDistance = s.recommendedDistance
                            var newDistance = s.distance

                            if (finalWeight != null && finalWeight > (s.weight ?: s.recommendedWeight ?: 0.0)) {
                                newRecWeight = finalWeight
                                newWeight = finalWeight
                                changed = true
                            }
                            if (finalReps != null && finalReps > (s.reps ?: s.recommendedReps ?: 0)) {
                                newRecReps = finalReps
                                newReps = finalReps
                                changed = true
                            }
                            if (finalTime != null && finalTime > (s.time ?: s.recommendedTime ?: 0)) {
                                newRecTime = finalTime
                                newTime = finalTime
                                changed = true
                            }
                            if (finalDistance != null && finalDistance > (s.distance ?: s.recommendedDistance ?: 0.0)) {
                                newRecDistance = finalDistance
                                newDistance = finalDistance
                                changed = true
                            }

                            if (changed) {
                                repository.updateWorkoutSet(s.copy(
                                    recommendedWeight = newRecWeight,
                                    weight = newWeight,
                                    recommendedReps = newRecReps,
                                    reps = newReps,
                                    recommendedTime = newRecTime,
                                    time = newTime,
                                    recommendedDistance = newRecDistance,
                                    distance = newDistance
                                ))
                            }
                        }
                    }

                    // Snapshot the pre-existing best set BEFORE reevaluation overwrites it,
                    // so we can measure the PR margin (how much this set beat the old best by).
                    val pastBestSetForScoring = if (state.exercise.type == "LIFT") {
                        repository.getBestSetForExercise(match.exerciseId)
                    } else null

                    reevaluatePRsForExercise(activeId, match.exerciseId)
                    if (isCompleted) {
                        applyFatigue(match, state.exercise)
                    }
                    loadExerciseStatesForWorkout(activeId)

                    var exerciseCompletedNow = false
                    var exerciseIntensityPoints = 0

                    // Track exercise completion order for sorting
                    if (isCompleted) {
                        val wasCompletedBefore = state.sets.isNotEmpty() && state.sets.all { it.isCompleted }
                        val setsAfterUpdate = repository.getSetsForWorkout(activeId)
                            .filter { it.exerciseId == state.exercise.id }
                        val allSetsCompleted = setsAfterUpdate.isNotEmpty() && setsAfterUpdate.all { it.isCompleted }
                        
                        if (allSetsCompleted) {
                            val currentOrder = exerciseCompletionOrder.value.toMutableMap()
                            if (!currentOrder.containsKey(state.exercise.id)) {
                                currentOrder[state.exercise.id] = currentOrder.size
                                exerciseCompletionOrder.value = currentOrder
                            }
                            
                            if (!wasCompletedBefore) {
                                exerciseCompletedNow = true
                                val profile = repository.getUserProfile()
                                val userBodyWeight = profile?.weight ?: 70.0
                                val totalExerciseSO = setsAfterUpdate.sumOf { set ->
                                    calculateSetPerformance(
                                        exerciseName = state.exercise.name,
                                        weight = set.weight,
                                        reps = set.reps,
                                        time = set.time,
                                        distance = set.distance,
                                        exerciseType = state.exercise.type,
                                        userBodyWeight = userBodyWeight,
                                        inclinePct = set.inclinePct
                                    )
                                }
                                exerciseIntensityPoints = Math.round(totalExerciseSO).toInt()
                            }
                        }

                        // Update global Strength Score and Stamina Score of user
                        val profile = repository.getUserProfile()
                        if (profile != null) {
                            val perf = calculateSetPerformance(
                                exerciseName = state.exercise.name,
                                weight = finalWeight,
                                reps = finalReps,
                                time = finalTime,
                                distance = finalDistance,
                                exerciseType = state.exercise.type,
                                userBodyWeight = profile.weight,
                                inclinePct = match.inclinePct
                            )
                            val oldStr = profile.strengthScore
                            val oldStamina = profile.staminaScore

                            // Cap regular (non-bonus) gains at 3 per workout — the bonus point
                            // for hitting high intensity is applied separately at completion.
                            val startingStrength = activeWorkout.value?.startingStrengthScore ?: oldStr
                            val startingStamina = activeWorkout.value?.startingStaminaScore ?: oldStamina
                            val maxStrengthThisWorkout = startingStrength + MAX_REGULAR_GAIN_PER_WORKOUT
                            val maxStaminaThisWorkout = startingStamina + MAX_REGULAR_GAIN_PER_WORKOUT

                            // Strength is a genuine reflection of lifting ability: it only moves
                            // when a LIFT set sets a new PR for that exercise, scaled by how much
                            // the new PR beats the previous best by. Non-PR lifts, cardio, and
                            // holds never touch strength.
                            val updatedSetIsPR = repository.getSetsForWorkout(activeId)
                                .firstOrNull { it.id == setId }?.isPR == true

                            val newStr = if (state.exercise.type == "LIFT" && updatedSetIsPR) {
                                val pastBestPerf = pastBestSetForScoring?.let { past ->
                                    calculateSetPerformance(
                                        exerciseName = state.exercise.name,
                                        weight = past.weight,
                                        reps = past.reps,
                                        time = past.time,
                                        distance = past.distance,
                                        exerciseType = state.exercise.type,
                                        userBodyWeight = profile.weight,
                                        inclinePct = past.inclinePct
                                    )
                                } ?: 0.0
                                val prMargin = (perf - pastBestPerf).coerceAtLeast(0.0)
                                val gain = (STRENGTH_PR_GAIN_FACTOR * prMargin).coerceAtMost(MAX_STRENGTH_GAIN_PER_PR)
                                (oldStr + gain).coerceAtMost(maxStrengthThisWorkout)
                            } else {
                                oldStr
                            }

                            // Stamina is earned through cardio and holds (both endurance-based);
                            // lift sets only nudge it negligibly.
                            val newStamina = (if (state.exercise.type == "CARDIO" || state.exercise.type == "HOLD") {
                                if (perf > oldStamina) oldStamina + 0.02 * (perf - oldStamina) + 1.0 else oldStamina + 1.0
                            } else {
                                if (perf > oldStamina) oldStamina + 0.001 * (perf - oldStamina) + 0.1 else oldStamina + 0.1
                            }).coerceAtMost(maxStaminaThisWorkout)

                            repository.saveUserProfile(
                                profile.copy(
                                    strengthScore = newStr.coerceIn(30.0, 999.0),
                                    staminaScore = newStamina.coerceIn(30.0, 999.0)
                                )
                            )
                        }
                    } else {
                        // Un-completing: remove from order map
                        val currentOrder = exerciseCompletionOrder.value.toMutableMap()
                        currentOrder.remove(state.exercise.id)
                        exerciseCompletionOrder.value = currentOrder
                    }

                    // Trigger Cooldown Timer if set is marked complete
                    if (isCompleted) {
                        triggerCooldown(state.exercise)
                    }
                    recalculateActiveWorkoutIntensity()
                    break
                }
            }
        }
    }

    private fun recalculateActiveWorkoutIntensity() {
        val activeId = activeWorkout.value?.id ?: return
        viewModelScope.launch {
            val profile = repository.getUserProfile()
            val userBodyWeight = profile?.weight ?: 70.0
            updateWorkoutIntensity(activeId, userBodyWeight)
        }
    }

    private suspend fun updateWorkoutIntensity(workoutId: Long, userBodyWeight: Double) {
        val workout = repository.getWorkoutById(workoutId) ?: return
        val sets = repository.getSetsForWorkout(workoutId)
        val exerciseIds = sets.map { it.exerciseId }.distinct()
        var totalIntensity = 0.0
        var totalCalories = 0.0
        for (exerciseId in exerciseIds) {
            val exercise = repository.getExerciseById(exerciseId) ?: continue
            val exerciseSets = sets.filter { it.exerciseId == exerciseId && it.isCompleted }
            for (set in exerciseSets) {
                val performance = calculateSetPerformance(
                    exerciseName = exercise.name,
                    weight = set.weight,
                    reps = set.reps,
                    time = set.time,
                    distance = set.distance,
                    exerciseType = exercise.type,
                    userBodyWeight = userBodyWeight,
                    inclinePct = set.inclinePct
                )
                totalIntensity += performance

                // Calorie calculation
                when (exercise.type) {
                    "LIFT" -> {
                        val w = set.weight ?: 0.0
                        val r = set.reps ?: 0
                        totalCalories += if (w == 0.0) {
                            (r * 0.2) + 3.0
                        } else {
                            (w * r * 0.05) + 3.0
                        }
                    }
                    "CARDIO" -> {
                        val durationMin = (set.time ?: 0) / 60.0
                        val distance = set.distance ?: 0.0
                        totalCalories += when {
                            exercise.name.contains("Running", ignoreCase = true) -> 75.0 * distance * (1.0 + (set.inclinePct ?: 0.0) * 0.10)
                            exercise.name.contains("Walking", ignoreCase = true) -> 40.0 * distance * (1.0 + (set.inclinePct ?: 0.0) * 0.12)
                            exercise.name.contains("Cycling", ignoreCase = true) -> 30.0 * distance * (1.0 + (set.inclinePct ?: 0.0) * 0.08)
                            exercise.name.contains("Elliptical", ignoreCase = true) -> 7.0 * durationMin
                            else -> exercise.calorieBurnRate * durationMin
                        }
                    }
                    "HOLD" -> {
                        val durationSec = set.time ?: 0
                        totalCalories += durationSec * 0.15
                    }
                }
            }
        }
        val updatedWorkout = workout.copy(
            intensityScore = totalIntensity,
            totalCalories = totalCalories
        )
        repository.updateWorkout(updatedWorkout)
        
        // Update activeWorkout flow if applicable
        activeWorkout.value?.let {
            if (it.id == workoutId) {
                activeWorkout.value = updatedWorkout
            }
        }

        if (!isExerciseScreenOpen.value) {
            displayedIntensity.value = totalIntensity
            displayedCalories.value = totalCalories
        }
    }

    // --- Cooldown Timer ---

    private fun startCooldownJob() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            while (cooldownRemaining.value > 0) {
                delay(1000)
                cooldownRemaining.value -= 1
                if (cooldownRemaining.value in 1..3) {
                    soundPlayer.play(AppSound.TICK)
                }
            }
            playBeep(isRestTimer = true)
            cooldownExerciseName.value = null
        }
    }

    private fun triggerCooldown(exercise: ExerciseEntity) {
        if (_userProfile.value?.restTimerEnabled == false) return
        val duration = when (exercise.impactLevel) {
            "HEAVY" -> 180 // 3 min
            "HIGH" -> 120 // 2 min
            "MEDIUM" -> 60 // 1 min
            "LOW" -> 30  // 30 sec
            else -> 60
        }
        
        cooldownExerciseName.value = exercise.name
        cooldownDuration.value = duration
        cooldownRemaining.value = duration
        startCooldownJob()
    }

    fun skipCooldown() {
        cooldownJob?.cancel()
        cooldownRemaining.value = 0
        cooldownExerciseName.value = null
    }

    // --- Cardio/Hold Countdown Timer ---

    private fun startCountdownJob() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (countdownRemaining.value > 0) {
                delay(1000)
                if (!isCountdownPaused.value) {
                    countdownRemaining.value -= 1
                    if (countdownRemaining.value in 1..3) {
                        soundPlayer.play(AppSound.TICK)
                    }
                }
            }
            onCountdownComplete()
        }
    }

    fun startCountdown(setId: Long, exerciseName: String, durationSeconds: Int) {
        countdownSetId.value = setId
        countdownExerciseName.value = exerciseName
        countdownDuration.value = durationSeconds
        countdownRemaining.value = durationSeconds
        isCountdownActive.value = true
        isCountdownPaused.value = false
        startCountdownJob()
    }

    private fun onCountdownComplete() {
        playBeep(isRestTimer = false)
        isCountdownActive.value = false
        countdownExerciseName.value = null
        val setId = countdownSetId.value
        countdownSetId.value = null
        isCountdownPaused.value = false
        
        if (setId != null) {
            toggleSetCompletion(setId, true)
        }
    }

    fun toggleCountdownPause() {
        isCountdownPaused.value = !isCountdownPaused.value
    }

    fun completeCountdownEarly() {
        val setId = countdownSetId.value
        val elapsed = countdownDuration.value - countdownRemaining.value
        
        countdownJob?.cancel()
        isCountdownActive.value = false
        countdownExerciseName.value = null
        countdownSetId.value = null
        isCountdownPaused.value = false
        
        if (setId != null && elapsed > 0) {
            viewModelScope.launch {
                val states = activeExerciseStates.value
                for (state in states) {
                    val match = state.sets.find { it.id == setId }
                    if (match != null) {
                        val updatedSet = match.copy(
                            time = elapsed,
                            isCompleted = true
                        )
                        repository.updateWorkoutSet(updatedSet)

                        val activeId = activeWorkout.value?.id ?: return@launch
                        reevaluatePRsForExercise(activeId, match.exerciseId)
                        applyFatigue(updatedSet, state.exercise)
                        loadExerciseStatesForWorkout(activeId)
                        recalculateActiveWorkoutIntensity()
                        triggerCooldown(state.exercise)
                        break
                    }
                }
            }
        }
    }

    // Stops every workout-related timer (rest/cooldown, exercise countdown, and the overall
    // workout duration ticker) so nothing keeps running in the background once a workout ends.
    private fun stopAllWorkoutTimers() {
        cooldownJob?.cancel()
        cooldownRemaining.value = 0
        cooldownExerciseName.value = null

        countdownJob?.cancel()
        isCountdownActive.value = false
        countdownExerciseName.value = null
        countdownSetId.value = null
        isCountdownPaused.value = false

        timerJob?.cancel()
    }

    fun cancelCountdown() {
        countdownJob?.cancel()
        isCountdownActive.value = false
        countdownExerciseName.value = null
        countdownSetId.value = null
        isCountdownPaused.value = false
    }

    // --- Audio Player ---

    private fun playBeep(isRestTimer: Boolean) {
        Haptics.success(getApplication())
        soundPlayer.play(if (isRestTimer) AppSound.REST_TIMER_END else AppSound.TIMER_END)
    }

    override fun onCleared() {
        super.onCleared()
        soundPlayer.release()
    }

    // --- Completing Workout ---

    fun completeWorkout() {
        stopAllWorkoutTimers()
        viewModelScope.launch {
            val workout = activeWorkout.value ?: return@launch
            val states = activeExerciseStates.value
            val allSets = states.flatMap { it.sets }
            val completedSets = allSets.filter { it.isCompleted }

            if (completedSets.isNotEmpty()) {
                soundPlayer.play(AppSound.SUCCESS_DING)
                Haptics.success(getApplication())
            }

            if (completedSets.isEmpty()) {
                // If nothing completed, just delete/discard draft
                repository.deleteWorkout(workout.id)
                activeWorkout.value = null
                isWorkoutStarted.value = false
                loadOrGenerateActiveWorkout()
                return@launch
            }

            // Calculations
            var totalCalories = 0.0
            var totalSteps = 0
            var prCount = 0
            var totalVolumeKg = 0.0

            for (state in states) {
                val exercise = state.exercise
                val exerciseSets = state.sets.filter { it.isCompleted }
                
                for (set in exerciseSets) {
                    if (set.isPR) prCount++

                    // Calorie calculations
                    when (exercise.type) {
                        "LIFT" -> {
                            val w = set.weight ?: 0.0
                            val r = set.reps ?: 0
                            totalVolumeKg += w * r
                            totalCalories += if (w == 0.0) {
                                (r * 0.2) + 3.0
                            } else {
                                (w * r * 0.05) + 3.0
                            }
                        }
                        "CARDIO" -> {
                            val durationMin = (set.time ?: 0) / 60.0
                            val distance = set.distance ?: 0.0
                            totalCalories += when {
                                exercise.name.contains("Running", ignoreCase = true) -> 75.0 * distance * (1.0 + (set.inclinePct ?: 0.0) * 0.10)
                                exercise.name.contains("Walking", ignoreCase = true)  -> 40.0 * distance * (1.0 + (set.inclinePct ?: 0.0) * 0.12)
                                exercise.name.contains("Cycling", ignoreCase = true)  -> 30.0 * distance * (1.0 + (set.inclinePct ?: 0.0) * 0.08)
                                exercise.name.contains("Elliptical", ignoreCase = true) -> 7.0 * durationMin
                                else -> exercise.calorieBurnRate * durationMin
                            }

                            // Steps calculations
                            totalSteps += when {
                                exercise.name.contains("Running", ignoreCase = true) -> (distance * 1250).toInt()
                                exercise.name.contains("Walking", ignoreCase = true) -> (distance * 1300).toInt()
                                exercise.name.contains("Elliptical", ignoreCase = true) -> ((set.time ?: 0) * 2.0).toInt()
                                else -> 0
                            }
                        }
                        "HOLD" -> {
                            val durationSec = set.time ?: 0
                            totalCalories += durationSec * 0.15 // 9 kcal per min
                        }
                    }
                }
            }
            // Stop timers
            timerJob?.cancel()
            cooldownJob?.cancel()
            cooldownExerciseName.value = null

            // Delete all uncompleted sets for this workout from the database
            allSets.filter { !it.isCompleted }.forEach {
                repository.deleteWorkoutSet(it.id)
            }

            // High-intensity bonus: +1 to the relevant stat if this workout hit "high" on the
            // performance dial (-> strength) or the calorie-burn dial (-> stamina), using the
            // same ratio-to-target and HIGH threshold as the dials shown on this screen.
            val totalSetsCount = allSets.size.coerceAtLeast(1)
            val targetPerformanceScore = workout.startingStrengthScore * totalSetsCount
            val targetCalories = totalSetsCount * 20.0 + 100.0
            val performanceRatio = if (targetPerformanceScore > 0.0) workout.intensityScore / targetPerformanceScore else 0.0
            val caloriesRatio = if (targetCalories > 0.0) totalCalories / targetCalories else 0.0
            val hitHighPerformance = performanceRatio >= HIGH_INTENSITY_RATIO_THRESHOLD
            val hitHighCalories = caloriesRatio >= HIGH_INTENSITY_RATIO_THRESHOLD

            var profileBeforeCompletion = repository.getUserProfile()
            if (profileBeforeCompletion != null && (hitHighPerformance || hitHighCalories)) {
                val bonusedProfile = profileBeforeCompletion.copy(
                    strengthScore = if (hitHighPerformance) (profileBeforeCompletion.strengthScore + HIGH_INTENSITY_BONUS).coerceIn(30.0, 999.0) else profileBeforeCompletion.strengthScore,
                    staminaScore = if (hitHighCalories) (profileBeforeCompletion.staminaScore + HIGH_INTENSITY_BONUS).coerceIn(30.0, 999.0) else profileBeforeCompletion.staminaScore
                )
                repository.saveUserProfile(bonusedProfile)
                profileBeforeCompletion = bonusedProfile
            }

            // Capture strength and stamina delta before/after (now includes the bonus, if any)
            val currentStrength = profileBeforeCompletion?.strengthScore ?: 0.0
            val currentStamina = profileBeforeCompletion?.staminaScore ?: 0.0
            val strengthDelta = (currentStrength - workout.startingStrengthScore).coerceAtLeast(0.0)
            val staminaDelta = (currentStamina - workout.startingStaminaScore).coerceAtLeast(0.0)

            val finalWorkout = workout.copy(
                isCompleted = true,
                totalCalories = totalCalories,
                totalSteps = totalSteps,
                prCount = prCount,
                durationInSeconds = workoutDuration.value,
                totalVolumeKg = totalVolumeKg,
                date = System.currentTimeMillis(), // save completion date
                strengthGain = strengthDelta,
                staminaGain = staminaDelta
            )
            repository.updateWorkout(finalWorkout)

            // Trigger summary overlay
            workoutSummary.value = WorkoutSummaryState(
                workoutId = finalWorkout.id,
                category = finalWorkout.category,
                totalCalories = totalCalories,
                totalSteps = totalSteps,
                prCount = prCount,
                durationInSeconds = workoutDuration.value,
                totalVolumeKg = totalVolumeKg,
                strengthScoreDelta = strengthDelta,
                staminaScoreDelta = staminaDelta
            )

            // Clear state and prep next
            activeWorkout.value = null
            isWorkoutStarted.value = false
            isTimerPaused.value = false
            workoutDuration.value = 0L
            activeExerciseStates.value = emptyList()
            exerciseCompletionOrder.value = emptyMap()

            loadOrGenerateActiveWorkout()
            refreshExerciseUsageStats()
        }
    }

    fun updateCompletedWorkoutDuration(workoutId: Long, durationSeconds: Long) {
        viewModelScope.launch {
            val workout = repository.getWorkoutById(workoutId)
            if (workout != null) {
                val updated = workout.copy(durationInSeconds = durationSeconds)
                repository.updateWorkout(updated)
                // Refresh summary state if currently displayed
                workoutSummary.value?.let { currentSummary ->
                    if (currentSummary.workoutId == workoutId) {
                        workoutSummary.value = currentSummary.copy(durationInSeconds = durationSeconds)
                    }
                }
            }
        }
    }

    fun dismissSummary() {
        workoutSummary.value = null
    }

    // --- Workout Log / History Logic ---

    private fun updateWorkoutsForSelectedDate(timestamp: Long?) {
        viewModelScope.launch {
            if (timestamp == null) {
                workoutsForSelectedDate.value = emptyList()
                return@launch
            }
            
            // Get range for start and end of that date
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = timestamp
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis
            
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endOfDay = calendar.timeInMillis
            
            val workouts = repository.getCompletedWorkoutsForDate(startOfDay, endOfDay)
            workoutsForSelectedDate.value = workouts
        }
    }

    fun loadWorkoutDetail(workoutId: Long) {
        viewModelScope.launch {
            val workout = repository.getAllCompletedWorkouts().first().find { it.id == workoutId } ?: return@launch
            val sets = repository.getSetsForWorkout(workoutId)
            
            val detailsList = mutableListOf<ExerciseDetailState>()
            val exerciseIds = sets.map { it.exerciseId }.distinct()
            for (exerciseId in exerciseIds) {
                val exercise = repository.getExerciseById(exerciseId) ?: continue
                val exerciseSets = sets.filter { it.exerciseId == exerciseId }
                detailsList.add(ExerciseDetailState(exercise, exerciseSets))
            }
            
            // Calculate muscle group impacts
            val muscleImpacts = mutableMapOf<String, Double>()
            for (detail in detailsList) {
                val exercise = detail.exercise
                val completedCount = detail.sets.filter { it.isCompleted }.size.toDouble()
                if (completedCount == 0.0) continue
                
                // Primary muscle group gets 1.0 points per set, secondary groups get fractional
                val bodyPart = exercise.bodyPart
                muscleImpacts[bodyPart] = (muscleImpacts[bodyPart] ?: 0.0) + (completedCount * 1.0)
                
                // Add secondary targets based on standard splits
                when (exercise.name) {
                    "Barbell Bench Press", "Dumbbell Bench Press" -> {
                        muscleImpacts["Triceps"] = (muscleImpacts["Triceps"] ?: 0.0) + (completedCount * 0.5)
                        muscleImpacts["Shoulders"] = (muscleImpacts["Shoulders"] ?: 0.0) + (completedCount * 0.3)
                    }
                    "Barbell Overhead Press", "Dumbbell Shoulder Press" -> {
                        muscleImpacts["Triceps"] = (muscleImpacts["Triceps"] ?: 0.0) + (completedCount * 0.4)
                        muscleImpacts["Core"] = (muscleImpacts["Core"] ?: 0.0) + (completedCount * 0.2)
                    }
                    "Barbell Deadlift" -> {
                        muscleImpacts["Legs"] = (muscleImpacts["Legs"] ?: 0.0) + (completedCount * 0.8) // hamstrings
                        muscleImpacts["Core"] = (muscleImpacts["Core"] ?: 0.0) + (completedCount * 0.3)
                    }
                    "Barbell Row", "Dumbbell Row" -> {
                        muscleImpacts["Arms"] = (muscleImpacts["Arms"] ?: 0.0) + (completedCount * 0.5) // biceps
                    }
                    "Pull-ups", "Chin-ups" -> {
                        muscleImpacts["Arms"] = (muscleImpacts["Arms"] ?: 0.0) + (completedCount * 0.6) // biceps
                    }
                    else -> {
                        // Catches all squat variants (the seeded name is "Barbell Back Squat",
                        // not "Barbell Squat" - exact-match would silently never fire).
                        if (exercise.name.contains("Squat", ignoreCase = true)) {
                            muscleImpacts["Core"] = (muscleImpacts["Core"] ?: 0.0) + (completedCount * 0.3)
                        }
                        if (exercise.name == "Running" || exercise.name == "Walking" || exercise.name == "Cycling") {
                            muscleImpacts["Cardio"] = (muscleImpacts["Cardio"] ?: 0.0) + (completedCount * 1.0)
                        }
                    }
                }
            }

            selectedWorkoutDetail.value = WorkoutDetailState(
                workout = workout,
                exerciseDetails = detailsList,
                muscleImpacts = muscleImpacts.toList().sortedByDescending { it.second }
            )
        }
    }

    fun closeWorkoutDetail() {
        selectedWorkoutDetail.value = null
    }

    fun deleteCompletedWorkout(workoutId: Long) {
        viewModelScope.launch {
            val workout = repository.getWorkoutById(workoutId)
            if (workout != null) {
                val profile = repository.getUserProfile()
                if (profile != null) {
                    val reversedStrength = (profile.strengthScore - workout.strengthGain).coerceAtLeast(30.0)
                    val reversedStamina = (profile.staminaScore - workout.staminaGain).coerceAtLeast(30.0)
                    repository.saveUserProfile(
                        profile.copy(
                            strengthScore = reversedStrength,
                            staminaScore = reversedStamina
                        )
                    )
                }
            }
            repository.deleteWorkoutSetsForWorkout(workoutId)
            repository.deleteWorkout(workoutId)
            selectedWorkoutDetail.value = null
            
            // Refresh list of completed workouts / selected date
            selectedDate.value?.let {
                updateWorkoutsForSelectedDate(it)
            }
        }
    }

    // --- Dynamic Workout Sets & Exercises ---

    fun addSetToExercise(exerciseId: Int) {
        viewModelScope.launch {
            val activeId = activeWorkout.value?.id ?: return@launch
            val sets = repository.getSetsForWorkout(activeId).filter { it.exerciseId == exerciseId }
            val nextSetNum = (sets.maxOfOrNull { it.setNumber } ?: 0) + 1
            
            // Try to copy recommendation from the last set, or use defaults
            val lastSet = sets.lastOrNull()
            val exercise = repository.getExerciseById(exerciseId) ?: return@launch
            val profile = repository.getUserProfile()

            val newSet = WorkoutSetEntity(
                workoutId = activeId,
                exerciseId = exerciseId,
                setNumber = nextSetNum,
                recommendedWeight = lastSet?.recommendedWeight ?: run {
                    val bestSet = repository.getBestSetForExercise(exerciseId)
                    bestSet?.weight
                        ?: getAdaptiveStartWeight(exercise.name, profile?.strengthScore ?: 100.0, profile?.weight ?: 75.0)
                },
                recommendedReps = lastSet?.recommendedReps ?: run {
                    val bestSet = repository.getBestSetForExercise(exerciseId)
                    bestSet?.reps ?: 10
                },
                recommendedTime = lastSet?.recommendedTime ?: (if (exercise.type == "CARDIO") getStandardStartDuration(exercise.name, profile?.staminaScore ?: 100.0) else if (exercise.type == "HOLD") 60 else null),
                recommendedDistance = lastSet?.recommendedDistance ?: (if (exercise.type == "CARDIO" && !exercise.name.contains("Jump Rope", ignoreCase = true)) getStandardStartDistance(exercise.name, profile?.staminaScore ?: 100.0) else null)
            )

            repository.insertWorkoutSet(newSet)
            loadExerciseStatesForWorkout(activeId)
        }
    }

    fun removeLastSetFromExercise(exerciseId: Int) {
        viewModelScope.launch {
            val activeId = activeWorkout.value?.id ?: return@launch
            val sets = repository.getSetsForWorkout(activeId).filter { it.exerciseId == exerciseId }
            if (sets.isEmpty()) return@launch
            
            // Find set with max setNumber
            val setToDelete = sets.maxByOrNull { it.setNumber } ?: return@launch
            repository.deleteWorkoutSet(setToDelete.id)
            
            // Re-order remaining sets just in case
            val remainingSets = repository.getSetsForWorkout(activeId)
                .filter { it.exerciseId == exerciseId }
                .sortedBy { it.setNumber }
            
            remainingSets.forEachIndexed { index, set ->
                val updated = set.copy(setNumber = index + 1)
                repository.updateWorkoutSet(updated)
            }
            
            loadExerciseStatesForWorkout(activeId)
        }
    }

    fun removeSetFromExercise(setId: Long, exerciseId: Int) {
        viewModelScope.launch {
            val activeId = activeWorkout.value?.id ?: return@launch
            repository.deleteWorkoutSet(setId)
            
            // Re-order remaining sets
            val remainingSets = repository.getSetsForWorkout(activeId)
                .filter { it.exerciseId == exerciseId }
                .sortedBy { it.setNumber }
            
            remainingSets.forEachIndexed { index, set ->
                val updated = set.copy(setNumber = index + 1)
                repository.updateWorkoutSet(updated)
            }
            
            loadExerciseStatesForWorkout(activeId)
        }
    }

    fun addExerciseToWorkout(exerciseId: Int) {
        viewModelScope.launch {
            val activeId = activeWorkout.value?.id ?: return@launch
            
            // Check if already exists in active workout
            val existingSets = repository.getSetsForWorkout(activeId)
            if (existingSets.any { it.exerciseId == exerciseId }) return@launch // Already exists
            
            val exercise = repository.getExerciseById(exerciseId) ?: return@launch
            val prevSets = repository.getPreviousWorkoutSetsForExercise(exerciseId)
            val bestWeight = repository.getBestWeightForExercise(exerciseId)
            
            val setsToInsert = mutableListOf<WorkoutSetEntity>()
            val targetSets = if (exercise.type == "CARDIO") 1 else 3

            var recWeight: Double? = null
            var recReps: Int? = null
            var recTime: Int? = null
            var recDistance: Double? = null

            if (exercise.type == "LIFT") {
                val p = repository.getUserProfile()
                val baseWeight = bestWeight ?: getAdaptiveStartWeight(exercise.name, p?.strengthScore ?: 100.0, p?.weight ?: 75.0)
                recWeight = baseWeight
                val pastWorkoutCount = repository.getCompletedWorkoutCountForExercise(exerciseId)
                if (pastWorkoutCount > 0) {
                    val lastCompletedSet = prevSets.lastOrNull { it.isCompleted && it.reps != null && it.weight != null } ?: prevSets.lastOrNull()
                    val lastWeight = lastCompletedSet?.weight ?: lastCompletedSet?.recommendedWeight ?: baseWeight
                    val lastReps = lastCompletedSet?.reps ?: lastCompletedSet?.recommendedReps ?: 8

                    if (pastWorkoutCount == 1) {
                        recWeight = lastWeight + 2.5
                        recReps = 8
                    } else {
                        if (lastReps >= 12) {
                            recWeight = lastWeight + 2.5
                            recReps = 8
                        } else {
                            recWeight = lastWeight
                            recReps = lastReps + 1
                        }
                    }
                } else {
                    recReps = 12
                }
            } else if (exercise.type == "CARDIO") {
                val p = repository.getUserProfile()
                recTime = prevSets.firstOrNull()?.time ?: getStandardStartDuration(exercise.name, p?.staminaScore ?: 100.0)
                recDistance = if (exercise.name.contains("Jump Rope", ignoreCase = true)) null else (prevSets.firstOrNull()?.distance ?: getStandardStartDistance(exercise.name, p?.staminaScore ?: 100.0))
            } else if (exercise.type == "HOLD") {
                recTime = prevSets.firstOrNull()?.time ?: 60
            }

            for (setNum in 1..targetSets) {
                setsToInsert.add(
                    WorkoutSetEntity(
                        workoutId = activeId,
                        exerciseId = exerciseId,
                        setNumber = setNum,
                        recommendedWeight = recWeight,
                        recommendedReps = recReps,
                        recommendedTime = recTime,
                        recommendedDistance = recDistance
                    )
                )
            }
            repository.insertWorkoutSets(setsToInsert)
            loadExerciseStatesForWorkout(activeId)
        }
    }

    fun removeExerciseFromWorkout(exerciseId: Int) {
        viewModelScope.launch {
            val activeId = activeWorkout.value?.id ?: return@launch
            val sets = repository.getSetsForWorkout(activeId).filter { it.exerciseId == exerciseId }
            sets.forEach { set ->
                repository.deleteWorkoutSet(set.id)
            }
            loadExerciseStatesForWorkout(activeId)
        }
    }

    fun replaceExerciseInWorkout(oldExerciseId: Int, newExerciseId: Int) {
        viewModelScope.launch {
            val activeId = activeWorkout.value?.id ?: return@launch
            val newExercise = repository.getExerciseById(newExerciseId) ?: return@launch
            
            // Check if new exercise is already in the workout (can't replace with one that's already there)
            val existingSets = repository.getSetsForWorkout(activeId)
            if (existingSets.any { it.exerciseId == newExerciseId }) return@launch
            
            val sets = existingSets.filter { it.exerciseId == oldExerciseId }
            if (sets.isEmpty()) return@launch
            
            // Delete old sets
            sets.forEach { repository.deleteWorkoutSet(it.id) }
            
            // Insert new sets with same size
            val prevSets = repository.getPreviousWorkoutSetsForExercise(newExerciseId)
            val bestWeight = repository.getBestWeightForExercise(newExerciseId)
            
            val setsToInsert = mutableListOf<WorkoutSetEntity>()
            sets.forEachIndexed { index, _ ->
                val setNum = index + 1
                var recWeight: Double? = null
                var recReps: Int? = null
                var recTime: Int? = null
                var recDistance: Double? = null
                
                when (newExercise.type) {
                    "LIFT" -> {
                        val bestSet = repository.getBestSetForExercise(newExerciseId)
                        if (bestSet != null) {
                            recWeight = bestSet.weight
                            recReps = bestSet.reps
                        } else {
                            val p = repository.getUserProfile()
                            recWeight = getAdaptiveStartWeight(newExercise.name, p?.strengthScore ?: 100.0, p?.weight ?: 75.0)
                            recReps = 10
                        }
                    }
                    "CARDIO" -> {
                        val p = repository.getUserProfile()
                        recTime = prevSets.firstOrNull()?.time ?: getStandardStartDuration(newExercise.name, p?.staminaScore ?: 100.0)
                        recDistance = if (newExercise.name.contains("Jump Rope", ignoreCase = true)) null else (prevSets.firstOrNull()?.distance ?: getStandardStartDistance(newExercise.name, p?.staminaScore ?: 100.0))
                    }
                    "HOLD" -> {
                        recTime = prevSets.firstOrNull()?.time ?: 60
                    }
                }
                
                setsToInsert.add(
                    WorkoutSetEntity(
                        workoutId = activeId,
                        exerciseId = newExerciseId,
                        setNumber = setNum,
                        recommendedWeight = recWeight,
                        recommendedReps = recReps,
                        recommendedTime = recTime,
                        recommendedDistance = recDistance
                    )
                )
            }
            
            repository.insertWorkoutSets(setsToInsert)
            loadExerciseStatesForWorkout(activeId)
        }
    }

    fun createAndAddExerciseToWorkout(
        name: String,
        type: String,
        bodyPart: String,
        impactLevel: String,
        category: String
    ) {
        viewModelScope.launch {
            val newExercise = ExerciseEntity(
                name = name,
                type = type,
                bodyPart = bodyPart,
                impactLevel = impactLevel,
                category = category,
                calorieBurnRate = when (type) {
                    "LIFT" -> 3.0
                    "CARDIO" -> 8.0
                    "HOLD" -> 4.0
                    else -> 5.0
                },
                description = "Custom exercise",
                howToSteps = "Perform exercise.",
                id = 0
            )
            val exerciseId = repository.insertExercise(newExercise).toInt()
            addExerciseToWorkout(exerciseId)
        }
    }

    // Single funnel for every "record broken" celebration regardless of exercise type. Only
    // shows the popup once per workout+exercise: if set 2 and set 3 both beat the historical
    // PR again, later calls just ratchet the celebrationBaselineForExercise entry's implicit
    // "already celebrated" status without re-popping the dialog or re-playing the chime
    // (sound/haptics are triggered from the UI when the dialog actually becomes visible, not
    // here, since the dialog itself is deferred until the user leaves the exercise screen).
    private fun celebrateRecordBroken(workoutId: Long, exerciseId: Int, exerciseName: String, freshOldRecord: String, newRecord: String) {
        val key = "$workoutId:$exerciseId"
        val alreadyCelebrated = key in celebrationBaselineForExercise
        val oldRecord = celebrationBaselineForExercise.getOrPut(key) { freshOldRecord }
        if (!alreadyCelebrated) {
            recordBrokenCelebration.value = RecordBrokenState(exerciseName, oldRecord, newRecord)
        }
    }

    /**
     * Bumps fatigue for every individual muscle a set's exercise.bodyPart maps to.
     * Decays existing fatigue by elapsed time, then adds a dynamic bump scaled by how hard
     * the set was relative to the user's strength (LIFT/HOLD) or stamina (CARDIO) score.
     * A set performed exactly at the user's score level adds BASE_FATIGUE_BUMP (25%).
     * The intensity multiplier is clamped to [FATIGUE_INTENSITY_MIN, FATIGUE_INTENSITY_MAX].
     */
    private suspend fun applyFatigue(set: WorkoutSetEntity, exercise: ExerciseEntity) {
        val now = System.currentTimeMillis()
        val profile = repository.getUserProfile()
        val relevantScore = if (exercise.type == "CARDIO") {
            profile?.staminaScore ?: 100.0
        } else {
            profile?.strengthScore ?: 100.0
        }
        val perf = calculateSetPerformance(
            exerciseName = exercise.name,
            weight = set.weight,
            reps = set.reps,
            time = set.time,
            distance = set.distance,
            exerciseType = exercise.type,
            userBodyWeight = profile?.weight ?: 70.0,
            inclinePct = set.inclinePct
        )
        val intensityMultiplier = (perf / relevantScore)
            .coerceIn(FATIGUE_INTENSITY_MIN, FATIGUE_INTENSITY_MAX)
        val fatigueBump = BASE_FATIGUE_BUMP * intensityMultiplier

        for (muscle in muscleGroupsForBodyPart(exercise.bodyPart)) {
            val existing = repository.getRecovery(muscle)
            val decayed = if (existing == null) {
                0.0
            } else {
                val elapsedDays = (now - existing.lastUpdatedAt) / MS_PER_DAY
                (existing.fatiguePct - RECOVERY_PCT_PER_DAY * elapsedDays).coerceAtLeast(0.0)
            }
            val newFatigue = (decayed + fatigueBump).coerceAtMost(100.0)
            repository.upsertRecovery(MuscleGroupRecoveryEntity(muscle, newFatigue, now))
        }
    }

    private suspend fun reevaluatePRsForExercise(workoutId: Long, exerciseId: Int) {
        val exercise = repository.getExerciseById(exerciseId) ?: return
        val sets = repository.getSetsForWorkout(workoutId).filter { it.exerciseId == exerciseId }
        val completedSets = sets.filter { it.isCompleted }
        
        when (exercise.type) {
            "LIFT" -> {
                val currentBestSet = completedSets.maxWithOrNull(compareBy<WorkoutSetEntity> { it.weight ?: 0.0 }.thenBy { it.reps ?: 0 })
                val pastBestSet = repository.getBestSetForExercise(exerciseId)
                val hasNewPR = if (currentBestSet == null || pastBestSet == null) {
                    // No prior history for this exercise: nothing to break yet.
                    false
                } else {
                    val currentWeight = currentBestSet.weight ?: 0.0
                    val pastWeight = pastBestSet.weight ?: 0.0
                    val currentReps = currentBestSet.reps ?: 0
                    val pastReps = pastBestSet.reps ?: 0
                    currentWeight > pastWeight || (currentWeight == pastWeight && currentReps > pastReps)
                }
                
                val isBrokenRecord = currentBestSet != null && pastBestSet != null && !currentBestSet.isPR && (
                    (currentBestSet.weight ?: 0.0) > (pastBestSet.weight ?: 0.0) ||
                    ((currentBestSet.weight ?: 0.0) == (pastBestSet.weight ?: 0.0) && (currentBestSet.reps ?: 0) > (pastBestSet.reps ?: 0))
                )
                if (isBrokenRecord && currentBestSet != null && pastBestSet != null) {
                    val freshOldRec = "${formatDecimal(pastBestSet.weight ?: 0.0)} kg x ${pastBestSet.reps ?: 0}"
                    val newRec = "${formatDecimal(currentBestSet.weight ?: 0.0)} kg x ${currentBestSet.reps ?: 0}"
                    celebrateRecordBroken(workoutId, exerciseId, exercise.name, freshOldRec, newRec)
                }

                val prSet = if (hasNewPR) currentBestSet else null
                sets.forEach { set ->
                    val shouldBePR = prSet != null && set.id == prSet.id
                    if (set.isPR != shouldBePR) {
                        repository.updateWorkoutSet(set.copy(isPR = shouldBePR))
                    }
                }
            }
            "CARDIO" -> {
                if (exercise.name.contains("Jump Rope", ignoreCase = true)) {
                    val maxTime = completedSets.mapNotNull { it.time }.maxOrNull() ?: 0
                    val pastBestTime = repository.getBestTimeForExercise(exerciseId) ?: 0
                    // No prior history (pastBestTime == 0) means nothing to break yet.
                    val hasNewPR = pastBestTime > 0 && maxTime > pastBestTime

                    val isBrokenRecord = hasNewPR
                    val firstPRSet = if (hasNewPR) completedSets.firstOrNull { it.time == maxTime } else null

                    if (isBrokenRecord && firstPRSet != null && !firstPRSet.isPR) {
                        val freshOldRec = formatTime(pastBestTime)
                        val newRec = formatTime(maxTime)
                        celebrateRecordBroken(workoutId, exerciseId, exercise.name, freshOldRec, newRec)
                    }

                    sets.forEach { set ->
                        val shouldBePR = firstPRSet != null && set.id == firstPRSet.id
                        if (set.isPR != shouldBePR) {
                            repository.updateWorkoutSet(set.copy(isPR = shouldBePR))
                        }
                    }
                } else {
                    // Tie-break order (distance DESC, incline DESC, time ASC i.e. faster wins)
                    // matches getBestDistanceSetForExercise's SQL ORDER BY exactly, so the
                    // "best" set picked here and the historical "best" fetched from the DB
                    // agree on what counts as better when distance+incline tie.
                    val currentBestSet = completedSets.maxWithOrNull(
                        compareBy<WorkoutSetEntity> { it.distance ?: 0.0 }
                            .thenBy { it.inclinePct ?: 0.0 }
                            .thenBy { -(it.time ?: Int.MAX_VALUE) }
                    )
                    val pastBestSet = repository.getBestDistanceSetForExercise(exerciseId)
                    val hasNewPR = if (currentBestSet == null || pastBestSet == null) {
                        // No prior history for this exercise: nothing to break yet.
                        false
                    } else {
                        val currentDist = currentBestSet.distance ?: 0.0
                        val pastDist = pastBestSet.distance ?: 0.0
                        val currentIncl = currentBestSet.inclinePct ?: 0.0
                        val pastIncl = pastBestSet.inclinePct ?: 0.0
                        val currentTime = currentBestSet.time ?: Int.MAX_VALUE
                        val pastTime = pastBestSet.time ?: Int.MAX_VALUE
                        currentDist > pastDist ||
                            (currentDist == pastDist && currentIncl > pastIncl) ||
                            (currentDist == pastDist && currentIncl == pastIncl && currentTime < pastTime)
                    }

                    val isBrokenRecord = currentBestSet != null && pastBestSet != null && !currentBestSet.isPR && hasNewPR
                    if (isBrokenRecord && currentBestSet != null && pastBestSet != null) {
                        val oldIncl = if ((pastBestSet.inclinePct ?: 0.0) > 0.0) " at ${formatDecimal(pastBestSet.inclinePct ?: 0.0)}%" else ""
                        val newIncl = if ((currentBestSet.inclinePct ?: 0.0) > 0.0) " at ${formatDecimal(currentBestSet.inclinePct ?: 0.0)}%" else ""
                        val freshOldRec = "${formatDecimal(pastBestSet.distance ?: 0.0)} km$oldIncl"
                        val newRec = "${formatDecimal(currentBestSet.distance ?: 0.0)} km$newIncl"
                        celebrateRecordBroken(workoutId, exerciseId, exercise.name, freshOldRec, newRec)
                    }

                    val prSet = if (hasNewPR) currentBestSet else null
                    sets.forEach { set ->
                        val shouldBePR = prSet != null && set.id == prSet.id
                        if (set.isPR != shouldBePR) {
                            repository.updateWorkoutSet(set.copy(isPR = shouldBePR))
                        }
                    }
                }
            }
            "HOLD" -> {
                val maxTime = completedSets.mapNotNull { it.time }.maxOrNull() ?: 0
                val pastBestTime = repository.getBestTimeForExercise(exerciseId) ?: 0
                // No prior history (pastBestTime == 0) means nothing to break yet.
                val hasNewPR = pastBestTime > 0 && maxTime > pastBestTime

                val isBrokenRecord = hasNewPR
                val firstPRSet = if (hasNewPR) completedSets.firstOrNull { it.time == maxTime } else null
                
                if (isBrokenRecord && firstPRSet != null && !firstPRSet.isPR) {
                    val freshOldRec = formatTime(pastBestTime)
                    val newRec = formatTime(maxTime)
                    celebrateRecordBroken(workoutId, exerciseId, exercise.name, freshOldRec, newRec)
                }

                sets.forEach { set ->
                    val shouldBePR = firstPRSet != null && set.id == firstPRSet.id
                    if (set.isPR != shouldBePR) {
                        repository.updateWorkoutSet(set.copy(isPR = shouldBePR))
                    }
                }
            }
        }
    }

    // --- Helpers ---

    private fun formatDecimal(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            String.format("%.1f", value)
        }
    }

    private fun formatTime(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) {
            String.format("%d:%02d:%02d", h, m, s)
        } else {
            String.format("%d:%02d", m, s)
        }
    }

    // --- User Profile & Strength Logic ---

    companion object {
        // Drives the "Level" label shown on the profile screen off the user's current
        // strength/stamina scores, independent of the gym-experience choice picked once at
        // onboarding (that label never moved as the user actually progressed). Ranked-tier
        // naming, decoupled from onboarding's Beginner/Intermediate/Expert terms. Bands widen
        // at higher tiers since climbing from Master to Legend should take meaningfully longer
        // than Bronze to Silver.
        // Computes a muscle group's current recovery % (0 = fully fatigued, 100 = fully
        // recovered) on read, decaying the stored fatiguePct by RECOVERY_PCT_PER_DAY for
        // elapsed time since lastUpdatedAt. No entity (never trained) means fully recovered.
        fun currentRecoveryPct(entity: MuscleGroupRecoveryEntity?, now: Long = System.currentTimeMillis()): Double {
            if (entity == null) return 100.0
            val elapsedDays = (now - entity.lastUpdatedAt) / MS_PER_DAY
            val decayedFatigue = (entity.fatiguePct - RECOVERY_PCT_PER_DAY * elapsedDays).coerceIn(0.0, 100.0)
            return 100.0 - decayedFatigue
        }

        fun deriveRankTier(strengthScore: Double, staminaScore: Double): String {
            val avg = (strengthScore + staminaScore) / 2.0
            return when {
                avg < 60.0 -> "Bronze"
                avg < 80.0 -> "Silver"
                avg < 100.0 -> "Gold"
                avg < 125.0 -> "Platinum"
                avg < 160.0 -> "Diamond"
                avg < 200.0 -> "Master"
                avg < 260.0 -> "Grandmaster"
                else -> "Legend"
            }
        }

        // Badge icon shown next to the nickname for the current rank tier.
        fun rankBadgeRes(tier: String): Int = when (tier) {
            "Bronze" -> R.drawable.badge_bronze
            "Silver" -> R.drawable.badge_silver
            "Gold" -> R.drawable.badge_gold
            "Platinum" -> R.drawable.badge_platinum
            "Diamond" -> R.drawable.badge_diamond
            "Master" -> R.drawable.badge_master
            "Grandmaster" -> R.drawable.badge_grandmaster
            else -> R.drawable.badge_legend
        }

        // Strength declines more gradually with age than stamina does.
        private fun ageMultStrengthFor(age: Int): Double = when {
            age < 18 -> 0.8
            age in 18..35 -> 1.0
            else -> (1.0 - (age - 35) * 0.01).coerceAtLeast(0.6)
        }

        // Stamina (cardiovascular capacity) declines faster with age than raw strength.
        private fun ageMultStaminaFor(age: Int): Double = when {
            age < 18 -> 0.85
            age in 18..35 -> 1.0
            else -> (1.0 - (age - 35) * 0.015).coerceAtLeast(0.5)
        }

        private fun heightMultFor(height: Double): Double = when {
            height > 180.0 -> 1.05
            height < 160.0 -> 0.95
            else -> 1.0
        }

        // Secondary modifier for strength: heavier bodies tend to carry more raw
        // strength capacity, lighter bodies slightly less.
        private fun weightMultStrengthFor(weight: Double): Double = when {
            weight > 90.0 -> 1.05
            weight < 60.0 -> 0.95
            else -> 1.0
        }

        private fun gymMultFor(gymExperience: String): Double = when (gymExperience) {
            "Beginner" -> 0.7
            "Intermediate" -> 1.0
            "Expert" -> 1.3
            else -> 0.7
        }

        /**
         * Single source of truth for the onboarding strength estimate. Used both when
         * actually saving the profile and for the onboarding screen's live preview, so
         * the two can never drift apart.
         *
         * Strength is anchored on height rather than weight (taller frames imply more
         * leverage/limb length, which the app treats as the primary strength driver),
         * with weight as a secondary modifier and its own age curve.
         */
        fun calculateInitialStrengthScore(age: Int, height: Double, weight: Double, gender: String, gymExperience: String): Double {
            val genderMult = when (gender) {
                "Male" -> 1.2
                "Female" -> 0.9
                else -> 1.05
            }
            // Scale constant calibrated so a ~175cm frame lands in the same ballpark
            // the old weight-based formula produced for an average bodyweight.
            val base = height * 0.45 * genderMult
            return (base * ageMultStrengthFor(age) * weightMultStrengthFor(weight) * gymMultFor(gymExperience)).coerceIn(30.0, 999.0)
        }

        /**
         * Single source of truth for the onboarding stamina estimate. Mirrors strength's
         * height/gym multipliers, but weight and gender are inverted: lighter bodies
         * and (on average) female physiology carry endurance advantages, whereas strength
         * rewards taller frames and male physiology. Stamina also uses its own (steeper)
         * age decline curve, since cardiovascular capacity fades faster than raw strength.
         */
        fun calculateInitialStaminaScore(age: Int, height: Double, weight: Double, gender: String, gymExperience: String): Double {
            val weightFactorStamina = (70.0 / weight).coerceIn(0.7, 1.3)
            val genderStam = when (gender) {
                "Male" -> 0.95
                "Female" -> 1.05
                else -> 1.0
            }
            return (100.0 * weightFactorStamina * genderStam * ageMultStaminaFor(age) * heightMultFor(height) * gymMultFor(gymExperience)).coerceIn(30.0, 999.0)
        }
    }

    fun saveUserProfile(
        nickname: String,
        age: Int,
        height: Double,
        weight: Double,
        gender: String,
        gymExperience: String,
        equipmentOwned: Set<com.example.workoutbuddy.data.Equipment> = com.example.workoutbuddy.data.Equipment.entries.toSet()
    ) {
        viewModelScope.launch {
            val initialStrength = calculateInitialStrengthScore(age, height, weight, gender, gymExperience)
            val initialStamina = calculateInitialStaminaScore(age, height, weight, gender, gymExperience)

            val profile = UserProfileEntity(
                nickname = nickname,
                age = age,
                height = height,
                weight = weight,
                gender = gender,
                strengthScore = initialStrength,
                staminaScore = initialStamina,
                gymExperience = gymExperience,
                equipmentOwned = equipmentOwned.joinToString(",") { it.id },
                // Onboarding doesn't ask for a difficulty ceiling directly anymore - it's
                // proxied from gym experience so new users start at a sensible default and can
                // still change it later from Profile settings.
                difficultyCeiling = com.example.workoutbuddy.data.difficultyFromGymExperience(gymExperience).name
            )
            repository.saveUserProfile(profile)

            // Update active draft workout if exists to match user starting scores & regenerate recommended sets
            val draft = activeWorkout.value
            if (draft != null && !draft.isCompleted) {
                val updatedDraft = draft.copy(
                    startingStrengthScore = initialStrength,
                    startingStaminaScore = profile.staminaScore
                )
                repository.updateWorkout(updatedDraft)
                activeWorkout.value = updatedDraft
                
                repository.deleteWorkoutSetsForWorkout(draft.id)
                generateExercisesForWorkout(draft.id, draft.category)
                loadExerciseStatesForWorkout(draft.id)
            }
        }
    }

    // Tags (or clears, when freq == null) an exercise's frequency preference. Applies silently/
    // instantly to future generations - no animation, unlike the one-time first-launch overlay.
    fun setExerciseFrequency(exerciseId: Int, freq: com.example.workoutbuddy.data.Frequency?) {
        viewModelScope.launch {
            if (freq == null) {
                repository.deletePreference(exerciseId)
            } else {
                repository.upsertPreference(ExercisePreferenceEntity(exerciseId, freq.name))
            }

            if (freq == com.example.workoutbuddy.data.Frequency.NEVER) {
                replaceIfInActiveWorkout(exerciseId)
            }
        }
    }

    // When an exercise is tagged Never while it's part of the current draft/active workout, swap
    // it out immediately for a random eligible exercise sharing the same bodyPart - same gates
    // (equipment, difficulty ceiling, Never) as normal generation, just a single random pick
    // instead of the full weighted selection. If no candidate qualifies, the exercise is simply
    // left in place rather than leaving the workout short a slot.
    private suspend fun replaceIfInActiveWorkout(exerciseId: Int) {
        val activeId = activeWorkout.value?.id ?: return
        val existingSets = repository.getSetsForWorkout(activeId)
        if (existingSets.none { it.exerciseId == exerciseId }) return

        val oldExercise = repository.getExerciseById(exerciseId) ?: return
        val profile = repository.getUserProfile()
        val ownedEquipment = com.example.workoutbuddy.data.Equipment.parseCsv(
            profile?.equipmentOwned ?: com.example.workoutbuddy.data.Equipment.allIdsCsv
        )
        val difficultyCeiling = com.example.workoutbuddy.data.Difficulty.fromName(profile?.difficultyCeiling)
            ?: com.example.workoutbuddy.data.Difficulty.MEDIUM
        val preferences = repository.getAllPreferencesOnce()
            .mapNotNull { pref -> com.example.workoutbuddy.data.Frequency.fromName(pref.frequency)?.let { pref.exerciseId to it } }
            .toMap()
        val existingExerciseIds = existingSets.map { it.exerciseId }.toSet()

        val candidates = repository.getAllExercises().first()
            .filter { it.bodyPart == oldExercise.bodyPart && it.id !in existingExerciseIds }
            .filter { isExerciseAvailable(it, ownedEquipment) }
            .filter { (com.example.workoutbuddy.data.Difficulty.fromName(it.difficulty) ?: com.example.workoutbuddy.data.Difficulty.MEDIUM).tier <= difficultyCeiling.tier }
            .filter { preferences[it.id] != com.example.workoutbuddy.data.Frequency.NEVER }

        val replacement = candidates.randomOrNull() ?: return
        replaceExerciseInWorkout(exerciseId, replacement.id)
    }

    // Changes the difficulty ceiling from Profile settings. Regenerates the active draft so it
    // reflects the new ceiling immediately rather than waiting for the next workout.
    fun setDifficultyCeiling(difficulty: com.example.workoutbuddy.data.Difficulty) {
        viewModelScope.launch {
            val profile = repository.getUserProfile() ?: return@launch
            repository.saveUserProfile(profile.copy(difficultyCeiling = difficulty.name))
            _userProfile.value = repository.getUserProfile()
            regenerateActiveDraftIfSafe()
        }
    }

    // Re-rolls exercise selection for the current draft workout - used whenever a setting that
    // feeds into selection (equipment, difficulty) changes, and by the manual shuffle action.
    // Skipped if the draft is already started or completed, since wiping its sets at that point
    // would discard logged progress rather than just reshuffling an unstarted plan.
    private suspend fun regenerateActiveDraftIfSafe() {
        val draft = activeWorkout.value ?: return
        if (draft.isCompleted || draft.isStarted) return
        repository.deleteWorkoutSetsForWorkout(draft.id)
        generateExercisesForWorkout(draft.id, draft.category)
        loadExerciseStatesForWorkout(draft.id)
    }

    // Manual "shuffle" action next to the workout type selector - re-rolls the current draft's
    // exercises without changing any setting.
    fun shuffleActiveWorkout() {
        viewModelScope.launch {
            regenerateActiveDraftIfSafe()
        }
    }

    fun updateUserProfile(nickname: String, age: Int, height: Double, weight: Double, gender: String) {
        viewModelScope.launch {
            val currentProfile = repository.getUserProfile() ?: return@launch

            // Delegate to the same formulas used at onboarding (and its preview) so this
            // never drifts out of sync with them. Gym experience doesn't change here, so
            // it's held fixed on both sides of the delta — only the edited fields move it.
            val oldBaselineStrength = calculateInitialStrengthScore(currentProfile.age, currentProfile.height, currentProfile.weight, currentProfile.gender, currentProfile.gymExperience)
            val newBaselineStrength = calculateInitialStrengthScore(age, height, weight, gender, currentProfile.gymExperience)
            val newStrength = (currentProfile.strengthScore + (newBaselineStrength - oldBaselineStrength)).coerceIn(30.0, 999.0)

            val oldBaselineStamina = calculateInitialStaminaScore(currentProfile.age, currentProfile.height, currentProfile.weight, currentProfile.gender, currentProfile.gymExperience)
            val newBaselineStamina = calculateInitialStaminaScore(age, height, weight, gender, currentProfile.gymExperience)
            val newStamina = (currentProfile.staminaScore + (newBaselineStamina - oldBaselineStamina)).coerceIn(30.0, 999.0)

            val updatedProfile = UserProfileEntity(
                id = currentProfile.id,
                nickname = nickname,
                age = age,
                height = height,
                weight = weight,
                gender = gender,
                strengthScore = newStrength,
                staminaScore = newStamina,
                gymExperience = currentProfile.gymExperience,
                restTimerEnabled = currentProfile.restTimerEnabled
            )
            repository.saveUserProfile(updatedProfile)
        }
    }

    fun setRestTimerEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val currentProfile = repository.getUserProfile() ?: return@launch
            val updatedProfile = currentProfile.copy(restTimerEnabled = enabled)
            repository.saveUserProfile(updatedProfile)
        }
    }

    fun markWorkoutTourSeen() {
        viewModelScope.launch {
            val currentProfile = repository.getUserProfile() ?: return@launch
            val updatedProfile = currentProfile.copy(hasSeenWorkoutTour = true)
            repository.saveUserProfile(updatedProfile)
        }
    }

    fun setEquipmentOwned(equipment: com.example.workoutbuddy.data.Equipment, owned: Boolean) {
        viewModelScope.launch {
            val currentProfile = repository.getUserProfile() ?: return@launch
            val current = com.example.workoutbuddy.data.Equipment.parseCsv(currentProfile.equipmentOwned).toMutableSet()
            if (owned) current.add(equipment) else current.remove(equipment)
            val updatedProfile = currentProfile.copy(equipmentOwned = current.joinToString(",") { it.id })
            repository.saveUserProfile(updatedProfile)
            regenerateActiveDraftIfSafe()
        }
    }

    fun setAllEquipmentOwned() {
        viewModelScope.launch {
            val currentProfile = repository.getUserProfile() ?: return@launch
            repository.saveUserProfile(currentProfile.copy(equipmentOwned = com.example.workoutbuddy.data.Equipment.allIdsCsv))
        }
    }

    // Bulk replace, used by "Select All" / "Deselect All" and applying a saved preset, rather
    // than toggling each piece of equipment individually.
    fun setEquipmentOwnedSet(equipment: Set<com.example.workoutbuddy.data.Equipment>) {
        viewModelScope.launch {
            val currentProfile = repository.getUserProfile() ?: return@launch
            repository.saveUserProfile(currentProfile.copy(equipmentOwned = equipment.joinToString(",") { it.id }))
            regenerateActiveDraftIfSafe()
        }
    }

    val equipmentPresets = repository.getAllEquipmentPresets().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // Snapshots the currently-owned equipment under a user-given name (e.g. "Home", "Gym") so
    // it can be re-applied in one tap later instead of re-toggling each piece by hand.
    fun saveEquipmentPreset(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val currentProfile = repository.getUserProfile() ?: return@launch
            repository.insertEquipmentPreset(
                EquipmentPresetEntity(name = name.trim(), equipmentCsv = currentProfile.equipmentOwned)
            )
        }
    }

    fun applyEquipmentPreset(preset: EquipmentPresetEntity) {
        setEquipmentOwnedSet(com.example.workoutbuddy.data.Equipment.parseCsv(preset.equipmentCsv))
    }

    fun deleteEquipmentPreset(id: Int) {
        viewModelScope.launch {
            repository.deleteEquipmentPreset(id)
        }
    }

    // --- Intensity Calculations ---

    fun calculateSetPerformance(
        exerciseName: String,
        weight: Double?,
        reps: Int?,
        time: Int?,
        distance: Double?,
        exerciseType: String,
        userBodyWeight: Double,
        inclinePct: Double? = null
    ): Double {
        val basePerf = when (exerciseType) {
            "LIFT" -> {
                val r = reps ?: 0
                val w = weight ?: 0.0
                val isBodyweight = w == 0.0
                val effectiveWeight = if (isBodyweight) userBodyWeight * 0.6 else w
                // Weighted lifts score 50% higher than before; bodyweight moves (w == 0) are
                // left untouched since those were already scoring fine.
                val weightedBoost = if (isBodyweight) 1.0 else 1.5
                effectiveWeight * (1.0 + r / 30.0) * weightedBoost
            }
            "CARDIO" -> {
                val tSec = time ?: 1
                if (exerciseName.contains("Jump Rope", ignoreCase = true)) {
                    // No distance is ever recorded for Jump Rope, so the distance/speed formula
                    // below would always score it 0 - score off duration instead.
                    tSec * 0.2
                } else if (exerciseName.contains("Battle Ropes", ignoreCase = true)) {
                    // Battle Ropes' "distance" is an artificial metric only used to derive a
                    // default duration (see getStandardStartDistance) - it's not a real
                    // distance, so the distance/speed formula below undersells how demanding
                    // this actually is. Score off duration instead, same as Jump Rope.
                    tSec * 0.03
                } else {
                    // Distance/speed driven, same formula now used for Stair Climber too
                    // (its old flat distance*0.1 was disconnected from pace and scored near 0).
                    // Coefficients tuned so cardio lands in the same rough range as LIFT/HOLD
                    // (~15-110) rather than dwarfing them. Cycling/Rowing get an extra
                    // dampening factor since their higher cruising speed otherwise pushes them
                    // well above other cardio at the same perceived effort.
                    val dist = distance ?: 0.0
                    val speedKmh = if (tSec > 0) (dist / (tSec / 3600.0)).coerceAtMost(30.0) else 0.0
                    val inclineMult = 1.0 + ((inclinePct ?: 0.0) * 0.05)
                    val activityFactor = if (exerciseName.contains("Cycling", ignoreCase = true) ||
                        exerciseName.contains("Rowing", ignoreCase = true)) 0.5 else 1.0
                    (dist * 6.0 + speedKmh * 1.5) * inclineMult * activityFactor
                }
            }
            "HOLD" -> {
                val tSec = time ?: 0
                tSec * 0.6
            }
            else -> 0.0
        }
        return basePerf * PERFORMANCE_MULTIPLIER
    }

    fun triggerFloatingNumber(text: String, colorType: String = "purple") {
        val list = floatingNumbers.value.toMutableList()
        val id = System.currentTimeMillis() + (0..1000).random()
        list.add(FloatingNumber(id = id, text = text, colorType = colorType))
        floatingNumbers.value = list

        // Auto remove after 4 seconds (matches slowed animation)
        viewModelScope.launch {
            delay(4000)
            dismissFloatingNumber(id)
        }
    }

    fun dismissFloatingNumber(id: Long) {
        val list = floatingNumbers.value.toMutableList()
        list.removeAll { it.id == id }
        floatingNumbers.value = list
    }

    fun calculateSetCalories(set: WorkoutSetEntity, exercise: ExerciseEntity): Double {
        return when (exercise.type) {
            "LIFT" -> {
                val w = set.weight ?: 0.0
                val r = set.reps ?: 0
                if (w == 0.0) {
                    (r * 0.2) + 3.0
                } else {
                    (w * r * 0.05) + 3.0
                }
            }
            "CARDIO" -> {
                val durationMin = (set.time ?: 0) / 60.0
                val distance = set.distance ?: 0.0
                when {
                    exercise.name.contains("Running", ignoreCase = true) -> 75.0 * distance * (1.0 + (set.inclinePct ?: 0.0) * 0.10)
                    exercise.name.contains("Walking", ignoreCase = true)  -> 40.0 * distance * (1.0 + (set.inclinePct ?: 0.0) * 0.12)
                    exercise.name.contains("Cycling", ignoreCase = true)  -> 30.0 * distance * (1.0 + (set.inclinePct ?: 0.0) * 0.08)
                    exercise.name.contains("Elliptical", ignoreCase = true) -> 7.0 * durationMin
                    else -> exercise.calorieBurnRate * durationMin
                }
            }
            "HOLD" -> {
                val durationSec = set.time ?: 0
                durationSec * 0.15
            }
            else -> 0.0
        }
    }

    fun onExerciseScreenClosed(exerciseId: Int) {
        isExerciseScreenOpen.value = false
        activeWorkout.value?.let {
            displayedIntensity.value = it.intensityScore
            displayedCalories.value = it.totalCalories
        }
        viewModelScope.launch {
            if (newlyCompletedSetIds.isEmpty()) return@launch
            val activeId = activeWorkout.value?.id ?: return@launch
            val sets = repository.getSetsForWorkout(activeId)
                .filter { it.exerciseId == exerciseId && it.id in newlyCompletedSetIds }
            val exercise = repository.getExerciseById(exerciseId) ?: return@launch
            val profile = repository.getUserProfile()
            val userBodyWeight = profile?.weight ?: 70.0

            var totalStrength = 0.0
            var totalCalories = 0.0

            for (set in sets) {
                totalStrength += calculateSetPerformance(
                    exerciseName = exercise.name,
                    weight = set.weight ?: set.recommendedWeight,
                    reps = set.reps ?: set.recommendedReps,
                    time = set.time ?: set.recommendedTime,
                    distance = set.distance ?: set.recommendedDistance,
                    exerciseType = exercise.type,
                    userBodyWeight = userBodyWeight,
                    inclinePct = set.inclinePct
                )
                totalCalories += calculateSetCalories(set, exercise)
            }

            newlyCompletedSetIds.clear()

            if (totalStrength > 0.0 || totalCalories > 0.0) {
                if (totalStrength > 0.0) {
                    triggerFloatingNumber("+${totalStrength.toInt()}", colorType = "red")
                }
                if (totalCalories > 0.0) {
                    delay(1000)
                    triggerFloatingNumber("+${totalCalories.toInt()}", colorType = "yellow")
                }
            }
        }
    }

    fun dismissRecordCelebration() {
        recordBrokenCelebration.value = null
    }

    // --- Background/Foreground Timer Management & Alarms ---
    private var backgroundTimestamp: Long = 0L

    fun onAppBackgrounded() {
        backgroundTimestamp = System.currentTimeMillis()
        
        // Cooldown timer active -> Schedule alarm
        if (cooldownRemaining.value > 0 && cooldownExerciseName.value != null) {
            scheduleNotification(
                title = "Rest Complete!",
                message = "Time for your next set of ${cooldownExerciseName.value}!",
                delaySeconds = cooldownRemaining.value,
                isRest = true
            )
        }
        
        // Countdown timer active and not paused -> Schedule alarm
        if (isCountdownActive.value && !isCountdownPaused.value && countdownRemaining.value > 0) {
            scheduleNotification(
                title = "Timer Complete!",
                message = "${countdownExerciseName.value ?: "Cardio"} set complete!",
                delaySeconds = countdownRemaining.value,
                isRest = false
            )
        }

        // Cancel running jobs to prevent duplicate ticking/drifts when backgrounded
        timerJob?.cancel()
        timerJob = null
        cooldownJob?.cancel()
        cooldownJob = null
        countdownJob?.cancel()
        countdownJob = null
    }

    fun onAppForegrounded() {
        if (backgroundTimestamp == 0L) return
        val elapsedMs = System.currentTimeMillis() - backgroundTimestamp
        val elapsedSeconds = elapsedMs / 1000
        backgroundTimestamp = 0L

        // Cancel alarms & remove visual notifications
        cancelNotification(isRest = true)
        cancelNotification(isRest = false)

        // Catch up workout duration
        if (isWorkoutStarted.value && !isTimerPaused.value) {
            workoutDuration.value += elapsedSeconds
            activeWorkout.value?.let {
                viewModelScope.launch {
                    repository.updateWorkout(it.copy(durationInSeconds = workoutDuration.value))
                }
            }
            startWorkoutTimer()
        }

        // Catch up cooldown timer
        if (cooldownRemaining.value > 0 && cooldownExerciseName.value != null) {
            val remaining = (cooldownRemaining.value - elapsedSeconds).toInt()
            if (remaining > 0) {
                cooldownRemaining.value = remaining
                startCooldownJob()
            } else {
                cooldownRemaining.value = 0
                cooldownExerciseName.value = null
                playBeep(isRestTimer = true)
            }
        }

        // Catch up countdown timer
        if (isCountdownActive.value && countdownRemaining.value > 0) {
            if (!isCountdownPaused.value) {
                val remaining = (countdownRemaining.value - elapsedSeconds).toInt()
                if (remaining > 0) {
                    countdownRemaining.value = remaining
                    startCountdownJob()
                } else {
                    countdownRemaining.value = 0
                    onCountdownComplete()
                }
            }
        }
    }

    private fun scheduleNotification(title: String, message: String, delaySeconds: Int, isRest: Boolean) {
        if (delaySeconds <= 0) return
        val context = getApplication<Application>()
        val intent = Intent(context, com.example.workoutbuddy.TimerExpiredReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("message", message)
            putExtra("isRest", isRest)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            if (isRest) 1001 else 1002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerTime = System.currentTimeMillis() + delaySeconds * 1000

        try {
            val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }
            if (canScheduleExact) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            }
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        }
    }

    private fun cancelNotification(isRest: Boolean) {
        val context = getApplication<Application>()
        val intent = Intent(context, com.example.workoutbuddy.TimerExpiredReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            if (isRest) 1001 else 1002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.cancel(if (isRest) 1001 else 1002)
    }
}

data class FloatingNumber(
    val id: Long,
    val text: String,
    val colorType: String = "purple",
    val timestamp: Long = System.currentTimeMillis()
)

data class RecordBrokenState(
    val exerciseName: String,
    val oldRecord: String,
    val newRecord: String
)

enum class ExerciseTrend { UP, FLAT, DOWN }

// Body tab "Results" row: an exercise the user has logged at least once, with its current
// PR and a trend across the last few sessions.
data class ExerciseResultSummary(
    val exerciseId: Int,
    val name: String,
    val prLabel: String,
    val trend: ExerciseTrend
)

// State classes

data class ActiveExerciseState(
    val exercise: ExerciseEntity,
    val sets: List<WorkoutSetEntity>,
    val bestLiftText: String,
    val prevLiftText: String
)

data class WorkoutSummaryState(
    val workoutId: Long,
    val category: String,
    val totalCalories: Double,
    val totalSteps: Int,
    val prCount: Int,
    val durationInSeconds: Long,
    val totalVolumeKg: Double = 0.0,
    val strengthScoreDelta: Double = 0.0,
    val staminaScoreDelta: Double = 0.0
)

data class ExerciseDetailState(
    val exercise: ExerciseEntity,
    val sets: List<WorkoutSetEntity>
)

data class WorkoutDetailState(
    val workout: WorkoutEntity,
    val exerciseDetails: List<ExerciseDetailState>,
    val muscleImpacts: List<Pair<String, Double>> // body part to load score
)
