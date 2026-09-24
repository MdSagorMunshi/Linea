package com.ryanshelby.linea.nfc

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import com.ryanshelby.linea.qr_code.QrCodeEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.lang.ref.WeakReference
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
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Singleton
class NfcBeamManager @Inject constructor() {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _incomingBeamedContact = MutableSharedFlow<QrCodeEngine.ContactPayload>(replay = 1, extraBufferCapacity = 8)
    val incomingBeamedContact: SharedFlow<QrCodeEngine.ContactPayload> = _incomingBeamedContact.asSharedFlow()

    fun clearIncomingBeamedContact() {
        _incomingBeamedContact.resetReplayCache()
    }

    private val _tagWriteStatus = MutableSharedFlow<TagWriteResult>(replay = 1, extraBufferCapacity = 8)
    val tagWriteStatus: SharedFlow<TagWriteResult> = _tagWriteStatus.asSharedFlow()

    fun clearTagWriteStatus() {
        _tagWriteStatus.resetReplayCache()
    }

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
        data class TagDetected(
            val tagType: String,
            val uidHex: String
        ) : TagWriteResult()

        data class Success(
            val contactName: String,
            val tagType: String,
            val bytesWritten: Int
        ) : TagWriteResult()

        data class Error(
            val title: String,
            val reason: String,
            val tagType: String? = null,
            val requiredBytes: Int = 0,
            val availableBytes: Int = 0
        ) : TagWriteResult()
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

    @Volatile
    private var isBeamSessionActive = false

    @Volatile
    private var isTagOperationInProgress = false

    private var beamCycleJob: Job? = null
    private val mainScope = CoroutineScope(Dispatchers.Main)
    private var currentActivity: WeakReference<Activity>? = null

    /**
     * Enables Reader Mode in the active activity with adaptive duty cycling.
     */
    fun enableReaderMode(activity: Activity) {
        startBeamSession(activity)
    }

    /**
     * Disables Reader Mode when activity pauses.
     */
    fun disableReaderMode(activity: Activity) {
        stopBeamSession(activity)
    }

    @Synchronized
    fun startBeamSession(activity: Activity) {
        currentActivity = WeakReference(activity)
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        if (!adapter.isEnabled) return

        stopBeamSession(activity)
        isBeamSessionActive = true
        currentActivity = WeakReference(activity)

        val readerFlags = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS

        val readerExtras = Bundle().apply {
            putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 500)
        }

