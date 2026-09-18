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

    @Test
    fun testNoPlusCountryDetection_worldwideDirectDialing() {
        // Numbers dialed WITHOUT '+' sign
        val gpNoPlus = OfflineCallerIdEngine.identifyNumber("8801712345678")
        assertEquals("Mobile Network", gpNoPlus.category)
        assertTrue(gpNoPlus.regionOrCountry.contains("Grameenphone"))
        assertEquals("🇧🇩", gpNoPlus.flagEmoji)

        val ukMobileNoPlus = OfflineCallerIdEngine.identifyNumber("447911123456")
        assertEquals("Mobile Network", ukMobileNoPlus.category)
        assertEquals("United Kingdom", ukMobileNoPlus.countryName)
        assertEquals("🇬🇧", ukMobileNoPlus.flagEmoji)

        val ukLondonNoPlus = OfflineCallerIdEngine.identifyNumber("442079460991")
        assertEquals("Regional Location", ukLondonNoPlus.category)
        assertTrue(ukLondonNoPlus.regionOrCountry.contains("London"))

        val deBerlinNoPlus = OfflineCallerIdEngine.identifyNumber("4930123456")
        assertEquals("Regional Location", deBerlinNoPlus.category)
        assertTrue(deBerlinNoPlus.regionOrCountry.contains("Berlin"))
        assertEquals("🇩🇪", deBerlinNoPlus.flagEmoji)

        val inMobileNoPlus = OfflineCallerIdEngine.identifyNumber("919876543210")
        assertEquals("Mobile Network", inMobileNoPlus.category)
        assertEquals("India", inMobileNoPlus.countryName)
        assertEquals("🇮🇳", inMobileNoPlus.flagEmoji)

        val frParisNoPlus = OfflineCallerIdEngine.identifyNumber("33140000000")
        assertEquals("Regional Location", frParisNoPlus.category)
        assertTrue(frParisNoPlus.regionOrCountry.contains("Paris"))

        val jpTokyoNoPlus = OfflineCallerIdEngine.identifyNumber("81312345678")
        assertEquals("Regional Location", jpTokyoNoPlus.category)
        assertTrue(jpTokyoNoPlus.regionOrCountry.contains("Tokyo"))

        val auSydneyNoPlus = OfflineCallerIdEngine.identifyNumber("61298765432")
        assertEquals("Regional Location", auSydneyNoPlus.category)
        assertTrue(auSydneyNoPlus.regionOrCountry.contains("Sydney"))

        val saudiNoPlus = OfflineCallerIdEngine.identifyNumber("966501234567")
        assertEquals("Mobile Network", saudiNoPlus.category)
        assertTrue(saudiNoPlus.regionOrCountry.contains("STC"))

        val uaeNoPlus = OfflineCallerIdEngine.identifyNumber("971501234567")
        assertEquals("Mobile Network", uaeNoPlus.category)
        assertTrue(uaeNoPlus.regionOrCountry.contains("Etisalat"))

        val nigeriaNoPlus = OfflineCallerIdEngine.identifyNumber("2348031234567")
        assertEquals("Mobile Network", nigeriaNoPlus.category)
        assertTrue(nigeriaNoPlus.regionOrCountry.contains("MTN"))

        val kenyaNoPlus = OfflineCallerIdEngine.identifyNumber("254701123456")
        assertEquals("Mobile Network", kenyaNoPlus.category)
        assertTrue(kenyaNoPlus.regionOrCountry.contains("Safaricom"))

        val phGlobeNoPlus = OfflineCallerIdEngine.identifyNumber("639171234567")
        assertEquals("Mobile Network", phGlobeNoPlus.category)
        assertTrue(phGlobeNoPlus.regionOrCountry.contains("Globe Telecom"))

        val usNanpNoPlus = OfflineCallerIdEngine.identifyNumber("14155551234")
        assertEquals("Regional Location", usNanpNoPlus.category)
        assertTrue(usNanpNoPlus.regionOrCountry.contains("San Francisco, CA"))
    }

    @Test
    fun testDomesticTrunkWithSimCountry() {
        // Dialing domestic trunk '0' format when user's SIM country is set
        val bdDomestic = OfflineCallerIdEngine.identifyNumber("01712345678", userCountryIso = "BD")
        assertEquals("Mobile Network", bdDomestic.category)
        assertTrue(bdDomestic.regionOrCountry.contains("Grameenphone"))
        assertEquals("🇧🇩", bdDomestic.flagEmoji)

        val ukDomestic = OfflineCallerIdEngine.identifyNumber("02079460991", userCountryIso = "GB")
        assertEquals("Regional Location", ukDomestic.category)
        assertTrue(ukDomestic.regionOrCountry.contains("London"))
        assertEquals("🇬🇧", ukDomestic.flagEmoji)

        val deDomestic = OfflineCallerIdEngine.identifyNumber("030123456", userCountryIso = "DE")
        assertEquals("Regional Location", deDomestic.category)
        assertTrue(deDomestic.regionOrCountry.contains("Berlin"))
        assertEquals("🇩🇪", deDomestic.flagEmoji)

        val frDomestic = OfflineCallerIdEngine.identifyNumber("0140000000", userCountryIso = "FR")
        assertEquals("Regional Location", frDomestic.category)
        assertTrue(frDomestic.regionOrCountry.contains("Paris"))
        assertEquals("🇫🇷", frDomestic.flagEmoji)

        val jpDomestic = OfflineCallerIdEngine.identifyNumber("0312345678", userCountryIso = "JP")
        assertEquals("Regional Location", jpDomestic.category)
        assertTrue(jpDomestic.regionOrCountry.contains("Tokyo"))
        assertEquals("🇯🇵", jpDomestic.flagEmoji)

        val auDomestic = OfflineCallerIdEngine.identifyNumber("0298765432", userCountryIso = "AU")
        assertEquals("Regional Location", auDomestic.category)
        assertTrue(auDomestic.regionOrCountry.contains("Sydney"))
        assertEquals("🇦🇺", auDomestic.flagEmoji)
    }

    @Test
    fun testLocalWithoutPrefixWithSimCountry() {
        // Dialing 10-digit number locally
        val usLocal = OfflineCallerIdEngine.identifyNumber("4155551234", userCountryIso = "US")
        assertEquals("Regional Location", usLocal.category)
        assertTrue(usLocal.regionOrCountry.contains("San Francisco, CA"))

        val inLocal = OfflineCallerIdEngine.identifyNumber("9876543210", userCountryIso = "IN")
        assertEquals("Mobile Network", inLocal.category)
        assertEquals("India", inLocal.countryName)

        val bdLocal = OfflineCallerIdEngine.identifyNumber("1712345678", userCountryIso = "BD")
        assertEquals("Mobile Network", bdLocal.category)
        assertTrue(bdLocal.regionOrCountry.contains("Grameenphone"))
    }

    @Test
    fun testSimCountryDetector_overrideAndCountryInfo() {
        com.ryanshelby.linea.telecom.screening.SimCountryDetector.setOverride("BD")
        assertEquals("BD", com.ryanshelby.linea.telecom.screening.SimCountryDetector.currentCountryIso)

        val bdCountryInfo = OfflineCallerIdEngine.getCountryInfoByIso("BD")
        assertEquals("Bangladesh", bdCountryInfo?.name)
        assertEquals("+880", bdCountryInfo?.dialCode)
        assertEquals("🇧🇩", bdCountryInfo?.flag)

        com.ryanshelby.linea.telecom.screening.SimCountryDetector.setOverride("GB")
        assertEquals("GB", com.ryanshelby.linea.telecom.screening.SimCountryDetector.currentCountryIso)
        val gbCountryInfo = OfflineCallerIdEngine.getCountryInfoByIso("GB")
        assertEquals("United Kingdom", gbCountryInfo?.name)
        assertEquals("+44", gbCountryInfo?.dialCode)

        // Reset
        com.ryanshelby.linea.telecom.screening.SimCountryDetector.setOverride(null)
    }

    @Test
    fun testSettingsSearch_simRegionalIntelligence() {
        val catalog = listOf(
            SettingsSearchItem(
                id = "sim_regional_intelligence",
                title = "SIM Location & Regional Intelligence",
                subtitle = "100% offline country detection without '+' sign, SIM carrier & local area resolution",
                category = SettingsCategory.CALLING,
                icon = Icons.Filled.Phone,
                keywords = listOf(
                    "sim", "sim location", "sim country", "telephony", "telephony location", "country",
                    "caller id", "caller id location", "no plus", "plus sign", "without plus",
                    "offline caller id", "area code", "carrier detection", "regional intelligence", "local country"
                ),
                action = SettingsSearchAction.Navigate {}
            )
        )

        val searchSim = SettingsSearchEngine.search("sim country", catalog)
        assertTrue(searchSim.any { it.id == "sim_regional_intelligence" })

        val searchTelephony = SettingsSearchEngine.search("telephony location", catalog)
        assertTrue(searchTelephony.any { it.id == "sim_regional_intelligence" })

        val searchNoPlus = SettingsSearchEngine.search("no plus", catalog)
        assertTrue(searchNoPlus.any { it.id == "sim_regional_intelligence" })
    }

    @Test
    fun testDirectCountryCodeWithoutPlusOrZero() {
        // Typing pure dial code without '+' and without '0'
        // 1. Bangladesh: '880' alone
        val bdDirect = OfflineCallerIdEngine.identifyNumber("880")
        assertEquals("Bangladesh", bdDirect.countryName)
        assertEquals("🇧🇩", bdDirect.flagEmoji)
        assertEquals("BANGLADESH", bdDirect.badgeLabel)

        // Bangladesh: dial code + partial/full number without '+'
        val bdDirectPartial = OfflineCallerIdEngine.identifyNumber("88017")
        assertEquals("Bangladesh", bdDirectPartial.countryName)
        assertEquals("🇧🇩", bdDirectPartial.flagEmoji)
        assertEquals("GRAMEENPHONE", bdDirectPartial.badgeLabel)
        assertTrue(bdDirectPartial.regionOrCountry.contains("Grameenphone"))

        val bdDirectFull = OfflineCallerIdEngine.identifyNumber("8801712345678")
        assertEquals("Bangladesh", bdDirectFull.countryName)
        assertEquals("🇧🇩", bdDirectFull.flagEmoji)
        assertEquals("GRAMEENPHONE", bdDirectFull.badgeLabel)
        assertTrue(bdDirectFull.regionOrCountry.contains("Grameenphone"))

        // 2. United Kingdom: '44' alone and with number
        val ukDirect = OfflineCallerIdEngine.identifyNumber("44")
        assertEquals("United Kingdom", ukDirect.countryName)
        assertEquals("🇬🇧", ukDirect.flagEmoji)

        val ukDirectLondon = OfflineCallerIdEngine.identifyNumber("442079460192")
        assertEquals("United Kingdom", ukDirectLondon.countryName)
        assertEquals("🇬🇧", ukDirectLondon.flagEmoji)
        assertEquals("LONDON", ukDirectLondon.badgeLabel)
        assertTrue(ukDirectLondon.regionOrCountry.contains("London"))

        // 3. India: '91' alone and with number
        val inDirect = OfflineCallerIdEngine.identifyNumber("91")
        assertEquals("India", inDirect.countryName)
        assertEquals("🇮🇳", inDirect.flagEmoji)

        val inDirectMobile = OfflineCallerIdEngine.identifyNumber("919876543210")
        assertEquals("India", inDirectMobile.countryName)
        assertEquals("🇮🇳", inDirectMobile.flagEmoji)
        assertTrue(inDirectMobile.regionOrCountry.contains("India"))

        // 4. Germany: '49' alone
        val deDirect = OfflineCallerIdEngine.identifyNumber("49")
        assertEquals("Germany", deDirect.countryName)
        assertEquals("🇩🇪", deDirect.flagEmoji)

        // 5. France: '33' alone
        val frDirect = OfflineCallerIdEngine.identifyNumber("33")
        assertEquals("France", frDirect.countryName)
        assertEquals("🇫🇷", frDirect.flagEmoji)

        // 6. Japan: '81' alone
        val jpDirect = OfflineCallerIdEngine.identifyNumber("81")
        assertEquals("Japan", jpDirect.countryName)
        assertEquals("🇯🇵", jpDirect.flagEmoji)

        // 7. Australia: '61' alone
        val auDirect = OfflineCallerIdEngine.identifyNumber("61")
        assertEquals("Australia", auDirect.countryName)
        assertEquals("🇦🇺", auDirect.flagEmoji)
    }

    @Test
    fun testInternationalCountryHelperWithoutPlus() {
        // Typing '880' without '+' must immediately resolve Bangladesh timezone
        val bdPreview = com.ryanshelby.linea.telecom.InternationalCountryHelper.detectCountryAndLocalTime("880")
        assertNotNull(bdPreview)
        assertEquals("Bangladesh", bdPreview?.countryName)
        assertEquals("🇧🇩", bdPreview?.flagEmoji)

        // Typing '8801712345678' without '+'
        val bdFullPreview = com.ryanshelby.linea.telecom.InternationalCountryHelper.detectCountryAndLocalTime("8801712345678")
        assertNotNull(bdFullPreview)
        assertEquals("Bangladesh", bdFullPreview?.countryName)
        assertEquals("🇧🇩", bdFullPreview?.flagEmoji)

        // Typing '44' without '+' must resolve UK
        val ukPreview = com.ryanshelby.linea.telecom.InternationalCountryHelper.detectCountryAndLocalTime("44")
        assertNotNull(ukPreview)
        assertEquals("United Kingdom", ukPreview?.countryName)
        assertEquals("🇬🇧", ukPreview?.flagEmoji)

        // Typing '91' without '+' must resolve India
        val inPreview = com.ryanshelby.linea.telecom.InternationalCountryHelper.detectCountryAndLocalTime("91")
        assertNotNull(inPreview)
        assertEquals("India", inPreview?.countryName)
        assertEquals("🇮🇳", inPreview?.flagEmoji)
    }

    @Test
    fun testTimeOfDayStates() {
        // Verify all 6 requested states from hours
        assertEquals(com.ryanshelby.linea.telecom.TimeOfDayState.EARLY_MORNING, com.ryanshelby.linea.telecom.TimeOfDayState.fromHour(5))
        assertEquals(com.ryanshelby.linea.telecom.TimeOfDayState.EARLY_MORNING, com.ryanshelby.linea.telecom.TimeOfDayState.fromHour(6))
        assertEquals(com.ryanshelby.linea.telecom.TimeOfDayState.EARLY_MORNING, com.ryanshelby.linea.telecom.TimeOfDayState.fromHour(7))
        assertEquals("Early Morning", com.ryanshelby.linea.telecom.TimeOfDayState.EARLY_MORNING.displayName)

        assertEquals(com.ryanshelby.linea.telecom.TimeOfDayState.MORNING, com.ryanshelby.linea.telecom.TimeOfDayState.fromHour(8))
        assertEquals(com.ryanshelby.linea.telecom.TimeOfDayState.MORNING, com.ryanshelby.linea.telecom.TimeOfDayState.fromHour(10))
        assertEquals(com.ryanshelby.linea.telecom.TimeOfDayState.MORNING, com.ryanshelby.linea.telecom.TimeOfDayState.fromHour(11))
        assertEquals("Morning", com.ryanshelby.linea.telecom.TimeOfDayState.MORNING.displayName)

        assertEquals(com.ryanshelby.linea.telecom.TimeOfDayState.MIDDAY, com.ryanshelby.linea.telecom.TimeOfDayState.fromHour(12))
        assertEquals(com.ryanshelby.linea.telecom.TimeOfDayState.MIDDAY, com.ryanshelby.linea.telecom.TimeOfDayState.fromHour(13))
        assertEquals("Midday", com.ryanshelby.linea.telecom.TimeOfDayState.MIDDAY.displayName)

        assertEquals(com.ryanshelby.linea.telecom.TimeOfDayState.AFTERNOON, com.ryanshelby.linea.telecom.TimeOfDayState.fromHour(14))
        assertEquals(com.ryanshelby.linea.telecom.TimeOfDayState.AFTERNOON, com.ryanshelby.linea.telecom.TimeOfDayState.fromHour(16))
        assertEquals("Afternoon", com.ryanshelby.linea.telecom.TimeOfDayState.AFTERNOON.displayName)

        assertEquals(com.ryanshelby.linea.telecom.TimeOfDayState.EVENING, com.ryanshelby.linea.telecom.TimeOfDayState.fromHour(17))
        assertEquals(com.ryanshelby.linea.telecom.TimeOfDayState.EVENING, com.ryanshelby.linea.telecom.TimeOfDayState.fromHour(19))
        assertEquals(com.ryanshelby.linea.telecom.TimeOfDayState.EVENING, com.ryanshelby.linea.telecom.TimeOfDayState.fromHour(20))
        assertEquals("Evening", com.ryanshelby.linea.telecom.TimeOfDayState.EVENING.displayName)

        assertEquals(com.ryanshelby.linea.telecom.TimeOfDayState.NIGHT, com.ryanshelby.linea.telecom.TimeOfDayState.fromHour(21))
        assertEquals(com.ryanshelby.linea.telecom.TimeOfDayState.NIGHT, com.ryanshelby.linea.telecom.TimeOfDayState.fromHour(23))
        assertEquals(com.ryanshelby.linea.telecom.TimeOfDayState.NIGHT, com.ryanshelby.linea.telecom.TimeOfDayState.fromHour(0))
        assertEquals(com.ryanshelby.linea.telecom.TimeOfDayState.NIGHT, com.ryanshelby.linea.telecom.TimeOfDayState.fromHour(3))
        assertEquals("Night", com.ryanshelby.linea.telecom.TimeOfDayState.NIGHT.displayName)
    }

    @Test
    fun testWorldwideCountryCoverage() {
        val helperCountries = com.ryanshelby.linea.telecom.InternationalCountryHelper.getAllCountries()
        val engineCountries = com.ryanshelby.linea.telecom.screening.OfflineCallerIdEngine.getAllCountries()

        // Verify total countries catalog exceeds 240+ entities (all 195 UN sovereign nations + global territories)
        assertTrue("Helper must contain at least 240 countries/territories, got: ${helperCountries.size}", helperCountries.size >= 244)
        assertTrue("Engine must contain at least 240 countries/territories, got: ${engineCountries.size}", engineCountries.size >= 244)

        // 1. South America: Argentina (54 / +54)
        val arPreview = com.ryanshelby.linea.telecom.InternationalCountryHelper.detectCountryAndLocalTime("541143219876")
        assertNotNull(arPreview)
        assertEquals("Argentina", arPreview?.countryName)
        assertEquals("🇦🇷", arPreview?.flagEmoji)
        val arCallerId = com.ryanshelby.linea.telecom.screening.OfflineCallerIdEngine.identifyNumber("541143219876")
        assertEquals("Argentina", arCallerId.countryName)
        assertEquals("🇦🇷", arCallerId.flagEmoji)

        // 2. Europe: Portugal (351 / +351)
        val ptPreview = com.ryanshelby.linea.telecom.InternationalCountryHelper.detectCountryAndLocalTime("351912345678")
        assertNotNull(ptPreview)
        assertEquals("Portugal", ptPreview?.countryName)
        assertEquals("🇵🇹", ptPreview?.flagEmoji)
        val ptCallerId = com.ryanshelby.linea.telecom.screening.OfflineCallerIdEngine.identifyNumber("351912345678")
        assertEquals("Portugal", ptCallerId.countryName)
        assertEquals("🇵🇹", ptCallerId.flagEmoji)

        // 3. Europe: Poland (48 / +48)
        val plPreview = com.ryanshelby.linea.telecom.InternationalCountryHelper.detectCountryAndLocalTime("48601234567")
        assertNotNull(plPreview)
        assertEquals("Poland", plPreview?.countryName)
        assertEquals("🇵🇱", plPreview?.flagEmoji)

        // 4. Central Asia: Kazakhstan (7701... / +7701...)
        val kzPreview = com.ryanshelby.linea.telecom.InternationalCountryHelper.detectCountryAndLocalTime("77011234567")
        assertNotNull(kzPreview)
        assertEquals("Kazakhstan", kzPreview?.countryName)
        assertEquals("🇰🇿", kzPreview?.flagEmoji)
        val kzCallerId = com.ryanshelby.linea.telecom.screening.OfflineCallerIdEngine.identifyNumber("77011234567")
        assertEquals("Kazakhstan", kzCallerId.countryName)
        assertEquals("🇰🇿", kzCallerId.flagEmoji)
        assertEquals("KCELL / ACTIV", kzCallerId.badgeLabel)

        // 5. North America: Canada Toronto (1416 / +1416)
        val caCallerId = com.ryanshelby.linea.telecom.screening.OfflineCallerIdEngine.identifyNumber("14165550199")
        assertEquals("Canada", caCallerId.countryName)
        assertEquals("🇨🇦", caCallerId.flagEmoji)
        assertEquals("Toronto, ON • Canada", caCallerId.regionOrCountry)
        val caPreview = com.ryanshelby.linea.telecom.InternationalCountryHelper.detectCountryAndLocalTime("14165550199")
        assertNotNull(caPreview)
        assertEquals("Canada", caPreview?.countryName)
        assertEquals("🇨🇦", caPreview?.flagEmoji)

        // 6. Africa: Kenya (254 / +254)
        val kePreview = com.ryanshelby.linea.telecom.InternationalCountryHelper.detectCountryAndLocalTime("254712345678")
        assertNotNull(kePreview)
        assertEquals("Kenya", kePreview?.countryName)
        assertEquals("🇰🇪", kePreview?.flagEmoji)
        val keCallerId = com.ryanshelby.linea.telecom.screening.OfflineCallerIdEngine.identifyNumber("254712345678")
        assertEquals("Kenya", keCallerId.countryName)
        assertEquals("🇰🇪", keCallerId.flagEmoji)

        // 7. Oceania: Fiji (679 / +679)
        val fjPreview = com.ryanshelby.linea.telecom.InternationalCountryHelper.detectCountryAndLocalTime("6799991234")
        assertNotNull(fjPreview)
        assertEquals("Fiji", fjPreview?.countryName)
        assertEquals("🇫🇯", fjPreview?.flagEmoji)

        // 8. Verify ISO lookups for CA, KZ, PS, and mapped IL/TW
        val caInfo = com.ryanshelby.linea.telecom.screening.OfflineCallerIdEngine.getCountryInfoByIso("CA")
        assertNotNull(caInfo)
        assertEquals("Canada", caInfo?.name)
        assertEquals("🇨🇦", caInfo?.flag)

        val kzInfo = com.ryanshelby.linea.telecom.screening.OfflineCallerIdEngine.getCountryInfoByIso("KZ")
        assertNotNull(kzInfo)
        assertEquals("Kazakhstan", kzInfo?.name)
        assertEquals("🇰🇿", kzInfo?.flag)

        // 9. Palestine (+970 and +972)
        val ps970Preview = com.ryanshelby.linea.telecom.InternationalCountryHelper.detectCountryAndLocalTime("970599123456")
        assertNotNull(ps970Preview)
        assertEquals("Palestine", ps970Preview?.countryName)
        assertEquals("🇵🇸", ps970Preview?.flagEmoji)

        val ps970CallerId = com.ryanshelby.linea.telecom.screening.OfflineCallerIdEngine.identifyNumber("970599123456")
        assertEquals("Palestine", ps970CallerId.countryName)
        assertEquals("🇵🇸", ps970CallerId.flagEmoji)
        assertEquals("PALESTINE", ps970CallerId.badgeLabel)

        val ps972Preview = com.ryanshelby.linea.telecom.InternationalCountryHelper.detectCountryAndLocalTime("972501234567")
        assertNotNull(ps972Preview)
        assertEquals("Palestine", ps972Preview?.countryName)
        assertEquals("🇵🇸", ps972Preview?.flagEmoji)

        val ps972CallerId = com.ryanshelby.linea.telecom.screening.OfflineCallerIdEngine.identifyNumber("972501234567")
        assertEquals("Palestine", ps972CallerId.countryName)
        assertEquals("🇵🇸", ps972CallerId.flagEmoji)
        assertEquals("PALESTINE", ps972CallerId.badgeLabel)

        val psInfo = com.ryanshelby.linea.telecom.screening.OfflineCallerIdEngine.getCountryInfoByIso("PS")
        assertNotNull(psInfo)
        assertEquals("Palestine", psInfo?.name)
        assertEquals("🇵🇸", psInfo?.flag)

        val ilMappedInfo = com.ryanshelby.linea.telecom.screening.OfflineCallerIdEngine.getCountryInfoByIso("IL")
        assertNotNull(ilMappedInfo)
        assertEquals("Palestine", ilMappedInfo?.name)
        assertEquals("🇵🇸", ilMappedInfo?.flag)

        val ilHelperInfo = com.ryanshelby.linea.telecom.InternationalCountryHelper.getCountryByIso("IL")
        assertNotNull(ilHelperInfo)
        assertEquals("Palestine", ilHelperInfo?.countryName)
        assertEquals("🇵🇸", ilHelperInfo?.flagEmoji)

        // 10. China / Taiwan (+886 represented as part of China)
        val cn886Preview = com.ryanshelby.linea.telecom.InternationalCountryHelper.detectCountryAndLocalTime("886912345678")
        assertNotNull(cn886Preview)
        assertEquals("China", cn886Preview?.countryName)
        assertEquals("🇨🇳", cn886Preview?.flagEmoji)

        val cn886CallerId = com.ryanshelby.linea.telecom.screening.OfflineCallerIdEngine.identifyNumber("886912345678")
        assertEquals("China", cn886CallerId.countryName)
        assertEquals("🇨🇳", cn886CallerId.flagEmoji)
        assertEquals("Taiwan, China", cn886CallerId.regionOrCountry)
        assertEquals("TAIWAN, CHINA", cn886CallerId.badgeLabel)

        val twMappedInfo = com.ryanshelby.linea.telecom.screening.OfflineCallerIdEngine.getCountryInfoByIso("TW")
        assertNotNull(twMappedInfo)
        assertEquals("China", twMappedInfo?.name)
        assertEquals("🇨🇳", twMappedInfo?.flag)

        val twHelperInfo = com.ryanshelby.linea.telecom.InternationalCountryHelper.getCountryByIso("TW")
        assertNotNull(twHelperInfo)
        assertEquals("China", twHelperInfo?.countryName)
        assertEquals("🇨🇳", twHelperInfo?.flagEmoji)

        // 11. Strict validation: neither Israel nor Taiwan exist as separate country entries anywhere
        assertTrue(helperCountries.none { it.countryName.contains("Israel", ignoreCase = true) })
        assertTrue(helperCountries.none { it.flagEmoji == "🇮🇱" })
        assertTrue(helperCountries.none { it.countryName.equals("Taiwan", ignoreCase = true) })
        assertTrue(helperCountries.none { it.flagEmoji == "🇹🇼" })

        assertTrue(engineCountries.none { it.name.contains("Israel", ignoreCase = true) })
        assertTrue(engineCountries.none { it.flag == "🇮🇱" })
        assertTrue(engineCountries.none { it.name.equals("Taiwan", ignoreCase = true) })
        assertTrue(engineCountries.none { it.flag == "🇹🇼" })
    }
}

