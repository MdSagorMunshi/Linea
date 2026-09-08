package com.ryanshelby.linea.ui.screens.settings.permissions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ryanshelby.linea.ui.components.FrostedGlassBox
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography

data class PermissionItem(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val permissionKey: String? = null,
    val isCustomCheck: ((Context) -> Boolean)? = null,
    val customAction: ((Context) -> Unit)? = null,
    val isOptional: Boolean = false
)

@Composable
fun PermissionsScreen(
    isDefaultDialer: Boolean,
    onRequestDefaultDialer: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val permissionItems = listOf(
        PermissionItem(
            name = "Default Phone Dialer Role",
            description = "Handles cellular calls, incoming lockscreen UI, and telecom routing",
            icon = Icons.Filled.Security,
            isCustomCheck = { isDefaultDialer },
            customAction = { onRequestDefaultDialer() }
        ),
        PermissionItem(
            name = "Display Over Other Apps (Optional)",
            description = "Optional: enables custom Mini Call Float card above active apps (not required for standard call notifications)",
            icon = Icons.Filled.Settings,
            isCustomCheck = { Settings.canDrawOverlays(it) },
            customAction = { ctx ->
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${ctx.packageName}")
                )
                ctx.startActivity(intent)
            },
            isOptional = true
        ),
        PermissionItem(
            name = "Phone State & Calls",
            description = "Initiates outgoing calls and monitors telephony network status",
            icon = Icons.Filled.Phone,
            permissionKey = android.Manifest.permission.READ_PHONE_STATE
        ),
        PermissionItem(
            name = "Call Log Read & Write",
            description = "Synchronizes and preserves historical call logs on device",
            icon = Icons.Filled.History,
            permissionKey = android.Manifest.permission.READ_CALL_LOG
        ),
        PermissionItem(
            name = "Contacts Read & Write",
            description = "Enables on-device T9 search, caller ID, and contact management",
            icon = Icons.Filled.Contacts,
            permissionKey = android.Manifest.permission.READ_CONTACTS
        ),
        PermissionItem(
            name = "Audio Recording & Route",
            description = "Audio streaming for phone calls and earpiece/speaker routing",
            icon = Icons.Filled.Mic,
            permissionKey = android.Manifest.permission.RECORD_AUDIO
        ),
        PermissionItem(
            name = "Bluetooth Headsets",
            description = "Connects and routes calls to wireless headsets and vehicle audio",
            icon = Icons.Filled.Bluetooth,
            permissionKey = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                android.Manifest.permission.BLUETOOTH_CONNECT
            } else null
        ),
        PermissionItem(
            name = "System Notifications",
            description = "Posts missed call alerts and ongoing call controls",
            icon = Icons.Filled.Notifications,
            permissionKey = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                android.Manifest.permission.POST_NOTIFICATIONS
            } else null
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(LineaColors.BackgroundTop, LineaColors.BackgroundBottom)
                )
            )
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = LineaDimensions.ScreenPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        text = "App Permissions",
                        style = LineaTypography.headlineMedium,
                        color = LineaColors.TextPrimary
                    )
                    Text(
                        text = "Security & Telecom Privileges",
                        style = LineaTypography.bodyMedium,
                        color = LineaColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Status Summary Card
            val allGranted = permissionItems.filter { !it.isOptional }.all { item ->
                if (item.isCustomCheck != null) item.isCustomCheck.invoke(context)
                else if (item.permissionKey != null) {
                    ContextCompat.checkSelfPermission(context, item.permissionKey) == PackageManager.PERMISSION_GRANTED
                } else true
            }

            FrostedGlassBox(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (allGranted) LineaColors.MutedSageGreen.copy(alpha = 0.15f)
                                else LineaColors.CarmineRed.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (allGranted) Icons.Filled.Check else Icons.Filled.Security,
                            contentDescription = null,
                            tint = if (allGranted) LineaColors.MutedSageGreen else LineaColors.CarmineRed,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (allGranted) "All Permissions Granted" else "Action Required",
                            style = LineaTypography.titleMedium,
                            color = LineaColors.TextPrimary
                        )
                        Text(
                            text = if (allGranted) "Linea has full hardware & telecom access" else "Some permissions are missing",
                            style = LineaTypography.bodySmall,
                            color = LineaColors.TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Detailed Permissions List
            Text(
                text = "System Permissions Breakdown",
                style = LineaTypography.titleSmall,
                color = LineaColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))

            permissionItems.forEach { item ->
                val isGranted = if (item.isCustomCheck != null) {
                    item.isCustomCheck.invoke(context)
                } else if (item.permissionKey != null) {
                    ContextCompat.checkSelfPermission(context, item.permissionKey) == PackageManager.PERMISSION_GRANTED
                } else true

                val badgeColor = when {
                    isGranted -> LineaColors.MutedSageGreen
                    item.isOptional -> LineaColors.TitaniumBlue
                    else -> LineaColors.CarmineRed
                }
                val badgeText = when {
                    isGranted -> "GRANTED"
                    item.isOptional -> "OPTIONAL"
                    else -> "MISSING"
                }

                FrostedGlassBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (item.customAction != null) {
                                Modifier.clickable { item.customAction.invoke(context) }
                            } else Modifier
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(LineaColors.GlassFill),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = if (isGranted) LineaColors.TitaniumBlue else LineaColors.TextTertiary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                style = LineaTypography.bodyLarge,
                                color = LineaColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.description,
                                style = LineaTypography.bodySmall,
                                color = LineaColors.TextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(badgeColor.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isGranted) Icons.Filled.Check else if (item.isOptional) Icons.Filled.Settings else Icons.Filled.Close,
                                    contentDescription = null,
                                    tint = badgeColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = badgeText,
                                    fontSize = 10.sp,
                                    color = badgeColor
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Open App Settings Deep Link Button
            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(LineaDimensions.ButtonCornerRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LineaColors.GlassFill,
                    contentColor = LineaColors.TextPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = null,
                    tint = LineaColors.TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Open App System Settings",
                    style = LineaTypography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
