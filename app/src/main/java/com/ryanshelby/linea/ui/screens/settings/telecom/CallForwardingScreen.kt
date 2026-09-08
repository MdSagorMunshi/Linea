package com.ryanshelby.linea.ui.screens.settings.telecom

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneForwarded
import androidx.compose.material.icons.filled.PhonePaused
import androidx.compose.material.icons.filled.SignalWifiBad
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.telecom.SimAccountInfo
import com.ryanshelby.linea.ui.components.FrostedGlassBox
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography

enum class ForwardingCondition(
    val title: String,
    val description: String,
    val mmiActivateCode: String,
    val mmiDeactivateCode: String,
    val mmiQueryCode: String,
    val icon: ImageVector
) {
    ALWAYS(
        title = "Always Forward",
        description = "Forward all incoming calls unconditionally",
        mmiActivateCode = "*21*",
        mmiDeactivateCode = "##21#",
        mmiQueryCode = "*#21#",
        icon = Icons.Filled.PhoneForwarded
    ),
    WHEN_BUSY(
        title = "When Busy",
        description = "Forward when you decline or are on another call",
        mmiActivateCode = "*67*",
        mmiDeactivateCode = "##67#",
        mmiQueryCode = "*#67#",
        icon = Icons.Filled.PhonePaused
    ),
    WHEN_UNANSWERED(
        title = "When Unanswered",
        description = "Forward when incoming calls are not picked up",
        mmiActivateCode = "*61*",
        mmiDeactivateCode = "##61#",
        mmiQueryCode = "*#61#",
        icon = Icons.Filled.CallMissed
    ),
    WHEN_UNREACHABLE(
        title = "When Unreachable",
        description = "Forward when out of range or in airplane mode",
        mmiActivateCode = "*62*",
        mmiDeactivateCode = "##62#",
        mmiQueryCode = "*#62#",
        icon = Icons.Filled.SignalWifiBad
    )
}

