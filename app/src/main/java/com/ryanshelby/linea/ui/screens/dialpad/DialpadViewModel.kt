package com.ryanshelby.linea.ui.screens.dialpad

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryanshelby.linea.data.preferences.LineaPreferences
import com.ryanshelby.linea.data.repository.ContactSyncRepository
import com.ryanshelby.linea.telecom.CallManager
import com.ryanshelby.linea.telecom.PhoneAccountManager
import com.ryanshelby.linea.telecom.SimAccountInfo
import com.ryanshelby.linea.telecom.InternationalCountryHelper
import com.ryanshelby.linea.telecom.InternationalPreview
import com.ryanshelby.linea.telecom.T9Contact
import com.ryanshelby.linea.telecom.T9SearchEngine
import com.ryanshelby.linea.telecom.T9SearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DialpadViewModel @Inject constructor(
    private val callManager: CallManager,
    private val contactSyncRepository: ContactSyncRepository,
    private val phoneAccountManager: PhoneAccountManager,
    private val preferences: LineaPreferences
) : ViewModel() {

    companion object {
        const val SIM_SLOT_1 = 0
        const val SIM_SLOT_2 = 1
        const val SIM_ALWAYS_ASK = -1
    }

    private val _enteredNumber = MutableStateFlow("")
    val enteredNumber: StateFlow<String> = _enteredNumber.asStateFlow()

    private val _simAccounts = MutableStateFlow<List<SimAccountInfo>>(emptyList())
    val simAccounts: StateFlow<List<SimAccountInfo>> = _simAccounts.asStateFlow()

    private val _selectedSimIndex = MutableStateFlow(SIM_SLOT_1)
    val selectedSimIndex: StateFlow<Int> = _selectedSimIndex.asStateFlow()

    private val _contacts = MutableStateFlow<List<T9Contact>>(emptyList())

    val soundEnabled: StateFlow<Boolean> = preferences.dialpadSoundEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val vibrationEnabled: StateFlow<Boolean> = preferences.dialpadVibrationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val defaultSim: StateFlow<Int> = preferences.defaultSim
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val askSimBeforeDial: StateFlow<Boolean> = preferences.askSimBeforeDial
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isAlwaysAskEnabled: StateFlow<Boolean> = combine(
        preferences.askSimBeforeDial,
        preferences.defaultSim
    ) { ask, default ->
        ask || default == -1
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val callConfirmationEnabled: StateFlow<Boolean> = preferences.callConfirmationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val callCountdownSeconds: StateFlow<Int> = preferences.callCountdownSeconds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)

    val activeProfile: StateFlow<String> = preferences.activeProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "PERSONAL")

    val callerIdResult: StateFlow<com.ryanshelby.linea.telecom.screening.CallerIdResult?> = _enteredNumber
        .map { number ->
            if (number.length >= 3) {
                com.ryanshelby.linea.telecom.screening.OfflineCallerIdEngine.identifyNumber(number)
            } else {
                null
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val internationalPreview: StateFlow<InternationalPreview?> = _enteredNumber
        .map { number ->
            InternationalCountryHelper.detectCountryAndLocalTime(number)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val dialpadHapticProfile: StateFlow<String> = preferences.dialpadHapticProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LineaPreferences.DialpadHapticProfile.TITANIUM_GLASS)

    val t9Matches: StateFlow<List<T9SearchResult>> = combine(
        _enteredNumber,
        _contacts
    ) { query, contacts ->
        if (query.isEmpty()) {
            emptyList()
        } else {
            T9SearchEngine.search(contacts, query)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadSimAccounts()
        loadContacts()
        resetToDefaultSim()
    }

    fun resetToDefaultSim() {
        viewModelScope.launch {
            val ask = preferences.askSimBeforeDial.first()
            val default = preferences.defaultSim.first()
            val hasSim2 = _simAccounts.value.size >= 2 || _simAccounts.value.any { it.slotIndex == 1 }
            _selectedSimIndex.value = if (ask || default == -1) {
                SIM_ALWAYS_ASK
            } else if (default == 1 && hasSim2) {
                SIM_SLOT_2
            } else {
                SIM_SLOT_1
            }
        }
    }

    fun switchProfile() {
        viewModelScope.launch {
            val next = if (activeProfile.value == "PERSONAL") "WORK" else "PERSONAL"
            preferences.setActiveProfile(next)
        }
    }

    fun onSpeedDialLongPress(digit: Char) {
        if (digit == '1') {
            placeCall("123")
        } else {
            val contacts = _contacts.value
            val index = digit.digitToIntOrNull()?.minus(2) ?: -1
            if (index in contacts.indices) {
                placeCall(contacts[index].phoneNumber)
            }
        }
    }

    fun loadSimAccounts() {
        val accounts = phoneAccountManager.registerPhoneAccounts()
        _simAccounts.value = accounts
        val hasSim2 = accounts.size >= 2 || accounts.any { it.slotIndex == 1 }
        if (!hasSim2) {
            viewModelScope.launch {
                if (preferences.defaultSim.first() == 1) {
                    preferences.setDefaultSim(0)
                }
            }
        }
    }

    fun loadContacts() {
        viewModelScope.launch {
            val contactsList = contactSyncRepository.loadContacts()
            _contacts.value = contactsList
        }
    }

    fun appendDigit(digit: Char) {
        _enteredNumber.value += digit
    }

    fun appendString(digits: String) {
        _enteredNumber.value += digits
    }

    fun deleteLastDigit() {
        if (_enteredNumber.value.isNotEmpty()) {
            _enteredNumber.value = _enteredNumber.value.dropLast(1)
        }
    }

    fun clearNumber() {
        _enteredNumber.value = ""
    }

    fun setNumber(number: String) {
        _enteredNumber.value = number
    }

    fun selectSim(index: Int) {
        if (index in 0..1 || index == SIM_ALWAYS_ASK) {
            _selectedSimIndex.value = index
        }
    }

    fun selectSimBySubscriptionId(subscriptionId: Int) {
        val index = _simAccounts.value.indexOfFirst { it.subscriptionId == subscriptionId }
        if (index >= 0) _selectedSimIndex.value = index
    }

    private var lastPlaceCallTimestamp: Long = 0L

    fun placeCall(phoneNumber: String? = null) {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastPlaceCallTimestamp < 1000L) {
            return
        }
        lastPlaceCallTimestamp = now

        val numberToCall = (phoneNumber ?: _enteredNumber.value).trim()
        if (numberToCall.isEmpty()) return

        val accounts = _simAccounts.value
        val targetSlot = if (_selectedSimIndex.value in 0..1) _selectedSimIndex.value else 0
        val selectedAccount = accounts.find { it.slotIndex == targetSlot }
            ?: accounts.getOrNull(targetSlot)
            ?: accounts.firstOrNull()
        callManager.placeCall(numberToCall, selectedAccount?.phoneAccountHandle)
    }

    fun placeCallWithAccount(phoneNumber: String? = null, account: SimAccountInfo) {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastPlaceCallTimestamp < 1000L) {
            return
        }
        lastPlaceCallTimestamp = now

        val numberToCall = (phoneNumber ?: _enteredNumber.value).trim()
        if (numberToCall.isEmpty()) return

        callManager.placeCall(numberToCall, account.phoneAccountHandle)
    }
}
