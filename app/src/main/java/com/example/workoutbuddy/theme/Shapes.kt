package com.example.workoutbuddy.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

// Corner radius outside the standard M3 scale, used for a few emphasis surfaces
// (e.g. onboarding cards) that intentionally read as more rounded than extraLarge.
val ShapeHero = RoundedCornerShape(24.dp)
