package com.ryanshelby.linea.ui.screens.settings.blocking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Public
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
import com.ryanshelby.linea.data.local.entities.CountryRuleEntity
import com.ryanshelby.linea.data.local.entities.CountryRuleType
import com.ryanshelby.linea.ui.components.FrostedGlassBox
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryBlockingScreen(
    viewModel: CountryBlockingViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var isAddSheetVisible by remember { mutableStateOf(false) }
    val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        containerColor = LineaColors.BackgroundTop,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isAddSheetVisible = true },
                containerColor = if (uiState.selectedTab == 0) LineaColors.CarmineRed else LineaColors.TitaniumBlue,
                contentColor = LineaColors.TextPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp, end = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add Country Rule",
                    modifier = Modifier.size(24.dp)
                )
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
                    .padding(horizontal = LineaDimensions.ScreenPadding),
                contentPadding = PaddingValues(bottom = 96.dp)
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
                                text = "Country Call Blocking",
                                style = LineaTypography.headlineMedium,
                                color = LineaColors.TextPrimary
                            )
                            Text(
                                text = "Autonomous border protection • Zero cloud calls",
                                style = LineaTypography.bodyMedium,
                                color = LineaColors.TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Master Toggle Card
                    FrostedGlassBox(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(LineaDimensions.PanelPadding)) {
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
                                            .background(
                                                if (uiState.isEnabled)
                                                    LineaColors.TitaniumBlue.copy(alpha = 0.2f)
                                                else
                                                    LineaColors.GlassFill
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Shield,
                                            contentDescription = null,
                                            tint = if (uiState.isEnabled) LineaColors.TitaniumBlue else LineaColors.TextTertiary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Country-Based Call Blocking",
                                            style = LineaTypography.titleSmall,
                                            color = LineaColors.TextPrimary
                                        )
                                        Text(
                                            text = if (uiState.isEnabled) "Actively screening incoming calls" else "Disabled",
                                            style = LineaTypography.bodySmall,
                                            color = if (uiState.isEnabled) LineaColors.MutedSageGreen else LineaColors.TextTertiary
                                        )
                                    }
                                }

                                Switch(
                                    checked = uiState.isEnabled,
                                    onCheckedChange = { viewModel.toggleCountryBlocking(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = LineaColors.TextPrimary,
                                        checkedTrackColor = LineaColors.TitaniumBlue,
                                        uncheckedThumbColor = LineaColors.TextSecondary,
                                        uncheckedTrackColor = LineaColors.BackgroundTop
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Mode Configuration & Interaction Card
                    FrostedGlassBox(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Screening Mode & Policies",
                                style = LineaTypography.titleSmall,
                                color = LineaColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // Mode Selector (Blacklist vs Whitelist)
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
                                        .background(if (uiState.filterMode == "BLACKLIST_ONLY") LineaColors.TitaniumBlue else LineaColors.BackgroundTop)
                                        .clickable { viewModel.setFilterMode("BLACKLIST_ONLY") }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Blacklist Mode",
                                        style = LineaTypography.labelMedium,
                                        color = if (uiState.filterMode == "BLACKLIST_ONLY") LineaColors.TextPrimary else LineaColors.TextSecondary
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (uiState.filterMode == "WHITELIST_ONLY") LineaColors.TitaniumBlue else LineaColors.BackgroundTop)
                                        .clickable { viewModel.setFilterMode("WHITELIST_ONLY") }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Whitelist Mode",
                                        style = LineaTypography.labelMedium,
                                        color = if (uiState.filterMode == "WHITELIST_ONLY") LineaColors.TextPrimary else LineaColors.TextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (uiState.filterMode == "WHITELIST_ONLY") {
                                    "Strict mode: automatically rejects all calls unless originating from a whitelisted country or local SIM country."
                                } else {
                                    "Standard mode: incoming calls ring normally unless originating from a country added to the blacklist."
                                },
                                style = LineaTypography.bodySmall,
                                color = LineaColors.TextTertiary
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Interaction Switch: Whitelist Overrides Blacklist
                            ScreeningSwitchRow(
                                title = "Whitelist Overrides Blacklist",
                                subtitle = "Whitelisted countries are always permitted, even if a shared code (+1, +7) matches",
                                isChecked = uiState.whitelistOverrides,
                                onCheckedChange = { viewModel.toggleWhitelistOverrides(it) }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Interaction Switch: Exempt Saved Contacts
                            ScreeningSwitchRow(
                                title = "Exempt Saved Contacts",
                                subtitle = "Callers in your Contacts always ring through regardless of their country origin",
                                isChecked = uiState.exemptContacts,
                                onCheckedChange = { viewModel.toggleExemptContacts(it) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Tab Selector: Blacklist vs Whitelist
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TabPill(
                            label = "Blacklist (${uiState.blacklistedCountries.size})",
                            isSelected = uiState.selectedTab == 0,
                            onClick = { viewModel.setSelectedTab(0) }
                        )
                        TabPill(
                            label = "Whitelist (${uiState.whitelistedCountries.size})",
                            isSelected = uiState.selectedTab == 1,
                            onClick = { viewModel.setSelectedTab(1) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Active Tab Content
                val activeList = if (uiState.selectedTab == 0) uiState.blacklistedCountries else uiState.whitelistedCountries

                if (activeList.isEmpty()) {
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
                                        imageVector = Icons.Filled.Public,
                                        contentDescription = null,
                                        tint = LineaColors.TextTertiary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (uiState.selectedTab == 0) "No Countries Blacklisted" else "No Countries Whitelisted",
                                    style = LineaTypography.titleMedium,
                                    color = LineaColors.TextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (uiState.selectedTab == 0)
                                        "Tap + below to blacklist calls originating from specific countries"
                                    else
                                        "Tap + below to whitelist specific countries",
                                    style = LineaTypography.bodyMedium,
                                    color = LineaColors.TextTertiary
                                )
                            }
                        }
                    }
                } else {
                    items(activeList, key = { it.id }) { rule ->
                        CountryRuleRow(
                            rule = rule,
                            onDelete = { viewModel.removeCountryRule(rule.id) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    if (isAddSheetVisible) {
        val currentRuleType = if (uiState.selectedTab == 0) CountryRuleType.BLACKLIST else CountryRuleType.WHITELIST
        val existingRules = if (uiState.selectedTab == 0) uiState.blacklistedCountries else uiState.whitelistedCountries

        CountrySelectorSheet(
            sheetState = addSheetState,
            initialRuleType = currentRuleType,
            existingRules = existingRules,
            allCountries = uiState.allCatalogCountries,
            onDismiss = { isAddSheetVisible = false },
            onAddRule = { country, ruleType, appliesToAllSharedCode ->
                viewModel.addCountryRule(country, ruleType, appliesToAllSharedCode)
            }
        )
    }
}

@Composable
fun CountryRuleRow(
    rule: CountryRuleEntity,
    onDelete: () -> Unit
) {
    FrostedGlassBox(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = rule.flagEmoji,
                    fontSize = 26.sp
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = rule.countryName,
                            style = LineaTypography.titleSmall,
                            color = LineaColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (rule.ruleType == CountryRuleType.BLACKLIST)
                                        LineaColors.CarmineRed.copy(alpha = 0.15f)
                                    else
                                        LineaColors.TitaniumBlue.copy(alpha = 0.15f)
                                )
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = rule.countryIso,
                                style = LineaTypography.bodySmall,
                                fontSize = 10.sp,
                                color = if (rule.ruleType == CountryRuleType.BLACKLIST)
                                    LineaColors.CarmineRed
                                else
                                    LineaColors.TitaniumBlue
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = rule.dialCode,
                            style = LineaTypography.bodySmall,
                            color = LineaColors.TitaniumBlue
                        )
                        if (rule.appliesToAllSharedCode) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• All ${rule.dialCode} territories",
                                style = LineaTypography.bodySmall,
                                color = LineaColors.TextTertiary
                            )
                        } else {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• Area-code precision",
                                style = LineaTypography.bodySmall,
                                color = LineaColors.TextTertiary
                            )
                        }
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Remove Country Rule",
                    tint = LineaColors.TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
