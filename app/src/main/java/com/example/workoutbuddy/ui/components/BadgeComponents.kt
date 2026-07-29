package com.example.workoutbuddy.ui.components

import kotlin.math.roundToInt
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workoutbuddy.theme.*

fun getExerciseDrawableResourceName(name: String): String {
    val clean = name.lowercase()
        .replace(Regex("[^a-z0-9]"), "_")
        .replace(Regex("_+"), "_")
        .trim('_')
    return "ic_ex_$clean"
}

@Composable
fun ExerciseThumbnail(
    exerciseName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val resName = remember(exerciseName) { getExerciseDrawableResourceName(exerciseName) }
    val resId = remember(resName) {
        context.resources.getIdentifier(resName, "drawable", context.packageName)
    }

    Crossfade(targetState = resId, label = "exerciseThumbnailCrossfade") { animatedResId ->
        if (animatedResId != 0) {
            Image(
                painter = painterResource(id = animatedResId),
                contentDescription = "$exerciseName thumbnail",
                modifier = modifier
                    .clip(MaterialTheme.shapes.extraSmall),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = modifier
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(LightBlueContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = "Placeholder",
                    tint = BluePrimary,
                    modifier = Modifier.fillMaxSize(0.5f)
                )
            }
        }
    }
}


@Composable
fun ExerciseInfoChip(text: String) {
    Box(
        modifier = Modifier
            .border(1.dp, BorderLight, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = TextDark
        )
    }
}

// --- Frequency Slider (Never <-> Always, 5 snap positions) ---

// The middle position is intentionally `null` and unlabeled: it represents "no change" /
// neutral, the same as an exercise with no preference row at all. It's a real, reachable slider
// position (not just a fallback default) so users can explicitly drag back to neutral instead
// of only being able to move away from it.
private val FREQUENCY_SLIDER_STEPS: List<com.example.workoutbuddy.data.Frequency?> = listOf(
    com.example.workoutbuddy.data.Frequency.NEVER,
    com.example.workoutbuddy.data.Frequency.LESS,
    null,
    com.example.workoutbuddy.data.Frequency.OFTEN,
    com.example.workoutbuddy.data.Frequency.ALWAYS
)

private val FREQUENCY_SLIDER_NEUTRAL_DEFAULT_INDEX = 2 // the null/no-change position

/**
 * A 5-position discrete slider between Never and Always, used both in the in-context exercise
 * card menu and the Manage Exercises screen. The middle position is the unlabeled "no change"
 * slot. Reports the snapped [Frequency] (or `null` for "no change") only once the user releases
 * the thumb on a different position than where it started.
 */
@Composable
fun FrequencySlider(
    currentFrequency: com.example.workoutbuddy.data.Frequency?,
    onFrequencyChange: (com.example.workoutbuddy.data.Frequency?) -> Unit,
    modifier: Modifier = Modifier
) {
    val initialIndex = currentFrequency?.let { FREQUENCY_SLIDER_STEPS.indexOf(it) }
        .let { if (it == null || it < 0) FREQUENCY_SLIDER_NEUTRAL_DEFAULT_INDEX else it }
    var sliderPosition by remember(currentFrequency) { mutableFloatStateOf(initialIndex.toFloat()) }
    val snappedIndex = sliderPosition.roundToInt().coerceIn(0, FREQUENCY_SLIDER_STEPS.lastIndex)

    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            onValueChangeFinished = {
                val finalIndex = sliderPosition.roundToInt().coerceIn(0, FREQUENCY_SLIDER_STEPS.lastIndex)
                sliderPosition = finalIndex.toFloat()
                if (finalIndex != initialIndex) {
                    onFrequencyChange(FREQUENCY_SLIDER_STEPS[finalIndex])
                }
            },
            valueRange = 0f..(FREQUENCY_SLIDER_STEPS.size - 1).toFloat(),
            steps = FREQUENCY_SLIDER_STEPS.size - 2, // 3 intermediate stops between the 5 positions
            colors = SliderDefaults.colors(
                thumbColor = BluePrimary,
                activeTrackColor = BluePrimary,
                inactiveTrackColor = BluePrimary.copy(alpha = 0.2f)
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("Never", "Less", "", "Often", "Always").forEachIndexed { index, label ->
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = if (index == snappedIndex) FontWeight.Bold else FontWeight.Normal,
                    color = if (index == snappedIndex) BluePrimary else TextMuted
                )
            }
        }
    }
}

