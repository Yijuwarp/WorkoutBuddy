package com.example.workoutbuddy.ui.components

import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.workoutbuddy.audio.AppSound
import com.example.workoutbuddy.data.database.WorkoutEntity
import com.example.workoutbuddy.theme.*
import com.example.workoutbuddy.ui.util.LocalSoundPlayer

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
                                                    "CARDIO" -> PerformanceRed
                                                    "FULL_BODY" -> PurpleAccent
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

private fun formatDecimal(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format("%.1f", value)
    }
}

