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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallRecord(callRecord: CallRecordEntity): Long

    @Update
    suspend fun updateCallRecord(callRecord: CallRecordEntity)

    @Query("DELETE FROM call_records WHERE id = :id")
    suspend fun deleteCallRecordById(id: Long)

    @Query("DELETE FROM call_records")
    suspend fun clearAllCallRecords()

    @Query("DELETE FROM call_records WHERE timestamp < :cutoffTimestamp AND isPrivateContact = 0")
    suspend fun deleteRecordsOlderThan(cutoffTimestamp: Long): Int
}
