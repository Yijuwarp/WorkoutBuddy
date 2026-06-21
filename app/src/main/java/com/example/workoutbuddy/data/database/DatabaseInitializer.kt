package com.example.workoutbuddy.data.database

object DatabaseInitializer {
    fun getSeedExercises(): List<ExerciseEntity> {
        return listOf(

            // ==================== PUSH ====================

            ExerciseEntity(
                name = "Barbell Bench Press",
                category = "PUSH",
                type = "LIFT",
                bodyPart = "Chest",
                calorieBurnRate = 0.35,
                description = "The king of chest exercises. Lie flat on a bench and press a barbell from chest to full extension. Targets pectorals, front deltoids, and triceps.",
                howToSteps = "Set up a barbell on the rack at arm's length height.\nLie flat on the bench, eyes under the bar.\nGrip the bar slightly wider than shoulder-width.\nUnrack the bar, lower it slowly to mid-chest.\nPress up explosively until arms are fully extended.\nRe-rack the bar after completing reps.",
                impactLevel = "HIGH",
                youtubeUrl = "https://www.youtube.com/watch?v=vcBig73ojpE"
            ),
            ExerciseEntity(
                name = "Incline Barbell Bench Press",
                category = "PUSH",
                type = "LIFT",
                bodyPart = "Upper Chest",
                calorieBurnRate = 0.34,
                description = "Pressing a barbell on an inclined bench to emphasize the upper pectorals and front deltoids.",
                howToSteps = "Set bench to 30-45 degree incline.\nGrip barbell slightly wider than shoulder-width.\nUnrack and lower bar to upper chest.\nPress up in a slight arc back toward the rack.\nControl the descent, repeat.",
                impactLevel = "HIGH",
                youtubeUrl = "https://www.youtube.com/watch?v=DbFgADa2PL8"
            ),
            ExerciseEntity(
                name = "Dumbbell Bench Press",
                category = "PUSH",
                type = "LIFT",
                bodyPart = "Chest",
                calorieBurnRate = 0.32,
                description = "Lie flat on a bench, pressing dumbbells upward for balanced chest growth with greater range of motion.",
                howToSteps = "Lie on bench with a dumbbell in each hand at chest level.\nPress both dumbbells directly upward until arms are extended.\nSlowly lower them back to chest level.\nKeep wrists straight throughout.",
                impactLevel = "MEDIUM",
                youtubeUrl = "https://www.youtube.com/watch?v=QsYre__-aro"
            ),
            ExerciseEntity(
                name = "Incline Dumbbell Press",
                category = "PUSH",
                type = "LIFT",
                bodyPart = "Upper Chest",
                calorieBurnRate = 0.30,
                description = "Pressing dumbbells on an inclined bench to build the upper chest with balanced strength.",
                howToSteps = "Set bench to 30-45 degrees.\nSit with dumbbells on thighs, then lean back as you raise them to chest.\nPress up and slightly inward.\nLower with control back to starting position.",
                impactLevel = "MEDIUM",
                youtubeUrl = "https://www.youtube.com/watch?v=8iPEnn-ltC8"
            ),
            ExerciseEntity(
                name = "Cable Chest Fly",
                category = "PUSH",
                type = "LIFT",
                bodyPart = "Chest",
                calorieBurnRate = 0.20,
                description = "Standing cable fly to isolate the pectoral muscles with constant tension throughout the movement.",
                howToSteps = "Set both cable pulleys at high position.\nGrab handles with palms facing inward.\nStep forward into a split stance.\nWith a slight bend in the elbow, sweep arms forward and together.\nSlowly return to start.",
                impactLevel = "LOW",
                youtubeUrl = "https://www.youtube.com/watch?v=taI4XduLpTk"
            ),
            ExerciseEntity(
                name = "Pec Deck Machine",
                category = "PUSH",
                type = "LIFT",
                bodyPart = "Chest",
                calorieBurnRate = 0.18,
                description = "Machine fly that isolates the chest with a fixed arc of motion for beginners and pre-exhaust work.",
                howToSteps = "Sit with back against pad, forearms against arm pads.\nBring arms together in front of you.\nSqueeze chest at peak contraction.\nSlowly open arms back to start.",
                impactLevel = "LOW",
                youtubeUrl = "https://www.youtube.com/watch?v=Z57CtFmRMxA"
            ),
            ExerciseEntity(
                name = "Barbell Overhead Press",
                category = "PUSH",
                type = "LIFT",
                bodyPart = "Shoulders",
                calorieBurnRate = 0.30,
                description = "The fundamental standing overhead press with a barbell to build raw shoulder power and core stability.",
                howToSteps = "Stand with barbell at shoulder height, grip shoulder-width.\nBrace core and press bar directly overhead.\nLock out arms at top.\nLower bar in a controlled arc back to shoulders.",
                impactLevel = "HIGH",
                youtubeUrl = "https://www.youtube.com/watch?v=2yjwXTZQDDI"
            ),
            ExerciseEntity(
                name = "Dumbbell Shoulder Press",
                category = "PUSH",
                type = "LIFT",
                bodyPart = "Shoulders",
                calorieBurnRate = 0.28,
                description = "Pressing dumbbells overhead from shoulder level. Allows a more natural pressing arc and greater range of motion.",
                howToSteps = "Sit or stand holding dumbbells at shoulder height, palms facing forward.\nPress both dumbbells up until arms are fully extended.\nLower back to shoulder level with control.",
                impactLevel = "MEDIUM",
                youtubeUrl = "https://www.youtube.com/watch?v=qEwKCR5JCog"
            ),
            ExerciseEntity(
                name = "Dumbbell Lateral Raises",
                category = "PUSH",
                type = "LIFT",
                bodyPart = "Shoulders",
                calorieBurnRate = 0.15,
                description = "Isolates the lateral head of the deltoid to build broader, wider shoulders.",
                howToSteps = "Stand with dumbbells at sides, slight bend in elbows.\nRaise both arms out to sides until parallel to the floor.\nPause briefly at the top.\nLower with control. Avoid swinging.",
                impactLevel = "LOW",
                youtubeUrl = "https://www.youtube.com/watch?v=3VcKaXpzqRo"
            ),
            ExerciseEntity(
                name = "Dumbbell Front Raises",
                category = "PUSH",
                type = "LIFT",
                bodyPart = "Front Deltoids",
                calorieBurnRate = 0.14,
                description = "Targets the anterior deltoid by raising dumbbells forward to shoulder height.",
                howToSteps = "Stand with dumbbells in front of thighs.\nRaise one or both arms forward to shoulder height.\nKeep a slight bend in the elbows.\nLower with control.",
                impactLevel = "LOW",
                youtubeUrl = "https://www.youtube.com/watch?v=soxrZlIl35U"
            ),
            ExerciseEntity(
                name = "Arnold Press",
                category = "PUSH",
                type = "LIFT",
                bodyPart = "Shoulders",
                calorieBurnRate = 0.26,
                description = "A rotational dumbbell press that hits all three heads of the deltoid in one movement, popularized by Arnold Schwarzenegger.",
                howToSteps = "Sit holding dumbbells at chin height, palms facing you.\nAs you press up, rotate palms to face forward.\nFully extend arms at the top.\nReverse the rotation as you lower.",
                impactLevel = "MEDIUM",
                youtubeUrl = "https://www.youtube.com/watch?v=vj2w851ZHRM"
            ),
            ExerciseEntity(
                name = "Tricep Pushdown (Cable)",
                category = "PUSH",
                type = "LIFT",
                bodyPart = "Triceps",
                calorieBurnRate = 0.16,
                description = "Cable pushdown to isolate the triceps with constant tension throughout the entire rep.",
                howToSteps = "Attach a bar or rope to a high cable pulley.\nGrip the attachment, elbows at sides.\nPush down until arms are fully extended.\nSqueeze triceps at the bottom.\nSlowly return to start.",
                impactLevel = "LOW",
                youtubeUrl = "https://www.youtube.com/watch?v=2-LAMcpzODU"
            ),
            ExerciseEntity(
                name = "Overhead Tricep Extension",
                category = "PUSH",
                type = "LIFT",
                bodyPart = "Triceps",
                calorieBurnRate = 0.17,
                description = "Overhead extension using a dumbbell or cable to fully stretch and target the long head of the tricep.",
                howToSteps = "Hold one dumbbell with both hands overhead.\nKeep elbows close to head, pointing forward.\nLower the dumbbell behind your head.\nExtend arms back to full extension.",
                impactLevel = "LOW",
                youtubeUrl = "https://www.youtube.com/watch?v=_gsUck-7M74"
            ),
            ExerciseEntity(
                name = "Skull Crushers",
                category = "PUSH",
                type = "LIFT",
                bodyPart = "Triceps",
                calorieBurnRate = 0.18,
                description = "Lying barbell or EZ-bar extension that brutally isolates the triceps for mass building.",
                howToSteps = "Lie on a bench holding an EZ-bar above your chest, arms extended.\nKeeping upper arms fixed, hinge elbows to lower the bar toward your forehead.\nExtend arms back to full extension.",
                impactLevel = "MEDIUM",
                youtubeUrl = "https://www.youtube.com/watch?v=d_KZxkY_0cM"
            ),
            ExerciseEntity(
                name = "Push-ups",
                category = "PUSH",
                type = "LIFT",
                bodyPart = "Chest",
                calorieBurnRate = 0.20,
                description = "The foundational bodyweight pushing movement targeting chest, shoulders, and triceps.",
                howToSteps = "Start in a plank position, hands shoulder-width apart.\nLower your chest to just above the floor by bending elbows.\nPush back up to starting position.\nKeep body in a straight line throughout.",
                impactLevel = "MEDIUM",
                youtubeUrl = "https://www.youtube.com/watch?v=IODxDxX7oi4"
            ),
            ExerciseEntity(
                name = "Dips",
                category = "PUSH",
                type = "LIFT",
                bodyPart = "Triceps",
                calorieBurnRate = 0.22,
                description = "Parallel bar dips: a powerful compound bodyweight movement for triceps and chest.",
                howToSteps = "Grip parallel bars and support your body with arms extended.\nLower your body by bending elbows until upper arms are parallel to floor.\nPush back up to starting position.\nLean forward slightly to emphasize chest, stay upright for triceps.",
                impactLevel = "MEDIUM",
                youtubeUrl = "https://www.youtube.com/watch?v=2z8JmcrW-As"
            ),
            ExerciseEntity(
                name = "Plank",
                category = "PUSH",
                type = "HOLD",
                bodyPart = "Core",
                calorieBurnRate = 0.15,
                description = "The fundamental isometric core hold that builds deep abdominal stability and total-body tension.",
                howToSteps = "Place forearms on floor, elbows under shoulders.\nExtend legs behind you, toes on floor.\nKeep body in a perfectly straight line from head to heels.\nEngage core and glutes throughout. Hold.",
                impactLevel = "LOW",
                youtubeUrl = "https://www.youtube.com/watch?v=pSHjTRCQxIw"
            ),

            // ==================== PULL ====================

            ExerciseEntity(
                name = "Barbell Deadlift",
                category = "PULL",
                type = "LIFT",
                bodyPart = "Full Body",
                calorieBurnRate = 0.50,
                description = "The most fundamental compound lift. Pulling a loaded barbell from the floor engages the entire posterior chain.",
                howToSteps = "Stand with mid-foot under the bar, feet hip-width.\nHinge at hips, grip bar just outside your legs.\nFlatten your back, take a deep breath and brace.\nDrive through the floor with legs as the bar passes the knees, lock hips forward.\nLower bar by hinging at hips, then bend knees once it passes them.",
                impactLevel = "HIGH",
                youtubeUrl = "https://www.youtube.com/watch?v=op9kVnSso6Q"
            ),
            ExerciseEntity(
                name = "Romanian Deadlift",
                category = "PULL",
                type = "LIFT",
                bodyPart = "Hamstrings",
                calorieBurnRate = 0.40,
                description = "Hip-hinge dominant deadlift variation that maximally targets hamstrings and glutes through a long range of motion.",
                howToSteps = "Hold barbell at hip level, feet hip-width.\nKeeping knees slightly bent, hinge at hips pushing them back.\nSlide bar down your legs, keeping back flat.\nFeel a deep hamstring stretch at the bottom.\nDrive hips forward to return to standing.",
                impactLevel = "HIGH",
                youtubeUrl = "https://www.youtube.com/watch?v=hCDzSR6bW10"
            ),
            ExerciseEntity(
                name = "Barbell Row",
                category = "PULL",
                type = "LIFT",
                bodyPart = "Back",
                calorieBurnRate = 0.38,
                description = "Bent-over barbell row for building back thickness. The primary mass-building exercise for the lats and rhomboids.",
                howToSteps = "Stand with barbell, hinge forward until torso is nearly parallel to floor.\nKeep back flat and core braced.\nPull bar to your lower chest/upper abdomen.\nSqueeze shoulder blades together at the top.\nLower the bar with control.",
                impactLevel = "HIGH",
                youtubeUrl = "https://www.youtube.com/watch?v=FWJR5Ve8bnQ"
            ),
            ExerciseEntity(
                name = "Dumbbell Row",
                category = "PULL",
                type = "LIFT",
                bodyPart = "Back",
                calorieBurnRate = 0.35,
                description = "Single-arm dumbbell row on a bench. Great for correcting strength imbalances and targeting the lat.",
                howToSteps = "Place one hand and same-side knee on a flat bench.\nHold a dumbbell in the other hand, letting it hang.\nPull the dumbbell up toward your hip, elbow close to body.\nSqueeze at the top, lower with control.",
                impactLevel = "MEDIUM",
                youtubeUrl = "https://www.youtube.com/watch?v=pYcpY20QaE8"
            ),
            ExerciseEntity(
                name = "Lat Pulldown",
                category = "PULL",
                type = "LIFT",
                bodyPart = "Lats",
                calorieBurnRate = 0.30,
                description = "Cable machine exercise that simulates a pull-up, excellent for building wide lats and improving pull-up strength.",
                howToSteps = "Sit at the lat pulldown machine, thighs secured under pad.\nGrip the bar wider than shoulder-width, palms facing away.\nLean back slightly and pull the bar down to your upper chest.\nSqueeze your lats at the bottom.\nSlowly extend arms back to the top.",
                impactLevel = "MEDIUM",
                youtubeUrl = "https://www.youtube.com/watch?v=CAwf7n6Luuc"
            ),
            ExerciseEntity(
                name = "Close-Grip Lat Pulldown",
                category = "PULL",
                type = "LIFT",
                bodyPart = "Lats",
                calorieBurnRate = 0.28,
                description = "Neutral-grip lat pulldown variation that emphasizes lower lats and allows for a stronger pulling position.",
                howToSteps = "Attach a V-bar or close-grip handle.\nSit with thighs secured, grip the handle.\nPull down to upper chest, elbows travelling straight down.\nSqueeze lats at bottom, extend arms slowly.",
                impactLevel = "MEDIUM",
                youtubeUrl = "https://www.youtube.com/watch?v=jdhf0oKZWE8"
            ),
            ExerciseEntity(
                name = "Seated Cable Row",
                category = "PULL",
                type = "LIFT",
                bodyPart = "Back",
                calorieBurnRate = 0.28,
                description = "Seated cable row with a V-bar to build mid-back thickness and improve posture.",
                howToSteps = "Sit at the cable row machine, feet on the footrests.\nGrip the V-bar handle, sit tall.\nPull the handle to your lower abdomen, driving elbows back.\nSqueeze shoulder blades at the end.\nExtend arms forward with a slight forward lean.",
                impactLevel = "MEDIUM",
                youtubeUrl = "https://www.youtube.com/watch?v=GZbfZ033f74"
            ),
            ExerciseEntity(
                name = "Pull-ups",
                category = "PULL",
                type = "LIFT",
                bodyPart = "Back",
                calorieBurnRate = 0.40,
                description = "The gold standard of bodyweight pulling exercises. Overhand grip pull-up for lat width and upper back development.",
                howToSteps = "Hang from a bar with a wide overhand grip.\nPull your chest up toward the bar, leading with your elbows.\nChin clears the bar at the top.\nLower yourself with full control to a dead hang.",
                impactLevel = "MEDIUM",
                youtubeUrl = "https://www.youtube.com/watch?v=eGo4IYlbE5g"
            ),
            ExerciseEntity(
                name = "Chin-ups",
                category = "PULL",
                type = "LIFT",
                bodyPart = "Back & Biceps",
                calorieBurnRate = 0.38,
                description = "Underhand grip pull-up that places more emphasis on the biceps and lower lats.",
                howToSteps = "Hang from a bar with a shoulder-width underhand grip.\nPull yourself up until your chin is above the bar.\nSqueeze at the top.\nLower with control to a dead hang.",
                impactLevel = "MEDIUM",
                youtubeUrl = "https://www.youtube.com/watch?v=e1YSApl-QcM"
            ),
            ExerciseEntity(
                name = "Face Pulls",
                category = "PULL",
                type = "LIFT",
                bodyPart = "Rear Deltoids",
                calorieBurnRate = 0.14,
                description = "Cable rear-delt exercise that improves posture, shoulder health, and develops the posterior deltoid.",
                howToSteps = "Set a rope attachment at face height on a cable machine.\nGrip both ends with pronated hands.\nPull the rope toward your face, flaring elbows out.\nSeparate the rope ends at your ears at the peak.\nReturn with control.",
                impactLevel = "LOW",
                youtubeUrl = "https://www.youtube.com/watch?v=rep-qVOkqgk"
            ),
            ExerciseEntity(
                name = "Dumbbell Bicep Curls",
                category = "PULL",
                type = "LIFT",
                bodyPart = "Biceps",
                calorieBurnRate = 0.18,
                description = "The classic bicep isolation exercise using dumbbells with a supinating grip for a full contraction.",
                howToSteps = "Stand holding dumbbells at your sides, palms facing forward.\nCurl the weights up toward your shoulders, rotating palms as you lift.\nSqueeze biceps at the top.\nLower slowly.",
                impactLevel = "LOW",
                youtubeUrl = "https://www.youtube.com/watch?v=ykJmrZ5v0Oo"
            ),
            ExerciseEntity(
                name = "Barbell Bicep Curl",
                category = "PULL",
                type = "LIFT",
                bodyPart = "Biceps",
                calorieBurnRate = 0.20,
                description = "The heavy barbell curl for maximum bicep overload and strength building.",
                howToSteps = "Stand with a barbell, underhand grip shoulder-width apart.\nKeeping elbows at sides, curl the bar up to shoulder level.\nSqueeze at the top.\nLower slowly in a controlled manner.",
                impactLevel = "LOW",
                youtubeUrl = "https://www.youtube.com/watch?v=kwG2ipFRgfo"
            ),
            ExerciseEntity(
                name = "Hammer Curls",
                category = "PULL",
                type = "LIFT",
                bodyPart = "Biceps & Brachialis",
                calorieBurnRate = 0.17,
                description = "Neutral-grip curl that targets the brachialis and brachioradialis, building arm thickness.",
                howToSteps = "Hold dumbbells at sides with neutral grip (palms facing body).\nCurl weights up keeping palms facing each other throughout.\nPeak contraction at shoulder height.\nLower with control.",
                impactLevel = "LOW",
                youtubeUrl = "https://www.youtube.com/watch?v=zC3nLlEvin4"
            ),
            ExerciseEntity(
                name = "Cable Bicep Curl",
                category = "PULL",
                type = "LIFT",
                bodyPart = "Biceps",
                calorieBurnRate = 0.16,
                description = "Cable curl provides constant tension on the bicep throughout the entire range of motion.",
                howToSteps = "Attach a straight bar or EZ-bar to a low cable pulley.\nStand facing the machine and curl the bar up to shoulder level.\nSqueeze biceps at top.\nLower with controlled resistance.",
                impactLevel = "LOW",
                youtubeUrl = "https://www.youtube.com/watch?v=NFzTWp2qpiE"
            ),
            ExerciseEntity(
                name = "Preacher Curl",
                category = "PULL",
                type = "LIFT",
                bodyPart = "Biceps",
                calorieBurnRate = 0.15,
                description = "Curling from a preacher bench pad isolates the bicep by eliminating body swing and cheating.",
                howToSteps = "Sit at a preacher bench, rest upper arms on the pad.\nGrip an EZ-bar or dumbbells.\nCurl the weight up to shoulder level, squeezing at the top.\nLower until arms are nearly extended.",
                impactLevel = "LOW",
                youtubeUrl = "https://www.youtube.com/watch?v=fIWP-FRFNU0"
            ),
            ExerciseEntity(
                name = "T-Bar Row",
                category = "PULL",
                type = "LIFT",
                bodyPart = "Back",
                calorieBurnRate = 0.36,
                description = "A powerful back thickness exercise using a T-bar machine or landmine attachment.",
                howToSteps = "Stand over a T-bar or landmine, straddling the bar.\nGrip the handles and hinge forward.\nRow the weight toward your chest.\nSqueeze back muscles at the top.\nLower with control.",
                impactLevel = "HIGH",
                youtubeUrl = "https://www.youtube.com/watch?v=KDEl3AmZbVE"
            ),
            ExerciseEntity(
                name = "Chest-Supported Row",
                category = "PULL",
                type = "LIFT",
                bodyPart = "Back",
                calorieBurnRate = 0.32,
                description = "Row performed lying face-down on an incline bench, eliminating lower back stress and isolating the upper back.",
                howToSteps = "Set an incline bench to 45 degrees.\nLie face-down with chest on the pad.\nGrip dumbbells and let arms hang.\nRow both dumbbells up toward your hips.\nSqueeze shoulder blades, lower slowly.",
                impactLevel = "MEDIUM",
                youtubeUrl = "https://www.youtube.com/watch?v=FTwvmczf7bE"
            ),
            ExerciseEntity(
                name = "Hollow Body Hold",
                category = "PULL",
                type = "HOLD",
                bodyPart = "Core",
                calorieBurnRate = 0.15,
                description = "Gymnastics-derived core strengthening hold that teaches full-body tension and a strong braced position.",
                howToSteps = "Lie on your back with arms extended overhead.\nPress your lower back firmly into the floor.\nLift your shoulder blades and legs slightly off the floor.\nKeep the core tight and body in a 'banana' shape. Hold.",
                impactLevel = "LOW",
                youtubeUrl = "https://www.youtube.com/watch?v=LlDNef_Ztsc"
            ),
            ExerciseEntity(
                name = "Reverse Fly (Dumbbell)",
                category = "PULL",
                type = "LIFT",
                bodyPart = "Rear Deltoids",
                calorieBurnRate = 0.14,
                description = "Bent-over dumbbell fly that targets the rear deltoids and upper back for better posture.",
                howToSteps = "Hinge forward at hips until torso is nearly horizontal.\nHold dumbbells hanging below chest.\nWith slight bend in elbows, raise arms out to sides.\nSqueeze rear delts at the top.\nLower with control.",
                impactLevel = "LOW",
                youtubeUrl = "https://www.youtube.com/watch?v=shYSBJOFBeM"
            ),
            ExerciseEntity(
                name = "Shrugs",
                category = "PULL",
                type = "LIFT",
                bodyPart = "Traps",
                calorieBurnRate = 0.15,
                description = "Shoulder shrug with a barbell or dumbbells to build the upper trapezius muscle.",
                howToSteps = "Hold barbell or dumbbells at arms' length in front of body.\nRaise shoulders directly upward as high as possible.\nHold briefly at the top.\nLower with control. Do not roll shoulders.",
                impactLevel = "LOW",
                youtubeUrl = "https://www.youtube.com/watch?v=cJRVVxmytaM"
            ),

            // ==================== LOWER_BODY ====================

            ExerciseEntity(
                name = "Barbell Back Squat",
                category = "LOWER_BODY",
                type = "LIFT",
                bodyPart = "Legs",
                calorieBurnRate = 0.48,
                description = "The king of lower body exercises. Bar on your upper back, squat to parallel to build quads, glutes, and overall lower body power.",
                howToSteps = "Place barbell across upper traps, feet shoulder-width.\nBreath, brace core.\nPush knees out and descend until thighs are parallel to floor.\nDrive up through heels, push knees out.",
                impactLevel = "HIGH",
                youtubeUrl = "https://www.youtube.com/watch?v=ultWZbUMPL8"
            ),
            ExerciseEntity(
                name = "Front Squat",
                category = "LOWER_BODY",
                type = "LIFT",
                bodyPart = "Quads",
                calorieBurnRate = 0.45,
                description = "Barbell squat with the bar held in a front rack position to emphasize the quadriceps and maintain a more upright torso.",
                howToSteps = "Rest barbell on front delts with elbows forward.\nFeet shoulder-width, toes slightly out.\nBreath and brace, descend keeping torso upright.\nDrive up through the whole foot.",
                impactLevel = "HIGH",
                youtubeUrl = "https://www.youtube.com/watch?v=uYumuL_G_V0"
            ),
            ExerciseEntity(
                name = "Goblet Squat",
                category = "LOWER_BODY",
                type = "LIFT",
                bodyPart = "Legs",
                calorieBurnRate = 0.30,
                description = "Holding a dumbbell or kettlebell at the chest while squatting. Great for beginners and high-rep work.",
                howToSteps = "Hold a dumbbell vertically at chest height.\nFeet shoulder-width, toes slightly out.\nSquat down keeping the dumbbell close to your body.\nDrive heels into floor to stand.",
                impactLevel = "MEDIUM",
                youtubeUrl = "https://www.youtube.com/watch?v=MxsFDhcyFyE"
            ),
            ExerciseEntity(
                name = "Leg Press",
                category = "LOWER_BODY",
                type = "LIFT",
                bodyPart = "Quads",
                calorieBurnRate = 0.38,
                description = "Machine-based leg exercise allowing heavy loading with less spinal compression than barbell squats.",
                howToSteps = "Sit on the leg press machine with back flat against pad.\nPlace feet shoulder-width on the platform.\nRelease the safety, lower the weight by bending knees to 90 degrees.\nPress through your whole foot back to start. Don't lock knees.",
                impactLevel = "HIGH",
                youtubeUrl = "https://www.youtube.com/watch?v=IZxyjW7MPJQ"
            ),
            ExerciseEntity(
                name = "Hack Squat (Machine)",
                category = "LOWER_BODY",
                type = "LIFT",
                bodyPart = "Quads",
                calorieBurnRate = 0.40,
                description = "Machine squat that places the load through a fixed plane, heavily targeting the quadriceps.",
                howToSteps = "Place feet on the hack squat platform.\nShoulders under the pads, disengage safety.\nDescend by bending knees, keeping back against the pad.\nPush through feet to return.",
                impactLevel = "HIGH",
                youtubeUrl = "https://www.youtube.com/watch?v=EdtPAT_1Mwg"
            ),
            ExerciseEntity(
                name = "Walking Lunges",
                category = "LOWER_BODY",
                type = "LIFT",
                bodyPart = "Legs",
                calorieBurnRate = 0.30,
                description = "Dynamic lunge variation building unilateral leg strength, balance, and glute activation.",
                howToSteps = "Stand holding dumbbells at your sides.\nStep forward with one foot, lowering back knee toward the floor.\nPush off the front foot to bring rear foot forward.\nAlternate legs with each step.",
                impactLevel = "MEDIUM",
                youtubeUrl = "https://www.youtube.com/watch?v=Pbmj6xPo-Hw"
            ),
            ExerciseEntity(
                name = "Bulgarian Split Squat",
                category = "LOWER_BODY",
                type = "LIFT",
                bodyPart = "Quads & Glutes",
                calorieBurnRate = 0.35,
                description = "Single-leg squat with the rear foot elevated. One of the most effective unilateral leg exercises.",
                howToSteps = "Stand facing away from a bench, rear foot elevated on it.\nHold dumbbells at sides.\nLower your back knee toward the floor.\nKeep torso upright and drive up through front heel.",
                impactLevel = "HIGH",
                youtubeUrl = "https://www.youtube.com/watch?v=2C-uNgKwPLE"
            ),
            ExerciseEntity(
                name = "Dumbbell Romanian Deadlift",
                category = "LOWER_BODY",
                type = "LIFT",
                bodyPart = "Hamstrings",
                calorieBurnRate = 0.38,
                description = "Hip-hinge movement with dumbbells targeting the hamstrings and glutes through a long range of motion.",
                howToSteps = "Stand with dumbbells at hip level.\nHinge at hips, pushing them back, sliding weights down legs.\nFeel deep hamstring stretch.\nSqueeze glutes and drive hips forward to stand.",
                impactLevel = "MEDIUM",
                youtubeUrl = "https://www.youtube.com/watch?v=QbbURJEUALw"
            ),
            ExerciseEntity(
                name = "Leg Curl (Machine)",
                category = "LOWER_BODY",
                type = "LIFT",
                bodyPart = "Hamstrings",
                calorieBurnRate = 0.22,
                description = "Seated or lying leg curl machine to isolate the hamstrings.",
                howToSteps = "Lie or sit in the machine with pads behind ankles.\nCurl your legs toward your glutes.\nSqueeze hamstrings at peak contraction.\nLower with control. Don't let weight slam.",
                impactLevel = "LOW",
                youtubeUrl = "https://www.youtube.com/watch?v=1Tq3QdYUuHs"
            ),
            ExerciseEntity(
                name = "Leg Extension (Machine)",
                category = "LOWER_BODY",
                type = "LIFT",
                bodyPart = "Quads",
                calorieBurnRate = 0.20,
                description = "Machine-based quad isolation exercise. Great for pre-exhaust or finishing sets.",
                howToSteps = "Sit in the machine with pads over your ankles.\nExtend legs until fully straight.\nSqueeze quads at the top.\nLower weight back down with control.",
                impactLevel = "LOW",
                youtubeUrl = "https://www.youtube.com/watch?v=ljO4jkwv8fQ"
            ),
            ExerciseEntity(
                name = "Hip Thrust",
                category = "LOWER_BODY",
                type = "LIFT",
                bodyPart = "Glutes",
                calorieBurnRate = 0.28,
                description = "The premier glute-building exercise. Drive a loaded barbell upward using glute power while back is on a bench.",
                howToSteps = "Sit on the floor with your upper back against a bench.\nRoll a barbell over your hips with a pad.\nFeet flat on the floor, hip-width.\nDrive hips up by squeezing glutes.\nPause at the top, lower with control.",
                impactLevel = "MEDIUM",
                youtubeUrl = "https://www.youtube.com/watch?v=xDmFkJxPzeM"
            ),
            ExerciseEntity(
                name = "Glute Bridge",
                category = "LOWER_BODY",
                type = "LIFT",
                bodyPart = "Glutes",
                calorieBurnRate = 0.18,
                description = "Bodyweight or loaded glute bridge for glute activation and hip extension strength.",
                howToSteps = "Lie on your back, knees bent, feet flat on floor.\nPush through your heels to raise your hips.\nSqueeze glutes at the top.\nLower your hips back down.",
                impactLevel = "LOW",
                youtubeUrl = "https://www.youtube.com/watch?v=OUgsJ8-Vi0E"
            ),
            ExerciseEntity(
                name = "Standing Calf Raise",
                category = "LOWER_BODY",
                type = "LIFT",
                bodyPart = "Calves",
                calorieBurnRate = 0.14,
                description = "Raise up on your toes to build the gastrocnemius calf muscle.",
                howToSteps = "Stand on the edge of a step or flat ground.\nRise up on your toes as high as possible.\nHold briefly at the top.\nLower your heels as far as comfortable below step level.",
                impactLevel = "LOW",
                youtubeUrl = "https://www.youtube.com/watch?v=SorIB5_zO9A"
            ),
            ExerciseEntity(
                name = "Seated Calf Raise",
                category = "LOWER_BODY",
                type = "LIFT",
                bodyPart = "Calves",
                calorieBurnRate = 0.12,
                description = "Machine or dumbbell calf raise done seated to target the soleus muscle.",
                howToSteps = "Sit with weight on your lower thighs.\nRaise your heels by pressing on the balls of your feet.\nFull extension at the top, full stretch at the bottom.",
                impactLevel = "LOW",
                youtubeUrl = "https://www.youtube.com/watch?v=JbyjNymZOt0"
            ),
            ExerciseEntity(
                name = "Sumo Deadlift",
                category = "LOWER_BODY",
                type = "LIFT",
                bodyPart = "Legs & Back",
                calorieBurnRate = 0.46,
                description = "Wide-stance deadlift variation that places more emphasis on the inner thighs, glutes, and quads.",
                howToSteps = "Stand with feet wide, toes pointing outward.\nGrip the bar inside your legs.\nKeep chest tall and back flat.\nDrive through heels and push knees outward as you stand.\nLower with control.",
                impactLevel = "HIGH",
                youtubeUrl = "https://www.youtube.com/watch?v=JbY72Him34Q"
            ),
            ExerciseEntity(
                name = "Bodyweight Squats",
                category = "LOWER_BODY",
                type = "LIFT",
                bodyPart = "Legs",
                calorieBurnRate = 0.22,
                description = "Simple unloaded squat for warm-ups, high-rep conditioning, or beginners.",
                howToSteps = "Stand feet shoulder-width, toes slightly out.\nLower hips until thighs parallel to floor.\nKeep chest up and knees over toes.\nStand back up.",
                impactLevel = "LOW",
                youtubeUrl = "https://www.youtube.com/watch?v=aclHkVaku9U"
            ),
            ExerciseEntity(
                name = "Step-ups",
                category = "LOWER_BODY",
                type = "LIFT",
                bodyPart = "Quads & Glutes",
                calorieBurnRate = 0.25,
                description = "Unilateral exercise stepping up onto a box to build quad and glute strength.",
                howToSteps = "Stand in front of a sturdy bench or box.\nStep up with one foot, pressing through heel to stand on the box.\nBring other foot up.\nStep back down one foot at a time. Alternate lead leg.",
                impactLevel = "LOW",
                youtubeUrl = "https://www.youtube.com/watch?v=dQqApCGd5Ss"
            ),
            ExerciseEntity(
                name = "Side Plank",
                category = "LOWER_BODY",
                type = "HOLD",
                bodyPart = "Core",
                calorieBurnRate = 0.12,
                description = "Lateral isometric hold targeting the obliques and lateral core stabilizers.",
                howToSteps = "Lie on your side with forearm on the floor, elbow under shoulder.\nStack your feet or place one in front.\nLift your hips off the floor to form a straight line.\nHold, keeping core tight and hips elevated.",
                impactLevel = "LOW",
                youtubeUrl = "https://www.youtube.com/watch?v=XeN4pEZZJNI"
            ),
            ExerciseEntity(
                name = "Ab Wheel Rollout",
                category = "LOWER_BODY",
                type = "LIFT",
                bodyPart = "Core",
                calorieBurnRate = 0.20,
                description = "One of the most effective core exercises. Rolling out with an ab wheel challenges anti-extension core strength.",
                howToSteps = "Kneel on the floor holding an ab wheel.\nRoll forward, extending your body toward the floor.\nGo as far as you can while maintaining tension.\nPull back using your core, not your arms.",
                impactLevel = "MEDIUM",
                youtubeUrl = "https://www.youtube.com/watch?v=rqiTPdK1c_I"
            ),

            // ==================== CARDIO ====================

            ExerciseEntity(
                name = "Running",
                category = "LOWER_BODY",
                type = "CARDIO",
                bodyPart = "Cardio",
                calorieBurnRate = 10.0,
                description = "Steady-state or interval running for cardiovascular conditioning and calorie burn.",
                howToSteps = "Warm up with a brisk walk for 5 minutes.\nMaintain an upright posture with a slight forward lean.\nLand mid-foot, not heel.\nBreath rhythmically.\nCool down with a slow jog and stretch.",
                impactLevel = "HIGH",
                youtubeUrl = "https://www.youtube.com/watch?v=_kGESn8ArrU"
            ),
            ExerciseEntity(
                name = "Walking",
                category = "LOWER_BODY",
                type = "CARDIO",
                bodyPart = "Cardio",
                calorieBurnRate = 4.0,
                description = "Steady brisk walking for active recovery, low-impact cardio, and daily step count.",
                howToSteps = "Walk at a brisk, purposeful pace.\nSwing arms naturally.\nKeep shoulders relaxed and down.\nMaintain upright posture.",
                impactLevel = "LOW",
                youtubeUrl = "https://www.youtube.com/watch?v=ZllXIKITzfg"
            ),
            ExerciseEntity(
                name = "Cycling",
                category = "LOWER_BODY",
                type = "CARDIO",
                bodyPart = "Cardio",
                calorieBurnRate = 6.0,
                description = "Stationary bike or road cycling for lower-body cardio with minimal joint impact.",
                howToSteps = "Adjust seat height so knee has a slight bend at bottom of pedal stroke.\nPedal at a steady cadence of 70-90 RPM.\nMaintain a flat back and slight forward lean.\nBreathe consistently throughout.",
                impactLevel = "MEDIUM",
                youtubeUrl = "https://www.youtube.com/watch?v=YeHSHwBXbAU"
            ),
            ExerciseEntity(
                name = "Elliptical Trainer (EFX)",
                category = "LOWER_BODY",
                type = "CARDIO",
                bodyPart = "Cardio",
                calorieBurnRate = 7.0,
                description = "Low-impact full-body cardio machine that mimics running motion without the ground impact.",
                howToSteps = "Step onto pedals and grip the handles.\nMove feet in a smooth elliptical motion.\nPush and pull the handles to engage upper body.\nMaintain upright posture and steady breathing.",
                impactLevel = "MEDIUM",
                youtubeUrl = "https://www.youtube.com/watch?v=Q0O5fJjlCQQ"
            ),
            ExerciseEntity(
                name = "Rowing Machine",
                category = "LOWER_BODY",
                type = "CARDIO",
                bodyPart = "Full Body",
                calorieBurnRate = 8.0,
                description = "Full-body cardio that simulates rowing and provides one of the highest calorie burns of any cardio machine.",
                howToSteps = "Sit on the seat and strap feet in.\nStart with legs bent, arms extended, leaning slightly forward.\nDrive with legs first, then lean back, then pull handle to lower chest.\nReverse: arms extend, body forward, knees bend.",
                impactLevel = "MEDIUM",
                youtubeUrl = "https://www.youtube.com/watch?v=bqVgDaMsK8w"
            ),
            ExerciseEntity(
                name = "Jump Rope",
                category = "LOWER_BODY",
                type = "CARDIO",
                bodyPart = "Cardio",
                calorieBurnRate = 11.0,
                description = "High-intensity cardio using a jump rope to improve coordination, footwork, and cardiovascular fitness.",
                howToSteps = "Hold handles with arms slightly bent, elbows near waist.\nSpin the rope using wrists, not arms.\nJump with both feet, clearing the rope with minimal air time.\nKeep a consistent rhythm.",
                impactLevel = "HIGH",
                youtubeUrl = "https://www.youtube.com/watch?v=FJmRQ5iTXKE"
            ),
            ExerciseEntity(
                name = "Stair Climber",
                category = "LOWER_BODY",
                type = "CARDIO",
                bodyPart = "Legs",
                calorieBurnRate = 9.0,
                description = "Stair climbing machine that builds cardiovascular fitness and lower body endurance simultaneously.",
                howToSteps = "Step onto the machine and hold rails lightly for balance only.\nStep at a comfortable but challenging pace.\nMaintain upright posture, don't lean heavily on rails.\nKeep full foot contact on each step.",
                impactLevel = "MEDIUM",
                youtubeUrl = "https://www.youtube.com/watch?v=SZU9Rm0sNOo"
            ),
            ExerciseEntity(
                name = "Battle Ropes",
                category = "LOWER_BODY",
                type = "CARDIO",
                bodyPart = "Full Body",
                calorieBurnRate = 10.5,
                description = "High-intensity interval training using heavy ropes to challenge cardiovascular system and build power.",
                howToSteps = "Anchor ropes around a sturdy object. Stand in an athletic stance.\nGrip one end in each hand.\nCreate alternating waves by rapidly raising and lowering arms.\nKeep core engaged and knees slightly bent.",
                impactLevel = "HIGH",
                youtubeUrl = "https://www.youtube.com/watch?v=pQb2xIGioyQ"
            )
        )
    }
}
