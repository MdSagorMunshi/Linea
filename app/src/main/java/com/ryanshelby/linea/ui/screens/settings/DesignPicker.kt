package com.ryanshelby.linea.ui.screens.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.data.preferences.DesignStyle
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaTypography

@Composable
fun DesignPicker(selected: DesignStyle, onSelect: (DesignStyle) -> Unit) {
    Text("Design", style = LineaTypography.titleMedium, color = LineaColors.TextPrimary)
    Spacer(Modifier.height(4.dp))
    Text("Make LiNEA feel like you.", style = LineaTypography.bodySmall, color = LineaColors.TextSecondary)
    Spacer(Modifier.height(14.dp))
    Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        DesignStyle.entries.forEach { style ->
            val active = selected == style
            Column(
                modifier = Modifier.weight(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(LineaColors.BackgroundBottom.copy(alpha = .55f))
                    .border(if (active) 1.5.dp else 1.dp,
                        if (active) LineaColors.TitaniumBlue else LineaColors.GlassBorder, RoundedCornerShape(22.dp))
                    .selectable(selected = active, role = Role.RadioButton, onClick = { onSelect(style) })
                    .padding(10.dp)
            ) {
                DesignMiniature(style, Modifier.fillMaxWidth().height(132.dp))
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (style == DesignStyle.CLASSIC) "Classic" else "Crystal",
                        style = LineaTypography.titleSmall, color = LineaColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold)
                    Icon(if (active) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = null, tint = LineaColors.TitaniumBlue, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.height(4.dp))
                Text(if (style == DesignStyle.CLASSIC) "Soft & sculpted" else "Glass & titanium",
                    fontSize = 11.sp, color = LineaColors.TextSecondary)
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun DesignMiniature(style: DesignStyle, modifier: Modifier) {
    val light = LineaColors.isLight
    val crystal = style == DesignStyle.CRYSTAL
    val base = if (light) Color(0xFFDDE4E9) else Color(0xFF1C242A)
    Canvas(modifier.clip(RoundedCornerShape(14.dp)).background(base)) {
        val w = size.width
        val h = size.height
        if (crystal) {
            drawRect(Brush.linearGradient(listOf(Color.White.copy(alpha = .25f), Color.Transparent)))
            drawArc(Color.White.copy(alpha = .18f), 110f, 150f, false,
                Offset(w * .4f, -h * .3f), Size(w, h), style = Stroke(2.dp.toPx()))
        }
        drawRoundRect(Color.White.copy(alpha = if (light) .65f else .12f),
            Offset(w * .16f, h * .12f), Size(w * .68f, h * .12f), CornerRadius(5.dp.toPx()))
        val radius = minOf(w * .055f, h * .043f)
        repeat(4) { row -> repeat(3) { col ->
            val center = Offset(w * (.3f + col * .2f), h * (.36f + row * .115f))
            drawCircle(Color.Black.copy(alpha = .14f), radius + 1.dp.toPx(), center + Offset(2f, 3f))
            if (crystal) {
                drawRoundRect(Brush.linearGradient(listOf(Color.White.copy(alpha = .4f), Color.White.copy(alpha = .06f))),
                    center - Offset(radius, radius), Size(radius * 2, radius * 2), CornerRadius(radius * .65f))
                drawRoundRect(Color.White.copy(alpha = .5f), center - Offset(radius, radius),
                    Size(radius * 2, radius * 2), CornerRadius(radius * .65f), style = Stroke(.6.dp.toPx()))
            } else {
                drawCircle(if (light) Color(0xFFE8EDF2) else Color(0xFF2B353D), radius, center)
            }
            drawCircle(if (light) Color(0xFF456070) else Color(0xFFD7E2E8), radius * .15f, center)
        } }
        drawRoundRect(if (crystal) Color(0xFF77919F) else Color(0xFF4A6B88),
            Offset(w * .36f, h * .84f), Size(w * .28f, h * .09f), CornerRadius(8.dp.toPx()))
    }
}
