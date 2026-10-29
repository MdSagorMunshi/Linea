package com.ryanshelby.linea.ui.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.data.preferences.DesignStyle
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaTypography

@Composable
fun DesignPicker(
    selected: DesignStyle,
    onSelect: (DesignStyle) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Design System & UX Themes",
            style = LineaTypography.titleMedium,
            color = LineaColors.TextPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Personalize LiNEA with distinctive design languages, including the original Classic & Crystal alongside Neumorphism, Organic, Fintech Minimal, Material 3 Expressive, Neo-Brutalism, and Handwritten.",
            style = LineaTypography.bodySmall,
            color = LineaColors.TextSecondary
        )
        Spacer(Modifier.height(16.dp))

        // 2-Column Responsive Showcase Grid of the Themes
        val themes = DesignStyle.appThemes
        val rows = themes.chunked(2)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            rows.forEach { rowThemes ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowThemes.forEach { style ->
                        val active = selected == style
                        ThemeSelectionCard(
                            style = style,
                            isActive = active,
                            onSelect = { onSelect(style) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowThemes.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeSelectionCard(
    style: DesignStyle,
    isActive: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeBorderColor by animateColorAsState(
        targetValue = if (isActive) LineaColors.TitaniumBlue else LineaColors.GlassBorder,
        label = "ThemeCardBorder"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(LineaColors.NeuSurfaceRaised)
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = activeBorderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .selectable(
                selected = isActive,
                role = Role.RadioButton,
                onClick = onSelect
            )
            .padding(10.dp)
    ) {
        // High-Fidelity Miniature Preview of the Theme
        DesignMiniature(
            style = style,
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp)
        )

        Spacer(Modifier.height(10.dp))

        // Title and Selection Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = style.displayName,
                style = LineaTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = LineaColors.TextPrimary,
                maxLines = 1
            )
            Icon(
                imageVector = if (isActive) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isActive) LineaColors.TitaniumBlue else LineaColors.TextTertiary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.height(3.dp))

        // Tagline
        Text(
            text = style.tagLine,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (isActive) LineaColors.TitaniumBlue else LineaColors.TextSecondary,
            maxLines = 1
        )

        Spacer(Modifier.height(4.dp))

        // Description
        Text(
            text = style.description,
            fontSize = 10.sp,
            lineHeight = 13.sp,
            color = LineaColors.TextTertiary,
            maxLines = 2
        )
    }
}

@Composable
private fun DesignMiniature(style: DesignStyle, modifier: Modifier) {
    val light = LineaColors.isLight

    when (style) {
        DesignStyle.CLASSIC -> {
            ClassicMiniature(light = light, modifier = modifier)
        }
        DesignStyle.CRYSTAL -> {
            CrystalMiniature(light = light, modifier = modifier)
        }
        DesignStyle.NEUMORPHISM -> {
            NeumorphicMiniature(light = light, modifier = modifier)
        }
        DesignStyle.ORGANIC -> {
            OrganicMiniature(light = light, modifier = modifier)
        }
        DesignStyle.FINTECH -> {
            FintechMiniature(light = light, modifier = modifier)
        }
        DesignStyle.MATERIAL3 -> {
            Material3Miniature(light = light, modifier = modifier)
        }
        DesignStyle.NEO_BRUTALISM -> {
            NeoBrutalismMiniature(light = light, modifier = modifier)
        }
        DesignStyle.HANDWRITTEN -> {
            HandwrittenMiniature(light = light, modifier = modifier)
        }
    }
}

// =============================================================================
// Theme-Specific Miniature Canvas Previews
// =============================================================================

@Composable
private fun ClassicMiniature(light: Boolean, modifier: Modifier) {
    val base = if (light) Color(0xFFDDE4E9) else Color(0xFF1C242A)
    Canvas(modifier.clip(RoundedCornerShape(14.dp)).background(base)) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            Color.White.copy(alpha = if (light) 0.65f else 0.12f),
            Offset(w * 0.16f, h * 0.12f),
            Size(w * 0.68f, h * 0.12f),
            CornerRadius(5.dp.toPx())
        )
        val radius = minOf(w * 0.055f, h * 0.043f)
        repeat(4) { row ->
            repeat(3) { col ->
                val center = Offset(w * (0.30f + col * 0.20f), h * (0.36f + row * 0.115f))
                drawCircle(Color.Black.copy(alpha = 0.14f), radius + 1.dp.toPx(), center + Offset(2f, 3f))
                drawCircle(if (light) Color(0xFFE8EDF2) else Color(0xFF2B353D), radius, center)
                drawCircle(if (light) Color(0xFF456070) else Color(0xFFD7E2E8), radius * 0.15f, center)
            }
        }
        drawRoundRect(
            Color(0xFF4A6B88),
            Offset(w * 0.36f, h * 0.84f),
            Size(w * 0.28f, h * 0.09f),
            CornerRadius(8.dp.toPx())
        )
    }
}

