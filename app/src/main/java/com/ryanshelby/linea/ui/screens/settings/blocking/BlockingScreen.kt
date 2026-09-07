package com.ryanshelby.linea.ui.screens.settings.blocking

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.data.local.entities.BlockedNumberEntity
import com.ryanshelby.linea.ui.components.FrostedGlassBox
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockingScreen(
    viewModel: BlockingViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var isAddSheetVisible by remember { mutableStateOf(false) }
    val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        containerColor = LineaColors.BackgroundTop,
        floatingActionButton = {
            if (uiState.selectedTab == 0) {
                FloatingActionButton(
                    onClick = { isAddSheetVisible = true },
                    containerColor = LineaColors.TitaniumBlue,
                    contentColor = LineaColors.TextPrimary,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 16.dp, end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add Block Rule",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(LineaColors.BackgroundTop, LineaColors.BackgroundBottom)
                    )
                )
                .statusBarsPadding()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = LineaDimensions.ScreenPadding)
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Top Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                text = "Call Screening & Blocking",
                                style = LineaTypography.headlineMedium,
                                color = LineaColors.TextPrimary
                            )
                            Text(
                                text = "On-device intelligence • Zero cloud calls",
                                style = LineaTypography.bodyMedium,
                                color = LineaColors.TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Mode Selector Pill (Block-List vs Allow-List)
                    FrostedGlassBox(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(LineaDimensions.ButtonCornerRadius))
                                    .background(LineaColors.BackgroundTop)
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (!uiState.isAllowListMode) LineaColors.TitaniumBlue else LineaColors.BackgroundTop)
                                        .clickable { viewModel.toggleAllowListMode(false) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Block-List Mode",
                                        style = LineaTypography.labelMedium,
                                        color = if (!uiState.isAllowListMode) LineaColors.TextPrimary else LineaColors.TextSecondary
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (uiState.isAllowListMode) LineaColors.TitaniumBlue else LineaColors.BackgroundTop)
                                        .clickable { viewModel.toggleAllowListMode(true) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Allow-List Mode",
                                        style = LineaTypography.labelMedium,
                                        color = if (uiState.isAllowListMode) LineaColors.TextPrimary else LineaColors.TextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (uiState.isAllowListMode) {
                                    "Strict mode: rejects all incoming calls unless the caller is in your Contacts."
                                } else {
                                    "Standard mode: incoming calls ring normally unless matched by an active block rule."
                                },
                                style = LineaTypography.bodySmall,
                                color = LineaColors.TextTertiary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Screening Toggles Card
                    FrostedGlassBox(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(LineaDimensions.PanelPadding)) {
                            Text(
                                text = "Quick Screening Rules",
                                style = LineaTypography.titleSmall,
                                color = LineaColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            ScreeningSwitchRow(
                                title = "Block Private / Hidden Numbers",
                                subtitle = "Rejects calls without caller ID",
                                isChecked = uiState.blockPrivate,
                                onCheckedChange = { viewModel.toggleBlockPrivate(it) }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            ScreeningSwitchRow(
                                title = "Block Non-Contacts",
                                subtitle = "Silently rejects unknown phone numbers",
                                isChecked = uiState.blockNonContacts,
                                onCheckedChange = { viewModel.toggleBlockNonContacts(it) }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            ScreeningSwitchRow(
                                title = "Block International Calls",
                                subtitle = "Blocks numbers originating outside local country",
                                isChecked = uiState.blockInternational,
                                onCheckedChange = { viewModel.toggleBlockInternational(it) }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            ScreeningSwitchRow(
                                title = "Emergency Repeated-Call Override",
                                subtitle = "Allows callers through if they call twice within 5 mins",
                                isChecked = uiState.repeatCallOverride,
                                onCheckedChange = { viewModel.toggleRepeatCallOverride(it) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quiet Hours / Scheduled Blocking Card
                    FrostedGlassBox(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(LineaDimensions.PanelPadding)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.NightsStay,
                                        contentDescription = null,
                                        tint = LineaColors.TitaniumBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Quiet Hours Schedule",
                                        style = LineaTypography.titleSmall,
                                        color = LineaColors.TextPrimary
                                    )
                                }

                                Switch(
                                    checked = uiState.quietHoursRule?.isEnabled ?: false,
                                    onCheckedChange = { viewModel.toggleQuietHours(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = LineaColors.TextPrimary,
                                        checkedTrackColor = LineaColors.TitaniumBlue,
                                        uncheckedThumbColor = LineaColors.TextSecondary,
                                        uncheckedTrackColor = LineaColors.BackgroundTop
                                    )
                                )
                            }

                            val rule = uiState.quietHoursRule
                            if (rule != null && rule.isEnabled) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Active: ${rule.startTime} – ${rule.endTime}",
                                        style = LineaTypography.bodyMedium,
                                        color = LineaColors.TitaniumBlue
                                    )
                                    Text(
                                        text = "Favorites Ring Through",
                                        fontSize = 12.sp,
                                        color = LineaColors.MutedSageGreen
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Tab Selector: Active Rules vs Blocked History
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TabPill(
                            label = "Rules (${uiState.blockedNumbers.size})",
                            isSelected = uiState.selectedTab == 0,
                            onClick = { viewModel.setSelectedTab(0) }
                        )
                        TabPill(
                            label = "Blocked History (${uiState.blockedLogs.size})",
                            isSelected = uiState.selectedTab == 1,
                            onClick = { viewModel.setSelectedTab(1) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                if (uiState.selectedTab == 0) {
                    if (uiState.blockedNumbers.isEmpty()) {
                        item {
                            FrostedGlassBox(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(LineaColors.GlassFill),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Shield,
                                            contentDescription = null,
                                            tint = LineaColors.TextTertiary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No Block Rules",
                                        style = LineaTypography.titleMedium,
                                        color = LineaColors.TextSecondary
                                    )
                                    Text(
                                        text = "Tap + below to block numbers, prefixes, or ranges",
                                        style = LineaTypography.bodyMedium,
                                        color = LineaColors.TextTertiary
                                    )
                                }
                            }
                        }
                    } else {
                        items(uiState.blockedNumbers, key = { it.id }) { rule ->
                            BlockedRuleRow(
                                rule = rule,
                                onUnblock = { viewModel.unblockRule(rule.id) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                } else {
                    item {
                        BlockedCallsLogView(logs = uiState.blockedLogs)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    if (isAddSheetVisible) {
        AddBlockSheet(
            sheetState = addSheetState,
            onDismiss = { isAddSheetVisible = false },
            onAddBlock = { number, matchType, action, reason, duration ->
                viewModel.addBlockedRule(number, matchType, action, reason, duration)
            }
        )
    }
}

@Composable
private fun ScreeningSwitchRow(
    title: String,
    subtitle: String,
    isChecked: Boolean,
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
            checked = isChecked,
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
private fun BlockedRuleRow(
    rule: BlockedNumberEntity,
    onUnblock: () -> Unit
) {
    FrostedGlassBox(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(LineaColors.CarmineRed.copy(alpha = 0.12f)),
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

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.numberOrPrefix,
                    style = LineaTypography.titleMedium,
                    color = LineaColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(LineaColors.GlassFill)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = rule.matchType.name,
                            fontSize = 10.sp,
                            color = LineaColors.TitaniumBlue
                        )
                    }

                    val durationText = if (rule.expiresAt == null) "Permanent" else "Temporary"
                    Text(
                        text = " • $durationText",
                        fontSize = 12.sp,
                        color = LineaColors.TextSecondary
                    )

                    if (!rule.reason.isNullOrBlank()) {
                        Text(
                            text = " • ${rule.reason}",
                            fontSize = 12.sp,
                            color = LineaColors.TextTertiary
                        )
                    }
                }
            }

            IconButton(onClick = onUnblock) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Unblock",
                    tint = LineaColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun TabPill(
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
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = if (isSelected) LineaColors.TextPrimary else LineaColors.TextSecondary
        )
    }
}
