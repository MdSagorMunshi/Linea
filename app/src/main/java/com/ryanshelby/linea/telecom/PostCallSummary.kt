package com.ryanshelby.linea.telecom

import com.ryanshelby.linea.telecom.screening.CallerIdResult
import java.util.Locale

data class PostCallSummary(
    val phoneNumber: String,
    val callerName: String? = null,
    val photoUri: String? = null,
    val durationSeconds: Long = 0L,
    val isIncoming: Boolean = false,
    val wasConnected: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val callerIdResult: CallerIdResult? = null
) {
    val displayTitle: String
        get() = callerName?.takeIf { it.isNotBlank() } ?: phoneNumber

    val formattedDuration: String
        get() {
            if (!wasConnected || durationSeconds <= 0L) {
                return if (isIncoming) "Missed" else "Unanswered"
            }
            val hours = durationSeconds / 3600
            val minutes = (durationSeconds % 3600) / 60
            val seconds = durationSeconds % 60
            return if (hours > 0) {
                String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(Locale.US, "%d:%02d", minutes, seconds)
            }
        }
}
