package com.ryanshelby.linea.ui.screens.settings.search

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.ui.components.FrostedGlassBox
import com.ryanshelby.linea.ui.screens.settings.SettingsUiState
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsSearchResultsView(
    searchQuery: String,
    searchResults: List<SettingsSearchItem>,
    uiState: SettingsUiState,
    onClearQuery: () -> Unit,
    onSelectSuggestedQuery: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (searchResults.isEmpty()) {
            // Empty State
            FrostedGlassBox(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(LineaColors.GlassFill),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SearchOff,
                            contentDescription = null,
                            tint = LineaColors.TextTertiary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "No Settings Found",
                        style = LineaTypography.titleMedium,
                        color = LineaColors.TextPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "No configurations match \"$searchQuery\"",
                        style = LineaTypography.bodySmall,
                        color = LineaColors.TextSecondary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Suggested Searches",
                        style = LineaTypography.labelMedium,
                        color = LineaColors.TitaniumBlue
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val suggestions = listOf(
                        "Ringtone", "Dual SIM", "Theme", "Gestures", "Flip",
                        "Haptics", "Blocking", "Voicemail", "Diagnostics", "Duration", "Backup"
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        suggestions.forEach { suggestion ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(LineaColors.GlassFill)
                                    .border(
                                        1.dp,
                                        LineaColors.GlassBorder,
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable { onSelectSuggestedQuery(suggestion) }
                                    .heightIn(min = 36.dp)
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = suggestion,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = LineaColors.TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Results Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${searchResults.size} matching ${if (searchResults.size == 1) "setting" else "settings"}",
                    style = LineaTypography.labelMedium,
                    color = LineaColors.TextSecondary
                )

                TextButton(onClick = onClearQuery) {
                    Text(
                        text = "Clear",
                        style = LineaTypography.labelMedium,
                        color = LineaColors.TitaniumBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Results List
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                searchResults.forEach { item ->
                    SettingsSearchResultCard(
                        item = item,
                        uiState = uiState
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(110.dp))
    }
}

@Composable
private fun SettingsSearchResultCard(
    item: SettingsSearchItem,
    uiState: SettingsUiState,
    modifier: Modifier = Modifier
) {
    val isClickableCard = item.action is SettingsSearchAction.Navigate || item.action is SettingsSearchAction.Select

    FrostedGlassBox(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isClickableCard) {
                    Modifier.clickable {
                        when (val action = item.action) {
                            is SettingsSearchAction.Navigate -> action.onNavigate()
                            is SettingsSearchAction.Select -> action.onAction()
                            else -> {}
                        }
                    }
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Category Badge Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(LineaColors.GlassFill)
                        .border(
                            LineaDimensions.HairlineBorder,
                            LineaColors.GlassBorder,
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = item.category.displayName.uppercase(),
                        fontSize = 9.sp,
                        style = LineaTypography.labelSmall,
                        color = LineaColors.TitaniumBlue,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Content Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon Container
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(LineaColors.GlassFill),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = LineaColors.TitaniumBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title & Subtitle
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = LineaTypography.titleMedium,
                        color = LineaColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.subtitle,
                        style = LineaTypography.bodySmall,
                        color = LineaColors.TextSecondary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Action Control
                when (val action = item.action) {
                    is SettingsSearchAction.Toggle -> {
                        Switch(
                            checked = action.isChecked(uiState),
                            onCheckedChange = action.onToggle,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = LineaColors.TextPrimary,
                                checkedTrackColor = LineaColors.TitaniumBlue,
                                uncheckedThumbColor = LineaColors.TextSecondary,
                                uncheckedTrackColor = LineaColors.BackgroundTop
                            )
                        )
                    }
                    is SettingsSearchAction.Navigate -> {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(LineaColors.GlassFill),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = LineaColors.TextTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    is SettingsSearchAction.Select -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(LineaColors.GlassFill)
                                .border(
                                    1.dp,
                                    LineaColors.TitaniumBlue.copy(alpha = 0.4f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = action.currentValue(uiState),
                                fontSize = 11.sp,
                                style = LineaTypography.labelMedium,
                                color = LineaColors.TitaniumBlue
                            )
                        }
                    }
                    is SettingsSearchAction.Action -> {
                        OutlinedButton(
                            onClick = action.onAction,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = LineaColors.TitaniumBlue)
                        ) {
                            Text(
                                text = action.buttonLabel,
                                fontSize = 11.sp,
                                style = LineaTypography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
