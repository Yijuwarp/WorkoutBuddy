package com.example.workoutbuddy.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workoutbuddy.theme.*

// Callout label for a 0-100 performance score.
fun performanceCallout(score: Int): String = when {
    score <= 50 -> "OK"
    score <= 75 -> "Good"
    score <= 90 -> "Great"
    else -> "Amazing!"
}

/**
 * Hero card content: performance score ring on the left, Current Burn and set Progress
 * stats on the right. [score] is null until the first set is completed, rendered as "--".
 */
@Composable
fun PerformanceHeroCard(
    score: Int?,
    burnedCalories: Double,
    completedSets: Int,
    totalSets: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PerformanceScoreRing(
            score = score,
            modifier = Modifier.size(170.dp)
        )

        Spacer(modifier = Modifier.width(20.dp))

        Column(modifier = Modifier.weight(1f)) {
            HeroStat(
                icon = Icons.Default.LocalFireDepartment,
                iconTint = PerformanceRed,
                label = "Current Burn",
                value = "${burnedCalories.toInt()}",
                unit = " kcal"
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 14.dp),
                color = BorderLight
            )
            HeroStat(
                icon = Icons.Default.Bolt,
                iconTint = GreenSuccess,
                label = "Progress",
                value = "$completedSets/$totalSets",
                unit = "",
                valueColor = TextBlue
            )
        }
    }
}

@Composable
private fun HeroStat(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    unit: String,
    valueColor: Color = TextDark
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = valueColor
            )
            if (unit.isNotEmpty()) {
                Text(
                    text = unit,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun PerformanceScoreRing(
    score: Int?,
    modifier: Modifier = Modifier
) {
    val animatedFraction by animateFloatAsState(
        targetValue = (score ?: 0) / 100f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "performanceRing"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val inset = strokeWidth / 2
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = BorderLight,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            if (animatedFraction > 0f) {
                drawArc(
                    color = BluePrimary,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedFraction.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Performance\nScore",
                fontSize = 13.sp,
                lineHeight = 16.sp,
                color = TextDark,
                textAlign = TextAlign.Center
            )
            Text(
                text = score?.toString() ?: "--",
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                color = TextDark
            )
            Text(
                text = score?.let { performanceCallout(it) } ?: "Not started",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextBlue
            )
        }
    }
}
