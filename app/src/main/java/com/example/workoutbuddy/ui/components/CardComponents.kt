package com.example.workoutbuddy.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.workoutbuddy.data.database.WorkoutSetEntity
import com.example.workoutbuddy.theme.*
import com.example.workoutbuddy.ui.util.pressScale
import com.example.workoutbuddy.viewmodel.ActiveExerciseState

@Composable
fun ExerciseListItem(
    exerciseState: ActiveExerciseState,
    isWorkoutStarted: Boolean,
    onReplaceExercise: () -> Unit,
    onRemoveExercise: () -> Unit,
    onClick: () -> Unit,
    currentFrequency: com.example.workoutbuddy.data.Frequency? = null,
    onSetFrequency: (com.example.workoutbuddy.data.Frequency?) -> Unit = {}
) {
    val totalSets = exerciseState.sets.size
    val completedSets = exerciseState.sets.count { it.isCompleted }
    val isCompleted = totalSets > 0 && completedSets == totalSets
    
    val repsText = remember(exerciseState.sets, exerciseState.exercise.type) {
        val firstSet = exerciseState.sets.firstOrNull()
        when (exerciseState.exercise.type) {
            "LIFT" -> {
                val reps = firstSet?.recommendedReps ?: 10
                "$reps Reps"
            }
            "CARDIO" -> {
                val time = firstSet?.recommendedTime ?: 600
                formatTime(time)
            }
            "HOLD" -> {
                val time = firstSet?.recommendedTime ?: 60
                "$time Secs"
            }
            else -> ""
        }
    }

    val cardInteractionSource = remember { MutableInteractionSource() }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(cardInteractionSource)
            .clickable(interactionSource = cardInteractionSource, indication = LocalIndication.current) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) LightBlueContainer else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, if (isCompleted) BluePrimary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(76.dp)) {
                ExerciseThumbnail(
                    exerciseName = exerciseState.exercise.name,
                    modifier = Modifier.size(76.dp)
                )
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = GreenSuccess,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(20.dp)
                            .background(Color.White, CircleShape)
                            .padding(1.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exerciseState.exercise.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextDark
                )
                Text(
                    text = exerciseState.exercise.bodyPart,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExerciseInfoChip("$totalSets Sets")
                    if (repsText.isNotEmpty()) {
                        ExerciseInfoChip(repsText)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                run {
                    var showMenu by remember { mutableStateOf(false) }
                    var showFrequencyDialog by remember { mutableStateOf(false) }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Exercise Options",
                                tint = TextMuted
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
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Control frequency") },
                                onClick = {
                                    showMenu = false
                                    showFrequencyDialog = true
                                }
                            )
                        }
                    }

                    if (showFrequencyDialog) {
                        Dialog(onDismissRequest = { showFrequencyDialog = false }) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.extraLarge,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text(
                                        text = "How often should we show ${exerciseState.exercise.name}?",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TextDark
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    FrequencySlider(
                                        currentFrequency = currentFrequency,
                                        onFrequencyChange = { freq ->
                                            onSetFrequency(freq)
                                            // Never triggers an immediate swap to a different
                                            // exercise, so this card no longer refers to what
                                            // the dialog's title/state was about - close it
                                            // rather than leave it open on a stale exercise.
                                            if (freq == com.example.workoutbuddy.data.Frequency.NEVER) {
                                                showFrequencyDialog = false
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Set-completion progress
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                progress = { if (totalSets > 0) completedSets.toFloat() / totalSets else 0f },
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp),
                color = BluePrimary,
                trackColor = BorderLight,
                strokeCap = StrokeCap.Round
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "$completedSets/$totalSets",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = TextBlue
            )
        }
        }
    }
}


