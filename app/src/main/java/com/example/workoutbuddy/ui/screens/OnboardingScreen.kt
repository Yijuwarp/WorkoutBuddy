package com.example.workoutbuddy.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.Layout
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.workoutbuddy.audio.AppSound
import com.example.workoutbuddy.theme.*
import com.example.workoutbuddy.ui.util.LocalSoundPlayer
import com.example.workoutbuddy.ui.util.pressScale
import com.example.workoutbuddy.viewmodel.WorkoutViewModel
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import kotlin.math.roundToInt
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

@OptIn(ExperimentalAnimationApi::class, ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableStateOf(1) }

    // User Form State
    var nickname by remember { mutableStateOf("") }
    // Empty until the user picks — gender is required and Next is gated on it.
    var gender by remember { mutableStateOf("") }
    var age by remember { mutableStateOf(35) }
    var height by remember { mutableStateOf(178.0) }
    var weight by remember { mutableStateOf(78.0) }
    var heightWeightTouched by remember { mutableStateOf(false) }
    var gymExperience by remember { mutableStateOf("Beginner") }
    var equipmentOwned by remember { mutableStateOf(com.example.workoutbuddy.data.Equipment.entries.toSet()) }

    // Keep height/weight defaults matched to gender until the user manually adjusts either
    // slider, so switching gender on the previous step doesn't silently keep stale defaults.
    LaunchedEffect(gender) {
        if (!heightWeightTouched) {
            when (gender) {
                "Male" -> { height = 178.0; weight = 78.0 }
                "Female" -> { height = 170.0; weight = 70.0 }
            }
        }
    }

    val gradientBg = Brush.verticalGradient(
        colors = listOf(
            LightBackground,
            SlateDarker,
            LightBackground
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradientBg)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        val isKeyboardOpen = androidx.compose.foundation.layout.WindowInsets.isImeVisible
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Header Branding
            if (!isKeyboardOpen) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(BluePrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Workout Buddy",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }

            // Glassmorphic Card Container
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.15f), ShapeHero),
                shape = ShapeHero,
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Step Indicator (1 to 5)
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                    ) {
                        for (i in 1..7) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (step >= i) BlueSecondary else Color.White.copy(alpha = 0.2f))
                            )
                            if (i < 7) {
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                        }
                    }

                    // Content based on Step
                    AnimatedContent(
                        targetState = step,
                        modifier = Modifier.fillMaxWidth(),
                        transitionSpec = {
                            if (targetState > initialState) {
                                slideInHorizontally { width -> width } + fadeIn() togetherWith
                                        slideOutHorizontally { width -> -width } + fadeOut()
                            } else {
                                slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                        slideOutHorizontally { width -> width } + fadeOut()
                            }.using(SizeTransform(clip = false))
                        },
                        label = "OnboardingSteps"
                    ) { currentStep ->
                        when (currentStep) {
                            1 -> OnboardingStep1(
                                nickname = nickname,
                                onNicknameChange = { nickname = it }
                            )
                            2 -> OnboardingGenderStep(
                                gender = gender,
                                onGenderSelect = { gender = it }
                            )
                            3 -> OnboardingStep2(
                                age = age,
                                onAgeChange = { age = it }
                            )
                            4 -> OnboardingStep3(
                                height = height,
                                onHeightChange = { height = it; heightWeightTouched = true },
                                weight = weight,
                                onWeightChange = { weight = it; heightWeightTouched = true }
                            )
                            5 -> OnboardingGymExperienceStep(
                                experience = gymExperience,
                                onExperienceChange = { gymExperience = it }
                            )
                            6 -> OnboardingEquipmentStep(
                                selected = equipmentOwned,
                                onToggle = { equipment, owned ->
                                    equipmentOwned = if (owned) equipmentOwned + equipment else equipmentOwned - equipment
                                },
                                onSelectAll = { equipmentOwned = com.example.workoutbuddy.data.Equipment.entries.toSet() },
                                onDeselectAll = { equipmentOwned = emptySet() },
                                onSkip = { step++ }
                            )
                            7 -> OnboardingStep5(
                                nickname = nickname,
                                gender = gender,
                                age = age,
                                height = height,
                                weight = weight,
                                gymExperience = gymExperience,
                                onComplete = {
                                    viewModel.saveUserProfile(nickname.ifBlank { "Buddy" }, age, height, weight, gender, gymExperience, equipmentOwned)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Action buttons
                    val nextButtonContent: @Composable RowScope.() -> Unit = {
                        Text("Next", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    val nextEnabled = when (step) {
                        1 -> nickname.isNotBlank()
                        2 -> gender.isNotEmpty()
                        else -> true
                    }
                    val onNextClick: () -> Unit = {
                        if (nextEnabled) {
                            step++
                        }
                    }
                    val nextColors = ButtonDefaults.buttonColors(
                        containerColor = BluePrimary,
                        disabledContainerColor = Color.White.copy(alpha = 0.1f)
                    )

                    if (step == 1) {
                        // Only the Next button exists on this step - render it directly with
                        // fillMaxWidth() instead of inside a weighted Row, since that's a single
                        // unambiguous width source with no sibling/arrangement interaction.
                        if (step < 7) {
                            Button(
                                onClick = onNextClick,
                                enabled = nextEnabled,
                                colors = nextColors,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                content = nextButtonContent
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { step-- },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("Back", fontWeight = FontWeight.Bold)
                            }

                            if (step < 7) {
                                Button(
                                    onClick = onNextClick,
                                    enabled = nextEnabled,
                                    colors = nextColors,
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 12.dp)
                                        .height(48.dp),
                                    content = nextButtonContent
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingEquipmentStep(
    selected: Set<com.example.workoutbuddy.data.Equipment>,
    onToggle: (com.example.workoutbuddy.data.Equipment, Boolean) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onSkip: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "What equipment do you have?",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, color = Color.White),
            textAlign = TextAlign.Center
        )
        Text(
            text = "We'll only recommend exercises you can actually do. Everything is on by default.",
            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.7f)),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onSelectAll,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
            ) {
                Text("Select All", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            OutlinedButton(
                onClick = onDeselectAll,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
            ) {
                Text("Deselect All", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
        ) {
            com.example.workoutbuddy.ui.components.EquipmentPickerColumn(
                selected = selected,
                onToggle = onToggle,
                modifier = Modifier.padding(horizontal = 12.dp).verticalScroll(rememberScrollState()),
                textColor = Color.White
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onSkip) {
            Text("Skip for now", color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingGymExperienceStep(
    experience: String,
    onExperienceChange: (String) -> Unit
) {
    val levels = listOf("Beginner", "Intermediate", "Expert")
    val snapValues = listOf(0.02f, 0.50f, 0.98f)
    val currentIndex = levels.indexOf(experience).coerceAtLeast(0)
    val sliderValue = snapValues[currentIndex]

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "What's your gym experience?",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, color = Color.White),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Custom Slider with manual track drawing for 2%, 50%, 98% notches
        Slider(
            value = sliderValue,
            onValueChange = { newValue ->
                val nearestIndex = snapValues.minByOrNull { kotlin.math.abs(it - newValue) }
                    ?.let { snapValues.indexOf(it) } ?: 0
                onExperienceChange(levels[nearestIndex])
            },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = BlueSecondary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            track = { sliderState ->
                val activeColor = BluePrimary
                val inactiveColor = Color.White.copy(alpha = 0.1f)
                val notchColor = BlueSecondary.copy(alpha = 0.8f)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                ) {
                    val trackHeight = 6.dp.toPx()
                    val centerY = size.height / 2f
                    val trackWidth = size.width

                    // 1. Draw Inactive Track
                    drawRoundRect(
                        color = inactiveColor,
                        topLeft = Offset(0f, centerY - trackHeight / 2f),
                        size = Size(trackWidth, trackHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackHeight / 2f)
                    )

                    // 2. Draw Active Track up to current value
                    val activeWidth = trackWidth * sliderValue
                    drawRoundRect(
                        color = activeColor,
                        topLeft = Offset(0f, centerY - trackHeight / 2f),
                        size = Size(activeWidth, trackHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackHeight / 2f)
                    )

                    // 3. Draw notches at 2%, 50%, 98%
                    snapValues.forEach { fraction ->
                        val notchX = trackWidth * fraction
                        drawCircle(
                            color = if (sliderValue >= fraction) Color.White else notchColor,
                            radius = 3.dp.toPx(),
                            center = Offset(notchX, centerY)
                        )
                    }
                }
            }
        )

        // Custom Layout to align text labels centered on 2%, 50%, 98% notches
        Layout(
            content = {
                levels.forEachIndexed { idx, level ->
                    Text(
                        text = level,
                        color = if (idx == currentIndex) BlueSecondary else Color.White.copy(alpha = 0.4f),
                        fontWeight = if (idx == currentIndex) FontWeight.Black else FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
            val trackWidth = constraints.maxWidth

            layout(trackWidth, placeables.maxOfOrNull { it.height } ?: 0) {
                placeables.forEachIndexed { idx, placeable ->
                    val fraction = snapValues[idx]
                    val notchX = trackWidth * fraction
                    // Center the text horizontally on the notch
                    val x = (notchX - placeable.width / 2f)
                        .coerceIn(0f, trackWidth.toFloat() - placeable.width)
                    placeable.placeRelative(x.toInt(), 0)
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Card displaying the selected level description
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.1f), MaterialTheme.shapes.large),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = when (experience) {
                        "Beginner" -> Icons.Default.FitnessCenter
                        "Intermediate" -> Icons.Default.TrendingUp
                        else -> Icons.Default.EmojiEvents
                    },
                    contentDescription = null,
                    tint = BlueSecondary,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when (experience) {
                        "Beginner" -> "Just starting out. Learning correct form and building a solid workout routine."
                        "Intermediate" -> "Familiar with lifts. Consistently training and looking to scale up intensity."
                        else -> "Highly experienced. Maximizing strength score and optimizing recovery targets."
                    },
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingStep1(
    nickname: String,
    onNicknameChange: (String) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Welcome!",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, color = Color.White),
            textAlign = TextAlign.Center
        )
        Text(
            text = "Let's personalize your experience. What should we call you?",
            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.7f)),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        OutlinedTextField(
            value = nickname,
            onValueChange = onNicknameChange,
            placeholder = { Text("Enter your nickname...", color = Color.White.copy(alpha = 0.4f)) },
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BluePrimary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                cursorColor = BluePrimary
            ),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingGenderStep(
    gender: String,
    onGenderSelect: (String) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "What's your gender?",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, color = Color.White),
            textAlign = TextAlign.Center
        )
        Text(
            text = "This helps us tune baseline strength and stamina estimates.",
            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.7f)),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        val isKeyboardOpen = androidx.compose.foundation.layout.WindowInsets.isImeVisible
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf("Male", "Female", "Other").forEach { option ->
                val isSelected = gender == option
                val border = BorderStroke(
                    1.5.dp,
                    if (isSelected) BluePrimary else Color.White.copy(alpha = 0.1f)
                )
                val bg = if (isSelected) BluePrimary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.02f)

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onGenderSelect(option) },
                    shape = MaterialTheme.shapes.large,
                    border = border,
                    colors = CardDefaults.cardColors(containerColor = bg)
                ) {
                    if (isKeyboardOpen) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = option.uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = when (option) {
                                    "Male" -> Icons.Default.Male
                                    "Female" -> Icons.Default.Female
                                    else -> Icons.Default.Transgender
                                },
                                contentDescription = null,
                                tint = if (isSelected) BlueSecondary else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = option.uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingStep2(
    age: Int,
    onAgeChange: (Int) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "How old are you?",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, color = Color.White)
        )
        Text(
            text = "Age affects baseline cardiorespiratory and muscular recovery capabilities.",
            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.7f)),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        val soundPlayer = LocalSoundPlayer.current
        val decrementInteractionSource = remember { MutableInteractionSource() }
        val incrementInteractionSource = remember { MutableInteractionSource() }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = {
                    if (age > 10) {
                        soundPlayer.play(AppSound.BUTTON_TAP)
                        onAgeChange(age - 1)
                    }
                },
                interactionSource = decrementInteractionSource,
                modifier = Modifier
                    .size(48.dp)
                    .pressScale(decrementInteractionSource)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease Age", tint = Color.White)
            }

            val ageScale = remember { Animatable(1f) }
            LaunchedEffect(age) {
                ageScale.snapTo(0.85f)
                ageScale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
            }
            Text(
                text = "$age",
                fontSize = 64.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .scale(ageScale.value)
            )

            IconButton(
                onClick = {
                    if (age < 100) {
                        soundPlayer.play(AppSound.BUTTON_TAP)
                        onAgeChange(age + 1)
                    }
                },
                interactionSource = incrementInteractionSource,
                modifier = Modifier
                    .size(48.dp)
                    .pressScale(incrementInteractionSource)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase Age", tint = Color.White)
            }
        }

        Text(
            text = "years old",
            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingStep3(
    height: Double,
    onHeightChange: (Double) -> Unit,
    weight: Double,
    onWeightChange: (Double) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Physical Metrics",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, color = Color.White)
        )
        Text(
            text = "Height and weight help us approximate muscle mass and absolute lift ratios.",
            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.7f)),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Height selector
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Height", color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                Text("${height.toInt()} cm", color = Color.White, fontWeight = FontWeight.Black)
            }
            val heightInteractionSource = remember { MutableInteractionSource() }
            val heightDragging by heightInteractionSource.collectIsDraggedAsState()
            val heightThumbScale by animateFloatAsState(if (heightDragging) 1.3f else 1f, label = "heightThumbScale")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                MetricStepButton("−") { onHeightChange((height - 1.0).coerceAtLeast(100.0)) }
                Slider(
                    value = height.toFloat(),
                    onValueChange = { onHeightChange(it.toDouble()) },
                    valueRange = 100f..230f,
                    interactionSource = heightInteractionSource,
                    thumb = {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .scale(heightThumbScale)
                                .clip(CircleShape)
                                .background(BlueSecondary)
                        )
                    },
                    colors = SliderDefaults.colors(
                        activeTrackColor = BluePrimary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.1f),
                        thumbColor = BlueSecondary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                )
                MetricStepButton("+") { onHeightChange((height + 1.0).coerceAtMost(230.0)) }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Weight selector
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Weight", color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                Text("${String.format("%.1f", weight)} kg", color = Color.White, fontWeight = FontWeight.Black)
            }
            val weightInteractionSource = remember { MutableInteractionSource() }
            val weightDragging by weightInteractionSource.collectIsDraggedAsState()
            val weightThumbScale by animateFloatAsState(if (weightDragging) 1.3f else 1f, label = "weightThumbScale")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                MetricStepButton("−") { onWeightChange((weight - 0.1).coerceAtLeast(30.0)) }
                Slider(
                    value = weight.toFloat(),
                    onValueChange = { onWeightChange(it.toDouble()) },
                    valueRange = 30f..180f,
                    interactionSource = weightInteractionSource,
                    thumb = {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .scale(weightThumbScale)
                                .clip(CircleShape)
                                .background(BlueSecondary)
                        )
                    },
                    colors = SliderDefaults.colors(
                        activeTrackColor = BluePrimary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.1f),
                        thumbColor = BlueSecondary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                )
                MetricStepButton("+") { onWeightChange((weight + 0.1).coerceAtMost(180.0)) }
            }
        }
    }
}

