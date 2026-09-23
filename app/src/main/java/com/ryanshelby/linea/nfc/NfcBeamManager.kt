package com.ryanshelby.linea.nfc

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Bundle
import android.provider.Settings
import com.ryanshelby.linea.qr_code.QrCodeEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-performance NFC Manager for LiNEA Beam.
 *
 * Coordinates:
 * 1. Hardware availability & state detection.
 * 2. Foreground reader mode (Android 11+ / API 30+).
 * 3. Fast ISO-DEP APDU exchange for LiNEA-to-LiNEA phone bumps.
 * 4. Standard NDEF read/write operations for physical NFC cards/tags/stickers.
 */
@Singleton
class NfcBeamManager @Inject constructor() {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _incomingBeamedContact = MutableSharedFlow<QrCodeEngine.ContactPayload>(extraBufferCapacity = 1)
    val incomingBeamedContact: SharedFlow<QrCodeEngine.ContactPayload> = _incomingBeamedContact.asSharedFlow()

    private val _tagWriteStatus = MutableSharedFlow<TagWriteResult>(extraBufferCapacity = 1)
    val tagWriteStatus: SharedFlow<TagWriteResult> = _tagWriteStatus.asSharedFlow()

    // When set, the next scanned physical NFC tag will be programmed with this payload
    @Volatile
    var pendingTagWritePayload: QrCodeEngine.ContactPayload? = null

    @Volatile
    var userProfilePayload: QrCodeEngine.ContactPayload? = null
        private set

    @Volatile
    private var activeBeamPayload: QrCodeEngine.ContactPayload? = null

    init {
        // Bridge HCE service incoming beam transmissions to UI flow
        LineaBeamApduService.onContactReceivedListener = { payload ->
            _incomingBeamedContact.tryEmit(payload)
        }
    }

    /**
     * Checks if an incoming contact was received and stored while the app was closed.
     */
    fun checkPendingBeamedContact(context: Context) {
        val pending = NfcProfileStorage.consumePendingBeamedContact(context)
        if (pending != null) {
            _incomingBeamedContact.tryEmit(pending)
        }
    }

    sealed class TagWriteResult {
        data class Success(val contactName: String) : TagWriteResult()
        data class Error(val message: String) : TagWriteResult()
    }

    /**
     * Checks whether NFC hardware is present on this device.
     */
    fun isNfcSupported(context: Context): Boolean {
        val adapter = NfcAdapter.getDefaultAdapter(context.applicationContext)
        return adapter != null
    }

    /**
     * Checks whether NFC is turned on in system settings.
     */
    fun isNfcEnabled(context: Context): Boolean {
        val adapter = NfcAdapter.getDefaultAdapter(context.applicationContext)
        return adapter?.isEnabled == true
    }

