package com.ryanshelby.linea.mergefix

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ryanshelby.linea.data.local.entities.MergeHistoryEntity
import com.ryanshelby.linea.ui.components.ContactAvatar
import com.ryanshelby.linea.ui.components.FrostedGlassBox
import com.ryanshelby.linea.ui.components.neumorphic
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FixAndManageScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FixAndManageViewModel = hiltViewModel()
) {
    val currentSection by viewModel.currentSection.collectAsState()
    val highGroups by viewModel.highConfidenceGroups.collectAsState()
    val mediumGroups by viewModel.mediumConfidenceGroups.collectAsState()
    val lowGroups by viewModel.lowConfidenceGroups.collectAsState()
    val unnamedContacts by viewModel.unnamedContacts.collectAsState()
    val malformedNames by viewModel.malformedNameContacts.collectAsState()
    val unnormalizedNumbers by viewModel.unnormalizedNumberIssues.collectAsState()
    val emptyContacts by viewModel.emptyContacts.collectAsState()
    val mergeHistory by viewModel.mergeHistory.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()
    val undoCountdown by viewModel.undoCountdownSeconds.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var renamingContact by remember { mutableStateOf<NormalizedContact?>(null) }
    var renameInput by remember { mutableStateOf("") }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            scope.launch {
                snackbarHostState.showSnackbar(msg)
                viewModel.clearUserMessage()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = LineaColors.BackgroundDeep,
        modifier = modifier.fillMaxSize().statusBarsPadding()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = LineaDimensions.ScreenPadding)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = LineaColors.TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Fix & Manage",
                                style = LineaTypography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = LineaColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(LineaColors.TitaniumBlue.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "SOVEREIGN",
                                    style = LineaTypography.labelSmall,
                                    color = LineaColors.TitaniumBlue,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Text(
                            text = "Duplicates, Contact Cleanup & Merge History",
                            style = LineaTypography.bodySmall,
                            color = LineaColors.TextSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (undoCountdown > 0) {
                        IconButton(onClick = { viewModel.undoLastMerge() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo",
                                tint = LineaColors.MutedSageGreen
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.rescan() }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh Scan",
                            tint = LineaColors.TitaniumBlue
                        )
                    }
                }
            }

            // Section Tabs Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val dupCount = mediumGroups.size + lowGroups.size
                val fixCount = unnamedContacts.size + malformedNames.size + unnormalizedNumbers.size + emptyContacts.size
                val historyCount = mergeHistory.size

                SectionChip(
                    title = "Duplicates",
                    count = dupCount,
                    isSelected = currentSection == FixAndManageSection.MERGE_DUPLICATES,
                    onClick = { viewModel.setSection(FixAndManageSection.MERGE_DUPLICATES) },
                    modifier = Modifier.weight(1f)
                )
                SectionChip(
                    title = "Fix Issues",
                    count = fixCount,
                    isSelected = currentSection == FixAndManageSection.FIX_ISSUES,
                    onClick = { viewModel.setSection(FixAndManageSection.FIX_ISSUES) },
                    modifier = Modifier.weight(1f)
                )
                SectionChip(
                    title = "History",
                    count = historyCount,
                    isSelected = currentSection == FixAndManageSection.MERGE_HISTORY,
                    onClick = { viewModel.setSection(FixAndManageSection.MERGE_HISTORY) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 30-Second Undo Banner if active
            AnimatedVisibility(visible = undoCountdown > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(LineaColors.MutedSageGreen.copy(alpha = 0.15f))
                        .border(1.dp, LineaColors.MutedSageGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = null,
                                tint = LineaColors.MutedSageGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Merged · ${undoCountdown}s undo",
                                style = LineaTypography.bodySmall,
                                color = LineaColors.MutedSageGreen,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        TextButton(
                            onClick = { viewModel.undoLastMerge() },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Undo",
                                style = LineaTypography.labelMedium,
                                color = LineaColors.MutedSageGreen,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            if (isScanning) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = LineaColors.TitaniumBlue,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Analyzing contacts locally with Zero-Cloud...",
                            style = LineaTypography.bodyMedium,
                            color = LineaColors.TextSecondary
                        )
                    }
                }
            } else {
                when (currentSection) {
                    FixAndManageSection.MERGE_DUPLICATES -> {
                        MergeDuplicatesSection(
                            highGroups = highGroups,
                            mediumGroups = mediumGroups,
                            lowGroups = lowGroups,
                            undoCountdown = undoCountdown,
                            onMergeGroup = { viewModel.mergeGroup(it) },
                            onDismissMedium = { viewModel.dismissMedium(it) },
                            onDismissLow = { viewModel.dismissLow(it) },
                            onUndoMerge = { viewModel.undoLastMerge() }
                        )
                    }
                    FixAndManageSection.FIX_ISSUES -> {
                        FixIssuesSection(
                            unnamedContacts = unnamedContacts,
                            malformedNames = malformedNames,
                            unnormalizedNumbers = unnormalizedNumbers,
                            emptyContacts = emptyContacts,
                            onRenameContact = { contact ->
                                renamingContact = contact
                                renameInput = contact.displayName
                            },
                            onNormalizeNumber = { contactId, raw, e164 ->
                                viewModel.normalizeNumber(contactId, raw, e164)
                            },
                            onNormalizeAll = { viewModel.normalizeAllNumbers() },
                            onDeleteEmpty = { viewModel.deleteEmptyContact(it.id) },
                            onDeleteAllEmpty = { viewModel.deleteAllEmptyContacts() }
                        )
                    }
                    FixAndManageSection.MERGE_HISTORY -> {
                        MergeHistorySection(
                            historyList = mergeHistory,
                            onUnlink = { viewModel.unlinkMerge(it.id) }
                        )
                    }
                }
            }
        }
    }

    // Rename Dialog
    renamingContact?.let { contact ->
        AlertDialog(
            onDismissRequest = { renamingContact = null },
            title = {
                Text(
                    text = if (contact.displayName.isBlank()) "Set Contact Name" else "Fix Malformed Name",
                    style = LineaTypography.titleMedium,
                    color = LineaColors.TextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Phone Number: ${contact.normalizedNumbers.firstOrNull() ?: contact.numbers.firstOrNull()?.number ?: ""}",
                        style = LineaTypography.bodySmall,
                        color = LineaColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = renameInput,
                        onValueChange = { renameInput = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LineaColors.TitaniumBlue,
                            unfocusedBorderColor = LineaColors.GlassBorder,
                            focusedTextColor = LineaColors.TextPrimary,
                            unfocusedTextColor = LineaColors.TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameContact(contact.id, renameInput)
                        renamingContact = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LineaColors.TitaniumBlue)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingContact = null }) {
                    Text("Cancel", color = LineaColors.TextSecondary)
                }
            },
            containerColor = LineaColors.NeuSurfaceRaised
        )
    }
}

