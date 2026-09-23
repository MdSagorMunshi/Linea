package com.ryanshelby.linea.nfc

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import androidx.core.app.NotificationCompat
import com.ryanshelby.linea.LineaApp
import com.ryanshelby.linea.R
import com.ryanshelby.linea.qr_code.QrCodeEngine
import java.nio.charset.StandardCharsets
import java.util.Arrays

/**
 * Host Card Emulation (HCE) service for LiNEA Beam.
 *
 * Emulates:
 * 1. LiNEA Proprietary Contact Beam AID (F0010203040506) for high-speed LiNEA-to-LiNEA mutual exchange.
 * 2. NFC Forum Type 4 Tag NDEF Application (D2760000850101) so ANY NFC phone or standard NFC reader
 *    can read the beamed contact card.
 *
 * Supports Always Share Mode: allows sharing contact card even when LiNEA is closed or backgrounded.
 */
class LineaBeamApduService : HostApduService() {

    companion object {
        // LiNEA Custom AID: F0 01 02 03 04 05 06
        val LINEA_AID = byteArrayOf(0xF0.toByte(), 0x01, 0x02, 0x03, 0x04, 0x05, 0x06)
        
        // NFC Forum Type 4 Tag NDEF Application AID: D2 76 00 00 85 01 01
        val NDEF_AID = byteArrayOf(
            0xD2.toByte(), 0x76.toByte(), 0x00, 0x00, 0x85.toByte(), 0x01, 0x01
        )

        // Status words
        val STATUS_SUCCESS = byteArrayOf(0x90.toByte(), 0x00)
        val STATUS_FAILED = byteArrayOf(0x6F.toByte(), 0x00)
        val STATUS_FILE_NOT_FOUND = byteArrayOf(0x6A.toByte(), 0x82.toByte())

        // Capability Container (CC) File ID: E1 03
        val CC_FILE_ID = byteArrayOf(0xE1.toByte(), 0x03)
        // NDEF File ID: E1 04
        val NDEF_FILE_ID = byteArrayOf(0xE1.toByte(), 0x04)

        // Type 4 Tag CC file content (15 bytes)
        // [0..1] CCLEN: 00 0F
        // [2]    Mapping version: 2.0 (20)
        // [3..4] MLe: 00 FF
        // [5..6] MLc: 00 FF
        // [7]    NDEF File Control TLV T: 04
        // [8]    L: 06
        // [9..10] File ID: E1 04
        // [11..12] Max NDEF file size: 20 00 (8192 bytes)
        // [13]   Read access: 00 (unrestricted)
        // [14]   Write access: FF (read only)
        val CC_FILE = byteArrayOf(
            0x00, 0x0F,
            0x20,
            0x00, 0xFF.toByte(),
            0x00, 0xFF.toByte(),
            0x04, 0x06,
            0xE1.toByte(), 0x04,
            0x20, 0x00, // Max NDEF size: 8192 bytes
            0x00,
            0xFF.toByte()
        )

        const val INS_SWAP_CARDS = 0xA5

        @Volatile
        var activePayload: QrCodeEngine.ContactPayload? = null

        // Callback invoked on HCE device when peer transmits its contact card during bump
        var onContactReceivedListener: ((QrCodeEngine.ContactPayload) -> Unit)? = null

        fun setActiveContact(payload: QrCodeEngine.ContactPayload?) {
            activePayload = payload
        }
    }

    private var selectedFile: ByteArray? = null

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        if (commandApdu == null || commandApdu.size < 4) {
            return STATUS_FAILED
        }

        val ins = commandApdu[1].toInt() and 0xFF
        val p1 = commandApdu[2].toInt() and 0xFF
        val p2 = commandApdu[3].toInt() and 0xFF

        // Check whether sharing is permitted:
        // 1. If active in foreground (LineaApp.isAppInForeground or activePayload != null)
        // 2. OR Always Share Mode is enabled in settings
        val isAlwaysShare = try {
            NfcProfileStorage.isAlwaysShareEnabled(this)
        } catch (_: Exception) {
            false
        }
        val isSharingAllowed = LineaApp.isAppInForeground || activePayload != null || isAlwaysShare

        // Synchronously load profile from fast storage if not in memory
        val currentPayload = activePayload ?: if (isSharingAllowed) {
            try {
                NfcProfileStorage.getEffectivePayload(this)
            } catch (_: Exception) {
                null
            }
        } else null

        // 1. SELECT command (INS = A4)
        if (ins == 0xA4) {
            if (p1 == 0x04) {
                // Select by AID
                val length = if (commandApdu.size > 4) commandApdu[4].toInt() and 0xFF else 0
                if (commandApdu.size >= 5 + length) {
                    val aid = commandApdu.copyOfRange(5, 5 + length)
                    if (Arrays.equals(aid, LINEA_AID)) {
                        // LiNEA-to-LiNEA direct protocol: return raw serialized URI
                        return if (currentPayload != null && isSharingAllowed) {
                            val uriBytes = QrCodeEngine.serializeToUri(currentPayload).toByteArray(StandardCharsets.UTF_8)
                            uriBytes + STATUS_SUCCESS
                        } else {
                            STATUS_SUCCESS
                        }
                    } else if (Arrays.equals(aid, NDEF_AID)) {
                        selectedFile = null
                        return STATUS_SUCCESS
                    }
                }
            } else if (p1 == 0x00 && p2 == 0x0C) {
                // Select by File ID (P1=00, P2=0C)
                if (commandApdu.size >= 7) {
                    val fileId = commandApdu.copyOfRange(5, 7)
                    if (Arrays.equals(fileId, CC_FILE_ID) || Arrays.equals(fileId, NDEF_FILE_ID)) {
                        selectedFile = fileId
                        return STATUS_SUCCESS
                    }
                }
                return STATUS_FILE_NOT_FOUND
            }
        }

