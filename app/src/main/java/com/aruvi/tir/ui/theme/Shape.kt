package com.aruvi.tir.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

// TV-specific shapes
val CardShape = RoundedCornerShape(8.dp)
val DialogShape = RoundedCornerShape(16.dp)
val ButtonShape = RoundedCornerShape(8.dp)

// Shared spacing constants for consistent layout
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
    val section = 24.dp
    val grid = 16.dp
    val cardPadding = 12.dp
    val contentPadding = 16.dp
}
