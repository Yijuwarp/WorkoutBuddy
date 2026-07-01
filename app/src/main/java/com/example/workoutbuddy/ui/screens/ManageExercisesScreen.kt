package com.example.workoutbuddy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workoutbuddy.data.Equipment
import com.example.workoutbuddy.data.Frequency
import com.example.workoutbuddy.data.database.ExerciseEntity
import com.example.workoutbuddy.theme.BluePrimary
import com.example.workoutbuddy.theme.BorderLight
import com.example.workoutbuddy.theme.LightBackground
import com.example.workoutbuddy.theme.LightBlueContainer
import com.example.workoutbuddy.theme.TextDark
import com.example.workoutbuddy.theme.TextMuted
import com.example.workoutbuddy.ui.components.EquipmentFilterMode
import com.example.workoutbuddy.ui.components.ExerciseSortMode
import com.example.workoutbuddy.ui.components.FrequencySlider
import com.example.workoutbuddy.viewmodel.WorkoutViewModel
import com.example.workoutbuddy.viewmodel.isExerciseAvailable

private val MUSCLE_GROUPS = listOf(
    "Chest", "Back", "Shoulders", "Biceps", "Triceps", "Legs", "Core", "Glutes", "Calves", "Cardio", "Full Body"
)

/**
 * Lists the entire exercise library so users can tune frequency preferences for exercises they
 * haven't necessarily encountered yet in a generated workout. Mirrors the All / Muscle Group +
 * search browsing structure from the Add/Replace Exercise picker, but each row carries a
 * frequency slider instead of a tap-to-select action. Equipment-locked exercises are still shown
 * (greyed out) and still taggable, so a preference set now survives the user later adding that
 * equipment, rather than starting untagged.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageExercisesScreen(
    viewModel: WorkoutViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allExercises by viewModel.allExercises.collectAsState()
    val preferences by viewModel.exercisePreferences.collectAsState()
    val profile by viewModel.userProfile.collectAsState()
    val usageStats by viewModel.exerciseUsageStats.collectAsState()
    val ownedEquipment = remember(profile) {
        Equipment.parseCsv(profile?.equipmentOwned ?: Equipment.allIdsCsv)
    }

    var searchQuery by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf(0) } // 0 = All, 1 = Muscle Group
    var selectedMuscleGroup by remember { mutableStateOf<String?>(null) }
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

    androidx.activity.compose.BackHandler {
        if (activeTab == 1 && selectedMuscleGroup != null) selectedMuscleGroup = null
        else onBack()
    }

    Column(modifier = modifier.fillMaxSize().background(LightBackground)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (activeTab == 1 && selectedMuscleGroup != null) selectedMuscleGroup = null
                    else onBack()
                }
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextDark)
            }
            Text(
                text = if (activeTab == 1 && selectedMuscleGroup != null) selectedMuscleGroup!! else "Manage Exercises",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                color = TextDark
            )
        }

        if (!(activeTab == 1 && selectedMuscleGroup != null)) {
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = LightBackground,
                contentColor = BluePrimary
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
        }

        when {
            activeTab == 0 -> {
                SearchAndFilterRow(
                    searchQuery = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Search exercises...",
                    showSortFilterMenu = showSortFilterMenu,
                    onToggleMenu = { showSortFilterMenu = !showSortFilterMenu },
                    onDismissMenu = { showSortFilterMenu = false },
                    sortMode = sortMode,
                    onSortChange = { sortMode = it },
                    equipmentFilter = equipmentFilter,
                    onEquipmentFilterChange = { equipmentFilter = it }
                )
                val filtered = remember(searchQuery, allExercises, sortMode, equipmentFilter, usageStats) {
                    sortExercises(
                        allExercises.filter {
                            (it.name.contains(searchQuery, ignoreCase = true) ||
                                it.bodyPart.contains(searchQuery, ignoreCase = true)) &&
                                passesEquipmentFilter(it)
                        }
                    )
                }
                ExerciseFrequencyList(
                    exercises = filtered,
                    ownedEquipment = ownedEquipment,
                    preferences = preferences,
                    onSetFrequency = { id, freq -> viewModel.setExerciseFrequency(id, freq) }
                )
            }

            selectedMuscleGroup == null -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(MUSCLE_GROUPS) { muscle ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
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
                                Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = BluePrimary)
                            }
                        }
                    }
                }
            }

            else -> {
                SearchAndFilterRow(
                    searchQuery = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Search in $selectedMuscleGroup...",
                    showSortFilterMenu = showSortFilterMenu,
                    onToggleMenu = { showSortFilterMenu = !showSortFilterMenu },
                    onDismissMenu = { showSortFilterMenu = false },
                    sortMode = sortMode,
                    onSortChange = { sortMode = it },
                    equipmentFilter = equipmentFilter,
                    onEquipmentFilterChange = { equipmentFilter = it }
                )
                val filtered = remember(searchQuery, allExercises, selectedMuscleGroup, sortMode, equipmentFilter, usageStats) {
                    sortExercises(
                        allExercises.filter {
                            it.bodyPart.contains(selectedMuscleGroup!!, ignoreCase = true) &&
                                (searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true)) &&
                                passesEquipmentFilter(it)
                        }
                    )
                }
                ExerciseFrequencyList(
                    exercises = filtered,
                    ownedEquipment = ownedEquipment,
                    preferences = preferences,
                    onSetFrequency = { id, freq -> viewModel.setExerciseFrequency(id, freq) }
                )
            }
        }
    }
}

@Composable
private fun SearchAndFilterRow(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    showSortFilterMenu: Boolean,
    onToggleMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    sortMode: ExerciseSortMode,
    onSortChange: (ExerciseSortMode) -> Unit,
    equipmentFilter: EquipmentFilterMode,
    onEquipmentFilterChange: (EquipmentFilterMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(placeholder) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box {
            IconButton(
                onClick = onToggleMenu,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .border(1.dp, BorderLight, MaterialTheme.shapes.medium)
            ) {
                Icon(Icons.Default.FilterList, contentDescription = "Sort and filter", tint = BluePrimary)
            }
            DropdownMenu(
                expanded = showSortFilterMenu,
                onDismissRequest = onDismissMenu
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
                        onClick = { onSortChange(mode) }
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
                        onClick = { onEquipmentFilterChange(mode) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseFrequencyList(
    exercises: List<ExerciseEntity>,
    ownedEquipment: Set<Equipment>,
    preferences: Map<Int, Frequency>,
    onSetFrequency: (Int, Frequency?) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(exercises) { exercise ->
            ManageExerciseRow(
                exercise = exercise,
                isAvailable = isExerciseAvailable(exercise, ownedEquipment),
                currentFrequency = preferences[exercise.id],
                onSetFrequency = { freq -> onSetFrequency(exercise.id, freq) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        }
    }
}

@Composable
private fun ManageExerciseRow(
    exercise: ExerciseEntity,
    isAvailable: Boolean,
    currentFrequency: Frequency?,
    onSetFrequency: (Frequency?) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().alpha(if (isAvailable) 1f else 0.45f).padding(vertical = 12.dp)) {
        Text(
            text = exercise.name,
            fontWeight = FontWeight.Bold,
            color = TextDark,
            fontSize = 15.sp
        )
        Text(
            text = if (!isAvailable) "Requires equipment you don't own" else "${exercise.bodyPart} • ${exercise.type}",
            color = TextMuted,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        FrequencySlider(
            currentFrequency = currentFrequency,
            onFrequencyChange = onSetFrequency
        )
    }
}
