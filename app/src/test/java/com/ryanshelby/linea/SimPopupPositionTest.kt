package com.ryanshelby.linea

import com.ryanshelby.linea.data.preferences.LineaPreferences
import org.junit.Assert.assertEquals
import org.junit.Test

class SimPopupPositionTest {

    private class MockPreferences(
        var currentPosition: String = LineaPreferences.SimPopupPosition.MIDDLE
    ) {
        val simSelectorPosition: String
            get() = currentPosition

        fun setSimSelectorPosition(position: String) {
            currentPosition = position
        }
    }

    @Test
    fun defaultPosition_isMiddle() {
        val prefs = MockPreferences()
        assertEquals(LineaPreferences.SimPopupPosition.MIDDLE, prefs.simSelectorPosition)
        assertEquals("MIDDLE", LineaPreferences.SimPopupPosition.MIDDLE)
    }

    @Test
    fun setPosition_toBottom_updatesSuccessfully() {
        val prefs = MockPreferences()
        prefs.setSimSelectorPosition(LineaPreferences.SimPopupPosition.BOTTOM)
        assertEquals(LineaPreferences.SimPopupPosition.BOTTOM, prefs.simSelectorPosition)
        assertEquals("BOTTOM", LineaPreferences.SimPopupPosition.BOTTOM)
    }

    @Test
    fun setPosition_toMiddle_updatesSuccessfully() {
        val prefs = MockPreferences(currentPosition = LineaPreferences.SimPopupPosition.BOTTOM)
        assertEquals(LineaPreferences.SimPopupPosition.BOTTOM, prefs.simSelectorPosition)

        prefs.setSimSelectorPosition(LineaPreferences.SimPopupPosition.MIDDLE)
        assertEquals(LineaPreferences.SimPopupPosition.MIDDLE, prefs.simSelectorPosition)
    }
}
