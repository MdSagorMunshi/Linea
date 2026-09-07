@file:OptIn(ExperimentalFoundationApi::class)

package com.ryanshelby.linea.ui.screens.dialpad

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ryanshelby.linea.telecom.SimAccountInfo
import com.ryanshelby.linea.ui.components.FrostedGlassBox
import com.ryanshelby.linea.ui.components.RoleBanner
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography
import com.ryanshelby.linea.ui.theme.LocalReduceAnimations

@Composable
fun DialpadScreen(
    isDefaultDialer: Boolean,
    simAccounts: List<SimAccountInfo>,
    onRequestDefaultDialer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DialpadViewModel = hiltViewModel()
) {
    val enteredNumber by viewModel.enteredNumber.collectAsState()
    val t9Matches by viewModel.t9Matches.collectAsState()
    val selectedSimIndex by viewModel.selectedSimIndex.collectAsState()
    val activeSims by viewModel.simAccounts.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val askSimBeforeDial by viewModel.askSimBeforeDial.collectAsState()
    val callConfirmationEnabled by viewModel.callConfirmationEnabled.collectAsState()
    val callCountdownSeconds by viewModel.callCountdownSeconds.collectAsState()

    var pendingCallNumber by remember { mutableStateOf<String?>(null) }
    var showCountdownDialog by remember { mutableStateOf(false) }
    var showSimSelectSheet by remember { mutableStateOf(false) }
    val simSheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    val displayedSims = if (activeSims.isNotEmpty()) activeSims else simAccounts

    val initiateCall = { number: String ->
        pendingCallNumber = number
        if (askSimBeforeDial && displayedSims.size > 1) {
            showSimSelectSheet = true
        } else if (callConfirmationEnabled) {
            showCountdownDialog = true
        } else {
            viewModel.placeCall(number)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = LineaDimensions.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "LINEA",
                    style = LineaTypography.titleLarge,
                    color = LineaColors.TextPrimary
                )
                Text(
                    text = "Cellular Dial Pad",
                    style = LineaTypography.bodySmall,
                    color = LineaColors.TitaniumBlue
                )
            }

            // SIM Selector Pill
            if (displayedSims.isNotEmpty()) {
                SimSelectorPill(
                    sims = displayedSims,
                    selectedIndex = selectedSimIndex,
                    onSelectSim = { viewModel.selectSim(it) }
                )
            }
        }

        // Show Default Dialer Banner if not set
        if (!isDefaultDialer) {
            Spacer(modifier = Modifier.height(10.dp))
            RoleBanner(
                isDefaultDialer = false,
                onRequestRole = onRequestDefaultDialer
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Live T9 Contact Match List (expands smoothly when typing digits)
        T9MatchList(
            matches = t9Matches,
            onSelectContact = { number ->
                viewModel.setNumber(number)
            },
            onDirectCall = { number ->
                initiateCall(number)
            },
            modifier = Modifier.padding(bottom = 6.dp)
        )

        // Number Display Area (tabular figures, copy/paste support)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = enteredNumber.ifEmpty { " " },
                style = LineaTypography.displayLarge.copy(
                    fontFeatureSettings = "tnum",
                    fontSize = if (enteredNumber.length > 12) 26.sp else 34.sp
                ),
                color = LineaColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Dialpad Grid (4 rows x 3 columns)
        val rows = listOf(
            listOf(Triple('1', "", null), Triple('2', "A B C", null), Triple('3', "D E F", null)),
            listOf(Triple('4', "G H I", null), Triple('5', "J K L", null), Triple('6', "M N O", null)),
            listOf(Triple('7', "P Q R S", null), Triple('8', "T U V", null), Triple('9', "W X Y Z", null)),
            listOf(
                Triple('*', "", null),
                Triple('0', "+", { viewModel.appendDigit('+') }),
                Triple('#', "", null)
            )
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { (digit, sub, longPress) ->
                        DialpadKey(
                            digit = digit,
                            subLetters = sub,
                            onDigitPress = { d -> viewModel.appendDigit(d) },
                            onLongPress = longPress,
                            soundEnabled = soundEnabled,
                            vibrationEnabled = vibrationEnabled
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Row: Left Spacer / Call Button / Backspace Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Action / Balance Spacer
            Box(modifier = Modifier.size(54.dp))

            // Call Button (Titanium Blue, 72dp)
            CallButton(
                onClick = {
                    if (enteredNumber.isNotEmpty()) {
                        initiateCall(enteredNumber)
                    }
                }
            )

            // Backspace / Clear Button
            Box(
                modifier = Modifier.size(54.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = enteredNumber.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    BackspaceButton(
                        onTap = { viewModel.deleteLastDigit() },
                        onLongPress = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.clearNumber()
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(96.dp)) // Space for floating bottom navigation bar
    }

    if (showSimSelectSheet) {
        SimSelectSheet(
            sheetState = simSheetState,
            phoneNumber = pendingCallNumber ?: enteredNumber,
            accounts = displayedSims,
            onSelectSim = { slot ->
                viewModel.selectSim(slot)
                showSimSelectSheet = false
                if (callConfirmationEnabled) {
                    showCountdownDialog = true
                } else {
                    viewModel.placeCall(pendingCallNumber)
                }
            },
            onDismiss = { showSimSelectSheet = false }
        )
    }

    if (showCountdownDialog) {
        CallCountdownDialog(
            phoneNumber = pendingCallNumber ?: enteredNumber,
            totalSeconds = callCountdownSeconds,
            onConfirmCall = {
                showCountdownDialog = false
                viewModel.placeCall(pendingCallNumber)
            },
            onCancel = { showCountdownDialog = false }
        )
    }
}

@Composable
private fun SimSelectorPill(
    sims: List<SimAccountInfo>,
    selectedIndex: Int,
    onSelectSim: (Int) -> Unit
) {
    FrostedGlassBox(
        shape = RoundedCornerShape(16.dp),
        borderColor = LineaColors.GlassBorder
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            sims.forEachIndexed { index, sim ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) LineaColors.TitaniumBlue else Color.Transparent)
                        .clickable { onSelectSim(index) }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.SimCard,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else LineaColors.TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "SIM ${sim.slotIndex + 1}",
                            style = LineaTypography.labelSmall,
                            color = if (isSelected) Color.White else LineaColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CallButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reduceAnimations = LocalReduceAnimations.current

    val scale = if (isPressed && !reduceAnimations) 0.90f else 1.0f

    Box(
        modifier = modifier
            .scale(scale)
            .size(72.dp)
            .clip(CircleShape)
            .background(LineaColors.TitaniumBlue)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Call,
            contentDescription = "Call",
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BackspaceButton(
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reduceAnimations = LocalReduceAnimations.current

    val scale = if (isPressed && !reduceAnimations) 0.88f else 1.0f

    Box(
        modifier = Modifier
            .scale(scale)
            .size(48.dp)
            .clip(CircleShape)
            .background(LineaColors.GlassFill)
            .border(LineaDimensions.HairlineBorder, LineaColors.GlassBorder, CircleShape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onTap,
                onLongClick = onLongPress
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Backspace,
            contentDescription = "Backspace",
            tint = LineaColors.TextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}
