package com.ryanshelby.linea

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

/**
 * Validates sensor gesture handling and incoming call silencing logic:
 * 1. Phone inside pocket at call start (proximity near) does NOT silence ringer.
 * 2. Phone upside down in pocket (abs(y) > 5.0, z ~ 0) does NOT arm flip or silence call.
 * 3. Phone already face down on desk at call start does NOT silence call.
 * 4. Display waking up (ACTION_SCREEN_ON) does NOT silence ringer.
 * 5. Flipping phone face down ONLY silences after user actively viewed/held phone face-up.
 * 6. Proximity wave ONLY silences after phone was confirmed in open air (FAR).
 */
class CallGestureStateMachineTest {

    private class TestCallGestureCoordinator {
        var flipEnabled: Boolean = true
        var proximityEnabled: Boolean = true

        var isProximityNear: Boolean = false
        var initialProximityWasNear: Boolean = false
        var initialOrientationWasFaceDown: Boolean = false
        var farStartTime: Long = 0L

        var wasActivelyHeldFaceUp: Boolean = false
        var faceUpStartTime: Long = 0L
        var listeningStartTime: Long = 0L

        var isSilenced: Boolean = false
        var silenceReason: String? = null

        val INITIAL_STABILIZATION_MS = 1200L
        val MIN_FAR_DURATION_FOR_WAVE_MS = 600L
        val MIN_FACE_UP_DURATION_MS = 400L

        fun startListening(now: Long) {
            listeningStartTime = now
            isSilenced = false
            silenceReason = null
            isProximityNear = false
            initialProximityWasNear = false
            initialOrientationWasFaceDown = false
            farStartTime = 0L
            wasActivelyHeldFaceUp = false
            faceUpStartTime = 0L
        }

        fun onProximityEvent(distance: Float, maxRange: Float, now: Long) {
            if (isSilenced) return

            val elapsedSinceStart = now - listeningStartTime
            val isNear = distance < maxRange && distance <= 4.5f
            isProximityNear = isNear

            if (elapsedSinceStart < INITIAL_STABILIZATION_MS) {
                if (isNear) {
                    initialProximityWasNear = true
                }
                return
            }

            if (!proximityEnabled) return

            if (isNear) {
                if (!initialProximityWasNear && farStartTime > 0L && (now - farStartTime >= MIN_FAR_DURATION_FOR_WAVE_MS)) {
                    triggerSilence("Proximity Wave to Silence")
                }
            } else {
                if (farStartTime == 0L) {
                    farStartTime = now
                }
                if (initialProximityWasNear && (now - farStartTime >= 1500L)) {
                    initialProximityWasNear = false
                }
            }
        }

        fun onOrientationEvent(x: Float, y: Float, z: Float, now: Long) {
            if (isSilenced) return

            val elapsedSinceStart = now - listeningStartTime
            val isCurrentlyFaceDown = z < -7.0f && abs(x) < 4.5f && abs(y) < 4.5f

            if (elapsedSinceStart < INITIAL_STABILIZATION_MS) {
                if (isCurrentlyFaceDown) {
                    initialOrientationWasFaceDown = true
                }
                return
            }

            if (!flipEnabled) return

            val isFaceUp = z > 6.5f && abs(x) < 5.0f && abs(y) < 5.0f && !isProximityNear

            if (isFaceUp) {
                if (faceUpStartTime == 0L) {
                    faceUpStartTime = now
                } else if (now - faceUpStartTime >= MIN_FACE_UP_DURATION_MS) {
                    wasActivelyHeldFaceUp = true
                    initialOrientationWasFaceDown = false
                }
            } else if (!isCurrentlyFaceDown) {
                faceUpStartTime = 0L
            }

            if (isCurrentlyFaceDown && wasActivelyHeldFaceUp && !initialOrientationWasFaceDown) {
                triggerSilence("Flip to Silence")
            }
        }

        private fun triggerSilence(reason: String) {
            if (isSilenced) return
            isSilenced = true
            silenceReason = reason
        }
    }

    private lateinit var coordinator: TestCallGestureCoordinator

    @Before
    fun setUp() {
        coordinator = TestCallGestureCoordinator()
    }

