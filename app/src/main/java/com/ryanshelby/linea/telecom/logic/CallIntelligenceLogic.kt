package com.ryanshelby.linea.telecom.logic

object AutoRecordRuleEvaluator {
    fun shouldRecord(autoRecordAll: Boolean, autoRecordContactsOnly: Boolean, isContact: Boolean): Boolean {
        if (!autoRecordAll) return false
        return if (autoRecordContactsOnly) isContact else true
    }
}

object CallDurationWarningEvaluator {
    fun shouldTriggerWarning(warningMinutes: Int, durationSeconds: Long, alreadyTriggered: Boolean): Boolean {
        if (alreadyTriggered) return false
        if (warningMinutes <= 0) return false
        return durationSeconds >= (warningMinutes * 60L)
    }
}

data class MultiCallSession(
    val activeCallId: String?,
    val heldCallId: String?,
    val waitingCallId: String?,
    val isConference: Boolean = false
)

enum class MultiCallAction {
    HOLD_AND_ANSWER,
    END_AND_ANSWER,
    REJECT_WAITING,
    SWAP,
    MERGE_CONFERENCE
}

object MultiCallStateMachine {
    fun reduce(current: MultiCallSession, action: MultiCallAction): MultiCallSession {
        return when (action) {
            MultiCallAction.HOLD_AND_ANSWER -> {
                val waiting = current.waitingCallId ?: return current
                val active = current.activeCallId
                MultiCallSession(
                    activeCallId = waiting,
                    heldCallId = active,
                    waitingCallId = null,
                    isConference = false
                )
            }
            MultiCallAction.END_AND_ANSWER -> {
                val waiting = current.waitingCallId ?: return current
                MultiCallSession(
                    activeCallId = waiting,
                    heldCallId = current.heldCallId,
                    waitingCallId = null,
                    isConference = false
                )
            }
            MultiCallAction.REJECT_WAITING -> {
                current.copy(waitingCallId = null)
            }
            MultiCallAction.SWAP -> {
                if (current.heldCallId == null || current.activeCallId == null) return current
                current.copy(
                    activeCallId = current.heldCallId,
                    heldCallId = current.activeCallId
                )
            }
            MultiCallAction.MERGE_CONFERENCE -> {
                if (current.heldCallId == null || current.activeCallId == null) return current
                current.copy(
                    activeCallId = "conf_${current.activeCallId}_${current.heldCallId}",
                    heldCallId = null,
                    isConference = true
                )
            }
        }
    }
}
