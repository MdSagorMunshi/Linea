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
    fun groupRecords_recordsSeparateEntriesForCallsFromSameNumber() {
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
        // Each call must appear as a separate entry with accurate timestamp and details
        assertEquals(3, items.size)

        // Item 0 is latest (Bob, missed, 0s)
        assertEquals("5559999", items[0].primaryRecord.phoneNumber)
        assertEquals(CallDirectionType.MISSED, items[0].primaryRecord.callType)
        assertEquals(0L, items[0].primaryRecord.durationSeconds)

        // Item 1 is Alice incoming (60s)
        assertEquals("5551234", items[1].primaryRecord.phoneNumber)
        assertEquals(CallDirectionType.INCOMING, items[1].primaryRecord.callType)
        assertEquals(60L, items[1].primaryRecord.durationSeconds)

        // Item 2 is Alice outgoing (30s)
        assertEquals("5551234", items[2].primaryRecord.phoneNumber)
        assertEquals(CallDirectionType.OUTGOING, items[2].primaryRecord.callType)
        assertEquals(30L, items[2].primaryRecord.durationSeconds)
    }

    @Test
    fun groupRecords_deduplicatesOnlyExactDuplicateWritesWithin3Seconds() {
        val now = System.currentTimeMillis()
        // Simulate a call logged twice simultaneously (e.g. system and app double write within 1s)
        val records = listOf(
            CallRecordEntity(
                id = 114,
                phoneNumber = "15559012345",
                formattedNumber = "15559012345",
                callerName = "Sarah Connor",
                callType = CallDirectionType.OUTGOING,
                timestamp = now - 1000,
                durationSeconds = 10
            ),
            CallRecordEntity(
                id = 115,
                phoneNumber = "15559012345",
                formattedNumber = "15559012345",
                callerName = "Sarah Connor",
                callType = CallDirectionType.OUTGOING,
                timestamp = now - 1500, // 500ms difference
                durationSeconds = 10
            )
        )

        val groups = HistoryGrouper.groupRecords(records)
        assertEquals(1, groups.size)
        val items = groups.first().items
        assertEquals(1, items.size)
        val sarah = items.first()
        assertEquals(1, sarah.callCount)
    }

    @Test
    fun groupSessions_coalescesSameDayCallsFromSameNumber() {
        val now = System.currentTimeMillis()
        val records = listOf(
            CallRecordEntity(
                id = 1,
                phoneNumber = "+1 (555) 901-2345",
                formattedNumber = "+1 (555) 901-2345",
                callerName = "Sarah Connor",
                callType = CallDirectionType.OUTGOING,
                timestamp = now - 60000,
                durationSeconds = 20
            ),
            CallRecordEntity(
                id = 2,
                phoneNumber = "555-901-2345",
                formattedNumber = "555-901-2345",
                callerName = "Sarah Connor",
                callType = CallDirectionType.OUTGOING,
                timestamp = now - 120000,
                durationSeconds = 15
            ),
            CallRecordEntity(
                id = 3,
                phoneNumber = "15559012345",
                formattedNumber = "15559012345",
                callerName = "Sarah Connor",
                callType = CallDirectionType.OUTGOING,
                timestamp = now - 180000,
                durationSeconds = 30
            )
        )

        val sessions = HistoryGrouper.groupSessions(records)
        // All variants must be grouped into a single session
        assertEquals(1, sessions.size)
        val sarah = sessions.first()
        assertEquals(3, sarah.callCount)
        assertEquals(65L, sarah.totalDurationSeconds)
        assertEquals("Sarah Connor", sarah.callerName)
    }

    @Test
    fun groupRecords_latestMissedCallSwitchesToNormalWhenFollowedByIncomingOrOutgoing() {
        val now = System.currentTimeMillis()

        // 1. Initially, latest call is MISSED
        val recordsWithMissedLatest = listOf(
            CallRecordEntity(
                id = 2,
                phoneNumber = "5551234",
                formattedNumber = "5551234",
                callerName = "John",
                callType = CallDirectionType.MISSED,
                timestamp = now - 1000,
                durationSeconds = 0
            ),
            CallRecordEntity(
                id = 1,
                phoneNumber = "5551234",
                formattedNumber = "5551234",
                callerName = "John",
                callType = CallDirectionType.OUTGOING,
                timestamp = now - 60000,
                durationSeconds = 40
            )
        )

        val initialGroups = HistoryGrouper.groupRecords(recordsWithMissedLatest)
        val initialItem = initialGroups.first().items.first()
        assertEquals(CallDirectionType.MISSED, initialItem.primaryRecord.callType)

        // 2. Later, John calls back (INCOMING) or user calls back (OUTGOING)
        val recordsWithSubsequentCall = listOf(
            CallRecordEntity(
                id = 3,
                phoneNumber = "5551234",
                formattedNumber = "5551234",
                callerName = "John",
                callType = CallDirectionType.INCOMING,
                timestamp = now + 5000,
                durationSeconds = 25
            )
        ) + recordsWithMissedLatest

        val updatedGroups = HistoryGrouper.groupRecords(recordsWithSubsequentCall)
        val updatedItem = updatedGroups.first().items.first()
        // Latest call is now INCOMING, switching it back to normal (non-missed)
        assertEquals(CallDirectionType.INCOMING, updatedItem.primaryRecord.callType)
    }

    @Test
    fun groupRecords_handlesEmptyList() {
        val groups = HistoryGrouper.groupRecords(emptyList())
        assertTrue(groups.isEmpty())
    }
}
