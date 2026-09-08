package com.ryanshelby.linea.ui.screens.rules

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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
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
import com.ryanshelby.linea.data.local.entities.CallRuleEntity
import com.ryanshelby.linea.data.local.entities.ContactGroupEntity
import com.ryanshelby.linea.data.local.entities.RuleAction
import com.ryanshelby.linea.data.local.entities.RuleAllowedFilter
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCallRuleSheet(
    sheetState: SheetState,
    existingRule: CallRuleEntity? = null,
    groups: List<ContactGroupEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSaveRule: (CallRuleEntity) -> Unit
) {
    var nameInput by remember { mutableStateOf(existingRule?.name ?: "") }
    var startTimeInput by remember { mutableStateOf(existingRule?.startTime ?: "22:00") }
    var endTimeInput by remember { mutableStateOf(existingRule?.endTime ?: "07:00") }
    var selectedDays by remember {
        mutableStateOf(
            existingRule?.daysOfWeek?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.toSet()
                ?: setOf(1, 2, 3, 4, 5, 6, 7)
        )
    }
    var selectedAllowedFilter by remember {
        mutableStateOf(existingRule?.allowedFilter ?: RuleAllowedFilter.FAVORITES_ONLY)
    }
    var selectedGroupId by remember { mutableStateOf(existingRule?.allowedGroupId) }
    var selectedSimSlot by remember { mutableStateOf(existingRule?.simSlot) } // null = all
    var selectedAction by remember { mutableStateOf(existingRule?.action ?: RuleAction.REJECT) }

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
                            .background(LineaColors.TitaniumBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = LineaColors.TitaniumBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (existingRule != null) "Edit Schedule Rule" else "New Schedule Rule",
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

            // Rule Name
            Text(
                text = "Rule Name",
                style = LineaTypography.labelMedium,
                color = LineaColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                placeholder = {
                    Text("e.g. Night Quiet Hours, Workday Focus", color = LineaColors.TextTertiary)
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

            // Time Range (Start - End)
            Text(
                text = "Time Window (24h Format: HH:mm)",
                style = LineaTypography.labelMedium,
                color = LineaColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = startTimeInput,
                    onValueChange = { startTimeInput = it },
                    label = { Text("Start Time") },
                    modifier = Modifier.weight(1f),
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

                OutlinedTextField(
                    value = endTimeInput,
                    onValueChange = { endTimeInput = it },
                    label = { Text("End Time") },
                    modifier = Modifier.weight(1f),
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
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Days of Week
            Text(
                text = "Active Days",
                style = LineaTypography.labelMedium,
                color = LineaColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            val dayLabels = listOf("M" to 1, "T" to 2, "W" to 3, "T" to 4, "F" to 5, "S" to 6, "S" to 7)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                dayLabels.forEach { (label, dayNum) ->
                    val isSelected = dayNum in selectedDays
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) LineaColors.TitaniumBlue else LineaColors.GlassFill
                            )
                            .border(
                                1.dp,
                                if (isSelected) LineaColors.TitaniumBlue else LineaColors.GlassBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                selectedDays = if (isSelected) {
                                    if (selectedDays.size > 1) selectedDays - dayNum else selectedDays
                                } else {
                                    selectedDays + dayNum
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = LineaTypography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else LineaColors.TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Allowed Contacts Filter
            Text(
                text = "Allowed Callers",
                style = LineaTypography.labelMedium,
                color = LineaColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RuleOptionPill(
                    label = "Favorites Only",
                    isSelected = selectedAllowedFilter == RuleAllowedFilter.FAVORITES_ONLY,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedAllowedFilter = RuleAllowedFilter.FAVORITES_ONLY }
                )
                RuleOptionPill(
                    label = "Everyone",
                    isSelected = selectedAllowedFilter == RuleAllowedFilter.ALL,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedAllowedFilter = RuleAllowedFilter.ALL }
                )
                RuleOptionPill(
                    label = "Group",
                    isSelected = selectedAllowedFilter == RuleAllowedFilter.SPECIFIC_GROUP,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedAllowedFilter = RuleAllowedFilter.SPECIFIC_GROUP }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // SIM Slot Selection
            Text(
                text = "Apply to Cellular SIM",
                style = LineaTypography.labelMedium,
                color = LineaColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RuleOptionPill(
                    label = "All SIMs",
                    isSelected = selectedSimSlot == null,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedSimSlot = null }
                )
                RuleOptionPill(
                    label = "SIM 1 Only",
                    isSelected = selectedSimSlot == 0,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedSimSlot = 0 }
                )
                RuleOptionPill(
                    label = "SIM 2 Only",
                    isSelected = selectedSimSlot == 1,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedSimSlot = 1 }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action for Disallowed Callers
            Text(
                text = "Action for Disallowed Callers",
                style = LineaTypography.labelMedium,
                color = LineaColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RuleOptionPill(
                    label = "Reject / Hang Up",
                    isSelected = selectedAction == RuleAction.REJECT,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedAction = RuleAction.REJECT }
                )
                RuleOptionPill(
                    label = "Silent / Voicemail",
                    isSelected = selectedAction == RuleAction.SILENT,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedAction = RuleAction.SILENT }
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Save Button
            Button(
                onClick = {
                    if (nameInput.isNotBlank()) {
                        val daysString = selectedDays.sorted().joinToString(",")
                        val rule = CallRuleEntity(
                            id = existingRule?.id ?: 0L,
                            name = nameInput.trim(),
                            isEnabled = existingRule?.isEnabled ?: true,
                            startTime = startTimeInput.trim(),
                            endTime = endTimeInput.trim(),
                            daysOfWeek = daysString,
                            allowedFilter = selectedAllowedFilter,
                            allowedGroupId = selectedGroupId,
                            simSlot = selectedSimSlot,
                            action = selectedAction
                        )
                        onSaveRule(rule)
                        onDismiss()
                    }
                },
                enabled = nameInput.isNotBlank(),
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
                    text = if (existingRule != null) "Update Schedule Rule" else "Save Schedule Rule",
                    style = LineaTypography.labelLarge,
                    color = LineaColors.TextPrimary
                )
            }
        }
    }
}

@Composable
private fun RuleOptionPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) LineaColors.TitaniumBlue.copy(alpha = 0.2f) else LineaColors.GlassFill
            )
            .border(
                1.dp,
                if (isSelected) LineaColors.TitaniumBlue else LineaColors.GlassBorder,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = LineaTypography.labelSmall.copy(fontSize = 11.sp),
            color = if (isSelected) LineaColors.TitaniumBlue else LineaColors.TextSecondary,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
