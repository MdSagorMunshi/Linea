package com.ryanshelby.linea.mergefix

import com.ryanshelby.linea.data.local.entities.ContactEmailEntity
import com.ryanshelby.linea.data.local.entities.ContactEntity
import com.ryanshelby.linea.data.local.entities.ContactNumberEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactMergeFixTest {

    // =====================================================================
    // 1. NORMALIZATION TESTS
    // =====================================================================

    @Test
    fun testPhoneNumberNormalization_convertsToE164() {
        assertEquals("+8801712345678", ContactNormalizer.normalizePhoneNumber("01712345678"))
        assertEquals("+8801712345678", ContactNormalizer.normalizePhoneNumber("01712-345-678"))
        assertEquals("+8801712345678", ContactNormalizer.normalizePhoneNumber("+8801712345678"))
        assertEquals("+8801712345678", ContactNormalizer.normalizePhoneNumber("008801712345678"))
        assertEquals("+8801712345678", ContactNormalizer.normalizePhoneNumber("01712 345 678"))
        assertEquals("+8801712345678", ContactNormalizer.normalizePhoneNumber("(01712) 345-678"))

        // Global country normalization
        assertEquals("+447911123456", ContactNormalizer.normalizePhoneNumber("07911 123456", defaultCountryIso = "GB"))
        assertEquals("+15552345678", ContactNormalizer.normalizePhoneNumber("(555) 234-5678", defaultCountryIso = "US"))
        assertEquals("+61412345678", ContactNormalizer.normalizePhoneNumber("0412 345 678", defaultCountryIso = "AU"))
        assertEquals("+491711234567", ContactNormalizer.normalizePhoneNumber("0171 1234567", defaultCountryIso = "DE"))
        assertEquals("+33612345678", ContactNormalizer.normalizePhoneNumber("06 12 34 56 78", defaultCountryIso = "FR"))
        assertEquals("+919876543210", ContactNormalizer.normalizePhoneNumber("098765 43210", defaultCountryIso = "IN"))

        // Global numbers with explicit + are always preserved regardless of default country
        assertEquals("+447911123456", ContactNormalizer.normalizePhoneNumber("+44 7911 123456", defaultCountryIso = "US"))
        assertEquals("+819012345678", ContactNormalizer.normalizePhoneNumber("+81 90 1234 5678", defaultCountryIso = "BD"))
        assertEquals("+33612345678", ContactNormalizer.normalizePhoneNumber("+33 6 12 34 56 78", defaultCountryIso = "GB"))

        // Global 00 international prefix
        assertEquals("+447911123456", ContactNormalizer.normalizePhoneNumber("0044 7911 123456"))
        assertEquals("+15552345678", ContactNormalizer.normalizePhoneNumber("001 555 234 5678"))
    }

    @Test
    fun testDisplayNameNormalization_removesHonorificsAndTrims() {
        assertEquals("rahim uddin", ContactNormalizer.normalizeDisplayName("Dr. Rahim Uddin"))
        assertEquals("rahim uddin", ContactNormalizer.normalizeDisplayName("dr rahim uddin"))
        assertEquals("john doe", ContactNormalizer.normalizeDisplayName("Prof. John Doe"))
        assertEquals("rahim", ContactNormalizer.normalizeDisplayName("Md. Rahim"))
        assertEquals("alvee", ContactNormalizer.normalizeDisplayName("Engr. Alvee"))
        assertEquals("sarah connor", ContactNormalizer.normalizeDisplayName("  SARAH   CONNOR  "))

        // Global international honorifics
        assertEquals("schmidt", ContactNormalizer.normalizeDisplayName("Herr Schmidt"))
        assertEquals("dupont", ContactNormalizer.normalizeDisplayName("Monsieur Dupont"))
        assertEquals("gonzalez", ContactNormalizer.normalizeDisplayName("Señor Gonzalez"))
        assertEquals("sharma", ContactNormalizer.normalizeDisplayName("Advocate Sharma"))
        assertEquals("abdullah", ContactNormalizer.normalizeDisplayName("Sheikh Abdullah"))
        assertEquals("miller", ContactNormalizer.normalizeDisplayName("Pastor Miller"))
    }

    @Test
    fun testEmailNormalization() {
        assertEquals("john@example.com", ContactNormalizer.normalizeEmail("  JOHN@EXAMPLE.COM  "))
    }

    // =====================================================================
    // 2. PHASE ONE: DUPLICATE DETECTION TIERS
    // =====================================================================

    @Test
    fun testTierOne_exactPhoneAndNameMatch_producesHighConfidence() {
        val c1 = createNormalizedContact(
            id = 1,
            name = "Rahim",
            numbers = listOf("01712345678")
        )
        val c2 = createNormalizedContact(
            id = 2,
            name = "rahim",
            numbers = listOf("01712-345-678")
        )

        val pair = ContactMatchEngine.evaluatePair(c1, c2)
        assertNotNull(pair)
        assertEquals(ConfidenceTier.HIGH, pair?.confidence)
    }

    @Test
    fun testTierOne_exactPhoneNamelessMatch_producesHighConfidence() {
        val c1 = createNormalizedContact(
            id = 1,
            name = "",
            numbers = listOf("01712345678")
        )
        val c2 = createNormalizedContact(
            id = 2,
            name = "",
            numbers = listOf("+8801712345678")
        )

        val pair = ContactMatchEngine.evaluatePair(c1, c2)
        assertNotNull(pair)
        assertEquals(ConfidenceTier.HIGH, pair?.confidence)
    }

    @Test
    fun testNoSharedPhoneOrEmail_neverMatchesRegardlessOfName() {
        // Identical names but different phone numbers and no shared email must NEVER match
        val c1 = createNormalizedContact(
            id = 1,
            name = "Rahim",
            numbers = listOf("01711111111")
        )
        val c2 = createNormalizedContact(
            id = 2,
            name = "rahim",
            numbers = listOf("01722222222")
        )
        val pair1 = ContactMatchEngine.evaluatePair(c1, c2)
        assertNull(pair1)

        // Nicknames / fuzzy names with different numbers must NEVER match
        val c3 = createNormalizedContact(id = 3, name = "Bob", numbers = listOf("01711111111"))
        val c4 = createNormalizedContact(id = 4, name = "Robert", numbers = listOf("01722222222"))
        val pair2 = ContactMatchEngine.evaluatePair(c3, c4)
        assertNull(pair2)

        // Weak signal "Mom" vs "Ammu" with different numbers must NEVER match
        val c5 = createNormalizedContact(id = 5, name = "Mom", numbers = listOf("01711111111"))
        val c6 = createNormalizedContact(id = 6, name = "Ammu", numbers = listOf("01722222222"))
        val pair3 = ContactMatchEngine.evaluatePair(c5, c6)
        assertNull(pair3)
    }

    @Test
    fun testTierTwo_sameNumberDifferentNames_producesMediumConfidence() {
        val c1 = createNormalizedContact(
            id = 1,
            name = "Rayan Shelby",
            numbers = listOf("01711111111")
        )
        val c2 = createNormalizedContact(
            id = 2,
            name = "Ryan Shelby",
            numbers = listOf("01711111111")
        )

        val pair = ContactMatchEngine.evaluatePair(c1, c2)
        assertNotNull(pair)
        assertEquals(ConfidenceTier.MEDIUM, pair?.confidence)
    }

    @Test
    fun testTierTwo_sameNumberFullNameAndAbbreviation_producesMediumConfidence() {
        val c1 = createNormalizedContact(
            id = 1,
            name = "Rahim Uddin",
            numbers = listOf("01711111111")
        )
        val c2 = createNormalizedContact(
            id = 2,
            name = "Rahim U.",
            numbers = listOf("01711111111")
        )

        val pair = ContactMatchEngine.evaluatePair(c1, c2)
        assertNotNull(pair)
        assertEquals(ConfidenceTier.MEDIUM, pair?.confidence)
    }

    @Test
    fun testTierTwo_sameNumberPhoneticVariation_producesMediumConfidence() {
        // "Jon" vs "John" with same number
        val c1 = createNormalizedContact(id = 1, name = "Jon", numbers = listOf("01711111111"))
        val c2 = createNormalizedContact(id = 2, name = "John", numbers = listOf("01711111111"))
        val pair1 = ContactMatchEngine.evaluatePair(c1, c2)
        assertNotNull(pair1)
        assertEquals(ConfidenceTier.MEDIUM, pair1?.confidence)

        // "Mohammad" vs "Muhammed" with same number
        val c3 = createNormalizedContact(id = 3, name = "Mohammad", numbers = listOf("01722222222"))
        val c4 = createNormalizedContact(id = 4, name = "Muhammed", numbers = listOf("01722222222"))
        val pair2 = ContactMatchEngine.evaluatePair(c3, c4)
        assertNotNull(pair2)
        assertEquals(ConfidenceTier.MEDIUM, pair2?.confidence)

        // "Shuvo" vs "Shouvo" with same number
        val c5 = createNormalizedContact(id = 5, name = "Shuvo", numbers = listOf("01733333333"))
        val c6 = createNormalizedContact(id = 6, name = "Shouvo", numbers = listOf("01733333333"))
        val pair3 = ContactMatchEngine.evaluatePair(c5, c6)
        assertNotNull(pair3)
        assertEquals(ConfidenceTier.MEDIUM, pair3?.confidence)
    }

    @Test
    fun testTierTwo_sameNumberNicknameTable_producesMediumConfidence() {
        // "Bob" vs "Robert" with same number
        val c1 = createNormalizedContact(id = 1, name = "Bob", numbers = listOf("01711111111"))
        val c2 = createNormalizedContact(id = 2, name = "Robert", numbers = listOf("01711111111"))
        val pair1 = ContactMatchEngine.evaluatePair(c1, c2)
        assertNotNull(pair1)
        assertEquals(ConfidenceTier.MEDIUM, pair1?.confidence)

        // "Mike" vs "Michael" with same number
        val c3 = createNormalizedContact(id = 3, name = "Mike", numbers = listOf("01722222222"))
        val c4 = createNormalizedContact(id = 4, name = "Michael", numbers = listOf("01722222222"))
        val pair2 = ContactMatchEngine.evaluatePair(c3, c4)
        assertNotNull(pair2)
        assertEquals(ConfidenceTier.MEDIUM, pair2?.confidence)
    }

    // =====================================================================
    // 3. HARD RULES TESTS
    // =====================================================================

    @Test
    fun testHardRule_vaultContactsCompletelyOffLimits() {
        val vaultContact = createNormalizedContact(
            id = 1,
            name = "Rahim",
            numbers = listOf("01712345678"),
            isPrivate = true
        )
        val regularContact = createNormalizedContact(
            id = 2,
            name = "Rahim",
            numbers = listOf("01712345678"),
            isPrivate = false
        )

        val pair = ContactMatchEngine.evaluatePair(vaultContact, regularContact)
        assertNull(pair)
    }

    @Test
    fun testHardRule_dualSimAwareness_differentSimSlotsNotDuplicates() {
        val sim1Contact = createNormalizedContact(
            id = 1,
            name = "Alice Work",
            numbers = listOf("01712345678"),
            preferredSimSlot = 0
        )
        val sim2Contact = createNormalizedContact(
            id = 2,
            name = "Alice Personal",
            numbers = listOf("01712345678"),
            preferredSimSlot = 1
        )

        val pair = ContactMatchEngine.evaluatePair(sim1Contact, sim2Contact)
        assertNull(pair)
    }

    // =====================================================================
    // 4. PHASE TWO: CANDIDATE GROUPING TESTS
    // =====================================================================

    @Test
    fun testCandidateGrouping_sameNumberDifferentNames_formsMediumGroup() {
        val c1 = createNormalizedContact(id = 1, name = "Rahim", numbers = listOf("01712345678"))
        val c2 = createNormalizedContact(id = 2, name = "Rahim Uddin", numbers = listOf("01712-345-678"))
        val c3 = createNormalizedContact(id = 3, name = "Rahim U.", numbers = listOf("+8801712345678"))

        val groups = ContactGroupingEngine.findMergeGroups(listOf(c1, c2, c3))
        assertEquals(1, groups.size)
        assertEquals(3, groups[0].contacts.size)
        // Group confidence is MEDIUM because names differ on the same phone number (requires user choice)
        assertEquals(ConfidenceTier.MEDIUM, groups[0].confidence)
    }

    @Test
    fun testCandidateGrouping_exactPhoneAndName_formsHighGroup() {
        val c1 = createNormalizedContact(id = 1, name = "Rahim", numbers = listOf("01712345678"))
        val c2 = createNormalizedContact(id = 2, name = "Rahim", numbers = listOf("01712-345-678"))
        val c3 = createNormalizedContact(id = 3, name = "rahim", numbers = listOf("+8801712345678"))

        val groups = ContactGroupingEngine.findMergeGroups(listOf(c1, c2, c3))
        assertEquals(1, groups.size)
        assertEquals(3, groups[0].contacts.size)
        assertEquals(ConfidenceTier.HIGH, groups[0].confidence)
    }

    @Test
    fun testMergePreview_allowsUserToChooseName() {
        val c1 = createNormalizedContact(id = 1, name = "Rahim", numbers = listOf("01712345678"))
        val c2 = createNormalizedContact(id = 2, name = "Rahim Uddin", numbers = listOf("01712345678"))
        val groups = ContactGroupingEngine.findMergeGroups(listOf(c1, c2))
        assertEquals(1, groups.size)

        val group = groups[0]
        // Default preview picks the longest name
        val defaultPreview = ContactMergeExecutor.computePreview(group)
        assertEquals("Rahim Uddin", defaultPreview.selectedDisplayName)
        assertEquals(listOf("Rahim", "Rahim Uddin"), defaultPreview.candidateNames)

        // User chooses the first name ("Rahim")
        val rahimPreview = ContactMergeExecutor.computePreview(group, chosenDisplayName = "Rahim")
        assertEquals("Rahim", rahimPreview.selectedDisplayName)

        // User enters a custom name
        val customPreview = ContactMergeExecutor.computePreview(group, chosenDisplayName = "Rahim Bhai")
        assertEquals("Rahim Bhai", customPreview.selectedDisplayName)
    }

    @Test
    fun testCandidateGrouping_namelessDuplicatesGroup() {
        val c1 = createNormalizedContact(id = 1, name = "", numbers = listOf("01711223344"))
        val c2 = createNormalizedContact(id = 2, name = "", numbers = listOf("+8801711223344"))

        val groups = ContactGroupingEngine.findMergeGroups(listOf(c1, c2))
        assertEquals(1, groups.size)
        assertEquals(ConfidenceTier.HIGH, groups[0].confidence)
    }

    @Test
    fun testCandidateGrouping_singleContactBelongsToOnlyOneGroup() {
        val c1 = createNormalizedContact(id = 1, name = "Rahim", numbers = listOf("01712345678"))
        val c2 = createNormalizedContact(id = 2, name = "Rahim", numbers = listOf("01712345678"))
        val c3 = createNormalizedContact(id = 3, name = "Rahim", numbers = listOf("01712345678"))
        val c4 = createNormalizedContact(id = 4, name = "Bob", numbers = listOf("01812345678"))
        val c5 = createNormalizedContact(id = 5, name = "Robert", numbers = listOf("01812345678"))

        val groups = ContactGroupingEngine.findMergeGroups(listOf(c1, c2, c3, c4, c5))
        assertEquals(2, groups.size)

        val allIds = groups.flatMap { it.contacts.map { c -> c.id } }
        assertEquals(allIds.size, allIds.distinct().size)
    }

    // =====================================================================
    // 5. PHASE FIVE: FIX ISSUES TESTS
    // =====================================================================

    @Test
    fun testFixIssues_unnamedContactFlagged() {
        val c = createNormalizedContact(id = 1, name = "", numbers = listOf("01712345678"))
        val issues = ContactFixEngine.scanForFixIssues(listOf(c))
        assertTrue(issues.any { it is FixIssue.Unnamed })
    }

    @Test
    fun testFixIssues_malformedNameContactFlagged() {
        val c = createNormalizedContact(id = 1, name = "01712345678", numbers = listOf("01712345678"))
        val issues = ContactFixEngine.scanForFixIssues(listOf(c))
        assertTrue(issues.any { it is FixIssue.MalformedName })
    }

    @Test
    fun testFixIssues_unnormalizedNumberFlagged() {
        val c = createNormalizedContact(id = 1, name = "John", numbers = listOf("01712-345-678"))
        val issues = ContactFixEngine.scanForFixIssues(listOf(c))
        assertTrue(issues.any { it is FixIssue.UnnormalizedNumber && it.proposedE164 == "+8801712345678" })
    }

    @Test
    fun testFixIssues_emptyContactFlaggedForDeletion() {
        val c = createNormalizedContact(id = 1, name = "Empty Person", numbers = emptyList())
        val issues = ContactFixEngine.scanForFixIssues(listOf(c))
        assertTrue(issues.any { it is FixIssue.EmptyContact })
    }

    // =====================================================================
    // HELPER
    // =====================================================================

    private fun createNormalizedContact(
        id: Long,
        name: String,
        numbers: List<String>,
        emails: List<String> = emptyList(),
        isPrivate: Boolean = false,
        preferredSimSlot: Int? = null,
        notes: String? = null
    ): NormalizedContact {
        val contactEntity = ContactEntity(
            id = id,
            displayName = name,
            isPrivate = isPrivate,
            preferredSimSlot = preferredSimSlot,
            notes = notes
        )
        val numberEntities = numbers.mapIndexed { idx, num ->
            ContactNumberEntity(
                id = idx.toLong() + 1,
                contactId = id,
                number = num,
                normalizedNumber = ContactNormalizer.normalizePhoneNumber(num)
            )
        }
        val emailEntities = emails.mapIndexed { idx, em ->
            ContactEmailEntity(
                id = idx.toLong() + 1,
                contactId = id,
                email = em
            )
        }
        return NormalizedContact(
            contact = contactEntity,
            normalizedDisplayName = ContactNormalizer.normalizeDisplayName(name),
            numbers = numberEntities,
            normalizedNumbers = numberEntities.map { it.normalizedNumber },
            emails = emailEntities,
            normalizedEmails = emailEntities.map { ContactNormalizer.normalizeEmail(it.email) }
        )
    }
}

