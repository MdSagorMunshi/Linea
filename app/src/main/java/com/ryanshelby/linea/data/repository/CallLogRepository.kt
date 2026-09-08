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
        if (callRecordDao.hasRecordNearTimestamp(phoneNumber, timestamp) == 0) {
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
        if (!isPrivate) {
            try {
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

    @SuppressLint("Range")
    suspend fun syncSystemCallLog() = withContext(Dispatchers.IO) {
        // 1. Purge any duplicate records already accumulated in the Room DB
        try {
            callRecordDao.deduplicateRecords()
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

            cursor?.use {
                val numIdx = it.getColumnIndex(CallLog.Calls.NUMBER)
                val dateIdx = it.getColumnIndex(CallLog.Calls.DATE)
                val durIdx = it.getColumnIndex(CallLog.Calls.DURATION)
                val typeIdx = it.getColumnIndex(CallLog.Calls.TYPE)
                val nameIdx = it.getColumnIndex(CallLog.Calls.CACHED_NAME)

                while (it.moveToNext()) {
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
                        // Prevent inserting duplicates during system sync
                        if (callRecordDao.hasRecordNearTimestamp(number, timestamp) == 0) {
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
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
