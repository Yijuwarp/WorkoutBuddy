package com.example.workoutbuddy.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.workoutbuddy.audio.AppSound
import com.example.workoutbuddy.audio.Haptics
import com.example.workoutbuddy.data.Equipment
import com.example.workoutbuddy.theme.*
import com.example.workoutbuddy.ui.components.*
import com.example.workoutbuddy.ui.util.LocalSoundPlayer
import com.example.workoutbuddy.ui.util.pressScale
import com.example.workoutbuddy.viewmodel.WorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier
) {
    val activeWorkout by viewModel.activeWorkout.collectAsState()
    val isStarted by viewModel.isWorkoutStarted.collectAsState()
    val durationSec by viewModel.workoutDuration.collectAsState()
    val exercises by viewModel.activeExerciseStates.collectAsState()
    val summary by viewModel.workoutSummary.collectAsState()
    val recordBrokenCelebration by viewModel.recordBrokenCelebration.collectAsState()
    val displayedCalories by viewModel.displayedCalories.collectAsState()
    val performanceScore by viewModel.performanceScore.collectAsState()
    val userProfileForEquipment by viewModel.userProfile.collectAsState()
    val ownedEquipment = remember(userProfileForEquipment) {
        Equipment.parseCsv(userProfileForEquipment?.equipmentOwned ?: Equipment.allIdsCsv)
    }
    val exerciseUsageStats by viewModel.exerciseUsageStats.collectAsState()

    // Countdown and Cooldown states
    val cooldownExercise by viewModel.cooldownExerciseName.collectAsState()
    val cooldownRemaining by viewModel.cooldownRemaining.collectAsState()
    val cooldownTotal by viewModel.cooldownDuration.collectAsState()

    val countdownExercise by viewModel.countdownExerciseName.collectAsState()
    val countdownRemaining by viewModel.countdownRemaining.collectAsState()
    val countdownTotal by viewModel.countdownDuration.collectAsState()
    val isCountdownActive by viewModel.isCountdownActive.collectAsState()
    val isCountdownPaused by viewModel.isCountdownPaused.collectAsState()
    val allExercises by viewModel.allExercises.collectAsState()
    val exercisePreferences by viewModel.exercisePreferences.collectAsState()

    var showCategoryMenu by remember { mutableStateOf(false) }
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showReplaceDialogForExerciseId by remember { mutableStateOf<Int?>(null) }
    var selectedExerciseId by remember { mutableStateOf<Int?>(null) }
    var showRestModal by remember { mutableStateOf(false) }
    var showCountdownModal by remember { mutableStateOf(false) }
    var showIncompleteWarning by remember { mutableStateOf(false) }

    // First-open coach-mark tour state
    var tourStep by remember { mutableStateOf(0) }
    var categoryBadgeBounds by remember { mutableStateOf<Rect?>(null) }
    var refreshButtonBounds by remember { mutableStateOf<Rect?>(null) }
    var dialsBounds by remember { mutableStateOf<Rect?>(null) }
    val showTour = userProfileForEquipment?.hasSeenWorkoutTour == false && activeWorkout != null && !isStarted

    LaunchedEffect(cooldownExercise) {
        if (cooldownExercise != null) {
            showRestModal = true
        } else {
            showRestModal = false
        }
    }

    LaunchedEffect(isCountdownActive) {
        if (isCountdownActive) {
            showCountdownModal = true
        } else {
            showCountdownModal = false
        }
    }

    val selectedExerciseState = remember(selectedExerciseId, exercises) {
        exercises.find { it.exercise.id == selectedExerciseId }
    }

    // Root of this screen's own coordinate space - anchor bounds are captured in window
    // coordinates, but the overlay is laid out relative to this Box (which may itself be
    // offset from the window origin by Scaffold/status-bar padding), so anchors must be
    // translated into this Box's local space before being used to position the highlight.
    var screenRootBounds by remember { mutableStateOf<Rect?>(null) }

    WavyFloatingNumbersContainer(viewModel = viewModel) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .imePadding()
                .onGloballyPositioned { screenRootBounds = it.boundsInWindow() }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
            
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Workout Category Badge & Switcher, plus the shuffle action right beside it
                activeWorkout?.let { workout ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { if (!isStarted) showCategoryMenu = true }
                                    .onGloballyPositioned { categoryBadgeBounds = it.boundsInWindow() }
                            ) {
                                val badgeColor = when (workout.category) {
                                    "PUSH" -> BluePrimary
                                    "PULL" -> GreenSuccess
                                    "LOWER_BODY" -> GoldPR
                                    "CARDIO" -> PerformanceRed
                                    else -> BlueSecondary
                                }
                                Text(
                                    text = workout.category.replace("_", " "),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                    color = badgeColor
                                )
                                if (!isStarted) {
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = "Change category",
                                        tint = TextMuted,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showCategoryMenu,
                                onDismissRequest = { showCategoryMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("PUSH Workout", color = BluePrimary, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        viewModel.loadOrGenerateActiveWorkout("PUSH")
                                        showCategoryMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("PULL Workout", color = GreenSuccess, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        viewModel.loadOrGenerateActiveWorkout("PULL")
                                        showCategoryMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("LOWER BODY Workout", color = GoldPR, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        viewModel.loadOrGenerateActiveWorkout("LOWER_BODY")
                                        showCategoryMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("CARDIO Workout", color = PerformanceRed, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        viewModel.loadOrGenerateActiveWorkout("CARDIO")
                                        showCategoryMenu = false
                                    }
                                )
                            }
                        }

                        if (!isStarted) {
                            IconButton(
                                onClick = { viewModel.shuffleActiveWorkout() },
                                modifier = Modifier.onGloballyPositioned { refreshButtonBounds = it.boundsInWindow() }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Shuffle workout",
                                    tint = TextMuted
                                )
                            }
                        }
                    }
                }

                // Workout Ticker Timer Box
                val isPaused by viewModel.isTimerPaused.collectAsState()
                val timerBgColor = if (isStarted && isPaused) AmberWarningBgStrong else LightBlueContainer
                val timerTextColor = if (isStarted) {
                    if (isPaused) GoldPR else BluePrimary
                } else {
                    TextDark
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(timerBgColor)
                        .clickable(enabled = isStarted) {
                            viewModel.toggleWorkoutTimer()
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = when {
                            !isStarted -> Icons.Default.Timer
                            isPaused -> Icons.Default.PlayArrow
                            else -> Icons.Default.Pause
                        },
                        contentDescription = "Timer Icon",
                        tint = if (isStarted) timerTextColor else BluePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formatDuration(durationSec),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, fontSize = 24.sp),
                        color = timerTextColor
                    )
                }
            }

            // Exercise List (Dial is first scrollable item, not sticky)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dashboard Intensity & Burn Dials — scrolls with list
                item {
                    activeWorkout?.let { workout ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val allSets = remember(exercises) { exercises.flatMap { it.sets } }
                                val completedSetsCount = allSets.count { it.isCompleted }

                                PerformanceHeroCard(
                                    score = performanceScore,
                                    burnedCalories = displayedCalories,
                                    completedSets = completedSetsCount,
                                    totalSets = allSets.size,
                                    modifier = Modifier
                                        .onGloballyPositioned { dialsBounds = it.boundsInWindow() }
                                )
                            }
                        }
                    }
                }
                if (exercises.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = BluePrimary)
                        }
                    }
                } else {
                    items(exercises) { exerciseState ->
                        ExerciseListItem(
                            exerciseState = exerciseState,
                            isWorkoutStarted = isStarted,
                            onReplaceExercise = {
                                showReplaceDialogForExerciseId = exerciseState.exercise.id
                            },
                            onRemoveExercise = {
                                viewModel.removeExerciseFromWorkout(exerciseState.exercise.id)
                            },
                            onClick = {
                                viewModel.onExerciseScreenOpened()
                                selectedExerciseId = exerciseState.exercise.id
                            },
                            currentFrequency = exercisePreferences[exerciseState.exercise.id],
                            onSetFrequency = { freq ->
                                viewModel.setExerciseFrequency(exerciseState.exercise.id, freq)
                            }
                        )
                    }

                    // Add Exercise button
                    item {
                        Button(
                            onClick = { showAddExerciseDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LightBlueContainer,
                                contentColor = BluePrimary
                            ),
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.5.dp, BluePrimary.copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = BluePrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Exercise", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, color = BluePrimary)
                        }
                    }
                }
            }
        }

        // Rest Timer Modal (shows on top of everything)
        if (cooldownExercise != null && showRestModal) {
            RestTimerModal(
                exerciseName = cooldownExercise ?: "",
                remainingSeconds = cooldownRemaining,
                totalDuration = cooldownTotal,
                onSkip = { viewModel.skipCooldown() },
                onDismissRequest = { showRestModal = false }
            )
        }

        // Float Cooldown Timer Banner (minimized state) above the button
        AnimatedVisibility(
            visible = cooldownExercise != null && !showRestModal && selectedExerciseId == null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut()
        ) {
            val soundPlayer = LocalSoundPlayer.current
            Box(
                modifier = Modifier
                    .padding(bottom = 88.dp)
                    .clickable {
                        soundPlayer.play(AppSound.BUTTON_TAP)
                        showRestModal = true
                    }
            ) {
                CooldownBanner(
                    exerciseName = cooldownExercise ?: "",
                    remainingSeconds = cooldownRemaining,
                    totalDuration = cooldownTotal,
                    onSkip = { viewModel.skipCooldown() }
                )
            }
        }

        // Docked Start/Complete Workout bar — solid background, flush with the bottom edge
        // so no list content is ever visible behind the button.
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp
        ) {
            val soundPlayer = LocalSoundPlayer.current
            val barInteractionSource = remember { MutableInteractionSource() }
            Button(
                onClick = {
                    if (!isStarted) {
                        soundPlayer.play(AppSound.WHOOSH)
                        viewModel.startWorkout()
                    } else {
                        val hasIncomplete = exercises.any { state -> state.sets.any { !it.isCompleted } }
                        if (hasIncomplete) {
                            showIncompleteWarning = true
                        } else {
                            viewModel.completeWorkout()
                        }
                    }
                },
                interactionSource = barInteractionSource,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp)
                    .pressScale(barInteractionSource),
                colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                shape = RoundedCornerShape(28.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(
                    if (isStarted) Icons.Default.Check else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (isStarted) "Complete Workout" else "Start Workout",
                    fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp
                )
            }
        }

        // Incomplete Exercises Warning
        if (showIncompleteWarning) {
            AlertDialog(
                onDismissRequest = { showIncompleteWarning = false },
                title = { Text("Incomplete Exercises", fontWeight = FontWeight.Bold) },
                text = { Text("You still have unfinished sets. Complete the workout anyway?") },
                confirmButton = {
                    TextButton(onClick = {
                        showIncompleteWarning = false
                        viewModel.completeWorkout()
                    }) {
                        Text("Complete Anyway", color = RedDanger, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showIncompleteWarning = false }) {
                        Text("Keep Going")
                    }
                }
            )
        }

        // Countdown Timer Overlay Dialog
        if (isCountdownActive && showCountdownModal && countdownExercise != null) {
            CountdownTimerDialog(
                exerciseName = countdownExercise ?: "",
                remainingSeconds = countdownRemaining,
                totalDuration = countdownTotal,
                isPaused = isCountdownPaused,
                onTapTimer = { viewModel.toggleCountdownPause() },
                onDone = { viewModel.completeCountdownEarly() },
                onCancel = { viewModel.cancelCountdown() },
                onMinimize = { showCountdownModal = false }
            )
        }

        // Float Countdown Timer Banner (minimized state) above the button
        AnimatedVisibility(
            visible = isCountdownActive && !showCountdownModal && selectedExerciseId == null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut()
        ) {
            val soundPlayer = LocalSoundPlayer.current
            Box(
                modifier = Modifier
                    .padding(bottom = 88.dp)
                    .clickable {
                        soundPlayer.play(AppSound.BUTTON_TAP)
                        showCountdownModal = true
                    }
            ) {
                CountdownBanner(
                    exerciseName = countdownExercise ?: "",
                    remainingSeconds = countdownRemaining,
                    totalDuration = countdownTotal,
                    onDone = { viewModel.completeCountdownEarly() }
                )
            }
        }

        // Summary Overlay Dialog on Completion
        summary?.let { sum ->
            WorkoutSummaryDialog(
                summary = sum,
                onDismiss = { viewModel.dismissSummary() },
                onUpdateDuration = { newDur ->
                    viewModel.updateCompletedWorkoutDuration(sum.workoutId, newDur)
                }
            )
        }

        // Celebratory Record Broken Dialog — deferred until the user closes the exercise
        // screen and returns to the main workout screen, rather than popping up over it.
        if (selectedExerciseId == null) {
        recordBrokenCelebration?.let { celeb ->
            val cardEntrance = remember { Animatable(0f) }
            val trophyBounce = remember { Animatable(0f) }
            val shimmer = rememberInfiniteTransition(label = "prShimmer")
            val shimmerPulse by shimmer.animateFloat(
                initialValue = 0.35f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "prShimmerPulse"
            )
            val soundPlayer = LocalSoundPlayer.current
            val context = LocalContext.current
            // Keyed on the exercise, not the whole state object: a value-only refresh of the
            // pending record (user edited the set after completing it) must not replay the
            // chime or restart the entrance animation.
            LaunchedEffect(celeb.exerciseName) {
                // Fire the chime/haptic here, when the dialog actually becomes visible, rather
                // than from the ViewModel - the dialog itself is deferred until the user leaves
                // the exercise detail screen, so playing the sound at detection time meant it
                // could fire well before the popup was ever shown.
                soundPlayer.play(AppSound.CHIME)
                Haptics.celebrate(context)
                cardEntrance.snapTo(0f)
                trophyBounce.snapTo(0f)
                cardEntrance.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                trophyBounce.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessLow))
                // Auto-dismiss so the celebration never blocks a mid-workout user;
                // keyed on `celeb`, so a queued second record restarts the window.
                kotlinx.coroutines.delay(3000)
                viewModel.dismissRecordCelebration()
            }
            Dialog(onDismissRequest = { viewModel.dismissRecordCelebration() }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .scale(0.8f + 0.2f * cardEntrance.value)
                        .alpha(cardEntrance.value),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, GoldPR.copy(alpha = shimmerPulse)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Celebration Gold Badge with Icon, plus a soft pulsing glow behind it
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .scale(0.3f + 0.7f * trophyBounce.value)
                                    .blur(18.dp)
                                    .clip(CircleShape)
                                    .background(GoldPR.copy(alpha = 0.45f * shimmerPulse))
                            )
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .scale(0.3f + 0.7f * trophyBounce.value)
                                    .clip(CircleShape)
                                    .background(AmberWarningBgLight)
                                    .border(2.dp, GoldPR, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = "Trophy",
                                    tint = GoldPR,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Record Broken!",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = GoldPR,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = celeb.exerciseName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextDark,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Comparison Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = LightBlueContainer),
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.5.dp, BorderLight)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Previous", fontSize = 12.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(celeb.oldRecord, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = TextDark)
                                }

                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "to",
                                    tint = BluePrimary,
                                    modifier = Modifier.size(24.dp)
                                )

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("New Best", fontSize = 12.sp, color = GoldPR, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(celeb.newRecord, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = GoldPR)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { viewModel.dismissRecordCelebration() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPR),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Awesome!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
        }

        // Add Exercise Dialog
        if (showAddExerciseDialog) {
            ExercisePickerDialog(
                title = "Add Exercise",
                exercises = allExercises.filter { ex ->
                    // Exclude exercises already in active workout
                    exercises.none { it.exercise.id == ex.id }
                },
                onDismiss = { showAddExerciseDialog = false },
                onExerciseSelected = { exercise ->
                    viewModel.addExerciseToWorkout(exercise.id)
                    showAddExerciseDialog = false
                },
                onCreateExercise = { name, type, bodyPart, impactLevel ->
                    val cat = activeWorkout?.category ?: "PUSH"
                    viewModel.createAndAddExerciseToWorkout(name, type, bodyPart, impactLevel, cat)
                    showAddExerciseDialog = false
                },
                ownedEquipment = ownedEquipment,
                usageStats = exerciseUsageStats
            )
        }

        // Replace Exercise Dialog
        showReplaceDialogForExerciseId?.let { oldId ->
            val replacedBodyPart = exercises.find { it.exercise.id == oldId }?.exercise?.bodyPart
            val availableExercises = allExercises.filter { ex ->
                exercises.none { it.exercise.id == ex.id }
            }
            // Mirror the dialog's default AVAILABLE equipment filter so we don't open the
            // Muscle Group tab when every exercise in that group requires unowned equipment.
            val hasSameMuscleGroup = replacedBodyPart != null &&
                availableExercises.any { ex ->
                    ex.bodyPart == replacedBodyPart &&
                    Equipment.parseCsv(ex.equipment).let { req ->
                        req.isEmpty() || req.all { it in ownedEquipment }
                    }
                }
            ExercisePickerDialog(
                title = "Replace Exercise",
                exercises = availableExercises,
                onDismiss = { showReplaceDialogForExerciseId = null },
                onExerciseSelected = { newExercise ->
                    viewModel.replaceExerciseInWorkout(oldId, newExercise.id)
                    viewModel.onExerciseScreenClosed(oldId)
                    showReplaceDialogForExerciseId = null
                    selectedExerciseId = null
                },
                ownedEquipment = ownedEquipment,
                usageStats = exerciseUsageStats,
                initialTab = if (hasSameMuscleGroup) 1 else 0,
                initialMuscleGroup = if (hasSameMuscleGroup) replacedBodyPart else null
            )
        }

        // Exercise Detail Bottom Sheet (Slide-Up View)
        selectedExerciseState?.let { exerciseState ->
            ExerciseDetailBottomSheet(
                exerciseState = exerciseState,
                isWorkoutStarted = isStarted,
                cooldownExerciseName = cooldownExercise,
                cooldownRemaining = cooldownRemaining,
                cooldownDuration = cooldownTotal,
                isRestTimerExpanded = showRestModal,
                onSkipCooldown = { viewModel.skipCooldown() },
                onShowRestTimer = { showRestModal = true },
                countdownExerciseName = countdownExercise,
                countdownRemaining = countdownRemaining,
                countdownDuration = countdownTotal,
                isCountdownExpanded = showCountdownModal,
                onShowCountdownTimer = { showCountdownModal = true },
                onCompleteCountdownEarly = { viewModel.completeCountdownEarly() },
                onDismissRequest = {
                    viewModel.onExerciseScreenClosed(exerciseState.exercise.id)
                    selectedExerciseId = null
                },
                onSetValuesChanged = { setId, w, r, t, d, inc ->
                    viewModel.updateSetValues(setId, w, r, t, d, inc)
                },
                onSetCompleteToggled = { setId, complete, w, r, t, d, inc ->
                    viewModel.toggleSetCompletion(setId, complete, w, r, t, d, inc)
                },
                onStartCountdown = { setId, name, seconds ->
                    viewModel.startCountdown(setId, name, seconds)
                },
                onAddSet = {
                    viewModel.addSetToExercise(exerciseState.exercise.id)
                },
                onRemoveSet = { setId ->
                    viewModel.removeSetFromExercise(setId, exerciseState.exercise.id)
                },
                onReplaceExercise = {
                    showReplaceDialogForExerciseId = exerciseState.exercise.id
                },
                onRemoveExercise = {
                    viewModel.removeExerciseFromWorkout(exerciseState.exercise.id)
                    viewModel.onExerciseScreenClosed(exerciseState.exercise.id)
                    selectedExerciseId = null
                },
                onStartWorkout = {
                    viewModel.startWorkout()
                }
            )
        }

        // First-open coach-mark tour, shown once until completed/skipped
        if (showTour) {
            WorkoutTourOverlay(
                step = tourStep,
                categoryBounds = relativeTo(categoryBadgeBounds, screenRootBounds),
                refreshBounds = relativeTo(refreshButtonBounds, screenRootBounds),
                dialsBounds = relativeTo(dialsBounds, screenRootBounds),
                onNext = {
                    if (tourStep < 3) {
                        tourStep += 1
                    } else {
                        viewModel.markWorkoutTourSeen()
                    }
                },
                onSkip = { viewModel.markWorkoutTourSeen() }
            )
        }
    }
}
}

