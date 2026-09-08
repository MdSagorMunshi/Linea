package com.ryanshelby.linea.ui.screens.voicemail

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.telecom.TelecomManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryanshelby.linea.data.local.dao.ContactDao
import com.ryanshelby.linea.data.local.dao.VoicemailDao
import com.ryanshelby.linea.data.local.entities.VoicemailEntity
import com.ryanshelby.linea.data.preferences.LineaPreferences
import com.ryanshelby.linea.telecom.CallManager
import com.ryanshelby.linea.ui.screens.history.HistoryGrouper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class VoicemailUiItem(
    val entity: VoicemailEntity,
    val callerName: String?,
    val formattedDate: String,
    val formattedDuration: String,
    val isPlaying: Boolean,
    val playbackProgress: Float
)

@HiltViewModel
class VoicemailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val voicemailDao: VoicemailDao,
    private val contactDao: ContactDao,
    private val callManager: CallManager,
    private val preferences: LineaPreferences
) : ViewModel() {

    private val _playingId = MutableStateFlow<Long?>(null)
    val playingId: StateFlow<Long?> = _playingId.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    val unreadCount: StateFlow<Int> = voicemailDao.getUnreadVoicemailCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val voicemails: StateFlow<List<VoicemailUiItem>> = combine(
        voicemailDao.getAllVoicemails(),
        contactDao.getAllContacts(),
        _playingId,
        _isPlaying,
        _playbackProgress
    ) { list, contacts, currentId, playing, progress ->
        val contactMap = contacts.associateBy { it.displayName }
        val dateFormat = SimpleDateFormat("MMM d • h:mm a", Locale.getDefault())

        list.map { vm ->
            val contact = contactMap[vm.sender]
            val isThisPlaying = currentId == vm.id && playing
            val thisProgress = if (currentId == vm.id) progress else 0f

            VoicemailUiItem(
                entity = vm,
                callerName = contact?.displayName,
                formattedDate = dateFormat.format(Date(vm.timestamp)),
                formattedDuration = HistoryGrouper.formatDuration(vm.duration),
                isPlaying = isThisPlaying,
                playbackProgress = thisProgress
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        seedSampleVoicemailsIfEmpty()
    }

    private fun seedSampleVoicemailsIfEmpty() {
        viewModelScope.launch {
            val existing = voicemailDao.getAllVoicemails().first()
            if (existing.isEmpty()) {
                val now = System.currentTimeMillis()
                voicemailDao.insertVoicemail(
                    VoicemailEntity(
                        sender = "+1 (555) 019-2834",
                        timestamp = now - 3600_000 * 3,
                        duration = 42,
                        transcriptionText = "Hi Ryan, this is John from the project team. Just following up on the design system specs. Give me a call back whenever you have a chance.",
                        isRead = false
                    )
                )
                voicemailDao.insertVoicemail(
                    VoicemailEntity(
                        sender = "+1 (555) 014-9988",
                        timestamp = now - 3600_000 * 26,
                        duration = 18,
                        transcriptionText = "Hey there, confirming our scheduled hardware testing tomorrow at 10 AM. Talk soon!",
                        isRead = true
                    )
                )
            }
        }
    }

    fun playOrPause(voicemail: VoicemailEntity) {
        if (_playingId.value == voicemail.id) {
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
            startPlayback(voicemail)
        }
        if (!voicemail.isRead) {
            markAsRead(voicemail.id)
        }
    }

    private fun startPlayback(voicemail: VoicemailEntity) {
        val audioFile = voicemail.audioFilePath?.let { File(it) }
        if (audioFile != null && audioFile.exists()) {
            try {
                val player = MediaPlayer().apply {
                    setDataSource(context, Uri.fromFile(audioFile))
                    prepare()
                    setOnCompletionListener {
                        _isPlaying.value = false
                        _playbackProgress.value = 0f
                        progressJob?.cancel()
                    }
                    start()
                }
                mediaPlayer = player
                _playingId.value = voicemail.id
                _isPlaying.value = true
                startProgressTracker()
                return
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Simulate playback smoothly for demo / synthesized visual voicemails
        _playingId.value = voicemail.id
        _isPlaying.value = true
        val durationMs = (voicemail.duration * 1000).coerceAtLeast(3000)
        startSimulatedPlayback(durationMs)
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive && _isPlaying.value) {
                mediaPlayer?.let { player ->
                    try {
                        if (player.isPlaying) {
                            val dur = player.duration.toFloat()
                            if (dur > 0) {
                                _playbackProgress.value = player.currentPosition / dur
                            }
                        }
                    } catch (_: Exception) {}
                }
                delay(100)
            }
        }
    }

    private fun startSimulatedPlayback(durationMs: Long) {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            val start = System.currentTimeMillis()
            while (isActive && _isPlaying.value) {
                val elapsed = System.currentTimeMillis() - start
                if (elapsed >= durationMs) {
                    _isPlaying.value = false
                    _playbackProgress.value = 0f
                    break
                }
                _playbackProgress.value = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                delay(100)
            }
        }
    }

    fun seekTo(voicemail: VoicemailEntity, progress: Float) {
        if (_playingId.value == voicemail.id) {
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
    }

    fun markAsRead(id: Long) {
        viewModelScope.launch {
            voicemailDao.markAsRead(id)
        }
    }

    fun deleteVoicemail(voicemail: VoicemailEntity) {
        if (_playingId.value == voicemail.id) {
            stopPlayback()
        }
        viewModelScope.launch {
            voicemail.audioFilePath?.let { path ->
                val f = File(path)
                if (f.exists()) f.delete()
            }
            voicemailDao.deleteVoicemailById(voicemail.id)
        }
    }

    fun callBack(phoneNumber: String) {
        callManager.placeCall(phoneNumber)
    }

    fun dialCarrierVoicemail() {
        viewModelScope.launch {
            val number = preferences.voicemailNumber.first().ifBlank { "123" }
            callManager.placeCall(number)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPlayback()
    }
}
