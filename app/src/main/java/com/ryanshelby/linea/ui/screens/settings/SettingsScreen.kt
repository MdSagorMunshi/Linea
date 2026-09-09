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
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.VolumeUp
import com.ryanshelby.linea.data.preferences.LineaPreferences
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
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.FileDownload
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    onNavigateToDiagnostics: () -> Unit = {},
    onNavigateToStats: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToRingtone: () -> Unit = {},
    scrollState: ScrollState = rememberScrollState(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()
    val context = LocalContext.current
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            viewModel.exportCallHistoryCsv(it, context.contentResolver) { success, msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = LineaDimensions.ScreenPadding)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = com.ryanshelby.linea.R.drawable.ic_linea_logo),
                contentDescription = "Linea Logo",
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        LineaDimensions.HairlineBorder,
                        LineaColors.GlassBorder,
                        RoundedCornerShape(12.dp)
                    )
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
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
            }
        }

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

        Spacer(modifier = Modifier.height(10.dp))

        // Multi-Profile Switcher Panel
        FrostedGlassBox(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(LineaDimensions.PanelPadding)) {
                SettingsSectionHeader(icon = Icons.Filled.Person, title = "Dialer Profile")

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Active Environment",
                    style = LineaTypography.bodyMedium,
                    color = LineaColors.TextPrimary
                )
                Text(
                    text = "Switch smart routing rules, default SIM affinity, and focus filters",
                    style = LineaTypography.bodySmall,
                    color = LineaColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SelectionPill(
                        label = "Personal",
                        isSelected = activeProfile == "PERSONAL",
                        onClick = { viewModel.setActiveProfile("PERSONAL") }
                    )
                    SelectionPill(
                        label = "Work",
                        isSelected = activeProfile == "WORK",
                        onClick = { viewModel.setActiveProfile("WORK") }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Cellular Diagnostics
        SettingsNavCard(
            icon = Icons.Filled.Speed,
            title = "Cellular Diagnostics",
            subtitle = "Signal strength dBm, 5G/LTE radio, VoLTE & VoWiFi telemetry",
            onClick = onNavigateToDiagnostics
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Call Analytics & Statistics
        SettingsNavCard(
            icon = Icons.Filled.BarChart,
            title = "Call Analytics & Stats",
            subtitle = "Total talk time, direction metrics, top frequent contacts",
            onClick = onNavigateToStats
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Backup & Migration
        SettingsNavCard(
            icon = Icons.Filled.Backup,
            title = "Backup & Data Migration",
            subtitle = "Local JSON export & restore for call logs, contacts, and rules",
            onClick = onNavigateToBackup
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

                Spacer(modifier = Modifier.height(14.dp))

                SettingsNavCard(
                    icon = Icons.Filled.MusicNote,
                    title = "Phone Ringtone",
                    subtitle = if (uiState.ringtoneType == com.ryanshelby.linea.data.preferences.LineaPreferences.RingtoneType.APP_DEFAULT) {
                        "Linea Signature (Default)"
                    } else {
                        "System Default"
                    },
                    badge = if (uiState.ringtoneType == com.ryanshelby.linea.data.preferences.LineaPreferences.RingtoneType.APP_DEFAULT) {
                        "LINEA"
                    } else {
                        "SYSTEM"
                    },
                    onClick = onNavigateToRingtone
                )

                Spacer(modifier = Modifier.height(14.dp))

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

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Dialpad Haptic & Audio Profile",
                    style = LineaTypography.bodyMedium,
                    color = LineaColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (uiState.dialpadHapticProfile) {
                        LineaPreferences.DialpadHapticProfile.TITANIUM_GLASS -> "Titanium Glass: Crisp dual-micro clicks with high fidelity tones"
                        LineaPreferences.DialpadHapticProfile.MECHANICAL_RELAY -> "Mechanical Relay: Heavy tactile thump with classic relay acoustics"
                        LineaPreferences.DialpadHapticProfile.STEALTH -> "Stealth: Subtle near-silent haptics with muted tones"
                        else -> "Classic: Standard Android dialpad vibration"
                    },
                    style = LineaTypography.bodySmall,
                    color = LineaColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SelectionPill(
                        label = "Titanium",
                        isSelected = uiState.dialpadHapticProfile == LineaPreferences.DialpadHapticProfile.TITANIUM_GLASS,
                        onClick = { viewModel.setDialpadHapticProfile(LineaPreferences.DialpadHapticProfile.TITANIUM_GLASS) }
                    )
                    SelectionPill(
                        label = "Mechanical",
                        isSelected = uiState.dialpadHapticProfile == LineaPreferences.DialpadHapticProfile.MECHANICAL_RELAY,
                        onClick = { viewModel.setDialpadHapticProfile(LineaPreferences.DialpadHapticProfile.MECHANICAL_RELAY) }
                    )
                    SelectionPill(
                        label = "Stealth",
                        isSelected = uiState.dialpadHapticProfile == LineaPreferences.DialpadHapticProfile.STEALTH,
                        onClick = { viewModel.setDialpadHapticProfile(LineaPreferences.DialpadHapticProfile.STEALTH) }
                    )
                    SelectionPill(
                        label = "Classic",
                        isSelected = uiState.dialpadHapticProfile == LineaPreferences.DialpadHapticProfile.CLASSIC,
                        onClick = { viewModel.setDialpadHapticProfile(LineaPreferences.DialpadHapticProfile.CLASSIC) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                SettingsToggleRow(
                    title = "Call State Vibrations",
                    subtitle = "Haptic pulse when calls connect and disconnect",
                    checked = uiState.callVibration,
                    onCheckedChange = { viewModel.setCallVibration(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Motion & Call Gestures Panel
        FrostedGlassBox(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(LineaDimensions.PanelPadding)) {
                SettingsSectionHeader(icon = Icons.Filled.ScreenRotation, title = "Motion & Call Gestures")

                Spacer(modifier = Modifier.height(12.dp))

                SettingsToggleRow(
                    title = "Flip to Silence",
                    subtitle = "Turn phone face-down during incoming call to mute ringer",
                    checked = uiState.flipToSilence,
                    onCheckedChange = { viewModel.setFlipToSilence(it) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingsToggleRow(
                    title = "Proximity Wave to Silence",
                    subtitle = "Wave hand over top sensor during incoming call to mute ringer",
                    checked = uiState.proximityWaveToSilence,
                    onCheckedChange = { viewModel.setProximityWaveToSilence(it) }
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SelectionPill(
                        label = "Light Glass",
                        isSelected = uiState.theme.uppercase() == "LIGHT",
                        onClick = { viewModel.setTheme("LIGHT") }
                    )
                    SelectionPill(
                        label = "Dark",
                        isSelected = uiState.theme.uppercase() == "DARK",
                        onClick = { viewModel.setTheme("DARK") }
                    )
                    SelectionPill(
                        label = "OLED",
                        isSelected = uiState.theme.uppercase() == "AMOLED",
                        onClick = { viewModel.setTheme("AMOLED") }
                    )
                    SelectionPill(
                        label = "System",
                        isSelected = uiState.theme.uppercase() == "SYSTEM",
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

                OutlinedButton(
                    onClick = {
                        viewModel.cleanHistoryNow { deleted ->
                            val msg = if (uiState.cleanupDays == 0) {
                                "Retention set to 'Forever'. Select 30 or 90 days to automatically prune older logs."
                            } else if (deleted > 0) {
                                "Cleaned $deleted call record(s) older than ${uiState.cleanupDays} days."
                            } else {
                                "History is clean. No records older than ${uiState.cleanupDays} days found."
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(LineaDimensions.ButtonCornerRadius),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LineaColors.TitaniumBlue)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CleaningServices,
                        contentDescription = null,
                        tint = LineaColors.TitaniumBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Run History Cleanup Now",
                        style = LineaTypography.labelLarge,
                        color = LineaColors.TitaniumBlue
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                        exportCsvLauncher.launch("linea_call_history_$timestamp.csv")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(LineaDimensions.ButtonCornerRadius),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LineaColors.TextPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Filled.FileDownload,
                        contentDescription = null,
                        tint = LineaColors.TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Export Call History (.csv)",
                        style = LineaTypography.labelLarge,
                        color = LineaColors.TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

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

        Spacer(modifier = Modifier.height(14.dp))

        // About Linea Navigation Card
        SettingsNavCard(
            icon = Icons.Filled.Info,
            title = "About Linea & FOSS",
            subtitle = "Ryan Shelby • Open Source • Support & Specs",
            badge = "ABOUT",
            onClick = onNavigateToAbout
        )

        Spacer(modifier = Modifier.height(16.dp))

        // About Linea Card (Interactive Preview)
        FrostedGlassBox(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToAbout() },
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.ryanshelby.linea.R.drawable.ic_linea_logo),
                    contentDescription = "Linea",
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Linea Dialer",
                    style = LineaTypography.titleLarge,
                    color = LineaColors.TextPrimary
                )
                Text(
                    text = "Version 1.0.0 • Free & Open Source (FOSS)",
                    style = LineaTypography.labelSmall,
                    color = LineaColors.TitaniumBlue
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Developer: Ryan Shelby • Support: ryn@disr.it",
                    style = LineaTypography.labelSmall,
                    color = LineaColors.AccentGreen
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "On-device SQLite • AES-256-GCM Vault • Zero-telemetry\nTap to view full developer info & source repository",
                    style = LineaTypography.bodySmall,
                    color = LineaColors.TextTertiary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

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
                        Toast.makeText(context, "Call history cleared", Toast.LENGTH_SHORT).show()
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
