package com.ryanshelby.linea.di

import android.content.Context
import android.media.AudioManager
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TelecomModule {

    @Provides
    @Singleton
    fun provideTelecomManager(@ApplicationContext context: Context): TelecomManager {
        return context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
    }

    @Provides
    @Singleton
    fun provideSubscriptionManager(@ApplicationContext context: Context): SubscriptionManager {
        return context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
    }

    @Provides
    @Singleton
    fun provideAudioManager(@ApplicationContext context: Context): AudioManager {
        return context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
}
