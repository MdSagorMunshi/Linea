package com.ryanshelby.linea.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ryanshelby.linea.data.local.entities.BlockedCallLogEntity
import com.ryanshelby.linea.data.local.entities.BlockedNumberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedNumberDao {

    @Query("SELECT * FROM blocked_numbers ORDER BY createdAt DESC")
    fun getAllBlockedNumbers(): Flow<List<BlockedNumberEntity>>

    @Query("SELECT * FROM blocked_numbers WHERE expiresAt IS NULL OR expiresAt > :currentTime")
    suspend fun getActiveBlockedNumbers(currentTime: Long = System.currentTimeMillis()): List<BlockedNumberEntity>

    @Query("SELECT * FROM blocked_numbers WHERE numberOrPrefix = :number LIMIT 1")
    suspend fun findBlockedNumber(number: String): BlockedNumberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedNumber(blockedNumber: BlockedNumberEntity): Long

    @Query("SELECT * FROM blocked_numbers WHERE id = :id LIMIT 1")
    suspend fun getBlockedNumberById(id: Long): BlockedNumberEntity?

    @Query("DELETE FROM blocked_numbers WHERE id = :id")
    suspend fun deleteBlockedNumberById(id: Long)

    @Query("DELETE FROM blocked_numbers WHERE numberOrPrefix = :number")
    suspend fun deleteBlockedNumberByNumber(number: String): Int

    @Query("DELETE FROM blocked_numbers WHERE expiresAt IS NOT NULL AND expiresAt <= :currentTime")
    suspend fun deleteExpiredBlocks(currentTime: Long = System.currentTimeMillis()): Int

    @Query("SELECT * FROM blocked_call_logs ORDER BY timestamp DESC")
    fun getAllBlockedLogs(): Flow<List<BlockedCallLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedLog(log: BlockedCallLogEntity): Long
}
