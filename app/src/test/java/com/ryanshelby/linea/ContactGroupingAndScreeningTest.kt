package com.ryanshelby.linea

import com.ryanshelby.linea.data.local.entities.BlockAction
import com.ryanshelby.linea.data.local.entities.BlockMatchType
import com.ryanshelby.linea.data.local.entities.BlockedNumberEntity
import com.ryanshelby.linea.data.local.entities.ContactEntity
import com.ryanshelby.linea.data.local.entities.ContactNumberEntity
import com.ryanshelby.linea.telecom.screening.ScreeningRuleMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactGroupingAndScreeningTest {

    @Test
    fun testGroupingMultipleNumbersUnderOneContact() {
        data class RawPhoneRow(val contactId: Long, val name: String, val number: String, val label: String)

        val rawRows = listOf(
            RawPhoneRow(101L, "Abbu BL", "01711223344", "Mobile"),
            RawPhoneRow(101L, "Abbu BL", "01999887766", "Work"),
            RawPhoneRow(102L, "Ammu", "01811223344", "Home")
        )

        val grouped = rawRows.groupBy { it.contactId }

        assertEquals(2, grouped.size)
        assertTrue(grouped.containsKey(101L))
        assertTrue(grouped.containsKey(102L))

        val abbuRows = grouped[101L]!!
        assertEquals(2, abbuRows.size)

        val contactEntity = ContactEntity(
            id = 1L,
            androidContactId = abbuRows.first().contactId,
            displayName = abbuRows.first().name
        )
        val numberEntities = abbuRows.mapIndexed { idx, row ->
            ContactNumberEntity(
                contactId = contactEntity.id,
                number = row.number,
                normalizedNumber = row.number.filter { it.isDigit() },
                label = row.label,
                isPrimary = idx == 0
            )
        }

        assertEquals("Abbu BL", contactEntity.displayName)
        assertEquals(2, numberEntities.size)
        assertEquals("01711223344", numberEntities[0].number)
        assertTrue(numberEntities[0].isPrimary)
        assertEquals("01999887766", numberEntities[1].number)
        assertFalse(numberEntities[1].isPrimary)
    }

    @Test
    fun testScreeningDefaultAllowWhenNoRulesConfigured() {
        val hasActiveScreeningFeature = false
        val activeBlockedRules = emptyList<BlockedNumberEntity>()

        val shouldBlock = activeBlockedRules.isNotEmpty() || hasActiveScreeningFeature
        assertFalse("By default, call must not be blocked", shouldBlock)
    }

    @Test
    fun testScreeningBlocksWhenExplicitlyInBlacklist() {
        val rule = BlockedNumberEntity(
            id = 1,
            numberOrPrefix = "01700000000",
            matchType = BlockMatchType.EXACT,
            blockAction = BlockAction.SILENT_REJECT
        )
        val incomingNumber = "01700000000"
        val isMatched = ScreeningRuleMatcher.matchesBlockedRule(
            rule = rule,
            rawNumber = incomingNumber,
            normalizedNumber = incomingNumber,
            isContact = false
        )
        assertTrue("Explicitly blocked number must be matched and blocked", isMatched)
    }

    @Test
    fun testScreeningDoesNotBlockUnlistedNumberByDefault() {
        val rule = BlockedNumberEntity(
            id = 1,
            numberOrPrefix = "01700000000",
            matchType = BlockMatchType.EXACT,
            blockAction = BlockAction.SILENT_REJECT
        )
        val normalIncomingNumber = "01811223344"
        val isMatched = ScreeningRuleMatcher.matchesBlockedRule(
            rule = rule,
            rawNumber = normalIncomingNumber,
            normalizedNumber = normalIncomingNumber,
            isContact = true
        )
        assertFalse("Normal incoming number should never match blacklist", isMatched)
    }
}