@Composable
fun CallForwardingScreen(
    simAccounts: List<SimAccountInfo> = emptyList(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("linea_call_forwarding", Context.MODE_PRIVATE) }

    var selectedSimIndex by remember { mutableIntStateOf(0) }

    var activeConditionToEdit by remember { mutableStateOf<ForwardingCondition?>(null) }
    var enteredNumber by remember { mutableStateOf("") }

    var alwaysEnabled by remember { mutableStateOf(false) }
    var alwaysNumber by remember { mutableStateOf("") }

    var busyEnabled by remember { mutableStateOf(false) }
    var busyNumber by remember { mutableStateOf("") }

    var unansweredEnabled by remember { mutableStateOf(false) }
    var unansweredNumber by remember { mutableStateOf("") }

    var unreachableEnabled by remember { mutableStateOf(false) }
    var unreachableNumber by remember { mutableStateOf("") }

    // Load state whenever selected SIM slot changes
    LaunchedEffect(selectedSimIndex) {
        alwaysEnabled = prefs.getBoolean("sim_${selectedSimIndex}_always_en", false)
        alwaysNumber = prefs.getString("sim_${selectedSimIndex}_always_num", "") ?: ""
        busyEnabled = prefs.getBoolean("sim_${selectedSimIndex}_busy_en", false)
        busyNumber = prefs.getString("sim_${selectedSimIndex}_busy_num", "") ?: ""
        unansweredEnabled = prefs.getBoolean("sim_${selectedSimIndex}_unanswered_en", false)
        unansweredNumber = prefs.getString("sim_${selectedSimIndex}_unanswered_num", "") ?: ""
        unreachableEnabled = prefs.getBoolean("sim_${selectedSimIndex}_unreachable_en", false)
        unreachableNumber = prefs.getString("sim_${selectedSimIndex}_unreachable_num", "") ?: ""
    }

    fun saveSimState() {
        prefs.edit().apply {
            putBoolean("sim_${selectedSimIndex}_always_en", alwaysEnabled)
            putString("sim_${selectedSimIndex}_always_num", alwaysNumber)
            putBoolean("sim_${selectedSimIndex}_busy_en", busyEnabled)
            putString("sim_${selectedSimIndex}_busy_num", busyNumber)
            putBoolean("sim_${selectedSimIndex}_unanswered_en", unansweredEnabled)
            putString("sim_${selectedSimIndex}_unanswered_num", unansweredNumber)
            putBoolean("sim_${selectedSimIndex}_unreachable_en", unreachableEnabled)
            putString("sim_${selectedSimIndex}_unreachable_num", unreachableNumber)
            apply()
        }
    }

    fun sendMmiIntent(code: String) {
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
                        text = "Call Forwarding",
                        style = LineaTypography.headlineMedium,
                        color = LineaColors.TextPrimary
                    )
                    Text(
                        text = "Carrier cellular forwarding rules & MMI controls",
                        style = LineaTypography.bodySmall,
                        color = LineaColors.TextSecondary
                    )
                }
            }

            // SIM Selector Pill if multiple SIMs are detected
            if (simAccounts.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    simAccounts.forEachIndexed { index, sim ->
                        val isSelected = selectedSimIndex == index
                        FrostedGlassBox(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedSimIndex = index },
                            fillColor = if (isSelected) LineaColors.TitaniumBlue else LineaColors.GlassFill,
                            borderColor = if (isSelected) LineaColors.TitaniumBlue else LineaColors.GlassBorder
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SimCard,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else LineaColors.TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = sim.displayName.ifBlank { "SIM ${sim.slotIndex + 1}" },
                                    style = LineaTypography.labelMedium,
                                    color = if (isSelected) Color.White else LineaColors.TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Always Forward
                ForwardingItemCard(
                    condition = ForwardingCondition.ALWAYS,
                    isEnabled = alwaysEnabled,
                    forwardingNumber = alwaysNumber,
                    onToggle = { enabled ->
                        if (enabled && alwaysNumber.isBlank()) {
                            activeConditionToEdit = ForwardingCondition.ALWAYS
                            enteredNumber = ""
                        } else {
                            alwaysEnabled = enabled
                            saveSimState()
                            if (enabled) {
                                sendMmiIntent("${ForwardingCondition.ALWAYS.mmiActivateCode}$alwaysNumber#")
                            } else {
                                sendMmiIntent(ForwardingCondition.ALWAYS.mmiDeactivateCode)
                            }
                        }
                    },
                    onEditNumber = {
                        activeConditionToEdit = ForwardingCondition.ALWAYS
                        enteredNumber = alwaysNumber
                    }
                )

                // 2. When Busy
                ForwardingItemCard(
                    condition = ForwardingCondition.WHEN_BUSY,
                    isEnabled = busyEnabled,
                    forwardingNumber = busyNumber,
                    onToggle = { enabled ->
                        if (enabled && busyNumber.isBlank()) {
                            activeConditionToEdit = ForwardingCondition.WHEN_BUSY
                            enteredNumber = ""
                        } else {
                            busyEnabled = enabled
                            saveSimState()
                            if (enabled) {
                                sendMmiIntent("${ForwardingCondition.WHEN_BUSY.mmiActivateCode}$busyNumber#")
                            } else {
                                sendMmiIntent(ForwardingCondition.WHEN_BUSY.mmiDeactivateCode)
                            }
                        }
                    },
                    onEditNumber = {
                        activeConditionToEdit = ForwardingCondition.WHEN_BUSY
                        enteredNumber = busyNumber
                    }
                )

                // 3. When Unanswered
                ForwardingItemCard(
                    condition = ForwardingCondition.WHEN_UNANSWERED,
                    isEnabled = unansweredEnabled,
                    forwardingNumber = unansweredNumber,
                    onToggle = { enabled ->
                        if (enabled && unansweredNumber.isBlank()) {
                            activeConditionToEdit = ForwardingCondition.WHEN_UNANSWERED
                            enteredNumber = ""
                        } else {
                            unansweredEnabled = enabled
                            saveSimState()
                            if (enabled) {
                                sendMmiIntent("${ForwardingCondition.WHEN_UNANSWERED.mmiActivateCode}$unansweredNumber#")
                            } else {
                                sendMmiIntent(ForwardingCondition.WHEN_UNANSWERED.mmiDeactivateCode)
                            }
                        }
                    },
                    onEditNumber = {
                        activeConditionToEdit = ForwardingCondition.WHEN_UNANSWERED
                        enteredNumber = unansweredNumber
                    }
                )

                // 4. When Unreachable
                ForwardingItemCard(
                    condition = ForwardingCondition.WHEN_UNREACHABLE,
                    isEnabled = unreachableEnabled,
                    forwardingNumber = unreachableNumber,
                    onToggle = { enabled ->
                        if (enabled && unreachableNumber.isBlank()) {
                            activeConditionToEdit = ForwardingCondition.WHEN_UNREACHABLE
                            enteredNumber = ""
                        } else {
                            unreachableEnabled = enabled
                            saveSimState()
                            if (enabled) {
                                sendMmiIntent("${ForwardingCondition.WHEN_UNREACHABLE.mmiActivateCode}$unreachableNumber#")
                            } else {
                                sendMmiIntent(ForwardingCondition.WHEN_UNREACHABLE.mmiDeactivateCode)
                            }
                        }
                    },
                    onEditNumber = {
                        activeConditionToEdit = ForwardingCondition.WHEN_UNREACHABLE
                        enteredNumber = unreachableNumber
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Carrier Status Query Card
                FrostedGlassBox(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                sendMmiIntent("*#21#")
                                Toast.makeText(context, "Querying network forwarding status...", Toast.LENGTH_SHORT).show()
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = LineaColors.TitaniumBlue,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Query Network Status (*#21#)",
                                style = LineaTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = LineaColors.TextPrimary
                            )
                            Text(
                                text = "Interrogates carrier network for active forward rules",
                                style = LineaTypography.bodySmall,
                                color = LineaColors.TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Edit Number Dialog
        if (activeConditionToEdit != null) {
            val cond = activeConditionToEdit!!
            AlertDialog(
                onDismissRequest = { activeConditionToEdit = null },
                containerColor = LineaColors.BackgroundElevated,
                title = {
                    Text(
                        text = cond.title,
                        style = LineaTypography.titleMedium,
                        color = LineaColors.TextPrimary
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Enter the destination phone number to forward calls to:",
                            style = LineaTypography.bodySmall,
                            color = LineaColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = enteredNumber,
                            onValueChange = { enteredNumber = it },
                            placeholder = { Text("Phone number", color = LineaColors.TextTertiary) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = LineaColors.GlassFill,
                                unfocusedContainerColor = LineaColors.GlassFill,
                                focusedBorderColor = LineaColors.TitaniumBlue,
                                unfocusedBorderColor = LineaColors.GlassBorder,
                                focusedTextColor = LineaColors.TextPrimary,
                                unfocusedTextColor = LineaColors.TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val num = enteredNumber.trim()
                            when (cond) {
                                ForwardingCondition.ALWAYS -> {
                                    alwaysNumber = num
                                    alwaysEnabled = num.isNotBlank()
                                    if (alwaysEnabled) sendMmiIntent("${cond.mmiActivateCode}$num#")
                                }
                                ForwardingCondition.WHEN_BUSY -> {
                                    busyNumber = num
                                    busyEnabled = num.isNotBlank()
                                    if (busyEnabled) sendMmiIntent("${cond.mmiActivateCode}$num#")
                                }
                                ForwardingCondition.WHEN_UNANSWERED -> {
                                    unansweredNumber = num
                                    unansweredEnabled = num.isNotBlank()
                                    if (unansweredEnabled) sendMmiIntent("${cond.mmiActivateCode}$num#")
                                }
                                ForwardingCondition.WHEN_UNREACHABLE -> {
                                    unreachableNumber = num
                                    unreachableEnabled = num.isNotBlank()
                                    if (unreachableEnabled) sendMmiIntent("${cond.mmiActivateCode}$num#")
                                }
                            }
                            saveSimState()
                            activeConditionToEdit = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LineaColors.TitaniumBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save & Apply", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { activeConditionToEdit = null }) {
                        Text("Cancel", color = LineaColors.TextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
private fun ForwardingItemCard(
    condition: ForwardingCondition,
    isEnabled: Boolean,
    forwardingNumber: String,
    onToggle: (Boolean) -> Unit,
    onEditNumber: () -> Unit
) {
    FrostedGlassBox(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        borderColor = if (isEnabled) LineaColors.TitaniumBlue.copy(alpha = 0.5f) else LineaColors.GlassBorder
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
                            if (isEnabled) LineaColors.TitaniumBlue.copy(alpha = 0.2f) else LineaColors.GlassFill
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = condition.icon,
                        contentDescription = null,
                        tint = if (isEnabled) LineaColors.TitaniumBlue else LineaColors.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = condition.title,
                        style = LineaTypography.titleMedium,
                        color = LineaColors.TextPrimary
                    )
                    Text(
                        text = if (isEnabled && forwardingNumber.isNotBlank()) "Forwarding to $forwardingNumber" else condition.description,
                        style = LineaTypography.bodySmall,
                        color = if (isEnabled) LineaColors.TitaniumBlue else LineaColors.TextSecondary
                    )
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = LineaColors.TitaniumBlue,
                        uncheckedThumbColor = LineaColors.TextTertiary,
                        uncheckedTrackColor = LineaColors.GlassFill
                    )
                )
            }

            if (isEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(LineaColors.GlassFill)
                        .clickable(onClick = onEditNumber)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = forwardingNumber.ifBlank { "Tap to set number" },
                        style = LineaTypography.bodyMedium,
                        color = if (forwardingNumber.isNotBlank()) LineaColors.TextPrimary else LineaColors.TextTertiary
                    )
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit Number",
                        tint = LineaColors.TitaniumBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
