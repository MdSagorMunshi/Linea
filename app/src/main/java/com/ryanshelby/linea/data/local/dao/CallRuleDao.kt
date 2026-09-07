package com.ryanshelby.linea.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ryanshelby.linea.data.local.entities.CallRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CallRuleDao {

    @Query("SELECT * FROM call_rules ORDER BY id ASC")
    fun getAllRules(): Flow<List<CallRuleEntity>>

    @Query("SELECT * FROM call_rules WHERE isEnabled = 1")
    suspend fun getActiveRules(): List<CallRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: CallRuleEntity): Long

    @Update
    suspend fun updateRule(rule: CallRuleEntity)

    @Query("DELETE FROM call_rules WHERE id = :id")
    suspend fun deleteRuleById(id: Long)
}
