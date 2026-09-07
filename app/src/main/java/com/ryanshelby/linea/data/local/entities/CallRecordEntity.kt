package com.ryanshelby.linea.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class CallDirectionType {
    INCOMING,
    OUTGOING,
    MISSED,
    REJECTED,
    BLOCKED
}

@Entity(
    tableName = "call_records",
    indices = [
        Index(value = ["phoneNumber"]),
        Index(value = ["timestamp"]),
        Index(value = ["sessionGroupId"])
    ]
)
data class CallRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val phoneNumber: String,
    val formattedNumber: String,
    val callerName: String? = null,
    val photoUri: String? = null,
    val callType: CallDirectionType,
    val timestamp: Long,
    val durationSeconds: Long = 0,
    val simSlot: Int = 0,
    val simDisplayName: String? = null,
    val networkType: String = "UNKNOWN", // 5G, LTE, VoLTE, VoWiFi, 3G, 2G, UNKNOWN
    val isPrivateContact: Boolean = false,
    val isLocationOptIn: Boolean = false,
    val locationCategory: String? = null, // Home, Work, None
    val notes: String? = null,
    val sessionGroupId: String? = null // For collapsed session grouping in Phase 6
)
