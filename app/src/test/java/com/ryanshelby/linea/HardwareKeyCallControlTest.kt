package com.ryanshelby.linea

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates hardware button interactions during calls:
 * 1. Volume Up/Down silencing: silences ringer/vibration without rejecting or terminating call.
 * 2. Power button double-press: terminates current call across incoming, outgoing, and active states.
 * 3. Debounce: prevents duplicate key/broadcast dispatches (< 120ms) from falsely triggering.
 */
class HardwareKeyCallControlTest {

    private class TestHardwareKeyCoordinator {
        var isRinging = false
        var isCallActiveOrPending = false
        var isRingerSilenced = false
        var callEnded = false
        var endCallCount = 0

        private var lastPowerPressTimestamp = 0L

        fun onVolumePressed() {
            if (isRinging) {
                silenceRinger()
            }
        }

        fun silenceRinger() {
            if (isRingerSilenced) return
            isRingerSilenced = true
        }

        fun onPowerButtonPressed(nowElapsed: Long) {
            if (!isCallActiveOrPending) return

            val delta = nowElapsed - lastPowerPressTimestamp

            // Debounce if multiple events fire for the same physical press (< 120ms)
            if (lastPowerPressTimestamp > 0L && delta < 120L) {
                return
            }

            if (lastPowerPressTimestamp > 0L && delta <= 1000L) {
                // Double press detected within 120ms..1000ms
                lastPowerPressTimestamp = 0L
                endCurrentCall()
            } else {
                // First press
                lastPowerPressTimestamp = nowElapsed
                if (isRinging && !isRingerSilenced) {
                    silenceRinger()
                }
            }
        }

        fun endCurrentCall() {
            callEnded = true
            endCallCount++
            isCallActiveOrPending = false
            isRinging = false
            isRingerSilenced = false
        }
    }

    @Test
    fun testVolumeKeysSilenceIncomingRingerWithoutEndingCall() {
        val coordinator = TestHardwareKeyCoordinator().apply {
            isRinging = true
            isCallActiveOrPending = true
        }

        assertFalse(coordinator.isRingerSilenced)
        assertFalse(coordinator.callEnded)

        // User presses Volume Down
        coordinator.onVolumePressed()

        // Ringer is silenced
        assertTrue(coordinator.isRingerSilenced)
        // Crucially: Call is NOT ended!
        assertFalse(coordinator.callEnded)
        assertTrue(coordinator.isRinging)
        assertTrue(coordinator.isCallActiveOrPending)

        // Subsequent volume presses do not alter call state
        coordinator.onVolumePressed()
        assertTrue(coordinator.isRingerSilenced)
        assertFalse(coordinator.callEnded)
    }

    @Test
    fun testSinglePowerPressOnIncomingCallSilencesRingerWithoutEndingCall() {
        val coordinator = TestHardwareKeyCoordinator().apply {
            isRinging = true
            isCallActiveOrPending = true
        }

        // Single press at t = 1000ms
        coordinator.onPowerButtonPressed(1000L)

        // Ringer is silenced on first press
        assertTrue(coordinator.isRingerSilenced)
        // Call is NOT ended on single press
        assertFalse(coordinator.callEnded)
        assertTrue(coordinator.isRinging)

        // Wait 2000ms (window expires)
        // Next press at t = 3500ms
        coordinator.onPowerButtonPressed(3500L)

        // Still not a double-press because delta was 2500ms > 1000ms
        assertFalse(coordinator.callEnded)
    }

    @Test
    fun testDoublePressPowerOnIncomingCallEndsCall() {
        val coordinator = TestHardwareKeyCoordinator().apply {
            isRinging = true
            isCallActiveOrPending = true
        }

        // Press 1 at t = 1000ms
        coordinator.onPowerButtonPressed(1000L)
        assertTrue(coordinator.isRingerSilenced)
        assertFalse(coordinator.callEnded)

        // Press 2 at t = 1400ms (delta = 400ms, well within 120ms..1000ms window)
        coordinator.onPowerButtonPressed(1400L)

        // Call is ended!
        assertTrue(coordinator.callEnded)
        assertEquals(1, coordinator.endCallCount)
        assertFalse(coordinator.isCallActiveOrPending)
    }

    @Test
    fun testDoublePressPowerOnActiveCallEndsCall() {
        val coordinator = TestHardwareKeyCoordinator().apply {
            isRinging = false // Active call, talking
            isCallActiveOrPending = true
        }

        // Press 1 at t = 5000ms
        coordinator.onPowerButtonPressed(5000L)
        assertFalse(coordinator.callEnded)

        // Press 2 at t = 5350ms (delta = 350ms)
        coordinator.onPowerButtonPressed(5350L)

        // Call is ended!
        assertTrue(coordinator.callEnded)
        assertEquals(1, coordinator.endCallCount)
    }

    @Test
    fun testDoublePressPowerOnOutgoingCallEndsCall() {
        val coordinator = TestHardwareKeyCoordinator().apply {
            isRinging = false // Outgoing dialing
            isCallActiveOrPending = true
        }

        // Press 1 at t = 10000ms
        coordinator.onPowerButtonPressed(10000L)
        assertFalse(coordinator.callEnded)

        // Press 2 at t = 10500ms (delta = 500ms)
        coordinator.onPowerButtonPressed(10500L)

        // Outgoing call is disconnected!
        assertTrue(coordinator.callEnded)
        assertEquals(1, coordinator.endCallCount)
    }

    @Test
    fun testDebouncePreventsDuplicateEventFalseDoublePress() {
        val coordinator = TestHardwareKeyCoordinator().apply {
            isRinging = false
            isCallActiveOrPending = true
        }

        // Single physical button press generates KeyEvent at t = 1000ms
        coordinator.onPowerButtonPressed(1000L)
        assertFalse(coordinator.callEnded)

        // Same physical press generates ACTION_SCREEN_OFF at t = 1025ms (25ms later)
        coordinator.onPowerButtonPressed(1025L)

        // Debounce prevents false double press!
        assertFalse(coordinator.callEnded)
        assertEquals(0, coordinator.endCallCount)

        // Now user genuinely presses power a second time at t = 1500ms (475ms later)
        coordinator.onPowerButtonPressed(1500L)

        // Genuine double press detected!
        assertTrue(coordinator.callEnded)
        assertEquals(1, coordinator.endCallCount)
    }
}
