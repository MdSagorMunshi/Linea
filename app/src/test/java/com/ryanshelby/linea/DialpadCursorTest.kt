package com.ryanshelby.linea

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests validating Dialpad in-place cursor editing:
 * 1. Typing 1234568, tapping between 2 and 3, and typing 2 produces 12234568.
 * 2. Moving cursor between 6 and 8, typing 7 produces 122345678.
 * 3. Deleting at cursor removes the character to the left of the cursor.
 * 4. Deleting at index 0 does nothing.
 * 5. Appending a string inserts at the cursor position.
 * 6. Clearing resets the buffer and cursor position.
 */
class DialpadCursorTest {

    private class DialpadBuffer(initial: String = "") {
        var text: String = initial
            private set
        var cursorPosition: Int = initial.length
            private set

        fun setCursorPosition(pos: Int) {
            cursorPosition = pos.coerceIn(0, text.length)
        }

        fun appendDigit(digit: Char) {
            val pos = cursorPosition.coerceIn(0, text.length)
            text = text.substring(0, pos) + digit + text.substring(pos)
            cursorPosition = pos + 1
        }

        fun appendString(digits: String) {
            val pos = cursorPosition.coerceIn(0, text.length)
            text = text.substring(0, pos) + digits + text.substring(pos)
            cursorPosition = pos + digits.length
        }

        fun deleteLastDigit() {
            val pos = cursorPosition.coerceIn(0, text.length)
            if (pos > 0 && text.isNotEmpty()) {
                text = text.substring(0, pos - 1) + text.substring(pos)
                cursorPosition = pos - 1
            }
        }

        fun setNumber(number: String) {
            text = number
            cursorPosition = number.length
        }

        fun clearNumber() {
            text = ""
            cursorPosition = 0
        }
    }

    @Test
    fun testUserTypoCorrection_1234568_to_122345678() {
        val buffer = DialpadBuffer()
        "1234568".forEach { buffer.appendDigit(it) }

        assertEquals("1234568", buffer.text)
        assertEquals(7, buffer.cursorPosition)

        // 1. User taps between '2' and '3' (position 2)
        buffer.setCursorPosition(2)
        assertEquals(2, buffer.cursorPosition)

        // 2. User types missing '2' -> should produce 12234568
        buffer.appendDigit('2')
        assertEquals("12234568", buffer.text)
        assertEquals(3, buffer.cursorPosition)

        // 3. User taps between '6' and '8' (position 7)
        buffer.setCursorPosition(7)
        assertEquals(7, buffer.cursorPosition)

        // 4. User types missing '7' -> should produce 122345678
        buffer.appendDigit('7')
        assertEquals("122345678", buffer.text)
        assertEquals(8, buffer.cursorPosition)
    }

    @Test
    fun testDeleteAtCursor() {
        val buffer = DialpadBuffer("122345678")
        assertEquals(9, buffer.cursorPosition)

        // Move cursor to position 8 (between '7' and '8')
        buffer.setCursorPosition(8)
        buffer.deleteLastDigit() // deletes '7'
        assertEquals("12234568", buffer.text)
        assertEquals(7, buffer.cursorPosition)

        // Move cursor to position 3 (between duplicate '2' and '3')
        buffer.setCursorPosition(3)
        buffer.deleteLastDigit() // deletes the second '2'
        assertEquals("1234568", buffer.text)
        assertEquals(2, buffer.cursorPosition)

        // Move to start (position 0)
        buffer.setCursorPosition(0)
        buffer.deleteLastDigit() // should do nothing
        assertEquals("1234568", buffer.text)
        assertEquals(0, buffer.cursorPosition)
    }

    @Test
    fun testAppendStringAtCursor() {
        val buffer = DialpadBuffer("128")
        buffer.setCursorPosition(2) // between 2 and 8
        buffer.appendString("34567")
        assertEquals("12345678", buffer.text)
        assertEquals(7, buffer.cursorPosition)
    }

    @Test
    fun testSetNumberAndClear() {
        val buffer = DialpadBuffer()
        buffer.setNumber("5550199")
        assertEquals("5550199", buffer.text)
        assertEquals(7, buffer.cursorPosition)

        buffer.clearNumber()
        assertEquals("", buffer.text)
        assertEquals(0, buffer.cursorPosition)
    }

    @Test
    fun testCursorClamping() {
        val buffer = DialpadBuffer("123")
        buffer.setCursorPosition(-5)
        assertEquals(0, buffer.cursorPosition)

        buffer.setCursorPosition(100)
        assertEquals(3, buffer.cursorPosition)
    }
}