@Composable
private fun NeumorphicMiniature(light: Boolean, modifier: Modifier) {
    val baseBg = if (light) Color(0xFFE6EBF2) else Color(0xFF1E2228)
    val raisedColor = if (light) Color(0xFFE2E7EE) else Color(0xFF21252C)
    val lightShadow = if (light) Color.White.copy(alpha = 0.85f) else Color(0xFF2C333E)
    val darkShadow = if (light) Color(0xFFA3B1C6).copy(alpha = 0.45f) else Color(0xFF0F1216)
    val accent = if (light) Color(0xFF2B5B92) else Color(0xFF5C8DE6)

    Canvas(modifier.clip(RoundedCornerShape(14.dp)).background(baseBg)) {
        val w = size.width
        val h = size.height

        // Number display well (sunken)
        drawRoundRect(
            color = if (light) Color(0xFFD8DFE7) else Color(0xFF181B20),
            topLeft = Offset(w * 0.12f, h * 0.10f),
            size = Size(w * 0.76f, h * 0.14f),
            cornerRadius = CornerRadius(6.dp.toPx())
        )

        // 3x4 Dialpad Keys with dual shadows
        val radius = minOf(w * 0.052f, h * 0.040f)
        repeat(4) { row ->
            repeat(3) { col ->
                val center = Offset(w * (0.30f + col * 0.20f), h * (0.36f + row * 0.11f))
                // Dark shadow
                drawCircle(darkShadow, radius, center + Offset(1.5f, 2.5f))
                // Light highlight
                drawCircle(lightShadow, radius, center - Offset(1.5f, 2f))
                // Key body
                drawCircle(raisedColor, radius, center)
                // Center digit mark
                drawCircle(if (light) Color(0xFF456070) else Color(0xFFD7E2E8), radius * 0.2f, center)
            }
        }

        // Call button
        val btnCenter = Offset(w * 0.50f, h * 0.87f)
        drawCircle(accent, radius * 1.3f, btnCenter)
    }
}

@Composable
private fun OrganicMiniature(light: Boolean, modifier: Modifier) {
    val baseBg = if (light) Color(0xFFF3EFE6) else Color(0xFF16211A)
    val pebbleColor = if (light) Color(0xFFEDE6D7) else Color(0xFF1D2C22)
    val sageGreen = if (light) Color(0xFF2D6A4F) else Color(0xFF5AC68B)
    val terracotta = if (light) Color(0xFFBA5635) else Color(0xFFD47C59)

    Canvas(modifier.clip(RoundedCornerShape(14.dp)).background(baseBg)) {
        val w = size.width
        val h = size.height

        // Asymmetric organic header well
        drawRoundRect(
            color = pebbleColor,
            topLeft = Offset(w * 0.12f, h * 0.09f),
            size = Size(w * 0.76f, h * 0.15f),
            cornerRadius = CornerRadius(10.dp.toPx())
        )

        // Botanical Leaf Vein Ribbon
        val leaf = Path().apply {
            moveTo(w * 0.14f, h * 0.16f)
            quadraticTo(w * 0.50f, h * 0.12f, w * 0.86f, h * 0.16f)
        }
        drawPath(leaf, sageGreen.copy(alpha = 0.5f), style = Stroke(1.5f * density))

        // Asymmetrical Pebble Dialpad Keys
        repeat(4) { row ->
            repeat(3) { col ->
                val center = Offset(w * (0.30f + col * 0.20f), h * (0.36f + row * 0.11f))
                val pw = w * 0.11f
                val ph = h * 0.085f
                drawRoundRect(
                    color = pebbleColor,
                    topLeft = center - Offset(pw / 2f, ph / 2f),
                    size = Size(pw, ph),
                    cornerRadius = CornerRadius(7.dp.toPx(), 4.dp.toPx())
                )
                drawCircle(sageGreen.copy(alpha = 0.7f), 2.5f * density, center)
            }
        }

        // Organic Call Button (Terracotta / Sage Pebble)
        val btnCenter = Offset(w * 0.50f, h * 0.87f)
        drawRoundRect(
            color = terracotta,
            topLeft = btnCenter - Offset(w * 0.14f, h * 0.045f),
            size = Size(w * 0.28f, h * 0.09f),
            cornerRadius = CornerRadius(10.dp.toPx(), 5.dp.toPx())
        )
    }
}

