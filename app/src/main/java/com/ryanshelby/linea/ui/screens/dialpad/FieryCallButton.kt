package com.ryanshelby.linea.ui.screens.dialpad

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.ryanshelby.linea.ui.components.neumorphic
import com.ryanshelby.linea.ui.theme.LineaColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Stages for the secret Easter Egg animation.
 */
private enum class BlastPhase {
    IDLE,
    BLAST_SLOW_MO, // Exploding outward in dramatic slow motion (~1600ms)
    ZENITH,        // Brief suspension at peak dispersion (~200ms)
    HEALING        // Reverse magnetic/temporal rewind & reassembly (~1400ms)
}

/**
 * Model for an explosive debris fragment of the shattered call button.
 */
private data class DebrisShard(
    val initialAngle: Float,
    val initialDist: Float,
    val targetDist: Float,
    val initialRot: Float,
    val targetRot: Float,
    val size: Float,
    val color: Color
)

/**
 * Model for a glowing fire / ember particle.
 */
private data class FireParticle(
    val angle: Float,
    val maxDist: Float,
    val baseSize: Float,
    val color: Color,
    val speedFactor: Float,
    val wobbleFreq: Float
)

/**
 * Ambient floating flame ember for the fiery idle state.
 */
private data class AmbientEmber(
    val startXRatio: Float,
    val speed: Float,
    val size: Float,
    val color: Color,
    val phaseOffset: Float
)

/**
 * CallButton with:
 * 1. Default Titanium Blue neumorphic style OR Fiery Magma style.
 * 2. Regular tap triggers [onClick] (< 400ms).
 * 3. Tap & Hold for 3 seconds triggers the secret Easter Egg:
 *    - Slow-motion explosion of fragments, fire shockwave, and burning sparks.
 *    - Slow-motion healing / rearranging sequence as fragments pull inward.
 *    - Toggles permanent fiery style ([onToggleFiery]).
 */
