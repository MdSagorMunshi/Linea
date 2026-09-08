package com.ryanshelby.linea.ui.screens.contacts

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.ryanshelby.linea.ui.components.ContactAvatar
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
import com.ryanshelby.linea.data.local.entities.ContactEntity
import com.ryanshelby.linea.ui.components.FrostedGlassBox
import com.ryanshelby.linea.ui.components.PrivatePinDialog
import com.ryanshelby.linea.ui.screens.dialpad.CallCountdownDialog
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography
import kotlinx.coroutines.launch

@Composable
fun ContactsScreen(
    modifier: Modifier = Modifier,
    onContactClick: ((Long) -> Unit)? = null,
    viewModel: ContactsViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeLetter by viewModel.activeLetter.collectAsState()
    val pinnedFavorites by viewModel.pinnedFavorites.collectAsState()
    val groupedContacts by viewModel.groupedContacts.collectAsState()
    val selectedContactForDetail by viewModel.selectedContactForDetail.collectAsState()
    val numbersForSelected by viewModel.numbersForSelectedContact.collectAsState()
    val isCreateSheetOpen by viewModel.isCreateSheetOpen.collectAsState()
    val contactToEdit by viewModel.contactToEdit.collectAsState()
    val availableAccounts by viewModel.availableAccounts.collectAsState()
    val selectedContactAccount by viewModel.selectedContactAccount.collectAsState()
    val preCallNoteForSelected by viewModel.preCallNoteForSelected.collectAsState()
    val callCountdownSeconds by viewModel.callCountdownSeconds.collectAsState()
    val callConfirmationEnabled by viewModel.callConfirmationEnabled.collectAsState()
    val isPrivateModeUnlocked by viewModel.isPrivateModeUnlocked.collectAsState()
    val privatePin by viewModel.privatePin.collectAsState()
    val isPinDialogOpen by viewModel.isPinDialogOpen.collectAsState()

    var pendingCallNumber by remember { mutableStateOf<String?>(null) }
    var pendingSimSlot by remember { mutableStateOf<Int?>(null) }
    var showCountdownDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = LineaDimensions.ScreenPadding, end = LineaDimensions.ScreenPadding)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Title Header with Private Mode Lock Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Contacts",
                        style = LineaTypography.titleLarge,
                        color = LineaColors.TextPrimary
                    )
                    Text(
                        text = if (isPrivateModeUnlocked) "Unlocked: Showing private contacts" else "Address book with SIM affinity",
                        style = LineaTypography.bodySmall,
                        color = if (isPrivateModeUnlocked) LineaColors.MutedSageGreen else LineaColors.TitaniumBlue
                    )
                }

                IconButton(
                    onClick = {
                        if (isPrivateModeUnlocked) {
                            viewModel.lockPrivateMode()
                        } else {
                            viewModel.openPinDialog()
                        }
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isPrivateModeUnlocked) LineaColors.MutedSageGreen.copy(alpha = 0.15f) else LineaColors.GlassFill)
                        .border(
                            LineaDimensions.HairlineBorder,
                            if (isPrivateModeUnlocked) LineaColors.MutedSageGreen.copy(alpha = 0.4f) else LineaColors.GlassBorder,
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isPrivateModeUnlocked) Icons.Filled.LockOpen else Icons.Filled.Lock,
                        contentDescription = "Private Contacts Lock",
                        tint = if (isPrivateModeUnlocked) LineaColors.MutedSageGreen else LineaColors.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar (with text & T9 number filtering)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = {
                    Text(
                        text = "Search contacts or dial T9...",
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
                    focusedContainerColor = LineaColors.GlassFill,
                    unfocusedContainerColor = LineaColors.GlassFill,
                    focusedBorderColor = LineaColors.TitaniumBlue,
                    unfocusedBorderColor = LineaColors.GlassBorder,
                    focusedTextColor = LineaColors.TextPrimary,
                    unfocusedTextColor = LineaColors.TextPrimary
                )
            )

            // Pinned / Favorite Contacts Section
            if (pinnedFavorites.isNotEmpty() && searchQuery.isEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Favorites",
                    style = LineaTypography.labelSmall,
                    color = LineaColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(pinnedFavorites, key = { "fav_${it.id}" }) { contact ->
                        PinnedContactCard(
                            contact = contact,
                            onClick = { onContactClick?.invoke(contact.id) ?: viewModel.selectContactForDetail(contact) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (groupedContacts.isEmpty()) {
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
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = LineaColors.TitaniumBlue.copy(alpha = 0.6f),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No matching contacts" else "No contacts yet",
                                style = LineaTypography.titleMedium,
                                color = LineaColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "Check spelling or search by phone number." else "Contacts synced from your device and added in LINEA will appear here.",
                                style = LineaTypography.bodySmall,
                                color = LineaColors.TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // Alphabetical Contact List with Scrubber
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Contact List
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        val sortedLetters = groupedContacts.keys.sorted()

                        sortedLetters.forEach { letter ->
                            val contactsInLetter = groupedContacts[letter] ?: emptyList()

                            // Section Header
                            item(key = "letter_$letter") {
                                Text(
                                    text = letter.toString(),
                                    style = LineaTypography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = LineaColors.TitaniumBlue,
                                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp, start = 4.dp)
                                )
                            }

                            items(contactsInLetter, key = { it.id }) { contact ->
                                ContactCardRow(
                                    contact = contact,
                                    onClick = { onContactClick?.invoke(contact.id) ?: viewModel.selectContactForDetail(contact) }
                                )
                            }
                        }
                    }

                    // Vertical A-Z Scrubber
                    AlphabeticalScrubber(
                        activeLetter = activeLetter,
                        onLetterSelected = { letter ->
                            viewModel.onScrubberLetter(letter)
                            scope.launch {
                                // Find index of section header in LazyColumn
                                val sortedLetters = groupedContacts.keys.sorted()
                                val targetIndex = sortedLetters.indexOf(letter)
                                if (targetIndex >= 0) {
                                    // Calculate position offset
                                    var cumulative = 0
                                    for (i in 0 until targetIndex) {
                                        val l = sortedLetters[i]
                                        cumulative += 1 + (groupedContacts[l]?.size ?: 0)
                                    }
                                    listState.scrollToItem(cumulative)
                                }
                            }
                        }
                    )
                }
            }
        }

        // Floating Action Button ("+" Add Contact)
        FloatingActionButton(
            onClick = { viewModel.openCreateSheet() },
            containerColor = LineaColors.TitaniumBlue,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 106.dp)
                .size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add Contact",
                modifier = Modifier.size(28.dp)
            )
        }
    }

    // Contact Detail Bottom Sheet
    val selected = selectedContactForDetail
    if (selected != null) {
        ContactDetailSheet(
            contact = selected,
            numbers = numbersForSelected,
            emails = emptyList(),
            onDismiss = { viewModel.selectContactForDetail(null) },
            onCallNumber = { num, simSlot ->
                if (callConfirmationEnabled && callCountdownSeconds > 0) {
                    pendingCallNumber = num
                    pendingSimSlot = simSlot
                    showCountdownDialog = true
                } else {
                    viewModel.callContact(num, simSlot)
                }
            },
            onToggleFavorite = { viewModel.toggleFavorite(it) },
            onToggleRuleOverride = { contact, override -> viewModel.toggleRuleOverride(contact, override) },
            onEditContact = { contact -> viewModel.openEditSheet(contact) },
            onDeleteContact = { contact -> viewModel.deleteContact(contact) },
            preCallNote = preCallNoteForSelected,
            onSetPreCallNote = { noteText ->
                val primaryNum = numbersForSelected.firstOrNull()?.number ?: ""
                viewModel.setPreCallNote(selected.id, primaryNum, noteText)
            },
            onClearPreCallNote = {
                val primaryNum = numbersForSelected.firstOrNull()?.number ?: ""
                viewModel.clearPreCallNote(selected.id, primaryNum)
            }
        )
    }

    // Call Countdown Dialog
    if (showCountdownDialog && pendingCallNumber != null) {
        CallCountdownDialog(
            phoneNumber = pendingCallNumber!!,
            totalSeconds = callCountdownSeconds,
            onConfirmCall = {
                showCountdownDialog = false
                viewModel.callContact(pendingCallNumber!!, pendingSimSlot)
                pendingCallNumber = null
            },
            onCancel = {
                showCountdownDialog = false
                pendingCallNumber = null
            }
        )
    }

    // Contact Create / Edit Bottom Sheet
    if (isCreateSheetOpen) {
        ContactCreateEditSheet(
            contactToEdit = contactToEdit,
            availableAccounts = availableAccounts,
            selectedAccount = selectedContactAccount,
            onSelectAccount = { viewModel.selectContactAccount(it) },
            onDismiss = { viewModel.dismissCreateOrEditSheet() },
            onSave = { displayName, company, numbers, emails, preferredSimSlot, notes, photoUri, photoBytes ->
                viewModel.saveContact(displayName, company, numbers, emails, preferredSimSlot, notes, photoUri, photoBytes)
            }
        )
    }

    // Private Contacts Unlock PIN Dialog
    if (isPinDialogOpen) {
        PrivatePinDialog(
            correctPin = privatePin,
            onSuccess = { viewModel.unlockPrivateMode() },
            onDismiss = { viewModel.dismissPinDialog() }
        )
    }
}

