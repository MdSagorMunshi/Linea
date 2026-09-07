package com.ryanshelby.linea.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "contact_numbers",
    foreignKeys = [
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["contactId"]),
        Index(value = ["number"]),
        Index(value = ["normalizedNumber"])
    ]
)
data class ContactNumberEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contactId: Long,
    val number: String,
    val normalizedNumber: String,
    val label: String = "Mobile", // Mobile, Home, Work, Main, Custom
    val isPrimary: Boolean = false
)
