package com.ryanshelby.linea

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.ryanshelby.linea.telecom.PhoneAccountManager
import com.ryanshelby.linea.telecom.screening.CallScreeningEngine
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class LineaApp : Application() {

    @Inject
    lateinit var phoneAccountManager: PhoneAccountManager

    @Inject
    lateinit var screeningEngine: CallScreeningEngine

    @Inject
    lateinit var cleanupManager: com.ryanshelby.linea.telecom.cleanup.CallHistoryCleanupManager

    companion object {
        const val CHANNEL_ONGOING_CALLS = "linea_ongoing_calls"
        const val CHANNEL_INCOMING_CALLS = "linea_incoming_calls"
        const val CHANNEL_MISSED_CALLS = "linea_missed_calls"
        const val CHANNEL_VOICEMAIL = "linea_voicemail"
        const val CHANNEL_REMINDERS = "linea_reminders"

        var isAppInForeground: Boolean = false
            private set
    }

    private var startedActivityCount = 0

    override fun onCreate() {
        super.onCreate()
        setupLifecycleTracking()
        createNotificationChannels()
        registerPhoneAccounts()
        purgeLegacyRules()
        cleanupManager.schedulePeriodicCleanup()
    }

    private fun purgeLegacyRules() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                screeningEngine.purgeLegacySeededRules()
            } catch (_: Exception) {}
        }
    }

    private fun setupLifecycleTracking() {
        registerActivityLifecycleCallbacks(object : android.app.Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) {}
            override fun onActivityStarted(activity: android.app.Activity) {
                startedActivityCount++
                isAppInForeground = startedActivityCount > 0
            }
            override fun onActivityResumed(activity: android.app.Activity) {}
            override fun onActivityPaused(activity: android.app.Activity) {}
            override fun onActivityStopped(activity: android.app.Activity) {
                startedActivityCount--
                isAppInForeground = startedActivityCount > 0
            }
            override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) {}
            override fun onActivityDestroyed(activity: android.app.Activity) {}
        })
    }

    private fun registerPhoneAccounts() {
        try {
            phoneAccountManager.registerPhoneAccounts()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java) ?: return

            val incomingCallChannel = NotificationChannel(
                CHANNEL_INCOMING_CALLS,
                "Incoming Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows heads-up notifications for incoming cellular calls"
                enableVibration(true)
            }

            val ongoingCallChannel = NotificationChannel(
                CHANNEL_ONGOING_CALLS,
                "Ongoing Calls",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows notifications for active cellular calls"
            }

            val missedCallChannel = NotificationChannel(
                CHANNEL_MISSED_CALLS,
                "Missed Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts you when a cellular call is missed"
                enableVibration(true)
            }

            val voicemailChannel = NotificationChannel(
                CHANNEL_VOICEMAIL,
                "Voicemail",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for new voicemails"
            }

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Callback Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders to call back contacts"
            }

            notificationManager.createNotificationChannels(
                listOf(incomingCallChannel, ongoingCallChannel, missedCallChannel, voicemailChannel, reminderChannel)
            )
        }
    }
}
