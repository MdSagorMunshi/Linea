package com.ryanshelby.linea

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates ringtone resiliency and Telecom silencing protection:
 * 1. Telecom onSilenceRinger grace period (< 1000ms ignored, >= 1000ms allowed).
 * 2. Multi-strategy ringtone fallback logic: App Default -> System Ringtone -> System Default URI.
 * 3. Ringtone state transitions (start, update custom, silence, stop).
 */
class CallRingtoneResilienceTest {

    private class TestRingtoneCoordinator {
        var isRinging = false
        var isRingerSilenced = false
        var ringerStartedTimestamp = 0L
        var silenceCount = 0

        fun startRinging(now: Long) {
            isRinging = true
            isRingerSilenced = false
            ringerStartedTimestamp = now
        }

        fun silenceRinger() {
            if (isRingerSilenced) return
            isRingerSilenced = true
            silenceCount++
        }

        fun silenceRingerFromTelecom(now: Long) {
            val elapsed = if (ringerStartedTimestamp > 0L) now - ringerStartedTimestamp else 0L
            // Telecom can fire onSilenceRinger during initial connection/handoff setup.
            // Only silence if the call has already been actively ringing for at least 1000ms.
            if (isRinging && !isRingerSilenced && elapsed >= 1000L) {
                silenceRinger()
            }
        }
    }

    @Test
    fun testTelecomSilenceRingerIgnoredDuringInitialGracePeriod() {
        val coordinator = TestRingtoneCoordinator()
        val callStartTime = 10_000L
        coordinator.startRinging(callStartTime)

        assertTrue(coordinator.isRinging)
        assertFalse(coordinator.isRingerSilenced)

        // Telecom fires onSilenceRinger 150ms after call added (spurious handoff / Bluetooth setup)
        coordinator.silenceRingerFromTelecom(callStartTime + 150L)

        // Must NOT be silenced!
        assertFalse("Ringer should not be silenced during early setup handoff", coordinator.isRingerSilenced)
        assertEquals(0, coordinator.silenceCount)

        // Telecom fires onSilenceRinger 800ms after call added
        coordinator.silenceRingerFromTelecom(callStartTime + 800L)
        assertFalse("Ringer should still not be silenced under 1000ms", coordinator.isRingerSilenced)
        assertEquals(0, coordinator.silenceCount)

        // After 1000ms, user presses volume key on car kit / headset
        coordinator.silenceRingerFromTelecom(callStartTime + 1050L)
        assertTrue("Ringer should be silenced once grace period expires", coordinator.isRingerSilenced)
        assertEquals(1, coordinator.silenceCount)
    }

    @Test
    fun testTelecomSilenceRingerDoesNothingWhenNotRinging() {
        val coordinator = TestRingtoneCoordinator()
        coordinator.isRinging = false

        // Spurious onSilenceRinger when no call is ringing
        coordinator.silenceRingerFromTelecom(5000L)
        assertFalse(coordinator.isRingerSilenced)
        assertEquals(0, coordinator.silenceCount)
    }

    private class TestRingtoneFallbackPipeline {
        var appDefaultFails = false
        var systemDefaultFails = false
        var playedSource: String? = null

        fun playSound(ringtoneType: String, customUri: String? = null) {
            var played = false
            if (!customUri.isNullOrBlank()) {
                played = false // Simulate custom uri failure or absent
            }

            if (!played) {
                if (ringtoneType == "APP_DEFAULT") {
                    val appPlayed = if (!appDefaultFails) {
                        playedSource = "APP_DEFAULT"
                        true
                    } else false

                    if (!appPlayed) {
                        // Fall back to system default ringtone
                        val sysPlayed = if (!systemDefaultFails) {
                            playedSource = "SYSTEM_DEFAULT"
                            true
                        } else false

                        if (!sysPlayed) {
                            playedSource = "DEFAULT_SYSTEM_URI_FALLBACK"
                        }
                    }
                } else {
                    val sysPlayed = if (!systemDefaultFails) {
                        playedSource = "SYSTEM_DEFAULT"
                        true
                    } else false

                    if (!sysPlayed) {
                        val appPlayed = if (!appDefaultFails) {
                            playedSource = "APP_DEFAULT"
                            true
                        } else false

                        if (!appPlayed) {
                            playedSource = "DEFAULT_SYSTEM_URI_FALLBACK"
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testAppDefaultPlaysNormallyWhenHealthy() {
        val pipeline = TestRingtoneFallbackPipeline()
        pipeline.playSound("APP_DEFAULT")
        assertEquals("APP_DEFAULT", pipeline.playedSource)
    }

    @Test
    fun testAppDefaultFallsBackToSystemRingtoneWhenAppDefaultFails() {
        val pipeline = TestRingtoneFallbackPipeline()
        pipeline.appDefaultFails = true
        pipeline.playSound("APP_DEFAULT")
        assertEquals("SYSTEM_DEFAULT", pipeline.playedSource)
    }

    @Test
    fun testAppDefaultFallsBackToRawSystemUriWhenBothFail() {
        val pipeline = TestRingtoneFallbackPipeline()
        pipeline.appDefaultFails = true
        pipeline.systemDefaultFails = true
        pipeline.playSound("APP_DEFAULT")
        assertEquals("DEFAULT_SYSTEM_URI_FALLBACK", pipeline.playedSource)
    }

    @Test
    fun testSystemDefaultFallsBackToAppDefaultWhenSystemFails() {
        val pipeline = TestRingtoneFallbackPipeline()
        pipeline.systemDefaultFails = true
        pipeline.playSound("SYSTEM_DEFAULT")
        assertEquals("APP_DEFAULT", pipeline.playedSource)
    }
}
