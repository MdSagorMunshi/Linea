package com.ryanshelby.linea

import com.ryanshelby.linea.data.local.entities.BlockAction
import com.ryanshelby.linea.data.local.entities.CountryRuleEntity
import com.ryanshelby.linea.data.local.entities.CountryRuleType
import com.ryanshelby.linea.telecom.screening.CountryDetails
import com.ryanshelby.linea.telecom.screening.CountryDetectionResult
import com.ryanshelby.linea.telecom.screening.CountryDetector
import com.ryanshelby.linea.telecom.screening.ScreeningDecision
import com.ryanshelby.linea.telecom.screening.ScreeningRuleMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CountryCallBlockingTest {

    // ==========================================
    // 1. Reliable Country Detection Tests
    // ==========================================

    @Test
    fun detectCountry_unitedStates_resolvesUsWithSharedCode() {
        // New York area code (212)
        val result = CountryDetector.detectCountry("+12125551234")
        assertTrue("Expected Identified result for US number", result is CountryDetectionResult.Identified)
        val details = (result as CountryDetectionResult.Identified).details
        assertEquals("US", details.countryIso)
        assertEquals("United States", details.countryName)
        assertEquals("+1", details.dialCode)
        assertEquals("🇺🇸", details.flagEmoji)
        assertTrue("Expected shared calling code flag for +1", details.isSharedCallingCode)
    }

    @Test
    fun detectCountry_canada_resolvesCaWithSharedCode() {
        // Toronto area code (416)
        val result = CountryDetector.detectCountry("+14165550199")
        assertTrue("Expected Identified result for Canadian number", result is CountryDetectionResult.Identified)
        val details = (result as CountryDetectionResult.Identified).details
        assertEquals("CA", details.countryIso)
        assertEquals("Canada", details.countryName)
        assertEquals("+1", details.dialCode)
        assertEquals("🇨🇦", details.flagEmoji)
        assertTrue("Expected shared calling code flag for +1", details.isSharedCallingCode)
    }

    @Test
    fun detectCountry_jamaica_resolvesJmInNanp() {
        // Jamaica calling code / area code (+1876)
        val result = CountryDetector.detectCountry("+18765551234")
        assertTrue("Expected Identified result for Jamaica", result is CountryDetectionResult.Identified)
        val details = (result as CountryDetectionResult.Identified).details
        assertEquals("JM", details.countryIso)
        assertEquals("Jamaica", details.countryName)
        assertEquals("🇯🇲", details.flagEmoji)
    }

    @Test
    fun detectCountry_unitedKingdom_resolvesGb() {
        val result = CountryDetector.detectCountry("+442079460919")
        assertTrue("Expected Identified result for UK", result is CountryDetectionResult.Identified)
        val details = (result as CountryDetectionResult.Identified).details
        assertEquals("GB", details.countryIso)
        assertEquals("United Kingdom", details.countryName)
        assertEquals("+44", details.dialCode)
        assertEquals("🇬🇧", details.flagEmoji)
        assertTrue("Expected shared calling code flag for +44", details.isSharedCallingCode)
    }

    @Test
    fun detectCountry_germany_resolvesDe() {
        val result = CountryDetector.detectCountry("+4930123456")
        assertTrue("Expected Identified result for Germany", result is CountryDetectionResult.Identified)
        val details = (result as CountryDetectionResult.Identified).details
        assertEquals("DE", details.countryIso)
        assertEquals("Germany", details.countryName)
        assertEquals("+49", details.dialCode)
        assertEquals("🇩🇪", details.flagEmoji)
    }

    @Test
    fun detectCountry_bangladesh_resolvesBd() {
        val result = CountryDetector.detectCountry("+8801712345678")
        assertTrue("Expected Identified result for Bangladesh", result is CountryDetectionResult.Identified)
        val details = (result as CountryDetectionResult.Identified).details
        assertEquals("BD", details.countryIso)
        assertEquals("Bangladesh", details.countryName)
        assertEquals("+880", details.dialCode)
        assertEquals("🇧🇩", details.flagEmoji)
    }

    @Test
    fun detectCountry_russiaAndKazakhstan_sharedPlusSevenSeparatedAccurately() {
        // Russia Moscow (+7 495)
        val ruResult = CountryDetector.detectCountry("+74951234567")
        assertTrue("Expected Identified result for Russia", ruResult is CountryDetectionResult.Identified)
        val ruDetails = (ruResult as CountryDetectionResult.Identified).details
        assertEquals("RU", ruDetails.countryIso)
        assertEquals("Russia", ruDetails.countryName)

        // Kazakhstan (+7 701)
        val kzResult = CountryDetector.detectCountry("+77011234567")
        assertTrue("Expected Identified result for Kazakhstan", kzResult is CountryDetectionResult.Identified)
        val kzDetails = (kzResult as CountryDetectionResult.Identified).details
        assertEquals("KZ", kzDetails.countryIso)
        assertEquals("Kazakhstan", kzDetails.countryName)
    }

    @Test
    fun detectCountry_withZeroZeroPrefix_handlesInternationalTrunk() {
        // 0044 -> +44 (UK)
        val result = CountryDetector.detectCountry("00442079460919")
        assertTrue("Expected Identified result for 00 prefix", result is CountryDetectionResult.Identified)
        val details = (result as CountryDetectionResult.Identified).details
        assertEquals("GB", details.countryIso)
    }

    // ==========================================
    // 2. Edge Cases (Private, Unknown, Withheld)
    // ==========================================

    @Test
    fun detectCountry_privateAndUnknownNumbers_returnsUnknownNeverMisclassifies() {
        assertTrue(CountryDetector.isPrivateOrUnknown("private"))
        assertTrue(CountryDetector.isPrivateOrUnknown("Private Number"))
        assertTrue(CountryDetector.isPrivateOrUnknown("unknown"))
        assertTrue(CountryDetector.isPrivateOrUnknown("Unknown Caller"))
        assertTrue(CountryDetector.isPrivateOrUnknown("withheld"))
        assertTrue(CountryDetector.isPrivateOrUnknown("caller id withheld"))
        assertTrue(CountryDetector.isPrivateOrUnknown("restricted"))
        assertTrue(CountryDetector.isPrivateOrUnknown("anonymous"))
        assertTrue(CountryDetector.isPrivateOrUnknown(null))
        assertTrue(CountryDetector.isPrivateOrUnknown(""))
        assertTrue(CountryDetector.isPrivateOrUnknown("   "))

        val privateResult = CountryDetector.detectCountry("Private Number")
        assertTrue("Expected Unknown for private caller ID", privateResult is CountryDetectionResult.Unknown)

        val unknownResult = CountryDetector.detectCountry("unknown")
        assertTrue("Expected Unknown for unknown caller ID", unknownResult is CountryDetectionResult.Unknown)

        val nullResult = CountryDetector.detectCountry(null)
        assertTrue("Expected Unknown for null caller ID", nullResult is CountryDetectionResult.Unknown)

        val singleDigitResult = CountryDetector.detectCountry("1")
        assertTrue("Expected Unknown for single digit caller ID", singleDigitResult is CountryDetectionResult.Unknown)
    }

    // ==========================================
    // 3. Country Blacklist Matching & Precision
    // ==========================================

    @Test
    fun evaluateCountryRules_blacklistUsOnly_blocksUsAndAllowsCanada() {
        val usRule = CountryRuleEntity(
            id = 1,
            countryIso = "US",
            countryName = "United States",
            dialCode = "+1",
            flagEmoji = "🇺🇸",
            ruleType = CountryRuleType.BLACKLIST,
            appliesToAllSharedCode = false // Precision area-code blocking
        )
        val activeRules = listOf(usRule)

        // Incoming call from US
        val usCall = (CountryDetector.detectCountry("+12125551234") as CountryDetectionResult.Identified).details
        val usDecision = ScreeningRuleMatcher.evaluateCountryRules(
            country = usCall,
            activeRules = activeRules,
            filterMode = "BLACKLIST_ONLY"
        )
        assertTrue("US call must be blocked", usDecision is ScreeningDecision.Block)
        val block = usDecision as ScreeningDecision.Block
        assertEquals(BlockAction.SILENT_REJECT, block.action)
        assertTrue("Reason should mention United States", block.reason.contains("United States"))
        assertEquals("COUNTRY_BLACKLIST", block.matchedRuleType)

        // Incoming call from Canada (same +1 calling code, but CA area code)
        val caCall = (CountryDetector.detectCountry("+14165550199") as CountryDetectionResult.Identified).details
        val caDecision = ScreeningRuleMatcher.evaluateCountryRules(
            country = caCall,
            activeRules = activeRules,
            filterMode = "BLACKLIST_ONLY"
        )
        assertEquals("Canada must NOT be blocked when only US is blacklisted", ScreeningDecision.Allow, caDecision)
    }

    @Test
    fun evaluateCountryRules_blacklistEntirePlusOne_blocksBothUsAndCanada() {
        val sharedNanpRule = CountryRuleEntity(
            id = 2,
            countryIso = "US",
            countryName = "United States",
            dialCode = "+1",
            flagEmoji = "🇺🇸",
            ruleType = CountryRuleType.BLACKLIST,
            appliesToAllSharedCode = true // Block entire +1 region
        )
        val activeRules = listOf(sharedNanpRule)

        val usCall = (CountryDetector.detectCountry("+12125551234") as CountryDetectionResult.Identified).details
        val usDecision = ScreeningRuleMatcher.evaluateCountryRules(
            country = usCall,
            activeRules = activeRules,
            filterMode = "BLACKLIST_ONLY"
        )
        assertTrue("US call must be blocked", usDecision is ScreeningDecision.Block)

        val caCall = (CountryDetector.detectCountry("+14165550199") as CountryDetectionResult.Identified).details
        val caDecision = ScreeningRuleMatcher.evaluateCountryRules(
            country = caCall,
            activeRules = activeRules,
            filterMode = "BLACKLIST_ONLY"
        )
        assertTrue("Canada must be blocked when appliesToAllSharedCode is true", caDecision is ScreeningDecision.Block)

        val ukCall = (CountryDetector.detectCountry("+442079460919") as CountryDetectionResult.Identified).details
        val ukDecision = ScreeningRuleMatcher.evaluateCountryRules(
            country = ukCall,
            activeRules = activeRules,
            filterMode = "BLACKLIST_ONLY"
        )
        assertEquals("UK (+44) should not be blocked by +1 rule", ScreeningDecision.Allow, ukDecision)
    }

    @Test
    fun evaluateCountryRules_blacklistGermany_blocksGermanyOnly() {
        val deRule = CountryRuleEntity(
            id = 3,
            countryIso = "DE",
            countryName = "Germany",
            dialCode = "+49",
            flagEmoji = "🇩🇪",
            ruleType = CountryRuleType.BLACKLIST
        )
        val activeRules = listOf(deRule)

        val deCall = (CountryDetector.detectCountry("+4930123456") as CountryDetectionResult.Identified).details
        val deDecision = ScreeningRuleMatcher.evaluateCountryRules(
            country = deCall,
            activeRules = activeRules
        )
        assertTrue("Germany call must be blocked", deDecision is ScreeningDecision.Block)

        val frCall = (CountryDetector.detectCountry("+33123456789") as CountryDetectionResult.Identified).details
        val frDecision = ScreeningRuleMatcher.evaluateCountryRules(
            country = frCall,
            activeRules = activeRules
        )
        assertEquals("France must be allowed", ScreeningDecision.Allow, frDecision)
    }

    // ==========================================
    // 4. Country Whitelist & Whitelist Overrides
    // ==========================================

    @Test
    fun evaluateCountryRules_whitelistOverridesBlacklist() {
        // Broad rule: entire +1 calling code is blacklisted
        val blacklistNanp = CountryRuleEntity(
            id = 4,
            countryIso = "US",
            countryName = "United States",
            dialCode = "+1",
            flagEmoji = "🇺🇸",
            ruleType = CountryRuleType.BLACKLIST,
            appliesToAllSharedCode = true
        )
        // Exception: Canada is explicitly whitelisted
        val whitelistCanada = CountryRuleEntity(
            id = 5,
            countryIso = "CA",
            countryName = "Canada",
            dialCode = "+1",
            flagEmoji = "🇨🇦",
            ruleType = CountryRuleType.WHITELIST
        )
        val activeRules = listOf(blacklistNanp, whitelistCanada)

        val caCall = (CountryDetector.detectCountry("+14165550199") as CountryDetectionResult.Identified).details
        val caDecision = ScreeningRuleMatcher.evaluateCountryRules(
            country = caCall,
            activeRules = activeRules,
            filterMode = "BLACKLIST_ONLY",
            whitelistOverrides = true
        )
        assertEquals("Canada must be allowed because whitelist overrides blacklist", ScreeningDecision.Allow, caDecision)

        val usCall = (CountryDetector.detectCountry("+12125551234") as CountryDetectionResult.Identified).details
        val usDecision = ScreeningRuleMatcher.evaluateCountryRules(
            country = usCall,
            activeRules = activeRules,
            filterMode = "BLACKLIST_ONLY",
            whitelistOverrides = true
        )
        assertTrue("US must remain blocked", usDecision is ScreeningDecision.Block)
    }

    @Test
    fun evaluateCountryRules_whitelistOnlyStrictFilterMode() {
        // Strict Whitelist Mode: Only UK and Japan are whitelisted
        val whitelistUk = CountryRuleEntity(
            id = 6,
            countryIso = "GB",
            countryName = "United Kingdom",
            dialCode = "+44",
            flagEmoji = "🇬🇧",
            ruleType = CountryRuleType.WHITELIST
        )
        val whitelistJp = CountryRuleEntity(
            id = 7,
            countryIso = "JP",
            countryName = "Japan",
            dialCode = "+81",
            flagEmoji = "🇯🇵",
            ruleType = CountryRuleType.WHITELIST
        )
        val activeRules = listOf(whitelistUk, whitelistJp)

        // Home SIM country is Bangladesh (BD)
        val homeIso = "BD"

        // 1. UK call -> Allowed (whitelisted)
        val ukCall = (CountryDetector.detectCountry("+442079460919") as CountryDetectionResult.Identified).details
        assertEquals(
            ScreeningDecision.Allow,
            ScreeningRuleMatcher.evaluateCountryRules(ukCall, activeRules, "WHITELIST_ONLY", userHomeIso = homeIso)
        )

        // 2. Japan call -> Allowed (whitelisted)
        val jpCall = (CountryDetector.detectCountry("+81312345678") as CountryDetectionResult.Identified).details
        assertEquals(
            ScreeningDecision.Allow,
            ScreeningRuleMatcher.evaluateCountryRules(jpCall, activeRules, "WHITELIST_ONLY", userHomeIso = homeIso)
        )

        // 3. Bangladesh domestic call -> Allowed (home country)
        val bdCall = (CountryDetector.detectCountry("+8801712345678") as CountryDetectionResult.Identified).details
        assertEquals(
            ScreeningDecision.Allow,
            ScreeningRuleMatcher.evaluateCountryRules(bdCall, activeRules, "WHITELIST_ONLY", userHomeIso = homeIso)
        )

        // 4. US call -> BLOCKED (not whitelisted)
        val usCall = (CountryDetector.detectCountry("+12125551234") as CountryDetectionResult.Identified).details
        val usDecision = ScreeningRuleMatcher.evaluateCountryRules(usCall, activeRules, "WHITELIST_ONLY", userHomeIso = homeIso)
        assertTrue("Non-whitelisted country must be blocked in strict mode", usDecision is ScreeningDecision.Block)
        assertEquals("COUNTRY_WHITELIST_STRICT", (usDecision as ScreeningDecision.Block).matchedRuleType)
    }

    // ==========================================
    // 5. Country Catalog & Shared Code Metadata
    // ==========================================

    @Test
    fun catalog_sharedCodes_communicatedClearly() {
        val sharedTerritoriesPlusOne = CountryDetector.getSharedTerritoriesForDialCode("+1")
        assertTrue("Expected USA in +1 shared territories", sharedTerritoriesPlusOne.any { it.countryIso == "US" })
        assertTrue("Expected Canada in +1 shared territories", sharedTerritoriesPlusOne.any { it.countryIso == "CA" })
        assertTrue("Expected Jamaica in +1 shared territories", sharedTerritoriesPlusOne.any { it.countryIso == "JM" })

        val sharedTerritoriesPlusSeven = CountryDetector.getSharedTerritoriesForDialCode("+7")
        assertTrue("Expected Russia in +7 shared territories", sharedTerritoriesPlusSeven.any { it.countryIso == "RU" })
        assertTrue("Expected Kazakhstan in +7 shared territories", sharedTerritoriesPlusSeven.any { it.countryIso == "KZ" })

        val allCountries = CountryDetector.getAllCatalogCountries()
        assertTrue("Expected at least 200 catalog countries", allCountries.size >= 200)

        // Verify distinct ISOs
        val distinctCount = allCountries.map { it.countryIso }.distinct().size
        assertEquals(allCountries.size, distinctCount)
    }
}
