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
    val TextTertiary: Color,
    // Neumorphic Design Tokens
    val NeuBackground: Color,
    val NeuLightShadow: Color,
    val NeuDarkShadow: Color,
    val NeuSurfaceRaised: Color,
    val NeuSurfaceSunken: Color,
    val NeuRaisedGradient: Brush,
    val NeuSunkenGradient: Brush,
    val NeuBorderHighlight: Color,
    val NeuBorderShadow: Color
)

val DarkPalette = LineaColorPalette(
    isLight = false,
    BackgroundTop = Color(0xFF1E2228),
    BackgroundBottom = Color(0xFF181B20),
    BackgroundDeep = Color(0xFF15181C),
    BackgroundElevated = Color(0xFF242930),
    BackgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF1E2228), Color(0xFF181B20))
    ),
    GlassFill = Color(0xFF22262E),
    GlassBorder = Color.White.copy(alpha = 0.09f),
    GlassFallbackFill = Color(0xFF1E2228),
    TitaniumBlue = Color(0xFF5C8DE6),
    DesaturatedTeal = Color(0xFF427A7A),
    MutedRust = Color(0xFFD4834E),
    MutedBrickRed = Color(0xFFE05347),
    MutedSageGreen = Color(0xFF4EAA78),
    GlassBorderFocused = Color(0xFF5C8DE6).copy(alpha = 0.6f),
    Danger = Color(0xFFE05347),
    Warning = Color(0xFFE69A38),
    Success = Color(0xFF4EAA78),
    CarmineRed = Color(0xFFE05347),
    AccentGreen = Color(0xFF4EAA78),
    AccentAmber = Color(0xFFE69A38),
    SurfaceElevated = Color(0xFF22262E),
    TextPrimary = Color(0xFFF1F3F5),
    TextSecondary = Color(0xFFA6ACB5),
    TextTertiary = Color(0xFF6E7580),
    // Neumorphic Values (Dark Neu)
    NeuBackground = Color(0xFF1E2228),
    NeuLightShadow = Color(0xFF2C333E),
    NeuDarkShadow = Color(0xFF0F1216),
    NeuSurfaceRaised = Color(0xFF21252C),
    NeuSurfaceSunken = Color(0xFF181B20),
    NeuRaisedGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF262B34), Color(0xFF1B1F25))
    ),
    NeuSunkenGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF16181D), Color(0xFF232830))
    ),
    NeuBorderHighlight = Color.White.copy(alpha = 0.10f),
    NeuBorderShadow = Color.Black.copy(alpha = 0.40f)
)

val AmoledPalette = LineaColorPalette(
    isLight = false,
    BackgroundTop = Color(0xFF0C0E12),
    BackgroundBottom = Color(0xFF08090C),
    BackgroundDeep = Color(0xFF050608),
    BackgroundElevated = Color(0xFF14171E),
    BackgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0C0E12), Color(0xFF08090C))
    ),
    GlassFill = Color(0xFF111419),
    GlassBorder = Color.White.copy(alpha = 0.08f),
    GlassFallbackFill = Color(0xFF0C0E12),
    TitaniumBlue = Color(0xFF6A9CE8),
    DesaturatedTeal = Color(0xFF457E7E),
    MutedRust = Color(0xFFD98852),
    MutedBrickRed = Color(0xFFE25A4E),
    MutedSageGreen = Color(0xFF55B380),
    GlassBorderFocused = Color(0xFF6A9CE8).copy(alpha = 0.7f),
    Danger = Color(0xFFE25A4E),
    Warning = Color(0xFFE89E3C),
    Success = Color(0xFF55B380),
    CarmineRed = Color(0xFFE25A4E),
    AccentGreen = Color(0xFF55B380),
    AccentAmber = Color(0xFFE89E3C),
    SurfaceElevated = Color(0xFF14171E),
    TextPrimary = Color(0xFFFFFFFF),
    TextSecondary = Color(0xFFA6ACB5),
    TextTertiary = Color(0xFF6E7580),
    // Neumorphic Values (Amoled Deep Neu)
    NeuBackground = Color(0xFF0C0E12),
    NeuLightShadow = Color(0xFF1C212A),
    NeuDarkShadow = Color(0xFF020304),
    NeuSurfaceRaised = Color(0xFF111419),
    NeuSurfaceSunken = Color(0xFF07080A),
    NeuRaisedGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF161A21), Color(0xFF0C0E12))
    ),
    NeuSunkenGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF050608), Color(0xFF13161C))
    ),
    NeuBorderHighlight = Color.White.copy(alpha = 0.08f),
    NeuBorderShadow = Color.Black.copy(alpha = 0.60f)
)