@Composable
fun CallButton(
    isFiery: Boolean,
    onClick: () -> Unit,
    onToggleFiery: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    var isPressed by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var blastPhase by remember { mutableStateOf(BlastPhase.IDLE) }

    // Blast phase progress: 0f -> 1f
    val blastProgress = remember { Animatable(0f) }
    // Healing phase progress: 0f -> 1f
    val healProgress = remember { Animatable(0f) }
    // Final flash pulse at moment of completion
    val finalFlash = remember { Animatable(0f) }

    // Generate deterministic shards and particles once per trigger
    val shards = remember {
        val rand = Random(42)
        val shardColorsDefault = listOf(
            Color(0xFF4A6B88), Color(0xFF3B566E), Color(0xFF5E82A3),
            Color(0xFF6E94B8), Color(0xFF2E465A), Color(0xFFFFA726)
        )
        val shardColorsFiery = listOf(
            Color(0xFFFF3D00), Color(0xFFFF9100), Color(0xFFFFD600),
            Color(0xFFD50000), Color(0xFF8B0000), Color(0xFFFFAB40)
        )
        List(28) { index ->
            val angle = (index.toFloat() / 28f) * 2f * PI.toFloat() + (rand.nextFloat() - 0.5f) * 0.3f
            val initDist = rand.nextFloat() * 26f + 4f
            val targetDist = rand.nextFloat() * 110f + 70f
            DebrisShard(
                initialAngle = angle,
                initialDist = initDist,
                targetDist = targetDist,
                initialRot = rand.nextFloat() * 360f,
                targetRot = (rand.nextFloat() - 0.5f) * 720f,
                size = rand.nextFloat() * 10f + 8f,
                color = if (isFiery) shardColorsFiery[index % shardColorsFiery.size] else shardColorsDefault[index % shardColorsDefault.size]
            )
        }
    }

    val fireParticles = remember {
        val rand = Random(1337)
        val flameColors = listOf(
            Color(0xFFFFFFFF), // Core incandescent white
            Color(0xFFFFF59D), // Light yellow
            Color(0xFFFFD54F), // Golden amber
            Color(0xFFFF9800), // Pure blaze orange
            Color(0xFFFF5722), // Fiery vermilion
            Color(0xFFD50000)  // Deep crimson
        )
        List(54) { index ->
            FireParticle(
                angle = (index.toFloat() / 54f) * 2f * PI.toFloat() + (rand.nextFloat() - 0.5f) * 0.4f,
                maxDist = rand.nextFloat() * 160f + 60f,
                baseSize = rand.nextFloat() * 6f + 2.5f,
                color = flameColors[rand.nextInt(flameColors.size)],
                speedFactor = rand.nextFloat() * 0.4f + 0.8f,
                wobbleFreq = rand.nextFloat() * 4f + 2f
            )
        }
    }

    // Ambient floating embers for the fiery idle state
    val ambientEmbers = remember {
        val rand = Random(777)
        List(8) {
            AmbientEmber(
                startXRatio = rand.nextFloat(),
                speed = rand.nextFloat() * 0.5f + 0.5f,
                size = rand.nextFloat() * 3.5f + 1.5f,
                color = if (rand.nextBoolean()) Color(0xFFFFD54F) else Color(0xFFFF7043),
                phaseOffset = rand.nextFloat() * 2f * PI.toFloat()
            )
        }
    }

    // Infinite animation for fiery breathing aura and idle embers
    val infiniteTransition = rememberInfiniteTransition(label = "FieryAuraTransition")
    val fieryPulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FieryAuraPulse"
    )
    val emberLoopTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "EmberLoopTime"
    )
    val flameWobble by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FlameWobble"
    )

    // Trigger full blast & healing sequence
    val triggerEasterEgg: () -> Unit = {
        coroutineScope.launch {
            blastPhase = BlastPhase.BLAST_SLOW_MO
            holdProgress = 0f
            isPressed = false

            // Stage 1: Explosive blast in slow motion (~1600ms)
            // Begins with swift initial burst then drifts outwards with slow-drag physics
            blastProgress.snapTo(0f)
            blastProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1600, easing = FastOutSlowInEasing)
            )

            // Stage 2: Brief suspension at zenith (~200ms)
            blastPhase = BlastPhase.ZENITH
            delay(200)

            // Stage 3: Rearrange & healing temporal rewind (~1400ms)
            blastPhase = BlastPhase.HEALING
            healProgress.snapTo(0f)
            healProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing)
            )

            // Stage 4: Flash on completion & state commit
            finalFlash.snapTo(1f)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onToggleFiery()

            finalFlash.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 400, easing = LinearEasing)
            )

            blastProgress.snapTo(0f)
            healProgress.snapTo(0f)
            blastPhase = BlastPhase.IDLE
        }
    }

    // Standard scale compression on normal tap press
    val scale by animateFloatAsState(
        targetValue = when {
            blastPhase != BlastPhase.IDLE -> 1.0f
            isPressed -> 0.92f
            else -> 1.0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "CallButtonScale"
    )

    val currentElevation = if (isPressed) 1.5.dp else 6.5.dp

    // Color definitions
    val defaultButtonColor = if (isPressed) Color(0xFF3B566E) else Color(0xFF4A6B88)
    val fieryColorPressed = Color(0xFFBF360C)

    val isAnimating = blastPhase != BlastPhase.IDLE

    Box(
        modifier = modifier
            .size(68.dp)
            .graphicsLayer(clip = false),
        contentAlignment = Alignment.Center
    ) {
        // --- AMBIENT FIERY AURA (when isFiery == true and idle) ---
        if (isFiery && blastPhase == BlastPhase.IDLE) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .scale(fieryPulse)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF3D00).copy(alpha = 0.65f),
                                Color(0xFFFF9100).copy(alpha = 0.35f),
                                Color(0xFFFFD600).copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Ambient rising embers
            Canvas(modifier = Modifier.size(68.dp)) {
                ambientEmbers.forEach { ember ->
                    val progress = (emberLoopTime * ember.speed + ember.phaseOffset) % 1f
                    val y = size.height * (1f - progress * 1.3f)
                    val sway = sin((progress * 2f * PI.toFloat()) + ember.phaseOffset) * 8f
                    val x = size.width * (0.2f + ember.startXRatio * 0.6f) + sway
                    val alpha = (1f - progress).coerceIn(0f, 1f) * 0.85f

                    drawCircle(
                        color = ember.color.copy(alpha = alpha),
                        radius = ember.size,
                        center = Offset(x, y)
                    )
                }
            }
        }

        // --- CHARGING GLOW & HEAT RING (while holding 0..3s) ---
        if (holdProgress > 0f && blastPhase == BlastPhase.IDLE) {
            Canvas(modifier = Modifier.size(82.dp)) {
                val strokeWidth = 3.5.dp.toPx()
                val radius = (size.width - strokeWidth) / 2f
                // Charging ring
                drawCircle(
                    brush = Brush.sweepGradient(
                        listOf(
                            Color(0xFFFF5722),
                            Color(0xFFFFD54F),
                            Color(0xFFFF3D00),
                            Color(0xFFFFEB3B),
                            Color(0xFFFF5722)
                        )
                    ),
                    radius = radius,
                    style = Stroke(width = strokeWidth * holdProgress)
                )

                // Heat glow halo
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(
                            Color(0xFFFF9800).copy(alpha = 0.45f * holdProgress),
                            Color.Transparent
                        )
                    ),
                    radius = radius + 6.dp.toPx()
                )
            }
        }

        // --- MAIN BUTTON SURFACE (hidden when exploded in blast phase) ---
        val buttonVisible = blastPhase == BlastPhase.IDLE || blastPhase == BlastPhase.HEALING
        val buttonAlpha = when (blastPhase) {
            BlastPhase.IDLE -> 1f
            BlastPhase.HEALING -> healProgress.value
            else -> 0f
        }

        if (buttonVisible) {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        this.alpha = buttonAlpha
                        this.scaleX = scale
                        this.scaleY = scale
                    }
                    .size(68.dp)
                    .then(
                        if (isFiery) {
                            Modifier.neumorphic(
                                shape = CircleShape,
                                elevation = currentElevation,
                                isSunken = isPressed,
                                surfaceColor = if (isPressed) fieryColorPressed else Color(0xFFE64A19)
                            )
                        } else {
                            Modifier.neumorphic(
                                shape = CircleShape,
                                elevation = currentElevation,
                                isSunken = isPressed,
                                surfaceColor = defaultButtonColor
                            )
                        }
                    )
                    // Pointer gesture detection: Handles quick tap vs 3-second hold
                    .pointerInput(isAnimating) {
                        if (isAnimating) return@pointerInput
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val startTime = SystemClock.elapsedRealtime()
                            isPressed = true
                            var triggered = false

                            val holdDuration = 3000L
                            val holdJob = coroutineScope.launch {
                                var lastHapticStep = 0
                                while (true) {
                                    val elapsed = SystemClock.elapsedRealtime() - startTime
                                    val progress = (elapsed.toFloat() / holdDuration).coerceIn(0f, 1f)
                                    holdProgress = progress

                                    // Periodic haptic pulses as charge approaches 3s
                                    if (elapsed >= 1500L && lastHapticStep < 1) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        lastHapticStep = 1
                                    } else if (elapsed >= 2400L && lastHapticStep < 2) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        lastHapticStep = 2
                                    }

                                    if (progress >= 1f) {
                                        triggered = true
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        triggerEasterEgg()
                                        break
                                    }
                                    delay(16L)
                                }
                            }

                            val up = waitForUpOrCancellation()
                            holdJob.cancel()
                            val totalHeld = SystemClock.elapsedRealtime() - startTime
                            isPressed = false
                            holdProgress = 0f

                            if (!triggered && up != null && totalHeld < 400L) {
                                onClick()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // If fiery, apply radiant volcanic magma fill and incandescent rim
                if (isFiery) {
                    Canvas(modifier = Modifier.size(68.dp)) {
                        val cx = size.width * 0.44f
                        val cy = size.height * 0.40f
                        val radius = size.width * 0.62f

                        // Volcanic magma radial gradient covering full button
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFF9C4), // Hot white-gold core
                                    Color(0xFFFFEA00), // Bright solar yellow
                                    Color(0xFFFF6D00), // Intense magma orange
                                    Color(0xFFD50000), // Volcanic scarlet
                                    Color(0xFF5C0000), // Deep lava crimson
                                    Color(0xFF210000)  // Obsidian volcanic rim
                                ),
                                center = Offset(cx, cy),
                                radius = radius
                            )
                        )

                        // Incandescent blazing rim ring
                        drawCircle(
                            color = Color(0xFFFFD54F).copy(alpha = 0.8f),
                            radius = size.width / 2f - 1.5.dp.toPx(),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }

                // Call phone icon: Crazy fiery animated icon when fiery, or clean default icon
                if (isFiery) {
                    FieryCrazyCallIcon(
                        flameWobble = flameWobble,
                        emberTime = emberLoopTime,
                        modifier = Modifier.size(54.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Call,
                        contentDescription = "Call",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }

        // --- SLOW-MOTION BLAST & HEALING PARTICLE ENGINE ---
        if (blastPhase != BlastPhase.IDLE) {
            Canvas(
                modifier = Modifier
                    .size(280.dp)
                    .graphicsLayer(clip = false)
            ) {
                val centerOffset = Offset(size.width / 2f, size.height / 2f)

                when (blastPhase) {
                    BlastPhase.BLAST_SLOW_MO, BlastPhase.ZENITH -> {
                        val p = if (blastPhase == BlastPhase.ZENITH) 1.0f else blastProgress.value
                        drawBlastPhase(centerOffset, p, shards, fireParticles, isFiery)
                    }
                    BlastPhase.HEALING -> {
                        val p = healProgress.value
                        drawHealingPhase(centerOffset, p, shards, fireParticles, isFiery)
                    }
                    else -> {}
                }

                // Flash on completion
                if (finalFlash.value > 0f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(
                                Color.White.copy(alpha = finalFlash.value * 0.95f),
                                Color(0xFFFFD54F).copy(alpha = finalFlash.value * 0.7f),
                                Color(0xFFFF5722).copy(alpha = finalFlash.value * 0.4f),
                                Color.Transparent
                            )
                        ),
                        radius = 80.dp.toPx() * (1f - finalFlash.value * 0.3f),
                        center = centerOffset
                    )
                }
            }
        }
    }
}

