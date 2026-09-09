package com.ryanshelby.linea.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryanshelby.linea.data.local.dao.BlockedNumberDao
import com.ryanshelby.linea.data.local.dao.CallRecordDao
import com.ryanshelby.linea.data.local.dao.CallRuleDao
import com.ryanshelby.linea.data.preferences.LineaPreferences
import com.ryanshelby.linea.telecom.cleanup.CallHistoryCleanupManager
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
    val dontInterruptMe: Boolean = false,
    val repeatCallOverride: Boolean = true,
    val callRulesCount: Int = 0,
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
    val voicemailNumber: String = "123",
    val ringtoneType: String = com.ryanshelby.linea.data.preferences.LineaPreferences.RingtoneType.APP_DEFAULT,
    val flipToSilence: Boolean = false,
    val proximityWaveToSilence: Boolean = false,
    val dialpadHapticProfile: String = com.ryanshelby.linea.data.preferences.LineaPreferences.DialpadHapticProfile.TITANIUM_GLASS
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: LineaPreferences,
    private val blockedNumberDao: BlockedNumberDao,
    private val callRuleDao: CallRuleDao,
    private val callRecordDao: CallRecordDao,
    private val cleanupManager: CallHistoryCleanupManager
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        preferences.defaultSim,
        preferences.askSimBeforeDial,
        preferences.callConfirmationEnabled,
        preferences.callCountdownSeconds,
        preferences.dontInterruptMe,
        preferences.repeatCallOverride,
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
        preferences.voicemailNumber,
        preferences.ringtoneType,
        preferences.flipToSilenceEnabled,
        preferences.proximityWaveToSilenceEnabled,
        preferences.dialpadHapticProfile
    ) { args ->
        val defaultSim = args[0] as Int
        val askSim = args[1] as Boolean
        val confirm = args[2] as Boolean
        val countdown = args[3] as Int
        val dim = args[4] as Boolean
        val repeatOverride = args[5] as Boolean
        val prox = args[6] as Boolean
        val dtmf = args[7] as Boolean
        val haptic = args[8] as Boolean
        val callVib = args[9] as Boolean
        val theme = args[10] as String
        val cleanup = args[11] as Int
        val reduceAnim = args[12] as Boolean
        val blockedList = args[13] as List<*>
        val rulesList = args[14] as List<*>
        val missedCount = args[15] as Int
        val autoRecord = args[16] as Boolean
        val durationWarn = args[17] as Int
        val vmNumber = args[18] as String
        val ringtone = args[19] as String
        val flip = args[20] as Boolean
        val proxWave = args[21] as Boolean
        val hapticProfile = args[22] as String

        val quietRule = rulesList.filterIsInstance<com.ryanshelby.linea.data.local.entities.CallRuleEntity>()
            .firstOrNull { it.name.contains("Quiet", ignoreCase = true) || it.name.contains("Night", ignoreCase = true) }

        SettingsUiState(
            defaultSim = defaultSim,
            askSimBeforeDial = askSim,
            callConfirmation = confirm,
            callCountdownSeconds = if (countdown == 0) 3 else countdown,
            dontInterruptMe = dim,
            repeatCallOverride = repeatOverride,
            callRulesCount = rulesList.size,
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
            voicemailNumber = vmNumber,
            ringtoneType = ringtone,
            flipToSilence = flip,
            proximityWaveToSilence = proxWave,
            dialpadHapticProfile = hapticProfile
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = SettingsUiState()
    )

    fun setFlipToSilence(enabled: Boolean) {
        viewModelScope.launch { preferences.setFlipToSilence(enabled) }
    }

    fun setProximityWaveToSilence(enabled: Boolean) {
        viewModelScope.launch { preferences.setProximityWaveToSilence(enabled) }
    }

    fun setDialpadHapticProfile(profile: String) {
        viewModelScope.launch { preferences.setDialpadHapticProfile(profile) }
    }

    fun setRingtoneType(type: String) {
        viewModelScope.launch { preferences.setRingtoneType(type) }
    }

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

    fun setDontInterruptMe(enabled: Boolean) {
        viewModelScope.launch { preferences.setDontInterruptMe(enabled) }
    }

    fun setRepeatCallOverride(enabled: Boolean) {
        viewModelScope.launch { preferences.setRepeatCallOverride(enabled) }
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

    val activeProfile: StateFlow<String> = preferences.activeProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "PERSONAL")

    fun setActiveProfile(profile: String) {
        viewModelScope.launch {
            preferences.setActiveProfile(profile)
        }
    }

    fun cleanHistoryNow(onResult: ((Int) -> Unit)? = null) {
        viewModelScope.launch {
            val deleted = cleanupManager.runCleanupNow()
            onResult?.invoke(deleted)
        }
    }

    fun clearAllCallHistory() {
        viewModelScope.launch {
            callRecordDao.clearAllCallRecords()
        }
    }

    fun exportCallHistoryCsv(
        uri: android.net.Uri,
        contentResolver: android.content.ContentResolver,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val records = callRecordDao.getAllRecordsOnce()
                contentResolver.openOutputStream(uri)?.use { os ->
                    val writer = java.io.BufferedWriter(java.io.OutputStreamWriter(os, Charsets.UTF_8))
                    writer.write("ID,Phone Number,Caller Name,Call Type,Date,Duration (Seconds),SIM Slot,Network Type,Notes\n")
                    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                    for (r in records) {
                        val dateStr = dateFormat.format(java.util.Date(r.timestamp))
                        val cleanName = (r.callerName ?: "").replace("\"", "\"\"")
                        val cleanNum = r.phoneNumber.replace("\"", "\"\"")
                        val cleanNotes = (r.notes ?: "").replace("\"", "\"\"")
                        writer.write("${r.id},\"$cleanNum\",\"$cleanName\",${r.callType.name},\"$dateStr\",${r.durationSeconds},${r.simSlot},${r.networkType},\"$cleanNotes\"\n")
                    }
                    writer.flush()
                }
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(true, "Call history exported (${records.size} records)")
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(false, "Export failed: ${e.localizedMessage}")
                }
            }
        }
    }
}
