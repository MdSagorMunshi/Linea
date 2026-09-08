package com.ryanshelby.linea.ui.screens.rules

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ryanshelby.linea.data.local.entities.CallRuleEntity
import com.ryanshelby.linea.data.local.entities.RuleAction
import com.ryanshelby.linea.data.local.entities.RuleAllowedFilter
import com.ryanshelby.linea.ui.components.FrostedGlassBox
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallRulesScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CallRulesViewModel = hiltViewModel()
) {
    val rules by viewModel.rules.collectAsState()
    val groups by viewModel.groups.collectAsState()

    var showSheet by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<CallRuleEntity?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        containerColor = LineaColors.BackgroundTop,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingRule = null
                    showSheet = true
                },
                containerColor = LineaColors.TitaniumBlue,
                contentColor = LineaColors.TextPrimary,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 16.dp, end = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add Call Rule",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(LineaColors.BackgroundTop, LineaColors.BackgroundBottom)
                    )
                )
                .statusBarsPadding()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(LineaColors.GlassFill)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = LineaColors.TextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = "Smart Rules & Schedules",
                                style = LineaTypography.headlineMedium,
                                color = LineaColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "100% on-device scheduled call filtering",
                                style = LineaTypography.labelSmall,
                                color = LineaColors.TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Info Card
                    FrostedGlassBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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

                            Column {
                                Text(
                                    text = "Automated Schedule Enforcement",
                                    style = LineaTypography.bodyMedium,
                                    color = LineaColors.TextPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Rules auto-silence or reject calls outside your allowed windows. Emergency repeated calls always override rules.",
                                    style = LineaTypography.labelSmall,
                                    color = LineaColors.TextTertiary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                if (rules.isEmpty()) {
                    item {
                        FrostedGlassBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 32.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.NightsStay,
                                    contentDescription = null,
                                    tint = LineaColors.TextTertiary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No Call Rules Configured",
                                    style = LineaTypography.titleMedium,
                                    color = LineaColors.TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap the + button to create a time or day schedule rule.",
                                    style = LineaTypography.labelSmall,
                                    color = LineaColors.TextSecondary
                                )
                            }
                        }
                    }
                } else {
                    items(rules, key = { it.id }) { rule ->
                        CallRuleCard(
                            rule = rule,
                            groupName = groups.find { it.id == rule.allowedGroupId }?.name,
                            onToggle = { viewModel.toggleRule(rule) },
                            onClick = {
                                editingRule = rule
                                showSheet = true
                            },
                            onDelete = { viewModel.deleteRule(rule.id) },
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    if (showSheet) {
        AddEditCallRuleSheet(
            sheetState = sheetState,
            existingRule = editingRule,
            groups = groups,
            onDismiss = { showSheet = false },
            onSaveRule = { savedRule ->
                viewModel.saveRule(savedRule)
                showSheet = false
            }
        )
    }
}

@Composable
private fun CallRuleCard(
    rule: CallRuleEntity,
    groupName: String?,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    FrostedGlassBox(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (rule.isEnabled) LineaColors.TitaniumBlue.copy(alpha = 0.15f)
                                else LineaColors.GlassFill
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (rule.action) {
                                RuleAction.REJECT -> Icons.Filled.PhoneDisabled
                                RuleAction.SILENT -> Icons.Filled.VolumeOff
                                RuleAction.ALLOW -> Icons.Filled.Schedule
                            },
                            contentDescription = null,
                            tint = if (rule.isEnabled) LineaColors.TitaniumBlue else LineaColors.TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = rule.name,
                            style = LineaTypography.titleMedium,
                            color = if (rule.isEnabled) LineaColors.TextPrimary else LineaColors.TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = when (rule.action) {
                                RuleAction.REJECT -> "Action: Block & Reject"
                                RuleAction.SILENT -> "Action: Silence Ringer"
                                RuleAction.ALLOW -> "Action: Allow"
                            },
                            style = LineaTypography.labelSmall,
                            color = if (rule.action == RuleAction.REJECT) LineaColors.Danger
                                   else LineaColors.TextTertiary,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete Rule",
                            tint = LineaColors.TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Switch(
                        checked = rule.isEnabled,
                        onCheckedChange = { onToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = LineaColors.TextPrimary,
                            checkedTrackColor = LineaColors.TitaniumBlue,
                            uncheckedThumbColor = LineaColors.TextTertiary,
                            uncheckedTrackColor = LineaColors.GlassFill
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Time & Allowed badges row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time Range Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(LineaColors.SurfaceElevated)
                        .border(1.dp, LineaColors.GlassBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccessTime,
                        contentDescription = null,
                        tint = LineaColors.TextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${rule.startTime} - ${rule.endTime}",
                        style = LineaTypography.labelSmall,
                        color = LineaColors.TextPrimary,
                        fontSize = 11.sp
                    )
                }

                // Allowed Filter Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(LineaColors.SurfaceElevated)
                        .border(1.dp, LineaColors.GlassBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (rule.allowedFilter) {
                            RuleAllowedFilter.FAVORITES_ONLY -> Icons.Filled.Star
                            RuleAllowedFilter.SPECIFIC_GROUP -> Icons.Filled.Group
                            RuleAllowedFilter.ALL -> Icons.Filled.Schedule
                        },
                        contentDescription = null,
                        tint = LineaColors.TitaniumBlue,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when (rule.allowedFilter) {
                            RuleAllowedFilter.FAVORITES_ONLY -> "Favorites Only"
                            RuleAllowedFilter.SPECIFIC_GROUP -> groupName ?: "Group"
                            RuleAllowedFilter.ALL -> "Everyone"
                        },
                        style = LineaTypography.labelSmall,
                        color = LineaColors.TextSecondary,
                        fontSize = 11.sp
                    )
                }

                // SIM Slot Badge (if restricted to specific SIM)
                if (rule.simSlot != null) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(LineaColors.SurfaceElevated)
                            .border(1.dp, LineaColors.GlassBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SimCard,
                            contentDescription = null,
                            tint = LineaColors.TextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "SIM ${rule.simSlot + 1}",
                            style = LineaTypography.labelSmall,
                            color = LineaColors.TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Days of week pills (M T W T F S S)
            val daySet = remember(rule.daysOfWeek) {
                rule.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
            }
            val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                dayLabels.forEachIndexed { index, label ->
                    val dayNum = index + 1
                    val isSelected = daySet.contains(dayNum)

                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) LineaColors.TitaniumBlue.copy(alpha = 0.25f)
                                else LineaColors.GlassFill
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) LineaColors.TitaniumBlue
                                else LineaColors.GlassBorder.copy(alpha = 0.4f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = LineaTypography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) LineaColors.TextPrimary else LineaColors.TextTertiary
                        )
                    }
                }
            }
        }
    }
}
