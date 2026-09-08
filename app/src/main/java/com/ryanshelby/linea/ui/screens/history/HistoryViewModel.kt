package com.ryanshelby.linea.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryanshelby.linea.data.local.dao.BlockedNumberDao
import com.ryanshelby.linea.data.local.dao.CallNoteDao
import com.ryanshelby.linea.data.local.dao.CallbackReminderDao
import com.ryanshelby.linea.data.local.entities.BlockMatchType
import com.ryanshelby.linea.data.local.entities.BlockedNumberEntity
import com.ryanshelby.linea.data.local.entities.CallDirectionType
import com.ryanshelby.linea.data.local.entities.CallNoteEntity
import com.ryanshelby.linea.data.local.entities.CallRecordEntity
import com.ryanshelby.linea.data.local.entities.CallbackReminderEntity
import com.ryanshelby.linea.data.repository.CallLogRepository
import com.ryanshelby.linea.telecom.CallManager
import com.ryanshelby.linea.telecom.PhoneAccountManager
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

import com.ryanshelby.linea.data.local.dao.CallRecordingDao
import com.ryanshelby.linea.telecom.reminder.CallbackReminderScheduler

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val callLogRepository: CallLogRepository,
    private val callManager: CallManager,
    private val phoneAccountManager: PhoneAccountManager,
    private val callNoteDao: CallNoteDao,
    private val callbackReminderDao: CallbackReminderDao,
    private val blockedNumberDao: BlockedNumberDao,
    private val recordingDao: CallRecordingDao,
    private val reminderScheduler: CallbackReminderScheduler
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(HistoryFilter.ALL)
    val selectedFilter: StateFlow<HistoryFilter> = _selectedFilter.asStateFlow()

    private val _expandedItemIds = MutableStateFlow<Set<String>>(emptySet())
    val expandedItemIds: StateFlow<Set<String>> = _expandedItemIds.asStateFlow()

    private val _selectedItemForDetail = MutableStateFlow<CallHistoryItem?>(null)
    val selectedItemForDetail: StateFlow<CallHistoryItem?> = _selectedItemForDetail.asStateFlow()

    val notesForSelectedCall: StateFlow<List<CallNoteEntity>> = _selectedItemForDetail
        .flatMapLatest { item ->
            if (item != null) {
                callNoteDao.getNotesForContact(null, item.primaryRecord.phoneNumber)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rawCallRecords = callLogRepository.getAllCallRecords()

    val dateGroups: StateFlow<List<DateGroup>> = combine(
        rawCallRecords,
        recordingDao.getAllRecordings(),
        _searchQuery,
        _selectedFilter,
        _expandedItemIds
    ) { records, recordings, query, filter, expandedIds ->
        val recordedNumbers = recordings.map { it.phoneNumber.filter { c -> c.isDigit() } }.toSet()

        // 1. Filter by category
        val filteredByCategory = when (filter) {
            HistoryFilter.ALL -> records
            HistoryFilter.MISSED -> records.filter { it.callType == CallDirectionType.MISSED }
            HistoryFilter.BLOCKED -> records.filter { it.callType == CallDirectionType.BLOCKED }
            HistoryFilter.RECORDINGS -> records.filter {
                it.notes?.contains("recording", ignoreCase = true) == true ||
                recordedNumbers.contains(it.phoneNumber.filter { c -> c.isDigit() })
            }
        }

        // 2. Filter by search query (name, number, or date)
        val filteredByQuery = if (query.isBlank()) {
            filteredByCategory
        } else {
            val q = query.trim().lowercase()
            filteredByCategory.filter { rec ->
                rec.phoneNumber.contains(q) ||
                (rec.callerName?.lowercase()?.contains(q) == true) ||
                HistoryGrouper.formatRelativeDate(rec.timestamp).lowercase().contains(q)
            }
        }

        // 3. Group by date and coalesce repeated calls
        val grouped = HistoryGrouper.groupRecords(filteredByQuery)

        // 4. Map expansion states
        grouped.map { dateGroup ->
            dateGroup.copy(
                items = dateGroup.items.map { item ->
                    item.copy(isExpanded = expandedIds.contains(item.id))
                }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            callLogRepository.syncSystemCallLog()
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterSelect(filter: HistoryFilter) {
        _selectedFilter.value = filter
    }

    fun toggleItemExpanded(itemId: String) {
        val current = _expandedItemIds.value
        _expandedItemIds.value = if (current.contains(itemId)) {
            current - itemId
        } else {
            current + itemId
        }
    }

    fun selectItemForDetail(item: CallHistoryItem?) {
        _selectedItemForDetail.value = item
    }

    fun callBack(record: CallRecordEntity) {
        val simAccounts = phoneAccountManager.registerPhoneAccounts()
        val simHandle = simAccounts.find { it.slotIndex == record.simSlot }?.phoneAccountHandle
            ?: simAccounts.firstOrNull()?.phoneAccountHandle
        callManager.placeCall(record.phoneNumber, simHandle)
    }

    fun deleteRecord(recordId: Long) {
        viewModelScope.launch {
            callLogRepository.deleteCall(recordId)
        }
    }

    fun deleteGroup(item: CallHistoryItem) {
        viewModelScope.launch {
            val ids = item.groupedCalls.map { it.id }
            callLogRepository.deleteCallGroup(ids)
            if (_selectedItemForDetail.value?.id == item.id) {
                _selectedItemForDetail.value = null
            }
        }
    }

    fun blockNumber(number: String, reason: String? = "Blocked from history") {
        viewModelScope.launch {
            blockedNumberDao.insertBlockedNumber(
                BlockedNumberEntity(
                    numberOrPrefix = number,
                    matchType = BlockMatchType.EXACT,
                    reason = reason
                )
            )
        }
    }

    fun addNote(phoneNumber: String, contactId: Long?, noteText: String) {
        if (noteText.isBlank()) return
        viewModelScope.launch {
            callNoteDao.insertNote(
                CallNoteEntity(
                    contactId = contactId,
                    phoneNumber = phoneNumber,
                    noteText = noteText.trim()
                )
            )
        }
    }

    fun scheduleCallbackReminder(phoneNumber: String, callerName: String?, delayHours: Long) {
        viewModelScope.launch {
            val delayMs = delayHours * 3600 * 1000
            reminderScheduler.scheduleReminder(
                phoneNumber = phoneNumber,
                callerName = callerName,
                delayMs = delayMs
            )
        }
    }
}
