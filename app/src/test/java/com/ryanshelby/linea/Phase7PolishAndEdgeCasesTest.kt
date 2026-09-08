package com.ryanshelby.linea

import com.ryanshelby.linea.data.local.entities.CallDirectionType
import com.ryanshelby.linea.data.local.entities.CallRecordEntity
import com.ryanshelby.linea.telecom.logic.AvailabilityInsightEngine
import com.ryanshelby.linea.telecom.screening.OfflineCallerIdEngine
import com.ryanshelby.linea.telecom.screening.RepeatCallTracker
import com.ryanshelby.linea.ui.screens.history.HistoryGrouper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class Phase7PolishAndEdgeCasesTest {

    @Test
    fun testDurationFormattingEdgeCases() {
        assertEquals("", HistoryGrouper.formatDuration(0)) // Zero duration returns empty string for missed calls
        assertEquals("", HistoryGrouper.formatDuration(-10)) // Negative duration returns empty string
        assertEquals("45s", HistoryGrouper.formatDuration(45))
        assertEquals("1m 0s", HistoryGrouper.formatDuration(60))
        assertEquals("1m 5s", HistoryGrouper.formatDuration(65))
        assertEquals("1h 0m 0s", HistoryGrouper.formatDuration(3600))
        assertEquals("2h 5m 9s", HistoryGrouper.formatDuration(7509))
        assertEquals("100h 0m 0s", HistoryGrouper.formatDuration(360000))
    }

    @Test
    fun testOfflineCallerIdEdgeCases() {
        // Null and blank inputs
        val empty = OfflineCallerIdEngine.identifyNumber("")
        assertEquals("Cellular", empty.regionOrCountry)
        assertEquals("Standard", empty.category)
        assertFalse(empty.isEmergency)
        assertFalse(empty.isTollFree)

        val spaces = OfflineCallerIdEngine.identifyNumber("   ")
        assertEquals("Cellular", spaces.regionOrCountry)

        // Emergency services with whitespace or dashes
        val em1 = OfflineCallerIdEngine.identifyNumber(" 911 ")
        assertTrue(em1.isEmergency)
        assertEquals("EMERGENCY", em1.badgeLabel)

        val em2 = OfflineCallerIdEngine.identifyNumber("112")
        assertTrue(em2.isEmergency)
        assertEquals("Emergency Services", em2.regionOrCountry)

        val em3 = OfflineCallerIdEngine.identifyNumber("999")
        assertTrue(em3.isEmergency)

        // Toll-free variations
        val tf1 = OfflineCallerIdEngine.identifyNumber("+1 (800) 555-0199")
        assertTrue(tf1.isTollFree)
        assertEquals("TOLL-FREE", tf1.badgeLabel)

        val tf2 = OfflineCallerIdEngine.identifyNumber("18882345678")
        assertTrue(tf2.isTollFree)
        assertEquals("TOLL-FREE", tf2.badgeLabel)

        // Domestic carrier prefixes
        val gp = OfflineCallerIdEngine.identifyNumber("+8801712345678")
        assertFalse(gp.isEmergency)
        assertTrue(gp.regionOrCountry.contains("Grameenphone"))

        val robi = OfflineCallerIdEngine.identifyNumber("01812345678")
        assertTrue(robi.regionOrCountry.contains("Robi"))

        val bl = OfflineCallerIdEngine.identifyNumber("01912345678")
        assertTrue(bl.regionOrCountry.contains("Banglalink"))

        // Country code only
        val uk = OfflineCallerIdEngine.identifyNumber("+442079460991")
        assertEquals("United Kingdom", uk.regionOrCountry)

        val jp = OfflineCallerIdEngine.identifyNumber("+81312345678")
        assertEquals("Japan", jp.regionOrCountry)
    }

    @Test
    fun testAvailabilityInsightEmptyAndExtremeHistory() {
        // Empty history
        val emptyResult = AvailabilityInsightEngine.computeInsight(emptyList())
        assertEquals(0, emptyResult.answerRatePercent)
        assertFalse(emptyResult.hasSufficientData)

        // Insufficient calls (< 3)
        val fewCalls = listOf(
            CallRecordEntity(
                phoneNumber = "5551111",
                formattedNumber = "555-1111",
                callType = CallDirectionType.INCOMING,
                timestamp = 1700000000000L,
                durationSeconds = 60,
                simSlot = 0
            )
        )
        val fewResult = AvailabilityInsightEngine.computeInsight(fewCalls)
        assertFalse(fewResult.hasSufficientData)

        // All missed calls -> 0% answer rate
        val cal = Calendar.getInstance(TimeZone.getDefault())
        val timestamp = cal.timeInMillis
        val allMissed = (1..5).map { i ->
            CallRecordEntity(
                id = i.toLong(),
                phoneNumber = "5551111",
                formattedNumber = "555-1111",
                callType = CallDirectionType.MISSED,
                timestamp = timestamp + i * 1000L,
                durationSeconds = 0,
                simSlot = 0
            )
        }
        val missedResult = AvailabilityInsightEngine.computeInsight(allMissed)
        assertEquals(0, missedResult.answerRatePercent)

        // All answered calls -> 100% answer rate
        val allAnswered = (1..5).map { i ->
            CallRecordEntity(
                id = i.toLong(),
                phoneNumber = "5551111",
                formattedNumber = "555-1111",
                callType = CallDirectionType.INCOMING,
                timestamp = timestamp + i * 1000L,
                durationSeconds = 60,
                simSlot = 0
            )
        }
        val answeredResult = AvailabilityInsightEngine.computeInsight(allAnswered)
        assertEquals(100, answeredResult.answerRatePercent)
        assertTrue(answeredResult.hasSufficientData)
    }

    @Test
    fun testRepeatCallTrackerSlidingWindowBoundary() {
        val tracker = RepeatCallTracker()
        val numA = "+15551234567"
        val numB = "+15559876543"

        val t0 = 1000000000L
        val fiveMinutesAgo = { t: Long -> t - 300_000L }

        // Call 1 at t0
        tracker.recordAttempt(numA, t0)
        assertEquals(1, tracker.getRecentAttemptsCount(numA, fiveMinutesAgo(t0)))

        // Call 2 at t0 + 2 min (120s)
        val t1 = t0 + 120_000L
        tracker.recordAttempt(numA, t1)
        assertEquals(2, tracker.getRecentAttemptsCount(numA, fiveMinutesAgo(t1)))

        // Call from different number numB should not affect numA
        tracker.recordAttempt(numB, t1 + 30_000L)
        assertEquals(2, tracker.getRecentAttemptsCount(numA, fiveMinutesAgo(t1 + 30_000L)))
        assertEquals(1, tracker.getRecentAttemptsCount(numB, fiveMinutesAgo(t1 + 30_000L)))

        // Call 3 at t0 + 4 min (240s) within 5-minute window (300s) -> 3 calls reached
        val t2 = t0 + 240_000L
        tracker.recordAttempt(numA, t2)
        val countA = tracker.getRecentAttemptsCount(numA, fiveMinutesAgo(t2))
        assertEquals(3, countA)
        assertTrue(countA >= 3)

        // Beyond 5 minutes from t0 (e.g. t0 + 6 min), the first call drops off
        val t3 = t0 + 360_000L
        val countBeyond = tracker.getRecentAttemptsCount(numA, fiveMinutesAgo(t3))
        assertEquals(2, countBeyond) // Only calls at t1 (120s) and t2 (240s) remain
    }
}
