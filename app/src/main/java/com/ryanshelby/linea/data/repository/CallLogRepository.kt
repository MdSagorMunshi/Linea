package com.ryanshelby.linea.data.repository

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.provider.CallLog
import com.ryanshelby.linea.data.local.dao.CallRecordDao
import com.ryanshelby.linea.data.local.entities.CallDirectionType
import com.ryanshelby.linea.data.local.entities.CallRecordEntity
import com.ryanshelby.linea.telecom.ContactLookupHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallLogRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val callRecordDao: CallRecordDao,
    private val contactLookupHelper: ContactLookupHelper
) {

    private fun normalizeNumber(phoneNumber: String): String {
        val digits = phoneNumber.filter { it.isDigit() }
        return when {
            digits.length >= 10 -> digits.takeLast(10)
            digits.length >= 7 -> digits.takeLast(7)
            digits.isNotEmpty() -> digits
            else -> phoneNumber.trim()
        }
    }

    private suspend fun hasRecordNearTimestamp(
        phoneNumber: String,
        timestamp: Long,
        durationSeconds: Long = 0L
    ): Boolean {
        val normTarget = normalizeNumber(phoneNumber)
        val window = kotlin.math.max(45_000L, durationSeconds * 1000L + 20_000L)
        val records = callRecordDao.getRecordsInTimeWindow(timestamp - window, timestamp + window)
        return records.any { existing ->
            val normExisting = normalizeNumber(existing.phoneNumber)
            val numberMatches = (normTarget.isNotEmpty() && normExisting.isNotEmpty() &&
                    (normTarget == normExisting || normTarget.endsWith(normExisting) || normExisting.endsWith(normTarget)))
                    || (existing.phoneNumber == phoneNumber)
            val timeDiff = kotlin.math.abs(existing.timestamp - timestamp)
            val maxDur = kotlin.math.max(existing.durationSeconds, durationSeconds) * 1000L
            numberMatches && timeDiff <= kotlin.math.max(45_000L, maxDur + 20_000L)
        }
    }

    private fun checkSystemCallLogExists(number: String, timestamp: Long, durationSeconds: Long): Boolean {
        val norm = normalizeNumber(number)
        val window = kotlin.math.max(45_000L, durationSeconds * 1000L + 20_000L)
        val minDate = timestamp - window
        val maxDate = timestamp + window
        try {
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls._ID, CallLog.Calls.NUMBER, CallLog.Calls.DATE),
                "${CallLog.Calls.DATE} BETWEEN ? AND ?",
                arrayOf(minDate.toString(), maxDate.toString()),
                null
            )
            cursor?.use {
                val numIdx = it.getColumnIndex(CallLog.Calls.NUMBER)
                while (it.moveToNext()) {
                    val sysNum = if (numIdx >= 0) it.getString(numIdx) ?: "" else ""
                    val sysNorm = normalizeNumber(sysNum)
                    if (sysNum == number || (norm.isNotEmpty() && sysNorm.isNotEmpty() &&
                            (norm == sysNorm || norm.endsWith(sysNorm) || sysNorm.endsWith(norm)))) {
                        return true
                    }
                }
            }
        } catch (_: Exception) {}
        return false
    }

    @SuppressLint("MissingPermission")
    suspend fun logCall(
        phoneNumber: String,
        formattedNumber: String,
        callerName: String?,
        photoUri: String?,
        direction: CallDirectionType,
        timestamp: Long,
        durationSeconds: Long,
        simSlot: Int = 0,
        simDisplayName: String? = null,
        networkType: String = "VoLTE"
    ) = withContext(Dispatchers.IO) {
        if (phoneNumber.isBlank()) return@withContext

        val lookup = contactLookupHelper.lookupContact(phoneNumber)
        val isPrivate = lookup.isPrivate

        val resolvedName = if (!callerName.isNullOrBlank()) {
            callerName
        } else {
            lookup.displayName
        }

        val resolvedPhoto = if (!photoUri.isNullOrBlank()) {
            photoUri
        } else {
            lookup.photoUri
        }

        // Check if this call was already logged to prevent duplicates
        if (!hasRecordNearTimestamp(phoneNumber, timestamp, durationSeconds)) {
            // 1. Insert into LINEA local Room database
            val record = CallRecordEntity(
                phoneNumber = phoneNumber,
                formattedNumber = formattedNumber,
                callerName = resolvedName,
                photoUri = resolvedPhoto,
                callType = direction,
                timestamp = timestamp,
                durationSeconds = durationSeconds,
                simSlot = simSlot,
                simDisplayName = simDisplayName,
                networkType = networkType,
                sessionGroupId = "session_${phoneNumber.filter { it.isDigit() }}",
                isPrivateContact = isPrivate
            )
            callRecordDao.insertCallRecord(record)
        }

        // 2. Insert into Android System CallLog Provider ONLY if NOT a private contact
        // and not already logged by Android Telecom subsystem
        if (!isPrivate) {
            try {
                if (!checkSystemCallLogExists(phoneNumber, timestamp, durationSeconds)) {
                    val systemCallType = when (direction) {
                        CallDirectionType.INCOMING -> CallLog.Calls.INCOMING_TYPE
                        CallDirectionType.OUTGOING -> CallLog.Calls.OUTGOING_TYPE
                        CallDirectionType.MISSED -> CallLog.Calls.MISSED_TYPE
                        CallDirectionType.REJECTED -> CallLog.Calls.REJECTED_TYPE
                        CallDirectionType.BLOCKED -> CallLog.Calls.BLOCKED_TYPE
                    }

                    val values = ContentValues().apply {
                        put(CallLog.Calls.NUMBER, phoneNumber)
                        put(CallLog.Calls.DATE, timestamp)
                        put(CallLog.Calls.DURATION, durationSeconds)
                        put(CallLog.Calls.TYPE, systemCallType)
                        put(CallLog.Calls.NEW, if (direction == CallDirectionType.MISSED) 1 else 0)
                        if (resolvedName != null) {
                            put(CallLog.Calls.CACHED_NAME, resolvedName)
                        }
                    }
                    context.contentResolver.insert(CallLog.Calls.CONTENT_URI, values)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getAllCallRecords(): kotlinx.coroutines.flow.Flow<List<CallRecordEntity>> {
        return callRecordDao.getAllCallRecords()
    }

    suspend fun deleteCall(id: Long) = withContext(Dispatchers.IO) {
        callRecordDao.deleteCallRecordById(id)
    }

    suspend fun deleteCallGroup(ids: List<Long>) = withContext(Dispatchers.IO) {
        for (id in ids) {
            callRecordDao.deleteCallRecordById(id)
        }
    }

    suspend fun deduplicateRecords() = withContext(Dispatchers.IO) {
        val allRecords = callRecordDao.getAllRecordsOnce()
        if (allRecords.isEmpty()) return@withContext

        val duplicateIdsToDelete = mutableListOf<Long>()
        val keptRecords = mutableListOf<CallRecordEntity>()

        for (record in allRecords) {
            val normNum = normalizeNumber(record.phoneNumber)
            val duplicate = keptRecords.find { existing ->
                val normExisting = normalizeNumber(existing.phoneNumber)
                val sameNumber = (normNum.isNotEmpty() && normExisting.isNotEmpty() &&
                        (normNum == normExisting || normNum.endsWith(normExisting) || normExisting.endsWith(normNum)))
                        || (record.phoneNumber == existing.phoneNumber)
                        || (!record.callerName.isNullOrBlank() && record.callerName.equals(existing.callerName, ignoreCase = true))

                val sameType = existing.callType == record.callType
                val timeDiff = kotlin.math.abs(existing.timestamp - record.timestamp)
                val maxDuration = kotlin.math.max(existing.durationSeconds, record.durationSeconds) * 1000L
                sameNumber && sameType && timeDiff <= kotlin.math.max(45_000L, maxDuration + 20_000L)
            }

            if (duplicate != null) {
                duplicateIdsToDelete.add(record.id)
            } else {
                keptRecords.add(record)
            }
        }

        if (duplicateIdsToDelete.isNotEmpty()) {
            callRecordDao.deleteCallRecordsByIds(duplicateIdsToDelete)
        }
    }

    @SuppressLint("Range")
    suspend fun syncSystemCallLog() = withContext(Dispatchers.IO) {
        // 1. Purge any duplicate records already accumulated in the Room DB
        try {
            deduplicateRecords()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.TYPE,
            CallLog.Calls.CACHED_NAME
        )

        try {
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                "${CallLog.Calls.DATE} DESC LIMIT 150"
            )

            val seenSystemCalls = mutableListOf<Triple<Long, String, Long>>() // id, normalizedNumber, date

            cursor?.use {
                val idIdx = it.getColumnIndex(CallLog.Calls._ID)
                val numIdx = it.getColumnIndex(CallLog.Calls.NUMBER)
                val dateIdx = it.getColumnIndex(CallLog.Calls.DATE)
                val durIdx = it.getColumnIndex(CallLog.Calls.DURATION)
                val typeIdx = it.getColumnIndex(CallLog.Calls.TYPE)
                val nameIdx = it.getColumnIndex(CallLog.Calls.CACHED_NAME)

                while (it.moveToNext()) {
                    val sysId = if (idIdx >= 0) it.getLong(idIdx) else -1L
                    val number = if (numIdx >= 0) it.getString(numIdx) ?: "" else ""
                    val timestamp = if (dateIdx >= 0) it.getLong(dateIdx) else 0L
                    val duration = if (durIdx >= 0) it.getLong(durIdx) else 0L
                    val sysType = if (typeIdx >= 0) it.getInt(typeIdx) else CallLog.Calls.INCOMING_TYPE
                    val name = if (nameIdx >= 0) it.getString(nameIdx) else null

                    val direction = when (sysType) {
                        CallLog.Calls.INCOMING_TYPE -> CallDirectionType.INCOMING
                        CallLog.Calls.OUTGOING_TYPE -> CallDirectionType.OUTGOING
                        CallLog.Calls.MISSED_TYPE -> CallDirectionType.MISSED
                        CallLog.Calls.REJECTED_TYPE -> CallDirectionType.REJECTED
                        CallLog.Calls.BLOCKED_TYPE -> CallDirectionType.BLOCKED
                        else -> CallDirectionType.INCOMING
                    }

                    if (number.isNotBlank()) {
                        val norm = normalizeNumber(number)
                        // Clean up duplicate row in system call log if already seen
                        val isDuplicateInSystem = seenSystemCalls.any { (_, seenNorm, seenDate) ->
                            (seenNorm == norm || seenNorm.endsWith(norm) || norm.endsWith(seenNorm)) &&
                                    kotlin.math.abs(seenDate - timestamp) <= kotlin.math.max(45_000L, duration * 1000L + 20_000L)
                        }

                        if (isDuplicateInSystem && sysId > 0) {
                            try {
                                context.contentResolver.delete(
                                    CallLog.Calls.CONTENT_URI,
                                    "${CallLog.Calls._ID} = ?",
                                    arrayOf(sysId.toString())
                                )
                            } catch (_: Exception) {}
                        } else {
                            if (sysId > 0) {
                                seenSystemCalls.add(Triple(sysId, norm, timestamp))
                            }

                            // Prevent inserting duplicates into LINEA Room DB during system sync
                            if (!hasRecordNearTimestamp(number, timestamp, duration)) {
                                val lookup = contactLookupHelper.lookupContact(number)
                                val isPrivate = lookup.isPrivate

                                val resolvedName = if (!name.isNullOrBlank() && !isPrivate) {
                                    name
                                } else {
                                    lookup.displayName
                                }

                                val record = CallRecordEntity(
                                    phoneNumber = number,
                                    formattedNumber = number,
                                    callerName = resolvedName,
                                    callType = direction,
                                    timestamp = timestamp,
                                    durationSeconds = duration,
                                    simSlot = 0,
                                    simDisplayName = "SIM 1",
                                    sessionGroupId = "session_${number.filter { c -> c.isDigit() }}",
                                    isPrivateContact = isPrivate
                                )
                                callRecordDao.insertCallRecord(record)
                            }
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
