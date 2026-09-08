@file:OptIn(ExperimentalFoundationApi::class)

package com.ryanshelby.linea.ui.screens.dialpad

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.PersonAdd
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ryanshelby.linea.telecom.SimAccountInfo
import com.ryanshelby.linea.ui.components.FrostedGlassBox
import com.ryanshelby.linea.ui.components.RoleBanner
import com.ryanshelby.linea.ui.screens.contacts.ContactCreateEditSheet
import com.ryanshelby.linea.ui.screens.contacts.ContactsViewModel
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
    viewModel: DialpadViewModel = hiltViewModel(),
    contactsViewModel: ContactsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val enteredNumber by viewModel.enteredNumber.collectAsState()
    val t9Matches by viewModel.t9Matches.collectAsState()
    val selectedSimIndex by viewModel.selectedSimIndex.collectAsState()
    val activeSims by viewModel.simAccounts.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val askSimBeforeDial by viewModel.askSimBeforeDial.collectAsState()
    val callConfirmationEnabled by viewModel.callConfirmationEnabled.collectAsState()
    val callCountdownSeconds by viewModel.callCountdownSeconds.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()
    val callerIdResult by viewModel.callerIdResult.collectAsState()

    var pendingCallNumber by remember { mutableStateOf<String?>(null) }
    var showCountdownDialog by remember { mutableStateOf(false) }
    var showSimSelectSheet by remember { mutableStateOf(false) }
    var showCreateContactSheet by remember { mutableStateOf(false) }
    val simSheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    val displayedSims = if (activeSims.isNotEmpty()) activeSims else simAccounts

    // Back gesture clears typed buffer before app exit or screen switch
    BackHandler(enabled = enteredNumber.isNotEmpty()) {
        viewModel.clearNumber()
    }

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
            .navigationBarsPadding()
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Profile Switcher Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(LineaColors.GlassFill)
                        .border(LineaDimensions.HairlineBorder, LineaColors.GlassBorder, RoundedCornerShape(16.dp))
                        .clickable { viewModel.switchProfile() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (activeProfile == "WORK") LineaColors.TitaniumBlue else LineaColors.MutedSageGreen)
                        )
                        Text(
                            text = activeProfile.lowercase().replaceFirstChar { it.uppercase() },
                            style = LineaTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = LineaColors.TextPrimary
                        )
                    }
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
                .height(60.dp)
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .combinedClickable(
                    onClick = {
                        if (enteredNumber.isEmpty()) {
                            val clipText = clipboardManager.getText()?.text?.trim()
                            if (!clipText.isNullOrEmpty() && clipText.any { it.isDigit() }) {
                                viewModel.setNumber(clipText.filter { it.isDigit() || it in "+*#" })
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onLongClick = {
                        if (enteredNumber.isNotEmpty()) {
                            clipboardManager.setText(AnnotatedString(enteredNumber))
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            Toast.makeText(context, "Copied $enteredNumber", Toast.LENGTH_SHORT).show()
                        } else {
                            val clipText = clipboardManager.getText()?.text?.trim()
                            if (!clipText.isNullOrEmpty() && clipText.any { it.isDigit() }) {
                                viewModel.setNumber(clipText.filter { it.isDigit() || it in "+*#" })
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (enteredNumber.isEmpty()) {
                val clipText = clipboardManager.getText()?.text?.trim()
                if (!clipText.isNullOrEmpty() && clipText.any { it.isDigit() }) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(LineaColors.GlassFill)
                            .border(LineaDimensions.HairlineBorder, LineaColors.GlassBorder, RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentPaste,
                            contentDescription = "Paste",
                            tint = LineaColors.TitaniumBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Paste ${clipText.take(16)}${if (clipText.length > 16) "…" else ""}",
                            style = LineaTypography.labelSmall,
                            color = LineaColors.TitaniumBlue
                        )
                    }
                }
            } else {
                Text(
                    text = enteredNumber,
                    style = LineaTypography.displayLarge.copy(
                        fontFeatureSettings = "tnum",
                        fontSize = if (enteredNumber.length > 12) 26.sp else 34.sp,
                        letterSpacing = 1.sp
                    ),
                    color = LineaColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Offline Caller ID Badge (Emergency / Toll-Free / Country / Region)
        if (callerIdResult != null) {
            val cid = callerIdResult!!
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (cid.isEmergency) LineaColors.Danger.copy(alpha = 0.2f) else LineaColors.GlassFill)
                    .border(
                        0.5.dp,
                        if (cid.isEmergency) LineaColors.Danger.copy(alpha = 0.5f) else LineaColors.GlassBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = cid.badgeLabel,
                        style = LineaTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        fontSize = 9.sp,
                        color = if (cid.isEmergency) LineaColors.Danger else LineaColors.TitaniumBlue
                    )
                    Text(
                        text = "•",
                        style = LineaTypography.labelSmall,
                        color = LineaColors.TextTertiary
                    )
                    Text(
                        text = cid.regionOrCountry,
                        style = LineaTypography.labelSmall,
                        fontSize = 10.sp,
                        color = LineaColors.TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Dialpad Grid (4 rows x 3 columns) with Speed Dial support
        val rows = listOf(
            listOf(
                Triple('1', "", { viewModel.onSpeedDialLongPress('1') }),
                Triple('2', "A B C", { viewModel.onSpeedDialLongPress('2') }),
                Triple('3', "D E F", { viewModel.onSpeedDialLongPress('3') })
            ),
            listOf(
                Triple('4', "G H I", { viewModel.onSpeedDialLongPress('4') }),
                Triple('5', "J K L", { viewModel.onSpeedDialLongPress('5') }),
                Triple('6', "M N O", { viewModel.onSpeedDialLongPress('6') })
            ),
            listOf(
                Triple('7', "P Q R S", { viewModel.onSpeedDialLongPress('7') }),
                Triple('8', "T U V", { viewModel.onSpeedDialLongPress('8') }),
                Triple('9', "W X Y Z", { viewModel.onSpeedDialLongPress('9') })
            ),
            listOf(
                Triple('*', "", null),
                Triple('0', "+", { viewModel.appendDigit('+') }),
                Triple('#', "", null)
            )
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
                            size = 68.dp,
                            soundEnabled = soundEnabled,
                            vibrationEnabled = vibrationEnabled
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Row: Add Contact / Call Button / Backspace Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Action: Add Contact button when number is entered, or balance spacer
            Box(
                modifier = Modifier.size(52.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = enteredNumber.isNotEmpty(),
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    AddContactButton(
                        onClick = {
                            contactsViewModel.openCreateSheet()
                            showCreateContactSheet = true
                        }
                    )
                }
            }

            // Call Button (Titanium Blue Gradient, 68dp)
            CallButton(
                onClick = {
                    if (enteredNumber.isNotEmpty()) {
                        initiateCall(enteredNumber)
                    }
                }
            )

            // Backspace / Clear Button
            Box(
                modifier = Modifier.size(52.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = enteredNumber.isNotEmpty(),
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
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

        Spacer(modifier = Modifier.height(108.dp)) // Comfortable clearance above floating bottom navbar
    }

    if (showCreateContactSheet) {
        val availableAccounts by contactsViewModel.availableAccounts.collectAsState()
        val selectedAccount by contactsViewModel.selectedContactAccount.collectAsState()

        ContactCreateEditSheet(
            initialNumbers = listOf(enteredNumber to "Mobile"),
            availableAccounts = availableAccounts,
            selectedAccount = selectedAccount,
            onSelectAccount = { contactsViewModel.selectContactAccount(it) },
            onDismiss = { showCreateContactSheet = false },
            onSave = { displayName, company, numbers, emails, preferredSimSlot, notes, photoUri, photoBytes ->
                contactsViewModel.saveContact(displayName, company, numbers, emails, preferredSimSlot, notes, photoUri, photoBytes)
                showCreateContactSheet = false
                Toast.makeText(context, "Saved $displayName", Toast.LENGTH_SHORT).show()
            }
        )
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

    val scale by animateFloatAsState(
        targetValue = if (isPressed && !reduceAnimations) 0.90f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "CallButtonScale"
    )

    val callGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF6E93B3), // Lighter titanium blue top highlight
            Color(0xFF4A6B88)  // Deep rich titanium blue
        )
    )

    Box(
        modifier = modifier
            .scale(scale)
            .size(68.dp)
            .clip(CircleShape)
            .background(callGradient)
            .border(
                1.5.dp,
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.10f)
                    )
                ),
                CircleShape
            )
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
            modifier = Modifier.size(30.dp)
        )
    }
}

@Composable
private fun AddContactButton(
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reduceAnimations = LocalReduceAnimations.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed && !reduceAnimations) 0.88f else 1.0f,
        label = "AddContactScale"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .size(52.dp)
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.03f)
                    )
                )
            )
            .border(
                LineaDimensions.HairlineBorder,
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.PersonAdd,
            contentDescription = "Add to Contacts",
            tint = LineaColors.TitaniumBlue,
            modifier = Modifier.size(22.dp)
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

    val scale by animateFloatAsState(
        targetValue = if (isPressed && !reduceAnimations) 0.88f else 1.0f,
        label = "BackspaceScale"
    )

    val bgBrush = if (isPressed) {
        Brush.verticalGradient(
            listOf(
                LineaColors.MutedBrickRed.copy(alpha = 0.35f),
                LineaColors.MutedBrickRed.copy(alpha = 0.15f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.03f)
            )
        )
    }

    val borderBrush = if (isPressed) {
        Brush.verticalGradient(
            listOf(
                LineaColors.MutedBrickRed.copy(alpha = 0.6f),
                LineaColors.MutedBrickRed.copy(alpha = 0.2f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.15f),
                Color.White.copy(alpha = 0.05f)
            )
        )
    }

    Box(
        modifier = Modifier
            .scale(scale)
            .size(52.dp)
            .clip(CircleShape)
            .background(bgBrush)
            .border(LineaDimensions.HairlineBorder, borderBrush, CircleShape)
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
            tint = if (isPressed) LineaColors.MutedBrickRed else LineaColors.TextSecondary,
            modifier = Modifier.size(22.dp)
        )
    }
}
