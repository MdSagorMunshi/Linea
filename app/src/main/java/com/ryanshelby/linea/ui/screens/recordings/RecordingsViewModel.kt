package com.ryanshelby.linea.ui.screens.recordings

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryanshelby.linea.data.local.dao.CallRecordingDao
import com.ryanshelby.linea.data.local.dao.ContactDao
import com.ryanshelby.linea.data.local.entities.CallRecordingEntity
import com.ryanshelby.linea.ui.screens.history.HistoryGrouper
import com.ryanshelby.linea.data.preferences.LineaPreferences
import com.ryanshelby.linea.telecom.recorder.CallAudioRecorder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class RecordingUiItem(
    val entity: CallRecordingEntity,
    val callerName: String?,
    val formattedDuration: String,
    val formattedDate: String,
    val formattedSize: String,
    val isPlaying: Boolean,
    val progress: Float
)

@HiltViewModel
class RecordingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recordingDao: CallRecordingDao,
    private val contactDao: ContactDao,
    private val preferences: LineaPreferences,
    private val audioRecorder: CallAudioRecorder
) : ViewModel() {

    private val _playingId = MutableStateFlow<Long?>(null)
    val playingId: StateFlow<Long?> = _playingId.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private var currentDecryptedTempFile: File? = null

    val recordings: StateFlow<List<RecordingUiItem>> = combine(
        recordingDao.getAllRecordings(),
        contactDao.getAllContacts(),
        preferences.privateModeUnlocked,
        _playingId,
        _isPlaying,
        _playbackProgress
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val rawRecordings = args[0] as List<CallRecordingEntity>
        @Suppress("UNCHECKED_CAST")
        val contactsList = args[1] as List<com.ryanshelby.linea.data.local.entities.ContactEntity>
        val isPrivateUnlocked = args[2] as Boolean
        val currentPlayingId = args[3] as Long?
        val isCurrentlyPlaying = args[4] as Boolean
        val progress = args[5] as Float

        val recordingsList = if (isPrivateUnlocked) rawRecordings else rawRecordings.filter { !it.isEncrypted }
        val contactMap = contactsList.associateBy { it.id }
        val dateFormat = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())

        recordingsList.map { recording ->
            val contact = recording.contactId?.let { contactMap[it] }
            val name = contact?.displayName
            val isThisPlaying = currentPlayingId == recording.id && isCurrentlyPlaying
            val thisProgress = if (currentPlayingId == recording.id) progress else 0f

            val durationSec = (recording.durationMs / 1000).coerceAtLeast(1)
            val formattedDur = HistoryGrouper.formatDuration(durationSec)
            val formattedDate = dateFormat.format(Date(recording.timestamp))
            val sizeKb = recording.fileSize / 1024
            val formattedSize = if (sizeKb > 1024) {
                String.format(Locale.US, "%.1f MB", sizeKb / 1024.0)
            } else {
                "$sizeKb KB"
            }

            RecordingUiItem(
                entity = recording,
                callerName = name,
                formattedDuration = formattedDur,
                formattedDate = formattedDate,
                formattedSize = formattedSize,
                isPlaying = isThisPlaying,
                progress = thisProgress
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun playOrPause(recording: CallRecordingEntity) {
        if (_playingId.value == recording.id) {
            if (_isPlaying.value) {
                mediaPlayer?.pause()
                _isPlaying.value = false
            } else {
                mediaPlayer?.start()
                _isPlaying.value = true
                startProgressTracker()
            }
        } else {
            stopPlayback()
            startPlayback(recording)
        }
    }

    private fun startPlayback(recording: CallRecordingEntity) {
        val file = File(recording.filePath)
        if (!file.exists()) {
            stopPlayback()
            return
        }

        try {
            val playbackFile = if (recording.isEncrypted) {
                currentDecryptedTempFile?.delete()
                val temp = audioRecorder.decryptRecordingToTemp(file) ?: file
                currentDecryptedTempFile = temp
                temp
            } else {
                file
            }

            val player = MediaPlayer().apply {
                setDataSource(context, Uri.fromFile(playbackFile))
                prepare()
                setOnCompletionListener {
                    _isPlaying.value = false
                    _playbackProgress.value = 0f
                    progressJob?.cancel()
                }
                start()
            }
            mediaPlayer = player
            _playingId.value = recording.id
            _isPlaying.value = true
            startProgressTracker()
        } catch (e: Exception) {
            e.printStackTrace()
            stopPlayback()
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive && _isPlaying.value) {
                mediaPlayer?.let { player ->
                    try {
                        if (player.isPlaying) {
                            val duration = player.duration.toFloat()
                            if (duration > 0) {
                                _playbackProgress.value = player.currentPosition / duration
                            }
                        }
                    } catch (_: Exception) {}
                }
                delay(100)
            }
        }
    }

    fun seekTo(recording: CallRecordingEntity, progress: Float) {
        if (_playingId.value == recording.id) {
            mediaPlayer?.let { player ->
                try {
                    val targetMs = (player.duration * progress).toInt()
                    player.seekTo(targetMs)
                    _playbackProgress.value = progress
                } catch (_: Exception) {}
            }
        }
    }

    fun stopPlayback() {
        progressJob?.cancel()
        progressJob = null
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        _playingId.value = null
        _isPlaying.value = false
        _playbackProgress.value = 0f
        currentDecryptedTempFile?.delete()
        currentDecryptedTempFile = null
    }

    fun togglePin(recording: CallRecordingEntity) {
        viewModelScope.launch {
            recordingDao.updatePinned(recording.id, !recording.isPinned)
        }
    }

    fun deleteRecording(recording: CallRecordingEntity) {
        if (_playingId.value == recording.id) {
            stopPlayback()
        }
        viewModelScope.launch {
            val file = File(recording.filePath)
            if (file.exists()) file.delete()
            recordingDao.deleteRecordingById(recording.id)
        }
    }

    fun getShareIntent(recording: CallRecordingEntity): Intent? {
        val file = File(recording.filePath)
        if (!file.exists()) return null

        val uri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

        return Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPlayback()
    }
}
