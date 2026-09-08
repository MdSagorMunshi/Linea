package com.ryanshelby.linea.telecom.screening

import android.content.Context
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import com.ryanshelby.linea.data.local.dao.BlockedNumberDao
import com.ryanshelby.linea.data.local.dao.CallRecordDao
import com.ryanshelby.linea.data.local.dao.CallRuleDao
import com.ryanshelby.linea.data.local.dao.ContactDao
import com.ryanshelby.linea.data.local.entities.BlockAction
import com.ryanshelby.linea.data.local.entities.BlockMatchType
import com.ryanshelby.linea.data.local.entities.BlockedCallLogEntity
import com.ryanshelby.linea.data.local.entities.BlockedNumberEntity
import com.ryanshelby.linea.data.local.entities.CallRuleEntity
import com.ryanshelby.linea.data.local.entities.RuleAction
import com.ryanshelby.linea.data.local.entities.RuleAllowedFilter
import com.ryanshelby.linea.data.preferences.LineaPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

sealed class ScreeningDecision {
    data object Allow : ScreeningDecision()
    data class Block(
        val action: BlockAction,
        val reason: String,
        val matchedRuleId: Long? = null,
        val matchedRuleType: String = "EXACT"
    ) : ScreeningDecision()
}

@Singleton
class CallScreeningEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val blockedNumberDao: BlockedNumberDao,
    private val callRuleDao: CallRuleDao,
    private val contactDao: ContactDao,
    private val callRecordDao: CallRecordDao,
    private val preferences: LineaPreferences,
    private val repeatCallTracker: RepeatCallTracker,
    private val contactLookupHelper: com.ryanshelby.linea.telecom.ContactLookupHelper
) {

    suspend fun screenCall(
        phoneNumber: String?,
        isPrivate: Boolean = false,
        simSlot: Int = 0,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): ScreeningDecision {
        val rawNumber = phoneNumber?.trim().orEmpty()
        val normalizedNumber = normalize(rawNumber)

        // Record incoming call attempt for sliding window repeated-call tracking
        if (normalizedNumber.isNotBlank()) {
            repeatCallTracker.recordAttempt(normalizedNumber, currentTimeMillis)
        }

        // 1. Resolve Contact identity (checks both system ContactsContract and local Room DB)
        val contactNumber = if (normalizedNumber.isNotBlank()) {
            contactDao.findNumberByNormalized(normalizedNumber)
        } else null

        val localContact = if (contactNumber != null) {
            contactDao.getContactById(contactNumber.contactId)
        } else null

        val systemContactLookup = if (rawNumber.isNotBlank()) {
            contactLookupHelper.lookupContact(rawNumber)
        } else null

        val isKnownContact = localContact != null || systemContactLookup?.displayName != null

        // 2. Contact Override: If contact has VIP rules, always allow immediately
        if (localContact != null && (localContact.allowDuringRestrictedHours || localContact.alwaysRing)) {
            return ScreeningDecision.Allow
        }

        // 3. Evaluate Active Blocked Numbers Rules (Explicit user manual blacklist)
        val activeBlockedRules = blockedNumberDao.getActiveBlockedNumbers(currentTimeMillis)
        for (rule in activeBlockedRules) {
            if (rule.expiresAt != null && rule.expiresAt <= currentTimeMillis) {
                continue
            }

            if (matchesBlockedRule(rule, rawNumber, normalizedNumber, isKnownContact)) {
                return ScreeningDecision.Block(
                    action = rule.blockAction,
                    reason = rule.reason ?: "Blocked via Rule #${rule.id}",
                    matchedRuleId = rule.id,
                    matchedRuleType = rule.matchType.name
                )
            }
        }

        // 4. Default: Zero Call Blocking Logic unless explicitly configured in Settings
        val callBlockingMasterPref = preferences.callBlockingEnabled.first()
        val blockPrivatePref = preferences.blockPrivate.first()
        val allowListMode = preferences.allowListMode.first()
        val blockNonContactsPref = preferences.blockNonContacts.first()
        val blockUnknownPref = preferences.blockUnknown.first()
        val blockInternationalPref = preferences.blockInternational.first()
        val activeCallRules = callRuleDao.getActiveRules()

        val hasActiveScreeningFeature = callBlockingMasterPref ||
                blockPrivatePref ||
                allowListMode ||
                blockNonContactsPref ||
                blockUnknownPref ||
                blockInternationalPref ||
                activeCallRules.isNotEmpty()

        if (!hasActiveScreeningFeature) {
            // Default mode: no rules configured -> always allow all incoming calls!
            return ScreeningDecision.Allow
        }

        // 5. Emergency Repeat-Call Override ("Allow if called 3 times in 5 minutes")
        val repeatOverridePref = preferences.repeatCallOverride.first()
        if (repeatOverridePref && normalizedNumber.isNotBlank()) {
            val fiveMinutesAgo = currentTimeMillis - (5 * 60 * 1000L)
            val recentCallsCount = callRecordDao.getRecentCallCountForNumber(normalizedNumber, fiveMinutesAgo)
            val trackedAttempts = repeatCallTracker.getRecentAttemptsCount(normalizedNumber, fiveMinutesAgo)
            if (recentCallsCount >= 2 || trackedAttempts >= 3) {
                return ScreeningDecision.Allow
            }
        }

        // 6. Private or Hidden Caller ID check
        val isHidden = isPrivate || rawNumber.isBlank() || rawNumber.equals("private", ignoreCase = true) || rawNumber.equals("unknown", ignoreCase = true)
        if (isHidden && blockPrivatePref) {
            return ScreeningDecision.Block(
                action = BlockAction.SILENT_REJECT,
                reason = "Private/Hidden Caller ID Blocked",
                matchedRuleType = "PRIVATE"
            )
        }

        // 7. Allow-List Mode Check (Only allowed if contact exists)
        if (allowListMode && !isKnownContact) {
            return ScreeningDecision.Block(
                action = BlockAction.SILENT_REJECT,
                reason = "Allow-List Mode Active (Non-Contact)",
                matchedRuleType = "ALLOW_LIST"
            )
        }

        // 8. Block Non-Contacts Preference
        if (blockNonContactsPref && !isKnownContact && !isHidden) {
            return ScreeningDecision.Block(
                action = BlockAction.SILENT_REJECT,
                reason = "Non-Contact Callers Blocked",
                matchedRuleType = "NON_CONTACT"
            )
        }

        // 9. Block Unknown / Empty Number Preference
        if (blockUnknownPref && isHidden) {
            return ScreeningDecision.Block(
                action = BlockAction.SILENT_REJECT,
                reason = "Unknown Caller ID Blocked",
                matchedRuleType = "UNKNOWN"
            )
        }

        // 10. Block International Calls Preference
        if (blockInternationalPref && isInternationalNumber(rawNumber)) {
            return ScreeningDecision.Block(
                action = BlockAction.SILENT_REJECT,
                reason = "International Numbers Blocked",
                matchedRuleType = "INTERNATIONAL"
            )
        }

        // 11. Evaluate Scheduled Quiet Hours & Call Rules (Only if rules are active)
        for (rule in activeCallRules) {
            if (matchesScheduleRule(rule, currentTimeMillis, simSlot)) {
                val isAllowed = when (rule.allowedFilter) {
                    RuleAllowedFilter.ALL -> true
                    RuleAllowedFilter.FAVORITES_ONLY -> localContact?.isFavorite == true
                    RuleAllowedFilter.SPECIFIC_GROUP -> {
                        if (localContact == null || rule.allowedGroupId == null) {
                            false
                        } else {
                            contactDao.isContactInGroup(rule.allowedGroupId, localContact.id) > 0
                        }
                    }
                }

                if (!isAllowed) {
                    return ScreeningDecision.Block(
                        action = when (rule.action) {
                            RuleAction.REJECT -> BlockAction.SILENT_REJECT
                            RuleAction.SILENT -> BlockAction.VOICEMAIL
                            RuleAction.ALLOW -> BlockAction.SILENT_REJECT
                        },
                        reason = "Smart Schedule Active: ${rule.name}",
                        matchedRuleId = rule.id,
                        matchedRuleType = "SMART_SCHEDULE"
                    )
                }
            }
        }

        return ScreeningDecision.Allow
    }

    suspend fun recordBlockedCall(decision: ScreeningDecision.Block, number: String) {
        val log = BlockedCallLogEntity(
            number = number.ifBlank { "Private / Unknown" },
            timestamp = System.currentTimeMillis(),
            matchedRuleId = decision.matchedRuleId,
            matchedRuleType = decision.matchedRuleType,
            actionTaken = decision.action.name
        )
        blockedNumberDao.insertBlockedLog(log)
    }

    private fun getSimCountryIso(): String {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        return telephonyManager?.simCountryIso?.uppercase() ?: "BD"
    }

    private fun matchesBlockedRule(
        rule: BlockedNumberEntity,
        rawNumber: String,
        normalizedNumber: String,
        isContact: Boolean
    ): Boolean {
        return ScreeningRuleMatcher.matchesBlockedRule(
            rule = rule,
            rawNumber = rawNumber,
            normalizedNumber = normalizedNumber,
            isContact = isContact,
            simCountryIso = getSimCountryIso()
        )
    }

    private fun matchesScheduleRule(rule: CallRuleEntity, currentTimeMillis: Long, simSlot: Int): Boolean {
        return ScreeningRuleMatcher.matchesScheduleRule(rule, currentTimeMillis, simSlot)
    }

    private fun isInternationalNumber(rawNumber: String): Boolean {
        return ScreeningRuleMatcher.isInternationalNumber(rawNumber, getSimCountryIso())
    }

    fun normalize(number: String): String = ScreeningRuleMatcher.normalize(number)

    suspend fun purgeLegacySeededRules() {
        try {
            val rules = callRuleDao.getActiveRules()
            val dummyNames = setOf("Nighttime Favorites Only", "Workday Priority Only")
            for (rule in rules) {
                if (rule.name in dummyNames) {
                    callRuleDao.deleteRuleById(rule.id)
                }
            }
        } catch (_: Exception) {}
    }
}

