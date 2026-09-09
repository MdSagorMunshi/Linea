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

    private val _enteredNumber = MutableStateFlow("")
    val enteredNumber: StateFlow<String> = _enteredNumber.asStateFlow()

    private val _simAccounts = MutableStateFlow<List<SimAccountInfo>>(emptyList())
    val simAccounts: StateFlow<List<SimAccountInfo>> = _simAccounts.asStateFlow()

    private val _selectedSimIndex = MutableStateFlow(0)
    val selectedSimIndex: StateFlow<Int> = _selectedSimIndex.asStateFlow()

    private val _contacts = MutableStateFlow<List<T9Contact>>(emptyList())

    val soundEnabled: StateFlow<Boolean> = preferences.dialpadSoundEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val vibrationEnabled: StateFlow<Boolean> = preferences.dialpadVibrationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val askSimBeforeDial: StateFlow<Boolean> = preferences.askSimBeforeDial
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

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
        _simAccounts.value = phoneAccountManager.registerPhoneAccounts()
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
        _selectedSimIndex.value = index
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
        val simHandle = if (accounts.isNotEmpty()) {
            val idx = _selectedSimIndex.value.coerceIn(0, accounts.size - 1)
            accounts[idx].phoneAccountHandle
        } else null

        callManager.placeCall(numberToCall, simHandle)
    }
}
