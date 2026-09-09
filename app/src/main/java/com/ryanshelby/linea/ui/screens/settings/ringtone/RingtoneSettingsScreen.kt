package com.ryanshelby.linea.ui.screens.settings.ringtone

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.R
import com.ryanshelby.linea.data.preferences.LineaPreferences
import com.ryanshelby.linea.ui.components.FrostedGlassBox
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography
import kotlinx.coroutines.launch

@Composable
fun RingtoneSettingsScreen(
    preferences: LineaPreferences,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val currentRingtoneType by preferences.ringtoneType.collectAsState(
        initial = LineaPreferences.RingtoneType.APP_DEFAULT
    )

    // Dynamic System Default Ringtone Name
    val systemRingtoneTitle = remember(context) {
        try {
            val uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
                ?: Settings.System.DEFAULT_RINGTONE_URI
            val r = RingtoneManager.getRingtone(context, uri)
            r?.getTitle(context) ?: "System Default"
        } catch (_: Exception) {
            "System Default"
        }
    }

    // Audio Preview State: null = stopped, "APP_DEFAULT" = playing Linea, "SYSTEM_DEFAULT" = playing system
    var previewingMode by remember { mutableStateOf<String?>(null) }
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    val stopPreview = remember {
        {
            previewPlayer?.let { player ->
                try {
                    if (player.isPlaying) {
                        player.stop()
                    }
                    player.release()
                } catch (_: Exception) {}
            }
            previewPlayer = null
            previewingMode = null
        }
    }

    // Lifecycle cleanup: stop audio when user exits screen
    DisposableEffect(Unit) {
        onDispose {
            stopPreview()
        }
    }

    val playPreview = remember(context) {
        { mode: String ->
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            if (previewingMode == mode) {
                stopPreview()
            } else {
                stopPreview()
                try {
                    val audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setLegacyStreamType(AudioManager.STREAM_RING)
                        .build()

                    val player = if (mode == LineaPreferences.RingtoneType.APP_DEFAULT) {
                        val afd = context.resources.openRawResourceFd(R.raw.linea_ringtone)
                        if (afd != null) {
                            MediaPlayer().apply {
                                setAudioAttributes(audioAttributes)
                                @Suppress("DEPRECATION")
                                setAudioStreamType(AudioManager.STREAM_RING)
                                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                                afd.close()
                                setVolume(1.0f, 1.0f)
                                prepare()
                            }
                        } else null
                    } else {
                        val uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
                            ?: Settings.System.DEFAULT_RINGTONE_URI
                        MediaPlayer().apply {
                            setAudioAttributes(audioAttributes)
                            @Suppress("DEPRECATION")
                            setAudioStreamType(AudioManager.STREAM_RING)
                            setDataSource(context, uri)
                            setVolume(1.0f, 1.0f)
                            prepare()
                        }
                    }

                    if (player != null) {
                        player.setOnCompletionListener {
                            stopPreview()
                        }
                        player.start()
                        previewPlayer = player
                        previewingMode = mode
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    stopPreview()
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(LineaColors.BackgroundTop, LineaColors.BackgroundBottom)
                )
            )
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = LineaDimensions.ScreenPadding)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

        // Top Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    stopPreview()
                    onNavigateBack()
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Navigate Back",
                    tint = LineaColors.TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Ringtone",
                    style = LineaTypography.headlineLarge,
                    color = LineaColors.TextPrimary
                )
                Text(
                    text = "Cellular incoming call audio",
                    style = LineaTypography.bodyMedium,
                    color = LineaColors.TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Active Ringtone Summary Banner
        FrostedGlassBox(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(LineaColors.TitaniumBlue.copy(alpha = 0.15f))
                        .border(1.dp, LineaColors.TitaniumBlue.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.GraphicEq,
                        contentDescription = null,
                        tint = LineaColors.TitaniumBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (currentRingtoneType == LineaPreferences.RingtoneType.APP_DEFAULT) {
                                "Linea Signature"
                            } else {
                                systemRingtoneTitle
                            },
                            style = LineaTypography.titleLarge,
                            color = LineaColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(LineaColors.TitaniumBlue.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ACTIVE",
                                style = LineaTypography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.ExtraBold),
                                color = LineaColors.TitaniumBlue
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (currentRingtoneType == LineaPreferences.RingtoneType.APP_DEFAULT) {
                            "Crafted ambient chime (Recommended)"
                        } else {
                            "Using standard Android device ringtone"
                        },
                        style = LineaTypography.bodySmall,
                        color = LineaColors.TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "RINGTONE SOURCES",
            style = LineaTypography.labelMedium.copy(letterSpacing = 1.2.sp),
            color = LineaColors.TextTertiary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Option 1: Linea Signature (App Default)
        RingtoneOptionCard(
            title = "Linea Signature",
            subtitle = "Custom ambient chime crafted exclusively for Linea",
            tag = "RECOMMENDED",
            icon = Icons.Filled.MusicNote,
            isSelected = currentRingtoneType == LineaPreferences.RingtoneType.APP_DEFAULT,
            isPlayingPreview = previewingMode == LineaPreferences.RingtoneType.APP_DEFAULT,
            onSelect = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                scope.launch {
                    preferences.setRingtoneType(LineaPreferences.RingtoneType.APP_DEFAULT)
                }
            },
            onTogglePreview = {
                playPreview(LineaPreferences.RingtoneType.APP_DEFAULT)
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Option 2: System Default
        RingtoneOptionCard(
            title = "System Default",
            subtitle = systemRingtoneTitle,
            tag = "ANDROID SYSTEM",
            icon = Icons.Filled.PhoneAndroid,
            isSelected = currentRingtoneType == LineaPreferences.RingtoneType.SYSTEM_DEFAULT,
            isPlayingPreview = previewingMode == LineaPreferences.RingtoneType.SYSTEM_DEFAULT,
            onSelect = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                scope.launch {
                    preferences.setRingtoneType(LineaPreferences.RingtoneType.SYSTEM_DEFAULT)
                }
            },
            onTogglePreview = {
                playPreview(LineaPreferences.RingtoneType.SYSTEM_DEFAULT)
            }
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Telecom & Audio Guidance Note
        FrostedGlassBox(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = LineaColors.TextTertiary,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Contact Overrides & Silent Modes",
                        style = LineaTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = LineaColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Custom ringtones set on specific contact cards will always take priority over the default ringtone. In silent or vibrate mode, Linea automatically mutes ringtone playback and vibrates according to your preferences.",
                        style = LineaTypography.bodySmall,
                        color = LineaColors.TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun RingtoneOptionCard(
    title: String,
    subtitle: String,
    tag: String,
    icon: ImageVector,
    isSelected: Boolean,
    isPlayingPreview: Boolean,
    onSelect: () -> Unit,
    onTogglePreview: () -> Unit
) {
    val borderColor = if (isSelected) LineaColors.TitaniumBlue else LineaColors.GlassBorder
    val borderWidth = if (isSelected) 1.5.dp else 1.dp

    FrostedGlassBox(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Leading Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) LineaColors.TitaniumBlue.copy(alpha = 0.2f)
                            else LineaColors.GlassFill
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) LineaColors.TitaniumBlue else LineaColors.TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Titles & Tag
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = title,
                            style = LineaTypography.titleMedium,
                            color = LineaColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = tag,
                                style = LineaTypography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold),
                                color = LineaColors.TextTertiary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = subtitle,
                        style = LineaTypography.bodySmall,
                        color = LineaColors.TextSecondary
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Radio Indicator
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .border(
                            2.dp,
                            if (isSelected) LineaColors.TitaniumBlue else LineaColors.TextTertiary.copy(alpha = 0.5f),
                            CircleShape
                        )
                        .background(if (isSelected) LineaColors.TitaniumBlue else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action row: Preview Player Button & Waveform
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Interactive Preview Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isPlayingPreview) LineaColors.TitaniumBlue.copy(alpha = 0.25f)
                            else Color.White.copy(alpha = 0.08f)
                        )
                        .border(
                            1.dp,
                            if (isPlayingPreview) LineaColors.TitaniumBlue else LineaColors.GlassBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable(onClick = onTogglePreview)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlayingPreview) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlayingPreview) "Stop Preview" else "Play Preview",
                            tint = if (isPlayingPreview) LineaColors.TitaniumBlue else LineaColors.TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isPlayingPreview) "Stop Preview" else "Play Preview",
                            style = LineaTypography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isPlayingPreview) LineaColors.TitaniumBlue else LineaColors.TextPrimary
                        )
                    }
                }

                // Animated Equalizer Visualizer when playing
                AnimatedVisibility(
                    visible = isPlayingPreview,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    EqualizerVisualizer()
                }
            }
        }
    }
}

@Composable
private fun EqualizerVisualizer() {
    val transition = rememberInfiniteTransition(label = "equalizer")
    val bar1Height by transition.animateFloat(
        initialValue = 6f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val bar2Height by transition.animateFloat(
        initialValue = 18f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val bar3Height by transition.animateFloat(
        initialValue = 10f,
        targetValue = 22f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )
    val bar4Height by transition.animateFloat(
        initialValue = 16f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar4"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.height(24.dp)
    ) {
        listOf(bar1Height, bar2Height, bar3Height, bar4Height).forEach { h ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(h.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(LineaColors.TitaniumBlue)
            )
        }
    }
}
