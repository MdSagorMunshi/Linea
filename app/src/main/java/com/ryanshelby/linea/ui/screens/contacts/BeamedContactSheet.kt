package com.ryanshelby.linea.ui.screens.contacts

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.qr_code.QrCodeEngine
import com.ryanshelby.linea.ui.screens.settings.profile.decodeBase64ToBitmap
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaTypography
import com.ryanshelby.linea.ui.theme.captionBold
import com.ryanshelby.linea.ui.theme.captionMedium

/**
 * High-tech glass bottom sheet displayed when a contact is received via LiNEA Beam (NFC).
 * Features animated glowing border, decoded photo avatar, phone numbers, email, and social handles.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeamedContactSheet(
    payload: QrCodeEngine.ContactPayload,
    onDismiss: () -> Unit,
    onSaveContact: (QrCodeEngine.ContactPayload) -> Unit,
    onCallNumber: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Entrance Haptic Celebration
    LaunchedEffect(Unit) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    // Infinite Cyber Celebratory Wave & Rotation Transitions
    val infiniteTransition = rememberInfiniteTransition(label = "beamed_celebration")
    val auraRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "aura_rotation"
    )
    val waveProgress1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1"
    )
    val waveProgress2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, delayMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave2"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LineaColors.BackgroundTop.copy(alpha = 0.96f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(LineaColors.EmeraldPrimary.copy(alpha = 0.45f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row with Live Status Badge
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
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        LineaColors.EmeraldPrimary.copy(alpha = 0.3f),
                                        LineaColors.CyanPrimary.copy(alpha = 0.15f)
                                    )
                                )
                            )
                            .border(1.dp, LineaColors.EmeraldPrimary.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Nfc,
                            contentDescription = "NFC Beam",
                            tint = LineaColors.EmeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = "NFC BEAM DETECTED • CARD RECEIVED",
                        style = LineaTypography.captionBold.copy(
                            fontSize = 11.sp,
                            letterSpacing = 1.4.sp
                        ),
                        color = LineaColors.EmeraldPrimary
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = LineaColors.TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Holographic Pulsing Radar Aura & Rotating Avatar
            val photoBitmap = remember(payload.photoThumbnailBase64) {
                payload.photoThumbnailBase64?.let { decodeBase64ToBitmap(it) }
            }
            val initial = payload.name.trim().firstOrNull()?.uppercase() ?: "C"

            Box(
                modifier = Modifier.size(132.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val baseRadius = 44.dp.toPx()
                    val maxRadius = size.width / 2f

                    // Expanding Electromagnetic Wave 1
                    val r1 = baseRadius + (maxRadius - baseRadius) * waveProgress1
                    val a1 = (1f - waveProgress1).coerceIn(0f, 1f) * 0.75f
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(
                                LineaColors.EmeraldPrimary.copy(alpha = a1 * 0.45f),
                                Color.Transparent
                            ),
                            center = Offset(cx, cy),
                            radius = r1
                        ),
                        radius = r1,
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = LineaColors.EmeraldPrimary.copy(alpha = a1),
                        radius = r1,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    // Expanding Electromagnetic Wave 2
                    val r2 = baseRadius + (maxRadius - baseRadius) * waveProgress2
                    val a2 = (1f - waveProgress2).coerceIn(0f, 1f) * 0.75f
                    drawCircle(
                        color = LineaColors.CyanPrimary.copy(alpha = a2),
                        radius = r2,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                // Central Rotating Border Ring
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .scale(pulseScale)
                        .rotate(auraRotation)
                        .clip(CircleShape)
                        .border(
                            width = 2.5.dp,
                            brush = Brush.sweepGradient(
                                listOf(
                                    LineaColors.EmeraldPrimary,
                                    LineaColors.CyanPrimary,
                                    LineaColors.NeonOrchid,
                                    Color(0xFF7C4DFF),
                                    LineaColors.EmeraldPrimary
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // Central Avatar Core
                Box(
                    modifier = Modifier
                        .size(82.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF141A22)),
                    contentAlignment = Alignment.Center
                ) {
                    if (photoBitmap != null) {
                        Image(
                            bitmap = photoBitmap.asImageBitmap(),
                            contentDescription = "Contact Photo",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = initial,
                            style = LineaTypography.titleLarge.copy(
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = LineaColors.EmeraldPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Contact Name
            Text(
                text = payload.name,
                style = LineaTypography.titleLarge.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = LineaColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Contact card beamed via physical NFC touch",
                style = LineaTypography.captionMedium.copy(fontSize = 12.sp),
                color = LineaColors.TextTertiary
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Numbers List
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                payload.numbers.forEach { number ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF141A22).copy(alpha = 0.85f))
                            .border(1.dp, Color(0xFF2A3A4A).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .clickable {
                                clipboardManager.setText(AnnotatedString(number))
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                Toast.makeText(context, "Copied $number", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PHONE NUMBER",
                                style = LineaTypography.captionBold.copy(fontSize = 9.sp, letterSpacing = 1.sp),
                                color = LineaColors.TextTertiary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = number,
                                style = LineaTypography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.4.sp
                                ),
                                color = LineaColors.TextPrimary
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Copy button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(LineaColors.CyanPrimary.copy(alpha = 0.12f))
                                    .clickable {
                                        clipboardManager.setText(AnnotatedString(number))
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        Toast.makeText(context, "Copied $number", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = LineaColors.CyanPrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Copy",
                                        style = LineaTypography.captionBold.copy(fontSize = 11.sp),
                                        color = LineaColors.CyanPrimary
                                    )
                                }
                            }

                            // Quick Call button
                            IconButton(
                                onClick = {
                                    onCallNumber(number)
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(LineaColors.EmeraldPrimary.copy(alpha = 0.15f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Call",
                                    tint = LineaColors.EmeraldPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Email Row (if present)
            if (!payload.email.isNullOrBlank()) {
                val email = payload.email
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF141A22).copy(alpha = 0.85f))
                        .border(1.dp, Color(0xFF2A3A4A).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .clickable {
                            clipboardManager.setText(AnnotatedString(email))
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            Toast.makeText(context, "Copied $email", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "EMAIL ADDRESS",
                            style = LineaTypography.captionBold.copy(fontSize = 9.sp, letterSpacing = 1.sp),
                            color = LineaColors.TextTertiary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = email,
                            style = LineaTypography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = LineaColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:$email")
                                }
                                try { context.startActivity(intent) } catch (_: Exception) {}
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(LineaColors.CyanPrimary.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Send Email",
                                tint = LineaColors.CyanPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Social profiles row (if present)
            val hasSocial1 = !payload.social1Platform.isNullOrBlank() && !payload.social1Handle.isNullOrBlank()
            val hasSocial2 = !payload.social2Platform.isNullOrBlank() && !payload.social2Handle.isNullOrBlank()

            if (hasSocial1 || hasSocial2) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (hasSocial1) {
                        BeamedSocialCard(
                            platform = payload.social1Platform!!,
                            handle = payload.social1Handle!!,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (hasSocial2) {
                        BeamedSocialCard(
                            platform = payload.social2Platform!!,
                            handle = payload.social2Handle!!,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Bottom Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LineaColors.TextTertiary.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "Dismiss",
                        style = LineaTypography.labelLarge,
                        color = LineaColors.TextSecondary
                    )
                }

                Button(
                    onClick = {
                        onSaveContact(payload)
                        Toast.makeText(context, "Saved ${payload.name} to contacts", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LineaColors.EmeraldPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Save",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save Contact",
                        style = LineaTypography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black
                    )
                }
            }
        }
    }
}

/**
 * Clickable interactive social card for a beamed contact.
 */
