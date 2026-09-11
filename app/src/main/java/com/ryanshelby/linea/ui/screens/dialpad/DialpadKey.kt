package com.ryanshelby.linea.ui.screens.dialpad

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.data.preferences.LineaPreferences
import com.ryanshelby.linea.ui.components.neumorphic
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography
import com.ryanshelby.linea.ui.theme.LocalReduceAnimations

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun DialpadKey(
    digit: Char,
    subLetters: String,
    onDigitPress: (Char) -> Unit,
    onLongPress: (() -> Unit)? = null,
    size: Dp = 68.dp,
    soundEnabled: Boolean = true,
    vibrationEnabled: Boolean = true,
    hapticProfile: String = LineaPreferences.DialpadHapticProfile.TITANIUM_GLASS,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val reduceAnimations = LocalReduceAnimations.current
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }

    val vibrator = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    val toneGenerator = remember(soundEnabled) {
        if (soundEnabled) {
            try {
                ToneGenerator(AudioManager.STREAM_DTMF, 60)
            } catch (e: Exception) {
                null
            }
        } else null
    }

    DisposableEffect(toneGenerator) {
        onDispose {
            toneGenerator?.release()
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed && !reduceAnimations) 0.91f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "KeyScale"
    )

    val bgBrush = if (isPressed) {
        Brush.verticalGradient(
            colors = listOf(
                LineaColors.TitaniumBlue.copy(alpha = 0.36f),
                LineaColors.TitaniumBlue.copy(alpha = 0.18f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.03f)
            )
        )
    }

    val borderBrush = if (isPressed) {
        Brush.verticalGradient(
            colors = listOf(
                LineaColors.TitaniumBlue,
                LineaColors.TitaniumBlue.copy(alpha = 0.45f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.18f),
                Color.White.copy(alpha = 0.06f)
            )
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .neumorphic(
                shape = CircleShape,
                elevation = if (isPressed) 1.dp else 5.dp,
                isPressed = isPressed
            )
            .pointerInput(digit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        if (vibrationEnabled) {
                            if (vibrator != null && vibrator.hasVibrator()) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    when (hapticProfile) {
                                        LineaPreferences.DialpadHapticProfile.STEALTH ->
                                            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                                        LineaPreferences.DialpadHapticProfile.MECHANICAL_RELAY ->
                                            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                                        LineaPreferences.DialpadHapticProfile.TITANIUM_GLASS ->
                                            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                                        else ->
                                            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                                    }
                                } else {
                                    @Suppress("DEPRECATION")
                                    vibrator.vibrate(25)
                                }
                            } else {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }

                        val toneDuration = when (hapticProfile) {
                            LineaPreferences.DialpadHapticProfile.STEALTH -> 0
                            LineaPreferences.DialpadHapticProfile.TITANIUM_GLASS -> 40
                            LineaPreferences.DialpadHapticProfile.MECHANICAL_RELAY -> 65
                            else -> 80
                        }
                        if (soundEnabled && toneDuration > 0) {
                            toneGenerator?.startTone(getToneForChar(digit), toneDuration)
                        }
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = {
                        onDigitPress(digit)
                    },
                    onLongPress = {
                        if (onLongPress != null) {
                            if (vibrationEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            onLongPress()
                        } else {
                            onDigitPress(digit)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = digit.toString(),
                style = LineaTypography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum"
                ),
                color = if (isPressed) Color.White else LineaColors.TextPrimary,
                fontSize = if (digit in listOf('*', '#')) 24.sp else 26.sp
            )

            if (subLetters.isNotEmpty()) {
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = subLetters,
                    style = LineaTypography.labelSmall.copy(
                        letterSpacing = 1.8.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = if (isPressed) LineaColors.TextPrimary else LineaColors.TextSecondary,
                    fontSize = 9.sp
                )
            }
        }
    }
}

private fun getToneForChar(ch: Char): Int {
    return when (ch) {
        '0' -> ToneGenerator.TONE_DTMF_0
        '1' -> ToneGenerator.TONE_DTMF_1
        '2' -> ToneGenerator.TONE_DTMF_2
        '3' -> ToneGenerator.TONE_DTMF_3
        '4' -> ToneGenerator.TONE_DTMF_4
        '5' -> ToneGenerator.TONE_DTMF_5
        '6' -> ToneGenerator.TONE_DTMF_6
        '7' -> ToneGenerator.TONE_DTMF_7
        '8' -> ToneGenerator.TONE_DTMF_8
        '9' -> ToneGenerator.TONE_DTMF_9
        '*' -> ToneGenerator.TONE_DTMF_S
        '#' -> ToneGenerator.TONE_DTMF_P
        else -> ToneGenerator.TONE_PROP_BEEP
    }
}
