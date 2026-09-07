package com.ryanshelby.linea.ui.screens.dialpad

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ryanshelby.linea.ui.components.FrostedGlassBox
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography
import kotlinx.coroutines.delay

@Composable
fun CallCountdownDialog(
    phoneNumber: String,
    totalSeconds: Int = 3,
    onConfirmCall: () -> Unit,
    onCancel: () -> Unit
) {
    val duration = if (totalSeconds > 0) totalSeconds else 3
    var secondsRemaining by remember { mutableIntStateOf(duration) }

    LaunchedEffect(Unit) {
        while (secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining -= 1
        }
        onConfirmCall()
    }

    Dialog(onDismissRequest = onCancel) {
        FrostedGlassBox(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Confirm Call",
                    style = LineaTypography.titleLarge,
                    color = LineaColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = phoneNumber,
                    style = LineaTypography.headlineMedium,
                    color = LineaColors.TitaniumBlue
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Circular countdown
                Box(
                    modifier = Modifier.size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { secondsRemaining.toFloat() / duration.toFloat() },
                        modifier = Modifier.size(80.dp),
                        color = LineaColors.TitaniumBlue,
                        trackColor = LineaColors.GlassFill,
                        strokeWidth = 4.dp
                    )
                    Text(
                        text = "$secondsRemaining",
                        style = LineaTypography.headlineLarge,
                        color = LineaColors.TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(LineaDimensions.ButtonCornerRadius)
                    ) {
                        Text(
                            text = "Cancel",
                            style = LineaTypography.labelLarge,
                            color = LineaColors.CarmineRed
                        )
                    }

                    Button(
                        onClick = onConfirmCall,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(LineaDimensions.ButtonCornerRadius),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LineaColors.TitaniumBlue,
                            contentColor = LineaColors.TextPrimary
                        )
                    ) {
                        Text(
                            text = "Call Now",
                            style = LineaTypography.labelLarge,
                            color = LineaColors.TextPrimary
                        )
                    }
                }
            }
        }
    }
}
