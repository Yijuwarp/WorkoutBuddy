package com.example.workoutbuddy.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workoutbuddy.theme.*
import kotlin.math.cos
import kotlin.math.sin

private const val MEDIUM_THRESHOLD  = 0.30
private const val HIGH_THRESHOLD    = 0.65
private const val EXTREME_THRESHOLD = 0.95

@Composable
fun WorkoutIntensityDial(
    intensityScore: Double,
    targetScore: Double,
    modifier: Modifier = Modifier
) {
    val safeTarget = if (targetScore <= 0.0) 100.0f else targetScore.toFloat()

    // Smoothly animate sweep angle
    val animatedIntensity by animateFloatAsState(
        targetValue = intensityScore.toFloat(),
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "DialSweep"
    )

    val progress = (animatedIntensity / safeTarget).coerceIn(0f, 1.0f)
    val startAngle = 140f
    val totalSweepAngle = 260f
    val currentSweepAngle = progress * totalSweepAngle

    val ratio = intensityScore / safeTarget
    val zoneText = remember(ratio) {
        when {
            ratio < MEDIUM_THRESHOLD  -> "LOW"
            ratio < HIGH_THRESHOLD    -> "MEDIUM"
            ratio < EXTREME_THRESHOLD -> "HIGH"
            else                      -> "EXTREME"
        }
    }

    val dialColor = PerformanceRed

    Box(
        modifier = modifier.size(130.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 10.dp.toPx()
            val sizePx = size.width
            val center = Offset(sizePx / 2f, sizePx / 2f)
            val radius = (sizePx - strokeWidth) / 2f

            // 1. Background track
            drawArc(
                color = BorderLight.copy(alpha = 0.5f),
                startAngle = startAngle,
                sweepAngle = totalSweepAngle,
                useCenter = false,
                topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // 2. Active arc
            if (currentSweepAngle > 0.1f) {
                drawArc(
                    color = dialColor,
                    startAngle = startAngle,
                    sweepAngle = currentSweepAngle,
                    useCenter = false,
                    topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // 3. Glowing pointer dot at arc tip
                val endAngleRad = Math.toRadians((startAngle + currentSweepAngle).toDouble())
                val pointerX = center.x + radius * cos(endAngleRad).toFloat()
                val pointerY = center.y + radius * sin(endAngleRad).toFloat()
                drawCircle(color = Color.White, radius = 5.dp.toPx(), center = Offset(pointerX, pointerY))
                drawCircle(color = dialColor, radius = 3.dp.toPx(), center = Offset(pointerX, pointerY))
            }
        }

        // 4. Center text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${intensityScore.toInt()}",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = TextDark,
                lineHeight = 32.sp
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = zoneText,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = dialColor,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "PERFORMANCE",
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                color = dialColor,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun WorkoutBurnDial(
    burnedCalories: Double,
    targetCalories: Double,
    modifier: Modifier = Modifier
) {
    val safeTarget = if (targetCalories <= 0.0) 100.0f else targetCalories.toFloat()

    val animatedCalories by animateFloatAsState(
        targetValue = burnedCalories.toFloat(),
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "BurnSweep"
    )

    val progress = (animatedCalories / safeTarget).coerceIn(0f, 1.0f)
    val startAngle = 140f
    val totalSweepAngle = 260f
    val currentSweepAngle = progress * totalSweepAngle

    val ratio = burnedCalories / safeTarget
    val zoneText = remember(ratio) {
        when {
            ratio < 0.30 -> "LOW"
            ratio < 0.65 -> "MEDIUM"
            ratio < 0.95 -> "HIGH"
            else         -> "EXTREME"
        }
    }

    val dialColor = BurnAmber

    Box(
        modifier = modifier.size(130.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 10.dp.toPx()
            val sizePx = size.width
            val center = Offset(sizePx / 2f, sizePx / 2f)
            val radius = (sizePx - strokeWidth) / 2f

            // 1. Background track
            drawArc(
                color = BorderLight.copy(alpha = 0.5f),
                startAngle = startAngle,
                sweepAngle = totalSweepAngle,
                useCenter = false,
                topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // 2. Active arc
            if (currentSweepAngle > 0.1f) {
                drawArc(
                    color = dialColor,
                    startAngle = startAngle,
                    sweepAngle = currentSweepAngle,
                    useCenter = false,
                    topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // 3. Glowing pointer dot at arc tip
                val endAngleRad = Math.toRadians((startAngle + currentSweepAngle).toDouble())
                val pointerX = center.x + radius * cos(endAngleRad).toFloat()
                val pointerY = center.y + radius * sin(endAngleRad).toFloat()
                drawCircle(color = Color.White, radius = 5.dp.toPx(), center = Offset(pointerX, pointerY))
                drawCircle(color = dialColor, radius = 3.dp.toPx(), center = Offset(pointerX, pointerY))
            }
        }

        // Center text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${burnedCalories.toInt()}",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = TextDark,
                lineHeight = 32.sp
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = zoneText,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = dialColor,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "BURN",
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                color = dialColor,
                letterSpacing = 0.5.sp
            )
        }
    }
}
