package com.example.workoutbuddy.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.workoutbuddy.audio.AppSound
import com.example.workoutbuddy.theme.*
import com.example.workoutbuddy.ui.util.LocalSoundPlayer

@Composable
fun TimeCalculatorTextField(
    initialSeconds: Int?,
    onSecondsChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "0:00"
) {
    var rawDigits by remember {
        val initialText = if (initialSeconds != null && initialSeconds > 0) {
            val h = initialSeconds / 3600
            val m = (initialSeconds % 3600) / 60
            val s = initialSeconds % 60
            val totalStr = String.format("%d%02d%02d", h, m, s)
            totalStr.trimStart('0').take(5)
        } else {
            ""
        }
        mutableStateOf(initialText)
    }

    var isFocused by remember { mutableStateOf(false) }
    var lastFocusState by remember { mutableStateOf(false) }

    var textFieldValue by remember {
        val initialText = formatSecondsToDisplayString(initialSeconds ?: 0)
        mutableStateOf(TextFieldValue(text = initialText))
    }

    // Sync state when focus changes
    val focusModifier = modifier.onFocusChanged { focusState ->
        if (focusState.isFocused != lastFocusState) {
            lastFocusState = focusState.isFocused
            isFocused = focusState.isFocused
            if (!focusState.isFocused) {
                val finalSeconds = convertDigitsToSeconds(rawDigits)
                onSecondsChanged(finalSeconds)
                val dispStr = formatSecondsToDisplayString(finalSeconds)
                textFieldValue = TextFieldValue(text = dispStr)
            }
        }
    }

    LaunchedEffect(initialSeconds) {
        if (!isFocused) {
            val finalSeconds = initialSeconds ?: 0
            val dispStr = formatSecondsToDisplayString(finalSeconds)
            textFieldValue = TextFieldValue(text = dispStr)
            val h = finalSeconds / 3600
            val m = (finalSeconds % 3600) / 60
            val s = finalSeconds % 60
            val totalStr = String.format("%d%02d%02d", h, m, s)
            rawDigits = totalStr.trimStart('0').take(5)
        }
    }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            kotlinx.coroutines.delay(50)
            val editStr = formatSecondsToEditingString(convertDigitsToSeconds(rawDigits))
            textFieldValue = TextFieldValue(
                text = editStr,
                selection = TextRange(0, editStr.length)
            )
        }
    }

    OutlinedTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            if (!isFocused) {
                textFieldValue = newValue
                return@OutlinedTextField
            }

            val oldText = textFieldValue.text
            val newText = newValue.text

            if (newText == oldText) {
                // Only selection/cursor position changed (user clicked)
                textFieldValue = newValue
                return@OutlinedTextField
            }

            val oldDigits = oldText.filter { it.isDigit() }
            val newDigits = newText.filter { it.isDigit() }

            val isSelectAll = textFieldValue.selection.start == 0 && textFieldValue.selection.end == oldText.length && oldText.isNotEmpty()
            if (isSelectAll) {
                val wasDeletion = newText.length < oldText.length && newDigits.isEmpty()
                if (wasDeletion) {
                    rawDigits = ""
                } else {
                    rawDigits = newDigits.take(5)
                }
                val formatted = formatRawDigitsToEditingString(rawDigits)
                textFieldValue = TextFieldValue(
                    text = formatted,
                    selection = TextRange(formatted.length)
                )
                onSecondsChanged(convertDigitsToSeconds(rawDigits))
                return@OutlinedTextField
            }

            if (newDigits.length < oldDigits.length) {
                // Backspace / Deletion
                if (rawDigits.isNotEmpty()) {
                    rawDigits = rawDigits.dropLast(1)
                }
                val formatted = formatRawDigitsToEditingString(rawDigits)
                textFieldValue = TextFieldValue(
                    text = formatted,
                    selection = TextRange(formatted.length)
                )
                onSecondsChanged(convertDigitsToSeconds(rawDigits))
            } else {
                // Insertion / Typing
                val parsedDigits = newDigits.trimStart('0')
                if (parsedDigits.length <= 5) {
                    rawDigits = parsedDigits
                    val formatted = formatRawDigitsToEditingString(rawDigits)
                    textFieldValue = TextFieldValue(
                        text = formatted,
                        selection = TextRange(formatted.length)
                    )
                    onSecondsChanged(convertDigitsToSeconds(rawDigits))
                }
            }
        },
        modifier = focusModifier,
        textStyle = TextStyle(fontSize = 14.sp, color = TextDark, textAlign = TextAlign.Center),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BluePrimary,
            unfocusedBorderColor = BorderLight
        ),
        placeholder = { Text(placeholder, fontSize = 12.sp) }
    )
}

