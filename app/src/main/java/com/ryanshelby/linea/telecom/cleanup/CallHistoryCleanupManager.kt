package com.ryanshelby.linea.telecom.cleanup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ryanshelby.linea.data.local.dao.CallRecordDao
import com.ryanshelby.linea.data.local.dao.ContactDao
import com.ryanshelby.linea.data.preferences.LineaPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallHistoryCleanupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val callRecordDao: CallRecordDao,
    private val contactDao: ContactDao,
    private val preferences: LineaPreferences
) {
    suspend fun runCleanupNow(): Int = withContext(Dispatchers.IO) {
        val days = preferences.cleanupDays.first()
        if (days <= 0) return@withContext 0

        val cutoffTimestamp = System.currentTimeMillis() - (days * 86_400_000L)
        
        // Fetch favorite contact IDs to exempt
        val favorites = contactDao.getFavoriteContacts().first()
        val favoriteContactIds = favorites.map { it.id }.toSet()

        val allRecords = callRecordDao.getAllRecordsOnce()
        var deletedCount = 0

        for (record in allRecords) {
            // Only clean records older than cutoff and not private
            if (record.timestamp < cutoffTimestamp && !record.isPrivateContact) {
                // Never delete calls with notes
                if (!record.notes.isNullOrBlank()) continue

                // Check if number belongs to a favorite contact
                val numberEntity = contactDao.findNumberByNormalized(record.phoneNumber.replace(Regex("[^0-9+]"), ""))
                if (numberEntity != null && favoriteContactIds.contains(numberEntity.contactId)) {
                    // Exempt favorite
                    continue
                }

                callRecordDao.deleteCallRecordById(record.id)
                deletedCount++
            }
        }

        deletedCount
    }

    fun schedulePeriodicCleanup() {
        val request = PeriodicWorkRequestBuilder<CallHistoryCleanupWorker>(24, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "linea_history_cleanup",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}

class CallHistoryCleanupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // Entry point for periodic background cleanup
        return Result.success()
    }
}
