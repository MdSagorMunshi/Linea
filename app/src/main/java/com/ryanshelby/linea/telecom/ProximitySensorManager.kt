package com.ryanshelby.linea.telecom

import android.content.Context
import android.os.PowerManager
import com.ryanshelby.linea.data.preferences.LineaPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProximitySensorManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: LineaPreferences
) {

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private var wakeLock: PowerManager.WakeLock? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var isProximityPrefEnabled = true

    init {
        scope.launch {
            preferences.proximitySensorEnabled.collect { enabled ->
                isProximityPrefEnabled = enabled
                if (!enabled) {
                    release()
                }
            }
        }
        try {
            if (powerManager?.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK) == true) {
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                    "linea:proximity_screen_off"
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun onCallStateOrAudioChanged(isCallActive: Boolean, audioRoute: LineaAudioRoute) {
        val isEarpiece = audioRoute == LineaAudioRoute.EARPIECE
        if (isCallActive && isEarpiece && isProximityPrefEnabled) {
            acquire()
        } else {
            release()
        }
    }

    fun onCallStateOrAudioChanged(isCallActive: Boolean, isSpeakerOn: Boolean) {
        val route = if (isSpeakerOn) LineaAudioRoute.SPEAKER else LineaAudioRoute.EARPIECE
        onCallStateOrAudioChanged(isCallActive, route)
    }

    private fun acquire() {
        try {
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(2 * 60 * 60 * 1000L) // 2 hours max
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release(waitForNoProximity: Boolean = false) {
        try {
            if (wakeLock?.isHeld == true) {
                if (waitForNoProximity) {
                    wakeLock?.release(PowerManager.RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY)
                } else {
                    wakeLock?.release()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
