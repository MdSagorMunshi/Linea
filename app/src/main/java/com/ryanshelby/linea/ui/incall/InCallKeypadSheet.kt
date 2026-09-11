package com.ryanshelby.linea.ui.incall

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.ui.components.neumorphic
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaTypography
import com.ryanshelby.linea.ui.theme.LocalReduceAnimations

/**
 * Transparent In-Call DTMF Keypad Bottom Sheet.
 * Displays floating tactile Neumorphic digit discs with a fully transparent background.
 */
@Composable
fun InCallKeypadSheet(
    onDtmfPress: (Char) -> Unit,
    onDtmfRelease: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var typedDigits by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Title & Close Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Keypad",
                    style = LineaTypography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = LineaColors.TextPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close Keypad",
                        tint = LineaColors.TextSecondary
                    )
                }
            }

            // Display typed DTMF sequence with backspace
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 38.dp)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = typedDigits.ifEmpty { " " },
                    style = LineaTypography.headlineMedium.copy(
                        fontFeatureSettings = "tnum",
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = LineaColors.TitaniumBlue
                )
                if (typedDigits.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(10.dp))
                    IconButton(
                        onClick = { typedDigits = typedDigits.dropLast(1) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = "Backspace",
                            tint = LineaColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            val keys = listOf(
                listOf('1', '2', '3'),
                listOf('4', '5', '6'),
                listOf('7', '8', '9'),
                listOf('*', '0', '#')
            )

            keys.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { digit ->
                        DtmfKeyButton(
                            digit = digit,
                            onPress = {
                                typedDigits += digit
                                onDtmfPress(digit)
                            },
                            onRelease = onDtmfRelease
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun DtmfKeyButton(
    digit: Char,
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val reduceAnimations = LocalReduceAnimations.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed && !reduceAnimations) 0.91f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "DtmfKeyScale"
    )

    Box(
        modifier = Modifier
            .size(68.dp)
            .scale(scale)
            .neumorphic(
                shape = CircleShape,
                elevation = if (isPressed) 1.dp else 4.dp,
                isPressed = isPressed,
                surfaceColor = if (isPressed) {
                    LineaColors.TitaniumBlue.copy(alpha = 0.35f)
                } else {
                    LineaColors.NeuSurfaceRaised
                }
            )
            .pointerInput(digit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onPress()
                        tryAwaitRelease()
                        isPressed = false
                        onRelease()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = digit.toString(),
            style = LineaTypography.titleLarge.copy(
                fontWeight = FontWeight.Medium,
                fontFeatureSettings = "tnum"
            ),
            fontSize = 24.sp,
            color = LineaColors.TextPrimary
        )
    }
}
