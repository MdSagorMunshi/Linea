package com.ryanshelby.linea.mergefix

import com.ryanshelby.linea.data.local.entities.ContactEmailEntity
import com.ryanshelby.linea.data.local.entities.ContactEntity
import com.ryanshelby.linea.data.local.entities.ContactNumberEntity

/**
 * Three confidence tiers for duplicate detection according to specification:
 * - HIGH: Exact match (identical normalized phone numbers or identical normalized names). Auto-merge candidate.
 * - MEDIUM: Fuzzy match (Levenshtein <= 2, Soundex / phonetic match, Nickname, or shared phone with slightly differing names). User confirmation required.
 * - LOW: Weak signal (fuzzy name match but NO shared phone and NO shared email). Review list only, never auto-merged.
 */
enum class ConfidenceTier {
    HIGH,
    MEDIUM,
    LOW
}

/**
 * In-memory normalized contact profile used for matching and field survivorship.
 */
data class NormalizedContact(
    val contact: ContactEntity,
    val normalizedDisplayName: String,
    val numbers: List<ContactNumberEntity>,
    val normalizedNumbers: List<String>,
    val emails: List<ContactEmailEntity>,
    val normalizedEmails: List<String>,
    val groupIds: List<Long> = emptyList()
) {
    val id: Long get() = contact.id
    val displayName: String get() = contact.displayName
    val isPrivate: Boolean get() = contact.isPrivate
    val preferredSimSlot: Int? get() = contact.preferredSimSlot
}

/**
 * A pair of matched contacts with confidence and explainability.
 */
data class MergeCandidatePair(
    val contactA: NormalizedContact,
    val contactB: NormalizedContact,
    val confidence: ConfidenceTier,
    val reason: String
) {
    val pairKey: String
        get() {
            val minId = minOf(contactA.id, contactB.id)
            val maxId = maxOf(contactA.id, contactB.id)
            return "${minId}_${maxId}"
        }
}

/**
 * Group of 2 or more contacts identified for merging.
 * A single contact belongs to only one merge group.
 */
data class MergeGroup(
    val id: String,
    val confidence: ConfidenceTier,
    val contacts: List<NormalizedContact>,
    val explanation: String
)

/**
 * Detailed preview of a merge operation before execution, allowing user selection
 * of the final display name and inspection of surviving fields.
 */
data class MergePreview(
    val group: MergeGroup,
    val selectedDisplayName: String,
    val candidateNames: List<String>,
    val numbersToKeep: List<ContactNumberEntity>,
    val emailsToKeep: List<ContactEmailEntity>,
    val notesToKeep: String?,
    val isFavorite: Boolean
)

/**
 * Non-duplicate issues detected on contacts.
 */
sealed class FixIssue {
    data class Unnamed(val contact: NormalizedContact) : FixIssue()
    data class MalformedName(val contact: NormalizedContact, val extractedNumber: String) : FixIssue()
    data class DuplicateNumberInRecord(val contact: NormalizedContact, val duplicateNumber: String) : FixIssue()
    data class UnnormalizedNumber(val contact: NormalizedContact, val rawNumber: String, val proposedE164: String) : FixIssue()
    data class EmptyContact(val contact: NormalizedContact) : FixIssue()
}

/**
 * Serialized DTO for storing original contact data in Room merge_history table.
 */
data class SavedSourceContactDto(
    val id: Long,
    val androidContactId: Long?,
    val lookupKey: String?,
    val displayName: String,
    val photoUri: String?,
    val isFavorite: Boolean,
    val isPinned: Boolean,
    val isPrivate: Boolean,
    val company: String?,
    val jobTitle: String?,
    val nickname: String?,
    val birthday: String?,
    val website: String?,
    val address: String?,
    val notes: String?,
    val customRingtoneUri: String?,
    val customVibrationPattern: String?,
    val preferredSimSlot: Int?,
    val allowDuringRestrictedHours: Boolean,
    val alwaysRing: Boolean,
    val numbers: List<SavedNumberDto>,
    val emails: List<SavedEmailDto>,
    val groupIds: List<Long> = emptyList()
)

data class SavedNumberDto(
    val number: String,
    val normalizedNumber: String,
    val label: String,
    val isPrimary: Boolean
)

data class SavedEmailDto(
    val email: String,
    val label: String
)

