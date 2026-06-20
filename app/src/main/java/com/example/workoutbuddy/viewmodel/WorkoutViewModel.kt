package com.example.workoutbuddy.viewmodel

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

class WorkoutViewModel(
    application: Application,
    private val repository: WorkoutRepository
) : AndroidViewModel(application) {

    // Active Workout State
    val activeWorkout = MutableStateFlow<WorkoutEntity?>(null)
    val isWorkoutStarted = MutableStateFlow(false)
    val isTimerPaused = MutableStateFlow(false)
    val workoutDuration = MutableStateFlow(0L)
    val activeExerciseStates = MutableStateFlow<List<ActiveExerciseState>>(emptyList())

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
                workoutDuration.value = draft.durationInSeconds
                isTimerPaused.value = false
                isWorkoutStarted.value = false
                loadExerciseStatesForWorkout(draft.id)
            } else {
                // Generate next workout
                val nextCategory = forceCategory ?: determineNextCategory()
                val newDraft = repository.createActiveWorkoutDraft(nextCategory)
                generateExercisesForWorkout(newDraft.id, nextCategory)
                
                activeWorkout.value = newDraft
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
            // Pick a compact subset: let's select up to 3 exercises of different types
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

        val setsToInsert = mutableListOf<WorkoutSetEntity>()
        for (exerciseId in exerciseIdsToUse) {
            val exercise = repository.getExerciseById(exerciseId) ?: continue
            val prevSets = repository.getPreviousWorkoutSetsForExercise(exerciseId)
            val bestWeight = repository.getBestWeightForExercise(exerciseId)
            val bestTime = repository.getBestTimeForExercise(exerciseId)
            val bestDistance = repository.getBestDistanceForExercise(exerciseId)

            val targetSets = 3
            for (setNum in 1..targetSets) {
                var recWeight: Double? = null
                var recReps: Int? = null
                var recTime: Int? = null
                var recDistance: Double? = null

                when (exercise.type) {
                    "LIFT" -> {
                        // Default weight is the best weight ever lifted
                        recWeight = bestWeight ?: getStandardStartWeight(exercise.name)
                        
                        // Progressive overload reps:
                        if (prevSets.isNotEmpty()) {
                            val lastSetReps = prevSets.lastOrNull()?.reps ?: 0
                            if (lastSetReps >= 12) {
                                // Increase weight and drop reps to 8
                                val increment = if (exercise.name.contains("Squat") || exercise.name.contains("Deadlift")) 5.0 else 2.5
                                recWeight = (recWeight ?: 0.0) + increment
                                recReps = 8
                            } else {
                                // Match weight, recommend 10 reps (or match last completed reps)
                                recReps = 10
                            }
                        } else {
                            recReps = 10
                        }
                    }
                    "CARDIO" -> {
                        recTime = prevSets.firstOrNull()?.time ?: getStandardStartDuration(exercise.name)
                        recDistance = prevSets.firstOrNull()?.distance ?: getStandardStartDistance(exercise.name)
                    }
                    "HOLD" -> {
                        recTime = prevSets.firstOrNull()?.time ?: 60 // 60 seconds
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

    private fun getStandardStartWeight(name: String): Double {
        return when {
            name.contains("Barbell Squat") -> 40.0
            name.contains("Barbell Deadlift") -> 50.0
            name.contains("Barbell Bench Press") -> 40.0
            name.contains("Barbell Overhead Press") -> 30.0
            name.contains("Barbell Row") -> 30.0
            name.contains("Dumbbell") -> 10.0
            name.contains("Push-ups") || name.contains("Dips") || name.contains("Pull-ups") || name.contains("Chin-ups") -> 0.0
            else -> 10.0
        }
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
                        "BEST ${bestSet.reps} rep ${formatDecimal(bestSet.weight ?: 0.0)} weight"
                    } else {
                        "BEST None"
                    }
                }
                "CARDIO" -> {
                    if (bestDistanceSet != null) {
                        "BEST ${formatTime(bestDistanceSet.time ?: 0)} (${formatDecimal(bestDistanceSet.distance ?: 0.0)}km)"
                    } else {
                        "BEST None"
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
                        "LAST $reps rep ${formatDecimal(weight)} weight"
                    }
                    "CARDIO" -> {
                        val time = lastCompletedSet.time ?: lastCompletedSet.recommendedTime ?: 0
                        val dist = lastCompletedSet.distance ?: lastCompletedSet.recommendedDistance ?: 0.0
                        "LAST ${formatTime(time)} (${formatDecimal(dist)}km)"
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
        activeExerciseStates.value = states
    }

    // --- Workout Ticker Timer ---

    fun startWorkout() {
        isWorkoutStarted.value = true
        isTimerPaused.value = false
        startWorkoutTimer()
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
        distance: Double?
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
                        distance = distance
                    )
                    repository.updateWorkoutSet(updatedSet)
                    
                    val activeId = activeWorkout.value?.id ?: return@launch
                    reevaluatePRsForExercise(activeId, match.exerciseId)
                    loadExerciseStatesForWorkout(activeId)
                    break
                }
            }
        }
    }

    fun toggleSetCompletion(setId: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            val activeId = activeWorkout.value?.id ?: return@launch
            val states = activeExerciseStates.value
            
            for (state in states) {
                val match = state.sets.find { it.id == setId }
                if (match != null) {
                    var finalWeight = match.weight
                    var finalReps = match.reps
                    var finalTime = match.time
                    var finalDistance = match.distance

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
                        distance = finalDistance
                    )
                    repository.updateWorkoutSet(updatedSet)
                    
                    reevaluatePRsForExercise(activeId, match.exerciseId)
                    loadExerciseStatesForWorkout(activeId)

                    // Trigger Cooldown Timer if set is marked complete
                    if (isCompleted) {
                        triggerCooldown(state.exercise)
                    }
                    break
                }
            }
        }
    }

    // --- Cooldown Timer ---

    private fun triggerCooldown(exercise: ExerciseEntity) {
        val duration = when (exercise.impactLevel) {
            "HIGH" -> 120 // 2 min
            "MEDIUM" -> 60 // 1 min
            "LOW" -> 30  // 30 sec
            else -> 60
        }
        
        cooldownExerciseName.value = exercise.name
        cooldownDuration.value = duration
        cooldownRemaining.value = duration

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

    fun skipCooldown() {
        cooldownJob?.cancel()
        cooldownRemaining.value = 0
        cooldownExerciseName.value = null
    }

    // --- Cardio/Hold Countdown Timer ---

    fun startCountdown(setId: Long, exerciseName: String, durationSeconds: Int) {
        countdownSetId.value = setId
        countdownExerciseName.value = exerciseName
        countdownDuration.value = durationSeconds
        countdownRemaining.value = durationSeconds
        isCountdownActive.value = true
        isCountdownPaused.value = false

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
                                exercise.name.contains("Running") -> 75.0 * distance
                                exercise.name.contains("Walking") -> 40.0 * distance // walking burn
                                exercise.name.contains("Cycling") -> 30.0 * distance
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

            val finalWorkout = workout.copy(
                isCompleted = true,
                totalCalories = totalCalories,
                totalSteps = totalSteps,
                prCount = prCount,
                durationInSeconds = workoutDuration.value,
                date = System.currentTimeMillis() // save completion date
            )
            repository.updateWorkout(finalWorkout)

            // Trigger summary overlay
            workoutSummary.value = WorkoutSummaryState(
                workoutId = finalWorkout.id,
                category = finalWorkout.category,
                totalCalories = totalCalories,
                totalSteps = totalSteps,
                prCount = prCount,
                durationInSeconds = workoutDuration.value
            )

            // Clear state and prep next
            activeWorkout.value = null
            isWorkoutStarted.value = false
            isTimerPaused.value = false
            workoutDuration.value = 0L
            activeExerciseStates.value = emptyList()
            
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
                recommendedWeight = lastSet?.recommendedWeight ?: (repository.getBestWeightForExercise(exerciseId) ?: getStandardStartWeight(exercise.name)),
                recommendedReps = lastSet?.recommendedReps ?: 10,
                recommendedTime = lastSet?.recommendedTime ?: (if (exercise.type == "CARDIO") getStandardStartDuration(exercise.name) else if (exercise.type == "HOLD") 60 else null),
                recommendedDistance = lastSet?.recommendedDistance ?: (if (exercise.type == "CARDIO") getStandardStartDistance(exercise.name) else null)
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
            val targetSets = 3
            for (setNum in 1..targetSets) {
                var recWeight: Double? = null
                var recReps: Int? = null
                var recTime: Int? = null
                var recDistance: Double? = null
                
                when (exercise.type) {
                    "LIFT" -> {
                        recWeight = bestWeight ?: getStandardStartWeight(exercise.name)
                        if (prevSets.isNotEmpty()) {
                            val lastSetReps = prevSets.lastOrNull()?.reps ?: 0
                            if (lastSetReps >= 12) {
                                val increment = if (exercise.name.contains("Squat") || exercise.name.contains("Deadlift")) 5.0 else 2.5
                                recWeight = (recWeight ?: 0.0) + increment
                                recReps = 8
                            } else {
                                recReps = 10
                            }
                        } else {
                            recReps = 10
                        }
                    }
                    "CARDIO" -> {
                        recTime = prevSets.firstOrNull()?.time ?: getStandardStartDuration(exercise.name)
                        recDistance = prevSets.firstOrNull()?.distance ?: getStandardStartDistance(exercise.name)
                    }
                    "HOLD" -> {
                        recTime = prevSets.firstOrNull()?.time ?: 60
                    }
                }
                
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
                        recWeight = bestWeight ?: getStandardStartWeight(newExercise.name)
                        if (prevSets.isNotEmpty()) {
                            val lastSetReps = prevSets.lastOrNull()?.reps ?: 0
                            if (lastSetReps >= 12) {
                                val increment = if (newExercise.name.contains("Squat") || newExercise.name.contains("Deadlift")) 5.0 else 2.5
                                recWeight = (recWeight ?: 0.0) + increment
                                recReps = 8
                            } else {
                                recReps = 10
                            }
                        } else {
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
                val maxWeight = completedSets.mapNotNull { it.weight }.maxOrNull() ?: 0.0
                val pastBestWeight = repository.getBestWeightForExercise(exerciseId) ?: 0.0
                val hasNewPR = maxWeight > pastBestWeight
                val firstPRSet = if (hasNewPR) completedSets.firstOrNull { it.weight == maxWeight } else null
                
                sets.forEach { set ->
                    val shouldBePR = firstPRSet != null && set.id == firstPRSet.id
                    if (set.isPR != shouldBePR) {
                        repository.updateWorkoutSet(set.copy(isPR = shouldBePR))
                    }
                }
            }
            "CARDIO" -> {
                val maxDist = completedSets.mapNotNull { it.distance }.maxOrNull() ?: 0.0
                val pastBestDist = repository.getBestDistanceForExercise(exerciseId) ?: 0.0
                val hasNewPR = maxDist > pastBestDist
                val firstPRSet = if (hasNewPR) completedSets.firstOrNull { it.distance == maxDist } else null
                
                sets.forEach { set ->
                    val shouldBePR = firstPRSet != null && set.id == firstPRSet.id
                    if (set.isPR != shouldBePR) {
                        repository.updateWorkoutSet(set.copy(isPR = shouldBePR))
                    }
                }
            }
            "HOLD" -> {
                val maxTime = completedSets.mapNotNull { it.time }.maxOrNull() ?: 0
                val pastBestTime = repository.getBestTimeForExercise(exerciseId) ?: 0
                val hasNewPR = maxTime > pastBestTime
                val firstPRSet = if (hasNewPR) completedSets.firstOrNull { it.time == maxTime } else null
                
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
}

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
    val durationInSeconds: Long
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
