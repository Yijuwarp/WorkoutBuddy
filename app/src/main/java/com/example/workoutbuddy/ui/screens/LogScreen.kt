package com.example.workoutbuddy.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Delete
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
import com.example.workoutbuddy.data.database.WorkoutEntity
import com.example.workoutbuddy.theme.*
import com.example.workoutbuddy.ui.components.CalendarWidget
import com.example.workoutbuddy.ui.components.ExerciseThumbnail
import com.example.workoutbuddy.viewmodel.WorkoutViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LogScreen(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier
) {
    val completedWorkouts by viewModel.allCompletedWorkouts.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val workoutsForSelectedDate by viewModel.workoutsForSelectedDate.collectAsState()
    val selectedWorkoutDetail by viewModel.selectedWorkoutDetail.collectAsState()

    // Determine what to show in the list
    val listToShow = if (selectedDate != null) workoutsForSelectedDate else completedWorkouts

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Calendar
            item {
                CalendarWidget(
                    completedWorkouts = completedWorkouts,
                    selectedDate = selectedDate,
                    onDateSelected = { timestamp ->
                        // Toggle selection if clicked again
                        if (selectedDate == timestamp) {
                            viewModel.selectedDate.value = null
                        } else {
                            viewModel.selectedDate.value = timestamp
                        }
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedDate != null) "Filtered Workouts" else "All Workouts",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextDark
                    )
                    
                    if (selectedDate != null) {
                        TextButton(
                            onClick = { viewModel.selectedDate.value = null },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Clear filter", color = BluePrimary, fontSize = 13.sp)
                        }
                    }
                }
            }

            if (listToShow.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (selectedDate != null) "No workouts completed on this date." else "No workouts logged yet. Start a workout!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(listToShow) { workout ->
                    CompletedWorkoutItem(
                        workout = workout,
                        onClick = { viewModel.loadWorkoutDetail(workout.id) }
                    )
                }
            }
        }

        // Detailed Workout Dialog
        selectedWorkoutDetail?.let { detail ->
            WorkoutDetailDialog(
                detail = detail,
                onDismiss = { viewModel.closeWorkoutDetail() },
                onDelete = { viewModel.deleteCompletedWorkout(detail.workout.id) }
            )
        }
    }
}

@Composable
fun CompletedWorkoutItem(
    workout: WorkoutEntity,
    onClick: () -> Unit
) {
    val dateText = remember(workout.date) {
        SimpleDateFormat("EEE, MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(workout.date))
    }

    val categoryColor = when (workout.category) {
        "PUSH" -> BluePrimary
        "PULL" -> GreenSuccess
        "LOWER_BODY" -> GoldPR
        else -> BlueSecondary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                 Text(
                    text = workout.category.replace("_", " "),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = categoryColor
                )
                Text(
                    text = formatDuration(workout.durationInSeconds),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = TextMuted
                )
            }

            Text(
                text = dateText,
                style = MaterialTheme.typography.bodySmall,
                color = TextBlue, // Blue subtext as requested!
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Quick statistics
            WorkoutQuickStats(
                calories = workout.totalCalories.toInt(),
                steps = workout.totalSteps,
                strengthGain = workout.strengthGain,
                staminaGain = workout.staminaGain,
                prCount = workout.prCount
            )
        }
    }
}

@Composable
fun QuickStatItem(
    label: String,
    subtext: String,
    labelColor: Color = TextDark
) {
    Column {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = labelColor
        )
        Text(
            text = subtext,
            fontSize = 10.sp,
            color = TextMuted
        )
    }
}