@Composable
private fun BeamedSocialCard(
    platform: String,
    handle: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val cleanHandle = handle.removePrefix("@").removePrefix("u/")

    val (brandColor, prefix, url) = remember(platform, cleanHandle) {
        when (platform.uppercase()) {
            "INSTAGRAM" -> Triple(Color(0xFFE1306C), "@", "https://instagram.com/$cleanHandle")
            "TELEGRAM" -> Triple(Color(0xFF29B6F6), "@", "https://t.me/$cleanHandle")
            "GITHUB" -> Triple(Color(0xFFE0E0E0), "@", "https://github.com/$cleanHandle")
            "REDDIT" -> Triple(Color(0xFFFF5722), "u/", "https://reddit.com/u/$cleanHandle")
            else -> Triple(Color(0xFF80CBC4), "@", "")
        }
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(brandColor.copy(alpha = 0.14f))
            .border(1.dp, brandColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                if (url.isNotEmpty()) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        Toast.makeText(context, "$platform: $handle", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(brandColor)
        )
        Column {
            Text(
                text = platform.uppercase(),
                style = LineaTypography.captionBold.copy(fontSize = 9.sp, letterSpacing = 0.5.sp),
                color = brandColor
            )
            Text(
                text = "$prefix$cleanHandle",
                style = LineaTypography.captionMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                color = LineaColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
