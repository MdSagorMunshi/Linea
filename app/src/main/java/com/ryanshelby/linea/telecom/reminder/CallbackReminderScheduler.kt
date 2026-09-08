package com.ryanshelby.linea.telecom.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ryanshelby.linea.data.local.dao.CallbackReminderDao
import com.ryanshelby.linea.data.local.entities.CallbackReminderEntity
import com.ryanshelby.linea.data.local.entities.ReminderStatus
import com.ryanshelby.linea.notifications.CallbackReminderReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallbackReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reminderDao: CallbackReminderDao
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    suspend fun scheduleReminder(
        phoneNumber: String,
        callerName: String?,
        delayMs: Long,
        contactId: Long? = null
    ): Long = withContext(Dispatchers.IO) {
        val triggerAt = System.currentTimeMillis() + delayMs

        val reminderId = reminderDao.insertReminder(
            CallbackReminderEntity(
                contactId = contactId,
                phoneNumber = phoneNumber,
                callerName = callerName,
                reminderTime = triggerAt,
                status = ReminderStatus.PENDING
            )
        )

        val intent = Intent(context, CallbackReminderReceiver::class.java).apply {
            putExtra(CallbackReminderReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(CallbackReminderReceiver.EXTRA_PHONE_NUMBER, phoneNumber)
            putExtra(CallbackReminderReceiver.EXTRA_CALLER_NAME, callerName)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } catch (_: SecurityException) {
            // Fallback for missing exact alarm permission
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }

        reminderId
    }
}