// --- Difficulty Slider (Easy -> Hard, 3 snap positions) ---

private val DIFFICULTY_SLIDER_STEPS = listOf(
    com.example.workoutbuddy.data.Difficulty.EASY,
    com.example.workoutbuddy.data.Difficulty.MEDIUM,
    com.example.workoutbuddy.data.Difficulty.HARD
)

/**
 * A custom Canvas-drawn slider with N evenly-spaced snap notches and labels aligned exactly
 * under each notch, matching the gym-experience slider used in onboarding
 * (see OnboardingGymExperienceStep). Shared by [DifficultySlider] and any other discrete
 * snap-to-N-positions slider that wants the same look.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnapNotchSlider(
    labels: List<String>,
    selectedIndex: Int,
    onIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val snapValues = if (labels.size == 1) listOf(0.5f) else
        labels.indices.map { idx ->
            0.02f + (0.96f) * (idx.toFloat() / (labels.size - 1))
        }
    val sliderValue = snapValues[selectedIndex.coerceIn(0, labels.lastIndex)]

    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = sliderValue,
            onValueChange = { newValue ->
                val nearestIndex = snapValues.indices.minByOrNull { kotlin.math.abs(snapValues[it] - newValue) } ?: 0
                onIndexChange(nearestIndex)
            },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = BlueSecondary
            ),
            modifier = Modifier
                .fillMaxWidth(),
            track = { _ ->
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

                    drawRoundRect(
                        color = inactiveColor,
                        topLeft = Offset(0f, centerY - trackHeight / 2f),
                        size = Size(trackWidth, trackHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackHeight / 2f)
                    )

                    val activeWidth = trackWidth * sliderValue
                    drawRoundRect(
                        color = activeColor,
                        topLeft = Offset(0f, centerY - trackHeight / 2f),
                        size = Size(activeWidth, trackHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackHeight / 2f)
                    )

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

        Layout(
            content = {
                labels.forEachIndexed { idx, label ->
                    Text(
                        text = label,
                        color = if (idx == selectedIndex) BlueSecondary else Color.White.copy(alpha = 0.4f),
                        fontWeight = if (idx == selectedIndex) FontWeight.Black else FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
            val trackWidth = constraints.maxWidth

            layout(trackWidth, placeables.maxOfOrNull { it.height } ?: 0) {
                placeables.forEachIndexed { idx, placeable ->
                    val fraction = snapValues[idx]
                    val notchX = trackWidth * fraction
                    val x = (notchX - placeable.width / 2f)
                        .coerceIn(0f, trackWidth.toFloat() - placeable.width)
                    placeable.placeRelative(x.toInt(), 0)
                }
            }
        }
    }
}

/**
 * A 3-position discrete slider between Easy and Hard, used for both the first-launch difficulty
 * ceiling picker and the later settings control. Unlike [FrequencySlider] there's no neutral/
 * untagged state to preserve, so [onSelectedChange] fires on every settled position (including
 * landing back on the value it started at) and callers hold their own "pending selection" state
 * until an explicit Continue/Save action persists it. Visually matches the onboarding
 * gym-experience slider via the shared [SnapNotchSlider].
 */
@Composable
fun DifficultySlider(
    selected: com.example.workoutbuddy.data.Difficulty,
    onSelectedChange: (com.example.workoutbuddy.data.Difficulty) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = DIFFICULTY_SLIDER_STEPS.indexOf(selected).coerceIn(0, DIFFICULTY_SLIDER_STEPS.lastIndex)

    SnapNotchSlider(
        labels = listOf("Easy", "Medium", "Hard"),
        selectedIndex = selectedIndex,
        onIndexChange = { index -> onSelectedChange(DIFFICULTY_SLIDER_STEPS[index]) },
        modifier = modifier
    )
}

