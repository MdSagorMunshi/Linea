package com.ryanshelby.linea.ui.screens.settings.telecom

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.PublicOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.telecom.SimAccountInfo
import com.ryanshelby.linea.ui.components.FrostedGlassBox
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography

enum class BarringType(
    val title: String,
    val subtitle: String,
    val code: String, // 33, 331, 332, 35, 351
    val icon: ImageVector
) {
    ALL_OUTGOING(
        title = "All Outgoing Calls",
        subtitle = "Block placing any outgoing calls",
        code = "33",
        icon = Icons.Filled.Block
    ),
    INTL_OUTGOING(
        title = "International Outgoing",
        subtitle = "Block placing calls to foreign numbers",
        code = "331",
        icon = Icons.Filled.PublicOff
    ),
    INTL_ROAMING_OUTGOING(
        title = "International While Roaming",
        subtitle = "Block calls except to home country when abroad",
        code = "332",
        icon = Icons.Filled.Public
    ),
    ALL_INCOMING(
        title = "All Incoming Calls",
        subtitle = "Block all incoming calls to this SIM",
        code = "35",
        icon = Icons.Filled.CallReceived
    ),
    ROAMING_INCOMING(
        title = "Incoming When Roaming",
        subtitle = "Block incoming calls when connected to foreign carriers",
        code = "351",
        icon = Icons.Filled.PublicOff
    )
}

