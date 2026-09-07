package com.ryanshelby.linea.di

import android.content.Context
import androidx.room.Room
import com.ryanshelby.linea.data.local.LineaDatabase
import com.ryanshelby.linea.data.local.dao.BlockedNumberDao
import com.ryanshelby.linea.data.local.dao.CallNoteDao
import com.ryanshelby.linea.data.local.dao.CallRecordDao
import com.ryanshelby.linea.data.local.dao.CallRecordingDao
import com.ryanshelby.linea.data.local.dao.CallRuleDao
import com.ryanshelby.linea.data.local.dao.CallbackReminderDao
import com.ryanshelby.linea.data.local.dao.ContactDao
import com.ryanshelby.linea.data.local.dao.DialerProfileDao
import com.ryanshelby.linea.data.local.dao.VoicemailDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LineaDatabase {
        return Room.databaseBuilder(
            context,
            LineaDatabase::class.java,
            "linea.db"
        ).fallbackToDestructiveMigration(true).build()
    }

    @Provides
    fun provideCallRecordDao(db: LineaDatabase): CallRecordDao = db.callRecordDao()

    @Provides
    fun provideContactDao(db: LineaDatabase): ContactDao = db.contactDao()

    @Provides
    fun provideBlockedNumberDao(db: LineaDatabase): BlockedNumberDao = db.blockedNumberDao()

    @Provides
    fun provideCallRuleDao(db: LineaDatabase): CallRuleDao = db.callRuleDao()

    @Provides
    fun provideCallRecordingDao(db: LineaDatabase): CallRecordingDao = db.callRecordingDao()

    @Provides
    fun provideVoicemailDao(db: LineaDatabase): VoicemailDao = db.voicemailDao()

    @Provides
    fun provideDialerProfileDao(db: LineaDatabase): DialerProfileDao = db.dialerProfileDao()

    @Provides
    fun provideCallNoteDao(db: LineaDatabase): CallNoteDao = db.callNoteDao()

    @Provides
    fun provideCallbackReminderDao(db: LineaDatabase): CallbackReminderDao = db.callbackReminderDao()
}