/**
 * Draws the slow-motion explosive blast:
 * - Shockwave fire ring expanding in slow motion
 * - Shattered shards flying outward with drag and slow spin
 * - Glowing embers and flame sparks trailing outward
 */
private fun DrawScope.drawBlastPhase(
    center: Offset,
    progress: Float,
    shards: List<DebrisShard>,
    particles: List<FireParticle>,
    wasFiery: Boolean
) {
    // 1. Slow-motion fire shockwave ring
    val shockwaveRadius = 34.dp.toPx() + progress * 95.dp.toPx()
    val shockwaveAlpha = (1f - progress * 0.85f).coerceIn(0f, 1f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                Color(0xFFFFD54F).copy(alpha = shockwaveAlpha * 0.8f),
                Color(0xFFFF5722).copy(alpha = shockwaveAlpha * 0.5f),
                Color.Transparent
            ),
            center = center,
            radius = shockwaveRadius + 14.dp.toPx()
        ),
        radius = shockwaveRadius,
        center = center,
        style = Stroke(width = 8.dp.toPx() * (1f - progress * 0.5f))
    )

    // 2. Glowing flame embers / sparks expanding outward in slow motion
    particles.forEach { p ->
        val currentDist = (p.maxDist * p.speedFactor * progress).coerceAtLeast(0f)
        val wobble = sin(progress * p.wobbleFreq * PI.toFloat()) * 6.dp.toPx()
        val x = center.x + cos(p.angle) * currentDist - sin(p.angle) * wobble
        val y = center.y + sin(p.angle) * currentDist + cos(p.angle) * wobble
        val alpha = (1f - progress * 0.75f).coerceIn(0f, 1f)
        val particleRadius = (p.baseSize * (1f - progress * 0.35f)).coerceAtLeast(1.5f)

        drawCircle(
            color = p.color.copy(alpha = alpha),
            radius = particleRadius,
            center = Offset(x, y)
        )
    }

    // 3. Shards of button flying outward with rotation
    shards.forEach { shard ->
        val currentDist = shard.initialDist + (shard.targetDist - shard.initialDist) * progress
        val currentRot = shard.initialRot + shard.targetRot * progress
        val x = center.x + cos(shard.initialAngle) * currentDist
        val y = center.y + sin(shard.initialAngle) * currentDist
        val alpha = (1f - progress * 0.6f).coerceIn(0f, 1f)

        drawShard(
            center = Offset(x, y),
            size = shard.size,
            rotationDeg = currentRot,
            color = shard.color.copy(alpha = alpha)
        )
    }
}

