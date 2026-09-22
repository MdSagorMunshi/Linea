package com.ryanshelby.linea.ui.screens.settings.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.data.preferences.LineaPreferences
import com.ryanshelby.linea.data.preferences.SocialPlatform
import com.ryanshelby.linea.data.preferences.UserProfile
import com.ryanshelby.linea.nfc.NfcBeamManager
import com.ryanshelby.linea.ui.components.NeumorphicCard
import com.ryanshelby.linea.ui.components.NeumorphicWell
import com.ryanshelby.linea.ui.screens.contacts.LineaBeamSheet
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaTypography
import com.ryanshelby.linea.ui.theme.captionBold
import com.ryanshelby.linea.ui.theme.captionMedium
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Screen for setting up the user's personal digital profile card:
 * Name, Number, Compressed Profile Photo, Email, and 2 Social usernames (Instagram, Telegram, GitHub, Reddit).
 * Displays hardware status for NFC and handles devices without NFC gracefully.
 */
@Composable
fun MyProfileCardScreen(
    lineaPreferences: LineaPreferences,
    nfcBeamManager: NfcBeamManager?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val currentProfile by lineaPreferences.userProfile.collectAsState(initial = UserProfile())

    var name by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var photoBase64 by remember { mutableStateOf<String?>(null) }
    var photoUri by remember { mutableStateOf<String?>(null) }
    var social1Platform by remember { mutableStateOf(SocialPlatform.NONE) }
    var social1Handle by remember { mutableStateOf("") }
    var social2Platform by remember { mutableStateOf(SocialPlatform.NONE) }
    var social2Handle by remember { mutableStateOf("") }

    var isInitialized by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var showQrShareSheet by remember { mutableStateOf(false) }

    val isNfcSupported = remember {
        nfcBeamManager?.isNfcSupported(context) ?: false
    }
    val isNfcEnabled = remember {
        nfcBeamManager?.isNfcEnabled(context) ?: false
    }

    // Populate local editing state once loaded
    LaunchedEffect(currentProfile) {
        if (!isInitialized) {
            name = currentProfile.name
            number = currentProfile.number
            email = currentProfile.email
            photoBase64 = currentProfile.photoBase64
            photoUri = currentProfile.photoUri
            social1Platform = currentProfile.social1Platform
            social1Handle = currentProfile.social1Handle
            social2Platform = currentProfile.social2Platform
            social2Handle = currentProfile.social2Handle
            isInitialized = true
        }
    }

    // Photo picker with automatic downscaling & compression to <= 128x128 JPEG Base64 (~2-4 KB)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val compressed = withContext(Dispatchers.IO) {
                    compressImageToThumbnailBase64(context, uri)
                }
                if (compressed != null) {
                    photoBase64 = compressed.first
                    photoUri = uri.toString()
                    val kb = compressed.second.size / 1024
                    Toast.makeText(context, "Photo compressed ($kb KB) for instant NFC Beam", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to process photo", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Cyber shimmer border animation for digital card preview
    val infiniteTransition = rememberInfiniteTransition(label = "card_shimmer")
    val shimmerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_rotation"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LineaColors.BackgroundBottom)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Top Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "My Digital Card",
                        style = LineaTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = LineaColors.TextPrimary
                    )
                    Text(
                        text = "LiNEA Beam 1-Tap NFC Exchange",
                        style = LineaTypography.bodySmall,
                        color = LineaColors.TextSecondary
                    )
                }
            }

            if (isNfcSupported) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(LineaColors.EmeraldPrimary.copy(alpha = 0.15f))
                        .border(1.dp, LineaColors.EmeraldPrimary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Nfc,
                            contentDescription = "NFC Ready",
                            tint = LineaColors.EmeraldPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isNfcEnabled) "NFC READY" else "NFC OFF",
                            style = LineaTypography.captionBold.copy(fontSize = 10.sp),
                            color = LineaColors.EmeraldPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hardware Support Warning Banner for Devices without NFC
        if (!isNfcSupported) {
            NeumorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(
                                Color(0xFFE57373).copy(alpha = 0.7f),
                                Color(0xFFFFB74D).copy(alpha = 0.4f)
                            )
                        ),
                        RoundedCornerShape(18.dp)
                    ),
                surfaceColor = Color(0xFF261914)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE57373).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = Color(0xFFEF5350),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "NFC Not Supported",
                            style = LineaTypography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFFFCDD2)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "This device does not have an NFC controller chip. 1-Tap mutual contact sharing via NFC is unavailable. You can still set up your card and share it anytime using LiNEA QR Code.",
                            style = LineaTypography.bodySmall.copy(lineHeight = 18.sp),
                            color = Color(0xFFEF9A9A)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Live Holographic Digital Card Preview
        Text(
            text = "LIVE CARD PREVIEW",
            style = LineaTypography.captionBold.copy(letterSpacing = 1.2.sp),
            color = LineaColors.TextTertiary
        )

        Spacer(modifier = Modifier.height(8.dp))

        DigitalCardPreview(
            name = name,
            number = number,
            email = email,
            photoBase64 = photoBase64,
            social1Platform = social1Platform,
            social1Handle = social1Handle,
            social2Platform = social2Platform,
            social2Handle = social2Handle,
            isNfcSupported = isNfcSupported
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Profile Photo & Avatar Section
        Text(
            text = "PROFILE AVATAR",
            style = LineaTypography.captionBold.copy(letterSpacing = 1.2.sp),
            color = LineaColors.TextTertiary
        )
        Spacer(modifier = Modifier.height(8.dp))

        NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val avatarBitmap = remember(photoBase64) {
                        photoBase64?.let { decodeBase64ToBitmap(it) }
                    }

                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF141A22))
                            .border(
                                2.dp,
                                Brush.sweepGradient(
                                    listOf(
                                        LineaColors.EmeraldPrimary,
                                        LineaColors.CyanPrimary,
                                        LineaColors.NeonOrchid,
                                        LineaColors.EmeraldPrimary
                                    )
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarBitmap != null) {
                            Image(
                                bitmap = avatarBitmap.asImageBitmap(),
                                contentDescription = "Profile Photo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            val initial = name.trim().firstOrNull()?.uppercase() ?: "U"
                            Text(
                                text = initial,
                                style = LineaTypography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp
                                ),
                                color = LineaColors.EmeraldPrimary
                            )
                        }
                    }

                    Column {
                        Text(
                            text = if (photoBase64 != null) "Compressed Avatar" else "Monogram Avatar",
                            style = LineaTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = LineaColors.TextPrimary
                        )
                        Text(
                            text = if (photoBase64 != null) "Optimized for <100ms NFC APDU swap" else "Auto-generated from name",
                            style = LineaTypography.bodySmall,
                            color = LineaColors.TextSecondary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Pick Photo
                    IconButton(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(LineaColors.CyanPrimary.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = "Pick Photo",
                            tint = LineaColors.CyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Remove Photo
                    if (photoBase64 != null) {
                        IconButton(
                            onClick = {
                                photoBase64 = null
                                photoUri = null
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE57373).copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove Photo",
                                tint = Color(0xFFEF5350),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Personal Information Fields
        Text(
            text = "PERSONAL DETAILS",
            style = LineaTypography.captionBold.copy(letterSpacing = 1.2.sp),
            color = LineaColors.TextTertiary
        )
        Spacer(modifier = Modifier.height(8.dp))

        NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Name
                ProfileInputField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Display Name",
                    placeholder = "Your full name",
                    leadingIcon = Icons.Default.Person
                )

                // Number
                ProfileInputField(
                    value = number,
                    onValueChange = { number = it },
                    label = "Phone Number",
                    placeholder = "+1 234 567 8900",
                    leadingIcon = Icons.Default.Phone,
                    keyboardType = KeyboardType.Phone
                )

                // Email
                ProfileInputField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email Address",
                    placeholder = "you@domain.com",
                    leadingIcon = Icons.Default.Email,
                    keyboardType = KeyboardType.Email
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2 Social Usernames Setup
        Text(
            text = "SOCIAL PROFILES (UP TO 2 SIMULTANEOUS)",
            style = LineaTypography.captionBold.copy(letterSpacing = 1.2.sp),
            color = LineaColors.TextTertiary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Select 2 platforms to exchange with peers on physical tap",
            style = LineaTypography.bodySmall,
            color = LineaColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Social Slot 1
        SocialSlotEditor(
            slotTitle = "Social Profile #1",
            selectedPlatform = social1Platform,
            handle = social1Handle,
            onPlatformSelected = { social1Platform = it },
            onHandleChange = { social1Handle = it }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Social Slot 2
        SocialSlotEditor(
            slotTitle = "Social Profile #2",
            selectedPlatform = social2Platform,
            handle = social2Handle,
            onPlatformSelected = { social2Platform = it },
            onHandleChange = { social2Handle = it }
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // View / Share QR Code button
            OutlinedButton(
                onClick = { showQrShareSheet = true },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, LineaColors.CyanPrimary.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = "Share QR",
                    tint = LineaColors.CyanPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "View QR",
                    style = LineaTypography.labelLarge,
                    color = LineaColors.CyanPrimary
                )
            }

            // Save Profile Button
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch {
                        isSaving = true
                        val newProfile = UserProfile(
                            name = name.trim(),
                            number = number.trim(),
                            email = email.trim(),
                            photoBase64 = photoBase64,
                            photoUri = photoUri,
                            social1Platform = social1Platform,
                            social1Handle = social1Handle.trim(),
                            social2Platform = social2Platform,
                            social2Handle = social2Handle.trim()
                        )
                        lineaPreferences.updateUserProfile(newProfile)

                        // Update active payload in NfcBeamManager immediately
                        val payload = newProfile.toContactPayload()
                        nfcBeamManager?.setUserProfilePayload(payload)

                        isSaving = false
                        Toast.makeText(context, "Profile card saved & synced for NFC Beam!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .weight(1.3f)
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LineaColors.EmeraldPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Save",
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSaving) "Saving..." else "Save Card",
                    style = LineaTypography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // Sheet to show personal card as QR code / manual beam
    if (showQrShareSheet && nfcBeamManager != null) {
        val payload = remember(name, number, email, photoBase64, social1Platform, social1Handle, social2Platform, social2Handle) {
            UserProfile(
                name = name.ifBlank { "LiNEA Contact" },
                number = number,
                email = email,
                photoBase64 = photoBase64,
                social1Platform = social1Platform,
                social1Handle = social1Handle,
                social2Platform = social2Platform,
                social2Handle = social2Handle
            ).toContactPayload()
        }

        if (payload != null) {
            LineaBeamSheet(
                name = payload.name,
                numbers = payload.numbers,
                nfcBeamManager = nfcBeamManager,
                onDismiss = { showQrShareSheet = false }
            )
        } else {
            Toast.makeText(context, "Please enter your name or number first", Toast.LENGTH_SHORT).show()
            showQrShareSheet = false
        }
    }
}

/**
 * Holographic live preview of the digital card.
 */
@Composable
private fun DigitalCardPreview(
    name: String,
    number: String,
    email: String,
    photoBase64: String?,
    social1Platform: SocialPlatform,
    social1Handle: String,
    social2Platform: SocialPlatform,
    social2Handle: String,
    isNfcSupported: Boolean
) {
    val avatarBitmap = remember(photoBase64) {
        photoBase64?.let { decodeBase64ToBitmap(it) }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF131922),
                        Color(0xFF0D1219),
                        Color(0xFF101720)
                    )
                )
            )
            .border(
                1.5.dp,
                Brush.linearGradient(
                    listOf(
                        LineaColors.EmeraldPrimary.copy(alpha = 0.6f),
                        LineaColors.CyanPrimary.copy(alpha = 0.3f),
                        LineaColors.NeonOrchid.copy(alpha = 0.4f),
                        LineaColors.EmeraldPrimary.copy(alpha = 0.6f)
                    )
                ),
                RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        Column {
            // Card Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(LineaColors.EmeraldPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Nfc,
                            contentDescription = "NFC",
                            tint = LineaColors.EmeraldPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = "LiNEA BEAM CARD",
                        style = LineaTypography.captionBold.copy(fontSize = 10.sp, letterSpacing = 1.4.sp),
                        color = LineaColors.EmeraldPrimary
                    )
                }

                Text(
                    text = if (isNfcSupported) "TAP TO SHARE" else "QR ONLY",
                    style = LineaTypography.captionBold.copy(fontSize = 9.sp),
                    color = if (isNfcSupported) LineaColors.CyanPrimary else Color(0xFFFFB74D)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Avatar & Name Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A222D))
                        .border(1.5.dp, LineaColors.EmeraldPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarBitmap != null) {
                        Image(
                            bitmap = avatarBitmap.asImageBitmap(),
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        val initial = name.trim().firstOrNull()?.uppercase() ?: "U"
                        Text(
                            text = initial,
                            style = LineaTypography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp
                            ),
                            color = LineaColors.EmeraldPrimary
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name.ifBlank { "Your Name" },
                        style = LineaTypography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = LineaColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = number.ifBlank { "+ Phone Number" },
                        style = LineaTypography.bodyMedium,
                        color = LineaColors.CyanPrimary,
                        maxLines = 1
                    )
                    if (email.isNotBlank()) {
                        Text(
                            text = email,
                            style = LineaTypography.bodySmall,
                            color = LineaColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Social handles preview pills
            val hasSocial1 = social1Platform != SocialPlatform.NONE && social1Handle.isNotBlank()
            val hasSocial2 = social2Platform != SocialPlatform.NONE && social2Handle.isNotBlank()

            if (hasSocial1 || hasSocial2) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (hasSocial1) {
                        SocialBadgePill(
                            platform = social1Platform,
                            handle = social1Handle,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (hasSocial2) {
                        SocialBadgePill(
                            platform = social2Platform,
                            handle = social2Handle,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Platform selector and handle input for a social slot.
 */
@Composable
private fun SocialSlotEditor(
    slotTitle: String,
    selectedPlatform: SocialPlatform,
    handle: String,
    onPlatformSelected: (SocialPlatform) -> Unit,
    onHandleChange: (String) -> Unit
) {
    NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = slotTitle,
                style = LineaTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = LineaColors.TextPrimary
            )

            // Platform selection pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SocialPlatform.entries.forEach { platform ->
                    val isSelected = platform == selectedPlatform
                    val brandColor = getPlatformColor(platform)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) brandColor.copy(alpha = 0.22f)
                                else Color(0xFF141A22)
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) brandColor else Color(0xFF2A3644),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onPlatformSelected(platform) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (platform) {
                                SocialPlatform.NONE -> "Off"
                                SocialPlatform.INSTAGRAM -> "IG"
                                SocialPlatform.TELEGRAM -> "TG"
                                SocialPlatform.GITHUB -> "GH"
                                SocialPlatform.REDDIT -> "Reddit"
                            },
                            style = LineaTypography.captionBold.copy(fontSize = 11.sp),
                            color = if (isSelected) brandColor else LineaColors.TextSecondary,
                            maxLines = 1
                        )
                    }
                }
            }

            // Username input (visible when platform != NONE)
            if (selectedPlatform != SocialPlatform.NONE) {
                ProfileInputField(
                    value = handle,
                    onValueChange = onHandleChange,
                    label = "${selectedPlatform.displayName} Username",
                    placeholder = "username without @",
                    leadingIcon = Icons.Default.AlternateEmail
                )
            }
        }
    }
}

/**
 * Sleek interactive social badge pill.
 */
@Composable
fun SocialBadgePill(
    platform: SocialPlatform,
    handle: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val brandColor = getPlatformColor(platform)
    val cleanHandle = handle.removePrefix("@").removePrefix("u/")

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(brandColor.copy(alpha = 0.15f))
            .border(1.dp, brandColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(brandColor)
        )

        Column {
            Text(
                text = platform.displayName.uppercase(),
                style = LineaTypography.captionBold.copy(fontSize = 8.sp),
                color = brandColor
            )
            Text(
                text = "${platform.handlePrefix}$cleanHandle",
                style = LineaTypography.captionMedium.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                color = LineaColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProfileInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = LineaTypography.bodySmall) },
        placeholder = { Text(placeholder, style = LineaTypography.bodySmall) },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = label,
                tint = LineaColors.CyanPrimary,
                modifier = Modifier.size(18.dp)
            )
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LineaColors.EmeraldPrimary,
            unfocusedBorderColor = Color(0xFF2E3A4A),
            focusedTextColor = LineaColors.TextPrimary,
            unfocusedTextColor = LineaColors.TextPrimary,
            focusedContainerColor = Color(0xFF111720),
            unfocusedContainerColor = Color(0xFF111720)
        )
    )
}

