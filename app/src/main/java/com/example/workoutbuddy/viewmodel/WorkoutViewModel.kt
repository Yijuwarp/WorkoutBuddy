package com.example.workoutbuddy.viewmodel

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.app.Application
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.workoutbuddy.R
import com.example.workoutbuddy.data.WorkoutRepository
import com.example.workoutbuddy.data.database.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.Calendar

private const val PERFORMANCE_MULTIPLIER = 1.5

class WorkoutViewModel(
    application: Application,
    private val repository: WorkoutRepository
) : AndroidViewModel(application) {

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
        // Look up last 5 workouts to see if we did this category
        val last5 = repository.getLast5CompletedWorkouts()
        val lastWorkoutOfSameCategory = last5.firstOrNull { it.category == category }

        val exerciseIdsToUse = mutableListOf<Int>()
        if (lastWorkoutOfSameCategory != null) {
            val pastSets = repository.getSetsForWorkout(lastWorkoutOfSameCategory.id)
            exerciseIdsToUse.addAll(pastSets.map { it.exerciseId }.distinct())
        }

        // Fallback: If no history for this category, load defaults
        if (exerciseIdsToUse.isEmpty()) {
            val categoryExercises = repository.getExercisesByCategory(category)
            val lifts = categoryExercises.filter { it.type == "LIFT" }.take(2)
            val others = categoryExercises.filter { it.type != "LIFT" }.take(1)
            exerciseIdsToUse.addAll(lifts.map { it.id })
            exerciseIdsToUse.addAll(others.map { it.id })
        }

        // Add a random cardio exercise to each workout at the end
        val allExs = repository.getAllExercises().filter { it.isNotEmpty() }.first()
        val cardioExercises = allExs.filter { it.type == "CARDIO" }
        if (cardioExercises.isNotEmpty()) {
            val randomCardio = cardioExercises.random()
            exerciseIdsToUse.remove(randomCardio.id)
            exerciseIdsToUse.add(randomCardio.id)
        }

        // Fetch user profile for adaptive weight recommendations
        val profile = repository.getUserProfile()
        val strengthScore = profile?.strengthScore ?: 100.0
        val bodyWeight = profile?.weight ?: 75.0

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
                        val bestSet = repository.getBestSetForExercise(exerciseId)
                        if (bestSet != null) {
                            recWeight = bestSet.weight
                            recReps = bestSet.reps
                        } else {
                            recWeight = getAdaptiveStartWeight(exercise.name, strengthScore, bodyWeight)
                            recReps = 10
                        }
                    }
                    "CARDIO" -> {
                        recTime = prevSets.firstOrNull()?.time ?: getStandardStartDuration(exercise.name)
                        recDistance = if (exercise.name.contains("Jump Rope", ignoreCase = true)) null else (prevSets.firstOrNull()?.distance ?: getStandardStartDistance(exercise.name))
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
     * Returns an adaptive starting weight based on the user's strength score and body weight.
     * Used only when no PR exists for the exercise. Scales proportionally so stronger
     * users get heavier defaults without the original hardcoded values.
     *
     * Rationale fractions (of body weight) are calibrated to typical beginner-intermediate ratios:
     *   Squat 0.75x, Deadlift 0.90x, Bench 0.55x, OHP 0.35x, Row 0.45x, Dumbbell 0.15x
     * These are then blended with the strength score (S) as a secondary scaling factor
     * so that a user with a higher S gets nudged toward heavier weights.
     */
    private fun getAdaptiveStartWeight(name: String, strengthScore: Double, bodyWeight: Double): Double {
        // Strength score modifier: S=100 is baseline, scales linearly
        val sFactor = (strengthScore / 100.0).coerceIn(0.5, 3.0)
        val raw = when {
            name.contains("Barbell Squat", ignoreCase = true)    -> bodyWeight * 0.75
            name.contains("Deadlift", ignoreCase = true)          -> bodyWeight * 0.90
            name.contains("Barbell Bench Press", ignoreCase = true) -> bodyWeight * 0.55
            name.contains("Barbell Overhead Press", ignoreCase = true) -> bodyWeight * 0.35
            name.contains("Barbell Row", ignoreCase = true)       -> bodyWeight * 0.45
            name.contains("Dumbbell", ignoreCase = true)          -> bodyWeight * 0.15
            name.contains("Push-ups", ignoreCase = true) ||
                name.contains("Dips", ignoreCase = true) ||
                name.contains("Pull-ups", ignoreCase = true) ||
                name.contains("Chin-ups", ignoreCase = true)      -> 0.0  // bodyweight exercises
            else -> bodyWeight * 0.20
        }
        // Round to nearest 2.5kg plate increment
        val scaled = raw * sFactor
        return (Math.round(scaled / 2.5) * 2.5).coerceAtLeast(0.0)
    }

    private fun getStandardStartDuration(name: String): Int {
        return when {
            name.contains("Running") -> 1200 // 20 min
            name.contains("Walking") -> 1800 // 30 min
            name.contains("Cycling") -> 1200 // 20 min
            name.contains("Elliptical") -> 1200 // 20 min
            else -> 600 // 10 min
        }
    }

    private fun getStandardStartDistance(name: String): Double {
        return when {
            name.contains("Running") -> 2.5
            name.contains("Walking") -> 2.0
            name.contains("Cycling") -> 5.0
            name.contains("Elliptical") -> 3.0
            else -> 1.0
        }
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

                    // Cascade values to all subsequent sets
                    val setsToUpdate = state.sets.filter { s ->
                        s.setNumber > match.setNumber
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
                    
                    reevaluatePRsForExercise(activeId, match.exerciseId)
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

                            val newStr = if (state.exercise.type == "LIFT") {
                                if (perf > oldStr) oldStr + 0.02 * (perf - oldStr) + 1.0 else oldStr + 1.0
                            } else {
                                val cardioStrGain = if (state.exercise.name.contains("Stair Climber", ignoreCase = true)) 0.01 else 0.05
                                oldStr + cardioStrGain
                            }

                            val newStamina = if (state.exercise.type == "CARDIO") {
                                if (perf > oldStamina) oldStamina + 0.02 * (perf - oldStamina) + 1.0 else oldStamina + 1.0
                            } else {
                                if (perf > oldStamina) oldStamina + 0.001 * (perf - oldStamina) + 0.1 else oldStamina + 0.1
                            }

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
            }
            playBeep(isRestTimer = true)
            cooldownExerciseName.value = null
        }
    }

    private fun triggerCooldown(exercise: ExerciseEntity) {
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
                        loadExerciseStatesForWorkout(activeId)
                        triggerCooldown(state.exercise)
                        break
                    }
                }
            }
        }
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
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100) // 100 is Max Volume
                if (isRestTimer) {
                    // Rest complete: cheerful rising "ta-da" (lower register)
                    toneGen.startTone(ToneGenerator.TONE_DTMF_1, 250)
                    delay(300) // 250ms play + 50ms gap
                    toneGen.startTone(ToneGenerator.TONE_DTMF_5, 600)
                } else {
                    // Exercise complete: cheerful rising "ta-da" (higher/brighter register)
                    toneGen.startTone(ToneGenerator.TONE_DTMF_5, 250)
                    delay(300) // 250ms play + 50ms gap
                    toneGen.startTone(ToneGenerator.TONE_DTMF_9, 600)
                }
                delay(700) // wait for second tone to complete
                toneGen.release()
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to system notification sound
                try {
                    val context = getApplication<Application>().applicationContext
                    val notification: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    val r = RingtoneManager.getRingtone(context, notification)
                    r.play()
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
    }

    // --- Completing Workout ---

    fun completeWorkout() {
        viewModelScope.launch {
            val workout = activeWorkout.value ?: return@launch
            val states = activeExerciseStates.value
            val allSets = states.flatMap { it.sets }
            val completedSets = allSets.filter { it.isCompleted }

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
                            val incMult = 1.0 + ((set.inclinePct ?: 0.0) * 0.10)
                            totalCalories += when {
                                exercise.name.contains("Running") -> 75.0 * distance * (1.0 + (set.inclinePct ?: 0.0) * 0.10)
                                exercise.name.contains("Walking")  -> 40.0 * distance * (1.0 + (set.inclinePct ?: 0.0) * 0.12)
                                exercise.name.contains("Cycling")  -> 30.0 * distance * (1.0 + (set.inclinePct ?: 0.0) * 0.08)
                                exercise.name.contains("Elliptical") -> 7.0 * durationMin
                                else -> exercise.calorieBurnRate * durationMin
                            }

                            // Steps calculations
                            totalSteps += when {
                                exercise.name.contains("Running") -> (distance * 1250).toInt()
                                exercise.name.contains("Walking") -> (distance * 1300).toInt()
                                exercise.name.contains("Elliptical") -> ((set.time ?: 0) * 2.0).toInt()
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

            // Capture strength and stamina delta before/after
            val profileBeforeCompletion = repository.getUserProfile()
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
                    "Barbell Overhead Press", "Dumbbell Overhead Press" -> {
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
                    "Barbell Squat", "Dumbbell Squat" -> {
                        muscleImpacts["Core"] = (muscleImpacts["Core"] ?: 0.0) + (completedCount * 0.3)
                    }
                    "Running", "Walking", "Cycling" -> {
                        muscleImpacts["Cardio"] = (muscleImpacts["Cardio"] ?: 0.0) + (completedCount * 1.0)
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
            
            val newSet = WorkoutSetEntity(
                workoutId = activeId,
                exerciseId = exerciseId,
                setNumber = nextSetNum,
                recommendedWeight = lastSet?.recommendedWeight ?: run {
                    val bestSet = repository.getBestSetForExercise(exerciseId)
                    val p = repository.getUserProfile()
                    bestSet?.weight
                        ?: getAdaptiveStartWeight(exercise.name, p?.strengthScore ?: 100.0, p?.weight ?: 75.0)
                },
                recommendedReps = lastSet?.recommendedReps ?: run {
                    val bestSet = repository.getBestSetForExercise(exerciseId)
                    bestSet?.reps ?: 10
                },
                recommendedTime = lastSet?.recommendedTime ?: (if (exercise.type == "CARDIO") getStandardStartDuration(exercise.name) else if (exercise.type == "HOLD") 60 else null),
                recommendedDistance = lastSet?.recommendedDistance ?: (if (exercise.type == "CARDIO" && !exercise.name.contains("Jump Rope", ignoreCase = true)) getStandardStartDistance(exercise.name) else null)
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
                recTime = prevSets.firstOrNull()?.time ?: getStandardStartDuration(exercise.name)
                recDistance = if (exercise.name.contains("Jump Rope", ignoreCase = true)) null else (prevSets.firstOrNull()?.distance ?: getStandardStartDistance(exercise.name))
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
                        recTime = prevSets.firstOrNull()?.time ?: getStandardStartDuration(newExercise.name)
                        recDistance = prevSets.firstOrNull()?.distance ?: getStandardStartDistance(newExercise.name)
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

    private suspend fun reevaluatePRsForExercise(workoutId: Long, exerciseId: Int) {
        val exercise = repository.getExerciseById(exerciseId) ?: return
        val sets = repository.getSetsForWorkout(workoutId).filter { it.exerciseId == exerciseId }
        val completedSets = sets.filter { it.isCompleted }
        
        when (exercise.type) {
            "LIFT" -> {
                val currentBestSet = completedSets.maxWithOrNull(compareBy<WorkoutSetEntity> { it.weight ?: 0.0 }.thenBy { it.reps ?: 0 })
                val pastBestSet = repository.getBestSetForExercise(exerciseId)
                val hasNewPR = if (currentBestSet == null) {
                    false
                } else if (pastBestSet == null) {
                    true
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
                    val oldRec = "${formatDecimal(pastBestSet.weight ?: 0.0)} kg x ${pastBestSet.reps ?: 0}"
                    val newRec = "${formatDecimal(currentBestSet.weight ?: 0.0)} kg x ${currentBestSet.reps ?: 0}"
                    recordBrokenCelebration.value = RecordBrokenState(exercise.name, oldRec, newRec)
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
                    val hasNewPR = maxTime > pastBestTime
                    
                    val isBrokenRecord = pastBestTime > 0 && maxTime > pastBestTime
                    val firstPRSet = if (hasNewPR) completedSets.firstOrNull { it.time == maxTime } else null
                    
                    if (isBrokenRecord && firstPRSet != null && !firstPRSet.isPR) {
                        val oldRec = formatTime(pastBestTime)
                        val newRec = formatTime(maxTime)
                        recordBrokenCelebration.value = RecordBrokenState(exercise.name, oldRec, newRec)
                    }

                    sets.forEach { set ->
                        val shouldBePR = firstPRSet != null && set.id == firstPRSet.id
                        if (set.isPR != shouldBePR) {
                            repository.updateWorkoutSet(set.copy(isPR = shouldBePR))
                        }
                    }
                } else {
                    val currentBestSet = completedSets.maxWithOrNull(
                        compareBy<WorkoutSetEntity> { it.distance ?: 0.0 }
                            .thenBy { it.inclinePct ?: 0.0 }
                    )
                    val pastBestSet = repository.getBestDistanceSetForExercise(exerciseId)
                    val hasNewPR = if (currentBestSet == null) {
                        false
                    } else if (pastBestSet == null) {
                        true
                    } else {
                        val currentDist = currentBestSet.distance ?: 0.0
                        val pastDist = pastBestSet.distance ?: 0.0
                        val currentIncl = currentBestSet.inclinePct ?: 0.0
                        val pastIncl = pastBestSet.inclinePct ?: 0.0
                        currentDist > pastDist || (currentDist == pastDist && currentIncl > pastIncl)
                    }
                    
                    val isBrokenRecord = currentBestSet != null && pastBestSet != null && !currentBestSet.isPR && (
                        (currentBestSet.distance ?: 0.0) > (pastBestSet.distance ?: 0.0) ||
                        ((currentBestSet.distance ?: 0.0) == (pastBestSet.distance ?: 0.0) && (currentBestSet.inclinePct ?: 0.0) > (pastBestSet.inclinePct ?: 0.0))
                    )
                    if (isBrokenRecord && currentBestSet != null && pastBestSet != null) {
                        val oldIncl = if ((pastBestSet.inclinePct ?: 0.0) > 0.0) " at ${formatDecimal(pastBestSet.inclinePct ?: 0.0)}%" else ""
                        val newIncl = if ((currentBestSet.inclinePct ?: 0.0) > 0.0) " at ${formatDecimal(currentBestSet.inclinePct ?: 0.0)}%" else ""
                        val oldRec = "${formatDecimal(pastBestSet.distance ?: 0.0)} km$oldIncl"
                        val newRec = "${formatDecimal(currentBestSet.distance ?: 0.0)} km$newIncl"
                        recordBrokenCelebration.value = RecordBrokenState(exercise.name, oldRec, newRec)
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
                val hasNewPR = maxTime > pastBestTime
                
                val isBrokenRecord = pastBestTime > 0 && maxTime > pastBestTime
                val firstPRSet = if (hasNewPR) completedSets.firstOrNull { it.time == maxTime } else null
                
                if (isBrokenRecord && firstPRSet != null && !firstPRSet.isPR) {
                    val oldRec = formatTime(pastBestTime)
                    val newRec = formatTime(maxTime)
                    recordBrokenCelebration.value = RecordBrokenState(exercise.name, oldRec, newRec)
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

    fun saveUserProfile(nickname: String, age: Int, height: Double, weight: Double, gender: String, gymExperience: String) {
        viewModelScope.launch {
            val base = when (gender) {
                "Male" -> weight * 1.2
                "Female" -> weight * 0.9
                else -> weight * 1.05
            }
            val ageMult = when {
                age < 18 -> 0.8
                age in 18..35 -> 1.0
                else -> (1.0 - (age - 35) * 0.01).coerceAtLeast(0.6)
            }
            val heightMult = when {
                height > 180.0 -> 1.05
                height < 160.0 -> 0.95
                else -> 1.0
            }
            val gymMult = when (gymExperience) {
                "Intermediate" -> 1.25
                "Expert" -> 1.5
                else -> 1.0
            }
            val initialStrength = (base * ageMult * heightMult * gymMult).coerceIn(30.0, 999.0)

            val profile = UserProfileEntity(
                nickname = nickname,
                age = age,
                height = height,
                weight = weight,
                gender = gender,
                strengthScore = initialStrength,
                gymExperience = gymExperience
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

    fun updateUserProfile(nickname: String, age: Int, height: Double, weight: Double, gender: String) {
        viewModelScope.launch {
            val currentProfile = repository.getUserProfile() ?: return@launch

            // Calculate old baseline estimation
            val oldBase = when (currentProfile.gender) {
                "Male" -> currentProfile.weight * 1.2
                "Female" -> currentProfile.weight * 0.9
                else -> currentProfile.weight * 1.05
            }
            val oldAgeMult = when {
                currentProfile.age < 18 -> 0.8
                currentProfile.age in 18..35 -> 1.0
                else -> (1.0 - (currentProfile.age - 35) * 0.01).coerceAtLeast(0.6)
            }
            val oldHeightMult = when {
                currentProfile.height > 180.0 -> 1.05
                currentProfile.height < 160.0 -> 0.95
                else -> 1.0
            }
            val oldBaseScore = oldBase * oldAgeMult * oldHeightMult

            // Calculate new baseline estimation
            val newBase = when (gender) {
                "Male" -> weight * 1.2
                "Female" -> weight * 0.9
                else -> weight * 1.05
            }
            val newAgeMult = when {
                age < 18 -> 0.8
                age in 18..35 -> 1.0
                else -> (1.0 - (age - 35) * 0.01).coerceAtLeast(0.6)
            }
            val newHeightMult = when {
                height > 180.0 -> 1.05
                height < 160.0 -> 0.95
                else -> 1.0
            }
            val newBaseScore = newBase * newAgeMult * newHeightMult

            // Calculate delta
            val delta = newBaseScore - oldBaseScore

            // Adjust strength score by delta
            val newStrength = (currentProfile.strengthScore + delta).coerceIn(30.0, 999.0)

            val updatedProfile = UserProfileEntity(
                id = currentProfile.id,
                nickname = nickname,
                age = age,
                height = height,
                weight = weight,
                gender = gender,
                strengthScore = newStrength,
                staminaScore = currentProfile.staminaScore,
                gymExperience = currentProfile.gymExperience
            )
            repository.saveUserProfile(updatedProfile)
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
                val effectiveWeight = if (w == 0.0) userBodyWeight * 0.6 else w
                effectiveWeight * (1.0 + r / 30.0)
            }
            "CARDIO" -> {
                val tSec = time ?: 1
                if (exerciseName.contains("Stair Climber", ignoreCase = true)) {
                    val dist = distance ?: 0.0
                    dist * 0.1
                } else {
                    val dist = distance ?: 0.0
                    val speedKmh = if (tSec > 0) (dist / (tSec / 3600.0)).coerceAtMost(30.0) else 0.0
                    val inclineMult = 1.0 + ((inclinePct ?: 0.0) * 0.05)
                    (dist * 15.0 + speedKmh * 3.0) * inclineMult
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
