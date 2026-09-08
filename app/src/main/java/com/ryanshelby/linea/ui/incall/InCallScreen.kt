package com.ryanshelby.linea.ui.incall

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapCalls
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.data.local.entities.CallNoteEntity
import com.ryanshelby.linea.telecom.ActiveCallInfo
import com.ryanshelby.linea.telecom.LineaAudioRoute
import com.ryanshelby.linea.telecom.LineaCallState
import com.ryanshelby.linea.ui.components.FrostedGlassBox
import com.ryanshelby.linea.ui.components.PreCallNoteBanner
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography
import com.ryanshelby.linea.ui.theme.LocalReduceAnimations

@Composable
fun InCallScreen(
    callInfo: ActiveCallInfo,
    secondaryCall: ActiveCallInfo? = null,
    isRecording: Boolean = false,
    recordingDurationSeconds: Long = 0L,
    durationWarningActive: Boolean = false,
    preCallNote: String? = null,
    existingNotes: List<CallNoteEntity> = emptyList(),
    onDisconnect: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleHold: () -> Unit,
    onDtmfPress: (Char) -> Unit,
    onDtmfRelease: () -> Unit,
    onAddCall: () -> Unit,
    onToggleRecord: () -> Unit = {},
    onSaveNote: (String) -> Unit = {},
    onAnswerWaitingHold: () -> Unit = {},
    onAnswerWaitingEnd: () -> Unit = {},
    onRejectWaiting: () -> Unit = {},
    onSwapCalls: () -> Unit = {},
    onMergeConference: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val reduceAnimations = LocalReduceAnimations.current
    var showKeypad by remember { mutableStateOf(false) }
    var showNoteSheet by remember { mutableStateOf(false) }

    val stateLabel = when (callInfo.state) {
        LineaCallState.DIALING -> "Dialing..."
        LineaCallState.RINGING -> "Ringing..."
        LineaCallState.HOLDING -> "On Hold"
        LineaCallState.ACTIVE -> formatDuration(callInfo.durationSeconds)
        LineaCallState.DISCONNECTED -> "Call Ended"
        LineaCallState.IDLE -> ""
    }

    // Pulsing recording dot animation
    val infiniteTransition = rememberInfiniteTransition(label = "RecPulse")
    val recPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RecPulseAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LineaColors.BackgroundGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = LineaDimensions.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Banners, Avatar & Contact Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Active Recording Pill Indicator
                AnimatedVisibility(
                    visible = isRecording,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(LineaColors.Danger.copy(alpha = 0.2f))
                            .border(1.dp, LineaColors.Danger.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(LineaColors.Danger.copy(alpha = if (reduceAnimations) 1f else recPulseAlpha))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "REC ${formatDuration(recordingDurationSeconds)}",
                                style = LineaTypography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = LineaColors.Danger
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Call Duration Warning Banner
                AnimatedVisibility(
                    visible = durationWarningActive,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(LineaColors.Warning.copy(alpha = 0.2f))
                            .border(1.dp, LineaColors.Warning, RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.WarningAmber,
                                contentDescription = null,
                                tint = LineaColors.Warning,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Call duration limit reached (${formatDuration(callInfo.durationSeconds)})",
                                style = LineaTypography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = LineaColors.Warning
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Call Waiting Banner (Incoming 2nd call)
                if (secondaryCall != null && secondaryCall.state == LineaCallState.RINGING) {
                    FrostedGlassBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        borderColor = LineaColors.TitaniumBlue
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(LineaColors.TitaniumBlue.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Call,
                                        contentDescription = null,
                                        tint = LineaColors.TitaniumBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Call Waiting...",
                                        style = LineaTypography.bodySmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = LineaColors.TitaniumBlue
                                    )
                                    Text(
                                        text = secondaryCall.displayName ?: secondaryCall.phoneNumber,
                                        style = LineaTypography.titleSmall,
                                        color = LineaColors.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onAnswerWaitingHold,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = LineaColors.AccentGreen,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("Hold & Answer", style = LineaTypography.bodySmall.copy(fontSize = 12.sp))
                                }

                                Button(
                                    onClick = onAnswerWaitingEnd,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = LineaColors.TitaniumBlue,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("End & Answer", style = LineaTypography.bodySmall.copy(fontSize = 12.sp))
                                }

                                Button(
                                    onClick = onRejectWaiting,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = LineaColors.Danger.copy(alpha = 0.3f),
                                        contentColor = LineaColors.Danger
                                    )
                                ) {
                                    Text("Decline", style = LineaTypography.bodySmall.copy(fontSize = 12.sp))
                                }
                            }
                        }
                    }
                }

                // Pre-Call Note Banner (if present)
                preCallNote?.let { note ->
                    PreCallNoteBanner(
                        noteText = note,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // Dual Active/Held Multi-Call Cards (if secondary connected)
                if (secondaryCall != null && secondaryCall.state != LineaCallState.RINGING) {
                    FrostedGlassBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        shape = RoundedCornerShape(16.dp),
                        borderColor = LineaColors.GlassBorder
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Primary Call Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(LineaColors.AccentGreen.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        style = LineaTypography.bodySmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = LineaColors.AccentGreen
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = callInfo.displayName ?: callInfo.phoneNumber,
                                    style = LineaTypography.titleSmall,
                                    color = LineaColors.TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = formatDuration(callInfo.durationSeconds),
                                    style = LineaTypography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                    color = LineaColors.AccentGreen
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Secondary Held Call Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(LineaColors.Warning.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "ON HOLD",
                                        style = LineaTypography.bodySmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = LineaColors.Warning
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = secondaryCall.displayName ?: secondaryCall.phoneNumber,
                                    style = LineaTypography.bodyMedium,
                                    color = LineaColors.TextSecondary,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Multi-call controls: Swap & Merge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onSwapCalls,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = LineaColors.GlassFill,
                                        contentColor = LineaColors.TitaniumBlue
                                    )
                                ) {
                                    Icon(Icons.Filled.SwapCalls, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Swap Calls", style = LineaTypography.bodySmall)
                                }

                                Button(
                                    onClick = onMergeConference,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = LineaColors.GlassFill,
                                        contentColor = LineaColors.AccentGreen
                                    )
                                ) {
                                    Icon(Icons.Filled.CallMerge, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Merge Conference", style = LineaTypography.bodySmall)
                                }
                            }
                        }
                    }
                } else {
                    // Standard Single Caller Avatar
                    FrostedGlassBox(
                        modifier = Modifier.size(100.dp),
                        shape = CircleShape,
                        borderColor = if (callInfo.state == LineaCallState.HOLDING) {
                            LineaColors.Warning.copy(alpha = 0.5f)
                        } else {
                            LineaColors.GlassBorderFocused
                        }
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "Caller",
                                modifier = Modifier.size(52.dp),
                                tint = LineaColors.TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Caller Display Name
                    Text(
                        text = callInfo.displayName?.ifBlank { null } ?: callInfo.phoneNumber.ifBlank { "Unknown" },
                        style = LineaTypography.headlineMedium,
                        color = LineaColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Phone Number
                    if (!callInfo.displayName.isNullOrBlank() && callInfo.displayName != callInfo.phoneNumber) {
                        Text(
                            text = callInfo.phoneNumber,
                            style = LineaTypography.bodyMedium,
                            color = LineaColors.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Call State / Duration
                    AnimatedContent(
                        targetState = stateLabel,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "CallStateTransition"
                    ) { targetText ->
                        Text(
                            text = targetText,
                            style = LineaTypography.titleMedium.copy(fontFeatureSettings = "tnum"),
                            color = if (callInfo.state == LineaCallState.HOLDING) {
                                LineaColors.Warning
                            } else {
                                LineaColors.TitaniumBlue
                            },
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Middle Section: Controls Grid (2 Rows)
            FrostedGlassBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(24.dp),
                borderColor = LineaColors.GlassBorder
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp, horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Row 1: Mute, Keypad, Speaker, Hold
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        InCallActionButton(
                            icon = if (callInfo.isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                            label = if (callInfo.isMuted) "Unmute" else "Mute",
                            isActive = callInfo.isMuted,
                            activeColor = LineaColors.Danger,
                            onClick = onToggleMute
                        )

                        InCallActionButton(
                            icon = Icons.Filled.Dialpad,
                            label = "Keypad",
                            isActive = showKeypad,
                            activeColor = LineaColors.TitaniumBlue,
                            onClick = { showKeypad = !showKeypad }
                        )

                        val isSpeakerOn = callInfo.audioRoute == LineaAudioRoute.SPEAKER
                        InCallActionButton(
                            icon = if (isSpeakerOn) Icons.Filled.VolumeUp else Icons.Filled.VolumeDown,
                            label = "Speaker",
                            isActive = isSpeakerOn,
                            activeColor = LineaColors.TitaniumBlue,
                            onClick = onToggleSpeaker
                        )

                        val isHeld = callInfo.state == LineaCallState.HOLDING || callInfo.isHeld
                        InCallActionButton(
                            icon = if (isHeld) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            label = if (isHeld) "Resume" else "Hold",
                            isActive = isHeld,
                            activeColor = LineaColors.Warning,
                            onClick = onToggleHold
                        )
                    }

                    // Row 2: Record, Notes, Add Call, Swap/Merge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        InCallActionButton(
                            icon = Icons.Filled.FiberManualRecord,
                            label = if (isRecording) "Recording" else "Record",
                            isActive = isRecording,
                            activeColor = LineaColors.Danger,
                            onClick = onToggleRecord
                        )

                        InCallActionButton(
                            icon = Icons.Filled.EditNote,
                            label = "Notes",
                            isActive = showNoteSheet,
                            activeColor = LineaColors.TitaniumBlue,
                            onClick = { showNoteSheet = true }
                        )

                        InCallActionButton(
                            icon = Icons.Filled.PersonAdd,
                            label = "Add Call",
                            isActive = false,
                            onClick = onAddCall
                        )

                        if (secondaryCall != null) {
                            InCallActionButton(
                                icon = Icons.Filled.SwapCalls,
                                label = "Swap",
                                isActive = false,
                                activeColor = LineaColors.TitaniumBlue,
                                onClick = onSwapCalls
                            )
                        } else {
                            InCallActionButton(
                                icon = Icons.Filled.CallMerge,
                                label = "Merge",
                                isActive = false,
                                activeColor = LineaColors.TextTertiary,
                                onClick = {}
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // Bottom Section: End Call Button
            EndCallButton(
                onClick = onDisconnect,
                modifier = Modifier.padding(bottom = 28.dp)
            )
        }

        // Animated In-Call DTMF Keypad Bottom Sheet
        AnimatedVisibility(
            visible = showKeypad,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            InCallKeypadSheet(
                onDtmfPress = onDtmfPress,
                onDtmfRelease = onDtmfRelease,
                onDismiss = { showKeypad = false }
            )
        }

        // In-Call Note Taking Sheet
        if (showNoteSheet) {
            InCallNoteSheet(
                callerName = callInfo.displayName,
                phoneNumber = callInfo.phoneNumber,
                existingNotes = existingNotes,
                onSaveNote = onSaveNote,
                onDismiss = { showNoteSheet = false }
            )
        }
    }
}

@Composable
private fun InCallActionButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color = LineaColors.TitaniumBlue,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reduceAnimations = LocalReduceAnimations.current

    val scale = if (isPressed && !reduceAnimations) 0.90f else 1.0f
    val backgroundColor by animateColorAsState(
        targetValue = if (isActive) activeColor.copy(alpha = 0.25f) else LineaColors.GlassFill,
        label = "ButtonBgColor"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isActive) activeColor else LineaColors.GlassBorder,
        label = "ButtonBorderColor"
    )
    val iconTint by animateColorAsState(
        targetValue = if (isActive) activeColor else LineaColors.TextPrimary,
        label = "ButtonIconTint"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(backgroundColor)
                .border(LineaDimensions.HairlineBorder, borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            style = LineaTypography.bodySmall.copy(fontSize = 11.sp),
            color = if (isActive) activeColor else LineaColors.TextSecondary
        )
    }
}

@Composable
private fun EndCallButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reduceAnimations = LocalReduceAnimations.current

    val scale = if (isPressed && !reduceAnimations) 0.92f else 1.0f

    Box(
        modifier = modifier
            .scale(scale)
            .size(70.dp)
            .clip(CircleShape)
            .background(LineaColors.Danger)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.CallEnd,
            contentDescription = "End Call",
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}

private fun formatDuration(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hrs > 0) {
        String.format("%02d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format("%02d:%02d", mins, secs)
    }
}
