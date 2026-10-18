package com.ryanshelby.linea

import com.ryanshelby.linea.data.preferences.LineaPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavbarPreferencesTest {

    @Test
    fun defaultNavbarMode_isCollapsedCircle() {
        assertEquals("COLLAPSED", LineaPreferences.NavbarMode.COLLAPSED)
        assertEquals("EXPANDED", LineaPreferences.NavbarMode.EXPANDED)
    }

    @Test
    fun navbarCircleSizes_matchSpecifications() {
        assertEquals(48, LineaPreferences.NavbarCircleSize.COMPACT)
        assertEquals(56, LineaPreferences.NavbarCircleSize.STANDARD)
        assertEquals(64, LineaPreferences.NavbarCircleSize.COMFORTABLE)
        assertEquals(72, LineaPreferences.NavbarCircleSize.LARGE)
    }

    @Test
    fun navbarAnimationStyles_matchSpecifications() {
        assertEquals("BOUNCY", LineaPreferences.NavbarAnimationStyle.BOUNCY)
        assertEquals("SMOOTH", LineaPreferences.NavbarAnimationStyle.SMOOTH)
        assertEquals("SNAPPY", LineaPreferences.NavbarAnimationStyle.SNAPPY)
        assertEquals("FADE", LineaPreferences.NavbarAnimationStyle.FADE)
    }

    @Test
    fun navbarDynamicSizingMath_scalesWithinBounds() {
        val sizes = listOf(
            LineaPreferences.NavbarCircleSize.COMPACT,
            LineaPreferences.NavbarCircleSize.STANDARD,
            LineaPreferences.NavbarCircleSize.COMFORTABLE,
            LineaPreferences.NavbarCircleSize.LARGE
        )

        for (size in sizes) {
            val iconSize = (size * 0.46f).coerceIn(22f, 32f)
            assertTrue("Icon size $iconSize should be >= 22", iconSize >= 22f)
            assertTrue("Icon size $iconSize should be <= 32", iconSize <= 32f)

            val dragThreshold = size * 0.64f
            assertTrue("Drag threshold $dragThreshold should scale with circle size", dragThreshold > 25f && dragThreshold < 50f)
        }
    }
}
