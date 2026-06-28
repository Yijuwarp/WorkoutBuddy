package com.example.workoutbuddy.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.workoutbuddy.data.Equipment
import com.example.workoutbuddy.theme.*
import com.example.workoutbuddy.ui.components.EquipmentPickerList
import com.example.workoutbuddy.viewmodel.WorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profileState by viewModel.userProfile.collectAsState()

    // Form inputs state
    var nickname by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var ageText by remember { mutableStateOf("") }
    var heightText by remember { mutableStateOf("") }
    var weightText by remember { mutableStateOf("") }
    var showEquipmentScreen by remember { mutableStateOf(false) }
    var showDifficultyDialog by remember { mutableStateOf(false) }
    var showManageExercises by remember { mutableStateOf(false) }

    // Sync input states once profile is loaded
    LaunchedEffect(profileState) {
        profileState?.let { p ->
            nickname = p.nickname
            gender = p.gender
            ageText = p.age.toString()
            heightText = p.height.toInt().toString()
            weightText = String.format("%.1f", p.weight)
        }
    }

    if (showManageExercises) {
        ManageExercisesScreen(
            viewModel = viewModel,
            onBack = { showManageExercises = false },
            modifier = modifier
        )
        return
    }

    if (showEquipmentScreen) {
        EquipmentScreen(
            viewModel = viewModel,
            onBack = { showEquipmentScreen = false },
            modifier = modifier
        )
        return
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LightBackground)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Title Bar
        Text(
            text = "My Profile",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
            color = TextDark,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        profileState?.let { p ->
            // --- Premium Scores Card ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Rank badge + name on the left
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val rankTier = WorkoutViewModel.deriveRankTier(p.strengthScore, p.staminaScore)
                        Image(
                            painter = painterResource(id = WorkoutViewModel.rankBadgeRes(rankTier)),
                            contentDescription = "$rankTier rank badge",
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = p.nickname,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                color = TextDark
                            )
                            Text(
                                text = "Level: $rankTier",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // 2 Scores on the right
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Strength Score Group
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(RedDangerBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${p.strengthScore.toInt()}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = RedDangerLight,
                                    lineHeight = 24.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "STRENGTH",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Stamina Score Group
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(AmberWarningBgLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${p.staminaScore.toInt()}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = AmberWarning,
                                    lineHeight = 24.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "STAMINA",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }

            // --- Profile Form Details ---
            Text(
                text = "Personal Details",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextDark,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Nickname Input
                    Text("Nickname", fontWeight = FontWeight.Bold, color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { nickname = it },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BluePrimary,
                            unfocusedBorderColor = BorderLight
                        )
                    )

                    // Gender Row Selectors
                    Text("Gender", fontWeight = FontWeight.Bold, color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Male", "Female", "Other").forEach { option ->
                            val isSelected = gender == option
                            val border = BorderStroke(1.5.dp, if (isSelected) BluePrimary else BorderLight)
                            val bg = if (isSelected) LightBlueContainer else Color.Transparent

                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .clickable { gender = option },
                                shape = MaterialTheme.shapes.medium,
                                border = border,
                                colors = CardDefaults.cardColors(containerColor = bg)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = option,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) BluePrimary else TextMuted,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    // Numeric stats: Age, Height, Weight
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Age field
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Age", fontWeight = FontWeight.Bold, color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
                            OutlinedTextField(
                                value = ageText,
                                onValueChange = { ageText = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BluePrimary,
                                    unfocusedBorderColor = BorderLight
                                )
                            )
                        }

                        // Height field
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Height (cm)", fontWeight = FontWeight.Bold, color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
                            OutlinedTextField(
                                value = heightText,
                                onValueChange = { heightText = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BluePrimary,
                                    unfocusedBorderColor = BorderLight
                                )
                            )
                        }

                        // Weight field
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Weight (kg)", fontWeight = FontWeight.Bold, color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
                            OutlinedTextField(
                                value = weightText,
                                onValueChange = { weightText = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BluePrimary,
                                    unfocusedBorderColor = BorderLight
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Save Details Button
                    Button(
                        onClick = {
                            val ageVal = ageText.toIntOrNull()
                            val heightVal = heightText.toDoubleOrNull()
                            val weightVal = weightText.replace(",", ".").toDoubleOrNull()

                            if (nickname.isBlank()) {
                                Toast.makeText(context, "Nickname cannot be empty", Toast.LENGTH_SHORT).show()
                            } else if (ageVal == null || ageVal <= 0 || ageVal > 120) {
                                Toast.makeText(context, "Please enter a valid age", Toast.LENGTH_SHORT).show()
                            } else if (heightVal == null || heightVal <= 0.0 || heightVal > 300.0) {
                                Toast.makeText(context, "Please enter a valid height", Toast.LENGTH_SHORT).show()
                            } else if (weightVal == null || weightVal <= 0.0 || weightVal > 500.0) {
                                Toast.makeText(context, "Please enter a valid weight", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.updateUserProfile(nickname, ageVal, heightVal, weightVal, gender)
                                Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // --- Settings ---
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextDark,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Rest Timers", fontWeight = FontWeight.Bold, color = TextDark, fontSize = 14.sp)
                        Text(
                            "Automatically start a rest timer after each completed set",
                            color = TextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Switch(
                        checked = p.restTimerEnabled,
                        onCheckedChange = { viewModel.setRestTimerEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = BluePrimary,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = TextMuted.copy(alpha = 0.5f),
                            uncheckedBorderColor = SlateBorder
                        )
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showEquipmentScreen = true }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Equipment", fontWeight = FontWeight.Bold, color = TextDark, fontSize = 14.sp)
                        Text(
                            "Choose what you have access to so workouts only use it",
                            color = TextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = TextMuted)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDifficultyDialog = true }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Difficulty", fontWeight = FontWeight.Bold, color = TextDark, fontSize = 14.sp)
                        Text(
                            "Currently: ${p.difficultyCeiling?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Easy"}",
                            color = TextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = TextMuted)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showManageExercises = true }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Manage Exercises", fontWeight = FontWeight.Bold, color = TextDark, fontSize = 14.sp)
                        Text(
                            "Tell us which exercises you want to see more or less of",
                            color = TextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = TextMuted)
                }
            }
        } ?: Box(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = BluePrimary)
        }
        
        Spacer(modifier = Modifier.height(80.dp)) // Extra padding at bottom
    }

    if (showDifficultyDialog) {
        val currentCeiling = remember(profileState) {
            com.example.workoutbuddy.data.Difficulty.fromName(profileState?.difficultyCeiling)
                ?: com.example.workoutbuddy.data.Difficulty.EASY
        }
        var selected by remember(profileState) { mutableStateOf(currentCeiling) }
        val entrance = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            entrance.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        }

        Dialog(onDismissRequest = { showDifficultyDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(0.92f + 0.08f * entrance.value)
                    .alpha(entrance.value),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Difficulty",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = TextDark
                        )
                        IconButton(onClick = { showDifficultyDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                        }
                    }
                    Text(
                        "Picking a higher tier also unlocks exercises at every tier below it.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                    )

                    com.example.workoutbuddy.ui.components.DifficultySlider(
                        selected = selected,
                        onSelectedChange = { selected = it },
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Button(
                        onClick = {
                            viewModel.setDifficultyCeiling(selected)
                            showDifficultyDialog = false
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
