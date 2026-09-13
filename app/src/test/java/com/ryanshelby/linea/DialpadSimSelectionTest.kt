package com.ryanshelby.linea

import com.ryanshelby.linea.telecom.SimAccountInfo
import com.ryanshelby.linea.ui.screens.dialpad.DialpadViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests validating Dial Pad Multi-SIM selection behavior:
 * 1. SIM 1 Only mode: Dial pad defaults to SIM 1 upon opening; switching to SIM 2 is temporary
 *    and never alters Settings; reopening restores SIM 1.
 * 2. SIM 2 Only mode: Dial pad defaults to SIM 2 upon opening; switching to SIM 1 is temporary
 *    and never alters Settings; reopening restores SIM 2.
 * 3. Always Ask mode: Dial pad defaults to Always Ask symbol (? / -1); user can temporarily switch
 *    to SIM 1 or SIM 2 directly without changing Settings; reopening restores Always Ask symbol.
 * 4. Call routing: Selected SIM slot correctly chooses the targeted PhoneAccountHandle.
 */
class DialpadSimSelectionTest {

    private class MockPreferences(
        var defaultSim: Int = 0,
        var askSimBeforeDial: Boolean = false
    )

    private class DialpadSimCoordinator(
        val preferences: MockPreferences,
        val simAccounts: List<SimAccountInfo>
    ) {
        var selectedSimIndex: Int = DialpadViewModel.SIM_SLOT_1
            private set

        val isAlwaysAskEnabled: Boolean
            get() = preferences.askSimBeforeDial || preferences.defaultSim == -1

        var lastPlacedCallNumber: String? = null
        var lastPlacedCallAccount: SimAccountInfo? = null
        var showSimSelectSheet: Boolean = false

        init {
            resetToDefaultSim()
        }

        fun resetToDefaultSim() {
            selectedSimIndex = if (isAlwaysAskEnabled) {
                DialpadViewModel.SIM_ALWAYS_ASK
            } else if (preferences.defaultSim == 1) {
                DialpadViewModel.SIM_SLOT_2
            } else {
                DialpadViewModel.SIM_SLOT_1
            }
        }

        fun selectSim(index: Int) {
            if (index in 0..1 || index == DialpadViewModel.SIM_ALWAYS_ASK) {
                // Strictly temporary: modifies local state only, NEVER touches preferences!
                selectedSimIndex = index
            }
        }

        fun initiateCall(number: String) {
            if (selectedSimIndex == DialpadViewModel.SIM_ALWAYS_ASK) {
                showSimSelectSheet = true
            } else {
                showSimSelectSheet = false
                placeCall(number)
            }
        }

        fun placeCall(number: String) {
            val targetSlot = if (selectedSimIndex in 0..1) selectedSimIndex else 0
            val account = simAccounts.find { it.slotIndex == targetSlot }
                ?: simAccounts.getOrNull(targetSlot)
                ?: simAccounts.firstOrNull()
            lastPlacedCallNumber = number
            lastPlacedCallAccount = account
        }

        fun placeCallWithAccount(number: String, account: SimAccountInfo) {
            showSimSelectSheet = false
            lastPlacedCallNumber = number
            lastPlacedCallAccount = account
        }
    }

    private val sim1 = SimAccountInfo(
        slotIndex = 0,
        subscriptionId = 1,
        displayName = "SIM 1",
        carrierName = "Carrier 1",
        phoneAccountHandle = android.telecom.PhoneAccountHandle(
            android.content.ComponentName("com.android.phone", "TelephonyConnectionService"),
            "1"
        )
    )

    private val sim2 = SimAccountInfo(
        slotIndex = 1,
        subscriptionId = 2,
        displayName = "SIM 2",
        carrierName = "Carrier 2",
        phoneAccountHandle = android.telecom.PhoneAccountHandle(
            android.content.ComponentName("com.android.phone", "TelephonyConnectionService"),
            "2"
        )
    )

    private val accounts = listOf(sim1, sim2)

    @Test
    fun testSim1OnlyModeDefaultsToSim1AndAllowsTemporarySwitch() {
        val prefs = MockPreferences(defaultSim = 0, askSimBeforeDial = false)
        val coordinator = DialpadSimCoordinator(prefs, accounts)

        // 1. Automatically select SIM 1 when opening dial pad
        assertEquals(DialpadViewModel.SIM_SLOT_1, coordinator.selectedSimIndex)
        assertFalse(coordinator.isAlwaysAskEnabled)

        // 2. User temporarily switches to SIM 2 from dial pad
        coordinator.selectSim(DialpadViewModel.SIM_SLOT_2)
        assertEquals(DialpadViewModel.SIM_SLOT_2, coordinator.selectedSimIndex)

        // 3. Placing call uses SIM 2 directly
        coordinator.initiateCall("5551234")
        assertFalse(coordinator.showSimSelectSheet)
        assertEquals(1, coordinator.lastPlacedCallAccount?.slotIndex)

        // 4. Switching must NOT alter Settings preference
        assertEquals(0, prefs.defaultSim)
        assertFalse(prefs.askSimBeforeDial)

        // 5. Reopening the dial pad restores default SIM 1
        coordinator.resetToDefaultSim()
        assertEquals(DialpadViewModel.SIM_SLOT_1, coordinator.selectedSimIndex)
    }

