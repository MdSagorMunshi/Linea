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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ryanshelby.linea.data.local.entities.ContactEntity
import com.ryanshelby.linea.security.PrivateVaultSecurityManager
import com.ryanshelby.linea.ui.components.ContactAvatar
import com.ryanshelby.linea.ui.components.FrostedGlassBox
import com.ryanshelby.linea.ui.components.PrivatePinDialog
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateSafeScreen(
    onNavigateBack: () -> Unit,
    onContactClick: (Long) -> Unit,
    viewModel: ContactsViewModel = hiltViewModel(),
    vaultSecurityManager: PrivateVaultSecurityManager
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val privateContacts by viewModel.privateContacts.collectAsState()
    val allRegularContacts by viewModel.regularContacts.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showOptionsDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showAddExistingContactDialog by remember { mutableStateOf(false) }
    var showSecuritySpecsDialog by remember { mutableStateOf(false) }

    val filteredContacts = remember(privateContacts, searchQuery) {
        if (searchQuery.isBlank()) {
            privateContacts
        } else {
            val q = searchQuery.trim().lowercase()
            privateContacts.filter {
                it.displayName.lowercase().contains(q) || (it.company?.lowercase()?.contains(q) == true)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LineaColors.BackgroundGradient)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = LineaDimensions.ScreenPadding)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Top App Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(42.dp)
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

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Private Safe",
                            style = LineaTypography.titleLarge,
                            color = LineaColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            tint = LineaColors.MutedSageGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "AES-256-GCM • Post-Quantum Secure",
                        style = LineaTypography.labelSmall,
                        color = LineaColors.MutedSageGreen
                    )
                }

                // Options (Gear / Settings) Button
                IconButton(
                    onClick = { showOptionsDialog = true },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(LineaColors.GlassFill)
                        .border(LineaDimensions.HairlineBorder, LineaColors.GlassBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Safe Options",
                        tint = LineaColors.TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Quick Lock Button
                IconButton(
                    onClick = {
                        viewModel.lockPrivateMode()
                        onNavigateBack()
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(LineaColors.Danger.copy(alpha = 0.15f))
                        .border(LineaDimensions.HairlineBorder, LineaColors.Danger.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Lock Safe",
                        tint = LineaColors.Danger,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Post-Quantum Security Card Banner
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(LineaColors.MutedSageGreen.copy(alpha = 0.2f))
                            .border(1.dp, LineaColors.MutedSageGreen.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            tint = LineaColors.MutedSageGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Quantum-Resistant Vault Active",
                            style = LineaTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = LineaColors.TextPrimary
                        )
                        Text(
                            text = "Protected with 256-bit AES-GCM and PBKDF2-SHA512. Hidden from call logs and normal contacts.",
                            style = LineaTypography.bodySmall.copy(fontSize = 11.sp),
                            color = LineaColors.TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Search private safe...",
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
                        IconButton(onClick = { searchQuery = "" }) {
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
                    focusedBorderColor = LineaColors.MutedSageGreen,
                    unfocusedBorderColor = LineaColors.GlassBorder,
                    focusedTextColor = LineaColors.TextPrimary,
                    unfocusedTextColor = LineaColors.TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Contact Count & Actions Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredContacts.size} PRIVATE CONTACT${if (filteredContacts.size == 1) "" else "S"}",
                    style = LineaTypography.labelSmall,
                    color = LineaColors.TextSecondary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { showAddExistingContactDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PersonAdd,
                            contentDescription = null,
                            tint = LineaColors.TitaniumBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add Existing",
                            style = LineaTypography.labelSmall,
                            color = LineaColors.TitaniumBlue
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredContacts.isEmpty()) {
                // Empty State
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    FrostedGlassBox(
                        modifier = Modifier.fillMaxWidth(0.92f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = null,
                                tint = LineaColors.MutedSageGreen.copy(alpha = 0.7f),
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No Matching Private Contacts" else "Private Safe is Empty",
                                style = LineaTypography.titleMedium,
                                color = LineaColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "Check your search spelling." else "Contacts moved here are encrypted and hidden from normal address book, dialpad search, and call logs.",
                                style = LineaTypography.bodySmall,
                                color = LineaColors.TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = { showAddExistingContactDialog = true },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = LineaColors.MutedSageGreen)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Move Contacts to Safe", color = Color.Black, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            } else {
                // Private Contacts List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(filteredContacts, key = { it.id }) { contact ->
                        PrivateContactRow(
                            contact = contact,
                            onClick = { onContactClick(contact.id) },
                            onCall = {
                                coroutineScope.launch {
                                    val numbers = viewModel.getNumbersForContactDirect(contact.id)
                                    val primaryNumber = numbers.firstOrNull()?.number
                                    if (!primaryNumber.isNullOrBlank()) {
                                        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$primaryNumber")).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
                                    }
                                }
                            },
                            onRemoveFromSafe = {
                                viewModel.setContactPrivate(contact.id, false)
                            }
                        )
                    }
                }
            }
        }

        // Floating Action Button ("+" Create New Private Contact)
        FloatingActionButton(
            onClick = {
                viewModel.openCreateSheet(isPrivate = true)
            },
            containerColor = LineaColors.MutedSageGreen,
            contentColor = Color.Black,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 28.dp)
                .size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add New Private Contact",
                modifier = Modifier.size(28.dp)
            )
        }
    }

    // Safe Options Dialog
    if (showOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showOptionsDialog = false },
            containerColor = LineaColors.BackgroundElevated,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = null,
                        tint = LineaColors.MutedSageGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Private Safe Options", color = LineaColors.TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Option 1: Change 6-Digit PIN
                    FrostedGlassBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showOptionsDialog = false
                                showChangePinDialog = true
                            },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Password, contentDescription = null, tint = LineaColors.TitaniumBlue)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Change 6-Digit PIN", style = LineaTypography.titleSmall, color = LineaColors.TextPrimary)
                                Text("Update master vault access code", style = LineaTypography.bodySmall, color = LineaColors.TextSecondary)
                            }
                        }
                    }

                    // Option 2: Security & Encryption Specs
                    FrostedGlassBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showOptionsDialog = false
                                showSecuritySpecsDialog = true
                            },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = LineaColors.MutedSageGreen)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Security Specifications", style = LineaTypography.titleSmall, color = LineaColors.TextPrimary)
                                Text("AES-256-GCM, PBKDF2 & Post-Quantum", style = LineaTypography.bodySmall, color = LineaColors.TextSecondary)
                            }
                        }
                    }

                    // Option 3: Lock Vault Now
                    FrostedGlassBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showOptionsDialog = false
                                viewModel.lockPrivateMode()
                                onNavigateBack()
                            },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = LineaColors.Danger)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Lock Safe Immediately", style = LineaTypography.titleSmall, color = LineaColors.Danger)
                                Text("Wipes master key from RAM & exits", style = LineaTypography.bodySmall, color = LineaColors.TextSecondary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOptionsDialog = false }) {
                    Text("Close", color = LineaColors.TitaniumBlue)
                }
            }
        )
    }

    // Change PIN Dialog
    if (showChangePinDialog) {
        PrivatePinDialog(
            vaultSecurityManager = vaultSecurityManager,
            isChangePinMode = true,
            onSuccess = {
                showChangePinDialog = false
            },
            onDismiss = {
                showChangePinDialog = false
            }
        )
    }

    // Security Specs Dialog
    if (showSecuritySpecsDialog) {
        AlertDialog(
            onDismissRequest = { showSecuritySpecsDialog = false },
            containerColor = LineaColors.BackgroundElevated,
            title = {
                Text("Quantum Cryptography Architecture", color = LineaColors.TextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "• Algorithm: AES-256-GCM (Galois/Counter Mode with 128-bit authentication tag).",
                        style = LineaTypography.bodySmall,
                        color = LineaColors.TextPrimary
                    )
                    Text(
                        text = "• Quantum Resistance: 256-bit symmetric security provides 128 bits of security against Grover's quantum search algorithm (NIST Level 5).",
                        style = LineaTypography.bodySmall,
                        color = LineaColors.TextSecondary
                    )
                    Text(
                        text = "• Key Derivation: PBKDF2WithHmacSHA512 with 100,000 rounds and 32-byte cryptographic salt.",
                        style = LineaTypography.bodySmall,
                        color = LineaColors.TextSecondary
                    )
                    Text(
                        text = "• Anti-Brute-Force: Progressive exponential lockout (30s, 2m, 10m) and constant-time equality comparisons.",
                        style = LineaTypography.bodySmall,
                        color = LineaColors.TextSecondary
                    )
                    Text(
                        text = "• Memory Protection: Decrypted keys wiped with zeros in memory when safe is locked or app is closed.",
                        style = LineaTypography.bodySmall,
                        color = LineaColors.MutedSageGreen
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSecuritySpecsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = LineaColors.TitaniumBlue)
                ) {
                    Text("Understood")
                }
            }
        )
    }

    // Add Existing Contact to Safe Dialog
    if (showAddExistingContactDialog) {
        var addSearch by remember { mutableStateOf("") }
        val nonPrivateFiltered = remember(allRegularContacts, addSearch) {
            if (addSearch.isBlank()) allRegularContacts
            else allRegularContacts.filter { it.displayName.lowercase().contains(addSearch.trim().lowercase()) }
        }

        AlertDialog(
            onDismissRequest = { showAddExistingContactDialog = false },
            containerColor = LineaColors.BackgroundElevated,
            title = {
                Text("Move Contact to Private Safe", color = LineaColors.TextPrimary)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    OutlinedTextField(
                        value = addSearch,
                        onValueChange = { addSearch = it },
                        placeholder = { Text("Search contact to move...", color = LineaColors.TextTertiary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = LineaColors.TextPrimary,
                            unfocusedTextColor = LineaColors.TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (nonPrivateFiltered.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No regular contacts found.", color = LineaColors.TextSecondary)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(nonPrivateFiltered, key = { it.id }) { contact ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            viewModel.setContactPrivate(contact.id, true)
                                            showAddExistingContactDialog = false
                                        }
                                        .background(LineaColors.GlassFill)
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ContactAvatar(
                                        displayName = contact.displayName,
                                        photoUri = contact.photoUri,
                                        size = 36.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = contact.displayName,
                                        style = LineaTypography.bodyMedium,
                                        color = LineaColors.TextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = "Protect",
                                        tint = LineaColors.MutedSageGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddExistingContactDialog = false }) {
                    Text("Cancel", color = LineaColors.TextSecondary)
                }
            }
        )
    }
}

@Composable
fun PrivateContactRow(
    contact: ContactEntity,
    onClick: () -> Unit,
    onCall: () -> Unit,
    onRemoveFromSafe: () -> Unit
) {
    FrostedGlassBox(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContactAvatar(
                displayName = contact.displayName,
                photoUri = contact.photoUri,
                size = 46.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = contact.displayName,
                        style = LineaTypography.titleMedium,
                        color = LineaColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(LineaColors.MutedSageGreen.copy(alpha = 0.2f))
                            .border(LineaDimensions.HairlineBorder, LineaColors.MutedSageGreen.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "SAFE",
                            style = LineaTypography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = LineaColors.MutedSageGreen
                        )
                    }
                }

                if (!contact.company.isNullOrBlank()) {
                    Text(
                        text = contact.company,
                        style = LineaTypography.bodySmall,
                        color = LineaColors.TextSecondary
                    )
                }
            }

            // Quick Call Button
            IconButton(
                onClick = onCall,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(LineaColors.TitaniumBlue.copy(alpha = 0.15f))
            ) {
                Icon(
                    imageVector = Icons.Filled.Call,
                    contentDescription = "Call",
                    tint = LineaColors.TitaniumBlue,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Remove from Safe Button
            IconButton(
                onClick = onRemoveFromSafe,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(LineaColors.GlassFill)
            ) {
                Icon(
                    imageVector = Icons.Filled.LockOpen,
                    contentDescription = "Remove from Safe",
                    tint = LineaColors.TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
