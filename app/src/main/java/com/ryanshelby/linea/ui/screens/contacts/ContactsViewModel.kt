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
import com.ryanshelby.linea.qr_code.QrCodeEngine
import dagger.hilt.android.qualifiers.ApplicationContext

enum class ContactFilter(val label: String) {
    ALL("All contacts"),
    PHONE("Phone contacts"),
    EMAIL("Email contacts"),
    COMPANY("Company"),
    DUPLICATES("Duplicates"),
    INCOMPLETE("Incomplete"),
    FAVORITES("Favorites"),
    FIX_MANAGE("Fix & manage")
}

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
    private val vaultExportImportManager: com.ryanshelby.linea.security.PrivateVaultExportImportManager,
    val mergeFixRepository: com.ryanshelby.linea.mergefix.ContactMergeFixRepository,
    val importExportManager: com.ryanshelby.linea.data.contacts.ContactImportExportManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(ContactFilter.ALL)
    val selectedFilter: StateFlow<ContactFilter> = _selectedFilter.asStateFlow()

    val duplicateContactIds: StateFlow<Set<Long>> = mergeFixRepository.duplicateContactIds

    fun setFilter(filter: ContactFilter) {
        _selectedFilter.value = filter
    }

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

    sealed class ImportExportState {
        object Idle : ImportExportState()
        object Loading : ImportExportState()
        data class ExportSuccess(val count: Int, val format: com.ryanshelby.linea.data.contacts.ContactExportFormat) : ImportExportState()
        data class ImportSuccess(val summary: com.ryanshelby.linea.data.contacts.ContactImportSummary) : ImportExportState()
        data class Error(val message: String) : ImportExportState()
    }

    private val _importExportState = MutableStateFlow<ImportExportState>(ImportExportState.Idle)
    val importExportState: StateFlow<ImportExportState> = _importExportState.asStateFlow()

    fun resetImportExportState() {
        _importExportState.value = ImportExportState.Idle
    }

    fun exportContacts(
        uri: android.net.Uri,
        format: com.ryanshelby.linea.data.contacts.ContactExportFormat,
        scope: com.ryanshelby.linea.data.contacts.ExportScope,
        contentResolver: android.content.ContentResolver
    ) {
        viewModelScope.launch {
            _importExportState.value = ImportExportState.Loading
            try {
                val contacts = importExportManager.getExportableContacts(scope)
                val result = importExportManager.exportContactsToUri(uri, format, contacts, contentResolver)
                if (result.isSuccess) {
                    _importExportState.value = ImportExportState.ExportSuccess(result.getOrNull() ?: contacts.size, format)
                } else {
                    _importExportState.value = ImportExportState.Error(result.exceptionOrNull()?.message ?: "Export failed")
                }
            } catch (e: Exception) {
                _importExportState.value = ImportExportState.Error(e.message ?: "Export error")
            }
        }
    }

    fun importContacts(
        uri: android.net.Uri,
        format: com.ryanshelby.linea.data.contacts.ContactExportFormat?,
        policy: com.ryanshelby.linea.data.contacts.DuplicatePolicy,
        accountName: String?,
        accountType: String?,
        contentResolver: android.content.ContentResolver
    ) {
        viewModelScope.launch {
            _importExportState.value = ImportExportState.Loading
            try {
                val readResult = importExportManager.readContactsFromUri(uri, format, contentResolver)
                if (readResult.isFailure) {
                    _importExportState.value = ImportExportState.Error(readResult.exceptionOrNull()?.message ?: "Failed to read file")
                    return@launch
                }
                val contacts = readResult.getOrNull() ?: emptyList()
                val summary = importExportManager.importContacts(contacts, policy, accountName, accountType)
                _importExportState.value = ImportExportState.ImportSuccess(summary)
            } catch (e: Exception) {
                _importExportState.value = ImportExportState.Error(e.message ?: "Import error")
            }
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

    private val idsWithNumbers: Flow<List<Long>> = contactDao.getAllContactIdsWithNumbers()
    private val idsWithEmails: Flow<List<Long>> = contactDao.getAllContactIdsWithEmails()

    val filteredContacts: StateFlow<List<ContactEntity>> = combine(
        rawContacts,
        _searchQuery,
        _selectedFilter,
        idsWithNumbers,
        combine(idsWithEmails, duplicateContactIds) { ems, dups -> ems.toSet() to dups }
    ) { contacts, query, filter, hasNums, (hasEms, dupIds) ->
        val numsSet = hasNums.toSet()
        val baseFiltered = when (filter) {
            ContactFilter.ALL -> contacts
            ContactFilter.PHONE -> contacts.filter { it.id in numsSet }
            ContactFilter.EMAIL -> contacts.filter { it.id in hasEms }
            ContactFilter.COMPANY -> contacts.filter { !it.company.isNullOrBlank() }
            ContactFilter.DUPLICATES -> contacts.filter { it.id in dupIds }
            ContactFilter.INCOMPLETE -> contacts.filter { it.id !in numsSet || it.displayName.isBlank() }
            ContactFilter.FAVORITES -> contacts.filter { it.isFavorite }
            ContactFilter.FIX_MANAGE -> contacts
        }

        if (query.isBlank()) {
            baseFiltered.sortedBy { it.displayName.lowercase() }
        } else {
            val q = query.trim().lowercase()
            val isDigitsOnly = q.all { it.isDigit() }
            baseFiltered.filter { contact ->
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
            mergeFixRepository.refreshDuplicateIds()
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
                val saved = contactSyncRepository.updateContact(
                    contact = updated,
                    numbers = numbers,
                    emails = emails,
                    photoBytes = photoBytes,
                    hasPhotoChanged = photoChanged
                )
                _selectedContactForDetail.value = saved
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

    fun createContactFromQr(name: String, numbers: List<String>) {
        viewModelScope.launch {
            val acc = _selectedContactAccount.value
            val accName = if (acc != null && !acc.isDevice) acc.name else null
            val accType = acc?.type
            contactSyncRepository.createContact(
                displayName = name.ifEmpty { "Scanned Contact" },
                company = "",
                numbers = numbers.map { it to "Mobile" },
                emails = emptyList(),
                preferredSimSlot = null,
                notes = "Scanned via LiNEA QR",
                accountName = accName,
                accountType = accType,
                photoUri = null,
                photoBytes = null
            )
        }
    }

    fun createContactFromPayload(payload: QrCodeEngine.ContactPayload) {
        viewModelScope.launch {
            val acc = _selectedContactAccount.value
            val accName = if (acc != null && !acc.isDevice) acc.name else null
            val accType = acc?.type

            // Construct notes from social profiles
            val notesList = mutableListOf<String>()
            notesList.add("Shared via LiNEA Beam (NFC)")
            if (!payload.social1Platform.isNullOrBlank() && !payload.social1Handle.isNullOrBlank()) {
                notesList.add("${payload.social1Platform}: ${payload.social1Handle}")
            }
            if (!payload.social2Platform.isNullOrBlank() && !payload.social2Handle.isNullOrBlank()) {
                notesList.add("${payload.social2Platform}: ${payload.social2Handle}")
            }
            val notes = notesList.joinToString("\n")

            // Decode photo bytes if thumbnail base64 is present
            val photoBytes = payload.photoThumbnailBase64?.let { base64 ->
                try {
                    android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                } catch (_: Exception) { null }
            }

            contactSyncRepository.createContact(
                displayName = payload.name.ifEmpty { "Beamed Contact" },
                company = "",
                numbers = payload.numbers.map { it to "Mobile" },
                emails = payload.email?.takeIf { it.isNotBlank() }?.let { listOf(it) } ?: emptyList(),
                preferredSimSlot = null,
                notes = notes,
                accountName = accName,
                accountType = accType,
                photoUri = null,
                photoBytes = photoBytes
            )
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
        val simHandle = if (preferredSimSlot != null) {
            val simAccounts = phoneAccountManager.registerPhoneAccounts()
            simAccounts.find { it.slotIndex == preferredSimSlot }?.phoneAccountHandle
        } else {
            null
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
