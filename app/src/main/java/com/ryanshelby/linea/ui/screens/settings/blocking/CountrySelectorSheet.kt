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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.data.local.entities.CountryRuleEntity
import com.ryanshelby.linea.data.local.entities.CountryRuleType
import com.ryanshelby.linea.telecom.screening.CountryDetails
import com.ryanshelby.linea.ui.components.FrostedGlassBox
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountrySelectorSheet(
    sheetState: SheetState,
    initialRuleType: CountryRuleType = CountryRuleType.BLACKLIST,
    existingRules: List<CountryRuleEntity>,
    allCountries: List<CountryDetails>,
    onDismiss: () -> Unit,
    onAddRule: (country: CountryDetails, ruleType: CountryRuleType, appliesToAllSharedCode: Boolean) -> Unit
) {
    var selectedRuleType by remember { mutableStateOf(initialRuleType) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedRegion by remember { mutableStateOf("All") }
    var selectedCountry by remember { mutableStateOf<CountryDetails?>(null) }
    var appliesToAllSharedCode by remember { mutableStateOf(false) }

    val regions = listOf("All", "Popular", "North America", "Europe", "Asia", "Africa", "Americas", "Oceania")

    val popularIsos = setOf("US", "CA", "GB", "IN", "DE", "FR", "AU", "CN", "BD", "JP", "RU", "BR", "MX")

    val filteredCountries by remember(searchQuery, selectedRegion, allCountries) {
        derivedStateOf {
            allCountries.filter { country ->
                val matchesQuery = if (searchQuery.isBlank()) true else {
                    val q = searchQuery.trim().lowercase(Locale.ROOT)
                    country.countryName.lowercase(Locale.ROOT).contains(q) ||
                    country.countryIso.lowercase(Locale.ROOT).contains(q) ||
                    country.dialCode.contains(q) ||
                    country.dialCode.removePrefix("+").contains(q)
                }

                val matchesRegion = when (selectedRegion) {
                    "All" -> true
                    "Popular" -> country.countryIso in popularIsos
                    "North America" -> country.countryIso in setOf("US", "CA", "MX", "JM", "BS", "BB", "CU", "DO", "PR")
                    "Europe" -> country.countryIso in setOf("GB", "DE", "FR", "IT", "ES", "NL", "PL", "SE", "NO", "FI", "CH", "AT", "IE", "PT", "GR", "UA", "RU")
                    "Asia" -> country.countryIso in setOf("CN", "IN", "JP", "KR", "BD", "PK", "ID", "PH", "VN", "TH", "MY", "SG", "SA", "AE", "TR", "KZ")
                    "Africa" -> country.countryIso in setOf("NG", "EG", "ZA", "KE", "ET", "GH", "MA", "DZ", "UG", "TZ")
                    "Americas" -> country.countryIso in setOf("US", "CA", "MX", "BR", "AR", "CO", "CL", "PE", "VE")
                    "Oceania" -> country.countryIso in setOf("AU", "NZ", "FJ", "PG")
                    else -> true
                }

                matchesQuery && matchesRegion
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LineaColors.BackgroundBottom,
        tonalElevation = 0.dp,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = LineaDimensions.ScreenPadding, vertical = 20.dp)
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
                            .background(
                                if (selectedRuleType == CountryRuleType.BLACKLIST)
                                    LineaColors.CarmineRed.copy(alpha = 0.15f)
                                else
                                    LineaColors.TitaniumBlue.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Public,
                            contentDescription = null,
                            tint = if (selectedRuleType == CountryRuleType.BLACKLIST)
                                LineaColors.CarmineRed
                            else
                                LineaColors.TitaniumBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Add Country Rule",
                            style = LineaTypography.titleLarge,
                            color = LineaColors.TextPrimary
                        )
                        Text(
                            text = "Select a country to filter calls",
                            style = LineaTypography.bodySmall,
                            color = LineaColors.TextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(LineaColors.GlassFill)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = LineaColors.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Rule Type Selector (Blacklist vs Whitelist)
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
                        .background(if (selectedRuleType == CountryRuleType.BLACKLIST) LineaColors.CarmineRed.copy(alpha = 0.85f) else Color.Transparent)
                        .clickable { selectedRuleType = CountryRuleType.BLACKLIST }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Add to Blacklist",
                        style = LineaTypography.labelMedium,
                        color = if (selectedRuleType == CountryRuleType.BLACKLIST) LineaColors.TextPrimary else LineaColors.TextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedRuleType == CountryRuleType.WHITELIST) LineaColors.TitaniumBlue else Color.Transparent)
                        .clickable { selectedRuleType = CountryRuleType.WHITELIST }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Add to Whitelist",
                        style = LineaTypography.labelMedium,
                        color = if (selectedRuleType == CountryRuleType.WHITELIST) LineaColors.TextPrimary else LineaColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Search by country name, flag, or code...",
                        style = LineaTypography.bodyMedium,
                        color = LineaColors.TextTertiary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = LineaColors.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Clear",
                                tint = LineaColors.TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LineaColors.TitaniumBlue,
                    unfocusedBorderColor = LineaColors.GlassBorder,
                    focusedTextColor = LineaColors.TextPrimary,
                    unfocusedTextColor = LineaColors.TextPrimary,
                    focusedContainerColor = LineaColors.BackgroundTop,
                    unfocusedContainerColor = LineaColors.BackgroundTop
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Region Filter Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(regions) { region ->
                    val isSelected = selectedRegion == region
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) LineaColors.TitaniumBlue else LineaColors.BackgroundTop)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) LineaColors.TitaniumBlue else LineaColors.GlassBorder,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedRegion = region }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = region,
                            style = LineaTypography.labelSmall,
                            color = if (isSelected) LineaColors.TextPrimary else LineaColors.TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Shared Code Precision Configuration Alert (when selected country has a shared calling code)
            AnimatedVisibility(visible = selectedCountry?.isSharedCallingCode == true) {
                val sc = selectedCountry
                if (sc != null) {
                    FrostedGlassBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = LineaColors.TitaniumBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Shared Calling Code (${sc.dialCode})",
                                    style = LineaTypography.titleSmall,
                                    color = LineaColors.TextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${sc.countryName} shares international calling code ${sc.dialCode} with other territories (e.g. USA, Canada, and Caribbean).",
                                style = LineaTypography.bodySmall,
                                color = LineaColors.TextSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Precision option 1: Country-specific
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { appliesToAllSharedCode = false },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = !appliesToAllSharedCode,
                                    onClick = { appliesToAllSharedCode = false },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = LineaColors.TitaniumBlue,
                                        unselectedColor = LineaColors.TextTertiary
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "${if (selectedRuleType == CountryRuleType.BLACKLIST) "Block" else "Whitelist"} ${sc.countryName} only (Recommended)",
                                        style = LineaTypography.labelMedium,
                                        color = LineaColors.TextPrimary
                                    )
                                    Text(
                                        text = "Uses area-code parsing. Other countries on ${sc.dialCode} will not be affected.",
                                        style = LineaTypography.bodySmall,
                                        fontSize = 11.sp,
                                        color = LineaColors.TextTertiary
                                    )
                                }
                            }

                            // Precision option 2: Entire Calling Code
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { appliesToAllSharedCode = true },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = appliesToAllSharedCode,
                                    onClick = { appliesToAllSharedCode = true },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = LineaColors.TitaniumBlue,
                                        unselectedColor = LineaColors.TextTertiary
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "${if (selectedRuleType == CountryRuleType.BLACKLIST) "Block" else "Whitelist"} all countries on ${sc.dialCode}",
                                        style = LineaTypography.labelMedium,
                                        color = LineaColors.TextPrimary
                                    )
                                    Text(
                                        text = "Applies to all territories sharing ${sc.dialCode}.",
                                        style = LineaTypography.bodySmall,
                                        fontSize = 11.sp,
                                        color = LineaColors.TextTertiary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Country List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (filteredCountries.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No countries match your search",
                                style = LineaTypography.bodyMedium,
                                color = LineaColors.TextTertiary
                            )
                        }
                    }
                } else {
                    items(filteredCountries, key = { it.countryIso }) { country ->
                        val isSelected = selectedCountry?.countryIso == country.countryIso
                        val alreadyConfigured = existingRules.any {
                            it.countryIso.equals(country.countryIso, ignoreCase = true) &&
                            it.ruleType == selectedRuleType
                        }

                        CountryPickerItem(
                            country = country,
                            isSelected = isSelected,
                            alreadyConfigured = alreadyConfigured,
                            ruleType = selectedRuleType,
                            onClick = {
                                selectedCountry = country
                                appliesToAllSharedCode = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Button
            val canAdd = selectedCountry != null
            Button(
                onClick = {
                    val country = selectedCountry ?: return@Button
                    onAddRule(country, selectedRuleType, appliesToAllSharedCode)
                    onDismiss()
                },
                enabled = canAdd,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(LineaDimensions.ButtonCornerRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedRuleType == CountryRuleType.BLACKLIST)
                        LineaColors.CarmineRed
                    else
                        LineaColors.TitaniumBlue,
                    contentColor = LineaColors.TextPrimary,
                    disabledContainerColor = LineaColors.GlassFill,
                    disabledContentColor = LineaColors.TextTertiary
                )
            ) {
                val actionLabel = if (selectedCountry != null) {
                    val prefix = if (selectedRuleType == CountryRuleType.BLACKLIST) "Blacklist" else "Whitelist"
                    val sharedNote = if (appliesToAllSharedCode) " (All ${selectedCountry?.dialCode})" else ""
                    "$prefix ${selectedCountry?.countryName}$sharedNote"
                } else {
                    "Select a Country Above"
                }
                Text(
                    text = actionLabel,
                    style = LineaTypography.labelLarge
                )
            }
        }
    }
}

