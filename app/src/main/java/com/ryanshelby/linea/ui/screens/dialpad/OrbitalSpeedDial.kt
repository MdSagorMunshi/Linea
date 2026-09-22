package com.ryanshelby.linea.ui.screens.dialpad

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaTypography
import com.ryanshelby.linea.ui.theme.captionBold
import com.ryanshelby.linea.ui.theme.captionMedium
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Data model for Orbital Planetary Speed Dial contacts.
 */
data class OrbitalSpeedDialContact(
    val id: Long,
    val name: String,
    val phoneNumber: String,
    val photoUri: String? = null,
    val initials: String = name.trim().firstOrNull()?.uppercase() ?: "C",
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false
)

/**
 * High-tech radial "Planetary Dial" that blossoms around the user's thumb
 * with magnetic snapping, animated laser tethers, and instant drag-and-release calling.
 */
@Composable
fun OrbitalSpeedDialOverlay(
    contacts: List<OrbitalSpeedDialContact>,
    centerOffset: Offset,
    onSelectContact: (OrbitalSpeedDialContact) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    // Display up to 6–8 contacts max for optimal radial distribution
    val orbitalList = remember(contacts) {
        if (contacts.isEmpty()) {
            listOf(
                OrbitalSpeedDialContact(1, "Voicemail", "123", isFavorite = true),
                OrbitalSpeedDialContact(2, "Emergency", "911", isFavorite = true)
            )
        } else {
            contacts.take(7)
        }
    }

    // Planetary orbit radius in pixels (approx 140dp)
    val orbitRadiusPx = with(density) { 145.dp.toPx() }
    val nodeRadiusPx = with(density) { 28.dp.toPx() }
    val magneticThresholdPx = with(density) { 46.dp.toPx() }

    // Touch position relative to centerOffset
    var dragTouchOffset by remember { mutableStateOf<Offset?>(null) }
    var selectedIndex by remember { mutableIntStateOf(-1) }

    // Planetary angles distributed evenly across upper arc (from 195° down to 345°, i.e. -165° to -15°)
    val count = orbitalList.size
    val nodeAnglesRad = remember(count) {
        if (count == 1) {
            listOf(-PI.toFloat() / 2f)
        } else {
            val startAngleRad = -160f * (PI.toFloat() / 180f)
            val endAngleRad = -20f * (PI.toFloat() / 180f)
            val step = (endAngleRad - startAngleRad) / (count - 1)
            List(count) { i -> startAngleRad + i * step }
        }
    }

    // Node pixel positions relative to centerOffset
    val nodeOffsets = remember(nodeAnglesRad, orbitRadiusPx) {
        nodeAnglesRad.map { angle ->
            Offset(
                x = orbitRadiusPx * cos(angle),
                y = orbitRadiusPx * sin(angle)
            )
        }
    }

    // Ambient orbit track rotation
    val infiniteTransition = rememberInfiniteTransition(label = "orbital_rot")
    val orbitRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_rot"
    )

    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    // Helper to evaluate magnetic capture
    fun evaluateMagneticTarget(touch: Offset) {
        var closestIdx = -1
        var minDistance = Float.MAX_VALUE

        for (i in nodeOffsets.indices) {
            val nodePos = nodeOffsets[i]
            val dist = hypot(touch.x - nodePos.x, touch.y - nodePos.y)
            if (dist < magneticThresholdPx && dist < minDistance) {
                minDistance = dist
                closestIdx = i
            }
        }

        if (closestIdx != selectedIndex) {
            selectedIndex = closestIdx
            if (closestIdx != -1) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF02060B).copy(alpha = 0.78f))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        val localOffset = startOffset - centerOffset
                        dragTouchOffset = localOffset
                        evaluateMagneticTarget(localOffset)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val updated = (dragTouchOffset ?: Offset.Zero) + dragAmount
                        dragTouchOffset = updated
                        evaluateMagneticTarget(updated)
                    },
                    onDragEnd = {
                        if (selectedIndex in orbitalList.indices) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSelectContact(orbitalList[selectedIndex])
                        } else {
                            onDismiss()
                        }
                    },
                    onDragCancel = {
                        onDismiss()
                    }
                )
            }
    ) {
        // Upper HUD Cyber Status Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 24.dp, end = 24.dp)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val activeContact = if (selectedIndex in orbitalList.indices) orbitalList[selectedIndex] else null

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (activeContact != null) LineaColors.EmeraldPrimary else LineaColors.CyanPrimary)
                )
                Text(
                    text = if (activeContact != null) "TARGET ACQUIRED" else "PLANETARY ORBITAL DIAL",
                    style = LineaTypography.captionBold.copy(
                        fontSize = 11.sp,
                        letterSpacing = 1.8.sp
                    ),
                    color = if (activeContact != null) LineaColors.EmeraldPrimary else LineaColors.CyanPrimary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (activeContact != null) {
                Text(
                    text = activeContact.name,
                    style = LineaTypography.titleLarge.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = LineaColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = activeContact.phoneNumber,
                    style = LineaTypography.bodyLarge.copy(
                        color = LineaColors.CyanPrimary,
                        letterSpacing = 0.5.sp
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Release thumb to dial instantly",
                    style = LineaTypography.captionMedium.copy(fontSize = 12.sp),
                    color = LineaColors.EmeraldPrimary
                )
            } else {
                Text(
                    text = "Slide thumb to select contact",
                    style = LineaTypography.bodyMedium.copy(fontSize = 14.sp),
                    color = LineaColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Release in center to cancel",
                    style = LineaTypography.captionMedium.copy(fontSize = 11.sp),
                    color = LineaColors.TextTertiary
                )
            }
        }

        // Concentric Orbit Tracks & Dynamic Laser Tether Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = centerOffset.x
            val cy = centerOffset.y

            // Outer primary orbit track
            drawCircle(
                color = LineaColors.CyanPrimary.copy(alpha = 0.25f),
                radius = orbitRadiusPx,
                center = Offset(cx, cy),
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), orbitRotation)
                )
            )

            // Inner guidance track
            drawCircle(
                color = Color(0xFF1B2A38).copy(alpha = 0.45f),
                radius = orbitRadiusPx * 0.52f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.dp.toPx())
            )

            // Radial spokes from center to each contact node
            nodeOffsets.forEachIndexed { index, nodeOffset ->
                val isTargeted = index == selectedIndex
                drawLine(
                    color = if (isTargeted) LineaColors.EmeraldPrimary.copy(alpha = 0.5f)
                    else Color(0xFF223548).copy(alpha = 0.35f),
                    start = Offset(cx, cy),
                    end = Offset(cx + nodeOffset.x, cy + nodeOffset.y),
                    strokeWidth = if (isTargeted) 2.dp.toPx() else 1.dp.toPx()
                )
            }

            // Laser tether from center to active thumb touch position
            val touch = dragTouchOffset
            if (touch != null) {
                val targetEnd = if (selectedIndex != -1) {
                    // Magnetic snap line to node center
                    Offset(cx + nodeOffsets[selectedIndex].x, cy + nodeOffsets[selectedIndex].y)
                } else {
                    Offset(cx + touch.x, cy + touch.y)
                }

                // Outer energy glow beam
                drawLine(
                    brush = Brush.linearGradient(
                        listOf(
                            LineaColors.CyanPrimary.copy(alpha = 0.4f),
                            if (selectedIndex != -1) LineaColors.EmeraldPrimary.copy(alpha = 0.6f)
                            else LineaColors.CyanPrimary.copy(alpha = 0.3f)
                        )
                    ),
                    start = Offset(cx, cy),
                    end = targetEnd,
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // High-intensity core laser
                drawLine(
                    color = if (selectedIndex != -1) LineaColors.EmeraldPrimary else LineaColors.CyanPrimary,
                    start = Offset(cx, cy),
                    end = targetEnd,
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // Center Call Button Core Anchor
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (centerOffset.x - with(density) { 34.dp.toPx() }).roundToInt(),
                        (centerOffset.y - with(density) { 34.dp.toPx() }).roundToInt()
                    )
                }
                .size(68.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            LineaColors.CyanPrimary.copy(alpha = 0.35f),
                            Color(0xFF0E1620).copy(alpha = 0.95f)
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    color = if (selectedIndex != -1) LineaColors.EmeraldPrimary else LineaColors.CyanPrimary,
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onDismiss()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (selectedIndex != -1) Icons.Default.Call else Icons.Default.Close,
                contentDescription = "Cancel or Call",
                tint = if (selectedIndex != -1) LineaColors.EmeraldPrimary else LineaColors.TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }

        // Planetary Contact Spheres
        orbitalList.forEachIndexed { index, contact ->
            val nodeOffset = nodeOffsets[index]
            val isTargeted = index == selectedIndex

            val scale by animateFloatAsState(
                targetValue = if (isTargeted) 1.28f else 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "node_scale_$index"
            )

            val nodeX = centerOffset.x + nodeOffset.x
            val nodeY = centerOffset.y + nodeOffset.y

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (nodeX - with(density) { 32.dp.toPx() }).roundToInt(),
                            (nodeY - with(density) { 32.dp.toPx() }).roundToInt()
                        )
                    }
                    .size(64.dp)
                    .scale(scale)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSelectContact(contact)
                    },
                contentAlignment = Alignment.Center
            ) {
                // Outer glow aura when magnetically targeted
                if (isTargeted) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        LineaColors.EmeraldPrimary.copy(alpha = 0.45f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }

                // Planetary Node Core
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    if (isTargeted) Color(0xFF143026) else Color(0xFF131D28),
                                    Color(0xFF090E14)
                                )
                            )
                        )
                        .border(
                            width = if (isTargeted) 2.5.dp else 1.5.dp,
                            color = if (isTargeted) LineaColors.EmeraldPrimary else LineaColors.CyanPrimary.copy(alpha = 0.6f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contact.initials,
                        style = LineaTypography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isTargeted) LineaColors.EmeraldPrimary else LineaColors.TextPrimary
                    )
                }

                // Contact First Name Pill underneath node
                Box(
                    modifier = Modifier
                        .offset(y = 38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0A1017).copy(alpha = 0.9f))
                        .border(
                            width = 1.dp,
                            color = if (isTargeted) LineaColors.EmeraldPrimary.copy(alpha = 0.6f)
                            else Color(0xFF223547).copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = contact.name.split(" ").firstOrNull() ?: contact.name,
                        style = LineaTypography.captionBold.copy(fontSize = 10.sp),
                        color = if (isTargeted) LineaColors.EmeraldPrimary else LineaColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
