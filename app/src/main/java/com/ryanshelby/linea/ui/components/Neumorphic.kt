package com.ryanshelby.linea.ui.components

import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.composed
import com.ryanshelby.linea.ui.theme.LocalDesignStyle
import com.ryanshelby.linea.data.preferences.DesignStyle
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LocalReduceAnimations
import com.ryanshelby.linea.ui.theme.LocalThemeMotion

/**
 * High-performance Theme-Aware Surface Modifier.
 * Dynamically switches between the 6 distinct UI/UX design systems:
 * 1. Neumorphism: Dual-shadow tactile extruded & sunken surfaces.
 * 2. Organic / Biophilic: Ambient diffuse natural lighting, botanical curves, and earthy tones.
 * 3. Fintech Minimal: Razor-sharp 1dp hairline precision, flat architectural planes.
 * 4. Material 3 Expressive: Dynamic tonal containers, pill geometry, expressive outline.
 * 5. Neo-Brutalism: Chunky 2.5dp black borders and solid non-blurred block drop shadows.
 * 6. Handwritten Art: Artisanal sketchbook parchment with hand-drawn pencil/ink borders.
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
): Modifier = composed {
    when (LocalDesignStyle.current) {
        DesignStyle.CRYSTAL -> {
            val neutral = surfaceColor == LineaColors.NeuSurfaceRaised || surfaceColor == LineaColors.NeuSurfaceSunken || surfaceColor == LineaColors.GlassFill
            crystalSurface(shape, elevation, isPressed || isSunken, if (neutral) null else surfaceColor)
        }
        DesignStyle.ORGANIC -> {
            organicSurface(shape, elevation, isPressed, isSunken, surfaceColor, surfaceBrush,
                borderHighlightColor)
        }
        DesignStyle.FINTECH -> {
            fintechSurface(shape, elevation, isPressed, isSunken, surfaceColor, surfaceBrush,
                borderHighlightColor)
        }
        DesignStyle.MATERIAL3 -> {
            material3Surface(shape, elevation, isPressed, isSunken, surfaceColor, surfaceBrush,
                borderHighlightColor)
        }
        DesignStyle.NEO_BRUTALISM -> {
            neoBrutalismSurface(shape, elevation, isPressed, isSunken, surfaceColor, surfaceBrush,
                borderHighlightColor)
        }
        DesignStyle.HANDWRITTEN -> {
            handwrittenSurface(shape, elevation, isPressed, isSunken, surfaceColor, surfaceBrush,
                borderHighlightColor)
        }
        DesignStyle.NEUMORPHISM, DesignStyle.CLASSIC -> {
            classicNeumorphic(shape, elevation, isPressed, isSunken, surfaceColor, surfaceBrush,
                lightShadowColor, darkShadowColor, borderHighlightColor, borderShadowColor)
        }
    }
}

// =============================================================================
// 1. NEUMORPHISM SURFACE
// =============================================================================

private fun Modifier.classicNeumorphic(
    shape: Shape,
    elevation: Dp,
    isPressed: Boolean,
    isSunken: Boolean,
    surfaceColor: Color?,
    surfaceBrush: Brush?,
    lightShadowColor: Color?,
    darkShadowColor: Color?,
    borderHighlightColor: Color?,
    borderShadowColor: Color?
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

// =============================================================================
// 2. ORGANIC / BIOPHILIC SURFACE
// =============================================================================

private fun Modifier.organicSurface(
    shape: Shape,
    elevation: Dp,
    isPressed: Boolean,
    isSunken: Boolean,
    surfaceColor: Color?,
    surfaceBrush: Brush?,
    borderHighlightColor: Color?
): Modifier = this.drawBehind {
    val effectivePressed = isPressed || isSunken
    if (!effectivePressed && elevation > 0.dp) {
        val elevationPx = elevation.toPx()
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            val shadowColor = LineaColors.NeuDarkShadow.copy(alpha = if (LineaColors.isLight) 0.18f else 0.45f)
            val shadowPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.TRANSPARENT
                setShadowLayer(elevationPx * 1.6f, 0f, elevationPx * 0.7f, shadowColor.toArgb())
            }
            val rect = RectF(0f, 0f, size.width, size.height)
            val cornerRadiusPx = if (shape is CornerBasedShape) {
                shape.topStart.toPx(size, this)
            } else if (shape == CircleShape) {
                size.minDimension / 2f
            } else {
                20f * density
            }
            native.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, shadowPaint)
        }
    }
}.then(
    run {
        val effectiveModifier = Modifier.clip(shape)
        val baseColor = surfaceColor ?: (if (isPressed || isSunken) LineaColors.NeuSurfaceSunken else LineaColors.NeuSurfaceRaised)
        val bgModifier = when {
            surfaceBrush != null -> effectiveModifier.background(surfaceBrush)
            else -> effectiveModifier.background(
                Brush.verticalGradient(
                    colors = listOf(
                        baseColor,
                        baseColor.copy(alpha = 0.92f)
                    )
                )
            )
        }
        val borderColor = borderHighlightColor ?: LineaColors.GlassBorder
        bgModifier.border(1.dp, borderColor, shape)
    }
)

// =============================================================================
// 3. FINTECH MINIMAL SURFACE
// =============================================================================

private fun Modifier.fintechSurface(
    shape: Shape,
    elevation: Dp,
    isPressed: Boolean,
    isSunken: Boolean,
    surfaceColor: Color?,
    surfaceBrush: Brush?,
    borderHighlightColor: Color?
): Modifier = this.drawBehind {
    val effectivePressed = isPressed || isSunken
    if (!effectivePressed && elevation >= 4.dp) {
        // Crisp 1.5dp micro-shadow
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            val shadowPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.TRANSPARENT
                setShadowLayer(2f * density, 0f, 1.5f * density, Color.Black.copy(alpha = if (LineaColors.isLight) 0.12f else 0.50f).toArgb())
            }
            val rect = RectF(0f, 0f, size.width, size.height)
            val cornerRadiusPx = if (shape is CornerBasedShape) {
                shape.topStart.toPx(size, this)
            } else if (shape == CircleShape) {
                size.minDimension / 2f
            } else 4f * density
            native.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, shadowPaint)
        }
    }
}.then(
    run {
        val effectiveModifier = Modifier.clip(shape)
        val baseColor = surfaceColor ?: (if (isPressed || isSunken) LineaColors.NeuSurfaceSunken else LineaColors.NeuSurfaceRaised)
        val bgModifier = when {
            surfaceBrush != null -> effectiveModifier.background(surfaceBrush)
            else -> effectiveModifier.background(baseColor)
        }
        val borderColor = borderHighlightColor ?: (if (isPressed || isSunken) LineaColors.GlassBorderFocused else LineaColors.GlassBorder)
        bgModifier.border(1.dp, borderColor, shape)
    }
)

// =============================================================================
// 4. MATERIAL 3 EXPRESSIVE SURFACE
// =============================================================================

private fun Modifier.material3Surface(
    shape: Shape,
    elevation: Dp,
    isPressed: Boolean,
    isSunken: Boolean,
    surfaceColor: Color?,
    surfaceBrush: Brush?,
    borderHighlightColor: Color?
): Modifier = this.drawBehind {
    val effectivePressed = isPressed || isSunken
    if (!effectivePressed && elevation > 0.dp) {
        val elevationPx = elevation.toPx()
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            val shadowPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.TRANSPARENT
                setShadowLayer(elevationPx * 1.5f, 0f, elevationPx * 0.7f, Color.Black.copy(alpha = if (LineaColors.isLight) 0.10f else 0.40f).toArgb())
            }
            val rect = RectF(0f, 0f, size.width, size.height)
            val cornerRadiusPx = if (shape is CornerBasedShape) {
                shape.topStart.toPx(size, this)
            } else if (shape == CircleShape) {
                size.minDimension / 2f
            } else 20f * density
            native.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, shadowPaint)
        }
    }
}.then(
    run {
        val effectiveModifier = Modifier.clip(shape)
        val baseColor = surfaceColor ?: (if (isPressed || isSunken) LineaColors.NeuSurfaceSunken else LineaColors.NeuSurfaceRaised)
        val bgModifier = when {
            surfaceBrush != null -> effectiveModifier.background(surfaceBrush)
            else -> effectiveModifier.background(baseColor)
        }
        val borderColor = borderHighlightColor ?: LineaColors.GlassBorder
        bgModifier.border(1.dp, borderColor, shape)
    }
)

// =============================================================================
// 5. NEO-BRUTALISM SURFACE (Solid Block Shadow & Chunky 2.5dp Border)
// =============================================================================

private fun Modifier.neoBrutalismSurface(
    shape: Shape,
    elevation: Dp,
    isPressed: Boolean,
    isSunken: Boolean,
    surfaceColor: Color?,
    surfaceBrush: Brush?,
    borderHighlightColor: Color?
): Modifier = this.drawBehind {
    val effectivePressed = isPressed || isSunken
    if (elevation > 0.dp) {
        val shadowOffset = if (effectivePressed) 1f * density else 3.5f * density
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            val blockPaint = Paint().apply {
                isAntiAlias = true
                color = Color.Black.toArgb()
                style = Paint.Style.FILL
            }
            val cornerRadiusPx = if (shape is CornerBasedShape) {
                shape.topStart.toPx(size, this)
            } else if (shape == CircleShape) {
                size.minDimension / 2f
            } else 8f * density
            val shadowRect = RectF(shadowOffset, shadowOffset, size.width + shadowOffset, size.height + shadowOffset)
            native.drawRoundRect(shadowRect, cornerRadiusPx, cornerRadiusPx, blockPaint)
        }
    }
}.then(
    run {
        val effectiveModifier = Modifier.clip(shape)
        val baseColor = surfaceColor ?: (if (isPressed || isSunken) LineaColors.NeuSurfaceSunken else LineaColors.NeuSurfaceRaised)
        val bgModifier = when {
            surfaceBrush != null -> effectiveModifier.background(surfaceBrush)
            else -> effectiveModifier.background(baseColor)
        }
        val strokeColor = borderHighlightColor ?: (if (LineaColors.isLight) Color.Black else if (baseColor == Color.Black) Color.White else Color.Black)
        bgModifier.border(2.5.dp, strokeColor, shape)
    }
)

// =============================================================================
// 6. HANDWRITTEN ART SURFACE (Sketchbook Parchment & Hand-Drawn Stroke)
// =============================================================================

private fun Modifier.handwrittenSurface(
    shape: Shape,
    elevation: Dp,
    isPressed: Boolean,
    isSunken: Boolean,
    surfaceColor: Color?,
    surfaceBrush: Brush?,
    borderHighlightColor: Color?
): Modifier = this.drawBehind {
    val effectivePressed = isPressed || isSunken
    if (!effectivePressed && elevation > 0.dp) {
        val elevationPx = elevation.toPx()
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            val shadowPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.TRANSPARENT
                setShadowLayer(elevationPx * 1.3f, 1f * density, elevationPx * 0.7f, Color(0xFF2C241D).copy(alpha = if (LineaColors.isLight) 0.16f else 0.42f).toArgb())
            }
            val rect = RectF(0f, 0f, size.width, size.height)
            val cornerRadiusPx = if (shape is CornerBasedShape) {
                shape.topStart.toPx(size, this)
            } else if (shape == CircleShape) {
                size.minDimension / 2f
            } else 16f * density
            native.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, shadowPaint)
        }
    }
}.then(
    run {
        val effectiveModifier = Modifier.clip(shape)
        val baseColor = surfaceColor ?: (if (isPressed || isSunken) LineaColors.NeuSurfaceSunken else LineaColors.NeuSurfaceRaised)
        val bgModifier = when {
            surfaceBrush != null -> effectiveModifier.background(surfaceBrush)
            else -> effectiveModifier.background(
                Brush.verticalGradient(
                    listOf(
                        baseColor,
                        baseColor.copy(alpha = 0.94f)
                    )
                )
            )
        }
        val inkBorder = borderHighlightColor ?: LineaColors.GlassBorder
        bgModifier.border(1.5.dp, inkBorder, shape)
    }
)

// =============================================================================
// Reusable Themed Containers
// =============================================================================

/**
 * Reusable Card container for elevated panels, cards, and list groups.
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
 * Reusable Sunken Well for search inputs, text entry fields, and slider troughs.
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
 * Tactile Button with theme-specific physical motion (squish, snap, tilt, block translate).
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
    val style = LocalDesignStyle.current
    val motion = LocalThemeMotion.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reduceAnimations = LocalReduceAnimations.current
    val haptic = LocalHapticFeedback.current

    val effectivePressed = (isPressed && enabled) || isActive

    val scale by animateFloatAsState(
        targetValue = if (effectivePressed) motion.pressScale else 1.0f,
        animationSpec = motion.pressAnimationSpec,
        label = "ThemeButtonScale"
    )

    val offsetX by animateDpAsState(
        targetValue = if (effectivePressed) motion.pressOffsetX else 0.dp,
        animationSpec = motion.pressOffsetAnimationSpec,
        label = "ThemeButtonOffsetX"
    )
    val offsetY by animateDpAsState(
        targetValue = if (effectivePressed) motion.pressOffsetY else 0.dp,
        animationSpec = motion.pressOffsetAnimationSpec,
        label = "ThemeButtonOffsetY"
    )
    val rotationZ by animateFloatAsState(
        targetValue = if (effectivePressed) motion.pressRotationZ else 0f,
        animationSpec = motion.pressAnimationSpec,
        label = "ThemeButtonRotationZ"
    )

    val activeGlowColor = activeAccentColor ?: LineaColors.TitaniumBlue
    val activeBorderColor by animateColorAsState(
        targetValue = if (isActive) activeGlowColor.copy(alpha = 0.85f) else Color.Transparent,
        label = "ThemeButtonActiveBorder"
    )

    Box(
        modifier = modifier
            .offset(x = offsetX, y = offsetY)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.rotationZ = rotationZ
            }
            .neumorphic(
                shape = shape,
                elevation = if (effectivePressed) 1.dp else elevation,
                isPressed = effectivePressed
            )
            .then(
                if (isActive) {
                    Modifier.border(
                        if (style == DesignStyle.NEO_BRUTALISM) 2.5.dp else 1.5.dp,
                        activeBorderColor,
                        shape
                    )
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
 * Tactile Icon Button with theme-specific motion (dialer actions, call buttons, navigation).
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
    val style = LocalDesignStyle.current
    val motion = LocalThemeMotion.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reduceAnimations = LocalReduceAnimations.current
    val haptic = LocalHapticFeedback.current

    val effectivePressed = (isPressed && enabled) || isActive

    val scale by animateFloatAsState(
        targetValue = if (effectivePressed) motion.pressScale else 1.0f,
        animationSpec = motion.pressAnimationSpec,
        label = "ThemeIconScale"
    )

    val offsetX by animateDpAsState(
        targetValue = if (effectivePressed) motion.pressOffsetX else 0.dp,
        animationSpec = motion.pressOffsetAnimationSpec,
        label = "ThemeIconOffsetX"
    )
    val offsetY by animateDpAsState(
        targetValue = if (effectivePressed) motion.pressOffsetY else 0.dp,
        animationSpec = motion.pressOffsetAnimationSpec,
        label = "ThemeIconOffsetY"
    )
    val rotationZ by animateFloatAsState(
        targetValue = if (effectivePressed) motion.pressRotationZ else 0f,
        animationSpec = motion.pressAnimationSpec,
        label = "ThemeIconRotationZ"
    )

    val currentTint by animateColorAsState(
        targetValue = if (isActive) activeColor else if (!enabled) tint.copy(alpha = 0.35f) else tint,
        label = "ThemeIconTint"
    )

    Box(
        modifier = modifier
            .offset(x = offsetX, y = offsetY)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.rotationZ = rotationZ
            }
            .size(size)
            .neumorphic(
                shape = shape,
                elevation = if (effectivePressed) 1.dp else elevation,
                isPressed = effectivePressed
            )
            .then(
                if (isActive) {
                    Modifier.border(
                        if (style == DesignStyle.NEO_BRUTALISM) 2.5.dp else 1.5.dp,
                        activeColor.copy(alpha = 0.85f),
                        shape
                    )
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