@Composable
fun CountryPickerItem(
    country: CountryDetails,
    isSelected: Boolean,
    alreadyConfigured: Boolean,
    ruleType: CountryRuleType,
    onClick: () -> Unit
) {
    val borderColor = when {
        isSelected -> if (ruleType == CountryRuleType.BLACKLIST) LineaColors.CarmineRed else LineaColors.TitaniumBlue
        alreadyConfigured -> LineaColors.GlassBorder
        else -> LineaColors.GlassBorder.copy(alpha = 0.5f)
    }

    val backgroundColor = when {
        isSelected -> if (ruleType == CountryRuleType.BLACKLIST)
            LineaColors.CarmineRed.copy(alpha = 0.12f)
        else
            LineaColors.TitaniumBlue.copy(alpha = 0.12f)
        else -> LineaColors.BackgroundTop
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(width = if (isSelected) 1.5.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = country.flagEmoji,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = country.countryName,
                        style = LineaTypography.titleSmall,
                        color = LineaColors.TextPrimary
                    )
                    if (country.isSharedCallingCode) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(LineaColors.TitaniumBlue.copy(alpha = 0.15f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "Shared ${country.dialCode}",
                                style = LineaTypography.bodySmall,
                                fontSize = 9.sp,
                                color = LineaColors.TitaniumBlue
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = country.dialCode,
                        style = LineaTypography.bodySmall,
                        color = LineaColors.TitaniumBlue
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•  ${country.countryIso}",
                        style = LineaTypography.bodySmall,
                        color = LineaColors.TextTertiary
                    )
                }
            }
        }

        if (alreadyConfigured) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(LineaColors.GlassFill)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "ACTIVE",
                    style = LineaTypography.bodySmall,
                    fontSize = 10.sp,
                    color = LineaColors.TextTertiary
                )
            }
        } else if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Selected",
                tint = if (ruleType == CountryRuleType.BLACKLIST) LineaColors.CarmineRed else LineaColors.TitaniumBlue,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
