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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.telecom.InternationalPreview
import com.ryanshelby.linea.telecom.TimeOfDayState
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
            val isNight = preview.timeOfDay == TimeOfDayState.NIGHT

            val accentColor = when (preview.timeOfDay) {
                TimeOfDayState.EARLY_MORNING -> Color(0xFFFFB74D) // Amber sunrise
                TimeOfDayState.MORNING -> Color(0xFF4FC3F7)       // Sky morning cyan
                TimeOfDayState.MIDDAY -> Color(0xFFFFD54F)        // Sun midday gold
                TimeOfDayState.AFTERNOON -> Color(0xFFFF8A65)     // Afternoon warm coral
                TimeOfDayState.EVENING -> Color(0xFFBA68C8)       // Sunset dusk lavender
                TimeOfDayState.NIGHT -> Color(0xFFFF7043)         // Sleep night caution amber-red
            }

            val borderBrush = Brush.horizontalGradient(
                listOf(
                    accentColor.copy(alpha = if (isNight) 0.55f else 0.40f),
                    LineaColors.GlassBorder
                )
            )

            val bgBrush = Brush.horizontalGradient(
                listOf(
                    accentColor.copy(alpha = if (isNight) 0.16f else 0.10f),
                    Color.Black.copy(alpha = 0.45f)
                )
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(bgBrush)
                    .border(1.dp, borderBrush, RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = preview.flagEmoji,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.width(5.dp))

                    Text(
                        text = preview.countryName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LineaColors.TextPrimary,
                        maxLines = 1,
                        softWrap = false
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "•",
                        fontSize = 11.sp,
                        color = LineaColors.TextTertiary
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Icon(
                        painter = painterResource(id = preview.timeOfDay.iconRes),
                        contentDescription = preview.timeOfDay.displayName,
                        tint = accentColor,
                        modifier = Modifier.size(13.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = "${preview.localTimeFormatted} (${preview.timeZoneShort})",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = LineaColors.TextSecondary,
                        maxLines = 1,
                        softWrap = false
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "•",
                        fontSize = 11.sp,
                        color = LineaColors.TextTertiary
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = preview.timeOfDay.displayName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
