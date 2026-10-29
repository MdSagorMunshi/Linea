package com.ryanshelby.linea

import androidx.compose.ui.graphics.Color
import com.ryanshelby.linea.data.preferences.DesignStyle
import com.ryanshelby.linea.ui.theme.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.pow

class SixThemesSystemTest {

    @Test
    fun allThemesAreDistinctAndRegistered() {
        val themes = DesignStyle.appThemes
        assertEquals(8, themes.size)
        assertTrue(themes.contains(DesignStyle.CLASSIC))
        assertTrue(themes.contains(DesignStyle.CRYSTAL))
        assertTrue(themes.contains(DesignStyle.NEUMORPHISM))
        assertTrue(themes.contains(DesignStyle.ORGANIC))
        assertTrue(themes.contains(DesignStyle.FINTECH))
        assertTrue(themes.contains(DesignStyle.MATERIAL3))
        assertTrue(themes.contains(DesignStyle.NEO_BRUTALISM))
        assertTrue(themes.contains(DesignStyle.HANDWRITTEN))
    }

    @Test
    fun persistenceFromStoredParsesAllThemesAndFallbacks() {
        assertEquals(DesignStyle.CLASSIC, DesignStyle.fromStored(null))
        assertEquals(DesignStyle.CLASSIC, DesignStyle.fromStored(""))
        assertEquals(DesignStyle.CLASSIC, DesignStyle.fromStored("unknown_theme"))
        assertEquals(DesignStyle.CLASSIC, DesignStyle.fromStored("CLASSIC"))
        assertEquals(DesignStyle.CRYSTAL, DesignStyle.fromStored("CRYSTAL"))
        assertEquals(DesignStyle.NEUMORPHISM, DesignStyle.fromStored("NEUMORPHISM"))
        assertEquals(DesignStyle.ORGANIC, DesignStyle.fromStored("ORGANIC"))
        assertEquals(DesignStyle.ORGANIC, DesignStyle.fromStored("BIOPHILIC"))
        assertEquals(DesignStyle.FINTECH, DesignStyle.fromStored("FINTECH"))
        assertEquals(DesignStyle.FINTECH, DesignStyle.fromStored("FINTECH_MINIMAL"))
        assertEquals(DesignStyle.MATERIAL3, DesignStyle.fromStored("MATERIAL3"))
        assertEquals(DesignStyle.MATERIAL3, DesignStyle.fromStored("EXPRESSIVE"))
        assertEquals(DesignStyle.NEO_BRUTALISM, DesignStyle.fromStored("NEO_BRUTALISM"))
        assertEquals(DesignStyle.NEO_BRUTALISM, DesignStyle.fromStored("NEOBRUTALISM"))
        assertEquals(DesignStyle.HANDWRITTEN, DesignStyle.fromStored("HANDWRITTEN"))
        assertEquals(DesignStyle.HANDWRITTEN, DesignStyle.fromStored("SKETCH"))
    }

    @Test
    fun eachThemeProvidesCompleteLightDarkAndAmoledPalettes() {
        DesignStyle.appThemes.forEach { style ->
            val dark = resolveThemePalette(style, "DARK", systemInDark = true)
            val light = resolveThemePalette(style, "LIGHT", systemInDark = false)
            val amoled = resolveThemePalette(style, "AMOLED", systemInDark = true)

            assertFalse("${style.displayName} DARK palette must be dark", dark.isLight)
            assertTrue("${style.displayName} LIGHT palette must be light", light.isLight)
            assertFalse("${style.displayName} AMOLED palette must be dark", amoled.isLight)

            // Verify essential semantic tokens are non-transparent
            assertNotEquals(Color.Transparent, dark.BackgroundTop)
            assertNotEquals(Color.Transparent, dark.BackgroundBottom)
            assertNotEquals(Color.Transparent, dark.TitaniumBlue)
            assertNotEquals(Color.Transparent, dark.TextPrimary)
            assertNotEquals(Color.Transparent, dark.Danger)
            assertNotEquals(Color.Transparent, dark.Success)

            assertNotEquals(Color.Transparent, light.BackgroundTop)
            assertNotEquals(Color.Transparent, light.BackgroundBottom)
            assertNotEquals(Color.Transparent, light.TitaniumBlue)
            assertNotEquals(Color.Transparent, light.TextPrimary)

            assertNotEquals(Color.Transparent, amoled.BackgroundTop)
            assertNotEquals(Color.Transparent, amoled.BackgroundBottom)
            assertNotEquals(Color.Transparent, amoled.TitaniumBlue)
        }
    }

