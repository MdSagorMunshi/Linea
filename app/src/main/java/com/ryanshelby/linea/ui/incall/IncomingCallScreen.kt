package com.ryanshelby.linea.ui.incall

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.ui.components.ContactAvatar
import com.ryanshelby.linea.telecom.ActiveCallInfo
import com.ryanshelby.linea.ui.components.FrostedGlassBox
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaMotion
import com.ryanshelby.linea.ui.theme.LineaTypography
import com.ryanshelby.linea.ui.theme.LocalReduceAnimations

@Composable
fun IncomingCallScreen(
    callInfo: ActiveCallInfo,
    onAnswer: () -> Unit,
    onReject: () -> Unit,
    onSilence: () -> Unit,
    onQuickSms: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val reduceAnimations = LocalReduceAnimations.current
    var isVisible by remember { mutableStateOf(false) }
    var showSmsSheet by remember { mutableStateOf(false) }
    var isSilenced by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val slideSpec = if (reduceAnimations) {
        spring<IntOffset>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)
    } else {
        spring<IntOffset>(dampingRatio = 0.65f, stiffness = 280f) // Overshoot bounce
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = slideSpec
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(LineaColors.BackgroundGradient)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = LineaDimensions.ScreenPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(30.dp))

                // Caller Avatar & Details Panel
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ContactAvatar(
                        photoUri = callInfo.photoUri,
                        displayName = callInfo.displayName ?: callInfo.phoneNumber,
                        size = 110.dp,
                        initialsTextSize = 42.sp,
                        borderWidth = 1.5.dp,
                        borderColor = LineaColors.TitaniumBlue.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = callInfo.displayName ?: "Incoming Call",
                        style = LineaTypography.headlineLarge,
                        color = LineaColors.TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = callInfo.phoneNumber,
                        style = LineaTypography.displayLarge.copy(fontSize = 22.sp),
                        color = LineaColors.TextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    FrostedGlassBox(
                        shape = RoundedCornerShape(8.dp),
                        borderColor = LineaColors.TitaniumBlue.copy(alpha = 0.3f)
                    ) {
                        Text(
                            text = "CELLULAR CALL",
                            style = LineaTypography.labelSmall,
                            color = LineaColors.TitaniumBlue,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Middle Quick Actions (Silence & Quick SMS)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Silence Button
                    FrostedGlassBox(
                        shape = CircleShape,
                        modifier = Modifier
                            .size(56.dp)
                            .clickable {
                                isSilenced = true
                                onSilence()
                            }
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VolumeOff,
                                contentDescription = "Silence Ringer",
                                tint = if (isSilenced) LineaColors.MutedRust else LineaColors.TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Quick SMS Button
                    FrostedGlassBox(
                        shape = CircleShape,
                        modifier = Modifier
                            .size(56.dp)
                            .clickable { showSmsSheet = true }
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Message,
                                contentDescription = "Quick SMS Reply",
                                tint = LineaColors.TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Bottom Answer & Reject Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reject Button (Muted Brick Red)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(LineaColors.MutedBrickRed)
                                .clickable { onReject() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CallEnd,
                                contentDescription = "Decline Call",
                                tint = LineaColors.TextPrimary,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Decline",
                            style = LineaTypography.bodyMedium,
                            color = LineaColors.TextSecondary
                        )
                    }

                    // Answer Button (Muted Sage Green)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(LineaColors.MutedSageGreen)
                                .clickable { onAnswer() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Call,
                                contentDescription = "Answer Call",
                                tint = LineaColors.TextPrimary,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Answer",
                            style = LineaTypography.bodyMedium,
                            color = LineaColors.TextSecondary
                        )
                    }
                }
            }

            // Quick SMS Reply Sheet
            if (showSmsSheet) {
                QuickSmsSheet(
                    onSendSms = { message ->
                        showSmsSheet = false
                        onQuickSms(message)
                    },
                    onDismiss = { showSmsSheet = false },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
private fun QuickSmsSheet(
    onSendSms: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val quickMessages = listOf(
        "Can't talk right now. What's up?",
        "I'll call you right back.",
        "In a meeting. Please text me.",
        "On my way."
    )

    FrostedGlassBox(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        borderColor = LineaColors.GlassBorder
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Quick SMS Reply",
                style = LineaTypography.titleMedium,
                color = LineaColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))

            quickMessages.forEach { msg ->
                Button(
                    onClick = { onSendSms(msg) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(LineaDimensions.ButtonCornerRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LineaColors.GlassFill,
                        contentColor = LineaColors.TextPrimary
                    )
                ) {
                    Text(
                        text = msg,
                        style = LineaTypography.bodyLarge,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(LineaDimensions.ButtonCornerRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LineaColors.GlassFallbackFill,
                    contentColor = LineaColors.TextSecondary
                )
            ) {
                Text(text = "Cancel", style = LineaTypography.bodyMedium)
            }
        }
    }
}