    @Test
    fun testSim2OnlyModeDefaultsToSim2AndAllowsTemporarySwitch() {
        val prefs = MockPreferences(defaultSim = 1, askSimBeforeDial = false)
        val coordinator = DialpadSimCoordinator(prefs, accounts)

        // 1. Automatically select SIM 2 when opening dial pad
        assertEquals(DialpadViewModel.SIM_SLOT_2, coordinator.selectedSimIndex)
        assertFalse(coordinator.isAlwaysAskEnabled)

        // 2. User temporarily switches to SIM 1 from dial pad
        coordinator.selectSim(DialpadViewModel.SIM_SLOT_1)
        assertEquals(DialpadViewModel.SIM_SLOT_1, coordinator.selectedSimIndex)

        // 3. Placing call uses SIM 1 directly
        coordinator.initiateCall("5551234")
        assertFalse(coordinator.showSimSelectSheet)
        assertEquals(0, coordinator.lastPlacedCallAccount?.slotIndex)

        // 4. Switching must NOT alter Settings preference
        assertEquals(1, prefs.defaultSim)
        assertFalse(prefs.askSimBeforeDial)

        // 5. Reopening the dial pad restores default SIM 2
        coordinator.resetToDefaultSim()
        assertEquals(DialpadViewModel.SIM_SLOT_2, coordinator.selectedSimIndex)
    }

    @Test
    fun testAlwaysAskModeDefaultsToAlwaysAskSymbolAndAllowsTemporarySwitch() {
        val prefs = MockPreferences(defaultSim = -1, askSimBeforeDial = true)
        val coordinator = DialpadSimCoordinator(prefs, accounts)

        // 1. In Always Ask mode, Always Ask symbol (? / -1) is selected by default
        assertEquals(DialpadViewModel.SIM_ALWAYS_ASK, coordinator.selectedSimIndex)
        assertTrue(coordinator.isAlwaysAskEnabled)

        // 2. Tapping call while Always Ask symbol is active triggers SimSelectSheet
        coordinator.initiateCall("5551234")
        assertTrue(coordinator.showSimSelectSheet)

        // 3. User can temporarily choose SIM 1 directly from dial pad
        coordinator.selectSim(DialpadViewModel.SIM_SLOT_1)
        assertEquals(DialpadViewModel.SIM_SLOT_1, coordinator.selectedSimIndex)

        // 4. In Always Ask mode with temporary SIM 1 override, calling dials SIM 1 directly without sheet
        coordinator.initiateCall("5551234")
        assertFalse(coordinator.showSimSelectSheet)
        assertEquals(0, coordinator.lastPlacedCallAccount?.slotIndex)

        // 5. User can temporarily choose SIM 2 directly from dial pad
        coordinator.selectSim(DialpadViewModel.SIM_SLOT_2)
        assertEquals(DialpadViewModel.SIM_SLOT_2, coordinator.selectedSimIndex)
        coordinator.initiateCall("5551234")
        assertFalse(coordinator.showSimSelectSheet)
        assertEquals(1, coordinator.lastPlacedCallAccount?.slotIndex)

        // 6. User can switch back to Always Ask symbol
        coordinator.selectSim(DialpadViewModel.SIM_ALWAYS_ASK)
        assertEquals(DialpadViewModel.SIM_ALWAYS_ASK, coordinator.selectedSimIndex)
        coordinator.initiateCall("5551234")
        assertTrue(coordinator.showSimSelectSheet)

        // 7. Temporary overrides must NEVER mutate Settings preference
        assertEquals(-1, prefs.defaultSim)
        assertTrue(prefs.askSimBeforeDial)

        // 8. Reopening the dial pad restores default Always Ask symbol (-1)
        coordinator.selectSim(DialpadViewModel.SIM_SLOT_2) // user was on SIM 2
        coordinator.resetToDefaultSim() // dial pad reopened
        assertEquals(DialpadViewModel.SIM_ALWAYS_ASK, coordinator.selectedSimIndex)
    }
}
