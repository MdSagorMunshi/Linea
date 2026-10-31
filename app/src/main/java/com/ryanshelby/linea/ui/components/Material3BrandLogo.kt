package com.ryanshelby.linea.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaTypography
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Material 3 Expressive 4-Lobed Scalloped Flower Shape.
 * Iconic geometric shape used in Google Material 3 Expressive motion and branding.
 */
class Material3FlowerShape(private val petalCount: Int = 4) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val maxR = minOf(size.width, size.height) / 2f
            val steps = 72
            for (i in 0..steps) {
                val angle = (i.toFloat() / steps) * 2f * PI.toFloat()
                // Smooth scalloped petal equation
                val r = maxR * (0.84f + 0.16f * cos(petalCount * angle))
                val x = cx + r * cos(angle)
                val y = cy + r * sin(angle)
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * Interactive Material 3 Expressive Logo Badge with playful spring physics,
 * rotating scalloped geometry, sparkle pulse, and dynamic tonal gradient.
 */
@Composable
fun Material3LogoBadge(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    onClick: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val scaleAnim = remember { Animatable(1.0f) }
    val rotationAnim = remember { Animatable(0f) }
    val burstAnim = remember { Animatable(0f) }

    // Ambient breathing pulse for the glow halo
    val infiniteTransition = rememberInfiniteTransition(label = "M3LogoGlow")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "M3LogoPulse"
    )

    val shape = remember { Material3FlowerShape(4) }

    fun triggerBounce() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        coroutineScope.launch {
            launch {
                scaleAnim.animateTo(1.25f, spring(dampingRatio = 0.42f, stiffness = 420f))
                scaleAnim.animateTo(1.0f, spring(dampingRatio = 0.50f, stiffness = 320f))
            }
            launch {
                rotationAnim.animateTo(24f, spring(dampingRatio = 0.40f, stiffness = 400f))
                rotationAnim.animateTo(-14f, spring(dampingRatio = 0.45f, stiffness = 380f))
                rotationAnim.animateTo(6f, spring(dampingRatio = 0.50f, stiffness = 350f))
                rotationAnim.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 300f))
            }
            launch {
                burstAnim.snapTo(0f)
                burstAnim.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
                burstAnim.snapTo(0f)
            }
            onClick?.invoke()
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scaleAnim.value
                scaleY = scaleAnim.value
                rotationZ = rotationAnim.value
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { triggerBounce() },
        contentAlignment = Alignment.Center
    ) {
        // 1. Ambient soft radial aura behind badge
        Box(
            modifier = Modifier
                .size(size * 1.35f)
                .scale(pulseGlow)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            LineaColors.TitaniumBlue.copy(alpha = 0.35f),
                            LineaColors.DesaturatedTeal.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )

        // 2. Burst particles on tap
        if (burstAnim.value > 0f) {
            Canvas(modifier = Modifier.size(size * 2f)) {
                val p = burstAnim.value
                val particleCount = 8
                val maxDist = size.toPx() * 0.9f
                for (i in 0 until particleCount) {
                    val angle = (i.toFloat() / particleCount) * 2f * PI.toFloat()
                    val dist = p * maxDist
                    val px = center.x + dist * cos(angle)
                    val py = center.y + dist * sin(angle)
                    val alpha = (1f - p).coerceIn(0f, 1f)
                    drawCircle(
                        color = LineaColors.TitaniumBlue.copy(alpha = alpha),
                        radius = (3.5f * (1f - p * 0.5f)).dp.toPx(),
                        center = Offset(px, py)
                    )
                }
            }
        }

        // 3. Main Scalloped Starburst Container
        Box(
            modifier = Modifier
                .size(size)
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            LineaColors.TitaniumBlue,
                            LineaColors.DesaturatedTeal
                        )
                    )
                )
                .border(
                    width = 1.2.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.45f),
                            LineaColors.TitaniumBlue.copy(alpha = 0.20f)
                        )
                    ),
                    shape = shape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Material 3 Stylized Dialer Sparkle Icon inside
            Icon(
                imageVector = Icons.Filled.Call,
                contentDescription = "LiNEA Brand Mark",
                tint = Color.White,
                modifier = Modifier
                    .size(size * 0.52f)
                    .graphicsLayer {
                        rotationZ = -5f
                    }
            )
        }
    }
}

/**
 * Top App Header for Material 3 Expressive UI/UX mode.
 * Shows the animated Material 3 logo, dynamic pill chip, and right-side action slot.
 */
@Composable
fun Material3Header(
    modifier: Modifier = Modifier,
    title: String = "LiNEA",
    subtitle: String = "Cellular Dial Pad",
    onLogoClick: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Material3LogoBadge(
                size = 42.dp,
                onClick = onLogoClick
            )

            Column(verticalArrangement = Arrangement.Center) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        style = LineaTypography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.6.sp
                        ),
                        color = LineaColors.TextPrimary
                    )

                    // Expressive Pill Chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(LineaColors.TitaniumBlue.copy(alpha = 0.16f))
                            .border(
                                0.8.dp,
                                LineaColors.TitaniumBlue.copy(alpha = 0.35f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = LineaColors.TitaniumBlue,
                                modifier = Modifier.size(9.dp)
                            )
                            Text(
                                text = "M3 EXPRESSIVE",
                                style = LineaTypography.labelSmall.copy(
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = LineaColors.TitaniumBlue
                            )
                        }
                    }
                }

                Text(
                    text = subtitle,
                    style = LineaTypography.bodySmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = LineaColors.TitaniumBlue
                )
            }
        }

        actions()
    }
}
