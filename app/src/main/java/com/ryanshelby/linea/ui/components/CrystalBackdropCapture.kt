package com.ryanshelby.linea.ui.components

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.shape.CornerBasedShape
import kotlin.math.roundToInt

/** A GPU display list. Sources and consumers are separate, so glass cannot capture itself. */
class CrystalBackdropSource internal constructor(val layer: GraphicsLayer, val owner: View) {
    var position = Offset.Zero
    var ready = false
}

val LocalCrystalBackdrop = staticCompositionLocalOf<CrystalBackdropSource?> { null }

@Composable
fun rememberCrystalBackdropSource(): CrystalBackdropSource {
    val layer = rememberGraphicsLayer()
    val view = LocalView.current
    return remember(layer, view) { CrystalBackdropSource(layer, view) }
}

fun Modifier.captureCrystalBackdrop(source: CrystalBackdropSource): Modifier =
    onGloballyPositioned { source.position = it.positionInRoot() }
        .drawWithContent {
            source.layer.record { this@drawWithContent.drawContent() }
            source.ready = true
            drawLayer(source.layer)
        }

/** Samples only the backdrop. Foreground text/icons are drawn afterward and stay sharp. */
fun Modifier.crystalOptics(shape: Shape, pressed: Boolean, blurDp: Float = 10f): Modifier = composed {
    val source = LocalCrystalBackdrop.current
    val view = LocalView.current
    val mode = crystalOpticsMode(Build.VERSION.SDK_INT, view.isHardwareAccelerated, source != null && source.owner === view)
    if (source == null || mode == CrystalOpticsMode.OPAQUE || Build.VERSION.SDK_INT < 31) {
        this
    } else {
        val effectLayer = rememberGraphicsLayer()
        val optics = remember { if (Build.VERSION.SDK_INT >= 33) CrystalRefraction() else null }
        val placement = remember { CrystalPlacement() }
        this.onGloballyPositioned { placement.position = it.positionInRoot() }
            .drawWithContent {
                if (source.ready && drawContext.canvas.nativeCanvas.isHardwareAccelerated && size.minDimension > 0f) {
                    val radius = ((shape as? CornerBasedShape)?.topStart?.toPx(size, this)
                        ?: size.minDimension / 2f).coerceAtMost(size.minDimension / 2f)
                    val blur = blurDp * density
                    effectLayer.renderEffect = if (Build.VERSION.SDK_INT >= 33 && optics != null) {
                        optics.effect(size.width, size.height, radius, blur, if (pressed) 3f * density else 6f * density)
                    } else {
                        RenderEffect.createBlurEffect(blur, blur, Shader.TileMode.CLAMP).asComposeRenderEffect()
                    }
                    val displacement = placement.position - source.position
                    effectLayer.record(size = IntSize(size.width.roundToInt(), size.height.roundToInt())) {
                        translate(-displacement.x, -displacement.y) { drawLayer(source.layer) }
                    }
                    drawLayer(effectLayer)
                }
                drawContent()
            }
    }
}

private class CrystalPlacement { var position = Offset.Zero }

@RequiresApi(33)
private class CrystalRefraction {
    private val shader = RuntimeShader(SOURCE)
    private var lastWidth = -1f
    private var lastHeight = -1f
    private var lastRadius = -1f
    private var lastBlur = -1f
    private var lastPower = -1f
    private var cached: androidx.compose.ui.graphics.RenderEffect? = null

    fun effect(width: Float, height: Float, radius: Float, blur: Float, power: Float): androidx.compose.ui.graphics.RenderEffect {
        if (width != lastWidth || height != lastHeight || radius != lastRadius || blur != lastBlur || power != lastPower) {
            shader.setFloatUniform("resolution", width, height)
            shader.setFloatUniform("corner", radius)
            shader.setFloatUniform("power", power)
            shader.setFloatUniform("rimWidth", 14f.coerceAtMost(width * .15f))
            val lens = RenderEffect.createRuntimeShaderEffect(shader, "scene")
            val diffusion = RenderEffect.createBlurEffect(blur, blur, Shader.TileMode.CLAMP)
            cached = RenderEffect.createChainEffect(lens, diffusion).asComposeRenderEffect()
            lastWidth = width; lastHeight = height; lastRadius = radius; lastBlur = blur; lastPower = power
        }
        return checkNotNull(cached)
    }

    companion object {
        // Monochromatic optics: no colored dispersion or rainbow edge artifacts.
        private const val SOURCE = """
            uniform shader scene;
            uniform float2 resolution;
            uniform float corner;
            uniform float power;
            uniform float rimWidth;
            float distanceToEdge(float2 p) {
                float2 q = abs(p - resolution * 0.5) - (resolution * 0.5 - corner);
                return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - corner;
            }
            half4 main(float2 p) {
                float d = distanceToEdge(p);
                float2 n = float2(distanceToEdge(p + float2(0.5, 0.0)) - distanceToEdge(p - float2(0.5, 0.0)),
                                  distanceToEdge(p + float2(0.0, 0.5)) - distanceToEdge(p - float2(0.0, 0.5)));
                n /= max(length(n), 0.001);
                float edge = pow(clamp(1.0 + d / rimWidth, 0.0, 1.0), 2.0);
                float2 samplePoint = clamp(p + n * power * edge, float2(0.0), resolution - 1.0);
                half4 color = scene.eval(samplePoint);
                float highlight = max(dot(n, normalize(float2(-0.6, -0.8))), 0.0);
                color.rgb += half3(edge * highlight * 0.12) * color.a;
                return color;
            }
        """
    }
}
