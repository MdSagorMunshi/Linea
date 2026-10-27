package com.ryanshelby.linea.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LocalReduceAnimations

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun CrystalIconButton(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, onLongClick: (() -> Unit)? = null, longClickLabel: String? = null) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val reduced = LocalReduceAnimations.current
    val scale by animateFloatAsState(if (pressed && !reduced) .94f else 1f,
        spring(dampingRatio = .8f, stiffness = 500f), label = "crystal_control_press")
    Box(modifier.size(48.dp).graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else .4f }
        .crystalSurface(CircleShape, pressed = pressed)
        .combinedClickable(enabled = enabled, interactionSource = source, indication = null, role = Role.Button,
            onClick = onClick, onLongClick = onLongClick, onLongClickLabel = longClickLabel),
        contentAlignment = Alignment.Center) {
        Icon(icon, label, Modifier.size(22.dp), tint = LineaColors.TextPrimary)
    }
}
