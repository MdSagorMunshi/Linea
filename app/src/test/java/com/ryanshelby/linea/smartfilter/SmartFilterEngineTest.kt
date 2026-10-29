package com.ryanshelby.linea.smartfilter

import com.ryanshelby.linea.data.local.entities.ContactEmailEntity
import com.ryanshelby.linea.data.local.entities.ContactEntity
import com.ryanshelby.linea.data.local.entities.ContactNumberEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartFilterEngineTest {

    @Test
    fun jaroWinklerSimilarity_exactMatchReturnsOne() {
        assertEquals(1.0, SmartFilterEngine.jaroWinklerSimilarity("Alexander", "Alexander"), 0.001)
        assertEquals(1.0, SmartFilterEngine.jaroWinklerSimilarity("john doe", "John Doe"), 0.001)
    }

    @Test
    fun jaroWinklerSimilarity_similarNamesScoreHigh() {
        val score = SmartFilterEngine.jaroWinklerSimilarity("Jonathan Doe", "John Doe")
        assertTrue("Expected similarity >= 0.85, got $score", score >= 0.85)

        val scoreTransposed = SmartFilterEngine.jaroWinklerSimilarity("Robert Fox", "Bob Fox")
        assertTrue("Expected similarity > 0.0", scoreTransposed > 0.0)
    }

    @Test
    fun normalizePhoneNumber_stripsFormattingCharacters() {
        assertEquals("+15551234567", SmartFilterEngine.normalizePhoneNumber("+1 (555) 123-4567"))
        assertEquals("5551234", SmartFilterEngine.normalizePhoneNumber("555-1234"))
        assertEquals("15551234", SmartFilterEngine.normalizePhoneNumber("(1) 555.1234"))
    }

    @Test
    fun areNumbersEquivalent_matchesNationalAndInternationalFormats() {
        assertTrue(SmartFilterEngine.areNumbersEquivalent("+1 (555) 019-2834", "5550192834"))
        assertTrue(SmartFilterEngine.areNumbersEquivalent("1-555-019-2834", "555-019-2834"))
        assertTrue(SmartFilterEngine.areNumbersEquivalent("+44 20 7946 0912", "020 7946 0912"))
        assertFalse(SmartFilterEngine.areNumbersEquivalent("5551111", "5552222"))
    }

    @Test
    fun cleanName_convertsAllCapsToTitleCase() {
        val cleanup = SmartFilterEngine.evaluateNameCleanup("JOHN DOE")
        assertNotNull(cleanup)
        assertEquals("John Doe", cleanup?.proposedName)
        assertEquals(CleanupType.ALL_CAPS, cleanup?.cleanupType)
    }

    @Test
    fun cleanName_convertsAllLowercaseToTitleCase() {
        val cleanup = SmartFilterEngine.evaluateNameCleanup("jane marie watson")
        assertNotNull(cleanup)
        assertEquals("Jane Marie Watson", cleanup?.proposedName)
        assertEquals(CleanupType.ALL_LOWERCASE, cleanup?.cleanupType)
    }

    @Test
    fun cleanName_removesRedundantExtraSpaces() {
        val cleanup = SmartFilterEngine.evaluateNameCleanup("Robert   M.    Smith")
        assertNotNull(cleanup)
        assertEquals("Robert M. Smith", cleanup?.proposedName)
        assertEquals(CleanupType.EXTRA_SPACES, cleanup?.cleanupType)
    }

    @Test
    fun cleanName_handlesParticlesAndSuffixes() {
        assertEquals("Vincent van Gogh", SmartFilterEngine.cleanName("VINCENT VAN GOGH"))
        assertEquals("John Smith Jr.", SmartFilterEngine.cleanName("JOHN SMITH JR."))
        assertEquals("Henry Tudor VIII", SmartFilterEngine.cleanName("HENRY TUDOR VIII"))
    }

    @Test
    fun findDuplicateMatches_detectsExactPhoneDuplicate() {
        val profileA = ContactProfile(
            contact = ContactEntity(id = 1, displayName = "Alice Smith"),
            numbers = listOf(ContactNumberEntity(contactId = 1, number = "+1 (555) 111-2222", normalizedNumber = "15551112222"))
        )
        val profileB = ContactProfile(
            contact = ContactEntity(id = 2, displayName = "Alice Smith"),
            numbers = listOf(ContactNumberEntity(contactId = 2, number = "555-111-2222", normalizedNumber = "5551112222"))
        )

        val matches = SmartFilterEngine.findDuplicateMatches(listOf(profileA, profileB), SmartFilterConfig())
        assertEquals(1, matches.size)
        val match = matches.first()
        assertEquals(100, match.confidence)
        assertEquals(MatchType.EXACT_PHONE, match.matchType)
        assertTrue(match.explanation.contains("Identical phone number"))
    }

    @Test
    fun findDuplicateMatches_detectsSameNumberDifferentName() {
        val profileA = ContactProfile(
            contact = ContactEntity(id = 1, displayName = "Mom"),
            numbers = listOf(ContactNumberEntity(contactId = 1, number = "555-999-0000", normalizedNumber = "5559990000"))
        )
        val profileB = ContactProfile(
            contact = ContactEntity(id = 2, displayName = "Mary Jane"),
            numbers = listOf(ContactNumberEntity(contactId = 2, number = "+15559990000", normalizedNumber = "15559990000"))
        )

        val matches = SmartFilterEngine.findDuplicateMatches(listOf(profileA, profileB), SmartFilterConfig())
        assertEquals(1, matches.size)
        val match = matches.first()
        assertEquals(90, match.confidence)
        assertEquals(MatchType.SHARED_NUMBER_DIFFERENT_NAME, match.matchType)
    }

    @Test
    fun findDuplicateMatches_detectsSameEmailDuplicate() {
        val profileA = ContactProfile(
            contact = ContactEntity(id = 1, displayName = "Bob Fox"),
            emails = listOf(ContactEmailEntity(contactId = 1, email = "bob.fox@example.com"))
        )
        val profileB = ContactProfile(
            contact = ContactEntity(id = 2, displayName = "Robert Fox"),
            emails = listOf(ContactEmailEntity(contactId = 2, email = "bob.fox@example.com"))
        )

        val matches = SmartFilterEngine.findDuplicateMatches(listOf(profileA, profileB), SmartFilterConfig())
        assertEquals(1, matches.size)
        val match = matches.first()
        assertEquals(90, match.confidence)
        assertEquals(MatchType.SAME_EMAIL, match.matchType)
    }

    @Test
    fun findDuplicateMatches_detectsTransposedNames() {
        val profileA = ContactProfile(
            contact = ContactEntity(id = 1, displayName = "Wayne Bruce"),
            numbers = listOf(ContactNumberEntity(contactId = 1, number = "5551234", normalizedNumber = "5551234"))
        )
        val profileB = ContactProfile(
            contact = ContactEntity(id = 2, displayName = "Bruce Wayne"),
            numbers = listOf(ContactNumberEntity(contactId = 2, number = "5559876", normalizedNumber = "5559876"))
        )

        val matches = SmartFilterEngine.findDuplicateMatches(listOf(profileA, profileB), SmartFilterConfig())
        assertEquals(1, matches.size)
        val match = matches.first()
        assertEquals(88, match.confidence)
        assertEquals(MatchType.HIGH_NAME_SIMILARITY, match.matchType)
    }

    @Test
    fun generateMergePreview_combinesNumbersAndEmailsWithoutLoss() {
        val profileA = ContactProfile(
            contact = ContactEntity(id = 1, displayName = "Sarah Connor", company = "Cyberdyne", notes = "Note 1"),
            numbers = listOf(ContactNumberEntity(contactId = 1, number = "555-1111", normalizedNumber = "5551111", label = "Mobile")),
            emails = listOf(ContactEmailEntity(contactId = 1, email = "sarah@resistance.org"))
        )
        val profileB = ContactProfile(
            contact = ContactEntity(id = 2, displayName = "Sarah", company = null, notes = "Note 2"),
            numbers = listOf(ContactNumberEntity(contactId = 2, number = "555-2222", normalizedNumber = "5552222", label = "Home")),
            emails = listOf(ContactEmailEntity(contactId = 2, email = "sarah.connor@gmail.com"))
        )

        val preview = SmartFilterEngine.generateMergePreview(profileA, profileB)
        assertEquals("Sarah Connor", preview.proposedDisplayName)
        assertEquals("Cyberdyne", preview.proposedCompany)
        assertEquals(2, preview.mergedNumbers.size)
        assertTrue(preview.mergedNumbers.any { it.first == "555-1111" })
        assertTrue(preview.mergedNumbers.any { it.first == "555-2222" })
        assertEquals(2, preview.mergedEmails.size)
        assertTrue(preview.mergedNotes?.contains("Note 1") == true)
        assertTrue(preview.mergedNotes?.contains("Note 2") == true)
    }

    @Test
    fun evaluateAddressBookQuality_computesAccurateHealthScore() {
        val cleanProfile = ContactProfile(
            contact = ContactEntity(id = 1, displayName = "Clark Kent", company = "Daily Planet"),
            numbers = listOf(ContactNumberEntity(contactId = 1, number = "555-0100", normalizedNumber = "5550100", label = "Work")),
            emails = listOf(ContactEmailEntity(contactId = 1, email = "clark@dailyplanet.com"))
        )

        val report = SmartFilterEngine.evaluateAddressBookQuality(
            profiles = listOf(cleanProfile),
            duplicateMatches = emptyList(),
            cleanupItems = emptyList(),
            duplicateNumberGroups = emptyList()
        )

        assertTrue("Expected score >= 85, got ${report.overallScore}", report.overallScore >= 85)
        assertEquals("A", report.letterGrade.take(1))
    }
}
