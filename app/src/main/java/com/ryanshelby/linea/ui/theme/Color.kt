package com.ryanshelby.linea.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

data class LineaColorPalette(
    val isLight: Boolean,
    val BackgroundTop: Color,
    val BackgroundBottom: Color,
    val BackgroundDeep: Color,
    val BackgroundElevated: Color,
    val BackgroundGradient: Brush,
    val GlassFill: Color,
    val GlassBorder: Color,
    val GlassFallbackFill: Color,
    val TitaniumBlue: Color,
    val DesaturatedTeal: Color,
    val MutedRust: Color,
    val MutedBrickRed: Color,
    val MutedSageGreen: Color,
    val GlassBorderFocused: Color,
    val Danger: Color,
    val Warning: Color,
    val Success: Color,
    val CarmineRed: Color,
    val AccentGreen: Color,
    val AccentAmber: Color,
    val SurfaceElevated: Color,
    val TextPrimary: Color,
    val TextSecondary: Color,
    val TextTertiary: Color
)

val DarkPalette = LineaColorPalette(
    isLight = false,
    BackgroundTop = Color(0xFF0D0F12),
    BackgroundBottom = Color(0xFF1A1D21),
    BackgroundDeep = Color(0xFF0D0F12),
    BackgroundElevated = Color(0xFF1A1D21),
    BackgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0D0F12), Color(0xFF1A1D21))
    ),
    GlassFill = Color.White.copy(alpha = 0.08f),
    GlassBorder = Color.White.copy(alpha = 0.12f),
    GlassFallbackFill = Color(0xFF14171B),
    TitaniumBlue = Color(0xFF5C7C99),
    DesaturatedTeal = Color(0xFF3E5C5C),
    MutedRust = Color(0xFFC97B4A),
    MutedBrickRed = Color(0xFFB5473F),
    MutedSageGreen = Color(0xFF5A8F6B),
    GlassBorderFocused = Color(0xFF5C7C99).copy(alpha = 0.6f),
    Danger = Color(0xFFB5473F),
    Warning = Color(0xFFC97B4A),
    Success = Color(0xFF5A8F6B),
    CarmineRed = Color(0xFFB5473F),
    AccentGreen = Color(0xFF5A8F6B),
    AccentAmber = Color(0xFFC97B4A),
    SurfaceElevated = Color(0xFF16191D),
    TextPrimary = Color(0xFFE8EAED),
    TextSecondary = Color(0xFF9BA1A8),
    TextTertiary = Color(0xFF5C6169)
)

val AmoledPalette = LineaColorPalette(
    isLight = false,
    BackgroundTop = Color(0xFF000000),
    BackgroundBottom = Color(0xFF000000),
    BackgroundDeep = Color(0xFF000000),
    BackgroundElevated = Color(0xFF0A0A0A),
    BackgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF000000), Color(0xFF0A0A0A))
    ),
    GlassFill = Color.White.copy(alpha = 0.06f),
    GlassBorder = Color.White.copy(alpha = 0.16f),
    GlassFallbackFill = Color(0xFF0D0D0D),
    TitaniumBlue = Color(0xFF6B8FA8),
    DesaturatedTeal = Color(0xFF3E5C5C),
    MutedRust = Color(0xFFC97B4A),
    MutedBrickRed = Color(0xFFB5473F),
    MutedSageGreen = Color(0xFF5A8F6B),
    GlassBorderFocused = Color(0xFF6B8FA8).copy(alpha = 0.7f),
    Danger = Color(0xFFB5473F),
    Warning = Color(0xFFC97B4A),
    Success = Color(0xFF5A8F6B),
    CarmineRed = Color(0xFFB5473F),
    AccentGreen = Color(0xFF5A8F6B),
    AccentAmber = Color(0xFFC97B4A),
    SurfaceElevated = Color(0xFF0D0D0D),
    TextPrimary = Color(0xFFFFFFFF),
    TextSecondary = Color(0xFFA0A6AD),
    TextTertiary = Color(0xFF666C73)
)

