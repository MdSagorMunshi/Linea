package com.ryanshelby.linea.ui.screens.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.data.local.entities.CallDirectionType
import com.ryanshelby.linea.data.local.entities.CallRecordEntity
import com.ryanshelby.linea.ui.components.FrostedGlassBox
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryRow(
    item: CallHistoryItem,
    onClick: () -> Unit,
    onCallBack: (CallRecordEntity) -> Unit,
    onToggleExpand: () -> Unit,
    onAddContact: (String) -> Unit,
    onSendSms: (String) -> Unit,
    onBlockNumber: (String) -> Unit,
    onDelete: (CallHistoryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onCallBack(item.primaryRecord)
                    false // Return back to rest position
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    false // Keep open or let user tap action
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    // Right swipe background: Call Back (Sage Green)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(LineaColors.MutedSageGreen.copy(alpha = 0.25f))
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Call,
                                contentDescription = "Call Back",
                                tint = LineaColors.MutedSageGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Call Back",
                                style = LineaTypography.labelMedium,
                                color = LineaColors.MutedSageGreen
                            )
                        }
                    }
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    // Left swipe background: Quick Actions
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(LineaColors.GlassFill)
                            .border(LineaDimensions.HairlineBorder, LineaColors.GlassBorder, RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onAddContact(item.primaryRecord.phoneNumber) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PersonAdd,
                                    contentDescription = "Add Contact",
                                    tint = LineaColors.TitaniumBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { onSendSms(item.primaryRecord.phoneNumber) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Message,
                                    contentDescription = "SMS",
                                    tint = LineaColors.TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { onBlockNumber(item.primaryRecord.phoneNumber) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Block,
                                    contentDescription = "Block",
                                    tint = LineaColors.MutedRust,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { onDelete(item) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Delete",
                                    tint = LineaColors.Danger,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
                SwipeToDismissBoxValue.Settled -> Unit
            }
        },
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(LineaColors.GlassFill)
                .border(LineaDimensions.HairlineBorder, LineaColors.GlassBorder, RoundedCornerShape(16.dp))
        ) {
            // Main History Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Contact Avatar or Initials
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(LineaColors.BackgroundBottom)
                            .border(LineaDimensions.HairlineBorder, LineaColors.GlassBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val initials = getInitials(item.primaryRecord.callerName)
                        if (initials.isNotEmpty()) {
                            Text(
                                text = initials,
                                style = LineaTypography.titleSmall,
                                color = LineaColors.TextPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = LineaColors.TextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.primaryRecord.callerName ?: item.primaryRecord.phoneNumber,
                                style = LineaTypography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = if (item.primaryRecord.callType == CallDirectionType.MISSED) {
                                    LineaColors.MutedRust
                                } else {
                                    LineaColors.TextPrimary
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Grouped Call Count Badge (e.g. ×3)
                            if (item.callCount > 1) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(LineaColors.TitaniumBlue.copy(alpha = 0.25f))
                                        .clickable { onToggleExpand() }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "×${item.callCount}",
                                            style = LineaTypography.labelSmall.copy(
                                                fontFeatureSettings = "tnum"
                                            ),
                                            color = LineaColors.TitaniumBlue,
                                            fontSize = 10.sp
                                        )
                                        Icon(
                                            imageVector = if (item.isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                            contentDescription = "Expand",
                                            tint = LineaColors.TitaniumBlue,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        // Subline: Call type icon + Relative timestamp + Duration + SIM indicator
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CallTypeIcon(item.primaryRecord.callType)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = HistoryGrouper.formatRelativeTime(item.primaryRecord.timestamp),
                                style = LineaTypography.bodySmall,
                                color = LineaColors.TextSecondary
                            )

                            if (item.primaryRecord.durationSeconds > 0) {
                                Text(
                                    text = " • ${HistoryGrouper.formatDuration(item.primaryRecord.durationSeconds)}",
                                    style = LineaTypography.bodySmall.copy(
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = LineaColors.TextTertiary
                                )
                            }

                            // SIM slot indicator pill
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(LineaColors.GlassFill)
                                    .border(0.5.dp, LineaColors.GlassBorder, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "SIM ${item.primaryRecord.simSlot + 1}",
                                    style = LineaTypography.labelSmall,
                                    fontSize = 9.sp,
                                    color = LineaColors.TextTertiary
                                )
                            }
                        }
                    }
                }

                // Quick Call Back Button
                IconButton(
                    onClick = { onCallBack(item.primaryRecord) },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Call,
                        contentDescription = "Call",
                        tint = LineaColors.TitaniumBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Expandable List of Individual Calls in this Session
            AnimatedVisibility(
                visible = item.isExpanded && item.callCount > 1,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LineaColors.BackgroundTop.copy(alpha = 0.5f))
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item.groupedCalls.forEach { singleCall ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CallTypeIcon(singleCall.callType, size = 14.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = HistoryGrouper.formatExactTime(singleCall.timestamp),
                                    style = LineaTypography.bodySmall.copy(
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = LineaColors.TextSecondary
                                )
                            }

                            Text(
                                text = if (singleCall.durationSeconds > 0) {
                                    HistoryGrouper.formatDuration(singleCall.durationSeconds)
                                } else {
                                    when (singleCall.callType) {
                                        CallDirectionType.MISSED -> "Missed"
                                        CallDirectionType.REJECTED -> "Declined"
                                        CallDirectionType.BLOCKED -> "Blocked"
                                        else -> "No answer"
                                    }
                                },
                                style = LineaTypography.bodySmall.copy(
                                    fontFeatureSettings = "tnum"
                                ),
                                color = LineaColors.TextTertiary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CallTypeIcon(type: CallDirectionType, size: androidx.compose.ui.unit.Dp = 16.dp) {
    val (icon, tint) = when (type) {
        CallDirectionType.INCOMING -> Icons.AutoMirrored.Filled.CallReceived to LineaColors.MutedSageGreen
        CallDirectionType.OUTGOING -> Icons.AutoMirrored.Filled.CallMade to LineaColors.TitaniumBlue
        CallDirectionType.MISSED -> Icons.AutoMirrored.Filled.CallMissed to LineaColors.MutedRust
        CallDirectionType.REJECTED -> Icons.Filled.CallEnd to LineaColors.Danger
        CallDirectionType.BLOCKED -> Icons.Filled.Block to LineaColors.Danger
    }

    Icon(
        imageVector = icon,
        contentDescription = type.name,
        tint = tint,
        modifier = Modifier.size(size)
    )
}

private fun getInitials(name: String?): String {
    if (name.isNullOrBlank()) return ""
    val words = name.trim().split(" ")
    return if (words.size >= 2) {
        "${words[0].firstOrNull() ?: ""}${words[1].firstOrNull() ?: ""}".uppercase()
    } else {
        name.take(1).uppercase()
    }
}

@Composable
fun CallSessionRow(
    session: CallSessionItem,
    onClick: () -> Unit,
    onCallBack: (CallRecordEntity) -> Unit,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    FrostedGlassBox(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar / Initials
                val initials = getInitials(session.callerName)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(LineaColors.GlassFill)
                        .border(LineaDimensions.HairlineBorder, LineaColors.GlassBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (initials.isNotEmpty()) {
                        Text(
                            text = initials,
                            style = LineaTypography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = LineaColors.TitaniumBlue
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = LineaColors.TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Name & Metadata
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = session.callerName ?: session.phoneNumber,
                            style = LineaTypography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFeatureSettings = "tnum"
                            ),
                            color = LineaColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        // Call count pill
                        if (session.callCount > 1) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(LineaColors.TitaniumBlue.copy(alpha = 0.15f))
                                    .border(0.5.dp, LineaColors.TitaniumBlue.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "×${session.callCount}",
                                    style = LineaTypography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    fontSize = 10.sp,
                                    color = LineaColors.TitaniumBlue
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CallTypeIcon(session.latestCallType, size = 13.dp)

                        Text(
                            text = HistoryGrouper.formatRelativeTime(session.latestTimestamp),
                            style = LineaTypography.bodySmall.copy(fontFeatureSettings = "tnum"),
                            color = LineaColors.TextSecondary
                        )

                        Text(text = "•", color = LineaColors.TextTertiary, style = LineaTypography.bodySmall)

                        Text(
                            text = if (session.totalDurationSeconds > 0) {
                                HistoryGrouper.formatDuration(session.totalDurationSeconds)
                            } else {
                                "0s"
                            },
                            style = LineaTypography.bodySmall.copy(fontFeatureSettings = "tnum"),
                            color = LineaColors.TitaniumBlue
                        )

                        // SIM Slot
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(LineaColors.GlassFill)
                                .border(0.5.dp, LineaColors.GlassBorder, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "SIM ${session.latestSimSlot + 1}",
                                style = LineaTypography.labelSmall,
                                fontSize = 9.sp,
                                color = LineaColors.TextTertiary
                            )
                        }
                    }
                }

                // Actions: Expand & Call
                if (session.callCount > 1) {
                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (session.isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (session.isExpanded) "Collapse" else "Expand",
                            tint = LineaColors.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                val primaryRecord = session.calls.firstOrNull()
                if (primaryRecord != null) {
                    IconButton(
                        onClick = { onCallBack(primaryRecord) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Call,
                            contentDescription = "Call",
                            tint = LineaColors.TitaniumBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Expandable breakdown
            AnimatedVisibility(
                visible = session.isExpanded && session.callCount > 1,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LineaColors.BackgroundTop.copy(alpha = 0.5f))
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    session.calls.forEach { call ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CallTypeIcon(call.callType, size = 13.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${HistoryGrouper.formatRelativeDate(call.timestamp)}, ${HistoryGrouper.formatExactTime(call.timestamp)}",
                                    style = LineaTypography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                    color = LineaColors.TextSecondary
                                )
                            }

                            Text(
                                text = if (call.durationSeconds > 0) {
                                    HistoryGrouper.formatDuration(call.durationSeconds)
                                } else {
                                    when (call.callType) {
                                        CallDirectionType.MISSED -> "Missed"
                                        CallDirectionType.REJECTED -> "Declined"
                                        CallDirectionType.BLOCKED -> "Blocked"
                                        else -> "No answer"
                                    }
                                },
                                style = LineaTypography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                color = LineaColors.TextTertiary
                            )
                        }
                    }
                }
            }
        }
    }
}

