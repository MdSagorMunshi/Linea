package com.ryanshelby.linea.ui.screens.contacts

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ryanshelby.linea.ui.components.FrostedGlassBox
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ContactDashboardScreen(
    contactId: Long,
    onNavigateBack: () -> Unit,
    viewModel: ContactDashboardViewModel = hiltViewModel()
) {
    LaunchedEffect(contactId) {
        viewModel.loadContactData(contactId)
    }

    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var noteInput by remember { mutableStateOf("") }
    var isEditingNote by remember { mutableStateOf(false) }

    LaunchedEffect(state.preCallNote) {
        if (state.preCallNote != null) {
            noteInput = state.preCallNote!!.noteText
        }
    }

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

        // Top Navigation Bar
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

            Spacer(modifier = Modifier.weight(1f))

            // Star Favorite Button
            IconButton(
                onClick = { viewModel.toggleFavorite() },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(LineaColors.GlassFill)
                    .border(LineaDimensions.HairlineBorder, LineaColors.GlassBorder, CircleShape)
            ) {
                Icon(
                    imageVector = if (state.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (state.isFavorite) LineaColors.Warning else LineaColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Private Contact Lock Button
            IconButton(
                onClick = { viewModel.togglePrivate() },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(LineaColors.GlassFill)
                    .border(LineaDimensions.HairlineBorder, LineaColors.GlassBorder, CircleShape)
            ) {
                Icon(
                    imageVector = if (state.isPrivate) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    contentDescription = "Private Contact",
                    tint = if (state.isPrivate) LineaColors.Danger else LineaColors.TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val contact = state.contact
        if (contact != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Header Profile Card
                item {
                    FrostedGlassBox(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(LineaColors.TitaniumBlue.copy(alpha = 0.2f))
                                    .border(1.5.dp, LineaColors.TitaniumBlue, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = contact.displayName.take(1).uppercase(),
                                    style = LineaTypography.displayLarge.copy(fontWeight = FontWeight.Bold),
                                    color = LineaColors.TextPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = contact.displayName,
                                style = LineaTypography.titleLarge,
                                color = LineaColors.TextPrimary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )

                            if (!contact.company.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${contact.jobTitle?.let { "$it at " } ?: ""}${contact.company}",
                                    style = LineaTypography.bodySmall,
                                    color = LineaColors.TitaniumBlue
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Action Buttons Row (Call, SMS, Share)
                            val primaryNumber = state.numbers.firstOrNull()?.number ?: ""
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                DashboardActionButton(
                                    icon = Icons.Filled.Call,
                                    label = "Call",
                                    color = LineaColors.Success,
                                    onClick = {
                                        if (primaryNumber.isNotBlank()) {
                                            viewModel.placeCall(primaryNumber)
                                        }
                                    }
                                )
                                DashboardActionButton(
                                    icon = Icons.Filled.Message,
                                    label = "Message",
                                    color = LineaColors.TitaniumBlue,
                                    onClick = {
                                        if (primaryNumber.isNotBlank()) {
                                            val smsIntent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:$primaryNumber"))
                                            context.startActivity(smsIntent)
                                        }
                                    }
                                )
                                DashboardActionButton(
                                    icon = Icons.Filled.Share,
                                    label = "Share",
                                    color = LineaColors.TextSecondary,
                                    onClick = {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, "${contact.displayName}: $primaryNumber")
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Contact"))
                                    }
                                )
                            }
                        }
                    }
                }

                // Availability Insight Card (100% Local / Zero AI Heuristic)
                item {
                    val insight = state.availabilityInsight
                    FrostedGlassBox(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(LineaColors.TitaniumBlue.copy(alpha = 0.15f))
                                        .border(1.dp, LineaColors.TitaniumBlue.copy(alpha = 0.4f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.QueryBuilder,
                                        contentDescription = null,
                                        tint = LineaColors.TitaniumBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "LOCAL AVAILABILITY INSIGHT",
                                        style = LineaTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = LineaColors.TitaniumBlue
                                    )
                                    Text(
                                        text = if (insight?.hasSufficientData == true) insight.bestTimeWindow else "Calculating...",
                                        style = LineaTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = LineaColors.TextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                if (insight?.hasSufficientData == true) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(LineaColors.Success.copy(alpha = 0.15f))
                                            .border(1.dp, LineaColors.Success.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${insight.answerRatePercent}% MATCH",
                                            style = LineaTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = LineaColors.Success
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = insight?.summary ?: "No call logs yet to compute availability heuristic.",
                                style = LineaTypography.bodySmall,
                                color = LineaColors.TextSecondary
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Last interaction summary
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.History,
                                    contentDescription = null,
                                    tint = LineaColors.TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Last Interaction: ${state.lastInteractionSummary}",
                                    style = LineaTypography.bodySmall,
                                    color = LineaColors.TextPrimary
                                )
                            }
                        }
                    }
                }

                // Per-Contact Settings Card
                item {
                    FrostedGlassBox(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Text(
                                text = "Contact-Specific Behavior",
                                style = LineaTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = LineaColors.TextPrimary
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Preferred SIM selector
                            Text(
                                text = "Preferred Cellular SIM",
                                style = LineaTypography.labelSmall,
                                color = LineaColors.TextSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SimChip(
                                    label = "Auto / System",
                                    selected = state.preferredSimSlot == null,
                                    onClick = { viewModel.setPreferredSim(null) },
                                    modifier = Modifier.weight(1f)
                                )
                                SimChip(
                                    label = "Force SIM 1",
                                    selected = state.preferredSimSlot == 0,
                                    onClick = { viewModel.setPreferredSim(0) },
                                    modifier = Modifier.weight(1f)
                                )
                                SimChip(
                                    label = "Force SIM 2",
                                    selected = state.preferredSimSlot == 1,
                                    onClick = { viewModel.setPreferredSim(1) },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Always Ring Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Always Ring (Override Restrictions)",
                                        style = LineaTypography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = LineaColors.TextPrimary
                                    )
                                    Text(
                                        text = "Bypasses quiet hours, work focus schedules & screening",
                                        style = LineaTypography.bodySmall,
                                        color = LineaColors.TextSecondary
                                    )
                                }
                                Switch(
                                    checked = state.alwaysRing,
                                    onCheckedChange = { viewModel.toggleAlwaysRing() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = LineaColors.TitaniumBlue,
                                        checkedTrackColor = LineaColors.TitaniumBlue.copy(alpha = 0.3f),
                                        uncheckedThumbColor = LineaColors.TextSecondary,
                                        uncheckedTrackColor = LineaColors.GlassBorder
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Allow during restricted focus hours
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Allow During Work Focus",
                                        style = LineaTypography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = LineaColors.TextPrimary
                                    )
                                    Text(
                                        text = "Allowed through scheduled workday filtering rules",
                                        style = LineaTypography.bodySmall,
                                        color = LineaColors.TextSecondary
                                    )
                                }
                                Switch(
                                    checked = state.allowRestrictedHours,
                                    onCheckedChange = { viewModel.toggleAllowRestrictedHours() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = LineaColors.TitaniumBlue,
                                        checkedTrackColor = LineaColors.TitaniumBlue.copy(alpha = 0.3f),
                                        uncheckedThumbColor = LineaColors.TextSecondary,
                                        uncheckedTrackColor = LineaColors.GlassBorder
                                    )
                                )
                            }
                        }
                    }
                }

                // Pre-Call Note Card
                item {
                    FrostedGlassBox(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(LineaColors.Warning.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PushPin,
                                        contentDescription = null,
                                        tint = LineaColors.Warning,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Pre-Call Note",
                                        style = LineaTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = LineaColors.TextPrimary
                                    )
                                    Text(
                                        text = "Displayed on-screen during outgoing & active calls",
                                        style = LineaTypography.bodySmall,
                                        color = LineaColors.TextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = noteInput,
                                onValueChange = { noteInput = it },
                                placeholder = { Text("Enter agenda or discussion topic...", color = LineaColors.TextSecondary) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = LineaColors.TextPrimary,
                                    unfocusedTextColor = LineaColors.TextPrimary,
                                    focusedBorderColor = LineaColors.TitaniumBlue,
                                    unfocusedBorderColor = LineaColors.GlassBorder
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    viewModel.savePreCallNote(noteInput)
                                },
                                modifier = Modifier.align(Alignment.End),
                                colors = ButtonDefaults.buttonColors(containerColor = LineaColors.TitaniumBlue),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Save Note", style = LineaTypography.labelMedium)
                            }
                        }
                    }
                }

                // Recent Calls History
                if (state.callHistory.isNotEmpty()) {
                    item {
                        Text(
                            text = "Recent Calls with ${contact.displayName}",
                            style = LineaTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = LineaColors.TitaniumBlue,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(state.callHistory) { record ->
                        FrostedGlassBox(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val dateStr = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(record.timestamp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = dateStr,
                                        style = LineaTypography.bodyMedium,
                                        color = LineaColors.TextPrimary
                                    )
                                    Text(
                                        text = "${record.callType.name} • ${record.durationSeconds}s duration",
                                        style = LineaTypography.bodySmall,
                                        color = LineaColors.TextSecondary
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(LineaColors.GlassFill)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "SIM ${record.simSlot + 1}",
                                        style = LineaTypography.labelSmall,
                                        color = LineaColors.TitaniumBlue
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f))
                .border(1.dp, color.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = LineaTypography.labelSmall,
            color = LineaColors.TextPrimary
        )
    }
}

@Composable
private fun SimChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) LineaColors.TitaniumBlue else LineaColors.GlassFill)
            .border(
                LineaDimensions.HairlineBorder,
                if (selected) LineaColors.TitaniumBlue else LineaColors.GlassBorder,
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = LineaTypography.labelSmall.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal),
            color = if (selected) Color.White else LineaColors.TextSecondary
        )
    }
}
