package com.ryanshelby.linea.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaMotion
import com.ryanshelby.linea.ui.theme.LocalReduceAnimations

/**
 * Neumorphic Extruded Surface Box (backwards compatible drop-in replacement for FrostedGlassBox).
 * Renders physical Soft UI dual-shadows with directional rim specular reflection.
 */
@Composable
fun FrostedGlassBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(LineaDimensions.PanelCornerRadius),
    borderWidth: Dp = LineaDimensions.HairlineBorder,
    borderColor: Color = LineaColors.GlassBorder,
    fillColor: Color = LineaColors.GlassFill,
    fallbackFillColor: Color = LineaColors.GlassFallbackFill,
    blurRadius: Dp = LineaDimensions.GlassBlur,
    animateMaterialization: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val reduceAnimations = LocalReduceAnimations.current

    var materialized by remember { mutableStateOf(!animateMaterialization || reduceAnimations) }
    LaunchedEffect(Unit) {
        if (animateMaterialization && !reduceAnimations) {
            materialized = true
        }
    }

    val alphaAnimation by animateFloatAsState(
        targetValue = if (materialized) 1f else 0f,
        animationSpec = tween(durationMillis = LineaMotion.PanelMaterializeDuration),
        label = "neu_materialize"
    )

    Box(
        modifier = modifier
            .neumorphic(
                shape = shape,
                elevation = 5.dp
            ),
        content = content
    )
}

/** Neumorphic Extruded Surface Box rendering tactile dual shadows. */
@Composable
fun NeumorphicSurfaceBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(LineaDimensions.PanelCornerRadius),
    borderWidth: Dp = LineaDimensions.HairlineBorder,
    borderColor: Color = LineaColors.NeuBorderHighlight,
    fillColor: Color = LineaColors.NeuSurfaceRaised,
    fallbackFillColor: Color = LineaColors.NeuSurfaceRaised,
    elevation: Dp = 5.dp,
    animateMaterialization: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    FrostedGlassBox(
        modifier = modifier,
        shape = shape,
        borderWidth = borderWidth,
        borderColor = borderColor,
        fillColor = fillColor,
        fallbackFillColor = fallbackFillColor,
        animateMaterialization = animateMaterialization,
        content = content
    )
}

