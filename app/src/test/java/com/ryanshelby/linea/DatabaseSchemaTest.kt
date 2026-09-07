package com.ryanshelby.linea

import com.ryanshelby.linea.data.local.converters.Converters
import com.ryanshelby.linea.data.local.entities.BlockAction
import com.ryanshelby.linea.data.local.entities.BlockMatchType
import com.ryanshelby.linea.data.local.entities.BlockedNumberEntity
import com.ryanshelby.linea.data.local.entities.CallDirectionType
import com.ryanshelby.linea.data.local.entities.CallRecordEntity
import com.ryanshelby.linea.data.local.entities.CallRuleEntity
import com.ryanshelby.linea.data.local.entities.CallbackReminderEntity
import com.ryanshelby.linea.data.local.entities.ContactEntity
import com.ryanshelby.linea.data.local.entities.ContactNumberEntity
import com.ryanshelby.linea.data.local.entities.DialerProfileEntity
import com.ryanshelby.linea.data.local.entities.ReminderStatus
import com.ryanshelby.linea.data.local.entities.RuleAction
import com.ryanshelby.linea.data.local.entities.RuleAllowedFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseSchemaTest {

    private val converters = Converters()

    @Test
    fun testCallRecordEntitySchema() {
        val callRecord = CallRecordEntity(
            id = 1,
            phoneNumber = "+1234567890",
            formattedNumber = "+1 (234) 567-890",
            callerName = "Rahim",
            callType = CallDirectionType.INCOMING,
            timestamp = 1757289600000L,
            durationSeconds = 142,
            simSlot = 0,
            simDisplayName = "SIM 1",
            networkType = "VoLTE",
            isPrivateContact = false,
            isLocationOptIn = true,
            locationCategory = "Work",
            sessionGroupId = "session_rahim_1"
        )

        assertEquals(1L, callRecord.id)
        assertEquals("+1234567890", callRecord.phoneNumber)
        assertEquals(CallDirectionType.INCOMING, callRecord.callType)
        assertEquals("VoLTE", callRecord.networkType)
        assertEquals("Work", callRecord.locationCategory)
        assertEquals("session_rahim_1", callRecord.sessionGroupId)
    }

    @Test
    fun testContactAndNumberSchema() {
        val contact = ContactEntity(
            id = 10,
            displayName = "Rahim Shelby",
            isFavorite = true,
            isPinned = true,
            company = "Linea Labs",
            jobTitle = "Lead Engineer",
            preferredSimSlot = 0,
            allowDuringRestrictedHours = true
        )

        val number = ContactNumberEntity(
            id = 101,
            contactId = contact.id,
            number = "+8801700000000",
            normalizedNumber = "8801700000000",
            label = "Mobile",
            isPrimary = true
        )

        assertEquals("Rahim Shelby", contact.displayName)
        assertTrue(contact.isFavorite)
        assertTrue(contact.allowDuringRestrictedHours)
        assertEquals(10L, number.contactId)
        assertTrue(number.isPrimary)
    }

    @Test
    fun testBlockedNumberAndRuleSchema() {
        val blockedNumber = BlockedNumberEntity(
            id = 5,
            numberOrPrefix = "+1800",
            matchType = BlockMatchType.PREFIX,
            reason = "Toll free spam",
            blockAction = BlockAction.SILENT_REJECT
        )

        val callRule = CallRuleEntity(
            id = 1,
            name = "Night Restricted Mode",
            startTime = "22:00",
            endTime = "07:00",
            allowedFilter = RuleAllowedFilter.FAVORITES_ONLY,
            action = RuleAction.REJECT
        )

        assertEquals(BlockMatchType.PREFIX, blockedNumber.matchType)
        assertEquals(BlockAction.SILENT_REJECT, blockedNumber.blockAction)
        assertEquals(RuleAllowedFilter.FAVORITES_ONLY, callRule.allowedFilter)
    }

    @Test
    fun testTypeConverters() {
        assertEquals("INCOMING", converters.fromCallDirectionType(CallDirectionType.INCOMING))
        assertEquals(CallDirectionType.INCOMING, converters.toCallDirectionType("INCOMING"))

        assertEquals("PREFIX", converters.fromBlockMatchType(BlockMatchType.PREFIX))
        assertEquals(BlockMatchType.PREFIX, converters.toBlockMatchType("PREFIX"))

        assertEquals("FAVORITES_ONLY", converters.fromRuleAllowedFilter(RuleAllowedFilter.FAVORITES_ONLY))
        assertEquals(RuleAllowedFilter.FAVORITES_ONLY, converters.toRuleAllowedFilter("FAVORITES_ONLY"))

        assertEquals("PENDING", converters.fromReminderStatus(ReminderStatus.PENDING))
        assertEquals(ReminderStatus.PENDING, converters.toReminderStatus("PENDING"))
    }
}