/**
 * Draws the slow-motion healing / rearranging phase:
 * - Shards reverse direction and fly inward towards the center
 * - Embers spiral into a magnetic vortex
 * - Glowing core energy forms as button coalesces
 */
private fun DrawScope.drawHealingPhase(
    center: Offset,
    progress: Float,
    shards: List<DebrisShard>,
    particles: List<FireParticle>,
    wasFiery: Boolean
) {
    val rewindFactor = 1f - progress // Goes from 1.0 down to 0.0

    // 1. Inward magnetic fire vortex glow
    val vortexRadius = 34.dp.toPx() + rewindFactor * 80.dp.toPx()
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFD54F).copy(alpha = progress * 0.65f),
                Color(0xFFFF5722).copy(alpha = progress * 0.35f),
                Color.Transparent
            ),
            center = center,
            radius = vortexRadius
        ),
        radius = vortexRadius,
        center = center
    )

    // 2. Swirling ember sparks collapsing into center
    particles.forEach { p ->
        val currentDist = (p.maxDist * p.speedFactor * rewindFactor).coerceAtLeast(0f)
        // Add vortex spin angle during rewind
        val vortexAngle = p.angle + progress * 2.5f
        val x = center.x + cos(vortexAngle) * currentDist
        val y = center.y + sin(vortexAngle) * currentDist
        val alpha = (1f - rewindFactor * 0.5f).coerceIn(0f, 1f)
        val particleRadius = (p.baseSize * (0.8f + progress * 0.4f)).coerceAtLeast(1.5f)

        drawCircle(
            color = p.color.copy(alpha = alpha),
            radius = particleRadius,
            center = Offset(x, y)
        )
    }

    // 3. Shards pulling back inward into the circular button
    shards.forEach { shard ->
        val currentDist = shard.initialDist + (shard.targetDist - shard.initialDist) * rewindFactor
        val currentRot = shard.initialRot + shard.targetRot * rewindFactor
        val x = center.x + cos(shard.initialAngle) * currentDist
        val y = center.y + sin(shard.initialAngle) * currentDist

        drawShard(
            center = Offset(x, y),
            size = shard.size,
            rotationDeg = currentRot,
            color = shard.color
        )
    }
}

