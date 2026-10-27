package com.ryanshelby.linea.ui.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.ui.components.crystalSurface
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LocalReduceAnimations

@Composable
fun CrystalNavBar(current: LineaDestination, onNavigate: (LineaDestination) -> Unit, missed: Int, modifier: Modifier = Modifier) {
    val reduced = LocalReduceAnimations.current
    val index by animateFloatAsState(current.ordinal.toFloat(),
        if (reduced) tween(0) else spring(dampingRatio = .85f, stiffness = 350f), label = "crystal_dock_lens")
    Box(modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 22.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center) {
        BoxWithConstraints(Modifier.widthIn(max = 430.dp).fillMaxWidth()
            .crystalSurface(RoundedCornerShape(30.dp), elevation = 14.dp).padding(6.dp)) {
            val slot = maxWidth / LineaDestination.entries.size
            val density = LocalDensity.current
            Box(Modifier.offset { androidx.compose.ui.unit.IntOffset(with(density) { (slot * index).roundToPx() }, 0) }
                .width(slot).height(56.dp)
                .crystalSurface(RoundedCornerShape(24.dp), elevation = 2.dp,
                    tint = LineaColors.NeuSurfaceRaised))
            Row(Modifier.fillMaxWidth().selectableGroup()) {
                LineaDestination.entries.forEach { destination ->
                    val selected = current == destination
                    Column(Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(24.dp))
                        .selectable(selected, role = Role.Tab, onClick = { onNavigate(destination) })
                        .semantics {
                            contentDescription = destination.title + if (destination == LineaDestination.HISTORY && missed > 0) ", $missed missed calls" else ""
                        }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Box {
                            Icon(painterResource(if (selected) destination.filledIcon else destination.outlineIcon), null,
                                tint = if (selected) LineaColors.TextPrimary else LineaColors.TextSecondary, modifier = Modifier.size(22.dp))
                            if (destination == LineaDestination.HISTORY && missed > 0) {
                                Box(Modifier.align(Alignment.TopEnd).size(6.dp).background(LineaColors.CarmineRed, CircleShape))
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(destination.title, fontSize = 10.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) LineaColors.TextPrimary else LineaColors.TextSecondary)
                    }
                }
            }
        }
    }
}
