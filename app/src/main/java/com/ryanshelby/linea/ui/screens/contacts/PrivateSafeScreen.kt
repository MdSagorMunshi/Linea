package com.ryanshelby.linea.ui.screens.contacts

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ryanshelby.linea.data.local.entities.ContactEntity
import com.ryanshelby.linea.data.repository.ContactAccount
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
    val availableAccounts by viewModel.availableAccounts.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showOptionsDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showAddExistingContactDialog by remember { mutableStateOf(false) }
    var showSecuritySpecsDialog by remember { mutableStateOf(false) }

    // Export & Import states
    var showExportDialog by remember { mutableStateOf(false) }
    var exportUseCustomPassword by remember { mutableStateOf(false) }
    var exportPassword by remember { mutableStateOf("") }
    var exportPasswordConfirm by remember { mutableStateOf("") }
    var exportPasswordVisible by remember { mutableStateOf(false) }
    var exportErrorMessage by remember { mutableStateOf<String?>(null) }
    var pendingExportKey by remember { mutableStateOf("") }

    var showImportDialog by remember { mutableStateOf(false) }
    var pendingImportBytes by remember { mutableStateOf<ByteArray?>(null) }
    var importUseCustomPassword by remember { mutableStateOf(false) }
    var importPassword by remember { mutableStateOf("") }
    var importPasswordVisible by remember { mutableStateOf(false) }
    var importErrorMessage by remember { mutableStateOf<String?>(null) }

    // Move contact to Google / Phone storage state
    var contactToMoveToPublic by remember { mutableStateOf<ContactEntity?>(null) }

    // Document creation launcher for .linea encrypted export
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val keyToUse = if (exportUseCustomPassword) {
                        pendingExportKey
                    } else {
                        vaultSecurityManager.getActiveSessionPin() ?: ""
                    }
                    val bytes = viewModel.exportPrivateSafe(keyToUse)
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(bytes)
                        os.flush()
                    }
                    Toast.makeText(
                        context,
                        "Exported ${privateContacts.size} private contacts to .linea file",
                        Toast.LENGTH_LONG
                    ).show()
                    showExportDialog = false
                    exportPassword = ""
                    exportPasswordConfirm = ""
                    exportErrorMessage = null
                } catch (e: Exception) {
                    Toast.makeText(context, "Export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Document open launcher for .linea encrypted import
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (bytes != null && bytes.isNotEmpty()) {
                        pendingImportBytes = bytes
                        importErrorMessage = null
                        importPassword = ""
                        importUseCustomPassword = false
                        showImportDialog = true
                    } else {
                        Toast.makeText(context, "Selected file is empty.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Could not open file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

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
                        text = "Local App Data • Quantum-Proof Encrypted",
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

            // Search Bar & Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text("Search safe contacts...", color = LineaColors.TextTertiary, fontSize = 14.sp)
                    },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = LineaColors.TextTertiary)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear", tint = LineaColors.TextSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = LineaColors.GlassFill,
                        unfocusedContainerColor = LineaColors.GlassFill,
                        focusedBorderColor = LineaColors.MutedSageGreen.copy(alpha = 0.6f),
                        unfocusedBorderColor = LineaColors.GlassBorder,
                        focusedTextColor = LineaColors.TextPrimary,
                        unfocusedTextColor = LineaColors.TextPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Move existing contact to safe button
                IconButton(
                    onClick = { showAddExistingContactDialog = true },
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(LineaColors.MutedSageGreen.copy(alpha = 0.18f))
                        .border(LineaDimensions.HairlineBorder, LineaColors.MutedSageGreen.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddModerator,
                        contentDescription = "Move Contact to Safe",
                        tint = LineaColors.MutedSageGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Private Contacts List / Empty State
            if (filteredContacts.isEmpty()) {
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
                                text = if (searchQuery.isNotEmpty())
                                    "Check your search spelling."
                                else
                                    "Contacts here are stored strictly inside local app data and encrypted with AES-256-GCM. When moved here, they are deleted from Google and Phone storage.",
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
                                        viewModel.placeCallFromPrivateSafe(
                                            number = primaryNumber,
                                            preferredSimSlot = contact.preferredSimSlot
                                        )
                                    }
                                }
                            },
                            onMoveToPublicStorage = {
                                contactToMoveToPublic = contact
                            }
                        )
                    }
                }
            }
        }

        // Floating Action Button ("+" Create New Private Contact directly inside safe)
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

                    // Option 2: Export Private Safe (.linea)
                    FrostedGlassBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showOptionsDialog = false
                                exportPassword = ""
                                exportPasswordConfirm = ""
                                exportErrorMessage = null
                                exportUseCustomPassword = false
                                showExportDialog = true
                            },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, tint = LineaColors.MutedSageGreen)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Export Private Safe (.linea)", style = LineaTypography.titleSmall, color = LineaColors.TextPrimary)
                                Text("Encrypted backup with PIN or custom password", style = LineaTypography.bodySmall, color = LineaColors.TextSecondary)
                            }
                        }
                    }

                    // Option 3: Import Private Safe (.linea)
                    FrostedGlassBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showOptionsDialog = false
                                importFileLauncher.launch(arrayOf("*/*", "application/octet-stream"))
                            },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Upload, contentDescription = null, tint = LineaColors.TitaniumBlue)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Import Private Safe (.linea)", style = LineaTypography.titleSmall, color = LineaColors.TextPrimary)
                                Text("Restore contacts from encrypted .linea file", style = LineaTypography.bodySmall, color = LineaColors.TextSecondary)
                            }
                        }
                    }

                    // Option 4: Security & Encryption Specs
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

                    // Option 5: Lock Safe Immediately
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

    // Export Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            containerColor = LineaColors.BackgroundElevated,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Download, contentDescription = null, tint = LineaColors.MutedSageGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Private Safe (.linea)", color = LineaColors.TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Export ${privateContacts.size} private contacts into a post-quantum AES-256-GCM encrypted .linea backup.",
                        style = LineaTypography.bodySmall,
                        color = LineaColors.TextSecondary
                    )

                    // Radio 1: Default 6-Digit PIN
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { exportUseCustomPassword = false }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !exportUseCustomPassword,
                            onClick = { exportUseCustomPassword = false },
                            colors = RadioButtonDefaults.colors(selectedColor = LineaColors.MutedSageGreen)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Use Current 6-Digit PIN (Default)", color = LineaColors.TextPrimary, style = LineaTypography.bodyMedium)
                            Text("Encrypted using your active vault PIN", color = LineaColors.TextSecondary, style = LineaTypography.bodySmall)
                        }
                    }

                    // Radio 2: Custom Password
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { exportUseCustomPassword = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = exportUseCustomPassword,
                            onClick = { exportUseCustomPassword = true },
                            colors = RadioButtonDefaults.colors(selectedColor = LineaColors.MutedSageGreen)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Create Custom Password", color = LineaColors.TextPrimary, style = LineaTypography.bodyMedium)
                            Text("Set a unique password for this export file", color = LineaColors.TextSecondary, style = LineaTypography.bodySmall)
                        }
                    }

                    if (exportUseCustomPassword) {
                        OutlinedTextField(
                            value = exportPassword,
                            onValueChange = {
                                exportPassword = it
                                exportErrorMessage = null
                            },
                            label = { Text("Custom Password") },
                            singleLine = true,
                            visualTransformation = if (exportPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { exportPasswordVisible = !exportPasswordVisible }) {
                                    Icon(
                                        imageVector = if (exportPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = null,
                                        tint = LineaColors.TextSecondary
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = exportPasswordConfirm,
                            onValueChange = {
                                exportPasswordConfirm = it
                                exportErrorMessage = null
                            },
                            label = { Text("Confirm Custom Password") },
                            singleLine = true,
                            visualTransformation = if (exportPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    if (exportErrorMessage != null) {
                        Text(
                            text = exportErrorMessage!!,
                            color = LineaColors.Danger,
                            style = LineaTypography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (exportUseCustomPassword) {
                            if (exportPassword.length < 4) {
                                exportErrorMessage = "Password must be at least 4 characters."
                                return@Button
                            }
                            if (exportPassword != exportPasswordConfirm) {
                                exportErrorMessage = "Passwords do not match."
                                return@Button
                            }
                            pendingExportKey = exportPassword
                        } else {
                            pendingExportKey = vaultSecurityManager.getActiveSessionPin() ?: ""
                            if (pendingExportKey.isBlank()) {
                                exportErrorMessage = "Vault session PIN unavailable. Please re-enter PIN."
                                return@Button
                            }
                        }
                        exportLauncher.launch("linea_vault_${System.currentTimeMillis()}.linea")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LineaColors.MutedSageGreen)
                ) {
                    Text("Select Destination & Export", color = Color.Black, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cancel", color = LineaColors.TextSecondary)
                }
            }
        )
    }

    // Import Dialog
    if (showImportDialog && pendingImportBytes != null) {
        AlertDialog(
            onDismissRequest = {
                showImportDialog = false
                pendingImportBytes = null
            },
            containerColor = LineaColors.BackgroundElevated,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Upload, contentDescription = null, tint = LineaColors.TitaniumBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Decrypt & Import .linea Safe", color = LineaColors.TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Choose the decryption key that was used when exporting this .linea file.",
                        style = LineaTypography.bodySmall,
                        color = LineaColors.TextSecondary
                    )

                    // Radio 1: Current 6-Digit PIN
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { importUseCustomPassword = false }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !importUseCustomPassword,
                            onClick = { importUseCustomPassword = false },
                            colors = RadioButtonDefaults.colors(selectedColor = LineaColors.TitaniumBlue)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Current 6-Digit PIN", color = LineaColors.TextPrimary, style = LineaTypography.bodyMedium)
                    }

                    // Radio 2: Custom Password
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { importUseCustomPassword = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = importUseCustomPassword,
                            onClick = { importUseCustomPassword = true },
                            colors = RadioButtonDefaults.colors(selectedColor = LineaColors.TitaniumBlue)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Custom Password", color = LineaColors.TextPrimary, style = LineaTypography.bodyMedium)
                    }

                    if (importUseCustomPassword) {
                        OutlinedTextField(
                            value = importPassword,
                            onValueChange = {
                                importPassword = it
                                importErrorMessage = null
                            },
                            label = { Text("Enter Custom Password") },
                            singleLine = true,
                            visualTransformation = if (importPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { importPasswordVisible = !importPasswordVisible }) {
                                    Icon(
                                        imageVector = if (importPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = null,
                                        tint = LineaColors.TextSecondary
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    if (importErrorMessage != null) {
                        Text(
                            text = importErrorMessage!!,
                            color = LineaColors.Danger,
                            style = LineaTypography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val keyToUse = if (importUseCustomPassword) {
                                importPassword
                            } else {
                                vaultSecurityManager.getActiveSessionPin() ?: ""
                            }
                            if (keyToUse.isBlank()) {
                                importErrorMessage = "Please provide the decryption PIN or password."
                                return@launch
                            }
                            val result = viewModel.importPrivateSafe(pendingImportBytes!!, keyToUse)
                            if (result.isSuccess) {
                                val count = result.getOrThrow()
                                Toast.makeText(context, "Successfully imported $count private contacts!", Toast.LENGTH_LONG).show()
                                showImportDialog = false
                                pendingImportBytes = null
                            } else {
                                importErrorMessage = result.exceptionOrNull()?.message ?: "Decryption failed. Incorrect key."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LineaColors.TitaniumBlue)
                ) {
                    Text("Decrypt & Import", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportDialog = false
                    pendingImportBytes = null
                }) {
                    Text("Cancel", color = LineaColors.TextSecondary)
                }
            }
        )
    }

    // Move to Google / Phone Storage Dialog
    val contactToMove = contactToMoveToPublic
    if (contactToMove != null) {
        AlertDialog(
            onDismissRequest = { contactToMoveToPublic = null },
            containerColor = LineaColors.BackgroundElevated,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = LineaColors.TitaniumBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Move to Google / Phone Storage", color = LineaColors.TextPrimary)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Move \"${contactToMove.displayName}\" out of Private Safe into public storage? Select destination account:",
                        style = LineaTypography.bodySmall,
                        color = LineaColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableAccounts) { acc ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.moveContactToPublicStorage(
                                            contactId = contactToMove.id,
                                            accountName = if (!acc.isDevice) acc.name else null,
                                            accountType = acc.type
                                        )
                                        Toast.makeText(
                                            context,
                                            "Moved ${contactToMove.displayName} to ${acc.name}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        contactToMoveToPublic = null
                                    }
                                    .background(LineaColors.GlassFill)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (acc.isDevice) LineaColors.GlassBorder.copy(alpha = 0.3f)
                                            else LineaColors.TitaniumBlue.copy(alpha = 0.2f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (acc.isDevice) Icons.Filled.PhoneAndroid else Icons.Filled.AccountCircle,
                                        contentDescription = null,
                                        tint = if (acc.isDevice) LineaColors.TextSecondary else LineaColors.TitaniumBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(acc.name, style = LineaTypography.bodyMedium, color = LineaColors.TextPrimary)
                                    Text(
                                        if (acc.isDevice) "Device Storage" else "Google Account",
                                        style = LineaTypography.bodySmall,
                                        color = LineaColors.TextSecondary
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Filled.ArrowForwardIos,
                                    contentDescription = null,
                                    tint = LineaColors.TextTertiary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { contactToMoveToPublic = null }) {
                    Text("Cancel", color = LineaColors.TextSecondary)
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
                        text = "• Local Storage Only: Private Safe contacts are saved only in local app data and deleted from Google/Phone contacts.",
                        style = LineaTypography.bodySmall,
                        color = LineaColors.MutedSageGreen
                    )
                    Text(
                        text = "• Encrypted Backups: .linea files are protected with PBKDF2-HMAC-SHA512 + AES-256-GCM.",
                        style = LineaTypography.bodySmall,
                        color = LineaColors.TitaniumBlue
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSecuritySpecsDialog = false }) {
                    Text("Close", color = LineaColors.TitaniumBlue)
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
                    Text(
                        text = "Select a contact to move into Private Safe. It will be removed from Google and device contacts and stored encrypted in app data.",
                        style = LineaTypography.bodySmall,
                        color = LineaColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

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
                                            viewModel.moveContactToPrivateSafe(contact.id)
                                            Toast.makeText(
                                                context,
                                                "Moved ${contact.displayName} to Private Safe",
                                                Toast.LENGTH_SHORT
                                            ).show()
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
    onMoveToPublicStorage: () -> Unit
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

            // Move to Google / Phone Storage Button
            IconButton(
                onClick = onMoveToPublicStorage,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(LineaColors.GlassFill)
            ) {
                Icon(
                    imageVector = Icons.Filled.CloudUpload,
                    contentDescription = "Move to Google / Phone Storage",
                    tint = LineaColors.TitaniumBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
