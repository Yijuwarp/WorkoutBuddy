package com.example.workoutbuddy.data.database

object DatabaseInitializer {
    fun getSeedExercises(): List<ExerciseEntity> {
        return listOf(
            // PUSH
            ExerciseEntity(
                name = "Barbell Bench Press",
                category = "PUSH",
                type = "LIFT",
                bodyPart = "Chest",
                calorieBurnRate = 0.35,
                description = "Classic upper body pressing exercise using a barbell.",
                howToSteps = "Lie flat on bench.\nLower bar slowly to chest.\nPress up until arms lock.",
                impactLevel = "HIGH"
            ),
            ExerciseEntity(
                name = "Dumbbell Bench Press",
                category = "PUSH",
                type = "LIFT",
                bodyPart = "Chest",
                calorieBurnRate = 0.32,
                description = "Lie flat on a bench, pressing dumbbells upward for balanced chest growth.",
                howToSteps = "Lie on bench with dumbbells.\nLower weights to chest sides.\nPress straight up.",
                impactLevel = "MEDIUM"
            ),
            ExerciseEntity(
                name = "Barbell Overhead Press",
                category = "PUSH",
                type = "LIFT",
                bodyPart = "Shoulders",
                calorieBurnRate = 0.30,
                description = "Pressing a barbell overhead while standing to build shoulder power.",
                howToSteps = "Stand holding bar at chest.\nPress bar directly overhead.\nControl down to chest.",
                impactLevel = "HIGH"
            ),
            ExerciseEntity(
                name = "Dumbbell Overhead Press",
                category = "PUSH",
                type = "LIFT",
                bodyPart = "Shoulders",
                calorieBurnRate = 0.28,
                description = "Pressing dumbbells overhead from shoulder level to strengthen deltoids.",
                howToSteps = "Sit or stand with weights at shoulders.\nPress up together overhead.\nLower slowly.",
                impactLevel = "MEDIUM"
            ),
            ExerciseEntity(
                name = "Dumbbell Lateral Raises",
                category = "PUSH",
                type = "LIFT",
                bodyPart = "Shoulders",
                calorieBurnRate = 0.15,
                description = "Raising dumbbells out to the sides to target the lateral deltoids.",
                howToSteps = "Stand with dumbbells.\nRaise arms out to sides until parallel to floor.\nLower with control.",
                impactLevel = "LOW"
            ),
            ExerciseEntity(
                name = "Push-ups",
                category = "PUSH",
                type = "LIFT",
                bodyPart = "Chest",
                calorieBurnRate = 0.20,
                description = "Classic bodyweight calisthenic targeting chest, shoulders, and triceps.",
                howToSteps = "Plank position, hands shoulder-width.\nLower chest to floor.\nPush back up.",
                impactLevel = "MEDIUM"
            ),
            ExerciseEntity(
                name = "Dips",
                category = "PUSH",
                type = "LIFT",
                bodyPart = "Triceps",
                calorieBurnRate = 0.22,
                description = "Lowering and raising body on parallel bars to target triceps and chest.",
                howToSteps = "Grip bars, support body.\nLower body bending elbows to 90 degrees.\nPush back up.",
                impactLevel = "MEDIUM"
            ),
            ExerciseEntity(
                name = "Plank",
                category = "PUSH",
                type = "HOLD",
                bodyPart = "Core",
                calorieBurnRate = 0.15,
                description = "Static core hold building abdominal stability and endurance.",
                howToSteps = "Forearms on floor, body straight from head to heels.\nEngage core.\nHold position.",
                impactLevel = "LOW"
            ),

            // PULL
            ExerciseEntity(
                name = "Barbell Deadlift",
                category = "PULL",
                type = "LIFT",
                bodyPart = "Lower Back",
                calorieBurnRate = 0.50,
                description = "Compound pulling movement targeting the entire posterior chain.",
                howToSteps = "Stand with mid-foot under bar.\nBend and grip bar.\nDrive hips forward to stand up.\nLower bar.",
                impactLevel = "HIGH"
            ),
            ExerciseEntity(
                name = "Barbell Row",
                category = "PULL",
                type = "LIFT",
                bodyPart = "Back",
                calorieBurnRate = 0.38,
                description = "Bent-over row using a barbell to build back thickness.",
                howToSteps = "Hinge hips, back straight.\nPull bar to lower chest.\nLower bar with control.",
                impactLevel = "HIGH"
            ),
            ExerciseEntity(
                name = "Dumbbell Row",
                category = "PULL",
                type = "LIFT",
                bodyPart = "Back",
                calorieBurnRate = 0.35,
                description = "Single-arm rows on a bench targeting the latissimus dorsi.",
                howToSteps = "Hand and knee on bench, back flat.\nPull dumbbell up to hip.\nLower weight down.",
                impactLevel = "MEDIUM"
            ),
            ExerciseEntity(
                name = "Pull-ups",
                category = "PULL",
                type = "LIFT",
                bodyPart = "Back",
                calorieBurnRate = 0.40,
                description = "Bodyweight pull targeting upper back and lats.",
                howToSteps = "Grip bar wide, palms facing away.\nPull chest to bar.\nLower body slowly.",
                impactLevel = "MEDIUM"
            ),
            ExerciseEntity(
                name = "Chin-ups",
                category = "PULL",
                type = "LIFT",
                bodyPart = "Back & Biceps",
                calorieBurnRate = 0.38,
                description = "Underhand grip pull-ups focusing on lats and biceps.",
                howToSteps = "Grip bar shoulder-width, palms facing you.\nPull chest to bar.\nLower with control.",
                impactLevel = "MEDIUM"
            ),
            ExerciseEntity(
                name = "Dumbbell Bicep Curls",
                category = "PULL",
                type = "LIFT",
                bodyPart = "Biceps",
                calorieBurnRate = 0.18,
                description = "Isolating biceps using dumbbells with a rotating grip.",
                howToSteps = "Stand holding weights.\nCurl dumbbells up rotating palms forward.\nLower slowly.",
                impactLevel = "LOW"
            ),
            ExerciseEntity(
                name = "Hollow Body Hold",
                category = "PULL",
                type = "HOLD",
                bodyPart = "Core",
                calorieBurnRate = 0.15,
                description = "Static hold forcing core tension and flat back position.",
                howToSteps = "Lie on back.\nLift legs and shoulder blades slightly off floor.\nPress lower back into floor and hold.",
                impactLevel = "LOW"
            ),

            // LOWER_BODY
            ExerciseEntity(
                name = "Barbell Squat",
                category = "LOWER_BODY",
                type = "LIFT",
                bodyPart = "Legs",
                calorieBurnRate = 0.48,
                description = "Fundamental lower body exercise for quad and glute strength.",
                howToSteps = "Barbell on back, stand shoulder-width.\nSquat down until thighs parallel to floor.\nPress back up.",
                impactLevel = "HIGH"
            ),
            ExerciseEntity(
                name = "Dumbbell Squat",
                category = "LOWER_BODY",
                type = "LIFT",
                bodyPart = "Legs",
                calorieBurnRate = 0.40,
                description = "Squatting with dumbbells held at sides for lower body development.",
                howToSteps = "Hold weights at sides.\nLower hips back and down.\nStand back up pushing through heels.",
                impactLevel = "MEDIUM"
            ),
            ExerciseEntity(
                name = "Dumbbell Romanian Deadlift",
                category = "LOWER_BODY",
                type = "LIFT",
                bodyPart = "Hamstrings",
                calorieBurnRate = 0.38,
                description = "Hinging at hips with dumbbells to target hamstrings and glutes.",
                howToSteps = "Stand with weights.\nHinge hips back, sliding weights down legs.\nSqueeze glutes and stand.",
                impactLevel = "MEDIUM"
            ),
            ExerciseEntity(
                name = "Bodyweight Squats",
                category = "LOWER_BODY",
                type = "LIFT",
                bodyPart = "Legs",
                calorieBurnRate = 0.22,
                description = "Simple bodyweight squats for leg endurance and warmups.",
                howToSteps = "Stand feet shoulder-width.\nLower hips until thighs are parallel.\nStand up.",
                impactLevel = "LOW"
            ),
            ExerciseEntity(
                name = "Side Plank",
                category = "LOWER_BODY",
                type = "HOLD",
                bodyPart = "Core",
                calorieBurnRate = 0.12,
                description = "Lateral core hold targeting the obliques and quadratus lumborum.",
                howToSteps = "Lie on side, forearm on floor.\nLift hips off floor to form straight line.\nHold.",
                impactLevel = "LOW"
            ),
            ExerciseEntity(
                name = "Running",
                category = "LOWER_BODY",
                type = "CARDIO",
                bodyPart = "Cardio",
                calorieBurnRate = 10.0, // calories per minute (base rate, we'll scale it)
                description = "Outdoor or treadmill running cardio session.",
                howToSteps = "Maintain upright posture.\nLand mid-foot.\nRun at steady pace.",
                impactLevel = "HIGH"
            ),
            ExerciseEntity(
                name = "Walking",
                category = "LOWER_BODY",
                type = "CARDIO",
                bodyPart = "Cardio",
                calorieBurnRate = 4.0, // calories per minute
                description = "Steady-state brisk walking for active recovery.",
                howToSteps = "Walk briskly.\nMove arms naturally.\nKeep shoulders relaxed.",
                impactLevel = "LOW"
            ),
            ExerciseEntity(
                name = "Cycling",
                category = "LOWER_BODY",
                type = "CARDIO",
                bodyPart = "Cardio",
                calorieBurnRate = 6.0, // calories per minute
                description = "Stationary or road cycling for cardiovascular fitness.",
                howToSteps = "Adjust seat height.\nPedal at steady cadence.\nMaintain flat back.",
                impactLevel = "MEDIUM"
            ),
            ExerciseEntity(
                name = "Elliptical Trainer (EFX)",
                category = "LOWER_BODY",
                type = "CARDIO",
                bodyPart = "Cardio",
                calorieBurnRate = 7.0, // calories per minute
                description = "Low-impact full-body cardio using elliptical motion.",
                howToSteps = "Stand on pedals, hold handles.\nMove feet in smooth elliptical path.\nPull/push handles.",
                impactLevel = "MEDIUM"
            )
        )
    }
}
