package com.ryanshelby.linea.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ryanshelby.linea.data.local.converters.Converters
import com.ryanshelby.linea.data.local.dao.BlockedNumberDao
import com.ryanshelby.linea.data.local.dao.CallNoteDao
import com.ryanshelby.linea.data.local.dao.CallRecordDao
import com.ryanshelby.linea.data.local.dao.CallRecordingDao
import com.ryanshelby.linea.data.local.dao.CallRuleDao
import com.ryanshelby.linea.data.local.dao.CallbackReminderDao
import com.ryanshelby.linea.data.local.dao.ContactDao
import com.ryanshelby.linea.data.local.dao.DialerProfileDao
import com.ryanshelby.linea.data.local.dao.VoicemailDao
import com.ryanshelby.linea.data.local.entities.BlockedCallLogEntity
import com.ryanshelby.linea.data.local.entities.BlockedNumberEntity
import com.ryanshelby.linea.data.local.entities.CallNoteEntity
import com.ryanshelby.linea.data.local.entities.CallRecordEntity
import com.ryanshelby.linea.data.local.entities.CallRecordingEntity
import com.ryanshelby.linea.data.local.entities.CallRuleEntity
import com.ryanshelby.linea.data.local.entities.CallbackReminderEntity
import com.ryanshelby.linea.data.local.entities.ContactEmailEntity
import com.ryanshelby.linea.data.local.entities.ContactEntity
import com.ryanshelby.linea.data.local.entities.ContactGroupEntity
import com.ryanshelby.linea.data.local.entities.ContactGroupMemberEntity
import com.ryanshelby.linea.data.local.entities.ContactNumberEntity
import com.ryanshelby.linea.data.local.entities.DialerProfileEntity
import com.ryanshelby.linea.data.local.entities.PinnedContactEntity
import com.ryanshelby.linea.data.local.entities.VoicemailEntity

@Database(
    entities = [
        CallRecordEntity::class,
        ContactEntity::class,
        ContactNumberEntity::class,
        ContactEmailEntity::class,
        ContactGroupEntity::class,
        ContactGroupMemberEntity::class,
        BlockedNumberEntity::class,
        BlockedCallLogEntity::class,
        CallRuleEntity::class,
        CallRecordingEntity::class,
        VoicemailEntity::class,
        DialerProfileEntity::class,
        PinnedContactEntity::class,
        CallNoteEntity::class,
        CallbackReminderEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LineaDatabase : RoomDatabase() {

    abstract fun callRecordDao(): CallRecordDao
    abstract fun contactDao(): ContactDao
    abstract fun blockedNumberDao(): BlockedNumberDao
    abstract fun callRuleDao(): CallRuleDao
    abstract fun callRecordingDao(): CallRecordingDao
    abstract fun voicemailDao(): VoicemailDao
    abstract fun dialerProfileDao(): DialerProfileDao
    abstract fun callNoteDao(): CallNoteDao
    abstract fun callbackReminderDao(): CallbackReminderDao
}
