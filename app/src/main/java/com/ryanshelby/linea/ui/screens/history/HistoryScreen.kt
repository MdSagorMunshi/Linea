package com.ryanshelby.linea.ui.screens.history

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ryanshelby.linea.ui.components.FrostedGlassBox
import com.ryanshelby.linea.ui.components.NeumorphicWell
import com.ryanshelby.linea.ui.components.neumorphic
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel(),
    sessionsListState: LazyListState = rememberLazyListState(),
    feedListState: LazyListState = rememberLazyListState()
) {
    val dateGroups by viewModel.dateGroups.collectAsState()
    val callSessions by viewModel.callSessions.collectAsState()
    val pinnedContacts by viewModel.pinnedContacts.collectAsState()
    val historyViewMode by viewModel.historyViewMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val selectedItemForDetail by viewModel.selectedItemForDetail.collectAsState()
    val notesForSelectedCall by viewModel.notesForSelectedCall.collectAsState()
    val blockedNumbers by viewModel.blockedNumbers.collectAsState()
    val blockedNumberSet by viewModel.blockedNumberSet.collectAsState()
    val context = LocalContext.current

    var swipedOpenItemId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(feedListState.isScrollInProgress) {
        if (feedListState.isScrollInProgress) {
            swipedOpenItemId = null
        }
    }

    val isNumberBlocked: (String) -> Boolean = { phone ->
        viewModel.isNumberBlocked(phone)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = LineaDimensions.ScreenPadding)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Title Header with Sessions / Feed Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Recent",
                    style = LineaTypography.titleLarge,
                    color = LineaColors.TextPrimary
                )
                Text(
                    text = if (historyViewMode == "SESSIONS") "Aggregated Contact Sessions" else "Cellular call logs & sessions",
                    style = LineaTypography.bodySmall,
                    color = LineaColors.TitaniumBlue
                )
            }

            // View Mode Toggle Pill (Feed vs Sessions) - Neumorphic debossed slider
            NeumorphicWell(
                shape = RoundedCornerShape(20.dp),
                depth = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .then(
                                if (historyViewMode == "FEED") {
                                    Modifier.neumorphic(
                                        shape = RoundedCornerShape(16.dp),
                                        elevation = 2.5.dp,
                                        surfaceColor = LineaColors.TitaniumBlue
                                    )
                                } else Modifier
                            )
                            .clickable { if (historyViewMode != "FEED") viewModel.toggleViewMode() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Feed",
                            style = LineaTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (historyViewMode == "FEED") Color.White else LineaColors.TextSecondary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .then(
                                if (historyViewMode == "SESSIONS") {
                                    Modifier.neumorphic(
                                        shape = RoundedCornerShape(16.dp),
                                        elevation = 2.5.dp,
                                        surfaceColor = LineaColors.TitaniumBlue
                                    )
                                } else Modifier
                            )
                            .clickable { if (historyViewMode != "SESSIONS") viewModel.toggleViewMode() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Sessions",
                            style = LineaTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (historyViewMode == "SESSIONS") Color.White else LineaColors.TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar (with text filtering) in a debossed Neumorphic well
        NeumorphicWell(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            depth = 2.5.dp
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = {
                    Text(
                        text = "Search by name, number, or date...",
                        style = LineaTypography.bodyMedium,
                        color = LineaColors.TextTertiary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = LineaColors.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Clear",
                                tint = LineaColors.TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = LineaColors.TextPrimary,
                    unfocusedTextColor = LineaColors.TextPrimary
                )
            )
        }

        // Pinned Contacts Carousel (if present and not searching)
        if (pinnedContacts.isNotEmpty() && searchQuery.isEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Pinned",
                style = LineaTypography.labelSmall,
                color = LineaColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(pinnedContacts, key = { it.id }) { contact ->
                    FrostedGlassBox(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { viewModel.onSearchQueryChange(contact.displayName) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(LineaColors.TitaniumBlue.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = contact.displayName.take(1).uppercase(),
                                    style = LineaTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    fontSize = 10.sp,
                                    color = LineaColors.TitaniumBlue
                                )
                            }
                            Text(
                                text = contact.displayName,
                                style = LineaTypography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = LineaColors.TextPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            items(HistoryFilter.values()) { filter ->
                val isSelected = filter == selectedFilter
                Box(
                    modifier = Modifier
                        .neumorphic(
                            shape = RoundedCornerShape(20.dp),
                            elevation = if (isSelected) 3.dp else 2.dp,
                            surfaceColor = if (isSelected) LineaColors.TitaniumBlue else LineaColors.NeuSurfaceRaised
                        )
                        .clickable { viewModel.onFilterSelect(filter) }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (filter) {
                            HistoryFilter.ALL -> "All Calls"
                            HistoryFilter.MISSED -> "Missed"
                            HistoryFilter.BLOCKED -> "Blocked"
                        },
                        style = LineaTypography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        color = if (isSelected) Color.White else LineaColors.TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // History Content: SESSIONS vs FEED
        if (historyViewMode == "SESSIONS") {
            if (callSessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    FrostedGlassBox(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = null,
                                tint = LineaColors.TitaniumBlue.copy(alpha = 0.6f),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No matching sessions" else "No call sessions yet",
                                style = LineaTypography.titleMedium,
                                color = LineaColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Calls are automatically grouped by contact into multi-call communication sessions.",
                                style = LineaTypography.bodySmall,
                                color = LineaColors.TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = sessionsListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(callSessions, key = { it.id }) { session ->
                        CallSessionRow(
                            session = session,
                            isBlocked = isNumberBlocked(session.phoneNumber),
                            onClick = {
                                val primary = session.calls.firstOrNull()
                                if (primary != null) {
                                    viewModel.selectItemForDetail(
                                        CallHistoryItem(
                                            id = session.id,
                                            primaryRecord = primary,
                                            groupedCalls = session.calls,
                                            callCount = session.callCount
                                        )
                                    )
                                }
                            },
                            onCallBack = { record -> viewModel.callBack(record) },
                            onToggleExpand = { viewModel.toggleSessionExpanded(session.id) }
                        )
                    }
                }
            }
        } else {
            // Standard Daily Feed
            if (dateGroups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    FrostedGlassBox(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = null,
                                tint = LineaColors.TitaniumBlue.copy(alpha = 0.6f),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No matching calls found" else "No recent calls yet",
                                style = LineaTypography.titleMedium,
                                color = LineaColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Calls placed or received on cellular SIMs will appear here grouped by day.",
                                style = LineaTypography.bodySmall,
                                color = LineaColors.TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = feedListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    dateGroups.forEach { dateGroup ->
                        item(key = "header_${dateGroup.dateHeader}") {
                            Text(
                                text = dateGroup.dateHeader,
                                style = LineaTypography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = LineaColors.TitaniumBlue,
                                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp, start = 4.dp)
                            )
                        }

                        items(dateGroup.items, key = { it.id }) { item ->
                            val isItemBlocked = isNumberBlocked(item.primaryRecord.phoneNumber)
                            HistoryRow(
                                item = item,
                                isBlocked = isItemBlocked,
                                isSwipedOpen = swipedOpenItemId == item.id,
                                onSwipeOpenChanged = { open ->
                                    swipedOpenItemId = if (open) item.id else if (swipedOpenItemId == item.id) null else swipedOpenItemId
                                },
                                onClick = { viewModel.selectItemForDetail(item) },
                                onCallBack = { record -> viewModel.callBack(record) },
                                onToggleExpand = { viewModel.toggleItemExpanded(item.id) },
                                onAddContact = { number ->
                                    try {
                                        val addIntent = Intent(Intent.ACTION_INSERT).apply {
                                            type = "vnd.android.cursor.dir/contact"
                                            putExtra("phone", number)
                                        }
                                        context.startActivity(addIntent)
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Cannot open contacts", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onSendSms = { number ->
                                    try {
                                        val smsIntent = Intent(Intent.ACTION_VIEW).apply {
                                            data = Uri.parse("sms:$number")
                                        }
                                        context.startActivity(smsIntent)
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Cannot open SMS app", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onBlockNumber = { number ->
                                    viewModel.blockNumber(number)
                                    android.widget.Toast.makeText(context, "Blocked $number", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                onUnblockNumber = { number ->
                                    viewModel.unblockNumber(number)
                                    android.widget.Toast.makeText(context, "Unblocked $number", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                onDelete = { historyItem -> viewModel.deleteGroup(historyItem) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Call Detail Bottom Sheet
    val detailItem = selectedItemForDetail
    if (detailItem != null) {
        val isDetailBlocked = isNumberBlocked(detailItem.primaryRecord.phoneNumber)
        CallDetailSheet(
            item = detailItem,
            isBlocked = isDetailBlocked,
            notes = notesForSelectedCall,
            onDismiss = { viewModel.selectItemForDetail(null) },
            onCall = { record -> viewModel.callBack(record) },
            onBlockNumber = { number ->
                viewModel.blockNumber(number)
                android.widget.Toast.makeText(context, "Blocked $number", android.widget.Toast.LENGTH_SHORT).show()
            },
            onUnblockNumber = { number ->
                viewModel.unblockNumber(number)
                android.widget.Toast.makeText(context, "Unblocked $number", android.widget.Toast.LENGTH_SHORT).show()
            },
            onAddNote = { number, contactId, noteText -> viewModel.addNote(number, contactId, noteText) },
            onScheduleReminder = { number, name, delayHours -> viewModel.scheduleCallbackReminder(number, name, delayHours) },
            onScheduleReminderMs = { number, name, delayMs -> viewModel.scheduleCallbackReminderMs(number, name, delayMs) }
        )
    }
}
