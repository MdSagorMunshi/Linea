package com.ryanshelby.linea.telecom.escape

import android.content.Context
import android.widget.Toast
import com.ryanshelby.linea.data.preferences.LineaPreferences
import com.ryanshelby.linea.telecom.CallManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class EscapeCallPreset(
    val id: String,
    val name: String,
    val number: String,
    val subtitle: String,
    val iconEmoji: String
)

@Singleton
class EscapeCallManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val callManager: CallManager,
    private val preferences: LineaPreferences
) {
    val presets = listOf(
        EscapeCallPreset(
            id = "boss",
            name = "The Boss",
            number = "+1 (202) 555-0199",
            subtitle = "Executive Urgent Request",
            iconEmoji = "💼"
        ),
        EscapeCallPreset(
            id = "doctor",
            name = "Doctor's Office",
            number = "+1 (212) 555-0143",
            subtitle = "Medical Center Escalation",
            iconEmoji = "🩺"
        ),
        EscapeCallPreset(
            id = "mom",
            name = "Mom",
            number = "+1 (415) 555-0182",
            subtitle = "Family Priority",
            iconEmoji = "❤️"
        ),
        EscapeCallPreset(
            id = "security",
            name = "Home Security",
            number = "+1 (800) 555-0191",
            subtitle = "Zone 4 Sensor Alert",
            iconEmoji = "🚨"
        ),
        EscapeCallPreset(
            id = "custom",
            name = "Custom Caller",
            number = "+1 (555) 012-3456",
            subtitle = "Custom configured persona",
            iconEmoji = "🎭"
        )
    )

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var countdownJob: Job? = null

    private val _isArmed = MutableStateFlow(false)
    val isArmed: StateFlow<Boolean> = _isArmed.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _selectedPreset = MutableStateFlow(presets.first())
    val selectedPreset: StateFlow<EscapeCallPreset> = _selectedPreset.asStateFlow()

    val customCode: StateFlow<String> = preferences.escapeCallCustomCode
        .stateIn(scope, SharingStarted.Eagerly, "*#*#3253#*#*")

    val secondaryCode: StateFlow<String> = preferences.escapeCallSecondaryCode
        .stateIn(scope, SharingStarted.Eagerly, "*#99#")

    val defaultDelay: StateFlow<Int> = preferences.escapeCallDefaultDelay
        .stateIn(scope, SharingStarted.Eagerly, 10)

    val vibrateOnly: StateFlow<Boolean> = preferences.escapeCallVibrateOnly
        .stateIn(scope, SharingStarted.Eagerly, false)

    val disarmNotification: StateFlow<Boolean> = preferences.escapeCallDisarmNotification
        .stateIn(scope, SharingStarted.Eagerly, true)

    val customName: StateFlow<String> = preferences.escapeCallCustomName
        .stateIn(scope, SharingStarted.Eagerly, "Office Dispatch")

    val customNumber: StateFlow<String> = preferences.escapeCallCustomNumber
        .stateIn(scope, SharingStarted.Eagerly, "+1 (555) 019-2834")

    private val _selectedDelaySeconds = MutableStateFlow(10)
    val selectedDelaySeconds: StateFlow<Int> = _selectedDelaySeconds.asStateFlow()

    fun selectPreset(preset: EscapeCallPreset) {
        _selectedPreset.value = preset
    }

    fun setDelaySeconds(seconds: Int) {
        _selectedDelaySeconds.value = seconds
    }

    fun setCustomPersona(name: String, number: String) {
        scope.launch {
            preferences.setEscapeCallCustomPersona(name, number)
        }
    }

    fun setCustomCode(code: String) {
        scope.launch {
            preferences.setEscapeCallCustomCode(code)
        }
    }

    fun setSecondaryCode(code: String) {
        scope.launch {
            preferences.setEscapeCallSecondaryCode(code)
        }
    }

    fun setDefaultDelay(delaySeconds: Int) {
        scope.launch {
            preferences.setEscapeCallDefaultDelay(delaySeconds)
        }
    }

    fun setVibrateOnly(enabled: Boolean) {
        scope.launch {
            preferences.setEscapeCallVibrateOnly(enabled)
        }
    }

    fun setDisarmNotification(enabled: Boolean) {
        scope.launch {
            preferences.setEscapeCallDisarmNotification(enabled)
        }
    }

    fun scheduleEscapeCall(
        delaySeconds: Int = _selectedDelaySeconds.value,
        callerName: String? = null,
        phoneNumber: String? = null,
        showToast: Boolean = true
    ) {
        val preset = _selectedPreset.value
        val finalName = callerName ?: if (preset.id == "custom") customName.value.ifBlank { "Unknown" } else preset.name
        val finalNumber = phoneNumber ?: if (preset.id == "custom") customNumber.value.ifBlank { "000" } else preset.number

        cancelCountdown()

        if (delaySeconds <= 0) {
            triggerNow(finalName, finalNumber)
            return
        }

        _isArmed.value = true
        _remainingSeconds.value = delaySeconds

        if (showToast) {
            Toast.makeText(context, "Tactical Escape Call armed (${delaySeconds}s)", Toast.LENGTH_SHORT).show()
        }

        countdownJob = scope.launch {
            var secsLeft = delaySeconds
            while (isActive && secsLeft > 0) {
                delay(1000)
                secsLeft--
                _remainingSeconds.value = secsLeft
            }
            if (isActive) {
                _isArmed.value = false
                _remainingSeconds.value = 0
                triggerNow(finalName, finalNumber)
            }
        }
    }

    fun triggerNow(callerName: String, phoneNumber: String) {
        cancelCountdown()
        callManager.triggerFakeCall(
            callerName = callerName,
            phoneNumber = phoneNumber,
            vibrateOnly = vibrateOnly.value
        )
    }

    fun cancelCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        _isArmed.value = false
        _remainingSeconds.value = 0
    }
}
