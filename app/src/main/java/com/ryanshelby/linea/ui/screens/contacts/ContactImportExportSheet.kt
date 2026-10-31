package com.ryanshelby.linea.ui.screens.contacts

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.data.contacts.ContactExportFormat
import com.ryanshelby.linea.data.contacts.DuplicatePolicy
import com.ryanshelby.linea.data.contacts.ExportScope
import com.ryanshelby.linea.data.preferences.DesignStyle
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaTypography
import com.ryanshelby.linea.ui.theme.LocalDesignStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ImportExportTab(val title: String, val icon: ImageVector) {
    EXPORT("Export", Icons.Default.FileUpload),
    IMPORT("Import", Icons.Default.FileDownload)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactImportExportSheet(
    viewModel: ContactsViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val style = LocalDesignStyle.current
    val isM3 = style == DesignStyle.MATERIAL3

    var activeTab by remember { mutableStateOf(ImportExportTab.EXPORT) }

    // Export options
    var selectedExportFormat by remember { mutableStateOf(ContactExportFormat.VCF) }
    var selectedExportScope by remember { mutableStateOf(ExportScope.ALL) }

    // Import options
    var selectedImportFormat by remember { mutableStateOf<ContactExportFormat?>(null) } // null = auto detect
    var selectedDuplicatePolicy by remember { mutableStateOf(DuplicatePolicy.MERGE_EXISTING) }

    val availableAccounts by viewModel.availableAccounts.collectAsState()
    var selectedAccountName by remember { mutableStateOf<String?>(null) }
    var selectedAccountType by remember { mutableStateOf<String?>(null) }

    val importExportState by viewModel.importExportState.collectAsState()

    // SAF File Creator for Export
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(selectedExportFormat.mimeType)
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportContacts(
                uri = uri,
                format = selectedExportFormat,
                scope = selectedExportScope,
                contentResolver = contentResolver
            )
        }
    }

    // SAF File Picker for Import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importContacts(
                uri = uri,
                format = selectedImportFormat,
                policy = selectedDuplicatePolicy,
                accountName = selectedAccountName,
                accountType = selectedAccountType,
                contentResolver = contentResolver
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.resetImportExportState()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = if (isM3) LineaColors.SurfaceElevated else LineaColors.BackgroundDeep,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(LineaColors.TitaniumBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = LineaColors.TitaniumBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Import & Export",
                            style = LineaTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = LineaColors.TextPrimary
                        )
                        Text(
                            text = "vCard (.vcf) • CSV • Sovereign JSON",
                            style = LineaTypography.labelSmall,
                            color = LineaColors.TextSecondary
                        )
                    }
                }
                IconButton(onClick = {
                    viewModel.resetImportExportState()
                    onDismiss()
                }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = LineaColors.TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Tab Selector Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(LineaColors.GlassFill)
                    .border(0.5.dp, LineaColors.GlassBorder, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ImportExportTab.values().forEach { tab ->
                    val isSelected = activeTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) LineaColors.TitaniumBlue.copy(alpha = if (isM3) 0.28f else 0.20f)
                                else Color.Transparent
                            )
                            .clickable {
                                activeTab = tab
                                viewModel.resetImportExportState()
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                tint = if (isSelected) LineaColors.TitaniumBlue else LineaColors.TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = tab.title,
                                style = LineaTypography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) LineaColors.TitaniumBlue else LineaColors.TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // State Feedback (Loading, Success, Error)
            when (val state = importExportState) {
                is ContactsViewModel.ImportExportState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(LineaColors.GlassFill)
                            .border(0.5.dp, LineaColors.GlassBorder, RoundedCornerShape(16.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = LineaColors.TitaniumBlue,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                text = if (activeTab == ImportExportTab.EXPORT) "Generating & saving contacts..." else "Reading & syncing contacts...",
                                style = LineaTypography.bodyMedium,
                                color = LineaColors.TextPrimary
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                is ContactsViewModel.ImportExportState.ExportSuccess -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(LineaColors.Success.copy(alpha = 0.15f))
                            .border(1.dp, LineaColors.Success.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = LineaColors.Success, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Export Complete",
                                    style = LineaTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = LineaColors.Success
                                )
                                Text(
                                    text = "Successfully saved ${state.count} contacts to ${state.format.displayName}.",
                                    style = LineaTypography.bodySmall,
                                    color = LineaColors.TextSecondary
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                is ContactsViewModel.ImportExportState.ImportSuccess -> {
                    val summary = state.summary
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(LineaColors.Success.copy(alpha = 0.15f))
                            .border(1.dp, LineaColors.Success.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = LineaColors.Success, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "Import Summary",
                                    style = LineaTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = LineaColors.Success
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total processed:", style = LineaTypography.bodySmall, color = LineaColors.TextSecondary)
                                Text("${summary.totalRead}", style = LineaTypography.bodySmall.copy(fontWeight = FontWeight.Bold), color = LineaColors.TextPrimary)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("New contacts added:", style = LineaTypography.bodySmall, color = LineaColors.TextSecondary)
                                Text("${summary.importedCount}", style = LineaTypography.bodySmall.copy(fontWeight = FontWeight.Bold), color = LineaColors.Success)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Contacts merged:", style = LineaTypography.bodySmall, color = LineaColors.TextSecondary)
                                Text("${summary.mergedCount}", style = LineaTypography.bodySmall.copy(fontWeight = FontWeight.Bold), color = LineaColors.TitaniumBlue)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Duplicates skipped:", style = LineaTypography.bodySmall, color = LineaColors.TextSecondary)
                                Text("${summary.skippedCount}", style = LineaTypography.bodySmall.copy(fontWeight = FontWeight.Bold), color = LineaColors.TextTertiary)
                            }
                            if (summary.errorCount > 0) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Errors encountered:", style = LineaTypography.bodySmall, color = LineaColors.Danger)
                                    Text("${summary.errorCount}", style = LineaTypography.bodySmall.copy(fontWeight = FontWeight.Bold), color = LineaColors.Danger)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                is ContactsViewModel.ImportExportState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(LineaColors.Danger.copy(alpha = 0.15f))
                            .border(1.dp, LineaColors.Danger.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Close, null, tint = LineaColors.Danger, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Operation Error",
                                    style = LineaTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = LineaColors.Danger
                                )
                                Text(
                                    text = state.message,
                                    style = LineaTypography.bodySmall,
                                    color = LineaColors.TextSecondary
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                is ContactsViewModel.ImportExportState.Idle -> {}
            }

            // Tab Content
            if (activeTab == ImportExportTab.EXPORT) {
                // EXPORT TAB
                Text(
                    text = "SELECT EXPORT FORMAT",
                    style = LineaTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = LineaColors.TextSecondary
                )
                Spacer(Modifier.height(8.dp))

                ContactExportFormat.values().forEach { format ->
                    val isSelected = selectedExportFormat == format
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) LineaColors.TitaniumBlue.copy(alpha = 0.14f)
                                else LineaColors.GlassFill
                            )
                            .border(
                                if (isSelected) 1.2.dp else 0.5.dp,
                                if (isSelected) LineaColors.TitaniumBlue.copy(alpha = 0.6f) else LineaColors.GlassBorder,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { selectedExportFormat = format }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (format) {
                                    ContactExportFormat.VCF -> Icons.Default.Description
                                    ContactExportFormat.CSV -> Icons.Default.TableChart
                                    ContactExportFormat.JSON -> Icons.Default.Security
                                },
                                contentDescription = null,
                                tint = if (isSelected) LineaColors.TitaniumBlue else LineaColors.TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = format.displayName,
                                    style = LineaTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = LineaColors.TextPrimary
                                )
                                Text(
                                    text = when (format) {
                                        ContactExportFormat.VCF -> "Compatible with all mobile devices & systems"
                                        ContactExportFormat.CSV -> "Spreadsheet table format for Excel or Google Sheets"
                                        ContactExportFormat.JSON -> "Full sovereign structured JSON backup"
                                    },
                                    style = LineaTypography.labelSmall,
                                    color = LineaColors.TextSecondary
                                )
                            }
                        }
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedExportFormat = format },
                            colors = RadioButtonDefaults.colors(selectedColor = LineaColors.TitaniumBlue)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "EXPORT SCOPE",
                    style = LineaTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = LineaColors.TextSecondary
                )
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ExportScope.values().forEach { scope ->
                        val isSelected = selectedExportScope == scope
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) LineaColors.TitaniumBlue.copy(alpha = 0.16f)
                                    else LineaColors.GlassFill
                                )
                                .border(
                                    if (isSelected) 1.dp else 0.5.dp,
                                    if (isSelected) LineaColors.TitaniumBlue.copy(alpha = 0.6f) else LineaColors.GlassBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedExportScope = scope }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = scope.displayName,
                                style = LineaTypography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) LineaColors.TitaniumBlue else LineaColors.TextSecondary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Sovereign Privacy Alert
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(LineaColors.GlassFill)
                        .border(0.5.dp, LineaColors.GlassBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = LineaColors.TextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Private Vault contacts are strictly isolated and never exported in standard plain formats.",
                            style = LineaTypography.labelSmall,
                            color = LineaColors.TextSecondary
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Action Button: Save to Device
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    LineaColors.TitaniumBlue,
                                    LineaColors.TitaniumBlue.copy(alpha = 0.85f)
                                )
                            )
                        )
                        .clickable {
                            val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                            val suggestedName = "linea_contacts_$dateStr.${selectedExportFormat.extension}"
                            exportLauncher.launch(suggestedName)
                        }
                        .padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FileUpload, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Save Contacts File",
                            style = LineaTypography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }

            } else {
                // IMPORT TAB
                Text(
                    text = "DUPLICATE RESOLUTION POLICY",
                    style = LineaTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = LineaColors.TextSecondary
                )
                Spacer(Modifier.height(8.dp))

                DuplicatePolicy.values().forEach { policy ->
                    val isSelected = selectedDuplicatePolicy == policy
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) LineaColors.TitaniumBlue.copy(alpha = 0.14f)
                                else LineaColors.GlassFill
                            )
                            .border(
                                if (isSelected) 1.2.dp else 0.5.dp,
                                if (isSelected) LineaColors.TitaniumBlue.copy(alpha = 0.6f) else LineaColors.GlassBorder,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { selectedDuplicatePolicy = policy }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = policy.displayName,
                                style = LineaTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = LineaColors.TextPrimary
                            )
                            Text(
                                text = policy.description,
                                style = LineaTypography.labelSmall,
                                color = LineaColors.TextSecondary
                            )
                        }
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedDuplicatePolicy = policy },
                            colors = RadioButtonDefaults.colors(selectedColor = LineaColors.TitaniumBlue)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Target Account Selector
                if (availableAccounts.isNotEmpty()) {
                    Text(
                        text = "DESTINATION ACCOUNT",
                        style = LineaTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = LineaColors.TextSecondary
                    )
                    Spacer(Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(LineaColors.GlassFill)
                            .border(0.5.dp, LineaColors.GlassBorder, RoundedCornerShape(14.dp))
                    ) {
                        availableAccounts.forEachIndexed { idx, acc ->
                            val isSelected = (selectedAccountName == acc.name && selectedAccountType == acc.type) ||
                                    (selectedAccountName == null && acc.isDevice)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedAccountName = acc.name
                                        selectedAccountType = acc.type
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = if (isSelected) LineaColors.TitaniumBlue else LineaColors.TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = acc.name,
                                        style = LineaTypography.bodyMedium,
                                        color = if (isSelected) LineaColors.TitaniumBlue else LineaColors.TextPrimary
                                    )
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, null, tint = LineaColors.TitaniumBlue, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // Zero Cloud Offline Guarantee
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(LineaColors.GlassFill)
                        .border(0.5.dp, LineaColors.GlassBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = LineaColors.Success,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "100% Offline: Imports are parsed locally on-device with zero cloud telemetry.",
                            style = LineaTypography.labelSmall,
                            color = LineaColors.TextSecondary
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Action Button: Select File to Import
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    LineaColors.Success,
                                    LineaColors.Success.copy(alpha = 0.85f)
                                )
                            )
                        )
                        .clickable {
                            // Launch document picker for vCard, CSV, JSON
                            importLauncher.launch(
                                arrayOf(
                                    "text/x-vcard",
                                    "text/vcard",
                                    "text/csv",
                                    "application/json",
                                    "text/plain",
                                    "*/*"
                                )
                            )
                        }
                        .padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FileDownload, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Select File & Import",
                            style = LineaTypography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}
