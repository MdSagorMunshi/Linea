package com.ryanshelby.linea.ui.screens.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryanshelby.linea.data.local.dao.ContactDao
import com.ryanshelby.linea.data.local.entities.ContactEmailEntity
import com.ryanshelby.linea.data.local.entities.ContactEntity
import com.ryanshelby.linea.data.local.entities.ContactNumberEntity
import com.ryanshelby.linea.data.repository.ContactSyncRepository
import com.ryanshelby.linea.telecom.CallManager
import com.ryanshelby.linea.telecom.PhoneAccountManager
import com.ryanshelby.linea.telecom.T9SearchEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.ryanshelby.linea.data.local.dao.CallNoteDao
import com.ryanshelby.linea.data.local.entities.CallNoteEntity
import com.ryanshelby.linea.data.preferences.LineaPreferences
import com.ryanshelby.linea.data.repository.ContactAccount
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val contactDao: ContactDao,
    private val contactSyncRepository: ContactSyncRepository,
    private val callManager: CallManager,
    private val phoneAccountManager: PhoneAccountManager,
    private val callNoteDao: CallNoteDao,
    private val preferences: LineaPreferences
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeLetter = MutableStateFlow<Char?>(null)
    val activeLetter: StateFlow<Char?> = _activeLetter.asStateFlow()

    private val _selectedContactForDetail = MutableStateFlow<ContactEntity?>(null)
    val selectedContactForDetail: StateFlow<ContactEntity?> = _selectedContactForDetail.asStateFlow()

    val callCountdownSeconds: StateFlow<Int> = preferences.callCountdownSeconds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val callConfirmationEnabled: StateFlow<Boolean> = preferences.callConfirmationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isPrivateModeUnlocked: StateFlow<Boolean> = preferences.privateModeUnlocked
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val privatePin: StateFlow<String> = preferences.privateModePin
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "1234")

    private val _isPinDialogOpen = MutableStateFlow(false)
    val isPinDialogOpen: StateFlow<Boolean> = _isPinDialogOpen.asStateFlow()

    fun openPinDialog() {
        _isPinDialogOpen.value = true
    }

    fun dismissPinDialog() {
        _isPinDialogOpen.value = false
    }

    fun unlockPrivateMode() {
        viewModelScope.launch {
            preferences.setPrivateModeUnlocked(true)
            _isPinDialogOpen.value = false
        }
    }

    fun lockPrivateMode() {
        viewModelScope.launch {
            preferences.setPrivateModeUnlocked(false)
        }
    }

    val preCallNoteForSelected: StateFlow<CallNoteEntity?> = _selectedContactForDetail
        .flatMapLatest { contact ->
            if (contact != null) {
                callNoteDao.getNotesForContact(contact.id, "")
                    .map { notes -> notes.firstOrNull { it.isPreCallNote } }
            } else {
                flowOf(null)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isCreateSheetOpen = MutableStateFlow(false)
    val isCreateSheetOpen: StateFlow<Boolean> = _isCreateSheetOpen.asStateFlow()

    private val _contactToEdit = MutableStateFlow<ContactEntity?>(null)
    val contactToEdit: StateFlow<ContactEntity?> = _contactToEdit.asStateFlow()

    private val _availableAccounts = MutableStateFlow<List<ContactAccount>>(emptyList())
    val availableAccounts: StateFlow<List<ContactAccount>> = _availableAccounts.asStateFlow()

    private val _selectedContactAccount = MutableStateFlow<ContactAccount?>(null)
    val selectedContactAccount: StateFlow<ContactAccount?> = _selectedContactAccount.asStateFlow()

    val pinnedFavorites: StateFlow<List<ContactEntity>> = contactDao.getFavoriteContacts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val rawContacts: Flow<List<ContactEntity>> = preferences.privateModeUnlocked
        .flatMapLatest { unlocked ->
            if (unlocked) contactDao.getAllContactsIncludingPrivate() else contactDao.getAllContacts()
        }

    val filteredContacts: StateFlow<List<ContactEntity>> = combine(
        rawContacts,
        _searchQuery
    ) { contacts, query ->
        if (query.isBlank()) {
            contacts.sortedBy { it.displayName.lowercase() }
        } else {
            val q = query.trim().lowercase()
            // Supports both text filter and T9 digit mapping
            val isDigitsOnly = q.all { it.isDigit() }
            contacts.filter { contact ->
                if (isDigitsOnly) {
                    val nameDigits = T9SearchEngine.convertNameToDigits(contact.displayName)
                    nameDigits.contains(q)
                } else {
                    contact.displayName.lowercase().contains(q) ||
                    (contact.company?.lowercase()?.contains(q) == true)
                }
            }.sortedBy { it.displayName.lowercase() }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val groupedContacts: StateFlow<Map<Char, List<ContactEntity>>> = filteredContacts.combine(
        MutableStateFlow(Unit)
    ) { contacts, _ ->
        contacts.groupBy { contact ->
            val first = contact.displayName.firstOrNull()?.uppercaseChar() ?: '#'
            if (first in 'A'..'Z') first else '#'
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    val numbersForSelectedContact: StateFlow<List<ContactNumberEntity>> = _selectedContactForDetail
        .flatMapLatest { contact ->
            if (contact != null) {
                contactDao.getNumbersForContact(contact.id)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            contactSyncRepository.syncWithLocalDb()
        }
        loadAccounts()
    }

    fun loadAccounts() {
        viewModelScope.launch {
            val accounts = contactSyncRepository.getAvailableAccounts()
            _availableAccounts.value = accounts
            val lastAccName = preferences.lastContactAccountName.first()
            val lastAccType = preferences.lastContactAccountType.first()

            val defaultAccount = accounts.firstOrNull { it.name == lastAccName && it.type == lastAccType }
                ?: accounts.firstOrNull { it.type == "com.google" }
                ?: accounts.firstOrNull { it.isDevice }
                ?: accounts.firstOrNull()

            _selectedContactAccount.value = defaultAccount
        }
    }

    fun selectContactAccount(account: ContactAccount) {
        _selectedContactAccount.value = account
        viewModelScope.launch {
            preferences.setLastContactAccount(
                name = if (!account.isDevice) account.name else null,
                type = account.type
            )
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onScrubberLetter(letter: Char) {
        _activeLetter.value = letter
    }

    fun selectContactForDetail(contact: ContactEntity?) {
        _selectedContactForDetail.value = contact
    }

    fun openCreateSheet() {
        _contactToEdit.value = null
        loadAccounts()
        _isCreateSheetOpen.value = true
    }

    fun openEditSheet(contact: ContactEntity) {
        _contactToEdit.value = contact
        _isCreateSheetOpen.value = true
    }

    fun dismissCreateOrEditSheet() {
        _isCreateSheetOpen.value = false
        _contactToEdit.value = null
    }

    fun saveContact(
        displayName: String,
        company: String?,
        numbers: List<Pair<String, String>>,
        emails: List<String>,
        preferredSimSlot: Int?,
        notes: String?,
        photoUri: String? = null,
        photoBytes: ByteArray? = null
    ) {
        viewModelScope.launch {
            val editing = _contactToEdit.value
            if (editing != null) {
                // Update existing
                val photoChanged = (photoUri != editing.photoUri) || (photoBytes != null)
                val updated = editing.copy(
                    displayName = displayName,
                    company = company,
                    preferredSimSlot = preferredSimSlot,
                    notes = notes,
                    photoUri = if (photoChanged) photoUri else editing.photoUri
                )
                contactSyncRepository.updateContact(
                    updated,
                    photoBytes = photoBytes,
                    hasPhotoChanged = photoChanged
                )
                _selectedContactForDetail.value = updated
            } else {
                // Create new
                val acc = _selectedContactAccount.value
                val accName = if (acc != null && !acc.isDevice) acc.name else null
                val accType = acc?.type
                contactSyncRepository.createContact(
                    displayName = displayName,
                    company = company,
                    numbers = numbers,
                    emails = emails,
                    preferredSimSlot = preferredSimSlot,
                    notes = notes,
                    accountName = accName,
                    accountType = accType,
                    photoUri = photoUri,
                    photoBytes = photoBytes
                )
            }
            dismissCreateOrEditSheet()
        }
    }

    fun toggleFavorite(contact: ContactEntity) {
        viewModelScope.launch {
            val updated = contact.copy(isFavorite = !contact.isFavorite)
            contactSyncRepository.updateContact(updated)
            if (_selectedContactForDetail.value?.id == contact.id) {
                _selectedContactForDetail.value = updated
            }
        }
    }

    fun toggleRuleOverride(contact: ContactEntity, override: Boolean) {
        viewModelScope.launch {
            val updated = contact.copy(alwaysRing = override)
            contactSyncRepository.updateContact(updated)
            if (_selectedContactForDetail.value?.id == contact.id) {
                _selectedContactForDetail.value = updated
            }
        }
    }

    fun deleteContact(contact: ContactEntity) {
        viewModelScope.launch {
            contactSyncRepository.deleteContact(contact.id, contact.androidContactId)
            if (_selectedContactForDetail.value?.id == contact.id) {
                _selectedContactForDetail.value = null
            }
        }
    }

    fun callContact(phoneNumber: String, preferredSimSlot: Int?) {
        val simAccounts = phoneAccountManager.registerPhoneAccounts()
        val simHandle = if (preferredSimSlot != null) {
            simAccounts.find { it.slotIndex == preferredSimSlot }?.phoneAccountHandle
                ?: simAccounts.firstOrNull()?.phoneAccountHandle
        } else {
            simAccounts.firstOrNull()?.phoneAccountHandle
        }
        callManager.placeCall(phoneNumber, simHandle)
    }

    fun setPreCallNote(contactId: Long?, phoneNumber: String, noteText: String) {
        viewModelScope.launch {
            callNoteDao.clearPreCallNotes(contactId, phoneNumber)
            if (noteText.isNotBlank()) {
                callNoteDao.insertNote(
                    CallNoteEntity(
                        contactId = contactId,
                        phoneNumber = phoneNumber,
                        noteText = noteText.trim(),
                        timestamp = System.currentTimeMillis(),
                        isPreCallNote = true
                    )
                )
            }
        }
    }

    fun clearPreCallNote(contactId: Long?, phoneNumber: String) {
        viewModelScope.launch {
            callNoteDao.clearPreCallNotes(contactId, phoneNumber)
        }
    }
}
