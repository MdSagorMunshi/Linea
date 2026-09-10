package com.ryanshelby.linea.ui.incall

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.telecom.ActiveCallInfo
import com.ryanshelby.linea.telecom.InternationalCountryHelper
import com.ryanshelby.linea.ui.components.ContactAvatar
import com.ryanshelby.linea.ui.components.FrostedGlassBox
import com.ryanshelby.linea.ui.incall.components.ContactPosterBackground
import com.ryanshelby.linea.ui.incall.components.QuickDeclineSheet
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography
import com.ryanshelby.linea.ui.theme.LocalReduceAnimations

@Composable
fun IncomingCallScreen(
    callInfo: ActiveCallInfo,
    onAnswer: () -> Unit,
    onReject: () -> Unit,
    onSilence: () -> Unit,
    onQuickSms: (String) -> Unit,
    modifier: Modifier = Modifier,
    isSilenced: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    val reduceAnimations = LocalReduceAnimations.current
    var isVisible by remember { mutableStateOf(false) }
    var showSmsSheet by remember { mutableStateOf(false) }
    var localSilenced by remember { mutableStateOf(false) }
    val effectiveSilenced = isSilenced || localSilenced

    val internationalPreview = remember(callInfo.phoneNumber) {
        InternationalCountryHelper.detectCountry(callInfo.phoneNumber)
    }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val slideSpec = if (reduceAnimations) {
        spring<IntOffset>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)
    } else {
        spring<IntOffset>(dampingRatio = 0.68f, stiffness = 320f)
    }

    // Expanding incoming aura ring pulse animation
    val ringTransition = rememberInfiniteTransition(label = "IncomingAura")
    val auraPulseScale by ringTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (reduceAnimations || effectiveSilenced) 1.0f else 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "AuraPulseScale"
    )
    val auraPulseAlpha by ringTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = if (reduceAnimations || effectiveSilenced) 0.0f else 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "AuraPulseAlpha"
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = slideSpec
        )
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            // 1. Full-Screen Frosted Contact Poster Background
            ContactPosterBackground(
                photoUri = callInfo.photoUri,
                displayName = callInfo.displayName ?: callInfo.phoneNumber
            )

            // 2. Main Content Container
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = LineaDimensions.ScreenPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // Top / Header: Incoming Call Label & Badge
                FrostedGlassBox(
                    shape = RoundedCornerShape(16.dp),
                    borderColor = LineaColors.TitaniumBlue.copy(alpha = 0.35f),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    val labelText = if (internationalPreview != null) {
                        "${internationalPreview.flagEmoji} ${internationalPreview.countryName} • INCOMING"
                    } else {
                        "INCOMING CELLULAR CALL"
                    }
                    Text(
                        text = labelText,
                        style = LineaTypography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = LineaColors.TitaniumBlue,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Center Caller Identity & Glowing Aura Avatar
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(170.dp)
                    ) {
                        // Radiant incoming ring pulses
                        if (!reduceAnimations && !effectiveSilenced) {
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .scale(auraPulseScale)
                                    .clip(CircleShape)
                                    .border(
                                        width = 2.dp,
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                LineaColors.TitaniumBlue.copy(alpha = auraPulseAlpha),
                                                Color.Transparent
                                            )
                                        ),
                                        shape = CircleShape
                                    )
                            )
                        }

                        ContactAvatar(
                            photoUri = callInfo.photoUri,
                            displayName = callInfo.displayName ?: callInfo.phoneNumber,
                            size = 120.dp,
                            initialsTextSize = 46.sp,
                            borderWidth = 2.dp,
                            borderColor = LineaColors.TitaniumBlue.copy(alpha = 0.8f)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = callInfo.displayName?.ifBlank { null } ?: "Incoming Call",
                        style = LineaTypography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = LineaColors.TextPrimary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = callInfo.phoneNumber,
                        style = LineaTypography.displayLarge.copy(
                            fontSize = 20.sp,
                            fontFeatureSettings = "tnum"
                        ),
                        color = LineaColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Middle Quick Actions (Silence Ringer & Quick Decline Sheet)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Silence Action Glass Button
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FrostedGlassBox(
                            shape = CircleShape,
                            borderColor = if (effectiveSilenced) LineaColors.MutedRust.copy(alpha = 0.6f) else LineaColors.GlassBorder,
                            modifier = Modifier
                                .size(58.dp)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    localSilenced = true
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
                                    tint = if (effectiveSilenced) LineaColors.MutedRust else LineaColors.TextPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (effectiveSilenced) "Silenced" else "Silence",
                            style = LineaTypography.bodySmall,
                            color = if (effectiveSilenced) LineaColors.MutedRust else LineaColors.TextSecondary
                        )
                    }

                    // Quick SMS Decline Glass Button
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FrostedGlassBox(
                            shape = CircleShape,
                            borderColor = LineaColors.GlassBorder,
                            modifier = Modifier
                                .size(58.dp)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showSmsSheet = true
                                }
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Message,
                                    contentDescription = "Quick Decline SMS",
                                    tint = LineaColors.TextPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Message",
                            style = LineaTypography.bodySmall,
                            color = LineaColors.TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                // Bottom Tactile Answer & Reject Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tactile Reject Button (Brick Red Glass)
                    TactileCallAction(
                        icon = Icons.Filled.CallEnd,
                        label = "Decline",
                        baseColor = LineaColors.MutedBrickRed,
                        glowColor = Color(0xFFC0392B),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onReject()
                        }
                    )

                    // Tactile Answer Button (Sage Green Glass)
                    TactileCallAction(
                        icon = Icons.Filled.Call,
                        label = "Answer",
                        baseColor = LineaColors.MutedSageGreen,
                        glowColor = Color(0xFF27AE60),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onAnswer()
                        }
                    )
                }
            }

            // Quick Decline Glass Sheet
            if (showSmsSheet) {
                QuickDeclineSheet(
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
private fun TactileCallAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    baseColor: Color,
    glowColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = if (isPressed) 0.92f else 1.0f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            baseColor.copy(alpha = 0.95f),
                            baseColor.copy(alpha = 0.78f)
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.45f),
                            glowColor.copy(alpha = 0.3f)
                        )
                    ),
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = label,
            style = LineaTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = LineaColors.TextPrimary
        )
    }
}

