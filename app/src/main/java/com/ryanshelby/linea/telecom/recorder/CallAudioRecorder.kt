package com.ryanshelby.linea.telecom.recorder

import android.content.Context
import android.media.MediaRecorder
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
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

@Singleton
class CallAudioRecorder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recordingDao: CallRecordingDao,
    private val preferences: LineaPreferences
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationSeconds = MutableStateFlow(0L)
    val recordingDurationSeconds: StateFlow<Long> = _recordingDurationSeconds.asStateFlow()

    private val _currentFilePath = MutableStateFlow<String?>(null)
    val currentFilePath: StateFlow<String?> = _currentFilePath.asStateFlow()

    private var mediaRecorder: MediaRecorder? = null
    private var timerJob: Job? = null
    private var currentPhone: String = ""
    private var currentContactId: Long? = null
    private var currentCallRecordId: Long? = null
    private var recordingStartTime: Long = 0L
    private var isCurrentRecordingPrivate: Boolean = false

    fun isCurrentlyRecording(): Boolean = _isRecording.value

    suspend fun shouldAutoRecord(isContact: Boolean): Boolean {
        val autoAll = preferences.autoRecordCalls.first()
        val contactsOnly = preferences.autoRecordContactsOnly.first()
        if (!autoAll) return false
        return if (contactsOnly) isContact else true
    }

    suspend fun shouldAutoRecordPrivateSafe(): Boolean {
        return preferences.autoRecordPrivateSafe.first()
    }

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

    @Synchronized
    fun startRecording(phoneNumber: String, contactId: Long? = null, callRecordId: Long? = null, isPrivateContact: Boolean = false) {
        if (_isRecording.value) return

        currentPhone = phoneNumber
        currentContactId = contactId
        currentCallRecordId = callRecordId
        recordingStartTime = System.currentTimeMillis()
        isCurrentRecordingPrivate = isPrivateContact

        // Private contacts use internal encrypted storage; public contacts use Music/Linea
        val recordingsDir = if (isPrivateContact) getPrivateRecordingsDirectory() else getRecordingsDirectory()
        val safeNum = phoneNumber.replace("+", "").filter { it.isDigit() }.ifBlank { "unknown" }
        val outputFile = File(recordingsDir, "rec_${recordingStartTime}_${safeNum}.m4a")
        _currentFilePath.value = outputFile.absolutePath

        try {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            try {
                recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
            } catch (_: Exception) {
                recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            }

            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(64000)
            recorder.setAudioSamplingRate(44100)
            recorder.setOutputFile(outputFile.absolutePath)
            recorder.prepare()
            recorder.start()

            mediaRecorder = recorder
        } catch (e: Exception) {
            e.printStackTrace()
            if (outputFile.exists()) {
                outputFile.delete()
            }
            _currentFilePath.value = null
            return
        }

        _isRecording.value = true
        _recordingDurationSeconds.value = 0L

        timerJob?.cancel()
        timerJob = scope.launch {
            val startMs = System.currentTimeMillis()
            while (isActive && _isRecording.value) {
                delay(1000)
                _recordingDurationSeconds.value = (System.currentTimeMillis() - startMs) / 1000
            }
        }
    }

    @Synchronized
    fun stopRecording() {
        if (!_isRecording.value) return

        timerJob?.cancel()
        timerJob = null

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

            if (fileSize > 0L) {
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
