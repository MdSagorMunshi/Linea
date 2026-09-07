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
    BLOCKED,
    RECORDINGS
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

object HistoryGrouper {

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
        val mins = seconds / 60
        val secs = seconds % 60
        return if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
    }

    /**
     * Groups raw call records by date, and within each date groups repeated calls
     * from the same phone number into a single collapsible item with badge count.
     */
    fun groupRecords(records: List<CallRecordEntity>): List<DateGroup> {
        if (records.isEmpty()) return emptyList()

        // 1. Group by date string ("Today", "Yesterday", "Mon, 14 Jul")
        val byDateMap = LinkedHashMap<String, MutableList<CallRecordEntity>>()
        for (record in records) {
            val dateKey = formatRelativeDate(record.timestamp)
            byDateMap.getOrPut(dateKey) { mutableListOf() }.add(record)
        }

        val dateGroups = mutableListOf<DateGroup>()

        for ((dateHeader, dateRecords) in byDateMap) {
            // Group repeated calls by phone number within the same day
            val byNumberMap = LinkedHashMap<String, MutableList<CallRecordEntity>>()
            for (rec in dateRecords) {
                val cleanNumber = rec.phoneNumber.filter { it.isDigit() }
                val key = if (cleanNumber.isNotEmpty()) cleanNumber else rec.phoneNumber
                byNumberMap.getOrPut(key) { mutableListOf() }.add(rec)
            }

            val items = mutableListOf<CallHistoryItem>()
            for ((key, callList) in byNumberMap) {
                val sortedList = callList.sortedByDescending { it.timestamp }
                val primary = sortedList.first()
                items.add(
                    CallHistoryItem(
                        id = "${dateHeader}_${key}_${primary.id}",
                        primaryRecord = primary,
                        groupedCalls = sortedList,
                        callCount = sortedList.size,
                        isExpanded = false
                    )
                )
            }
            dateGroups.add(DateGroup(dateHeader = dateHeader, items = items))
        }

        return dateGroups
    }
}
