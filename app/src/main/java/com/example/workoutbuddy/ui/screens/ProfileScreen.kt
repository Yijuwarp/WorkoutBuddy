package com.example.workoutbuddy.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
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
    var showWorkoutLengthDialog by remember { mutableStateOf(false) }
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
        profileState?.let { p ->
            // --- Premium Scores Card ---
            ProfileScoresCard(
                nickname = p.nickname,
                strengthScore = p.strengthScore,
                staminaScore = p.staminaScore,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // --- Profile Form Details (collapsible, starts collapsed) ---
            var personalDetailsExpanded by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { personalDetailsExpanded = !personalDetailsExpanded }
                    .padding(top = 4.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Personal Details",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextDark
                )
                Icon(
                    imageVector = if (personalDetailsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (personalDetailsExpanded) "Collapse personal details" else "Expand personal details",
                    tint = TextMuted
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = personalDetailsExpanded,
                enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
            ) {
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
                            "Auto-start a rest timer after each set",
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
                            "Only show workouts using gear you own",
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
                        .clickable { showWorkoutLengthDialog = true }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Workout Length", fontWeight = FontWeight.Bold, color = TextDark, fontSize = 14.sp)
                        Text(
                            "Currently: ${WORKOUT_LENGTH_OPTIONS.firstOrNull { it.first == p.workoutLengthMinutes }?.second ?: "45 min"}",
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
                            "Show more or less of specific exercises",
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

    if (showWorkoutLengthDialog) {
        Dialog(onDismissRequest = { showWorkoutLengthDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Workout Length",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = TextDark
                    )
                    Text(
                        "Auto-generated workouts are sized to fit. Short workouts skip the cardio finisher.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )
                    WORKOUT_LENGTH_OPTIONS.forEach { (minutes, label) ->
                        val isSelected = profileState?.workoutLengthMinutes == minutes
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .clickable {
                                    viewModel.setWorkoutLength(minutes)
                                    showWorkoutLengthDialog = false
                                },
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.5.dp, if (isSelected) BluePrimary else BorderLight),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) LightBlueContainer else Color.Transparent
                            )
                        ) {
                            Text(
                                text = label,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) BluePrimary else TextDark,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            }
        }
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

/**
 * Rank badge, nickname, and strength/stamina score circles. Shared between [ProfileScreen]
 * and the Body tab's Results view so both render the same "who am I" header.
 */
@Composable
fun ProfileScoresCard(
    nickname: String,
    strengthScore: Double,
    staminaScore: Double,
    modifier: Modifier = Modifier
) {
    val rankTier = remember(strengthScore, staminaScore) {
        WorkoutViewModel.deriveRankTier(strengthScore, staminaScore)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                colors = listOf(
                    StrengthRoseBorder.copy(alpha = 0.4f),
                    BorderLight,
                    StaminaCyanBorder.copy(alpha = 0.4f)
                )
            )
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF181C28),
                            Color(0xFF141822)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Rank badge + name on the left
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        BluePrimary.copy(alpha = 0.25f),
                                        Color(0xFF1E2533)
                                    )
                                )
                            )
                            .border(
                                BorderStroke(
                                    1.5.dp,
                                    Brush.sweepGradient(
                                        listOf(
                                            StrengthRoseBorder,
                                            BluePrimary,
                                            StaminaCyanBorder,
                                            StrengthRoseBorder
                                        )
                                    )
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = WorkoutViewModel.rankBadgeRes(rankTier)),
                            contentDescription = "$rankTier rank badge",
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = nickname,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            ),
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = BluePrimary.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, BluePrimary.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "LEVEL: ${rankTier.uppercase()}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = TextBlue,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 2 Scores on the right
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Strength Score Group
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF4C1D24),
                                            StrengthRoseBg
                                        )
                                    )
                                )
                                .border(
                                    BorderStroke(1.5.dp, StrengthRoseBorder.copy(alpha = 0.85f)),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    tint = StrengthRoseLight,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "${strengthScore.toInt()}",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(StrengthRoseLight)
                            )
                            Text(
                                text = "STRENGTH",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = StrengthRoseLight,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }

                    // Stamina Score Group
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF0F3843),
                                            StaminaCyanBg
                                        )
                                    )
                                )
                                .border(
                                    BorderStroke(1.5.dp, StaminaCyanBorder.copy(alpha = 0.85f)),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlashOn,
                                    contentDescription = null,
                                    tint = StaminaCyanLight,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "${staminaScore.toInt()}",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(StaminaCyanLight)
                            )
                            Text(
                                text = "STAMINA",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = StaminaCyanLight,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
