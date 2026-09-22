package com.ryanshelby.linea

import com.ryanshelby.linea.nfc.LineaBeamApduService
import com.ryanshelby.linea.qr_code.QrCodeEngine
import com.ryanshelby.linea.ui.screens.dialpad.OrbitalSpeedDialContact
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets

class NfcBeamAndOrbitalDialTest {

    @Test
    fun testLineaBeamApduServiceDirectExchange() {
        val service = LineaBeamApduService()
        val payload = QrCodeEngine.ContactPayload(
            name = "Aegis Vance",
            numbers = listOf("+15551234567", "+15557654321")
        )
        LineaBeamApduService.setActiveContact(payload)

        // Command: SELECT LiNEA AID (F0010203040506)
        val selectApdu = byteArrayOf(
            0x00, 0xA4.toByte(), 0x04, 0x00, 0x07,
            0xF0.toByte(), 0x01, 0x02, 0x03, 0x04, 0x05, 0x06
        )

        val response = service.processCommandApdu(selectApdu, null)
        assertNotNull("APDU response should not be null", response)
        assertTrue("Response must have at least 2 bytes (SW1 SW2)", response.size >= 2)

        val sw1 = response[response.size - 2]
        val sw2 = response[response.size - 1]
        assertEquals(0x90.toByte(), sw1)
        assertEquals(0x00.toByte(), sw2)

        val payloadBytes = response.copyOfRange(0, response.size - 2)
        val uriString = String(payloadBytes, StandardCharsets.UTF_8)
        val parsed = QrCodeEngine.parseLineaScannedText(uriString)

        assertNotNull("Parsed beamed contact must not be null", parsed)
        assertEquals("Aegis Vance", parsed?.name)
        assertEquals(listOf("+15551234567", "+15557654321"), parsed?.numbers)
    }

    @Test
    fun testLineaBeamNdefType4Emulation() {
        val service = LineaBeamApduService()
        val payload = QrCodeEngine.ContactPayload(
            name = "Elena Rostova",
            numbers = listOf("+447700900077")
        )
        LineaBeamApduService.setActiveContact(payload)

        // 1. SELECT NDEF Application (D2760000850101)
        val selectNdefApdu = byteArrayOf(
            0x00, 0xA4.toByte(), 0x04, 0x00, 0x07,
            0xD2.toByte(), 0x76.toByte(), 0x00, 0x00, 0x85.toByte(), 0x01, 0x01
        )
        val res1 = service.processCommandApdu(selectNdefApdu, null)
        assertArrayEquals(LineaBeamApduService.STATUS_SUCCESS, res1)

        // 2. SELECT CC File (E103)
        val selectCcApdu = byteArrayOf(
            0x00, 0xA4.toByte(), 0x00, 0x0C, 0x02,
            0xE1.toByte(), 0x03.toByte()
        )
        val res2 = service.processCommandApdu(selectCcApdu, null)
        assertArrayEquals(LineaBeamApduService.STATUS_SUCCESS, res2)

        // 3. READ CC File
        val readCcApdu = byteArrayOf(
            0x00, 0xB0.toByte(), 0x00, 0x00, 0x0F
        )
        val res3 = service.processCommandApdu(readCcApdu, null)
        assertTrue(res3.size == LineaBeamApduService.CC_FILE.size + 2)
        assertEquals(0x90.toByte(), res3[res3.size - 2])
        assertEquals(0x00.toByte(), res3[res3.size - 1])

        // 4. SELECT NDEF File (E104)
        val selectNdefFileApdu = byteArrayOf(
            0x00, 0xA4.toByte(), 0x00, 0x0C, 0x02,
            0xE1.toByte(), 0x04.toByte()
        )
        val res4 = service.processCommandApdu(selectNdefFileApdu, null)
        assertArrayEquals(LineaBeamApduService.STATUS_SUCCESS, res4)

        // 5. READ NDEF File length + data
        val readNdefApdu = byteArrayOf(
            0x00, 0xB0.toByte(), 0x00, 0x00, 0xFF.toByte()
        )
        val res5 = service.processCommandApdu(readNdefApdu, null)
        assertTrue(res5.size > 4)
        assertEquals(0x90.toByte(), res5[res5.size - 2])
        assertEquals(0x00.toByte(), res5[res5.size - 1])
    }

