package com.ryanshelby.linea.telecom.recorder

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.MediaScannerConnection
import android.os.Environment
import android.os.Process
import android.util.Log
import com.ryanshelby.linea.data.local.dao.CallRecordingDao
import com.ryanshelby.linea.data.local.entities.CallRecordingEntity
import com.ryanshelby.linea.data.preferences.LineaPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Unified Call Audio Capture and Recording Engine.
 *
 * Architectural Design:
 * 1. Single AudioRecord Pipeline: Eliminates all microphone hardware contention and HAL deadlocks
 *    by running exactly ONE capture loop during calls.
 * 2. Un-silenced Capture: Operates in tandem with [LineaCallAudioService] (Accessibility Client)
 *    to bypass AOSP AudioPolicyManager concurrent capture stream-silencing on Android 10+.
 * 3. Dual-mode Audio Sources: Tries direct telephony modem capture (VOICE_CALL) for rooted /
 *    system-privileged devices first (the ODialer method), then falls back to clean acoustic
 *    loudspeaker capture (VOICE_RECOGNITION -> MIC) with software digital gain (the Cube ACR method).
 * 4. Software Digital Gain (AGC): Boosts acoustic speakerphone samples by 3.0x with saturation
 *    clamping so remote caller voice is crisp, loud, and clearly audible.
 * 5. High-Reliability WAV Encoding: Writes 16-bit 16kHz mono WAV files directly, preventing MediaCodec
 *    initialization crashes and making recordings instantly playable everywhere.
 */
