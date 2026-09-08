package com.ryanshelby.linea.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ryanshelby.linea.data.local.entities.CallDirectionType
import com.ryanshelby.linea.data.local.entities.CallRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CallRecordDao {

    @Query("SELECT * FROM call_records ORDER BY timestamp DESC")
    fun getAllCallRecords(): Flow<List<CallRecordEntity>>

    @Query("SELECT * FROM call_records WHERE callType = :type ORDER BY timestamp DESC")
    fun getCallRecordsByType(type: CallDirectionType): Flow<List<CallRecordEntity>>

    @Query("SELECT * FROM call_records WHERE phoneNumber = :number ORDER BY timestamp DESC")
    fun getCallRecordsByNumber(number: String): Flow<List<CallRecordEntity>>

    @Query("SELECT * FROM call_records WHERE id = :id LIMIT 1")
    suspend fun getCallRecordById(id: Long): CallRecordEntity?

    @Query("SELECT COUNT(*) FROM call_records WHERE callType = 'MISSED'")
    fun getUnreadMissedCallCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM call_records WHERE phoneNumber = :number AND timestamp >= :sinceTimestamp")
    suspend fun getRecentCallCountForNumber(number: String, sinceTimestamp: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallRecord(callRecord: CallRecordEntity): Long

    @Update
    suspend fun updateCallRecord(callRecord: CallRecordEntity)

    @Query("DELETE FROM call_records WHERE id = :id")
    suspend fun deleteCallRecordById(id: Long)

    @Query("DELETE FROM call_records")
    suspend fun clearAllCallRecords()

    @Query("SELECT * FROM call_records WHERE isPrivateContact = 0 ORDER BY timestamp DESC")
    fun getPublicCallRecords(): Flow<List<CallRecordEntity>>

    @Query("SELECT * FROM call_records ORDER BY timestamp DESC")
    suspend fun getAllRecordsOnce(): List<CallRecordEntity>

    @Query("SELECT * FROM call_records WHERE phoneNumber = :number ORDER BY timestamp DESC")
    suspend fun getCallRecordsForNumberOnce(number: String): List<CallRecordEntity>

    @Query("DELETE FROM call_records WHERE timestamp < :cutoffTimestamp AND isPrivateContact = 0")
    suspend fun deleteRecordsOlderThan(cutoffTimestamp: Long): Int

    @Query("UPDATE call_records SET isPrivateContact = 1 WHERE phoneNumber = :number OR REPLACE(REPLACE(REPLACE(phoneNumber, '+', ''), '-', ''), ' ', '') = :normalized")
    suspend fun markRecordsAsPrivate(number: String, normalized: String)

    @Query("UPDATE call_records SET isPrivateContact = 0 WHERE phoneNumber = :number OR REPLACE(REPLACE(REPLACE(phoneNumber, '+', ''), '-', ''), ' ', '') = :normalized")
    suspend fun markRecordsAsPublic(number: String, normalized: String)

    @Query("""
        SELECT COUNT(*) FROM call_records 
        WHERE (phoneNumber = :number OR REPLACE(REPLACE(REPLACE(phoneNumber, '+', ''), '-', ''), ' ', '') = REPLACE(REPLACE(REPLACE(:number, '+', ''), '-', ''), ' ', ''))
        AND ABS(timestamp - :timestamp) <= 5000
    """)
    suspend fun hasRecordNearTimestamp(number: String, timestamp: Long): Int

    @Query("""
        DELETE FROM call_records 
        WHERE id NOT IN (
            SELECT MIN(id) 
            FROM call_records 
            GROUP BY REPLACE(REPLACE(REPLACE(phoneNumber, '+', ''), '-', ''), ' ', ''), timestamp, callType
        )
    """)
    suspend fun deduplicateRecords(): Int
}
