package com.ryanshelby.linea.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "dialer_profiles")
data class DialerProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String, // Personal, Work, Custom
    val simSlot: Int = 0,
    val ringtoneUri: String? = null,
    val allowedGroupIds: String? = null,
    val isDefault: Boolean = false
)

@Entity(
    tableName = "pinned_contacts",
    foreignKeys = [
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["contactId"], unique = true),
        Index(value = ["pinOrder"])
    ]
)
data class PinnedContactEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contactId: Long,
    val pinOrder: Int = 0
)

@Entity(
    tableName = "call_notes",
    indices = [
        Index(value = ["contactId"]),
        Index(value = ["phoneNumber"]),
        Index(value = ["timestamp"])
    ]
)
data class CallNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contactId: Long? = null,
    val phoneNumber: String,
    val noteText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPreCallNote: Boolean = false
)

enum class ReminderStatus {
    PENDING,
    COMPLETED,
    DISMISSED
}

@Entity(
    tableName = "callback_reminders",
    indices = [
        Index(value = ["contactId"]),
        Index(value = ["phoneNumber"]),
        Index(value = ["reminderTime"]),
        Index(value = ["status"])
    ]
)
data class CallbackReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contactId: Long? = null,
    val phoneNumber: String,
    val callerName: String? = null,
    val reminderTime: Long,
    val status: ReminderStatus = ReminderStatus.PENDING
)
