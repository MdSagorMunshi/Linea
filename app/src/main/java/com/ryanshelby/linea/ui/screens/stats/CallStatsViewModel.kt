package com.ryanshelby.linea.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryanshelby.linea.data.local.dao.CallRecordDao
import com.ryanshelby.linea.data.local.entities.CallDirectionType
import com.ryanshelby.linea.data.local.entities.CallRecordEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

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
    val longestCalls: List<CallRecordEntity> = emptyList()
)

@HiltViewModel
class CallStatsViewModel @Inject constructor(
    callRecordDao: CallRecordDao
) : ViewModel() {

    val stats: StateFlow<CallStatsState> = callRecordDao.getAllCallRecords()
        .map { records ->
            if (records.isEmpty()) return@map CallStatsState()

            val total = records.size
            val incoming = records.count { it.callType == CallDirectionType.INCOMING }
            val outgoing = records.count { it.callType == CallDirectionType.OUTGOING }
            val missed = records.count { it.callType == CallDirectionType.MISSED }
            val blocked = records.count { it.callType == CallDirectionType.BLOCKED || it.callType == CallDirectionType.REJECTED }
            val totalDuration = records.sumOf { it.durationSeconds }
            val connectedCalls = incoming + outgoing
            val avgDuration = if (connectedCalls > 0) totalDuration / connectedCalls else 0

            val sim1 = records.count { it.simSlot == 0 }
            val sim2 = records.count { it.simSlot == 1 }

            val topContacts = records.groupBy { it.callerName ?: it.formattedNumber }
                .map { (key, list) ->
                    ContactCallRank(
                        nameOrNumber = key,
                        callCount = list.size,
                        totalDuration = list.sumOf { it.durationSeconds }
                    )
                }
                .sortedByDescending { it.callCount }
                .take(5)

            val longest = records.filter { it.durationSeconds > 0 }
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
                longestCalls = longest
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CallStatsState()
        )
}
