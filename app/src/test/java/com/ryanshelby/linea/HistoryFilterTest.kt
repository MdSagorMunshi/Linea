package com.ryanshelby.linea

import com.ryanshelby.linea.data.local.entities.CallDirectionType
import com.ryanshelby.linea.data.local.entities.CallRecordEntity
import com.ryanshelby.linea.ui.screens.history.HistoryFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryFilterTest {

    private fun createRecord(id: Long, number: String, type: CallDirectionType): CallRecordEntity {
        return CallRecordEntity(
            id = id,
            phoneNumber = number,
            formattedNumber = number,
            callerName = "User $id",
            callType = type,
            timestamp = System.currentTimeMillis() - id * 1000
        )
    }

    private fun filterRecords(
        records: List<CallRecordEntity>,
        filter: HistoryFilter,
        blockedSet: Set<String> = emptySet()
    ): List<CallRecordEntity> {
        return when (filter) {
            HistoryFilter.ALL -> records
            HistoryFilter.MISSED -> records.filter { it.callType == CallDirectionType.MISSED }
            HistoryFilter.INCOMING -> records.filter { it.callType == CallDirectionType.INCOMING }
            HistoryFilter.OUTGOING -> records.filter { it.callType == CallDirectionType.OUTGOING }
            HistoryFilter.BLOCKED -> records.filter { rec ->
                rec.callType == CallDirectionType.BLOCKED ||
                blockedSet.contains(rec.phoneNumber)
            }
        }
    }

    @Test
    fun filter_incoming_returnsOnlyIncomingCalls() {
        val records = listOf(
            createRecord(1, "111", CallDirectionType.INCOMING),
            createRecord(2, "222", CallDirectionType.OUTGOING),
            createRecord(3, "333", CallDirectionType.MISSED),
            createRecord(4, "444", CallDirectionType.INCOMING)
        )

        val result = filterRecords(records, HistoryFilter.INCOMING)
        assertEquals(2, result.size)
        assertTrue(result.all { it.callType == CallDirectionType.INCOMING })
        assertEquals("111", result[0].phoneNumber)
        assertEquals("444", result[1].phoneNumber)
    }

    @Test
    fun filter_outgoing_returnsOnlyOutgoingCalls() {
        val records = listOf(
            createRecord(1, "111", CallDirectionType.INCOMING),
            createRecord(2, "222", CallDirectionType.OUTGOING),
            createRecord(3, "333", CallDirectionType.MISSED),
            createRecord(4, "555", CallDirectionType.OUTGOING)
        )

        val result = filterRecords(records, HistoryFilter.OUTGOING)
        assertEquals(2, result.size)
        assertTrue(result.all { it.callType == CallDirectionType.OUTGOING })
        assertEquals("222", result[0].phoneNumber)
        assertEquals("555", result[1].phoneNumber)
    }

    @Test
    fun filter_all_returnsAllCalls() {
        val records = listOf(
            createRecord(1, "111", CallDirectionType.INCOMING),
            createRecord(2, "222", CallDirectionType.OUTGOING),
            createRecord(3, "333", CallDirectionType.MISSED)
        )

        val result = filterRecords(records, HistoryFilter.ALL)
        assertEquals(3, result.size)
    }
}
