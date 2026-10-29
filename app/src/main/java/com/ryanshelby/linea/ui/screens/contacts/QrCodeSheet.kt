package com.ryanshelby.linea.ui.screens.contacts

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.ryanshelby.linea.nfc.NfcBeamManager
import com.ryanshelby.linea.qr_code.QrCodeEngine
import com.ryanshelby.linea.qr_code.StylishQrRenderer
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaTypography
import java.io.File
import java.io.FileOutputStream

/**
 * Modern Liquid Glass Contact Share & QR Sheet.
 *
 * Automatically activates NFC touch discovery & BLE handover when opened, allowing
 * instant tap-to-share between phones while maintaining an interactive QR code as visual fallback.
 * Automatically restores default Digital Card mode upon dismissal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrCodeSheet(
    name: String,
    numbers: List<String>,
    onDismiss: () -> Unit,
    nfcBeamManager: NfcBeamManager? = null,
    email: String? = null,
    photoThumbnailBase64: String? = null,
    social1Platform: String? = null,
    social1Handle: String? = null,
    social2Platform: String? = null,
    social2Handle: String? = null
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var currentTheme by remember { mutableStateOf(StylishQrRenderer.QrTheme.TITANIUM_CYAN) }

    // Build contact payload
    val payload = remember(name, numbers, email, photoThumbnailBase64, social1Platform, social1Handle, social2Platform, social2Handle) {
        QrCodeEngine.ContactPayload(
            name = name.ifBlank { "LiNEA Contact" },
            numbers = numbers,
            email = email,
            photoThumbnailBase64 = photoThumbnailBase64,
            social1Platform = social1Platform,
            social1Handle = social1Handle,
            social2Platform = social2Platform,
            social2Handle = social2Handle
        )
    }

    // NFC capability detection
    val isNfcSupported = remember { nfcBeamManager?.isNfcSupported(context) == true }
    var isNfcEnabled by remember { mutableStateOf(nfcBeamManager?.isNfcEnabled(context) == true) }

    // Automatic NFC & BLE activation when sharing is active
    DisposableEffect(payload, nfcBeamManager) {
        if (nfcBeamManager != null && isNfcSupported && isNfcEnabled) {
            nfcBeamManager.setActiveBeamContact(payload)
            if (activity != null) {
                nfcBeamManager.enableReaderMode(activity)
            }
        }
        onDispose {
            if (nfcBeamManager != null) {
                if (activity != null) {
                    nfcBeamManager.disableReaderMode(activity)
                }
                nfcBeamManager.setActiveBeamContact(null)
                nfcBeamManager.bleHandoverManager?.cancelActiveSession()
            }
        }
    }

    // Refresh NFC status on resume
    LaunchedEffect(Unit) {
        isNfcEnabled = nfcBeamManager?.isNfcEnabled(context) == true
    }

    // Generate QR bit matrix
    val bitMatrix = remember(payload) {
        val content = QrCodeEngine.serializeToUri(payload)
        QrCodeEngine.generateBitMatrix(content)
    }

    val appBgColor = LineaColors.BackgroundTop.toArgb()

    // Render styled QR bitmap for in-app display
    val qrBitmap = remember(bitMatrix, currentTheme) {
        val initialLetter = name.trim().firstOrNull()?.uppercase() ?: "C"
        StylishQrRenderer.render(
            bitMatrix = bitMatrix,
            size = 640,
            theme = currentTheme,
            badgeLetter = initialLetter,
            backgroundColor = android.graphics.Color.TRANSPARENT
        )
    }

    // High-res styled QR bitmap for saving/sharing
    val exportBitmap = remember(bitMatrix, currentTheme, appBgColor) {
        val initialLetter = name.trim().firstOrNull()?.uppercase() ?: "C"
        StylishQrRenderer.render(
            bitMatrix = bitMatrix,
            size = 768,
            theme = currentTheme,
            badgeLetter = initialLetter,
            backgroundColor = appBgColor
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LineaColors.BackgroundTop,
        contentColor = LineaColors.TextPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Contact Sharing",
                    style = LineaTypography.titleLarge,
                    color = LineaColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = LineaColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stylish QR Card - seamlessly blended with app glass fill
            Box(
                modifier = Modifier
                    .size(270.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(LineaColors.GlassFill)
                    .border(1.dp, LineaColors.GlassBorder, RoundedCornerShape(24.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "Contact QR Code",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Contact Name & Details
            Text(
                text = name.ifBlank { "Contact" },
                style = LineaTypography.headlineSmall,
                color = LineaColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            if (numbers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = numbers.joinToString(" • "),
                    style = LineaTypography.bodyMedium,
                    color = LineaColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // NFC / BLE Status Banner or Fallback Prompt
            if (isNfcSupported && isNfcEnabled) {
                // Active Beaming Status Banner
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.45f,
                    targetValue = 0.95f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseAlpha"
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = LineaColors.CyanPrimary.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, LineaColors.CyanPrimary.copy(alpha = pulseAlpha))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(LineaColors.CyanPrimary.copy(alpha = 0.20f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Nfc,
                                contentDescription = "NFC Active",
                                tint = LineaColors.CyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ready to beam — Tap phones back-to-back",
                                style = LineaTypography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = LineaColors.CyanPrimary
                            )
                            Text(
                                text = "Instant transfer via NFC & Bluetooth Low Energy",
                                style = LineaTypography.bodySmall,
                                color = LineaColors.TextSecondary
                            )
                        }
                    }
                }
            } else if (isNfcSupported && !isNfcEnabled) {
                // NFC Disabled Banner with Settings Shortcut
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFFB300).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Nfc,
                                contentDescription = "NFC Disabled",
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "NFC is currently disabled",
                                style = LineaTypography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFB300)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Turn on NFC in system settings to beam contacts instantly with a tap.",
                            style = LineaTypography.bodySmall,
                            color = LineaColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { nfcBeamManager?.openNfcSettings(context) },
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "Enable NFC in Settings",
                                style = LineaTypography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Theme Selector Chips
            Text(
                text = "STYLE THEME",
                style = LineaTypography.labelSmall,
                color = LineaColors.TextTertiary,
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StylishQrRenderer.QrTheme.entries.forEach { theme ->
                    val isSelected = theme == currentTheme
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(theme.startColor))
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color.White else LineaColors.GlassBorder,
                                shape = CircleShape
                            )
                            .clickable { currentTheme = theme },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color.White, CircleShape)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons: Save to Gallery & Share Image
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        saveQrToGallery(context, exportBitmap, name)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = LineaColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Save",
                        color = LineaColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = {
                        shareQrImage(context, exportBitmap, name)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LineaColors.TitaniumBlue
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share", fontWeight = FontWeight.SemiBold)
                }
            }

            // Unobtrusive fallback notice for devices lacking NFC hardware
            if (!isNfcSupported) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Your phone doesn't have NFC.",
                    style = LineaTypography.bodySmall,
                    color = LineaColors.TextTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Save rendered QR bitmap to device Pictures gallery.
 */
private fun saveQrToGallery(context: Context, bitmap: Bitmap, contactName: String) {
    try {
        val safeName = contactName.ifBlank { "Contact" }.replace(" ", "_")
        val filename = "LiNEA_QR_${safeName}_${System.currentTimeMillis()}.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/LiNEA")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
            }
            Toast.makeText(context, "QR Code saved to gallery", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Failed to save QR Code", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error saving QR: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Share rendered QR bitmap directly via system share sheet.
 */
private fun shareQrImage(context: Context, bitmap: Bitmap, contactName: String) {
    try {
        val safeName = contactName.ifBlank { "Contact" }.replace(" ", "_")
        val cachePath = File(context.cacheDir, "shared_qr")
        cachePath.mkdirs()
        val file = File(cachePath, "LiNEA_QR_${safeName}.png")
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }

        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareText = if (contactName.isNotBlank()) {
            "Contact QR Code for $contactName via LiNEA"
        } else {
            "Contact QR Code via LiNEA"
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_TEXT, shareText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share Contact QR"))
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to share QR: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
