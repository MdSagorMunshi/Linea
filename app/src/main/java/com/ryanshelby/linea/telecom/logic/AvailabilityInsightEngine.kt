package com.ryanshelby.linea.telecom.logic

import com.ryanshelby.linea.data.local.entities.CallDirectionType
import com.ryanshelby.linea.data.local.entities.CallRecordEntity
import java.util.Calendar

data class AvailabilityInsight(
    val hasSufficientData: Boolean,
    val bestTimeWindow: String,
    val dayType: String,
    val answeredCount: Int,
    val totalCountInWindow: Int,
    val answerRatePercent: Int,
    val summary: String
)

object AvailabilityInsightEngine {

    data class TimeSlot(
        val slotIndex: Int,
        val label: String,
        val startHour: Int,
        val endHour: Int
    )

    private val SLOTS = listOf(
        TimeSlot(0, "Morning (8 AM – 10 AM)", 8, 10),
        TimeSlot(1, "Late Morning (10 AM – 12 PM)", 10, 12),
        TimeSlot(2, "Early Afternoon (12 PM – 2 PM)", 12, 14),
        TimeSlot(3, "Afternoon (2 PM – 4 PM)", 14, 16),
        TimeSlot(4, "Late Afternoon (4 PM – 6 PM)", 16, 18),
        TimeSlot(5, "Evening (6 PM – 8 PM)", 18, 20),
        TimeSlot(6, "Night (8 PM – 10 PM)", 20, 22),
        TimeSlot(7, "Overnight (10 PM – 8 AM)", 22, 24)
    )

    fun computeInsight(records: List<CallRecordEntity>): AvailabilityInsight {
        if (records.size < 3) {
            return AvailabilityInsight(
                hasSufficientData = false,
                bestTimeWindow = "Insufficient Data",
                dayType = "N/A",
                answeredCount = 0,
                totalCountInWindow = 0,
                answerRatePercent = 0,
                summary = "Not enough call history to identify availability patterns yet."
            )
        }

        val calendar = Calendar.getInstance()

        // Map: SlotIndex to Pair(answeredCount, totalCount)
        val weekdaySlotCounts = mutableMapOf<Int, Pair<Int, Int>>()
        val weekendSlotCounts = mutableMapOf<Int, Pair<Int, Int>>()

        for (record in records) {
            calendar.timeInMillis = record.timestamp
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY

            val slot = SLOTS.firstOrNull {
                if (it.startHour < it.endHour) {
                    hour in it.startHour until it.endHour
                } else {
                    hour >= it.startHour || hour < 8
                }
            } ?: SLOTS[7]

            val isAnswered = record.durationSeconds > 0 &&
                    record.callType != CallDirectionType.MISSED &&
                    record.callType != CallDirectionType.REJECTED &&
                    record.callType != CallDirectionType.BLOCKED

            val targetMap = if (isWeekend) weekendSlotCounts else weekdaySlotCounts
            val current = targetMap[slot.slotIndex] ?: Pair(0, 0)
            val updated = Pair(
                if (isAnswered) current.first + 1 else current.first,
                current.second + 1
            )
            targetMap[slot.slotIndex] = updated
        }

        // Find best window across weekday and weekend
        var bestSlot = SLOTS[5] // Default evening
        var bestDayType = "Weekdays"
        var bestAnswered = 0
        var bestTotal = 0
        var bestRate = 0.0

        fun evaluateMap(map: Map<Int, Pair<Int, Int>>, dayLabel: String) {
            for ((slotIdx, counts) in map) {
                val answered = counts.first
                val total = counts.second
                if (total >= 2) {
                    val rate = answered.toDouble() / total.toDouble()
                    if (rate > bestRate || (rate == bestRate && answered > bestAnswered)) {
                        bestRate = rate
                        bestAnswered = answered
                        bestTotal = total
                        bestSlot = SLOTS.first { it.slotIndex == slotIdx }
                        bestDayType = dayLabel
                    }
                }
            }
        }

        evaluateMap(weekdaySlotCounts, "Weekdays")
        evaluateMap(weekendSlotCounts, "Weekends")

        // If no slot had >= 2 calls, fallback to overall slot with most answered
        if (bestTotal == 0) {
            val combinedCounts = mutableMapOf<Int, Pair<Int, Int>>()
            for (record in records) {
                calendar.timeInMillis = record.timestamp
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val slot = SLOTS.firstOrNull {
                    if (it.startHour < it.endHour) hour in it.startHour until it.endHour else hour >= it.startHour || hour < 8
                } ?: SLOTS[7]
                val isAnswered = record.durationSeconds > 0
                val cur = combinedCounts[slot.slotIndex] ?: Pair(0, 0)
                combinedCounts[slot.slotIndex] = Pair(if (isAnswered) cur.first + 1 else cur.first, cur.second + 1)
            }
            val maxEntry = combinedCounts.maxByOrNull { it.value.first }
            if (maxEntry != null && maxEntry.value.second > 0) {
                bestSlot = SLOTS.first { it.slotIndex == maxEntry.key }
                bestAnswered = maxEntry.value.first
                bestTotal = maxEntry.value.second
                bestRate = bestAnswered.toDouble() / bestTotal.toDouble()
                bestDayType = "Any Day"
            }
        }

        val ratePercent = (bestRate * 100).toInt()
        val summary = "Usually available $bestDayType during ${bestSlot.label.substringBefore(" (")} • $ratePercent% answer rate ($bestAnswered of $bestTotal calls connected)"

        return AvailabilityInsight(
            hasSufficientData = true,
            bestTimeWindow = bestSlot.label,
            dayType = bestDayType,
            answeredCount = bestAnswered,
            totalCountInWindow = bestTotal,
            answerRatePercent = ratePercent,
            summary = summary
        )
    }
}
