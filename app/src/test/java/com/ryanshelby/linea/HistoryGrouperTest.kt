package com.ryanshelby.linea

import com.ryanshelby.linea.data.local.entities.CallDirectionType
import com.ryanshelby.linea.data.local.entities.CallRecordEntity
import com.ryanshelby.linea.ui.screens.history.HistoryGrouper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class HistoryGrouperTest {

    @Test
    fun formatDuration_formatsSecondsAndMinutes() {
        assertEquals("45s", HistoryGrouper.formatDuration(45))
        assertEquals("2m 15s", HistoryGrouper.formatDuration(135))
        assertEquals("", HistoryGrouper.formatDuration(0))
    }

    @Test
    fun formatRelativeDate_returnsTodayAndYesterday() {
        val now = System.currentTimeMillis()
        assertEquals("Today", HistoryGrouper.formatRelativeDate(now))

        val yesterday = LocalDate.now().minusDays(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli() + 3600000 // yesterday afternoon
        assertEquals("Yesterday", HistoryGrouper.formatRelativeDate(yesterday))
    }

    @Test
    fun groupRecords_coalescesSameDayCallsFromSameNumber() {
        val now = System.currentTimeMillis()
        val records = listOf(
            CallRecordEntity(
                id = 1,
                phoneNumber = "5551234",
                formattedNumber = "5551234",
                callerName = "Alice",
                callType = CallDirectionType.OUTGOING,
                timestamp = now - 5000,
                durationSeconds = 30
            ),
            CallRecordEntity(
                id = 2,
                phoneNumber = "5551234",
                formattedNumber = "5551234",
                callerName = "Alice",
                callType = CallDirectionType.INCOMING,
                timestamp = now - 2000,
                durationSeconds = 60
            ),
            CallRecordEntity(
                id = 3,
                phoneNumber = "5559999",
                formattedNumber = "5559999",
                callerName = "Bob",
                callType = CallDirectionType.MISSED,
                timestamp = now - 1000,
                durationSeconds = 0
            )
        )

        val groups = HistoryGrouper.groupRecords(records)
        assertEquals(1, groups.size) // 1 day group: "Today"
        assertEquals("Today", groups.first().dateHeader)

        val items = groups.first().items
        assertEquals(2, items.size) // 2 distinct numbers: 5551234 and 5559999

        val aliceGroup = items.find { it.primaryRecord.phoneNumber == "5551234" }
        assertEquals(2, aliceGroup?.callCount)
        assertEquals(2, aliceGroup?.groupedCalls?.size)
        // Primary record should be the latest call (id = 2)
        assertEquals(2L, aliceGroup?.primaryRecord?.id)
        assertEquals(CallDirectionType.INCOMING, aliceGroup?.primaryRecord?.callType)
    }

    @Test
    fun groupRecords_handlesEmptyList() {
        val groups = HistoryGrouper.groupRecords(emptyList())
        assertTrue(groups.isEmpty())
    }
}
