package com.ryanshelby.linea.ui.incall

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.telecom.ActiveCallInfo
import com.ryanshelby.linea.telecom.LineaAudioRoute
import com.ryanshelby.linea.telecom.LineaCallState
import com.ryanshelby.linea.ui.components.FrostedGlassBox
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaMotion
import com.ryanshelby.linea.ui.theme.LineaTypography
import com.ryanshelby.linea.ui.theme.LocalReduceAnimations

@Composable
fun InCallScreen(
    callInfo: ActiveCallInfo,
    onDisconnect: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleHold: () -> Unit,
    onDtmfPress: (Char) -> Unit,
    onDtmfRelease: () -> Unit,
    onAddCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reduceAnimations = LocalReduceAnimations.current
    var showKeypad by remember { mutableStateOf(false) }

    val stateLabel = when (callInfo.state) {
        LineaCallState.DIALING -> "Dialing..."
        LineaCallState.RINGING -> "Ringing..."
        LineaCallState.HOLDING -> "On Hold"
        LineaCallState.ACTIVE -> formatDuration(callInfo.durationSeconds)
        LineaCallState.DISCONNECTED -> "Call Ended"
        LineaCallState.IDLE -> ""
    }

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
            Spacer(modifier = Modifier.height(24.dp))

            // Top Section: Avatar & Contact Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Frosted Glass Avatar
                FrostedGlassBox(
                    modifier = Modifier.size(110.dp),
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
                            modifier = Modifier.size(56.dp),
                            tint = LineaColors.TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Caller Display Name
                Text(
                    text = callInfo.displayName?.ifBlank { null } ?: callInfo.phoneNumber.ifBlank { "Unknown" },
                    style = LineaTypography.headlineMedium,
                    color = LineaColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Phone Number (if display name is present)
                if (!callInfo.displayName.isNullOrBlank() && callInfo.displayName != callInfo.phoneNumber) {
                    Text(
                        text = callInfo.phoneNumber,
                        style = LineaTypography.bodyMedium,
                        color = LineaColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Call State / Duration (tabular figures)
                AnimatedContent(
                    targetState = stateLabel,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "CallStateTransition"
                ) { targetText ->
                    Text(
                        text = targetText,
                        style = LineaTypography.titleMedium.copy(
                            fontFeatureSettings = "tnum"
                        ),
                        color = if (callInfo.state == LineaCallState.HOLDING) {
                            LineaColors.Warning
                        } else {
                            LineaColors.TitaniumBlue
                        },
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Middle Section: Controls Grid (2x3)
            FrostedGlassBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(24.dp),
                borderColor = LineaColors.GlassBorder
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Row 1: Mute, Keypad, Speaker
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
                    }

                    // Row 2: Hold, Add Call, Placeholder/Swap
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val isHeld = callInfo.state == LineaCallState.HOLDING || callInfo.isHeld
                        InCallActionButton(
                            icon = if (isHeld) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            label = if (isHeld) "Resume" else "Hold",
                            isActive = isHeld,
                            activeColor = LineaColors.Warning,
                            onClick = onToggleHold
                        )

                        InCallActionButton(
                            icon = Icons.Filled.PersonAdd,
                            label = "Add Call",
                            isActive = false,
                            onClick = onAddCall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // Bottom Section: End Call Button
            EndCallButton(
                onClick = onDisconnect,
                modifier = Modifier.padding(bottom = 32.dp)
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
                .size(60.dp)
                .clip(CircleShape)
                .background(backgroundColor)
                .border(LineaDimensions.HairlineBorder, borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            style = LineaTypography.bodySmall,
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
            .size(72.dp)
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