// Calories/Steps/Records always anchor row 1 (Records in slot 3); Strength/Stamina spill onto
// a second row since a 5-item Row with Arrangement.Start has no wrapping and squeezes whichever
// item runs out of width — that's what was crushing the "Records" column down to one letter per line.
@Composable
fun WorkoutQuickStats(
    calories: Int,
    steps: Int,
    strengthGain: Double,
    staminaGain: Double,
    prCount: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            QuickStatItem(label = "$calories kcal", subtext = "Burned")
            if (steps > 0) {
                Spacer(modifier = Modifier.width(24.dp))
                QuickStatItem(label = "$steps", subtext = "Steps")
            }
            if (prCount > 0) {
                Spacer(modifier = Modifier.width(24.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = GoldPR,
                            modifier = Modifier.size(14.dp).padding(end = 2.dp)
                        )
                        Text(
                            text = "$prCount",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldPR
                        )
                    }
                    Text(
                        text = "Records",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }
        }

        if (strengthGain > 0.0 || staminaGain > 0.0) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (strengthGain > 0.0) {
                    QuickStatItem(
                        label = "+${String.format("%.1f", strengthGain)}",
                        subtext = "Strength",
                        labelColor = Color(0xFFEF4444)
                    )
                }
                if (staminaGain > 0.0) {
                    if (strengthGain > 0.0) Spacer(modifier = Modifier.width(24.dp))
                    QuickStatItem(
                        label = "+${String.format("%.1f", staminaGain)}",
                        subtext = "Stamina",
                        labelColor = Color(0xFFF59E0B)
                    )
                }
            }
        }
    }
}

// --- Workout Detail Dialog ---

@Composable
fun WorkoutDetailDialog(
    detail: com.example.workoutbuddy.viewmodel.WorkoutDetailState,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val dateText = remember(detail.workout.date) {
        SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault()).format(Date(detail.workout.date))
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${detail.workout.category.replace("_", " ")} Workout",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = TextDark
                        )
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextBlue
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp), color = BorderLight)

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Quick statistics at the very top
                    item {
                        WorkoutQuickStats(
                            calories = detail.workout.totalCalories.toInt(),
                            steps = detail.workout.totalSteps,
                            strengthGain = detail.workout.strengthGain,
                            staminaGain = detail.workout.staminaGain,
                            prCount = detail.workout.prCount,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LightBackground, RoundedCornerShape(12.dp))
                                .border(1.dp, BorderLight, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        )
                    }

                    // Muscle Impacts section
                    if (detail.muscleImpacts.isNotEmpty()) {
                        item {
                            Text(
                                text = "Estimated Muscle Group Impact",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(LightBackground, RoundedCornerShape(12.dp))
                                    .border(1.dp, BorderLight, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                detail.muscleImpacts.forEach { (muscle, score) ->
                                    val scoreLabel = when {
                                        score >= 3.0 -> "Heavy Impact"
                                        score >= 1.5 -> "Medium Impact"
                                        else -> "Light Impact"
                                    }
                                    val progressVal = (score / 4.0).toFloat().coerceAtMost(1f)
                                    
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = muscle,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = TextDark
                                            )
                                            Text(
                                                text = scoreLabel,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextBlue
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = progressVal,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(CircleShape),
                                            color = BluePrimary,
                                            trackColor = BorderLight
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Exercises completed
                    item {
                        Text(
                            text = "Exercises Logged",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = TextMuted,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(detail.exerciseDetails) { exerciseDetail ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = LightBackground),
                            border = BorderStroke(0.5.dp, BorderLight)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                ExerciseThumbnail(
                                    exerciseName = exerciseDetail.exercise.name,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = exerciseDetail.exercise.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TextDark
                                    )
                                    Text(
                                        text = "${exerciseDetail.exercise.bodyPart} • ${exerciseDetail.exercise.type}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                    
                                    // Completed Sets
                                    exerciseDetail.sets.forEach { set ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Set ${set.setNumber}",
                                                fontSize = 12.sp,
                                                color = TextMuted
                                            )
                                            
                                            val statsText = when (exerciseDetail.exercise.type) {
                                                "LIFT" -> "${formatDecimal(set.weight ?: 0.0)}kg x ${set.reps ?: 0}"
                                                "CARDIO" -> "${formatTime(set.time ?: 0)} (${formatDecimal(set.distance ?: 0.0)}km)"
                                                "HOLD" -> formatTime(set.time ?: 0)
                                                else -> ""
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (set.isPR) {
                                                    Icon(
                                                        imageVector = Icons.Default.Star,
                                                        contentDescription = "PR",
                                                        tint = GoldPR,
                                                        modifier = Modifier.size(14.dp).padding(end = 2.dp)
                                                    )
                                                    Text(
                                                        text = "PR! ",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = GoldPR
                                                    )
                                                }
                                                Text(
                                                    text = statsText,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextDark
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Delete Workout Button
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Workout", color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun formatDecimal(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format("%.1f", value)
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
