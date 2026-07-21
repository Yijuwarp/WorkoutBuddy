package com.example.workoutbuddy.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workoutbuddy.theme.*
import com.example.workoutbuddy.viewmodel.ExerciseTrend
import com.example.workoutbuddy.viewmodel.WorkoutViewModel

private enum class BodyTab { RESULTS, RECOVERY }

@Composable
fun BodyScreen(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier
) {
    var tab by remember { mutableStateOf(BodyTab.RESULTS) }

    LaunchedEffect(Unit) {
        viewModel.refreshExerciseResultSummaries()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LightBackground)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(12.dp))
        ) {
            listOf(BodyTab.RESULTS to "Results", BodyTab.RECOVERY to "Recovery").forEach { (t, label) ->
                val selected = tab == t
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) BluePrimary else Color.Transparent)
                        .clickable { tab = t }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (selected) Color.White else TextMuted,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        when (tab) {
            BodyTab.RESULTS -> ResultsTab(viewModel)
            BodyTab.RECOVERY -> RecoveryTab(viewModel)
        }
    }
}

@Composable
private fun ResultsTab(viewModel: WorkoutViewModel) {
    val profile by viewModel.userProfile.collectAsState()
    val summaries by viewModel.exerciseResultSummaries.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            profile?.let { p ->
                ProfileScoresCard(
                    nickname = p.nickname,
                    strengthScore = p.strengthScore,
                    staminaScore = p.staminaScore,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        if (summaries.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Log a few exercises and your PRs will show up here.",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(summaries) { summary ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(summary.name, fontWeight = FontWeight.Bold, color = TextDark, fontSize = 14.sp)
                            Text(summary.prLabel, color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                        val (icon, tint) = when (summary.trend) {
                            ExerciseTrend.UP -> Icons.Default.TrendingUp to GreenSuccess
                            ExerciseTrend.DOWN -> Icons.Default.TrendingDown to RedDanger
                            ExerciseTrend.FLAT -> Icons.Default.TrendingFlat to TextMuted
                        }
                        Icon(icon, contentDescription = summary.trend.name, tint = tint)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecoveryTab(viewModel: WorkoutViewModel) {
    val recoveryByGroup by viewModel.muscleGroupRecovery.collectAsState()
    val muscleGroups = com.example.workoutbuddy.viewmodel.RECOVERY_MUSCLE_GROUPS

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(muscleGroups) { group ->
            val recoveryPct = WorkoutViewModel.currentRecoveryPct(recoveryByGroup[group])
            val barColor = when {
                recoveryPct >= 80 -> GreenSuccess
                recoveryPct >= 40 -> AmberWarning
                else -> RedDanger
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            group.replace("_", " "),
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            fontSize = 14.sp
                        )
                        Text(
                            "${recoveryPct.toInt()}%",
                            fontWeight = FontWeight.Bold,
                            color = barColor,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = (recoveryPct / 100.0).toFloat().coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(5.dp))
                                .background(barColor)
                        )
                    }
                }
            }
        }
    }
}
