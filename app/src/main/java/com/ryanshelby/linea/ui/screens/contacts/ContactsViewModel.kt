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

import android.content.Context
import com.ryanshelby.linea.data.local.dao.CallRecordDao
import dagger.hilt.android.qualifiers.ApplicationContext

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ContactsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactDao: ContactDao,
    private val callRecordDao: CallRecordDao,
    private val contactSyncRepository: ContactSyncRepository,
    private val callManager: CallManager,
    private val phoneAccountManager: PhoneAccountManager,
    private val callNoteDao: CallNoteDao,
    private val preferences: LineaPreferences,
    val vaultSecurityManager: com.ryanshelby.linea.security.PrivateVaultSecurityManager,
    private val vaultExportImportManager: com.ryanshelby.linea.security.PrivateVaultExportImportManager
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
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val isVaultPinSet: StateFlow<Boolean> = preferences.isVaultPinSet
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val privateContacts: StateFlow<List<ContactEntity>> = contactDao.getPrivateContacts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val regularContacts: StateFlow<List<ContactEntity>> = contactDao.getAllContacts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _createAsPrivate = MutableStateFlow(false)
    val createAsPrivate: StateFlow<Boolean> = _createAsPrivate.asStateFlow()

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
        vaultSecurityManager.lockVault()
    }

    fun setContactPrivate(contactId: Long, isPrivate: Boolean) {
        if (isPrivate) {
            moveContactToPrivateSafe(contactId)
        } else {
            viewModelScope.launch {
                contactDao.setContactPrivate(contactId, false)
            }
        }
    }

    fun moveContactToPrivateSafe(contactId: Long) {
        viewModelScope.launch {
            val contact = contactDao.getContactById(contactId) ?: return@launch
            val sysId = contact.androidContactId
            if (sysId != null && sysId > 0) {
                contactSyncRepository.deleteContactFromSystemOnly(sysId)
            }
            contactDao.updateContact(
                contact.copy(
                    isPrivate = true,
                    androidContactId = null,
                    lookupKey = null
                )
            )

            // Mark past call logs as private
            val numbers = contactDao.getNumbersForContact(contactId).first()
            for (num in numbers) {
                val norm = num.number.filter { it.isDigit() }
                callRecordDao.markRecordsAsPrivate(num.number, norm)
                // Delete from Android system call log to protect privacy
                try {
                    context.contentResolver.delete(
                        android.provider.CallLog.Calls.CONTENT_URI,
                        "${android.provider.CallLog.Calls.NUMBER} = ? OR ${android.provider.CallLog.Calls.NUMBER} = ?",
                        arrayOf(num.number, norm)
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            contactSyncRepository.loadContacts()
        }
    }

    fun moveContactToPublicStorage(contactId: Long, accountName: String?, accountType: String?) {
        viewModelScope.launch {
            val contact = contactDao.getContactById(contactId) ?: return@launch
            val numbers = contactDao.getNumbersForContact(contactId).first()
            val emails = contactDao.getEmailsForContact(contactId).first()

            val newRawId = contactSyncRepository.createContactInSystem(
                displayName = contact.displayName,
                company = contact.company,
                numbers = numbers.map { it.number to it.label },
                emails = emails.map { it.email },
                notes = contact.notes,
                accountName = accountName,
                accountType = accountType
            )

            contactDao.updateContact(
                contact.copy(
                    isPrivate = false,
                    androidContactId = newRawId
                )
            )

            // Mark call logs as public again
            for (num in numbers) {
                val norm = num.number.filter { it.isDigit() }
                callRecordDao.markRecordsAsPublic(num.number, norm)
            }

            contactSyncRepository.loadContacts()
        }
    }

    suspend fun exportPrivateSafe(key: String): ByteArray {
        val dtos = vaultExportImportManager.getPrivateContactsForExport()
        return vaultExportImportManager.exportToEncryptedLineaBytes(dtos, key)
    }

    suspend fun importPrivateSafe(fileBytes: ByteArray, key: String): Result<Int> {
        val parseResult = vaultExportImportManager.decryptAndParseLineaBytes(fileBytes, key)
        return if (parseResult.isSuccess) {
            val contacts = parseResult.getOrThrow()
            val count = vaultExportImportManager.importContactsIntoVault(contacts)
            Result.success(count)
        } else {
            Result.failure(parseResult.exceptionOrNull() ?: Exception("Failed to decrypt .linea file"))
        }
    }

    suspend fun getNumbersForContactDirect(contactId: Long): List<ContactNumberEntity> {
        return contactDao.getNumbersForContact(contactId).first()
    }

    fun placeCallFromPrivateSafe(number: String, preferredSimSlot: Int? = null) {
        val simSlot = preferredSimSlot ?: 0
        val simAccounts = phoneAccountManager.registerPhoneAccounts()
        val simHandle = simAccounts.find { it.slotIndex == simSlot }?.phoneAccountHandle
            ?: simAccounts.firstOrNull()?.phoneAccountHandle
        callManager.placeCall(number, simHandle)
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

    val rawContacts: Flow<List<ContactEntity>> = contactDao.getAllContacts()

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

    fun openCreateSheet(isPrivate: Boolean = false) {
        _contactToEdit.value = null
        loadAccounts()
        _createAsPrivate.value = isPrivate
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
                if (_createAsPrivate.value) {
                    contactSyncRepository.createPrivateContact(
                        displayName = displayName,
                        company = company,
                        numbers = numbers,
                        emails = emails,
                        preferredSimSlot = preferredSimSlot,
                        notes = notes,
                        photoUri = photoUri
                    )
                    _createAsPrivate.value = false
                } else {
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
