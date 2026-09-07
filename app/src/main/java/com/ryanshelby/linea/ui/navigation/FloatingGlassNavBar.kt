package com.ryanshelby.linea.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.ui.components.FrostedGlassBox
import com.ryanshelby.linea.ui.theme.InterFontFamily
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaMotion
import com.ryanshelby.linea.ui.theme.LocalReduceAnimations

@Composable
fun FloatingGlassNavBar(
    currentDestination: LineaDestination,
    onNavigate: (LineaDestination) -> Unit,
    unreadMissedCalls: Int = 0,
    modifier: Modifier = Modifier
) {
    val reduceAnimations = LocalReduceAnimations.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        FrostedGlassBox(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            borderWidth = LineaDimensions.HairlineBorder,
            borderColor = LineaColors.GlassBorder
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LineaDestination.entries.forEach { destination ->
                    val selected = destination == currentDestination

                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1.15f else 1.0f,
                        animationSpec = LineaMotion.springOrFade(
                            reduceAnimations,
                            LineaMotion.ScaleCompressSpring
                        ),
                        label = "nav_icon_scale"
                    )

                    val tintColor by animateColorAsState(
                        targetValue = if (selected) LineaColors.TitaniumBlue else LineaColors.TextSecondary,
                        animationSpec = tween(durationMillis = if (reduceAnimations) 100 else 200),
                        label = "nav_tint_color"
                    )

                    val interactionSource = remember { MutableInteractionSource() }

                    Column(
                        modifier = Modifier
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                if (!selected) {
                                    onNavigate(destination)
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box {
                            Icon(
                                painter = painterResource(
                                    id = if (selected) destination.filledIcon else destination.outlineIcon
                                ),
                                contentDescription = destination.title,
                                tint = tintColor,
                                modifier = Modifier
                                    .size(24.dp)
                                    .scale(iconScale)
                            )
                            if (destination == LineaDestination.HISTORY && unreadMissedCalls > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(LineaColors.CarmineRed),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (unreadMissedCalls > 9) "9+" else "$unreadMissedCalls",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LineaColors.TextPrimary
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = destination.title,
                            fontFamily = InterFontFamily,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = tintColor
                        )
                    }
                }
            }
        }
    }
}
