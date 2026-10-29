package com.ryanshelby.linea.smartfilter

import com.ryanshelby.linea.data.local.entities.ContactEmailEntity
import com.ryanshelby.linea.data.local.entities.ContactEntity
import com.ryanshelby.linea.data.local.entities.ContactNumberEntity

enum class MatchType {
    EXACT_PHONE,
    NORMALIZED_PHONE,
    SHARED_NUMBER_DIFFERENT_NAME,
    SAME_EMAIL,
    HIGH_NAME_SIMILARITY,
    NAME_AND_ATTRIBUTE_MATCH
}

enum class CleanupType {
    ALL_CAPS,
    ALL_LOWERCASE,
    EXTRA_SPACES,
    PUNCTUATION_FORMAT
}

enum class MatchSensitivity {
    STRICT,      // 90%+ confidence
    BALANCED,    // 75%+ confidence
    AGGRESSIVE   // 60%+ confidence
}

enum class SmartFilterTab {
    OVERVIEW,
    DUPLICATES,
    CLEANUP,
    INCOMPLETE,
    DUPLICATE_NUMBERS
}

data class ContactProfile(
    val contact: ContactEntity,
    val numbers: List<ContactNumberEntity> = emptyList(),
    val emails: List<ContactEmailEntity> = emptyList(),
    val completenessScore: Int = 0
)

data class DuplicateMatch(
    val id: String,
    val contactA: ContactProfile,
    val contactB: ContactProfile,
    val confidence: Int, // 0-100
    val matchType: MatchType,
    val explanation: String,
    val matchedValues: List<String> = emptyList()
)

data class MergePreview(
    val primaryContact: ContactProfile,
    val secondaryContact: ContactProfile,
    val proposedDisplayName: String,
    val proposedCompany: String?,
    val proposedPhotoUri: String?,
    val mergedNumbers: List<Pair<String, String>>, // number, label
    val mergedEmails: List<String>,
    val mergedNotes: String?,
    val explanation: String
)

data class NameCleanupItem(
    val contactId: Long,
    val originalName: String,
    val proposedName: String,
    val cleanupType: CleanupType,
    val reason: String
)

data class IncompleteContactItem(
    val contact: ContactEntity,
    val missingAttributes: List<String>,
    val suggestions: List<String>
)

data class DuplicateNumberGroup(
    val normalizedNumber: String,
    val displayFormattedNumber: String,
    val contacts: List<ContactProfile>
)

data class QualityReport(
    val overallScore: Int, // 0-100
    val letterGrade: String, // A+, A, B, C, Needs Cleanup
    val totalContacts: Int,
    val duplicatePairsCount: Int,
    val nameCleanupsCount: Int,
    val incompleteContactsCount: Int,
    val duplicateNumbersCount: Int,
    val completenessScore: Int,
    val consistencyScore: Int,
    val hygieneScore: Int,
    val recommendations: List<String>
)

data class SmartFilterConfig(
    val sensitivity: MatchSensitivity = MatchSensitivity.BALANCED,
    val autoCapitalizeNames: Boolean = true,
    val normalizeInternationalNumbers: Boolean = true,
    val ignoreCaseInEmails: Boolean = true
)

data class UndoRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val title: String,
    val description: String,
    val originalProfiles: List<ContactProfile>,
    val createdContactIds: List<Long> = emptyList(),
    val modifiedContactIds: List<Long> = emptyList(),
    val deletedContactIds: List<Long> = emptyList()
)
