package com.ryanshelby.linea.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "call_recordings",
    indices = [
        Index(value = ["callRecordId"]),
        Index(value = ["phoneNumber"]),
        Index(value = ["timestamp"])
    ]
)
data class CallRecordingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val callRecordId: Long? = null,
    val contactId: Long? = null,
    val phoneNumber: String,
    val filePath: String,
    val durationMs: Long = 0,
    val fileSize: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isEncrypted: Boolean = false
)

@Entity(
    tableName = "voicemails",
    indices = [
        Index(value = ["sender"]),
        Index(value = ["timestamp"])
    ]
)
data class VoicemailEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String,
    val timestamp: Long = System.currentTimeMillis(),
    val duration: Long = 0,
    val audioFilePath: String? = null,
    val transcriptionText: String? = null,
    val isRead: Boolean = false
)
