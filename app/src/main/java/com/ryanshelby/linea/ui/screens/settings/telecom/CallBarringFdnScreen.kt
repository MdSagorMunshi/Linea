package com.ryanshelby.linea.ui.screens.settings.telecom

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.PublicOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import com.ryanshelby.linea.ui.components.FrostedGlassBox
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaTypography

@Composable
fun CallBarringFdnScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    // Call Barring states
    var barAllOutgoing by remember { mutableStateOf(false) }
    var barInternationalOutgoing by remember { mutableStateOf(false) }
    var barRoamingOutgoing by remember { mutableStateOf(false) }
    var barAllIncoming by remember { mutableStateOf(false) }
    var barRoamingIncoming by remember { mutableStateOf(false) }

    // FDN state
    var fdnEnabled by remember { mutableStateOf(false) }

    fun sendMmiIntent(code: String) {
        val encodedHash = Uri.encode("#")
        val uri = Uri.parse("tel:" + code.replace("#", encodedHash))
        val intent = Intent(Intent.ACTION_DIAL, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LineaColors.BackgroundGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = LineaColors.TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = "Call Barring & FDN",
                        style = LineaTypography.headlineMedium,
                        color = LineaColors.TextPrimary
                    )
                    Text(
                        text = "Carrier call restrictions & Fixed Dialing Numbers",
                        style = LineaTypography.bodySmall,
                        color = LineaColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section: Call Barring
                Text(
                    text = "NETWORK CALL BARRING",
                    style = LineaTypography.titleSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = LineaColors.TitaniumBlue
                )

                BarringItemCard(
                    title = "All Outgoing Calls",
                    subtitle = "Block placing any outgoing calls",
                    icon = Icons.Filled.Block,
                    isChecked = barAllOutgoing,
                    onToggle = {
                        barAllOutgoing = it
                        sendMmiIntent(if (it) "*33*0000#" else "#33*0000#")
                    }
                )

                BarringItemCard(
                    title = "International Outgoing",
                    subtitle = "Block placing calls to international numbers",
                    icon = Icons.Filled.PublicOff,
                    isChecked = barInternationalOutgoing,
                    onToggle = {
                        barInternationalOutgoing = it
                        sendMmiIntent(if (it) "*331*0000#" else "#331*0000#")
                    }
                )

                BarringItemCard(
                    title = "International While Roaming",
                    subtitle = "Block calls except to home country when abroad",
                    icon = Icons.Filled.Public,
                    isChecked = barRoamingOutgoing,
                    onToggle = {
                        barRoamingOutgoing = it
                        sendMmiIntent(if (it) "*332*0000#" else "#332*0000#")
                    }
                )

                BarringItemCard(
                    title = "All Incoming Calls",
                    subtitle = "Block all incoming calls on this SIM",
                    icon = Icons.Filled.CallReceived,
                    isChecked = barAllIncoming,
                    onToggle = {
                        barAllIncoming = it
                        sendMmiIntent(if (it) "*35*0000#" else "#35*0000#")
                    }
                )

                BarringItemCard(
                    title = "Incoming When Roaming",
                    subtitle = "Block incoming calls when connected to foreign carriers",
                    icon = Icons.Filled.PublicOff,
                    isChecked = barRoamingIncoming,
                    onToggle = {
                        barRoamingIncoming = it
                        sendMmiIntent(if (it) "*351*0000#" else "#351*0000#")
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Section: Fixed Dialing Numbers (FDN)
                Text(
                    text = "FIXED DIALING NUMBERS (FDN)",
                    style = LineaTypography.titleSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = LineaColors.TitaniumBlue
                )

                FrostedGlassBox(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    borderColor = if (fdnEnabled) LineaColors.Warning.copy(alpha = 0.5f) else LineaColors.GlassBorder
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (fdnEnabled) LineaColors.Warning.copy(alpha = 0.2f) else LineaColors.GlassFill
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Security,
                                    contentDescription = null,
                                    tint = if (fdnEnabled) LineaColors.Warning else LineaColors.TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "FDN Mode",
                                    style = LineaTypography.titleMedium,
                                    color = LineaColors.TextPrimary
                                )
                                Text(
                                    text = if (fdnEnabled) "Restricted to authorized numbers" else "Disabled (all dials permitted)",
                                    style = LineaTypography.bodySmall,
                                    color = if (fdnEnabled) LineaColors.Warning else LineaColors.TextSecondary
                                )
                            }

                            Switch(
                                checked = fdnEnabled,
                                onCheckedChange = { fdnEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = LineaColors.Warning,
                                    uncheckedThumbColor = LineaColors.TextTertiary,
                                    uncheckedTrackColor = LineaColors.GlassFill
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = LineaColors.TextTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Requires SIM PIN2 code from your network operator.",
                                style = LineaTypography.bodySmall.copy(fontSize = 12.sp),
                                color = LineaColors.TextTertiary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun BarringItemCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isChecked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    FrostedGlassBox(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        borderColor = if (isChecked) LineaColors.Danger.copy(alpha = 0.5f) else LineaColors.GlassBorder
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isChecked) LineaColors.Danger.copy(alpha = 0.2f) else LineaColors.GlassFill
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isChecked) LineaColors.Danger else LineaColors.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = LineaTypography.titleMedium,
                    color = LineaColors.TextPrimary
                )
                Text(
                    text = subtitle,
                    style = LineaTypography.bodySmall,
                    color = LineaColors.TextSecondary
                )
            }

            Switch(
                checked = isChecked,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = LineaColors.Danger,
                    uncheckedThumbColor = LineaColors.TextTertiary,
                    uncheckedTrackColor = LineaColors.GlassFill
                )
            )
        }
    }
}
