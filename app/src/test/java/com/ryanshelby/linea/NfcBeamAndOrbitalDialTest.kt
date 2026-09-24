package com.ryanshelby.linea

import com.ryanshelby.linea.nfc.LineaBeamApduService
import com.ryanshelby.linea.nfc.NfcBeamManager
import com.ryanshelby.linea.qr_code.QrCodeEngine
import com.ryanshelby.linea.ui.screens.dialpad.OrbitalSpeedDialContact
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    @Test
    fun testNfcTagWriteResultTypes() {
        val detected = NfcBeamManager.TagWriteResult.TagDetected(
            tagType = "NTAG / Mifare Ultralight",
            uidHex = "04:A2:3B:5C:8D:1E:80"
        )
        assertEquals("NTAG / Mifare Ultralight", detected.tagType)
        assertEquals("04:A2:3B:5C:8D:1E:80", detected.uidHex)

        val success = NfcBeamManager.TagWriteResult.Success(
            contactName = "Sarah Connor",
            tagType = "NFC Forum NDEF Tag",
            bytesWritten = 480
        )
        assertEquals("Sarah Connor", success.contactName)
        assertEquals(480, success.bytesWritten)

        val errorSmall = NfcBeamManager.TagWriteResult.Error(
            title = "Tag Memory Too Small",
            reason = "Contact card requires 512 bytes, but this tag only holds 144 bytes.",
            tagType = "Mifare Ultralight",
            requiredBytes = 512,
            availableBytes = 144
        )
        assertEquals("Tag Memory Too Small", errorSmall.title)
        assertEquals(512, errorSmall.requiredBytes)
        assertEquals(144, errorSmall.availableBytes)

        val errorProtected = NfcBeamManager.TagWriteResult.Error(
            title = "Tag Is Write-Protected",
            reason = "This NFC tag or card is permanently locked.",
            tagType = "ISO 7816 Smart Card"
        )
        assertEquals("Tag Is Write-Protected", errorProtected.title)
        assertEquals("ISO 7816 Smart Card", errorProtected.tagType)
    }

    @Test
    fun testNfcBeamManagerClearMethods() {
        val manager = NfcBeamManager()
        manager.clearIncomingBeamedContact()
        manager.clearTagWriteStatus()
        assertNull(manager.pendingTagWritePayload)
    }

    @Test
    fun testLineaBeamChunkedProtocol() {
        val service = LineaBeamApduService()

        // Target Phone (HCE service)
        val targetPayload = QrCodeEngine.ContactPayload(
            name = "Daedalus Prime",
            numbers = listOf("+18005550199", "+18005550188"),
            email = "daedalus@linea.mesh",
            photoThumbnailBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAYAAABytg0kAAAAFElEQVR42mNk+M9QzwAEjDAGBiYACc8B3Z46/x0AAAAASUVORK5CYII=",
            social1Platform = "TELEGRAM",
            social1Handle = "daedalus_prime",
            social2Platform = "GITHUB",
            social2Handle = "daedalus-dev"
        )
        LineaBeamApduService.setActiveContact(targetPayload)

        var receivedOnTarget: QrCodeEngine.ContactPayload? = null
        LineaBeamApduService.onContactReceivedListener = {
            receivedOnTarget = it
        }

        // 1. SELECT LiNEA AID
        val selectApdu = byteArrayOf(
            0x00, 0xA4.toByte(), 0x04, 0x00, 0x07,
            0xF0.toByte(), 0x01, 0x02, 0x03, 0x04, 0x05, 0x06
        )
        val selectResp = service.processCommandApdu(selectApdu, null)
        assertNotNull(selectResp)
        assertEquals(0x90.toByte(), selectResp[selectResp.size - 2])
        assertEquals(0x00.toByte(), selectResp[selectResp.size - 1])

        // Reader Phone has its own large payload
        val readerPayload = QrCodeEngine.ContactPayload(
            name = "Cassandra Cross",
            numbers = listOf("+18005550144"),
            email = "cassandra@linea.mesh",
            photoThumbnailBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAYAAABytg0kAAAAFElEQVR42mNk+M9QzwAEjDAGBiYACc8B3Z46/x0AAAAASUVORK5CYII=",
            social1Platform = "INSTAGRAM",
            social1Handle = "cassandra_x"
        )
        val readerUriBytes = QrCodeEngine.serializeToUri(readerPayload).toByteArray(StandardCharsets.UTF_8)
        val readerChunks = readerUriBytes.toList().chunked(LineaBeamApduService.CHUNK_SIZE).map { it.toByteArray() }

        // 2. INIT_EXCHANGE (0xA0)
        val initApdu = byteArrayOf(
            0x00, LineaBeamApduService.INS_INIT_EXCHANGE.toByte(), 0x00, 0x00, 0x04,
            ((readerUriBytes.size shr 8) and 0xFF).toByte(),
            (readerUriBytes.size and 0xFF).toByte(),
            ((readerChunks.size shr 8) and 0xFF).toByte(),
            (readerChunks.size and 0xFF).toByte(),
            0x04
        )
        val initResp = service.processCommandApdu(initApdu, null)
        assertNotNull(initResp)
        assertTrue(initResp.size >= 6)
        assertEquals(0x90.toByte(), initResp[initResp.size - 2])
        assertEquals(0x00.toByte(), initResp[initResp.size - 1])

        val targetTotalBytes = ((initResp[0].toInt() and 0xFF) shl 8) or (initResp[1].toInt() and 0xFF)
        val targetTotalChunks = ((initResp[2].toInt() and 0xFF) shl 8) or (initResp[3].toInt() and 0xFF)
        assertTrue("Target total bytes should be > 0", targetTotalBytes > 0)
        assertTrue("Target total chunks should be > 0", targetTotalChunks > 0)

        // 3. SEND_CHUNK (0xA1): Reader streams chunks to Target
        for (i in readerChunks.indices) {
            val chunk = readerChunks[i]
            val sendApdu = byteArrayOf(
                0x00, LineaBeamApduService.INS_SEND_CHUNK.toByte(),
                ((i shr 8) and 0xFF).toByte(),
                (i and 0xFF).toByte(),
                chunk.size.toByte()
            ) + chunk + byteArrayOf(0x00)
            val sendResp = service.processCommandApdu(sendApdu, null)
            assertArrayEquals(LineaBeamApduService.STATUS_SUCCESS, sendResp)
        }

        // Verify Target received Reader's contact
        assertNotNull("Target should have received Reader's contact card", receivedOnTarget)
        assertEquals("Cassandra Cross", receivedOnTarget?.name)
        assertEquals(listOf("+18005550144"), receivedOnTarget?.numbers)
        assertEquals("cassandra@linea.mesh", receivedOnTarget?.email)

        // 4. GET_CHUNK (0xA2): Reader retrieves chunks from Target
        val targetChunks = mutableListOf<ByteArray>()
        for (i in 0 until targetTotalChunks) {
            val getApdu = byteArrayOf(
                0x00, LineaBeamApduService.INS_GET_CHUNK.toByte(),
                ((i shr 8) and 0xFF).toByte(),
                (i and 0xFF).toByte(),
                0x00
            )
            val getResp = service.processCommandApdu(getApdu, null)
            assertTrue("Chunk response must have data + status words", getResp.size >= 2)
            assertEquals(0x90.toByte(), getResp[getResp.size - 2])
            assertEquals(0x00.toByte(), getResp[getResp.size - 1])
            targetChunks.add(getResp.copyOfRange(0, getResp.size - 2))
        }

        assertEquals(targetTotalChunks, targetChunks.size)
        val combinedTargetBytes = targetChunks.flatMap { it.toList() }.toByteArray()
        val targetUriText = String(combinedTargetBytes, StandardCharsets.UTF_8).trim()
        val parsedTarget = QrCodeEngine.parseLineaScannedText(targetUriText)

        assertNotNull("Reader should parse Target's reconstructed contact card", parsedTarget)
        assertEquals("Daedalus Prime", parsedTarget?.name)
        assertEquals(listOf("+18005550199", "+18005550188"), parsedTarget?.numbers)
        assertEquals("daedalus@linea.mesh", parsedTarget?.email)
        assertEquals("TELEGRAM", parsedTarget?.social1Platform)
        assertEquals("daedalus_prime", parsedTarget?.social1Handle)
    }
}