@Composable
private fun FintechMiniature(light: Boolean, modifier: Modifier) {
    val baseBg = if (light) Color(0xFFFFFFFF) else Color(0xFF0F1116)
    val cardBg = if (light) Color(0xFFF8FAFC) else Color(0xFF141720)
    val hairline = if (light) Color(0xFFE2E7F0) else Color(0xFF262C3D)
    val cobalt = if (light) Color(0xFF0052FF) else Color(0xFF2970FF)
    val emerald = if (light) Color(0xFF039855) else Color(0xFF12B76A)

    Canvas(modifier.clip(RoundedCornerShape(14.dp)).background(baseBg)) {
        val w = size.width
        val h = size.height

        // Crisp flat card with razor hairline
        drawRoundRect(
            color = cardBg,
            topLeft = Offset(w * 0.10f, h * 0.08f),
            size = Size(w * 0.80f, h * 0.16f),
            cornerRadius = CornerRadius(3.dp.toPx())
        )
        drawRoundRect(
            color = hairline,
            topLeft = Offset(w * 0.10f, h * 0.08f),
            size = Size(w * 0.80f, h * 0.16f),
            cornerRadius = CornerRadius(3.dp.toPx()),
            style = Stroke(1.dp.toPx())
        )

        // Cobalt data metric line
        drawLine(cobalt, Offset(w * 0.16f, h * 0.16f), Offset(w * 0.45f, h * 0.16f), strokeWidth = 2.dp.toPx())
        drawLine(emerald, Offset(w * 0.58f, h * 0.16f), Offset(w * 0.82f, h * 0.16f), strokeWidth = 2.dp.toPx())

        // 3x4 Geometric Sharp Square Keys
        repeat(4) { row ->
            repeat(3) { col ->
                val center = Offset(w * (0.30f + col * 0.20f), h * (0.36f + row * 0.11f))
                val kw = w * 0.11f
                val kh = h * 0.08f
                drawRoundRect(
                    color = cardBg,
                    topLeft = center - Offset(kw / 2f, kh / 2f),
                    size = Size(kw, kh),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
                drawRoundRect(
                    color = hairline,
                    topLeft = center - Offset(kw / 2f, kh / 2f),
                    size = Size(kw, kh),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                    style = Stroke(1.dp.toPx())
                )
                drawCircle(if (light) Color(0xFF0F172A) else Color.White, 2f * density, center)
            }
        }

        // Razor Call Rectangle
        drawRoundRect(
            color = cobalt,
            topLeft = Offset(w * 0.36f, h * 0.83f),
            size = Size(w * 0.28f, h * 0.09f),
            cornerRadius = CornerRadius(3.dp.toPx())
        )
    }
}

@Composable
private fun Material3Miniature(light: Boolean, modifier: Modifier) {
    val baseBg = if (light) Color(0xFFFAF6FD) else Color(0xFF1A1528)
    val containerBg = if (light) Color(0xFFEFE6F6) else Color(0xFF251F36)
    val violet = if (light) Color(0xFF6750A4) else Color(0xFFD0BCFF)
    val coral = if (light) Color(0xFFB3261E) else Color(0xFFFFB4AB)

    Canvas(modifier.clip(RoundedCornerShape(14.dp)).background(baseBg)) {
        val w = size.width
        val h = size.height

        // Expressive Tonal Container
        drawRoundRect(
            color = containerBg,
            topLeft = Offset(w * 0.10f, h * 0.08f),
            size = Size(w * 0.80f, h * 0.16f),
            cornerRadius = CornerRadius(12.dp.toPx())
        )

        // Pill badge inside header
        drawRoundRect(
            color = violet.copy(alpha = 0.25f),
            topLeft = Offset(w * 0.18f, h * 0.13f),
            size = Size(w * 0.28f, h * 0.06f),
            cornerRadius = CornerRadius(6.dp.toPx())
        )

        // 3x4 Expressive Squircle Keys
        repeat(4) { row ->
            repeat(3) { col ->
                val center = Offset(w * (0.30f + col * 0.20f), h * (0.36f + row * 0.11f))
                val kw = w * 0.12f
                val kh = h * 0.085f
                drawRoundRect(
                    color = containerBg,
                    topLeft = center - Offset(kw / 2f, kh / 2f),
                    size = Size(kw, kh),
                    cornerRadius = CornerRadius(8.dp.toPx())
                )
                drawCircle(violet, 2.5f * density, center)
            }
        }

        // Floating Pill Call Action
        drawRoundRect(
            color = violet,
            topLeft = Offset(w * 0.34f, h * 0.83f),
            size = Size(w * 0.32f, h * 0.095f),
            cornerRadius = CornerRadius(14.dp.toPx())
        )
    }
}

@Composable
private fun NeoBrutalismMiniature(light: Boolean, modifier: Modifier) {
    val baseBg = if (light) Color(0xFFFFFDF5) else Color(0xFF18181E)
    val surfaceColor = if (light) Color.White else Color(0xFF22222C)
    val yellow = Color(0xFFFFE600)
    val pink = Color(0xFFFF3377)
    val strokeColor = Color.Black

    Canvas(modifier.clip(RoundedCornerShape(14.dp)).background(baseBg)) {
        val w = size.width
        val h = size.height
        val shadowOffset = 3f * density

        // Header Card with Solid Black Block Shadow
        val cardLeft = w * 0.10f
        val cardTop = h * 0.08f
        val cardW = w * 0.80f
        val cardH = h * 0.16f

        // Shadow block
        drawRoundRect(
            color = Color.Black,
            topLeft = Offset(cardLeft + shadowOffset, cardTop + shadowOffset),
            size = Size(cardW, cardH),
            cornerRadius = CornerRadius(4.dp.toPx())
        )
        // Card body
        drawRoundRect(
            color = yellow,
            topLeft = Offset(cardLeft, cardTop),
            size = Size(cardW, cardH),
            cornerRadius = CornerRadius(4.dp.toPx())
        )
        // 2.5dp black border
        drawRoundRect(
            color = strokeColor,
            topLeft = Offset(cardLeft, cardTop),
            size = Size(cardW, cardH),
            cornerRadius = CornerRadius(4.dp.toPx()),
            style = Stroke(2.2f * density)
        )

        // 3x4 Chunky Square Keys with hard block drop shadows
        repeat(4) { row ->
            repeat(3) { col ->
                val center = Offset(w * (0.30f + col * 0.20f), h * (0.36f + row * 0.11f))
                val kw = w * 0.11f
                val kh = h * 0.08f
                val kOffset = 2f * density

                // Hard shadow
                drawRoundRect(
                    color = Color.Black,
                    topLeft = center - Offset(kw / 2f - kOffset, kh / 2f - kOffset),
                    size = Size(kw, kh),
                    cornerRadius = CornerRadius(3.dp.toPx())
                )
                // Surface
                drawRoundRect(
                    color = surfaceColor,
                    topLeft = center - Offset(kw / 2f, kh / 2f),
                    size = Size(kw, kh),
                    cornerRadius = CornerRadius(3.dp.toPx())
                )
                // Border
                drawRoundRect(
                    color = strokeColor,
                    topLeft = center - Offset(kw / 2f, kh / 2f),
                    size = Size(kw, kh),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                    style = Stroke(1.8f * density)
                )
            }
        }

        // Chunky Neo-Brutalist Call Button (Hot Pink with Hard Black Shadow)
        val btnLeft = w * 0.35f
        val btnTop = h * 0.83f
        val btnW = w * 0.30f
        val btnH = h * 0.09f
        drawRoundRect(
            color = Color.Black,
            topLeft = Offset(btnLeft + shadowOffset, btnTop + shadowOffset),
            size = Size(btnW, btnH),
            cornerRadius = CornerRadius(4.dp.toPx())
        )
        drawRoundRect(
            color = pink,
            topLeft = Offset(btnLeft, btnTop),
            size = Size(btnW, btnH),
            cornerRadius = CornerRadius(4.dp.toPx())
        )
        drawRoundRect(
            color = strokeColor,
            topLeft = Offset(btnLeft, btnTop),
            size = Size(btnW, btnH),
            cornerRadius = CornerRadius(4.dp.toPx()),
            style = Stroke(2.2f * density)
        )
    }
}

@Composable
private fun HandwrittenMiniature(light: Boolean, modifier: Modifier) {
    val baseBg = if (light) Color(0xFFFAF7EE) else Color(0xFF1E1B19)
    val paperBg = if (light) Color(0xFFFFFDF5) else Color(0xFF25211E)
    val inkColor = if (light) Color(0xFF382E26) else Color(0xFFDCD1C4)
    val indigoWash = if (light) Color(0xFF32577A) else Color(0xFF7CB8DC)

    Canvas(modifier.clip(RoundedCornerShape(14.dp)).background(baseBg)) {
        val w = size.width
        val h = size.height

        // Hand-drawn sketchy note card
        val cardLeft = w * 0.10f
        val cardTop = h * 0.08f
        val cardW = w * 0.80f
        val cardH = h * 0.16f

        drawRoundRect(
            color = paperBg,
            topLeft = Offset(cardLeft, cardTop),
            size = Size(cardW, cardH),
            cornerRadius = CornerRadius(8.dp.toPx(), 5.dp.toPx())
        )
        drawRoundRect(
            color = inkColor.copy(alpha = 0.5f),
            topLeft = Offset(cardLeft, cardTop),
            size = Size(cardW, cardH),
            cornerRadius = CornerRadius(8.dp.toPx(), 5.dp.toPx()),
            style = Stroke(1.2f * density)
        )

        // Hand-drawn scribble underline
        val underline = Path().apply {
            moveTo(w * 0.18f, h * 0.17f)
            cubicTo(w * 0.35f, h * 0.18f, w * 0.55f, h * 0.16f, w * 0.75f, h * 0.175f)
        }
        drawPath(underline, indigoWash.copy(alpha = 0.8f), style = Stroke(1.5f * density))

        // 3x4 Wobbly Hand-Drawn Circular Rings
        repeat(4) { row ->
            repeat(3) { col ->
                val center = Offset(w * (0.30f + col * 0.20f), h * (0.36f + row * 0.11f))
                val radius = minOf(w * 0.052f, h * 0.040f)

                drawCircle(paperBg, radius, center)
                drawCircle(inkColor.copy(alpha = 0.55f), radius, center, style = Stroke(1.2f * density))
                drawCircle(indigoWash, 2f * density, center)
            }
        }

        // Hand-Drawn Call Circle
        val btnCenter = Offset(w * 0.50f, h * 0.87f)
        drawCircle(indigoWash, w * 0.075f, btnCenter)
        drawCircle(inkColor.copy(alpha = 0.7f), w * 0.075f, btnCenter, style = Stroke(1.4f * density))
    }
}

@Composable
private fun CrystalMiniature(light: Boolean, modifier: Modifier) {
    val base = if (light) Color(0xFFDDE4E9) else Color(0xFF1C242A)
    Canvas(modifier.clip(RoundedCornerShape(14.dp)).background(base)) {
        val w = size.width
        val h = size.height

        drawRect(Brush.linearGradient(listOf(Color.White.copy(alpha = .25f), Color.Transparent)))
        drawArc(
            Color.White.copy(alpha = .18f), 110f, 150f, false,
            Offset(w * .4f, -h * .3f), Size(w, h), style = Stroke(2.dp.toPx())
        )
        drawRoundRect(
            Color.White.copy(alpha = if (light) .65f else .12f),
            Offset(w * .16f, h * .12f), Size(w * .68f, h * .12f), CornerRadius(5.dp.toPx())
        )
        val radius = minOf(w * .055f, h * .043f)
        repeat(4) { row ->
            repeat(3) { col ->
                val center = Offset(w * (.3f + col * .2f), h * (.36f + row * .115f))
                drawCircle(Color.Black.copy(alpha = .14f), radius + 1.dp.toPx(), center + Offset(2f, 3f))
                drawRoundRect(
                    Brush.linearGradient(listOf(Color.White.copy(alpha = .4f), Color.White.copy(alpha = .06f))),
                    center - Offset(radius, radius), Size(radius * 2, radius * 2), CornerRadius(radius * .65f)
                )
                drawRoundRect(
                    Color.White.copy(alpha = .5f), center - Offset(radius, radius),
                    Size(radius * 2, radius * 2), CornerRadius(radius * .65f), style = Stroke(.6.dp.toPx())
                )
                drawCircle(if (light) Color(0xFF456070) else Color(0xFFD7E2E8), radius * .15f, center)
            }
        }
        drawRoundRect(
            Color(0xFF77919F),
            Offset(w * .36f, h * .84f), Size(w * .28f, h * .09f), CornerRadius(8.dp.toPx())
        )
    }
}
