package com.ryanshelby.linea

import com.ryanshelby.linea.telecom.T9Contact
import com.ryanshelby.linea.telecom.T9SearchEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class T9SearchEngineTest {

    private val sampleContacts = listOf(
        T9Contact(1L, "Ryan Shelby", "+15551234567"),
        T9Contact(2L, "Alice Smith", "+15559876543"),
        T9Contact(3L, "Bob Jones", "+15553334444"),
        T9Contact(4L, "Charlie Brown", "+15558889999")
    )

    @Test
    fun convertNameToDigits_correctlyMapsLetters() {
        // R-Y-A-N -> 7-9-2-6
        val digits = T9SearchEngine.convertNameToDigits("Ryan")
        assertEquals("7926", digits)

        // B-O-B -> 2-6-2
        val bobDigits = T9SearchEngine.convertNameToDigits("Bob")
        assertEquals("262", bobDigits)
    }

    @Test
    fun search_matchesContactByNamePrefix() {
        // "7926" matches "Ryan"
        val results = T9SearchEngine.search(sampleContacts, "7926")
        assertTrue(results.isNotEmpty())
        assertEquals("Ryan Shelby", results.first().contact.displayName)
        assertTrue(results.first().matchedName)
    }

    @Test
    fun search_matchesSecondWordByNamePrefix() {
        // "76484" -> S-M-I-T-H matches "Alice Smith"
        val results = T9SearchEngine.search(sampleContacts, "76484")
        assertTrue(results.isNotEmpty())
        assertEquals("Alice Smith", results.first().contact.displayName)
        assertTrue(results.first().matchedName)
    }

    @Test
    fun search_matchesByPhoneNumberSubstring() {
        // "9876" matches "+15559876543"
        val results = T9SearchEngine.search(sampleContacts, "9876")
        assertTrue(results.isNotEmpty())
        assertEquals("Alice Smith", results.first().contact.displayName)
        assertFalse(results.first().matchedName)
    }

    @Test
    fun search_emptyQuery_returnsEmptyList() {
        val results = T9SearchEngine.search(sampleContacts, "")
        assertTrue(results.isEmpty())
    }
}
