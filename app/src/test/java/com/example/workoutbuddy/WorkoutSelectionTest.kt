package com.example.workoutbuddy

import com.example.workoutbuddy.data.Difficulty
import com.example.workoutbuddy.data.Equipment
import com.example.workoutbuddy.data.Frequency
import com.example.workoutbuddy.data.database.ExerciseEntity
import com.example.workoutbuddy.viewmodel.selectWorkoutExercises
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class WorkoutSelectionTest {

    private fun exercise(
        id: Int,
        bodyPart: String = "Chest",
        difficulty: Difficulty = Difficulty.MEDIUM,
        equipment: String = "",
        type: String = "LIFT"
    ) = ExerciseEntity(
        id = id,
        name = "Exercise $id",
        category = "PUSH",
        type = type,
        bodyPart = bodyPart,
        calorieBurnRate = 0.2,
        description = "",
        howToSteps = "",
        impactLevel = "MEDIUM",
        equipment = equipment,
        difficulty = difficulty.name
    )

    private val allEquipment = Equipment.entries.toSet()

    @Test
    fun alwaysExerciseOverridesDiversityCap() {
        // 3 exercises sharing a bodyPart, cap is 2 - without Always, only 2 of the 3 could appear.
        val pool = listOf(
            exercise(1, bodyPart = "Chest"),
            exercise(2, bodyPart = "Chest"),
            exercise(3, bodyPart = "Chest")
        )
        val preferences = mapOf(1 to Frequency.ALWAYS, 2 to Frequency.ALWAYS, 3 to Frequency.ALWAYS)

        val result = selectWorkoutExercises(
            categoryPool = pool,
            cardioPool = emptyList(),
            ownedEquipment = allEquipment,
            difficultyCeiling = Difficulty.HARD,
            preferences = preferences,
            count = 4,
            maxPerBodyPart = 2
        )

        assertEquals(setOf(1, 2, 3), result.toSet())
    }

    @Test
    fun alwaysExerciseStillExcludedByEquipment() {
        val pool = listOf(
            exercise(1, equipment = "BARBELL"),
            exercise(2)
        )
        val preferences = mapOf(1 to Frequency.ALWAYS)

        val result = selectWorkoutExercises(
            categoryPool = pool,
            cardioPool = emptyList(),
            ownedEquipment = emptySet(), // owns no equipment
            difficultyCeiling = Difficulty.HARD,
            preferences = preferences,
            count = 4,
            maxPerBodyPart = 2
        )

        assertFalse(1 in result)
    }

    @Test
    fun alwaysExerciseStillExcludedByDifficultyCeiling() {
        val pool = listOf(
            exercise(1, difficulty = Difficulty.HARD),
            exercise(2, difficulty = Difficulty.EASY)
        )
        val preferences = mapOf(1 to Frequency.ALWAYS)

        val result = selectWorkoutExercises(
            categoryPool = pool,
            cardioPool = emptyList(),
            ownedEquipment = allEquipment,
            difficultyCeiling = Difficulty.EASY,
            preferences = preferences,
            count = 4,
            maxPerBodyPart = 2
        )

        assertFalse(1 in result)
    }

    @Test
    fun neverExerciseNeverAppears() {
        val pool = (1..6).map { exercise(it, bodyPart = "BodyPart$it") }
        val preferences = mapOf(3 to Frequency.NEVER)

        repeat(50) { seed ->
            val result = selectWorkoutExercises(
                categoryPool = pool,
                cardioPool = emptyList(),
                ownedEquipment = allEquipment,
                difficultyCeiling = Difficulty.HARD,
                preferences = preferences,
                count = 4,
                maxPerBodyPart = 2,
                rng = Random(seed)
            )
            assertFalse(3 in result)
        }
    }

    @Test
    fun tooManyNeverExclusionsProduceShorterWorkoutInsteadOfRelaxingGates() {
        // Pool of 5, 4 marked Never -> only 1 eligible, requesting 4 should yield exactly 1.
        val pool = (1..5).map { exercise(it, bodyPart = "BodyPart$it") }
        val preferences = (1..4).associateWith { Frequency.NEVER }

        val result = selectWorkoutExercises(
            categoryPool = pool,
            cardioPool = emptyList(),
            ownedEquipment = allEquipment,
            difficultyCeiling = Difficulty.HARD,
            preferences = preferences,
            count = 4,
            maxPerBodyPart = 2
        )

        assertEquals(listOf(5), result)
    }

    @Test
    fun cardioRespectsDifficultyCeilingFrequencyAndCanBeEmpty() {
        val hardCardio = exercise(10, difficulty = Difficulty.HARD, type = "CARDIO")
        val neverCardio = exercise(11, type = "CARDIO")
        val cardioPool = listOf(hardCardio, neverCardio)
        val preferences = mapOf(11 to Frequency.NEVER)

        val result = selectWorkoutExercises(
            categoryPool = emptyList(),
            cardioPool = cardioPool,
            ownedEquipment = allEquipment,
            difficultyCeiling = Difficulty.EASY, // excludes the HARD cardio
            preferences = preferences,
            count = 4,
            maxPerBodyPart = 2
        )

        // Both cardio candidates are gated out -> no cardio appended, no error.
        assertTrue(result.isEmpty())
    }

    @Test
    fun oftenSelectedMoreOftenThanNeutralAndLessSelectedLessOften() {
        val often = exercise(1, bodyPart = "A")
        val less = exercise(2, bodyPart = "B")
        val neutral = exercise(3, bodyPart = "C")
        val pool = listOf(often, less, neutral)
        val preferences = mapOf(1 to Frequency.OFTEN, 2 to Frequency.LESS)

        var oftenCount = 0
        var lessCount = 0
        var neutralCount = 0
        val iterations = 2000
        repeat(iterations) { seed ->
            val result = selectWorkoutExercises(
                categoryPool = pool,
                cardioPool = emptyList(),
                ownedEquipment = allEquipment,
                difficultyCeiling = Difficulty.HARD,
                preferences = preferences,
                count = 1, // force a choice between the three each iteration
                maxPerBodyPart = 3,
                rng = Random(seed)
            )
            when (result.firstOrNull()) {
                1 -> oftenCount++
                2 -> lessCount++
                3 -> neutralCount++
            }
        }

        // Directional only, per PRD ("doesn't have to be perfect"): Often > neutral > Less.
        assertTrue("often=$oftenCount should beat neutral=$neutralCount", oftenCount > neutralCount)
        assertTrue("neutral=$neutralCount should beat less=$lessCount", neutralCount > lessCount)
    }

    @Test
    fun untaggedExercisesBehaveLikeUniformRandomSelection() {
        val pool = (1..4).map { exercise(it, bodyPart = "BodyPart$it") }

        val result = selectWorkoutExercises(
            categoryPool = pool,
            cardioPool = emptyList(),
            ownedEquipment = allEquipment,
            difficultyCeiling = Difficulty.HARD,
            preferences = emptyMap(),
            count = 4,
            maxPerBodyPart = 2
        )

        assertEquals(setOf(1, 2, 3, 4), result.toSet())
    }
}
