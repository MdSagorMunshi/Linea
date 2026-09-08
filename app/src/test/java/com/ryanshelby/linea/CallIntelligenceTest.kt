package com.ryanshelby.linea

import com.ryanshelby.linea.telecom.logic.AutoRecordRuleEvaluator
import com.ryanshelby.linea.telecom.logic.CallDurationWarningEvaluator
import com.ryanshelby.linea.telecom.logic.MultiCallAction
import com.ryanshelby.linea.telecom.logic.MultiCallSession
import com.ryanshelby.linea.telecom.logic.MultiCallStateMachine
import com.ryanshelby.linea.ui.screens.settings.telecom.ForwardingCondition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CallIntelligenceTest {

    @Test
    fun testAutoRecordRuleEvaluator() {
        // Disabled globally
        assertFalse(AutoRecordRuleEvaluator.shouldRecord(autoRecordAll = false, autoRecordContactsOnly = false, isContact = true))
        assertFalse(AutoRecordRuleEvaluator.shouldRecord(autoRecordAll = false, autoRecordContactsOnly = false, isContact = false))

        // Enabled for all callers
        assertTrue(AutoRecordRuleEvaluator.shouldRecord(autoRecordAll = true, autoRecordContactsOnly = false, isContact = true))
        assertTrue(AutoRecordRuleEvaluator.shouldRecord(autoRecordAll = true, autoRecordContactsOnly = false, isContact = false))

        // Enabled for contacts only
        assertTrue(AutoRecordRuleEvaluator.shouldRecord(autoRecordAll = true, autoRecordContactsOnly = true, isContact = true))
        assertFalse(AutoRecordRuleEvaluator.shouldRecord(autoRecordAll = true, autoRecordContactsOnly = true, isContact = false))
    }

    @Test
    fun testCallDurationWarningEvaluator() {
        val warningMinutes = 5 // 300 seconds

        // Warning off
        assertFalse(CallDurationWarningEvaluator.shouldTriggerWarning(warningMinutes = 0, durationSeconds = 500, alreadyTriggered = false))

        // Warning on, before threshold
        assertFalse(CallDurationWarningEvaluator.shouldTriggerWarning(warningMinutes = warningMinutes, durationSeconds = 299, alreadyTriggered = false))

        // Warning on, exact threshold reached
        assertTrue(CallDurationWarningEvaluator.shouldTriggerWarning(warningMinutes = warningMinutes, durationSeconds = 300, alreadyTriggered = false))

        // Warning on, after threshold
        assertTrue(CallDurationWarningEvaluator.shouldTriggerWarning(warningMinutes = warningMinutes, durationSeconds = 305, alreadyTriggered = false))

        // Warning already triggered (must not re-trigger)
        assertFalse(CallDurationWarningEvaluator.shouldTriggerWarning(warningMinutes = warningMinutes, durationSeconds = 305, alreadyTriggered = true))
    }

    @Test
    fun testMultiCallHoldAndAnswer() {
        val session = MultiCallSession(
            activeCallId = "call_1",
            heldCallId = null,
            waitingCallId = "call_2"
        )

        val next = MultiCallStateMachine.reduce(session, MultiCallAction.HOLD_AND_ANSWER)
        assertEquals("call_2", next.activeCallId)
        assertEquals("call_1", next.heldCallId)
        assertNull(next.waitingCallId)
        assertFalse(next.isConference)
    }

    @Test
    fun testMultiCallSwap() {
        val session = MultiCallSession(
            activeCallId = "call_2",
            heldCallId = "call_1",
            waitingCallId = null
        )

        val swapped = MultiCallStateMachine.reduce(session, MultiCallAction.SWAP)
        assertEquals("call_1", swapped.activeCallId)
        assertEquals("call_2", swapped.heldCallId)
    }

    @Test
    fun testMultiCallMergeConference() {
        val session = MultiCallSession(
            activeCallId = "call_1",
            heldCallId = "call_2",
            waitingCallId = null
        )

        val conf = MultiCallStateMachine.reduce(session, MultiCallAction.MERGE_CONFERENCE)
        assertTrue(conf.isConference)
        assertNull(conf.heldCallId)
        assertEquals("conf_call_1_call_2", conf.activeCallId)
    }

    @Test
    fun testMultiCallEndAndAnswer() {
        val session = MultiCallSession(
            activeCallId = "call_1",
            heldCallId = "call_held",
            waitingCallId = "call_waiting"
        )

        val next = MultiCallStateMachine.reduce(session, MultiCallAction.END_AND_ANSWER)
        assertEquals("call_waiting", next.activeCallId)
        assertEquals("call_held", next.heldCallId)
        assertNull(next.waitingCallId)
    }

    @Test
    fun testMultiCallRejectWaiting() {
        val session = MultiCallSession(
            activeCallId = "call_1",
            heldCallId = null,
            waitingCallId = "call_waiting"
        )

        val next = MultiCallStateMachine.reduce(session, MultiCallAction.REJECT_WAITING)
        assertEquals("call_1", next.activeCallId)
        assertNull(next.waitingCallId)
    }

    @Test
    fun testCallForwardingMmiCodeFormatting() {
        val testNumber = "+15550192000"

        assertEquals("*21*", ForwardingCondition.ALWAYS.mmiActivateCode)
        assertEquals("##21#", ForwardingCondition.ALWAYS.mmiDeactivateCode)
        assertEquals("*21*$testNumber#", "${ForwardingCondition.ALWAYS.mmiActivateCode}$testNumber#")

        assertEquals("*67*", ForwardingCondition.WHEN_BUSY.mmiActivateCode)
        assertEquals("##67#", ForwardingCondition.WHEN_BUSY.mmiDeactivateCode)
        assertEquals("*67*$testNumber#", "${ForwardingCondition.WHEN_BUSY.mmiActivateCode}$testNumber#")

        assertEquals("*61*", ForwardingCondition.WHEN_UNANSWERED.mmiActivateCode)
        assertEquals("##61#", ForwardingCondition.WHEN_UNANSWERED.mmiDeactivateCode)
        assertEquals("*61*$testNumber#", "${ForwardingCondition.WHEN_UNANSWERED.mmiActivateCode}$testNumber#")

        assertEquals("*62*", ForwardingCondition.WHEN_UNREACHABLE.mmiActivateCode)
        assertEquals("##62#", ForwardingCondition.WHEN_UNREACHABLE.mmiDeactivateCode)
        assertEquals("*62*$testNumber#", "${ForwardingCondition.WHEN_UNREACHABLE.mmiActivateCode}$testNumber#")
    }
}
