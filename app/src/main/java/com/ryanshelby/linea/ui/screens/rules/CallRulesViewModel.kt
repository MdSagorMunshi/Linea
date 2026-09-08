package com.ryanshelby.linea.ui.screens.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryanshelby.linea.data.local.dao.CallRuleDao
import com.ryanshelby.linea.data.local.dao.ContactDao
import com.ryanshelby.linea.data.local.entities.CallRuleEntity
import com.ryanshelby.linea.data.local.entities.ContactGroupEntity
import com.ryanshelby.linea.data.local.entities.RuleAction
import com.ryanshelby.linea.data.local.entities.RuleAllowedFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CallRulesViewModel @Inject constructor(
    private val callRuleDao: CallRuleDao,
    private val contactDao: ContactDao
) : ViewModel() {

    val rules: StateFlow<List<CallRuleEntity>> = callRuleDao.getAllRules()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val groups: StateFlow<List<ContactGroupEntity>> = contactDao.getAllGroups()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            seedDefaultRulesIfEmpty()
        }
    }

    private suspend fun seedDefaultRulesIfEmpty() {
        val active = callRuleDao.getActiveRules()
        if (active.isEmpty()) {
            // Seed Nighttime Favorites rule
            callRuleDao.insertRule(
                CallRuleEntity(
                    name = "Nighttime Favorites Only",
                    isEnabled = true,
                    startTime = "22:00",
                    endTime = "07:00",
                    daysOfWeek = "1,2,3,4,5,6,7",
                    allowedFilter = RuleAllowedFilter.FAVORITES_ONLY,
                    simSlot = null,
                    action = RuleAction.REJECT
                )
            )

            // Seed Weekday Work Hours rule
            callRuleDao.insertRule(
                CallRuleEntity(
                    name = "Workday Priority Only",
                    isEnabled = false,
                    startTime = "09:00",
                    endTime = "17:00",
                    daysOfWeek = "1,2,3,4,5",
                    allowedFilter = RuleAllowedFilter.FAVORITES_ONLY,
                    simSlot = 0,
                    action = RuleAction.SILENT
                )
            )
        }
    }

    fun toggleRule(rule: CallRuleEntity) {
        viewModelScope.launch {
            callRuleDao.updateRule(rule.copy(isEnabled = !rule.isEnabled))
        }
    }

    fun deleteRule(id: Long) {
        viewModelScope.launch {
            callRuleDao.deleteRuleById(id)
        }
    }

    fun saveRule(rule: CallRuleEntity) {
        viewModelScope.launch {
            if (rule.id == 0L) {
                callRuleDao.insertRule(rule)
            } else {
                callRuleDao.updateRule(rule)
            }
        }
    }
}
