package com.ryanshelby.linea.ui.screens.settings.navbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.data.preferences.LineaPreferences
import com.ryanshelby.linea.ui.components.FrostedGlassBox
import com.ryanshelby.linea.ui.navigation.FloatingGlassNavBar
import com.ryanshelby.linea.ui.navigation.LineaDestination
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography
import kotlinx.coroutines.launch

@Composable
fun NavbarSettingsScreen(
    preferences: LineaPreferences,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val currentNavbarMode by preferences.navbarMode.collectAsState(
        initial = LineaPreferences.NavbarMode.COLLAPSED
    )
    val currentCircleSize by preferences.navbarCircleSize.collectAsState(
        initial = LineaPreferences.NavbarCircleSize.STANDARD
    )
    val currentAnimationStyle by preferences.navbarAnimationStyle.collectAsState(
        initial = LineaPreferences.NavbarAnimationStyle.BOUNCY
    )

    // Interactive Preview sandbox state
    var previewDestination by remember { mutableStateOf(LineaDestination.DIALPAD) }
    var isPreviewTemporarilyExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(LineaColors.BackgroundTop, LineaColors.BackgroundBottom)
                )
            )
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = LineaDimensions.ScreenPadding)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Top Navigation Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate Back",
                        tint = LineaColors.TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Navigation Bar",
                        style = LineaTypography.headlineLarge,
                        color = LineaColors.TextPrimary
                    )
                    Text(
                        text = "Customize layout style, button size & physics",
                        style = LineaTypography.bodyMedium,
                        color = LineaColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Interactive Live Preview Card
            FrostedGlassBox(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(LineaColors.EmeraldPrimary)
                            )
                            Text(
                                text = "INTERACTIVE SANDBOX",
                                style = LineaTypography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = LineaColors.EmeraldPrimary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(LineaColors.TitaniumBlue.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (currentNavbarMode == LineaPreferences.NavbarMode.COLLAPSED) {
                                    "${currentCircleSize}dp • ${currentAnimationStyle}"
                                } else {
                                    "EXPANDED • ${currentAnimationStyle}"
                                },
                                style = LineaTypography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = LineaColors.TitaniumBlue
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (currentNavbarMode == LineaPreferences.NavbarMode.COLLAPSED) {
                            "Tap circle to test morphing or drag in any direction"
                        } else {
                            "Tap any destination icon to test selection physics"
                        },
                        style = LineaTypography.bodySmall,
                        color = LineaColors.TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Embedded live interactive navbar sandbox
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0C0E11))
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        FloatingGlassNavBar(
                            currentDestination = previewDestination,
                            onNavigate = { previewDestination = it },
                            unreadMissedCalls = 2,
                            persistentNavbarMode = currentNavbarMode,
                            circleSizeDp = currentCircleSize,
                            animationStyle = currentAnimationStyle,
                            onTogglePersistentNavbarMode = {
                                scope.launch {
                                    val newMode = if (currentNavbarMode == LineaPreferences.NavbarMode.EXPANDED) {
                                        LineaPreferences.NavbarMode.COLLAPSED
                                    } else {
                                        LineaPreferences.NavbarMode.EXPANDED
                                    }
                                    preferences.setNavbarMode(newMode)
                                }
                            },
                            isTemporarilyExpanded = isPreviewTemporarilyExpanded,
                            onSetTemporarilyExpanded = { isPreviewTemporarilyExpanded = it }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 1: NAVBAR STYLE
            Text(
                text = "NAVBAR STYLE",
                style = LineaTypography.labelMedium.copy(letterSpacing = 1.2.sp),
                color = LineaColors.TextTertiary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Option 1: Floating Circle (Default)
            NavbarStyleCard(
                title = "Floating Circle",
                subtitle = "Compact floating circular button with tap-expansion, 5-second auto-collapse, and 4-way gesture flick",
                badge = "DEFAULT",
                icon = Icons.Filled.RadioButtonChecked,
                isSelected = currentNavbarMode == LineaPreferences.NavbarMode.COLLAPSED,
                onSelect = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch {
                        preferences.setNavbarMode(LineaPreferences.NavbarMode.COLLAPSED)
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Option 2: Expanded Pill Bar
            NavbarStyleCard(
                title = "Expanded Pill Bar",
                subtitle = "Permanent full-width frosted glass pill bar with instant direct one-tap tab switches",
                badge = "EXPANDED",
                icon = Icons.Filled.ViewStream,
                isSelected = currentNavbarMode == LineaPreferences.NavbarMode.EXPANDED,
                onSelect = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch {
                        preferences.setNavbarMode(LineaPreferences.NavbarMode.EXPANDED)
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 2: CIRCLE BUTTON SIZE
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "CIRCLE BUTTON SIZE",
                    style = LineaTypography.labelMedium.copy(letterSpacing = 1.2.sp),
                    color = LineaColors.TextTertiary
                )
                if (currentNavbarMode == LineaPreferences.NavbarMode.EXPANDED) {
                    Text(
                        text = "Applies to Circle mode",
                        style = LineaTypography.bodySmall.copy(fontSize = 11.sp),
                        color = LineaColors.TextTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            FrostedGlassBox(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Adjust the physical diameter and thumb touch footprint of the collapsed floating button.",
                        style = LineaTypography.bodySmall,
                        color = LineaColors.TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val sizes = listOf(
                            Triple(LineaPreferences.NavbarCircleSize.COMPACT, "48 dp", "Compact"),
                            Triple(LineaPreferences.NavbarCircleSize.STANDARD, "56 dp", "Standard (Def)"),
                            Triple(LineaPreferences.NavbarCircleSize.COMFORTABLE, "64 dp", "Comfortable"),
                            Triple(LineaPreferences.NavbarCircleSize.LARGE, "72 dp", "Large")
                        )

                        sizes.forEach { (sizeDp, label, sublabel) ->
                            val isSelected = currentCircleSize == sizeDp
                            val borderColor by animateColorAsState(
                                targetValue = if (isSelected) LineaColors.TitaniumBlue else LineaColors.GlassBorder,
                                label = "size_border_color"
                            )
                            val bgColor by animateColorAsState(
                                targetValue = if (isSelected) LineaColors.TitaniumBlue.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.04f),
                                label = "size_bg_color"
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(bgColor)
                                    .border(if (isSelected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        scope.launch {
                                            preferences.setNavbarCircleSize(sizeDp)
                                        }
                                    }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Visual size indicator dot
                                    Box(
                                        modifier = Modifier
                                            .size((sizeDp / 3.8f).dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) LineaColors.TitaniumBlue else LineaColors.TextTertiary
                                            )
                                    )
                                    Text(
                                        text = label,
                                        style = LineaTypography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp
                                        ),
                                        color = if (isSelected) LineaColors.TitaniumBlue else LineaColors.TextPrimary
                                    )
                                    Text(
                                        text = sublabel,
                                        style = LineaTypography.bodySmall.copy(fontSize = 9.sp),
                                        color = if (isSelected) LineaColors.TextPrimary else LineaColors.TextTertiary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 3: ANIMATION & MOTION DYNAMICS
            Text(
                text = "ANIMATION & MOTION PHYSICS",
                style = LineaTypography.labelMedium.copy(letterSpacing = 1.2.sp),
                color = LineaColors.TextTertiary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Option 1: Bouncy Spring
            NavbarAnimationCard(
                title = "Bouncy Spring",
                physicsSpec = "Damping 0.68 • Stiffness 320",
                description = "Expressive organic spring with natural momentum and subtle rebound upon morphing",
                badge = "DEFAULT",
                icon = Icons.Filled.Animation,
                isSelected = currentAnimationStyle == LineaPreferences.NavbarAnimationStyle.BOUNCY,
                onSelect = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch {
                        preferences.setNavbarAnimationStyle(LineaPreferences.NavbarAnimationStyle.BOUNCY)
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Option 2: Smooth Fluid
            NavbarAnimationCard(
                title = "Smooth Fluid",
                physicsSpec = "Damping 0.88 • Stiffness 220",
                description = "Gentle cushioned spring motion with minimal bounce for refined and calm elegance",
                badge = "CUSHIONED",
                icon = Icons.Filled.AutoAwesome,
                isSelected = currentAnimationStyle == LineaPreferences.NavbarAnimationStyle.SMOOTH,
                onSelect = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch {
                        preferences.setNavbarAnimationStyle(LineaPreferences.NavbarAnimationStyle.SMOOTH)
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Option 3: Snappy Quick
            NavbarAnimationCard(
                title = "Snappy Quick",
                physicsSpec = "Damping 0.58 • Stiffness 550",
                description = "High-tension crisp response with instant expansion and zero perceptual latency",
                badge = "FAST",
                icon = Icons.Filled.Speed,
                isSelected = currentAnimationStyle == LineaPreferences.NavbarAnimationStyle.SNAPPY,
                onSelect = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch {
                        preferences.setNavbarAnimationStyle(LineaPreferences.NavbarAnimationStyle.SNAPPY)
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Option 4: Minimal Fade
            NavbarAnimationCard(
                title = "Minimal Fade",
                physicsSpec = "FastOutSlowInEasing • 240ms",
                description = "Standard geometric ease with smooth cross-fade and no bouncy morph physics",
                badge = "SUBTLE",
                icon = Icons.Filled.TouchApp,
                isSelected = currentAnimationStyle == LineaPreferences.NavbarAnimationStyle.FADE,
                onSelect = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch {
                        preferences.setNavbarAnimationStyle(LineaPreferences.NavbarAnimationStyle.FADE)
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 4: GESTURES & SHORTCUTS GUIDE
            FrostedGlassBox(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Gesture,
                            contentDescription = null,
                            tint = LineaColors.TitaniumBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Gesture Quick Reference",
                            style = LineaTypography.titleMedium,
                            color = LineaColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    GestureRow(
                        gesture = "Single Tap",
                        detail = "Expands the circle into the navigation bar for 5 seconds of active usage"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    GestureRow(
                        gesture = "Hold & Drag",
                        detail = "Flick in 4 directions to jump directly: Left (Keypad), Up (Recent), Down (Contacts), Right (Settings)"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    GestureRow(
                        gesture = "Triple Tap",
                        detail = "Quickly toggles persistent pinning between Floating Circle and Expanded Pill bar"
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

@Composable
private fun NavbarStyleCard(
    title: String,
    subtitle: String,
    badge: String,
    icon: ImageVector,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) LineaColors.TitaniumBlue else LineaColors.GlassBorder,
        animationSpec = tween(150),
        label = "style_card_border"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) LineaColors.TitaniumBlue.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f),
        animationSpec = tween(150),
        label = "style_card_bg"
    )

    FrostedGlassBox(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(18.dp),
        borderWidth = if (isSelected) 1.5.dp else LineaDimensions.HairlineBorder,
        borderColor = borderColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor)
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) LineaColors.TitaniumBlue.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)
                    )
                    .border(
                        1.dp,
                        if (isSelected) LineaColors.TitaniumBlue.copy(alpha = 0.5f) else Color.Transparent,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) LineaColors.TitaniumBlue else LineaColors.TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = LineaTypography.titleMedium,
                        color = LineaColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isSelected) LineaColors.TitaniumBlue.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge,
                            style = LineaTypography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = if (isSelected) LineaColors.TitaniumBlue else LineaColors.TextSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = LineaTypography.bodySmall,
                    color = LineaColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) LineaColors.TitaniumBlue else Color.Transparent
                    )
                    .border(
                        1.5.dp,
                        if (isSelected) LineaColors.TitaniumBlue else LineaColors.TextTertiary.copy(alpha = 0.4f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Selected",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NavbarAnimationCard(
    title: String,
    physicsSpec: String,
    description: String,
    badge: String,
    icon: ImageVector,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) LineaColors.TitaniumBlue else LineaColors.GlassBorder,
        animationSpec = tween(150),
        label = "anim_card_border"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) LineaColors.TitaniumBlue.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f),
        animationSpec = tween(150),
        label = "anim_card_bg"
    )

    FrostedGlassBox(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(18.dp),
        borderWidth = if (isSelected) 1.5.dp else LineaDimensions.HairlineBorder,
        borderColor = borderColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) LineaColors.TitaniumBlue.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)
                    )
                    .border(
                        1.dp,
                        if (isSelected) LineaColors.TitaniumBlue.copy(alpha = 0.5f) else Color.Transparent,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) LineaColors.TitaniumBlue else LineaColors.TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = LineaTypography.titleMedium,
                        color = LineaColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isSelected) LineaColors.TitaniumBlue.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge,
                            style = LineaTypography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = if (isSelected) LineaColors.TitaniumBlue else LineaColors.TextSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = physicsSpec,
                    style = LineaTypography.labelSmall.copy(fontSize = 11.sp),
                    color = if (isSelected) LineaColors.TitaniumBlue else LineaColors.TextTertiary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = LineaTypography.bodySmall,
                    color = LineaColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) LineaColors.TitaniumBlue else Color.Transparent
                    )
                    .border(
                        1.5.dp,
                        if (isSelected) LineaColors.TitaniumBlue else LineaColors.TextTertiary.copy(alpha = 0.4f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Selected",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GestureRow(
    gesture: String,
    detail: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(LineaColors.TitaniumBlue)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = gesture,
                style = LineaTypography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = LineaColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = detail,
                style = LineaTypography.bodySmall,
                color = LineaColors.TextSecondary
            )
        }
    }
}
