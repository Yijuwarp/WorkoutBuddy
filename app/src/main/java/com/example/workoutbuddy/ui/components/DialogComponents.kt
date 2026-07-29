package com.example.workoutbuddy.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.workoutbuddy.audio.AppSound
import com.example.workoutbuddy.ui.util.LocalSoundPlayer
import com.example.workoutbuddy.data.Equipment
import com.example.workoutbuddy.data.database.ExerciseEntity
import com.example.workoutbuddy.data.database.ExerciseUsageStat
import com.example.workoutbuddy.theme.*
import com.example.workoutbuddy.viewmodel.ActiveExerciseState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailBottomSheet(
    exerciseState: ActiveExerciseState,
    isWorkoutStarted: Boolean,
    cooldownExerciseName: String?,
    cooldownRemaining: Int,
    cooldownDuration: Int,
    isRestTimerExpanded: Boolean,
    onSkipCooldown: () -> Unit,
    onShowRestTimer: () -> Unit,
    countdownExerciseName: String? = null,
    countdownRemaining: Int = 0,
    countdownDuration: Int = 0,
    isCountdownExpanded: Boolean = true,
    onShowCountdownTimer: () -> Unit = {},
    onCompleteCountdownEarly: () -> Unit = {},
    onDismissRequest: () -> Unit,
    onSetValuesChanged: (Long, Double?, Int?, Int?, Double?, Double?) -> Unit,
    onSetCompleteToggled: (Long, Boolean, Double?, Int?, Int?, Double?, Double?) -> Unit,
    onStartCountdown: (Long, String, Int) -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: (Long) -> Unit,
    onReplaceExercise: () -> Unit,
    onRemoveExercise: () -> Unit,
    onStartWorkout: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showHowToSheet by remember { mutableStateOf(false) }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    // If the user is mid-typing in a set's input (e.g. queuing up the next set's weight while
    // resting) when a timer completes elsewhere, a still-focused field can cause the keyboard
    // to pop back open on the resulting recomposition. Defocus/hide whenever either timer
    // transitions to "finished" so that can't happen, not just when a button is tapped.
    LaunchedEffect(cooldownExerciseName) {
        if (cooldownExerciseName == null) {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }
    LaunchedEffect(countdownExerciseName) {
        if (countdownExerciseName == null) {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = BorderLight) }
    ) {
      Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            // Exercise Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showHowToSheet = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExerciseThumbnail(
                        exerciseName = exerciseState.exercise.name,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = exerciseState.exercise.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = TextDark,
                            maxLines = 2
                        )
                        Text(
                            text = "${exerciseState.exercise.bodyPart} • ${exerciseState.exercise.type}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = TextBlue
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    // How-To button
                    OutlinedButton(
                        onClick = { showHowToSheet = true },
                        shape = MaterialTheme.shapes.extraSmall,
                        border = BorderStroke(1.dp, BluePrimary),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "How To",
                            tint = BluePrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "How-To",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BluePrimary
                        )
                    }

                    if (isWorkoutStarted) {
                        var showMenu by remember { mutableStateOf(false) }

                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Exercise Options",
                                    tint = BluePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Replace Exercise") },
                                    onClick = {
                                        showMenu = false
                                        onReplaceExercise()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Remove Exercise", color = Color.Red) },
                                    onClick = {
                                        showMenu = false
                                        onRemoveExercise()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // TOP Pane: LAST and BEST Records
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(LightBlueContainer)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = exerciseState.prevLiftText, // Formatted as "LAST X rep Y weight"
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextBlue
                )
                Text(
                    text = exerciseState.bestLiftText, // Formatted as "BEST X rep N weight"
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = GoldPR
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Header for Inputs List
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("#", modifier = Modifier.width(48.dp), style = MaterialTheme.typography.bodyMedium, color = TextMuted, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (exerciseState.exercise.type) {
                        "LIFT" -> {
                            Text("Reps", modifier = Modifier.width(80.dp), style = MaterialTheme.typography.bodyMedium, color = TextMuted, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.width(20.dp)) // aligns with the " x " text and its spacers
                            Text("Weight (kg)", modifier = Modifier.width(88.dp), style = MaterialTheme.typography.bodyMedium, color = TextMuted, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                        "CARDIO" -> {
                            val isJumpRope = exerciseState.exercise.name.contains("Jump Rope", ignoreCase = true)
                            val isIncline = !isJumpRope && (exerciseState.exercise.name.contains("Walking", ignoreCase = true) ||
                                            exerciseState.exercise.name.contains("Running", ignoreCase = true) ||
                                            exerciseState.exercise.name.contains("Cycling", ignoreCase = true))
                            if (isJumpRope) {
                                Text("Time", modifier = Modifier.width(180.dp), style = MaterialTheme.typography.bodyMedium, color = TextMuted, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            } else if (isIncline) {
                                Text("Time", modifier = Modifier.width(80.dp), style = MaterialTheme.typography.bodyMedium, color = TextMuted, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Dist (km)", modifier = Modifier.width(64.dp), style = MaterialTheme.typography.bodyMedium, color = TextMuted, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Incl (%)", modifier = Modifier.width(64.dp), style = MaterialTheme.typography.bodyMedium, color = TextMuted, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            } else {
                                Text("Time", modifier = Modifier.width(100.dp), style = MaterialTheme.typography.bodyMedium, color = TextMuted, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Distance (km)", modifier = Modifier.width(72.dp), style = MaterialTheme.typography.bodyMedium, color = TextMuted, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            }
                        }
                        "HOLD" -> {
                            Text("Time", modifier = Modifier.width(100.dp), style = MaterialTheme.typography.bodyMedium, color = TextMuted, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                    }
                }
                Text("Done", modifier = Modifier.width(48.dp), style = MaterialTheme.typography.bodyMedium, color = TextMuted, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            Spacer(modifier = Modifier.height(8.dp))

            // Set List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(weight = 1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(exerciseState.sets, key = { it.id }) { set ->
                    val soundPlayer = LocalSoundPlayer.current
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { dismissValue ->
                            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                soundPlayer.play(AppSound.WHOOSH)
                                onRemoveSet(set.id)
                                true
                            } else {
                                false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        modifier = Modifier.animateItem(),
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            Box(modifier = Modifier.fillMaxSize())
                        }
                    ) {
                        SetRowItem(
                            set = set,
                            type = exerciseState.exercise.type,
                            exerciseName = exerciseState.exercise.name,
                            isWorkoutStarted = isWorkoutStarted,
                            onStartCountdown = onStartCountdown,
                            onValuesChanged = { w, r, t, d, inc ->
                                onSetValuesChanged(set.id, w, r, t, d, inc)
                            },
                            onCompleteToggled = { complete, w, r, t, d, inc ->
                                 if (complete) {
                                     focusManager.clearFocus(force = true)
                                keyboardController?.hide()
                                 }
                                 onSetCompleteToggled(set.id, complete, w, r, t, d, inc)
                            }
                        )
                    }
                }

                if (isWorkoutStarted) {
                    item {
                        val soundPlayer = LocalSoundPlayer.current
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            TextButton(
                                onClick = {
                                    soundPlayer.play(AppSound.POP)
                                    onAddSet()
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = BluePrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Set", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sticky Log Buttons or Starter Reminder
            if (isWorkoutStarted) {
                val allCompleted = exerciseState.sets.isNotEmpty() && exerciseState.sets.all { it.isCompleted }
                if (allCompleted) {
                    Button(
                        onClick = onDismissRequest,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Log All Sets button
                        Button(
                            onClick = {
                                focusManager.clearFocus(force = true)
                                keyboardController?.hide()
                                exerciseState.sets.forEach { set ->
                                    if (!set.isCompleted) {
                                        onSetCompleteToggled(set.id, true, null, null, null, null, null)
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BorderLight),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Log All Sets", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        // Log Set button (or Start Timer if timer is available)
                        val nextSet = exerciseState.sets.firstOrNull { !it.isCompleted }
                        val hasTimer = exerciseState.exercise.type == "CARDIO" || exerciseState.exercise.type == "HOLD"
                        Button(
                            onClick = {
                                focusManager.clearFocus(force = true)
                                keyboardController?.hide()
                                if (nextSet != null) {
                                    if (hasTimer) {
                                        val seconds = nextSet.time ?: nextSet.recommendedTime ?: 60
                                        onStartCountdown(nextSet.id, exerciseState.exercise.name, seconds)
                                    } else {
                                        onSetCompleteToggled(nextSet.id, true, null, null, null, null, null)
                                    }
                                }
                            },
                            modifier = Modifier.weight(1.2f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            val text = if (hasTimer) "Start Timer" else "Log Set"
                            Text(text, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Start the workout to log your sets",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = AmberWarning,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )
                    Button(
                        onClick = {
                            onStartWorkout?.invoke()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Start Workout", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                    }
                }
            }
        }

        // Float Cooldown/Countdown Timer Banners (minimized state), mirrors the main workout
        // screen - previously only the rest-timer (cooldown) banner was wired up here, so the
        // exercise/cardio countdown timer would silently keep running with no minimized
        // indicator while the user was on this screen.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (countdownExerciseName != null && !isCountdownExpanded) {
                Box(modifier = Modifier.clickable { onShowCountdownTimer() }) {
                    CountdownBanner(
                        exerciseName = countdownExerciseName,
                        remainingSeconds = countdownRemaining,
                        totalDuration = countdownDuration,
                        onDone = onCompleteCountdownEarly
                    )
                }
            }
            if (cooldownExerciseName != null && !isRestTimerExpanded) {
                Box(modifier = Modifier.clickable { onShowRestTimer() }) {
                    CooldownBanner(
                        exerciseName = cooldownExerciseName,
                        remainingSeconds = cooldownRemaining,
                        totalDuration = cooldownDuration,
                        onSkip = onSkipCooldown
                    )
                }
            }
        }
      }
    }

    // How-To Sheet
    if (showHowToSheet) {
        ExerciseHowToSheet(
            exercise = exerciseState.exercise,
            onDismiss = { showHowToSheet = false }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseHowToSheet(
    exercise: ExerciseEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val howToSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = howToSheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = BorderLight) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExerciseThumbnail(
                    exerciseName = exercise.name,
                    modifier = Modifier.size(100.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Black),
                        color = TextDark
                    )
                    Text(
                        text = "${exercise.bodyPart} • ${exercise.type}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = TextBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Description
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LightBlueContainer),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = exercise.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextDark,
                    modifier = Modifier.padding(14.dp),
                    lineHeight = 22.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Step-by-step instructions
            Text(
                text = "Step-by-Step Instructions",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextDark,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val steps = exercise.howToSteps.split("\n").filter { it.isNotBlank() }
            steps.forEachIndexed { index, step ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(BluePrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = step.trim(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextDark,
                        modifier = Modifier.weight(1f).padding(top = 3.dp),
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // YouTube linkout button
            if (exercise.youtubeUrl.isNotBlank()) {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(exercise.youtubeUrl))
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = YoutubeRed),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Watch on YouTube",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
enum class ExerciseSortMode(val label: String) {
    ALPHABETICAL("Alphabetical"),
    MOST_LOGGED("Most logged"),
    RECENT("Recent")
}

enum class EquipmentFilterMode(val label: String) {
    ALL("All"),
    AVAILABLE("Available"),
    BODYWEIGHT("Bodyweight")
}

@Composable
fun ExercisePickerDialog(
    title: String,
    exercises: List<ExerciseEntity>,
    onDismiss: () -> Unit,
    onExerciseSelected: (ExerciseEntity) -> Unit,
    onCreateExercise: ((String, String, String, String) -> Unit)? = null,
    ownedEquipment: Set<Equipment> = Equipment.entries.toSet(),
    usageStats: Map<Int, ExerciseUsageStat> = emptyMap(),
    initialTab: Int = 0,
    initialMuscleGroup: String? = null
) {
    var isCreating by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newType by remember { mutableStateOf("LIFT") }
    var selectedBodyParts by remember { mutableStateOf(emptySet<String>()) }
    var newImpact by remember { mutableStateOf("MEDIUM") }

    var searchQuery by remember { mutableStateOf("") }

    // Tab state: 0 = All, 1 = Muscle Group. Replace opens this scoped to the exercise being
    // replaced (initialTab=1, initialMuscleGroup=its bodyPart); the user can still switch tabs
    // or back out to the muscle-group list to pick a different group, since these are just the
    // starting values, not a lock-in.
    var activeTab by remember { mutableStateOf(initialTab) }
    var selectedMuscleGroup by remember { mutableStateOf(initialMuscleGroup) }
    var showBodyPartPicker by remember { mutableStateOf(false) }

    var showSortFilterMenu by remember { mutableStateOf(false) }
    var sortMode by remember { mutableStateOf(ExerciseSortMode.ALPHABETICAL) }
    var equipmentFilter by remember { mutableStateOf(EquipmentFilterMode.AVAILABLE) }

    fun passesEquipmentFilter(exercise: ExerciseEntity): Boolean {
        return when (equipmentFilter) {
            EquipmentFilterMode.ALL -> true
            EquipmentFilterMode.BODYWEIGHT -> exercise.equipment.isBlank()
            EquipmentFilterMode.AVAILABLE -> {
                val required = Equipment.parseCsv(exercise.equipment)
                required.isEmpty() || required.all { it in ownedEquipment }
            }
        }
    }

    fun sortExercises(list: List<ExerciseEntity>): List<ExerciseEntity> {
        return when (sortMode) {
            ExerciseSortMode.ALPHABETICAL -> list.sortedBy { it.name }
            ExerciseSortMode.MOST_LOGGED -> list.sortedByDescending { usageStats[it.id]?.logCount ?: 0 }
            ExerciseSortMode.RECENT -> list.sortedByDescending { usageStats[it.id]?.lastUsedDate ?: 0L }
        }
    }

    val filteredExercises = remember(searchQuery, exercises, sortMode, equipmentFilter) {
        sortExercises(
            exercises.filter {
                (it.name.contains(searchQuery, ignoreCase = true) || it.bodyPart.contains(searchQuery, ignoreCase = true)) &&
                    passesEquipmentFilter(it)
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        androidx.activity.compose.BackHandler(
            enabled = isCreating || (activeTab == 1 && selectedMuscleGroup != null)
        ) {
            if (isCreating) {
                isCreating = false
            } else if (activeTab == 1 && selectedMuscleGroup != null) {
                selectedMuscleGroup = null
            }
        }
 
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(20.dp)
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isCreating) {
                        IconButton(onClick = { isCreating = false }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = BluePrimary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    } else if (activeTab == 1 && selectedMuscleGroup != null) {
                        IconButton(onClick = { selectedMuscleGroup = null }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = BluePrimary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    val displayTitle = if (isCreating) {
                        "Create Custom Exercise"
                    } else if (activeTab == 1 && selectedMuscleGroup != null) {
                        selectedMuscleGroup!!
                    } else {
                        title
                    }
                    
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = TextDark,
                        modifier = Modifier.weight(1f)
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }
 
                if (isCreating) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Exercise Name") },
                            placeholder = { Text("e.g. Bench Press") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
 
                        Text(
                            text = "Exercise Type",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextDark
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("LIFT", "CARDIO", "HOLD").forEach { type ->
                                val selected = newType == type
                                FilterChip(
                                    selected = selected,
                                    onClick = { newType = type },
                                    label = { Text(type) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BluePrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
 
                        // Body part picker trigger
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedBodyParts.sorted().joinToString(", "),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Body Part(s) Impacted") },
                                placeholder = { Text("Select body parts...") },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Open selector"
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { showBodyPartPicker = true }
                            )
                        }
 
                        Text(
                            text = "Impact Level / Cooldown Rest",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextDark
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("LOW", "MEDIUM", "HIGH", "HEAVY").forEach { level ->
                                val selected = newImpact == level
                                val durationText = when (level) {
                                    "LOW" -> "30s"
                                    "MEDIUM" -> "60s"
                                    "HIGH" -> "120s"
                                    "HEAVY" -> "180s"
                                    else -> ""
                                }
                                FilterChip(
                                    selected = selected,
                                    onClick = { newImpact = level },
                                    label = { Text("$level ($durationText)") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BluePrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
 
                    Spacer(modifier = Modifier.height(16.dp))
 
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (newName.isNotBlank() && selectedBodyParts.isNotEmpty() && onCreateExercise != null) {
                                    onCreateExercise(newName, newType, selectedBodyParts.joinToString(", "), newImpact)
                                }
                            },
                            enabled = newName.isNotBlank() && selectedBodyParts.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Create & Add", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Tabs
                    TabRow(
                        selectedTabIndex = activeTab,
                        containerColor = Color.Transparent,
                        contentColor = BluePrimary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Tab(
                            selected = activeTab == 0,
                            onClick = { activeTab = 0 },
                            text = { Text("All", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                        )
                        Tab(
                            selected = activeTab == 1,
                            onClick = { activeTab = 1 },
                            text = { Text("Muscle Group", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                        )
                    }
 
                    if (activeTab == 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Search exercises...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box {
                                IconButton(
                                    onClick = { showSortFilterMenu = true },
                                    modifier = Modifier
                                        .clip(MaterialTheme.shapes.medium)
                                        .border(1.dp, BorderLight, MaterialTheme.shapes.medium)
                                ) {
                                    Icon(Icons.Default.FilterList, contentDescription = "Sort and filter", tint = BluePrimary)
                                }
                                DropdownMenu(
                                    expanded = showSortFilterMenu,
                                    onDismissRequest = { showSortFilterMenu = false }
                                ) {
                                    Text(
                                        "Sort by",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = TextMuted,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                    )
                                    ExerciseSortMode.entries.forEach { mode ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (sortMode == mode) {
                                                        Icon(Icons.Default.Check, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(18.dp))
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                    } else {
                                                        Spacer(modifier = Modifier.width(26.dp))
                                                    }
                                                    Text(mode.label)
                                                }
                                            },
                                            onClick = { sortMode = mode }
                                        )
                                    }
                                    HorizontalDivider(color = BorderLight.copy(alpha = 0.5f))
                                    Text(
                                        "Equipment",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = TextMuted,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                    )
                                    EquipmentFilterMode.entries.forEach { mode ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (equipmentFilter == mode) {
                                                        Icon(Icons.Default.Check, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(18.dp))
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                    } else {
                                                        Spacer(modifier = Modifier.width(26.dp))
                                                    }
                                                    Text(mode.label)
                                                }
                                            },
                                            onClick = { equipmentFilter = mode }
                                        )
                                    }
                                }
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredExercises) { exercise ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.extraSmall)
                                        .clickable { onExerciseSelected(exercise) }
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ExerciseThumbnail(
                                            exerciseName = exercise.name,
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = exercise.name,
                                                fontWeight = FontWeight.Bold,
                                                color = TextDark
                                            )
                                            Text(
                                                text = "${exercise.bodyPart} • ${exercise.type}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextBlue
                                            )
                                            val logCount = usageStats[exercise.id]?.logCount ?: 0
                                            Text(
                                                text = if (logCount > 0) "Logged ${logCount}x" else "Not logged yet",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextMuted
                                            )
                                        }
                                    }
                                    Icon(
                                        Icons.Default.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = BluePrimary
                                    )
                                }
                                HorizontalDivider(color = BorderLight.copy(alpha = 0.5f))
                            }
                        }
                    } else {
                        // Muscle Group Tab
                        if (selectedMuscleGroup == null) {
                            // Ordered anatomically (upper body top-down, then lower body,
                            // then the catch-alls) so related muscles sit next to each other.
                            val muscleGroups = listOf(
                                "Chest", "Shoulders", "Traps", "Back", "Lats",
                                "Biceps", "Triceps", "Core",
                                "Glutes", "Quads", "Hamstrings", "Calves",
                                "Full Body", "Cardio"
                            )
                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(muscleGroups) { muscle ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedMuscleGroup = muscle },
                                        shape = MaterialTheme.shapes.small,
                                        colors = CardDefaults.cardColors(containerColor = LightBlueContainer.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = muscle,
                                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                                color = TextDark
                                            )
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowRight,
                                                contentDescription = null,
                                                tint = BluePrimary
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            val muscleFilteredExercises = remember(searchQuery, exercises, selectedMuscleGroup, sortMode, equipmentFilter) {
                                sortExercises(
                                    exercises.filter {
                                        (it.bodyPart.contains(selectedMuscleGroup!!, ignoreCase = true)) &&
                                        (searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true)) &&
                                        passesEquipmentFilter(it)
                                    }
                                )
                            }
 
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("Search in $selectedMuscleGroup...") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box {
                                    IconButton(
                                        onClick = { showSortFilterMenu = true },
                                        modifier = Modifier
                                            .clip(MaterialTheme.shapes.medium)
                                            .border(1.dp, BorderLight, MaterialTheme.shapes.medium)
                                    ) {
                                        Icon(Icons.Default.FilterList, contentDescription = "Sort and filter", tint = BluePrimary)
                                    }
                                    DropdownMenu(
                                        expanded = showSortFilterMenu,
                                        onDismissRequest = { showSortFilterMenu = false }
                                    ) {
                                        Text(
                                            "Sort by",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = TextMuted,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                        )
                                        ExerciseSortMode.entries.forEach { mode ->
                                            DropdownMenuItem(
                                                text = {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        if (sortMode == mode) {
                                                            Icon(Icons.Default.Check, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(18.dp))
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                        } else {
                                                            Spacer(modifier = Modifier.width(26.dp))
                                                        }
                                                        Text(mode.label)
                                                    }
                                                },
                                                onClick = { sortMode = mode }
                                            )
                                        }
                                        HorizontalDivider(color = BorderLight.copy(alpha = 0.5f))
                                        Text(
                                            "Equipment",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = TextMuted,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                        )
                                        EquipmentFilterMode.entries.forEach { mode ->
                                            DropdownMenuItem(
                                                text = {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        if (equipmentFilter == mode) {
                                                            Icon(Icons.Default.Check, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(18.dp))
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                        } else {
                                                            Spacer(modifier = Modifier.width(26.dp))
                                                        }
                                                        Text(mode.label)
                                                    }
                                                },
                                                onClick = { equipmentFilter = mode }
                                            )
                                        }
                                    }
                                }
                            }

                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(muscleFilteredExercises) { exercise ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(MaterialTheme.shapes.extraSmall)
                                            .clickable { onExerciseSelected(exercise) }
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            ExerciseThumbnail(
                                                exerciseName = exercise.name,
                                                modifier = Modifier.size(40.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = exercise.name,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextDark
                                                )
                                                Text(
                                                    text = "${exercise.bodyPart} • ${exercise.type}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = TextBlue
                                                )
                                                val logCount = usageStats[exercise.id]?.logCount ?: 0
                                                Text(
                                                    text = if (logCount > 0) "Logged ${logCount}x" else "Not logged yet",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextMuted
                                                )
                                            }
                                        }
                                        Icon(
                                            Icons.Default.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = BluePrimary
                                        )
                                    }
                                    HorizontalDivider(color = BorderLight.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
 
                    Spacer(modifier = Modifier.height(8.dp))
 
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = TextMuted)
                        }
 
                        if (onCreateExercise != null) {
                            TextButton(onClick = { isCreating = true }) {
                                Text("Create", color = BluePrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
 
    if (showBodyPartPicker) {
        BodyPartPickerDialog(
            selectedParts = selectedBodyParts,
            onDismissRequest = { showBodyPartPicker = false },
            onApply = { parts ->
                selectedBodyParts = parts
                showBodyPartPicker = false
            }
        )
    }
}
 
@Composable
fun BodyPartPickerDialog(
    selectedParts: Set<String>,
    onDismissRequest: () -> Unit,
    onApply: (Set<String>) -> Unit
) {
    var tempSelected by remember(selectedParts) { mutableStateOf(selectedParts) }
    val bodyPartsList = listOf("Chest", "Back", "Shoulders", "Biceps", "Triceps", "Legs", "Core", "Glutes", "Calves", "Cardio", "Full Body")
 
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onDismissRequest() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .clickable(enabled = false) { /* stop propagation */ },
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Select Body Parts",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextBlue,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
 
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                    ) {
                        items(bodyPartsList.size) { index ->
                            val part = bodyPartsList[index]
                            val isSelected = tempSelected.contains(part)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clickable {
                                        tempSelected = if (isSelected) {
                                            tempSelected - part
                                        } else {
                                            tempSelected + part
                                        }
                                    },
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) LightBlueContainer else Color.Transparent
                                ),
                                border = BorderStroke(
                                    1.5.dp,
                                    if (isSelected) BluePrimary else BorderLight
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 8.dp)
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            tempSelected = if (checked == true) {
                                                tempSelected + part
                                            } else {
                                                tempSelected - part
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = BluePrimary)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = part,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (isSelected) BluePrimary else TextDark
                                    )
                                }
                            }
                        }
                    }
 
                    Spacer(modifier = Modifier.height(20.dp))
 
                    Button(
                        onClick = { onApply(tempSelected) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Apply", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

