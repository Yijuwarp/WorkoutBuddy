package com.example.workoutbuddy.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workoutbuddy.viewmodel.FloatingNumber
import com.example.workoutbuddy.viewmodel.WorkoutViewModel
import kotlinx.coroutines.delay
import kotlin.math.sin

@Composable
fun WavyFloatingNumbersContainer(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val floatingNumbers by viewModel.floatingNumbers.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        content()

        // Render each active floating number overlay
        floatingNumbers.forEach { item ->
            key(item.id) {
                // Determine a random-ish starting X position near the middle
                val seed = (item.id % 100).toInt()
                val startingXShift = (seed - 50) * 2f // -100 to 100 dp shift
                
                WavyFloatingItem(
                    text = item.text,
                    colorType = item.colorType,
                    startXShift = startingXShift,
                    onAnimationComplete = {
                        viewModel.dismissFloatingNumber(item.id)
                    }
                )
            }
        }
    }
}

@Composable
fun WavyFloatingItem(
    text: String,
    colorType: String,
    startXShift: Float,
    onAnimationComplete: () -> Unit
) {
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 3800, easing = LinearEasing)
        )
        onAnimationComplete()
    }

    val progress = animProgress.value
    
    // Wave calculations
    // Y goes from bottom of screen (say +200dp offset) to top (-350dp offset)
    val yOffset = (200 - progress * 550).dp
    
    // X waves left and right using sine wave
    val waveX = (sin(progress * 3.5 * Math.PI) * 32).toFloat()
    val xOffset = (startXShift + waveX).dp
    
    // Fade out in the last 30% of the animation
    val alpha = if (progress < 0.7f) 1f else (1f - progress) / 0.3f

    // Scale up slightly at start and down at the end
    val scale = if (progress < 0.2f) {
        progress / 0.2f * 1.3f
    } else {
        (1.3f - (progress - 0.2f) * 0.37f).coerceAtLeast(0.8f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(
                translationX = xOffset.value,
                translationY = yOffset.value,
                alpha = alpha,
                scaleX = scale,
                scaleY = scale
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            color = when (colorType) {
                "red" -> Color(0xFFEF4444)
                "yellow" -> Color(0xFFF59E0B)
                else -> Color(0xFF8B5CF6)
            },
            letterSpacing = 1.sp
        )
    }
}
