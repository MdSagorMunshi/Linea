package com.ryanshelby.linea.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ryanshelby.linea.data.local.entities.CallNoteEntity
import com.ryanshelby.linea.data.local.entities.CallRecordingEntity
import com.ryanshelby.linea.data.local.entities.CallbackReminderEntity
import com.ryanshelby.linea.data.local.entities.DialerProfileEntity
import com.ryanshelby.linea.data.local.entities.ReminderStatus
import com.ryanshelby.linea.data.local.entities.VoicemailEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CallRecordingDao {
    @Query("SELECT * FROM call_recordings ORDER BY timestamp DESC")
    fun getAllRecordings(): Flow<List<CallRecordingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: CallRecordingEntity): Long

    @Query("UPDATE call_recordings SET isPinned = :isPinned WHERE id = :id")
    suspend fun updatePinned(id: Long, isPinned: Boolean)

    @Query("SELECT * FROM call_recordings WHERE id = :id LIMIT 1")
    suspend fun getRecordingById(id: Long): CallRecordingEntity?

    @Query("DELETE FROM call_recordings WHERE id = :id")
    suspend fun deleteRecordingById(id: Long)

    @Query("SELECT * FROM call_recordings WHERE contactId = :contactId OR phoneNumber = :number ORDER BY timestamp DESC")
    fun getRecordingsForContact(contactId: Long, number: String): Flow<List<CallRecordingEntity>>

    @Query("UPDATE call_recordings SET isEncrypted = 1 WHERE phoneNumber = :number OR contactId = :contactId")
    suspend fun markRecordingsAsEncrypted(contactId: Long, number: String)

    @Query("UPDATE call_recordings SET isEncrypted = 0 WHERE phoneNumber = :number OR contactId = :contactId")
    suspend fun markRecordingsAsDecrypted(contactId: Long, number: String)
}

@Dao
interface VoicemailDao {
    @Query("SELECT * FROM voicemails ORDER BY timestamp DESC")
    fun getAllVoicemails(): Flow<List<VoicemailEntity>>

    @Query("SELECT COUNT(*) FROM voicemails WHERE isRead = 0")
    fun getUnreadVoicemailCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoicemail(voicemail: VoicemailEntity): Long

    @Query("UPDATE voicemails SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("DELETE FROM voicemails WHERE id = :id")
    suspend fun deleteVoicemailById(id: Long)
}

@Dao
interface DialerProfileDao {
    @Query("SELECT * FROM dialer_profiles ORDER BY id ASC")
    fun getAllProfiles(): Flow<List<DialerProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: DialerProfileEntity): Long

    @Query("SELECT * FROM dialer_profiles WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultProfile(): DialerProfileEntity?

    @Query("SELECT * FROM dialer_profiles WHERE name = :name LIMIT 1")
    suspend fun getProfileByName(name: String): DialerProfileEntity?

    @Query("UPDATE dialer_profiles SET simSlot = :simSlot, ringtoneUri = :ringtoneUri WHERE name = :name")
    suspend fun updateProfileSettings(name: String, simSlot: Int, ringtoneUri: String?)
}

@Dao
interface CallNoteDao {
    @Query("SELECT * FROM call_notes WHERE contactId = :contactId OR phoneNumber = :phoneNumber ORDER BY timestamp DESC")
    fun getNotesForContact(contactId: Long?, phoneNumber: String): Flow<List<CallNoteEntity>>

    @Query("SELECT * FROM call_notes WHERE (contactId = :contactId OR phoneNumber = :phoneNumber) AND isPreCallNote = 1 ORDER BY timestamp DESC LIMIT 1")
    suspend fun getActivePreCallNote(contactId: Long?, phoneNumber: String): CallNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: CallNoteEntity): Long

    @Query("DELETE FROM call_notes WHERE id = :id")
    suspend fun deleteNote(id: Long)

    @Query("DELETE FROM call_notes WHERE (contactId = :contactId OR phoneNumber = :phoneNumber) AND isPreCallNote = 1")
    suspend fun clearPreCallNotes(contactId: Long?, phoneNumber: String)
}

@Dao
interface CallbackReminderDao {
    @Query("SELECT * FROM callback_reminders WHERE status = :status ORDER BY reminderTime ASC")
    fun getRemindersByStatus(status: ReminderStatus = ReminderStatus.PENDING): Flow<List<CallbackReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: CallbackReminderEntity): Long

    @Query("UPDATE callback_reminders SET status = :status WHERE id = :id")
    suspend fun updateReminderStatus(id: Long, status: ReminderStatus)
}
