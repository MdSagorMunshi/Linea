package com.ryanshelby.linea.telecom.screening

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RepeatCallTracker @Inject constructor() {

    // Maps normalized phone number to list of attempt epoch timestamps
    private val attemptsMap = ConcurrentHashMap<String, MutableList<Long>>()

    /**
     * Records an incoming call attempt from a given phone number.
     */
    @Synchronized
    fun recordAttempt(phoneNumber: String, timestamp: Long = System.currentTimeMillis()) {
        val cleanNumber = phoneNumber.filter { it.isDigit() || it == '+' }
        if (cleanNumber.isBlank()) return

        val list = attemptsMap.getOrPut(cleanNumber) { mutableListOf() }
        list.add(timestamp)

        // Prune older than 10 minutes to prevent memory leak
        val tenMinutesAgo = timestamp - 10 * 60 * 1000L
        list.removeAll { it < tenMinutesAgo }
    }

    /**
     * Returns the number of recorded attempts for this phone number since [sinceTime].
     */
    @Synchronized
    fun getRecentAttemptsCount(phoneNumber: String, sinceTime: Long): Int {
        val cleanNumber = phoneNumber.filter { it.isDigit() || it == '+' }
        if (cleanNumber.isBlank()) return 0

        val list = attemptsMap[cleanNumber] ?: return 0
        return list.count { it >= sinceTime }
    }

    /**
     * Clears all recorded history for this number once call has been answered or acknowledged.
     */
    @Synchronized
    fun clearAttempts(phoneNumber: String) {
        val cleanNumber = phoneNumber.filter { it.isDigit() || it == '+' }
        attemptsMap.remove(cleanNumber)
    }
}
