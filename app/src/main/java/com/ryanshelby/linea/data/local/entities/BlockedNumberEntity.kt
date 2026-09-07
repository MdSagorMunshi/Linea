package com.ryanshelby.linea.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class BlockMatchType {
    EXACT,
    PREFIX,
    RANGE,
    UNKNOWN,
    PRIVATE,
    NON_CONTACT,
    INTERNATIONAL
}

enum class BlockAction {
    SILENT_REJECT,
    VOICEMAIL,
    HANGUP
}

@Entity(
    tableName = "blocked_numbers",
    indices = [
        Index(value = ["numberOrPrefix"]),
        Index(value = ["matchType"]),
        Index(value = ["expiresAt"])
    ]
)
data class BlockedNumberEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val numberOrPrefix: String,
    val matchType: BlockMatchType = BlockMatchType.EXACT,
    val reason: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null, // null for permanent, timestamp for 1h, 1d, 1w, tomorrow
    val blockAction: BlockAction = BlockAction.SILENT_REJECT
)

@Entity(
    tableName = "blocked_call_logs",
    indices = [
        Index(value = ["number"]),
        Index(value = ["timestamp"])
    ]
)
data class BlockedCallLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val number: String,
    val timestamp: Long = System.currentTimeMillis(),
    val matchedRuleId: Long? = null,
    val matchedRuleType: String = "EXACT",
    val actionTaken: String = "SILENT_REJECT"
)