        beamCycleJob = mainScope.launch {
            while (isBeamSessionActive && !activity.isFinishing && !activity.isDestroyed) {
                // When tag write or active tag communication is underway, stay locked in reader mode
                if (pendingTagWritePayload != null || isTagOperationInProgress) {
                    try {
                        adapter.enableReaderMode(activity, { tag -> handleDiscoveredTag(tag) }, readerFlags, readerExtras)
                    } catch (_: Exception) {}
                    delay(300)
                    continue
                }

                // Phase 1: Reader Mode (active tag/phone discovery)
                try {
                    adapter.enableReaderMode(activity, { tag -> handleDiscoveredTag(tag) }, readerFlags, readerExtras)
                } catch (_: Exception) {}

                val readerDuration = 320L + (0..100).random()
                delay(readerDuration)

                if (isTagOperationInProgress || pendingTagWritePayload != null) {
                    continue
                }

                // Phase 2: Passive Listen Mode (disable reader mode so HCE chip can listen to peer's carrier)
                try {
                    adapter.disableReaderMode(activity)
                } catch (_: Exception) {}

                val listenDuration = 220L + (0..80).random()
                delay(listenDuration)
            }
        }
    }

    @Synchronized
    fun stopBeamSession(activity: Activity) {
        isBeamSessionActive = false
        beamCycleJob?.cancel()
        beamCycleJob = null
        try {
            val adapter = NfcAdapter.getDefaultAdapter(activity)
            adapter?.disableReaderMode(activity)
        } catch (_: Exception) {}
    }

    private fun triggerHapticFeedback() {
        try {
            val ctx = currentActivity?.get() ?: return
            val vibrator = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 70, 50, 90), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(120)
            }
        } catch (_: Exception) {}
    }

    /**
     * Handle incoming NFC tag discovery.
     */
    fun handleDiscoveredTag(tag: Tag) {
        scope.launch {
            // Check if there is a pending write request first
            val writePayload = pendingTagWritePayload
            if (writePayload != null) {
                val tagType = getTagTypeName(tag)
                val uidHex = tag.id?.joinToString(":") { "%02X".format(it) } ?: "UNKNOWN"
                _tagWriteStatus.tryEmit(TagWriteResult.TagDetected(tagType, uidHex))

                val result = writePayloadToTagDetailed(tag, writePayload, tagType)
                _tagWriteStatus.tryEmit(result)
                if (result is TagWriteResult.Success) {
                    pendingTagWritePayload = null
                    triggerHapticFeedback()
                }
                return@launch
            }

            // Otherwise, attempt to read contact
            val payload = readContactFromTag(tag)
            if (payload != null) {
                triggerHapticFeedback()
                _incomingBeamedContact.tryEmit(payload)
            }
        }
    }

    /**
     * Handle incoming Intent containing NDEF payload.
     */
    fun handleIntent(intent: Intent?) {
        if (intent == null) return

        // 1. Direct deep-link URI (e.g. launched via NDEF_DISCOVERED, AAR, or VIEW)
        val dataUri = intent.data?.toString()
        if (!dataUri.isNullOrBlank()) {
            val payload = QrCodeEngine.parseLineaScannedText(dataUri) ?: QrCodeEngine.parseScannedText(dataUri)
            if (payload != null) {
                _incomingBeamedContact.tryEmit(payload)
                return
            }
        }

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
        isTagOperationInProgress = true
        try {
            // 1. Try ISO-DEP (Peer-to-peer phone Host Card Emulation)
            val isoDep = IsoDep.get(tag)
            if (isoDep != null) {
                try {
                    isoDep.connect()
                    isoDep.timeout = 3000

                    // Step A: Send SELECT AID command for LiNEA Custom AID F0010203040506:
                    // CLA=00, INS=A4, P1=04, P2=00, Lc=07, DATA=F0010203040506 (clean 12 bytes)
                    val selectAidApdu = byteArrayOf(
                        0x00, 0xA4.toByte(), 0x04, 0x00, 0x07,
                        0xF0.toByte(), 0x01, 0x02, 0x03, 0x04, 0x05, 0x06
                    )
                    val selectResponse = isoDep.transceive(selectAidApdu)

                    var peerPayload: QrCodeEngine.ContactPayload? = null

                    if (selectResponse != null && selectResponse.size >= 2 &&
                        selectResponse[selectResponse.size - 2] == 0x90.toByte() &&
                        selectResponse[selectResponse.size - 1] == 0x00.toByte()
                    ) {
                        // Check if peer returned payload directly on SELECT AID (for small cards <= 240 bytes)
                        if (selectResponse.size > 2) {
                            val payloadBytes = selectResponse.copyOfRange(0, selectResponse.size - 2)
                            val text = String(payloadBytes, StandardCharsets.UTF_8).trim()
                            peerPayload = QrCodeEngine.parseLineaScannedText(text) ?: QrCodeEngine.parseScannedText(text)
                        }

                        // Prepare outbound chunks to transmit reader's contact card to the target phone
                        val outboundPayload = activeBeamPayload ?: userProfilePayload
                        val outboundUriBytes = if (outboundPayload != null) {
                            QrCodeEngine.serializeToUri(outboundPayload).toByteArray(StandardCharsets.UTF_8)
                        } else ByteArray(0)

                        val outboundChunks = if (outboundUriBytes.isNotEmpty()) {
                            outboundUriBytes.toList().chunked(LineaBeamApduService.CHUNK_SIZE).map { it.toByteArray() }
                        } else emptyList()

                        val readerTotalBytes = outboundUriBytes.size
                        val readerTotalChunks = outboundChunks.size

                        // Step B: INIT_EXCHANGE (INS = A0)
                        val initApdu = byteArrayOf(
                            0x00, LineaBeamApduService.INS_INIT_EXCHANGE.toByte(), 0x00, 0x00, 0x04,
                            ((readerTotalBytes shr 8) and 0xFF).toByte(),
                            (readerTotalBytes and 0xFF).toByte(),
                            ((readerTotalChunks shr 8) and 0xFF).toByte(),
                            (readerTotalChunks and 0xFF).toByte(),
                            0x04
                        )
                        var targetTotalChunks = 0
                        try {
                            val initResp = isoDep.transceive(initApdu)
                            if (initResp != null && initResp.size >= 6 &&
                                initResp[initResp.size - 2] == 0x90.toByte() &&
                                initResp[initResp.size - 1] == 0x00.toByte()
                            ) {
                                targetTotalChunks = ((initResp[2].toInt() and 0xFF) shl 8) or (initResp[3].toInt() and 0xFF)
                            }
                        } catch (_: Exception) {}

                        // Step C: Send reader chunks to target (INS = A1)
                        for (i in outboundChunks.indices) {
                            val chunk = outboundChunks[i]
                            val sendChunkApdu = byteArrayOf(
                                0x00, LineaBeamApduService.INS_SEND_CHUNK.toByte(),
                                ((i shr 8) and 0xFF).toByte(),
                                (i and 0xFF).toByte(),
                                chunk.size.toByte()
                            ) + chunk + byteArrayOf(0x00)
                            try {
                                isoDep.transceive(sendChunkApdu)
                            } catch (_: Exception) {
                                break
                            }
                        }

                        // Step D: Retrieve target chunks (INS = A2) if needed
                        if (peerPayload == null && targetTotalChunks > 0) {
                            val targetChunks = mutableListOf<ByteArray>()
                            for (i in 0 until targetTotalChunks) {
                                val getChunkApdu = byteArrayOf(
                                    0x00, LineaBeamApduService.INS_GET_CHUNK.toByte(),
                                    ((i shr 8) and 0xFF).toByte(),
                                    (i and 0xFF).toByte(),
                                    0x00
                                )
                                val chunkResp = isoDep.transceive(getChunkApdu)
                                if (chunkResp != null && chunkResp.size >= 2 &&
                                    chunkResp[chunkResp.size - 2] == 0x90.toByte() &&
                                    chunkResp[chunkResp.size - 1] == 0x00.toByte()
                                ) {
                                    targetChunks.add(chunkResp.copyOfRange(0, chunkResp.size - 2))
                                } else {
                                    break
                                }
                            }

                            if (targetChunks.size == targetTotalChunks) {
                                val combinedBytes = targetChunks.flatMap { it.toList() }.toByteArray()
                                val text = String(combinedBytes, StandardCharsets.UTF_8).trim()
                                peerPayload = QrCodeEngine.parseLineaScannedText(text) ?: QrCodeEngine.parseScannedText(text)
                            }
                        }

                        // Fallback: Legacy SWAP_CARDS (INS = A5) if chunking was not used
                        if (peerPayload == null && outboundUriBytes.size <= 240) {
                            try {
                                val swapApdu = byteArrayOf(0x00, LineaBeamApduService.INS_SWAP_CARDS.toByte(), 0x00, 0x00, outboundUriBytes.size.toByte()) +
                                        outboundUriBytes + byteArrayOf(0x00)
                                val swapResponse = isoDep.transceive(swapApdu)
                                if (swapResponse != null && swapResponse.size >= 2 &&
                                    swapResponse[swapResponse.size - 2] == 0x90.toByte() &&
                                    swapResponse[swapResponse.size - 1] == 0x00.toByte()
                                ) {
                                    val payloadBytes = swapResponse.copyOfRange(0, swapResponse.size - 2)
                                    val text = String(payloadBytes, StandardCharsets.UTF_8).trim()
                                    peerPayload = QrCodeEngine.parseLineaScannedText(text) ?: QrCodeEngine.parseScannedText(text)
                                }
                            } catch (_: Exception) {}
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
        } finally {
            isTagOperationInProgress = false
        }
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
                    val parsed = QrCodeEngine.parseLineaScannedText(uri.toString()) ?: QrCodeEngine.parseScannedText(uri.toString())
                    if (parsed != null) return parsed
                }
            } catch (_: Exception) {}

            // Check payload as text/vcard or raw URI string
            try {
                val text = String(record.payload, StandardCharsets.UTF_8).trim()
                val parsed = QrCodeEngine.parseLineaScannedText(text) ?: QrCodeEngine.parseScannedText(text)
                if (parsed != null) return parsed
            } catch (_: Exception) {}
        }
        return null
    }

    /**
     * Determines readable human tag technology name.
     */
    fun getTagTypeName(tag: Tag): String {
        val techList = tag.techList.map { it.substringAfterLast('.') }
        return when {
            techList.contains("Ndef") -> "NFC Forum NDEF Tag"
            techList.contains("NdefFormatable") -> "Blank/Unformatted NFC Tag"
            techList.contains("MifareUltralight") -> "NTAG / Mifare Ultralight"
            techList.contains("MifareClassic") -> "Mifare Classic"
            techList.contains("IsoDep") -> "ISO-DEP Smart Card / Phone"
            techList.contains("NfcA") -> "NFC-A Tag"
            techList.contains("NfcB") -> "NFC-B Tag"
            techList.contains("NfcF") -> "NFC-F (FeliCa)"
            techList.contains("NfcV") -> "NFC-V (ISO 15693)"
            else -> techList.firstOrNull() ?: "Standard NFC Tag"
        }
    }

    /**
     * Writes contact payload to a physical rewritable NFC tag with exact diagnostics.
     */
    private fun writePayloadToTagDetailed(
        tag: Tag,
        payload: QrCodeEngine.ContactPayload,
        tagType: String
    ): TagWriteResult {
        val uri = QrCodeEngine.serializeToUri(payload)
        val uriRecord = NdefRecord.createUri(Uri.parse(uri))
        val aarRecord = NdefRecord.createApplicationRecord("com.ryanshelby.linea")
        val message = NdefMessage(arrayOf(uriRecord, aarRecord))
        val requiredBytes = message.toByteArray().size

        val ndef = Ndef.get(tag)
        if (ndef != null) {
            try {
                ndef.connect()
                if (!ndef.isWritable) {
                    try { ndef.close() } catch (_: Exception) {}
                    return TagWriteResult.Error(
                        title = "Tag Is Write-Protected",
                        reason = "This NFC tag or card is permanently locked or configured as read-only. Please use an unlocked rewritable tag.",
                        tagType = tagType
                    )
                }
                if (ndef.maxSize < requiredBytes) {
                    val available = ndef.maxSize
                    try { ndef.close() } catch (_: Exception) {}
                    return TagWriteResult.Error(
                        title = "Tag Memory Too Small",
                        reason = "Contact card requires $requiredBytes bytes, but this tag only holds $available bytes. Consider using an NTAG215 (504 bytes) or NTAG216 (888 bytes), or remove profile photo to reduce card size.",
                        tagType = tagType,
                        requiredBytes = requiredBytes,
                        availableBytes = available
                    )
                }
                ndef.writeNdefMessage(message)
                try { ndef.close() } catch (_: Exception) {}
                return TagWriteResult.Success(
                    contactName = payload.name,
                    tagType = tagType,
                    bytesWritten = requiredBytes
                )
            } catch (e: android.nfc.TagLostException) {
                try { ndef.close() } catch (_: Exception) {}
                return TagWriteResult.Error(
                    title = "Tag Connection Lost",
                    reason = "The tag was moved away before writing finished. Hold the tag firmly against the NFC antenna on the back of your phone.",
                    tagType = tagType
                )
            } catch (e: android.nfc.FormatException) {
                try { ndef.close() } catch (_: Exception) {}
                return TagWriteResult.Error(
                    title = "NDEF Format Error",
                    reason = "Tag rejected NDEF structure: ${e.localizedMessage ?: "Corrupted sector format"}.",
                    tagType = tagType
                )
            } catch (e: java.io.IOException) {
                try { ndef.close() } catch (_: Exception) {}
                return TagWriteResult.Error(
                    title = "I/O Communication Error",
                    reason = "Failed to communicate with tag (${e.localizedMessage ?: "Hardware timeout"}). Ensure tag is positioned steadily.",
                    tagType = tagType
                )
            } catch (e: Exception) {
                try { ndef.close() } catch (_: Exception) {}
                return TagWriteResult.Error(
                    title = "Writing Failed",
                    reason = e.localizedMessage ?: "Unknown error writing to NFC tag.",
                    tagType = tagType
                )
            }
        }

        val formatable = NdefFormatable.get(tag)
        if (formatable != null) {
            try {
                formatable.connect()
                formatable.format(message)
                try { formatable.close() } catch (_: Exception) {}
                return TagWriteResult.Success(
                    contactName = payload.name,
                    tagType = "$tagType (Formatted)",
                    bytesWritten = requiredBytes
                )
            } catch (e: android.nfc.TagLostException) {
                try { formatable.close() } catch (_: Exception) {}
                return TagWriteResult.Error(
                    title = "Tag Connection Lost",
                    reason = "The tag was moved away during formatting. Keep tag held steadily.",
                    tagType = tagType
                )
            } catch (e: Exception) {
                try { formatable.close() } catch (_: Exception) {}
                return TagWriteResult.Error(
                    title = "Formatting Failed",
                    reason = "Failed to format tag as NDEF: ${e.localizedMessage ?: "Tag not compatible with NDEF standard"}.",
                    tagType = tagType
                )
            }
        }

        // Tag does not support NDEF or formatting
        val techList = tag.techList.map { it.substringAfterLast('.') }.joinToString(", ")
        return TagWriteResult.Error(
            title = "Unsupported NFC Tag",
            reason = "This tag does not support standard NDEF formatting (Detected: $techList). Linea requires rewritable NDEF tags (such as NTAG213, NTAG215, NTAG216, or Mifare Ultralight). Payment cards and transit tokens cannot be overwritten.",
            tagType = tagType
        )
    }
}