/**
 * Draws a sharp angular debris shard with a stylized triangular/quad path.
 */
private fun DrawScope.drawShard(
    center: Offset,
    size: Float,
    rotationDeg: Float,
    color: Color
) {
    val rad = rotationDeg * (PI.toFloat() / 180f)
    val cosA = cos(rad)
    val sinA = sin(rad)

    fun rotatePoint(dx: Float, dy: Float): Offset {
        return Offset(
            center.x + dx * cosA - dy * sinA,
            center.y + dx * sinA + dy * cosA
        )
    }

    val path = Path().apply {
        val p1 = rotatePoint(-size * 0.5f, -size * 0.7f)
        val p2 = rotatePoint(size * 0.6f, -size * 0.2f)
        val p3 = rotatePoint(size * 0.3f, size * 0.7f)
        val p4 = rotatePoint(-size * 0.6f, size * 0.4f)
        moveTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        lineTo(p3.x, p3.y)
        lineTo(p4.x, p4.y)
        close()
    }

    drawPath(path = path, color = color, style = Fill)
}

/**
 * Custom-designed crazy animated fiery call icon:
 * - Official telephone handset transformed into molten white-gold fire relic.
 * - Leaping flame plumes from the earpiece.
 * - Fire dragon crest fins along the outer spine.
 * - Exhaust flame rocket jet flaring off the mouthpiece.
 * - Hot incandescent white core energy line.
 * - Dynamic flame flickering and floating ember sparks.
 */
