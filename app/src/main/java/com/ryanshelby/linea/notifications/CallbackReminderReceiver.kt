package com.ryanshelby.linea.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ryanshelby.linea.MainActivity
import com.ryanshelby.linea.R
import com.ryanshelby.linea.data.local.dao.CallbackReminderDao
import com.ryanshelby.linea.data.local.entities.ReminderStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CallbackReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderDao: CallbackReminderDao

    companion object {
        const val CHANNEL_ID = "linea_reminders"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_PHONE_NUMBER = "extra_phone_number"
        const val EXTRA_CALLER_NAME = "extra_caller_name"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: return
        val callerName = intent.getStringExtra(EXTRA_CALLER_NAME)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Callback Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders to call back contacts"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Action: Call Number
        val callIntent = Intent(Intent.ACTION_CALL, Uri.fromParts("tel", phoneNumber, null)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val callPendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            callIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Main Tap: Open App
        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            (reminderId + 1000).toInt(),
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val displayName = callerName ?: phoneNumber
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Callback Reminder")
            .setContentText("Reminder to call back $displayName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_call,
                "Call $displayName",
                callPendingIntent
            )
            .build()

        notificationManager.notify(reminderId.toInt().coerceAtLeast(1001), notification)

        // Mark reminder as completed in DB
        if (reminderId > 0) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    reminderDao.updateReminderStatus(reminderId, ReminderStatus.COMPLETED)
                } catch (_: Exception) {}
            }
        }
    }
}