@Composable
fun SetRowItem(
    set: WorkoutSetEntity,
    type: String,
    exerciseName: String,
    isWorkoutStarted: Boolean,
    onStartCountdown: (Long, String, Int) -> Unit,
    onValuesChanged: (Double?, Int?, Int?, Double?, Double?) -> Unit,
    onCompleteToggled: (Boolean, Double?, Int?, Int?, Double?, Double?) -> Unit
) {
    var weightInput by remember(set.id) {
        val initialText = set.weight?.toString() ?: set.recommendedWeight?.toString() ?: ""
        mutableStateOf(TextFieldValue(initialText))
    }
    var repsInput by remember(set.id) {
        val initialText = set.reps?.toString() ?: set.recommendedReps?.toString() ?: ""
        mutableStateOf(TextFieldValue(initialText))
    }
    var distanceInput by remember(set.id) {
        val initialText = set.distance?.toString() ?: set.recommendedDistance?.toString() ?: ""
        mutableStateOf(TextFieldValue(initialText))
    }
    var inclineInput by remember(set.id) {
        val initialText = set.inclinePct?.toString() ?: ""
        mutableStateOf(TextFieldValue(initialText))
    }

    var localTime by remember(set.id, set.time, set.recommendedTime) {
        mutableStateOf(set.time ?: set.recommendedTime)
    }

    var weightFocused by remember { mutableStateOf(false) }
    LaunchedEffect(set.weight, set.recommendedWeight) {
        if (!weightFocused) {
            weightInput = TextFieldValue(set.weight?.toString() ?: set.recommendedWeight?.toString() ?: "")
        }
    }

    var repsFocused by remember { mutableStateOf(false) }
    LaunchedEffect(set.reps, set.recommendedReps) {
        if (!repsFocused) {
            repsInput = TextFieldValue(set.reps?.toString() ?: set.recommendedReps?.toString() ?: "")
        }
    }

    var distanceFocused by remember { mutableStateOf(false) }
    LaunchedEffect(set.distance, set.recommendedDistance) {
        if (!distanceFocused) {
            distanceInput = TextFieldValue(set.distance?.toString() ?: set.recommendedDistance?.toString() ?: "")
        }
    }

    var inclineFocused by remember { mutableStateOf(false) }
    LaunchedEffect(set.inclinePct) {
        if (!inclineFocused) {
            inclineInput = TextFieldValue(set.inclinePct?.toString() ?: "")
        }
    }

    // Force select-all on focus changes
    LaunchedEffect(repsFocused) {
        if (repsFocused) {
            kotlinx.coroutines.delay(50)
            repsInput = repsInput.copy(selection = TextRange(0, repsInput.text.length))
        }
    }
    LaunchedEffect(weightFocused) {
        if (weightFocused) {
            kotlinx.coroutines.delay(50)
            weightInput = weightInput.copy(selection = TextRange(0, weightInput.text.length))
        }
    }
    LaunchedEffect(distanceFocused) {
        if (distanceFocused) {
            kotlinx.coroutines.delay(50)
            distanceInput = distanceInput.copy(selection = TextRange(0, distanceInput.text.length))
        }
    }
    LaunchedEffect(inclineFocused) {
        if (inclineFocused) {
            kotlinx.coroutines.delay(50)
            inclineInput = inclineInput.copy(selection = TextRange(0, inclineInput.text.length))
        }
    }

    val rowFlash = remember { Animatable(0f) }
    LaunchedEffect(set.isCompleted) {
        if (set.isCompleted) {
            rowFlash.snapTo(1f)
            rowFlash.animateTo(0f, animationSpec = tween(durationMillis = 600))
        }
    }
    val flashColor = if (set.isPR) GoldPR else GreenSuccess
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(flashColor.copy(alpha = 0.25f * rowFlash.value))
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Styled circular badge for Set number
        Box(
            modifier = Modifier.width(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (set.isCompleted) GreenSuccess.copy(alpha = 0.15f) else LightBlueContainer)
                    .border(1.5.dp, if (set.isCompleted) GreenSuccess else BluePrimary.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${set.setNumber}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (set.isCompleted) GreenSuccess else BluePrimary
                )
            }
        }

        // Inputs Column
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            if (isWorkoutStarted) {
                when (type) {
                    "LIFT" -> { // Reps input first
                        OutlinedTextField(
                            value = repsInput,
                            onValueChange = {
                                repsInput = it
                                val r = it.text.toIntOrNull()
                                if (r != null) {
                                    val w = weightInput.text.toDoubleOrNull()
                                    onValuesChanged(w, r, null, null, null)
                                }
                            },
                            modifier = Modifier
                                .width(80.dp)
                                .height(50.dp)
                                .onFocusChanged { focusState ->
                                    repsFocused = focusState.isFocused
                                },
                            textStyle = TextStyle(fontSize = 16.sp, color = TextDark),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BluePrimary,
                                unfocusedBorderColor = BorderLight
                            ),
                            placeholder = { Text("reps", fontSize = 14.sp) }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("x", color = TextMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        // Weight input second
                        OutlinedTextField(
                            value = weightInput,
                            onValueChange = {
                                weightInput = it
                                val w = it.text.toDoubleOrNull()
                                if (w != null) {
                                    val r = repsInput.text.toIntOrNull()
                                    onValuesChanged(w, r, null, null, null)
                                }
                            },
                            modifier = Modifier
                                .width(88.dp)
                                .height(50.dp)
                                .onFocusChanged { focusState ->
                                    weightFocused = focusState.isFocused
                                },
                            textStyle = TextStyle(fontSize = 16.sp, color = TextDark),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BluePrimary,
                                unfocusedBorderColor = BorderLight
                            ),
                            placeholder = { Text("kg", fontSize = 14.sp) }
                        )
                    }
                    "CARDIO" -> {
                        val isJumpRope = exerciseName.contains("Jump Rope", ignoreCase = true)
                        val isIncline = !isJumpRope && (exerciseName.contains("Walking", ignoreCase = true) ||
                                        exerciseName.contains("Running", ignoreCase = true) ||
                                        exerciseName.contains("Cycling", ignoreCase = true))
                        
                        // Time input (HH:MM:SS format using Calculator input)
                        TimeCalculatorTextField(
                            initialSeconds = set.time ?: set.recommendedTime,
                            onSecondsChanged = { t ->
                                localTime = t
                                val d = if (isJumpRope) null else distanceInput.text.toDoubleOrNull()
                                val inc = if (isIncline) inclineInput.text.toDoubleOrNull() else null
                                onValuesChanged(null, null, t, d, inc)
                            },
                            modifier = Modifier.width(if (isJumpRope) 180.dp else if (isIncline) 80.dp else 100.dp).height(50.dp)
                        )
                        
                        if (!isJumpRope) {
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            // Distance input (km)
                            OutlinedTextField(
                                value = distanceInput,
                                onValueChange = {
                                    distanceInput = it
                                },
                                modifier = Modifier
                                    .width(if (isIncline) 64.dp else 72.dp)
                                    .height(50.dp)
                                    .onFocusChanged { focusState ->
                                        val wasFocused = distanceFocused
                                        distanceFocused = focusState.isFocused
                                        if (wasFocused && !focusState.isFocused) {
                                            val d = distanceInput.text.toDoubleOrNull()
                                            val t = localTime
                                            val inc = if (isIncline) inclineInput.text.toDoubleOrNull() else null
                                            onValuesChanged(null, null, t, d, inc)
                                        }
                                    },
                                textStyle = TextStyle(fontSize = 16.sp, color = TextDark),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BluePrimary,
                                    unfocusedBorderColor = BorderLight
                                ),
                                placeholder = { Text("km", fontSize = 14.sp) }
                            )

                            if (isIncline) {
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                // Incline input (%)
                                OutlinedTextField(
                                    value = inclineInput,
                                    onValueChange = {
                                        inclineInput = it
                                    },
                                    modifier = Modifier
                                        .width(64.dp)
                                        .height(50.dp)
                                        .onFocusChanged { focusState ->
                                            val wasFocused = inclineFocused
                                            inclineFocused = focusState.isFocused
                                            if (wasFocused && !focusState.isFocused) {
                                                val inc = inclineInput.text.toDoubleOrNull()
                                                val d = distanceInput.text.toDoubleOrNull()
                                                val t = localTime
                                                onValuesChanged(null, null, t, d, inc)
                                            }
                                        },
                                    textStyle = TextStyle(fontSize = 16.sp, color = TextDark),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BluePrimary,
                                        unfocusedBorderColor = BorderLight
                                    ),
                                    placeholder = { Text("%", fontSize = 14.sp) }
                                )
                            }
                        }
 
                        Spacer(modifier = Modifier.weight(1f))
 
                        IconButton(onClick = {
                            val seconds = localTime ?: 60
                            onStartCountdown(set.id, exerciseName, seconds)
                        }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Start Countdown Timer", tint = BluePrimary, modifier = Modifier.size(20.dp))
                        }
                    }
                    "HOLD" -> {
                        // Time input (HH:MM:SS format using Calculator input)
                        TimeCalculatorTextField(
                            initialSeconds = set.time ?: set.recommendedTime,
                            onSecondsChanged = { t ->
                                localTime = t
                                onValuesChanged(null, null, t, null, null)
                            },
                            modifier = Modifier.width(100.dp).height(50.dp)
                        )
 
                        Spacer(modifier = Modifier.weight(1f))
 
                        IconButton(onClick = {
                            val seconds = localTime ?: 60
                            onStartCountdown(set.id, exerciseName, seconds)
                        }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Start Countdown Timer", tint = BluePrimary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            } else {
                Text(text = "Start workout to log", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        }

        // Tappable check/star completion icon on the right
        Box(
            modifier = Modifier
                .width(48.dp)
                .clickable(enabled = isWorkoutStarted) {
                    val w = if (type == "LIFT") weightInput.text.toDoubleOrNull() else null
                    val r = if (type == "LIFT") repsInput.text.toIntOrNull() else null
                    val t = if (type == "CARDIO" || type == "HOLD") (set.time ?: set.recommendedTime) else null
                    val d = if (type == "CARDIO" && !exerciseName.contains("Jump Rope", ignoreCase = true)) distanceInput.text.toDoubleOrNull() else null
                    val inc = if (type == "CARDIO" && (exerciseName.contains("Walking", ignoreCase = true) || exerciseName.contains("Running", ignoreCase = true) || exerciseName.contains("Cycling", ignoreCase = true))) inclineInput.text.toDoubleOrNull() else null
                    onCompleteToggled(!set.isCompleted, w, r, t, d, inc)
                },
            contentAlignment = Alignment.Center
        ) {
            val iconScale = remember { Animatable(1f) }
            LaunchedEffect(set.isCompleted, set.isPR) {
                if (set.isCompleted) {
                    iconScale.snapTo(0.5f)
                    iconScale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                }
            }
            if (set.isCompleted) {
                if (set.isPR) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "PR",
                        tint = GoldPR,
                        modifier = Modifier.size(24.dp).scale(iconScale.value)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = GreenSuccess,
                        modifier = Modifier.size(24.dp).scale(iconScale.value)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Not Completed",
                    tint = BorderLight,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