@Composable
private fun FieryCrazyCallIcon(
    flameWobble: Float,
    emberTime: Float,
    modifier: Modifier = Modifier
) {
    val handsetPath = remember {
        PathParser().parsePathString(
            "M20.01 15.38c-1.23 0-2.42-.2-3.53-.56a.977.977 0 0 0-1.01.24l-2.2 2.2a15.053 15.053 0 0 1-6.59-6.59l2.2-2.21a.96.96 0 0 0 .25-1.01A11.36 11.36 0 0 1 8.5 4c0-.55-.45-1-1-1H4c-.55 0-1 .45-1 1 0 9.39 7.61 17 17 17 .55 0 1-.45 1-1v-3.5c0-.55-.45-1-1-1z"
        ).toPath()
    }

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val scale = size.width / 34f

        withTransform({
            translate(left = cx, top = cy)
            scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
            translate(left = -12f, top = -12f)
        }) {
            // Layer 1: Massive outer blaze flame plumes (crimson to vivid orange)
            drawFlameLayer(
                scaleFactor = 1.25f,
                topColor = Color(0xFFFF9100),
                botColor = Color(0xFFD50000),
                alpha = 0.82f,
                wobble = flameWobble
            )

            // Layer 2: Radiant mid fire tongues (solar yellow to blaze orange)
            drawFlameLayer(
                scaleFactor = 1.10f,
                topColor = Color(0xFFFFEA00),
                botColor = Color(0xFFFF6D00),
                alpha = 0.92f,
                wobble = -flameWobble * 0.85f
            )

            // Layer 3: Inner incandescent fire crests (white to golden amber)
            drawFlameLayer(
                scaleFactor = 0.95f,
                topColor = Color(0xFFFFFFFF),
                botColor = Color(0xFFFFD600),
                alpha = 0.96f,
                wobble = flameWobble * 0.5f
            )

            // Handset silhouette filled with molten fiery white-gold gradient
            val phoneBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFFFFFF), // Pure white hot tip
                    Color(0xFFFFF9C4), // Incandescent gold
                    Color(0xFFFFD600), // Solar yellow
                    Color(0xFFFF9100), // Lava orange
                    Color(0xFFFF3D00)  // Blazing red-orange
                ),
                start = Offset(3f, 3f),
                end = Offset(21f, 21f)
            )

            drawPath(
                path = handsetPath,
                brush = phoneBrush
            )

            // Sharp fiery electric border outline
            drawPath(
                path = handsetPath,
                color = Color(0xFFFF3D00),
                style = Stroke(width = 0.7f)
            )

            // Inner white-hot energy core line
            val corePath = Path().apply {
                moveTo(5f, 5f)
                cubicTo(5f, 12f, 12f, 19f, 19f, 19f)
            }
            drawPath(
                path = corePath,
                color = Color.White.copy(alpha = 0.95f),
                style = Stroke(width = 0.85f, cap = StrokeCap.Round)
            )

            // Micro ember spark particles dancing around the flames
            val sparks = listOf(
                Triple(4.5f, -7.0f, 0.75f),
                Triple(8.5f, -5.5f, 0.6f),
                Triple(-4.2f, 9.0f, 0.7f),
                Triple(-3.2f, 14.0f, 0.65f),
                Triple(27.0f, 24.5f, 0.75f),
                Triple(25.0f, 16.5f, 0.55f),
                Triple(1.5f, 1.0f, 0.5f),
                Triple(10.0f, -7.0f, 0.6f)
            )
            sparks.forEachIndexed { i, (sx, sy, baseR) ->
                val flicker = sin((emberTime * 2f * PI.toFloat() * 3f) + i).coerceIn(0.25f, 1f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = flicker),
                            Color(0xFFFFD600).copy(alpha = flicker * 0.8f),
                            Color(0xFFFF3D00).copy(alpha = 0f)
                        ),
                        center = Offset(sx, sy),
                        radius = baseR
                    ),
                    radius = baseR,
                    center = Offset(sx, sy)
                )
            }
        }
    }
}

