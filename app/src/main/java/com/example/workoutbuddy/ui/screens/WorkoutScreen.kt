package com.example.workoutbuddy.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.workoutbuddy.theme.*
import com.example.workoutbuddy.ui.components.*
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
    val displayedIntensity by viewModel.displayedIntensity.collectAsState()
    val displayedCalories by viewModel.displayedCalories.collectAsState()

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

    var showCategoryMenu by remember { mutableStateOf(false) }
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showReplaceDialogForExerciseId by remember { mutableStateOf<Int?>(null) }
    var selectedExerciseId by remember { mutableStateOf<Int?>(null) }
    var showRestModal by remember { mutableStateOf(false) }
    var showCountdownModal by remember { mutableStateOf(false) }

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

    WavyFloatingNumbersContainer(viewModel = viewModel) {
        Box(modifier = modifier.fillMaxSize().imePadding()) {
            Column(modifier = Modifier.fillMaxSize()) {
            
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Workout Category Badge & Switcher
                activeWorkout?.let { workout ->
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { if (!isStarted) showCategoryMenu = true }
                        ) {
                            val badgeColor = when (workout.category) {
                                "PUSH" -> BluePrimary
                                "PULL" -> GreenSuccess
                                "LOWER_BODY" -> GoldPR
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
                        }
                    }
                }

                // Workout Ticker Timer Box
                val isPaused by viewModel.isTimerPaused.collectAsState()
                val timerBgColor = if (isStarted) {
                    if (isPaused) Color(0xFFFEF3C7) else LightBlueContainer
                } else {
                    BorderLight.copy(alpha = 0.5f)
                }
                val timerTextColor = if (isStarted) {
                    if (isPaused) GoldPR else BluePrimary
                } else {
                    TextMuted
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(timerBgColor)
                        .clickable(enabled = isStarted) {
                            viewModel.toggleWorkoutTimer()
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (isPaused || !isStarted) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = "Timer Icon",
                        tint = timerTextColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formatDuration(durationSec),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = timerTextColor
                    )
                }
            }

            // Exercise List (Dial is first scrollable item, not sticky)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dashboard Intensity & Burn Dials — scrolls with list
                item {
                    activeWorkout?.let { workout ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val totalSetsCount = remember(exercises) {
                                    exercises.flatMap { it.sets }.size.coerceAtLeast(1)
                                }
                                val targetScore = workout.startingStrengthScore * totalSetsCount
                                val targetCalories = totalSetsCount * 20.0 + 100.0

                                val currentBurnedCalories = workout.totalCalories

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    WorkoutIntensityDial(
                                        intensityScore = displayedIntensity,
                                        targetScore = targetScore
                                    )

                                    WorkoutBurnDial(
                                        burnedCalories = displayedCalories,
                                        targetCalories = targetCalories
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Strength target", fontSize = 11.sp, color = TextMuted)
                                        Text("${workout.startingStrengthScore.toInt()}", fontWeight = FontWeight.Bold, color = TextDark)
                                    }
                                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(BorderLight))
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Burn target", fontSize = 11.sp, color = TextMuted)
                                        Text("${targetCalories.toInt()} kcal", fontWeight = FontWeight.Bold, color = TextDark)
                                    }
                                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(BorderLight))
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Active sets", fontSize = 11.sp, color = TextMuted)
                                        Text("$totalSetsCount sets", fontWeight = FontWeight.Bold, color = TextDark)
                                    }
                                }
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
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, BluePrimary.copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = BluePrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Exercise", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, color = BluePrimary)
                        }
                    }
                    
                    // Extra spacing at bottom of list so button doesn't block cards
                    item {
                        Spacer(modifier = Modifier.height(100.dp))
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
        if (cooldownExercise != null && !showRestModal && selectedExerciseId == null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (isStarted) 80.dp else 16.dp)
                    .clickable { showRestModal = true }
            ) {
                CooldownBanner(
                    exerciseName = cooldownExercise ?: "",
                    remainingSeconds = cooldownRemaining,
                    totalDuration = cooldownTotal,
                    onSkip = { viewModel.skipCooldown() }
                )
            }
        }

        // Bottom Start Workout button or Floating Complete Workout button
        if (!isStarted) {
            Button(
                onClick = { viewModel.startWorkout() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Workout", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
            }
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                ExtendedFloatingActionButton(
                    text = { Text("Complete Workout", fontWeight = FontWeight.Bold, color = Color.White) },
                    icon = { Icon(Icons.Default.Check, contentDescription = null, tint = Color.White) },
                    onClick = { viewModel.completeWorkout() },
                    containerColor = GreenSuccess,
                    shape = RoundedCornerShape(12.dp)
                )
            }
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
        if (isCountdownActive && !showCountdownModal && selectedExerciseId == null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (isStarted) 80.dp else 16.dp)
                    .clickable { showCountdownModal = true }
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

        // Celebratory Record Broken Dialog
        recordBrokenCelebration?.let { celeb ->
            Dialog(onDismissRequest = { viewModel.dismissRecordCelebration() }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, GoldPR),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Celebration Gold Badge with Icon
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFFBEB))
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
                            shape = RoundedCornerShape(12.dp),
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
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Awesome!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                }
            )
        }

        // Replace Exercise Dialog
        showReplaceDialogForExerciseId?.let { oldId ->
            ExercisePickerDialog(
                title = "Replace Exercise",
                exercises = allExercises.filter { ex ->
                    // Exclude exercises already in active workout
                    exercises.none { it.exercise.id == ex.id }
                },
                onDismiss = { showReplaceDialogForExerciseId = null },
                onExerciseSelected = { newExercise ->
                    viewModel.replaceExerciseInWorkout(oldId, newExercise.id)
                    viewModel.onExerciseScreenClosed(oldId)
                    showReplaceDialogForExerciseId = null
                    selectedExerciseId = null
                }
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
                onSkipCooldown = { viewModel.skipCooldown() },
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

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Calories card
                        StatBox(
                            value = "${summary.totalCalories.toInt()}",
                            label = "kcal",
                            subtext = "Burned",
                            modifier = Modifier.weight(1f)
                        )
                        // Steps card
                        StatBox(
                            value = "${summary.totalSteps}",
                            label = "steps",
                            subtext = "Moved",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // PRs card
                        StatBox(
                            value = "${summary.prCount}",
                            label = "Records",
                            subtext = "Broken",
                            modifier = Modifier.weight(1f),
                            highlight = true
                        )
                        // Volume card
                        StatBox(
                            value = "${summary.totalVolumeKg.toInt()}",
                            label = "kg",
                            subtext = "Volume",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Strength & Stamina Gained deltas
                    if (summary.strengthScoreDelta > 0.001 && summary.staminaScoreDelta > 0.001) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatBox(
                                value = "+${String.format("%.1f", summary.strengthScoreDelta)}",
                                label = "STR Score",
                                subtext = "Gained",
                                modifier = Modifier.weight(1f),
                                highlight = true
                            )
                            StatBox(
                                value = "+${String.format("%.1f", summary.staminaScoreDelta)}",
                                label = "STM Score",
                                subtext = "Gained",
                                modifier = Modifier.weight(1f),
                                highlight = true
                            )
                        }
                    } else if (summary.strengthScoreDelta > 0.001) {
                        StatBox(
                            value = "+${String.format("%.1f", summary.strengthScoreDelta)}",
                            label = "STR Score",
                            subtext = "Gained",
                            modifier = Modifier.fillMaxWidth(),
                            highlight = true
                        )
                    } else if (summary.staminaScoreDelta > 0.001) {
                        StatBox(
                            value = "+${String.format("%.1f", summary.staminaScoreDelta)}",
                            label = "STM Score",
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
                        .clip(RoundedCornerShape(8.dp))
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
                    shape = RoundedCornerShape(12.dp)
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
                shape = RoundedCornerShape(16.dp),
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
                            shape = RoundedCornerShape(12.dp)
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
