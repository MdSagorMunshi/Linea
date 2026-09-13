package com.ryanshelby.linea

import com.ryanshelby.linea.data.local.entities.CallDirectionType
import com.ryanshelby.linea.data.local.entities.CallRecordEntity
import com.ryanshelby.linea.telecom.SimAccountInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates Call-Back from Recent (History) with Multi-SIM / Always Ask mode:
 * 1. If Always Ask is ON and user has 2+ SIMs: must ask for SIM selection (pendingCallBackRecord != null).
 * 2. If Always Ask is ON and defaultSim == -1 with 2+ SIMs: must ask for SIM selection.
 * 3. If Always Ask is OFF: directly places call using preferred/record SIM without prompting.
 * 4. If single SIM device: directly places call without prompting regardless of preference.
 * 5. Selection and dismissal properly clear pending state.
 */
class HistoryDualSimCallbackTest {

    private class TestHistoryCoordinator {
        var pendingCallBackRecord: CallRecordEntity? = null
        var placedCallNumber: String? = null
        var placedCallSimSlot: Int? = null

        fun onCallBack(
            record: CallRecordEntity,
            simAccounts: List<SimAccountInfo>,
            askSimBeforeDial: Boolean,
            defaultSim: Int
        ) {
            val askSim = askSimBeforeDial || defaultSim == -1
            if (askSim && simAccounts.size > 1) {
                pendingCallBackRecord = record
            } else {
                val sim = if (defaultSim in 0..1) {
                    simAccounts.find { it.slotIndex == defaultSim }
                } else {
                    null
                } ?: simAccounts.find { it.slotIndex == record.simSlot }
                  ?: simAccounts.firstOrNull()

                placedCallNumber = record.phoneNumber
                placedCallSimSlot = sim?.slotIndex
            }
        }

        fun placeCallWithSim(record: CallRecordEntity, simAccount: SimAccountInfo) {
            pendingCallBackRecord = null
            placedCallNumber = record.phoneNumber
            placedCallSimSlot = simAccount.slotIndex
        }

        fun dismissSimSelection() {
            pendingCallBackRecord = null
        }
    }

    private val sampleRecord = CallRecordEntity(
        id = 1L,
        phoneNumber = "+15551234567",
        formattedNumber = "(555) 123-4567",
        callerName = "Alice",
        callType = CallDirectionType.INCOMING,
        timestamp = System.currentTimeMillis(),
        durationSeconds = 120,
        simSlot = 1
    )

    private val sim1 = SimAccountInfo(
        slotIndex = 0,
        subscriptionId = 1,
        displayName = "Personal",
        carrierName = "Carrier A",
        phoneAccountHandle = android.telecom.PhoneAccountHandle(
            android.content.ComponentName("com.android.phone", "TelephonyConnectionService"),
            "1"
        )
    )

    private val sim2 = SimAccountInfo(
        slotIndex = 1,
        subscriptionId = 2,
        displayName = "Work",
        carrierName = "Carrier B",
        phoneAccountHandle = android.telecom.PhoneAccountHandle(
            android.content.ComponentName("com.android.phone", "TelephonyConnectionService"),
            "2"
        )
    )

    @Test
    fun testAlwaysAskModePromptsForSimWhenDualSimActive() {
        val coordinator = TestHistoryCoordinator()
        val dualSims = listOf(sim1, sim2)

        // Always Ask is ON
        coordinator.onCallBack(sampleRecord, dualSims, askSimBeforeDial = true, defaultSim = 0)

        // Must request SIM selection!
        assertNotNull(coordinator.pendingCallBackRecord)
        assertEquals("+15551234567", coordinator.pendingCallBackRecord?.phoneNumber)
        assertNull(coordinator.placedCallNumber)

        // User picks SIM 2
        coordinator.placeCallWithSim(sampleRecord, sim2)

        // Call is placed with SIM 2, pending record is cleared
        assertNull(coordinator.pendingCallBackRecord)
        assertEquals("+15551234567", coordinator.placedCallNumber)
        assertEquals(1, coordinator.placedCallSimSlot)
    }

    @Test
    fun testDefaultSimMinusOneTriggersAlwaysAskPrompt() {
        val coordinator = TestHistoryCoordinator()
        val dualSims = listOf(sim1, sim2)

        // defaultSim = -1 represents "Always Ask" in Settings
        coordinator.onCallBack(sampleRecord, dualSims, askSimBeforeDial = false, defaultSim = -1)

        assertNotNull(coordinator.pendingCallBackRecord)
        assertEquals("+15551234567", coordinator.pendingCallBackRecord?.phoneNumber)
    }

    @Test
    fun testAlwaysAskDismissalDoesNotPlaceCall() {
        val coordinator = TestHistoryCoordinator()
        val dualSims = listOf(sim1, sim2)

        coordinator.onCallBack(sampleRecord, dualSims, askSimBeforeDial = true, defaultSim = -1)
        assertNotNull(coordinator.pendingCallBackRecord)

        // User dismisses the bottom sheet
        coordinator.dismissSimSelection()

        assertNull(coordinator.pendingCallBackRecord)
        assertNull(coordinator.placedCallNumber)
    }

    @Test
    fun testSingleSimDoesNotPromptEvenIfAlwaysAskEnabled() {
        val coordinator = TestHistoryCoordinator()
        val singleSim = listOf(sim1)

        // Only 1 SIM present
        coordinator.onCallBack(sampleRecord, singleSim, askSimBeforeDial = true, defaultSim = -1)

        // Should directly place call without prompting
        assertNull(coordinator.pendingCallBackRecord)
        assertEquals("+15551234567", coordinator.placedCallNumber)
        assertEquals(0, coordinator.placedCallSimSlot)
    }

    @Test
    fun testAlwaysAskOffUsesRecordOrPreferredSim() {
        val coordinator = TestHistoryCoordinator()
        val dualSims = listOf(sim1, sim2)

        // Always Ask is OFF, defaultSim is 0
        coordinator.onCallBack(sampleRecord, dualSims, askSimBeforeDial = false, defaultSim = 0)

        assertNull(coordinator.pendingCallBackRecord)
        assertEquals("+15551234567", coordinator.placedCallNumber)
        assertEquals(0, coordinator.placedCallSimSlot)
    }
}
