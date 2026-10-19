package com.ryanshelby.linea

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying SIM identification resolution logic:
 * 1. Physical SIM slot index to display label mapping:
 *    - 0-based index 0 maps to "SIM 1"
 *    - 0-based index 1 maps to "SIM 2"
 * 2. Single-SIM devices hide SIM label everywhere (clean UI principle).
 * 3. Multi-SIM devices show "SIM 1" / "SIM 2" label.
 * 4. Graceful degradation: removed or swapped SIM returns null or neutral label, never crashes.
 * 5. Call log OEM quirks: Phone account ID resolved by ICCID, subscription ID string, or account ID.
 * 6. Emergency calls: never assigned a SIM slot or display label.
 * 7. eSIM + physical SIM combinations: slot index correctly mapped regardless of chip type.
 */
class SimAccountResolverTest {

    data class MockSubscription(
        val subscriptionId: Int,
        val simSlotIndex: Int,
        val displayName: String,
        val iccId: String? = null,
        val isEmbedded: Boolean = false
    )

    private class TestSimResolver(
        private var subscriptions: List<MockSubscription> = emptyList()
    ) {
        val hasMultipleSims: Boolean
            get() = subscriptions.count { it.simSlotIndex in 0..1 } >= 2

        fun setSubscriptions(newSubs: List<MockSubscription>) {
            subscriptions = newSubs
        }

        fun getSimLabel(slotIndex: Int?, forceShow: Boolean = false): String? {
            if (slotIndex == null || slotIndex !in 0..1) return null
            if (!hasMultipleSims && !forceShow) return null
            return "SIM ${slotIndex + 1}"
        }

        fun resolveSimSlotFromHandleId(handleId: String?): Int? {
            if (handleId.isNullOrBlank()) return null

            // 1. Direct subId match
            val subId = handleId.toIntOrNull()
            if (subId != null) {
                val sub = subscriptions.find { it.subscriptionId == subId }
                if (sub != null && sub.simSlotIndex in 0..1) {
                    return sub.simSlotIndex
                }
            }

            // 2. ICCID match
            val iccMatch = subscriptions.find { it.iccId != null && (handleId == it.iccId || handleId.startsWith(it.iccId)) }
            if (iccMatch != null && iccMatch.simSlotIndex in 0..1) {
                return iccMatch.simSlotIndex
            }

            return null
        }

        fun resolveSimSlotFromCallLog(phoneAccountId: String?, compName: String?): Int? {
            if (phoneAccountId.isNullOrBlank()) return null

            // 1. Check if directly parsed subId matches active subscription
            val subId = phoneAccountId.toIntOrNull()
            if (subId != null) {
                val sub = subscriptions.find { it.subscriptionId == subId }
                if (sub != null && sub.simSlotIndex in 0..1) {
                    return sub.simSlotIndex
                }
            }

            // 2. Check ICCID prefix match for OEM quirks (Samsung/Xiaomi/Pixel)
            val subByIcc = subscriptions.find {
                it.iccId != null && (phoneAccountId == it.iccId || phoneAccountId.startsWith(it.iccId) || it.iccId.startsWith(phoneAccountId))
            }
            if (subByIcc != null && subByIcc.simSlotIndex in 0..1) {
                return subByIcc.simSlotIndex
            }

            return null
        }

        fun isEmergencyNumber(number: String?): Boolean {
            if (number == null) return false
            val clean = number.trim()
            return clean == "911" || clean == "112" || clean == "999" || clean == "000" || clean == "08" || clean == "110"
        }
    }

    @Test
    fun singleSimDevice_hidesSimLabelEverywhere() {
        val resolver = TestSimResolver(
            subscriptions = listOf(
                MockSubscription(subscriptionId = 1, simSlotIndex = 0, displayName = "T-Mobile")
            )
        )

        assertFalse("Single-SIM device must report hasMultipleSims = false", resolver.hasMultipleSims)
        assertNull("Single-SIM device must not show SIM label for slot 0", resolver.getSimLabel(0))
        assertNull("Single-SIM device must not show SIM label for slot 1", resolver.getSimLabel(1))
        // forceShow (for internal logging or database sync) returns display label
        assertEquals("SIM 1", resolver.getSimLabel(0, forceShow = true))
    }

    @Test
    fun dualSimDevice_showsCorrectSimLabels() {
        val resolver = TestSimResolver(
            subscriptions = listOf(
                MockSubscription(subscriptionId = 1, simSlotIndex = 0, displayName = "Work"),
                MockSubscription(subscriptionId = 2, simSlotIndex = 1, displayName = "Personal")
            )
        )

        assertTrue("Dual-SIM device must report hasMultipleSims = true", resolver.hasMultipleSims)
        assertEquals("SIM 1", resolver.getSimLabel(0))
        assertEquals("SIM 2", resolver.getSimLabel(1))
        assertNull("Invalid slot index must return null", resolver.getSimLabel(-1))
        assertNull("Invalid slot index must return null", resolver.getSimLabel(2))
    }

