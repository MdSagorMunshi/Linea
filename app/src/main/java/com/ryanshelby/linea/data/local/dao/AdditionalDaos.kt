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

    @Query("DELETE FROM call_recordings WHERE id = :id")
    suspend fun deleteRecordingById(id: Long)
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
}

@Dao
interface CallNoteDao {
    @Query("SELECT * FROM call_notes WHERE contactId = :contactId OR phoneNumber = :phoneNumber ORDER BY timestamp DESC")
    fun getNotesForContact(contactId: Long?, phoneNumber: String): Flow<List<CallNoteEntity>>

    @Query("SELECT * FROM call_notes WHERE (contactId = :contactId OR phoneNumber = :phoneNumber) AND isPreCallNote = 1 ORDER BY timestamp DESC LIMIT 1")
    suspend fun getActivePreCallNote(contactId: Long?, phoneNumber: String): CallNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: CallNoteEntity): Long
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