@Composable
private fun PinnedContactCard(
    contact: ContactEntity,
    onClick: () -> Unit
) {
    FrostedGlassBox(
        shape = RoundedCornerShape(16.dp),
        borderColor = LineaColors.GlassBorder,
        modifier = Modifier
            .width(100.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ContactAvatar(
                photoUri = contact.photoUri,
                displayName = contact.displayName,
                size = 42.dp,
                borderColor = LineaColors.TitaniumBlue.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = contact.displayName,
                style = LineaTypography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = LineaColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (contact.preferredSimSlot != null) {
                Text(
                    text = "SIM ${contact.preferredSimSlot + 1}",
                    style = LineaTypography.labelSmall,
                    fontSize = 9.sp,
                    color = LineaColors.TitaniumBlue
                )
            }
        }
    }
}

@Composable
private fun ContactCardRow(
    contact: ContactEntity,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(LineaColors.GlassFill)
            .border(LineaDimensions.HairlineBorder, LineaColors.GlassBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Avatar
            ContactAvatar(
                photoUri = contact.photoUri,
                displayName = contact.displayName,
                size = 42.dp,
                borderColor = LineaColors.GlassBorder
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = contact.displayName,
                        style = LineaTypography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = LineaColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (contact.isFavorite) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Favorite",
                            tint = LineaColors.MutedRust,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                if (!contact.company.isNullOrBlank()) {
                    Text(
                        text = contact.company,
                        style = LineaTypography.bodySmall,
                        color = LineaColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // SIM Affinity indicator if present
        if (contact.preferredSimSlot != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(LineaColors.GlassFill)
                    .border(0.5.dp, LineaColors.GlassBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "SIM ${contact.preferredSimSlot + 1}",
                    style = LineaTypography.labelSmall,
                    fontSize = 10.sp,
                    color = LineaColors.TitaniumBlue
                )
            }
        }
    }
}
