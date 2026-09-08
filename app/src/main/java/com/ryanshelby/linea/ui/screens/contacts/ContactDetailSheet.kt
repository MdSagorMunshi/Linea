package com.ryanshelby.linea.ui.screens.contacts

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.PushPin
import com.ryanshelby.linea.data.local.entities.CallNoteEntity
import com.ryanshelby.linea.data.local.entities.ContactEmailEntity
import com.ryanshelby.linea.data.local.entities.ContactEntity
import com.ryanshelby.linea.data.local.entities.ContactNumberEntity
import com.ryanshelby.linea.ui.components.FrostedGlassBox
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailSheet(
    contact: ContactEntity,
    numbers: List<ContactNumberEntity>,
    emails: List<ContactEmailEntity>,
    onDismiss: () -> Unit,
    onCallNumber: (String, Int?) -> Unit,
    onToggleFavorite: (ContactEntity) -> Unit,
    onToggleRuleOverride: (ContactEntity, Boolean) -> Unit,
    onEditContact: (ContactEntity) -> Unit,
    onDeleteContact: (ContactEntity) -> Unit,
    preCallNote: CallNoteEntity? = null,
    onSetPreCallNote: ((String) -> Unit)? = null,
    onClearPreCallNote: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()
    val context = LocalContext.current

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
            // Top Bar: Dismiss, Favorite, Edit, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = LineaColors.TextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onToggleFavorite(contact) }) {
                        Icon(
                            imageVector = if (contact.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (contact.isFavorite) LineaColors.MutedRust else LineaColors.TextSecondary
                        )
                    }

                    IconButton(onClick = { onEditContact(contact) }) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit",
                            tint = LineaColors.TitaniumBlue
                        )
                    }

                    IconButton(onClick = {
                        onDeleteContact(contact)
                        onDismiss()
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = LineaColors.Danger
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Contact Avatar & Name Header
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FrostedGlassBox(
                    modifier = Modifier.size(88.dp),
                    shape = CircleShape,
                    borderColor = LineaColors.TitaniumBlue.copy(alpha = 0.5f)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = LineaColors.TextSecondary,
                            modifier = Modifier.size(46.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = contact.displayName,
                    style = LineaTypography.headlineMedium,
                    color = LineaColors.TextPrimary
                )

                if (!contact.company.isNullOrBlank()) {
                    Text(
                        text = contact.company,
                        style = LineaTypography.bodyMedium,
                        color = LineaColors.TitaniumBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Actions: Call, SMS, Share
            val primaryNumber = numbers.firstOrNull()?.number ?: ""
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ContactActionButton(
                    icon = Icons.Filled.Call,
                    label = "Call",
                    tint = LineaColors.TitaniumBlue,
                    onClick = {
                        if (primaryNumber.isNotBlank()) {
                            onCallNumber(primaryNumber, contact.preferredSimSlot)
                        }
                    }
                )

                ContactActionButton(
                    icon = Icons.Filled.Message,
                    label = "SMS",
                    tint = LineaColors.TextPrimary,
                    onClick = {
                        if (primaryNumber.isNotBlank()) {
                            val smsIntent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("sms:$primaryNumber")
                            }
                            context.startActivity(smsIntent)
                        }
                    }
                )

                ContactActionButton(
                    icon = Icons.Filled.Share,
                    label = "Share",
                    tint = LineaColors.TextPrimary,
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "${contact.displayName}: $primaryNumber")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Contact"))
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Phone Numbers Card
            FrostedGlassBox(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Phone Numbers",
                        style = LineaTypography.labelSmall,
                        color = LineaColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (numbers.isEmpty()) {
                        Text(
                            text = "No phone numbers added",
                            style = LineaTypography.bodyMedium,
                            color = LineaColors.TextTertiary
                        )
                    } else {
                        numbers.forEach { numEntity ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
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
                                        if (contact.preferredSimSlot != null) {
                                            Text(
                                                text = " • SIM ${contact.preferredSimSlot + 1} Affinity",
                                                style = LineaTypography.bodySmall,
                                                color = LineaColors.TitaniumBlue
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { onCallNumber(numEntity.number, contact.preferredSimSlot) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Call,
                                        contentDescription = "Call",
                                        tint = LineaColors.TitaniumBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Emails Card
            if (emails.isNotEmpty()) {
                FrostedGlassBox(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Email",
                            style = LineaTypography.labelSmall,
                            color = LineaColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        emails.forEach { emailEntity ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Email,
                                    contentDescription = null,
                                    tint = LineaColors.TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = emailEntity.email,
                                    style = LineaTypography.bodyMedium,
                                    color = LineaColors.TextPrimary
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Rule Override: Always Allow Calls Through
            FrostedGlassBox(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Rule Override",
                            style = LineaTypography.titleSmall,
                            color = LineaColors.TextPrimary
                        )
                        Text(
                            text = "Always allow calls through (bypasses quiet hours & screening)",
                            style = LineaTypography.bodySmall,
                            color = LineaColors.TextSecondary
                        )
                    }

                    Switch(
                        checked = contact.alwaysRing,
                        onCheckedChange = { onToggleRuleOverride(contact, it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = LineaColors.TitaniumBlue,
                            uncheckedThumbColor = LineaColors.TextSecondary,
                            uncheckedTrackColor = LineaColors.GlassFill
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Pre-Call Note Section
            FrostedGlassBox(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                borderColor = LineaColors.AccentAmber.copy(alpha = 0.35f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(LineaColors.AccentAmber.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PushPin,
                                    contentDescription = null,
                                    tint = LineaColors.AccentAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Pre-Call Note",
                                    style = LineaTypography.titleSmall,
                                    color = LineaColors.TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Shows on-screen when calling this contact",
                                    style = LineaTypography.bodySmall,
                                    color = LineaColors.TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        if (preCallNote != null) {
                            IconButton(
                                onClick = { onClearPreCallNote?.invoke() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear Note",
                                    tint = LineaColors.TextTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    if (preCallNote != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(LineaColors.SurfaceElevated)
                                .border(1.dp, LineaColors.AccentAmber.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = preCallNote.noteText,
                                style = LineaTypography.bodyMedium,
                                color = LineaColors.TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    var preCallInput by remember { mutableStateOf("") }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = preCallInput,
                            onValueChange = { preCallInput = it },
                            placeholder = {
                                Text(
                                    text = if (preCallNote != null) "Update note..." else "Attach a note before calling...",
                                    style = LineaTypography.bodySmall,
                                    color = LineaColors.TextTertiary,
                                    fontSize = 12.sp
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LineaColors.AccentAmber,
                                unfocusedBorderColor = LineaColors.GlassBorder,
                                focusedTextColor = LineaColors.TextPrimary,
                                unfocusedTextColor = LineaColors.TextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (preCallInput.isNotBlank()) LineaColors.AccentAmber else LineaColors.GlassFill)
                                .clickable(enabled = preCallInput.isNotBlank()) {
                                    onSetPreCallNote?.invoke(preCallInput)
                                    preCallInput = ""
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PushPin,
                                contentDescription = "Set Pre-Call Note",
                                tint = if (preCallInput.isNotBlank()) Color.Black else LineaColors.TextTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Notes
            if (!contact.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                FrostedGlassBox(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Notes",
                            style = LineaTypography.labelSmall,
                            color = LineaColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = contact.notes,
                            style = LineaTypography.bodyMedium,
                            color = LineaColors.TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun ContactActionButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(LineaColors.GlassFill)
                .border(LineaDimensions.HairlineBorder, LineaColors.GlassBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = LineaTypography.labelSmall,
            color = LineaColors.TextSecondary
        )
    }
}
