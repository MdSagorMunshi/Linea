package com.ryanshelby.linea.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryanshelby.linea.data.local.dao.CallRecordDao
import com.ryanshelby.linea.data.local.entities.CallDirectionType
import com.ryanshelby.linea.data.local.entities.CallRecordEntity
import com.ryanshelby.linea.data.repository.CallLogRepository
import com.ryanshelby.linea.telecom.PhoneAccountManager
import com.ryanshelby.linea.telecom.SimAccountInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class StatsTimeFrame(val label: String) {
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    ALL_TIME("All Time")
}

data class ContactCallRank(
    val nameOrNumber: String,
    val callCount: Int,
    val totalDuration: Long
)

data class CallStatsState(
    val totalCalls: Int = 0,
    val incomingCalls: Int = 0,
    val outgoingCalls: Int = 0,
    val missedCalls: Int = 0,
    val blockedCalls: Int = 0,
    val totalDurationSeconds: Long = 0,
    val avgDurationSeconds: Long = 0,
    val sim1Count: Int = 0,
    val sim2Count: Int = 0,
    val topContacts: List<ContactCallRank> = emptyList(),
    val longestCalls: List<CallRecordEntity> = emptyList(),
    val selectedTimeFrame: StatsTimeFrame = StatsTimeFrame.ALL_TIME,
    val selectedSimFilter: Int = -1, // -1 means All SIMs, 0 is SIM 1, 1 is SIM 2
    val availableSims: List<SimAccountInfo> = emptyList(),
    val isSyncing: Boolean = false
)

@HiltViewModel
class CallStatsViewModel @Inject constructor(
    private val callRecordDao: CallRecordDao,
    private val callLogRepository: CallLogRepository,
    private val phoneAccountManager: PhoneAccountManager
) : ViewModel() {

    private val _selectedTimeFrame = MutableStateFlow(StatsTimeFrame.ALL_TIME)
    private val _selectedSimFilter = MutableStateFlow(-1)
    private val _isSyncing = MutableStateFlow(false)

    init {
        refresh()
    }

    fun setTimeFrame(timeFrame: StatsTimeFrame) {
        _selectedTimeFrame.value = timeFrame
    }

    fun setSimFilter(simSlot: Int) {
        _selectedSimFilter.value = simSlot
    }

    fun refresh() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                callLogRepository.syncSystemCallLog()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSyncing.value = false
            }
        }
    }

    val stats: StateFlow<CallStatsState> = combine(
        callRecordDao.getAllCallRecords(),
        _selectedTimeFrame,
        _selectedSimFilter,
        _isSyncing
    ) { allRecords, timeFrame, simFilter, isSyncing ->
        val sims = try {
            phoneAccountManager.getSimAccounts()
        } catch (_: Exception) {
            emptyList()
        }

        // Calculate time boundary
        val now = System.currentTimeMillis()
        val minTimestamp = when (timeFrame) {
            StatsTimeFrame.THIS_WEEK -> {
                val cal = Calendar.getInstance()
                cal.firstDayOfWeek = Calendar.MONDAY
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            StatsTimeFrame.THIS_MONTH -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            StatsTimeFrame.ALL_TIME -> 0L
        }

        // Filter by timeframe
        val timeFiltered = allRecords.filter { it.timestamp >= minTimestamp }

        // Filter by SIM slot if specified
        val filtered = if (simFilter >= 0) {
            timeFiltered.filter { it.simSlot == simFilter }
        } else {
            timeFiltered
        }

        if (filtered.isEmpty()) {
            return@combine CallStatsState(
                selectedTimeFrame = timeFrame,
                selectedSimFilter = simFilter,
                availableSims = sims,
                isSyncing = isSyncing
            )
        }

        val total = filtered.size
        val incoming = filtered.count { it.callType == CallDirectionType.INCOMING }
        val outgoing = filtered.count { it.callType == CallDirectionType.OUTGOING }
        val missed = filtered.count { it.callType == CallDirectionType.MISSED }
        val blocked = filtered.count { it.callType == CallDirectionType.BLOCKED || it.callType == CallDirectionType.REJECTED }
        val totalDuration = filtered.sumOf { it.durationSeconds }
        val connectedCalls = incoming + outgoing
        val avgDuration = if (connectedCalls > 0) totalDuration / connectedCalls else 0

        val sim1 = timeFiltered.count { it.simSlot == 0 }
        val sim2 = timeFiltered.count { it.simSlot == 1 }

        val topContacts = filtered.groupBy { it.callerName?.ifBlank { null } ?: it.formattedNumber.ifBlank { it.phoneNumber } }
            .map { (key, list) ->
                ContactCallRank(
                    nameOrNumber = key,
                    callCount = list.size,
                    totalDuration = list.sumOf { it.durationSeconds }
                )
            }
            .sortedByDescending { it.callCount }
            .take(5)

        val longest = filtered.filter { it.durationSeconds > 0 }
            .sortedByDescending { it.durationSeconds }
            .take(5)

        CallStatsState(
            totalCalls = total,
            incomingCalls = incoming,
            outgoingCalls = outgoing,
            missedCalls = missed,
            blockedCalls = blocked,
            totalDurationSeconds = totalDuration,
            avgDurationSeconds = avgDuration,
            sim1Count = sim1,
            sim2Count = sim2,
            topContacts = topContacts,
            longestCalls = longest,
            selectedTimeFrame = timeFrame,
            selectedSimFilter = simFilter,
            availableSims = sims,
            isSyncing = isSyncing
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CallStatsState()
    )
}