// Translates a window-space anchor rect into the coordinate space of the screen's own root
// Box, since the overlay is laid out relative to that root (which is itself offset from the
// window origin by Scaffold/status-bar/IME padding).
private fun relativeTo(anchor: Rect?, root: Rect?): Rect? {
    if (anchor == null || root == null) return null
    return Rect(
        left = anchor.left - root.left,
        top = anchor.top - root.top,
        right = anchor.right - root.left,
        bottom = anchor.bottom - root.top
    )
}

@Composable
private fun WorkoutTourOverlay(
    step: Int,
    categoryBounds: Rect?,
    refreshBounds: Rect?,
    dialsBounds: Rect?,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    val density = LocalDensity.current
    val anchor = when (step) {
        0 -> categoryBounds
        1 -> refreshBounds
        2 -> dialsBounds
        else -> null
    }
    val message = when (step) {
        0 -> "Switch muscle group here."
        1 -> "Don't like this set? Refresh for a new one."
        2 -> "These track your performance and calories burned during the workout."
        else -> "We've set up your first workout — hit Start Workout when you're ready."
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { /* swallow taps behind the tour */ }
        )

        if (anchor != null) {
            Box(
                modifier = Modifier
                    .offset(
                        x = with(density) { anchor.left.toDp() } - 6.dp,
                        y = with(density) { anchor.top.toDp() } - 6.dp
                    )
                    .size(
                        width = with(density) { anchor.width.toDp() } + 12.dp,
                        height = with(density) { anchor.height.toDp() } + 12.dp
                    )
                    .border(2.dp, BluePrimary, MaterialTheme.shapes.medium)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (anchor != null) Modifier.padding(top = with(density) { anchor.bottom.toDp() } + 16.dp)
                    else Modifier
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (anchor == null) Arrangement.Center else Arrangement.Top
        ) {
            Card(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 320.dp),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = message,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (step < 3) {
                            TextButton(onClick = onSkip) { Text("Skip") }
                        }
                        Button(
                            onClick = onNext,
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                        ) {
                            Text(if (step < 3) "Next" else "Got it")
                        }
                    }
                }
            }
        }
    }
}

