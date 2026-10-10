package com.ryanshelby.linea.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ryanshelby.linea.data.local.entities.CountryRuleEntity
import com.ryanshelby.linea.data.local.entities.CountryRuleType
import kotlinx.coroutines.flow.Flow

@Dao
interface CountryRuleDao {

    @Query("SELECT * FROM country_rules ORDER BY countryName ASC")
    fun getAllRules(): Flow<List<CountryRuleEntity>>

    @Query("SELECT * FROM country_rules WHERE ruleType = :type ORDER BY countryName ASC")
    fun getRulesByType(type: CountryRuleType): Flow<List<CountryRuleEntity>>

    @Query("SELECT * FROM country_rules WHERE isEnabled = 1")
    suspend fun getActiveRules(): List<CountryRuleEntity>

    @Query("SELECT * FROM country_rules WHERE countryIso = :iso LIMIT 1")
    suspend fun findRuleByIso(iso: String): CountryRuleEntity?

    @Query("SELECT * FROM country_rules WHERE countryIso = :iso AND ruleType = :type LIMIT 1")
    suspend fun findRuleByIsoAndType(iso: String, type: CountryRuleType): CountryRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: CountryRuleEntity): Long

    @Update
    suspend fun updateRule(rule: CountryRuleEntity)

    @Query("DELETE FROM country_rules WHERE id = :id")
    suspend fun deleteRuleById(id: Long)

    @Query("DELETE FROM country_rules WHERE countryIso = :iso")
    suspend fun deleteRuleByIso(iso: String)

    @Query("DELETE FROM country_rules WHERE countryIso = :iso AND ruleType = :type")
    suspend fun deleteRuleByIsoAndType(iso: String, type: CountryRuleType)

    @Query("SELECT COUNT(*) FROM country_rules WHERE ruleType = :type AND isEnabled = 1")
    fun getActiveRuleCountByType(type: CountryRuleType): Flow<Int>
}
