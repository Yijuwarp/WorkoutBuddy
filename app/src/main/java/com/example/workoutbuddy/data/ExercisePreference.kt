package com.example.workoutbuddy.data

// Difficulty ceiling chosen by the user (UserProfileEntity.difficultyCeiling) and the fixed
// difficulty rating stamped on each exercise (ExerciseEntity.difficulty). Selecting a ceiling
// includes that tier and everything below it: HARD unlocks EASY+MEDIUM+HARD.
enum class Difficulty(val tier: Int) {
    EASY(0), MEDIUM(1), HARD(2);

    companion object {
        fun fromName(name: String?): Difficulty? = name?.let { n -> entries.find { it.name == n } }
    }
}

// Per-exercise frequency tag (ExercisePreferenceEntity.frequency). Absence of a row for an
// exercise means "neutral" - the same uniform-random behavior as before this feature existed.
enum class Frequency(val weight: Double) {
    ALWAYS(Double.POSITIVE_INFINITY), // guaranteed inclusion, handled separately from weighting
    OFTEN(1.5),
    LESS(0.5),
    NEVER(0.0); // hard exclude, handled separately from weighting

    companion object {
        fun fromName(name: String?): Frequency? = name?.let { n -> entries.find { it.name == n } }
    }
}

// Initial difficulty seeding derives from the exercise's existing impactLevel as a starting
// point (HIGH->HARD, MEDIUM->MEDIUM, LOW->EASY), to be hand-tuned afterward for outliers rather
// than rated from scratch.
fun difficultyFromImpactLevel(impactLevel: String): Difficulty = when (impactLevel) {
    "HIGH" -> Difficulty.HARD
    "LOW" -> Difficulty.EASY
    else -> Difficulty.MEDIUM
}

// Onboarding no longer asks for a difficulty ceiling directly - it's proxied from the gym
// experience level the user already picks (OnboardingScreen's "Beginner"/"Intermediate"/
// "Expert" step), so a brand-new profile starts at a sensible ceiling without an extra
// decision. Users can still change it later from Profile settings.
fun difficultyFromGymExperience(gymExperience: String): Difficulty = when (gymExperience) {
    "Beginner" -> Difficulty.EASY
    "Expert" -> Difficulty.HARD
    else -> Difficulty.MEDIUM
}
