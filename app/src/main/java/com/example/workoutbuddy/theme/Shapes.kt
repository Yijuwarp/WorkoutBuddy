package com.example.workoutbuddy.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

// Corner radius outside the standard M3 scale, used for a few emphasis surfaces
// (e.g. onboarding cards) that intentionally read as more rounded than extraLarge.
val ShapeHero = RoundedCornerShape(36.dp)
