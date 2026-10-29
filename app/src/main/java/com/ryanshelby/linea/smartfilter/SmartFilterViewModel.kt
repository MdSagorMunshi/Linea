package com.ryanshelby.linea.smartfilter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SmartFilterUiState(
    val isScanning: Boolean = false,
    val currentTab: SmartFilterTab = SmartFilterTab.OVERVIEW,
    val profiles: List<ContactProfile> = emptyList(),
    val duplicateMatches: List<DuplicateMatch> = emptyList(),
    val nameCleanups: List<NameCleanupItem> = emptyList(),
    val duplicateNumbers: List<DuplicateNumberGroup> = emptyList(),
    val incompleteContacts: List<IncompleteContactItem> = emptyList(),
    val qualityReport: QualityReport? = null,
    val config: SmartFilterConfig = SmartFilterConfig(),
    val activeMergePreview: MergePreview? = null,
    val showRulesSheet: Boolean = false,
    val showUndoSheet: Boolean = false,
    val userMessage: String? = null
)

@HiltViewModel
class SmartFilterViewModel @Inject constructor(
    private val repository: SmartFilterRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SmartFilterUiState())
    val uiState: StateFlow<SmartFilterUiState> = _uiState.asStateFlow()

    val canUndo: StateFlow<Boolean> = repository.undoManager.canUndo

    init {
        scanContacts()
    }

    fun scanContacts() {
        _uiState.value = _uiState.value.copy(isScanning = true)
        viewModelScope.launch {
            val profiles = repository.loadAllProfiles()
            val config = _uiState.value.config

            withContext(Dispatchers.Default) {
                // 1. Detect duplicates
                val rawDuplicates = SmartFilterEngine.findDuplicateMatches(profiles, config)
                val dismissedDups = repository.dismissedDuplicatePairIds.value
                val duplicates = rawDuplicates.filter { !dismissedDups.contains(it.id) }
                repository.setDuplicateContactIds(duplicates.flatMap { listOf(it.contactA.contact.id, it.contactB.contact.id) }.toSet())

                // 2. Evaluate name cleanup
                val dismissedClean = repository.dismissedCleanupContactIds.value
                val cleanups = profiles.mapNotNull { profile ->
                    if (dismissedClean.contains(profile.contact.id)) null
                    else {
                        val cleanup = SmartFilterEngine.evaluateNameCleanup(profile.contact.displayName)
                        cleanup?.copy(contactId = profile.contact.id)
                    }
                }

                // 3. Find duplicate numbers across contacts
                val duplicateNumbers = SmartFilterEngine.findDuplicateNumbersAcrossContacts(profiles)

                // 4. Incomplete contacts
                val dismissedInc = repository.dismissedIncompleteContactIds.value
                val incomplete = profiles.filter { it.completenessScore < 60 && !dismissedInc.contains(it.contact.id) }.map { profile ->
                    val missing = mutableListOf<String>()
                    val suggestions = mutableListOf<String>()
                    if (profile.numbers.isEmpty()) {
                        missing.add("No phone number")
                        suggestions.add("Add a mobile or work number")
                    }
                    if (profile.emails.isEmpty()) {
                        missing.add("No email")
                        suggestions.add("Add an email address")
                    }
                    if (!profile.contact.displayName.contains(" ")) {
                        missing.add("Missing last name")
                        suggestions.add("Add a full name for better identification")
                    }
                    if (profile.contact.photoUri.isNullOrBlank()) {
                        missing.add("No photo")
                        suggestions.add("Assign a contact photo or avatar")
                    }
                    IncompleteContactItem(
                        contact = profile.contact,
                        missingAttributes = missing,
                        suggestions = suggestions
                    )
                }

                // 5. Evaluate quality report
                val qualityReport = SmartFilterEngine.evaluateAddressBookQuality(
                    profiles = profiles,
                    duplicateMatches = duplicates,
                    cleanupItems = cleanups,
                    duplicateNumberGroups = duplicateNumbers
                )

                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    profiles = profiles,
                    duplicateMatches = duplicates,
                    nameCleanups = cleanups,
                    duplicateNumbers = duplicateNumbers,
                    incompleteContacts = incomplete,
                    qualityReport = qualityReport
                )
            }
        }
    }

    fun setTab(tab: SmartFilterTab) {
        _uiState.value = _uiState.value.copy(currentTab = tab)
    }

    fun openMergePreview(match: DuplicateMatch) {
        val preview = SmartFilterEngine.generateMergePreview(match.contactA, match.contactB)
        _uiState.value = _uiState.value.copy(activeMergePreview = preview)
    }

    fun dismissMergePreview() {
        _uiState.value = _uiState.value.copy(activeMergePreview = null)
    }

    fun confirmMerge(preview: MergePreview) {
        viewModelScope.launch {
            val success = repository.executeMerge(preview.primaryContact, preview.secondaryContact, preview)
            if (success) {
                _uiState.value = _uiState.value.copy(
                    activeMergePreview = null,
                    userMessage = "Merged into '${preview.proposedDisplayName}'"
                )
                scanContacts()
            } else {
                _uiState.value = _uiState.value.copy(
                    userMessage = "Failed to merge contacts"
                )
            }
        }
    }

    fun dismissDuplicate(match: DuplicateMatch) {
        repository.dismissDuplicate(match.id)
        scanContacts()
    }

    fun dismissCleanup(item: NameCleanupItem) {
        repository.dismissCleanup(item.contactId)
        scanContacts()
    }

    fun dismissIncomplete(item: IncompleteContactItem) {
        repository.dismissIncomplete(item.contact.id)
        scanContacts()
    }

    fun bulkMergeAll() {
        val matches = _uiState.value.duplicateMatches
        if (matches.isEmpty()) {
            _uiState.value = _uiState.value.copy(userMessage = "No duplicates found to merge")
            return
        }

        viewModelScope.launch {
            val count = repository.executeBulkMerge(matches)
            _uiState.value = _uiState.value.copy(
                userMessage = "Successfully merged $count duplicate contact(s)"
            )
            scanContacts()
        }
    }

    fun bulkMergeAllHighConfidence() {
        val matches = _uiState.value.duplicateMatches.filter { it.confidence >= 90 }
        if (matches.isEmpty()) {
            _uiState.value = _uiState.value.copy(userMessage = "No high-confidence duplicates found to merge")
            return
        }

        viewModelScope.launch {
            val count = repository.executeBulkMerge(matches)
            _uiState.value = _uiState.value.copy(
                userMessage = "Successfully merged $count duplicate contact(s)"
            )
            scanContacts()
        }
    }

    fun cleanContactName(item: NameCleanupItem) {
        val profile = _uiState.value.profiles.find { it.contact.id == item.contactId } ?: return
        viewModelScope.launch {
            val success = repository.cleanContactName(item, profile)
            if (success) {
                _uiState.value = _uiState.value.copy(
                    userMessage = "Renamed to '${item.proposedName}'"
                )
                scanContacts()
            }
        }
    }

    fun cleanAllNames() {
        val items = _uiState.value.nameCleanups
        if (items.isEmpty()) return

        val profilesMap = _uiState.value.profiles.associateBy { it.contact.id }
        viewModelScope.launch {
            val count = repository.cleanAllContactNames(items, profilesMap)
            _uiState.value = _uiState.value.copy(
                userMessage = "Fixed capitalization and formatting for $count contact(s)"
            )
            scanContacts()
        }
    }

    fun undoLastAction() {
        viewModelScope.launch {
            val success = repository.undoLastAction()
            if (success) {
                _uiState.value = _uiState.value.copy(userMessage = "Restored previous contact state")
                scanContacts()
            } else {
                _uiState.value = _uiState.value.copy(userMessage = "Nothing to undo")
            }
        }
    }

    fun openRulesSheet() {
        _uiState.value = _uiState.value.copy(showRulesSheet = true)
    }

    fun dismissRulesSheet() {
        _uiState.value = _uiState.value.copy(showRulesSheet = false)
    }

    fun updateConfig(newConfig: SmartFilterConfig) {
        _uiState.value = _uiState.value.copy(config = newConfig)
        scanContacts()
    }

    fun clearUserMessage() {
        _uiState.value = _uiState.value.copy(userMessage = null)
    }

    fun getUndoHistory(): List<UndoRecord> {
        return repository.undoManager.getHistory()
    }
}