val LightPalette = LineaColorPalette(
    isLight = true,
    BackgroundTop = Color(0xFFF4F6F9),
    BackgroundBottom = Color(0xFFE2E7ED),
    BackgroundDeep = Color(0xFFF4F6F9),
    BackgroundElevated = Color(0xFFFFFFFF),
    BackgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF4F6F9), Color(0xFFE2E7ED))
    ),
    GlassFill = Color.White.copy(alpha = 0.65f),
    GlassBorder = Color.White.copy(alpha = 0.90f),
    GlassFallbackFill = Color(0xFFF1F5F9),
    TitaniumBlue = Color(0xFF2C5E8A),
    DesaturatedTeal = Color(0xFF2B5B5B),
    MutedRust = Color(0xFFB45309),
    MutedBrickRed = Color(0xFFDC2626),
    MutedSageGreen = Color(0xFF16A34A),
    GlassBorderFocused = Color(0xFF2C5E8A).copy(alpha = 0.7f),
    Danger = Color(0xFFDC2626),
    Warning = Color(0xFFB45309),
    Success = Color(0xFF16A34A),
    CarmineRed = Color(0xFFDC2626),
    AccentGreen = Color(0xFF16A34A),
    AccentAmber = Color(0xFFB45309),
    SurfaceElevated = Color(0xFFFFFFFF),
    TextPrimary = Color(0xFF0F172A),
    TextSecondary = Color(0xFF475569),
    TextTertiary = Color(0xFF94A3B8)
)

object LineaColors {
    var isLight by mutableStateOf(DarkPalette.isLight)
        internal set

    var BackgroundTop by mutableStateOf(DarkPalette.BackgroundTop)
        internal set

    var BackgroundBottom by mutableStateOf(DarkPalette.BackgroundBottom)
        internal set

    var BackgroundDeep by mutableStateOf(DarkPalette.BackgroundDeep)
        internal set

    var BackgroundElevated by mutableStateOf(DarkPalette.BackgroundElevated)
        internal set

    var BackgroundGradient by mutableStateOf(DarkPalette.BackgroundGradient)
        internal set

    var GlassFill by mutableStateOf(DarkPalette.GlassFill)
        internal set

    var GlassBorder by mutableStateOf(DarkPalette.GlassBorder)
        internal set

    var GlassFallbackFill by mutableStateOf(DarkPalette.GlassFallbackFill)
        internal set

    var TitaniumBlue by mutableStateOf(DarkPalette.TitaniumBlue)
        internal set

    var DesaturatedTeal by mutableStateOf(DarkPalette.DesaturatedTeal)
        internal set

    var MutedRust by mutableStateOf(DarkPalette.MutedRust)
        internal set

    var MutedBrickRed by mutableStateOf(DarkPalette.MutedBrickRed)
        internal set

    var MutedSageGreen by mutableStateOf(DarkPalette.MutedSageGreen)
        internal set

    var GlassBorderFocused by mutableStateOf(DarkPalette.GlassBorderFocused)
        internal set

    var Danger by mutableStateOf(DarkPalette.Danger)
        internal set

    var Warning by mutableStateOf(DarkPalette.Warning)
        internal set

    var Success by mutableStateOf(DarkPalette.Success)
        internal set

    var CarmineRed by mutableStateOf(DarkPalette.CarmineRed)
        internal set

    var AccentGreen by mutableStateOf(DarkPalette.AccentGreen)
        internal set

    var AccentAmber by mutableStateOf(DarkPalette.AccentAmber)
        internal set

    var SurfaceElevated by mutableStateOf(DarkPalette.SurfaceElevated)
        internal set

    var TextPrimary by mutableStateOf(DarkPalette.TextPrimary)
        internal set

    var TextSecondary by mutableStateOf(DarkPalette.TextSecondary)
        internal set

    var TextTertiary by mutableStateOf(DarkPalette.TextTertiary)
        internal set

    fun updatePalette(palette: LineaColorPalette) {
        isLight = palette.isLight
        BackgroundTop = palette.BackgroundTop
        BackgroundBottom = palette.BackgroundBottom
        BackgroundDeep = palette.BackgroundDeep
        BackgroundElevated = palette.BackgroundElevated
        BackgroundGradient = palette.BackgroundGradient
        GlassFill = palette.GlassFill
        GlassBorder = palette.GlassBorder
        GlassFallbackFill = palette.GlassFallbackFill
        TitaniumBlue = palette.TitaniumBlue
        DesaturatedTeal = palette.DesaturatedTeal
        MutedRust = palette.MutedRust
        MutedBrickRed = palette.MutedBrickRed
        MutedSageGreen = palette.MutedSageGreen
        GlassBorderFocused = palette.GlassBorderFocused
        Danger = palette.Danger
        Warning = palette.Warning
        Success = palette.Success
        CarmineRed = palette.CarmineRed
        AccentGreen = palette.AccentGreen
        AccentAmber = palette.AccentAmber
        SurfaceElevated = palette.SurfaceElevated
        TextPrimary = palette.TextPrimary
        TextSecondary = palette.TextSecondary
        TextTertiary = palette.TextTertiary
    }
}
