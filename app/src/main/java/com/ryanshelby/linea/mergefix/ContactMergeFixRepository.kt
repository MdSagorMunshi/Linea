package com.ryanshelby.linea.mergefix

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.ryanshelby.linea.data.local.dao.ContactDao
import com.ryanshelby.linea.data.local.dao.MergeHistoryDao
import com.ryanshelby.linea.data.local.entities.ContactEntity
import com.ryanshelby.linea.data.local.entities.MergeHistoryEntity
import com.ryanshelby.linea.data.repository.ContactSyncRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactMergeFixRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactDao: ContactDao,
    private val mergeHistoryDao: MergeHistoryDao,
    private val mergeExecutor: ContactMergeExecutor,
    private val contactSyncRepository: ContactSyncRepository
) {

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    private val _duplicateContactIds = MutableStateFlow<Set<Long>>(emptySet())
    val duplicateContactIds: StateFlow<Set<Long>> = _duplicateContactIds.asStateFlow()

    private val _highConfidenceGroups = MutableStateFlow<List<MergeGroup>>(emptyList())
    val highConfidenceGroups: StateFlow<List<MergeGroup>> = _highConfidenceGroups.asStateFlow()

    private val _mediumConfidenceGroups = MutableStateFlow<List<MergeGroup>>(emptyList())
    val mediumConfidenceGroups: StateFlow<List<MergeGroup>> = _mediumConfidenceGroups.asStateFlow()

    private val _lowConfidenceGroups = MutableStateFlow<List<MergeGroup>>(emptyList())
    val lowConfidenceGroups: StateFlow<List<MergeGroup>> = _lowConfidenceGroups.asStateFlow()

    private val _unnamedContacts = MutableStateFlow<List<NormalizedContact>>(emptyList())
    val unnamedContacts: StateFlow<List<NormalizedContact>> = _unnamedContacts.asStateFlow()

    private val _malformedNameContacts = MutableStateFlow<List<NormalizedContact>>(emptyList())
    val malformedNameContacts: StateFlow<List<NormalizedContact>> = _malformedNameContacts.asStateFlow()

    private val _duplicateNumberIssues = MutableStateFlow<List<FixIssue.DuplicateNumberInRecord>>(emptyList())
    val duplicateNumberIssues: StateFlow<List<FixIssue.DuplicateNumberInRecord>> = _duplicateNumberIssues.asStateFlow()

    private val _unnormalizedNumberIssues = MutableStateFlow<List<FixIssue.UnnormalizedNumber>>(emptyList())
    val unnormalizedNumberIssues: StateFlow<List<FixIssue.UnnormalizedNumber>> = _unnormalizedNumberIssues.asStateFlow()

    private val _emptyContacts = MutableStateFlow<List<NormalizedContact>>(emptyList())
    val emptyContacts: StateFlow<List<NormalizedContact>> = _emptyContacts.asStateFlow()

    private val _dismissedGroupIds = MutableStateFlow<Set<String>>(emptySet())
    val dismissedGroupIds: StateFlow<Set<String>> = _dismissedGroupIds.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    val mergeHistory: Flow<List<MergeHistoryEntity>> = mergeHistoryDao.getAllMergeHistory()

    init {
        repositoryScope.launch {
            scanAll()
        }
    }

    suspend fun refreshDuplicateIds() = withContext(Dispatchers.IO) {
        scanAll()
    }

    /**
     * Executes full scan for duplicates and fix issues across public contacts.
     * Enforces hard rule: Vault contacts (isPrivate == true) are completely isolated.
     */
    suspend fun scanAll() = withContext(Dispatchers.IO) {
        _isScanning.value = true
        try {
            val publicContacts = contactDao.getAllPublicContactsOnce()
            val normalizedList = publicContacts.map { entity ->
                val numbers = contactDao.getNumbersForContact(entity.id).first()
                val emails = contactDao.getEmailsForContact(entity.id).first()
                NormalizedContact(
                    contact = entity,
                    normalizedDisplayName = ContactNormalizer.normalizeDisplayName(entity.displayName),
                    numbers = numbers,
                    normalizedNumbers = numbers.map { ContactNormalizer.normalizePhoneNumber(it.number) },
                    emails = emails,
                    normalizedEmails = emails.map { ContactNormalizer.normalizeEmail(it.email) }
                )
            }

            // Phase 1 & 2: Grouping by tiers
            val allGroups = ContactGroupingEngine.findMergeGroups(normalizedList)
            val dismissed = _dismissedGroupIds.value

            val activeGroups = allGroups.filter { !dismissed.contains(it.id) }
            val high = activeGroups.filter { it.confidence == ConfidenceTier.HIGH }
            val medium = activeGroups.filter { it.confidence == ConfidenceTier.MEDIUM }
            val low = activeGroups.filter { it.confidence == ConfidenceTier.LOW }

            _highConfidenceGroups.value = high
            _mediumConfidenceGroups.value = medium
            _lowConfidenceGroups.value = low

            val allDuplicateIds = activeGroups.flatMap { it.contacts.map { c -> c.id } }.toSet()
            _duplicateContactIds.value = allDuplicateIds

            // Phase 5: Non-duplicate fix issues
            val fixIssues = ContactFixEngine.scanForFixIssues(normalizedList)
            _unnamedContacts.value = fixIssues.filterIsInstance<FixIssue.Unnamed>().map { it.contact }
            _malformedNameContacts.value = fixIssues.filterIsInstance<FixIssue.MalformedName>().map { it.contact }
            _duplicateNumberIssues.value = fixIssues.filterIsInstance<FixIssue.DuplicateNumberInRecord>()
            _unnormalizedNumberIssues.value = fixIssues.filterIsInstance<FixIssue.UnnormalizedNumber>()
            _emptyContacts.value = fixIssues.filterIsInstance<FixIssue.EmptyContact>().map { it.contact }

            // Auto-trigger single duplicate number deduplication within record
            for (dupIssue in _duplicateNumberIssues.value) {
                deduplicateNumbersInRecordInternal(dupIssue.contact.id)
            }
        } finally {
            _isScanning.value = false
        }
    }

    /**
     * Executes merge for a group (user tap or background).
     */
    suspend fun mergeGroup(group: MergeGroup): Long = withContext(Dispatchers.IO) {
        val mergedId = mergeExecutor.executeMerge(group)
        scanAll()
        mergedId
    }

    /**
     * Unlinks a past merge using merge_history data.
     */
    suspend fun unlinkMerge(historyId: Long): Boolean = withContext(Dispatchers.IO) {
        val success = mergeExecutor.unlinkMerge(historyId)
        if (success) {
            scanAll()
        }
        success
    }

    fun dismissGroup(groupId: String) {
        _dismissedGroupIds.value = _dismissedGroupIds.value + groupId
        _highConfidenceGroups.value = _highConfidenceGroups.value.filter { it.id != groupId }
        _mediumConfidenceGroups.value = _mediumConfidenceGroups.value.filter { it.id != groupId }
        _lowConfidenceGroups.value = _lowConfidenceGroups.value.filter { it.id != groupId }
        _duplicateContactIds.value = (_highConfidenceGroups.value + _mediumConfidenceGroups.value + _lowConfidenceGroups.value)
            .flatMap { it.contacts.map { c -> c.id } }.toSet()
    }

    suspend fun renameContact(contactId: Long, newName: String) = withContext(Dispatchers.IO) {
        val contact = contactDao.getContactById(contactId) ?: return@withContext
        val updated = contact.copy(displayName = newName.trim())
        contactDao.updateContact(updated)
        contactSyncRepository.loadContacts()
        scanAll()
    }

    suspend fun normalizeNumber(contactId: Long, rawNumber: String, e164Number: String) = withContext(Dispatchers.IO) {
        val numbers = contactDao.getNumbersForContactOnce(contactId)
        val target = numbers.find { it.number == rawNumber } ?: return@withContext
        contactDao.deleteNumbersForContact(contactId)
        val updatedNumbers = numbers.map {
            if (it.id == target.id) it.copy(number = e164Number, normalizedNumber = e164Number)
            else it
        }
        contactDao.insertNumbers(updatedNumbers)
        contactSyncRepository.loadContacts()
        scanAll()
    }

    suspend fun normalizeAllNumbers() = withContext(Dispatchers.IO) {
        val issues = _unnormalizedNumberIssues.value
        for (issue in issues) {
            val contactId = issue.contact.id
            val numbers = contactDao.getNumbersForContactOnce(contactId)
            contactDao.deleteNumbersForContact(contactId)
            val updatedNumbers = numbers.map {
                if (it.number == issue.rawNumber) it.copy(number = issue.proposedE164, normalizedNumber = issue.proposedE164)
                else it
            }
            contactDao.insertNumbers(updatedNumbers)
        }
        contactSyncRepository.loadContacts()
        scanAll()
    }

    suspend fun deleteContact(contactId: Long) = withContext(Dispatchers.IO) {
        val contact = contactDao.getContactById(contactId)
        contactDao.deleteNumbersForContact(contactId)
        contactDao.deleteEmailsForContact(contactId)
        contactDao.removeContactFromAllGroups(contactId)
        contactDao.deleteContactById(contactId)
        if (contact?.androidContactId != null && contact.androidContactId > 0) {
            try {
                contactSyncRepository.deleteContactFromSystemOnly(contact.androidContactId)
            } catch (_: Exception) {}
        }
        contactSyncRepository.loadContacts()
        scanAll()
    }

    suspend fun deleteAllEmptyContacts() = withContext(Dispatchers.IO) {
        val empties = _emptyContacts.value
        for (c in empties) {
            deleteContact(c.id)
        }
        scanAll()
    }

    private suspend fun deduplicateNumbersInRecordInternal(contactId: Long) {
        val numbers = contactDao.getNumbersForContactOnce(contactId)
        val seen = mutableSetOf<String>()
        val distinct = mutableListOf<com.ryanshelby.linea.data.local.entities.ContactNumberEntity>()
        for (num in numbers) {
            val norm = num.normalizedNumber.ifBlank { ContactNormalizer.normalizePhoneNumber(num.number) }
            if (norm.isNotBlank() && seen.add(norm)) {
                distinct.add(num)
            }
        }
        if (distinct.size < numbers.size) {
            contactDao.deleteNumbersForContact(contactId)
            contactDao.insertNumbers(distinct)
        }
    }

    /**
     * Enqueues silent auto-merge in WorkManager for HIGH confidence groups.
     */
    fun triggerAutoMergeWork() {
        val request = OneTimeWorkRequestBuilder<AutoMergeWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            AutoMergeWorker.WORK_NAME_AUTO_MERGE,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}