private fun formatSecondsToDisplayString(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        String.format("%d:%02d:%02d", h, m, s)
    } else {
        String.format("%d:%02d", m, s)
    }
}

private fun formatSecondsToEditingString(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return String.format("%02d:%02d:%02d", h, m, s)
}

private fun formatRawDigitsToEditingString(digits: String): String {
    val padded = digits.padStart(5, '0')
    val h = padded.substring(0, 1).toInt()
    val m = padded.substring(1, 3).toInt()
    val s = padded.substring(3, 5).toInt()
    return String.format("%02d:%02d:%02d", h, m, s)
}

private fun convertDigitsToSeconds(digits: String): Int {
    if (digits.isEmpty()) return 0
    val padded = digits.padStart(5, '0')
    val h = padded.substring(0, 1).toInt()
    val m = padded.substring(1, 3).toInt()
    val s = padded.substring(3, 5).toInt()
    return h * 3600 + m * 60 + s
}

@Composable
fun CountdownTimerDialog(
    exerciseName: String,
    remainingSeconds: Int,
    totalDuration: Int,
    isPaused: Boolean,
    onTapTimer: () -> Unit,
    onDone: () -> Unit,
    onCancel: () -> Unit,
    onMinimize: () -> Unit
) {
    val soundPlayer = LocalSoundPlayer.current
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
    }
    Dialog(onDismissRequest = onCancel) {
        Card(
            modifier = Modifier
                .width(320.dp)
                .scale(0.85f + 0.15f * entrance.value)
                .alpha(entrance.value),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = exerciseName,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = TextDark,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Circular Timer Representation - Center aligned & clickable to pause/resume
                val urgent = !isPaused && remainingSeconds in 1..3
                val urgencyScale by animateFloatAsState(
                    targetValue = if (urgent) 1.08f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
                    label = "urgencyPulse"
                )
                val urgencyTextColor by animateColorAsState(
                    targetValue = if (urgent) GoldPR else TextDark,
                    label = "urgencyTextColor"
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(160.dp)
                        .scale(urgencyScale)
                        .clip(CircleShape)
                        .clickable { onTapTimer() }
                ) {
                    val progress = if (totalDuration > 0) remainingSeconds.toFloat() / totalDuration else 0f
                    CircularProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxSize(),
                        color = if (isPaused) GoldPR else BluePrimary,
                        trackColor = BorderLight,
                        strokeWidth = 8.dp
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = formatTime(remainingSeconds),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = urgencyTextColor
                        )
                        if (isPaused) {
                            Text(
                                text = "PAUSED",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = GoldPR,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(28.dp))
                
                // Buttons at bottom: Cancel, Minimize, and Done side by side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { soundPlayer.play(AppSound.BUTTON_TAP); onCancel() },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text("Cancel", color = RedDanger, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = { soundPlayer.play(AppSound.BUTTON_TAP); onMinimize() },
                        modifier = Modifier.weight(1.2f),
                        shape = MaterialTheme.shapes.medium,
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text("Minimize", fontSize = 13.sp)
                    }

                    Button(
                        onClick = { soundPlayer.play(AppSound.BUTTON_TAP); onDone() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                        shape = MaterialTheme.shapes.medium,
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text("Done", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}


@Composable
fun RestTimerModal(
    exerciseName: String,
    remainingSeconds: Int,
    totalDuration: Int,
    onSkip: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val soundPlayer = LocalSoundPlayer.current
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
    }
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f * entrance.value))
                .clickable { onDismissRequest() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .alpha(entrance.value)
                    .scale(0.9f + 0.1f * entrance.value)
                    .clickable(enabled = false) { /* stop propagation */ },
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Rest Timer",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextBlue
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val restUrgent = remainingSeconds in 1..3
                    val urgencyScale by animateFloatAsState(
                        targetValue = if (restUrgent) 1.08f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
                        label = "restTimerUrgencyPulse"
                    )
                    val restUrgencyTextColor by animateColorAsState(
                        targetValue = if (restUrgent) GoldPR else TextDark,
                        label = "restTimerUrgencyTextColor"
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(100.dp).scale(urgencyScale)
                    ) {
                        val progress = if (totalDuration > 0) remainingSeconds.toFloat() / totalDuration else 0f
                        CircularProgressIndicator(
                            progress = progress,
                            modifier = Modifier.fillMaxSize(),
                            color = if (restUrgent) GoldPR else BluePrimary,
                            trackColor = BorderLight,
                            strokeWidth = 6.dp
                        )
                        Text(
                            text = formatTime(remainingSeconds),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = restUrgencyTextColor
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { soundPlayer.play(AppSound.BUTTON_TAP); onDismissRequest() },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Minimize")
                        }
                        Button(
                            onClick = { soundPlayer.play(AppSound.BUTTON_TAP); onSkip() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Skip", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// --- Cooldown Banner ---

@Composable
fun CooldownBanner(
    exerciseName: String,
    remainingSeconds: Int,
    totalDuration: Int,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = LightBlueContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Mini timer
                val cooldownUrgent = remainingSeconds in 1..3
                val cooldownPulse by animateFloatAsState(
                    targetValue = if (cooldownUrgent) 1.12f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
                    label = "cooldownBannerPulse"
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(36.dp).scale(cooldownPulse)
                ) {
                    val progress = if (totalDuration > 0) remainingSeconds.toFloat() / totalDuration else 0f
                    CircularProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxSize(),
                        color = if (cooldownUrgent) GoldPR else BluePrimary,
                        trackColor = Color.White.copy(alpha = 0.5f),
                        strokeWidth = 3.dp
                    )
                    Text(
                        text = remainingSeconds.toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (cooldownUrgent) GoldPR else TextBlue
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Rest",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextDark
                    )
                }
            }
            
            val soundPlayer = LocalSoundPlayer.current
            TextButton(
                onClick = { soundPlayer.play(AppSound.BUTTON_TAP); onSkip() },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("Skip", color = BluePrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

// --- Countdown Banner ---

@Composable
fun CountdownBanner(
    exerciseName: String,
    remainingSeconds: Int,
    totalDuration: Int,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = LightBlueContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Mini timer
                val countdownUrgent = remainingSeconds in 1..3
                val countdownPulse by animateFloatAsState(
                    targetValue = if (countdownUrgent) 1.12f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
                    label = "countdownBannerPulse"
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(36.dp).scale(countdownPulse)
                ) {
                    val progress = if (totalDuration > 0) remainingSeconds.toFloat() / totalDuration else 0f
                    CircularProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxSize(),
                        color = if (countdownUrgent) GoldPR else BluePrimary,
                        trackColor = Color.White.copy(alpha = 0.5f),
                        strokeWidth = 3.dp
                    )
                    Text(
                        text = remainingSeconds.toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (countdownUrgent) GoldPR else TextBlue
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = exerciseName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextDark
                )
            }
            
            val soundPlayer = LocalSoundPlayer.current
            Button(
                onClick = { soundPlayer.play(AppSound.BUTTON_TAP); onDone() },
                colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                shape = MaterialTheme.shapes.extraSmall,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Done", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}


fun formatTime(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        String.format("%d:%02d:%02d", h, m, s)
    } else {
        String.format("%d:%02d", m, s)
    }
}

