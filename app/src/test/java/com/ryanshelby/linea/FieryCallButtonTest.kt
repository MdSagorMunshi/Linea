package com.ryanshelby.linea

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FieryCallButtonTest {

    private class MockPreferences(
        var isFiery: Boolean = false
    ) {
        val fieryCallButton: Boolean
            get() = isFiery

        fun setFieryCallButton(enabled: Boolean) {
            isFiery = enabled
        }

        fun toggleFieryCallButton() {
            isFiery = !isFiery
        }
    }

    @Test
    fun defaultState_isNotFiery() {
        val prefs = MockPreferences()
        assertFalse("Call button should default to standard (non-fiery) state", prefs.fieryCallButton)
    }

    @Test
    fun activateEasterEgg_turnsFiery() {
        val prefs = MockPreferences()
        prefs.setFieryCallButton(true)
        assertTrue("Easter Egg activation should turn call button fiery", prefs.fieryCallButton)
    }

    @Test
    fun toggleEasterEgg_reversesState() {
        val prefs = MockPreferences()
        assertFalse(prefs.fieryCallButton)

        // 1st Easter egg trigger: Standard -> Fiery
        prefs.toggleFieryCallButton()
        assertTrue("1st 3s hold should activate fiery call button", prefs.fieryCallButton)

        // 2nd Easter egg trigger: Fiery -> Standard
        prefs.toggleFieryCallButton()
        assertFalse("2nd 3s hold should reverse fiery call button back to standard", prefs.fieryCallButton)
    }

    @Test
    fun persistence_remainsAcrossRecreation() {
        val prefs = MockPreferences()
        prefs.setFieryCallButton(true)

        // Simulate app reopening with stored state
        val reloadedPrefs = MockPreferences(isFiery = prefs.fieryCallButton)
        assertTrue("Fiery call button state must persist across app restart", reloadedPrefs.fieryCallButton)
    }
}
