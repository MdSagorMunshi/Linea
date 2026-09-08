package com.ryanshelby.linea.ui.screens.contacts

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.ryanshelby.linea.ui.components.ContactAvatar
import com.ryanshelby.linea.util.ContactPhotoHelper
import kotlinx.coroutines.launch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.data.local.entities.ContactEntity
import com.ryanshelby.linea.data.repository.ContactAccount
import com.ryanshelby.linea.ui.components.FrostedGlassBox
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactCreateEditSheet(
    contactToEdit: ContactEntity? = null,
    initialNumbers: List<Pair<String, String>> = listOf("" to "Mobile"),
    availableAccounts: List<ContactAccount> = emptyList(),
    selectedAccount: ContactAccount? = null,
    onSelectAccount: (ContactAccount) -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (
        displayName: String,
        company: String?,
        numbers: List<Pair<String, String>>,
        emails: List<String>,
        preferredSimSlot: Int?,
        notes: String?,
        photoUri: String?,
        photoBytes: ByteArray?
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    var displayName by remember { mutableStateOf(contactToEdit?.displayName ?: "") }
    var company by remember { mutableStateOf(contactToEdit?.company ?: "") }
    var notes by remember { mutableStateOf(contactToEdit?.notes ?: "") }
    var preferredSimSlot by remember { mutableStateOf(contactToEdit?.preferredSimSlot) }
    var emailInput by remember { mutableStateOf("") }

    var currentPhotoUri by remember { mutableStateOf(contactToEdit?.photoUri) }
    var currentPhotoBytes by remember { mutableStateOf<ByteArray?>(null) }
    var showPhotoOptionsDialog by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            coroutineScope.launch {
                val (savedUri, bytes) = ContactPhotoHelper.saveUriToInternal(context, tempCameraUri!!)
                if (savedUri.isNotBlank()) {
                    currentPhotoUri = savedUri
                    currentPhotoBytes = bytes
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val (uri, _) = ContactPhotoHelper.createTempCameraUri(context)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val (savedUri, bytes) = ContactPhotoHelper.saveUriToInternal(context, uri)
                if (savedUri.isNotBlank()) {
                    currentPhotoUri = savedUri
                    currentPhotoBytes = bytes
                }
            }
        }
    }

    val numbersList = remember {
        mutableStateListOf<Pair<String, String>>().apply {
            if (initialNumbers.isNotEmpty()) {
                addAll(initialNumbers)
            } else {
                add("" to "Mobile")
            }
        }
    }

    if (showPhotoOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoOptionsDialog = false },
            title = {
                Text(
                    text = "Contact Photo",
                    style = LineaTypography.titleMedium,
                    color = LineaColors.TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Take photo with camera
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                showPhotoOptionsDialog = false
                                val hasCam = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasCam) {
                                    val (uri, _) = ContactPhotoHelper.createTempCameraUri(context)
                                    tempCameraUri = uri
                                    cameraLauncher.launch(uri)
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhotoCamera,
                            contentDescription = null,
                            tint = LineaColors.TitaniumBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Take Photo",
                            style = LineaTypography.bodyMedium,
                            color = LineaColors.TextPrimary
                        )
                    }

                    // Choose from gallery
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                showPhotoOptionsDialog = false
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhotoLibrary,
                            contentDescription = null,
                            tint = LineaColors.TitaniumBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Choose from Gallery",
                            style = LineaTypography.bodyMedium,
                            color = LineaColors.TextPrimary
                        )
                    }

                    // Remove photo option if one is set
                    if (!currentPhotoUri.isNullOrBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    showPhotoOptionsDialog = false
                                    currentPhotoUri = null
                                    currentPhotoBytes = null
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = null,
                                tint = LineaColors.Danger,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Remove Photo",
                                style = LineaTypography.bodyMedium,
                                color = LineaColors.Danger
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPhotoOptionsDialog = false }) {
                    Text("Cancel", color = LineaColors.TextSecondary)
                }
            },
            containerColor = LineaColors.BackgroundTop,
            shape = RoundedCornerShape(20.dp)
        )
    }

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
                .verticalScroll(scrollState)
        ) {
            // Header: Title & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Cancel",
                        tint = LineaColors.TextSecondary
                    )
                }

                Text(
                    text = if (contactToEdit == null) "New Contact" else "Edit Contact",
                    style = LineaTypography.titleMedium,
                    color = LineaColors.TextPrimary
                )

                Button(
                    onClick = {
                        val validNumbers = numbersList.filter { it.first.isNotBlank() }
                        if (displayName.isNotBlank() && validNumbers.isNotEmpty()) {
                            onSave(
                                displayName.trim(),
                                company.ifBlank { null },
                                validNumbers,
                                if (emailInput.isNotBlank()) listOf(emailInput.trim()) else emptyList(),
                                preferredSimSlot,
                                notes.ifBlank { null },
                                currentPhotoUri,
                                currentPhotoBytes
                            )
                        }
                    },
                    enabled = displayName.isNotBlank() && numbersList.any { it.first.isNotBlank() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LineaColors.TitaniumBlue,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Save", style = LineaTypography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Contact Photo Avatar with Camera Badge
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                        .clickable { showPhotoOptionsDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    ContactAvatar(
                        photoUri = currentPhotoUri,
                        displayName = displayName.ifBlank { "New Contact" },
                        size = 86.dp,
                        initialsTextSize = 28.sp,
                        borderWidth = 1.5.dp,
                        borderColor = LineaColors.TitaniumBlue.copy(alpha = 0.5f)
                    )

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(LineaColors.TitaniumBlue)
                            .border(1.5.dp, LineaColors.BackgroundTop, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhotoCamera,
                            contentDescription = "Set Photo",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (currentPhotoUri.isNullOrBlank()) "Add Photo" else "Change Photo",
                    style = LineaTypography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = LineaColors.TitaniumBlue,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showPhotoOptionsDialog = true }
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Account Selector (Save destination for new contacts)
            if (contactToEdit == null) {
                var expanded by remember { mutableStateOf(false) }

                FrostedGlassBox(
                    shape = RoundedCornerShape(14.dp),
                    borderColor = LineaColors.GlassBorder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { expanded = !expanded }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (selectedAccount?.isDevice == true)
                                                LineaColors.GlassFill
                                            else
                                                LineaColors.TitaniumBlue.copy(alpha = 0.2f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (selectedAccount?.isDevice == true)
                                            Icons.Filled.PhoneAndroid
                                        else
                                            Icons.Filled.AccountCircle,
                                        contentDescription = null,
                                        tint = if (selectedAccount?.isDevice == true)
                                            LineaColors.TextSecondary
                                        else
                                            LineaColors.TitaniumBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = if (selectedAccount?.isDevice == true) "Save to Phone" else "Save to Google",
                                        style = LineaTypography.labelSmall,
                                        color = LineaColors.TextSecondary
                                    )
                                    Text(
                                        text = selectedAccount?.name ?: "Phone storage",
                                        style = LineaTypography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = LineaColors.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(LineaColors.GlassFill)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Switch",
                                    style = LineaTypography.labelSmall,
                                    color = LineaColors.TitaniumBlue
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Filled.ExpandMore,
                                    contentDescription = "Switch Account",
                                    tint = LineaColors.TitaniumBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Expanded list of accounts
                        if (expanded && availableAccounts.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = LineaColors.GlassBorder, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(6.dp))

                            availableAccounts.forEach { account ->
                                val isSelected = (selectedAccount?.name == account.name && selectedAccount?.type == account.type)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) LineaColors.TitaniumBlue.copy(alpha = 0.12f) else Color.Transparent)
                                        .clickable {
                                            onSelectAccount(account)
                                            expanded = false
                                        }
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = if (account.isDevice) Icons.Filled.PhoneAndroid else Icons.Filled.AccountCircle,
                                            contentDescription = null,
                                            tint = if (isSelected) LineaColors.TitaniumBlue else LineaColors.TextTertiary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = account.name,
                                                style = LineaTypography.bodyMedium,
                                                color = if (isSelected) LineaColors.TitaniumBlue else LineaColors.TextPrimary
                                            )
                                            Text(
                                                text = if (account.isDevice) "Device only (no cloud sync)" else if (account.type == "com.google") "Google account" else (account.type ?: ""),
                                                style = LineaTypography.labelSmall.copy(fontSize = 11.sp),
                                                color = LineaColors.TextTertiary
                                            )
                                        }
                                    }

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Selected",
                                            tint = LineaColors.TitaniumBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Display Name
            Text(
                text = "Full Name",
                style = LineaTypography.labelSmall,
                color = LineaColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                placeholder = { Text("Name", color = LineaColors.TextTertiary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = outlinedColors()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Company
            Text(
                text = "Company & Title",
                style = LineaTypography.labelSmall,
                color = LineaColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = company,
                onValueChange = { company = it },
                placeholder = { Text("Company / Job Title", color = LineaColors.TextTertiary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = outlinedColors()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Phone Numbers Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Phone Numbers",
                    style = LineaTypography.labelSmall,
                    color = LineaColors.TextSecondary
                )
                Row(
                    modifier = Modifier.clickable { numbersList.add("" to "Mobile") },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add Number",
                        tint = LineaColors.TitaniumBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Add Number",
                        style = LineaTypography.labelSmall,
                        color = LineaColors.TitaniumBlue
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            numbersList.forEachIndexed { index, (num, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = num,
                        onValueChange = { newNum ->
                            numbersList[index] = newNum to label
                        },
                        placeholder = { Text("Phone Number", color = LineaColors.TextTertiary) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = outlinedColors()
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    if (numbersList.size > 1) {
                        IconButton(
                            onClick = { numbersList.removeAt(index) },
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

            Spacer(modifier = Modifier.height(14.dp))

            // SIM Affinity Selector
            Text(
                text = "Preferred SIM Affinity",
                style = LineaTypography.labelSmall,
                color = LineaColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    null to "Auto",
                    0 to "Force SIM 1",
                    1 to "Force SIM 2"
                ).forEach { (simSlot, label) ->
                    val isSelected = preferredSimSlot == simSlot
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) LineaColors.TitaniumBlue else LineaColors.GlassFill)
                            .border(
                                LineaDimensions.HairlineBorder,
                                if (isSelected) LineaColors.TitaniumBlue else LineaColors.GlassBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { preferredSimSlot = simSlot }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.SimCard,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else LineaColors.TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = label,
                                style = LineaTypography.labelSmall,
                                color = if (isSelected) Color.White else LineaColors.TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Email
            Text(
                text = "Email",
                style = LineaTypography.labelSmall,
                color = LineaColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = emailInput,
                onValueChange = { emailInput = it },
                placeholder = { Text("Email address", color = LineaColors.TextTertiary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = outlinedColors()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Notes
            Text(
                text = "Notes",
                style = LineaTypography.labelSmall,
                color = LineaColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                placeholder = { Text("Contact notes...", color = LineaColors.TextTertiary) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = outlinedColors()
            )

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun outlinedColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = LineaColors.GlassFill,
    unfocusedContainerColor = LineaColors.GlassFill,
    focusedBorderColor = LineaColors.TitaniumBlue,
    unfocusedBorderColor = LineaColors.GlassBorder,
    focusedTextColor = LineaColors.TextPrimary,
    unfocusedTextColor = LineaColors.TextPrimary
)