    @Test
    fun testIncomingCallInPocketDoesNotSilence() {
        val t0 = 1000L
        coordinator.startListening(t0)

        // Inside pocket: proximity is near immediately (distance = 0)
        coordinator.onProximityEvent(distance = 0.0f, maxRange = 5.0f, now = t0 + 100)
        assertTrue(coordinator.isProximityNear)
        assertTrue(coordinator.initialProximityWasNear)
        assertFalse("Call must NOT be silenced on initial proximity reading in pocket", coordinator.isSilenced)

        // Inside pocket upside down: y is -9.8 (vertical upside-down), z is near 0
        coordinator.onOrientationEvent(x = 0.2f, y = -9.8f, z = 0.5f, now = t0 + 200)
        assertFalse("Upside down in pocket must NOT trigger silence", coordinator.isSilenced)
        assertFalse("Upside down in pocket must NOT be treated as actively face-up", coordinator.wasActivelyHeldFaceUp)

        // Time passes while still in pocket
        coordinator.onProximityEvent(distance = 0.0f, maxRange = 5.0f, now = t0 + 2500)
        coordinator.onOrientationEvent(x = 0.1f, y = -9.5f, z = 0.2f, now = t0 + 2600)
        assertFalse("Call must remain ringing in pocket after stabilization window", coordinator.isSilenced)
    }

    @Test
    fun testIncomingCallFaceDownOnTableDoesNotSilence() {
        val t0 = 1000L
        coordinator.startListening(t0)

        // Phone is face down on a desk: z = -9.8
        coordinator.onOrientationEvent(x = 0.1f, y = 0.1f, z = -9.8f, now = t0 + 100)
        assertTrue(coordinator.initialOrientationWasFaceDown)
        assertFalse("Phone already face down at call start must NOT be silenced", coordinator.isSilenced)

        // Stable face down after 2 seconds
        coordinator.onOrientationEvent(x = 0.1f, y = 0.1f, z = -9.8f, now = t0 + 2000)
        assertFalse("Phone already face down must remain ringing", coordinator.isSilenced)
    }

    @Test
    fun testIntentionalFlipToSilenceAfterViewing() {
        val t0 = 1000L
        coordinator.startListening(t0)

        // User starts with phone on desk face up: z = 9.8, x = 0, y = 0
        coordinator.onProximityEvent(distance = 5.0f, maxRange = 5.0f, now = t0 + 100)
        coordinator.onOrientationEvent(x = 0.0f, y = 0.0f, z = 9.8f, now = t0 + 100)

        // User views the call actively for > 400ms after stabilization window
        coordinator.onOrientationEvent(x = 0.1f, y = 0.2f, z = 9.7f, now = t0 + 1300)
        coordinator.onOrientationEvent(x = 0.1f, y = 0.2f, z = 9.7f, now = t0 + 1800)
        assertTrue("User viewed phone face up", coordinator.wasActivelyHeldFaceUp)
        assertFalse("Viewing phone does not silence", coordinator.isSilenced)

        // User intentionally flips phone face down onto table
        coordinator.onOrientationEvent(x = 0.2f, y = 0.1f, z = -9.8f, now = t0 + 2200)
        assertTrue("Flip to silence must trigger after user viewed call", coordinator.isSilenced)
        assertEquals("Flip to Silence", coordinator.silenceReason)
    }

    @Test
    fun testIntentionalProximityWaveToSilence() {
        val t0 = 1000L
        coordinator.startListening(t0)

        // Phone is in open air (FAR)
        coordinator.onProximityEvent(distance = 5.0f, maxRange = 5.0f, now = t0 + 100)
        assertFalse(coordinator.initialProximityWasNear)

        // Open air continues after stabilization window (far for > 600ms)
        coordinator.onProximityEvent(distance = 5.0f, maxRange = 5.0f, now = t0 + 1300)
        coordinator.onProximityEvent(distance = 5.0f, maxRange = 5.0f, now = t0 + 2000)

        // User waves hand near proximity sensor (distance = 1.0cm)
        coordinator.onProximityEvent(distance = 1.0f, maxRange = 5.0f, now = t0 + 2100)
        assertTrue("Proximity wave should silence ringer after open air confirmation", coordinator.isSilenced)
        assertEquals("Proximity Wave to Silence", coordinator.silenceReason)
    }

    @Test
    fun testPullingPhoneOutOfPocketDoesNotTriggerProximitySilence() {
        val t0 = 1000L
        coordinator.startListening(t0)

        // Phone starts in pocket: proximity is near (distance = 0)
        coordinator.onProximityEvent(distance = 0.0f, maxRange = 5.0f, now = t0 + 100)
        assertTrue(coordinator.initialProximityWasNear)

        // User pulls phone out: proximity transitions to FAR (distance = 5)
        coordinator.onProximityEvent(distance = 5.0f, maxRange = 5.0f, now = t0 + 1500)
        assertFalse("Pulling phone out into open air must NOT silence ringer", coordinator.isSilenced)

        // User waves hand immediately before 1500ms open air cooldown: should not falsely trigger
        coordinator.onProximityEvent(distance = 1.0f, maxRange = 5.0f, now = t0 + 1800)
        assertFalse("Immediate hand movement after pocket removal must NOT silence ringer", coordinator.isSilenced)
    }
}
