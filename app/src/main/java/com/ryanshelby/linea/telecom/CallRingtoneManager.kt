package com.ryanshelby.linea.telecom

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import com.ryanshelby.linea.R
import com.ryanshelby.linea.data.preferences.LineaPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallRingtoneManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: LineaPreferences
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var activeRingtone: Ringtone? = null
    private var activeMediaPlayer: MediaPlayer? = null
    private var isRinging = false
    private var loopJob: Job? = null

    @Synchronized
    fun startRinging(phoneNumber: String, customRingtoneUri: String? = null) {
        if (isRinging) return
        isRinging = true

        scope.launch {
            val ringerMode = audioManager.ringerMode
            val vibrateEnabled = preferences.callVibrationEnabled.first()

            when (ringerMode) {
                AudioManager.RINGER_MODE_SILENT -> {
                    // Mute audio and no vibration
                    return@launch
                }
                AudioManager.RINGER_MODE_VIBRATE -> {
                    // Only vibrate, no sound
                    startVibration()
                }
                AudioManager.RINGER_MODE_NORMAL -> {
                    // Play sound + vibrate if enabled
                    playRingtoneSound(customRingtoneUri)
                    if (vibrateEnabled) {
                        startVibration()
                    }
                }
            }
        }
    }

    private suspend fun playRingtoneSound(customUriStr: String?) {
        try {
            stopRingtoneSound()
            val ringtoneType = preferences.ringtoneType.first()

            if (!customUriStr.isNullOrBlank()) {
                // Contact assigned a specific custom ringtone
                playUriRingtone(Uri.parse(customUriStr))
            } else if (ringtoneType == LineaPreferences.RingtoneType.APP_DEFAULT) {
                // App Default: Linea Signature Ringtone
                playAppDefaultRingtone()
            } else {
                // System Default Ringtone
                val systemUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
                    ?: Settings.System.DEFAULT_RINGTONE_URI
                playUriRingtone(systemUri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playAppDefaultRingtone() {
        try {
            val afd = context.resources.openRawResourceFd(R.raw.linea_ringtone) ?: return
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setLegacyStreamType(AudioManager.STREAM_RING)
                .build()

            val player = MediaPlayer().apply {
                setAudioAttributes(audioAttributes)
                @Suppress("DEPRECATION")
                setAudioStreamType(AudioManager.STREAM_RING)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                isLooping = true
                setVolume(1.0f, 1.0f)
                prepare()
                start()
            }
            afd.close()
            activeMediaPlayer = player
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playUriRingtone(uri: Uri) {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setLegacyStreamType(AudioManager.STREAM_RING)
                .build()

            val ringtone = RingtoneManager.getRingtone(context, uri)
            if (ringtone != null) {
                ringtone.audioAttributes = audioAttributes
                @Suppress("DEPRECATION")
                ringtone.streamType = AudioManager.STREAM_RING
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ringtone.isLooping = true
                    ringtone.volume = 1.0f
                }
                ringtone.play()
                activeRingtone = ringtone

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                    loopJob?.cancel()
                    loopJob = scope.launch {
                        while (isActive && isRinging) {
                            delay(1000)
                            if (isRinging && activeRingtone?.isPlaying == false) {
                                activeRingtone?.play()
                            }
                        }
                    }
                }
            } else {
                val player = MediaPlayer().apply {
                    setAudioAttributes(audioAttributes)
                    @Suppress("DEPRECATION")
                    setAudioStreamType(AudioManager.STREAM_RING)
                    setDataSource(context, uri)
                    isLooping = true
                    setVolume(1.0f, 1.0f)
                    prepare()
                    start()
                }
                activeMediaPlayer = player
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startVibration() {
        try {
            val vibrator = getVibrator() ?: return
            if (!vibrator.hasVibrator()) return

            val pattern = longArrayOf(0, 1000, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(pattern, 0) // 0 = repeat index
                val attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                vibrator.vibrate(effect, attributes)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Synchronized
    fun silence() {
        stopRingtoneSound()
        stopVibration()
    }

    @Synchronized
    fun stopRinging() {
        isRinging = false
        loopJob?.cancel()
        loopJob = null
        stopRingtoneSound()
        stopVibration()
    }

    private fun stopRingtoneSound() {
        try {
            activeMediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            activeMediaPlayer = null
        }

        try {
            activeRingtone?.let {
                if (it.isPlaying) {
                    it.stop()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            activeRingtone = null
        }
    }

    private fun stopVibration() {
        try {
            val vibrator = getVibrator()
            vibrator?.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getVibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
