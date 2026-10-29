package com.ryanshelby.linea.ui.screens.history

import com.ryanshelby.linea.data.local.entities.CallDirectionType
import com.ryanshelby.linea.data.local.entities.CallRecordEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class HistoryFilter {
    ALL,
    MISSED,
    INCOMING,
    OUTGOING,
    BLOCKED
}

data class CallHistoryItem(
    val id: String,
    val primaryRecord: CallRecordEntity,
    val groupedCalls: List<CallRecordEntity>,
    val callCount: Int,
    val isExpanded: Boolean = false
)

data class DateGroup(
    val dateHeader: String,
    val items: List<CallHistoryItem>
)

data class CallSessionItem(
    val id: String,
    val phoneNumber: String,
    val callerName: String?,
    val callCount: Int,
    val totalDurationSeconds: Long,
    val latestTimestamp: Long,
    val latestCallType: CallDirectionType,
    val latestSimSlot: Int,
    val calls: List<CallRecordEntity>,
    val isExpanded: Boolean = false
)

object HistoryGrouper {

    fun normalizeNumberKey(number: String): String {
        val digits = number.filter { it.isDigit() }
        return when {
            digits.length >= 10 -> digits.takeLast(10)
            digits.length >= 7 -> digits.takeLast(7)
            digits.isNotEmpty() -> digits
            else -> number.trim()
        }
    }

    fun getGroupingKey(rec: CallRecordEntity): String {
        val norm = normalizeNumberKey(rec.phoneNumber)
        return if (norm.isNotEmpty()) {
            "num_$norm"
        } else if (!rec.callerName.isNullOrBlank()) {
            "name_${rec.callerName.trim().lowercase()}"
        } else {
            rec.phoneNumber.trim()
        }
    }

    fun deduplicateCallRecords(sortedDescending: List<CallRecordEntity>): List<CallRecordEntity> {
        val result = mutableListOf<CallRecordEntity>()
        for (rec in sortedDescending) {
            val existingIndex = result.indexOfFirst { existing ->
                val timeDiff = kotlin.math.abs(existing.timestamp - rec.timestamp)
                val sameType = existing.callType == rec.callType
                val sameNumber = normalizeNumberKey(existing.phoneNumber) == normalizeNumberKey(rec.phoneNumber)
                val durDiff = kotlin.math.abs(existing.durationSeconds - rec.durationSeconds)
                sameNumber && sameType && timeDiff <= 3_000L && durDiff <= 2L
            }
            if (existingIndex >= 0) {
                val existing = result[existingIndex]
                val preferred = when {
                    existing.callerName.isNullOrBlank() && !rec.callerName.isNullOrBlank() -> rec
                    existing.durationSeconds < rec.durationSeconds -> rec
                    rec.timestamp < existing.timestamp && rec.durationSeconds == existing.durationSeconds -> rec
                    else -> existing
                }
                result[existingIndex] = preferred
            } else {
                result.add(rec)
            }
        }
        return result
    }

    fun groupSessions(records: List<CallRecordEntity>): List<CallSessionItem> {
        return records.groupBy { getGroupingKey(it) }
            .mapNotNull { (groupKey, calls) ->
                val sorted = calls.sortedByDescending { it.timestamp }
                val deduplicatedList = deduplicateCallRecords(sorted)
                if (deduplicatedList.isEmpty()) return@mapNotNull null
                val latest = deduplicatedList.first()
                val bestCallerName = deduplicatedList.firstOrNull { !it.callerName.isNullOrBlank() }?.callerName ?: latest.callerName
                val bestNumber = deduplicatedList.firstOrNull { it.phoneNumber.isNotBlank() }?.phoneNumber ?: latest.phoneNumber
                CallSessionItem(
                    id = "session_${groupKey}_${latest.id}",
                    phoneNumber = bestNumber,
                    callerName = bestCallerName,
                    callCount = deduplicatedList.size,
                    totalDurationSeconds = deduplicatedList.sumOf { it.durationSeconds },
                    latestTimestamp = latest.timestamp,
                    latestCallType = latest.callType,
                    latestSimSlot = latest.simSlot,
                    calls = deduplicatedList
                )
            }
            .sortedByDescending { it.latestTimestamp }
    }

    private val dateFormatter = DateTimeFormatter.ofPattern("EEE, d MMM")
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    fun formatRelativeDate(timestamp: Long): String {
        val callDate = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        return when (callDate) {
            today -> "Today"
            yesterday -> "Yesterday"
            else -> callDate.format(dateFormatter)
        }
    }

    fun formatExactTime(timestamp: Long): String {
        return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(timeFormatter)
    }

    fun formatRelativeTime(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val mins = diff / (60 * 1000)
        val hours = diff / (60 * 60 * 1000)
        val days = diff / (24 * 60 * 60 * 1000)
        return when {
            mins < 1 -> "Just now"
            mins < 60 -> "${mins}m ago"
            hours < 24 -> "${hours}h ago"
            days == 1L -> "Yesterday"
            else -> "${days}d ago"
        }
    }

    fun formatDuration(seconds: Long): String {
        if (seconds <= 0) return ""
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return when {
            hrs > 0 -> "${hrs}h ${mins}m ${secs}s"
            mins > 0 -> "${mins}m ${secs}s"
            else -> "${secs}s"
        }
    }

    /**
     * Groups raw call records by date, displaying each call as its own distinct entry
     * with accurate timestamp, call status, and duration.
     */
    fun groupRecords(records: List<CallRecordEntity>): List<DateGroup> {
        if (records.isEmpty()) return emptyList()

        val deduplicated = deduplicateCallRecords(records.sortedByDescending { it.timestamp })

        // 1. Group by date string ("Today", "Yesterday", "Mon, 14 Jul")
        val byDateMap = LinkedHashMap<String, MutableList<CallRecordEntity>>()
        for (record in deduplicated) {
            val dateKey = formatRelativeDate(record.timestamp)
            byDateMap.getOrPut(dateKey) { mutableListOf() }.add(record)
        }

        val dateGroups = mutableListOf<DateGroup>()

        for ((dateHeader, dateRecords) in byDateMap) {
            val items = dateRecords.map { rec ->
                CallHistoryItem(
                    id = "call_${rec.id}_${rec.timestamp}",
                    primaryRecord = rec,
                    groupedCalls = listOf(rec),
                    callCount = 1,
                    isExpanded = false
                )
            }
            dateGroups.add(DateGroup(dateHeader = dateHeader, items = items))
        }

        return dateGroups
    }
}
