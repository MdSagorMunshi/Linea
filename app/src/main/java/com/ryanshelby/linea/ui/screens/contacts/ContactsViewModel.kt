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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val contactDao: ContactDao,
    private val contactSyncRepository: ContactSyncRepository,
    private val callManager: CallManager,
    private val phoneAccountManager: PhoneAccountManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeLetter = MutableStateFlow<Char?>(null)
    val activeLetter: StateFlow<Char?> = _activeLetter.asStateFlow()

    private val _selectedContactForDetail = MutableStateFlow<ContactEntity?>(null)
    val selectedContactForDetail: StateFlow<ContactEntity?> = _selectedContactForDetail.asStateFlow()

    private val _isCreateSheetOpen = MutableStateFlow(false)
    val isCreateSheetOpen: StateFlow<Boolean> = _isCreateSheetOpen.asStateFlow()

    private val _contactToEdit = MutableStateFlow<ContactEntity?>(null)
    val contactToEdit: StateFlow<ContactEntity?> = _contactToEdit.asStateFlow()

    val pinnedFavorites: StateFlow<List<ContactEntity>> = contactDao.getFavoriteContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rawContacts = contactDao.getAllContacts()

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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groupedContacts: StateFlow<Map<Char, List<ContactEntity>>> = filteredContacts.combine(
        MutableStateFlow(Unit)
    ) { contacts, _ ->
        contacts.groupBy { contact ->
            val first = contact.displayName.firstOrNull()?.uppercaseChar() ?: '#'
            if (first in 'A'..'Z') first else '#'
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

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
        notes: String?
    ) {
        viewModelScope.launch {
            val editing = _contactToEdit.value
            if (editing != null) {
                // Update existing
                val updated = editing.copy(
                    displayName = displayName,
                    company = company,
                    preferredSimSlot = preferredSimSlot,
                    notes = notes
                )
                contactSyncRepository.updateContact(updated)
                _selectedContactForDetail.value = updated
            } else {
                // Create new
                contactSyncRepository.createContact(
                    displayName = displayName,
                    company = company,
                    numbers = numbers,
                    emails = emails,
                    preferredSimSlot = preferredSimSlot,
                    notes = notes
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
}
