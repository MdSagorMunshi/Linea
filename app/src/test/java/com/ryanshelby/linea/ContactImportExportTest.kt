package com.ryanshelby.linea

import com.ryanshelby.linea.data.contacts.ContactExportModel
import com.ryanshelby.linea.data.contacts.ContactImportExportManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ContactImportExportTest {

    private lateinit var manager: ContactImportExportManager

    @Before
    fun setUp() {
        manager = ContactImportExportManager()
    }

    @Test
    fun testVCard_exportAndParse_roundTrip() {
        val sampleContacts = listOf(
            ContactExportModel(
                displayName = "Rahim Uddin",
                company = "Acme Corp",
                phoneNumbers = listOf("+8801712345678" to "Mobile", "+8801811223344" to "Work"),
                emails = listOf("rahim@example.com"),
                notes = "Important client, call after 2pm",
                isFavorite = true
            ),
            ContactExportModel(
                displayName = "Sarah Connor",
                phoneNumbers = listOf("+14165550199" to "Home"),
                emails = listOf("sarah@resistance.org", "s.connor@personal.net"),
                isFavorite = false
            )
        )

        val vcfString = manager.generateVcfString(sampleContacts)
        assertTrue(vcfString.contains("BEGIN:VCARD"))
        assertTrue(vcfString.contains("FN:Rahim Uddin"))
        assertTrue(vcfString.contains("TEL;TYPE=CELL:+8801712345678"))
        assertTrue(vcfString.contains("TEL;TYPE=WORK:+8801811223344"))
        assertTrue(vcfString.contains("EMAIL;TYPE=INTERNET:rahim@example.com"))
        assertTrue(vcfString.contains("ORG:Acme Corp"))
        assertTrue(vcfString.contains("X-STARRED:1"))
        assertTrue(vcfString.contains("END:VCARD"))

        val parsed = manager.parseVcfString(vcfString)
        assertEquals(2, parsed.size)

        val c1 = parsed[0]
        assertEquals("Rahim Uddin", c1.displayName)
        assertEquals("Acme Corp", c1.company)
        assertEquals(2, c1.phoneNumbers.size)
        assertEquals("+8801712345678", c1.phoneNumbers[0].first)
        assertEquals("rahim@example.com", c1.emails[0])
        assertEquals("Important client, call after 2pm", c1.notes)
        assertTrue(c1.isFavorite)

        val c2 = parsed[1]
        assertEquals("Sarah Connor", c2.displayName)
        assertEquals("+14165550199", c2.phoneNumbers[0].first)
        assertEquals(2, c2.emails.size)
        assertFalse(c2.isFavorite)
    }

    @Test
    fun testCsv_exportAndParse_roundTrip() {
        val sampleContacts = listOf(
            ContactExportModel(
                displayName = "O'Connor, John",
                company = "Cyberdyne Systems, Inc.",
                phoneNumbers = listOf("+12125551234" to "Work", "+12125555678" to "Mobile"),
                emails = listOf("john@cyberdyne.com"),
                notes = "Line 1\nLine 2 with \"quotes\"",
                isFavorite = true
            ),
            ContactExportModel(
                displayName = "Bruce Wayne",
                phoneNumbers = listOf("+14155559999" to "Mobile"),
                emails = listOf("bruce@wayne-enterprises.com"),
                isFavorite = false
            )
        )

        val csvString = manager.generateCsvString(sampleContacts)
        assertTrue(csvString.startsWith("Display Name,Company,"))

        val parsed = manager.parseCsvString(csvString)
        assertEquals(2, parsed.size)

        val c1 = parsed[0]
        assertEquals("O'Connor, John", c1.displayName)
        assertEquals("Cyberdyne Systems, Inc.", c1.company)
        assertEquals(2, c1.phoneNumbers.size)
        assertEquals("+12125551234", c1.phoneNumbers[0].first)
        assertEquals("Work", c1.phoneNumbers[0].second)
        assertEquals("john@cyberdyne.com", c1.emails[0])
        assertEquals("Line 1\nLine 2 with \"quotes\"", c1.notes)
        assertTrue(c1.isFavorite)

        val c2 = parsed[1]
        assertEquals("Bruce Wayne", c2.displayName)
        assertEquals("+14155559999", c2.phoneNumbers[0].first)
        assertFalse(c2.isFavorite)
    }

    @Test
    fun testJson_exportAndParse_roundTrip() {
        val sampleContacts = listOf(
            ContactExportModel(
                displayName = "Ada Lovelace",
                company = "Pioneer Computing",
                phoneNumbers = listOf("+447911123456" to "Mobile"),
                emails = listOf("ada@analytical-engine.org"),
                notes = "First programmer",
                isFavorite = true
            )
        )

        val jsonString = manager.generateJsonString(sampleContacts)
        assertTrue(jsonString.contains("\"displayName\": \"Ada Lovelace\""))

        val parsed = manager.parseJsonString(jsonString)
        assertEquals(1, parsed.size)
        assertEquals("Ada Lovelace", parsed[0].displayName)
        assertEquals("Pioneer Computing", parsed[0].company)
        assertEquals("+447911123456", parsed[0].phoneNumbers[0].first)
        assertEquals("Mobile", parsed[0].phoneNumbers[0].second)
        assertEquals("ada@analytical-engine.org", parsed[0].emails[0])
        assertEquals("First programmer", parsed[0].notes)
        assertTrue(parsed[0].isFavorite)
    }

    @Test
    fun testVCard_escapedSpecialCharacters() {
        val sampleContacts = listOf(
            ContactExportModel(
                displayName = "Smith, Agent; Matrix",
                phoneNumbers = listOf("01711223344" to "Mobile"),
                notes = "Multi, comma; semicolon\\backslash\nnew line"
            )
        )

        val vcf = manager.generateVcfString(sampleContacts)
        val parsed = manager.parseVcfString(vcf)
        assertEquals(1, parsed.size)
        assertEquals("Smith, Agent; Matrix", parsed[0].displayName)
        assertEquals("Multi, comma; semicolon\\backslash\nnew line", parsed[0].notes)
    }
}
