package com.ryanshelby.linea

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import com.ryanshelby.linea.telecom.escape.EscapeCallPreset
import com.ryanshelby.linea.telecom.screening.OfflineCallerIdEngine
import com.ryanshelby.linea.ui.screens.settings.search.SettingsCategory
import com.ryanshelby.linea.ui.screens.settings.search.SettingsSearchAction
import com.ryanshelby.linea.ui.screens.settings.search.SettingsSearchEngine
import com.ryanshelby.linea.ui.screens.settings.search.SettingsSearchItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EscapeCallAndOfflineCallerIdTest {

    @Test
    fun testOfflineCallerId_worldwideContinents() {
        // Asia
        val japan = OfflineCallerIdEngine.identifyNumber("+819012345678")
        assertEquals("Japan", japan.countryName)
        assertEquals("🇯🇵", japan.flagEmoji)

        val india = OfflineCallerIdEngine.identifyNumber("+919876543210")
        assertEquals("India", india.countryName)
        assertEquals("🇮🇳", india.flagEmoji)

        val sk = OfflineCallerIdEngine.identifyNumber("+821012345678")
        assertEquals("South Korea", sk.countryName)
        assertEquals("🇰🇷", sk.flagEmoji)

        val sg = OfflineCallerIdEngine.identifyNumber("+6561234567")
        assertEquals("Singapore", sg.countryName)
        assertEquals("🇸🇬", sg.flagEmoji)

        // Europe
        val france = OfflineCallerIdEngine.identifyNumber("+33140000000")
        assertEquals("France", france.countryName)
        assertEquals("🇫🇷", france.flagEmoji)
        assertTrue(france.regionOrCountry.contains("Paris"))

        val germany = OfflineCallerIdEngine.identifyNumber("+4930123456")
        assertEquals("Germany", germany.countryName)
        assertEquals("🇩🇪", germany.flagEmoji)
        assertTrue(germany.regionOrCountry.contains("Berlin"))

        val italy = OfflineCallerIdEngine.identifyNumber("+3906123456")
        assertEquals("Italy", italy.countryName)
        assertEquals("🇮🇹", italy.flagEmoji)

        val vatican = OfflineCallerIdEngine.identifyNumber("+379123456")
        assertEquals("Vatican City", vatican.countryName)
        assertEquals("🇻🇦", vatican.flagEmoji)

        // Americas
        val brazil = OfflineCallerIdEngine.identifyNumber("+5511987654321")
        assertEquals("Brazil", brazil.countryName)
        assertEquals("🇧🇷", brazil.flagEmoji)

        val argentina = OfflineCallerIdEngine.identifyNumber("+541112345678")
        assertEquals("Argentina", argentina.countryName)
        assertEquals("🇦🇷", argentina.flagEmoji)

        val mexico = OfflineCallerIdEngine.identifyNumber("+525512345678")
        assertEquals("Mexico", mexico.countryName)
        assertEquals("🇲🇽", mexico.flagEmoji)

        // Africa
        val egypt = OfflineCallerIdEngine.identifyNumber("+201012345678")
        assertEquals("Egypt", egypt.countryName)
        assertEquals("🇪🇬", egypt.flagEmoji)

        val sa = OfflineCallerIdEngine.identifyNumber("+27111234567")
        assertEquals("South Africa", sa.countryName)
        assertEquals("🇿🇦", sa.flagEmoji)

        val morocco = OfflineCallerIdEngine.identifyNumber("+212522123456")
        assertEquals("Morocco", morocco.countryName)
        assertEquals("🇲🇦", morocco.flagEmoji)

        // Oceania
        val aus = OfflineCallerIdEngine.identifyNumber("+61298765432")
        assertEquals("Australia", aus.countryName)
        assertEquals("🇦🇺", aus.flagEmoji)
        assertTrue(aus.regionOrCountry.contains("Sydney"))

        val nz = OfflineCallerIdEngine.identifyNumber("+6491234567")
        assertEquals("New Zealand", nz.countryName)
        assertEquals("🇳🇿", nz.flagEmoji)
    }

    @Test
    fun testOfflineCallerId_carrierAndOperatorFallbacks() {
        // Bangladesh operators
        val teletalk = OfflineCallerIdEngine.identifyNumber("+8801512345678")
        assertTrue(teletalk.regionOrCountry.contains("Teletalk"))

        // Pakistan operators
        val telenorPk = OfflineCallerIdEngine.identifyNumber("+923451234567")
        assertTrue(telenorPk.regionOrCountry.contains("Telenor"))
        assertEquals("Pakistan", telenorPk.countryName)

        val zong = OfflineCallerIdEngine.identifyNumber("+923121234567")
        assertTrue(zong.regionOrCountry.contains("Zong"))

        // UAE operators
        val du = OfflineCallerIdEngine.identifyNumber("+971551234567")
        assertTrue(du.regionOrCountry.contains("du"))
        assertEquals("United Arab Emirates", du.countryName)

        // Saudi operators
        val mobily = OfflineCallerIdEngine.identifyNumber("+966561234567")
        assertTrue(mobily.regionOrCountry.contains("Mobily"))
        assertEquals("Saudi Arabia", mobily.countryName)

        // Kenya operators
        val safaricom = OfflineCallerIdEngine.identifyNumber("+254701123456")
        assertTrue(safaricom.regionOrCountry.contains("Safaricom"))
        assertEquals("Kenya", safaricom.countryName)

        // Philippines operators
        val globe = OfflineCallerIdEngine.identifyNumber("+639171234567")
        assertTrue(globe.regionOrCountry.contains("Globe"))
        assertEquals("Philippines", globe.countryName)
    }

    @Test
    fun testOfflineCallerId_nanpRegionalLocations() {
        // Chicago, IL
        val chi = OfflineCallerIdEngine.identifyNumber("+13125550199")
        assertEquals("Regional Location", chi.category)
        assertTrue(chi.regionOrCountry.contains("Chicago"))
        assertEquals("United States", chi.countryName)

        // Los Angeles, CA
        val la = OfflineCallerIdEngine.identifyNumber("+12135550199")
        assertTrue(la.regionOrCountry.contains("Los Angeles, CA"))

        // Vancouver, BC
        val van = OfflineCallerIdEngine.identifyNumber("+16045550199")
        assertTrue(van.regionOrCountry.contains("Vancouver, BC"))
        assertEquals("Canada", van.countryName)

        // Montreal, QC
        val mtl = OfflineCallerIdEngine.identifyNumber("+15145550199")
        assertTrue(mtl.regionOrCountry.contains("Montreal, QC"))
        assertEquals("Canada", mtl.countryName)
    }

    @Test
    fun testEscapeCallPresets_dataStructure() {
        val presets = listOf(
            EscapeCallPreset("boss", "The Boss", "+1 (202) 555-0199", "Executive Urgent Request", "💼"),
            EscapeCallPreset("doctor", "Doctor's Office", "+1 (212) 555-0143", "Medical Center Escalation", "🩺"),
            EscapeCallPreset("mom", "Mom", "+1 (415) 555-0182", "Family Priority", "❤️"),
            EscapeCallPreset("security", "Home Security", "+1 (800) 555-0191", "Zone 4 Sensor Alert", "🚨"),
            EscapeCallPreset("custom", "Custom Caller", "+1 (555) 012-3456", "Custom configured", "🎭")
        )

        assertEquals(5, presets.size)
        assertEquals("The Boss", presets[0].name)
        assertEquals("💼", presets[0].iconEmoji)
        assertEquals("Doctor's Office", presets[1].name)
        assertEquals("❤️", presets[2].iconEmoji)
        assertEquals("Home Security", presets[3].name)
        assertEquals("custom", presets[4].id)
    }

    @Test
    fun testSettingsSearchEngine_findsEscapeCall() {
        var navigatedEscape = false
        var navigatedSettings = false
        var navigatedHelp = false

        val catalog = listOf(
            SettingsSearchItem(
                id = "escape_call_simulator",
                title = "Tactical Escape Call",
                subtitle = "Simulate an urgent incoming call with custom timing to exit any situation",
                category = SettingsCategory.CALLING,
                icon = Icons.Filled.Phone,
                keywords = listOf(
                    "escape", "escape call", "fake call", "fake", "simulator", "call simulator",
                    "fake phone call", "emergency excuse", "pretend call", "bogus call", "tactical"
                ),
                action = SettingsSearchAction.Navigate { navigatedEscape = true }
            ),
            SettingsSearchItem(
                id = "escape_call_settings",
                title = "Escape Call Trigger & Settings",
                subtitle = "Custom secret dial code, countdown delay, and stealth vibration mode",
                category = SettingsCategory.CALLING,
                icon = Icons.Filled.Phone,
                keywords = listOf(
                    "custom prefix", "secret code", "dial code", "fake call code", "escape code",
                    "prefix", "dial trigger", "escape settings", "fake call settings", "stealth escape", "vibrate only"
                ),
                action = SettingsSearchAction.Navigate { navigatedSettings = true }
            ),
            SettingsSearchItem(
                id = "escape_call_help",
                title = "Escape Call Field Manual & Help",
                subtitle = "Tactical usage guide, hands-under-table procedures, and decoy scenarios",
                category = SettingsCategory.CALLING,
                icon = Icons.Filled.Phone,
                keywords = listOf(
                    "escape call help", "fake call help", "how to use fake call", "how to use escape call",
                    "field manual", "escape guide", "fake call guide", "tactical help", "excuse guide"
                ),
                action = SettingsSearchAction.Navigate { navigatedHelp = true }
            )
        )

        // 1. Search "escape"
        val searchEscape = SettingsSearchEngine.search("escape", catalog)
        assertTrue(searchEscape.any { it.id == "escape_call_simulator" })

        // 2. Search "fake call"
        val searchFake = SettingsSearchEngine.search("fake call", catalog)
        assertTrue(searchFake.any { it.id == "escape_call_simulator" })

        // 3. Search "custom prefix" / "secret code"
        val searchPrefix = SettingsSearchEngine.search("custom prefix", catalog)
        assertTrue(searchPrefix.any { it.id == "escape_call_settings" })

        val searchCode = SettingsSearchEngine.search("secret code", catalog)
        assertTrue(searchCode.any { it.id == "escape_call_settings" })

        // 4. Search "field manual" / "help"
        val searchHelp = SettingsSearchEngine.search("field manual", catalog)
        assertTrue(searchHelp.any { it.id == "escape_call_help" })

        // 5. Test navigation triggers
        val itemSettings = searchPrefix.first { it.id == "escape_call_settings" }
        (itemSettings.action as SettingsSearchAction.Navigate).onNavigate()
        assertTrue(navigatedSettings)

        val itemHelp = searchHelp.first { it.id == "escape_call_help" }
        (itemHelp.action as SettingsSearchAction.Navigate).onNavigate()
        assertTrue(navigatedHelp)
    }
}
