package com.example.workoutbuddy

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.workoutbuddy.ui.screens.LogScreen
import com.example.workoutbuddy.ui.screens.WorkoutScreen
import com.example.workoutbuddy.ui.screens.OnboardingScreen
import com.example.workoutbuddy.ui.screens.ProfileScreen
import com.example.workoutbuddy.viewmodel.WorkoutViewModel

enum class WorkoutTab {
    WORKOUT, LOG, PROFILE
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

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == WorkoutTab.WORKOUT,
                        onClick = { currentTab = WorkoutTab.WORKOUT },
                        label = { Text("Workout") },
                        icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Active Workout") }
                    )
                    NavigationBarItem(
                        selected = currentTab == WorkoutTab.LOG,
                        onClick = { currentTab = WorkoutTab.LOG },
                        label = { Text("Log") },
                        icon = { Icon(Icons.Default.DateRange, contentDescription = "Workout Log") }
                    )
                    NavigationBarItem(
                        selected = currentTab == WorkoutTab.PROFILE,
                        onClick = { currentTab = WorkoutTab.PROFILE },
                        label = { Text("Profile") },
                        icon = { Icon(Icons.Default.Person, contentDescription = "My Profile") }
                    )
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
                    WorkoutTab.LOG -> LogScreen(viewModel = viewModel)
                    WorkoutTab.PROFILE -> ProfileScreen(viewModel = viewModel)
                }
            }
        }
    }
}
