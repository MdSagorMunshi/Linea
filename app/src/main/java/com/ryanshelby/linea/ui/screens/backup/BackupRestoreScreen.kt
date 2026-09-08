package com.ryanshelby.linea.ui.screens.backup

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ryanshelby.linea.ui.components.FrostedGlassBox
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography

@Composable
fun BackupRestoreScreen(
    onNavigateBack: () -> Unit,
    viewModel: BackupRestoreViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var importText by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }

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

        // Header
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

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "Backup & Migration",
                    style = LineaTypography.titleLarge,
                    color = LineaColors.TextPrimary
                )
                Text(
                    text = "Export and restore calls & contacts",
                    style = LineaTypography.bodySmall,
                    color = LineaColors.TitaniumBlue
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Export Card
            item {
                FrostedGlassBox(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(LineaColors.TitaniumBlue.copy(alpha = 0.15f))
                                    .border(1.dp, LineaColors.TitaniumBlue.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Upload,
                                    contentDescription = null,
                                    tint = LineaColors.TitaniumBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Export Local Backup",
                                    style = LineaTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = LineaColors.TextPrimary
                                )
                                Text(
                                    text = "Serializes call history, contacts & rules into JSON",
                                    style = LineaTypography.bodySmall,
                                    color = LineaColors.TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                viewModel.exportData()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = LineaColors.TitaniumBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (state.isExporting) {
                                CircularProgressIndicator(
                                    color = LineaColors.TextPrimary,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.FileDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate Backup JSON", style = LineaTypography.labelMedium)
                            }
                        }

                        if (state.exportedJson != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("LINEA_BACKUP", state.exportedJson)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Backup copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = null,
                                    tint = LineaColors.TitaniumBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Copy Backup to Clipboard", color = LineaColors.TitaniumBlue)
                            }
                        }
                    }
                }
            }

            // Restore Card
            item {
                FrostedGlassBox(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(LineaColors.Success.copy(alpha = 0.15f))
                                    .border(1.dp, LineaColors.Success.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Download,
                                    contentDescription = null,
                                    tint = LineaColors.Success,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Restore from Backup",
                                    style = LineaTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = LineaColors.TextPrimary
                                )
                                Text(
                                    text = "Restores records from exported JSON",
                                    style = LineaTypography.bodySmall,
                                    color = LineaColors.TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = clipboard.primaryClip
                                if (clip != null && clip.itemCount > 0) {
                                    val text = clip.getItemAt(0).text?.toString() ?: ""
                                    if (text.contains("LINEA")) {
                                        viewModel.restoreData(text)
                                        return@Button
                                    }
                                }
                                showImportDialog = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = LineaColors.SurfaceElevated),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Restore,
                                contentDescription = null,
                                tint = LineaColors.TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Restore from Clipboard", color = LineaColors.TextPrimary)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { showImportDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Paste JSON Manually", color = LineaColors.TextSecondary)
                        }

                        if (state.restoreResult != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            val res = state.restoreResult!!
                            Text(
                                text = res.message,
                                style = LineaTypography.bodySmall,
                                color = if (res.success) LineaColors.Success else LineaColors.Danger
                            )
                        }
                    }
                }
            }
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Paste Backup JSON", color = LineaColors.TextPrimary) },
            text = {
                OutlinedTextField(
                    value = importText,
                    onValueChange = { importText = it },
                    placeholder = { Text("Paste JSON here...", color = LineaColors.TextSecondary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = LineaColors.TextPrimary,
                        unfocusedTextColor = LineaColors.TextPrimary
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImportDialog = false
                        if (importText.isNotBlank()) {
                            viewModel.restoreData(importText)
                        }
                    }
                ) {
                    Text("Restore", color = LineaColors.TitaniumBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel", color = LineaColors.TextSecondary)
                }
            },
            containerColor = LineaColors.BackgroundElevated
        )
    }
}
