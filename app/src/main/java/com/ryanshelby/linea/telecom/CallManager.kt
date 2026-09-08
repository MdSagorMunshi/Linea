package com.ryanshelby.linea.telecom

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.TelecomManager
import com.ryanshelby.linea.data.local.dao.ContactDao
import com.ryanshelby.linea.data.local.entities.CallDirectionType
import com.ryanshelby.linea.data.repository.CallLogRepository
import com.ryanshelby.linea.notifications.CallNotificationManager
import com.ryanshelby.linea.ui.incall.InCallActivity
import com.ryanshelby.linea.telecom.screening.CallScreeningEngine
import com.ryanshelby.linea.telecom.screening.ScreeningDecision
import com.ryanshelby.linea.telecom.recorder.CallAudioRecorder
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
import javax.inject.Inject
import javax.inject.Singleton

enum class LineaCallState {
    IDLE,
    DIALING,
    RINGING,
    ACTIVE,
    HOLDING,
    DISCONNECTED
}

enum class LineaAudioRoute {
    EARPIECE,
    SPEAKER,
    BLUETOOTH,
    WIRED_HEADSET
}

data class ActiveCallInfo(
    val call: Call,
    val phoneNumber: String,
    val displayName: String?,
    val photoUri: String? = null,
    val state: LineaCallState,
    val isIncoming: Boolean,
    val connectTimeMillis: Long = 0,
    val durationSeconds: Long = 0,
    val isMuted: Boolean = false,
    val isHeld: Boolean = false,
    val audioRoute: LineaAudioRoute = LineaAudioRoute.EARPIECE
)