// =========================================================================
// SECTION 1: MERGE DUPLICATES
// =========================================================================

@Composable
private fun MergeDuplicatesSection(
    highGroups: List<MergeGroup>,
    mediumGroups: List<MergeGroup>,
    lowGroups: List<MergeGroup>,
    undoCountdown: Int,
    onMergeGroup: (MergeGroup) -> Unit,
    onDismissMedium: (MergeGroup) -> Unit,
    onDismissLow: (MergeGroup) -> Unit,
    onUndoMerge: () -> Unit
) {
    if (highGroups.isEmpty() && mediumGroups.isEmpty() && lowGroups.isEmpty()) {
        EmptySectionPlaceholder(
            title = "No Duplicates Found",
            message = "Every contact in your address book is unique across normalized phone numbers and names."
        )
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Summary row for Auto-Merged Contacts (HIGH confidence)
        if (highGroups.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neumorphic(
                            shape = RoundedCornerShape(16.dp),
                            elevation = 3.dp,
                            surfaceColor = LineaColors.NeuSurfaceRaised
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(LineaColors.MutedSageGreen.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = LineaColors.MutedSageGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${highGroups.size} Auto-Merge Candidate Group(s)",
                                        style = LineaTypography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = LineaColors.TextPrimary
                                    )
                                    Text(
                                        text = "Processed silently in WorkManager with exact match",
                                        style = LineaTypography.bodySmall,
                                        color = LineaColors.TextSecondary
                                    )
                                }
                            }

                            if (undoCountdown > 0) {
                                Button(
                                    onClick = onUndoMerge,
                                    colors = ButtonDefaults.buttonColors(containerColor = LineaColors.MutedSageGreen),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Undo", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // MEDIUM Confidence Suggested Merges (Side-by-Side Cards)
        if (mediumGroups.isNotEmpty()) {
            item {
                Text(
                    text = "Suggested Merges (${mediumGroups.size})",
                    style = LineaTypography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = LineaColors.TitaniumBlue
                )
                Text(
                    text = "Fuzzy name match with shared phone or close phonetic match (User Confirmation Required)",
                    style = LineaTypography.bodySmall,
                    color = LineaColors.TextSecondary
                )
            }

            items(mediumGroups, key = { it.id }) { group ->
                MediumConfidenceCardPair(
                    group = group,
                    onMerge = { onMergeGroup(group) },
                    onNotTheSame = { onDismissMedium(group) }
                )
            }
        }

        // LOW Confidence Review Flags
        if (lowGroups.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Review Only (${lowGroups.size})",
                    style = LineaTypography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = LineaColors.Warning
                )
                Text(
                    text = "Names fuzzy-match but share no phone and no email. Never auto-merged.",
                    style = LineaTypography.bodySmall,
                    color = LineaColors.TextSecondary
                )
            }

            items(lowGroups, key = { it.id }) { group ->
                LowConfidenceReviewCard(
                    group = group,
                    onMerge = { onMergeGroup(group) },
                    onDismiss = { onDismissLow(group) }
                )
            }
        }
    }
}

