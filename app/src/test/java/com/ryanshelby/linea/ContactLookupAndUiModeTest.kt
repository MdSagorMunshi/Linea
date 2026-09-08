package com.ryanshelby.linea

import com.ryanshelby.linea.telecom.CallUiMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactLookupAndUiModeTest {

    // Simulates the UI mode logic for screen interactive, keyguard, and launcher state
    private fun evaluateUiMode(
        isScreenInteractive: Boolean,
        isKeyguardLocked: Boolean,
        isOnLauncher: Boolean
    ): CallUiMode {
        if (!isScreenInteractive) return CallUiMode.FULL_SCREEN
        if (isKeyguardLocked) return CallUiMode.FULL_SCREEN
        if (isOnLauncher) return CallUiMode.FULL_SCREEN
        return CallUiMode.MINI_FLOAT
    }

    @Test
    fun testScreenOffAlwaysShowsFullScreen() {
        // When screen is off, incoming call must wake up full screen
        val mode = evaluateUiMode(
            isScreenInteractive = false,
            isKeyguardLocked = false,
            isOnLauncher = false
        )
        assertEquals(CallUiMode.FULL_SCREEN, mode)
    }

    @Test
    fun testLockScreenAlwaysShowsFullScreen() {
        // When phone is on lockscreen, incoming call must show full screen
        val mode = evaluateUiMode(
            isScreenInteractive = true,
            isKeyguardLocked = true,
            isOnLauncher = false
        )
        assertEquals(CallUiMode.FULL_SCREEN, mode)
    }

    @Test
    fun testHomeScreenShowsFullScreen() {
        // When user is on launcher home screen (not inside an app), show full screen
        val mode = evaluateUiMode(
            isScreenInteractive = true,
            isKeyguardLocked = false,
            isOnLauncher = true
        )
        assertEquals(CallUiMode.FULL_SCREEN, mode)
    }

    @Test
    fun testInsideAnotherAppShowsMiniCallFloat() {
        // When user is actively using another app, show Mini Call Float banner!
        val mode = evaluateUiMode(
            isScreenInteractive = true,
            isKeyguardLocked = false,
            isOnLauncher = false
        )
        assertEquals(CallUiMode.MINI_FLOAT, mode)
    }

    @Test
    fun testPhoneNumberDigitNormalization() {
        val rawInternational = "+880 1915-413503"
        val digitsOnly = rawInternational.filter { it.isDigit() }
        assertEquals("8801915413503", digitsOnly)

        val localFormat = "01915413503"
        val localDigits = localFormat.filter { it.isDigit() }

        // Last 7-9 digits match across country-code vs local formatting
        assertEquals(digitsOnly.takeLast(7), localDigits.takeLast(7))
        assertEquals(digitsOnly.takeLast(9), localDigits.takeLast(9))
    }

    enum class SimulatedRingerAction {
        SOUND_AND_VIBRATE,
        SOUND_ONLY,
        VIBRATE_ONLY,
        SILENT
    }

    private fun evaluateRingerAction(
        ringerMode: Int, // 0 = SILENT, 1 = VIBRATE, 2 = NORMAL
        vibrateEnabled: Boolean
    ): SimulatedRingerAction {
        return when (ringerMode) {
            0 -> SimulatedRingerAction.SILENT
            1 -> SimulatedRingerAction.VIBRATE_ONLY
            2 -> if (vibrateEnabled) SimulatedRingerAction.SOUND_AND_VIBRATE else SimulatedRingerAction.SOUND_ONLY
            else -> SimulatedRingerAction.SOUND_AND_VIBRATE
        }
    }

    @Test
    fun testRingerModes() {
        // Normal mode with vibration enabled
        assertEquals(
            SimulatedRingerAction.SOUND_AND_VIBRATE,
            evaluateRingerAction(ringerMode = 2, vibrateEnabled = true)
        )

        // Normal mode with vibration disabled
        assertEquals(
            SimulatedRingerAction.SOUND_ONLY,
            evaluateRingerAction(ringerMode = 2, vibrateEnabled = false)
        )

        // Vibrate mode (phone silenced but set to vibrate)
        assertEquals(
            SimulatedRingerAction.VIBRATE_ONLY,
            evaluateRingerAction(ringerMode = 1, vibrateEnabled = true)
        )
        assertEquals(
            SimulatedRingerAction.VIBRATE_ONLY,
            evaluateRingerAction(ringerMode = 1, vibrateEnabled = false)
        )

        // Completely silent mode
        assertEquals(
            SimulatedRingerAction.SILENT,
            evaluateRingerAction(ringerMode = 0, vibrateEnabled = true)
        )
    }
}
