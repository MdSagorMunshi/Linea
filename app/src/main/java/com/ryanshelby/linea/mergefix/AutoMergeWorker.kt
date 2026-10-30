package com.ryanshelby.linea.mergefix

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ryanshelby.linea.R
import com.ryanshelby.linea.data.local.dao.ContactDao
import com.ryanshelby.linea.data.local.dao.MergeHistoryDao
import com.ryanshelby.linea.telecom.screening.SimCountryDetector
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class AutoMergeWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AutoMergeEntryPoint {
        fun contactDao(): ContactDao
        fun mergeHistoryDao(): MergeHistoryDao
        fun mergeExecutor(): ContactMergeExecutor
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            AutoMergeEntryPoint::class.java
        )
        val contactDao = entryPoint.contactDao()
        val mergeHistoryDao = entryPoint.mergeHistoryDao()
        val mergeExecutor = entryPoint.mergeExecutor()

        try {
            // Load all public contacts (vault contacts strictly excluded)
            val publicContacts = contactDao.getAllPublicContactsOnce()
            if (publicContacts.size < 2) return@withContext Result.success()

            val defaultIso = SimCountryDetector.detectSimCountryIso(applicationContext)
            val normalizedList = publicContacts.map { entity ->
                val numbers = contactDao.getNumbersForContact(entity.id).first()
                val emails = contactDao.getEmailsForContact(entity.id).first()
                val contactIso = if (entity.preferredSimSlot != null) {
                    SimCountryDetector.getCountryIsoForSimSlot(applicationContext, entity.preferredSimSlot) ?: defaultIso
                } else {
                    defaultIso
                }
                NormalizedContact(
                    contact = entity,
                    normalizedDisplayName = ContactNormalizer.normalizeDisplayName(entity.displayName),
                    numbers = numbers,
                    normalizedNumbers = numbers.map { ContactNormalizer.normalizePhoneNumber(it.number, contactIso) },
                    emails = emails,
                    normalizedEmails = emails.map { ContactNormalizer.normalizeEmail(it.email) }
                )
            }

            // Find duplicate groups
            val groups = ContactGroupingEngine.findMergeGroups(normalizedList)
            // Hard rule: For HIGH confidence groups only, merge silently inside WorkManager
            val highGroups = groups.filter { it.confidence == ConfidenceTier.HIGH }
            if (highGroups.isEmpty()) return@withContext Result.success()

            var totalContactsMerged = 0
            var lastHistoryId: Long = -1L

            for (group in highGroups) {
                mergeExecutor.executeMerge(group)
                totalContactsMerged += group.contacts.size
                val latestHistory = mergeHistoryDao.getLatestMergeHistory()
                if (latestHistory != null) {
                    lastHistoryId = latestHistory.id
                }
            }

            if (totalContactsMerged > 0) {
                showAutoMergeNotification(
                    context = applicationContext,
                    mergedCount = totalContactsMerged,
                    historyId = lastHistoryId
                )
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private fun showAutoMergeNotification(context: Context, mergedCount: Int, historyId: Long) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channelId = "linea_merge_fix"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Contact Merge & Fix",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for automatic contact duplicate merges"
            }
            nm.createNotificationChannel(channel)
        }

        val notificationId = 9100

        val undoIntent = Intent(context, MergeUndoReceiver::class.java).apply {
            action = MergeUndoReceiver.ACTION_UNDO_MERGE
            putExtra(MergeUndoReceiver.EXTRA_HISTORY_ID, historyId)
            putExtra(MergeUndoReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val undoPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            undoIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_call_merge)
            .setContentTitle("Contacts Merged")
            .setContentText("$mergedCount contacts merged automatically")
            .addAction(0, "Undo (30s)", undoPendingIntent)
            .setAutoCancel(true)
            .setTimeoutAfter(30_000L) // 30-second undo window
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        nm.notify(notificationId, notification)
    }

    companion object {
        const val WORK_NAME_AUTO_MERGE = "linea_contact_auto_merge"
    }
}

