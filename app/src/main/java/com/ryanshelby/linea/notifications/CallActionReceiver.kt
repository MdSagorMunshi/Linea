package com.ryanshelby.linea.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.ryanshelby.linea.telecom.CallManager
import com.ryanshelby.linea.ui.incall.InCallActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CallActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var callManager: CallManager

    @Inject
    lateinit var notificationManager: CallNotificationManager

    companion object {
        const val ACTION_ANSWER = "com.ryanshelby.linea.ACTION_ANSWER"
        const val ACTION_REJECT = "com.ryanshelby.linea.ACTION_REJECT"
        const val ACTION_HANG_UP = "com.ryanshelby.linea.ACTION_HANG_UP"
        const val ACTION_TOGGLE_MUTE = "com.ryanshelby.linea.ACTION_TOGGLE_MUTE"
        const val ACTION_TOGGLE_SPEAKER = "com.ryanshelby.linea.ACTION_TOGGLE_SPEAKER"
        const val ACTION_IGNORE = "com.ryanshelby.linea.ACTION_IGNORE"
        const val ACTION_MESSAGE = "com.ryanshelby.linea.ACTION_MESSAGE"
        const val EXTRA_PHONE_NUMBER = "extra_phone_number"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: ""

        when (intent.action) {
            ACTION_ANSWER -> {
                callManager.answerCall()
                callManager.dismissFloatingCall()
                notificationManager.dismissIncomingCallHeadsUpNotification()

                // Launch in-call screen for active conversation
                val activityIntent = Intent(context, InCallActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                context.startActivity(activityIntent)
            }
            ACTION_REJECT -> {
                callManager.rejectCall()
                callManager.dismissFloatingCall()
                notificationManager.dismissIncomingCallHeadsUpNotification()
            }
            ACTION_HANG_UP -> {
                callManager.disconnectCall()
                notificationManager.dismissOngoingCallNotification()
            }
            ACTION_TOGGLE_MUTE -> {
                callManager.toggleMute()
            }
            ACTION_TOGGLE_SPEAKER -> {
                callManager.toggleSpeaker()
            }
            ACTION_IGNORE -> {
                // Silences ringer but leaves call ringing in background
                callManager.silenceRinger()
                callManager.dismissFloatingCall()
                notificationManager.dismissIncomingCallHeadsUpNotification()
            }
            ACTION_MESSAGE -> {
                callManager.rejectCall(rejectWithMessage = true, textMessage = "I am busy, will call you back.")
                callManager.dismissFloatingCall()
                notificationManager.dismissIncomingCallHeadsUpNotification()

                if (phoneNumber.isNotBlank()) {
                    val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("smsto:$phoneNumber")
                        putExtra("sms_body", "I am currently busy. I will call you back shortly.")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(smsIntent)
                }
            }
        }
    }
}
