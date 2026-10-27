package com.ryanshelby.linea.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import android.os.Build
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LocalReduceAnimations

/** Live refracted glass over a separately recorded GPU backdrop, with readable material tints. */
fun Modifier.crystalSurface(
    shape: Shape,
    elevation: Dp = 6.dp,
    pressed: Boolean = false,
    tint: Color? = null,
    opaque: Boolean = false
): Modifier = composed {
    val light = LineaColors.isLight
    val source = LocalCrystalBackdrop.current
    val view = LocalView.current
    val fallback = crystalOpticsMode(Build.VERSION.SDK_INT, view.isHardwareAccelerated,
        source != null && source.owner === view) == CrystalOpticsMode.OPAQUE
    val reduceMotion = LocalReduceAnimations.current
    val surface by animateColorAsState(
        targetValue = tint ?: if (pressed) LineaColors.NeuSurfaceSunken else LineaColors.GlassFill,
        animationSpec = tween(if (reduceMotion) 0 else 220), label = "crystal_surface"
    )
    val rim = Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = if (light) .85f else .38f),
            Color.White.copy(alpha = if (light) .2f else .04f),
            Color.White.copy(alpha = if (light) .7f else .2f)
        )
    )
    this
        .drawWithCache {
            val depth = if (pressed) 1.dp.toPx() else elevation.toPx()
            val radius = (shape as? CornerBasedShape)?.topStart?.toPx(size, this)
                ?: size.minDimension / 2f
            val rect = RectF(0f, 0f, size.width, size.height)
            val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.TRANSPARENT
                setShadowLayer(depth * 1.6f, 0f, depth * .7f,
                    Color.Black.copy(alpha = if (light) .18f else .4f).toArgb())
            }
            onDrawBehind {
                drawIntoCanvas { it.nativeCanvas.drawRoundRect(rect, radius, radius, shadow) }
            }
        }
        .clip(shape)
        .then(if (!opaque && tint == null) Modifier.crystalOptics(shape, pressed) else Modifier)
        .background(Brush.verticalGradient(listOf(
            surface.copy(alpha = if (opaque || tint != null || fallback) 1f else if (light) .78f else .72f),
            surface.copy(alpha = if (opaque || tint != null || fallback) 1f else if (light) .66f else .58f)
        )))
        .background(Brush.linearGradient(listOf(
            Color.White.copy(alpha = if (light) .42f else .10f),
            Color.Transparent,
            Color.Black.copy(alpha = if (light) .035f else .10f)
        )))
        .border(1.dp, rim, shape)
}
