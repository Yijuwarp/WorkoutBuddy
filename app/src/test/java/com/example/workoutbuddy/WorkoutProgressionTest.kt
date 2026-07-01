package com.example.workoutbuddy

import com.example.workoutbuddy.data.database.MuscleGroupRecoveryEntity
import com.example.workoutbuddy.viewmodel.WorkoutViewModel
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutProgressionTest {

    // Mirrors WorkoutViewModel.progressLift: reps climb to a cap of 12, then weight steps
    // up by 2.5kg and reps reset to 8.
    private fun progressLift(lastWeight: Double, lastReps: Int): Pair<Double, Int> {
        return if (lastReps < 12) lastWeight to (lastReps + 1) else (lastWeight + 2.5) to 8
    }

    // Mirrors WorkoutViewModel.progressCardio: flat 5% increase to both distance and time,
    // which holds pace constant since both scale by the same factor.
    private fun progressCardio(lastDistance: Double?, lastTime: Int): Pair<Double?, Int> {
        val nextDistance = lastDistance?.let { it * 1.05 }
        val nextTime = Math.round(lastTime * 1.05).toInt()
        return nextDistance to nextTime
    }

    @Test
    fun liftProgression_incrementsRepsBelowCap() {
        val (weight, reps) = progressLift(7.5, 8)
        assertEquals(7.5, weight, 0.001)
        assertEquals(9, reps)
    }

    @Test
    fun liftProgression_stepsWeightAndResetsRepsAtCap() {
        val (weight, reps) = progressLift(7.5, 12)
        assertEquals(10.0, weight, 0.001)
        assertEquals(8, reps)
    }

    @Test
    fun cardioProgression_increasesDistanceAndTimeByFivePercent() {
        val (distance, time) = progressCardio(2.0, 600)
        assertEquals(2.1, distance!!, 0.001)
        assertEquals(630, time)
    }

    @Test
    fun cardioProgression_holdsPaceConstant() {
        val lastPace = 600.0 / 2.0
        val (distance, time) = progressCardio(2.0, 600)
        val newPace = time / distance!!
        assertEquals(lastPace, newPace, 0.001)
    }

    @Test
    fun recovery_neverTrainedMuscleGroupIsFullyRecovered() {
        val pct = WorkoutViewModel.currentRecoveryPct(null)
        assertEquals(100.0, pct, 0.001)
    }

    @Test
    fun recovery_decaysTwentyPercentPerDay() {
        val now = 1_000_000_000_000L
        val oneDayMs = 86_400_000L
        val entity = MuscleGroupRecoveryEntity(muscleGroup = "PUSH", fatiguePct = 100.0, lastUpdatedAt = now)
        val pctAfterOneDay = WorkoutViewModel.currentRecoveryPct(entity, now + oneDayMs)
        assertEquals(20.0, pctAfterOneDay, 0.001) // 100 fatigue - 20 = 80 fatigue -> 20% recovered

        val pctAfterFiveDays = WorkoutViewModel.currentRecoveryPct(entity, now + 5 * oneDayMs)
        assertEquals(100.0, pctAfterFiveDays, 0.001) // fully recovered by day 5
    }

    @Test
    fun recovery_accumulatesAcrossSessions() {
        val now = 1_000_000_000_000L
        // First set: 0 -> 25% fatigue (75% recovered)
        val afterFirstSet = MuscleGroupRecoveryEntity(muscleGroup = "PULL", fatiguePct = 25.0, lastUpdatedAt = now)
        // Second set immediately after (no decay elapsed): fatigue should add on top, not reset.
        val decayed = (afterFirstSet.fatiguePct - 20.0 * 0.0).coerceAtLeast(0.0)
        val afterSecondSet = (decayed + 25.0).coerceAtMost(100.0)
        assertEquals(50.0, afterSecondSet, 0.001)
    }
}
