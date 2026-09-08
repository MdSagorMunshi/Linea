package com.ryanshelby.linea.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography
import java.util.Calendar

enum class ReminderPreset(val label: String) {
    TEN_MINUTES("10 min"),
    THIRTY_MINUTES("30 min"),
    ONE_HOUR("1 hour"),
    TONIGHT_8PM("Tonight (8:00 PM)"),
    TOMORROW_9AM("Tomorrow (9:00 AM)"),
    CUSTOM("Custom")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CallbackReminderSheet(
    sheetState: SheetState,
    phoneNumber: String,
    callerName: String?,
    onDismiss: () -> Unit,
    onScheduleReminder: (delayMs: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPreset by remember { mutableStateOf(ReminderPreset.THIRTY_MINUTES) }
    var customMinutesInput by remember { mutableStateOf("45") }

    fun calculateDelayMs(preset: ReminderPreset): Long {
        val now = Calendar.getInstance()
        return when (preset) {
            ReminderPreset.TEN_MINUTES -> 10 * 60 * 1000L
            ReminderPreset.THIRTY_MINUTES -> 30 * 60 * 1000L
            ReminderPreset.ONE_HOUR -> 60 * 60 * 1000L
            ReminderPreset.TONIGHT_8PM -> {
                val target = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 20)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (target.before(now)) {
                    target.add(Calendar.DAY_OF_YEAR, 1)
                }
                (target.timeInMillis - now.timeInMillis).coerceAtLeast(60 * 1000L)
            }
            ReminderPreset.TOMORROW_9AM -> {
                val target = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 9)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                (target.timeInMillis - now.timeInMillis).coerceAtLeast(60 * 1000L)
            }
            ReminderPreset.CUSTOM -> {
                val mins = customMinutesInput.toLongOrNull() ?: 30L
                (mins * 60 * 1000L).coerceAtLeast(60 * 1000L)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LineaColors.SurfaceElevated,
        dragHandle = null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
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
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(LineaColors.TitaniumBlue.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            tint = LineaColors.TitaniumBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Callback Reminder",
                            style = LineaTypography.titleMedium,
                            color = LineaColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = callerName ?: phoneNumber,
                            style = LineaTypography.labelSmall,
                            color = LineaColors.TextSecondary
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = LineaColors.TextTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "When should LINEA remind you?",
                style = LineaTypography.bodyMedium,
                color = LineaColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Presets grid
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReminderPreset.entries.forEach { preset ->
                    val isSelected = selectedPreset == preset
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) LineaColors.TitaniumBlue.copy(alpha = 0.2f)
                                else LineaColors.GlassFill
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) LineaColors.TitaniumBlue
                                else LineaColors.GlassBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedPreset = preset }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = preset.label,
                            style = LineaTypography.bodyMedium,
                            color = if (isSelected) LineaColors.TitaniumBlue else LineaColors.TextPrimary,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            if (selectedPreset == ReminderPreset.CUSTOM) {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = customMinutesInput,
                    onValueChange = { if (it.all { c -> c.isDigit() }) customMinutesInput = it },
                    label = { Text("Minutes from now") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = LineaColors.TextPrimary,
                        unfocusedTextColor = LineaColors.TextPrimary,
                        focusedBorderColor = LineaColors.TitaniumBlue,
                        unfocusedBorderColor = LineaColors.GlassBorder,
                        focusedLabelColor = LineaColors.TitaniumBlue,
                        unfocusedLabelColor = LineaColors.TextTertiary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val delay = calculateDelayMs(selectedPreset)
                    onScheduleReminder(delay)
                },
                colors = ButtonDefaults.buttonColors(containerColor = LineaColors.TitaniumBlue),
                shape = RoundedCornerShape(LineaDimensions.ButtonCornerRadius),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Alarm,
                    contentDescription = null,
                    tint = LineaColors.TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Schedule Reminder",
                    style = LineaTypography.titleSmall,
                    color = LineaColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
