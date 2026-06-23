package com.example.workoutbuddy.data

import com.example.workoutbuddy.R

// Equipment a user may or may not have access to. Exercises are tagged with a CSV of these
// ids (ExerciseEntity.equipment); an exercise with no tags is bodyweight-only and always
// available regardless of what the user owns.
enum class Equipment(val id: String, val displayName: String, val iconRes: Int) {
    BARBELL("BARBELL", "Barbell", R.drawable.eq_barbell_v2),
    DUMBBELL("DUMBBELL", "Dumbbell", R.drawable.eq_dumbbell_v2),
    KETTLEBELL("KETTLEBELL", "Kettlebell", R.drawable.eq_kettlebell_v2),
    CABLE_MACHINE("CABLE_MACHINE", "Cable machine", R.drawable.eq_cable_machine_v2),
    LEG_PRESS_MACHINE("LEG_PRESS_MACHINE", "Leg press machine", R.drawable.eq_leg_press_machine_v2),
    LAT_PULLDOWN_MACHINE("LAT_PULLDOWN_MACHINE", "Lat pulldown machine", R.drawable.eq_lat_pulldown_machine_v2),
    PULL_UP_BAR("PULL_UP_BAR", "Pull-up bar", R.drawable.eq_pull_up_bar_v2),
    WEIGHT_BENCH("WEIGHT_BENCH", "Weight bench", R.drawable.eq_weight_bench_v2),
    JUMP_ROPE("JUMP_ROPE", "Jump rope", R.drawable.eq_jump_rope_v2),
    AB_WHEEL("AB_WHEEL", "Ab wheel", R.drawable.eq_ab_wheel_v2),
    BATTLE_ROPES("BATTLE_ROPES", "Battle ropes", R.drawable.eq_battle_ropes_v2),
    TREADMILL("TREADMILL", "Treadmill", R.drawable.eq_treadmill_v2),
    STATIONARY_BIKE("STATIONARY_BIKE", "Stationary bike", R.drawable.eq_stationary_bike_v2),
    STAIR_MACHINE("STAIR_MACHINE", "Stair machine", R.drawable.eq_stair_machine_v2),
    ROWING_MACHINE("ROWING_MACHINE", "Rowing machine", R.drawable.eq_rowing_machine_v2);

    companion object {
        fun fromId(id: String): Equipment? = entries.find { it.id == id }

        val allIdsCsv: String = entries.joinToString(",") { it.id }

        fun parseCsv(csv: String): Set<Equipment> =
            if (csv.isBlank()) emptySet()
            else csv.split(",").mapNotNull { fromId(it.trim()) }.toSet()
    }
}

// Deterministic name-based equipment inference for the seed exercise list, so each of the
// ~80 entries doesn't need a manually-typed equipment list. Exercises that match nothing keep
// an empty equipment set, meaning bodyweight-only / always available.
fun inferEquipmentForExercise(name: String): String {
    val n = name.lowercase()
    val tags = mutableSetOf<Equipment>()

    val needsBench = n.contains("bench press") || n.contains("dumbbell fly") ||
        n.contains("pullover") || n.contains("skull crusher") || n.contains("dumbbell row") ||
        n.contains("preacher curl") || n.contains("chest-supported row") ||
        n.contains("bulgarian split squat") || n.contains("hip thrust")

    when {
        n.contains("barbell") -> tags.add(Equipment.BARBELL)
        n.contains("dumbbell") -> tags.add(Equipment.DUMBBELL)
    }
    if (n.contains("kettlebell")) tags.add(Equipment.KETTLEBELL)
    if (n.contains("cable") || n.contains("pushdown") || n.contains("face pull")) {
        tags.add(Equipment.CABLE_MACHINE)
    }
    if (n.contains("lat pulldown")) tags.add(Equipment.LAT_PULLDOWN_MACHINE)
    if (n.contains("machine") || n.contains("leg press") || n.contains("leg curl") ||
        n.contains("leg extension") || n.contains("hack squat") || n.contains("pec deck")
    ) {
        tags.add(Equipment.LEG_PRESS_MACHINE)
    }
    if (n.contains("pull-up") || n.contains("pull up") || n.contains("chin-up") || n.contains("chin up") ||
        n.contains("inverted row") || n.contains("dip")
    ) {
        tags.add(Equipment.PULL_UP_BAR)
    }
    if (n.contains("t-bar row") || n.contains("good morning") || n.contains("shrug") ||
        n.contains("upright row") || n.contains("sumo deadlift") || n.contains("back squat") ||
        n.contains("front squat") || n.contains("lunge") && n.contains("barbell")
    ) {
        tags.add(Equipment.BARBELL)
    }
    if (n.contains("goblet squat")) tags.add(Equipment.DUMBBELL)
    if (needsBench) tags.add(Equipment.WEIGHT_BENCH)
    if (n.contains("ab wheel") || n.contains("rollout")) tags.add(Equipment.AB_WHEEL)
    if (n.contains("battle rope")) tags.add(Equipment.BATTLE_ROPES)
    if (n.contains("jump rope")) tags.add(Equipment.JUMP_ROPE)
    if (n.contains("rowing machine")) tags.add(Equipment.ROWING_MACHINE)
    if (n.contains("stair climber")) tags.add(Equipment.STAIR_MACHINE)
    if (n.contains("elliptical")) tags.add(Equipment.STATIONARY_BIKE)
    if (n.contains("cycling")) tags.add(Equipment.STATIONARY_BIKE)

    return tags.joinToString(",") { it.id }
}
