package com.ryanshelby.linea.notifications

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import com.ryanshelby.linea.LineaApp
import com.ryanshelby.linea.MainActivity
import com.ryanshelby.linea.R
import com.ryanshelby.linea.ui.incall.InCallActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val NOTIFICATION_ID_ONGOING_CALL = 1001
        const val NOTIFICATION_ID_MISSED_CALL = 1002
        const val NOTIFICATION_ID_INCOMING_HEADS_UP = 1003
    }

    @SuppressLint("MissingPermission")
    fun showOngoingCallNotification(
        callerName: String?,
        phoneNumber: String,
        stateText: String,
        connectTimeMillis: Long = 0L,
        isMuted: Boolean = false,
        isSpeakerOn: Boolean = false,
        photoUri: String? = null
    ) {
        val displayName = when {
            !callerName.isNullOrBlank() -> callerName
            phoneNumber.isNotBlank() -> phoneNumber
            else -> "Unknown Caller"
        }
        val personBuilder = Person.Builder()
            .setName(displayName)
            .setImportant(true)

        if (phoneNumber.isNotBlank()) {
            personBuilder.setUri("tel:$phoneNumber")
        }

        if (!photoUri.isNullOrBlank()) {
            try {
                personBuilder.setIcon(IconCompat.createWithContentUri(photoUri))
            } catch (_: Exception) {}
        }
        val callerPerson = personBuilder.build()

        // 1. Dedicated Telecom Hang Up Action (prominent red button)
        val hangUpIntent = Intent(context, CallActionReceiver::class.java).apply {
            action = CallActionReceiver.ACTION_HANG_UP
            putExtra(CallActionReceiver.EXTRA_PHONE_NUMBER, phoneNumber)
        }
        val hangUpPendingIntent = PendingIntent.getBroadcast(
            context,
            20,
            hangUpIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Full Screen / Tap Intent: returns to InCallActivity
        val fullScreenIntent = Intent(context, InCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Native Android Telecom CallStyle for ongoing calls
        val callStyle = NotificationCompat.CallStyle.forOngoingCall(
            callerPerson,
            hangUpPendingIntent
        )

        // 4. In-notification Quick Controls: Mute & Speaker
        val muteIntent = Intent(context, CallActionReceiver::class.java).apply {
            action = CallActionReceiver.ACTION_TOGGLE_MUTE
            putExtra(CallActionReceiver.EXTRA_PHONE_NUMBER, phoneNumber)
        }
        val mutePendingIntent = PendingIntent.getBroadcast(
            context,
            21,
            muteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val speakerIntent = Intent(context, CallActionReceiver::class.java).apply {
            action = CallActionReceiver.ACTION_TOGGLE_SPEAKER
            putExtra(CallActionReceiver.EXTRA_PHONE_NUMBER, phoneNumber)
        }
        val speakerPendingIntent = PendingIntent.getBroadcast(
            context,
            22,
            speakerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, LineaApp.CHANNEL_ONGOING_CALLS)
            .setSmallIcon(R.drawable.ic_stat_call)
            .setStyle(callStyle)
            .addPerson(callerPerson)
            .setContentTitle(displayName)
            .setContentText(stateText)
            .setContentIntent(contentPendingIntent)
            .setFullScreenIntent(contentPendingIntent, false)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(0xFF537193.toInt())
            .setOnlyAlertOnce(true)
            .addAction(
                R.drawable.ic_mic_off,
                if (isMuted) "Unmute" else "Mute",
                mutePendingIntent
            )
            .addAction(
                R.drawable.ic_volume_up,
                if (isSpeakerOn) "Earpiece" else "Speaker",
                speakerPendingIntent
            )

        if (connectTimeMillis > 0L) {
            notificationBuilder.setWhen(connectTimeMillis)
            notificationBuilder.setUsesChronometer(true)
            notificationBuilder.setShowWhen(true)
        }

        notificationManager.notify(NOTIFICATION_ID_ONGOING_CALL, notificationBuilder.build())
    }

    fun dismissOngoingCallNotification() {
        notificationManager.cancel(NOTIFICATION_ID_ONGOING_CALL)
    }

    @SuppressLint("MissingPermission")
    fun showMissedCallNotification(callerName: String?, phoneNumber: String) {
        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phoneNumber")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val dialPendingIntent = PendingIntent.getActivity(
            context,
            1,
            dialIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phoneNumber")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val smsPendingIntent = PendingIntent.getActivity(
            context,
            2,
            smsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, LineaApp.CHANNEL_MISSED_CALLS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Missed Call")
            .setContentText(callerName ?: phoneNumber)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
            .setAutoCancel(true)
            .addAction(0, "Call Back", dialPendingIntent)
            .addAction(0, "Message", smsPendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID_MISSED_CALL, notification)
    }

    @SuppressLint("MissingPermission")
    fun showIncomingCallHeadsUpNotification(
        callerName: String?,
        phoneNumber: String,
        photoUri: String? = null
    ) {
        val displayName = when {
            !callerName.isNullOrBlank() -> callerName
            phoneNumber.isNotBlank() -> phoneNumber
            else -> "Unknown Caller"
        }
        val personBuilder = Person.Builder()
            .setName(displayName)
            .setImportant(true)

        if (phoneNumber.isNotBlank()) {
            personBuilder.setUri("tel:$phoneNumber")
        }

        if (!photoUri.isNullOrBlank()) {
            try {
                personBuilder.setIcon(IconCompat.createWithContentUri(photoUri))
            } catch (_: Exception) {}
        }
        val callerPerson = personBuilder.build()

        // 1. Answer Action: launches InCallActivity directly and answers the call
        val answerIntent = Intent(context, InCallActivity::class.java).apply {
            action = InCallActivity.ACTION_ANSWER_CALL
            putExtra(CallActionReceiver.EXTRA_PHONE_NUMBER, phoneNumber)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val answerPendingIntent = PendingIntent.getActivity(
            context,
            10,
            answerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Decline Action: broadcasts to CallActionReceiver to reject call
        val rejectIntent = Intent(context, CallActionReceiver::class.java).apply {
            action = CallActionReceiver.ACTION_REJECT
            putExtra(CallActionReceiver.EXTRA_PHONE_NUMBER, phoneNumber)
        }
        val rejectPendingIntent = PendingIntent.getBroadcast(
            context,
            11,
            rejectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Quick Message Action
        val messageIntent = Intent(context, CallActionReceiver::class.java).apply {
            action = CallActionReceiver.ACTION_MESSAGE
            putExtra(CallActionReceiver.EXTRA_PHONE_NUMBER, phoneNumber)
        }
        val messagePendingIntent = PendingIntent.getBroadcast(
            context,
            13,
            messageIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 4. Full Screen Intent for lockscreen / screen off
        val fullScreenIntent = Intent(context, InCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            5,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Native Android CallStyle for incoming calls (dedicated Telecom path like Google Phone)
        val callStyle = NotificationCompat.CallStyle.forIncomingCall(
            callerPerson,
            rejectPendingIntent,
            answerPendingIntent
        )

        val notification = NotificationCompat.Builder(context, LineaApp.CHANNEL_INCOMING_CALLS)
            .setSmallIcon(R.drawable.ic_stat_call)
            .setStyle(callStyle)
            .addPerson(callerPerson)
            .setContentTitle(displayName)
            .setContentText("Incoming Call")
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(0, "Message", messagePendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID_INCOMING_HEADS_UP, notification)
    }

    fun dismissIncomingCallHeadsUpNotification() {
        notificationManager.cancel(NOTIFICATION_ID_INCOMING_HEADS_UP)
    }
}
