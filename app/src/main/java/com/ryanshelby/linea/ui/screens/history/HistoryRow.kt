package com.ryanshelby.linea.ui.screens.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.data.local.entities.CallDirectionType
import com.ryanshelby.linea.data.local.entities.CallRecordEntity
import com.ryanshelby.linea.ui.components.FrostedGlassBox
import com.ryanshelby.linea.ui.components.neumorphic
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun HistoryRow(
    item: CallHistoryItem,
    onClick: () -> Unit,
    onCallBack: (CallRecordEntity) -> Unit,
    onToggleExpand: () -> Unit,
    onAddContact: (String) -> Unit,
    onSendSms: (String) -> Unit,
    onBlockNumber: (String) -> Unit,
    onDelete: (CallHistoryItem) -> Unit,
    modifier: Modifier = Modifier,
    isBlocked: Boolean = false,
    onUnblockNumber: ((String) -> Unit)? = null,
    isSwipedOpen: Boolean = false,
    onSwipeOpenChanged: ((Boolean) -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    var offsetX by remember { mutableFloatStateOf(0f) }
    var animationJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val density = LocalDensity.current
    val viewConfig = LocalViewConfiguration.current
    val touchSlop = viewConfig.touchSlop

    val actionWidth = 204.dp
    val actionWidthPx = remember(density) { with(density) { actionWidth.toPx() } }
    val callBackThresholdPx = remember(density) { with(density) { 80.dp.toPx() } }
    val callBackMaxPx = remember(density) { with(density) { 120.dp.toPx() } }

    val isOpen by remember { derivedStateOf { offsetX < -10f } }

    val animateOffsetTo: (Float, AnimationSpec<Float>) -> Unit = { target, spec ->
        animationJob?.cancel()
        animationJob = coroutineScope.launch {
            androidx.compose.animation.core.animate(
                initialValue = offsetX,
                targetValue = target,
                animationSpec = spec
            ) { value, _ ->
                offsetX = value
            }
        }
    }

    // Synchronize open/closed state when another row is swiped or reset externally
    LaunchedEffect(isSwipedOpen) {
        if (!isSwipedOpen && offsetX < -10f) {
            animateOffsetTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
        } else if (isSwipedOpen && offsetX >= -10f) {
            animateOffsetTo(-actionWidthPx, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
        }
    }

    // Reset offset if recycled item changes
    LaunchedEffect(item.id) {
        if (!isSwipedOpen && offsetX != 0f) {
            animationJob?.cancel()
            offsetX = 0f
        }
    }

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        // 1. Quick Actions Background Tray (revealed on left swipe)
        if (offsetX <= 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(LineaColors.NeuSurfaceSunken)
                    .border(
                        LineaDimensions.HairlineBorder,
                        LineaColors.NeuBorderHighlight.copy(alpha = 0.35f),
                        RoundedCornerShape(16.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .align(if (item.isExpanded) Alignment.TopEnd else Alignment.CenterEnd)
                        .then(
                            if (item.isExpanded) Modifier.height(72.dp) else Modifier.fillMaxHeight()
                        )
                        .padding(end = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Save / Add Contact
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(LineaColors.NeuSurfaceRaised)
                            .border(0.5.dp, LineaColors.NeuBorderHighlight.copy(alpha = 0.5f), CircleShape)
                            .clickable {
                                animateOffsetTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                onSwipeOpenChanged?.invoke(false)
                                onAddContact(item.primaryRecord.phoneNumber)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PersonAdd,
                            contentDescription = "Add Contact",
                            tint = LineaColors.TitaniumBlue,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Send SMS
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(LineaColors.NeuSurfaceRaised)
                            .border(0.5.dp, LineaColors.NeuBorderHighlight.copy(alpha = 0.5f), CircleShape)
                            .clickable {
                                animateOffsetTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                onSwipeOpenChanged?.invoke(false)
                                onSendSms(item.primaryRecord.phoneNumber)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Message,
                            contentDescription = "SMS",
                            tint = LineaColors.TextPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Block / Unblock Number
                    if (isBlocked && onUnblockNumber != null) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(LineaColors.NeuSurfaceRaised)
                                .border(0.5.dp, LineaColors.MutedSageGreen.copy(alpha = 0.4f), CircleShape)
                                .clickable {
                                    animateOffsetTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                    onSwipeOpenChanged?.invoke(false)
                                    onUnblockNumber(item.primaryRecord.phoneNumber)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Block,
                                contentDescription = "Unblock",
                                tint = LineaColors.MutedSageGreen,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(LineaColors.NeuSurfaceRaised)
                                .border(0.5.dp, LineaColors.Danger.copy(alpha = 0.4f), CircleShape)
                                .clickable {
                                    animateOffsetTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                    onSwipeOpenChanged?.invoke(false)
                                    onBlockNumber(item.primaryRecord.phoneNumber)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Block,
                                contentDescription = "Block",
                                tint = LineaColors.Danger,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    // Delete Call History Group
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(LineaColors.NeuSurfaceRaised)
                            .border(0.5.dp, LineaColors.Danger.copy(alpha = 0.4f), CircleShape)
                            .clickable {
                                animateOffsetTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                onSwipeOpenChanged?.invoke(false)
                                onDelete(item)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = LineaColors.Danger,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }

        // 2. Call Back Background (revealed on right swipe)
        if (offsetX > 0f) {
            val progress = (offsetX / callBackThresholdPx).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(LineaColors.MutedSageGreen.copy(alpha = 0.15f + 0.2f * progress))
                    .border(
                        LineaDimensions.HairlineBorder,
                        LineaColors.MutedSageGreen.copy(alpha = 0.3f * progress),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Call,
                        contentDescription = "Call Back",
                        tint = LineaColors.MutedSageGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Call Back",
                        style = LineaTypography.labelMedium,
                        color = LineaColors.MutedSageGreen
                    )
                }
            }
        }

        // 3. Foreground Main Card (swipable)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .pointerInput(item.id, actionWidthPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        var totalDx = 0f
                        var totalDy = 0f
                        var isDragging = false
                        val startOffset = offsetX

                        while (true) {
                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                            val change = event.changes.firstOrNull() ?: break

                            if (change.pressed) {
                                val dx = change.position.x - change.previousPosition.x
                                val dy = change.position.y - change.previousPosition.y
                                totalDx += dx
                                totalDy += dy

                                if (!isDragging) {
                                    if (abs(totalDx) > touchSlop && abs(totalDx) > abs(totalDy) * 1.1f) {
                                        isDragging = true
                                        animationJob?.cancel()
                                        change.consume()
                                    } else if (abs(totalDy) > touchSlop && abs(totalDy) > abs(totalDx)) {
                                        // Vertical scroll detected; allow LazyColumn to handle
                                        break
                                    }
                                }

                                if (isDragging) {
                                    change.consume()
                                    val target = offsetX + dx
                                    val clamped = when {
                                        target < -actionWidthPx -> -actionWidthPx + (target + actionWidthPx) * 0.2f
                                        target > callBackMaxPx -> callBackMaxPx + (target - callBackMaxPx) * 0.2f
                                        else -> target
                                    }
                                    offsetX = clamped
                                }
                            } else {
                                // Finger lifted
                                if (isDragging) {
                                    change.consume()
                                    val current = offsetX
                                    if (current > 0f) {
                                        if (current >= callBackThresholdPx) {
                                            onCallBack(item.primaryRecord)
                                        }
                                        animateOffsetTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                        onSwipeOpenChanged?.invoke(false)
                                    } else {
                                        val shouldOpen = if (startOffset < -10f) {
                                            !(current > -actionWidthPx * 0.7f)
                                        } else {
                                            current <= -actionWidthPx * 0.25f
                                        }

                                        if (shouldOpen) {
                                            animateOffsetTo(
                                                -actionWidthPx,
                                                spring(
                                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                                    stiffness = Spring.StiffnessMediumLow
                                                )
                                            )
                                            onSwipeOpenChanged?.invoke(true)
                                        } else {
                                            animateOffsetTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                            onSwipeOpenChanged?.invoke(false)
                                        }
                                    }
                                }
                                break
                            }
                        }
                    }
                }
                .neumorphic(
                    shape = RoundedCornerShape(16.dp),
                    elevation = 3.dp,
                    surfaceColor = LineaColors.NeuSurfaceRaised
                )
        ) {
            // Main History Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (isOpen) {
                            animateOffsetTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                            onSwipeOpenChanged?.invoke(false)
                        } else {
                            onClick()
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Contact Avatar or Initials
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(LineaColors.BackgroundBottom)
                            .border(LineaDimensions.HairlineBorder, LineaColors.GlassBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val initials = getInitials(item.primaryRecord.callerName)
                        if (initials.isNotEmpty()) {
                            Text(
                                text = initials,
                                style = LineaTypography.titleSmall,
                                color = LineaColors.TextPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = LineaColors.TextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.primaryRecord.callerName ?: item.primaryRecord.phoneNumber,
                                style = LineaTypography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = if (item.primaryRecord.callType == CallDirectionType.MISSED) {
                                    LineaColors.MutedRust
                                } else {
                                    LineaColors.TextPrimary
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (isBlocked) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(LineaColors.Danger.copy(alpha = 0.15f))
                                        .border(0.5.dp, LineaColors.Danger.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "Blocked",
                                        style = LineaTypography.labelSmall,
                                        fontSize = 9.sp,
                                        color = LineaColors.Danger,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Grouped Call Count Badge (e.g. ×3)
                            if (item.callCount > 1) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(LineaColors.TitaniumBlue.copy(alpha = 0.25f))
                                        .clickable {
                                            if (isOpen) {
                                                animateOffsetTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                                onSwipeOpenChanged?.invoke(false)
                                            } else {
                                                onToggleExpand()
                                            }
                                        }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "×${item.callCount}",
                                            style = LineaTypography.labelSmall.copy(
                                                fontFeatureSettings = "tnum"
                                            ),
                                            color = LineaColors.TitaniumBlue,
                                            fontSize = 10.sp
                                        )
                                        Icon(
                                            imageVector = if (item.isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                            contentDescription = "Expand",
                                            tint = LineaColors.TitaniumBlue,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        // Subline: Call type icon + Relative timestamp + Duration + SIM indicator
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CallTypeIcon(item.primaryRecord.callType)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = HistoryGrouper.formatRelativeTime(item.primaryRecord.timestamp),
                                style = LineaTypography.bodySmall,
                                color = LineaColors.TextSecondary
                            )

                            if (item.primaryRecord.durationSeconds > 0) {
                                Text(
                                    text = " • ${HistoryGrouper.formatDuration(item.primaryRecord.durationSeconds)}",
                                    style = LineaTypography.bodySmall.copy(
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = LineaColors.TextTertiary
                                )
                            }

                            // SIM slot indicator pill
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(LineaColors.GlassFill)
                                    .border(0.5.dp, LineaColors.GlassBorder, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "SIM ${item.primaryRecord.simSlot + 1}",
                                    style = LineaTypography.labelSmall,
                                    fontSize = 9.sp,
                                    color = LineaColors.TextTertiary
                                )
                            }
                        }
                    }
                }

                // Quick Call Back Button
                IconButton(
                    onClick = {
                        if (isOpen) {
                            animateOffsetTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                            onSwipeOpenChanged?.invoke(false)
                        } else {
                            onCallBack(item.primaryRecord)
                        }
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Call,
                        contentDescription = "Call",
                        tint = LineaColors.TitaniumBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Expandable List of Individual Calls in this Session
            AnimatedVisibility(
                visible = item.isExpanded && item.callCount > 1,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LineaColors.BackgroundTop.copy(alpha = 0.5f))
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item.groupedCalls.forEach { singleCall ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CallTypeIcon(singleCall.callType, size = 14.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = HistoryGrouper.formatExactTime(singleCall.timestamp),
                                    style = LineaTypography.bodySmall.copy(
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = LineaColors.TextSecondary
                                )
                            }

                            Text(
                                text = if (singleCall.durationSeconds > 0) {
                                    HistoryGrouper.formatDuration(singleCall.durationSeconds)
                                } else {
                                    when (singleCall.callType) {
                                        CallDirectionType.MISSED -> "Missed"
                                        CallDirectionType.REJECTED -> "Declined"
                                        CallDirectionType.BLOCKED -> "Blocked"
                                        else -> "No answer"
                                    }
                                },
                                style = LineaTypography.bodySmall.copy(
                                    fontFeatureSettings = "tnum"
                                ),
                                color = LineaColors.TextTertiary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CallTypeIcon(type: CallDirectionType, size: androidx.compose.ui.unit.Dp = 16.dp) {
    val (icon, tint) = when (type) {
        CallDirectionType.INCOMING -> Icons.AutoMirrored.Filled.CallReceived to LineaColors.MutedSageGreen
        CallDirectionType.OUTGOING -> Icons.AutoMirrored.Filled.CallMade to LineaColors.TitaniumBlue
        CallDirectionType.MISSED -> Icons.AutoMirrored.Filled.CallMissed to LineaColors.MutedRust
        CallDirectionType.REJECTED -> Icons.Filled.CallEnd to LineaColors.Danger
        CallDirectionType.BLOCKED -> Icons.Filled.Block to LineaColors.Danger
    }

    Icon(
        imageVector = icon,
        contentDescription = type.name,
        tint = tint,
        modifier = Modifier.size(size)
    )
}

private fun getInitials(name: String?): String {
    if (name.isNullOrBlank()) return ""
    val words = name.trim().split(" ")
    return if (words.size >= 2) {
        "${words[0].firstOrNull() ?: ""}${words[1].firstOrNull() ?: ""}".uppercase()
    } else {
        name.take(1).uppercase()
    }
}

@Composable
fun CallSessionRow(
    session: CallSessionItem,
    onClick: () -> Unit,
    onCallBack: (CallRecordEntity) -> Unit,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier,
    isBlocked: Boolean = false
) {
    FrostedGlassBox(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar / Initials
                val initials = getInitials(session.callerName)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(LineaColors.GlassFill)
                        .border(LineaDimensions.HairlineBorder, LineaColors.GlassBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (initials.isNotEmpty()) {
                        Text(
                            text = initials,
                            style = LineaTypography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = LineaColors.TitaniumBlue
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = LineaColors.TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Name & Metadata
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = session.callerName ?: session.phoneNumber,
                            style = LineaTypography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFeatureSettings = "tnum"
                            ),
                            color = LineaColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (isBlocked) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(LineaColors.Danger.copy(alpha = 0.15f))
                                    .border(0.5.dp, LineaColors.Danger.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "Blocked",
                                    style = LineaTypography.labelSmall,
                                    fontSize = 9.sp,
                                    color = LineaColors.Danger,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Call count pill
                        if (session.callCount > 1) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(LineaColors.TitaniumBlue.copy(alpha = 0.15f))
                                    .border(0.5.dp, LineaColors.TitaniumBlue.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "×${session.callCount}",
                                    style = LineaTypography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    fontSize = 10.sp,
                                    color = LineaColors.TitaniumBlue
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CallTypeIcon(session.latestCallType, size = 13.dp)

                        Text(
                            text = HistoryGrouper.formatRelativeTime(session.latestTimestamp),
                            style = LineaTypography.bodySmall.copy(fontFeatureSettings = "tnum"),
                            color = LineaColors.TextSecondary
                        )

                        Text(text = "•", color = LineaColors.TextTertiary, style = LineaTypography.bodySmall)

                        Text(
                            text = if (session.totalDurationSeconds > 0) {
                                HistoryGrouper.formatDuration(session.totalDurationSeconds)
                            } else {
                                "0s"
                            },
                            style = LineaTypography.bodySmall.copy(fontFeatureSettings = "tnum"),
                            color = LineaColors.TitaniumBlue
                        )

                        // SIM Slot
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(LineaColors.GlassFill)
                                .border(0.5.dp, LineaColors.GlassBorder, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "SIM ${session.latestSimSlot + 1}",
                                style = LineaTypography.labelSmall,
                                fontSize = 9.sp,
                                color = LineaColors.TextTertiary
                            )
                        }
                    }
                }

                // Actions: Expand & Call
                if (session.callCount > 1) {
                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (session.isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (session.isExpanded) "Collapse" else "Expand",
                            tint = LineaColors.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                val primaryRecord = session.calls.firstOrNull()
                if (primaryRecord != null) {
                    IconButton(
                        onClick = { onCallBack(primaryRecord) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Call,
                            contentDescription = "Call",
                            tint = LineaColors.TitaniumBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Expandable breakdown
            AnimatedVisibility(
                visible = session.isExpanded && session.callCount > 1,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LineaColors.BackgroundTop.copy(alpha = 0.5f))
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    session.calls.forEach { call ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CallTypeIcon(call.callType, size = 13.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${HistoryGrouper.formatRelativeDate(call.timestamp)}, ${HistoryGrouper.formatExactTime(call.timestamp)}",
                                    style = LineaTypography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                    color = LineaColors.TextSecondary
                                )
                            }

                            Text(
                                text = if (call.durationSeconds > 0) {
                                    HistoryGrouper.formatDuration(call.durationSeconds)
                                } else {
                                    when (call.callType) {
                                        CallDirectionType.MISSED -> "Missed"
                                        CallDirectionType.REJECTED -> "Declined"
                                        CallDirectionType.BLOCKED -> "Blocked"
                                        else -> "No answer"
                                    }
                                },
                                style = LineaTypography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                color = LineaColors.TextTertiary
                            )
                        }
                    }
                }
            }
        }
    }
}

