package com.ryanshelby.linea.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "merge_history",
    indices = [
        Index(value = ["mergedContactId"]),
        Index(value = ["timestamp"])
    ]
)
data class MergeHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mergedContactId: Long,
    val mergedContactName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sourceContactsCount: Int,
    val sourceContactNames: String,
    val sourceContactsJson: String
)