@Singleton
class CallAudioRecorder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recordingDao: CallRecordingDao,
    private val preferences: LineaPreferences,
    private val audioLevelMonitor: CallAudioLevelMonitor
) {
    companion object {
        private const val TAG = "CallAudioRecorder"
        private const val SAMPLE_RATE = 16000
        private const val CHANNELS = 1
        private const val BITS_PER_SAMPLE = 16
        private const val DIGITAL_GAIN_FACTOR = 3.0f

        /**
         * Priority source chain:
         * 1. VOICE_CALL: Direct modem stream (works if privileged/rooted like ODialer)
         * 2. VOICE_RECOGNITION: Bypasses telephony acoustic echo cancellation
         * 3. MIC: Universal hardware fallback
         * 4. VOICE_COMMUNICATION: Standard voice call audio path
         * 5. DEFAULT: System default
         */
        private val AUDIO_SOURCE_CANDIDATES = listOf(
            MediaRecorder.AudioSource.VOICE_CALL,
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MediaRecorder.AudioSource.DEFAULT
        )

        private fun audioSourceLabel(source: Int): String = when (source) {
            MediaRecorder.AudioSource.VOICE_CALL -> "VOICE_CALL (Direct Modem)"
            MediaRecorder.AudioSource.VOICE_RECOGNITION -> "VOICE_RECOGNITION (Acoustic)"
            MediaRecorder.AudioSource.MIC -> "MIC (Hardware)"
            MediaRecorder.AudioSource.VOICE_COMMUNICATION -> "VOICE_COMMUNICATION"
            MediaRecorder.AudioSource.DEFAULT -> "DEFAULT"
            else -> "UNKNOWN($source)"
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    // ── Public Observable State ──────────────────────────────────────────
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationSeconds = MutableStateFlow(0L)
    val recordingDurationSeconds: StateFlow<Long> = _recordingDurationSeconds.asStateFlow()

    private val _currentFilePath = MutableStateFlow<String?>(null)
    val currentFilePath: StateFlow<String?> = _currentFilePath.asStateFlow()

    private val _recordingUnavailableReason = MutableStateFlow<String?>(null)
    val recordingUnavailableReason: StateFlow<String?> = _recordingUnavailableReason.asStateFlow()

    // ── Internal Audio Capture State ─────────────────────────────────────
    private var activeAudioRecord: AudioRecord? = null
    private var captureThread: Thread? = null
    @Volatile private var isCaptureActive: Boolean = false

    // ── File Recording State ─────────────────────────────────────────────
    @Volatile private var isWritingToFile: Boolean = false
    private var recordingFileOutputStream: FileOutputStream? = null
    private var recordedBytesCount: Long = 0L
    private var recordingStartTime: Long = 0L
    private var currentPhone: String = ""
    private var currentContactId: Long? = null
    private var currentCallRecordId: Long? = null
    private var isCurrentRecordingPrivate: Boolean = false
    private var timerJob: Job? = null

    @Volatile private var lastPeakAmplitude: Int = 0

    fun isCurrentlyRecording(): Boolean = _isRecording.value

    fun getMaxAmplitude(): Int = lastPeakAmplitude

    // ── Auto-record Preferences ──────────────────────────────────────────
    suspend fun shouldAutoRecord(isContact: Boolean): Boolean {
        if (!LineaCallAudioService.isServiceEnabled(context)) return false
        val autoAll = preferences.autoRecordCalls.first()
        val contactsOnly = preferences.autoRecordContactsOnly.first()
        if (!autoAll) return false
        return if (contactsOnly) isContact else true
    }

    suspend fun shouldAutoRecordPrivateSafe(): Boolean {
        if (!LineaCallAudioService.isServiceEnabled(context)) return false
        return preferences.autoRecordPrivateSafe.first()
    }

    // ── Directory Helpers ────────────────────────────────────────────────
    private fun getRecordingsDirectory(): File {
        try {
            val extMusicDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            val lineaDir = File(extMusicDir, "Recordings")
            if (lineaDir.exists() || lineaDir.mkdirs()) {
                return lineaDir
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed creating external Music/Recordings dir: ${e.message}")
        }

        try {
            val publicMusicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            val lineaDir = File(publicMusicDir, "Linea")
            if (lineaDir.exists() || lineaDir.mkdirs()) {
                return lineaDir
            }
        } catch (_: Exception) {}

        return File(context.filesDir, "recordings").apply {
            if (!exists()) mkdirs()
        }
    }

    private fun getPrivateRecordingsDirectory(): File {
        return File(context.filesDir, "private_recordings").apply {
            if (!exists()) mkdirs()
        }
    }

    // ── Unified Audio Engine (Start & Stop Capture) ───────────────────────

    /**
     * Starts the single unified AudioRecord hardware session.
     * Called when a cellular call becomes ACTIVE to drive the live ECG waveform
     * and prepare for instantaneous recording with zero hardware delay or contention.
     */
    @Synchronized
    fun startCapture(): Boolean {
        if (isCaptureActive) return true

        if (context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            _recordingUnavailableReason.value = "Microphone permission required for call audio."
            Log.w(TAG, "RECORD_AUDIO permission not granted – capture aborted")
            return false
        }
        _recordingUnavailableReason.value = null

        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBuf * 2, 4096)

        var record: AudioRecord? = null
        var selectedSource = MediaRecorder.AudioSource.DEFAULT

        for (source in AUDIO_SOURCE_CANDIDATES) {
            try {
                val candidate = AudioRecord(
                    source,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )
                if (candidate.state == AudioRecord.STATE_INITIALIZED) {
                    record = candidate
                    selectedSource = source
                    Log.i(TAG, "AudioRecord successfully initialized using ${audioSourceLabel(source)}")
                    break
                } else {
                    candidate.release()
                }
            } catch (e: Exception) {
                Log.d(TAG, "Source ${audioSourceLabel(source)} not accepted: ${e.message}")
            }
        }

        if (record == null) {
            _recordingUnavailableReason.value = "Unable to initialize audio hardware for call capture."
            Log.e(TAG, "All audio sources exhausted. Capture failed.")
            return false
        }

        activeAudioRecord = record
        isCaptureActive = true

        val readerThread = Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            val chunkSamples = SAMPLE_RATE / 20 // 800 samples = 50ms chunk
            val shortBuffer = ShortArray(chunkSamples)
            val byteBuffer = ByteArray(chunkSamples * 2)

            try {
                record.startRecording()
                Log.i(TAG, "Audio capture loop running on ${audioSourceLabel(selectedSource)}")

                while (isCaptureActive) {
                    val readSamples = record.read(shortBuffer, 0, shortBuffer.size)
                    if (readSamples > 0) {
                        var sumSquares = 0.0
                        var peak = 0

                        for (i in 0 until readSamples) {
                            val sample = shortBuffer[i].toInt()
                            val abs = kotlin.math.abs(sample)
                            if (abs > peak) peak = abs
                            sumSquares += sample.toDouble() * sample.toDouble()
                        }
                        lastPeakAmplitude = peak

                        val rms = sqrt(sumSquares / readSamples)
                        // Feed the passive visualizer monitor directly
                        audioLevelMonitor.onAudioSampleRms(rms)

                        // If user has activated recording to file, apply digital gain and stream to disk
                        if (isWritingToFile) {
                            val fos = recordingFileOutputStream
                            if (fos != null) {
                                for (i in 0 until readSamples) {
                                    val boosted = (shortBuffer[i] * DIGITAL_GAIN_FACTOR)
                                        .toInt()
                                        .coerceIn(-32768, 32767)
                                        .toShort()

                                    byteBuffer[i * 2] = (boosted.toInt() and 0xFF).toByte()
                                    byteBuffer[i * 2 + 1] = ((boosted.toInt() shr 8) and 0xFF).toByte()
                                }
                                val bytesToWrite = readSamples * 2
                                fos.write(byteBuffer, 0, bytesToWrite)
                                recordedBytesCount += bytesToWrite
                            }
                        }
                    } else {
                        Thread.sleep(10)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Audio capture thread error: ${e.message}")
            } finally {
                try {
                    record.stop()
                    record.release()
                } catch (_: Exception) {}
                Log.i(TAG, "Audio capture thread ended")
            }
        }, "LineaAudioCaptureThread")

        captureThread = readerThread
        readerThread.start()
        return true
    }

    /**
     * Stops the unified audio capture engine and releases hardware resources.
     * Called when a call is DISCONNECTED.
     */
    @Synchronized
    fun stopCapture() {
        if (isWritingToFile) {
            stopRecording()
        }

        isCaptureActive = false
        captureThread?.interrupt()
        captureThread = null
        activeAudioRecord = null
        lastPeakAmplitude = 0
        audioLevelMonitor.reset()
        Log.i(TAG, "Audio capture stopped")
    }

    // ── Call Recording Methods ───────────────────────────────────────────

    @Synchronized
    fun startRecording(
        phoneNumber: String,
        contactId: Long? = null,
        callRecordId: Long? = null,
        isPrivateContact: Boolean = false
    ): Boolean {
        if (!LineaCallAudioService.isServiceEnabled(context)) {
            Log.w(TAG, "Cannot start recording: LineaCallAudioService is not enabled")
            _recordingUnavailableReason.value = "Call recording requires Linea Call Audio Service to be enabled in Settings."
            return false
        }
        if (_isRecording.value) return true

        if (!isCaptureActive) {
            val started = startCapture()
            if (!started) return false
        }

        currentPhone = phoneNumber
        currentContactId = contactId
        currentCallRecordId = callRecordId
        recordingStartTime = System.currentTimeMillis()
        isCurrentRecordingPrivate = isPrivateContact

        val recordingsDir = if (isPrivateContact) getPrivateRecordingsDirectory() else getRecordingsDirectory()
        val safeNum = phoneNumber.replace("+", "").filter { it.isDigit() }.ifBlank { "unknown" }
        val outputFile = File(recordingsDir, "rec_${recordingStartTime}_${safeNum}.wav")
        _currentFilePath.value = outputFile.absolutePath

        try {
            val fos = FileOutputStream(outputFile)
            // Write placeholder 44-byte WAV header
            writeWavHeaderPlaceholder(fos)
            recordingFileOutputStream = fos
            recordedBytesCount = 0L
            isWritingToFile = true
            _isRecording.value = true
            _recordingDurationSeconds.value = 0L
            _recordingUnavailableReason.value = null

            startTimerJob()
            Log.i(TAG, "Call recording started: ${outputFile.absolutePath} (WAV 16kHz)")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start file recording: ${e.message}")
            _recordingUnavailableReason.value = "Failed to create recording file."
            return false
        }
    }

    @Synchronized
    fun stopRecording() {
        if (!_isRecording.value && !isWritingToFile) return

        isWritingToFile = false
        _isRecording.value = false

        timerJob?.cancel()
        timerJob = null

        val durationMs = System.currentTimeMillis() - recordingStartTime
        val filePath = _currentFilePath.value
        val phone = currentPhone
        val contactId = currentContactId
        val callRecordId = currentCallRecordId
        val isPrivate = isCurrentRecordingPrivate

        _recordingDurationSeconds.value = 0L
        _currentFilePath.value = null

        try {
            recordingFileOutputStream?.flush()
            recordingFileOutputStream?.close()
        } catch (_: Exception) {}
        recordingFileOutputStream = null

        if (filePath != null) {
            val rawFile = File(filePath)
            if (rawFile.exists() && recordedBytesCount > 0L) {
                // Finalize WAV header with actual data chunk sizes
                finalizeWavHeader(rawFile, recordedBytesCount)
                val finalFileSize = rawFile.length()

                if (isPrivate) {
                    // Encrypt file for private contacts
                    scope.launch {
                        try {
                            val encryptedPath = encryptRecordingFile(rawFile)
                            if (encryptedPath != null) {
                                recordingDao.insertRecording(
                                    CallRecordingEntity(
                                        callRecordId = callRecordId,
                                        contactId = contactId,
                                        phoneNumber = phone,
                                        filePath = encryptedPath,
                                        durationMs = durationMs,
                                        fileSize = File(encryptedPath).length(),
                                        timestamp = recordingStartTime,
                                        isPinned = false,
                                        isEncrypted = true
                                    )
                                )
                                Log.i(TAG, "Private call recording encrypted: $encryptedPath")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Encryption failed: ${e.message}")
                        }
                    }
                } else {
                    // Register public recording with MediaScanner & Room DB
                    try {
                        MediaScannerConnection.scanFile(
                            context,
                            arrayOf(filePath),
                            arrayOf("audio/wav", "audio/x-wav"),
                            null
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "MediaScanner error: ${e.message}")
                    }

                    scope.launch {
                        recordingDao.insertRecording(
                            CallRecordingEntity(
                                callRecordId = callRecordId,
                                contactId = contactId,
                                phoneNumber = phone,
                                filePath = filePath,
                                durationMs = durationMs,
                                fileSize = finalFileSize,
                                timestamp = recordingStartTime,
                                isPinned = false,
                                isEncrypted = false
                            )
                        )
                        Log.i(TAG, "Call recording saved: $filePath ($finalFileSize bytes, ${durationMs}ms)")
                    }
                }
            } else {
                if (rawFile.exists()) rawFile.delete()
            }
        }
    }

    private fun startTimerJob() {
        timerJob?.cancel()
        timerJob = scope.launch {
            val startMs = System.currentTimeMillis()
            while (isActive && _isRecording.value) {
                delay(1000)
                _recordingDurationSeconds.value = (System.currentTimeMillis() - startMs) / 1000
            }
        }
    }

    // ── WAV Header Formatting ─────────────────────────────────────────────

    private fun writeWavHeaderPlaceholder(out: FileOutputStream) {
        val header = ByteArray(44)
        val byteRate = SAMPLE_RATE * CHANNELS * BITS_PER_SAMPLE / 8
        val blockAlign = (CHANNELS * BITS_PER_SAMPLE / 8).toShort()

        // RIFF chunk
        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        // bytes 4..7: placeholder for ChunkSize
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()

        // fmt chunk
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0 // Subchunk1Size = 16 for PCM
        header[20] = 1; header[21] = 0 // AudioFormat = 1 (PCM)
        header[22] = CHANNELS.toByte(); header[23] = 0
        header[24] = (SAMPLE_RATE and 0xFF).toByte()
        header[25] = ((SAMPLE_RATE shr 8) and 0xFF).toByte()
        header[26] = ((SAMPLE_RATE shr 16) and 0xFF).toByte()
        header[27] = ((SAMPLE_RATE shr 24) and 0xFF).toByte()
        header[28] = (byteRate and 0xFF).toByte()
        header[29] = ((byteRate shr 8) and 0xFF).toByte()
        header[30] = ((byteRate shr 16) and 0xFF).toByte()
        header[31] = ((byteRate shr 24) and 0xFF).toByte()
        header[32] = (blockAlign.toInt() and 0xFF).toByte()
        header[33] = ((blockAlign.toInt() shr 8) and 0xFF).toByte()
        header[34] = BITS_PER_SAMPLE.toByte(); header[35] = 0

        // data chunk
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        // bytes 40..43: placeholder for Subchunk2Size (data size)

        out.write(header)
    }

    private fun finalizeWavHeader(file: File, pcmDataLength: Long) {
        try {
            RandomAccessFile(file, "rw").use { raf ->
                val totalChunkSize = pcmDataLength + 36
                // Seek to RIFF ChunkSize (bytes 4..7)
                raf.seek(4)
                raf.write(
                    byteArrayOf(
                        (totalChunkSize and 0xFF).toByte(),
                        ((totalChunkSize shr 8) and 0xFF).toByte(),
                        ((totalChunkSize shr 16) and 0xFF).toByte(),
                        ((totalChunkSize shr 24) and 0xFF).toByte()
                    )
                )
                // Seek to data Subchunk2Size (bytes 40..43)
                raf.seek(40)
                raf.write(
                    byteArrayOf(
                        (pcmDataLength and 0xFF).toByte(),
                        ((pcmDataLength shr 8) and 0xFF).toByte(),
                        ((pcmDataLength shr 16) and 0xFF).toByte(),
                        ((pcmDataLength shr 24) and 0xFF).toByte()
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finalizing WAV header: ${e.message}")
        }
    }

    // ── Encryption & Decryption ──────────────────────────────────────────

    private fun encryptRecordingFile(rawFile: File): String? {
        try {
            val plainBytes = rawFile.readBytes()
            val random = SecureRandom()

            val salt = ByteArray(32).also { random.nextBytes(it) }
            val iv = ByteArray(12).also { random.nextBytes(it) }

            val deviceSeed = (context.packageName + android.os.Build.FINGERPRINT).toCharArray()
            val keySpec = PBEKeySpec(deviceSeed, salt, 50_000, 256)
            val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
            val keyBytes = keyFactory.generateSecret(keySpec).encoded
            val secretKey = SecretKeySpec(keyBytes, "AES")

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
            val cipherBytes = cipher.doFinal(plainBytes)

            val encryptedFile = File(rawFile.parent, rawFile.nameWithoutExtension + ".enc")
            encryptedFile.outputStream().use { os ->
                os.write(salt)
                os.write(iv)
                os.write(cipherBytes)
            }

            rawFile.delete()
            return encryptedFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "File encryption error: ${e.message}")
            return null
        }
    }

    fun decryptRecordingToTemp(encryptedFile: File): File? {
        try {
            if (!encryptedFile.exists()) return null
            val bytes = encryptedFile.readBytes()
            if (bytes.size < 32 + 12 + 16) return null

            val salt = bytes.copyOfRange(0, 32)
            val iv = bytes.copyOfRange(32, 44)
            val cipherBytes = bytes.copyOfRange(44, bytes.size)

            val deviceSeed = (context.packageName + android.os.Build.FINGERPRINT).toCharArray()
            val keySpec = PBEKeySpec(deviceSeed, salt, 50_000, 256)
            val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
            val keyBytes = keyFactory.generateSecret(keySpec).encoded
            val secretKey = SecretKeySpec(keyBytes, "AES")

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
            val plainBytes = cipher.doFinal(cipherBytes)

            val tempFile = File(context.cacheDir, "dec_${System.currentTimeMillis()}.wav")
            tempFile.writeBytes(plainBytes)
            tempFile.deleteOnExit()
            return tempFile
        } catch (e: Exception) {
            Log.e(TAG, "File decryption error: ${e.message}")
            return null
        }
    }
}
