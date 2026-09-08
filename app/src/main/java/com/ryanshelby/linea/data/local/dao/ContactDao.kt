package com.ryanshelby.linea.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ryanshelby.linea.data.local.entities.ContactEmailEntity
import com.ryanshelby.linea.data.local.entities.ContactEntity
import com.ryanshelby.linea.data.local.entities.ContactGroupEntity
import com.ryanshelby.linea.data.local.entities.ContactGroupMemberEntity
import com.ryanshelby.linea.data.local.entities.ContactNumberEntity
import kotlinx.coroutines.flow.Flow

data class ContactWithDetails(
    val contact: ContactEntity,
    val numbers: List<ContactNumberEntity>,
    val emails: List<ContactEmailEntity>
)

@Dao
interface ContactDao {

    @Query("SELECT * FROM contacts WHERE isPrivate = 0 ORDER BY displayName ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE isFavorite = 1 AND isPrivate = 0 ORDER BY displayName ASC")
    fun getFavoriteContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE isPinned = 1 AND isPrivate = 0 ORDER BY displayName ASC")
    fun getPinnedContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE id = :id LIMIT 1")
    suspend fun getContactById(id: Long): ContactEntity?

    @Query("SELECT * FROM contact_numbers WHERE contactId = :contactId")
    fun getNumbersForContact(contactId: Long): Flow<List<ContactNumberEntity>>

    @Query("SELECT * FROM contact_numbers WHERE normalizedNumber = :normalizedNumber LIMIT 1")
    suspend fun findNumberByNormalized(normalizedNumber: String): ContactNumberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNumbers(numbers: List<ContactNumberEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmails(emails: List<ContactEmailEntity>)

    @Update
    suspend fun updateContact(contact: ContactEntity)

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteContactById(id: Long)

    @Query("SELECT * FROM contact_groups ORDER BY name ASC")
    fun getAllGroups(): Flow<List<ContactGroupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: ContactGroupEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addMemberToGroup(member: ContactGroupMemberEntity)

    @Query("SELECT COUNT(*) FROM contact_group_members WHERE groupId = :groupId AND contactId = :contactId")
    suspend fun isContactInGroup(groupId: Long, contactId: Long): Int

    @Query("SELECT * FROM contacts ORDER BY displayName ASC")
    fun getAllContactsIncludingPrivate(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE isPrivate = 1 ORDER BY displayName ASC")
    fun getPrivateContacts(): Flow<List<ContactEntity>>

    @Query("UPDATE contacts SET isPrivate = :isPrivate WHERE id = :id")
    suspend fun setContactPrivate(id: Long, isPrivate: Boolean)

    @Query("UPDATE contacts SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setContactFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE contacts SET isPinned = :isPinned WHERE id = :id")
    suspend fun setContactPinned(id: Long, isPinned: Boolean)

    @Query("UPDATE contacts SET preferredSimSlot = :simSlot, allowDuringRestrictedHours = :allowRestricted, alwaysRing = :alwaysRing, customRingtoneUri = :ringtoneUri WHERE id = :id")
    suspend fun updatePerContactSettings(
        id: Long,
        simSlot: Int?,
        allowRestricted: Boolean,
        alwaysRing: Boolean,
        ringtoneUri: String?
    )
}
