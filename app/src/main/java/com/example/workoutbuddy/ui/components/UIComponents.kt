package com.example.workoutbuddy.ui.components

import android.content.Intent
import android.net.Uri
import kotlin.math.roundToInt
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.workoutbuddy.audio.AppSound
import com.example.workoutbuddy.ui.util.LocalSoundPlayer
import com.example.workoutbuddy.ui.util.pressScale
import com.example.workoutbuddy.data.Equipment
import com.example.workoutbuddy.data.database.ExerciseEntity
import com.example.workoutbuddy.data.database.ExerciseUsageStat
import com.example.workoutbuddy.data.database.WorkoutEntity
import com.example.workoutbuddy.data.database.WorkoutSetEntity
import com.example.workoutbuddy.viewmodel.ActiveExerciseState
import com.example.workoutbuddy.theme.*
import java.text.SimpleDateFormat
import java.util.*

// --- Custom Calendar Widget ---

@Composable
fun CalendarWidget(
    completedWorkouts: List<WorkoutEntity>,
    selectedDate: Long?,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var calendar by remember { mutableStateOf(Calendar.getInstance()) }
    val currentMonthName = remember(calendar) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
    }

    val days = remember(calendar) {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        
        val tempCal = Calendar.getInstance()
        tempCal.set(Calendar.YEAR, year)
        tempCal.set(Calendar.MONTH, month)
        tempCal.set(Calendar.DAY_OF_MONTH, 1)
        
        val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK)
        val daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        val list = mutableListOf<Long?>()
        // Padding for empty start slots
        for (i in 1 until firstDayOfWeek) {
            list.add(null)
        }
        // Actual days
        for (day in 1..daysInMonth) {
            tempCal.set(Calendar.DAY_OF_MONTH, day)
            // Save midnight timestamp
            tempCal.set(Calendar.HOUR_OF_DAY, 0)
            tempCal.set(Calendar.MINUTE, 0)
            tempCal.set(Calendar.SECOND, 0)
            tempCal.set(Calendar.MILLISECOND, 0)
            list.add(tempCal.timeInMillis)
        }
        // Padding for empty end slots to make it a multiple of 7
        while (list.size % 7 != 0) {
            list.add(null)
        }
        list
    }

    var dragAmountSum by remember { mutableStateOf(0f) }
    val soundPlayer = LocalSoundPlayer.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(calendar) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAmountSum = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragAmountSum += dragAmount
                    },
                    onDragEnd = {
                        val threshold = 100f // threshold in pixels to trigger swipe
                        if (dragAmountSum > threshold) {
                            // Swiped right -> Previous Month
                            val newCal = Calendar.getInstance().apply {
                                timeInMillis = calendar.timeInMillis
                                add(Calendar.MONTH, -1)
                            }
                            calendar = newCal
                        } else if (dragAmountSum < -threshold) {
                            // Swiped left -> Next Month
                            val newCal = Calendar.getInstance().apply {
                                timeInMillis = calendar.timeInMillis
                                add(Calendar.MONTH, 1)
                            }
                            calendar = newCal
                        }
                    }
                )
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Month Selector Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val newCal = Calendar.getInstance().apply {
                        timeInMillis = calendar.timeInMillis
                        add(Calendar.MONTH, -1)
                    }
                    calendar = newCal
                }) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous Month", tint = BluePrimary)
                }
                
                Text(
                    text = currentMonthName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextDark
                )
                
                IconButton(onClick = {
                    val newCal = Calendar.getInstance().apply {
                        timeInMillis = calendar.timeInMillis
                        add(Calendar.MONTH, 1)
                    }
                    calendar = newCal
                }) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next Month", tint = BluePrimary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Days of the Week Header
            val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            Row(modifier = Modifier.fillMaxWidth()) {
                daysOfWeek.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar Days Grid - AnimatedContent slides/fades between months on swipe/arrow nav
            AnimatedContent(
                targetState = days,
                label = "calendarMonthTransition",
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(220)) }
            ) { animatedDays ->
            val rows = animatedDays.chunked(7)
            Column {
            rows.forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    week.forEach { dateTimestamp ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (dateTimestamp != null) {
                                val dayCal = Calendar.getInstance().apply { timeInMillis = dateTimestamp }
                                val dayNum = dayCal.get(Calendar.DAY_OF_MONTH)
                                
                                val isSelected = selectedDate != null && isSameDay(dateTimestamp, selectedDate)
                                val isToday = isSameDay(dateTimestamp, System.currentTimeMillis())

                                val workoutsOnDay = completedWorkouts.filter { isSameDay(it.date, dateTimestamp) }
                                val hasWorkout = workoutsOnDay.isNotEmpty()

                                val animatedBgColor by animateColorAsState(
                                    targetValue = when {
                                        isSelected -> BluePrimary
                                        isToday -> BluePrimary.copy(alpha = 0.1f)
                                        else -> Color.Transparent
                                    },
                                    label = "dayCellBackground"
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(animatedBgColor)
                                        .clickable {
                                            soundPlayer.play(AppSound.BUTTON_TAP)
                                            onDateSelected(dateTimestamp)
                                        },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = dayNum.toString(),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = when {
                                            isSelected -> Color.White
                                            isToday -> BluePrimary
                                            else -> TextDark
                                        }
                                    )

                                    AnimatedVisibility(visible = hasWorkout, enter = fadeIn(), exit = fadeOut()) {
                                        Row(
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            workoutsOnDay.take(3).forEach { workout ->
                                                val dotColor = when (workout.category) {
                                                    "PUSH" -> BluePrimary
                                                    "PULL" -> GreenSuccess
                                                    "LOWER_BODY" -> GoldPR
                                                    else -> BlueSecondary
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .padding(horizontal = 0.5.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isSelected) Color.White else dotColor)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }
            }
        }
    }
}

private fun isSameDay(time1: Long, time2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

// Helper to generate sanitized resource name matching "ic_ex_..."
fun getExerciseDrawableResourceName(name: String): String {
    val clean = name.lowercase()
        .replace(Regex("[^a-z0-9]"), "_")
        .replace(Regex("_+"), "_")
        .trim('_')
    return "ic_ex_$clean"
}

@Composable
fun ExerciseThumbnail(
    exerciseName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val resName = remember(exerciseName) { getExerciseDrawableResourceName(exerciseName) }
    val resId = remember(resName) {
        context.resources.getIdentifier(resName, "drawable", context.packageName)
    }

    Crossfade(targetState = resId, label = "exerciseThumbnailCrossfade") { animatedResId ->
        if (animatedResId != 0) {
            Image(
                painter = painterResource(id = animatedResId),
                contentDescription = "$exerciseName thumbnail",
                modifier = modifier
                    .clip(MaterialTheme.shapes.extraSmall),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = modifier
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(LightBlueContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = "Placeholder",
                    tint = BluePrimary,
                    modifier = Modifier.fillMaxSize(0.5f)
                )
            }
        }
    }
}

// --- Exercise List Item ---

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
    val remainingSets = exerciseState.sets.count { !it.isCompleted }
    val isCompleted = exerciseState.sets.isNotEmpty() && exerciseState.sets.all { it.isCompleted }
    
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(52.dp)) {
                ExerciseThumbnail(
                    exerciseName = exerciseState.exercise.name,
                    modifier = Modifier.size(52.dp)
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
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exerciseState.exercise.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextDark
                )
                Text(
                    text = "${exerciseState.exercise.bodyPart} • ($remainingSets Sets Left. $repsText)",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = TextBlue // Blue subtext!
                )
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
    }
}

// --- Exercise Detail Bottom Sheet (Slide-Up View) ---

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

// --- Exercise How-To Bottom Sheet ---

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

// --- Time Calculator Text Field ---

@Composable
fun TimeCalculatorTextField(
    initialSeconds: Int?,
    onSecondsChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "0:00"
) {
    var rawDigits by remember {
        val initialText = if (initialSeconds != null && initialSeconds > 0) {
            val h = initialSeconds / 3600
            val m = (initialSeconds % 3600) / 60
            val s = initialSeconds % 60
            val totalStr = String.format("%d%02d%02d", h, m, s)
            totalStr.trimStart('0').take(5)
        } else {
            ""
        }
        mutableStateOf(initialText)
    }

    var isFocused by remember { mutableStateOf(false) }
    var lastFocusState by remember { mutableStateOf(false) }

    var textFieldValue by remember {
        val initialText = formatSecondsToDisplayString(initialSeconds ?: 0)
        mutableStateOf(TextFieldValue(text = initialText))
    }

    // Sync state when focus changes
    val focusModifier = modifier.onFocusChanged { focusState ->
        if (focusState.isFocused != lastFocusState) {
            lastFocusState = focusState.isFocused
            isFocused = focusState.isFocused
            if (!focusState.isFocused) {
                val finalSeconds = convertDigitsToSeconds(rawDigits)
                onSecondsChanged(finalSeconds)
                val dispStr = formatSecondsToDisplayString(finalSeconds)
                textFieldValue = TextFieldValue(text = dispStr)
            }
        }
    }

    LaunchedEffect(initialSeconds) {
        if (!isFocused) {
            val finalSeconds = initialSeconds ?: 0
            val dispStr = formatSecondsToDisplayString(finalSeconds)
            textFieldValue = TextFieldValue(text = dispStr)
            val h = finalSeconds / 3600
            val m = (finalSeconds % 3600) / 60
            val s = finalSeconds % 60
            val totalStr = String.format("%d%02d%02d", h, m, s)
            rawDigits = totalStr.trimStart('0').take(5)
        }
    }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            kotlinx.coroutines.delay(50)
            val editStr = formatSecondsToEditingString(convertDigitsToSeconds(rawDigits))
            textFieldValue = TextFieldValue(
                text = editStr,
                selection = TextRange(0, editStr.length)
            )
        }
    }

    OutlinedTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            if (!isFocused) {
                textFieldValue = newValue
                return@OutlinedTextField
            }

            val oldText = textFieldValue.text
            val newText = newValue.text

            if (newText == oldText) {
                // Only selection/cursor position changed (user clicked)
                textFieldValue = newValue
                return@OutlinedTextField
            }

            val oldDigits = oldText.filter { it.isDigit() }
            val newDigits = newText.filter { it.isDigit() }

            val isSelectAll = textFieldValue.selection.start == 0 && textFieldValue.selection.end == oldText.length && oldText.isNotEmpty()
            if (isSelectAll) {
                val wasDeletion = newText.length < oldText.length && newDigits.isEmpty()
                if (wasDeletion) {
                    rawDigits = ""
                } else {
                    rawDigits = newDigits.take(5)
                }
                val formatted = formatRawDigitsToEditingString(rawDigits)
                textFieldValue = TextFieldValue(
                    text = formatted,
                    selection = TextRange(formatted.length)
                )
                onSecondsChanged(convertDigitsToSeconds(rawDigits))
                return@OutlinedTextField
            }

            if (newDigits.length < oldDigits.length) {
                // Backspace / Deletion
                if (rawDigits.isNotEmpty()) {
                    rawDigits = rawDigits.dropLast(1)
                }
                val formatted = formatRawDigitsToEditingString(rawDigits)
                textFieldValue = TextFieldValue(
                    text = formatted,
                    selection = TextRange(formatted.length)
                )
                onSecondsChanged(convertDigitsToSeconds(rawDigits))
            } else {
                // Insertion / Typing
                val parsedDigits = newDigits.trimStart('0')
                if (parsedDigits.length <= 5) {
                    rawDigits = parsedDigits
                    val formatted = formatRawDigitsToEditingString(rawDigits)
                    textFieldValue = TextFieldValue(
                        text = formatted,
                        selection = TextRange(formatted.length)
                    )
                    onSecondsChanged(convertDigitsToSeconds(rawDigits))
                }
            }
        },
        modifier = focusModifier,
        textStyle = TextStyle(fontSize = 14.sp, color = TextDark, textAlign = TextAlign.Center),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BluePrimary,
            unfocusedBorderColor = BorderLight
        ),
        placeholder = { Text(placeholder, fontSize = 12.sp) }
    )
}

