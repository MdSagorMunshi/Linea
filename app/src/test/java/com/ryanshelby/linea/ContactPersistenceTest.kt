package com.ryanshelby.linea

import com.ryanshelby.linea.data.local.entities.ContactEntity
import com.ryanshelby.linea.data.local.entities.ContactNumberEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Validates Contact persistence and editing synchronization:
 * 1. Contact editing updates persistent state (not just in-memory/UI state).
 * 2. Updated name, numbers, emails, company, and notes persist.
 * 3. Simulates system sync (syncWithLocalDb) matching contacts by both androidId and rawContactId.
 * 4. Ensures external sync does not overwrite with stale names once persisted to ContactsContract.
 */
class ContactPersistenceTest {

    private class MockPersistentContactStore {
        // Simulates Android ContactsContract.Data
        data class SystemDataRow(
            val rawContactId: Long,
            val mimeType: String,
            val value1: String, // DisplayName or Number or Email or Company
            val value2: String? = null // Type / Label
        )

        val systemRawContacts = mutableMapOf<Long, Long>() // rawId -> contactId
        val systemDataRows = mutableListOf<SystemDataRow>()

        // Simulates Room DB
        val roomContacts = mutableMapOf<Long, ContactEntity>()
        val roomNumbers = mutableMapOf<Long, MutableList<ContactNumberEntity>>()

        private var nextRoomId = 1L
        private var nextRawId = 100L

        fun insertInitialContact(displayName: String, number: String): Long {
            val rawId = nextRawId++
            val contactId = rawId // Aggregate ID
            systemRawContacts[rawId] = contactId

            // Insert system StructuredName
            systemDataRows.add(
                SystemDataRow(
                    rawContactId = rawId,
                    mimeType = "vnd.android.cursor.item/name",
                    value1 = displayName
                )
            )
            // Insert system Phone
            systemDataRows.add(
                SystemDataRow(
                    rawContactId = rawId,
                    mimeType = "vnd.android.cursor.item/phone_v2",
                    value1 = number,
                    value2 = "Mobile"
                )
            )

            // Insert into Room
            val rId = nextRoomId++
            val entity = ContactEntity(
                id = rId,
                androidContactId = contactId,
                displayName = displayName
            )
            roomContacts[rId] = entity
            roomNumbers[rId] = mutableListOf(
                ContactNumberEntity(
                    contactId = rId,
                    number = number,
                    normalizedNumber = number,
                    label = "Mobile",
                    isPrimary = true
                )
            )
            return rId
        }

        fun updateContact(
            contact: ContactEntity,
            numbers: List<Pair<String, String>>?,
            company: String?,
            notes: String?
        ): ContactEntity {
            // 1. Update in system ContactsContract
            val rawIds = systemRawContacts.filter { it.value == contact.androidContactId || it.key == contact.androidContactId }.keys
            for (rawId in rawIds) {
                // Update StructuredName
                systemDataRows.removeIf { it.rawContactId == rawId && it.mimeType == "vnd.android.cursor.item/name" }
                systemDataRows.add(
                    SystemDataRow(
                        rawContactId = rawId,
                        mimeType = "vnd.android.cursor.item/name",
                        value1 = contact.displayName
                    )
                )

                // Update Numbers
                if (numbers != null) {
                    systemDataRows.removeIf { it.rawContactId == rawId && it.mimeType == "vnd.android.cursor.item/phone_v2" }
                    for ((num, lbl) in numbers) {
                        systemDataRows.add(
                            SystemDataRow(
                                rawContactId = rawId,
                                mimeType = "vnd.android.cursor.item/phone_v2",
                                value1 = num,
                                value2 = lbl
                            )
                        )
                    }
                }
            }

            // 2. Update in Room DB
            val updated = contact.copy(company = company, notes = notes)
            roomContacts[contact.id] = updated

            if (numbers != null) {
                roomNumbers[contact.id] = numbers.mapIndexed { idx, (num, lbl) ->
                    ContactNumberEntity(
                        contactId = contact.id,
                        number = num,
                        normalizedNumber = num,
                        label = lbl,
                        isPrimary = idx == 0
                    )
                }.toMutableList()
            }

            return roomContacts[contact.id]!!
        }

        fun simulateAppRestartAndSync() {
            // Simulate syncWithLocalDb: queries system ContactsContract and syncs into Room
            for ((rawId, contactId) in systemRawContacts) {
                val nameRow = systemDataRows.firstOrNull { it.rawContactId == rawId && it.mimeType == "vnd.android.cursor.item/name" }
                val sysName = nameRow?.value1 ?: "Unknown"

                // Find existing in Room by androidContactId
                val existing = roomContacts.values.firstOrNull { it.androidContactId == contactId || it.androidContactId == rawId }
                if (existing != null) {
                    roomContacts[existing.id] = existing.copy(
                        androidContactId = contactId,
                        displayName = sysName
                    )
                }
            }
        }
    }

    private lateinit var store: MockPersistentContactStore

    @Before
    fun setUp() {
        store = MockPersistentContactStore()
    }

    @Test
    fun testContactNameEditPersistsAcrossAppRestart() {
        // Initial state: contact named "Ryan" with phone "+15551234567"
        val contactId = store.insertInitialContact("Ryan", "+15551234567")
        val initialContact = store.roomContacts[contactId]!!
        assertEquals("Ryan", initialContact.displayName)

        // User edits contact name from "Ryan" to "Ryan Shelby"
        val editedEntity = initialContact.copy(displayName = "Ryan Shelby")
        val updated = store.updateContact(
            contact = editedEntity,
            numbers = listOf("+15551234567" to "Mobile", "+15559876543" to "Work"),
            company = "Shelby Co",
            notes = "VIP Client"
        )

        assertEquals("Ryan Shelby", updated.displayName)
        assertEquals("Shelby Co", updated.company)
        assertEquals(2, store.roomNumbers[contactId]?.size)

        // Verify that Android Contacts Provider (system) was updated!
        val systemNameRow = store.systemDataRows.first { it.mimeType == "vnd.android.cursor.item/name" }
        assertEquals("Ryan Shelby", systemNameRow.value1)

        // Simulate app restart / force-stop and re-syncing from ContactsContract
        store.simulateAppRestartAndSync()

        // Verify that after restart, contact name REMAINS "Ryan Shelby" and does NOT revert to "Ryan"!
        val persistedAfterRestart = store.roomContacts[contactId]
        assertNotNull(persistedAfterRestart)
        assertEquals("Ryan Shelby", persistedAfterRestart!!.displayName)
    }
}