        // 2. READ BINARY command (INS = B0)
        if (ins == 0xB0) {
            val offset = ((p1 and 0x7F) shl 8) or (p2 and 0xFF)
            val le = if (commandApdu.size > 4) commandApdu[4].toInt() and 0xFF else 0
            val requestedLength = if (le == 0) 256 else le

            if (Arrays.equals(selectedFile, CC_FILE_ID)) {
                if (offset >= CC_FILE.size) return STATUS_SUCCESS
                val bytesToRead = minOf(requestedLength, CC_FILE.size - offset)
                val response = CC_FILE.copyOfRange(offset, offset + bytesToRead)
                return response + STATUS_SUCCESS
            } else if (Arrays.equals(selectedFile, NDEF_FILE_ID)) {
                val ndefData = if (isSharingAllowed && currentPayload != null) {
                    buildNdefFileContent(currentPayload)
                } else {
                    byteArrayOf(0x00, 0x00)
                }
                if (offset >= ndefData.size) return STATUS_SUCCESS
                val bytesToRead = minOf(requestedLength, ndefData.size - offset)
                val response = ndefData.copyOfRange(offset, offset + bytesToRead)
                return response + STATUS_SUCCESS
            }
        }

        // 3. SWAP_CARDS command (INS = A5): Bidirectional LiNEA-to-LiNEA Exchange
        if (ins == INS_SWAP_CARDS) {
            // Extract peer's transmitted contact payload if present
            if (commandApdu.size > 5) {
                val isExtended = commandApdu.size >= 7 && commandApdu[4] == 0x00.toByte()
                val dataStartIndex = if (isExtended) 7 else 5
                val rawBytes = commandApdu.copyOfRange(dataStartIndex, commandApdu.size)
                val trimmedBytes = if (rawBytes.isNotEmpty() && rawBytes.last() == 0x00.toByte()) {
                    rawBytes.copyOfRange(0, rawBytes.size - 1)
                } else rawBytes

                val text = String(trimmedBytes, StandardCharsets.UTF_8).trim()
                val peerPayload = QrCodeEngine.parseLineaScannedText(text)
                if (peerPayload != null) {
                    val listener = onContactReceivedListener
                    if (listener != null) {
                        listener.invoke(peerPayload)
                    } else {
                        // App was closed/backgrounded when phone was tapped
                        try {
                            NfcProfileStorage.savePendingBeamedContact(this, peerPayload)
                            postBeamedContactNotification(peerPayload)
                        } catch (_: Exception) {}
                    }
                }
            }

            // Return local contact payload if sharing is enabled
            return if (currentPayload != null && isSharingAllowed) {
                val uriBytes = QrCodeEngine.serializeToUri(currentPayload).toByteArray(StandardCharsets.UTF_8)
                uriBytes + STATUS_SUCCESS
            } else {
                STATUS_SUCCESS
            }
        }

        return STATUS_FAILED
    }

    override fun onDeactivated(reason: Int) {
        selectedFile = null
    }

    /**
     * Builds standard Type 4 Tag NDEF file contents:
     * 2 bytes big-endian length + NFC Forum NDEF Message bytes (RTD-URI).
     */
    private fun buildNdefFileContent(payload: QrCodeEngine.ContactPayload): ByteArray {
        val uri = QrCodeEngine.serializeToUri(payload)
        val uriBytes = uri.toByteArray(StandardCharsets.UTF_8)

        // NFC Forum NDEF Record (Type 'U' / URI):
        // 0xD1 = MB(1) | ME(1) | CF(0) | SR(1) | IL(0) | TNF(001)
        val payloadLen = 1 + uriBytes.size
        val ndefRecord = if (payloadLen <= 255) {
            byteArrayOf(
                0xD1.toByte(),      // Header
                0x01,               // Type length
                payloadLen.toByte(),// Payload length
                0x55.toByte(),      // Type = 'U' (0x55)
                0x00                // Identifier code (0x00 = exact URI)
            ) + uriBytes
        } else {
            byteArrayOf(
                0xC1.toByte(),      // Header (SR=0)
                0x01,               // Type length
                ((payloadLen shr 24) and 0xFF).toByte(),
                ((payloadLen shr 16) and 0xFF).toByte(),
                ((payloadLen shr 8) and 0xFF).toByte(),
                (payloadLen and 0xFF).toByte(),
                0x55.toByte(),
                0x00
            ) + uriBytes
        }

        val len = ndefRecord.size
        val lenBytes = byteArrayOf(((len shr 8) and 0xFF).toByte(), (len and 0xFF).toByte())
        return lenBytes + ndefRecord
    }

    /**
     * Post a high-priority system notification when a contact is received while LiNEA is closed.
     */
    private fun postBeamedContactNotification(payload: QrCodeEngine.ContactPayload) {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val intent = Intent(this, com.ryanshelby.linea.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                9921,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, LineaApp.CHANNEL_LINEA_BEAM)
                .setSmallIcon(R.drawable.ic_contacts_filled)
                .setContentTitle("LiNEA Beam: New Contact Received")
                .setContentText("${payload.name} shared their digital card. Tap to view & save.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(9921, notification)
        } catch (_: Exception) {
            // Notification failed or permissions not granted
        }
    }
}
