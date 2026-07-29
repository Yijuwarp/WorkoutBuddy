package com.example.workoutbuddy.viewmodel

import com.example.workoutbuddy.data.WorkoutRepository
import com.example.workoutbuddy.data.database.WorkoutSetEntity
import kotlinx.coroutines.flow.MutableStateFlow

class WorkoutPRManager(
    private val repository: WorkoutRepository
) {
    val recordBrokenCelebration = MutableStateFlow<RecordBrokenState?>(null)

    private val celebrationBaselineForExercise = mutableMapOf<String, String>()

    fun celebrateRecordBroken(workoutId: Long, exerciseId: Int, exerciseName: String, freshOldRecord: String, newRecord: String) {
        val key = "${workoutId}:${exerciseId}"
        val alreadyCelebrated = key in celebrationBaselineForExercise
        val oldRecord = celebrationBaselineForExercise.getOrPut(key) { freshOldRecord }
        if (!alreadyCelebrated) {
            recordBrokenCelebration.value = RecordBrokenState(exerciseName, oldRecord, newRecord)
        } else {
            recordBrokenCelebration.value?.let { pending ->
                if (pending.exerciseName == exerciseName) {
                    recordBrokenCelebration.value = pending.copy(newRecord = newRecord)
                }
            }
        }
    }

    fun dismissRecordCelebration() {
        recordBrokenCelebration.value = null
    }

    suspend fun reevaluatePRsForExercise(workoutId: Long, exerciseId: Int) {
        val exercise = repository.getExerciseById(exerciseId) ?: return
        val sets = repository.getSetsForWorkout(workoutId).filter { it.exerciseId == exerciseId }
        val completedSets = sets.filter { it.isCompleted }

        when (exercise.type) {
            "LIFT" -> {
                val currentBestSet = completedSets.maxWithOrNull(compareBy<WorkoutSetEntity> { it.weight ?: 0.0 }.thenBy { it.reps ?: 0 })
                val pastBestSet = repository.getBestSetForExercise(exerciseId)
                val hasNewPR = if (currentBestSet == null || pastBestSet == null) {
                    false
                } else {
                    val currentWeight = currentBestSet.weight ?: 0.0
                    val pastWeight = pastBestSet.weight ?: 0.0
                    val currentReps = currentBestSet.reps ?: 0
                    val pastReps = pastBestSet.reps ?: 0
                    currentWeight > pastWeight || (currentWeight == pastWeight && currentReps > pastReps)
                }

                if (hasNewPR && currentBestSet != null && pastBestSet != null) {
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
                    val hasNewPR = pastBestTime > 0 && maxTime > pastBestTime

                    val firstPRSet = if (hasNewPR) completedSets.firstOrNull { it.time == maxTime } else null

                    if (hasNewPR && firstPRSet != null) {
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
                    val currentBestSet = completedSets.maxWithOrNull(
                        compareBy<WorkoutSetEntity> { it.distance ?: 0.0 }
                            .thenBy { it.inclinePct ?: 0.0 }
                            .thenBy { -(it.time ?: Int.MAX_VALUE) }
                    )
                    val pastBestSet = repository.getBestDistanceSetForExercise(exerciseId)
                    val hasNewPR = if (currentBestSet == null || pastBestSet == null) {
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

                    if (hasNewPR && currentBestSet != null && pastBestSet != null) {
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
                val hasNewPR = pastBestTime > 0 && maxTime > pastBestTime

                val firstPRSet = if (hasNewPR) completedSets.firstOrNull { it.time == maxTime } else null

                if (hasNewPR && firstPRSet != null) {
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
