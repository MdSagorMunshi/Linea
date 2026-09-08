package com.ryanshelby.linea.ui.screens.history

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.data.local.entities.CallNoteEntity
import com.ryanshelby.linea.data.local.entities.CallRecordEntity
import com.ryanshelby.linea.ui.components.FrostedGlassBox
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallDetailSheet(
    item: CallHistoryItem,
    notes: List<CallNoteEntity>,
    onDismiss: () -> Unit,
    onCall: (CallRecordEntity) -> Unit,
    onBlockNumber: (String) -> Unit,
    onAddNote: (String, Long?, String) -> Unit,
    onScheduleReminder: (String, String?, Long) -> Unit,
    onScheduleReminderMs: ((String, String?, Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    var noteInput by remember { mutableStateOf("") }
    var reminderMessage by remember { mutableStateOf<String?>(null) }
    var showReminderCustomSheet by remember { mutableStateOf(false) }

    val totalDuration = item.groupedCalls.sumOf { it.durationSeconds }
    val connectedCalls = item.groupedCalls.count { it.durationSeconds > 0 }
    val avgDuration = if (connectedCalls > 0) totalDuration / connectedCalls else 0L

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LineaColors.BackgroundTop,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header with Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Call Details",
                    style = LineaTypography.titleMedium,
                    color = LineaColors.TextPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = LineaColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Contact Card Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(LineaColors.GlassFill)
                        .border(LineaDimensions.HairlineBorder, LineaColors.GlassBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = LineaColors.TextSecondary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = item.primaryRecord.callerName ?: item.primaryRecord.phoneNumber,
                        style = LineaTypography.headlineMedium,
                        color = LineaColors.TextPrimary,
                        fontSize = 20.sp
                    )
                    if (item.primaryRecord.callerName != null) {
                        Text(
                            text = item.primaryRecord.phoneNumber,
                            style = LineaTypography.bodyMedium.copy(
                                fontFeatureSettings = "tnum"
                            ),
                            color = LineaColors.TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats Row (Frosted glass card with Total Talk Time & Avg Duration)
            FrostedGlassBox(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = HistoryGrouper.formatDuration(totalDuration).ifEmpty { "0s" },
                            style = LineaTypography.titleMedium.copy(
                                fontFeatureSettings = "tnum",
                                fontWeight = FontWeight.Bold
                            ),
                            color = LineaColors.TitaniumBlue
                        )
                        Text(
                            text = "Total Talk Time",
                            style = LineaTypography.labelSmall,
                            color = LineaColors.TextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .width(1.dp)
                            .background(LineaColors.GlassBorder)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = HistoryGrouper.formatDuration(avgDuration).ifEmpty { "0s" },
                            style = LineaTypography.titleMedium.copy(
                                fontFeatureSettings = "tnum",
                                fontWeight = FontWeight.Bold
                            ),
                            color = LineaColors.TextPrimary
                        )
                        Text(
                            text = "Avg Duration",
                            style = LineaTypography.labelSmall,
                            color = LineaColors.TextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .width(1.dp)
                            .background(LineaColors.GlassBorder)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${item.groupedCalls.size}",
                            style = LineaTypography.titleMedium.copy(
                                fontFeatureSettings = "tnum",
                                fontWeight = FontWeight.Bold
                            ),
                            color = LineaColors.TextPrimary
                        )
                        Text(
                            text = "Total Calls",
                            style = LineaTypography.labelSmall,
                            color = LineaColors.TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons Row (Call, SMS, Share, Block)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DetailActionButton(
                    icon = Icons.Filled.Call,
                    label = "Call",
                    tint = LineaColors.TitaniumBlue,
                    onClick = { onCall(item.primaryRecord) }
                )
                DetailActionButton(
                    icon = Icons.Filled.Message,
                    label = "SMS",
                    tint = LineaColors.TextPrimary,
                    onClick = {
                        val smsIntent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("sms:${item.primaryRecord.phoneNumber}")
                        }
                        context.startActivity(smsIntent)
                    }
                )
                DetailActionButton(
                    icon = Icons.Filled.Share,
                    label = "Share",
                    tint = LineaColors.TextPrimary,
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "${item.primaryRecord.callerName ?: "Contact"}: ${item.primaryRecord.phoneNumber}")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Contact"))
                    }
                )
                DetailActionButton(
                    icon = Icons.Filled.Block,
                    label = "Block",
                    tint = LineaColors.Danger,
                    onClick = {
                        onBlockNumber(item.primaryRecord.phoneNumber)
                        onDismiss()
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Callback Reminder Options
            Text(
                text = "Callback Reminder",
                style = LineaTypography.titleSmall,
                color = LineaColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    10L * 60 * 1000L to "10m",
                    30L * 60 * 1000L to "30m",
                    60L * 60 * 1000L to "1h"
                ).forEach { (delayMs, label) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(LineaColors.GlassFill)
                            .border(LineaDimensions.HairlineBorder, LineaColors.GlassBorder, RoundedCornerShape(10.dp))
                            .clickable {
                                onScheduleReminderMs?.invoke(item.primaryRecord.phoneNumber, item.primaryRecord.callerName, delayMs)
                                    ?: onScheduleReminder(item.primaryRecord.phoneNumber, item.primaryRecord.callerName, (delayMs / 3600000L).coerceAtLeast(1L))
                                reminderMessage = "Reminder scheduled for $label"
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = LineaTypography.labelSmall,
                            color = LineaColors.TitaniumBlue
                        )
                    }
                }

                // Custom / More Options
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(LineaColors.TitaniumBlue.copy(alpha = 0.15f))
                        .border(LineaDimensions.HairlineBorder, LineaColors.TitaniumBlue.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .clickable { showReminderCustomSheet = true }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "More...",
                        style = LineaTypography.labelSmall,
                        color = LineaColors.TitaniumBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (reminderMessage != null) {
                Text(
                    text = reminderMessage ?: "",
                    style = LineaTypography.bodySmall,
                    color = LineaColors.MutedSageGreen,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (showReminderCustomSheet) {
                CallbackReminderSheet(
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    phoneNumber = item.primaryRecord.phoneNumber,
                    callerName = item.primaryRecord.callerName,
                    onDismiss = { showReminderCustomSheet = false },
                    onScheduleReminder = { delayMs ->
                        onScheduleReminderMs?.invoke(item.primaryRecord.phoneNumber, item.primaryRecord.callerName, delayMs)
                            ?: onScheduleReminder(item.primaryRecord.phoneNumber, item.primaryRecord.callerName, (delayMs / 3600000L).coerceAtLeast(1L))
                        reminderMessage = "Reminder scheduled"
                        showReminderCustomSheet = false
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add Note Field
            Text(
                text = "Notes",
                style = LineaTypography.titleSmall,
                color = LineaColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    placeholder = {
                        Text(
                            text = "Add a note to this call...",
                            style = LineaTypography.bodySmall,
                            color = LineaColors.TextTertiary
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LineaColors.TitaniumBlue,
                        unfocusedBorderColor = LineaColors.GlassBorder,
                        focusedTextColor = LineaColors.TextPrimary,
                        unfocusedTextColor = LineaColors.TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (noteInput.isNotBlank()) LineaColors.TitaniumBlue else LineaColors.GlassFill)
                        .clickable(enabled = noteInput.isNotBlank()) {
                            onAddNote(item.primaryRecord.phoneNumber, null, noteInput)
                            noteInput = ""
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.NoteAdd,
                        contentDescription = "Save Note",
                        tint = if (noteInput.isNotBlank()) Color.White else LineaColors.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Existing Notes List
            if (notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    notes.forEach { note ->
                        FrostedGlassBox(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = note.noteText,
                                    style = LineaTypography.bodySmall,
                                    color = LineaColors.TextPrimary
                                )
                                Text(
                                    text = HistoryGrouper.formatRelativeTime(note.timestamp),
                                    style = LineaTypography.labelSmall,
                                    color = LineaColors.TextTertiary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chronological Call Timeline
            Text(
                text = "Call History",
                style = LineaTypography.titleSmall,
                color = LineaColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 180.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(item.groupedCalls, key = { it.id }) { call ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CallTypeIcon(call.callType, size = 16.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${HistoryGrouper.formatRelativeDate(call.timestamp)}, ${HistoryGrouper.formatExactTime(call.timestamp)}",
                                style = LineaTypography.bodySmall,
                                color = LineaColors.TextSecondary
                            )
                        }

                        Text(
                            text = if (call.durationSeconds > 0) {
                                HistoryGrouper.formatDuration(call.durationSeconds)
                            } else {
                                call.callType.name.lowercase().replaceFirstChar { it.uppercase() }
                            },
                            style = LineaTypography.bodySmall.copy(
                                fontFeatureSettings = "tnum"
                            ),
                            color = LineaColors.TextTertiary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DetailActionButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(LineaColors.GlassFill)
                .border(LineaDimensions.HairlineBorder, LineaColors.GlassBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = LineaTypography.labelSmall,
            color = LineaColors.TextSecondary
        )
    }
}
