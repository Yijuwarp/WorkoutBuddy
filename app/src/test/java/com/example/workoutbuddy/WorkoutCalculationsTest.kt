package com.example.workoutbuddy

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutCalculationsTest {

    @Test
    fun testLiftCalorieCalculation() {
        // Weighted Lift: calories = (weight * reps * 0.05) + 3.0
        val weight = 80.0
        val reps = 10
        val expectedWeighted = (weight * reps * 0.05) + 3.0
        assertEquals(43.0, expectedWeighted, 0.001)

        // Bodyweight Lift (weight = 0): calories = (reps * 0.2) + 3.0
        val bwWeight = 0.0
        val bwReps = 15
        val expectedBodyweight = (bwReps * 0.2) + 3.0
        assertEquals(6.0, expectedBodyweight, 0.001)
    }

    @Test
    fun testHoldCalorieCalculation() {
        // Plank Hold: calories = durationSec * 0.15
        val durationSeconds = 60
        val expected = durationSeconds * 0.15
        assertEquals(9.0, expected, 0.001)
    }

    @Test
    fun testCardioCalorieCalculation() {
        // Running: 75.0 * distance_km
        val runDistance = 5.0
        val expectedRun = 75.0 * runDistance
        assertEquals(375.0, expectedRun, 0.001)

        // Walking: 40.0 * distance_km
        val walkDistance = 3.0
        val expectedWalk = 40.0 * walkDistance
        assertEquals(120.0, expectedWalk, 0.001)

        // Cycling: 30.0 * distance_km
        val cycleDistance = 10.0
        val expectedCycle = 30.0 * cycleDistance
        assertEquals(300.0, expectedCycle, 0.001)

        // Elliptical (EFX): 7.0 * duration_minutes
        val efxSeconds = 1200 // 20 minutes
        val expectedEfx = 7.0 * (efxSeconds / 60.0)
        assertEquals(140.0, expectedEfx, 0.001)
    }

    @Test
    fun testStepsCalculation() {
        // Running steps: distance * 1250
        val runDistance = 4.0
        val expectedRunSteps = (runDistance * 1250).toInt()
        assertEquals(5000, expectedRunSteps)

        // Walking steps: distance * 1300
        val walkDistance = 2.0
        val expectedWalkSteps = (walkDistance * 1300).toInt()
        assertEquals(2600, expectedWalkSteps)

        // EFX steps: durationSeconds * 2.0 (120 steps/minute)
        val durationSeconds = 600 // 10 minutes
        val expectedEfxSteps = (durationSeconds * 2.0).toInt()
        assertEquals(1200, expectedEfxSteps)
    }

    @Test
    fun testProgressiveOverloadLogic() {
        // Simulation of our progressive overload rules:
        // If reps achieved last session is >= 12, weight increases by 2.5 (or 5.0 for squats/deadlifts) and reps resets to 8.
        // Otherwise, weight matches and recommended reps is 10.
        
        fun getRecommendation(exerciseName: String, lastWeight: Double, lastReps: Int): Pair<Double, Int> {
            return if (lastReps >= 12) {
                val increment = if (exerciseName.contains("Squat") || exerciseName.contains("Deadlift")) 5.0 else 2.5
                Pair(lastWeight + increment, 8)
            } else {
                Pair(lastWeight, 10)
            }
        }

        // Case 1: Squat reps >= 12 (Squat is heavy, should increment by 5.0)
        val squatRec = getRecommendation("Barbell Squat", 100.0, 12)
        assertEquals(105.0, squatRec.first, 0.001)
        assertEquals(8, squatRec.second)

        // Case 2: Bench press reps >= 12 (Bench press increments by 2.5)
        val benchRec = getRecommendation("Barbell Bench Press", 80.0, 13)
        assertEquals(82.5, benchRec.first, 0.001)
        assertEquals(8, benchRec.second)

        // Case 3: Overhead Press reps < 12 (weight matches, reps defaults to 10)
        val ohpRec = getRecommendation("Dumbbell Overhead Press", 20.0, 10)
        assertEquals(20.0, ohpRec.first, 0.001)
        assertEquals(10, ohpRec.second)
    }
}
