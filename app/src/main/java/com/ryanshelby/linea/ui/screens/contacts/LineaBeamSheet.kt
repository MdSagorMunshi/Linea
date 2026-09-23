package com.ryanshelby.linea.ui.screens.contacts

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.nfc.NfcBeamManager
import com.ryanshelby.linea.qr_code.QrCodeEngine
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaTypography
import com.ryanshelby.linea.ui.theme.captionBold
import com.ryanshelby.linea.ui.theme.captionMedium
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-tech cybernetic bottom sheet for beaming contact cards via physical NFC tap
 * and programming physical NFC cards/tags with real-time tag detection, exact diagnostic
 * failure reasons, and a Try Again retry workflow.
 */
sealed class TagWriteUiState {
    object Idle : TagWriteUiState()
    object WaitingForTag : TagWriteUiState()
    data class TagDetected(val tagType: String, val uidHex: String) : TagWriteUiState()
    data class Success(val contactName: String, val tagType: String, val bytesWritten: Int) : TagWriteUiState()
    data class Error(
        val title: String,
        val reason: String,
        val tagType: String? = null,
        val requiredBytes: Int = 0,
        val availableBytes: Int = 0
    ) : TagWriteUiState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LineaBeamSheet(
    name: String,
    numbers: List<String>,
    nfcBeamManager: NfcBeamManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val payload = remember(name, numbers) {
        QrCodeEngine.ContactPayload(name, numbers)
    }

    var tagWriteUiState by remember { mutableStateOf<TagWriteUiState>(TagWriteUiState.Idle) }

    val isNfcSupported = remember { nfcBeamManager.isNfcSupported(context) }
    var isNfcEnabled by remember { mutableStateOf(nfcBeamManager.isNfcEnabled(context)) }

    // Register active beam payload on enter and clear on dismiss
    DisposableEffect(payload) {
        nfcBeamManager.setActiveBeamContact(payload)
        onDispose {
            nfcBeamManager.setActiveBeamContact(null)
            nfcBeamManager.pendingTagWritePayload = null
            nfcBeamManager.clearTagWriteStatus()
        }
    }

    // Refresh NFC enabled status whenever resume or check
    LaunchedEffect(Unit) {
        isNfcEnabled = nfcBeamManager.isNfcEnabled(context)
    }

    // Listen to tag write results
    LaunchedEffect(Unit) {
        nfcBeamManager.tagWriteStatus.collect { result ->
            when (result) {
                is NfcBeamManager.TagWriteResult.TagDetected -> {
                    tagWriteUiState = TagWriteUiState.TagDetected(result.tagType, result.uidHex)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                is NfcBeamManager.TagWriteResult.Success -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    tagWriteUiState = TagWriteUiState.Success(
                        contactName = result.contactName,
                        tagType = result.tagType,
                        bytesWritten = result.bytesWritten
                    )
                }
                is NfcBeamManager.TagWriteResult.Error -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    tagWriteUiState = TagWriteUiState.Error(
                        title = result.title,
                        reason = result.reason,
                        tagType = result.tagType,
                        requiredBytes = result.requiredBytes,
                        availableBytes = result.availableBytes
                    )
                }
            }
        }
    }

    // Theme color based on live state
    val activeColor = when (tagWriteUiState) {
        is TagWriteUiState.Error -> Color(0xFFEF5350)
        is TagWriteUiState.Success -> LineaColors.EmeraldPrimary
        is TagWriteUiState.TagDetected -> LineaColors.CyanPrimary
        is TagWriteUiState.WaitingForTag -> LineaColors.NeonOrchid
        is TagWriteUiState.Idle -> if (!isNfcSupported) Color(0xFFEF5350) else if (!isNfcEnabled) Color(0xFFFFB300) else LineaColors.CyanPrimary
    }

