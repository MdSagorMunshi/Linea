package com.ryanshelby.linea.ui.screens.settings.escape

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.telecom.escape.EscapeCallManager
import com.ryanshelby.linea.telecom.escape.EscapeCallPreset
import com.ryanshelby.linea.ui.components.FrostedGlassBox
import com.ryanshelby.linea.ui.components.neumorphic
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography

enum class EscapeCallTab {
    SIMULATOR,
    SETTINGS,
    HELP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EscapeCallSheet(
    sheetState: SheetState,
    escapeCallManager: EscapeCallManager,
    initialTab: EscapeCallTab = EscapeCallTab.SIMULATOR,
    onDismiss: () -> Unit
) {
    var currentTab by remember { mutableStateOf(initialTab) }

    val isArmed by escapeCallManager.isArmed.collectAsState()
    val remainingSeconds by escapeCallManager.remainingSeconds.collectAsState()
    val selectedPreset by escapeCallManager.selectedPreset.collectAsState()
    val customName by escapeCallManager.customName.collectAsState()
    val customNumber by escapeCallManager.customNumber.collectAsState()
    val selectedDelaySeconds by escapeCallManager.selectedDelaySeconds.collectAsState()

    val customCode by escapeCallManager.customCode.collectAsState()
    val secondaryCode by escapeCallManager.secondaryCode.collectAsState()
    val defaultDelay by escapeCallManager.defaultDelay.collectAsState()
    val vibrateOnly by escapeCallManager.vibrateOnly.collectAsState()
    val disarmNotification by escapeCallManager.disarmNotification.collectAsState()

    var inputCustomName by remember(customName) { mutableStateOf(customName) }
    var inputCustomNumber by remember(customNumber) { mutableStateOf(customNumber) }
    var inputCustomCode by remember(customCode) { mutableStateOf(customCode) }
    var inputSecondaryCode by remember(secondaryCode) { mutableStateOf(secondaryCode) }

    val delays = listOf(
        0 to "Now",
        5 to "5s",
        15 to "15s",
        30 to "30s",
        60 to "1m",
        180 to "3m"
    )

    val dialpadDelayOptions = listOf(5, 10, 15, 30)

    val pulseTransition = rememberInfiniteTransition(label = "ArmedPulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ArmedPulseAlpha"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LineaColors.BackgroundTop,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            // Header with Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(LineaColors.GlassFill)
                            .border(LineaDimensions.HairlineBorder, LineaColors.GlassBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🎭", fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Tactical Escape Call",
                            style = LineaTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = LineaColors.TextPrimary
                        )
                        Text(
                            text = if (isArmed) "Armed (triggering in ${remainingSeconds}s)" else "Simulated Incoming Call Engine",
                            style = LineaTypography.bodySmall,
                            color = if (isArmed) LineaColors.Warning else LineaColors.TextSecondary
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = LineaColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Navigation Segmented Bar (Simulator | Custom Settings | Field Manual)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(LineaColors.GlassFill)
                    .border(LineaDimensions.HairlineBorder, LineaColors.GlassBorder, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TabPill(
                    label = "Simulator",
                    icon = "🎭",
                    isSelected = currentTab == EscapeCallTab.SIMULATOR,
                    modifier = Modifier.weight(1f)
                ) { currentTab = EscapeCallTab.SIMULATOR }

                TabPill(
                    label = "Settings",
                    icon = "⚙️",
                    isSelected = currentTab == EscapeCallTab.SETTINGS,
                    modifier = Modifier.weight(1f)
                ) { currentTab = EscapeCallTab.SETTINGS }

                TabPill(
                    label = "Manual",
                    icon = "📖",
                    isSelected = currentTab == EscapeCallTab.HELP,
                    modifier = Modifier.weight(1f)
                ) { currentTab = EscapeCallTab.HELP }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Armed Countdown Alert Banner (Visible across tabs if armed)
            AnimatedVisibility(visible = isArmed) {
                FrostedGlassBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    borderColor = LineaColors.Warning.copy(alpha = pulseAlpha)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Timer,
                                contentDescription = null,
                                tint = LineaColors.Warning,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Escape Call Armed",
                                    style = LineaTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = LineaColors.Warning
                                )
                                Text(
                                    text = "Triggering in ${remainingSeconds}s...",
                                    style = LineaTypography.bodySmall,
                                    color = LineaColors.TextSecondary
                                )
                            }
                        }
                        Button(
                            onClick = { escapeCallManager.cancelCountdown() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LineaColors.Danger.copy(alpha = 0.2f),
                                contentColor = LineaColors.Danger
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Disarm", style = LineaTypography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }

            // Tab Content
            when (currentTab) {
                EscapeCallTab.SIMULATOR -> {
                    // Section 1: Presets
                    Text(
                        text = "CALLER IDENTITY PRESET",
                        style = LineaTypography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = LineaColors.TitaniumBlue,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        escapeCallManager.presets.forEach { preset ->
                            val isSelected = selectedPreset.id == preset.id
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .neumorphic(
                                        shape = RoundedCornerShape(14.dp),
                                        elevation = if (isSelected) 1.dp else 2.dp,
                                        isSunken = isSelected,
                                        surfaceColor = if (isSelected) LineaColors.NeuSurfaceRaised.copy(alpha = 0.85f) else LineaColors.NeuSurfaceRaised
                                    )
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(1.dp, LineaColors.TitaniumBlue, RoundedCornerShape(14.dp))
                                        } else Modifier
                                    )
                                    .clickable {
                                        escapeCallManager.selectPreset(preset)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = preset.iconEmoji, fontSize = 22.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (preset.id == "custom") "Custom Persona..." else preset.name,
                                            style = LineaTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = if (isSelected) LineaColors.TitaniumBlue else LineaColors.TextPrimary
                                        )
                                        Text(
                                            text = if (preset.id == "custom") "$inputCustomName • $inputCustomNumber" else "${preset.subtitle} • ${preset.number}",
                                            style = LineaTypography.bodySmall,
                                            color = LineaColors.TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(LineaColors.TitaniumBlue),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // In-line custom persona editor if Custom selected
                    if (selectedPreset.id == "custom") {
                        Spacer(modifier = Modifier.height(12.dp))
                        FrostedGlassBox(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Custom Caller Name",
                                    style = LineaTypography.labelSmall,
                                    color = LineaColors.TextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = inputCustomName,
                                    onValueChange = {
                                        inputCustomName = it
                                        escapeCallManager.setCustomPersona(it, inputCustomNumber)
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = LineaColors.TitaniumBlue,
                                        unfocusedBorderColor = LineaColors.GlassBorder,
                                        focusedTextColor = LineaColors.TextPrimary,
                                        unfocusedTextColor = LineaColors.TextPrimary
                                    ),
                                    placeholder = { Text("e.g. Boardroom Dispatch, Clinic", color = LineaColors.TextTertiary) }
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "Phone Number",
                                    style = LineaTypography.labelSmall,
                                    color = LineaColors.TextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = inputCustomNumber,
                                    onValueChange = {
                                        inputCustomNumber = it
                                        escapeCallManager.setCustomPersona(inputCustomName, it)
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = LineaColors.TitaniumBlue,
                                        unfocusedBorderColor = LineaColors.GlassBorder,
                                        focusedTextColor = LineaColors.TextPrimary,
                                        unfocusedTextColor = LineaColors.TextPrimary
                                    ),
                                    placeholder = { Text("+1 (555) 019-2834", color = LineaColors.TextTertiary) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Delay Timing
                    Text(
                        text = "COUNTDOWN DELAY",
                        style = LineaTypography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = LineaColors.TitaniumBlue,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        delays.forEach { (sec, label) ->
                            val isSelected = selectedDelaySeconds == sec
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .neumorphic(
                                        shape = RoundedCornerShape(10.dp),
                                        elevation = if (isSelected) 1.dp else 2.dp,
                                        isSunken = isSelected,
                                        surfaceColor = if (isSelected) LineaColors.TitaniumBlue.copy(alpha = 0.2f) else LineaColors.NeuSurfaceRaised
                                    )
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(1.dp, LineaColors.TitaniumBlue, RoundedCornerShape(10.dp))
                                        } else Modifier
                                    )
                                    .clickable {
                                        escapeCallManager.setDelaySeconds(sec)
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = LineaTypography.bodySmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) LineaColors.TitaniumBlue else LineaColors.TextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Primary Button
                    Button(
                        onClick = {
                            val delay = selectedDelaySeconds
                            escapeCallManager.scheduleEscapeCall(delaySeconds = delay)
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LineaColors.TitaniumBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Call,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        val buttonText = if (selectedDelaySeconds == 0) {
                            "Trigger Call Immediately"
                        } else {
                            "Arm Escape Call in ${selectedDelaySeconds}s"
                        }
                        Text(
                            text = buttonText,
                            style = LineaTypography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Discreet Tip
                    FrostedGlassBox(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "💡", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Secret Code: Dial $customCode or $secondaryCode in dialpad to arm instantly (${defaultDelay}s).",
                                style = LineaTypography.bodySmall.copy(fontSize = 11.sp),
                                color = LineaColors.TextSecondary
                            )
                        }
                    }
                }

                EscapeCallTab.SETTINGS -> {
                    // Custom Trigger Codes Section
                    Text(
                        text = "SECRET DIALPAD CODES",
                        style = LineaTypography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = LineaColors.TitaniumBlue,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    FrostedGlassBox(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Primary Secret Code",
                                style = LineaTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = LineaColors.TextPrimary
                            )
                            Text(
                                text = "Dialing this on the Linea keypad arms an escape call immediately without pressing call",
                                style = LineaTypography.bodySmall,
                                color = LineaColors.TextSecondary,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = inputCustomCode,
                                onValueChange = {
                                    inputCustomCode = it
                                    escapeCallManager.setCustomCode(it)
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LineaColors.TitaniumBlue,
                                    unfocusedBorderColor = LineaColors.GlassBorder,
                                    focusedTextColor = LineaColors.TextPrimary,
                                    unfocusedTextColor = LineaColors.TextPrimary
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Quick Suggestions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("*#*#3253#*#*", "*#77#", "#007#", "#911#").forEach { suggestion ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(LineaColors.GlassFill)
                                            .border(0.5.dp, LineaColors.GlassBorder, RoundedCornerShape(8.dp))
                                            .clickable {
                                                inputCustomCode = suggestion
                                                escapeCallManager.setCustomCode(suggestion)
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = suggestion,
                                            style = LineaTypography.bodySmall.copy(fontSize = 11.sp),
                                            color = LineaColors.TitaniumBlue
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Secondary Quick Code",
                                style = LineaTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = LineaColors.TextPrimary
                            )
                            Text(
                                text = "Short emergency dial code fallback",
                                style = LineaTypography.bodySmall,
                                color = LineaColors.TextSecondary,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = inputSecondaryCode,
                                onValueChange = {
                                    inputSecondaryCode = it
                                    escapeCallManager.setSecondaryCode(it)
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LineaColors.TitaniumBlue,
                                    unfocusedBorderColor = LineaColors.GlassBorder,
                                    focusedTextColor = LineaColors.TextPrimary,
                                    unfocusedTextColor = LineaColors.TextPrimary
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Dialpad Trigger Delay",
                                style = LineaTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = LineaColors.TextPrimary
                            )
                            Text(
                                text = "Seconds to wait after dialing secret code before phone rings",
                                style = LineaTypography.bodySmall,
                                color = LineaColors.TextSecondary,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                dialpadDelayOptions.forEach { sec ->
                                    val isSelected = defaultDelay == sec
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) LineaColors.TitaniumBlue else LineaColors.GlassFill)
                                            .border(
                                                1.dp,
                                                if (isSelected) LineaColors.TitaniumBlue else LineaColors.GlassBorder,
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                escapeCallManager.setDefaultDelay(sec)
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${sec}s",
                                            style = LineaTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (isSelected) Color.White else LineaColors.TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Tactical Audio & Stealth Section
                    Text(
                        text = "TACTICAL AUDIO & NOTIFICATIONS",
                        style = LineaTypography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = LineaColors.TitaniumBlue,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    FrostedGlassBox(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Stealth Vibrate Only
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Stealth Mode (Vibrate Only)",
                                        style = LineaTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = LineaColors.TextPrimary
                                    )
                                    Text(
                                        text = "Vibrates rhythmically instead of playing audio ringtone",
                                        style = LineaTypography.bodySmall,
                                        color = LineaColors.TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                Switch(
                                    checked = vibrateOnly,
                                    onCheckedChange = { escapeCallManager.setVibrateOnly(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = LineaColors.TitaniumBlue
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Disarm Notification
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Arming Toast Confirmation",
                                        style = LineaTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = LineaColors.TextPrimary
                                    )
                                    Text(
                                        text = "Show discreet on-screen confirmation when code is dialed",
                                        style = LineaTypography.bodySmall,
                                        color = LineaColors.TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                Switch(
                                    checked = disarmNotification,
                                    onCheckedChange = { escapeCallManager.setDisarmNotification(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = LineaColors.TitaniumBlue
                                    )
                                )
                            }
                        }
                    }
                }

                EscapeCallTab.HELP -> {
                    // Field Manual & Tactics
                    Text(
                        text = "TACTICAL FIELD MANUAL",
                        style = LineaTypography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = LineaColors.TitaniumBlue,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    FrostedGlassBox(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            ManualSection(
                                emoji = "🧠",
                                title = "100% On-Device Simulation",
                                description = "The Escape Call generator operates entirely offline without making cellular calls, consuming SIM airtime, or contacting cloud services. It uses Linea's internal audio manager and telecom state pipeline."
                            )

                            ManualSection(
                                emoji = "🕵️",
                                title = "Discreet Hands-Under-Table Tactic",
                                description = "When in an awkward meeting, dinner, or situation: open the Linea dialpad, casually enter your secret code ($customCode or $secondaryCode), and slide your phone back into your pocket or on the table face up. In $defaultDelay seconds, the simulated incoming call rings naturally."
                            )

                            ManualSection(
                                emoji = "📱",
                                title = "Authentic In-Call Immersion",
                                description = "When you swipe to answer, the screen transitions to the genuine Linea In-Call screen with a real ticking call timer (00:01, 00:02...). Speaker, mute, and hold buttons respond realistically, and the proximity sensor blanks the screen when held to your ear so onlookers cannot tell it is simulated."
                            )

                            ManualSection(
                                emoji = "🛑",
                                title = "Disarming & Ending",
                                description = "If you change your mind while the countdown is ticking, tap the red 'Disarm' button in the status banner. During the call, press the red End Call button to hang up smoothly."
                            )

                            ManualSection(
                                emoji = "🎭",
                                title = "Persona Selection Strategy",
                                description = "• The Boss: High-stakes work escalation.\n• Doctor's Office: Urgent personal appointment callback.\n• Home Security: Alarm sensor alert requiring immediate departure.\n• Custom Persona: Tailored to your exact context."
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TabPill(
    label: String,
    icon: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) LineaColors.TitaniumBlue else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = icon, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = LineaTypography.bodySmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isSelected) Color.White else LineaColors.TextSecondary
            )
        }
    }
}

@Composable
private fun ManualSection(
    emoji: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = emoji, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = LineaTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = LineaColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = description,
                style = LineaTypography.bodySmall,
                color = LineaColors.TextSecondary,
                lineHeight = 16.sp
            )
        }
    }
}
