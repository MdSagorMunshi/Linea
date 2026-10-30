package com.ryanshelby.linea.mergefix

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryanshelby.linea.data.local.entities.MergeHistoryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FixAndManageSection(val title: String) {
    MERGE_DUPLICATES("Merge Duplicates"),
    FIX_ISSUES("Fix Issues"),
    MERGE_HISTORY("Merge History")
}

@HiltViewModel
class FixAndManageViewModel @Inject constructor(
    private val repository: ContactMergeFixRepository
) : ViewModel() {

    private val _currentSection = MutableStateFlow(FixAndManageSection.MERGE_DUPLICATES)
    val currentSection: StateFlow<FixAndManageSection> = _currentSection.asStateFlow()

    val highConfidenceGroups: StateFlow<List<MergeGroup>> = repository.highConfidenceGroups
    val mediumConfidenceGroups: StateFlow<List<MergeGroup>> = repository.mediumConfidenceGroups
    val lowConfidenceGroups: StateFlow<List<MergeGroup>> = repository.lowConfidenceGroups

    val unnamedContacts: StateFlow<List<NormalizedContact>> = repository.unnamedContacts
    val malformedNameContacts: StateFlow<List<NormalizedContact>> = repository.malformedNameContacts
    val unnormalizedNumberIssues: StateFlow<List<FixIssue.UnnormalizedNumber>> = repository.unnormalizedNumberIssues
    val emptyContacts: StateFlow<List<NormalizedContact>> = repository.emptyContacts

    val mergeHistory: StateFlow<List<MergeHistoryEntity>> = repository.mergeHistory.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val isScanning: StateFlow<Boolean> = repository.isScanning

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val _lastMergedHistoryId = MutableStateFlow<Long?>(null)
    val lastMergedHistoryId: StateFlow<Long?> = _lastMergedHistoryId.asStateFlow()

    private val _undoCountdownSeconds = MutableStateFlow(0)
    val undoCountdownSeconds: StateFlow<Int> = _undoCountdownSeconds.asStateFlow()

    private var undoTimerJob: Job? = null

    init {
        // Enqueue auto-merge for HIGH confidence groups in WorkManager as specified
        viewModelScope.launch {
            repository.scanAll()
            if (repository.highConfidenceGroups.value.isNotEmpty()) {
                repository.triggerAutoMergeWork()
            }
        }
    }

    fun setSection(section: FixAndManageSection) {
        _currentSection.value = section
    }

    fun rescan() {
        viewModelScope.launch {
            repository.scanAll()
            _userMessage.value = "Contacts scan complete"
        }
    }

    fun mergeGroup(group: MergeGroup) {
        viewModelScope.launch {
            val contactNames = group.contacts.joinToString(", ") { it.displayName }
            repository.mergeGroup(group)
            val latestHistory = repository.mergeHistoryDaoLatest()
            if (latestHistory != null) {
                _lastMergedHistoryId.value = latestHistory.id
                startUndoCountdown(latestHistory.id)
            }
            _userMessage.value = "Merged ${group.contacts.size} contacts ($contactNames)"
        }
    }

    fun dismissMedium(group: MergeGroup) {
        repository.dismissGroup(group.id)
        _userMessage.value = "Marked as not the same"
    }

    fun dismissLow(group: MergeGroup) {
        repository.dismissGroup(group.id)
        _userMessage.value = "Suggestion dismissed"
    }

    fun unlinkMerge(historyId: Long) {
        viewModelScope.launch {
            val success = repository.unlinkMerge(historyId)
            if (success) {
                _lastMergedHistoryId.value = null
                _undoCountdownSeconds.value = 0
                undoTimerJob?.cancel()
                _userMessage.value = "Merge unlinked: original contacts restored"
            } else {
                _userMessage.value = "Failed to unlink merge"
            }
        }
    }

    fun undoLastMerge() {
        val id = _lastMergedHistoryId.value ?: return
        unlinkMerge(id)
    }

    private fun startUndoCountdown(historyId: Long) {
        undoTimerJob?.cancel()
        _undoCountdownSeconds.value = 30
        undoTimerJob = viewModelScope.launch {
            for (sec in 30 downTo 1) {
                _undoCountdownSeconds.value = sec
                delay(1000L)
            }
            _undoCountdownSeconds.value = 0
            if (_lastMergedHistoryId.value == historyId) {
                _lastMergedHistoryId.value = null
            }
        }
    }

    fun renameContact(contactId: Long, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            repository.renameContact(contactId, newName)
            _userMessage.value = "Contact name updated"
        }
    }

    fun normalizeNumber(contactId: Long, rawNumber: String, e164Number: String) {
        viewModelScope.launch {
            repository.normalizeNumber(contactId, rawNumber, e164Number)
            _userMessage.value = "Number normalized to $e164Number"
        }
    }

    fun normalizeAllNumbers() {
        viewModelScope.launch {
            val count = repository.unnormalizedNumberIssues.value.size
            repository.normalizeAllNumbers()
            _userMessage.value = "Normalized $count phone number(s)"
        }
    }

    fun deleteEmptyContact(contactId: Long) {
        viewModelScope.launch {
            repository.deleteContact(contactId)
            _userMessage.value = "Empty contact deleted"
        }
    }

    fun deleteAllEmptyContacts() {
        viewModelScope.launch {
            val count = repository.emptyContacts.value.size
            repository.deleteAllEmptyContacts()
            _userMessage.value = "Deleted $count empty contact(s)"
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    private suspend fun ContactMergeFixRepository.mergeHistoryDaoLatest(): MergeHistoryEntity? {
        return mergeHistory.first().firstOrNull()
    }
}

