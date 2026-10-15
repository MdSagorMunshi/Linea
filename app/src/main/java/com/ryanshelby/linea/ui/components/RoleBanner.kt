package com.ryanshelby.linea.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.telecom.CallHandlingHealth
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography

@Composable
fun RoleBanner(
    isDefaultDialer: Boolean,
    onRequestRole: () -> Unit,
    modifier: Modifier = Modifier,
    health: CallHandlingHealth? = null,
    onRequestFullScreenIntent: (() -> Unit)? = null,
    onRequestNotifications: (() -> Unit)? = null,
    onRequestBatteryOptimization: (() -> Unit)? = null
) {
    val allOptimal = health?.allPassed ?: isDefaultDialer
    var isExpanded by remember(allOptimal) { mutableStateOf(!allOptimal) }

    FrostedGlassBox(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LineaDimensions.PanelCornerRadius),
        borderColor = if (allOptimal) LineaColors.MutedSageGreen.copy(alpha = 0.3f) else LineaColors.MutedRust.copy(alpha = 0.4f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LineaDimensions.PanelPadding)
        ) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (allOptimal) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                        contentDescription = null,
                        tint = if (allOptimal) LineaColors.MutedSageGreen else LineaColors.MutedRust,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (allOptimal) "Call Handling Ready" else "Call Setup Needs Attention",
                            style = LineaTypography.titleSmall,
                            color = LineaColors.TextPrimary
                        )
                        Text(
                            text = if (allOptimal) {
                                "Default dialer & permissions optimal"
                            } else {
                                val issues = health?.failureCount ?: 1
                                "$issues item(s) may prevent incoming calls from ringing"
                            },
                            style = LineaTypography.bodySmall,
                            color = if (allOptimal) LineaColors.TextSecondary else LineaColors.MutedRust
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = LineaColors.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    // Check item 1: Default Dialer Role
                    HealthStatusRow(
                        icon = Icons.Filled.Phone,
                        title = "Default Phone App",
                        subtitle = if (isDefaultDialer) "Handling incoming & outgoing cellular calls" else "Required to receive incoming calls & control ringer",
                        isPassed = isDefaultDialer,
                        actionLabel = "Set Default",
                        onAction = onRequestRole
                    )

                    // Check item 2: Full Screen Intent
                    health?.let { h ->
                        Spacer(modifier = Modifier.height(10.dp))
                        HealthStatusRow(
                            icon = Icons.Filled.ScreenLockPortrait,
                            title = "Full-Screen Alerts",
                            subtitle = if (h.isFullScreenIntentAllowed) "Allowed to wake screen for locked phone calls" else "Required to show incoming call UI when device is locked",
                            isPassed = h.isFullScreenIntentAllowed,
                            actionLabel = "Grant",
                            onAction = onRequestFullScreenIntent
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        // Check item 3: Notifications
                        HealthStatusRow(
                            icon = Icons.Filled.Notifications,
                            title = "Call Notifications",
                            subtitle = if (h.areNotificationsEnabled) "Incoming and missed notifications active" else "Incoming call banner or missed alerts are blocked",
                            isPassed = h.areNotificationsEnabled,
                            actionLabel = "Enable",
                            onAction = onRequestNotifications
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        // Check item 4: Battery Optimization
                        val isPowerExempt = h.isDefaultDialer || h.isBatteryOptimizationIgnored
                        HealthStatusRow(
                            icon = Icons.Filled.BatteryAlert,
                            title = "Background Execution",
                            subtitle = if (isPowerExempt) "Unrestricted: granted automatically by Default Dialer role" else "Battery saver may delay or drop incoming calls",
                            isPassed = isPowerExempt,
                            actionLabel = "Allow",
                            onAction = onRequestBatteryOptimization
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthStatusRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isPassed: Boolean,
    actionLabel: String,
    onAction: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isPassed) LineaColors.MutedSageGreen.copy(alpha = 0.08f) else LineaColors.MutedRust.copy(alpha = 0.10f))
            .border(
                LineaDimensions.HairlineBorder,
                if (isPassed) LineaColors.MutedSageGreen.copy(alpha = 0.25f) else LineaColors.MutedRust.copy(alpha = 0.35f),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isPassed) LineaColors.MutedSageGreen.copy(alpha = 0.2f) else LineaColors.MutedRust.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isPassed) LineaColors.MutedSageGreen else LineaColors.MutedRust,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = LineaTypography.labelMedium.copy(fontSize = 13.sp),
                        color = LineaColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPassed) "ACTIVE" else "ATTENTION",
                        style = LineaTypography.labelSmall.copy(fontSize = 9.sp),
                        color = if (isPassed) LineaColors.MutedSageGreen else LineaColors.MutedRust
                    )
                }
                Text(
                    text = subtitle,
                    style = LineaTypography.bodySmall.copy(fontSize = 11.sp),
                    color = LineaColors.TextSecondary,
                    maxLines = 2
                )
            }
        }

        if (!isPassed && onAction != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LineaColors.TitaniumBlue,
                    contentColor = Color.White
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = actionLabel,
                    style = LineaTypography.labelSmall.copy(fontSize = 11.sp)
                )
            }
        }
    }
}
