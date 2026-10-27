package com.ryanshelby.linea

import androidx.compose.ui.graphics.Color
import com.ryanshelby.linea.data.preferences.DesignStyle
import com.ryanshelby.linea.ui.theme.AmoledPalette
import com.ryanshelby.linea.ui.theme.DarkPalette
import com.ryanshelby.linea.ui.theme.LightPalette
import com.ryanshelby.linea.ui.theme.crystalPalette
import com.ryanshelby.linea.ui.components.CrystalOpticsMode
import com.ryanshelby.linea.ui.components.crystalOpticsMode
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.pow

class CrystalDesignTest {
    @Test
    fun glassUsesSupportedRenderingAndSafeFallbacks() {
        assertEquals(CrystalOpticsMode.OPAQUE, crystalOpticsMode(30, true, true))
        assertEquals(CrystalOpticsMode.BLUR, crystalOpticsMode(31, true, true))
        assertEquals(CrystalOpticsMode.BLUR, crystalOpticsMode(32, true, true))
        assertEquals(CrystalOpticsMode.REFRACTION, crystalOpticsMode(33, true, true))
        assertEquals(CrystalOpticsMode.REFRACTION, crystalOpticsMode(37, true, true))
        assertEquals(CrystalOpticsMode.OPAQUE, crystalOpticsMode(37, false, true))
        assertEquals(CrystalOpticsMode.OPAQUE, crystalOpticsMode(37, true, false))
    }

    @Test
    fun existingAndUnknownPreferencesKeepClassic() {
        assertEquals(DesignStyle.CLASSIC, DesignStyle.fromStored(null))
        assertEquals(DesignStyle.CLASSIC, DesignStyle.fromStored("unsupported"))
        assertEquals(DesignStyle.CRYSTAL, DesignStyle.fromStored("CRYSTAL"))
        assertEquals(DesignStyle.CLASSIC, DesignStyle.fromStored("CLASSIC"))
    }

    @Test
    fun crystalPreservesLightDarkAndOledChoices() {
        assertTrue(crystalPalette(LightPalette).isLight)
        assertFalse(crystalPalette(DarkPalette).isLight)
        assertEquals(Color.Black, crystalPalette(AmoledPalette).BackgroundBottom)
    }

    @Test
    fun bodyTextRemainsReadableOverTranslucentGlass() {
        listOf(LightPalette, DarkPalette, AmoledPalette).map(::crystalPalette).forEach { palette ->
            // Test the conservative least opaque glass fill against both background endpoints.
            listOf(palette.BackgroundTop, palette.BackgroundBottom).forEach { background ->
                val glass = blend(palette.GlassFill, background, .58f)
                listOf(palette.TextPrimary, palette.TextSecondary, palette.TextTertiary).forEach { text ->
                    assertTrue("Text contrast must be at least 4.5:1: ${contrast(text, glass)}", contrast(text, glass) >= 4.5)
                }
            }
            val onAccent = if (palette.isLight) Color.White else Color(0xFF142630)
            assertTrue("Selected controls must remain legible", contrast(onAccent, palette.TitaniumBlue) >= 4.5)
        }
    }

    private fun blend(front: Color, back: Color, alpha: Float) = Color(
        front.red * alpha + back.red * (1 - alpha),
        front.green * alpha + back.green * (1 - alpha),
        front.blue * alpha + back.blue * (1 - alpha)
    )

    private fun luminance(color: Color): Double {
        fun linear(c: Float) = if (c <= .04045f) c.toDouble() / 12.92 else ((c + .055) / 1.055).pow(2.4)
        return .2126 * linear(color.red) + .7152 * linear(color.green) + .0722 * linear(color.blue)
    }

    private fun contrast(a: Color, b: Color): Double {
        val l1 = luminance(a)
        val l2 = luminance(b)
        return (maxOf(l1, l2) + .05) / (minOf(l1, l2) + .05)
    }
}
