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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workoutbuddy.theme.*
import kotlin.math.cos
import kotlin.math.sin

private const val MEDIUM_THRESHOLD  = 0.30
private const val HIGH_THRESHOLD    = 0.65
private const val EXTREME_THRESHOLD = 0.95

// The target sits at 80% of the dial sweep; the dial's full range extends 25% past
// the target so the arc can visibly pass the tick when the user exceeds it.
private const val TARGET_SWEEP_FRACTION = 0.8f

private val START_ANGLE = 140f
private const val TOTAL_SWEEP = 260f

// Arc color as a function of dial fraction (0..1 of the full dial range):
// white -> green by halfway, green -> gold at the target tick, gold beyond.
private fun arcColorAt(dialFraction: Float): Color = when {
    dialFraction <= 0.5f -> lerp(Color.White, GreenSuccess, (dialFraction / 0.5f).coerceIn(0f, 1f))
    dialFraction <= TARGET_SWEEP_FRACTION ->
        lerp(GreenSuccess, GoldPR, ((dialFraction - 0.5f) / (TARGET_SWEEP_FRACTION - 0.5f)).coerceIn(0f, 1f))
    else -> GoldPR
}

@Composable
private fun WorkoutDial(
    value: Double,
    target: Double,
    captionText: String,
    labelColor: Color,
    modifier: Modifier = Modifier
) {
    val safeTarget = if (target <= 0.0) 100.0f else target.toFloat()
    // Full dial range extends past the target so the tick isn't at the very end.
    val dialMax = safeTarget / TARGET_SWEEP_FRACTION

    val animatedValue by animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "DialSweep"
    )

    val progress = (animatedValue / dialMax).coerceIn(0f, 1f)
    val currentSweepAngle = progress * TOTAL_SWEEP

    val ratio = value / safeTarget
    val zoneText = remember(ratio) {
        when {
            ratio < MEDIUM_THRESHOLD  -> "LOW"
            ratio < HIGH_THRESHOLD    -> "MEDIUM"
            ratio < EXTREME_THRESHOLD -> "HIGH"
            else                      -> "EXTREME"
        }
    }

    Box(
        modifier = modifier.size(130.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 10.dp.toPx()
            val sizePx = size.width
            val center = Offset(sizePx / 2f, sizePx / 2f)
            val radius = (sizePx - strokeWidth) / 2f
            val arcTopLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
            val arcSize = Size(radius * 2f, radius * 2f)

            // 1. Background track
            drawArc(
                color = BorderLight.copy(alpha = 0.5f),
                startAngle = START_ANGLE,
                sweepAngle = TOTAL_SWEEP,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // 2. Active arc, drawn in small segments so the color sweeps
            //    white -> green -> gold as progress increases.
            if (currentSweepAngle > 0.1f) {
                val segments = 60
                val segmentSweep = currentSweepAngle / segments
                for (i in 0 until segments) {
                    val segStart = START_ANGLE + i * segmentSweep
                    val segFraction = (i + 0.5f) * segmentSweep / TOTAL_SWEEP
                    drawArc(
                        color = arcColorAt(segFraction),
                        startAngle = segStart,
                        // Slight overlap between segments avoids hairline gaps.
                        sweepAngle = segmentSweep + 0.6f,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )
                }

                // 3. Glowing pointer dot at arc tip, tinted to match the tip color
                val tipColor = arcColorAt(progress)
                val endAngleRad = Math.toRadians((START_ANGLE + currentSweepAngle).toDouble())
                val pointerX = center.x + radius * cos(endAngleRad).toFloat()
                val pointerY = center.y + radius * sin(endAngleRad).toFloat()
                drawCircle(color = Color.White, radius = 5.dp.toPx(), center = Offset(pointerX, pointerY))
                drawCircle(color = tipColor, radius = 3.dp.toPx(), center = Offset(pointerX, pointerY))
            }

            // 4. Target tick mark: a short radial line crossing the track at the target angle.
            val tickAngleRad = Math.toRadians((START_ANGLE + TARGET_SWEEP_FRACTION * TOTAL_SWEEP).toDouble())
            val tickOverhang = 4.dp.toPx()
            val innerR = radius - strokeWidth / 2f - tickOverhang
            val outerR = radius + strokeWidth / 2f + tickOverhang
            val cosA = cos(tickAngleRad).toFloat()
            val sinA = sin(tickAngleRad).toFloat()
            drawLine(
                color = TextMuted,
                start = Offset(center.x + innerR * cosA, center.y + innerR * sinA),
                end = Offset(center.x + outerR * cosA, center.y + outerR * sinA),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // 5. Center text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${value.toInt()}",
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
                color = labelColor,
                letterSpacing = 0.5.sp
            )
            Text(
                text = captionText,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                color = labelColor,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun WorkoutIntensityDial(
    intensityScore: Double,
    targetScore: Double,
    modifier: Modifier = Modifier
) {
    WorkoutDial(
        value = intensityScore,
        target = targetScore,
        captionText = "PERFORMANCE",
        labelColor = PerformanceRed,
        modifier = modifier
    )
}

@Composable
fun WorkoutBurnDial(
    burnedCalories: Double,
    targetCalories: Double,
    modifier: Modifier = Modifier
) {
    WorkoutDial(
        value = burnedCalories,
        target = targetCalories,
        captionText = "BURN",
        labelColor = BurnAmber,
        modifier = modifier
    )
}
