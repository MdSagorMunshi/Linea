package com.ryanshelby.linea.ui.screens.history

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.ui.components.FrostedGlassBox
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaTypography

/**
 * Compact bottom sheet displayed when tapping and holding a contact or number on the Recent page for 1 second.
 * Displays contact info and a tap-to-copy card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentItemOptionsSheet(
    contactName: String?,
    phoneNumber: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onCall: (() -> Unit)? = null,
    onSms: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val copyNumberAction: () -> Unit = {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Phone Number", phoneNumber)
        clipboard.setPrimaryClip(clip)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        Toast.makeText(context, "Copied $phoneNumber to clipboard", Toast.LENGTH_SHORT).show()
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LineaColors.BackgroundTop,
        contentColor = LineaColors.TextPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(LineaColors.GlassBorder.copy(alpha = 0.6f))
            )
        }
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Contact Avatar & Name
            val initials = (contactName ?: "").split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .map { it.first().uppercase() }
                .joinToString("")

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(LineaColors.GlassFill)
                        .border(1.dp, LineaColors.TitaniumBlue.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (initials.isNotEmpty()) {
                        Text(
                            text = initials,
                            style = LineaTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = LineaColors.TitaniumBlue
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = LineaColors.TitaniumBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = contactName ?: phoneNumber,
                    style = LineaTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = LineaColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Phone Number Card (Tap anywhere to copy immediately)
            FrostedGlassBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(onClick = copyNumberAction),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PHONE NUMBER",
                            style = LineaTypography.labelSmall.copy(
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = LineaColors.TextTertiary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = phoneNumber,
                            style = LineaTypography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFeatureSettings = "tnum"
                            ),
                            color = LineaColors.TextPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(LineaColors.TitaniumBlue.copy(alpha = 0.15f))
                            .border(
                                1.dp,
                                LineaColors.TitaniumBlue.copy(alpha = 0.35f),
                                RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Copy",
                                tint = LineaColors.TitaniumBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Copy",
                                style = LineaTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = LineaColors.TitaniumBlue
                            )
                        }
                    }
                }
            }
        }
    }
}
