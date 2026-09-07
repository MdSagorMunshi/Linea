package com.ryanshelby.linea.ui.screens.contacts

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaTypography

@Composable
fun AlphabeticalScrubber(
    activeLetter: Char?,
    onLetterSelected: (Char) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val alphabet = remember { listOf('#') + ('A'..'Z').toList() }
    var componentHeight by remember { mutableStateOf(1) }

    Column(
        modifier = modifier
            .width(28.dp)
            .fillMaxHeight()
            .padding(vertical = 12.dp)
            .onGloballyPositioned { coordinates ->
                componentHeight = coordinates.size.height
            }
            .pointerInput(alphabet) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, _ ->
                        val y = change.position.y.coerceIn(0f, componentHeight.toFloat())
                        val index = ((y / componentHeight) * alphabet.size).toInt().coerceIn(0, alphabet.size - 1)
                        val selected = alphabet[index]
                        if (selected != activeLetter) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onLetterSelected(selected)
                        }
                    }
                )
            },
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        alphabet.forEach { letter ->
            val isSelected = letter == activeLetter
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = letter.toString(),
                    style = LineaTypography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (isSelected) LineaColors.TitaniumBlue else LineaColors.TextTertiary
                )
            }
        }
    }
}
