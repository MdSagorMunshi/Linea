package com.ryanshelby.linea.ui.screens.contacts

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.ryanshelby.linea.ui.components.ContactAvatar
import com.ryanshelby.linea.util.ContactPhotoHelper
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ryanshelby.linea.ui.components.FrostedGlassBox
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactDashboardScreen(
    contactId: Long,
    onNavigateBack: () -> Unit,
    viewModel: ContactDashboardViewModel = hiltViewModel()
) {
    LaunchedEffect(contactId) {
        viewModel.loadContactData(contactId)
    }

    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var noteInput by remember { mutableStateOf("") }
    var isEditingNote by remember { mutableStateOf(false) }

    var showPhotoDialog by remember { mutableStateOf(false) }
    var isEditingContact by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            coroutineScope.launch {
                val (savedUri, bytes) = ContactPhotoHelper.saveUriToInternal(context, tempCameraUri!!)
                if (savedUri.isNotBlank()) {
                    viewModel.updateContactPhoto(savedUri, bytes)
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
                    viewModel.updateContactPhoto(savedUri, bytes)
                }
            }
        }
    }

    LaunchedEffect(state.preCallNote) {
        if (state.preCallNote != null) {
            noteInput = state.preCallNote!!.noteText
        }
    }

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

        // Top Navigation Bar
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

            Spacer(modifier = Modifier.weight(1f))

            // Star Favorite Button
            IconButton(
                onClick = { viewModel.toggleFavorite() },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(LineaColors.GlassFill)
                    .border(LineaDimensions.HairlineBorder, LineaColors.GlassBorder, CircleShape)
            ) {
                Icon(
                    imageVector = if (state.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (state.isFavorite) LineaColors.Warning else LineaColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Private Contact Lock Button
            IconButton(
                onClick = { viewModel.togglePrivate() },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(LineaColors.GlassFill)
                    .border(LineaDimensions.HairlineBorder, LineaColors.GlassBorder, CircleShape)
            ) {
                Icon(
                    imageVector = if (state.isPrivate) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    contentDescription = "Private Contact",
                    tint = if (state.isPrivate) LineaColors.Danger else LineaColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Edit Contact Button
            IconButton(
                onClick = { isEditingContact = true },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(LineaColors.GlassFill)
                    .border(LineaDimensions.HairlineBorder, LineaColors.GlassBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit Contact",
                    tint = LineaColors.TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val contact = state.contact
        if (contact != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Header Profile Card
                item {
                    FrostedGlassBox(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .clickable { showPhotoDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                ContactAvatar(
                                    photoUri = contact.photoUri,
                                    displayName = contact.displayName,
                                    size = 80.dp,
                                    initialsTextSize = 30.sp,
                                    borderWidth = 2.dp,
                                    borderColor = LineaColors.TitaniumBlue
                                )

                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .align(Alignment.BottomEnd)
                                        .clip(CircleShape)
                                        .background(LineaColors.TitaniumBlue)
                                        .border(1.5.dp, LineaColors.BackgroundDeep, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PhotoCamera,
                                        contentDescription = "Change Photo",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = contact.displayName,
                                style = LineaTypography.titleLarge,
                                color = LineaColors.TextPrimary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )

                            if (!contact.company.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${contact.jobTitle?.let { "$it at " } ?: ""}${contact.company}",
                                    style = LineaTypography.bodySmall,
                                    color = LineaColors.TitaniumBlue
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Action Buttons Row (Call, SMS, Share)
                            val primaryNumber = state.numbers.firstOrNull()?.number ?: ""
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                DashboardActionButton(
                                    icon = Icons.Filled.Call,
                                    label = "Call",
                                    color = LineaColors.Success,
                                    onClick = {
                                        if (primaryNumber.isNotBlank()) {
                                            viewModel.placeCall(primaryNumber)
                                        }
                                    }
                                )
                                DashboardActionButton(
                                    icon = Icons.Filled.Message,
                                    label = "Message",
                                    color = LineaColors.TitaniumBlue,
                                    onClick = {
                                        if (primaryNumber.isNotBlank()) {
                                            val smsIntent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:$primaryNumber"))
                                            context.startActivity(smsIntent)
                                        }
                                    }
                                )
                                DashboardActionButton(
                                    icon = Icons.Filled.Share,
                                    label = "Share",
                                    color = LineaColors.TextSecondary,
                                    onClick = {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, "${contact.displayName}: $primaryNumber")
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Contact"))
                                    }
                                )
                            }
                        }
                    }
                }

                // Phone Numbers Card with 1-second hold-to-copy
                item {
                    val haptic = LocalHapticFeedback.current
                    FrostedGlassBox(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Phone Numbers",
                                    style = LineaTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = LineaColors.TextPrimary
                                )
                                Text(
                                    text = "Hold 1s to copy",
                                    style = LineaTypography.labelSmall,
                                    color = LineaColors.TextTertiary
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (state.numbers.isEmpty()) {
                                Text(
                                    text = "No phone numbers added",
                                    style = LineaTypography.bodyMedium,
                                    color = LineaColors.TextTertiary
                                )
                            } else {
                                state.numbers.forEachIndexed { index, numEntity ->
                                    if (index > 0) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            color = LineaColors.GlassBorder.copy(alpha = 0.4f)
                                        )
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .combinedClickable(
                                                onClick = {
                                                    viewModel.placeCall(numEntity.number)
                                                },
                                                onLongClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    val clip = ClipData.newPlainText("Phone Number", numEntity.number)
                                                    clipboard.setPrimaryClip(clip)
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    Toast.makeText(context, "Copied ${numEntity.number} to clipboard", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                            .padding(vertical = 6.dp, horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = numEntity.number,
                                                style = LineaTypography.bodyLarge.copy(
                                                    fontFeatureSettings = "tnum",
                                                    fontWeight = FontWeight.SemiBold
                                                ),
                                                color = LineaColors.TextPrimary
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = numEntity.label,
                                                    style = LineaTypography.bodySmall,
                                                    color = LineaColors.TextSecondary
                                                )
                                                if (state.preferredSimSlot != null) {
                                                    Text(
                                                        text = " • SIM ${state.preferredSimSlot!! + 1}",
                                                        style = LineaTypography.bodySmall,
                                                        color = LineaColors.TitaniumBlue
                                                    )
                                                }
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    val smsIntent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:${numEntity.number}"))
                                                    context.startActivity(smsIntent)
                                                },
                                                modifier = Modifier.size(38.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Message,
                                                    contentDescription = "SMS",
                                                    tint = LineaColors.TextSecondary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { viewModel.placeCall(numEntity.number) },
                                                modifier = Modifier.size(38.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Call,
                                                    contentDescription = "Call",
                                                    tint = LineaColors.Success,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Availability Insight Card (100% Local / Zero AI Heuristic)
                item {
                    val insight = state.availabilityInsight
                    FrostedGlassBox(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(LineaColors.TitaniumBlue.copy(alpha = 0.15f))
                                        .border(1.dp, LineaColors.TitaniumBlue.copy(alpha = 0.4f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.QueryBuilder,
                                        contentDescription = null,
                                        tint = LineaColors.TitaniumBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "LOCAL AVAILABILITY INSIGHT",
                                        style = LineaTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = LineaColors.TitaniumBlue
                                    )
                                    Text(
                                        text = if (insight?.hasSufficientData == true) insight.bestTimeWindow else "Calculating...",
                                        style = LineaTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = LineaColors.TextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                if (insight?.hasSufficientData == true) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(LineaColors.Success.copy(alpha = 0.15f))
                                            .border(1.dp, LineaColors.Success.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${insight.answerRatePercent}% MATCH",
                                            style = LineaTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = LineaColors.Success
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = insight?.summary ?: "No call logs yet to compute availability heuristic.",
                                style = LineaTypography.bodySmall,
                                color = LineaColors.TextSecondary
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Last interaction summary
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.History,
                                    contentDescription = null,
                                    tint = LineaColors.TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Last Interaction: ${state.lastInteractionSummary}",
                                    style = LineaTypography.bodySmall,
                                    color = LineaColors.TextPrimary
                                )
                            }
                        }
                    }
                }

                // Per-Contact Settings Card
                item {
                    FrostedGlassBox(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Text(
                                text = "Contact-Specific Behavior",
                                style = LineaTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = LineaColors.TextPrimary
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Preferred SIM selector
                            Text(
                                text = "Preferred Cellular SIM",
                                style = LineaTypography.labelSmall,
                                color = LineaColors.TextSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SimChip(
                                    label = "Auto / System",
                                    selected = state.preferredSimSlot == null,
                                    onClick = { viewModel.setPreferredSim(null) },
                                    modifier = Modifier.weight(1f)
                                )
                                SimChip(
                                    label = "Force SIM 1",
                                    selected = state.preferredSimSlot == 0,
                                    onClick = { viewModel.setPreferredSim(0) },
                                    modifier = Modifier.weight(1f)
                                )
                                SimChip(
                                    label = "Force SIM 2",
                                    selected = state.preferredSimSlot == 1,
                                    onClick = { viewModel.setPreferredSim(1) },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Always Ring Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Always Ring (Override Restrictions)",
                                        style = LineaTypography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = LineaColors.TextPrimary
                                    )
                                    Text(
                                        text = "Bypasses quiet hours, work focus schedules & screening",
                                        style = LineaTypography.bodySmall,
                                        color = LineaColors.TextSecondary
                                    )
                                }
                                Switch(
                                    checked = state.alwaysRing,
                                    onCheckedChange = { viewModel.toggleAlwaysRing() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = LineaColors.TitaniumBlue,
                                        checkedTrackColor = LineaColors.TitaniumBlue.copy(alpha = 0.3f),
                                        uncheckedThumbColor = LineaColors.TextSecondary,
                                        uncheckedTrackColor = LineaColors.GlassBorder
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Allow during restricted focus hours
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Allow During Work Focus",
                                        style = LineaTypography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = LineaColors.TextPrimary
                                    )
                                    Text(
                                        text = "Allowed through scheduled workday filtering rules",
                                        style = LineaTypography.bodySmall,
                                        color = LineaColors.TextSecondary
                                    )
                                }
                                Switch(
                                    checked = state.allowRestrictedHours,
                                    onCheckedChange = { viewModel.toggleAllowRestrictedHours() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = LineaColors.TitaniumBlue,
                                        checkedTrackColor = LineaColors.TitaniumBlue.copy(alpha = 0.3f),
                                        uncheckedThumbColor = LineaColors.TextSecondary,
                                        uncheckedTrackColor = LineaColors.GlassBorder
                                    )
                                )
                            }
                        }
                    }
                }

                // Pre-Call Note Card
                item {
                    FrostedGlassBox(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(LineaColors.Warning.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PushPin,
                                        contentDescription = null,
                                        tint = LineaColors.Warning,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Pre-Call Note",
                                        style = LineaTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = LineaColors.TextPrimary
                                    )
                                    Text(
                                        text = "Displayed on-screen during outgoing & active calls",
                                        style = LineaTypography.bodySmall,
                                        color = LineaColors.TextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = noteInput,
                                onValueChange = { noteInput = it },
                                placeholder = { Text("Enter agenda or discussion topic...", color = LineaColors.TextSecondary) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = LineaColors.TextPrimary,
                                    unfocusedTextColor = LineaColors.TextPrimary,
                                    focusedBorderColor = LineaColors.TitaniumBlue,
                                    unfocusedBorderColor = LineaColors.GlassBorder
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    viewModel.savePreCallNote(noteInput)
                                },
                                modifier = Modifier.align(Alignment.End),
                                colors = ButtonDefaults.buttonColors(containerColor = LineaColors.TitaniumBlue),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Save Note", style = LineaTypography.labelMedium)
                            }
                        }
                    }
                }

                // Recent Calls History
                if (state.callHistory.isNotEmpty()) {
                    item {
                        Text(
                            text = "Recent Calls with ${contact.displayName}",
                            style = LineaTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = LineaColors.TitaniumBlue,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(state.callHistory) { record ->
                        FrostedGlassBox(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val dateStr = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(record.timestamp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = dateStr,
                                        style = LineaTypography.bodyMedium,
                                        color = LineaColors.TextPrimary
                                    )
                                    Text(
                                        text = "${record.callType.name} • ${record.durationSeconds}s duration",
                                        style = LineaTypography.bodySmall,
                                        color = LineaColors.TextSecondary
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(LineaColors.GlassFill)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "SIM ${record.simSlot + 1}",
                                        style = LineaTypography.labelSmall,
                                        color = LineaColors.TitaniumBlue
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showPhotoDialog) {
            AlertDialog(
                onDismissRequest = { showPhotoDialog = false },
                title = {
                    Text(
                        text = "Contact Photo",
                        style = LineaTypography.titleMedium,
                        color = LineaColors.TextPrimary
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    showPhotoDialog = false
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

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    showPhotoDialog = false
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

                        if (!contact?.photoUri.isNullOrBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        showPhotoDialog = false
                                        viewModel.updateContactPhoto(null, null)
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
                    TextButton(onClick = { showPhotoDialog = false }) {
                        Text("Cancel", color = LineaColors.TextSecondary)
                    }
                },
                containerColor = LineaColors.BackgroundTop,
                shape = RoundedCornerShape(20.dp)
            )
        }

        if (isEditingContact && contact != null) {
            val contactNumbers = state.numbers.map { it.number to it.label }
            ContactCreateEditSheet(
                contactToEdit = contact,
                initialNumbers = if (contactNumbers.isNotEmpty()) contactNumbers else listOf("" to "Mobile"),
                onDismiss = { isEditingContact = false },
                onSave = { displayName, company, numbers, emails, preferredSimSlot, notes, photoUri, photoBytes ->
                    viewModel.updateContactPhoto(photoUri, photoBytes)
                    isEditingContact = false
                    viewModel.loadContactData(contactId)
                }
            )
        }
    }
}

@Composable
private fun DashboardActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f))
                .border(1.dp, color.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = LineaTypography.labelSmall,
            color = LineaColors.TextPrimary
        )
    }
}

@Composable
private fun SimChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) LineaColors.TitaniumBlue else LineaColors.GlassFill)
            .border(
                LineaDimensions.HairlineBorder,
                if (selected) LineaColors.TitaniumBlue else LineaColors.GlassBorder,
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = LineaTypography.labelSmall.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal),
            color = if (selected) Color.White else LineaColors.TextSecondary
        )
    }
}