@Singleton
class CallManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val telecomManager: TelecomManager,
    private val callLogRepository: CallLogRepository,
    private val notificationManager: CallNotificationManager,
    private val proximitySensorManager: ProximitySensorManager,
    private val screeningEngine: CallScreeningEngine,
    private val preferences: LineaPreferences,
    private val audioRecorder: CallAudioRecorder,
    private val contactDao: ContactDao
) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val _currentCall = MutableStateFlow<ActiveCallInfo?>(null)
    val currentCall: StateFlow<ActiveCallInfo?> = _currentCall.asStateFlow()

    private val _secondaryCall = MutableStateFlow<ActiveCallInfo?>(null)
    val secondaryCall: StateFlow<ActiveCallInfo?> = _secondaryCall.asStateFlow()

    private val _audioRoute = MutableStateFlow(LineaAudioRoute.EARPIECE)
    val audioRoute: StateFlow<LineaAudioRoute> = _audioRoute.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    val isRecording: StateFlow<Boolean> = audioRecorder.isRecording
    val recordingDurationSeconds: StateFlow<Long> = audioRecorder.recordingDurationSeconds

    private val _durationWarningActive = MutableStateFlow(false)
    val durationWarningActive: StateFlow<Boolean> = _durationWarningActive.asStateFlow()

    private var inCallService: LineaInCallService? = null
    private var timerJob: Job? = null
    private var previousCallState: LineaCallState = LineaCallState.IDLE

    fun registerInCallService(service: LineaInCallService) {
        this.inCallService = service
    }

    fun unregisterInCallService() {
        this.inCallService = null
    }

    fun onCallAdded(call: Call) {
        val number = call.details?.handle?.schemeSpecificPart ?: ""
        val isIncoming = call.state == Call.STATE_RINGING

        if (_currentCall.value == null) {
            if (isIncoming) {
                scope.launch {
                    val decision = screeningEngine.screenCall(
                        phoneNumber = number,
                        isPrivate = number.isBlank(),
                        simSlot = 0
                    )

                    if (decision is ScreeningDecision.Block) {
                        // Automatically reject call without ringing!
                        screeningEngine.recordBlockedCall(decision, number)
                        try {
                            call.reject(Call.REJECT_REASON_DECLINED)
                        } catch (_: Exception) {
                            call.disconnect()
                        }
                        callLogRepository.logCall(
                            phoneNumber = number,
                            formattedNumber = number,
                            callerName = null,
                            photoUri = null,
                            direction = CallDirectionType.BLOCKED,
                            timestamp = System.currentTimeMillis(),
                            durationSeconds = 0,
                            simSlot = 0
                        )
                        return@launch
                    }

                    // Call allowed -> proceed to normal ringing flow
                    updateCallState(call)
                    val intent = Intent(context, InCallActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    context.startActivity(intent)
                }
            } else {
                updateCallState(call)
                val intent = Intent(context, InCallActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                context.startActivity(intent)
            }
        } else {
            // Secondary call (Call Waiting)
            _secondaryCall.value = ActiveCallInfo(
                call = call,
                phoneNumber = number,
                displayName = call.details?.callerDisplayName,
                state = LineaCallState.RINGING,
                isIncoming = true
            )
        }

        call.registerCallback(object : Call.Callback() {
            override fun onStateChanged(c: Call, state: Int) {
                if (_currentCall.value?.call == c) {
                    updateCallState(c)
                } else if (_secondaryCall.value?.call == c) {
                    if (state == Call.STATE_DISCONNECTED) {
                        _secondaryCall.value = null
                    }
                }
            }

            override fun onDetailsChanged(c: Call, details: Call.Details) {
                if (_currentCall.value?.call == c) {
                    updateCallState(c)
                }
            }
        })
    }

    fun onCallRemoved(call: Call) {
        val active = _currentCall.value
        if (active?.call == call) {
            timerJob?.cancel()
            timerJob = null
            proximitySensorManager.release()
            notificationManager.dismissOngoingCallNotification()

            val direction = if (active.isIncoming) {
                if (active.connectTimeMillis > 0) CallDirectionType.INCOMING else CallDirectionType.MISSED
            } else {
                CallDirectionType.OUTGOING
            }

            scope.launch {
                callLogRepository.logCall(
                    phoneNumber = active.phoneNumber,
                    formattedNumber = active.phoneNumber,
                    callerName = active.displayName,
                    photoUri = active.photoUri,
                    direction = direction,
                    timestamp = System.currentTimeMillis(),
                    durationSeconds = active.durationSeconds
                )

                if (direction == CallDirectionType.MISSED) {
                    notificationManager.showMissedCallNotification(active.displayName, active.phoneNumber)
                }
            }

            if (audioRecorder.isCurrentlyRecording()) {
                audioRecorder.stopRecording()
            }

            _currentCall.value = active.copy(state = LineaCallState.DISCONNECTED)
            _currentCall.value = null

            // If secondary call is waiting, promote it
            val secondary = _secondaryCall.value
            if (secondary != null) {
                _currentCall.value = secondary
                _secondaryCall.value = null
            }
        } else if (_secondaryCall.value?.call == call) {
            _secondaryCall.value = null
        }
    }

    fun onAudioStateChanged(audioState: CallAudioState) {
        _isMuted.value = audioState.isMuted
        val route = when (audioState.route) {
            CallAudioState.ROUTE_SPEAKER -> LineaAudioRoute.SPEAKER
            CallAudioState.ROUTE_BLUETOOTH -> LineaAudioRoute.BLUETOOTH
            CallAudioState.ROUTE_WIRED_HEADSET -> LineaAudioRoute.WIRED_HEADSET
            else -> LineaAudioRoute.EARPIECE
        }
        _audioRoute.value = route

        val isSpeaker = route == LineaAudioRoute.SPEAKER
        val isCallActive = _currentCall.value?.state == LineaCallState.ACTIVE
        proximitySensorManager.onCallStateOrAudioChanged(isCallActive, isSpeaker)
    }

    private fun updateCallState(call: Call) {
        val number = call.details?.handle?.schemeSpecificPart ?: ""
        val callerName = call.details?.callerDisplayName
        val isIncoming = call.state == Call.STATE_RINGING

        val state = when (call.state) {
            Call.STATE_DIALING, Call.STATE_CONNECTING -> LineaCallState.DIALING
            Call.STATE_RINGING -> LineaCallState.RINGING
            Call.STATE_ACTIVE -> LineaCallState.ACTIVE
            Call.STATE_HOLDING -> LineaCallState.HOLDING
            Call.STATE_DISCONNECTED -> LineaCallState.DISCONNECTED
            else -> LineaCallState.IDLE
        }

        val connectTime = call.details?.connectTimeMillis ?: 0L
        val duration = if (connectTime > 0) (System.currentTimeMillis() - connectTime) / 1000 else 0L

        _currentCall.value = ActiveCallInfo(
            call = call,
            phoneNumber = number,
            displayName = callerName,
            state = state,
            isIncoming = isIncoming,
            connectTimeMillis = connectTime,
            durationSeconds = duration,
            isHeld = call.state == Call.STATE_HOLDING,
            isMuted = _isMuted.value,
            audioRoute = _audioRoute.value
        )

        val isSpeaker = _audioRoute.value == LineaAudioRoute.SPEAKER
        proximitySensorManager.onCallStateOrAudioChanged(state == LineaCallState.ACTIVE, isSpeaker)

        if (previousCallState != LineaCallState.ACTIVE && state == LineaCallState.ACTIVE) {
            scope.launch {
                if (preferences.callVibrationEnabled.first()) {
                    vibrateFeedback(longArrayOf(0, 80))
                }
                val normalized = number.filter { it.isDigit() }
                val isContact = contactDao.findNumberByNormalized(normalized) != null
                if (audioRecorder.shouldAutoRecord(isContact)) {
                    audioRecorder.startRecording(number)
                }
            }
        } else if (previousCallState == LineaCallState.ACTIVE && state == LineaCallState.DISCONNECTED) {
            scope.launch {
                if (preferences.callVibrationEnabled.first()) {
                    vibrateFeedback(longArrayOf(0, 60, 60, 60))
                }
            }
        }
        previousCallState = state

        if (state == LineaCallState.ACTIVE) {
            startDurationTimer(connectTime)
            notificationManager.showOngoingCallNotification(
                callerName = callerName,
                phoneNumber = number,
                stateText = "In call"
            )
        } else if (state == LineaCallState.DIALING) {
            notificationManager.showOngoingCallNotification(
                callerName = callerName,
                phoneNumber = number,
                stateText = "Dialing..."
            )
        }
    }

    private fun vibrateFeedback(pattern: LongArray) {
        try {
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            }
            vibrator?.let {
                if (it.hasVibrator()) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        it.vibrate(android.os.VibrationEffect.createWaveform(pattern, -1))
                    } else {
                        @Suppress("DEPRECATION")
                        it.vibrate(pattern, -1)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun startDurationTimer(connectTime: Long) {
        if (timerJob != null) return
        val baseTime = if (connectTime > 0) connectTime else System.currentTimeMillis()
        var warningFired = false

        timerJob = scope.launch {
            val warningMinutes = preferences.callDurationWarningMinutes.first()
            while (isActive) {
                val current = _currentCall.value
                if (current != null && current.state == LineaCallState.ACTIVE) {
                    val secs = (System.currentTimeMillis() - baseTime) / 1000
                    _currentCall.value = current.copy(durationSeconds = secs)

                    if (warningMinutes > 0 && secs >= (warningMinutes * 60) && !warningFired) {
                        warningFired = true
                        _durationWarningActive.value = true
                        vibrateFeedback(longArrayOf(0, 150, 100, 150))
                        scope.launch {
                            delay(6000)
                            _durationWarningActive.value = false
                        }
                    }
                }
                delay(1000)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun placeCall(phoneNumber: String, simAccountHandle: android.telecom.PhoneAccountHandle? = null) {
        val uri = Uri.fromParts("tel", phoneNumber, null)
        val extras = Bundle().apply {
            if (simAccountHandle != null) {
                putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, simAccountHandle)
            }
        }
        telecomManager.placeCall(uri, extras)
    }

    fun answerCall() {
        _currentCall.value?.call?.answer(0)
    }

    fun rejectCall(rejectWithMessage: Boolean = false, textMessage: String? = null) {
        _currentCall.value?.call?.reject(rejectWithMessage, textMessage)
    }

    fun silenceRinger() {
        try {
            telecomManager.silenceRinger()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun disconnectCall() {
        _currentCall.value?.call?.disconnect()
    }

    fun holdCall() {
        _currentCall.value?.call?.hold()
    }

    fun unholdCall() {
        _currentCall.value?.call?.unhold()
    }

    fun swapCalls() {
        val active = _currentCall.value
        val secondary = _secondaryCall.value
        if (active != null && secondary != null) {
            active.call.hold()
            secondary.call.unhold()
            _currentCall.value = secondary
            _secondaryCall.value = active
        }
    }

    fun mergeConference() {
        val active = _currentCall.value
        val secondary = _secondaryCall.value
        if (active != null && secondary != null) {
            active.call.conference(secondary.call)
        }
    }

    fun playDtmfTone(digit: Char) {
        _currentCall.value?.call?.playDtmfTone(digit)
    }

    fun stopDtmfTone() {
        _currentCall.value?.call?.stopDtmfTone()
    }

    fun toggleMute() {
        val newMute = !_isMuted.value
        inCallService?.setMuted(newMute)
        _isMuted.value = newMute
    }

    fun toggleSpeaker() {
        val newRoute = if (_audioRoute.value == LineaAudioRoute.SPEAKER) {
            CallAudioState.ROUTE_EARPIECE
        } else {
            CallAudioState.ROUTE_SPEAKER
        }
        inCallService?.setAudioRoute(newRoute)
    }

    fun setBluetoothAudio() {
        inCallService?.setAudioRoute(CallAudioState.ROUTE_BLUETOOTH)
    }

    fun answerWaitingCallAndHoldActive() {
        val secondary = _secondaryCall.value ?: return
        val active = _currentCall.value
        active?.call?.hold()
        secondary.call.answer(0)
        _currentCall.value = secondary.copy(state = LineaCallState.ACTIVE)
        if (active != null) {
            _secondaryCall.value = active.copy(state = LineaCallState.HOLDING)
        } else {
            _secondaryCall.value = null
        }
    }

    fun answerWaitingCallAndEndActive() {
        val secondary = _secondaryCall.value ?: return
        val active = _currentCall.value
        active?.call?.disconnect()
        secondary.call.answer(0)
        _currentCall.value = secondary.copy(state = LineaCallState.ACTIVE)
        _secondaryCall.value = null
    }

    fun rejectWaitingCall() {
        val secondary = _secondaryCall.value ?: return
        try {
            secondary.call.reject(Call.REJECT_REASON_DECLINED)
        } catch (_: Exception) {
            secondary.call.disconnect()
        }
        _secondaryCall.value = null
    }

    fun toggleRecording() {
        if (audioRecorder.isCurrentlyRecording()) {
            audioRecorder.stopRecording()
        } else {
            val phone = _currentCall.value?.phoneNumber ?: return
            audioRecorder.startRecording(phone)
        }
    }
}
