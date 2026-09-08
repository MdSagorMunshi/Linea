package com.ryanshelby.linea

import com.ryanshelby.linea.data.local.entities.CallDirectionType
import com.ryanshelby.linea.data.local.entities.CallRecordEntity
import com.ryanshelby.linea.telecom.logic.AvailabilityInsightEngine
import com.ryanshelby.linea.telecom.screening.OfflineCallerIdEngine
import com.ryanshelby.linea.ui.screens.history.HistoryGrouper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class Phase6DifferentiatorsTest {

    @Test
    fun testAvailabilityInsightEngine_insufficientHistory() {
        val insight = AvailabilityInsightEngine.computeInsight(emptyList())
        assertFalse(insight.hasSufficientData)
        assertEquals(0, insight.answerRatePercent)
        assertTrue(insight.summary.contains("Not enough call history", ignoreCase = true))
    }

    @Test
    fun testAvailabilityInsightEngine_answeredCallsPattern() {
        // Create 10 calls on Tuesday at 14:30 (2 PM)
        val cal = Calendar.getInstance(TimeZone.getDefault()).apply {
            set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY)
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 30)
        }
        val timestamp = cal.timeInMillis

        val calls = mutableListOf<CallRecordEntity>()
        // 8 answered incoming calls
        repeat(8) { i ->
            calls.add(
                CallRecordEntity(
                    id = (i + 1).toLong(),
                    phoneNumber = "+15551234567",
                    formattedNumber = "+1 (555) 123-4567",
                    callType = CallDirectionType.INCOMING,
                    timestamp = timestamp + (i * 1000L),
                    durationSeconds = 120,
                    simSlot = 0
                )
            )
        }
        // 2 missed calls
        repeat(2) { i ->
            calls.add(
                CallRecordEntity(
                    id = (i + 9).toLong(),
                    phoneNumber = "+15551234567",
                    formattedNumber = "+1 (555) 123-4567",
                    callType = CallDirectionType.MISSED,
                    timestamp = timestamp + (i * 1000L),
                    durationSeconds = 0,
                    simSlot = 0
                )
            )
        }

        val insight = AvailabilityInsightEngine.computeInsight(calls)
        assertTrue(insight.hasSufficientData)
        assertEquals(80, insight.answerRatePercent)
        assertEquals("Weekdays", insight.dayType)
        assertTrue(insight.bestTimeWindow.contains("2 PM", ignoreCase = true))
    }

    @Test
    fun testOfflineCallerIdEngine_emergency() {
        val r911 = OfflineCallerIdEngine.identifyNumber("911")
        assertTrue(r911.isEmergency)
        assertEquals("EMERGENCY", r911.badgeLabel)

        val r112 = OfflineCallerIdEngine.identifyNumber("112")
        assertTrue(r112.isEmergency)
        assertEquals("Emergency Services", r112.regionOrCountry)
    }

    @Test
    fun testOfflineCallerIdEngine_tollFree() {
        val r800 = OfflineCallerIdEngine.identifyNumber("+18005551212")
        assertTrue(r800.isTollFree)
        assertEquals("TOLL-FREE", r800.badgeLabel)

        val r888 = OfflineCallerIdEngine.identifyNumber("18885551212")
        assertTrue(r888.isTollFree)
        assertEquals("TOLL-FREE", r888.badgeLabel)
    }

    @Test
    fun testOfflineCallerIdEngine_bangladeshCarrier() {
        val gp = OfflineCallerIdEngine.identifyNumber("+8801712345678")
        assertFalse(gp.isEmergency)
        assertFalse(gp.isTollFree)
        assertEquals("Mobile Network", gp.category)
        assertTrue(gp.regionOrCountry.contains("Grameenphone"))

        val robi = OfflineCallerIdEngine.identifyNumber("+8801812345678")
        assertTrue(robi.regionOrCountry.contains("Robi"))
    }

    @Test
    fun testOfflineCallerIdEngine_internationalCountry() {
        val uk = OfflineCallerIdEngine.identifyNumber("+447911123456")
        assertEquals("United Kingdom", uk.regionOrCountry)

        val japan = OfflineCallerIdEngine.identifyNumber("+819012345678")
        assertEquals("Japan", japan.regionOrCountry)

        val germany = OfflineCallerIdEngine.identifyNumber("+4915123456789")
        assertEquals("Germany", germany.regionOrCountry)
    }

    @Test
    fun testHistoryGrouper_groupSessions() {
        val now = System.currentTimeMillis()
        val records = listOf(
            CallRecordEntity(
                id = 1L,
                phoneNumber = "+15551111111",
                formattedNumber = "+1 555 111 1111",
                callerName = "Alice",
                callType = CallDirectionType.INCOMING,
                timestamp = now - 5000,
                durationSeconds = 60,
                simSlot = 0
            ),
            CallRecordEntity(
                id = 2L,
                phoneNumber = "+15551111111",
                formattedNumber = "+1 555 111 1111",
                callerName = "Alice",
                callType = CallDirectionType.OUTGOING,
                timestamp = now - 2000,
                durationSeconds = 120,
                simSlot = 0
            ),
            CallRecordEntity(
                id = 3L,
                phoneNumber = "+15552222222",
                formattedNumber = "+1 555 222 2222",
                callerName = "Bob",
                callType = CallDirectionType.MISSED,
                timestamp = now - 1000,
                durationSeconds = 0,
                simSlot = 1
            )
        )

        val sessions = HistoryGrouper.groupSessions(records)
        assertEquals(2, sessions.size)

        // Bob called most recently (now - 1000)
        assertEquals("+15552222222", sessions[0].phoneNumber)
        assertEquals(1, sessions[0].callCount)
        assertEquals(0L, sessions[0].totalDurationSeconds)

        // Alice has 2 calls, total duration 180s
        assertEquals("+15551111111", sessions[1].phoneNumber)
        assertEquals("Alice", sessions[1].callerName)
        assertEquals(2, sessions[1].callCount)
        assertEquals(180L, sessions[1].totalDurationSeconds)
        assertEquals(CallDirectionType.OUTGOING, sessions[1].latestCallType)
    }
}
