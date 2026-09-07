package com.ryanshelby.linea.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.ui.components.FrostedGlassBox
import com.ryanshelby.linea.ui.components.RoleBanner
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography

@Composable
fun SettingsScreen(
    isDefaultDialer: Boolean,
    reduceAnimations: Boolean,
    onToggleReduceAnimations: (Boolean) -> Unit,
    onRequestDefaultDialer: () -> Unit,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = LineaDimensions.ScreenPadding)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Settings",
            style = LineaTypography.headlineLarge,
            color = LineaColors.TextPrimary
        )
        Text(
            text = "Telecom configuration & accessibility",
            style = LineaTypography.bodyMedium,
            color = LineaColors.TextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Default Dialer Role Status
        RoleBanner(
            isDefaultDialer = isDefaultDialer,
            onRequestRole = onRequestDefaultDialer
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Accessibility & Motion Panel
        FrostedGlassBox(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(LineaDimensions.PanelPadding)) {
                Text(
                    text = "Motion & Accessibility",
                    style = LineaTypography.titleSmall,
                    color = LineaColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Reduce Animations",
                            style = LineaTypography.bodyLarge,
                            color = LineaColors.TextPrimary
                        )
                        Text(
                            text = "Swaps spring physics for fast cross-fades",
                            style = LineaTypography.bodyMedium,
                            color = LineaColors.TextSecondary
                        )
                    }

                    Switch(
                        checked = reduceAnimations,
                        onCheckedChange = onToggleReduceAnimations,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = LineaColors.TextPrimary,
                            checkedTrackColor = LineaColors.TitaniumBlue,
                            uncheckedThumbColor = LineaColors.TextSecondary,
                            uncheckedTrackColor = LineaColors.BackgroundTop
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // System Permissions Panel
        FrostedGlassBox(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(LineaDimensions.PanelPadding)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = null,
                        tint = LineaColors.TitaniumBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Permissions Architecture",
                        style = LineaTypography.titleSmall,
                        color = LineaColors.TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val permissions = listOf(
                    "Phone Calls & Telecom Role" to "Active / Declared",
                    "Call Log Read / Write" to "Active / Declared",
                    "Contacts Read / Write" to "Active / Declared",
                    "Audio Recording & Route" to "Active / Declared",
                    "Bluetooth Headset Connect" to "Active / Declared",
                    "Full Screen Incoming Call Intent" to "Active / Declared",
                    "Post System Notifications" to "Active / Declared"
                )

                permissions.forEach { (perm, status) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = perm,
                            style = LineaTypography.bodyMedium,
                            color = LineaColors.TextPrimary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = LineaColors.MutedSageGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = status,
                                fontSize = 12.sp,
                                color = LineaColors.MutedSageGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onRequestPermissions,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(LineaDimensions.ButtonCornerRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LineaColors.GlassFill,
                        contentColor = LineaColors.TextPrimary
                    )
                ) {
                    Text(
                        text = "Re-request Runtime Permissions",
                        style = LineaTypography.bodyLarge
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(110.dp))
    }
}
