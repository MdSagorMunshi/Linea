package com.ryanshelby.linea.ui.screens.settings.blocking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryanshelby.linea.data.local.dao.BlockedNumberDao
import com.ryanshelby.linea.data.local.dao.CallRuleDao
import com.ryanshelby.linea.data.local.entities.BlockAction
import com.ryanshelby.linea.data.local.entities.BlockMatchType
import com.ryanshelby.linea.data.local.entities.BlockedCallLogEntity
import com.ryanshelby.linea.data.local.entities.BlockedNumberEntity
import com.ryanshelby.linea.data.local.entities.CallRuleEntity
import com.ryanshelby.linea.data.local.entities.RuleAction
import com.ryanshelby.linea.data.local.entities.RuleAllowedFilter
import com.ryanshelby.linea.data.preferences.LineaPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BlockingUiState(
    val blockedNumbers: List<BlockedNumberEntity> = emptyList(),
    val blockedLogs: List<BlockedCallLogEntity> = emptyList(),
    val quietHoursRule: CallRuleEntity? = null,
    val isAllowListMode: Boolean = false,
    val blockPrivate: Boolean = false,
    val blockUnknown: Boolean = false,
    val blockNonContacts: Boolean = false,
    val blockInternational: Boolean = false,
    val repeatCallOverride: Boolean = true,
    val selectedTab: Int = 0 // 0 = Rules, 1 = History
)

@HiltViewModel
class BlockingViewModel @Inject constructor(
    private val blockedNumberDao: BlockedNumberDao,
    private val callRuleDao: CallRuleDao,
    private val preferences: LineaPreferences
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(0)

    val uiState: StateFlow<BlockingUiState> = combine(
        blockedNumberDao.getAllBlockedNumbers(),
        blockedNumberDao.getAllBlockedLogs(),
        callRuleDao.getAllRules(),
        preferences.allowListMode,
        preferences.blockPrivate,
        preferences.blockUnknown,
        preferences.blockNonContacts,
        preferences.blockInternational,
        preferences.repeatCallOverride,
        _selectedTab
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val numbers = args[0] as List<BlockedNumberEntity>
        @Suppress("UNCHECKED_CAST")
        val logs = args[1] as List<BlockedCallLogEntity>
        @Suppress("UNCHECKED_CAST")
        val rules = args[2] as List<CallRuleEntity>
        val allowList = args[3] as Boolean
        val priv = args[4] as Boolean
        val unk = args[5] as Boolean
        val nonContacts = args[6] as Boolean
        val intl = args[7] as Boolean
        val repeat = args[8] as Boolean
        val tab = args[9] as Int

        var quietHours = rules.firstOrNull { it.name.contains("Quiet Hours", ignoreCase = true) }
        if (quietHours == null && rules.isNotEmpty()) {
            quietHours = rules.first()
        }

        BlockingUiState(
            blockedNumbers = numbers,
            blockedLogs = logs,
            quietHoursRule = quietHours,
            isAllowListMode = allowList,
            blockPrivate = priv,
            blockUnknown = unk,
            blockNonContacts = nonContacts,
            blockInternational = intl,
            repeatCallOverride = repeat,
            selectedTab = tab
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BlockingUiState()
    )

    init {
        // Ensure a default Quiet Hours rule exists in the database
        viewModelScope.launch {
            val rules = callRuleDao.getActiveRules()
            if (rules.isEmpty()) {
                callRuleDao.insertRule(
                    CallRuleEntity(
                        name = "Quiet Hours",
                        isEnabled = false,
                        startTime = "22:00",
                        endTime = "07:00",
                        daysOfWeek = "1,2,3,4,5,6,7",
                        allowedFilter = RuleAllowedFilter.FAVORITES_ONLY,
                        action = RuleAction.REJECT
                    )
                )
            }
        }
    }

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun addBlockedRule(
        numberOrPrefix: String,
        matchType: BlockMatchType,
        action: BlockAction,
        reason: String? = null,
        durationHours: Int? = null
    ) {
        viewModelScope.launch {
            val expiresAt = if (durationHours != null && durationHours > 0) {
                System.currentTimeMillis() + (durationHours * 3600L * 1000L)
            } else null

            blockedNumberDao.insertBlockedNumber(
                BlockedNumberEntity(
                    numberOrPrefix = numberOrPrefix.trim(),
                    matchType = matchType,
                    reason = reason ?: "Manual Block",
                    expiresAt = expiresAt,
                    blockAction = action
                )
            )
        }
    }

    fun unblockRule(id: Long) {
        viewModelScope.launch {
            blockedNumberDao.deleteBlockedNumberById(id)
        }
    }

    fun toggleAllowListMode(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setAllowListMode(enabled)
        }
    }

    fun toggleBlockPrivate(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setBlockPrivate(enabled)
        }
    }

    fun toggleBlockUnknown(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setBlockUnknown(enabled)
        }
    }

    fun toggleBlockNonContacts(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setBlockNonContacts(enabled)
        }
    }

    fun toggleBlockInternational(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setBlockInternational(enabled)
        }
    }

    fun toggleRepeatCallOverride(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setRepeatCallOverride(enabled)
        }
    }

    fun toggleQuietHours(enabled: Boolean) {
        viewModelScope.launch {
            val rule = uiState.value.quietHoursRule ?: return@launch
            callRuleDao.updateRule(rule.copy(isEnabled = enabled))
        }
    }

    fun updateQuietHoursTimes(startTime: String, endTime: String) {
        viewModelScope.launch {
            val rule = uiState.value.quietHoursRule ?: return@launch
            callRuleDao.updateRule(rule.copy(startTime = startTime, endTime = endTime))
        }
    }

    fun updateQuietHoursFilter(filter: RuleAllowedFilter) {
        viewModelScope.launch {
            val rule = uiState.value.quietHoursRule ?: return@launch
            callRuleDao.updateRule(rule.copy(allowedFilter = filter))
        }
    }
}
