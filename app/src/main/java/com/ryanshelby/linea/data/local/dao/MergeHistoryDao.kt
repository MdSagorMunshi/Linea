package com.ryanshelby.linea.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ryanshelby.linea.data.local.entities.MergeHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MergeHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMergeHistory(entry: MergeHistoryEntity): Long

    @Query("SELECT * FROM merge_history ORDER BY timestamp DESC")
    fun getAllMergeHistory(): Flow<List<MergeHistoryEntity>>

    @Query("SELECT * FROM merge_history ORDER BY timestamp DESC")
    suspend fun getAllMergeHistoryOnce(): List<MergeHistoryEntity>

    @Query("SELECT * FROM merge_history WHERE id = :id LIMIT 1")
    suspend fun getMergeHistoryById(id: Long): MergeHistoryEntity?

    @Query("SELECT * FROM merge_history ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestMergeHistory(): MergeHistoryEntity?

    @Query("DELETE FROM merge_history WHERE id = :id")
    suspend fun deleteMergeHistoryById(id: Long)

    @Query("DELETE FROM merge_history")
    suspend fun clearAllMergeHistory()
}