/**
 * Draws a multi-tongue flame layer around the 24x24 handset coordinates.
 */
private fun DrawScope.drawFlameLayer(
    scaleFactor: Float,
    topColor: Color,
    botColor: Color,
    alpha: Float,
    wobble: Float
) {
    withTransform({
        translate(left = 12f, top = 12f)
        scale(scaleX = scaleFactor, scaleY = scaleFactor, pivot = Offset.Zero)
        translate(left = -12f, top = -12f)
    }) {
        val flameBrush = Brush.verticalGradient(
            colors = listOf(
                topColor.copy(alpha = alpha),
                botColor.copy(alpha = alpha)
            ),
            startY = -8f,
            endY = 24f
        )

        // Earpiece fire plumes (top-left)
        val earpieceFlames = Path().apply {
            moveTo(3.5f, 4.0f)
            cubicTo(1.5f + wobble * 0.6f, 0.5f, 3.0f, -4.5f, 5.0f + wobble * 0.8f, -6.5f)
            cubicTo(5.5f, -3.5f, 6.5f, -3.0f, 7.5f - wobble * 0.5f, -4.8f)
            cubicTo(8.0f, -2.5f, 9.0f, -2.0f, 10.0f + wobble * 0.4f, -3.5f)
            cubicTo(9.8f, -0.5f, 9.0f, 1.5f, 8.5f, 4.0f)
            close()
        }
        drawPath(earpieceFlames, brush = flameBrush)

        // Outer spine dragon-crest flames (bottom-left)
        val spineFlames = Path().apply {
            moveTo(3.0f, 6.0f)
            cubicTo(0.5f, 4.5f, -2.8f + wobble * 0.5f, 6.5f, -3.5f + wobble * 0.7f, 9.5f)
            cubicTo(-1.8f, 9.2f, -1.0f - wobble * 0.4f, 11.5f, -2.8f + wobble * 0.6f, 13.5f)
            cubicTo(-0.8f, 13.2f, 0.5f, 14.5f, -1.5f + wobble * 0.5f, 17.0f)
            cubicTo(0.8f, 16.2f, 2.0f, 15.0f, 3.0f, 13.0f)
            close()
        }
        drawPath(spineFlames, brush = flameBrush)

        // Mouthpiece jet flames (bottom-right)
        val mouthpieceFlames = Path().apply {
            moveTo(16.0f, 19.5f)
            cubicTo(18.0f, 22.5f, 22.5f + wobble * 0.6f, 24.5f, 26.0f + wobble * 0.9f, 23.5f)
            cubicTo(23.5f, 21.0f, 23.0f, 19.5f, 25.0f - wobble * 0.5f, 17.5f)
            cubicTo(22.2f, 17.8f, 20.5f, 16.8f, 19.0f, 15.5f)
            close()
        }
        drawPath(mouthpieceFlames, brush = flameBrush)
    }
}