private fun formatSecondsToDisplayString(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        String.format("%d:%02d:%02d", h, m, s)
    } else {
        String.format("%d:%02d", m, s)
    }
}

private fun formatSecondsToEditingString(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return String.format("%02d:%02d:%02d", h, m, s)
}

private fun formatRawDigitsToEditingString(digits: String): String {
    val padded = digits.padStart(5, '0')
    val h = padded.substring(0, 1).toInt()
    val m = padded.substring(1, 3).toInt()
    val s = padded.substring(3, 5).toInt()
    return String.format("%02d:%02d:%02d", h, m, s)
}

private fun convertDigitsToSeconds(digits: String): Int {
    if (digits.isEmpty()) return 0
    val padded = digits.padStart(5, '0')
    val h = padded.substring(0, 1).toInt()
    val m = padded.substring(1, 3).toInt()
    val s = padded.substring(3, 5).toInt()
    return h * 3600 + m * 60 + s
}

// --- Countdown Dialog ---

@Composable
fun CountdownTimerDialog(
    exerciseName: String,
    remainingSeconds: Int,
    totalDuration: Int,
    isPaused: Boolean,
    onTapTimer: () -> Unit,
    onDone: () -> Unit,
    onCancel: () -> Unit,
    onMinimize: () -> Unit
) {
    val soundPlayer = LocalSoundPlayer.current
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
    }
    Dialog(onDismissRequest = onCancel) {
        Card(
            modifier = Modifier
                .width(320.dp)
                .scale(0.85f + 0.15f * entrance.value)
                .alpha(entrance.value),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = exerciseName,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = TextDark,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Circular Timer Representation - Center aligned & clickable to pause/resume
                val urgent = !isPaused && remainingSeconds in 1..3
                val urgencyScale by animateFloatAsState(
                    targetValue = if (urgent) 1.08f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
                    label = "urgencyPulse"
                )
                val urgencyTextColor by animateColorAsState(
                    targetValue = if (urgent) GoldPR else TextDark,
                    label = "urgencyTextColor"
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(160.dp)
                        .scale(urgencyScale)
                        .clip(CircleShape)
                        .clickable { onTapTimer() }
                ) {
                    val progress = if (totalDuration > 0) remainingSeconds.toFloat() / totalDuration else 0f
                    CircularProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxSize(),
                        color = if (isPaused) GoldPR else BluePrimary,
                        trackColor = BorderLight,
                        strokeWidth = 8.dp
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = formatTime(remainingSeconds),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = urgencyTextColor
                        )
                        if (isPaused) {
                            Text(
                                text = "PAUSED",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = GoldPR,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(28.dp))
                
                // Buttons at bottom: Cancel, Minimize, and Done side by side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { soundPlayer.play(AppSound.BUTTON_TAP); onCancel() },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text("Cancel", color = RedDanger, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = { soundPlayer.play(AppSound.BUTTON_TAP); onMinimize() },
                        modifier = Modifier.weight(1.2f),
                        shape = MaterialTheme.shapes.medium,
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text("Minimize", fontSize = 13.sp)
                    }

                    Button(
                        onClick = { soundPlayer.play(AppSound.BUTTON_TAP); onDone() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                        shape = MaterialTheme.shapes.medium,
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text("Done", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}


@Composable
fun RestTimerModal(
    exerciseName: String,
    remainingSeconds: Int,
    totalDuration: Int,
    onSkip: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val soundPlayer = LocalSoundPlayer.current
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
    }
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f * entrance.value))
                .clickable { onDismissRequest() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .alpha(entrance.value)
                    .scale(0.9f + 0.1f * entrance.value)
                    .clickable(enabled = false) { /* stop propagation */ },
                shape = MaterialTheme.shapes.large,
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
                        text = "Rest Timer",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextBlue
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val restUrgent = remainingSeconds in 1..3
                    val urgencyScale by animateFloatAsState(
                        targetValue = if (restUrgent) 1.08f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
                        label = "restTimerUrgencyPulse"
                    )
                    val restUrgencyTextColor by animateColorAsState(
                        targetValue = if (restUrgent) GoldPR else TextDark,
                        label = "restTimerUrgencyTextColor"
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(100.dp).scale(urgencyScale)
                    ) {
                        val progress = if (totalDuration > 0) remainingSeconds.toFloat() / totalDuration else 0f
                        CircularProgressIndicator(
                            progress = progress,
                            modifier = Modifier.fillMaxSize(),
                            color = if (restUrgent) GoldPR else BluePrimary,
                            trackColor = BorderLight,
                            strokeWidth = 6.dp
                        )
                        Text(
                            text = formatTime(remainingSeconds),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = restUrgencyTextColor
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { soundPlayer.play(AppSound.BUTTON_TAP); onDismissRequest() },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Minimize")
                        }
                        Button(
                            onClick = { soundPlayer.play(AppSound.BUTTON_TAP); onSkip() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Skip", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// --- Cooldown Banner ---

@Composable
fun CooldownBanner(
    exerciseName: String,
    remainingSeconds: Int,
    totalDuration: Int,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = LightBlueContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Mini timer
                val cooldownUrgent = remainingSeconds in 1..3
                val cooldownPulse by animateFloatAsState(
                    targetValue = if (cooldownUrgent) 1.12f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
                    label = "cooldownBannerPulse"
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(36.dp).scale(cooldownPulse)
                ) {
                    val progress = if (totalDuration > 0) remainingSeconds.toFloat() / totalDuration else 0f
                    CircularProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxSize(),
                        color = if (cooldownUrgent) GoldPR else BluePrimary,
                        trackColor = Color.White.copy(alpha = 0.5f),
                        strokeWidth = 3.dp
                    )
                    Text(
                        text = remainingSeconds.toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (cooldownUrgent) GoldPR else TextBlue
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Rest",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextDark
                    )
                }
            }
            
            val soundPlayer = LocalSoundPlayer.current
            TextButton(
                onClick = { soundPlayer.play(AppSound.BUTTON_TAP); onSkip() },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("Skip", color = BluePrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

// --- Countdown Banner ---

@Composable
fun CountdownBanner(
    exerciseName: String,
    remainingSeconds: Int,
    totalDuration: Int,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = LightBlueContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Mini timer
                val countdownUrgent = remainingSeconds in 1..3
                val countdownPulse by animateFloatAsState(
                    targetValue = if (countdownUrgent) 1.12f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
                    label = "countdownBannerPulse"
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(36.dp).scale(countdownPulse)
                ) {
                    val progress = if (totalDuration > 0) remainingSeconds.toFloat() / totalDuration else 0f
                    CircularProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxSize(),
                        color = if (countdownUrgent) GoldPR else BluePrimary,
                        trackColor = Color.White.copy(alpha = 0.5f),
                        strokeWidth = 3.dp
                    )
                    Text(
                        text = remainingSeconds.toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (countdownUrgent) GoldPR else TextBlue
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = exerciseName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextDark
                )
            }
            
            val soundPlayer = LocalSoundPlayer.current
            Button(
                onClick = { soundPlayer.play(AppSound.BUTTON_TAP); onDone() },
                colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                shape = MaterialTheme.shapes.extraSmall,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Done", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

// --- UI Helpers ---

private fun formatDecimal(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format("%.1f", value)
    }
}

private fun formatTime(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        String.format("%d:%02d:%02d", h, m, s)
    } else {
        String.format("%d:%02d", m, s)
    }
}

// --- Exercise Picker Dialog ---
 
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
                            val muscleGroups = listOf("Chest", "Back", "Shoulders", "Biceps", "Triceps", "Legs", "Core", "Glutes", "Calves", "Cardio", "Full Body")
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

// --- Frequency Slider (Never <-> Always, 5 snap positions) ---

// The middle position is intentionally `null` and unlabeled: it represents "no change" /
// neutral, the same as an exercise with no preference row at all. It's a real, reachable slider
// position (not just a fallback default) so users can explicitly drag back to neutral instead
// of only being able to move away from it.
private val FREQUENCY_SLIDER_STEPS: List<com.example.workoutbuddy.data.Frequency?> = listOf(
    com.example.workoutbuddy.data.Frequency.NEVER,
    com.example.workoutbuddy.data.Frequency.LESS,
    null,
    com.example.workoutbuddy.data.Frequency.OFTEN,
    com.example.workoutbuddy.data.Frequency.ALWAYS
)

private val FREQUENCY_SLIDER_NEUTRAL_DEFAULT_INDEX = 2 // the null/no-change position

/**
 * A 5-position discrete slider between Never and Always, used both in the in-context exercise
 * card menu and the Manage Exercises screen. The middle position is the unlabeled "no change"
 * slot. Reports the snapped [Frequency] (or `null` for "no change") only once the user releases
 * the thumb on a different position than where it started.
 */
@Composable
fun FrequencySlider(
    currentFrequency: com.example.workoutbuddy.data.Frequency?,
    onFrequencyChange: (com.example.workoutbuddy.data.Frequency?) -> Unit,
    modifier: Modifier = Modifier
) {
    val initialIndex = currentFrequency?.let { FREQUENCY_SLIDER_STEPS.indexOf(it) }
        .let { if (it == null || it < 0) FREQUENCY_SLIDER_NEUTRAL_DEFAULT_INDEX else it }
    var sliderPosition by remember(currentFrequency) { mutableFloatStateOf(initialIndex.toFloat()) }
    val snappedIndex = sliderPosition.roundToInt().coerceIn(0, FREQUENCY_SLIDER_STEPS.lastIndex)

    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            onValueChangeFinished = {
                val finalIndex = sliderPosition.roundToInt().coerceIn(0, FREQUENCY_SLIDER_STEPS.lastIndex)
                sliderPosition = finalIndex.toFloat()
                if (finalIndex != initialIndex) {
                    onFrequencyChange(FREQUENCY_SLIDER_STEPS[finalIndex])
                }
            },
            valueRange = 0f..(FREQUENCY_SLIDER_STEPS.size - 1).toFloat(),
            steps = FREQUENCY_SLIDER_STEPS.size - 2, // 3 intermediate stops between the 5 positions
            colors = SliderDefaults.colors(
                thumbColor = BluePrimary,
                activeTrackColor = BluePrimary,
                inactiveTrackColor = BluePrimary.copy(alpha = 0.2f)
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("Never", "Less", "", "Often", "Always").forEachIndexed { index, label ->
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = if (index == snappedIndex) FontWeight.Bold else FontWeight.Normal,
                    color = if (index == snappedIndex) BluePrimary else TextMuted
                )
            }
        }
    }
}

// --- Difficulty Slider (Easy -> Hard, 3 snap positions) ---

private val DIFFICULTY_SLIDER_STEPS = listOf(
    com.example.workoutbuddy.data.Difficulty.EASY,
    com.example.workoutbuddy.data.Difficulty.MEDIUM,
    com.example.workoutbuddy.data.Difficulty.HARD
)

/**
 * A 3-position discrete slider between Easy and Hard, used for both the first-launch difficulty
 * ceiling picker and the later settings control. Unlike [FrequencySlider] there's no neutral/
 * untagged state to preserve, so [onSelectedChange] fires on every settled position (including
 * landing back on the value it started at) and callers hold their own "pending selection" state
 * until an explicit Continue/Save action persists it.
 */
@Composable
fun DifficultySlider(
    selected: com.example.workoutbuddy.data.Difficulty,
    onSelectedChange: (com.example.workoutbuddy.data.Difficulty) -> Unit,
    modifier: Modifier = Modifier
) {
    val initialIndex = DIFFICULTY_SLIDER_STEPS.indexOf(selected).coerceIn(0, DIFFICULTY_SLIDER_STEPS.lastIndex)
    var sliderPosition by remember(selected) { mutableFloatStateOf(initialIndex.toFloat()) }
    val snappedIndex = sliderPosition.roundToInt().coerceIn(0, DIFFICULTY_SLIDER_STEPS.lastIndex)

    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            onValueChangeFinished = {
                val finalIndex = sliderPosition.roundToInt().coerceIn(0, DIFFICULTY_SLIDER_STEPS.lastIndex)
                sliderPosition = finalIndex.toFloat()
                onSelectedChange(DIFFICULTY_SLIDER_STEPS[finalIndex])
            },
            valueRange = 0f..(DIFFICULTY_SLIDER_STEPS.size - 1).toFloat(),
            steps = DIFFICULTY_SLIDER_STEPS.size - 2, // 1 intermediate stop between the 3 positions
            colors = SliderDefaults.colors(
                thumbColor = BluePrimary,
                activeTrackColor = BluePrimary,
                inactiveTrackColor = BluePrimary.copy(alpha = 0.2f)
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("Easy", "Medium", "Hard").forEachIndexed { index, label ->
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = if (index == snappedIndex) FontWeight.Bold else FontWeight.Normal,
                    color = if (index == snappedIndex) BluePrimary else TextMuted
                )
            }
        }
    }
}
