package com.ryanshelby.linea.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.telecom.ActiveCallInfo
import com.ryanshelby.linea.ui.incall.InCallActivity
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography

@Composable
fun DontInterruptMeBanner(
    callInfo: ActiveCallInfo?,
    onAnswer: () -> Unit,
    onReject: () -> Unit,
    onIgnore: () -> Unit,
    onMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isVisible = callInfo != null
    val context = LocalContext.current

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        if (callInfo != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(LineaDimensions.CardCornerRadius))
                    .background(Color(0xE614181D))
                    .border(
                        width = 1.dp,
                        color = LineaColors.TitaniumBlue.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(LineaDimensions.CardCornerRadius)
                    )
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(LineaColors.SurfaceElevated)
                                .border(1.dp, LineaColors.GlassBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = LineaColors.TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Caller Info
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = callInfo.displayName ?: callInfo.phoneNumber,
                                    style = LineaTypography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = LineaColors.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(LineaColors.TitaniumBlue.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "DIM",
                                        style = LineaTypography.labelSmall.copy(fontSize = 9.sp),
                                        color = LineaColors.TitaniumBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (callInfo.displayName != null && callInfo.phoneNumber.isNotBlank()) {
                                Text(
                                    text = callInfo.phoneNumber,
                                    style = LineaTypography.bodySmall,
                                    color = LineaColors.TextTertiary,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 4-Action Controls: Answer, Decline, Message, Ignore
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Answer
                        ActionButton(
                            label = "Answer",
                            icon = Icons.Filled.Call,
                            backgroundColor = LineaColors.AccentGreen,
                            contentColor = Color.White,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onAnswer()
                                val intent = Intent(context, InCallActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                }
                                context.startActivity(intent)
                            }
                        )

                        // Decline
                        ActionButton(
                            label = "Decline",
                            icon = Icons.Filled.CallEnd,
                            backgroundColor = LineaColors.CarmineRed,
                            contentColor = Color.White,
                            modifier = Modifier.weight(1f),
                            onClick = onReject
                        )

                        // Message
                        ActionButton(
                            label = "Message",
                            icon = Icons.Filled.Message,
                            backgroundColor = LineaColors.SurfaceElevated,
                            contentColor = LineaColors.TitaniumBlue,
                            borderColor = LineaColors.TitaniumBlue.copy(alpha = 0.4f),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onMessage()
                                val uri = Uri.parse("smsto:${callInfo.phoneNumber}")
                                val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            }
                        )

                        // Ignore (Silence)
                        ActionButton(
                            label = "Ignore",
                            icon = Icons.Filled.NotificationsOff,
                            backgroundColor = LineaColors.GlassFill,
                            contentColor = LineaColors.TextSecondary,
                            borderColor = LineaColors.GlassBorder,
                            modifier = Modifier.weight(1f),
                            onClick = onIgnore
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color? = null
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .then(
                if (borderColor != null) Modifier.border(1.dp, borderColor, RoundedCornerShape(8.dp))
                else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = LineaTypography.labelSmall.copy(fontSize = 11.sp),
                color = contentColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
