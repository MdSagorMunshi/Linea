package com.ryanshelby.linea.ui.screens.dialpad

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaTypography
import kotlinx.coroutines.delay

/**
 * Interactive dialed number display with tactile cursor positioning, horizontal scrubbing,
 * and seamless in-place editing across both Crystal and Neumorphic dialpad layouts.
 */
@Composable
fun EditableDialedNumberDisplay(
    number: String,
    cursorPosition: Int,
    onCursorPositionChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Enter a number",
    fontSize: TextUnit = if (number.length > 18) 22.sp else if (number.length > 12) 28.sp else 38.sp,
    cursorColor: Color = LineaColors.TitaniumBlue,
    onCopy: (() -> Unit)? = null,
    onPaste: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    // Responsive cursor blink: immediately solid/visible on interaction, then toggles gently
    var cursorVisible by remember { mutableStateOf(true) }
    LaunchedEffect(number, cursorPosition) {
        cursorVisible = true
        while (true) {
            delay(550)
            cursorVisible = !cursorVisible
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (number.isEmpty()) {
            Text(
                text = placeholder,
                style = LineaTypography.titleLarge,
                fontSize = 24.sp,
                color = LineaColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onPaste?.invoke() }
                        )
                    }
            )
        } else {
            val safeCursorPos = cursorPosition.coerceIn(0, number.length)

            Text(
                text = number,
                style = LineaTypography.displayLarge.copy(
                    fontFeatureSettings = "tnum",
                    fontSize = fontSize,
                    letterSpacing = 1.sp
                ),
                color = LineaColors.TextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { textLayoutResult = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .drawWithContent {
                        drawContent()
                        val layout = textLayoutResult
                        if (layout != null && cursorVisible) {
                            val cursorRect = layout.getCursorRect(safeCursorPos)
                            val cursorWidth = 3.dp.toPx()
                            val cursorTop = cursorRect.top
                            val cursorBottom = cursorRect.bottom
                            val cursorHeight = (cursorBottom - cursorTop).coerceAtLeast(24.dp.toPx())

                            // 1. Vertical rounded cursor line in accent color
                            drawRoundRect(
                                color = cursorColor,
                                topLeft = Offset(cursorRect.left - cursorWidth / 2f, cursorTop),
                                size = Size(cursorWidth, cursorHeight),
                                cornerRadius = CornerRadius(cursorWidth / 2f, cursorWidth / 2f)
                            )

                            // 2. Tactile circular handle at the bottom of the cursor
                            drawCircle(
                                color = cursorColor,
                                radius = 4.5.dp.toPx(),
                                center = Offset(cursorRect.left, cursorBottom + 3.dp.toPx())
                            )
                        }
                    }
                    .pointerInput(number) {
                        detectTapGestures(
                            onTap = { offset ->
                                val layout = textLayoutResult ?: return@detectTapGestures
                                val newIndex = layout.getOffsetForPosition(offset).coerceIn(0, number.length)
                                onCursorPositionChange(newIndex)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onLongPress = {
                                onCopy?.invoke()
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        )
                    }
                    .pointerInput(number) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                val layout = textLayoutResult ?: return@detectHorizontalDragGestures
                                val newIndex = layout.getOffsetForPosition(offset).coerceIn(0, number.length)
                                onCursorPositionChange(newIndex)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onHorizontalDrag = { change, _ ->
                                change.consume()
                                val layout = textLayoutResult ?: return@detectHorizontalDragGestures
                                val newIndex = layout.getOffsetForPosition(change.position).coerceIn(0, number.length)
                                if (newIndex != cursorPosition) {
                                    onCursorPositionChange(newIndex)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        )
                    }
            )
        }
    }
}
