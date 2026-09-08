package com.ryanshelby.linea.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object LineaColors {
    // Background Gradient (vertical from deep graphite to lighter graphite)
    val BackgroundTop = Color(0xFF0D0F12)
    val BackgroundBottom = Color(0xFF1A1D21)
    val BackgroundDeep = BackgroundTop
    val BackgroundElevated = BackgroundBottom
    
    val BackgroundGradient = Brush.verticalGradient(
        colors = listOf(BackgroundTop, BackgroundBottom)
    )

    // Frosted Glass Surfaces
    val GlassFill = Color.White.copy(alpha = 0.08f)
    val GlassBorder = Color.White.copy(alpha = 0.12f)
    val GlassFallbackFill = Color(0xFF14171B)

    // Accents
    val TitaniumBlue = Color(0xFF5C7C99)      // Primary accent (call button, active tab)
    val DesaturatedTeal = Color(0xFF3E5C5C)   // Secondary accent (active recording, rule badge)
    val MutedRust = Color(0xFFC97B4A)         // Warning (missed call badge, blocked indicator)
    val MutedBrickRed = Color(0xFFB5473F)     // Reject, end-call, delete actions
    val MutedSageGreen = Color(0xFF5A8F6B)    // Connected, answered, success states

    // Aliases & focused states
    val GlassBorderFocused = TitaniumBlue.copy(alpha = 0.6f)
    val Danger = MutedBrickRed
    val Warning = MutedRust
    val Success = MutedSageGreen
    val CarmineRed = MutedBrickRed
    val AccentGreen = MutedSageGreen
    val AccentAmber = MutedRust
    val SurfaceElevated = Color(0xFF16191D)

    // Text hierarchy
    val TextPrimary = Color(0xFFE8EAED)       // Off-white
    val TextSecondary = Color(0xFF9BA1A8)     // Cool gray
    val TextTertiary = Color(0xFF5C6169)      // Disabled / tertiary
}