    // Infinite cyber pulse animation for radar
    val infiniteTransition = rememberInfiniteTransition(label = "nfc_radar")
    val pulseProgress1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse1"
    )
    val pulseProgress2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, delayMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse2"
    )
    val pulseProgress3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, delayMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse3"
    )
    val radarRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_rot"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LineaColors.BackgroundTop.copy(alpha = 0.98f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(activeColor.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(activeColor.copy(alpha = 0.15f))
                            .border(1.dp, activeColor.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (tagWriteUiState) {
                                is TagWriteUiState.Success -> Icons.Default.CheckCircle
                                is TagWriteUiState.Error -> Icons.Default.Warning
                                is TagWriteUiState.TagDetected,
                                is TagWriteUiState.WaitingForTag -> Icons.Default.TouchApp
                                else -> Icons.Default.Nfc
                            },
                            contentDescription = "NFC",
                            tint = activeColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = if (tagWriteUiState is TagWriteUiState.Idle) "LiNEA BEAM" else "NFC TAG PROGRAMMER",
                        style = LineaTypography.captionBold.copy(
                            fontSize = 12.sp,
                            letterSpacing = 2.sp
                        ),
                        color = activeColor
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = LineaColors.TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Holographic Cyber Radar & Antenna Canvas
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(200.dp)) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val maxRadius = size.width / 2f

                    // Concentric radar grid rings
                    val gridSteps = 4
                    for (i in 1..gridSteps) {
                        val r = (maxRadius / gridSteps) * i
                        drawCircle(
                            color = Color(0xFF1E3A4A).copy(alpha = 0.45f),
                            radius = r,
                            center = Offset(cx, cy),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }

                    // Rotating radar beam line
                    val angleRad = (radarRotation * PI / 180.0).toFloat()
                    val lineEnd = Offset(
                        cx + maxRadius * cos(angleRad),
                        cy + maxRadius * sin(angleRad)
                    )
                    drawLine(
                        brush = Brush.radialGradient(
                            listOf(
                                activeColor.copy(alpha = 0.7f),
                                Color.Transparent
                            ),
                            center = Offset(cx, cy),
                            radius = maxRadius
                        ),
                        start = Offset(cx, cy),
                        end = lineEnd,
                        strokeWidth = 2.dp.toPx()
                    )

                    // Expanding pulsating wave rings
                    val pulses = listOf(pulseProgress1, pulseProgress2, pulseProgress3)
                    pulses.forEach { p ->
                        val r = maxRadius * p
                        val alpha = (1f - p).coerceIn(0f, 1f) * 0.8f
                        drawCircle(
                            color = activeColor.copy(alpha = alpha),
                            radius = r,
                            center = Offset(cx, cy),
                            style = Stroke(width = 2.dp.toPx() * (1f - p * 0.4f))
                        )
                    }
                }

                // Central NFC Core Emblem
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    activeColor.copy(alpha = 0.4f),
                                    Color(0xFF0D141C).copy(alpha = 0.95f)
                                )
                            )
                        )
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    activeColor,
                                    activeColor.copy(alpha = 0.6f)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (tagWriteUiState) {
                            is TagWriteUiState.Success -> Icons.Default.CheckCircle
                            is TagWriteUiState.Error -> Icons.Default.Warning
                            is TagWriteUiState.TagDetected,
                            is TagWriteUiState.WaitingForTag -> Icons.Default.TouchApp
                            else -> Icons.Default.Nfc
                        },
                        contentDescription = "NFC Beam Status",
                        tint = activeColor,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Dynamic State Card: Success, Error (with Try Again), TagDetected, Waiting, or Idle Beam
            when (val state = tagWriteUiState) {
                is TagWriteUiState.Success -> {
                    // Success View
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0E2218).copy(alpha = 0.9f))
                            .border(1.5.dp, LineaColors.EmeraldPrimary.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = LineaColors.EmeraldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "TAG PROGRAMMED SUCCESSFULLY",
                                style = LineaTypography.captionBold.copy(fontSize = 11.sp, letterSpacing = 1.2.sp),
                                color = LineaColors.EmeraldPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Contact card for ${state.contactName} is now saved to this NFC tag.",
                            style = LineaTypography.bodyMedium.copy(fontSize = 13.sp),
                            color = LineaColors.TextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(LineaColors.EmeraldPrimary.copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = state.tagType,
                                    style = LineaTypography.captionBold.copy(fontSize = 11.sp),
                                    color = LineaColors.EmeraldPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(LineaColors.CyanPrimary.copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${state.bytesWritten} bytes written",
                                    style = LineaTypography.captionBold.copy(fontSize = 11.sp),
                                    color = LineaColors.CyanPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    tagWriteUiState = TagWriteUiState.WaitingForTag
                                    nfcBeamManager.clearTagWriteStatus()
                                    nfcBeamManager.pendingTagWritePayload = payload
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, LineaColors.EmeraldPrimary.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = "Write Another",
                                    style = LineaTypography.labelLarge.copy(fontSize = 12.sp),
                                    color = LineaColors.EmeraldPrimary
                                )
                            }

                            Button(
                                onClick = {
                                    tagWriteUiState = TagWriteUiState.Idle
                                    nfcBeamManager.clearTagWriteStatus()
                                    nfcBeamManager.pendingTagWritePayload = null
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = LineaColors.EmeraldPrimary)
                            ) {
                                Text(
                                    text = "Done",
                                    style = LineaTypography.labelLarge.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }

                is TagWriteUiState.Error -> {
                    // Exact Failure Diagnostics Card with Try Again button
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF241315).copy(alpha = 0.95f))
                            .border(1.5.dp, Color(0xFFEF5350).copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFEF5350),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = state.title.uppercase(),
                                style = LineaTypography.captionBold.copy(fontSize = 12.sp, letterSpacing = 1.2.sp),
                                color = Color(0xFFEF5350)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = state.reason,
                            style = LineaTypography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
                            color = LineaColors.TextPrimary,
                            textAlign = TextAlign.Center
                        )

                        if (state.tagType != null || state.requiredBytes > 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (state.tagType != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF3B1E22))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = state.tagType,
                                            style = LineaTypography.captionBold.copy(fontSize = 11.sp),
                                            color = Color(0xFFFFA4A2)
                                        )
                                    }
                                }
                                if (state.requiredBytes > 0 && state.availableBytes > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF3B1E22))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${state.requiredBytes}B req / ${state.availableBytes}B avail",
                                            style = LineaTypography.captionBold.copy(fontSize = 11.sp),
                                            color = Color(0xFFFFA4A2)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Prominent "Try Again" Button & Cancel Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    tagWriteUiState = TagWriteUiState.Idle
                                    nfcBeamManager.clearTagWriteStatus()
                                    nfcBeamManager.pendingTagWritePayload = null
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, LineaColors.TextTertiary.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "Cancel",
                                    style = LineaTypography.labelLarge.copy(fontSize = 12.sp),
                                    color = LineaColors.TextSecondary
                                )
                            }

                            Button(
                                onClick = {
                                    tagWriteUiState = TagWriteUiState.WaitingForTag
                                    nfcBeamManager.clearTagWriteStatus()
                                    nfcBeamManager.pendingTagWritePayload = payload
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Try Again",
                                    style = LineaTypography.labelLarge.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                is TagWriteUiState.TagDetected -> {
                    // Tag Detected / Writing progress view
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF131A22).copy(alpha = 0.9f))
                            .border(1.5.dp, LineaColors.CyanPrimary.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(LineaColors.CyanPrimary)
                            )
                            Text(
                                text = "TAG DETECTED: ${state.tagType.uppercase()}",
                                style = LineaTypography.captionBold.copy(fontSize = 11.sp, letterSpacing = 1.2.sp),
                                color = LineaColors.CyanPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "UID: ${state.uidHex} • Programming contact card data...",
                            style = LineaTypography.bodyMedium.copy(fontSize = 13.sp),
                            color = LineaColors.TextPrimary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                is TagWriteUiState.WaitingForTag -> {
                    // Waiting for tag view
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF131A22).copy(alpha = 0.85f))
                            .border(1.dp, LineaColors.NeonOrchid.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(LineaColors.NeonOrchid)
                            )
                            Text(
                                text = "WAITING FOR NFC CARD / TAG",
                                style = LineaTypography.captionBold.copy(fontSize = 11.sp, letterSpacing = 1.2.sp),
                                color = LineaColors.NeonOrchid
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Touch a blank or rewritable NFC card, sticker, or keyfob to the back of your phone.",
                            style = LineaTypography.bodyMedium.copy(fontSize = 13.sp),
                            color = LineaColors.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                is TagWriteUiState.Idle -> {
                    // Standard LiNEA Beam Mode Card
                    val statusColor = when {
                        !isNfcSupported -> Color(0xFFEF5350)
                        !isNfcEnabled -> Color(0xFFFFB300)
                        else -> LineaColors.EmeraldPrimary
                    }

                    val statusTitle = when {
                        !isNfcSupported -> "NFC HARDWARE NOT FOUND"
                        !isNfcEnabled -> "NFC IS CURRENTLY DISABLED"
                        else -> "BEAM BROADCASTING"
                    }

                    val statusSubtitle = when {
                        !isNfcSupported -> "This device does not have an NFC chip. Use QR code sharing instead."
                        !isNfcEnabled -> "Turn on NFC in system settings to beam contacts instantly with a tap."
                        else -> "Hold your phone back-to-back with another phone to transfer ${payload.name}'s contact card."
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF131A22).copy(alpha = 0.85f))
                            .border(1.dp, statusColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Text(
                                text = statusTitle,
                                style = LineaTypography.captionBold.copy(fontSize = 11.sp, letterSpacing = 1.2.sp),
                                color = statusColor
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = statusSubtitle,
                            style = LineaTypography.bodyMedium.copy(fontSize = 13.sp),
                            color = LineaColors.TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        if (!isNfcEnabled && isNfcSupported) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { nfcBeamManager.openNfcSettings(context) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Open NFC Settings",
                                    style = LineaTypography.captionBold.copy(fontSize = 12.sp),
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Contact Summary Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF18222E).copy(alpha = 0.6f))
                    .border(1.dp, Color(0xFF2E3E50).copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val initial = payload.name.trim().firstOrNull()?.uppercase() ?: "C"
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(LineaColors.CyanPrimary.copy(alpha = 0.15f))
                        .border(1.5.dp, LineaColors.CyanPrimary.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        style = LineaTypography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                        color = LineaColors.TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = payload.name,
                        style = LineaTypography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = LineaColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = payload.numbers.firstOrNull() ?: "No number",
                        style = LineaTypography.captionMedium.copy(fontSize = 12.sp),
                        color = LineaColors.TextTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Bottom Actions based on active state
            if (isNfcEnabled && isNfcSupported) {
                when (tagWriteUiState) {
                    is TagWriteUiState.WaitingForTag,
                    is TagWriteUiState.TagDetected -> {
                        OutlinedButton(
                            onClick = {
                                tagWriteUiState = TagWriteUiState.Idle
                                nfcBeamManager.clearTagWriteStatus()
                                nfcBeamManager.pendingTagWritePayload = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LineaColors.NeonOrchid.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "Cancel Tag Writing",
                                style = LineaTypography.labelLarge,
                                color = LineaColors.NeonOrchid
                            )
                        }
                    }
                    is TagWriteUiState.Idle -> {
                        OutlinedButton(
                            onClick = {
                                tagWriteUiState = TagWriteUiState.WaitingForTag
                                nfcBeamManager.clearTagWriteStatus()
                                nfcBeamManager.pendingTagWritePayload = payload
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LineaColors.CyanPrimary.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = LineaColors.CyanPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Program Physical NFC Tag / Card",
                                style = LineaTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = LineaColors.CyanPrimary
                            )
                        }
                    }
                    is TagWriteUiState.Success,
                    is TagWriteUiState.Error -> {
                        // Handled directly inside the state card above for optimal UX
                    }
                }
            }
        }
    }
}