object ScreeningRuleMatcher {
    fun normalize(number: String): String {
        return number.filter { it.isDigit() }
    }

    fun matchesBlockedRule(
        rule: BlockedNumberEntity,
        rawNumber: String,
        normalizedNumber: String,
        isContact: Boolean,
        simCountryIso: String = "BD"
    ): Boolean {
        val target = rule.numberOrPrefix.trim()
        val normalizedTarget = normalize(target)

        return when (rule.matchType) {
            BlockMatchType.EXACT -> {
                normalizedNumber.isNotBlank() && (
                    normalizedNumber == normalizedTarget ||
                    normalizedNumber.endsWith(normalizedTarget) ||
                    normalizedTarget.endsWith(normalizedNumber)
                )
            }
            BlockMatchType.PREFIX -> {
                val cleanPrefix = target.replace("*", "").replace("+", "").replace("-", "")
                val cleanRaw = rawNumber.replace("+", "").replace("-", "")
                cleanRaw.startsWith(cleanPrefix) || normalizedNumber.startsWith(cleanPrefix)
            }
            BlockMatchType.RANGE -> {
                if (target.contains("-")) {
                    val parts = target.split("-")
                    val start = parts.getOrNull(0)?.trim()?.toLongOrNull() ?: 0L
                    val end = parts.getOrNull(1)?.trim()?.toLongOrNull() ?: 0L
                    val numVal = normalizedNumber.toLongOrNull() ?: 0L
                    numVal in start..end
                } else false
            }
            BlockMatchType.UNKNOWN -> rawNumber.isBlank()
            BlockMatchType.PRIVATE -> rawNumber.isBlank() || rawNumber.equals("private", ignoreCase = true)
            BlockMatchType.NON_CONTACT -> !isContact
            BlockMatchType.INTERNATIONAL -> isInternationalNumber(rawNumber, simCountryIso)
        }
    }

