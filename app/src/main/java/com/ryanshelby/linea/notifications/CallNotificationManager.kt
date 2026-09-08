package com.ryanshelby.linea.notifications

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
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
    fun showOngoingCallNotification(callerName: String?, phoneNumber: String, stateText: String) {
        val fullScreenIntent = Intent(context, InCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, LineaApp.CHANNEL_ONGOING_CALLS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(callerName ?: phoneNumber)
            .setContentText(stateText)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .build()

        notificationManager.notify(NOTIFICATION_ID_ONGOING_CALL, notification)
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
    fun showIncomingCallHeadsUpNotification(callerName: String?, phoneNumber: String) {
        val answerIntent = Intent(context, CallActionReceiver::class.java).apply {
            action = CallActionReceiver.ACTION_ANSWER
            putExtra(CallActionReceiver.EXTRA_PHONE_NUMBER, phoneNumber)
        }
        val answerPendingIntent = PendingIntent.getBroadcast(
            context,
            10,
            answerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

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

        val ignoreIntent = Intent(context, CallActionReceiver::class.java).apply {
            action = CallActionReceiver.ACTION_IGNORE
            putExtra(CallActionReceiver.EXTRA_PHONE_NUMBER, phoneNumber)
        }
        val ignorePendingIntent = PendingIntent.getBroadcast(
            context,
            12,
            ignoreIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

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

        val notification = NotificationCompat.Builder(context, LineaApp.CHANNEL_INCOMING_CALLS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(callerName ?: phoneNumber)
            .setContentText("Incoming Call")
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .addAction(0, "Answer", answerPendingIntent)
            .addAction(0, "Decline", rejectPendingIntent)
            .addAction(0, "Message", messagePendingIntent)
            .addAction(0, "Ignore", ignorePendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID_INCOMING_HEADS_UP, notification)
    }

    fun dismissIncomingCallHeadsUpNotification() {
        notificationManager.cancel(NOTIFICATION_ID_INCOMING_HEADS_UP)
    }
}