/**
 * Side-by-side comparison card for MEDIUM confidence merge groups.
 */
@Composable
private fun MediumConfidenceCardPair(
    group: MergeGroup,
    onMerge: () -> Unit,
    onNotTheSame: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .neumorphic(
                shape = RoundedCornerShape(18.dp),
                elevation = 3.dp,
                surfaceColor = LineaColors.NeuSurfaceRaised
            )
            .padding(16.dp)
    ) {
        Column {
            // Header Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(LineaColors.TitaniumBlue.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "MEDIUM CONFIDENCE",
                        style = LineaTypography.labelSmall,
                        color = LineaColors.TitaniumBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }

                Text(
                    text = "${group.contacts.size} contacts",
                    style = LineaTypography.labelSmall,
                    color = LineaColors.TextTertiary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Side-by-Side Contact Cards (or stacked if > 2)
            if (group.contacts.size == 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MiniContactProfileCard(
                        contact = group.contacts[0],
                        modifier = Modifier.weight(1f)
                    )
                    MiniContactProfileCard(
                        contact = group.contacts[1],
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    group.contacts.forEach { c ->
                        MiniContactProfileCard(contact = c, modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = LineaColors.GlassBorder.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))

            // Match Reason
            Text(
                text = group.explanation,
                style = LineaTypography.bodySmall,
                color = LineaColors.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: "Not the same" and "Merge"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onNotTheSame,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LineaColors.TextSecondary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LineaColors.GlassBorder)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Not the same", fontSize = 11.sp)
                }

                Button(
                    onClick = onMerge,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LineaColors.TitaniumBlue)
                ) {
                    Icon(Icons.Filled.Merge, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Merge", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun MiniContactProfileCard(
    contact: NormalizedContact,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(LineaColors.GlassFill)
            .border(LineaDimensions.HairlineBorder, LineaColors.GlassBorder, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ContactAvatar(
                    displayName = contact.displayName.ifBlank { "?" },
                    photoUri = contact.contact.photoUri,
                    size = 32.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = contact.displayName.ifBlank { "Unnamed" },
                        style = LineaTypography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = LineaColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (contact.preferredSimSlot != null) {
                        Text(
                            text = "SIM ${contact.preferredSimSlot!! + 1}",
                            style = LineaTypography.labelSmall,
                            color = LineaColors.TitaniumBlue,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            val num = contact.numbers.firstOrNull()?.number ?: contact.normalizedNumbers.firstOrNull() ?: "No number"
            Text(
                text = num,
                style = LineaTypography.bodySmall,
                color = LineaColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val email = contact.emails.firstOrNull()?.email
            if (!email.isNullOrBlank()) {
                Text(
                    text = email,
                    style = LineaTypography.labelSmall,
                    color = LineaColors.TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun LowConfidenceReviewCard(
    group: MergeGroup,
    onMerge: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .neumorphic(
                shape = RoundedCornerShape(16.dp),
                elevation = 2.dp,
                surfaceColor = LineaColors.NeuSurfaceRaised
            )
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(LineaColors.Warning.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "LOW CONFIDENCE · REVIEW ONLY",
                        style = LineaTypography.labelSmall,
                        color = LineaColors.Warning,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LineaColors.TextSecondary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LineaColors.GlassBorder),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Dismiss", fontSize = 11.sp)
                    }

                    Button(
                        onClick = onMerge,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LineaColors.TitaniumBlue),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Merge", fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val names = group.contacts.joinToString(" vs ") { it.displayName }
            Text(
                text = names,
                style = LineaTypography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = LineaColors.TextPrimary
            )
            Text(
                text = group.explanation,
                style = LineaTypography.bodySmall,
                color = LineaColors.TextSecondary
            )
        }
    }
}

// =========================================================================
// SECTION 2: FIX ISSUES
// =========================================================================

@Composable
private fun FixIssuesSection(
    unnamedContacts: List<NormalizedContact>,
    malformedNames: List<NormalizedContact>,
    unnormalizedNumbers: List<FixIssue.UnnormalizedNumber>,
    emptyContacts: List<NormalizedContact>,
    onRenameContact: (NormalizedContact) -> Unit,
    onNormalizeNumber: (Long, String, String) -> Unit,
    onNormalizeAll: () -> Unit,
    onDeleteEmpty: (NormalizedContact) -> Unit,
    onDeleteAllEmpty: () -> Unit
) {
    val totalIssues = unnamedContacts.size + malformedNames.size + unnormalizedNumbers.size + emptyContacts.size
    if (totalIssues == 0) {
        EmptySectionPlaceholder(
            title = "All Contacts Clean!",
            message = "No unnamed contacts, malformed phone-names, unnormalized numbers, or empty records found."
        )
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Quick bulk actions row
        if (unnormalizedNumbers.size > 1 || emptyContacts.size > 1) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (unnormalizedNumbers.isNotEmpty()) {
                        Button(
                            onClick = onNormalizeAll,
                            colors = ButtonDefaults.buttonColors(containerColor = LineaColors.TitaniumBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Normalize All (${unnormalizedNumbers.size})", fontSize = 11.sp)
                        }
                    }
                    if (emptyContacts.isNotEmpty()) {
                        Button(
                            onClick = onDeleteAllEmpty,
                            colors = ButtonDefaults.buttonColors(containerColor = LineaColors.Danger.copy(alpha = 0.8f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Delete Empty (${emptyContacts.size})", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // 1. Unnamed Contacts
        if (unnamedContacts.isNotEmpty()) {
            item {
                Text(
                    text = "Unnamed Contacts (${unnamedContacts.size})",
                    style = LineaTypography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = LineaColors.TextPrimary
                )
            }
            items(unnamedContacts, key = { "unnamed_${it.id}" }) { contact ->
                FixCardRow(
                    title = contact.numbers.firstOrNull()?.number ?: "No Phone",
                    subtitle = "Has phone number but no display name",
                    actionLabel = "Set Name",
                    onAction = { onRenameContact(contact) }
                )
            }
        }

        // 2. Malformed Names
        if (malformedNames.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Malformed Names (${malformedNames.size})",
                    style = LineaTypography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = LineaColors.TextPrimary
                )
            }
            items(malformedNames, key = { "malformed_${it.id}" }) { contact ->
                FixCardRow(
                    title = contact.displayName,
                    subtitle = "Display name contains only phone digits",
                    actionLabel = "Rename",
                    onAction = { onRenameContact(contact) }
                )
            }
        }

        // 3. Unnormalized Numbers
        if (unnormalizedNumbers.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Unnormalized Numbers (${unnormalizedNumbers.size})",
                    style = LineaTypography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = LineaColors.TextPrimary
                )
            }
            items(unnormalizedNumbers, key = { "unnorm_${it.contact.id}_${it.rawNumber}" }) { issue ->
                FixCardRow(
                    title = issue.contact.displayName.ifBlank { "Unnamed" },
                    subtitle = "${issue.rawNumber} → ${issue.proposedE164}",
                    actionLabel = "Normalize",
                    onAction = { onNormalizeNumber(issue.contact.id, issue.rawNumber, issue.proposedE164) }
                )
            }
        }

        // 4. Empty Contacts Flagged for Deletion
        if (emptyContacts.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Empty Contacts Flagged for Deletion (${emptyContacts.size})",
                    style = LineaTypography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = LineaColors.Danger
                )
            }
            items(emptyContacts, key = { "empty_${it.id}" }) { contact ->
                FixCardRow(
                    title = contact.displayName.ifBlank { "Blank Contact Record" },
                    subtitle = "Contains no phone numbers, emails, address or notes",
                    actionLabel = "Delete",
                    actionColor = LineaColors.Danger,
                    onAction = { onDeleteEmpty(contact) }
                )
            }
        }
    }
}

@Composable
private fun FixCardRow(
    title: String,
    subtitle: String,
    actionLabel: String,
    actionColor: Color = LineaColors.TitaniumBlue,
    onAction: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .neumorphic(
                shape = RoundedCornerShape(14.dp),
                elevation = 2.dp,
                surfaceColor = LineaColors.NeuSurfaceRaised
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = LineaTypography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = LineaColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = LineaTypography.bodySmall,
                    color = LineaColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Button(
                onClick = onAction,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = actionColor),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(actionLabel, fontSize = 11.sp)
            }
        }
    }
}

// =========================================================================
// SECTION 3: MERGE HISTORY
// =========================================================================

@Composable
private fun MergeHistorySection(
    historyList: List<MergeHistoryEntity>,
    onUnlink: (MergeHistoryEntity) -> Unit
) {
    if (historyList.isEmpty()) {
        EmptySectionPlaceholder(
            title = "No Merge History",
            message = "Every contact merge operation will be safely logged here with full field data, allowing instant unlinking."
        )
        return
    }

    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Text(
                text = "Past Merges (${historyList.size})",
                style = LineaTypography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = LineaColors.TextPrimary
            )
            Text(
                text = "Unlink will delete the merged contact and restore all original source contacts exactly as they were.",
                style = LineaTypography.bodySmall,
                color = LineaColors.TextSecondary
            )
        }

        items(historyList, key = { it.id }) { item ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphic(
                        shape = RoundedCornerShape(16.dp),
                        elevation = 2.dp,
                        surfaceColor = LineaColors.NeuSurfaceRaised
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = null,
                                tint = LineaColors.TitaniumBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = dateFormat.format(Date(item.timestamp)),
                                style = LineaTypography.labelSmall,
                                color = LineaColors.TextTertiary
                            )
                        }

                        Button(
                            onClick = { onUnlink(item) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LineaColors.Warning),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Unlink", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Merged into '${item.mergedContactName}'",
                        style = LineaTypography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = LineaColors.TextPrimary
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Sources (${item.sourceContactsCount}): ${item.sourceContactNames}",
                        style = LineaTypography.bodySmall,
                        color = LineaColors.TextSecondary
                    )
                }
            }
        }
    }
}

// =========================================================================
// HELPER COMPONENTS
// =========================================================================

@Composable
private fun SectionChip(
    title: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) LineaColors.TitaniumBlue else LineaColors.NeuSurfaceRaised
    val contentColor = if (isSelected) Color.White else LineaColors.TextPrimary

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(
                LineaDimensions.HairlineBorder,
                if (isSelected) LineaColors.TitaniumBlue else LineaColors.NeuBorderHighlight.copy(alpha = 0.5f),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = LineaTypography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                fontSize = 11.sp,
                maxLines = 1
            )
            if (count > 0) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color.White.copy(alpha = 0.25f) else LineaColors.TitaniumBlue.copy(alpha = 0.15f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = count.toString(),
                        style = LineaTypography.labelSmall,
                        fontSize = 9.sp,
                        color = if (isSelected) Color.White else LineaColors.TitaniumBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptySectionPlaceholder(title: String, message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 60.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = LineaColors.MutedSageGreen,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = LineaTypography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LineaColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                style = LineaTypography.bodyMedium,
                color = LineaColors.TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

