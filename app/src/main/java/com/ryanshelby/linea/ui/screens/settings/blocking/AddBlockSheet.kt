package com.ryanshelby.linea.ui.screens.settings.blocking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.data.local.entities.BlockAction
import com.ryanshelby.linea.data.local.entities.BlockMatchType
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddBlockSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onAddBlock: (numberOrPrefix: String, matchType: BlockMatchType, action: BlockAction, reason: String?, durationHours: Int?) -> Unit
) {
    var numberInput by remember { mutableStateOf("") }
    var reasonInput by remember { mutableStateOf("") }
    var selectedMatchType by remember { mutableStateOf(BlockMatchType.EXACT) }
    var selectedAction by remember { mutableStateOf(BlockAction.SILENT_REJECT) }
    var selectedDurationHours by remember { mutableStateOf<Int?>(null) } // null = permanent

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LineaColors.BackgroundBottom,
        tonalElevation = 0.dp,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = LineaDimensions.ScreenPadding, vertical = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(LineaColors.CarmineRed.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Block,
                            contentDescription = null,
                            tint = LineaColors.CarmineRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Add Block Rule",
                        style = LineaTypography.titleLarge,
                        color = LineaColors.TextPrimary
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = LineaColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Number or Prefix Input
            Text(
                text = "Number, Prefix, or Range",
                style = LineaTypography.labelMedium,
                color = LineaColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = numberInput,
                onValueChange = { numberInput = it },
                placeholder = {
                    Text(
                        text = when (selectedMatchType) {
                            BlockMatchType.EXACT -> "e.g. 5551234 or +8801700000000"
                            BlockMatchType.PREFIX -> "e.g. +1800 or 017"
                            BlockMatchType.RANGE -> "e.g. 5550100-5550200"
                            else -> "Enter pattern..."
                        },
                        style = LineaTypography.bodyLarge,
                        color = LineaColors.TextTertiary
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LineaColors.TitaniumBlue,
                    unfocusedBorderColor = LineaColors.GlassBorder,
                    focusedTextColor = LineaColors.TextPrimary,
                    unfocusedTextColor = LineaColors.TextPrimary,
                    focusedContainerColor = LineaColors.GlassFill,
                    unfocusedContainerColor = LineaColors.GlassFill
                ),
                shape = RoundedCornerShape(LineaDimensions.CardCornerRadius)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Match Type Selector
            Text(
                text = "Match Type",
                style = LineaTypography.labelMedium,
                color = LineaColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MatchTypePill(
                    label = "Exact Number",
                    isSelected = selectedMatchType == BlockMatchType.EXACT,
                    onClick = { selectedMatchType = BlockMatchType.EXACT }
                )
                MatchTypePill(
                    label = "Starts With (Prefix)",
                    isSelected = selectedMatchType == BlockMatchType.PREFIX,
                    onClick = { selectedMatchType = BlockMatchType.PREFIX }
                )
                MatchTypePill(
                    label = "Numeric Range",
                    isSelected = selectedMatchType == BlockMatchType.RANGE,
                    onClick = { selectedMatchType = BlockMatchType.RANGE }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action Selector
            Text(
                text = "Action When Matched",
                style = LineaTypography.labelMedium,
                color = LineaColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionPill(
                    label = "Silent Reject",
                    isSelected = selectedAction == BlockAction.SILENT_REJECT,
                    onClick = { selectedAction = BlockAction.SILENT_REJECT }
                )
                ActionPill(
                    label = "Voicemail",
                    isSelected = selectedAction == BlockAction.VOICEMAIL,
                    onClick = { selectedAction = BlockAction.VOICEMAIL }
                )
                ActionPill(
                    label = "Hang Up",
                    isSelected = selectedAction == BlockAction.HANGUP,
                    onClick = { selectedAction = BlockAction.HANGUP }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Duration Selector
            Text(
                text = "Block Duration",
                style = LineaTypography.labelMedium,
                color = LineaColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DurationPill(
                    label = "Permanent",
                    isSelected = selectedDurationHours == null,
                    onClick = { selectedDurationHours = null }
                )
                DurationPill(
                    label = "1 Hour",
                    isSelected = selectedDurationHours == 1,
                    onClick = { selectedDurationHours = 1 }
                )
                DurationPill(
                    label = "Tomorrow",
                    isSelected = selectedDurationHours == 24,
                    onClick = { selectedDurationHours = 24 }
                )
                DurationPill(
                    label = "1 Week",
                    isSelected = selectedDurationHours == 168,
                    onClick = { selectedDurationHours = 168 }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Reason / Label
            Text(
                text = "Label / Reason (Optional)",
                style = LineaTypography.labelMedium,
                color = LineaColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = reasonInput,
                onValueChange = { reasonInput = it },
                placeholder = {
                    Text(
                        text = "e.g. Telemarketer, Robocall, Spam",
                        style = LineaTypography.bodyLarge,
                        color = LineaColors.TextTertiary
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LineaColors.TitaniumBlue,
                    unfocusedBorderColor = LineaColors.GlassBorder,
                    focusedTextColor = LineaColors.TextPrimary,
                    unfocusedTextColor = LineaColors.TextPrimary,
                    focusedContainerColor = LineaColors.GlassFill,
                    unfocusedContainerColor = LineaColors.GlassFill
                ),
                shape = RoundedCornerShape(LineaDimensions.CardCornerRadius)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            Button(
                onClick = {
                    if (numberInput.isNotBlank()) {
                        onAddBlock(
                            numberInput,
                            selectedMatchType,
                            selectedAction,
                            reasonInput.ifBlank { null },
                            selectedDurationHours
                        )
                        onDismiss()
                    }
                },
                enabled = numberInput.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(LineaDimensions.ButtonCornerRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LineaColors.TitaniumBlue,
                    contentColor = LineaColors.TextPrimary,
                    disabledContainerColor = LineaColors.GlassFill,
                    disabledContentColor = LineaColors.TextTertiary
                )
            ) {
                Text(
                    text = "Save Block Rule",
                    style = LineaTypography.labelLarge,
                    color = LineaColors.TextPrimary
                )
            }
        }
    }
}

@Composable
private fun MatchTypePill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) LineaColors.TitaniumBlue else LineaColors.GlassFill)
            .border(
                1.dp,
                if (isSelected) LineaColors.TitaniumBlue else LineaColors.GlassBorder,
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = if (isSelected) LineaColors.TextPrimary else LineaColors.TextSecondary
        )
    }
}

@Composable
private fun ActionPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) LineaColors.CarmineRed.copy(alpha = 0.8f) else LineaColors.GlassFill)
            .border(
                1.dp,
                if (isSelected) LineaColors.CarmineRed else LineaColors.GlassBorder,
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = if (isSelected) LineaColors.TextPrimary else LineaColors.TextSecondary
        )
    }
}

@Composable
private fun DurationPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) LineaColors.TitaniumBlue else LineaColors.GlassFill)
            .border(
                1.dp,
                if (isSelected) LineaColors.TitaniumBlue else LineaColors.GlassBorder,
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isSelected) LineaColors.TextPrimary else LineaColors.TextSecondary
        )
    }
}