// --- Workout Summary Dialog ---

@Composable
fun WorkoutSummaryDialog(
    summary: com.example.workoutbuddy.viewmodel.WorkoutSummaryState,
    onDismiss: () -> Unit,
    onUpdateDuration: (Long) -> Unit
) {
    var showEditDurationDialog by remember { mutableStateOf(false) }
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .scale(0.85f + 0.15f * entrance.value)
                .alpha(entrance.value),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Trophy/Celebration Header
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(LightBlueContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = GoldPR,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Workout Completed!",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = TextDark
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Cards Layout
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val animatedCalories by animateIntAsState(
                        targetValue = summary.totalCalories.toInt(),
                        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
                        label = "caloriesCountUp"
                    )
                    val animatedSteps by animateIntAsState(
                        targetValue = summary.totalSteps,
                        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
                        label = "stepsCountUp"
                    )
                    val animatedPrCount by animateIntAsState(
                        targetValue = summary.prCount,
                        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
                        label = "prCountUp"
                    )
                    val animatedVolume by animateIntAsState(
                        targetValue = summary.totalVolumeKg.toInt(),
                        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
                        label = "volumeCountUp"
                    )
                    // Records Broken is the emotional payoff — when any were broken it
                    // gets a full-width hero row above everything else.
                    if (summary.prCount > 0) {
                        HeroRecordBox(
                            count = animatedPrCount,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Calories card
                        StatBox(
                            value = "$animatedCalories",
                            label = "kcal",
                            subtext = "Burned",
                            modifier = Modifier.weight(1f)
                        )
                        // Steps card (hidden when no steps were recorded)
                        if (summary.totalSteps > 0) {
                            StatBox(
                                value = "$animatedSteps",
                                label = "steps",
                                subtext = "Moved",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    // Volume card (records moved to the hero row above)
                    StatBox(
                        value = "$animatedVolume",
                        label = "kg",
                        subtext = "Lifted",
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Strength & Stamina Gained deltas
                    if (summary.strengthScoreDelta > 0.001 && summary.staminaScoreDelta > 0.001) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatBox(
                                value = "+${String.format("%.1f", summary.strengthScoreDelta)}",
                                label = "Strength",
                                subtext = "Gained",
                                modifier = Modifier.weight(1f),
                                highlight = true
                            )
                            StatBox(
                                value = "+${String.format("%.1f", summary.staminaScoreDelta)}",
                                label = "Stamina",
                                subtext = "Gained",
                                modifier = Modifier.weight(1f),
                                highlight = true
                            )
                        }
                    } else if (summary.strengthScoreDelta > 0.001) {
                        StatBox(
                            value = "+${String.format("%.1f", summary.strengthScoreDelta)}",
                            label = "Strength",
                            subtext = "Gained",
                            modifier = Modifier.fillMaxWidth(),
                            highlight = true
                        )
                    } else if (summary.staminaScoreDelta > 0.001) {
                        StatBox(
                            value = "+${String.format("%.1f", summary.staminaScoreDelta)}",
                            label = "Stamina",
                            subtext = "Gained",
                            modifier = Modifier.fillMaxWidth(),
                            highlight = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Duration Text (Clickable to Edit)
                Row(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.extraSmall)
                        .clickable { showEditDurationDialog = true }
                        .background(LightBlueContainer)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit duration",
                        tint = BluePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Total Duration: ${formatDuration(summary.durationInSeconds)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextBlue
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showEditDurationDialog) {
        var editedSeconds by remember { mutableStateOf(summary.durationInSeconds.toInt()) }
        Dialog(onDismissRequest = { showEditDurationDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Edit Workout Duration",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextDark,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    TimeCalculatorTextField(
                        initialSeconds = editedSeconds,
                        onSecondsChanged = { editedSeconds = it },
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = { showEditDurationDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = TextMuted)
                        }
                        Button(
                            onClick = {
                                onUpdateDuration(editedSeconds.toLong())
                                showEditDurationDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatBox(
    value: String,
    label: String,
    subtext: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) LightBlueContainer else LightBackground
        ),
        border = BorderStroke(1.dp, if (highlight) BluePrimary.copy(alpha = 0.5f) else BorderLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                color = if (highlight) GoldPR else TextDark
            )
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (highlight) TextBlue else TextMuted
            )
            Text(
                text = subtext,
                fontSize = 11.sp,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun HeroRecordBox(
    count: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = AmberWarningBgLight),
        border = BorderStroke(1.5.dp, GoldPR.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = GoldPR,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "$count",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                color = GoldPR
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = if (count == 1) "Record" else "Records",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldPR
                )
                Text(
                    text = "Broken",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        String.format("%d:%02d:%02d", h, m, s)
    } else {
        String.format("%d:%02d", m, s)
    }
}
