package com.ryanshelby.linea.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object LineaDimensions {
    val BaseUnit = 4.dp
    val ScreenPadding = 16.dp
    val RowGap = 8.dp
    val SectionGap = 24.dp
    val PanelPadding = 20.dp
    val PanelCornerRadius = 20.dp
    val CardCornerRadius = 20.dp
    val ButtonCornerRadius = 12.dp
    val HairlineBorder = 1.dp
    val GlassBlur = 20.dp
}

val LineaShapes = Shapes(
    small = RoundedCornerShape(12.dp),      // Buttons and chips: 12dp
    medium = RoundedCornerShape(20.dp),     // Panels and cards: 20dp
    large = RoundedCornerShape(20.dp),      // Large sheets: 20dp
    extraLarge = CircleShape                // Circular dialpad keys
)
