package com.ryanshelby.linea.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class CountryRuleType {
    BLACKLIST,
    WHITELIST
}

@Entity(
    tableName = "country_rules",
    indices = [
        Index(value = ["countryIso", "ruleType"], unique = true),
        Index(value = ["dialCode"]),
        Index(value = ["ruleType"])
    ]
)
data class CountryRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val countryIso: String,          // e.g. "US", "CA", "GB", "DE" (ISO 3166-1 alpha-2 uppercase)
    val countryName: String,         // e.g. "United States"
    val dialCode: String,            // e.g. "+1", "+44"
    val flagEmoji: String,           // e.g. "🇺🇸"
    val ruleType: CountryRuleType = CountryRuleType.BLACKLIST,
    val isEnabled: Boolean = true,
    val appliesToAllSharedCode: Boolean = false, // If true, matches all countries sharing this calling code
    val createdAt: Long = System.currentTimeMillis()
)
