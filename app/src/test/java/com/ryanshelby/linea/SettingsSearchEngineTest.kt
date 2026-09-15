package com.ryanshelby.linea

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SimCard
import com.ryanshelby.linea.ui.screens.settings.search.SettingsCategory
import com.ryanshelby.linea.ui.screens.settings.search.SettingsSearchAction
import com.ryanshelby.linea.ui.screens.settings.search.SettingsSearchEngine
import com.ryanshelby.linea.ui.screens.settings.search.SettingsSearchItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsSearchEngineTest {

    private lateinit var testCatalog: List<SettingsSearchItem>

    @Before
    fun setUp() {
        testCatalog = listOf(
            SettingsSearchItem(
                id = "flip_to_silence",
                title = "Flip to Silence",
                subtitle = "Flip phone face down to mute incoming ringer",
                category = SettingsCategory.GESTURES,
                icon = Icons.Filled.ScreenRotation,
                keywords = listOf("flip", "mute", "silence", "face down", "gesture", "turn over", "upside down"),
                action = SettingsSearchAction.Toggle(
                    isChecked = { true },
                    onToggle = {}
                )
            ),
            SettingsSearchItem(
                id = "theme_selection",
                title = "Color Theme",
                subtitle = "Select system, light, dark, or custom OLED theme",
                category = SettingsCategory.APPEARANCE,
                icon = Icons.Filled.Palette,
                keywords = listOf("theme", "dark mode", "light mode", "oled", "night", "black", "color", "appearance", "dark"),
                action = SettingsSearchAction.Navigate({})
            ),
            SettingsSearchItem(
                id = "call_blocking",
                title = "Call Screening & Blocking",
                subtitle = "Block private, unknown, and spam callers",
                category = SettingsCategory.SECURITY,
                icon = Icons.Filled.Block,
                keywords = listOf("block", "spam", "blacklist", "screen", "reject", "private", "unknown", "whitelist"),
                action = SettingsSearchAction.Navigate({})
            ),
            SettingsSearchItem(
                id = "dual_sim",
                title = "Dual SIM Management",
                subtitle = "Configure multi-SIM accounts, slot affinities, and prompt behaviors",
                category = SettingsCategory.GENERAL,
                icon = Icons.Filled.SimCard,
                keywords = listOf("sim", "dual sim", "carrier", "network", "slot", "esim", "multi sim"),
                action = SettingsSearchAction.Navigate({})
            ),
            SettingsSearchItem(
                id = "about_linea",
                title = "About Linea",
                subtitle = "Version 1.0.0, open-source licenses, and zero telemetry privacy policy",
                category = SettingsCategory.ABOUT,
                icon = Icons.Filled.Info,
                keywords = listOf("about", "version", "developer", "foss", "license", "privacy", "zero telemetry"),
                action = SettingsSearchAction.Navigate({})
            )
        )
    }

    @Test
    fun search_emptyOrBlankQuery_returnsEmptyList() {
        val emptyResult = SettingsSearchEngine.search("", testCatalog)
        assertTrue(emptyResult.isEmpty())

        val whitespaceResult = SettingsSearchEngine.search("    ", testCatalog)
        assertTrue(whitespaceResult.isEmpty())
    }

    @Test
    fun search_exactTitleMatch_returnsHighestScore() {
        val results = SettingsSearchEngine.search("Flip to Silence", testCatalog)
        assertFalse(results.isEmpty())
        assertEquals("flip_to_silence", results.first().id)
        // Exact title match gets 150 points + keyword matches
        assertTrue(results.first().score >= 150)
    }

    @Test
    fun search_caseInsensitiveMatch_findsTarget() {
        val results = SettingsSearchEngine.search("FLIP TO SILENCE", testCatalog)
        assertFalse(results.isEmpty())
        assertEquals("flip_to_silence", results.first().id)

        val mixedResults = SettingsSearchEngine.search("cOlOr ThEmE", testCatalog)
        assertFalse(mixedResults.isEmpty())
        assertEquals("theme_selection", mixedResults.first().id)
    }

    @Test
    fun search_prefixTitleMatch_findsTarget() {
        val results = SettingsSearchEngine.search("Dual", testCatalog)
        assertFalse(results.isEmpty())
        assertEquals("dual_sim", results.first().id)
    }

    @Test
    fun search_synonymKeywordMatch_resolvesCorrectItem() {
        // Query "mute" should find "Flip to Silence" via synonym keyword
        val muteResults = SettingsSearchEngine.search("mute", testCatalog)
        assertFalse(muteResults.isEmpty())
        assertEquals("flip_to_silence", muteResults.first().id)

        // Query "blacklist" should find "Call Screening & Blocking"
        val blacklistResults = SettingsSearchEngine.search("blacklist", testCatalog)
        assertFalse(blacklistResults.isEmpty())
        assertEquals("call_blocking", blacklistResults.first().id)

        // Query "dark" should find "Color Theme"
        val darkResults = SettingsSearchEngine.search("dark", testCatalog)
        assertFalse(darkResults.isEmpty())
        assertEquals("theme_selection", darkResults.first().id)
    }

    @Test
    fun search_multiTokenQuery_matchesAndBoostsScore() {
        // "face down" tokens both match keywords/subtitle of Flip to Silence
        val results = SettingsSearchEngine.search("face down", testCatalog)
        assertFalse(results.isEmpty())
        assertEquals("flip_to_silence", results.first().id)
    }

    @Test
    fun search_categoryMatch_findsItemsInCategory() {
        // Querying "Security" should find Call Screening & Blocking
        val results = SettingsSearchEngine.search("Security", testCatalog)
        assertFalse(results.isEmpty())
        assertTrue(results.any { it.id == "call_blocking" })
    }

    @Test
    fun search_subtitleMatch_findsItems() {
        // "telemetry" is only in the subtitle/keywords of About Linea
        val results = SettingsSearchEngine.search("telemetry", testCatalog)
        assertFalse(results.isEmpty())
        assertEquals("about_linea", results.first().id)
    }

    @Test
    fun search_rankingOrder_prioritizesBestMatchFirst() {
        // When searching for "sim", Dual SIM has both exact keyword "sim" and title prefix "Dual SIM"
        val results = SettingsSearchEngine.search("sim", testCatalog)
        assertFalse(results.isEmpty())
        assertEquals("dual_sim", results.first().id)

        // Ensure descending score order
        for (i in 0 until results.size - 1) {
            assertTrue(results[i].score >= results[i + 1].score)
        }
    }

    @Test
    fun search_unmatchedQuery_returnsEmptyList() {
        val results = SettingsSearchEngine.search("nonexistentxyz123", testCatalog)
        assertTrue(results.isEmpty())
    }
}
