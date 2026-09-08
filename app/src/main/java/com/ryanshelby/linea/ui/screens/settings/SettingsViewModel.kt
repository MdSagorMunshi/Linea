package com.ryanshelby.linea.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryanshelby.linea.data.local.dao.BlockedNumberDao
import com.ryanshelby.linea.data.local.dao.CallRecordDao
import com.ryanshelby.linea.data.local.dao.CallRuleDao
import com.ryanshelby.linea.data.preferences.LineaPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val defaultSim: Int = 0,
    val askSimBeforeDial: Boolean = false,
    val callConfirmation: Boolean = false,
    val callCountdownSeconds: Int = 3,
    val proximitySensorEnabled: Boolean = true,
    val dialpadSound: Boolean = true,
    val dialpadVibration: Boolean = true,
    val callVibration: Boolean = true,
    val theme: String = "DARK",
    val cleanupDays: Int = 0,
    val reduceAnimations: Boolean = false,
    val blockedRulesCount: Int = 0,
    val isQuietHoursActive: Boolean = false,
    val unreadMissedCalls: Int = 0,
    val autoRecordCalls: Boolean = false,
    val callDurationWarningMinutes: Int = 0,
    val voicemailNumber: String = "123"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: LineaPreferences,
    private val blockedNumberDao: BlockedNumberDao,
    private val callRuleDao: CallRuleDao,
    private val callRecordDao: CallRecordDao
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        preferences.defaultSim,
        preferences.askSimBeforeDial,
        preferences.callConfirmationEnabled,
        preferences.callCountdownSeconds,
        preferences.proximitySensorEnabled,
        preferences.dialpadSoundEnabled,
        preferences.dialpadVibrationEnabled,
        preferences.callVibrationEnabled,
        preferences.themePreference,
        preferences.cleanupDays,
        preferences.reduceAnimations,
        blockedNumberDao.getAllBlockedNumbers(),
        callRuleDao.getAllRules(),
        callRecordDao.getUnreadMissedCallCount(),
        preferences.autoRecordCalls,
        preferences.callDurationWarningMinutes,
        preferences.voicemailNumber
    ) { args ->
        val defaultSim = args[0] as Int
        val askSim = args[1] as Boolean
        val confirm = args[2] as Boolean
        val countdown = args[3] as Int
        val prox = args[4] as Boolean
        val dtmf = args[5] as Boolean
        val haptic = args[6] as Boolean
        val callVib = args[7] as Boolean
        val theme = args[8] as String
        val cleanup = args[9] as Int
        val reduceAnim = args[10] as Boolean
        val blockedList = args[11] as List<*>
        val rulesList = args[12] as List<*>
        val missedCount = args[13] as Int
        val autoRecord = args[14] as Boolean
        val durationWarn = args[15] as Int
        val vmNumber = args[16] as String

        val quietRule = rulesList.filterIsInstance<com.ryanshelby.linea.data.local.entities.CallRuleEntity>()
            .firstOrNull { it.name.contains("Quiet", ignoreCase = true) }

        SettingsUiState(
            defaultSim = defaultSim,
            askSimBeforeDial = askSim,
            callConfirmation = confirm,
            callCountdownSeconds = if (countdown == 0) 3 else countdown,
            proximitySensorEnabled = prox,
            dialpadSound = dtmf,
            dialpadVibration = haptic,
            callVibration = callVib,
            theme = theme,
            cleanupDays = cleanup,
            reduceAnimations = reduceAnim,
            blockedRulesCount = blockedList.size,
            isQuietHoursActive = quietRule?.isEnabled ?: false,
            unreadMissedCalls = missedCount,
            autoRecordCalls = autoRecord,
            callDurationWarningMinutes = durationWarn,
            voicemailNumber = vmNumber
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setAutoRecordCalls(enabled: Boolean) {
        viewModelScope.launch { preferences.setAutoRecordCalls(enabled) }
    }

    fun setCallDurationWarningMinutes(minutes: Int) {
        viewModelScope.launch { preferences.setCallDurationWarningMinutes(minutes) }
    }

    fun setVoicemailNumber(number: String) {
        viewModelScope.launch { preferences.setVoicemailNumber(number) }
    }

    fun setDefaultSim(sim: Int) {
        viewModelScope.launch { preferences.setDefaultSim(sim) }
    }

    fun setAskSimBeforeDial(enabled: Boolean) {
        viewModelScope.launch { preferences.setAskSimBeforeDial(enabled) }
    }

    fun setCallConfirmation(enabled: Boolean) {
        viewModelScope.launch { preferences.setCallConfirmation(enabled) }
    }

    fun setCountdownSeconds(seconds: Int) {
        viewModelScope.launch { preferences.setCountdownSeconds(seconds) }
    }

    fun setProximitySensor(enabled: Boolean) {
        viewModelScope.launch { preferences.setProximitySensor(enabled) }
    }

    fun setDialpadSound(enabled: Boolean) {
        viewModelScope.launch { preferences.setDialpadSound(enabled) }
    }

    fun setDialpadVibration(enabled: Boolean) {
        viewModelScope.launch { preferences.setDialpadVibration(enabled) }
    }

    fun setCallVibration(enabled: Boolean) {
        viewModelScope.launch { preferences.setCallVibration(enabled) }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch { preferences.setThemePreference(theme) }
    }

    fun setCleanupDays(days: Int) {
        viewModelScope.launch { preferences.setCleanupDays(days) }
    }

    fun setReduceAnimations(enabled: Boolean) {
        viewModelScope.launch { preferences.setReduceAnimations(enabled) }
    }

    fun clearAllCallHistory() {
        viewModelScope.launch {
            callRecordDao.clearAllCallRecords()
        }
    }
}