    @Test
    fun allThemesMaintainAccessibleTextContrast() {
        DesignStyle.appThemes.forEach { style ->
            listOf("DARK", "LIGHT", "AMOLED").forEach { mode ->
                val palette = resolveThemePalette(style, mode, systemInDark = mode != "LIGHT")
                val textPrimary = palette.TextPrimary
                val bgTop = palette.BackgroundTop
                val bgBottom = palette.BackgroundBottom
                val surface = palette.NeuSurfaceRaised

                val contrastTop = contrast(textPrimary, bgTop)
                val contrastBottom = contrast(textPrimary, bgBottom)
                val contrastSurface = contrast(textPrimary, surface)

                assertTrue(
                    "Primary text contrast against BackgroundTop must be >= 4.5:1 in ${style.displayName} ($mode). Got $contrastTop",
                    contrastTop >= 4.5
                )
                assertTrue(
                    "Primary text contrast against BackgroundBottom must be >= 4.5:1 in ${style.displayName} ($mode). Got $contrastBottom",
                    contrastBottom >= 4.5
                )
                assertTrue(
                    "Primary text contrast against NeuSurfaceRaised must be >= 4.0:1 in ${style.displayName} ($mode). Got $contrastSurface",
                    contrastSurface >= 4.0
                )
            }
        }
    }

    @Test
    fun allThemesProvideDistinctTypographyConfigurations() {
        DesignStyle.appThemes.forEach { style ->
            val typography = resolveThemeTypography(style)
            assertNotNull(typography.headlineLarge)
            assertNotNull(typography.headlineMedium)
            assertNotNull(typography.titleMedium)
            assertNotNull(typography.bodyLarge)
            assertNotNull(typography.bodyMedium)
            assertNotNull(typography.displayLarge)
        }
    }

    @Test
    fun allThemesProvideDistinctShapesAndComponentShapes() {
        DesignStyle.appThemes.forEach { style ->
            val shapes = resolveThemeShapes(style)
            assertNotNull(shapes.small)
            assertNotNull(shapes.medium)
            assertNotNull(shapes.large)
            assertNotNull(shapes.extraLarge)

            val keyShape = resolveDialpadKeyShape(style)
            assertNotNull(keyShape)

            val callButtonShape = resolveCallButtonShape(style)
            assertNotNull(callButtonShape)
        }
    }

    @Test
    fun motionSystemAdaptsToReducedMotion() {
        DesignStyle.appThemes.forEach { style ->
            val normalMotion = resolveThemeMotion(style, reduceAnimations = false)
            val reducedMotion = resolveThemeMotion(style, reduceAnimations = true)

            // When reduced motion is requested, scale should remain 1.0f and offsets 0.dp
            assertEquals(1.0f, reducedMotion.pressScale, 0.001f)
            assertEquals(0f, reducedMotion.pressOffsetX.value, 0.001f)
            assertEquals(0f, reducedMotion.pressOffsetY.value, 0.001f)
            assertEquals(0f, reducedMotion.pressRotationZ, 0.001f)

            // Verify normal motion has theme-specific character
            when (style) {
                DesignStyle.NEO_BRUTALISM -> {
                    assertEquals(3f, normalMotion.pressOffsetX.value, 0.001f)
                    assertEquals(3f, normalMotion.pressOffsetY.value, 0.001f)
                }
                DesignStyle.HANDWRITTEN -> {
                    assertEquals(-1.5f, normalMotion.pressRotationZ, 0.001f)
                }
                DesignStyle.FINTECH -> {
                    assertEquals(0.98f, normalMotion.pressScale, 0.001f)
                }
                DesignStyle.MATERIAL3 -> {
                    assertEquals(0.92f, normalMotion.pressScale, 0.001f)
                }
                DesignStyle.NEUMORPHISM -> {
                    assertEquals(0.95f, normalMotion.pressScale, 0.001f)
                }
                DesignStyle.ORGANIC -> {
                    assertEquals(0.96f, normalMotion.pressScale, 0.001f)
                }
                else -> Unit
            }
        }
    }

    @Test
    fun allThemesFormatAppearanceSummaryLabelCorrectly() {
        DesignStyle.appThemes.forEach { style ->
            listOf("LIGHT" to "Light", "DARK" to "Dark", "AMOLED" to "OLED", "SYSTEM" to "System").forEach { (theme, expectedLabel) ->
                val themeModeLabel = when (theme.uppercase()) {
                    "AMOLED" -> "OLED"
                    "LIGHT" -> "Light"
                    "DARK" -> "Dark"
                    "SYSTEM" -> "System"
                    else -> theme.lowercase().replaceFirstChar { it.uppercase() }
                }
                val summary = "${style.displayName} · $themeModeLabel"
                assertTrue(summary.startsWith(style.displayName))
                assertTrue(summary.endsWith(expectedLabel))
                assertFalse(summary.contains("null"))
            }
        }
    }

    // =========================================================================
    // WCAG Contrast Utilities
    // =========================================================================

    private fun luminance(color: Color): Double {
        fun linear(c: Float) = if (c <= 0.04045f) c.toDouble() / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        return 0.2126 * linear(color.red) + 0.7152 * linear(color.green) + 0.0722 * linear(color.blue)
    }

    private fun contrast(a: Color, b: Color): Double {
        val l1 = luminance(a)
        val l2 = luminance(b)
        return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
    }
}