val LightPalette = LineaColorPalette(
    isLight = true,
    BackgroundTop = Color(0xFFE6EBF2),
    BackgroundBottom = Color(0xFFDCE2E9),
    BackgroundDeep = Color(0xFFD5DCE4),
    BackgroundElevated = Color(0xFFE4E9F0),
    BackgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFE6EBF2), Color(0xFFDCE2E9))
    ),
    GlassFill = Color(0xFFE0E5EC),
    GlassBorder = Color.White.copy(alpha = 0.65f),
    GlassFallbackFill = Color(0xFFE0E5EC),
    TitaniumBlue = Color(0xFF2B5B92),
    DesaturatedTeal = Color(0xFF2A6B6B),
    MutedRust = Color(0xFFBA5D1A),
    MutedBrickRed = Color(0xFFD32F2F),
    MutedSageGreen = Color(0xFF2E7D32),
    GlassBorderFocused = Color(0xFF2B5B92).copy(alpha = 0.7f),
    Danger = Color(0xFFD32F2F),
    Warning = Color(0xFFED6C02),
    Success = Color(0xFF2E7D32),
    CarmineRed = Color(0xFFD32F2F),
    AccentGreen = Color(0xFF2E7D32),
    AccentAmber = Color(0xFFED6C02),
    SurfaceElevated = Color(0xFFE2E7EE),
    TextPrimary = Color(0xFF242A36),
    TextSecondary = Color(0xFF5C6675),
    TextTertiary = Color(0xFF8C96A5),
    // Neumorphic Values (Light Soft Ceramic)
    NeuBackground = Color(0xFFE0E5EC),
    NeuLightShadow = Color.White.copy(alpha = 0.90f),
    NeuDarkShadow = Color(0xFFA3B1C6).copy(alpha = 0.55f),
    NeuSurfaceRaised = Color(0xFFE2E7EE),
    NeuSurfaceSunken = Color(0xFFD8DFE7),
    NeuRaisedGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFF0F4F8), Color(0xFFDCE2EA))
    ),
    NeuSunkenGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFD5DCE5), Color(0xFFEDF2F7))
    ),
    NeuBorderHighlight = Color.White.copy(alpha = 0.85f),
    NeuBorderShadow = Color(0xFFA3B1C6).copy(alpha = 0.35f)
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

    // Neumorphic Tokens
    var NeuBackground by mutableStateOf(DarkPalette.NeuBackground)
        internal set

    var NeuLightShadow by mutableStateOf(DarkPalette.NeuLightShadow)
        internal set

    var NeuDarkShadow by mutableStateOf(DarkPalette.NeuDarkShadow)
        internal set

    var NeuSurfaceRaised by mutableStateOf(DarkPalette.NeuSurfaceRaised)
        internal set

    var NeuSurfaceSunken by mutableStateOf(DarkPalette.NeuSurfaceSunken)
        internal set

    var NeuRaisedGradient by mutableStateOf(DarkPalette.NeuRaisedGradient)
        internal set

    var NeuSunkenGradient by mutableStateOf(DarkPalette.NeuSunkenGradient)
        internal set

    var NeuBorderHighlight by mutableStateOf(DarkPalette.NeuBorderHighlight)
        internal set

    var NeuBorderShadow by mutableStateOf(DarkPalette.NeuBorderShadow)
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
        NeuBackground = palette.NeuBackground
        NeuLightShadow = palette.NeuLightShadow
        NeuDarkShadow = palette.NeuDarkShadow
        NeuSurfaceRaised = palette.NeuSurfaceRaised
        NeuSurfaceSunken = palette.NeuSurfaceSunken
        NeuRaisedGradient = palette.NeuRaisedGradient
        NeuSunkenGradient = palette.NeuSunkenGradient
        NeuBorderHighlight = palette.NeuBorderHighlight
        NeuBorderShadow = palette.NeuBorderShadow
    }
}
