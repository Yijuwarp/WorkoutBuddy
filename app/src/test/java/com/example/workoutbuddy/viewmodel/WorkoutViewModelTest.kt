package com.example.workoutbuddy.viewmodel

import com.example.workoutbuddy.data.database.ExerciseEntity
import com.example.workoutbuddy.data.database.MuscleGroupRecoveryEntity
import com.example.workoutbuddy.data.database.WorkoutSetEntity
import com.example.workoutbuddy.data.database.WorkoutEntity
import com.example.workoutbuddy.data.database.UserProfileEntity
import com.example.workoutbuddy.data.database.ExercisePreferenceEntity
import com.example.workoutbuddy.data.database.EquipmentPresetEntity
import org.junit.Assert.*
import org.junit.Test

class WorkoutViewModelTest {

    // --- Active Workout State & Progression Tests ---

    @Test
    fun testWorkoutSizing() {
        val shortSizing = WorkoutProgressionHelper.workoutSizing(15)
        assertEquals(2 to 0, shortSizing)

        val mediumSizing = WorkoutProgressionHelper.workoutSizing(45)
        assertEquals(4 to 1, mediumSizing)

        val longSizing = WorkoutProgressionHelper.workoutSizing(60)
        assertEquals(5 to 1, longSizing)
    }

    @Test
    fun testCardioWorkoutSizing() {
        val shortCardio = WorkoutProgressionHelper.cardioWorkoutSizing(15)
        assertEquals(0 to 1, shortCardio)

        val mediumCardio = WorkoutProgressionHelper.cardioWorkoutSizing(45)
        assertEquals(2 to 2, mediumCardio)
    }

    @Test
    fun testProgressLift() {
        val (w1, r1) = WorkoutProgressionHelper.progressLift(80.0, 8)
        assertEquals(80.0, w1, 0.001)
        assertEquals(9, r1)

        val (w2, r2) = WorkoutProgressionHelper.progressLift(80.0, 12)
        assertEquals(82.5, w2, 0.001)
        assertEquals(8, r2)
    }

    @Test
    fun testProgressCardio() {
        val (nextDist, nextTime) = WorkoutProgressionHelper.progressCardio(5.0, 1800)
        assertNotNull(nextDist)
        assertEquals(5.25, nextDist!!, 0.001)
        assertEquals(1890, nextTime)
    }

    @Test
    fun testAdaptiveStartWeight() {
        val pushupStart = WorkoutProgressionHelper.getAdaptiveStartWeight("Push-ups", 100.0, 75.0)
        assertEquals(0.0, pushupStart, 0.001)

        val squatStart = WorkoutProgressionHelper.getAdaptiveStartWeight("Barbell Squat", 100.0, 75.0)
        assertTrue(squatStart > 0.0)

        val deadliftStart = WorkoutProgressionHelper.getAdaptiveStartWeight("Deadlift", 100.0, 75.0)
        assertEquals(30.0, deadliftStart, 0.001)
    }

    // --- Performance & Calorie Calculation Tests ---

    @Test
    fun testCalculateSetPerformanceLift() {
        val bwPerf = WorkoutPerformanceCalculator.calculateSetPerformance(
            exerciseName = "Push-ups",
            weight = 0.0,
            reps = 10,
            time = null,
            distance = null,
            exerciseType = "LIFT",
            userBodyWeight = 70.0
        )
        assertTrue(bwPerf > 0.0)

        val weightedPerf = WorkoutPerformanceCalculator.calculateSetPerformance(
            exerciseName = "Barbell Bench Press",
            weight = 80.0,
            reps = 10,
            time = null,
            distance = null,
            exerciseType = "LIFT",
            userBodyWeight = 70.0
        )
        assertTrue(weightedPerf > bwPerf)
    }

    @Test
    fun testCalculateSetPerformanceCardio() {
        val jumpRopePerf = WorkoutPerformanceCalculator.calculateSetPerformance(
            exerciseName = "Jump Rope",
            weight = null,
            reps = null,
            time = 300,
            distance = null,
            exerciseType = "CARDIO",
            userBodyWeight = 70.0
        )
        assertEquals(300 * 0.2 * 1.5, jumpRopePerf, 0.001)

        val runPerf = WorkoutPerformanceCalculator.calculateSetPerformance(
            exerciseName = "Running",
            weight = null,
            reps = null,
            time = 1800,
            distance = 5.0,
            exerciseType = "CARDIO",
            userBodyWeight = 70.0
        )
        assertTrue(runPerf > 0.0)
    }

    @Test
    fun testCalculateSetPerformanceHold() {
        val plankPerf = WorkoutPerformanceCalculator.calculateSetPerformance(
            exerciseName = "Plank",
            weight = null,
            reps = null,
            time = 60,
            distance = null,
            exerciseType = "HOLD",
            userBodyWeight = 70.0
        )
        assertEquals(60 * 0.6 * 1.5, plankPerf, 0.001)
    }

