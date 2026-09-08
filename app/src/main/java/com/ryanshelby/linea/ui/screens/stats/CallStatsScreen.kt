package com.ryanshelby.linea.ui.screens.stats

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ryanshelby.linea.ui.components.FrostedGlassBox
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography

@Composable
fun CallStatsScreen(
    onNavigateBack: () -> Unit,
    viewModel: CallStatsViewModel = hiltViewModel()
) {
    val stats by viewModel.stats.collectAsState()

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

        // Header
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

            Column {
                Text(
                    text = "Call Statistics",
                    style = LineaTypography.titleLarge,
                    color = LineaColors.TextPrimary
                )
                Text(
                    text = "100% on-device cellular metrics",
                    style = LineaTypography.bodySmall,
                    color = LineaColors.TitaniumBlue
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
            // Overview Talk Time Card
            item {
                FrostedGlassBox(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "TOTAL TALK TIME",
                            style = LineaTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = LineaColors.TitaniumBlue
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = formatDuration(stats.totalDurationSeconds),
                            style = LineaTypography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = LineaColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Across ${stats.totalCalls} logged calls • Avg ${formatDuration(stats.avgDurationSeconds)} / call",
                            style = LineaTypography.bodySmall,
                            color = LineaColors.TextSecondary
                        )
                    }
                }
            }

            // Directional Breakdown Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricBox(
                        title = "Incoming",
                        count = stats.incomingCalls,
                        color = LineaColors.Success,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = "Outgoing",
                        count = stats.outgoingCalls,
                        color = LineaColors.TitaniumBlue,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricBox(
                        title = "Missed",
                        count = stats.missedCalls,
                        color = LineaColors.Danger,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = "Blocked",
                        count = stats.blockedCalls,
                        color = LineaColors.Warning,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // SIM Slot Usage
            item {
                FrostedGlassBox(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Text(
                            text = "Cellular SIM Usage",
                            style = LineaTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = LineaColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SIM 1",
                                style = LineaTypography.labelSmall,
                                color = LineaColors.TitaniumBlue,
                                modifier = Modifier.width(50.dp)
                            )
                            val simTotal = (stats.sim1Count + stats.sim2Count).coerceAtLeast(1)
                            val sim1Frac = stats.sim1Count.toFloat() / simTotal
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(LineaColors.GlassFill)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(sim1Frac)
                                        .fillMaxHeight()
                                        .background(LineaColors.TitaniumBlue)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "${stats.sim1Count} calls",
                                style = LineaTypography.bodySmall,
                                color = LineaColors.TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SIM 2",
                                style = LineaTypography.labelSmall,
                                color = LineaColors.Success,
                                modifier = Modifier.width(50.dp)
                            )
                            val simTotal = (stats.sim1Count + stats.sim2Count).coerceAtLeast(1)
                            val sim2Frac = stats.sim2Count.toFloat() / simTotal
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(LineaColors.GlassFill)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(sim2Frac)
                                        .fillMaxHeight()
                                        .background(LineaColors.Success)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "${stats.sim2Count} calls",
                                style = LineaTypography.bodySmall,
                                color = LineaColors.TextPrimary
                            )
                        }
                    }
                }
            }

            // Top Frequent Callers
            if (stats.topContacts.isNotEmpty()) {
                item {
                    Text(
                        text = "Most Frequent Callers",
                        style = LineaTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = LineaColors.TitaniumBlue,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                items(stats.topContacts.size) { idx ->
                    val contact = stats.topContacts[idx]
                    FrostedGlassBox(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(LineaColors.GlassFill)
                                    .border(1.dp, LineaColors.GlassBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "#${idx + 1}",
                                    style = LineaTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = LineaColors.TitaniumBlue
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = contact.nameOrNumber,
                                    style = LineaTypography.titleSmall,
                                    color = LineaColors.TextPrimary
                                )
                                Text(
                                    text = "${contact.callCount} calls • ${formatDuration(contact.totalDuration)}",
                                    style = LineaTypography.bodySmall,
                                    color = LineaColors.TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBox(
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    FrostedGlassBox(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(
                text = title,
                style = LineaTypography.labelSmall,
                color = LineaColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = count.toString(),
                style = LineaTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = color
            )
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return when {
        hrs > 0 -> "${hrs}h ${mins}m ${secs}s"
        mins > 0 -> "${mins}m ${secs}s"
        else -> "${secs}s"
    }
}
