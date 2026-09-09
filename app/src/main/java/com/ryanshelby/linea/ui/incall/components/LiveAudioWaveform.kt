package com.ryanshelby.linea.ui.incall.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LocalReduceAnimations
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin

@Composable
fun LiveAudioWaveform(
    isActiveCall: Boolean,
    isMuted: Boolean,
    isHeld: Boolean,
    isRecording: Boolean = false,
    modifier: Modifier = Modifier
) {
    val reduceAnimations = LocalReduceAnimations.current

    val transition = rememberInfiniteTransition(label = "WaveformAnimation")

    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (reduceAnimations) 0f else (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase"
    )

    val breathingAmplitude by transition.animateFloat(
        initialValue = 0.75f,
        targetValue = if (reduceAnimations) 0.85f else 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "WaveBreathing"
    )

    val barCount = 28
    val indices = remember { (0 until barCount).toList() }

    val baseColor = when {
        isHeld -> LineaColors.Warning
        isMuted -> LineaColors.MutedRust
        isRecording -> LineaColors.TitaniumBlue
        else -> LineaColors.TitaniumBlue
    }

    val secondaryColor = when {
        isHeld -> LineaColors.Warning.copy(alpha = 0.5f)
        isMuted -> LineaColors.MutedRust.copy(alpha = 0.3f)
        isRecording -> LineaColors.MutedBrickRed.copy(alpha = 0.7f)
        else -> LineaColors.MutedSageGreen.copy(alpha = 0.8f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(48.dp)) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerY = canvasHeight / 2f
            val totalSpacing = canvasWidth / (barCount + 1)
            val barWidth = (totalSpacing * 0.45f).coerceIn(2.5f, 6.5f)

            for (i in 0 until barCount) {
                val x = totalSpacing * (i + 1)

                // Normalized distance from center (-1.0 to 1.0)
                val normX = (i - barCount / 2f) / (barCount / 2f)
                val gaussianEnvelope = exp(-3.0 * normX * normX).toFloat()

                val heightFactor = if (!isActiveCall || isHeld) {
                    // Holding / idle pulse
                    val idleWave = sin(phase + i * 0.4f) * 0.2f + 0.3f
                    idleWave * gaussianEnvelope
                } else if (isMuted) {
                    // Muted flatline with faint ambient presence
                    0.08f * gaussianEnvelope
                } else {
                    // Active conversational voice waveform
                    val harmonic1 = sin(phase * 1.5f + i * 0.35f)
                    val harmonic2 = sin(phase * 2.8f - i * 0.5f) * 0.5f
                    val rawWave = abs(harmonic1 + harmonic2).toFloat() / 1.5f
                    (rawWave * breathingAmplitude * gaussianEnvelope).coerceIn(0.1f, 1.0f)
                }

                val barHeight = (heightFactor * (canvasHeight - 6.dp.toPx())).coerceAtLeast(4.dp.toPx())
                val topY = centerY - barHeight / 2f

                val barBrush = Brush.verticalGradient(
                    colors = listOf(baseColor, secondaryColor),
                    startY = topY,
                    endY = topY + barHeight
                )

                drawRoundRect(
                    brush = barBrush,
                    topLeft = Offset(x - barWidth / 2f, topY),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }
    }
}
