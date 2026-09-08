package com.ryanshelby.linea.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography

@Composable
fun PrivatePinDialog(
    correctPin: String = "1234",
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        FrostedGlassBox(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(LineaColors.TitaniumBlue.copy(alpha = 0.15f))
                        .border(1.dp, LineaColors.TitaniumBlue.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = LineaColors.TitaniumBlue,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Private Contacts",
                    style = LineaTypography.titleMedium,
                    color = LineaColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isError) "Incorrect PIN. Try again." else "Enter 4-digit security PIN to unlock",
                    style = LineaTypography.bodySmall,
                    color = if (isError) LineaColors.Danger else LineaColors.TextSecondary
                )

                Spacer(modifier = Modifier.height(20.dp))

                // PIN Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 4) {
                        val isFilled = i < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isFilled) {
                                        if (isError) LineaColors.Danger else LineaColors.TitaniumBlue
                                    } else {
                                        LineaColors.GlassBorder
                                    }
                                )
                                .border(
                                    1.dp,
                                    if (isFilled) LineaColors.TitaniumBlue else LineaColors.GlassBorder,
                                    CircleShape
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Number Pad (1-9, Clear, 0, Backspace)
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "DEL")
                )

                keys.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { key ->
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(LineaColors.GlassFill)
                                    .border(LineaDimensions.HairlineBorder, LineaColors.GlassBorder, CircleShape)
                                    .clickable {
                                        when (key) {
                                            "C" -> {
                                                enteredPin = ""
                                                isError = false
                                            }
                                            "DEL" -> {
                                                if (enteredPin.isNotEmpty()) {
                                                    enteredPin = enteredPin.dropLast(1)
                                                    isError = false
                                                }
                                            }
                                            else -> {
                                                if (enteredPin.length < 4) {
                                                    val newPin = enteredPin + key
                                                    enteredPin = newPin
                                                    isError = false
                                                    if (newPin.length == 4) {
                                                        if (newPin == correctPin) {
                                                            onSuccess()
                                                        } else {
                                                            isError = true
                                                            enteredPin = ""
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (key == "DEL") {
                                    Icon(
                                        imageVector = Icons.Filled.Backspace,
                                        contentDescription = "Backspace",
                                        tint = LineaColors.TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Text(
                                        text = key,
                                        style = LineaTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (key == "C") LineaColors.Warning else LineaColors.TextPrimary
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Cancel",
                        style = LineaTypography.bodyMedium,
                        color = LineaColors.TextSecondary
                    )
                }
            }
        }
    }
}
