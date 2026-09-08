package com.ryanshelby.linea.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ryanshelby.linea.security.PrivateVaultSecurityManager
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class PrivatePinDialogMode {
    UNLOCK,
    SETUP_ENTER_PIN,
    SETUP_CONFIRM_PIN,
    SETUP_RECOVERY_QUESTION,
    RECOVER_FORGOT_PIN,
    CHANGE_OLD_PIN,
    CHANGE_NEW_PIN,
    CHANGE_CONFIRM_PIN
}

val PRESET_RECOVERY_QUESTIONS = listOf(
    "What was the name of your first pet?",
    "In what city or town were you born?",
    "What was your childhood nickname?",
    "What was the make and model of your first car?",
    "What is your mother's maiden name?",
    "What was the name of your first elementary school?",
    "Custom Question"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivatePinDialog(
    vaultSecurityManager: PrivateVaultSecurityManager,
    isChangePinMode: Boolean = false,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var hasPinSet by remember { mutableStateOf<Boolean?>(null) }
    var currentMode by remember { mutableStateOf(PrivatePinDialogMode.UNLOCK) }

    var enteredPin by remember { mutableStateOf("") }
    var tempNewPin by remember { mutableStateOf("") }
    var tempOldPin by remember { mutableStateOf("") }

    var selectedQuestion by remember { mutableStateOf(PRESET_RECOVERY_QUESTIONS[0]) }
    var customQuestionText by remember { mutableStateOf("") }
    var recoveryAnswerText by remember { mutableStateOf("") }
    var storedQuestion by remember { mutableStateOf<String?>(null) }
    var isQuestionDropdownOpen by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lockoutSecondsRemaining by remember { mutableLongStateOf(0L) }

    // Initial check: is PIN configured?
    LaunchedEffect(Unit) {
        val pinSet = vaultSecurityManager.hasPinSet()
        hasPinSet = pinSet
        storedQuestion = vaultSecurityManager.getRecoveryQuestion()
        lockoutSecondsRemaining = vaultSecurityManager.getLockoutRemainingSeconds()

        if (isChangePinMode) {
            currentMode = PrivatePinDialogMode.CHANGE_OLD_PIN
        } else if (!pinSet) {
            currentMode = PrivatePinDialogMode.SETUP_ENTER_PIN
        } else {
            currentMode = PrivatePinDialogMode.UNLOCK
        }
    }

    // Lockout countdown timer
    LaunchedEffect(lockoutSecondsRemaining) {
        while (lockoutSecondsRemaining > 0) {
            delay(1000L)
            lockoutSecondsRemaining--
        }
    }

    fun submitPin(pin: String) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        when (currentMode) {
            PrivatePinDialogMode.UNLOCK -> {
                coroutineScope.launch {
                    val result = vaultSecurityManager.unlockWithPin(pin)
                    if (result.isSuccess) {
                        onSuccess()
                    } else {
                        val lockout = vaultSecurityManager.getLockoutRemainingSeconds()
                        if (lockout > 0) {
                            lockoutSecondsRemaining = lockout
                            errorMessage = "Too many failed attempts. Locked out for ${lockout}s."
                        } else {
                            val attempts = vaultSecurityManager.getFailedAttempts()
                            errorMessage = "Incorrect PIN. (Attempt $attempts/5)"
                        }
                        enteredPin = ""
                    }
                }
            }
            PrivatePinDialogMode.SETUP_ENTER_PIN -> {
                tempNewPin = pin
                enteredPin = ""
                errorMessage = null
                currentMode = PrivatePinDialogMode.SETUP_CONFIRM_PIN
            }
            PrivatePinDialogMode.SETUP_CONFIRM_PIN -> {
                if (pin == tempNewPin) {
                    enteredPin = ""
                    errorMessage = null
                    currentMode = PrivatePinDialogMode.SETUP_RECOVERY_QUESTION
                } else {
                    errorMessage = "PINs do not match. Enter new PIN again."
                    tempNewPin = ""
                    enteredPin = ""
                    currentMode = PrivatePinDialogMode.SETUP_ENTER_PIN
                }
            }
            PrivatePinDialogMode.CHANGE_OLD_PIN -> {
                coroutineScope.launch {
                    val result = vaultSecurityManager.unlockWithPin(pin)
                    if (result.isSuccess) {
                        tempOldPin = pin
                        enteredPin = ""
                        errorMessage = null
                        currentMode = PrivatePinDialogMode.CHANGE_NEW_PIN
                    } else {
                        errorMessage = "Current PIN is incorrect."
                        enteredPin = ""
                    }
                }
            }
            PrivatePinDialogMode.CHANGE_NEW_PIN -> {
                tempNewPin = pin
                enteredPin = ""
                errorMessage = null
                currentMode = PrivatePinDialogMode.CHANGE_CONFIRM_PIN
            }
            PrivatePinDialogMode.CHANGE_CONFIRM_PIN -> {
                if (pin == tempNewPin) {
                    coroutineScope.launch {
                        val result = vaultSecurityManager.changePin(tempOldPin, pin)
                        if (result.isSuccess) {
                            onSuccess()
                        } else {
                            errorMessage = "Failed to update PIN: ${result.exceptionOrNull()?.message}"
                            enteredPin = ""
                        }
                    }
                } else {
                    errorMessage = "PINs do not match. Re-enter new PIN."
                    tempNewPin = ""
                    enteredPin = ""
                    currentMode = PrivatePinDialogMode.CHANGE_NEW_PIN
                }
            }
            else -> Unit
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        FrostedGlassBox(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(26.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(LineaColors.TitaniumBlue.copy(alpha = 0.15f))
                        .border(1.dp, LineaColors.TitaniumBlue.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (currentMode == PrivatePinDialogMode.SETUP_RECOVERY_QUESTION || currentMode == PrivatePinDialogMode.RECOVER_FORGOT_PIN) {
                            Icons.Filled.Security
                        } else if (currentMode == PrivatePinDialogMode.SETUP_ENTER_PIN || currentMode == PrivatePinDialogMode.CHANGE_NEW_PIN) {
                            Icons.Filled.LockOpen
                        } else {
                            Icons.Filled.Lock
                        },
                        contentDescription = null,
                        tint = LineaColors.TitaniumBlue,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title based on mode
                Text(
                    text = when (currentMode) {
                        PrivatePinDialogMode.UNLOCK -> "Private Safe"
                        PrivatePinDialogMode.SETUP_ENTER_PIN -> "Create 6-Digit PIN"
                        PrivatePinDialogMode.SETUP_CONFIRM_PIN -> "Confirm 6-Digit PIN"
                        PrivatePinDialogMode.SETUP_RECOVERY_QUESTION -> "Recovery Question"
                        PrivatePinDialogMode.RECOVER_FORGOT_PIN -> "Reset Forgotten PIN"
                        PrivatePinDialogMode.CHANGE_OLD_PIN -> "Verify Current PIN"
                        PrivatePinDialogMode.CHANGE_NEW_PIN -> "Enter New 6-Digit PIN"
                        PrivatePinDialogMode.CHANGE_CONFIRM_PIN -> "Confirm New PIN"
                    },
                    style = LineaTypography.titleLarge,
                    color = LineaColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Subtitle / Instruction / Error
                val subtitleText = if (lockoutSecondsRemaining > 0) {
                    "Locked out. Try again in ${lockoutSecondsRemaining}s"
                } else if (errorMessage != null) {
                    errorMessage!!
                } else {
                    when (currentMode) {
                        PrivatePinDialogMode.UNLOCK -> "Enter 6-digit security PIN to unlock"
                        PrivatePinDialogMode.SETUP_ENTER_PIN -> "Set a secure 6-digit PIN for private contacts"
                        PrivatePinDialogMode.SETUP_CONFIRM_PIN -> "Re-enter your 6-digit PIN to confirm"
                        PrivatePinDialogMode.SETUP_RECOVERY_QUESTION -> "Used to recover your vault if PIN is forgotten"
                        PrivatePinDialogMode.RECOVER_FORGOT_PIN -> storedQuestion ?: "Answer your security question"
                        PrivatePinDialogMode.CHANGE_OLD_PIN -> "Enter your current 6-digit PIN to proceed"
                        PrivatePinDialogMode.CHANGE_NEW_PIN -> "Choose a new 6-digit security PIN"
                        PrivatePinDialogMode.CHANGE_CONFIRM_PIN -> "Re-enter your new 6-digit PIN"
                    }
                }

                Text(
                    text = subtitleText,
                    style = LineaTypography.bodySmall,
                    color = if (errorMessage != null || lockoutSecondsRemaining > 0) LineaColors.Danger else LineaColors.TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Post-Quantum Security Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(LineaColors.GlassFill)
                        .border(LineaDimensions.HairlineBorder, LineaColors.MutedSageGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(LineaColors.MutedSageGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AES-256-GCM • Post-Quantum Vault",
                        style = LineaTypography.labelSmall.copy(fontSize = 10.sp),
                        color = LineaColors.MutedSageGreen
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mode-specific input area
                if (currentMode == PrivatePinDialogMode.SETUP_RECOVERY_QUESTION) {
                    // Question Picker
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { isQuestionDropdownOpen = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = LineaColors.TextPrimary
                            )
                        ) {
                            Text(
                                text = selectedQuestion,
                                style = LineaTypography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                        }

                        DropdownMenu(
                            expanded = isQuestionDropdownOpen,
                            onDismissRequest = { isQuestionDropdownOpen = false },
                            modifier = Modifier.background(LineaColors.BackgroundElevated)
                        ) {
                            PRESET_RECOVERY_QUESTIONS.forEach { q ->
                                DropdownMenuItem(
                                    text = { Text(q, color = LineaColors.TextPrimary) },
                                    onClick = {
                                        selectedQuestion = q
                                        isQuestionDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }

                    if (selectedQuestion == "Custom Question") {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customQuestionText,
                            onValueChange = { customQuestionText = it },
                            placeholder = { Text("Enter your custom question", color = LineaColors.TextTertiary) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = LineaColors.TextPrimary,
                                unfocusedTextColor = LineaColors.TextPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = recoveryAnswerText,
                        onValueChange = { recoveryAnswerText = it },
                        placeholder = { Text("Your Secret Answer", color = LineaColors.TextTertiary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = LineaColors.TextPrimary,
                            unfocusedTextColor = LineaColors.TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            val finalQuestion = if (selectedQuestion == "Custom Question") customQuestionText.trim() else selectedQuestion
                            if (finalQuestion.isBlank() || recoveryAnswerText.isBlank()) {
                                errorMessage = "Please enter question and answer."
                                return@Button
                            }
                            coroutineScope.launch {
                                val result = vaultSecurityManager.setupInitialPin(
                                    pin = tempNewPin,
                                    recoveryQuestion = finalQuestion,
                                    recoveryAnswer = recoveryAnswerText
                                )
                                if (result.isSuccess) {
                                    onSuccess()
                                } else {
                                    errorMessage = "Setup error: ${result.exceptionOrNull()?.message}"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LineaColors.TitaniumBlue)
                    ) {
                        Text("Save & Unlock Private Safe", fontWeight = FontWeight.SemiBold)
                    }
                } else if (currentMode == PrivatePinDialogMode.RECOVER_FORGOT_PIN) {
                    // Forgot PIN Recovery Answer
                    OutlinedTextField(
                        value = recoveryAnswerText,
                        onValueChange = { recoveryAnswerText = it },
                        placeholder = { Text("Your Answer", color = LineaColors.TextTertiary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = LineaColors.TextPrimary,
                            unfocusedTextColor = LineaColors.TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            if (recoveryAnswerText.isBlank()) {
                                errorMessage = "Please enter your recovery answer."
                                return@Button
                            }
                            // Move to entering new PIN
                            currentMode = PrivatePinDialogMode.SETUP_ENTER_PIN
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LineaColors.TitaniumBlue)
                    ) {
                        Text("Verify & Set New PIN", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    // PIN Dots (6 digits)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until 6) {
                            val isFilled = i < enteredPin.length
                            Box(
                                modifier = Modifier
                                    .size(15.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isFilled) {
                                            if (errorMessage != null) LineaColors.Danger else LineaColors.TitaniumBlue
                                        } else {
                                            LineaColors.GlassBorder
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        if (isFilled) LineaColors.TitaniumBlue else LineaColors.GlassBorder,
                                        CircleShape
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 12-key Dialpad for PIN (1-9, C, 0, DEL)
                    val keys = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("C", "0", "DEL")
                    )

                    val isKeypadDisabled = lockoutSecondsRemaining > 0

                    keys.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEach { key ->
                                Box(
                                    modifier = Modifier
                                        .size(62.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isKeypadDisabled) LineaColors.GlassFill.copy(alpha = 0.05f) else LineaColors.GlassFill
                                        )
                                        .border(
                                            LineaDimensions.HairlineBorder,
                                            if (isKeypadDisabled) LineaColors.GlassBorder.copy(alpha = 0.3f) else LineaColors.GlassBorder,
                                            CircleShape
                                        )
                                        .clickable(enabled = !isKeypadDisabled) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            when (key) {
                                                "C" -> {
                                                    enteredPin = ""
                                                    errorMessage = null
                                                }
                                                "DEL" -> {
                                                    if (enteredPin.isNotEmpty()) {
                                                        enteredPin = enteredPin.dropLast(1)
                                                        errorMessage = null
                                                    }
                                                }
                                                else -> {
                                                    if (enteredPin.length < 6) {
                                                        val nextPin = enteredPin + key
                                                        enteredPin = nextPin
                                                        errorMessage = null
                                                        if (nextPin.length == 6) {
                                                            submitPin(nextPin)
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (key == "DEL") {
                                        Icon(
                                            imageVector = Icons.Filled.Backspace,
                                            contentDescription = "Backspace",
                                            tint = if (isKeypadDisabled) LineaColors.TextTertiary else LineaColors.TextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        Text(
                                            text = key,
                                            style = LineaTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (isKeypadDisabled) LineaColors.TextTertiary else if (key == "C") LineaColors.Warning else LineaColors.TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // "Forgot PIN?" option in Unlock mode
                    if (currentMode == PrivatePinDialogMode.UNLOCK && hasPinSet == true) {
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    storedQuestion = vaultSecurityManager.getRecoveryQuestion()
                                    currentMode = PrivatePinDialogMode.RECOVER_FORGOT_PIN
                                    errorMessage = null
                                }
                            }
                        ) {
                            Text(
                                text = "Forgot PIN?",
                                style = LineaTypography.bodyMedium,
                                color = LineaColors.TitaniumBlue
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Cancel",
                        style = LineaTypography.bodyMedium,
                        color = LineaColors.TextSecondary
                    )
                }
            }
        }
    }
}
