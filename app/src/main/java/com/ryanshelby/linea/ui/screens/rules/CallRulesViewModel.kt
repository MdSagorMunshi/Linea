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
            cleanupSeededRules()
        }
    }

    private suspend fun cleanupSeededRules() {
        try {
            val active = callRuleDao.getActiveRules()
            val dummyNames = setOf("Nighttime Favorites Only", "Workday Priority Only")
            active.filter { it.name in dummyNames }.forEach {
                callRuleDao.deleteRuleById(it.id)
            }
        } catch (_: Exception) {}
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
