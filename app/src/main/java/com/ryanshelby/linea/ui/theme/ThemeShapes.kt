package com.ryanshelby.linea.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.ryanshelby.linea.data.preferences.DesignStyle

// =============================================================================
// Theme Shapes
// =============================================================================

val NeumorphicShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

val OrganicShapes = Shapes(
    small = RoundedCornerShape(topStart = 16.dp, topEnd = 8.dp, bottomEnd = 16.dp, bottomStart = 12.dp),
    medium = RoundedCornerShape(topStart = 26.dp, topEnd = 14.dp, bottomEnd = 26.dp, bottomStart = 18.dp),
    large = RoundedCornerShape(topStart = 30.dp, topEnd = 18.dp, bottomEnd = 30.dp, bottomStart = 22.dp),
    extraLarge = RoundedCornerShape(topStart = 34.dp, topEnd = 24.dp, bottomEnd = 34.dp, bottomStart = 28.dp)
)

val FintechShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(10.dp),
    extraLarge = RoundedCornerShape(14.dp)
)

val Material3Shapes = Shapes(
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

val NeoBrutalismShapes = Shapes(
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(14.dp)
)

val HandwrittenShapes = Shapes(
    small = RoundedCornerShape(topStart = 14.dp, topEnd = 8.dp, bottomEnd = 14.dp, bottomStart = 10.dp),
    medium = RoundedCornerShape(topStart = 20.dp, topEnd = 12.dp, bottomEnd = 22.dp, bottomStart = 14.dp),
    large = RoundedCornerShape(topStart = 24.dp, topEnd = 16.dp, bottomEnd = 26.dp, bottomStart = 18.dp),
    extraLarge = RoundedCornerShape(topStart = 28.dp, topEnd = 20.dp, bottomEnd = 30.dp, bottomStart = 22.dp)
)

val CrystalShapesModern = Shapes(
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

// =============================================================================
// Helper Resolvers
// =============================================================================

fun resolveThemeShapes(style: DesignStyle): Shapes = when (style) {
    DesignStyle.NEUMORPHISM, DesignStyle.CLASSIC -> NeumorphicShapes
    DesignStyle.ORGANIC -> OrganicShapes
    DesignStyle.FINTECH -> FintechShapes
    DesignStyle.MATERIAL3 -> Material3Shapes
    DesignStyle.NEO_BRUTALISM -> NeoBrutalismShapes
    DesignStyle.HANDWRITTEN -> HandwrittenShapes
    DesignStyle.CRYSTAL -> CrystalShapesModern
}

fun resolveDialpadKeyShape(style: DesignStyle): Shape = when (style) {
    DesignStyle.NEUMORPHISM, DesignStyle.CLASSIC -> CircleShape
    DesignStyle.ORGANIC -> RoundedCornerShape(topStart = 26.dp, topEnd = 16.dp, bottomEnd = 26.dp, bottomStart = 18.dp)
    DesignStyle.FINTECH -> RoundedCornerShape(12.dp)
    DesignStyle.MATERIAL3 -> RoundedCornerShape(24.dp)
    DesignStyle.NEO_BRUTALISM -> RoundedCornerShape(8.dp)
    DesignStyle.HANDWRITTEN -> RoundedCornerShape(topStart = 22.dp, topEnd = 16.dp, bottomEnd = 20.dp, bottomStart = 18.dp)
    DesignStyle.CRYSTAL -> RoundedCornerShape(24.dp)
}

fun resolveCallButtonShape(style: DesignStyle): Shape = when (style) {
    DesignStyle.NEUMORPHISM, DesignStyle.CLASSIC -> CircleShape
    DesignStyle.ORGANIC -> RoundedCornerShape(topStart = 28.dp, topEnd = 18.dp, bottomEnd = 28.dp, bottomStart = 22.dp)
    DesignStyle.FINTECH -> RoundedCornerShape(12.dp)
    DesignStyle.MATERIAL3 -> RoundedCornerShape(28.dp)
    DesignStyle.NEO_BRUTALISM -> RoundedCornerShape(10.dp)
    DesignStyle.HANDWRITTEN -> RoundedCornerShape(topStart = 24.dp, topEnd = 18.dp, bottomEnd = 22.dp, bottomStart = 20.dp)
    DesignStyle.CRYSTAL -> CircleShape
}

