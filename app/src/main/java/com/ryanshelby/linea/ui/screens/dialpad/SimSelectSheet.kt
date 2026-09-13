package com.ryanshelby.linea.ui.screens.dialpad

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ryanshelby.linea.data.preferences.LineaPreferences
import com.ryanshelby.linea.telecom.SimAccountInfo
import com.ryanshelby.linea.ui.components.FrostedGlassBox
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimSelectSheet(
    phoneNumber: String,
    accounts: List<SimAccountInfo>,
    position: String = LineaPreferences.SimPopupPosition.MIDDLE,
    sheetState: SheetState? = null,
    onSelectSim: (SimAccountInfo) -> Unit,
    onDismiss: () -> Unit
) {
    if (position == LineaPreferences.SimPopupPosition.MIDDLE) {
        SimSelectMiddleDialog(
            phoneNumber = phoneNumber,
            accounts = accounts,
            onSelectSim = onSelectSim,
            onDismiss = onDismiss
        )
    } else {
        SimSelectBottomSheet(
            sheetState = sheetState,
            phoneNumber = phoneNumber,
            accounts = accounts,
            onSelectSim = onSelectSim,
            onDismiss = onDismiss
        )
    }
}

/**
 * Centered Floating Dialog with Translucent / Frosted Glass styling.
 */
@Composable
private fun SimSelectMiddleDialog(
    phoneNumber: String,
    accounts: List<SimAccountInfo>,
    onSelectSim: (SimAccountInfo) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true,
            dismissOnBackPress = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            FrostedGlassBox(
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .clip(RoundedCornerShape(26.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Block outside clicks from reaching container
                    ),
                shape = RoundedCornerShape(26.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Select Calling SIM",
                                style = LineaTypography.titleLarge,
                                color = LineaColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Calling $phoneNumber",
                                style = LineaTypography.bodyMedium,
                                color = LineaColors.TitaniumBlue
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close",
                                tint = LineaColors.TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    SimAccountList(
                        accounts = accounts,
                        onSelectSim = onSelectSim,
                        onDismiss = onDismiss
                    )
                }
            }
        }
    }
}

/**
 * Bottom Sheet modal style.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimSelectBottomSheet(
    sheetState: SheetState?,
    phoneNumber: String,
    accounts: List<SimAccountInfo>,
    onSelectSim: (SimAccountInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val state = sheetState ?: androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = LineaColors.BackgroundBottom,
        tonalElevation = 0.dp,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Drag handle pill
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 4.dp, bottom = 16.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(LineaColors.TextSecondary.copy(alpha = 0.35f))
            )

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Select Calling SIM",
                        style = LineaTypography.titleLarge,
                        color = LineaColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Calling $phoneNumber",
                        style = LineaTypography.bodyMedium,
                        color = LineaColors.TitaniumBlue
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = LineaColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            SimAccountList(
                accounts = accounts,
                onSelectSim = onSelectSim,
                onDismiss = onDismiss
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SimAccountList(
    accounts: List<SimAccountInfo>,
    onSelectSim: (SimAccountInfo) -> Unit,
    onDismiss: () -> Unit
) {
    if (accounts.isNotEmpty()) {
        accounts.forEach { acc ->
            FrostedGlassBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onSelectSim(acc)
                        onDismiss()
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(LineaColors.TitaniumBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SimCard,
                            contentDescription = null,
                            tint = LineaColors.TitaniumBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SIM ${acc.slotIndex + 1}: ${acc.displayName}",
                            style = LineaTypography.titleMedium,
                            color = LineaColors.TextPrimary
                        )
                        val subtitle = when {
                            acc.carrierName.isNotBlank() && !acc.carrierName.equals(acc.displayName, ignoreCase = true) -> acc.carrierName
                            acc.carrierName.isNotBlank() -> "Slot ${acc.slotIndex + 1} • Active"
                            else -> "Slot ${acc.slotIndex + 1}"
                        }
                        Text(
                            text = subtitle,
                            style = LineaTypography.bodyMedium,
                            color = LineaColors.TextSecondary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    } else {
        // Fallback row
        FrostedGlassBox(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onDismiss() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.SimCard,
                    contentDescription = null,
                    tint = LineaColors.TitaniumBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Call via Default SIM",
                    style = LineaTypography.titleMedium,
                    color = LineaColors.TextPrimary
                )
            }
        }
    }
}

