package com.ryanshelby.linea.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RuleAllowedFilter {
    ALL,
    FAVORITES_ONLY,
    SPECIFIC_GROUP
}

enum class RuleAction {
    ALLOW,
    REJECT,
    SILENT
}

@Entity(tableName = "call_rules")
data class CallRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val isEnabled: Boolean = true,
    val startTime: String = "22:00", // HH:mm
    val endTime: String = "07:00",   // HH:mm
    val daysOfWeek: String = "1,2,3,4,5,6,7", // 1 = Monday ... 7 = Sunday
    val allowedFilter: RuleAllowedFilter = RuleAllowedFilter.FAVORITES_ONLY,
    val allowedGroupId: Long? = null,
    val simSlot: Int? = null, // null for all SIMs
    val action: RuleAction = RuleAction.REJECT
)
