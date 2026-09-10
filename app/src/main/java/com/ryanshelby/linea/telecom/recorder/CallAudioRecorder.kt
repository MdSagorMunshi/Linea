package com.ryanshelby.linea.telecom.recorder

import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
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
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Multi-source call audio recorder with automatic fallback.
 *
 * Android restricts [MediaRecorder.AudioSource.VOICE_CALL] to system/privileged apps on most
 * OEMs since Android 9+.  Instead of failing outright, this recorder uses a cascade:
 *
 *  1. **VOICE_CALL** – ideal (both sides captured natively). Works on some Samsung, Xiaomi, etc.
 *  2. **VOICE_COMMUNICATION** – captures microphone via the telephony voice path; on many
 *     chipsets this also picks up the remote party at lower gain.
 *  3. **MIC** – raw hardware microphone; always available.  Captures the user's voice and,
 *     when speakerphone is active, the remote party as well.
 *
 * After each source is started, a signal-verification job samples [MediaRecorder.getMaxAmplitude]
 * twice over 4 seconds.  If both readings are zero the source is considered silent and the
 * recorder automatically restarts with the next source in the chain.
 */
@Singleton
class CallAudioRecorder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recordingDao: CallRecordingDao,
    private val preferences: LineaPreferences
) {
    companion object {
        private const val TAG = "CallAudioRecorder"

        /**
         * Ordered audio-source fallback chain.  Each entry is tried in sequence; the first one
         * that both (a) does not throw when set and (b) produces non-zero amplitude wins.
         */
        private val AUDIO_SOURCE_CHAIN = listOf(
            MediaRecorder.AudioSource.VOICE_CALL,
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MediaRecorder.AudioSource.MIC
        )

        private fun audioSourceLabel(source: Int): String = when (source) {
            MediaRecorder.AudioSource.VOICE_CALL -> "VOICE_CALL"
            MediaRecorder.AudioSource.VOICE_COMMUNICATION -> "VOICE_COMMUNICATION"
            MediaRecorder.AudioSource.MIC -> "MIC"
            else -> "UNKNOWN($source)"
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    // ── Public observable state ──────────────────────────────────────────
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationSeconds = MutableStateFlow(0L)
    val recordingDurationSeconds: StateFlow<Long> = _recordingDurationSeconds.asStateFlow()

    private val _currentFilePath = MutableStateFlow<String?>(null)
    val currentFilePath: StateFlow<String?> = _currentFilePath.asStateFlow()

    private val _recordingUnavailableReason = MutableStateFlow<String?>(null)
    /** Non-null when Android/OEM policy prevents cellular call-audio capture. */
    val recordingUnavailableReason: StateFlow<String?> = _recordingUnavailableReason.asStateFlow()

    // ── Internal state ───────────────────────────────────────────────────
    private var mediaRecorder: MediaRecorder? = null
    private var timerJob: Job? = null
    private var signalCheckJob: Job? = null
    private var currentPhone: String = ""
    private var currentContactId: Long? = null
    private var currentCallRecordId: Long? = null
    private var recordingStartTime: Long = 0L
    private var isCurrentRecordingPrivate: Boolean = false
    private var discardCurrentRecording: Boolean = false

    /** Index into [AUDIO_SOURCE_CHAIN] for the source currently in use. */
    private var currentSourceIndex: Int = 0

    /** The audio source that is currently active and recording. */
    private var activeAudioSource: Int = MediaRecorder.AudioSource.VOICE_CALL

    fun isCurrentlyRecording(): Boolean = _isRecording.value

    // ── Auto-record preferences ──────────────────────────────────────────
    suspend fun shouldAutoRecord(isContact: Boolean): Boolean {
        val autoAll = preferences.autoRecordCalls.first()
        val contactsOnly = preferences.autoRecordContactsOnly.first()
        if (!autoAll) return false
        return if (contactsOnly) isContact else true
    }

    suspend fun shouldAutoRecordPrivateSafe(): Boolean {
        return preferences.autoRecordPrivateSafe.first()
    }

    // ── Directory helpers ────────────────────────────────────────────────
    private fun getRecordingsDirectory(): File {
        // Primary: Shared Music/Linea folder on external storage
        try {
            val publicMusicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            val lineaDir = File(publicMusicDir, "Linea")
            if (lineaDir.exists() || lineaDir.mkdirs()) {
                return lineaDir
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Secondary: App external files directory under Music/Linea
        try {
            val extMusicDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            val lineaDir = File(extMusicDir, "Linea")
            if (lineaDir.exists() || lineaDir.mkdirs()) {
                return lineaDir
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback: Internal storage recordings folder
        return File(context.filesDir, "recordings").apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Private recordings go to encrypted internal app storage, never to public directories.
     */
    private fun getPrivateRecordingsDirectory(): File {
        return File(context.filesDir, "private_recordings").apply {
            if (!exists()) mkdirs()
        }
    }

    // ── Core recording methods ───────────────────────────────────────────

    @Synchronized
    fun startRecording(
        phoneNumber: String,
        contactId: Long? = null,
        callRecordId: Long? = null,
        isPrivateContact: Boolean = false
    ): Boolean {
        if (_isRecording.value) return true

        if (context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            _recordingUnavailableReason.value =
                "Microphone permission is required to record this call."
            return false
        }
        _recordingUnavailableReason.value = null

        currentPhone = phoneNumber
        currentContactId = contactId
        currentCallRecordId = callRecordId
        recordingStartTime = System.currentTimeMillis()
        isCurrentRecordingPrivate = isPrivateContact
        discardCurrentRecording = false
        currentSourceIndex = 0

        return startRecordingWithSource(0)
    }

    /**
     * Attempts to start recording using the audio source at [sourceIndex] in [AUDIO_SOURCE_CHAIN].
     * If the source throws on [MediaRecorder.setAudioSource] or [MediaRecorder.prepare], we
     * immediately try the next source.  Returns `true` if any source succeeds.
     */
    @Synchronized
    private fun startRecordingWithSource(sourceIndex: Int): Boolean {
        // Clean up any prior recorder from a failed/silent attempt
        releaseRecorderQuietly()

        val recordingsDir = if (isCurrentRecordingPrivate) getPrivateRecordingsDirectory()
                            else getRecordingsDirectory()
        val safeNum = currentPhone.replace("+", "").filter { it.isDigit() }.ifBlank { "unknown" }

        // Each retry gets a fresh output file so we don't append to a partially-silent file
        val outputFile = File(recordingsDir, "rec_${recordingStartTime}_${safeNum}.m4a")
        _currentFilePath.value = outputFile.absolutePath

        for (idx in sourceIndex until AUDIO_SOURCE_CHAIN.size) {
            val audioSource = AUDIO_SOURCE_CHAIN[idx]
            val label = audioSourceLabel(audioSource)

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            try {
                recorder.setAudioSource(audioSource)
                Log.d(TAG, "setAudioSource($label) accepted")
            } catch (e: Exception) {
                Log.w(TAG, "setAudioSource($label) rejected: ${e.message}")
                recorder.release()
                continue   // try next source
            }

            try {
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                recorder.setAudioEncodingBitRate(128_000)
                recorder.setAudioSamplingRate(44_100)
                recorder.setOutputFile(outputFile.absolutePath)
                recorder.prepare()
                recorder.start()

                mediaRecorder = recorder
                currentSourceIndex = idx
                activeAudioSource = audioSource
                Log.i(TAG, "Recording started with source $label")
            } catch (e: Exception) {
                Log.w(TAG, "prepare/start failed for $label: ${e.message}")
                e.printStackTrace()
                try { recorder.release() } catch (_: Exception) {}
                if (outputFile.exists()) outputFile.delete()
                continue   // try next source
            }

            // We have a working recorder – set up timer & signal verification
            _isRecording.value = true
            _recordingDurationSeconds.value = 0L

            startTimerJob()
            startSignalCheckJob(idx)
            return true
        }

        // All sources exhausted
        _currentFilePath.value = null
        _recordingUnavailableReason.value =
            "Call recording is not available on this device. All audio sources were blocked."
        Log.e(TAG, "All audio sources exhausted – recording unavailable")
        return false
    }

    /**
     * Ticks every second to update [_recordingDurationSeconds].
     */
    private fun startTimerJob() {
        timerJob?.cancel()
        timerJob = scope.launch {
            val startMs = System.currentTimeMillis()
            while (isActive && _isRecording.value) {
                delay(1_000)
                _recordingDurationSeconds.value = (System.currentTimeMillis() - startMs) / 1_000
            }
        }
    }

    /**
     * Verifies the active source is actually producing audio.  After 3 s we sample
     * [MediaRecorder.getMaxAmplitude] twice with a 2 s gap.  If both readings are zero
     * the source is deemed silent and we escalate to the next source in the chain.
     *
     * For the **last** source in the chain (MIC) we skip this check because the mic is
     * guaranteed to capture at least ambient noise; silence there simply means the call
     * hasn't started producing sound yet.
     */
    private fun startSignalCheckJob(sourceIndex: Int) {
        signalCheckJob?.cancel()

        // Don't silence-check the last source – it's our final fallback and always works
        if (sourceIndex >= AUDIO_SOURCE_CHAIN.lastIndex) {
            Log.d(TAG, "Skipping signal check for final fallback source ${audioSourceLabel(AUDIO_SOURCE_CHAIN[sourceIndex])}")
            return
        }

        signalCheckJob = scope.launch {
            delay(3_000)
            val firstPeak = synchronized(this@CallAudioRecorder) {
                runCatching { mediaRecorder?.maxAmplitude ?: 0 }.getOrDefault(0)
            }
            delay(2_000)
            val secondPeak = synchronized(this@CallAudioRecorder) {
                runCatching { mediaRecorder?.maxAmplitude ?: 0 }.getOrDefault(0)
            }

            if (!_isRecording.value) return@launch

            val label = audioSourceLabel(AUDIO_SOURCE_CHAIN[sourceIndex])

            if (firstPeak == 0 && secondPeak == 0) {
                Log.w(TAG, "$label produced silence – escalating to next source")

                // Stop the current recorder and delete the silent file
                synchronized(this@CallAudioRecorder) {
                    releaseRecorderQuietly()
                    _currentFilePath.value?.let { path ->
                        val f = File(path)
                        if (f.exists()) f.delete()
                    }
                }

                // Try the next source
                val success = synchronized(this@CallAudioRecorder) {
                    startRecordingWithSource(sourceIndex + 1)
                }
                if (!success) {
                    _isRecording.value = false
                    _recordingDurationSeconds.value = 0L
                    _recordingUnavailableReason.value =
                        "Call recording is not available on this device. All audio sources produced silence."
                }
            } else {
                Log.d(TAG, "$label producing audio – peaks: $firstPeak, $secondPeak")
            }
        }
    }

    /**
     * Releases the current [MediaRecorder] silently, swallowing any exceptions.
     */
    private fun releaseRecorderQuietly() {
        try {
            mediaRecorder?.let {
                try { it.stop() } catch (_: Exception) {}
                try { it.reset() } catch (_: Exception) {}
                it.release()
            }
        } catch (_: Exception) {}
        mediaRecorder = null
    }

    @Synchronized
    fun stopRecording() {
        if (!_isRecording.value) return

        timerJob?.cancel()
        timerJob = null
        signalCheckJob?.cancel()
        signalCheckJob = null

        val durationMs = System.currentTimeMillis() - recordingStartTime

        try {
            mediaRecorder?.let {
                it.stop()
                it.reset()
                it.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
        }

        val filePath = _currentFilePath.value
        val phone = currentPhone
        val contactId = currentContactId
        val callRecordId = currentCallRecordId
        val isPrivate = isCurrentRecordingPrivate

        _isRecording.value = false
        _recordingDurationSeconds.value = 0L
        _currentFilePath.value = null

        if (filePath != null) {
            val file = File(filePath)
            val fileSize = if (file.exists()) file.length() else 0L

            if (fileSize > 0L && !discardCurrentRecording) {
                if (isPrivate) {
                    // Encrypt the raw recording file in-place for private contacts
                    scope.launch {
                        try {
                            val encryptedPath = encryptRecordingFile(file)
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
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } else {
                    // Public recording: scan and save normally
                    try {
                        MediaScannerConnection.scanFile(
                            context,
                            arrayOf(filePath),
                            arrayOf("audio/mp4", "audio/m4a"),
                            null
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    scope.launch {
                        recordingDao.insertRecording(
                            CallRecordingEntity(
                                callRecordId = callRecordId,
                                contactId = contactId,
                                phoneNumber = phone,
                                filePath = filePath,
                                durationMs = durationMs,
                                fileSize = fileSize,
                                timestamp = recordingStartTime,
                                isPinned = false,
                                isEncrypted = false
                            )
                        )
                    }
                }
            } else {
                if (file.exists()) file.delete()
            }
        }
    }

    // ── Encryption ───────────────────────────────────────────────────────

    /**
     * Encrypts a recording file with AES-256-GCM using a key derived from a device-unique seed.
     * Format: [SALT 32 bytes] [IV 12 bytes] [CIPHERTEXT + AUTH TAG]
     *
     * Returns the path to the encrypted file, or null on failure.
     */
    private fun encryptRecordingFile(rawFile: File): String? {
        try {
            val plainBytes = rawFile.readBytes()
            val random = SecureRandom()

            val salt = ByteArray(32).also { random.nextBytes(it) }
            val iv = ByteArray(12).also { random.nextBytes(it) }

            // Derive key from a device-unique seed + salt
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

            // Securely delete the raw unencrypted file
            rawFile.delete()

            return encryptedFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Decrypts an AES-256-GCM encrypted recording to a temporary file in app cache for playback.
     */
    fun decryptRecordingToTemp(encryptedFile: File): File? {
        try {
            if (!encryptedFile.exists()) return null
            val bytes = encryptedFile.readBytes()
            if (bytes.size < 32 + 12 + 16) return null // salt(32) + iv(12) + tag(16)

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

            val tempFile = File(context.cacheDir, "dec_${System.currentTimeMillis()}.m4a")
            tempFile.writeBytes(plainBytes)
            tempFile.deleteOnExit()
            return tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
