package com.ryanshelby.linea.ui.incall

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.telecom.PostCallSummary
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaTypography
import kotlinx.coroutines.delay
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PostCallHudCard(
    summary: PostCallSummary,
    initialDurationSeconds: Int = 8,
    onDismiss: () -> Unit,
    onScheduleReminder: (delayMs: Long, label: String) -> Unit,
    onSaveNote: (noteText: String) -> Unit,
    onSendSms: (text: String) -> Unit,
    onBlockNumber: () -> Unit,
    modifier: Modifier = Modifier
) {
    var remainingSeconds by remember { mutableIntStateOf(initialDurationSeconds.coerceAtLeast(3)) }
    var isTimerPaused by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf("") }
    var isNoteSaved by remember { mutableStateOf(false) }
    var scheduledReminderLabel by remember { mutableStateOf<String?>(null) }
    var isNumberBlocked by remember { mutableStateOf(false) }
    var smsSentLabel by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // Countdown timer
    LaunchedEffect(isTimerPaused, remainingSeconds) {
        if (!isTimerPaused && remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds -= 1
            if (remainingSeconds <= 0) {
                onDismiss()
            }
        }
    }

    val progressAnim by animateFloatAsState(
        targetValue = if (initialDurationSeconds > 0) remainingSeconds.toFloat() / initialDurationSeconds.toFloat() else 0f,
        animationSpec = tween(durationMillis = 950),
        label = "hudCountdownProgress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(LineaColors.SurfaceElevated.copy(alpha = 0.96f))
            .border(1.dp, LineaColors.GlassBorder.copy(alpha = 0.4f), RoundedCornerShape(28.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(scrollState)
        ) {
            // 1. Top Countdown Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isTimerPaused) {
                        Icon(
                            imageVector = Icons.Filled.Pause,
                            contentDescription = "Paused",
                            tint = LineaColors.TitaniumBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Timer paused",
                            style = LineaTypography.labelSmall,
                            color = LineaColors.TitaniumBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = "Auto-closing in ${remainingSeconds}s",
                            style = LineaTypography.labelSmall,
                            color = LineaColors.TextTertiary
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Dismiss",
                        tint = LineaColors.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { progressAnim },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = if (isTimerPaused) LineaColors.TitaniumBlue else LineaColors.TitaniumBlue,
                trackColor = LineaColors.GlassFill
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Ended Call Summary Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar Circle
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            if (summary.isIncoming) {
                                if (summary.wasConnected) LineaColors.AccentGreen.copy(alpha = 0.18f)
                                else LineaColors.Danger.copy(alpha = 0.18f)
                            } else {
                                LineaColors.TitaniumBlue.copy(alpha = 0.18f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = summary.displayTitle.take(1).uppercase(),
                        style = LineaTypography.titleLarge,
                        color = if (summary.isIncoming) {
                            if (summary.wasConnected) LineaColors.AccentGreen else LineaColors.Danger
                        } else LineaColors.TitaniumBlue,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.displayTitle,
                        style = LineaTypography.titleMedium,
                        color = LineaColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )

                    if (summary.callerName != null && summary.callerName.isNotBlank()) {
                        Text(
                            text = summary.phoneNumber,
                            style = LineaTypography.bodySmall,
                            color = LineaColors.TextSecondary,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Direction & Duration Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val (dirIcon, dirColor, dirText) = when {
                            !summary.wasConnected && summary.isIncoming -> Triple(
                                Icons.Filled.CallMissed,
                                LineaColors.Danger,
                                "Missed Call"
                            )
                            summary.isIncoming -> Triple(
                                Icons.Filled.CallReceived,
                                LineaColors.AccentGreen,
                                "Incoming • ${summary.formattedDuration}"
                            )
                            else -> Triple(
                                Icons.Filled.CallMade,
                                LineaColors.TitaniumBlue,
                                "Outgoing • ${summary.formattedDuration}"
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(dirColor.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = dirIcon,
                                    contentDescription = null,
                                    tint = dirColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = dirText,
                                    style = LineaTypography.labelSmall,
                                    color = dirColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Regional Location Badge from Offline Caller ID
                        summary.callerIdResult?.let { cid ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(LineaColors.GlassFill)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${cid.flagEmoji} ${cid.regionOrCountry}",
                                    style = LineaTypography.labelSmall,
                                    color = LineaColors.TextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. Section: Callback Reminder
            Text(
                text = "CALLBACK REMINDER",
                style = LineaTypography.labelSmall,
                color = LineaColors.TextTertiary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (scheduledReminderLabel != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(LineaColors.AccentGreen.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = LineaColors.AccentGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Reminder set for $scheduledReminderLabel",
                        style = LineaTypography.bodySmall,
                        color = LineaColors.AccentGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickReminderChip(
                        label = "15 min",
                        onClick = {
                            isTimerPaused = true
                            scheduledReminderLabel = "in 15 min"
                            onScheduleReminder(15 * 60 * 1000L, "in 15 min")
                        },
                        modifier = Modifier.weight(1f)
                    )
                    QuickReminderChip(
                        label = "1 hour",
                        onClick = {
                            isTimerPaused = true
                            scheduledReminderLabel = "in 1 hour"
                            onScheduleReminder(60 * 60 * 1000L, "in 1 hour")
                        },
                        modifier = Modifier.weight(1f)
                    )
                    QuickReminderChip(
                        label = "Tomorrow 9 AM",
                        onClick = {
                            isTimerPaused = true
                            val now = Calendar.getInstance()
                            val target = Calendar.getInstance().apply {
                                add(Calendar.DAY_OF_YEAR, 1)
                                set(Calendar.HOUR_OF_DAY, 9)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            val delayMs = (target.timeInMillis - now.timeInMillis).coerceAtLeast(60 * 1000L)
                            scheduledReminderLabel = "Tomorrow 9:00 AM"
                            onScheduleReminder(delayMs, "Tomorrow 9:00 AM")
                        },
                        modifier = Modifier.weight(1.3f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 4. Section: Scratchpad Call Note
            Text(
                text = "SCRATCHPAD NOTE",
                style = LineaTypography.labelSmall,
                color = LineaColors.TextTertiary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick note tag chips
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Follow-up needed", "Sent details", "Call again", "Important").forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(LineaColors.GlassFill)
                            .clickable {
                                isTimerPaused = true
                                noteText = if (noteText.isBlank()) tag else "$noteText • $tag"
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = tag,
                            style = LineaTypography.labelSmall,
                            color = LineaColors.TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isNoteSaved) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(LineaColors.AccentGreen.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = LineaColors.AccentGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Note saved to call history",
                        style = LineaTypography.bodySmall,
                        color = LineaColors.AccentGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = {
                        noteText = it
                        isTimerPaused = true
                    },
                    placeholder = {
                        Text(
                            text = "Add note for this call...",
                            style = LineaTypography.bodySmall,
                            color = LineaColors.TextTertiary
                        )
                    },
                    trailingIcon = {
                        if (noteText.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    isTimerPaused = true
                                    isNoteSaved = true
                                    focusManager.clearFocus()
                                    onSaveNote(noteText.trim())
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Save Note",
                                    tint = LineaColors.AccentGreen
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (noteText.isNotBlank()) {
                                isTimerPaused = true
                                isNoteSaved = true
                                focusManager.clearFocus()
                                onSaveNote(noteText.trim())
                            }
                        }
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = LineaColors.TextPrimary,
                        unfocusedTextColor = LineaColors.TextPrimary,
                        focusedBorderColor = LineaColors.TitaniumBlue,
                        unfocusedBorderColor = LineaColors.GlassBorder,
                        focusedContainerColor = LineaColors.NeuSurfaceSunken,
                        unfocusedContainerColor = LineaColors.NeuSurfaceSunken
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 5. Section: Quick Follow-up SMS
            Text(
                text = "FOLLOW-UP SMS",
                style = LineaTypography.labelSmall,
                color = LineaColors.TextTertiary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (smsSentLabel != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(LineaColors.TitaniumBlue.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Message,
                        contentDescription = null,
                        tint = LineaColors.TitaniumBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Follow-up SMS opened: \"$smsSentLabel\"",
                        style = LineaTypography.bodySmall,
                        color = LineaColors.TitaniumBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "Thanks for the call!",
                        "I'll follow up shortly",
                        "Sending details now"
                    ).forEach { msg ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(LineaColors.GlassFill)
                                .border(1.dp, LineaColors.GlassBorder.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .clickable {
                                    isTimerPaused = true
                                    smsSentLabel = msg
                                    onSendSms(msg)
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = msg,
                                style = LineaTypography.labelSmall,
                                color = LineaColors.TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 6. Section: Block Number & Dismiss Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isNumberBlocked) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(LineaColors.Danger.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✓ Number Blocked",
                            style = LineaTypography.titleSmall,
                            color = LineaColors.Danger,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            isTimerPaused = true
                            isNumberBlocked = true
                            onBlockNumber()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LineaColors.Danger.copy(alpha = 0.12f),
                            contentColor = LineaColors.Danger
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Block,
                            contentDescription = null,
                            tint = LineaColors.Danger,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Block",
                            style = LineaTypography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = LineaColors.Danger
                        )
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LineaColors.TitaniumBlue,
                        contentColor = LineaColors.TextPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.4f)
                        .height(44.dp)
                ) {
                    Text(
                        text = if (isTimerPaused) "Done" else "Dismiss (${remainingSeconds}s)",
                        style = LineaTypography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = LineaColors.TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickReminderChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(LineaColors.GlassFill)
            .border(1.dp, LineaColors.GlassBorder.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Alarm,
                contentDescription = null,
                tint = LineaColors.TitaniumBlue,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = LineaTypography.labelSmall,
                color = LineaColors.TextPrimary,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}
