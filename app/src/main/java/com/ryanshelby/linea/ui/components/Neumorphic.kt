package com.ryanshelby.linea.ui.components

import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaMotion
import com.ryanshelby.linea.ui.theme.LocalReduceAnimations

/**
 * High-performance Neumorphic Dual Shadow Modifier.
 * Draws opposing directional light highlight (top-left) and ambient shadow (bottom-right).
 */
fun Modifier.neumorphic(
    shape: Shape = RoundedCornerShape(LineaDimensions.CardCornerRadius),
    elevation: Dp = 5.dp,
    isPressed: Boolean = false,
    isSunken: Boolean = false,
    surfaceColor: Color? = null,
    surfaceBrush: Brush? = null,
    lightShadowColor: Color? = null,
    darkShadowColor: Color? = null,
    borderHighlightColor: Color? = null,
    borderShadowColor: Color? = null
): Modifier = this.drawBehind {
    val light = lightShadowColor ?: LineaColors.NeuLightShadow
    val dark = darkShadowColor ?: LineaColors.NeuDarkShadow

    val density = this.density
    val elevationPx = elevation.toPx()
    val blurRadius = (elevationPx * 1.5f).coerceAtLeast(1f)
    val offsetDistance = elevationPx * 0.75f

    val effectivePressed = isPressed || isSunken

    if (!effectivePressed) {
        // Draw Raised Dual Shadows into Native Canvas
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas

            // 1. Bottom-Right Ambient Dark Shadow
            val darkPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.TRANSPARENT
                setShadowLayer(blurRadius, offsetDistance, offsetDistance, dark.toArgb())
            }

            // 2. Top-Left Directional Light Highlight
            val lightPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.TRANSPARENT
                setShadowLayer(blurRadius, -offsetDistance, -offsetDistance, light.toArgb())
            }

            val rect = RectF(0f, 0f, size.width, size.height)
            val cornerRadiusPx = if (shape is CornerBasedShape) {
                shape.topStart.toPx(size, this)
            } else if (shape == CircleShape) {
                size.minDimension / 2f
            } else {
                16f * density
            }

            // Draw dark shadow
            native.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, darkPaint)
            // Draw light highlight
            native.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, lightPaint)
        }
    }
}.then(
    run {
        // Surface Background & Rim-lit Specular Border
        val effectiveModifier = Modifier.clip(shape)
        val bgModifier = when {
            surfaceBrush != null -> effectiveModifier.background(surfaceBrush)
            surfaceColor != null -> {
                if (isPressed || isSunken) {
                    effectiveModifier.background(
                        Brush.linearGradient(
                            colors = listOf(
                                surfaceColor.copy(alpha = 0.85f),
                                surfaceColor
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    )
                } else {
                    effectiveModifier.background(
                        Brush.linearGradient(
                            colors = listOf(
                                surfaceColor,
                                surfaceColor.copy(alpha = 0.88f)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    )
                }
            }
            isPressed || isSunken -> effectiveModifier.background(LineaColors.NeuSunkenGradient)
            else -> effectiveModifier.background(LineaColors.NeuRaisedGradient)
        }

        bgModifier.border(
            width = 1.dp,
            brush = if (isPressed || isSunken) {
                Brush.linearGradient(
                    colors = listOf(
                        (borderShadowColor ?: LineaColors.NeuBorderShadow).copy(alpha = 0.55f),
                        Color.Transparent,
                        (borderHighlightColor ?: LineaColors.NeuBorderHighlight).copy(alpha = 0.45f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(
                        borderHighlightColor ?: LineaColors.NeuBorderHighlight,
                        Color.Transparent,
                        (borderShadowColor ?: LineaColors.NeuBorderShadow).copy(alpha = 0.6f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            },
            shape = shape
        )
    }
)

/**
 * Neumorphic Card container for elevated panels, cards, and list groups.
 */
@Composable
fun NeumorphicCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(LineaDimensions.CardCornerRadius),
    elevation: Dp = 5.dp,
    isSunken: Boolean = false,
    surfaceColor: Color? = null,
    surfaceBrush: Brush? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.neumorphic(
            shape = shape,
            elevation = elevation,
            isSunken = isSunken,
            surfaceColor = surfaceColor,
            surfaceBrush = surfaceBrush
        ),
        content = content
    )
}

/**
 * Neumorphic Sunken Well for search inputs, text entry fields, and slider troughs.
 */
@Composable
fun NeumorphicWell(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(LineaDimensions.PanelCornerRadius),
    depth: Dp = 3.dp,
    surfaceColor: Color? = null,
    surfaceBrush: Brush? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.neumorphic(
            shape = shape,
            elevation = depth,
            isSunken = true,
            surfaceColor = surfaceColor,
            surfaceBrush = surfaceBrush
        ),
        content = content
    )
}

/**
 * Tactile Neumorphic Button that physically depresses from raised to sunken on tap.
 */
@Composable
fun NeumorphicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    elevation: Dp = 5.dp,
    enabled: Boolean = true,
    isActive: Boolean = false,
    activeAccentColor: Color? = null,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reduceAnimations = LocalReduceAnimations.current
    val haptic = LocalHapticFeedback.current

    val effectivePressed = (isPressed && enabled) || isActive

    val scale by animateFloatAsState(
        targetValue = if (effectivePressed && !reduceAnimations) 0.95f else 1.0f,
        animationSpec = LineaMotion.ScaleCompressSpring,
        label = "NeuButtonScale"
    )

    val activeGlowColor = activeAccentColor ?: LineaColors.TitaniumBlue
    val activeBorderColor by animateColorAsState(
        targetValue = if (isActive) activeGlowColor.copy(alpha = 0.8f) else Color.Transparent,
        label = "NeuButtonActiveBorder"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .neumorphic(
                shape = shape,
                elevation = if (effectivePressed) 1.dp else elevation,
                isPressed = effectivePressed
            )
            .then(
                if (isActive) {
                    Modifier.border(1.5.dp, activeBorderColor, shape)
                } else Modifier
            )
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onClick()
                        }
                    )
                } else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/**
 * Tactile Neumorphic Icon Button (e.g. for dialer actions, call buttons, navigation).
 */
@Composable
fun NeumorphicIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    shape: Shape = CircleShape,
    size: Dp = 56.dp,
    iconSize: Dp = 24.dp,
    elevation: Dp = 5.dp,
    isActive: Boolean = false,
    enabled: Boolean = true,
    tint: Color = LineaColors.TextPrimary,
    activeColor: Color = LineaColors.TitaniumBlue
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reduceAnimations = LocalReduceAnimations.current
    val haptic = LocalHapticFeedback.current

    val effectivePressed = (isPressed && enabled) || isActive

    val scale by animateFloatAsState(
        targetValue = if (effectivePressed && !reduceAnimations) 0.92f else 1.0f,
        animationSpec = LineaMotion.ScaleCompressSpring,
        label = "NeuIconScale"
    )

    val currentTint by animateColorAsState(
        targetValue = if (isActive) activeColor else if (!enabled) tint.copy(alpha = 0.35f) else tint,
        label = "NeuIconTint"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .size(size)
            .neumorphic(
                shape = shape,
                elevation = if (effectivePressed) 1.dp else elevation,
                isPressed = effectivePressed
            )
            .then(
                if (isActive) {
                    Modifier.border(1.5.dp, activeColor.copy(alpha = 0.85f), shape)
                } else Modifier
            )
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onClick()
                        }
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = currentTint,
            modifier = Modifier.size(iconSize)
        )
    }
}
