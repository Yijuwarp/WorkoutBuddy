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

    val selectedExerciseState = remember(selectedExerciseId, exercises) {
        exercises.find { it.exercise.id == selectedExerciseId }
    }

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
                                text = workout.category,
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

            // Exercise List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
                                selectedExerciseId = exerciseState.exercise.id
                            }
                        )
                    }

                    // Add Exercise button
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Button(
                                onClick = { showAddExerciseDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Exercise", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                    
                    // Extra spacing at bottom of list so button doesn't block cards
                    item {
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }

        // Float Cooldown Timer Banner above the button
        if (cooldownExercise != null && selectedExerciseId == null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (isStarted) 80.dp else 16.dp)
            ) {
                CooldownBanner(
                    exerciseName = cooldownExercise ?: "",
                    remainingSeconds = cooldownRemaining,
                    totalDuration = cooldownTotal,
                    onSkip = { viewModel.skipCooldown() }
                )
            }
        }

        // Active State Floating Buttons at bottom right
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            if (!isStarted) {
                // START WORKOUT BUTTON (Floating)
                ExtendedFloatingActionButton(
                    text = { Text("Start Workout", fontWeight = FontWeight.Bold, color = Color.White) },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White) },
                    onClick = { viewModel.startWorkout() },
                    containerColor = BluePrimary,
                    shape = RoundedCornerShape(12.dp)
                )
            } else {
                // COMPLETE WORKOUT BUTTON
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
        if (isCountdownActive && countdownExercise != null) {
            CountdownTimerDialog(
                exerciseName = countdownExercise ?: "",
                remainingSeconds = countdownRemaining,
                totalDuration = countdownTotal,
                isPaused = isCountdownPaused,
                onTapTimer = { viewModel.toggleCountdownPause() },
                onDone = { viewModel.completeCountdownEarly() },
                onCancel = { viewModel.cancelCountdown() }
            )
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
                    showReplaceDialogForExerciseId = null
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
                onDismissRequest = { selectedExerciseId = null },
                onSetValuesChanged = { setId, w, r, t, d ->
                    viewModel.updateSetValues(setId, w, r, t, d)
                },
                onSetCompleteToggled = { setId, complete ->
                    viewModel.toggleSetCompletion(setId, complete)
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
                    selectedExerciseId = null
                }
            )
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
                    // PRs card
                    StatBox(
                        value = "${summary.prCount}",
                        label = "PRs",
                        subtext = "Records",
                        modifier = Modifier.weight(1f),
                        highlight = true
                    )
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
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (highlight) TextBlue else TextMuted
            )
            Text(
                text = subtext,
                fontSize = 9.sp,
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
