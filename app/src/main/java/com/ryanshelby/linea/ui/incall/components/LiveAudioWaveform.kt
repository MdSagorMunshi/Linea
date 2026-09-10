package com.ryanshelby.linea.ui.incall.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaTypography
import com.ryanshelby.linea.ui.theme.LocalReduceAnimations
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Medical ECG / Heartbeat voice visualizer line.
 *
 * Renders ONLY the heartbeat line directly on the screen without background cards, borders, grids, or text.
 * When there is silence / no audio detected from caller or receiver, the line is completely flat and straight.
 * When audio / voice is detected, clinical ECG heartbeat waves surge and animate dynamically based on loudness.
 */
@Composable
fun LiveAudioWaveform(
    isActiveCall: Boolean,
    isMuted: Boolean,
    isHeld: Boolean,
    isRecording: Boolean = false,
    audioLevelHistory: FloatArray = FloatArray(32),
    modifier: Modifier = Modifier
) {
    val reduceAnimations = LocalReduceAnimations.current

    val noiseFloor = 0.035f
    val maxAudio = remember(audioLevelHistory) {
        var m = 0f
        for (lvl in audioLevelHistory) {
            if (lvl > m) m = lvl
        }
        m
    }

    // Audio is considered active only when call is active, unmuted, not on hold, and exceeds noise floor
    val isAudioPresent = isActiveCall && !isMuted && !isHeld && (maxAudio > noiseFloor)

    // Smooth envelope transitioning between flat straight line (silence) and ECG heartbeat wave (sound detected)
    val voiceEnvelope by animateFloatAsState(
        targetValue = if (isAudioPresent) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isAudioPresent) 70 else 240,
            easing = FastOutSlowInEasing
        ),
        label = "VoiceEnvelope"
    )

    // Smooth horizontal scrolling animation for the heartbeat wave
    val transition = rememberInfiniteTransition(label = "EcgScrollTransition")
    val scrollPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (reduceAnimations) 0f else 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 800000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "EcgPhase"
    )

    // Clean colors matching state: Cyan for active voice, Amber for hold/mute, Red for recording
    val (lineColor, glowColor) = when {
        isHeld -> Pair(LineaColors.Warning, LineaColors.Warning.copy(alpha = 0.35f))
        isMuted -> Pair(Color(0xFFF59E0B), Color(0xFFF59E0B).copy(alpha = 0.30f))
        isRecording -> Pair(Color(0xFFFF3B30), Color(0xFFFF453A).copy(alpha = 0.35f))
        else -> Pair(Color(0xFF00F0FF), Color(0xFF00E5FF).copy(alpha = 0.38f))
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Text(
                text = "ECG • VOICE MONITORING",
                style = LineaTypography.bodySmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.8.sp
                ),
                color = lineColor.copy(alpha = if (isAudioPresent) 0.85f else 0.40f)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f

            val ecgPath = Path()

            // If voice envelope is 0 (silence / no audio from either party), render a strictly straight horizontal line
            if (voiceEnvelope <= 0.002f) {
                ecgPath.moveTo(0f, centerY)
                ecgPath.lineTo(width, centerY)
            } else {
                // Audio detected: render medical ECG heartbeat wave modulated by sound
                val stepX = 2.dp.toPx()
                val steps = (width / stepX).toInt().coerceAtLeast(20)
                val numBeatsAcross = 3.5f
                val currentPhase = (scrollPhase * 1.2f) % 1000f

                var first = true
                for (i in 0..steps) {
                    val x = (i * stepX).coerceAtMost(width)
                    val normX = x / width

                    // Interpolate audio level from rolling history
                    val audioSample = getInterpolatedAudioLevel(normX, audioLevelHistory, noiseFloor)
                    val localScale = (audioSample * 0.7f + maxAudio * 0.3f).coerceIn(0f, 1f)

                    // ECG cycle progress
                    val beatProgress = (normX * numBeatsAcross) - currentPhase
                    val u = ((beatProgress % 1f) + 1f) % 1f
                    val rawEcg = getEcgOffset(u)

                    // Edge taper to cleanly anchor the ends at baseline
                    val edgeFade = when {
                        normX < 0.04f -> normX / 0.04f
                        normX > 0.96f -> (1f - normX) / 0.04f
                        else -> 1f
                    }

                    // Displacement from baseline, scaled by voice energy, global envelope, and edge fade
                    val maxDisplacement = height * 0.44f
                    val displacement = rawEcg * localScale * voiceEnvelope * edgeFade * maxDisplacement
                    val y = centerY + displacement

                    if (first) {
                        ecgPath.moveTo(x, y)
                        first = false
                    } else {
                        ecgPath.lineTo(x, y)
                    }
                }
            }

            // 1. Soft Glow Aura
            drawPath(
                path = ecgPath,
                color = glowColor,
                style = Stroke(
                    width = 4.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // 2. Crisp Medical Neon Line
            drawPath(
                path = ecgPath,
                color = lineColor,
                style = Stroke(
                    width = 2.2.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}
}

/**
 * Interpolates audio level from rolling history for smooth continuity along the horizontal line.
 */
private fun getInterpolatedAudioLevel(normX: Float, history: FloatArray, noiseFloor: Float): Float {
    if (history.isEmpty()) return 0f
    val maxIdx = history.size - 1
    val floatIdx = (normX * maxIdx).coerceIn(0f, maxIdx.toFloat())
    val idx0 = floatIdx.toInt()
    val idx1 = (idx0 + 1).coerceAtMost(maxIdx)
    val frac = floatIdx - idx0
    val raw = history[idx0] * (1f - frac) + history[idx1] * frac
    if (raw <= noiseFloor) return 0f
    val gated = ((raw - noiseFloor) / (1f - noiseFloor)).coerceIn(0f, 1f)
    return kotlin.math.sqrt(gated)
}

/**
 * Continuous ECG heartbeat mathematical model:
 *
 * - 0.00 .. 0.10: Flat baseline
 * - 0.10 .. 0.22: P-wave (smooth rounded upward bump)
 * - 0.22 .. 0.30: Flat baseline
 * - 0.30 .. 0.46: Continuous QRS Complex (Q dip, R peak, S plunge, recovery)
 * - 0.46 .. 0.52: Flat baseline
 * - 0.52 .. 0.72: T-wave (smooth rounded upward wave)
 * - 0.72 .. 1.00: Flat baseline
 */
private fun getEcgOffset(u: Float): Float {
    val normU = (u % 1f + 1f) % 1f
    return when {
        normU < 0.10f -> 0f
        normU < 0.22f -> {
            val p = (normU - 0.10f) / 0.12f
            -(0.15f * sin(p * PI.toFloat()))
        }
        normU < 0.30f -> 0f
        normU < 0.46f -> {
            val qrs = (normU - 0.30f) / 0.16f
            when {
                qrs < 0.20f -> {
                    val sub = qrs / 0.20f
                    0.18f * sin(sub * (PI.toFloat() / 2f))
                }
                qrs < 0.52f -> {
                    val sub = (qrs - 0.20f) / 0.32f
                    0.18f - 1.10f * (0.5f - 0.5f * cos(sub * PI.toFloat()))
                }
                qrs < 0.82f -> {
                    val sub = (qrs - 0.52f) / 0.30f
                    -0.92f + 1.47f * (0.5f - 0.5f * cos(sub * PI.toFloat()))
                }
                else -> {
                    val sub = (qrs - 0.82f) / 0.18f
                    0.55f * cos(sub * (PI.toFloat() / 2f))
                }
            }
        }
        normU < 0.52f -> 0f
        normU < 0.72f -> {
            val p = (normU - 0.52f) / 0.20f
            -(0.25f * sin(p * PI.toFloat()))
        }
        else -> 0f
    }
}
