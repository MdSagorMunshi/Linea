package com.ryanshelby.linea.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.data.preferences.LineaPreferences
import com.ryanshelby.linea.ui.components.FrostedGlassBox
import com.ryanshelby.linea.ui.components.neumorphic
import com.ryanshelby.linea.ui.theme.InterFontFamily
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaMotion
import com.ryanshelby.linea.ui.theme.LocalReduceAnimations
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun FloatingGlassNavBar(
    currentDestination: LineaDestination,
    onNavigate: (LineaDestination) -> Unit,
    unreadMissedCalls: Int = 0,
    persistentNavbarMode: String = LineaPreferences.NavbarMode.COLLAPSED,
    onTogglePersistentNavbarMode: () -> Unit = {},
    isTemporarilyExpanded: Boolean = false,
    onSetTemporarilyExpanded: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val reduceAnimations = LocalReduceAnimations.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    val isExpanded = (persistentNavbarMode == LineaPreferences.NavbarMode.EXPANDED) || isTemporarilyExpanded

    // Dimensions: Collapsed = 56.dp circle; Expanded = full pill width (~340.dp) and 66.dp height
    val screenWidth = configuration.screenWidthDp.dp
    val expandedTargetWidth = (screenWidth - 40.dp).coerceAtLeast(300.dp).coerceAtMost(430.dp)
    val collapsedTargetSize = 56.dp

    val navWidth by animateDpAsState(
        targetValue = if (isExpanded) expandedTargetWidth else collapsedTargetSize,
        animationSpec = LineaMotion.springOrFade(
            reduceAnimations,
            spring(dampingRatio = 0.68f, stiffness = 320f)
        ),
        label = "navbar_morph_width"
    )

    val navHeight by animateDpAsState(
        targetValue = if (isExpanded) 66.dp else collapsedTargetSize,
        animationSpec = LineaMotion.springOrFade(
            reduceAnimations,
            spring(dampingRatio = 0.68f, stiffness = 320f)
        ),
        label = "navbar_morph_height"
    )

    // Press-and-drag gesture state on collapsed circle
    var isHolding by remember { mutableStateOf(false) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var candidateDestination by remember { mutableStateOf<LineaDestination?>(null) }

    // Directional confirmation stretch animation
    val stretchOffsetX = remember { Animatable(0f) }
    val stretchOffsetY = remember { Animatable(0f) }
    val stretchScaleX = remember { Animatable(1f) }
    val stretchScaleY = remember { Animatable(1f) }

    // Hold scale & glow animations
    val holdScale by animateFloatAsState(
        targetValue = if (isHolding) 1.14f else 1.0f,
        animationSpec = LineaMotion.springOrFade(reduceAnimations, LineaMotion.ScaleCompressSpring),
        label = "hold_scale"
    )

    val holdGlowAlpha by animateFloatAsState(
        targetValue = if (isHolding) 0.85f else 0.0f,
        animationSpec = tween(durationMillis = if (reduceAnimations) 80 else 180),
        label = "hold_glow"
    )

    // Tap tracking for single-tap vs triple-tap on collapsed circle
    var circleTapCount by remember { mutableIntStateOf(0) }
    var circleLastTapTime by remember { mutableLongStateOf(0L) }
    var singleTapJob by remember { mutableStateOf<Job?>(null) }

    // Tap tracking on expanded pill for triple-tap persistent collapse
    var pillTapCount by remember { mutableIntStateOf(0) }
    var pillLastTapTime by remember { mutableLongStateOf(0L) }

    val dragThresholdPx = with(density) { 36.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer soft glow when hold is active
        if (holdGlowAlpha > 0.01f && !isExpanded) {
            Box(
                modifier = Modifier
                    .size(collapsedTargetSize + 14.dp)
                    .scale(holdScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                LineaColors.TitaniumBlue.copy(alpha = 0.5f * holdGlowAlpha),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Main morphing Neumorphic container
        Box(
            modifier = Modifier
                .width(navWidth)
                .height(navHeight)
                .graphicsLayer {
                    translationX = stretchOffsetX.value + dragOffsetX * 0.35f
                    translationY = stretchOffsetY.value + dragOffsetY * 0.35f
                    scaleX = holdScale * stretchScaleX.value
                    scaleY = holdScale * stretchScaleY.value
                }
                .then(
                    if (!isExpanded) {
                        Modifier.pointerInput(persistentNavbarMode, isExpanded) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val downTime = System.currentTimeMillis()
                                val startPosition = down.position
                                var isDragGesture = false
                                var currentCandidate: LineaDestination? = null

                                // Launch hold detector after 160ms if still down
                                val holdJob = coroutineScope.launch {
                                    delay(160)
                                    isHolding = true
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val currentChange = event.changes.firstOrNull() ?: break

                                    if (currentChange.pressed) {
                                        val totalDx = currentChange.position.x - startPosition.x
                                        val totalDy = currentChange.position.y - startPosition.y
                                        val dist = hypot(totalDx, totalDy)

                                        if (dist > 12f) {
                                            isDragGesture = true
                                        }

                                        if (isHolding) {
                                            dragOffsetX = totalDx.coerceIn(-60f, 60f)
                                            dragOffsetY = totalDy.coerceIn(-60f, 60f)

                                            if (dist >= dragThresholdPx) {
                                                // Directional mapping:
                                                // Left (-X): Dial Pad
                                                // Up (-Y): Recent (History)
                                                // Down (+Y): Contacts
                                                // Right (+X): Settings
                                                currentCandidate = if (abs(totalDx) > abs(totalDy)) {
                                                    if (totalDx < 0) LineaDestination.DIALPAD else LineaDestination.SETTINGS
                                                } else {
                                                    if (totalDy < 0) LineaDestination.HISTORY else LineaDestination.CONTACTS
                                                }
                                                candidateDestination = currentCandidate
                                            } else {
                                                currentCandidate = null
                                                candidateDestination = null
                                            }
                                        }
                                    } else {
                                        // Pointer lifted (up)
                                        holdJob.cancel()
                                        val totalDx = currentChange.position.x - startPosition.x
                                        val totalDy = currentChange.position.y - startPosition.y
                                        val dist = hypot(totalDx, totalDy)

                                        val wasHoldingWithDirection = isHolding && dist >= dragThresholdPx && currentCandidate != null
                                        val selectedDest = currentCandidate

                                        // Reset hold & drag state
                                        isHolding = false
                                        dragOffsetX = 0f
                                        dragOffsetY = 0f
                                        candidateDestination = null

                                        if (wasHoldingWithDirection && selectedDest != null) {
                                            val dest = selectedDest
                                            // Trigger directional navigation
                                            onNavigate(dest)
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                                            // Confirming directional stretch animation
                                            coroutineScope.launch {
                                                val stretchSignX = if (selectedDest == LineaDestination.DIALPAD) -1f else if (selectedDest == LineaDestination.SETTINGS) 1f else 0f
                                                val stretchSignY = if (selectedDest == LineaDestination.HISTORY) -1f else if (selectedDest == LineaDestination.CONTACTS) 1f else 0f

                                                stretchOffsetX.snapTo(stretchSignX * 18f)
                                                stretchOffsetY.snapTo(stretchSignY * 18f)
                                                stretchScaleX.snapTo(if (stretchSignX != 0f) 1.28f else 0.90f)
                                                stretchScaleY.snapTo(if (stretchSignY != 0f) 1.28f else 0.90f)

                                                launch {
                                                    stretchOffsetX.animateTo(
                                                        0f,
                                                        spring(dampingRatio = 0.62f, stiffness = 380f)
                                                    )
                                                }
                                                launch {
                                                    stretchOffsetY.animateTo(
                                                        0f,
                                                        spring(dampingRatio = 0.62f, stiffness = 380f)
                                                    )
                                                }
                                                launch {
                                                    stretchScaleX.animateTo(
                                                        1f,
                                                        spring(dampingRatio = 0.62f, stiffness = 380f)
                                                    )
                                                }
                                                launch {
                                                    stretchScaleY.animateTo(
                                                        1f,
                                                        spring(dampingRatio = 0.62f, stiffness = 380f)
                                                    )
                                                }
                                            }
                                        } else if (!isDragGesture) {
                                            // Handle Tap on circle (Single tap vs Triple tap)
                                            val now = System.currentTimeMillis()
                                            if (now - circleLastTapTime < 360L) {
                                                circleTapCount++
                                            } else {
                                                circleTapCount = 1
                                            }
                                            circleLastTapTime = now

                                            if (circleTapCount >= 3) {
                                                // Triple Tap: Toggle persistent shape mode to EXPANDED!
                                                singleTapJob?.cancel()
                                                circleTapCount = 0
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onTogglePersistentNavbarMode()
                                            } else if (circleTapCount == 1) {
                                                // Wait briefly to distinguish single tap from triple tap
                                                singleTapJob?.cancel()
                                                singleTapJob = coroutineScope.launch {
                                                    delay(250)
                                                    if (circleTapCount < 3) {
                                                        circleTapCount = 0
                                                        onSetTemporarilyExpanded(true)
                                                    }
                                                }
                                            }
                                        }
                                        break
                                    }
                                }
                            }
                        }
                    } else {
                        // Expanded pill gesture listener for triple-tap persistent collapse toggle
                        Modifier.pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val now = System.currentTimeMillis()
                                if (now - pillLastTapTime < 380L) {
                                    pillTapCount++
                                } else {
                                    pillTapCount = 1
                                }
                                pillLastTapTime = now

                                if (pillTapCount >= 3) {
                                    pillTapCount = 0
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onTogglePersistentNavbarMode()
                                }
                            }
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            // Tactile Neumorphic surface with dual directional shadows
            FrostedGlassBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(navHeight),
                shape = RoundedCornerShape(28.dp),
                borderWidth = if (isHolding) 1.5.dp else LineaDimensions.HairlineBorder,
                borderColor = if (isHolding) {
                    LineaColors.TitaniumBlue.copy(alpha = 0.85f)
                } else {
                    LineaColors.GlassBorder
                },
                fillColor = Color.White.copy(alpha = 0.08f),
                fallbackFillColor = Color(0xFF14171A)
            ) {
                if (!isExpanded) {
                    // ==========================================
                    // Collapsed State: Single active section icon
                    // ==========================================
                    Box(
                        modifier = Modifier
                            .size(collapsedTargetSize),
                        contentAlignment = Alignment.Center
                    ) {
                        val activeIcon = when (candidateDestination ?: currentDestination) {
                            LineaDestination.DIALPAD -> LineaDestination.DIALPAD.filledIcon
                            LineaDestination.HISTORY -> LineaDestination.HISTORY.filledIcon
                            LineaDestination.CONTACTS -> LineaDestination.CONTACTS.filledIcon
                            LineaDestination.SETTINGS -> LineaDestination.SETTINGS.filledIcon
                        }

                        Icon(
                            painter = painterResource(id = activeIcon),
                            contentDescription = currentDestination.title,
                            tint = LineaColors.TitaniumBlue,
                            modifier = Modifier
                                .size(26.dp)
                                .scale(if (isHolding) 1.08f else 1.0f)
                        )

                        // If Recent has unread calls and active destination is Recent, or subtle indicator
                        if (currentDestination == LineaDestination.HISTORY && unreadMissedCalls > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 10.dp, end = 10.dp)
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(LineaColors.CarmineRed),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (unreadMissedCalls > 9) "9+" else "$unreadMissedCalls",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LineaColors.TextPrimary
                                )
                            }
                        } else if (unreadMissedCalls > 0) {
                            // Subtle red dot indicator when not on Recent so user knows calls are waiting
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 12.dp, end = 12.dp)
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(LineaColors.CarmineRed)
                            )
                        }

                        // Directional hint dots during hold
                        if (isHolding) {
                            // Top: Recent hint
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 3.dp)
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (candidateDestination == LineaDestination.HISTORY) LineaColors.TitaniumBlue else Color.White.copy(alpha = 0.35f)
                                    )
                            )
                            // Bottom: Contacts hint
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 3.dp)
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (candidateDestination == LineaDestination.CONTACTS) LineaColors.TitaniumBlue else Color.White.copy(alpha = 0.35f)
                                    )
                            )
                            // Left: Dial Pad hint
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 3.dp)
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (candidateDestination == LineaDestination.DIALPAD) LineaColors.TitaniumBlue else Color.White.copy(alpha = 0.35f)
                                    )
                            )
                            // Right: Settings hint
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 3.dp)
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (candidateDestination == LineaDestination.SETTINGS) LineaColors.TitaniumBlue else Color.White.copy(alpha = 0.35f)
                                    )
                            )
                        }
                    }
                } else {
                    // ==========================================
                    // Expanded State: All four destinations side by side
                    // ==========================================
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LineaDestination.entries.forEach { destination ->
                            val selected = destination == currentDestination

                            val iconScale by animateFloatAsState(
                                targetValue = if (selected) 1.15f else 1.0f,
                                animationSpec = LineaMotion.springOrFade(
                                    reduceAnimations,
                                    LineaMotion.ScaleCompressSpring
                                ),
                                label = "nav_icon_scale"
                            )

                            val tintColor by animateColorAsState(
                                targetValue = if (selected) LineaColors.TitaniumBlue else LineaColors.TextSecondary,
                                animationSpec = tween(durationMillis = if (reduceAnimations) 100 else 200),
                                label = "nav_tint_color"
                            )

                            val interactionSource = remember { MutableInteractionSource() }

                            Box(
                                modifier = Modifier
                                    .then(
                                        if (selected) {
                                            Modifier.neumorphic(
                                                shape = RoundedCornerShape(14.dp),
                                                elevation = 2.dp,
                                                isSunken = true,
                                                surfaceColor = LineaColors.NeuSurfaceSunken
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null
                                    ) {
                                        if (!selected) {
                                            onNavigate(destination)
                                        } else if (persistentNavbarMode == LineaPreferences.NavbarMode.COLLAPSED) {
                                            // Tapping the active item while temporarily expanded collapses back down
                                            onSetTemporarilyExpanded(false)
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box {
                                        Icon(
                                            painter = painterResource(
                                                id = if (selected) destination.filledIcon else destination.outlineIcon
                                            ),
                                            contentDescription = destination.title,
                                            tint = tintColor,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .scale(iconScale)
                                        )
                                        if (destination == LineaDestination.HISTORY && unreadMissedCalls > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .size(14.dp)
                                                    .clip(CircleShape)
                                                    .background(LineaColors.CarmineRed),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = if (unreadMissedCalls > 9) "9+" else "$unreadMissedCalls",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = LineaColors.TextPrimary
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = destination.title,
                                        fontFamily = InterFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = tintColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

