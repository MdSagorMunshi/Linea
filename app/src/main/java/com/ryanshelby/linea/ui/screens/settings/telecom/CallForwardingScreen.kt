package com.ryanshelby.linea.ui.screens.settings.telecom

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhoneForwarded
import androidx.compose.material.icons.filled.PhonePaused
import androidx.compose.material.icons.filled.SignalWifiBad
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
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.ui.components.FrostedGlassBox
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaTypography

enum class ForwardingCondition(
    val title: String,
    val description: String,
    val mmiActivateCode: String,
    val mmiDeactivateCode: String,
    val icon: ImageVector
) {
    ALWAYS(
        title = "Always Forward",
        description = "Forward all incoming calls unconditionally",
        mmiActivateCode = "*21*",
        mmiDeactivateCode = "##21#",
        icon = Icons.Filled.PhoneForwarded
    ),
    WHEN_BUSY(
        title = "When Busy",
        description = "Forward when you decline or are on another call",
        mmiActivateCode = "*67*",
        mmiDeactivateCode = "##67#",
        icon = Icons.Filled.PhonePaused
    ),
    WHEN_UNANSWERED(
        title = "When Unanswered",
        description = "Forward when incoming calls are not picked up",
        mmiActivateCode = "*61*",
        mmiDeactivateCode = "##61#",
        icon = Icons.Filled.CallMissed
    ),
    WHEN_UNREACHABLE(
        title = "When Unreachable",
        description = "Forward when out of range or in airplane mode",
        mmiActivateCode = "*62*",
        mmiDeactivateCode = "##62#",
        icon = Icons.Filled.SignalWifiBad
    )
}

@Composable
fun CallForwardingScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
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

    fun sendMmiIntent(code: String) {
        val encodedHash = Uri.encode("#")
        val uri = Uri.parse("tel:" + code.replace("#", encodedHash))
        val intent = Intent(Intent.ACTION_DIAL, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
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

            Spacer(modifier = Modifier.height(8.dp))

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
                        alwaysEnabled = enabled
                        if (enabled) {
                            sendMmiIntent("${ForwardingCondition.ALWAYS.mmiActivateCode}$alwaysNumber#")
                        } else {
                            sendMmiIntent(ForwardingCondition.ALWAYS.mmiDeactivateCode)
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
                        busyEnabled = enabled
                        if (enabled) {
                            sendMmiIntent("${ForwardingCondition.WHEN_BUSY.mmiActivateCode}$busyNumber#")
                        } else {
                            sendMmiIntent(ForwardingCondition.WHEN_BUSY.mmiDeactivateCode)
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
                        unansweredEnabled = enabled
                        if (enabled) {
                            sendMmiIntent("${ForwardingCondition.WHEN_UNANSWERED.mmiActivateCode}$unansweredNumber#")
                        } else {
                            sendMmiIntent(ForwardingCondition.WHEN_UNANSWERED.mmiDeactivateCode)
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
                        unreachableEnabled = enabled
                        if (enabled) {
                            sendMmiIntent("${ForwardingCondition.WHEN_UNREACHABLE.mmiActivateCode}$unreachableNumber#")
                        } else {
                            sendMmiIntent(ForwardingCondition.WHEN_UNREACHABLE.mmiDeactivateCode)
                        }
                    },
                    onEditNumber = {
                        activeConditionToEdit = ForwardingCondition.WHEN_UNREACHABLE
                        enteredNumber = unreachableNumber
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Info Note
                FrostedGlassBox(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    borderColor = LineaColors.GlassBorder
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "CARRIER COMPATIBILITY NOTE",
                            style = LineaTypography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = LineaColors.TitaniumBlue
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Forwarding requests are handled directly by your cellular network via GSM/UMTS MMI strings. Ensure your carrier supports unconditional and conditional call diverting.",
                            style = LineaTypography.bodySmall,
                            color = LineaColors.TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Edit Number Dialog
        activeConditionToEdit?.let { condition ->
            AlertDialog(
                onDismissRequest = { activeConditionToEdit = null },
                containerColor = LineaColors.SurfaceElevated,
                title = {
                    Text(
                        text = "Forward ${condition.title}",
                        style = LineaTypography.titleLarge,
                        color = LineaColors.TextPrimary
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Enter phone number to receive diverted calls:",
                            style = LineaTypography.bodySmall,
                            color = LineaColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = enteredNumber,
                            onValueChange = { enteredNumber = it },
                            placeholder = { Text("e.g. +1 555 019 2000", color = LineaColors.TextTertiary) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LineaColors.TitaniumBlue,
                                unfocusedBorderColor = LineaColors.GlassBorder,
                                focusedContainerColor = LineaColors.GlassFill,
                                unfocusedContainerColor = LineaColors.GlassFill,
                                focusedTextColor = LineaColors.TextPrimary,
                                unfocusedTextColor = LineaColors.TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            when (condition) {
                                ForwardingCondition.ALWAYS -> alwaysNumber = enteredNumber
                                ForwardingCondition.WHEN_BUSY -> busyNumber = enteredNumber
                                ForwardingCondition.WHEN_UNANSWERED -> unansweredNumber = enteredNumber
                                ForwardingCondition.WHEN_UNREACHABLE -> unreachableNumber = enteredNumber
                            }
                            activeConditionToEdit = null
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LineaColors.TitaniumBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Save")
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
fun ForwardingItemCard(
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
                        text = if (isEnabled) "Forwarding to $forwardingNumber" else condition.description,
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
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onEditNumber) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            tint = LineaColors.TitaniumBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Change Target Number", color = LineaColors.TitaniumBlue, style = LineaTypography.bodySmall)
                    }
                }
            }
        }
    }
}
