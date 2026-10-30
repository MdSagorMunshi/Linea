package com.ryanshelby.linea.mergefix

import com.ryanshelby.linea.data.local.dao.ContactDao
import com.ryanshelby.linea.data.local.dao.MergeHistoryDao
import com.ryanshelby.linea.data.local.entities.ContactEmailEntity
import com.ryanshelby.linea.data.local.entities.ContactEntity
import com.ryanshelby.linea.data.local.entities.ContactNumberEntity
import com.ryanshelby.linea.data.local.entities.MergeHistoryEntity
import com.ryanshelby.linea.data.repository.ContactSyncRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactMergeExecutor @Inject constructor(
    private val contactDao: ContactDao,
    private val mergeHistoryDao: MergeHistoryDao,
    private val contactSyncRepository: ContactSyncRepository
) {

    /**
     * Executes a merge of a MergeGroup according to field survivorship rules.
     * Before executing, records the full raw data of all source contacts in the merge_history table.
     * Allows user-chosen display name to override the default longest name.
     * Returns the ID of the surviving merged contact.
     */
    suspend fun executeMerge(group: MergeGroup, chosenDisplayName: String? = null): Long = withContext(Dispatchers.IO) {
        val contacts = group.contacts
        require(contacts.size >= 2) { "MergeGroup must contain at least 2 contacts" }

        // Pick the primary contact (the one with the longer/more complete display name, or lowest id)
        val sortedByCompleteness = contacts.sortedWith(
            compareByDescending<NormalizedContact> { it.displayName.length }
                .thenBy { it.id }
        )
        val primaryContact = sortedByCompleteness.first()
        val otherContacts = contacts.filter { it.id != primaryContact.id }

        // 1. Serialize all source contacts to JSON for merge_history
        val sourceContactsJson = serializeSourceContactsToJson(contacts)
        val sourceNames = contacts.map { it.displayName.ifBlank { "Unnamed" } }.joinToString(", ")

        // 2. Field Survivorship computation:
        // - Display name: user's chosen display name, or default longer/more complete value
        val survivingDisplayName = if (!chosenDisplayName.isNullOrBlank()) {
            chosenDisplayName.trim()
        } else {
            sortedByCompleteness.first().displayName.ifBlank {
                contacts.map { it.displayName }.firstOrNull { it.isNotBlank() } ?: "Unnamed Contact"
            }
        }

        // - Phone numbers: union all unique normalized numbers
        val seenNormalizedNumbers = mutableSetOf<String>()
        val survivingNumbers = mutableListOf<ContactNumberEntity>()
        for (c in contacts) {
            for (num in c.numbers) {
                val norm = if (num.normalizedNumber.isNotBlank()) num.normalizedNumber else ContactNormalizer.normalizePhoneNumber(num.number)
                if (norm.isNotBlank() && seenNormalizedNumbers.add(norm)) {
                    survivingNumbers.add(
                        num.copy(
                            id = 0,
                            contactId = primaryContact.id,
                            normalizedNumber = norm,
                            label = num.label.ifBlank { "Mobile" }
                        )
                    )
                }
            }
        }
        if (survivingNumbers.isNotEmpty()) {
            survivingNumbers[0] = survivingNumbers[0].copy(isPrimary = true)
        }

        // - Emails: union of unique emails
        val seenEmails = mutableSetOf<String>()
        val survivingEmails = mutableListOf<ContactEmailEntity>()
        for (c in contacts) {
            for (em in c.emails) {
                val normEmail = ContactNormalizer.normalizeEmail(em.email)
                if (normEmail.isNotBlank() && seenEmails.add(normEmail)) {
                    survivingEmails.add(
                        em.copy(
                            id = 0,
                            contactId = primaryContact.id,
                            email = normEmail,
                            label = em.label.ifBlank { "Work" }
                        )
                    )
                }
            }
        }

        // - Photo: manually set over auto-assigned over none
        val survivingPhoto = contacts.mapNotNull { it.contact.photoUri }.firstOrNull { it.isNotBlank() }

        // - Ringtone: custom over system default
        val survivingRingtone = contacts.mapNotNull { it.contact.customRingtoneUri }.firstOrNull { it.isNotBlank() }

        // - Starred: true if either source contact was starred
        val survivingStarred = contacts.any { it.contact.isFavorite }

        // - Pinned: true if either source contact was pinned
        val survivingPinned = contacts.any { it.contact.isPinned }

        // - Notes: concatenated with line separator if contacts have different note values
        val distinctNotes = contacts
            .mapNotNull { it.contact.notes?.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        val survivingNotes = if (distinctNotes.isEmpty()) null else distinctNotes.joinToString("\n")

        // - Additional fields: first non-blank
        val survivingCompany = contacts.mapNotNull { it.contact.company?.trim() }.firstOrNull { it.isNotBlank() }
        val survivingJobTitle = contacts.mapNotNull { it.contact.jobTitle?.trim() }.firstOrNull { it.isNotBlank() }
        val survivingNickname = contacts.mapNotNull { it.contact.nickname?.trim() }.firstOrNull { it.isNotBlank() }
        val survivingBirthday = contacts.mapNotNull { it.contact.birthday?.trim() }.firstOrNull { it.isNotBlank() }
        val survivingWebsite = contacts.mapNotNull { it.contact.website?.trim() }.firstOrNull { it.isNotBlank() }
        val survivingAddress = contacts.mapNotNull { it.contact.address?.trim() }.firstOrNull { it.isNotBlank() }
        val survivingSimSlot = contacts.mapNotNull { it.contact.preferredSimSlot }.firstOrNull()

        // 3. Write merge history record BEFORE executing database mutations
        val historyId = mergeHistoryDao.insertMergeHistory(
            MergeHistoryEntity(
                mergedContactId = primaryContact.id,
                mergedContactName = survivingDisplayName,
                timestamp = System.currentTimeMillis(),
                sourceContactsCount = contacts.size,
                sourceContactNames = sourceNames,
                sourceContactsJson = sourceContactsJson
            )
        )

        // 4. Update the primary contact entity
        val updatedPrimaryEntity = primaryContact.contact.copy(
            displayName = survivingDisplayName,
            photoUri = survivingPhoto,
            isFavorite = survivingStarred,
            isPinned = survivingPinned,
            company = survivingCompany,
            jobTitle = survivingJobTitle,
            nickname = survivingNickname,
            birthday = survivingBirthday,
            website = survivingWebsite,
            address = survivingAddress,
            notes = survivingNotes,
            customRingtoneUri = survivingRingtone,
            preferredSimSlot = survivingSimSlot
        )
        contactDao.updateContact(updatedPrimaryEntity)

        // Replace numbers and emails for primary contact
        contactDao.deleteNumbersForContact(primaryContact.id)
        if (survivingNumbers.isNotEmpty()) {
            contactDao.insertNumbers(survivingNumbers)
        }
        contactDao.deleteEmailsForContact(primaryContact.id)
        if (survivingEmails.isNotEmpty()) {
            contactDao.insertEmails(survivingEmails)
        }

        // 5. Delete other contacts from Room and system ContactsContract
        for (other in otherContacts) {
            contactDao.deleteNumbersForContact(other.id)
            contactDao.deleteEmailsForContact(other.id)
            contactDao.removeContactFromAllGroups(other.id)
            contactDao.deleteContactById(other.id)

            // Delete from system ContactsContract if synced
            val sysId = other.contact.androidContactId
            if (sysId != null && sysId > 0) {
                try {
                    contactSyncRepository.deleteContactFromSystemOnly(sysId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Trigger synchronization refresh
        contactSyncRepository.loadContacts()

        primaryContact.id
    }

    /**
     * Unlinks a past merge:
     * - Deletes the merged contact record.
     * - Recreates all original source contacts exactly as they were before the merge using the data stored in merge_history.
     * - Deletes the history entry.
     */
    suspend fun unlinkMerge(historyId: Long): Boolean = withContext(Dispatchers.IO) {
        val entry = mergeHistoryDao.getMergeHistoryById(historyId) ?: return@withContext false
        val sourceContacts = parseSourceContactsFromJson(entry.sourceContactsJson)
        if (sourceContacts.isEmpty()) return@withContext false

        // 1. Delete the merged contact record
        contactDao.deleteNumbersForContact(entry.mergedContactId)
        contactDao.deleteEmailsForContact(entry.mergedContactId)
        contactDao.removeContactFromAllGroups(entry.mergedContactId)
        contactDao.deleteContactById(entry.mergedContactId)

        // 2. Recreate all original source contacts exactly as they were
        for (dto in sourceContacts) {
            val contactEntity = ContactEntity(
                id = 0, // Generate new clean id in Room
                androidContactId = null,
                lookupKey = dto.lookupKey,
                displayName = dto.displayName,
                photoUri = dto.photoUri,
                isFavorite = dto.isFavorite,
                isPinned = dto.isPinned,
                isPrivate = dto.isPrivate,
                company = dto.company,
                jobTitle = dto.jobTitle,
                nickname = dto.nickname,
                birthday = dto.birthday,
                website = dto.website,
                address = dto.address,
                notes = dto.notes,
                customRingtoneUri = dto.customRingtoneUri,
                customVibrationPattern = dto.customVibrationPattern,
                preferredSimSlot = dto.preferredSimSlot,
                allowDuringRestrictedHours = dto.allowDuringRestrictedHours,
                alwaysRing = dto.alwaysRing
            )
            val newContactId = contactDao.insertContact(contactEntity)

            val numberEntities = dto.numbers.map {
                ContactNumberEntity(
                    contactId = newContactId,
                    number = it.number,
                    normalizedNumber = it.normalizedNumber,
                    label = it.label,
                    isPrimary = it.isPrimary
                )
            }
            if (numberEntities.isNotEmpty()) {
                contactDao.insertNumbers(numberEntities)
            }

            val emailEntities = dto.emails.map {
                ContactEmailEntity(
                    contactId = newContactId,
                    email = it.email,
                    label = it.label
                )
            }
            if (emailEntities.isNotEmpty()) {
                contactDao.insertEmails(emailEntities)
            }
        }

        // 3. Delete the history entry
        mergeHistoryDao.deleteMergeHistoryById(historyId)

        // 4. Refresh contact synchronization
        contactSyncRepository.loadContacts()

        true
    }

    private fun serializeSourceContactsToJson(contacts: List<NormalizedContact>): String {
        val array = JSONArray()
        for (c in contacts) {
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("androidContactId", c.contact.androidContactId ?: JSONObject.NULL)
            obj.put("lookupKey", c.contact.lookupKey ?: JSONObject.NULL)
            obj.put("displayName", c.contact.displayName)
            obj.put("photoUri", c.contact.photoUri ?: JSONObject.NULL)
            obj.put("isFavorite", c.contact.isFavorite)
            obj.put("isPinned", c.contact.isPinned)
            obj.put("isPrivate", c.contact.isPrivate)
            obj.put("company", c.contact.company ?: JSONObject.NULL)
            obj.put("jobTitle", c.contact.jobTitle ?: JSONObject.NULL)
            obj.put("nickname", c.contact.nickname ?: JSONObject.NULL)
            obj.put("birthday", c.contact.birthday ?: JSONObject.NULL)
            obj.put("website", c.contact.website ?: JSONObject.NULL)
            obj.put("address", c.contact.address ?: JSONObject.NULL)
            obj.put("notes", c.contact.notes ?: JSONObject.NULL)
            obj.put("customRingtoneUri", c.contact.customRingtoneUri ?: JSONObject.NULL)
            obj.put("preferredSimSlot", c.contact.preferredSimSlot ?: JSONObject.NULL)

            val numArr = JSONArray()
            for (num in c.numbers) {
                val numObj = JSONObject()
                numObj.put("number", num.number)
                numObj.put("normalizedNumber", num.normalizedNumber)
                numObj.put("label", num.label)
                numObj.put("isPrimary", num.isPrimary)
                numArr.put(numObj)
            }
            obj.put("numbers", numArr)

            val emArr = JSONArray()
            for (em in c.emails) {
                val emObj = JSONObject()
                emObj.put("email", em.email)
                emObj.put("label", em.label)
                emArr.put(emObj)
            }
            obj.put("emails", emArr)

            array.put(obj)
        }
        return array.toString()
    }

    private fun parseSourceContactsFromJson(jsonStr: String): List<SavedSourceContactDto> {
        val list = mutableListOf<SavedSourceContactDto>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val numArr = obj.optJSONArray("numbers") ?: JSONArray()
                val numbers = mutableListOf<SavedNumberDto>()
                for (j in 0 until numArr.length()) {
                    val nObj = numArr.getJSONObject(j)
                    numbers.add(
                        SavedNumberDto(
                            number = nObj.optString("number", ""),
                            normalizedNumber = nObj.optString("normalizedNumber", ""),
                            label = nObj.optString("label", "Mobile"),
                            isPrimary = nObj.optBoolean("isPrimary", false)
                        )
                    )
                }

                val emArr = obj.optJSONArray("emails") ?: JSONArray()
                val emails = mutableListOf<SavedEmailDto>()
                for (j in 0 until emArr.length()) {
                    val eObj = emArr.getJSONObject(j)
                    emails.add(
                        SavedEmailDto(
                            email = eObj.optString("email", ""),
                            label = eObj.optString("label", "Work")
                        )
                    )
                }

                list.add(
                    SavedSourceContactDto(
                        id = obj.optLong("id", 0),
                        androidContactId = if (obj.isNull("androidContactId")) null else obj.optLong("androidContactId"),
                        lookupKey = if (obj.isNull("lookupKey")) null else obj.optString("lookupKey"),
                        displayName = obj.optString("displayName", ""),
                        photoUri = if (obj.isNull("photoUri")) null else obj.optString("photoUri"),
                        isFavorite = obj.optBoolean("isFavorite", false),
                        isPinned = obj.optBoolean("isPinned", false),
                        isPrivate = obj.optBoolean("isPrivate", false),
                        company = if (obj.isNull("company")) null else obj.optString("company"),
                        jobTitle = if (obj.isNull("jobTitle")) null else obj.optString("jobTitle"),
                        nickname = if (obj.isNull("nickname")) null else obj.optString("nickname"),
                        birthday = if (obj.isNull("birthday")) null else obj.optString("birthday"),
                        website = if (obj.isNull("website")) null else obj.optString("website"),
                        address = if (obj.isNull("address")) null else obj.optString("address"),
                        notes = if (obj.isNull("notes")) null else obj.optString("notes"),
                        customRingtoneUri = if (obj.isNull("customRingtoneUri")) null else obj.optString("customRingtoneUri"),
                        customVibrationPattern = null,
                        preferredSimSlot = if (obj.isNull("preferredSimSlot")) null else obj.optInt("preferredSimSlot"),
                        allowDuringRestrictedHours = false,
                        alwaysRing = false,
                        numbers = numbers,
                        emails = emails
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun computePreview(group: MergeGroup, chosenDisplayName: String? = null): MergePreview =
        Companion.computePreview(group, chosenDisplayName)

    companion object {
        /**
         * Computes a preview of the merged contact without mutating the database.
         * Lets the user inspect the resulting fields and select which name to keep.
         */
        fun computePreview(group: MergeGroup, chosenDisplayName: String? = null): MergePreview {
            val contacts = group.contacts
            val candidateNames = contacts
                .map { it.displayName.trim() }
                .filter { it.isNotBlank() }
                .distinct()
            val defaultName = if (!chosenDisplayName.isNullOrBlank()) {
                chosenDisplayName.trim()
            } else {
                candidateNames.maxByOrNull { it.length } ?: "Unnamed Contact"
            }

            val seenNormalizedNumbers = mutableSetOf<String>()
            val numbersToKeep = mutableListOf<ContactNumberEntity>()
            for (c in contacts) {
                for (num in c.numbers) {
                    val norm = if (num.normalizedNumber.isNotBlank()) num.normalizedNumber else ContactNormalizer.normalizePhoneNumber(num.number)
                    if (norm.isNotBlank() && seenNormalizedNumbers.add(norm)) {
                        numbersToKeep.add(num.copy(label = num.label.ifBlank { "Mobile" }))
                    }
                }
            }

            val seenEmails = mutableSetOf<String>()
            val emailsToKeep = mutableListOf<ContactEmailEntity>()
            for (c in contacts) {
                for (em in c.emails) {
                    val normEmail = ContactNormalizer.normalizeEmail(em.email)
                    if (normEmail.isNotBlank() && seenEmails.add(normEmail)) {
                        emailsToKeep.add(em.copy(label = em.label.ifBlank { "Work" }))
                    }
                }
            }

            val notesToKeep = contacts
                .mapNotNull { it.contact.notes?.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString("\n")
                .ifBlank { null }

            val isFavorite = contacts.any { it.contact.isFavorite }

            return MergePreview(
                group = group,
                selectedDisplayName = defaultName,
                candidateNames = candidateNames,
                numbersToKeep = numbersToKeep,
                emailsToKeep = emailsToKeep,
                notesToKeep = notesToKeep,
                isFavorite = isFavorite
            )
        }
    }
}

