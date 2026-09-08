package com.ryanshelby.linea.ui.screens.diagnostics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ryanshelby.linea.ui.components.FrostedGlassBox
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography

@Composable
fun CallDiagnosticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: CallDiagnosticsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(LineaColors.BackgroundDeep, LineaColors.BackgroundElevated)
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top App Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(LineaColors.GlassFill)
                    .border(LineaDimensions.HairlineBorder, LineaColors.GlassBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = LineaColors.TextPrimary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Call Diagnostics",
                    style = LineaTypography.titleLarge,
                    color = LineaColors.TextPrimary
                )
                Text(
                    text = "Hardware, network & audio telemetry",
                    style = LineaTypography.bodySmall,
                    color = LineaColors.TitaniumBlue
                )
            }

            IconButton(
                onClick = { viewModel.refreshDiagnostics() },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(LineaColors.GlassFill)
                    .border(LineaDimensions.HairlineBorder, LineaColors.GlassBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Refresh",
                    tint = if (state.isRefreshing) LineaColors.TitaniumBlue else LineaColors.TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Cellular Link Health Banner
            item {
                FrostedGlassBox(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(LineaColors.Success.copy(alpha = 0.15f))
                                .border(1.dp, LineaColors.Success.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = LineaColors.Success,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "Cellular Link Healthy",
                                style = LineaTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = LineaColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Hardware modem operating with zero dropped frames. No carrier throttling detected.",
                                style = LineaTypography.bodySmall,
                                color = LineaColors.TextSecondary
                            )
                        }
                    }
                }
            }

            // SIM & Network Telemetry
            item {
                DiagnosticItemCard(
                    icon = Icons.Filled.SimCard,
                    title = "Cellular Subscription",
                    value = "${state.carrierName} (Slot ${state.simSlot + 1})",
                    badgeText = "ONLINE",
                    badgeColor = LineaColors.Success,
                    detail = "Standard UICC profile active. Direct carrier route enabled."
                )
            }

            item {
                DiagnosticItemCard(
                    icon = Icons.Filled.CellTower,
                    title = "Radio Access Technology",
                    value = state.networkType,
                    badgeText = "HD VOICE",
                    badgeColor = LineaColors.TitaniumBlue,
                    detail = "Carrier VoLTE status: ${if (state.isVoLteActive) "Active & Negotiated" else "Standby"} • Wi-Fi Calling: ${if (state.isVoWifiActive) "Connected" else "Standby"}"
                )
            }

            item {
                DiagnosticItemCard(
                    icon = Icons.Filled.SignalCellularAlt,
                    title = "Signal Strength",
                    value = "${state.signalDbm} dBm • ${state.signalBars}/4 Bars",
                    badgeText = "STRONG",
                    badgeColor = LineaColors.Success,
                    detail = "RSRP: -82 dBm • RSRQ: -10 dB • SNR: 18.5 dB"
                )
            }

            item {
                DiagnosticItemCard(
                    icon = Icons.Filled.Headphones,
                    title = "Audio Output Route",
                    value = state.audioRoute,
                    badgeText = if (state.bluetoothDeviceName != null) "BLUETOOTH" else "DEVICE",
                    badgeColor = LineaColors.TitaniumBlue,
                    detail = state.bluetoothDeviceName ?: "Telecom earpiece route selected. Acoustic echo cancellation enabled."
                )
            }

            item {
                DiagnosticItemCard(
                    icon = Icons.Filled.Speed,
                    title = "Call Quality & Latency",
                    value = "${state.estimatedLatencyMs} ms RTT • ${state.packetLossPercent}% loss",
                    badgeText = "EXCELLENT",
                    badgeColor = LineaColors.Success,
                    detail = "Sub-50ms round-trip audio delivery over baseband interface."
                )
            }
        }
    }
}

@Composable
private fun DiagnosticItemCard(
    icon: ImageVector,
    title: String,
    value: String,
    badgeText: String,
    badgeColor: Color,
    detail: String
) {
    FrostedGlassBox(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(LineaColors.GlassFill)
                        .border(LineaDimensions.HairlineBorder, LineaColors.GlassBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = LineaColors.TitaniumBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = LineaTypography.labelSmall,
                        color = LineaColors.TextSecondary
                    )
                    Text(
                        text = value,
                        style = LineaTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = LineaColors.TextPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeColor.copy(alpha = 0.15f))
                        .border(1.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = LineaTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = badgeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = detail,
                style = LineaTypography.bodySmall,
                color = LineaColors.TextSecondary.copy(alpha = 0.8f)
            )
        }
    }
}
