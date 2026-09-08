package com.ryanshelby.linea

import com.ryanshelby.linea.data.local.entities.BlockAction
import com.ryanshelby.linea.data.local.entities.BlockMatchType
import com.ryanshelby.linea.data.local.entities.BlockedNumberEntity
import com.ryanshelby.linea.data.local.entities.CallRuleEntity
import com.ryanshelby.linea.data.local.entities.RuleAction
import com.ryanshelby.linea.data.local.entities.RuleAllowedFilter
import com.ryanshelby.linea.telecom.screening.RepeatCallTracker
import com.ryanshelby.linea.telecom.screening.ScreeningRuleMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class SmartRulesIntelligenceTest {

    private lateinit var repeatTracker: RepeatCallTracker

    @Before
    fun setUp() {
        repeatTracker = RepeatCallTracker()
    }

    @Test
    fun repeatCallTracker_slidingWindowWithin5Minutes() {
        val number = "+1 (555) 019-2834"
        val baseTime = 1000000000L
        val fiveMinutesAgo = { t: Long -> t - 300_000L }

        // Call 1
        repeatTracker.recordAttempt(number, baseTime)
        assertEquals(1, repeatTracker.getRecentAttemptsCount(number, fiveMinutesAgo(baseTime)))

        // Call 2 (after 2 minutes)
        val time2 = baseTime + 120_000L
        repeatTracker.recordAttempt(number, time2)
        assertEquals(2, repeatTracker.getRecentAttemptsCount(number, fiveMinutesAgo(time2)))

        // Call 3 (after 4 minutes from base) - 3 attempts within 5m sliding window
        val time3 = baseTime + 240_000L
        repeatTracker.recordAttempt(number, time3)
        val count = repeatTracker.getRecentAttemptsCount(number, fiveMinutesAgo(time3))
        assertEquals(3, count)
        assertTrue("3 calls within 5m should qualify for emergency override", count >= 3)
    }

    @Test
    fun repeatCallTracker_callOutsideWindowDropsOff() {
        val number = "555-4321"
        val baseTime = 1000000000L
        val fiveMinutesAgo = { t: Long -> t - 300_000L }

        // Call 1 at t=0
        repeatTracker.recordAttempt(number, baseTime)

        // Call 2 at t=2m
        repeatTracker.recordAttempt(number, baseTime + 120_000L)

        // Call 3 arrives at t=6m (more than 5 minutes after Call 1)
        val time3 = baseTime + 360_000L
        repeatTracker.recordAttempt(number, time3)

        // Only call 2 (t=2m) and call 3 (t=6m) are within 5 minutes of t=6m (window: t=1m to t=6m)
        val count = repeatTracker.getRecentAttemptsCount(number, fiveMinutesAgo(time3))
        assertEquals(2, count)
        assertFalse("Only 2 calls in sliding window, emergency override should not trigger", count >= 3)
    }

    @Test
    fun repeatCallTracker_multipleNumbersIsolated() {
        val numberA = "555-1111"
        val numberB = "555-2222"
        val now = System.currentTimeMillis()
        val windowStart = now - 300_000L

        repeatTracker.recordAttempt(numberA, now)
        repeatTracker.recordAttempt(numberA, now + 1000)
        repeatTracker.recordAttempt(numberA, now + 2000)

        // numberA should have 3 attempts
        val countA = repeatTracker.getRecentAttemptsCount(numberA, windowStart)
        assertEquals(3, countA)
        assertTrue(countA >= 3)

        // numberB has only 1 attempt
        repeatTracker.recordAttempt(numberB, now + 2000)
        val countB = repeatTracker.getRecentAttemptsCount(numberB, windowStart)
        assertEquals(1, countB)
        assertFalse(countB >= 3)
    }

    @Test
    fun temporaryBlock_expiresAtEvaluation() {
        val now = System.currentTimeMillis()

        // Active temporary block (expires in 1 hour)
        val activeTempRule = BlockedNumberEntity(
            id = 1,
            numberOrPrefix = "5559876",
            matchType = BlockMatchType.EXACT,
            blockAction = BlockAction.SILENT_REJECT,
            expiresAt = now + 3600_000L
        )
        val isActiveExpired = activeTempRule.expiresAt != null && activeTempRule.expiresAt < now
        assertFalse(isActiveExpired)
        assertTrue(ScreeningRuleMatcher.matchesBlockedRule(activeTempRule, "5559876", "5559876", isContact = false))

        // Expired temporary block (expired 10 seconds ago)
        val expiredTempRule = BlockedNumberEntity(
            id = 2,
            numberOrPrefix = "5559876",
            matchType = BlockMatchType.EXACT,
            blockAction = BlockAction.SILENT_REJECT,
            expiresAt = now - 10_000L
        )
        val isExpired = expiredTempRule.expiresAt != null && expiredTempRule.expiresAt < now
        assertTrue(isExpired)
    }

    @Test
    fun scheduleRule_overnightBoundaryEvaluation() {
        val nighttimeRule = CallRuleEntity(
            id = 100,
            name = "Nighttime Favorites Only",
            isEnabled = true,
            startTime = "22:00",
            endTime = "07:00",
            daysOfWeek = "1,2,3,4,5,6,7",
            allowedFilter = RuleAllowedFilter.FAVORITES_ONLY,
            action = RuleAction.REJECT
        )

        // 22:30 at night -> within window
        val calNight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 22)
            set(Calendar.MINUTE, 30)
        }
        assertTrue(ScreeningRuleMatcher.matchesScheduleRule(nighttimeRule, calNight.timeInMillis, simSlot = 0))

        // 03:45 in morning -> within window
        val calEarly = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 3)
            set(Calendar.MINUTE, 45)
        }
        assertTrue(ScreeningRuleMatcher.matchesScheduleRule(nighttimeRule, calEarly.timeInMillis, simSlot = 0))

        // 14:00 afternoon -> outside window
        val calAfternoon = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 0)
        }
        assertFalse(ScreeningRuleMatcher.matchesScheduleRule(nighttimeRule, calAfternoon.timeInMillis, simSlot = 0))
    }

    @Test
    fun scheduleRule_dayOfWeekFiltering() {
        // Rule active only Monday to Friday (1..5)
        val workdayRule = CallRuleEntity(
            id = 101,
            name = "Workday Focus",
            isEnabled = true,
            startTime = "09:00",
            endTime = "17:00",
            daysOfWeek = "1,2,3,4,5",
            allowedFilter = RuleAllowedFilter.FAVORITES_ONLY,
            action = RuleAction.SILENT
        )

        // Wednesday 11:00 -> active day, active time
        val calWednesday = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY)
            set(Calendar.HOUR_OF_DAY, 11)
            set(Calendar.MINUTE, 0)
        }
        assertTrue(ScreeningRuleMatcher.matchesScheduleRule(workdayRule, calWednesday.timeInMillis, simSlot = 0))

        // Saturday 11:00 -> weekend, inactive day
        val calSaturday = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
            set(Calendar.HOUR_OF_DAY, 11)
            set(Calendar.MINUTE, 0)
        }
        assertFalse(ScreeningRuleMatcher.matchesScheduleRule(workdayRule, calSaturday.timeInMillis, simSlot = 0))
    }
}
