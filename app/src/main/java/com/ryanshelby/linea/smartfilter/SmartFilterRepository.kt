package com.ryanshelby.linea.smartfilter

import android.content.Context
import android.util.Log
import com.ryanshelby.linea.data.local.dao.CallRecordDao
import com.ryanshelby.linea.data.local.dao.ContactDao
import com.ryanshelby.linea.data.local.entities.ContactEmailEntity
import com.ryanshelby.linea.data.local.entities.ContactEntity
import com.ryanshelby.linea.data.local.entities.ContactNumberEntity
import com.ryanshelby.linea.data.repository.ContactSyncRepository
import com.ryanshelby.linea.telecom.ContactLookupHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartFilterRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactDao: ContactDao,
    private val callRecordDao: CallRecordDao,
    private val contactSyncRepository: ContactSyncRepository,
    private val contactLookupHelper: ContactLookupHelper,
    val undoManager: SmartFilterUndoManager
) {

    private val _duplicateContactIds = kotlinx.coroutines.flow.MutableStateFlow<Set<Long>>(emptySet())
    val duplicateContactIds: kotlinx.coroutines.flow.StateFlow<Set<Long>> = _duplicateContactIds

    private val _dismissedDuplicatePairIds = kotlinx.coroutines.flow.MutableStateFlow<Set<String>>(emptySet())
    val dismissedDuplicatePairIds: kotlinx.coroutines.flow.StateFlow<Set<String>> = _dismissedDuplicatePairIds

    private val _dismissedCleanupContactIds = kotlinx.coroutines.flow.MutableStateFlow<Set<Long>>(emptySet())
    val dismissedCleanupContactIds: kotlinx.coroutines.flow.StateFlow<Set<Long>> = _dismissedCleanupContactIds

    private val _dismissedIncompleteContactIds = kotlinx.coroutines.flow.MutableStateFlow<Set<Long>>(emptySet())
    val dismissedIncompleteContactIds: kotlinx.coroutines.flow.StateFlow<Set<Long>> = _dismissedIncompleteContactIds

    fun dismissDuplicate(matchId: String) {
        _dismissedDuplicatePairIds.value = _dismissedDuplicatePairIds.value + matchId
    }

    fun dismissCleanup(contactId: Long) {
        _dismissedCleanupContactIds.value = _dismissedCleanupContactIds.value + contactId
    }

    fun dismissIncomplete(contactId: Long) {
        _dismissedIncompleteContactIds.value = _dismissedIncompleteContactIds.value + contactId
    }

    fun setDuplicateContactIds(ids: Set<Long>) {
        _duplicateContactIds.value = ids
    }

    suspend fun refreshDuplicateIds(config: SmartFilterConfig = SmartFilterConfig()) = withContext(Dispatchers.IO) {
        val profiles = loadAllProfiles()
        val matches = SmartFilterEngine.findDuplicateMatches(profiles, config)
        val ids = matches.flatMap { listOf(it.contactA.contact.id, it.contactB.contact.id) }.toSet()
        _duplicateContactIds.value = ids
    }

    suspend fun loadAllProfiles(): List<ContactProfile> = withContext(Dispatchers.IO) {
        val contacts = contactDao.getAllPublicContactsOnce()
        contacts.map { contact ->
            val numbers = contactDao.getNumbersForContactOnce(contact.id)
            val emails = contactDao.getEmailsForContactOnce(contact.id)
            val profile = ContactProfile(
                contact = contact,
                numbers = numbers,
                emails = emails
            )
            profile.copy(completenessScore = SmartFilterEngine.calculateCompletenessScore(profile))
        }
    }

    suspend fun executeMerge(
        primary: ContactProfile,
        secondary: ContactProfile,
        preview: MergePreview
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. Record snapshot for undo
            val undoRecord = UndoRecord(
                title = "Merged contacts into ${preview.proposedDisplayName}",
                description = "Merged '${secondary.contact.displayName}' into '${primary.contact.displayName}'",
                originalProfiles = listOf(primary, secondary),
                modifiedContactIds = listOf(primary.contact.id),
                deletedContactIds = listOf(secondary.contact.id)
            )
            undoManager.pushUndo(undoRecord)

            // 2. Update primary contact in Room
            val updatedPrimary = primary.contact.copy(
                displayName = preview.proposedDisplayName,
                company = preview.proposedCompany,
                photoUri = preview.proposedPhotoUri,
                notes = preview.mergedNotes
            )
            contactDao.updateContact(updatedPrimary)

            // 3. Update numbers for primary contact
            contactDao.deleteNumbersForContact(primary.contact.id)
            val numberEntities = preview.mergedNumbers.mapIndexed { idx, (num, label) ->
                ContactNumberEntity(
                    contactId = primary.contact.id,
                    number = num,
                    normalizedNumber = num.filter { it.isDigit() || it == '+' },
                    label = label,
                    isPrimary = idx == 0
                )
            }
            contactDao.insertNumbers(numberEntities)

            // 4. Update emails for primary contact
            contactDao.deleteEmailsForContact(primary.contact.id)
            val emailEntities = preview.mergedEmails.map { email ->
                ContactEmailEntity(
                    contactId = primary.contact.id,
                    email = email,
                    label = "Work"
                )
            }
            if (emailEntities.isNotEmpty()) {
                contactDao.insertEmails(emailEntities)
            }

            // 5. Delete secondary contact from Room & system
            contactSyncRepository.deleteContact(secondary.contact.id, secondary.contact.androidContactId)

            // 6. Update system ContactsContract for primary if linked
            contactSyncRepository.updateContact(
                contact = updatedPrimary,
                numbers = preview.mergedNumbers,
                emails = preview.mergedEmails,
                hasPhotoChanged = false
            )

            // 7. Refresh caches and update call logs with primary name
            contactLookupHelper.clearCache()
            for ((num, _) in preview.mergedNumbers) {
                val norm = num.filter { it.isDigit() }
                callRecordDao.updateCallerNameForNumber(num, norm, preview.proposedDisplayName)
            }
            contactSyncRepository.loadContacts()

            true
        } catch (e: Exception) {
            Log.e("SmartFilterRepo", "Failed to merge contacts", e)
            false
        }
    }

    suspend fun executeBulkMerge(matches: List<DuplicateMatch>): Int = withContext(Dispatchers.IO) {
        var mergedCount = 0
        val handledContactIds = mutableSetOf<Long>()

        for (match in matches) {
            val idA = match.contactA.contact.id
            val idB = match.contactB.contact.id
            if (handledContactIds.contains(idA) || handledContactIds.contains(idB)) {
                continue
            }

            val preview = SmartFilterEngine.generateMergePreview(match.contactA, match.contactB)
            val success = executeMerge(match.contactA, match.contactB, preview)
            if (success) {
                handledContactIds.add(idA)
                handledContactIds.add(idB)
                mergedCount++
            }
        }
        mergedCount
    }

    suspend fun cleanContactName(item: NameCleanupItem, profile: ContactProfile): Boolean = withContext(Dispatchers.IO) {
        try {
            // Snapshot for undo
            val undoRecord = UndoRecord(
                title = "Cleaned name to ${item.proposedName}",
                description = "Renamed '${item.originalName}' to '${item.proposedName}' (${item.reason})",
                originalProfiles = listOf(profile),
                modifiedContactIds = listOf(profile.contact.id)
            )
            undoManager.pushUndo(undoRecord)

            val updatedContact = profile.contact.copy(displayName = item.proposedName)
            contactDao.updateContact(updatedContact)

            val numbers = profile.numbers.map { it.number to it.label }
            val emails = profile.emails.map { it.email }
            contactSyncRepository.updateContact(
                contact = updatedContact,
                numbers = numbers,
                emails = emails,
                hasPhotoChanged = false
            )

            contactLookupHelper.clearCache()
            for ((num, _) in numbers) {
                val norm = num.filter { it.isDigit() }
                callRecordDao.updateCallerNameForNumber(num, norm, item.proposedName)
            }
            contactSyncRepository.loadContacts()

            true
        } catch (e: Exception) {
            Log.e("SmartFilterRepo", "Failed to clean contact name", e)
            false
        }
    }

    suspend fun cleanAllContactNames(items: List<NameCleanupItem>, profilesMap: Map<Long, ContactProfile>): Int = withContext(Dispatchers.IO) {
        var count = 0
        for (item in items) {
            val profile = profilesMap[item.contactId] ?: continue
            val success = cleanContactName(item, profile)
            if (success) count++
        }
        count
    }

    suspend fun undoLastAction(): Boolean = withContext(Dispatchers.IO) {
        val record = undoManager.popUndo() ?: return@withContext false
        try {
            for (original in record.originalProfiles) {
                // Re-insert or update contact
                val existing = contactDao.getContactById(original.contact.id)
                val targetId = if (existing != null) {
                    contactDao.updateContact(original.contact)
                    original.contact.id
                } else {
                    contactDao.insertContact(original.contact)
                }

                // Restore numbers
                contactDao.deleteNumbersForContact(targetId)
                if (original.numbers.isNotEmpty()) {
                    val numbersToInsert = original.numbers.map { it.copy(contactId = targetId) }
                    contactDao.insertNumbers(numbersToInsert)
                }

                // Restore emails
                contactDao.deleteEmailsForContact(targetId)
                if (original.emails.isNotEmpty()) {
                    val emailsToInsert = original.emails.map { it.copy(contactId = targetId) }
                    contactDao.insertEmails(emailsToInsert)
                }

                // Sync back to Android ContactsContract
                val numbers = original.numbers.map { it.number to it.label }
                val emails = original.emails.map { it.email }
                if (existing != null && original.contact.androidContactId != null) {
                    contactSyncRepository.updateContact(
                        contact = original.contact,
                        numbers = numbers,
                        emails = emails,
                        hasPhotoChanged = false
                    )
                } else if (existing == null) {
                    // Recreated contact that was removed during merge
                    val newAndroidId = contactSyncRepository.createContact(
                        displayName = original.contact.displayName,
                        company = original.contact.company,
                        numbers = numbers,
                        emails = emails,
                        notes = original.contact.notes
                    )
                    if (newAndroidId > 0L) {
                        contactDao.updateContact(original.contact.copy(id = targetId, androidContactId = newAndroidId))
                    }
                }

                // Update call records and caches
                for (num in original.numbers) {
                    val norm = num.number.filter { it.isDigit() }
                    callRecordDao.updateCallerNameForNumber(num.number, norm, original.contact.displayName)
                }
            }

            contactLookupHelper.clearCache()
            contactSyncRepository.loadContacts()
            true
        } catch (e: Exception) {
            Log.e("SmartFilterRepo", "Error performing undo", e)
            false
        }
    }
}
