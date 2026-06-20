package com.example.workoutbuddy.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.workoutbuddy.data.database.ExerciseEntity
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

            // Calendar Days Grid
            val rows = days.chunked(7)
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

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isSelected -> BluePrimary
                                                isToday -> BluePrimary.copy(alpha = 0.1f)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .clickable { onDateSelected(dateTimestamp) },
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
                                    
                                    if (hasWorkout) {
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

private fun isSameDay(time1: Long, time2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

// --- Exercise List Item ---

@Composable
fun ExerciseListItem(
    exerciseState: ActiveExerciseState,
    isWorkoutStarted: Boolean,
    onReplaceExercise: () -> Unit,
    onRemoveExercise: () -> Unit,
    onClick: () -> Unit
) {
    val totalSets = exerciseState.sets.size
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exerciseState.exercise.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextDark
                )
                Text(
                    text = "${exerciseState.exercise.bodyPart} • ($totalSets Sets. $repsText)",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = TextBlue // Blue subtext!
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isWorkoutStarted) {
                    var showMenu by remember { mutableStateOf(false) }

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
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(4.dp))
                }

                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = GreenSuccess,
                        modifier = Modifier.size(24.dp)
                    )
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
    onSkipCooldown: () -> Unit,
    onDismissRequest: () -> Unit,
    onSetValuesChanged: (Long, Double?, Int?, Int?, Double?) -> Unit,
    onSetCompleteToggled: (Long, Boolean) -> Unit,
    onStartCountdown: (Long, String, Int) -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: (Long) -> Unit,
    onReplaceExercise: () -> Unit,
    onRemoveExercise: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = BorderLight) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exerciseState.exercise.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = TextDark
                    )
                    Text(
                        text = "${exerciseState.exercise.bodyPart} • ${exerciseState.exercise.type}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = TextBlue
                    )
                }

                if (isWorkoutStarted) {
                    var showMenu by remember { mutableStateOf(false) }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Exercise Options",
                                tint = BluePrimary
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

            Spacer(modifier = Modifier.height(12.dp))

            // TOP Pane: LAST and BEST Records
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
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
                Text("Set", modifier = Modifier.width(60.dp), style = MaterialTheme.typography.bodySmall, color = TextMuted, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (exerciseState.exercise.type) {
                        "LIFT" -> {
                            Text("Reps", modifier = Modifier.width(80.dp), style = MaterialTheme.typography.bodySmall, color = TextMuted, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.width(20.dp)) // aligns with the " x " text and its spacers
                            Text("Weight (kg)", modifier = Modifier.width(88.dp), style = MaterialTheme.typography.bodySmall, color = TextMuted, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                        "CARDIO" -> {
                            Text("Time", modifier = Modifier.width(100.dp), style = MaterialTheme.typography.bodySmall, color = TextMuted, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Distance (km)", modifier = Modifier.width(72.dp), style = MaterialTheme.typography.bodySmall, color = TextMuted, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                        "HOLD" -> {
                            Text("Time", modifier = Modifier.width(100.dp), style = MaterialTheme.typography.bodySmall, color = TextMuted, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                    }
                }
                Text("Done", modifier = Modifier.width(48.dp), style = MaterialTheme.typography.bodySmall, color = TextMuted, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
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
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { dismissValue ->
                            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                onRemoveSet(set.id)
                                true
                            } else {
                                false
                            }
                        }
                    )

                    SwipeToDismissBox(
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
                            onValuesChanged = { w, r, t, d ->
                                onSetValuesChanged(set.id, w, r, t, d)
                            },
                            onCompleteToggled = { complete ->
                                onSetCompleteToggled(set.id, complete)
                            }
                        )
                    }
                }

                if (isWorkoutStarted) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            TextButton(
                                onClick = onAddSet,
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
                if (cooldownExerciseName != null) {
                    CooldownBanner(
                        exerciseName = cooldownExerciseName,
                        remainingSeconds = cooldownRemaining,
                        totalDuration = cooldownDuration,
                        onSkip = onSkipCooldown,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                val allCompleted = exerciseState.sets.isNotEmpty() && exerciseState.sets.all { it.isCompleted }
                if (allCompleted) {
                    Button(
                        onClick = onDismissRequest,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        shape = RoundedCornerShape(12.dp)
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
                                exerciseState.sets.forEach { set ->
                                    if (!set.isCompleted) {
                                        onSetCompleteToggled(set.id, true)
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF374151)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Log All Sets", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        // Log Set button
                        Button(
                            onClick = {
                                val nextSet = exerciseState.sets.firstOrNull { !it.isCompleted }
                                if (nextSet != null) {
                                    onSetCompleteToggled(nextSet.id, true)
                                }
                            },
                            modifier = Modifier.weight(1.2f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            val nextSet = exerciseState.sets.firstOrNull { !it.isCompleted }
                            val text = if (nextSet != null) "Log Set ${nextSet.setNumber}" else "Log Set"
                            Text(text, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            } else {
                Text(
                    text = "Start the workout to log your sets",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFF43F5E),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
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
    onValuesChanged: (Double?, Int?, Int?, Double?) -> Unit,
    onCompleteToggled: (Boolean) -> Unit
) {
    var weightInput by remember(set.id) { mutableStateOf(set.weight?.toString() ?: set.recommendedWeight?.toString() ?: "") }
    var repsInput by remember(set.id) { mutableStateOf(set.reps?.toString() ?: set.recommendedReps?.toString() ?: "") }
    var distanceInput by remember(set.id) { mutableStateOf(set.distance?.toString() ?: set.recommendedDistance?.toString() ?: "") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Set number
        Text(
            text = "Set ${set.setNumber}",
            modifier = Modifier.width(60.dp),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = TextDark
        )

        // Inputs Column
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            if (isWorkoutStarted) {
                when (type) {
                    "LIFT" -> {
                        // Reps input first
                        OutlinedTextField(
                            value = repsInput,
                            onValueChange = {
                                repsInput = it
                                val r = it.toIntOrNull()
                                val w = weightInput.toDoubleOrNull()
                                onValuesChanged(w, r, null, null)
                            },
                            modifier = Modifier.width(80.dp).height(50.dp),
                            textStyle = TextStyle(fontSize = 14.sp, color = TextDark),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BluePrimary,
                                unfocusedBorderColor = BorderLight
                            ),
                            placeholder = { Text("reps", fontSize = 12.sp) }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("x", color = TextMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        // Weight input second
                        OutlinedTextField(
                            value = weightInput,
                            onValueChange = {
                                weightInput = it
                                val w = it.toDoubleOrNull()
                                val r = repsInput.toIntOrNull()
                                onValuesChanged(w, r, null, null)
                            },
                            modifier = Modifier.width(88.dp).height(50.dp),
                            textStyle = TextStyle(fontSize = 14.sp, color = TextDark),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BluePrimary,
                                unfocusedBorderColor = BorderLight
                            ),
                            placeholder = { Text("kg", fontSize = 12.sp) }
                        )
                    }
                    "CARDIO" -> {
                        // Time input (HH:MM:SS format using Calculator input)
                        TimeCalculatorTextField(
                            initialSeconds = set.time ?: set.recommendedTime,
                            onSecondsChanged = { t ->
                                val d = distanceInput.toDoubleOrNull()
                                onValuesChanged(null, null, t, d)
                            },
                            modifier = Modifier.width(100.dp).height(50.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Distance input (km)
                        OutlinedTextField(
                            value = distanceInput,
                            onValueChange = {
                                distanceInput = it
                                val t = set.time ?: set.recommendedTime
                                val d = it.toDoubleOrNull()
                                onValuesChanged(null, null, t, d)
                            },
                            modifier = Modifier.width(72.dp).height(50.dp),
                            textStyle = TextStyle(fontSize = 14.sp, color = TextDark),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BluePrimary,
                                unfocusedBorderColor = BorderLight
                            ),
                            placeholder = { Text("km", fontSize = 12.sp) }
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        IconButton(onClick = {
                            val seconds = set.time ?: set.recommendedTime ?: 60
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
                                onValuesChanged(null, null, t, null)
                            },
                            modifier = Modifier.width(100.dp).height(50.dp)
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        IconButton(onClick = {
                            val seconds = set.time ?: set.recommendedTime ?: 60
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
                    onCompleteToggled(!set.isCompleted)
                },
            contentAlignment = Alignment.Center
        ) {
            if (set.isCompleted) {
                if (set.isPR) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "PR",
                        tint = GoldPR,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = GreenSuccess,
                        modifier = Modifier.size(24.dp)
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
    var rawDigits by remember(initialSeconds) {
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

    var textFieldValue by remember(initialSeconds) {
        val initialText = formatSecondsToDisplayString(initialSeconds ?: 0)
        mutableStateOf(TextFieldValue(text = initialText))
    }

    // Sync state when focus changes
    val focusModifier = modifier.onFocusChanged { focusState ->
        if (focusState.isFocused != lastFocusState) {
            lastFocusState = focusState.isFocused
            isFocused = focusState.isFocused
            if (focusState.isFocused) {
                val editStr = formatSecondsToEditingString(convertDigitsToSeconds(rawDigits))
                textFieldValue = TextFieldValue(
                    text = editStr,
                    selection = TextRange(0, editStr.length)
                )
            } else {
                val finalSeconds = convertDigitsToSeconds(rawDigits)
                onSecondsChanged(finalSeconds)
                val dispStr = formatSecondsToDisplayString(finalSeconds)
                textFieldValue = TextFieldValue(text = dispStr)
            }
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
            } else {
                // Insertion / Typing
                if (textFieldValue.selection.start == 0 && textFieldValue.selection.end == oldText.length) {
                    // Select-all replacement
                    val typedChar = newDigits.firstOrNull()
                    if (typedChar != null) {
                        rawDigits = typedChar.toString()
                        val formatted = formatRawDigitsToEditingString(rawDigits)
                        textFieldValue = TextFieldValue(
                            text = formatted,
                            selection = TextRange(formatted.length)
                        )
                    }
                } else {
                    val parsedDigits = newDigits.trimStart('0')
                    if (parsedDigits.length <= 5) {
                        rawDigits = parsedDigits
                        val formatted = formatRawDigitsToEditingString(rawDigits)
                        textFieldValue = TextFieldValue(
                            text = formatted,
                            selection = TextRange(formatted.length)
                        )
                    }
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
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
        Card(
            modifier = Modifier.width(300.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
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
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(160.dp)
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
                            color = TextDark
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
                
                // Buttons at bottom: Cancel and Done side by side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = RedDanger, fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = onDone,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
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
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(36.dp)
                ) {
                    val progress = if (totalDuration > 0) remainingSeconds.toFloat() / totalDuration else 0f
                    CircularProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxSize(),
                        color = BluePrimary,
                        trackColor = Color.White.copy(alpha = 0.5f),
                        strokeWidth = 3.dp
                    )
                    Text(
                        text = remainingSeconds.toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextBlue
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = "Resting from $exerciseName",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextDark
                    )
                    Text(
                        text = "Nice set! Take a breather.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
            
            TextButton(
                onClick = onSkip,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("Skip", color = BluePrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
@Composable
fun ExercisePickerDialog(
    title: String,
    exercises: List<ExerciseEntity>,
    onDismiss: () -> Unit,
    onExerciseSelected: (ExerciseEntity) -> Unit,
    onCreateExercise: ((String, String, String, String) -> Unit)? = null
) {
    var isCreating by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newType by remember { mutableStateOf("LIFT") }
    var newBodyPart by remember { mutableStateOf("") }
    var newImpact by remember { mutableStateOf("MEDIUM") }

    var searchQuery by remember { mutableStateOf("") }
    val filteredExercises = remember(searchQuery, exercises) {
        exercises.filter { it.name.contains(searchQuery, ignoreCase = true) || it.bodyPart.contains(searchQuery, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (isCreating) {
                    Text(
                        text = "Create Custom Exercise",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextDark,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

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

                        OutlinedTextField(
                            value = newBodyPart,
                            onValueChange = { newBodyPart = it },
                            label = { Text("Body Part") },
                            placeholder = { Text("e.g. Chest") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Text(
                            text = "Impact Level / Cooldown Rest",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextDark
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("LOW", "MEDIUM", "HIGH").forEach { level ->
                                val selected = newImpact == level
                                val durationText = when (level) {
                                    "LOW" -> "30s"
                                    "MEDIUM" -> "60s"
                                    "HIGH" -> "120s"
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { isCreating = false }) {
                            Text("Back", color = TextMuted)
                        }

                        Button(
                            onClick = {
                                if (newName.isNotBlank() && newBodyPart.isNotBlank() && onCreateExercise != null) {
                                    onCreateExercise(newName, newType, newBodyPart, newImpact)
                                }
                            },
                            enabled = newName.isNotBlank() && newBodyPart.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Create & Add", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextDark,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        placeholder = { Text("Search exercises...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredExercises) { exercise ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onExerciseSelected(exercise) }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = exercise.name,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark
                                    )
                                    Text(
                                        text = "${exercise.bodyPart} • ${exercise.type}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextBlue
                                    )
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

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onCreateExercise != null) {
                            TextButton(onClick = { isCreating = true }) {
                                Text("Create", color = BluePrimary, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = TextMuted)
                        }
                    }
                }
            }
        }
    }
}

