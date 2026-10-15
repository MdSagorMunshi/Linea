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

    private val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? android.telecom.TelecomManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val activeMissedCalls = java.util.concurrent.ConcurrentHashMap<Int, MissedCallData>()

    data class MissedCallData(
        val id: Int,
        val displayName: String,
        val phoneNumber: String,
        val simSlot: Int,
        val timestamp: Long
    )

    companion object {
        const val NOTIFICATION_ID_ONGOING_CALL = 1001
        const val NOTIFICATION_ID_MISSED_CALL_BASE = 2000
        const val NOTIFICATION_ID_MISSED_SUMMARY = 1002
        const val NOTIFICATION_ID_INCOMING_HEADS_UP = 1003
        const val GROUP_MISSED_CALLS = "com.ryanshelby.linea.MISSED_CALLS"
    }

    @SuppressLint("MissingPermission")
    fun showOngoingCallNotification(
        callerName: String?,
        phoneNumber: String,
        stateText: String,
        connectTimeMillis: Long = 0L,
        isMuted: Boolean = false,
        isSpeakerOn: Boolean = false,
        photoUri: String? = null,
        hasSecondaryCall: Boolean = false,
        secondaryCallerName: String? = null,
        canMerge: Boolean = false,
        isHeld: Boolean = false
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
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
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

        // 4. In-notification Quick Controls
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

        val contentText = if (hasSecondaryCall) {
            val secName = secondaryCallerName?.ifBlank { "Secondary Call" } ?: "Secondary Call"
            "Active • On Hold: $secName"
        } else {
            stateText
        }

        val notificationBuilder = NotificationCompat.Builder(context, LineaApp.CHANNEL_ONGOING_CALLS)
            .setSmallIcon(R.drawable.ic_stat_call)
            .setStyle(callStyle)
            .addPerson(callerPerson)
            .setContentTitle(displayName)
            .setContentText(contentText)
            .setContentIntent(contentPendingIntent)
            .setFullScreenIntent(contentPendingIntent, false)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(0xFF537193.toInt())
            .setOnlyAlertOnce(true)

        if (hasSecondaryCall) {
            // Swap action
            val swapIntent = Intent(context, CallActionReceiver::class.java).apply {
                action = CallActionReceiver.ACTION_SWAP_CALLS
                putExtra(CallActionReceiver.EXTRA_PHONE_NUMBER, phoneNumber)
            }
            val swapPendingIntent = PendingIntent.getBroadcast(
                context,
                23,
                swapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            notificationBuilder.addAction(
                R.drawable.ic_swap_calls,
                "Swap",
                swapPendingIntent
            )

            // Merge action (if supported)
            if (canMerge) {
                val mergeIntent = Intent(context, CallActionReceiver::class.java).apply {
                    action = CallActionReceiver.ACTION_MERGE_CALLS
                    putExtra(CallActionReceiver.EXTRA_PHONE_NUMBER, phoneNumber)
                }
                val mergePendingIntent = PendingIntent.getBroadcast(
                    context,
                    24,
                    mergeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                notificationBuilder.addAction(
                    R.drawable.ic_call_merge,
                    "Merge",
                    mergePendingIntent
                )
            }

            // Mute action
            notificationBuilder.addAction(
                R.drawable.ic_mic_off,
                if (isMuted) "Unmute" else "Mute",
                mutePendingIntent
            )
        } else if (isHeld) {
            // Resume action for single held call
            val unholdIntent = Intent(context, CallActionReceiver::class.java).apply {
                action = CallActionReceiver.ACTION_UNHOLD
                putExtra(CallActionReceiver.EXTRA_PHONE_NUMBER, phoneNumber)
            }
            val unholdPendingIntent = PendingIntent.getBroadcast(
                context,
                25,
                unholdIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            notificationBuilder.addAction(
                R.drawable.ic_stat_call,
                "Resume",
                unholdPendingIntent
            )
            notificationBuilder.addAction(
                R.drawable.ic_mic_off,
                if (isMuted) "Unmute" else "Mute",
                mutePendingIntent
            )
        } else {
            notificationBuilder.addAction(
                R.drawable.ic_mic_off,
                if (isMuted) "Unmute" else "Mute",
                mutePendingIntent
            )
            notificationBuilder.addAction(
                R.drawable.ic_volume_up,
                if (isSpeakerOn) "Phone" else "Speaker",
                speakerPendingIntent
            )
        }

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
    fun showMissedCallNotification(
        callerName: String?,
        phoneNumber: String,
        simSlot: Int = 0,
        timestamp: Long = System.currentTimeMillis()
    ) {
        val displayName = when {
            !callerName.isNullOrBlank() -> callerName
            phoneNumber.isNotBlank() -> phoneNumber
            else -> "Unknown Caller"
        }

        // Cancel stock Telecom missed-call notification so only LINEA's notification is shown
        try {
            telecomManager?.cancelMissedCallsNotification()
        } catch (_: Exception) {}

        val notifId = if (phoneNumber.isNotBlank()) {
            val hash = phoneNumber.hashCode()
            NOTIFICATION_ID_MISSED_CALL_BASE + (if (hash == Int.MIN_VALUE) 0 else kotlin.math.abs(hash) % 5000)
        } else {
            NOTIFICATION_ID_MISSED_CALL_BASE + (timestamp % 5000).toInt()
        }

        activeMissedCalls[notifId] = MissedCallData(
            id = notifId,
            displayName = displayName,
            phoneNumber = phoneNumber,
            simSlot = simSlot,
            timestamp = timestamp
        )

        // 1. Dedicated Call Back Action (prominent button using LINEA dialer)
        val callBackIntent = Intent(context, CallActionReceiver::class.java).apply {
            action = CallActionReceiver.ACTION_CALLBACK
            putExtra(CallActionReceiver.EXTRA_PHONE_NUMBER, phoneNumber)
            putExtra(CallActionReceiver.EXTRA_NOTIFICATION_ID, notifId)
        }
        val callBackPendingIntent = PendingIntent.getBroadcast(
            context,
            notifId,
            callBackIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Direct SMS Action
        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phoneNumber")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val smsPendingIntent = PendingIntent.getActivity(
            context,
            notifId + 10000,
            smsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Tapping opens History screen directly on the Missed filter
        val historyIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("NAV_DESTINATION", "HISTORY")
            putExtra("HISTORY_FILTER", "MISSED")
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notifId + 20000,
            historyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 4. Delete intent when user swipes notification away
        val deleteIntent = Intent(context, CallActionReceiver::class.java).apply {
            action = CallActionReceiver.ACTION_DISMISS_MISSED
            putExtra(CallActionReceiver.EXTRA_NOTIFICATION_ID, notifId)
        }
        val deletePendingIntent = PendingIntent.getBroadcast(
            context,
            notifId + 30000,
            deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val simLabel = "SIM ${simSlot + 1}"
        val subText = if (phoneNumber.isNotBlank() && displayName != phoneNumber) "$phoneNumber • $simLabel" else simLabel

        // Generic public version for lock screen privacy protection
        val publicNotification = NotificationCompat.Builder(context, LineaApp.CHANNEL_MISSED_CALLS)
            .setSmallIcon(R.drawable.ic_stat_call)
            .setContentTitle("Missed Call")
            .setContentText("1 new missed call")
            .setColor(0xFFD4834E.toInt())
            .build()

        val notification = NotificationCompat.Builder(context, LineaApp.CHANNEL_MISSED_CALLS)
            .setSmallIcon(R.drawable.ic_stat_call)
            .setColor(0xFFD4834E.toInt()) // Muted Rust design accent for missed calls
            .setContentTitle(displayName)
            .setContentText("Missed call • $simLabel")
            .setSubText(subText)
            .setWhen(timestamp)
            .setShowWhen(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicNotification)
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(deletePendingIntent)
            .setAutoCancel(true)
            .setGroup(GROUP_MISSED_CALLS)
            .addAction(0, "Call Back", callBackPendingIntent)
            .addAction(0, "Message", smsPendingIntent)
            .build()

        notificationManager.notify(notifId, notification)

        // If multiple missed calls exist, post/update group summary notification
        if (activeMissedCalls.size > 1) {
            val inboxStyle = NotificationCompat.InboxStyle()
                .setBigContentTitle("${activeMissedCalls.size} Missed Calls")
                .setSummaryText("Missed calls")

            activeMissedCalls.values.sortedByDescending { it.timestamp }.take(5).forEach { item ->
                inboxStyle.addLine("${item.displayName} • SIM ${item.simSlot + 1}")
            }

            val summaryNotification = NotificationCompat.Builder(context, LineaApp.CHANNEL_MISSED_CALLS)
                .setSmallIcon(R.drawable.ic_stat_call)
                .setColor(0xFFD4834E.toInt())
                .setContentTitle("Missed Calls")
                .setContentText("${activeMissedCalls.size} missed calls")
                .setStyle(inboxStyle)
                .setGroup(GROUP_MISSED_CALLS)
                .setGroupSummary(true)
                .setContentIntent(contentPendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
                .build()

            notificationManager.notify(NOTIFICATION_ID_MISSED_SUMMARY, summaryNotification)
        }

        // Suppress and clear stock Telecom system "Phone Calls" notification
        try {
            telecomManager?.cancelMissedCallsNotification()
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    telecomManager?.cancelMissedCallsNotification()
                } catch (_: Exception) {}
            }, 500)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    telecomManager?.cancelMissedCallsNotification()
                } catch (_: Exception) {}
            }, 1200)
        } catch (_: Exception) {}
    }

    fun dismissMissedCallNotification(id: Int) {
        activeMissedCalls.remove(id)
        notificationManager.cancel(id)
        if (activeMissedCalls.size <= 1) {
            notificationManager.cancel(NOTIFICATION_ID_MISSED_SUMMARY)
        }
        try {
            telecomManager?.cancelMissedCallsNotification()
        } catch (_: Exception) {}
    }

    fun dismissAllMissedCallNotifications() {
        for (id in activeMissedCalls.keys) {
            notificationManager.cancel(id)
        }
        activeMissedCalls.clear()
        notificationManager.cancel(NOTIFICATION_ID_MISSED_SUMMARY)
        try {
            telecomManager?.cancelMissedCallsNotification()
        } catch (_: Exception) {}
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