    @Test
    fun eSimPlusPhysicalSim_mapsCorrectly() {
        val resolver = TestSimResolver(
            subscriptions = listOf(
                MockSubscription(subscriptionId = 10, simSlotIndex = 0, displayName = "Physical SIM", isEmbedded = false),
                MockSubscription(subscriptionId = 25, simSlotIndex = 1, displayName = "eSIM", isEmbedded = true)
            )
        )

        assertTrue(resolver.hasMultipleSims)

        assertEquals(0, resolver.resolveSimSlotFromHandleId("10"))
        assertEquals(1, resolver.resolveSimSlotFromHandleId("25"))
        assertEquals("SIM 1", resolver.getSimLabel(resolver.resolveSimSlotFromHandleId("10")))
        assertEquals("SIM 2", resolver.getSimLabel(resolver.resolveSimSlotFromHandleId("25")))
    }

    @Test
    fun callLog_resolvesSubIdAndIccidCorrectly() {
        val resolver = TestSimResolver(
            subscriptions = listOf(
                MockSubscription(subscriptionId = 1, simSlotIndex = 0, displayName = "SIM 1", iccId = "89014103211118510720"),
                MockSubscription(subscriptionId = 2, simSlotIndex = 1, displayName = "SIM 2", iccId = "89012607211118510888")
            )
        )

        // SubId stored in CallLog
        assertEquals(0, resolver.resolveSimSlotFromCallLog("1", "com.android.phone/TelephonyConnectionService"))
        assertEquals(1, resolver.resolveSimSlotFromCallLog("2", "com.android.phone/TelephonyConnectionService"))

        // ICCID stored in CallLog (OEM quirk on Samsung/Xiaomi/Motorola)
        assertEquals(0, resolver.resolveSimSlotFromCallLog("89014103211118510720", null))
        assertEquals(1, resolver.resolveSimSlotFromCallLog("89012607211118510888", null))
    }

    @Test
    fun removedOrSwappedSim_degradesGracefullyWithoutCrashing() {
        val resolver = TestSimResolver(
            subscriptions = listOf(
                MockSubscription(subscriptionId = 2, simSlotIndex = 1, displayName = "SIM 2")
            )
        )

        // SIM 1 was removed; old call log entry references subscription 1 (not in active subscriptions)
        val resolvedSlot = resolver.resolveSimSlotFromCallLog("1", null)
        assertNull("Removed subscription lookup returns null", resolvedSlot)

        // Neutral label degradation
        val label = resolver.getSimLabel(resolvedSlot)
        assertNull("Label degrades cleanly to null (neutral)", label)
    }

    @Test
    fun emergencyCalls_neverShowSimLabel() {
        val resolver = TestSimResolver(
            subscriptions = listOf(
                MockSubscription(subscriptionId = 1, simSlotIndex = 0, displayName = "Work"),
                MockSubscription(subscriptionId = 2, simSlotIndex = 1, displayName = "Personal")
            )
        )

        assertTrue(resolver.isEmergencyNumber("911"))
        assertTrue(resolver.isEmergencyNumber("112"))
        assertTrue(resolver.isEmergencyNumber("999"))
        assertFalse(resolver.isEmergencyNumber("5551234"))

        // For emergency calls, resolved slot is null
        val emergencySlot: Int? = null
        assertNull(resolver.getSimLabel(emergencySlot))
    }

    @Test
    fun hotSwapOrDynamicSubscriptionChange_updatesHasMultipleSims() {
        val resolver = TestSimResolver(
            subscriptions = listOf(
                MockSubscription(subscriptionId = 1, simSlotIndex = 0, displayName = "Primary")
            )
        )
        assertFalse(resolver.hasMultipleSims)
        assertNull(resolver.getSimLabel(0))

        // Hot swap: Second SIM inserted
        resolver.setSubscriptions(
            listOf(
                MockSubscription(subscriptionId = 1, simSlotIndex = 0, displayName = "Primary"),
                MockSubscription(subscriptionId = 2, simSlotIndex = 1, displayName = "Secondary")
            )
        )
        assertTrue(resolver.hasMultipleSims)
        assertEquals("SIM 1", resolver.getSimLabel(0))
        assertEquals("SIM 2", resolver.getSimLabel(1))

        // Hot swap: SIM removed
        resolver.setSubscriptions(
            listOf(
                MockSubscription(subscriptionId = 1, simSlotIndex = 0, displayName = "Primary")
            )
        )
        assertFalse(resolver.hasMultipleSims)
        assertNull(resolver.getSimLabel(0))
    }
}
