package com.example.workoutbuddy.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workoutbuddy.data.Equipment
import com.example.workoutbuddy.data.database.EquipmentPresetEntity
import com.example.workoutbuddy.theme.BluePrimary
import com.example.workoutbuddy.theme.LightBackground
import com.example.workoutbuddy.theme.TextDark
import com.example.workoutbuddy.theme.TextMuted
import com.example.workoutbuddy.ui.components.EquipmentPickerList
import com.example.workoutbuddy.viewmodel.WorkoutViewModel

/**
 * Fullscreen equipment management: bulk Select All / Deselect All actions for users who own
 * little to no equipment, named presets ("Home", "Gym") that snapshot the current selection for
 * one-tap re-application, and the per-item toggle list below.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipmentScreen(
    viewModel: WorkoutViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.userProfile.collectAsState()
    val presets by viewModel.equipmentPresets.collectAsState()
    val selected = remember(profile) {
        Equipment.parseCsv(profile?.equipmentOwned ?: Equipment.allIdsCsv)
    }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var presetPendingDelete by remember { mutableStateOf<EquipmentPresetEntity?>(null) }

    androidx.activity.compose.BackHandler(onBack = onBack)

    Column(modifier = modifier.fillMaxSize().background(LightBackground)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextDark)
            }
            Text(
                text = "Your Equipment",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                color = TextDark
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.setEquipmentOwnedSet(Equipment.entries.toSet()) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Select All", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            OutlinedButton(
                onClick = { viewModel.setEquipmentOwnedSet(emptySet()) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Deselect All", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Text(
            text = "Presets",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = TextMuted,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(presets, key = { it.id }) { preset ->
                PresetChip(
                    preset = preset,
                    onApply = { viewModel.applyEquipmentPreset(preset) },
                    onLongPress = { presetPendingDelete = preset }
                )
            }
            item {
                AssistChip(
                    onClick = { showSavePresetDialog = true },
                    label = { Text("Save current") },
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = BluePrimary.copy(alpha = 0.1f),
                        labelColor = BluePrimary,
                        leadingIconContentColor = BluePrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

        EquipmentPickerList(
            selected = selected,
            onToggle = { equipment, isOwned -> viewModel.setEquipmentOwned(equipment, isOwned) },
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)
        )
    }

    if (showSavePresetDialog) {
        SavePresetDialog(
            onDismiss = { showSavePresetDialog = false },
            onSave = { name ->
                viewModel.saveEquipmentPreset(name)
                showSavePresetDialog = false
            }
        )
    }

    presetPendingDelete?.let { preset ->
        AlertDialog(
            onDismissRequest = { presetPendingDelete = null },
            title = { Text("Delete preset?", fontWeight = FontWeight.Bold) },
            text = { Text("\"${preset.name}\" will be removed. This doesn't change your currently selected equipment.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteEquipmentPreset(preset.id)
                        presetPendingDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { presetPendingDelete = null }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PresetChip(
    preset: EquipmentPresetEntity,
    onApply: () -> Unit,
    onLongPress: () -> Unit
) {
    // Tap applies the preset; long-press is the only way to delete it, so an accidental tap
    // can't wipe out a saved setup the way a small always-visible "x" button could.
    Surface(
        modifier = Modifier.combinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = LocalIndication.current,
            onClick = onApply,
            onLongClick = onLongPress
        ),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Text(
            preset.name,
            color = TextDark,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun SavePresetDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save preset", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("e.g. Home, Gym") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }, enabled = name.isNotBlank()) {
                Text("Save", color = BluePrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}
