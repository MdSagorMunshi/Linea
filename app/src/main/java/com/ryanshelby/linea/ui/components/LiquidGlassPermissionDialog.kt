package com.ryanshelby.linea.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaTypography

/** A compact, app-styled replacement for platform Material permission alerts. */
@Composable
fun LiquidGlassPermissionDialog(
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    dismissible: Boolean,
    onDismiss: () -> Unit = {}
) {
    Dialog(
        onDismissRequest = { if (dismissible) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        FrostedGlassBox(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .clip(RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            borderWidth = 1.dp,
            borderColor = LineaColors.GlassBorderFocused,
            fillColor = LineaColors.GlassFill.copy(alpha = 0.22f),
            fallbackFillColor = LineaColors.SurfaceElevated
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                LineaColors.TitaniumBlue.copy(alpha = 0.15f),
                                LineaColors.GlassFill.copy(alpha = 0.03f)
                            )
                        )
                    )
                    .padding(horizontal = 22.dp, vertical = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(LineaColors.TitaniumBlue.copy(alpha = 0.18f))
                            .border(1.dp, LineaColors.TitaniumBlue.copy(alpha = 0.45f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            tint = LineaColors.TitaniumBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.size(12.dp))
                    Text(title, style = LineaTypography.titleLarge, color = LineaColors.TextPrimary)
                }

                Spacer(Modifier.height(16.dp))
                Text(message, style = LineaTypography.bodyMedium, color = LineaColors.TextSecondary)
                Spacer(Modifier.height(22.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = onAction,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LineaColors.TitaniumBlue,
                            contentColor = LineaColors.TextPrimary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text(actionLabel) }
                }
            }
        }
    }
}