    @Test
    fun testCalculateSetCalories() {
        val liftExercise = ExerciseEntity(
            id = 1, name = "Bench Press", type = "LIFT", bodyPart = "Chest",
            impactLevel = "HIGH", category = "PUSH", calorieBurnRate = 3.0,
            description = "", howToSteps = ""
        )
        val setWeighted = WorkoutSetEntity(id = 1, workoutId = 1, exerciseId = 1, setNumber = 1, weight = 80.0, reps = 10)
        val calWeighted = WorkoutPerformanceCalculator.calculateSetCalories(setWeighted, liftExercise)
        assertEquals((80.0 * 10 * 0.05) + 3.0, calWeighted, 0.001)

        val cardioExercise = ExerciseEntity(
            id = 2, name = "Running", type = "CARDIO", bodyPart = "Full Body",
            impactLevel = "HIGH", category = "CARDIO", calorieBurnRate = 8.0,
            description = "", howToSteps = ""
        )
        val setCardio = WorkoutSetEntity(id = 2, workoutId = 1, exerciseId = 2, setNumber = 1, distance = 5.0, time = 1800)
        val calCardio = WorkoutPerformanceCalculator.calculateSetCalories(setCardio, cardioExercise)
        assertEquals(75.0 * 5.0, calCardio, 0.001)
    }

    @Test
    fun testScoreCompletedSetAgainstExpectation() {
        val exercise = ExerciseEntity(
            id = 1, name = "Bench Press", type = "LIFT", bodyPart = "Chest",
            impactLevel = "HIGH", category = "PUSH", calorieBurnRate = 3.0,
            description = "", howToSteps = ""
        )
        val setOnPar = WorkoutSetEntity(
            id = 1, workoutId = 1, exerciseId = 1, setNumber = 1,
            recommendedWeight = 80.0, recommendedReps = 10,
            weight = 80.0, reps = 10
        )
        val scoreOnPar = WorkoutPerformanceCalculator.scoreCompletedSetAgainstExpectation(setOnPar, exercise, 70.0)
        assertEquals(70.0, scoreOnPar, 0.001)

        val setExceeding = WorkoutSetEntity(
            id = 2, workoutId = 1, exerciseId = 1, setNumber = 1,
            recommendedWeight = 80.0, recommendedReps = 10,
            weight = 100.0, reps = 12
        )
        val scoreExceeding = WorkoutPerformanceCalculator.scoreCompletedSetAgainstExpectation(setExceeding, exercise, 70.0)
        assertEquals(100.0, scoreExceeding, 0.001)
    }

    // --- Muscle Group & Fatigue Tests ---

    @Test
    fun testMuscleGroupsForBodyPart() {
        val chestLats = WorkoutPerformanceCalculator.muscleGroupsForBodyPart("Chest & Lats")
        assertTrue(chestLats.contains("Chest"))
        assertTrue(chestLats.contains("Back"))

        val shoulders = WorkoutPerformanceCalculator.muscleGroupsForBodyPart("Front Deltoids")
        assertEquals(listOf("Shoulders"), shoulders)
    }

    @Test
    fun testCurrentRecoveryPct() {
        assertEquals(100.0, WorkoutProgressionHelper.currentRecoveryPct(null), 0.001)

        val now = 1000000000L
        val oneDayAgo = now - 86400000L  // exactly 1 day ago in ms

        val recoveryEntity = MuscleGroupRecoveryEntity("Chest", 50.0, oneDayAgo)
        val recPct = WorkoutProgressionHelper.currentRecoveryPct(recoveryEntity, now)
        assertEquals(70.0, recPct, 0.001)
    }

    // --- Rank & Profile Score Tests ---

    @Test
    fun testDeriveRankTier() {
        assertEquals("Bronze", WorkoutProgressionHelper.deriveRankTier(50.0, 50.0))
        assertEquals("Silver", WorkoutProgressionHelper.deriveRankTier(70.0, 70.0))
        assertEquals("Gold", WorkoutProgressionHelper.deriveRankTier(90.0, 90.0))
        assertEquals("Platinum", WorkoutProgressionHelper.deriveRankTier(110.0, 110.0))
        assertEquals("Diamond", WorkoutProgressionHelper.deriveRankTier(140.0, 140.0))
        assertEquals("Master", WorkoutProgressionHelper.deriveRankTier(180.0, 180.0))
        assertEquals("Grandmaster", WorkoutProgressionHelper.deriveRankTier(220.0, 220.0))
        assertEquals("Legend", WorkoutProgressionHelper.deriveRankTier(300.0, 300.0))
    }

    @Test
    fun testInitialScoreFormulas() {
        val strScore = WorkoutProgressionHelper.calculateInitialStrengthScore(25, 175.0, 70.0, "Male", "Intermediate")
        assertTrue(strScore in 30.0..999.0)

        val stamScore = WorkoutProgressionHelper.calculateInitialStaminaScore(25, 175.0, 70.0, "Male", "Intermediate")
        assertTrue(stamScore in 30.0..999.0)
    }

    @Test
    fun testDatabaseInitializerCategories() {
        val seedExercises = com.example.workoutbuddy.data.database.DatabaseInitializer.getSeedExercises()
        val jumpingJacks = seedExercises.find { it.name == "Jumping Jacks" }
        assertNotNull(jumpingJacks)
        assertEquals("FULL_BODY", jumpingJacks?.category)

        val burpees = seedExercises.find { it.name == "Burpees" }
        assertNotNull(burpees)
        assertEquals("FULL_BODY", burpees?.category)

        val running = seedExercises.find { it.name == "Running" }
        assertNotNull(running)
        assertEquals("CARDIO", running?.category)

        val walking = seedExercises.find { it.name == "Walking" }
        assertNotNull(walking)
        assertEquals("CARDIO", walking?.category)
    }

}

