package com.ryanshelby.linea.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "contact_groups")
data class ContactGroupEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val isWorkHoursAllowed: Boolean = false
)

@Entity(
    tableName = "contact_group_members",
    primaryKeys = ["groupId", "contactId"],
    foreignKeys = [
        ForeignKey(
            entity = ContactGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["groupId"]),
        Index(value = ["contactId"])
    ]
)
data class ContactGroupMemberEntity(
    val groupId: Long,
    val contactId: Long
)