@Composable
fun CallBarringFdnScreen(
    simAccounts: List<SimAccountInfo> = emptyList(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("linea_call_barring", Context.MODE_PRIVATE) }

    var selectedSimIndex by remember { mutableIntStateOf(0) }

    // Dialog states
    var pendingBarringType by remember { mutableStateOf<BarringType?>(null) }
    var pendingBarringTargetState by remember { mutableStateOf(false) }
    var barringPinInput by remember { mutableStateOf("") }

    var showChangeBarringPinDialog by remember { mutableStateOf(false) }
    var oldBarringPin by remember { mutableStateOf("") }
    var newBarringPin by remember { mutableStateOf("") }
    var confirmNewBarringPin by remember { mutableStateOf("") }

    var showDeactivateAllDialog by remember { mutableStateOf(false) }
    var deactivateAllPin by remember { mutableStateOf("") }

    // FDN states
    var fdnEnabled by remember { mutableStateOf(false) }
    var showFdnPin2Dialog by remember { mutableStateOf(false) }
    var fdnPin2Input by remember { mutableStateOf("") }
    var showChangePin2Dialog by remember { mutableStateOf(false) }
    var oldPin2 by remember { mutableStateOf("") }
    var newPin2 by remember { mutableStateOf("") }
    var confirmNewPin2 by remember { mutableStateOf("") }

    // Barring states per SIM
    var barAllOutgoing by remember { mutableStateOf(false) }
    var barIntlOutgoing by remember { mutableStateOf(false) }
    var barRoamingOutgoing by remember { mutableStateOf(false) }
    var barAllIncoming by remember { mutableStateOf(false) }
    var barRoamingIncoming by remember { mutableStateOf(false) }

    // Load state for selected SIM
    LaunchedEffect(selectedSimIndex) {
        barAllOutgoing = prefs.getBoolean("sim_${selectedSimIndex}_bar_33", false)
        barIntlOutgoing = prefs.getBoolean("sim_${selectedSimIndex}_bar_331", false)
        barRoamingOutgoing = prefs.getBoolean("sim_${selectedSimIndex}_bar_332", false)
        barAllIncoming = prefs.getBoolean("sim_${selectedSimIndex}_bar_35", false)
        barRoamingIncoming = prefs.getBoolean("sim_${selectedSimIndex}_bar_351", false)
        fdnEnabled = prefs.getBoolean("sim_${selectedSimIndex}_fdn_en", false)
    }

    fun saveBarringState(type: BarringType, enabled: Boolean) {
        prefs.edit().putBoolean("sim_${selectedSimIndex}_bar_${type.code}", enabled).apply()
        when (type) {
            BarringType.ALL_OUTGOING -> barAllOutgoing = enabled
            BarringType.INTL_OUTGOING -> barIntlOutgoing = enabled
            BarringType.INTL_ROAMING_OUTGOING -> barRoamingOutgoing = enabled
            BarringType.ALL_INCOMING -> barAllIncoming = enabled
            BarringType.ROAMING_INCOMING -> barRoamingIncoming = enabled
        }
    }

    fun sendMmi(code: String) {
        val selectedSim = simAccounts.getOrNull(selectedSimIndex)
        val encodedHash = Uri.encode("#")
        val uri = Uri.parse("tel:" + code.replace("#", encodedHash))
        val intent = Intent(Intent.ACTION_CALL, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (selectedSim?.phoneAccountHandle != null) {
                putExtra(android.telecom.TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, selectedSim.phoneAccountHandle)
            }
        }
        try {
            context.startActivity(intent)
        } catch (_: SecurityException) {
            val dialIntent = Intent(Intent.ACTION_DIAL, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                if (selectedSim?.phoneAccountHandle != null) {
                    putExtra(android.telecom.TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, selectedSim.phoneAccountHandle)
                }
            }
            context.startActivity(dialIntent)
        }
    }

    fun launchSystemFdnSettings() {
        val simName = simAccounts.getOrNull(selectedSimIndex)?.displayName ?: "Selected SIM"
        val candidates = listOf(
            Intent().setComponent(ComponentName("com.android.phone", "com.android.phone.settings.fdn.FdnSetting")),
            Intent().setComponent(ComponentName("com.android.phone", "com.android.phone.CallFeaturesSetting")),
            Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS),
            Intent(Settings.ACTION_WIRELESS_SETTINGS)
        )

        var launched = false
        for (candidate in candidates) {
            try {
                candidate.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(candidate)
                launched = true
                break
            } catch (_: Exception) {}
        }
        if (!launched) {
            Toast.makeText(context, "Open System Settings > Mobile Network to configure FDN for $simName", Toast.LENGTH_LONG).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LineaColors.BackgroundGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = LineaColors.TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = "Call Barring & FDN",
                        style = LineaTypography.headlineMedium,
                        color = LineaColors.TextPrimary
                    )
                    Text(
                        text = "Carrier call restrictions & Fixed Dialing Numbers",
                        style = LineaTypography.bodySmall,
                        color = LineaColors.TextSecondary
                    )
                }
            }

            // Multi-SIM Selector Pill (if multi-SIM detected)
            if (simAccounts.size > 1) {
                FrostedGlassBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    fillColor = LineaColors.SurfaceElevated.copy(alpha = 0.6f),
                    borderColor = LineaColors.GlassBorder
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        simAccounts.forEachIndexed { index, account ->
                            val isSelected = selectedSimIndex == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) LineaColors.TitaniumBlue.copy(alpha = 0.25f)
                                        else Color.Transparent
                                    )
                                    .border(
                                        width = if (isSelected) 1.dp else 0.dp,
                                        color = if (isSelected) LineaColors.TitaniumBlue else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedSimIndex = index }
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SimCard,
                                        contentDescription = null,
                                        tint = if (isSelected) LineaColors.TitaniumBlue else LineaColors.TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${account.displayName.ifBlank { "SIM ${account.slotIndex + 1}" }} (${account.carrierName.ifBlank { "Carrier" }})",
                                        style = LineaTypography.labelMedium,
                                        color = if (isSelected) LineaColors.TitaniumBlue else LineaColors.TextSecondary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section: Call Barring
                Text(
                    text = "NETWORK CALL BARRING",
                    style = LineaTypography.titleSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = LineaColors.TitaniumBlue
                )

                BarringItemCard(
                    title = BarringType.ALL_OUTGOING.title,
                    subtitle = BarringType.ALL_OUTGOING.subtitle,
                    icon = BarringType.ALL_OUTGOING.icon,
                    isChecked = barAllOutgoing,
                    onToggle = { targetState ->
                        pendingBarringType = BarringType.ALL_OUTGOING
                        pendingBarringTargetState = targetState
                        barringPinInput = ""
                    },
                    onQueryStatus = {
                        sendMmi("*#33#")
                    }
                )

                BarringItemCard(
                    title = BarringType.INTL_OUTGOING.title,
                    subtitle = BarringType.INTL_OUTGOING.subtitle,
                    icon = BarringType.INTL_OUTGOING.icon,
                    isChecked = barIntlOutgoing,
                    onToggle = { targetState ->
                        pendingBarringType = BarringType.INTL_OUTGOING
                        pendingBarringTargetState = targetState
                        barringPinInput = ""
                    },
                    onQueryStatus = {
                        sendMmi("*#331#")
                    }
                )

                BarringItemCard(
                    title = BarringType.INTL_ROAMING_OUTGOING.title,
                    subtitle = BarringType.INTL_ROAMING_OUTGOING.subtitle,
                    icon = BarringType.INTL_ROAMING_OUTGOING.icon,
                    isChecked = barRoamingOutgoing,
                    onToggle = { targetState ->
                        pendingBarringType = BarringType.INTL_ROAMING_OUTGOING
                        pendingBarringTargetState = targetState
                        barringPinInput = ""
                    },
                    onQueryStatus = {
                        sendMmi("*#332#")
                    }
                )

                BarringItemCard(
                    title = BarringType.ALL_INCOMING.title,
                    subtitle = BarringType.ALL_INCOMING.subtitle,
                    icon = BarringType.ALL_INCOMING.icon,
                    isChecked = barAllIncoming,
                    onToggle = { targetState ->
                        pendingBarringType = BarringType.ALL_INCOMING
                        pendingBarringTargetState = targetState
                        barringPinInput = ""
                    },
                    onQueryStatus = {
                        sendMmi("*#35#")
                    }
                )

                BarringItemCard(
                    title = BarringType.ROAMING_INCOMING.title,
                    subtitle = BarringType.ROAMING_INCOMING.subtitle,
                    icon = BarringType.ROAMING_INCOMING.icon,
                    isChecked = barRoamingIncoming,
                    onToggle = { targetState ->
                        pendingBarringType = BarringType.ROAMING_INCOMING
                        pendingBarringTargetState = targetState
                        barringPinInput = ""
                    },
                    onQueryStatus = {
                        sendMmi("*#351#")
                    }
                )

                // Barring Actions: Deactivate All & Change Barring Password
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            deactivateAllPin = ""
                            showDeactivateAllDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LockOpen,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = LineaColors.Danger
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Deactivate All",
                            style = LineaTypography.labelMedium,
                            color = LineaColors.Danger
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            oldBarringPin = ""
                            newBarringPin = ""
                            confirmNewBarringPin = ""
                            showChangeBarringPinDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Key,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = LineaColors.TitaniumBlue
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Change PIN",
                            style = LineaTypography.labelMedium,
                            color = LineaColors.TitaniumBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Section: Fixed Dialing Numbers (FDN)
                Text(
                    text = "FIXED DIALING NUMBERS (FDN)",
                    style = LineaTypography.titleSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = LineaColors.TitaniumBlue
                )

                FrostedGlassBox(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    borderColor = if (fdnEnabled) LineaColors.Warning.copy(alpha = 0.5f) else LineaColors.GlassBorder
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (fdnEnabled) LineaColors.Warning.copy(alpha = 0.2f) else LineaColors.GlassFill
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Security,
                                    contentDescription = null,
                                    tint = if (fdnEnabled) LineaColors.Warning else LineaColors.TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "FDN Mode",
                                    style = LineaTypography.titleMedium,
                                    color = LineaColors.TextPrimary
                                )
                                Text(
                                    text = if (fdnEnabled) "Restricted to authorized numbers" else "Disabled (all dials permitted)",
                                    style = LineaTypography.bodySmall,
                                    color = if (fdnEnabled) LineaColors.Warning else LineaColors.TextSecondary
                                )
                            }

                            Switch(
                                checked = fdnEnabled,
                                onCheckedChange = { _ ->
                                    fdnPin2Input = ""
                                    showFdnPin2Dialog = true
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = LineaColors.Warning,
                                    uncheckedThumbColor = LineaColors.TextTertiary,
                                    uncheckedTrackColor = LineaColors.GlassFill
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = LineaColors.TextTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Requires SIM PIN2 code. 3 incorrect attempts will lock PIN2.",
                                style = LineaTypography.bodySmall.copy(fontSize = 12.sp),
                                color = LineaColors.TextTertiary
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { launchSystemFdnSettings() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = LineaColors.TitaniumBlue)
                            ) {
                                Text(
                                    text = "Manage FDN List",
                                    style = LineaTypography.labelMedium,
                                    color = Color.White
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    oldPin2 = ""
                                    newPin2 = ""
                                    confirmNewPin2 = ""
                                    showChangePin2Dialog = true
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "Change PIN2",
                                    style = LineaTypography.labelMedium,
                                    color = LineaColors.TextPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Dialog: Carrier Barring PIN prompt
    pendingBarringType?.let { bType ->
        val isActivating = pendingBarringTargetState
        AlertDialog(
            onDismissRequest = { pendingBarringType = null },
            title = {
                Text(
                    text = if (isActivating) "Enable ${bType.title}" else "Disable ${bType.title}",
                    style = LineaTypography.titleMedium,
                    color = LineaColors.TextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter your 4-digit Network Barring PIN from your carrier (usually 0000 or 1234).",
                        style = LineaTypography.bodyMedium,
                        color = LineaColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = barringPinInput,
                        onValueChange = { if (it.length <= 8) barringPinInput = it },
                        label = { Text("Barring PIN") },
                        placeholder = { Text("e.g. 0000") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = LineaColors.TextPrimary,
                            unfocusedTextColor = LineaColors.TextPrimary,
                            focusedBorderColor = LineaColors.TitaniumBlue,
                            unfocusedBorderColor = LineaColors.GlassBorder
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (barringPinInput.isBlank()) {
                            Toast.makeText(context, "Please enter your carrier Barring PIN", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        // Prefix: * for activate, # for deactivate
                        val prefix = if (isActivating) "*" else "#"
                        val mmi = "$prefix${bType.code}*$barringPinInput#"
                        sendMmi(mmi)
                        saveBarringState(bType, isActivating)
                        pendingBarringType = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LineaColors.TitaniumBlue)
                ) {
                    Text("Execute", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingBarringType = null }) {
                    Text("Cancel", color = LineaColors.TextSecondary)
                }
            },
            containerColor = LineaColors.SurfaceElevated
        )
    }

    // Dialog: Deactivate All Barring
    if (showDeactivateAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeactivateAllDialog = false },
            title = {
                Text(
                    text = "Deactivate All Barring",
                    style = LineaTypography.titleMedium,
                    color = LineaColors.TextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "This will disable all incoming and outgoing call barring on this SIM (#330*PIN#).",
                        style = LineaTypography.bodyMedium,
                        color = LineaColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = deactivateAllPin,
                        onValueChange = { if (it.length <= 8) deactivateAllPin = it },
                        label = { Text("Barring PIN") },
                        placeholder = { Text("e.g. 0000") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = LineaColors.TextPrimary,
                            unfocusedTextColor = LineaColors.TextPrimary,
                            focusedBorderColor = LineaColors.TitaniumBlue,
                            unfocusedBorderColor = LineaColors.GlassBorder
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (deactivateAllPin.isBlank()) {
                            Toast.makeText(context, "Please enter your carrier Barring PIN", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        sendMmi("#330*$deactivateAllPin#")
                        // Clear all local barring flags
                        BarringType.values().forEach { saveBarringState(it, false) }
                        showDeactivateAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LineaColors.Danger)
                ) {
                    Text("Deactivate All", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeactivateAllDialog = false }) {
                    Text("Cancel", color = LineaColors.TextSecondary)
                }
            },
            containerColor = LineaColors.SurfaceElevated
        )
    }

    // Dialog: Change Barring Password
    if (showChangeBarringPinDialog) {
        AlertDialog(
            onDismissRequest = { showChangeBarringPinDialog = false },
            title = {
                Text(
                    text = "Change Barring Password",
                    style = LineaTypography.titleMedium,
                    color = LineaColors.TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Dials GSM standard **03*330*OldPIN*NewPIN*NewPIN# to update network password.",
                        style = LineaTypography.bodySmall,
                        color = LineaColors.TextSecondary
                    )
                    OutlinedTextField(
                        value = oldBarringPin,
                        onValueChange = { if (it.length <= 8) oldBarringPin = it },
                        label = { Text("Current Barring PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = LineaColors.TextPrimary,
                            unfocusedTextColor = LineaColors.TextPrimary,
                            focusedBorderColor = LineaColors.TitaniumBlue,
                            unfocusedBorderColor = LineaColors.GlassBorder
                        )
                    )
                    OutlinedTextField(
                        value = newBarringPin,
                        onValueChange = { if (it.length <= 8) newBarringPin = it },
                        label = { Text("New Barring PIN (4 digits)") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = LineaColors.TextPrimary,
                            unfocusedTextColor = LineaColors.TextPrimary,
                            focusedBorderColor = LineaColors.TitaniumBlue,
                            unfocusedBorderColor = LineaColors.GlassBorder
                        )
                    )
                    OutlinedTextField(
                        value = confirmNewBarringPin,
                        onValueChange = { if (it.length <= 8) confirmNewBarringPin = it },
                        label = { Text("Confirm New Barring PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = LineaColors.TextPrimary,
                            unfocusedTextColor = LineaColors.TextPrimary,
                            focusedBorderColor = LineaColors.TitaniumBlue,
                            unfocusedBorderColor = LineaColors.GlassBorder
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (oldBarringPin.isBlank() || newBarringPin.isBlank() || confirmNewBarringPin.isBlank()) {
                            Toast.makeText(context, "Please fill in all PIN fields", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (newBarringPin != confirmNewBarringPin) {
                            Toast.makeText(context, "New PIN and Confirm PIN do not match", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        sendMmi("**03*330*$oldBarringPin*$newBarringPin*$confirmNewBarringPin#")
                        showChangeBarringPinDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LineaColors.TitaniumBlue)
                ) {
                    Text("Change PIN", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangeBarringPinDialog = false }) {
                    Text("Cancel", color = LineaColors.TextSecondary)
                }
            },
            containerColor = LineaColors.SurfaceElevated
        )
    }

    // Dialog: SIM PIN2 prompt for FDN Toggle
    if (showFdnPin2Dialog) {
        val targetFdnState = !fdnEnabled
        AlertDialog(
            onDismissRequest = { showFdnPin2Dialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = LineaColors.Warning,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (targetFdnState) "Enable FDN Mode" else "Disable FDN Mode",
                        style = LineaTypography.titleMedium,
                        color = LineaColors.TextPrimary
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Enter your carrier SIM PIN2 to toggle Fixed Dialing Numbers. If you enter the wrong PIN2 3 times, your SIM PIN2 will be blocked and require PUK2.",
                        style = LineaTypography.bodyMedium,
                        color = LineaColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = fdnPin2Input,
                        onValueChange = { if (it.length <= 8) fdnPin2Input = it },
                        label = { Text("SIM PIN2 Code") },
                        placeholder = { Text("e.g. 5678") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = LineaColors.TextPrimary,
                            unfocusedTextColor = LineaColors.TextPrimary,
                            focusedBorderColor = LineaColors.Warning,
                            unfocusedBorderColor = LineaColors.GlassBorder
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (fdnPin2Input.length < 4) {
                            Toast.makeText(context, "PIN2 is typically 4-8 digits", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        fdnEnabled = targetFdnState
                        prefs.edit().putBoolean("sim_${selectedSimIndex}_fdn_en", targetFdnState).apply()
                        showFdnPin2Dialog = false
                        Toast.makeText(
                            context,
                            if (targetFdnState) "FDN Mode Enabled on SIM slot" else "FDN Mode Disabled",
                            Toast.LENGTH_SHORT
                        ).show()
                        launchSystemFdnSettings()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LineaColors.Warning)
                ) {
                    Text("Confirm with PIN2", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFdnPin2Dialog = false }) {
                    Text("Cancel", color = LineaColors.TextSecondary)
                }
            },
            containerColor = LineaColors.SurfaceElevated
        )
    }

    // Dialog: Change SIM PIN2
    if (showChangePin2Dialog) {
        AlertDialog(
            onDismissRequest = { showChangePin2Dialog = false },
            title = {
                Text(
                    text = "Change SIM PIN2",
                    style = LineaTypography.titleMedium,
                    color = LineaColors.TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Dials GSM standard **042*OldPIN2*NewPIN2*NewPIN2# on the selected SIM.",
                        style = LineaTypography.bodySmall,
                        color = LineaColors.TextSecondary
                    )
                    OutlinedTextField(
                        value = oldPin2,
                        onValueChange = { if (it.length <= 8) oldPin2 = it },
                        label = { Text("Current SIM PIN2") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = LineaColors.TextPrimary,
                            unfocusedTextColor = LineaColors.TextPrimary,
                            focusedBorderColor = LineaColors.TitaniumBlue,
                            unfocusedBorderColor = LineaColors.GlassBorder
                        )
                    )
                    OutlinedTextField(
                        value = newPin2,
                        onValueChange = { if (it.length <= 8) newPin2 = it },
                        label = { Text("New SIM PIN2 (4-8 digits)") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = LineaColors.TextPrimary,
                            unfocusedTextColor = LineaColors.TextPrimary,
                            focusedBorderColor = LineaColors.TitaniumBlue,
                            unfocusedBorderColor = LineaColors.GlassBorder
                        )
                    )
                    OutlinedTextField(
                        value = confirmNewPin2,
                        onValueChange = { if (it.length <= 8) confirmNewPin2 = it },
                        label = { Text("Confirm New SIM PIN2") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = LineaColors.TextPrimary,
                            unfocusedTextColor = LineaColors.TextPrimary,
                            focusedBorderColor = LineaColors.TitaniumBlue,
                            unfocusedBorderColor = LineaColors.GlassBorder
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (oldPin2.isBlank() || newPin2.isBlank() || confirmNewPin2.isBlank()) {
                            Toast.makeText(context, "Please fill in all PIN2 fields", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (newPin2 != confirmNewPin2) {
                            Toast.makeText(context, "New PIN2 and Confirm PIN2 do not match", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        sendMmi("**042*$oldPin2*$newPin2*$confirmNewPin2#")
                        showChangePin2Dialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LineaColors.TitaniumBlue)
                ) {
                    Text("Update PIN2", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePin2Dialog = false }) {
                    Text("Cancel", color = LineaColors.TextSecondary)
                }
            },
            containerColor = LineaColors.SurfaceElevated
        )
    }
}

@Composable
fun BarringItemCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isChecked: Boolean,
    onToggle: (Boolean) -> Unit,
    onQueryStatus: () -> Unit
) {
    FrostedGlassBox(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        borderColor = if (isChecked) LineaColors.Danger.copy(alpha = 0.5f) else LineaColors.GlassBorder
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isChecked) LineaColors.Danger.copy(alpha = 0.2f) else LineaColors.GlassFill
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isChecked) LineaColors.Danger else LineaColors.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

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

                Switch(
                    checked = isChecked,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = LineaColors.Danger,
                        uncheckedThumbColor = LineaColors.TextTertiary,
                        uncheckedTrackColor = LineaColors.GlassFill
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onQueryStatus) {
                    Text(
                        text = "Check Network Status",
                        style = LineaTypography.labelMedium,
                        color = LineaColors.TitaniumBlue
                    )
                }
            }
        }
    }
}
