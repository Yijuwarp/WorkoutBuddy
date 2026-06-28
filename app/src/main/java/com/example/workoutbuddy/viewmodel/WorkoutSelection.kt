package com.example.workoutbuddy.viewmodel

import com.example.workoutbuddy.data.Difficulty
import com.example.workoutbuddy.data.Equipment
import com.example.workoutbuddy.data.Frequency
import com.example.workoutbuddy.data.database.ExerciseEntity
import kotlin.random.Random

// Pure exercise-selection logic, kept free of Room/coroutines/ViewModel so it can be unit
// tested directly. Used for both the main category pool and the cardio pool - the same
// gates (equipment, difficulty ceiling, Never) and weighting (Always/Often/Less) apply to both.

// An exercise with no equipment tags is bodyweight-only and always available; otherwise
// every tagged equipment must be in the user's owned set.
fun isExerciseAvailable(exercise: ExerciseEntity, ownedEquipment: Set<Equipment>): Boolean {
    val required = Equipment.parseCsv(exercise.equipment)
    return required.isEmpty() || required.all { it in ownedEquipment }
}

// Hard gates: equipment ownership, difficulty ceiling, and "Never" are absolute and never
// relaxed, regardless of frequency tag (including "Always" - see selectFromPool).
private fun eligibleForSelection(
    pool: List<ExerciseEntity>,
    ownedEquipment: Set<Equipment>,
    difficultyCeiling: Difficulty,
    preferences: Map<Int, Frequency>
): List<ExerciseEntity> = pool.filter { exercise ->
    isExerciseAvailable(exercise, ownedEquipment) &&
        (Difficulty.fromName(exercise.difficulty) ?: Difficulty.MEDIUM).tier <= difficultyCeiling.tier &&
        preferences[exercise.id] != Frequency.NEVER
}

/**
 * Selects exercises for one pool (the main category pool, or the cardio pool), already
 * gate-filtered to [eligiblePool]. "Always"-tagged exercises are guaranteed inclusion: they're
 * added unconditionally and can push the result past [count] and past the per-bodyPart cap that
 * otherwise governs the remaining weighted/random fill picks. If the cap blocks filling every
 * remaining slot, it's relaxed for the fill phase only (gates above are never relaxed) - so a
 * too-small eligible pool yields a shorter list rather than an error.
 */
private fun selectFromPool(
    eligiblePool: List<ExerciseEntity>,
    count: Int,
    maxPerBodyPart: Int,
    preferences: Map<Int, Frequency>,
    rng: Random
): List<ExerciseEntity> {
    val always = eligiblePool.filter { preferences[it.id] == Frequency.ALWAYS }
    val alwaysIds = always.map { it.id }.toSet()
    val selected = always.toMutableList()

    val bodyPartCounts = mutableMapOf<String, Int>()
    for (exercise in always) {
        bodyPartCounts[exercise.bodyPart] = bodyPartCounts.getOrDefault(exercise.bodyPart, 0) + 1
    }

    val remainingCandidates = eligiblePool.filter { it.id !in alwaysIds }
    val slotsLeft = (count - selected.size).coerceAtLeast(0)
    if (slotsLeft > 0 && remainingCandidates.isNotEmpty()) {
        selected.addAll(
            weightedFill(remainingCandidates, slotsLeft, maxPerBodyPart, bodyPartCounts, preferences, rng)
        )
    }
    return selected
}

private fun weightOf(exercise: ExerciseEntity, preferences: Map<Int, Frequency>): Double {
    val freq = preferences[exercise.id]
    // Always/Never are handled outside of weighting (guaranteed inclusion / hard exclude),
    // so any candidate reaching this point is either untagged (neutral, 1.0) or Often/Less.
    return if (freq == null) 1.0 else freq.weight
}

private fun weightedFill(
    candidates: List<ExerciseEntity>,
    slots: Int,
    maxPerBodyPart: Int,
    bodyPartCounts: MutableMap<String, Int>,
    preferences: Map<Int, Frequency>,
    rng: Random
): List<ExerciseEntity> {
    val pool = candidates.toMutableList()
    val picked = mutableListOf<ExerciseEntity>()

    repeat(slots) {
        if (pool.isEmpty()) return@repeat
        val capped = pool.filter { bodyPartCounts.getOrDefault(it.bodyPart, 0) < maxPerBodyPart }
        val from = capped.ifEmpty { pool }
        val totalWeight = from.sumOf { weightOf(it, preferences) }
        var roll = rng.nextDouble() * totalWeight
        var chosen = from.last()
        for (exercise in from) {
            roll -= weightOf(exercise, preferences)
            if (roll <= 0.0) {
                chosen = exercise
                break
            }
        }
        picked.add(chosen)
        pool.remove(chosen)
        bodyPartCounts[chosen.bodyPart] = bodyPartCounts.getOrDefault(chosen.bodyPart, 0) + 1
    }
    return picked
}

/**
 * Top-level entry point: selects the main-pool exercises and (if any are eligible) the cardio
 * pick(s) for a new workout, applying the difficulty ceiling, equipment availability, and
 * per-exercise frequency preferences uniformly to both pools. Returns exercise ids, main pool
 * first then cardio.
 */
fun selectWorkoutExercises(
    categoryPool: List<ExerciseEntity>,
    cardioPool: List<ExerciseEntity>,
    ownedEquipment: Set<Equipment>,
    difficultyCeiling: Difficulty,
    preferences: Map<Int, Frequency>,
    count: Int,
    maxPerBodyPart: Int,
    cardioCount: Int = 1,
    rng: Random = Random.Default
): List<Int> {
    val mainEligible = eligibleForSelection(categoryPool, ownedEquipment, difficultyCeiling, preferences)
    val mainSelected = selectFromPool(mainEligible, count, maxPerBodyPart, preferences, rng)

    val cardioEligible = eligibleForSelection(cardioPool, ownedEquipment, difficultyCeiling, preferences)
    val cardioSelected = selectFromPool(cardioEligible, cardioCount, Int.MAX_VALUE, preferences, rng)

    return (mainSelected + cardioSelected).map { it.id }
}
