package com.ryanshelby.linea.ui.screens.settings.dualsim

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.data.preferences.LineaPreferences
import com.ryanshelby.linea.telecom.PhoneAccountManager
import com.ryanshelby.linea.ui.components.FrostedGlassBox
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography
import kotlinx.coroutines.launch

@Composable
fun DualSimScreen(
    phoneAccountManager: PhoneAccountManager,
    preferences: LineaPreferences,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val defaultSim by preferences.defaultSim.collectAsState(initial = 0)
    val askBeforeDial by preferences.askSimBeforeDial.collectAsState(initial = false)
    val scope = rememberCoroutineScope()

    val accounts = phoneAccountManager.registerPhoneAccounts()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(LineaColors.BackgroundTop, LineaColors.BackgroundBottom)
                )
            )
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = LineaDimensions.ScreenPadding)
                .verticalScroll(rememberScrollState())
        ) {
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
                        text = "Dual SIM Management",
                        style = LineaTypography.headlineMedium,
                        color = LineaColors.TextPrimary
                    )
                    Text(
                        text = "Slot configuration & calling affinity",
                        style = LineaTypography.bodyMedium,
                        color = LineaColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Active SIM Slots Detected
            Text(
                text = "Detected SIM Slots",
                style = LineaTypography.titleSmall,
                color = LineaColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (accounts.isNotEmpty()) {
                accounts.forEach { acc ->
                    FrostedGlassBox(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(LineaColors.TitaniumBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SimCard,
                                    contentDescription = null,
                                    tint = LineaColors.TitaniumBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SIM ${acc.slotIndex + 1}: ${acc.displayName}",
                                    style = LineaTypography.titleMedium,
                                    color = LineaColors.TextPrimary
                                )
                                Text(
                                    text = "Carrier: ${acc.carrierName}",
                                    style = LineaTypography.bodyMedium,
                                    color = LineaColors.TextSecondary
                                )
                            }
                            if (defaultSim == acc.slotIndex) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(LineaColors.TitaniumBlue)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "DEFAULT",
                                        fontSize = 11.sp,
                                        color = LineaColors.TextPrimary
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            } else {
                FrostedGlassBox(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SimCard,
                            contentDescription = null,
                            tint = LineaColors.TextTertiary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "No physical SIM detected (using default cellular)",
                            style = LineaTypography.bodyMedium,
                            color = LineaColors.TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Default Calling SIM Selector
            Text(
                text = "Default Outgoing Calling SIM",
                style = LineaTypography.titleSmall,
                color = LineaColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))

            FrostedGlassBox(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(LineaDimensions.PanelPadding)) {
                    SimOptionRow(
                        title = "SIM 1",
                        subtitle = "Use Slot 1 as primary for outgoing calls",
                        isSelected = defaultSim == 0 && !askBeforeDial,
                        onClick = {
                            scope.launch {
                                preferences.setDefaultSim(0)
                                preferences.setAskSimBeforeDial(false)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SimOptionRow(
                        title = "SIM 2",
                        subtitle = "Use Slot 2 as primary for outgoing calls",
                        isSelected = defaultSim == 1 && !askBeforeDial,
                        onClick = {
                            scope.launch {
                                preferences.setDefaultSim(1)
                                preferences.setAskSimBeforeDial(false)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SimOptionRow(
                        title = "Always Ask",
                        subtitle = "Display SIM selection prompt before every outgoing call",
                        isSelected = askBeforeDial || defaultSim == -1,
                        onClick = {
                            scope.launch {
                                preferences.setDefaultSim(-1)
                                preferences.setAskSimBeforeDial(true)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Calling Behavior Switch
            FrostedGlassBox(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(LineaDimensions.PanelPadding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ask Before Every Call",
                            style = LineaTypography.bodyLarge,
                            color = LineaColors.TextPrimary
                        )
                        Text(
                            text = "Shows SIM 1 / SIM 2 selector overlay before dialing",
                            style = LineaTypography.bodyMedium,
                            color = LineaColors.TextSecondary
                        )
                    }
                    Switch(
                        checked = askBeforeDial,
                        onCheckedChange = { checked ->
                            scope.launch { preferences.setAskSimBeforeDial(checked) }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = LineaColors.TextPrimary,
                            checkedTrackColor = LineaColors.TitaniumBlue,
                            uncheckedThumbColor = LineaColors.TextSecondary,
                            uncheckedTrackColor = LineaColors.BackgroundTop
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SimOptionRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LineaDimensions.CardCornerRadius))
            .background(if (isSelected) LineaColors.TitaniumBlue.copy(alpha = 0.12f) else LineaColors.GlassFill)
            .border(
                1.dp,
                if (isSelected) LineaColors.TitaniumBlue else LineaColors.GlassBorder,
                RoundedCornerShape(LineaDimensions.CardCornerRadius)
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = LineaTypography.titleMedium,
                color = LineaColors.TextPrimary
            )
            Text(
                text = subtitle,
                style = LineaTypography.bodySmall,
                color = LineaColors.TextSecondary
            )
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(LineaColors.TitaniumBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = LineaColors.TextPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
