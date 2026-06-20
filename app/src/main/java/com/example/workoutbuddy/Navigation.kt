package com.example.workoutbuddy

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.workoutbuddy.ui.screens.LogScreen
import com.example.workoutbuddy.ui.screens.WorkoutScreen
import com.example.workoutbuddy.viewmodel.WorkoutViewModel

enum class WorkoutTab {
    WORKOUT, LOG
}

@Composable
fun MainNavigation(viewModel: WorkoutViewModel) {
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
            }
        }
    }
}
