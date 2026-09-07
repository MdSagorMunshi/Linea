package com.ryanshelby.linea.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography

@Composable
fun RoleBanner(
    isDefaultDialer: Boolean,
    onRequestRole: () -> Unit,
    modifier: Modifier = Modifier
) {
    FrostedGlassBox(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LineaDimensions.PanelCornerRadius),
        borderColor = if (isDefaultDialer) LineaColors.MutedSageGreen.copy(alpha = 0.3f) else LineaColors.MutedRust.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LineaDimensions.PanelPadding)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isDefaultDialer) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                        contentDescription = null,
                        tint = if (isDefaultDialer) LineaColors.MutedSageGreen else LineaColors.MutedRust,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isDefaultDialer) "Default Phone App Active" else "Default Dialer Required",
                            style = LineaTypography.titleSmall,
                            color = LineaColors.TextPrimary
                        )
                        Text(
                            text = if (isDefaultDialer) "Handling real cellular calls" else "Tap to enable system call routing",
                            style = LineaTypography.bodyMedium,
                            color = LineaColors.TextSecondary
                        )
                    }
                }
            }

            if (!isDefaultDialer) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onRequestRole,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(LineaDimensions.ButtonCornerRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LineaColors.TitaniumBlue,
                        contentColor = LineaColors.TextPrimary
                    )
                ) {
                    Text(
                        text = "Set as Default Dialer",
                        style = LineaTypography.titleSmall
                    )
                }
            }
        }
    }
}
