package com.ryanshelby.linea

import com.ryanshelby.linea.data.local.entities.BlockAction
import com.ryanshelby.linea.data.local.entities.BlockMatchType
import com.ryanshelby.linea.data.local.entities.BlockedNumberEntity
import com.ryanshelby.linea.data.local.entities.CallRuleEntity
import com.ryanshelby.linea.data.local.entities.RuleAction
import com.ryanshelby.linea.data.local.entities.RuleAllowedFilter
import com.ryanshelby.linea.telecom.screening.ScreeningRuleMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class CallScreeningEngineTest {

    @Test
    fun normalize_stripsNonDigits() {
        assertEquals("15551234567", ScreeningRuleMatcher.normalize("+1 (555) 123-4567"))
        assertEquals("01711223344", ScreeningRuleMatcher.normalize("017-11-22-33-44"))
        assertEquals("", ScreeningRuleMatcher.normalize("Private Number"))
    }

    @Test
    fun matchesBlockedRule_exactMatch() {
        val rule = BlockedNumberEntity(
            id = 1,
            numberOrPrefix = "5551234",
            matchType = BlockMatchType.EXACT,
            blockAction = BlockAction.SILENT_REJECT
        )

        val raw = "555-1234"
        val normalized = ScreeningRuleMatcher.normalize(raw)
        assertTrue(ScreeningRuleMatcher.matchesBlockedRule(rule, raw, normalized, isContact = false))

        val otherRaw = "5559999"
        val otherNormalized = ScreeningRuleMatcher.normalize(otherRaw)
        assertFalse(ScreeningRuleMatcher.matchesBlockedRule(rule, otherRaw, otherNormalized, isContact = false))
    }

    @Test
    fun matchesBlockedRule_prefixMatch() {
        val rule = BlockedNumberEntity(
            id = 2,
            numberOrPrefix = "+1800*",
            matchType = BlockMatchType.PREFIX,
            blockAction = BlockAction.SILENT_REJECT
        )

        val matchRaw = "+1-800-555-0199"
        val matchNormalized = ScreeningRuleMatcher.normalize(matchRaw)
        assertTrue(ScreeningRuleMatcher.matchesBlockedRule(rule, matchRaw, matchNormalized, isContact = false))

        val nonMatchRaw = "+1-888-555-0199"
        val nonMatchNormalized = ScreeningRuleMatcher.normalize(nonMatchRaw)
        assertFalse(ScreeningRuleMatcher.matchesBlockedRule(rule, nonMatchRaw, nonMatchNormalized, isContact = false))
    }

    @Test
    fun matchesBlockedRule_rangeMatch() {
        val rule = BlockedNumberEntity(
            id = 3,
            numberOrPrefix = "5550100-5550200",
            matchType = BlockMatchType.RANGE,
            blockAction = BlockAction.SILENT_REJECT
        )

        val inRangeRaw = "5550150"
        val inRangeNorm = ScreeningRuleMatcher.normalize(inRangeRaw)
        assertTrue(ScreeningRuleMatcher.matchesBlockedRule(rule, inRangeRaw, inRangeNorm, isContact = false))

        val boundaryMinRaw = "5550100"
        assertTrue(ScreeningRuleMatcher.matchesBlockedRule(rule, boundaryMinRaw, boundaryMinRaw, isContact = false))

        val boundaryMaxRaw = "5550200"
        assertTrue(ScreeningRuleMatcher.matchesBlockedRule(rule, boundaryMaxRaw, boundaryMaxRaw, isContact = false))

        val outOfRangeRaw = "5550250"
        assertFalse(ScreeningRuleMatcher.matchesBlockedRule(rule, outOfRangeRaw, outOfRangeRaw, isContact = false))
    }

    @Test
    fun matchesBlockedRule_privateAndUnknown() {
        val privateRule = BlockedNumberEntity(
            id = 4,
            numberOrPrefix = "Private",
            matchType = BlockMatchType.PRIVATE,
            blockAction = BlockAction.SILENT_REJECT
        )
        assertTrue(ScreeningRuleMatcher.matchesBlockedRule(privateRule, "private", "", isContact = false))
        assertTrue(ScreeningRuleMatcher.matchesBlockedRule(privateRule, "", "", isContact = false))
        assertFalse(ScreeningRuleMatcher.matchesBlockedRule(privateRule, "5551234", "5551234", isContact = false))

        val unknownRule = BlockedNumberEntity(
            id = 5,
            numberOrPrefix = "Unknown",
            matchType = BlockMatchType.UNKNOWN,
            blockAction = BlockAction.SILENT_REJECT
        )
        assertTrue(ScreeningRuleMatcher.matchesBlockedRule(unknownRule, "", "", isContact = false))
        assertFalse(ScreeningRuleMatcher.matchesBlockedRule(unknownRule, "5551234", "5551234", isContact = false))
    }

    @Test
    fun matchesBlockedRule_nonContact() {
        val rule = BlockedNumberEntity(
            id = 6,
            numberOrPrefix = "Non-Contacts",
            matchType = BlockMatchType.NON_CONTACT,
            blockAction = BlockAction.SILENT_REJECT
        )

        // If not in contacts -> block matches
        assertTrue(ScreeningRuleMatcher.matchesBlockedRule(rule, "5559876", "5559876", isContact = false))
        // If in contacts -> block does NOT match
        assertFalse(ScreeningRuleMatcher.matchesBlockedRule(rule, "5559876", "5559876", isContact = true))
    }

    @Test
    fun matchesBlockedRule_international() {
        val rule = BlockedNumberEntity(
            id = 7,
            numberOrPrefix = "International",
            matchType = BlockMatchType.INTERNATIONAL,
            blockAction = BlockAction.SILENT_REJECT
        )

        // If local country is US (+1)
        val usLocalNumber = "+15551234567"
        assertFalse(ScreeningRuleMatcher.matchesBlockedRule(rule, usLocalNumber, "15551234567", isContact = false, simCountryIso = "US"))

        val intlCallToUs = "+44207123456"
        assertTrue(ScreeningRuleMatcher.matchesBlockedRule(rule, intlCallToUs, "44207123456", isContact = false, simCountryIso = "US"))

        // Local call without + prefix is not treated as international
        assertFalse(ScreeningRuleMatcher.matchesBlockedRule(rule, "5551234", "5551234", isContact = false, simCountryIso = "US"))
    }

    @Test
    fun matchesScheduleRule_sameDayWindow() {
        val rule = CallRuleEntity(
            id = 10,
            name = "Work Quiet Hours",
            isEnabled = true,
            daysOfWeek = "1,2,3,4,5", // Mon-Fri
            startTime = "09:00",
            endTime = "17:00",
            action = RuleAction.REJECT,
            allowedFilter = RuleAllowedFilter.ALL
        )

        // Set up Monday at 14:30 (2:30 PM)
        val mondayCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
        }
        assertTrue(ScreeningRuleMatcher.matchesScheduleRule(rule, mondayCal.timeInMillis, simSlot = 0))

        // Set up Monday at 18:30 (outside 09:00-17:00)
        val mondayEveningCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 18)
            set(Calendar.MINUTE, 30)
        }
        assertFalse(ScreeningRuleMatcher.matchesScheduleRule(rule, mondayEveningCal.timeInMillis, simSlot = 0))

        // Set up Sunday at 14:30 (outside active days)
        val sundayCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 30)
        }
        assertFalse(ScreeningRuleMatcher.matchesScheduleRule(rule, sundayCal.timeInMillis, simSlot = 0))
    }

    @Test
    fun matchesScheduleRule_overnightWindow() {
        val rule = CallRuleEntity(
            id = 11,
            name = "Overnight Sleep",
            isEnabled = true,
            daysOfWeek = "1,2,3,4,5,6,7", // All week
            startTime = "22:00",
            endTime = "07:00",
            action = RuleAction.REJECT,
            allowedFilter = RuleAllowedFilter.ALL
        )

        // 23:30 (before midnight, after 22:00)
        val lateNightCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 30)
        }
        assertTrue(ScreeningRuleMatcher.matchesScheduleRule(rule, lateNightCal.timeInMillis, simSlot = 0))

        // 04:15 (after midnight, before 07:00)
        val earlyMorningCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 4)
            set(Calendar.MINUTE, 15)
        }
        assertTrue(ScreeningRuleMatcher.matchesScheduleRule(rule, earlyMorningCal.timeInMillis, simSlot = 0))

        // 12:00 (noon, outside quiet hours)
        val noonCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
        }
        assertFalse(ScreeningRuleMatcher.matchesScheduleRule(rule, noonCal.timeInMillis, simSlot = 0))
    }

    @Test
    fun matchesScheduleRule_simSlotFiltering() {
        val rule = CallRuleEntity(
            id = 12,
            name = "SIM 1 Quiet Hours",
            isEnabled = true,
            daysOfWeek = "1,2,3,4,5,6,7",
            startTime = "00:00",
            endTime = "23:59",
            action = RuleAction.REJECT,
            allowedFilter = RuleAllowedFilter.ALL,
            simSlot = 0 // Applies only to SIM 1
        )

        val now = System.currentTimeMillis()
        assertTrue(ScreeningRuleMatcher.matchesScheduleRule(rule, now, simSlot = 0))
        assertFalse(ScreeningRuleMatcher.matchesScheduleRule(rule, now, simSlot = 1))
    }
}
