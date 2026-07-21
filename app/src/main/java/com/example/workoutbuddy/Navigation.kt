package com.example.workoutbuddy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.workoutbuddy.theme.BluePrimary
import com.example.workoutbuddy.theme.TextMuted
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.workoutbuddy.ui.screens.BodyScreen
import com.example.workoutbuddy.ui.screens.LogScreen
import com.example.workoutbuddy.ui.screens.WorkoutScreen
import com.example.workoutbuddy.ui.screens.OnboardingScreen
import com.example.workoutbuddy.ui.screens.ProfileScreen
import com.example.workoutbuddy.viewmodel.WorkoutViewModel

enum class WorkoutTab {
    WORKOUT, BODY, LOG, PROFILE
}

@Composable
private fun PillNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (selected) BluePrimary else TextMuted,
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(if (selected) BluePrimary else androidx.compose.ui.graphics.Color.Transparent)
        )
    }
}

@Composable
fun MainNavigation(viewModel: WorkoutViewModel) {
    val profileState by viewModel.userProfile.collectAsState()
    val isProfileLoaded by viewModel.isProfileLoaded.collectAsState()

    if (!isProfileLoaded) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = "Loading...",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(44.dp)
                )
            }
        }
    } else if (profileState == null) {
        OnboardingScreen(viewModel = viewModel)
    } else {
        var currentTab by remember { mutableStateOf(WorkoutTab.WORKOUT) }
        val isWorkoutStarted by viewModel.isWorkoutStarted.collectAsState()
        val isTimerPaused by viewModel.isTimerPaused.collectAsState()
        // Focused mode: hide the nav bar while the workout timer is actively running.
        val showBottomBar = !(isWorkoutStarted && !isTimerPaused)

        // Safety net: never leave the user stranded on another tab with the nav hidden.
        LaunchedEffect(showBottomBar) {
            if (!showBottomBar && currentTab != WorkoutTab.WORKOUT) {
                currentTab = WorkoutTab.WORKOUT
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = expandVertically(expandFrom = Alignment.Top),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top)
                ) {
                Surface(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(36.dp)),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PillNavItem(
                            selected = currentTab == WorkoutTab.WORKOUT,
                            onClick = { currentTab = WorkoutTab.WORKOUT },
                            icon = Icons.Default.FitnessCenter,
                            contentDescription = "Active Workout"
                        )
                        PillNavItem(
                            selected = currentTab == WorkoutTab.BODY,
                            onClick = { currentTab = WorkoutTab.BODY },
                            icon = Icons.Default.MonitorHeart,
                            contentDescription = "Body"
                        )
                        PillNavItem(
                            selected = currentTab == WorkoutTab.LOG,
                            onClick = { currentTab = WorkoutTab.LOG },
                            icon = Icons.Default.DateRange,
                            contentDescription = "Workout Log"
                        )
                        PillNavItem(
                            selected = currentTab == WorkoutTab.PROFILE,
                            onClick = { currentTab = WorkoutTab.PROFILE },
                            icon = Icons.Default.Person,
                            contentDescription = "My Profile"
                        )
                    }
                }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    WorkoutTab.WORKOUT -> WorkoutScreen(viewModel = viewModel)
                    WorkoutTab.BODY -> BodyScreen(viewModel = viewModel)
                    WorkoutTab.LOG -> LogScreen(viewModel = viewModel)
                    WorkoutTab.PROFILE -> ProfileScreen(viewModel = viewModel)
                }
            }
        }
    }
}