/**
 * Returns signature color for social platforms.
 */
fun getPlatformColor(platform: SocialPlatform): Color {
    return when (platform) {
        SocialPlatform.NONE -> Color(0xFF78909C)
        SocialPlatform.INSTAGRAM -> Color(0xFFE1306C)
        SocialPlatform.TELEGRAM -> Color(0xFF29B6F6)
        SocialPlatform.GITHUB -> Color(0xFFE0E0E0)
        SocialPlatform.REDDIT -> Color(0xFFFF5722)
    }
}

/**
 * Downscales and compresses image to max 128x128 JPEG at 70% quality (~2-4 KB).
 * Returns Pair(Base64String, RawBytes) or null on failure.
 */
fun compressImageToThumbnailBase64(context: Context, uri: Uri): Pair<String, ByteArray>? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val original = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        if (original == null) return null

        val maxDim = 128
        val width = original.width
        val height = original.height

        val scaled = if (width > maxDim || height > maxDim) {
            val scale = maxDim.toFloat() / maxOf(width, height)
            val newW = (width * scale).toInt().coerceAtLeast(1)
            val newH = (height * scale).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(original, newW, newH, true)
        } else {
            original
        }

        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 70, baos)
        val bytes = baos.toByteArray()
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        Pair(base64, bytes)
    } catch (_: Exception) {
        null
    }
}

/**
 * Decodes Base64 string to Bitmap.
 */
fun decodeBase64ToBitmap(base64Str: String?): Bitmap? {
    if (base64Str.isNullOrBlank()) return null
    return try {
        val bytes = Base64.decode(base64Str, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (_: Exception) {
        null
    }
}
