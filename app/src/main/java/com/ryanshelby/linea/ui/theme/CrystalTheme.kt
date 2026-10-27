package com.ryanshelby.linea.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.ryanshelby.linea.data.preferences.DesignStyle

val LocalDesignStyle = staticCompositionLocalOf { DesignStyle.CLASSIC }

/** Neutral materials; semantic colors retain their meaning in both designs. */
fun crystalPalette(base: LineaColorPalette): LineaColorPalette {
    val light = base.isLight
    val oled = base === AmoledPalette
    val top = if (oled) Color(0xFF080D10) else if (light) Color(0xFFEDF0F1) else Color(0xFF20282E)
    val bottom = if (oled) Color.Black else if (light) Color(0xFFD9E0E3) else Color(0xFF101619)
    val surface = if (light) Color(0xFFE8EEF0) else Color(0xFF29343B)
    val inset = if (light) Color(0xFFDDE5E8) else Color(0xFF192229)
    val accent = if (light) Color(0xFF3F6275) else Color(0xFFAEC7D3)
    val green = if (light) Color(0xFF3D6A57) else Color(0xFF91B5A0)
    val red = if (light) Color(0xFF994A42) else Color(0xFFDAA097)
    val amber = if (light) Color(0xFF815A35) else Color(0xFFCCB190)
    return base.copy(
        BackgroundTop = top, BackgroundBottom = bottom, BackgroundDeep = bottom,
        BackgroundElevated = surface, BackgroundGradient = Brush.verticalGradient(listOf(top, bottom)),
        GlassFill = surface, GlassFallbackFill = surface,
        GlassBorder = if (light) Color.White.copy(alpha = .85f) else Color.White.copy(alpha = .23f),
        TitaniumBlue = accent, DesaturatedTeal = accent, MutedSageGreen = green,
        MutedRust = amber, MutedBrickRed = red, GlassBorderFocused = accent,
        Danger = red, Warning = amber, Success = green, CarmineRed = red,
        AccentGreen = green, AccentAmber = amber, SurfaceElevated = surface,
        TextPrimary = if (light) Color(0xFF182B35) else Color(0xFFF0F5F7),
        TextSecondary = if (light) Color(0xFF455D6A) else Color(0xFFBCCBD3),
        TextTertiary = if (light) Color(0xFF526873) else Color(0xFFA9BAC4),
        NeuBackground = top, NeuSurfaceRaised = surface, NeuSurfaceSunken = inset,
        NeuLightShadow = Color.White.copy(alpha = if (light) .7f else .1f),
        NeuDarkShadow = Color.Black.copy(alpha = if (light) .15f else .4f),
        NeuRaisedGradient = Brush.verticalGradient(listOf(surface, inset)),
        NeuSunkenGradient = Brush.verticalGradient(listOf(inset, surface)),
        NeuBorderHighlight = Color.White.copy(alpha = if (light) .95f else .35f),
        NeuBorderShadow = Color.Black.copy(alpha = .16f)
    )
}

/** Stationary light fields give the translucent surfaces something to reveal, without idle animation. */
@Composable
fun CrystalBackdrop() {
    val light = LineaColors.isLight
    Canvas(Modifier.fillMaxSize()) {
        val radius = size.width * 1.1f
        drawCircle(
            Brush.radialGradient(
                listOf(Color.White.copy(alpha = if (light) .9f else .1f), Color.Transparent),
                center = Offset(size.width * .9f, size.height * .25f), radius = radius
            ), radius, Offset(size.width * .9f, size.height * .25f)
        )
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0xFF829DAA).copy(alpha = if (light) .2f else .12f), Color.Transparent),
                center = Offset(0f, size.height * .72f), radius = radius
            ), radius, Offset(0f, size.height * .72f)
        )
        // Broad, quiet caustic curves behind the controls.
        repeat(3) { index ->
            drawArc(
                color = Color.White.copy(alpha = if (light) .32f else .055f),
                startAngle = 115f, sweepAngle = 160f, useCenter = false,
                topLeft = Offset(size.width * .28f + index * 12f, -size.height * .04f),
                size = Size(size.width * 1.8f, size.height * .68f),
                style = Stroke(width = (1f + index) * density)
            )
        }
    }
}
