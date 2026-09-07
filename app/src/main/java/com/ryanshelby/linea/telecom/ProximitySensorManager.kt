package com.ryanshelby.linea.telecom

import android.content.Context
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProximitySensorManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private var wakeLock: PowerManager.WakeLock? = null

    init {
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

    fun onCallStateOrAudioChanged(isCallActive: Boolean, isSpeakerOn: Boolean) {
        if (isCallActive && !isSpeakerOn) {
            acquire()
        } else {
            release()
        }
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

    fun release() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release(PowerManager.RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
