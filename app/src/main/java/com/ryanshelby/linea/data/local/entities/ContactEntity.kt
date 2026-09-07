package com.ryanshelby.linea.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "contacts",
    indices = [
        Index(value = ["displayName"]),
        Index(value = ["androidContactId"]),
        Index(value = ["isFavorite"]),
        Index(value = ["isPinned"]),
        Index(value = ["isPrivate"])
    ]
)
data class ContactEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val androidContactId: Long? = null,
    val lookupKey: String? = null,
    val displayName: String,
    val photoUri: String? = null,
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val isPrivate: Boolean = false,
    val company: String? = null,
    val jobTitle: String? = null,
    val nickname: String? = null,
    val birthday: String? = null,
    val website: String? = null,
    val address: String? = null,
    val notes: String? = null,
    val customRingtoneUri: String? = null,
    val customVibrationPattern: String? = null,
    val preferredSimSlot: Int? = null,
    val allowDuringRestrictedHours: Boolean = false,
    val alwaysRing: Boolean = false,
    val lastInteractionTime: Long? = null,
    val lastInteractionType: String? = null
)
