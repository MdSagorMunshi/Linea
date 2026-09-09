package com.ryanshelby.linea.ui.screens.dialpad

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.telecom.InternationalPreview
import com.ryanshelby.linea.ui.theme.LineaColors

@Composable
fun DialpadTimezonePill(
    preview: InternationalPreview?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = preview != null,
        enter = fadeIn(spring(stiffness = Spring.StiffnessMedium)) +
                slideInVertically(spring(stiffness = Spring.StiffnessMedium)) { -it / 2 },
        exit = fadeOut(spring(stiffness = Spring.StiffnessHigh)) +
                slideOutVertically(spring(stiffness = Spring.StiffnessHigh)) { -it / 2 },
        modifier = modifier
    ) {
        if (preview != null) {
            val isLate = preview.isLateNight

            val borderBrush = if (isLate) {
                Brush.horizontalGradient(
                    listOf(
                        LineaColors.Warning.copy(alpha = 0.5f),
                        LineaColors.Warning.copy(alpha = 0.2f)
                    )
                )
            } else {
                Brush.horizontalGradient(
                    listOf(
                        LineaColors.TitaniumBlue.copy(alpha = 0.45f),
                        LineaColors.GlassBorder
                    )
                )
            }

            val bgBrush = if (isLate) {
                Brush.horizontalGradient(
                    listOf(
                        LineaColors.Warning.copy(alpha = 0.14f),
                        Color.Black.copy(alpha = 0.45f)
                    )
                )
            } else {
                Brush.horizontalGradient(
                    listOf(
                        LineaColors.TitaniumBlue.copy(alpha = 0.12f),
                        Color.Black.copy(alpha = 0.45f)
                    )
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(bgBrush)
                    .border(1.dp, borderBrush, RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = preview.flagEmoji,
                        fontSize = 15.sp
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = preview.countryName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LineaColors.TextPrimary
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "•",
                        fontSize = 12.sp,
                        color = LineaColors.TextTertiary
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = if (isLate) Icons.Filled.WarningAmber else Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = if (isLate) LineaColors.Warning else LineaColors.TitaniumBlue,
                        modifier = Modifier.size(13.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = "${preview.localTimeFormatted} (${preview.timeZoneShort})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isLate) LineaColors.Warning else LineaColors.TextSecondary
                    )

                    if (isLate) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "🌙 Night",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = LineaColors.Warning
                        )
                    }
                }
            }
        }
    }
}