    @Test
    fun testOrbitalSpeedDialContactInitials() {
        val c1 = OrbitalSpeedDialContact(1, "Marcus Vance", "+15554321098")
        assertEquals("M", c1.initials)

        val c2 = OrbitalSpeedDialContact(2, "  alexander ", "+15551234567")
        assertEquals("A", c2.initials)

        val c3 = OrbitalSpeedDialContact(3, "", "+15550000000")
        assertEquals("C", c3.initials)
    }

    @Test
    fun testLineaBeamApduServiceBidirectionalSwap() {
        val service = LineaBeamApduService()

        // Phone B (Host Card Emulation Responder)
        val phoneBPayload = QrCodeEngine.ContactPayload(
            name = "Bob Cypher",
            numbers = listOf("+19998887777"),
            email = "bob@cypher.tech",
            photoThumbnailBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=",
            social1Platform = "INSTAGRAM",
            social1Handle = "bob_cypher",
            social2Platform = "GITHUB",
            social2Handle = "bob-code"
        )
        LineaBeamApduService.setActiveContact(phoneBPayload)

        // Track received payload on Phone B
        var receivedOnPhoneB: QrCodeEngine.ContactPayload? = null
        LineaBeamApduService.onContactReceivedListener = {
            receivedOnPhoneB = it
        }

        // Phone A (Reader Initiator) sends SWAP_CARDS (INS = 0xA5) carrying Phone A's payload
        val phoneAPayload = QrCodeEngine.ContactPayload(
            name = "Alice Nexus",
            numbers = listOf("+11112223333"),
            email = "alice@nexus.net",
            social1Platform = "TELEGRAM",
            social1Handle = "alice_nexus",
            social2Platform = "REDDIT",
            social2Handle = "alice_dev"
        )
        val phoneAUri = QrCodeEngine.serializeToUri(phoneAPayload)
        val phoneAUriBytes = phoneAUri.toByteArray(StandardCharsets.UTF_8)

        // Construct SWAP APDU: CLA=00, INS=A5, P1=00, P2=00, Lc, Data, Le=00
        val swapApdu = byteArrayOf(
            0x00, LineaBeamApduService.INS_SWAP_CARDS.toByte(), 0x00, 0x00, phoneAUriBytes.size.toByte()
        ) + phoneAUriBytes + byteArrayOf(0x00)

        val response = service.processCommandApdu(swapApdu, null)
        assertNotNull("Swap APDU response must not be null", response)
        assertTrue("Response must have at least 2 status bytes", response.size >= 2)

        val sw1 = response[response.size - 2]
        val sw2 = response[response.size - 1]
        assertEquals(0x90.toByte(), sw1)
        assertEquals(0x00.toByte(), sw2)

        // 1. Verify Phone B received Phone A's card
        assertNotNull("Phone B must receive Phone A's transmitted card", receivedOnPhoneB)
        assertEquals("Alice Nexus", receivedOnPhoneB?.name)
        assertEquals(listOf("+11112223333"), receivedOnPhoneB?.numbers)
        assertEquals("alice@nexus.net", receivedOnPhoneB?.email)
        assertEquals("TELEGRAM", receivedOnPhoneB?.social1Platform)
        assertEquals("alice_nexus", receivedOnPhoneB?.social1Handle)
        assertEquals("REDDIT", receivedOnPhoneB?.social2Platform)
        assertEquals("alice_dev", receivedOnPhoneB?.social2Handle)

        // 2. Verify Phone A received Phone B's card from the APDU response
        val responseData = response.copyOfRange(0, response.size - 2)
        val responseUri = String(responseData, StandardCharsets.UTF_8)
        val parsedPhoneB = QrCodeEngine.parseLineaScannedText(responseUri)

        assertNotNull("Phone A must parse Phone B's returned card", parsedPhoneB)
        assertEquals("Bob Cypher", parsedPhoneB?.name)
        assertEquals(listOf("+19998887777"), parsedPhoneB?.numbers)
        assertEquals("bob@cypher.tech", parsedPhoneB?.email)
        assertEquals(phoneBPayload.photoThumbnailBase64, parsedPhoneB?.photoThumbnailBase64)
        assertEquals("INSTAGRAM", parsedPhoneB?.social1Platform)
        assertEquals("bob_cypher", parsedPhoneB?.social1Handle)
        assertEquals("GITHUB", parsedPhoneB?.social2Platform)
        assertEquals("bob-code", parsedPhoneB?.social2Handle)
    }
}
