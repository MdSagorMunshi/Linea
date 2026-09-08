package com.ryanshelby.linea.telecom.recorder

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
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

    fun isCurrentlyRecording(): Boolean = _isRecording.value

    suspend fun shouldAutoRecord(isContact: Boolean): Boolean {
        val autoAll = preferences.autoRecordCalls.first()
        val contactsOnly = preferences.autoRecordContactsOnly.first()
        if (!autoAll) return false
        return if (contactsOnly) isContact else true
    }

    @Synchronized
    fun startRecording(phoneNumber: String, contactId: Long? = null, callRecordId: Long? = null) {
        if (_isRecording.value) return

        currentPhone = phoneNumber
        currentContactId = contactId
        currentCallRecordId = callRecordId
        recordingStartTime = System.currentTimeMillis()

        val recordingsDir = File(context.filesDir, "recordings").apply {
            if (!exists()) mkdirs()
        }
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

        _isRecording.value = false
        _recordingDurationSeconds.value = 0L
        _currentFilePath.value = null

        if (filePath != null) {
            val file = File(filePath)
            val fileSize = if (file.exists()) file.length() else 0L

            if (fileSize > 0L) {
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
                            isPinned = false
                        )
                    )
                }
            } else {
                if (file.exists()) file.delete()
            }
        }
    }
}