@Composable
private fun MetricStepButton(label: String, onClick: () -> Unit) {
    val scope = rememberCoroutineScope()
    val pressed = remember { mutableStateOf(false) }
    val currentOnClick by rememberUpdatedState(onClick)
    val offsetY by animateDpAsState(if (pressed.value) 1.dp else 0.dp, label = "btn3dOffset")
    val shadowAlpha by animateFloatAsState(if (pressed.value) 0.1f else 0.35f, label = "btn3dShadow")
    Box(contentAlignment = Alignment.Center) {
        // shadow layer
        Box(
            modifier = Modifier
                .size(28.dp)
                .offset(y = if (pressed.value) 1.dp else 3.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = shadowAlpha))
        )
        // face layer
        Box(
            modifier = Modifier
                .size(28.dp)
                .offset(y = offsetY)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.32f),
                            Color.White.copy(alpha = 0.08f)
                        )
                    )
                )
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        pressed.value = true
                        currentOnClick()
                        val job = scope.launch {
                            delay(350L)
                            while (isActive) {
                                currentOnClick()
                                delay(80L)
                            }
                        }
                        try {
                            do {
                                val event = awaitPointerEvent()
                            } while (event.changes.any { it.pressed })
                        } finally {
                            job.cancel()
                            pressed.value = false
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(label, color = Color.White, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
fun OnboardingStep5(
    nickname: String,
    gender: String,
    age: Int,
    height: Double,
    weight: Double,
    gymExperience: String,
    onComplete: () -> Unit
) {
    // Live estimate strength & stamina scores — delegates to the same formulas used when
    // the profile is actually saved (WorkoutViewModel.calculateInitial*Score), so this
    // preview can never drift out of sync with the real saved values.
    val strengthScore = remember(age, height, weight, gender, gymExperience) {
        WorkoutViewModel.calculateInitialStrengthScore(age, height, weight, gender, gymExperience)
    }
    val staminaScore = remember(age, height, weight, gender, gymExperience) {
        WorkoutViewModel.calculateInitialStaminaScore(age, height, weight, gender, gymExperience)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Your Fitness Profile",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, color = Color.White)
        )
        Text(
            text = "Based on your body parameters, we have initialized your personal strength and cardiovascular stamina scores:",
            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.7f)),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Strength Score Badge — red to match the Strength score treatment on ProfileScreen
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .border(2.dp, RedDangerLight.copy(alpha = 0.5f), CircleShape)
                    .background(RedDangerLight.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${strengthScore.toInt()}",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        lineHeight = 36.sp
                    )
                    Text(
                        text = "STRENGTH",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = RedDangerLight,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Stamina Score Badge (Amber/Yellow: 0xFFF59E0B)
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .border(2.dp, AmberWarning.copy(alpha = 0.5f), CircleShape)
                    .background(AmberWarning.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${staminaScore.toInt()}",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        lineHeight = 36.sp
                    )
                    Text(
                        text = "STAMINA",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberWarning,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Brief explainer
        Text(
            text = "As you complete workouts, these scores adapt. Log lifts to build strength, and cardio to level up your stamina score!",
            style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.6f), lineHeight = 16.sp),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onComplete,
            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Let's Begin!", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
        }
    }
}
