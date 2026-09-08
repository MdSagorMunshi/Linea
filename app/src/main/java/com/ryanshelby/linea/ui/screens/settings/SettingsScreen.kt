package com.ryanshelby.linea.ui.screens.settings

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.ui.components.FrostedGlassBox
import com.ryanshelby.linea.ui.components.RoleBanner
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography

import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneForwarded
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Voicemail

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    isDefaultDialer: Boolean,
    onRequestDefaultDialer: () -> Unit,
    onNavigateToBlocking: () -> Unit,
    onNavigateToDualSim: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToRecordings: () -> Unit = {},
    onNavigateToVoicemail: () -> Unit = {},
    onNavigateToCallForwarding: () -> Unit = {},
    onNavigateToCallBarring: () -> Unit = {},
    onNavigateToCallRules: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = LineaDimensions.ScreenPadding)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Settings",
            style = LineaTypography.headlineLarge,
            color = LineaColors.TextPrimary
        )
        Text(
            text = "Telecom configuration & on-device security",
            style = LineaTypography.bodyMedium,
            color = LineaColors.TextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Default Dialer Role Status
        RoleBanner(
            isDefaultDialer = isDefaultDialer,
            onRequestRole = onRequestDefaultDialer
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Dual SIM Quick Link
        SettingsNavCard(
            icon = Icons.Filled.SimCard,
            title = "Dual SIM Management",
            subtitle = when (uiState.defaultSim) {
                0 -> "Default: SIM 1"
                1 -> "Default: SIM 2"
                else -> "Ask before every call"
            },
            badge = if (uiState.askSimBeforeDial) "ALWAYS ASK" else null,
            onClick = onNavigateToDualSim
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Call Screening & Blocking Quick Link
        SettingsNavCard(
            icon = Icons.Filled.Block,
            title = "Call Screening & Blocking",
            subtitle = "${uiState.blockedRulesCount} active rules • Quiet hours ${if (uiState.isQuietHoursActive) "ON" else "OFF"}",
            badge = "${uiState.blockedRulesCount} RULES",
            onClick = onNavigateToBlocking
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Smart Rules & Schedules (Phase 5)
        SettingsNavCard(
            icon = Icons.Filled.Schedule,
            title = "Smart Rules & Schedules",
            subtitle = "${uiState.callRulesCount} rules configured • Nighttime & workday filters",
            badge = if (uiState.callRulesCount > 0) "${uiState.callRulesCount} RULES" else null,
            onClick = onNavigateToCallRules
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Call Recordings Link
        SettingsNavCard(
            icon = Icons.Filled.Mic,
            title = "Call Recordings",
            subtitle = "Local audio recordings with playback & export",
            onClick = onNavigateToRecordings
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Visual Voicemail Link
        SettingsNavCard(
            icon = Icons.Filled.Voicemail,
            title = "Visual Voicemail",
            subtitle = "On-device inbox with transcription previews",
            onClick = onNavigateToVoicemail
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Call Forwarding Link
        SettingsNavCard(
            icon = Icons.Filled.PhoneForwarded,
            title = "Call Forwarding",
            subtitle = "Carrier unconditional & conditional divert rules",
            onClick = onNavigateToCallForwarding
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Call Barring & FDN Link
        SettingsNavCard(
            icon = Icons.Filled.Security,
            title = "Call Barring & FDN",
            subtitle = "Network call restrictions & authorized numbers",
            onClick = onNavigateToCallBarring
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Calling Preferences Panel
        FrostedGlassBox(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(LineaDimensions.PanelPadding)) {
                SettingsSectionHeader(icon = Icons.Filled.Phone, title = "Calling Preferences")

                Spacer(modifier = Modifier.height(12.dp))

                SettingsToggleRow(
                    title = "Auto-Record All Calls",
                    subtitle = "Automatically record cellular calls to private storage",
                    checked = uiState.autoRecordCalls,
                    onCheckedChange = { viewModel.setAutoRecordCalls(it) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Call Duration Warning",
                    style = LineaTypography.titleSmall,
                    color = LineaColors.TextPrimary
                )
                Text(
                    text = "Vibrates and alerts when active call exceeds threshold",
                    style = LineaTypography.bodySmall,
                    color = LineaColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SelectionPill(
                        label = "Off",
                        isSelected = uiState.callDurationWarningMinutes == 0,
                        onClick = { viewModel.setCallDurationWarningMinutes(0) }
                    )
                    SelectionPill(
                        label = "5 Min",
                        isSelected = uiState.callDurationWarningMinutes == 5,
                        onClick = { viewModel.setCallDurationWarningMinutes(5) }
                    )
                    SelectionPill(
                        label = "10 Min",
                        isSelected = uiState.callDurationWarningMinutes == 10,
                        onClick = { viewModel.setCallDurationWarningMinutes(10) }
                    )
                    SelectionPill(
                        label = "15 Min",
                        isSelected = uiState.callDurationWarningMinutes == 15,
                        onClick = { viewModel.setCallDurationWarningMinutes(15) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                SettingsToggleRow(
                    title = "Confirm Before Calling",
                    subtitle = "Prevents accidental dials with a countdown prompt",
                    checked = uiState.callConfirmation,
                    onCheckedChange = { viewModel.setCallConfirmation(it) }
                )

                if (uiState.callConfirmation) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SelectionPill(
                            label = "3s Countdown",
                            isSelected = uiState.callCountdownSeconds == 3,
                            onClick = { viewModel.setCountdownSeconds(3) }
                        )
                        SelectionPill(
                            label = "5s Countdown",
                            isSelected = uiState.callCountdownSeconds == 5,
                            onClick = { viewModel.setCountdownSeconds(5) }
                        )
                        SelectionPill(
                            label = "10s Countdown",
                            isSelected = uiState.callCountdownSeconds == 10,
                            onClick = { viewModel.setCountdownSeconds(10) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                SettingsToggleRow(
                    title = "Proximity Sensor",
                    subtitle = "Turns off screen when held to ear during active calls",
                    checked = uiState.proximitySensorEnabled,
                    onCheckedChange = { viewModel.setProximitySensor(it) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingsToggleRow(
                    title = "Don't Interrupt Me Mode",
                    subtitle = "Incoming calls float as unobtrusive top banner instead of taking over full screen",
                    checked = uiState.dontInterruptMe,
                    onCheckedChange = { viewModel.setDontInterruptMe(it) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingsToggleRow(
                    title = "Repeated Call Emergency Override",
                    subtitle = "3 calls within 5 minutes bypass quiet hours and block rules",
                    checked = uiState.repeatCallOverride,
                    onCheckedChange = { viewModel.setRepeatCallOverride(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Sound & Vibration Panel
        FrostedGlassBox(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(LineaDimensions.PanelPadding)) {
                SettingsSectionHeader(icon = Icons.Filled.VolumeUp, title = "Sound & Haptics")

                Spacer(modifier = Modifier.height(12.dp))

                SettingsToggleRow(
                    title = "Dial Pad DTMF Tones",
                    subtitle = "Audible dual-tone multi-frequency key feedback",
                    checked = uiState.dialpadSound,
                    onCheckedChange = { viewModel.setDialpadSound(it) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingsToggleRow(
                    title = "Dial Pad Haptic Click",
                    subtitle = "Tactile vibration when tapping digits",
                    checked = uiState.dialpadVibration,
                    onCheckedChange = { viewModel.setDialpadVibration(it) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingsToggleRow(
                    title = "Call State Vibrations",
                    subtitle = "Haptic pulse when calls connect and disconnect",
                    checked = uiState.callVibration,
                    onCheckedChange = { viewModel.setCallVibration(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Appearance & Motion Panel
        FrostedGlassBox(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(LineaDimensions.PanelPadding)) {
                SettingsSectionHeader(icon = Icons.Filled.Palette, title = "Appearance & Motion")

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Color Theme",
                    style = LineaTypography.bodyMedium,
                    color = LineaColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SelectionPill(
                        label = "Graphite Dark",
                        isSelected = uiState.theme == "DARK",
                        onClick = { viewModel.setTheme("DARK") }
                    )
                    SelectionPill(
                        label = "OLED Pitch",
                        isSelected = uiState.theme == "AMOLED",
                        onClick = { viewModel.setTheme("AMOLED") }
                    )
                    SelectionPill(
                        label = "System",
                        isSelected = uiState.theme == "SYSTEM",
                        onClick = { viewModel.setTheme("SYSTEM") }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                SettingsToggleRow(
                    title = "Reduce Animations",
                    subtitle = "Swaps spring physics for fast cross-fades",
                    checked = uiState.reduceAnimations,
                    onCheckedChange = { viewModel.setReduceAnimations(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Call History & Retention Panel
        FrostedGlassBox(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(LineaDimensions.PanelPadding)) {
                SettingsSectionHeader(icon = Icons.Filled.Delete, title = "Call History & Data")

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Auto-Cleanup Retention",
                    style = LineaTypography.bodyMedium,
                    color = LineaColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SelectionPill(
                        label = "Forever",
                        isSelected = uiState.cleanupDays == 0,
                        onClick = { viewModel.setCleanupDays(0) }
                    )
                    SelectionPill(
                        label = "30 Days",
                        isSelected = uiState.cleanupDays == 30,
                        onClick = { viewModel.setCleanupDays(30) }
                    )
                    SelectionPill(
                        label = "90 Days",
                        isSelected = uiState.cleanupDays == 90,
                        onClick = { viewModel.setCleanupDays(90) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { showClearHistoryDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(LineaDimensions.ButtonCornerRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LineaColors.CarmineRed.copy(alpha = 0.15f),
                        contentColor = LineaColors.CarmineRed
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        tint = LineaColors.CarmineRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Clear Entire Call History",
                        style = LineaTypography.labelLarge,
                        color = LineaColors.CarmineRed
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Permissions Architecture Quick Link
        SettingsNavCard(
            icon = Icons.Filled.Security,
            title = "Permissions Architecture",
            subtitle = "Telecom, contacts, audio, and lockscreen privileges",
            badge = "VIEW STATUS",
            onClick = onNavigateToPermissions
        )

        Spacer(modifier = Modifier.height(110.dp))
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            containerColor = LineaColors.BackgroundBottom,
            title = {
                Text(
                    text = "Clear Call History?",
                    style = LineaTypography.titleLarge,
                    color = LineaColors.TextPrimary
                )
            },
            text = {
                Text(
                    text = "This will permanently remove all call records from Linea. This cannot be undone.",
                    style = LineaTypography.bodyMedium,
                    color = LineaColors.TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllCallHistory()
                        showClearHistoryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LineaColors.CarmineRed,
                        contentColor = LineaColors.TextPrimary
                    )
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel", color = LineaColors.TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = LineaColors.TitaniumBlue,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = LineaTypography.titleSmall,
            color = LineaColors.TextPrimary
        )
    }
}

@Composable
private fun SettingsNavCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String? = null,
    onClick: () -> Unit
) {
    FrostedGlassBox(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(LineaColors.GlassFill),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = LineaColors.TitaniumBlue,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = LineaTypography.titleMedium,
                    color = LineaColors.TextPrimary
                )
                Text(
                    text = subtitle,
                    style = LineaTypography.bodySmall,
                    color = LineaColors.TextSecondary
                )
            }

            if (badge != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(LineaColors.GlassFill)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badge,
                        fontSize = 10.sp,
                        color = LineaColors.TitaniumBlue
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = LineaColors.TextTertiary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = LineaTypography.bodyMedium,
                color = LineaColors.TextPrimary
            )
            Text(
                text = subtitle,
                style = LineaTypography.bodySmall,
                color = LineaColors.TextSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = LineaColors.TextPrimary,
                checkedTrackColor = LineaColors.TitaniumBlue,
                uncheckedThumbColor = LineaColors.TextSecondary,
                uncheckedTrackColor = LineaColors.BackgroundTop
            )
        )
    }
}

@Composable
private fun SelectionPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) LineaColors.TitaniumBlue else LineaColors.GlassFill)
            .border(
                1.dp,
                if (isSelected) LineaColors.TitaniumBlue else LineaColors.GlassBorder,
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isSelected) LineaColors.TextPrimary else LineaColors.TextSecondary
        )
    }
}
