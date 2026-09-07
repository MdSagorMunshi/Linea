package com.ryanshelby.linea.ui.components

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaMotion
import com.ryanshelby.linea.ui.theme.LocalReduceAnimations

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
        label = "glass_materialize"
    )

    val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (supportsBlur) {
                    Modifier
                        // Base background tint + glass layer
                        .background(fallbackFillColor.copy(alpha = 0.85f * alphaAnimation))
                        .background(fillColor.copy(alpha = fillColor.alpha * alphaAnimation))
                } else {
                    Modifier.background(fallbackFillColor.copy(alpha = alphaAnimation))
                }
            )
            .border(
                width = borderWidth,
                color = borderColor.copy(alpha = borderColor.alpha * alphaAnimation),
                shape = shape
            ),
        content = content
    )
}