    /**
     * Open system NFC settings page.
     */
    fun openNfcSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_NFC_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (_: Exception) {
                // Fallback ignored
            }
        }
    }

    /**
     * Syncs user's personal digital card for automatic HCE beaming.
     */
    fun setUserProfilePayload(payload: QrCodeEngine.ContactPayload?) {
        userProfilePayload = payload
        if (activeBeamPayload == null) {
            LineaBeamApduService.setActiveContact(payload)
        }
    }

    /**
     * Set active payload for Host Card Emulation sharing (e.g. sharing a specific contact).
     */
    fun setActiveBeamContact(payload: QrCodeEngine.ContactPayload?) {
        activeBeamPayload = payload
        LineaBeamApduService.setActiveContact(payload ?: userProfilePayload)
    }

    /**
     * Enables Reader Mode in the active activity.
     */
    fun enableReaderMode(activity: Activity) {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        if (!adapter.isEnabled) return

        val flags = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS

        adapter.enableReaderMode(activity, { tag ->
            handleDiscoveredTag(tag)
        }, flags, null)
    }

    /**
     * Disables Reader Mode when activity pauses.
     */
    fun disableReaderMode(activity: Activity) {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        try {
            adapter.disableReaderMode(activity)
        } catch (_: Exception) {
            // Activity might already be destroyed
        }
    }

    /**
     * Handle incoming NFC tag discovery.
     */
    fun handleDiscoveredTag(tag: Tag) {
        scope.launch {
            // Check if there is a pending write request first
            val writePayload = pendingTagWritePayload
            if (writePayload != null) {
                val writeSuccess = writePayloadToTag(tag, writePayload)
                if (writeSuccess) {
                    _tagWriteStatus.tryEmit(TagWriteResult.Success(writePayload.name))
                    pendingTagWritePayload = null
                } else {
                    _tagWriteStatus.tryEmit(TagWriteResult.Error("Failed to write to NFC tag. Ensure tag is writable."))
                }
                return@launch
            }

            // Otherwise, attempt to read contact
            val payload = readContactFromTag(tag)
            if (payload != null) {
                _incomingBeamedContact.tryEmit(payload)
            }
        }
    }

    /**
     * Handle incoming Intent containing NDEF payload.
     */
    fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action ?: return
        if (action == NfcAdapter.ACTION_NDEF_DISCOVERED ||
            action == NfcAdapter.ACTION_TECH_DISCOVERED ||
            action == NfcAdapter.ACTION_TAG_DISCOVERED
        ) {
            // Extract from raw NDEF messages
            val rawMessages = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, NdefMessage::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            }

            if (!rawMessages.isNullOrEmpty()) {
                for (rawMsg in rawMessages) {
                    val ndefMsg = rawMsg as? NdefMessage ?: continue
                    val payload = parseNdefMessage(ndefMsg)
                    if (payload != null) {
                        _incomingBeamedContact.tryEmit(payload)
                        return
                    }
                }
            }

            // Fallback: extract Tag parcelable
            val tag = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            }
            if (tag != null) {
                handleDiscoveredTag(tag)
            }
        }
    }

    /**
     * Reads contact payload from tag using ISO-DEP (HCE bump) or NDEF (physical tag/card).
     */
    private fun readContactFromTag(tag: Tag): QrCodeEngine.ContactPayload? {
        // 1. Try ISO-DEP (Peer-to-peer phone Host Card Emulation)
        val isoDep = IsoDep.get(tag)
        if (isoDep != null) {
            try {
                isoDep.connect()
                isoDep.timeout = 2500

                // Step A: Send SELECT AID command for LiNEA Custom AID F0010203040506:
                // CLA=00, INS=A4, P1=04, P2=00, Lc=07, DATA=F0010203040506, Le=00
                val selectAidApdu = byteArrayOf(
                    0x00, 0xA4.toByte(), 0x04, 0x00, 0x07,
                    0xF0.toByte(), 0x01, 0x02, 0x03, 0x04, 0x05, 0x06,
                    0x00
                )
                val selectResponse = isoDep.transceive(selectAidApdu)

                var peerPayload: QrCodeEngine.ContactPayload? = null

                // Step B: Bidirectional SWAP_CARDS (INS = A5).
                // Send reader's own contact payload in command data so the other phone receives it,
                // and parse peer's contact payload from the APDU response.
                val outboundPayload = activeBeamPayload ?: userProfilePayload
                val outboundUriBytes = if (outboundPayload != null) {
                    QrCodeEngine.serializeToUri(outboundPayload).toByteArray(StandardCharsets.UTF_8)
                } else ByteArray(0)

                val swapApdu = if (outboundUriBytes.size <= 255) {
                    byteArrayOf(0x00, 0xA5.toByte(), 0x00, 0x00, outboundUriBytes.size.toByte()) +
                            outboundUriBytes + byteArrayOf(0x00)
                } else {
                    // Extended APDU format
                    byteArrayOf(
                        0x00, 0xA5.toByte(), 0x00, 0x00, 0x00,
                        ((outboundUriBytes.size shr 8) and 0xFF).toByte(),
                        (outboundUriBytes.size and 0xFF).toByte()
                    ) + outboundUriBytes + byteArrayOf(0x00, 0x00)
                }

                try {
                    val swapResponse = isoDep.transceive(swapApdu)
                    if (swapResponse != null && swapResponse.size >= 2) {
                        val sw1 = swapResponse[swapResponse.size - 2]
                        val sw2 = swapResponse[swapResponse.size - 1]
                        if (sw1 == 0x90.toByte() && sw2 == 0x00.toByte()) {
                            val payloadBytes = swapResponse.copyOfRange(0, swapResponse.size - 2)
                            val text = String(payloadBytes, StandardCharsets.UTF_8).trim()
                            peerPayload = QrCodeEngine.parseLineaScannedText(text)
                        }
                    }
                } catch (_: Exception) {
                    // Fallback to selectResponse if peer did not support SWAP
                }

                // If SWAP did not return a payload, check response from initial SELECT
                if (peerPayload == null && selectResponse != null && selectResponse.size >= 2) {
                    val sw1 = selectResponse[selectResponse.size - 2]
                    val sw2 = selectResponse[selectResponse.size - 1]
                    if (sw1 == 0x90.toByte() && sw2 == 0x00.toByte()) {
                        val payloadBytes = selectResponse.copyOfRange(0, selectResponse.size - 2)
                        val text = String(payloadBytes, StandardCharsets.UTF_8).trim()
                        peerPayload = QrCodeEngine.parseLineaScannedText(text)
                    }
                }

                isoDep.close()
                if (peerPayload != null) return peerPayload
            } catch (_: Exception) {
                try {
                    isoDep.close()
                } catch (_: Exception) {}
            }
        }

        // 2. Try standard NDEF reading (Type 2/4 tags, stickers, cards)
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            try {
                ndef.connect()
                val ndefMessage = ndef.ndefMessage
                ndef.close()
                if (ndefMessage != null) {
                    val parsed = parseNdefMessage(ndefMessage)
                    if (parsed != null) return parsed
                }
            } catch (_: Exception) {
                try {
                    ndef.close()
                } catch (_: Exception) {}
            }
        }

        return null
    }

    /**
     * Parses an NdefMessage looking for LiNEA URI or vCard.
     */
    private fun parseNdefMessage(ndefMessage: NdefMessage): QrCodeEngine.ContactPayload? {
        for (record in ndefMessage.records) {
            // Check URI Record
            try {
                val uri = record.toUri()
                if (uri != null) {
                    val parsed = QrCodeEngine.parseLineaScannedText(uri.toString())
                    if (parsed != null) return parsed
                }
            } catch (_: Exception) {}

            // Check payload as text/vcard or raw URI string
            try {
                val text = String(record.payload, StandardCharsets.UTF_8).trim()
                val parsed = QrCodeEngine.parseLineaScannedText(text)
                if (parsed != null) return parsed
            } catch (_: Exception) {}
        }
        return null
    }

    /**
     * Writes contact payload to a physical rewritable NFC tag.
     */
    private fun writePayloadToTag(tag: Tag, payload: QrCodeEngine.ContactPayload): Boolean {
        val uri = QrCodeEngine.serializeToUri(payload)
        val uriRecord = NdefRecord.createUri(uri)
        val aarRecord = NdefRecord.createApplicationRecord("com.ryanshelby.linea")
        val message = NdefMessage(arrayOf(uriRecord, aarRecord))
        val size = message.toByteArray().size

        val ndef = Ndef.get(tag)
        if (ndef != null) {
            try {
                ndef.connect()
                if (!ndef.isWritable) {
                    ndef.close()
                    return false
                }
                if (ndef.maxSize < size) {
                    ndef.close()
                    return false
                }
                ndef.writeNdefMessage(message)
                ndef.close()
                return true
            } catch (e: Exception) {
                try {
                    ndef.close()
                } catch (_: Exception) {}
                return false
            }
        }

        val formatable = NdefFormatable.get(tag)
        if (formatable != null) {
            try {
                formatable.connect()
                formatable.format(message)
                formatable.close()
                return true
            } catch (e: Exception) {
                try {
                    formatable.close()
                } catch (_: Exception) {}
                return false
            }
        }

        return false
    }
}
