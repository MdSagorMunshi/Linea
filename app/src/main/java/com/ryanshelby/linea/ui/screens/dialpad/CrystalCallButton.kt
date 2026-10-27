package com.ryanshelby.linea.ui.screens.dialpad

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import com.ryanshelby.linea.ui.theme.LineaTypography
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ryanshelby.linea.ui.components.crystalSurface
import com.ryanshelby.linea.ui.theme.LocalReduceAnimations
import kotlinx.coroutines.launch

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun CrystalCallButton(alternateSkin: Boolean, onClick: () -> Unit, onToggleSkin: () -> Unit, modifier: Modifier = Modifier) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val reduced = LocalReduceAnimations.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val reveal = remember { Animatable(0f) }
    var revealing by remember { mutableStateOf(false) }
    val press by animateFloatAsState(if (pressed && !reduced) 1f else 0f,
        spring(dampingRatio = .8f, stiffness = 500f), label = "crystal_call_press")
    Box(
        modifier.width(128.dp).height(64.dp)
            .graphicsLayer {
                scaleX = 1f - .06f * press + .08f * reveal.value
                scaleY = scaleX
                rotationX = if (reduced) 0f else press * 7f - reveal.value * 12f
                cameraDistance = 14f * density
            }
            .crystalSurface(RoundedCornerShape(32.dp), elevation = 10.dp, pressed = pressed,
                tint = if (alternateSkin) Color(0xFFD6E4EB) else Color(0xFF405F70))
            .combinedClickable(
                enabled = !revealing, role = Role.Button,
                interactionSource = interactions, indication = null,
                onClick = onClick, onLongClickLabel = "Change call button skin",
                onLongClick = {
                    if (!revealing) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch {
                            revealing = true
                            try {
                                if (!reduced) reveal.animateTo(1f, tween(220))
                                onToggleSkin()
                                if (!reduced) reveal.animateTo(0f, spring(dampingRatio = .7f, stiffness = 200f))
                            } finally {
                                reveal.snapTo(0f)
                                revealing = false
                            }
                        }
                    }
                }
            ), contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Call, contentDescription = null,
                tint = if (alternateSkin) Color(0xFF243F4F) else Color.White, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
            Text("Call", style = LineaTypography.titleMedium,
                color = if (alternateSkin) Color(0xFF243F4F) else Color.White)
        }
    }
}
