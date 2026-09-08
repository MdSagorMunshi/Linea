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

import com.ryanshelby.linea.data.local.dao.ContactDao
import com.ryanshelby.linea.data.local.entities.ContactEntity
import com.ryanshelby.linea.data.preferences.LineaPreferences

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
    private val reminderScheduler: CallbackReminderScheduler,
    private val contactDao: ContactDao,
    private val preferences: LineaPreferences
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(HistoryFilter.ALL)
    val selectedFilter: StateFlow<HistoryFilter> = _selectedFilter.asStateFlow()

    private val _expandedItemIds = MutableStateFlow<Set<String>>(emptySet())
    val expandedItemIds: StateFlow<Set<String>> = _expandedItemIds.asStateFlow()

    private val _expandedSessionIds = MutableStateFlow<Set<String>>(emptySet())
    val expandedSessionIds: StateFlow<Set<String>> = _expandedSessionIds.asStateFlow()

    private val _selectedItemForDetail = MutableStateFlow<CallHistoryItem?>(null)
    val selectedItemForDetail: StateFlow<CallHistoryItem?> = _selectedItemForDetail.asStateFlow()

    val pinnedContacts: StateFlow<List<ContactEntity>> = contactDao.getPinnedContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val historyViewMode: StateFlow<String> = preferences.historyViewMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "FEED")

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
        _expandedItemIds,
        preferences.privateModeUnlocked
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val records = args[0] as List<CallRecordEntity>
        @Suppress("UNCHECKED_CAST")
        val recordings = args[1] as List<com.ryanshelby.linea.data.local.entities.CallRecordingEntity>
        val query = args[2] as String
        val filter = args[3] as HistoryFilter
        @Suppress("UNCHECKED_CAST")
        val expandedIds = args[4] as Set<String>
        val privateUnlocked = args[5] as Boolean
        val safeRecords = if (privateUnlocked) records else records.filter { !it.isPrivateContact }
        val recordedNumbers = recordings.map { it.phoneNumber.filter { c -> c.isDigit() } }.toSet()

        // 1. Filter by category
        val filteredByCategory = when (filter) {
            HistoryFilter.ALL -> safeRecords
            HistoryFilter.MISSED -> safeRecords.filter { it.callType == CallDirectionType.MISSED }
            HistoryFilter.BLOCKED -> safeRecords.filter { it.callType == CallDirectionType.BLOCKED }
            HistoryFilter.RECORDINGS -> safeRecords.filter {
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

    val callSessions: StateFlow<List<CallSessionItem>> = combine(
        rawCallRecords,
        _searchQuery,
        _selectedFilter,
        _expandedSessionIds,
        preferences.privateModeUnlocked
    ) { records, query, filter, expandedSessionIds, privateUnlocked ->
        val safeRecords = if (privateUnlocked) records else records.filter { !it.isPrivateContact }
        val filteredByCategory = when (filter) {
            HistoryFilter.ALL -> safeRecords
            HistoryFilter.MISSED -> safeRecords.filter { it.callType == CallDirectionType.MISSED }
            HistoryFilter.BLOCKED -> safeRecords.filter { it.callType == CallDirectionType.BLOCKED }
            HistoryFilter.RECORDINGS -> safeRecords.filter {
                it.notes?.contains("recording", ignoreCase = true) == true
            }
        }
        val filteredByQuery = if (query.isBlank()) {
            filteredByCategory
        } else {
            val q = query.trim().lowercase()
            filteredByCategory.filter { rec ->
                rec.phoneNumber.contains(q) || (rec.callerName?.lowercase()?.contains(q) == true)
            }
        }
        val sessions = HistoryGrouper.groupSessions(filteredByQuery)
        sessions.map { session ->
            session.copy(isExpanded = expandedSessionIds.contains(session.id))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            callLogRepository.syncSystemCallLog()
        }
    }

    fun toggleViewMode() {
        viewModelScope.launch {
            val current = historyViewMode.value
            val newMode = if (current == "FEED") "SESSIONS" else "FEED"
            preferences.setHistoryViewMode(newMode)
        }
    }

    fun toggleSessionExpanded(sessionId: String) {
        val current = _expandedSessionIds.value
        _expandedSessionIds.value = if (current.contains(sessionId)) {
            current - sessionId
        } else {
            current + sessionId
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
        scheduleCallbackReminderMs(phoneNumber, callerName, delayHours * 3600 * 1000)
    }

    fun scheduleCallbackReminderMs(phoneNumber: String, callerName: String?, delayMs: Long) {
        viewModelScope.launch {
            reminderScheduler.scheduleReminder(
                phoneNumber = phoneNumber,
                callerName = callerName,
                delayMs = delayMs
            )
        }
    }
}