    fun matchesScheduleRule(rule: CallRuleEntity, currentTimeMillis: Long, simSlot: Int): Boolean {
        if (!rule.isEnabled) return false
        if (rule.simSlot != null && rule.simSlot != simSlot) return false

        val cal = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
        val lineaDayOfWeek = when (val calDay = cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> 7
            else -> calDay - 1
        }

        val activeDays = rule.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
        if (lineaDayOfWeek !in activeDays) return false

        return try {
            val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val (startH, startM) = rule.startTime.split(":").map { it.toInt() }
            val (endH, endM) = rule.endTime.split(":").map { it.toInt() }

            val startMinutes = startH * 60 + startM
            val endMinutes = endH * 60 + endM

            if (startMinutes <= endMinutes) {
                // Same-day window, e.g. 09:00 to 17:00
                nowMinutes in startMinutes..endMinutes
            } else {
                // Overnight window, e.g. 22:00 to 07:00
                nowMinutes >= startMinutes || nowMinutes <= endMinutes
            }
        } catch (_: Exception) {
            false
        }
    }

    fun isInternationalNumber(rawNumber: String, simCountryIso: String = "BD"): Boolean {
        if (!rawNumber.startsWith("+")) return false
        val localPrefix = getCountryCodePrefix(simCountryIso)
        return !rawNumber.startsWith(localPrefix)
    }

    fun getCountryCodePrefix(countryIso: String): String {
        return when (countryIso.uppercase()) {
            "BD" -> "+880"
            "US", "CA" -> "+1"
            "GB" -> "+44"
            "IN" -> "+91"
            "AU" -> "+61"
            "DE" -> "+49"
            else -> "+880"
        }
    }
}
