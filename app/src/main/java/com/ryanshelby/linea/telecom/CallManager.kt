package com.ryanshelby.linea.telecom

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.TelecomManager
import android.widget.Toast
import com.ryanshelby.linea.data.local.dao.ContactDao
import com.ryanshelby.linea.data.local.entities.CallDirectionType
import com.ryanshelby.linea.data.repository.CallLogRepository
import com.ryanshelby.linea.notifications.CallNotificationManager
import com.ryanshelby.linea.ui.incall.InCallActivity
import com.ryanshelby.linea.telecom.screening.CallScreeningEngine
import com.ryanshelby.linea.telecom.screening.ScreeningDecision
import com.ryanshelby.linea.telecom.gestures.CallGestureManager
import com.ryanshelby.linea.telecom.recorder.CallAudioLevelMonitor
import com.ryanshelby.linea.telecom.recorder.CallAudioRecorder
import com.ryanshelby.linea.data.preferences.LineaPreferences
import com.ryanshelby.linea.ui.components.FloatingCallOverlayManager
import com.ryanshelby.linea.permissions.PermissionCoordinator
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
import kotlinx.coroutines.withContext
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
    private val audioLevelMonitor: CallAudioLevelMonitor,
    private val contactDao: ContactDao,
    private val contactLookupHelper: ContactLookupHelper,
    private val callRingtoneManager: CallRingtoneManager,
    private val callUiModeDecider: CallUiModeDecider,
    private val floatingOverlayManager: FloatingCallOverlayManager,
    private val callGestureManager: CallGestureManager,
    private val permissionCoordinator: PermissionCoordinator
) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val audioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

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
    val recordingUnavailableReason: StateFlow<String?> = audioRecorder.recordingUnavailableReason
    val audioLevelHistory: StateFlow<FloatArray> = audioLevelMonitor.audioLevelHistory

    private val _durationWarningActive = MutableStateFlow(false)
    val durationWarningActive: StateFlow<Boolean> = _durationWarningActive.asStateFlow()

    private val _incomingFloatingCall = MutableStateFlow<ActiveCallInfo?>(null)
    val incomingFloatingCall: StateFlow<ActiveCallInfo?> = _incomingFloatingCall.asStateFlow()

    fun dismissFloatingCall() {
        _incomingFloatingCall.value = null
        floatingOverlayManager.dismiss()
        notificationManager.dismissIncomingCallHeadsUpNotification()
    }

    private var inCallService: LineaInCallService? = null
    private var timerJob: Job? = null
    private var previousCallState: LineaCallState = LineaCallState.IDLE
    private var userSelectedSpeaker: Boolean = false
    private var requestedOutgoingAccount: android.telecom.PhoneAccountHandle? = null

    private fun verifySelectedPhoneAccount(call: Call) {
        val expected = requestedOutgoingAccount ?: return
        val actual = call.details?.accountHandle ?: return
        if (actual != expected) {
            // Never continue a call that Telecom assigned to the device default rather than the
            // explicit SIM selected in Linea.
            try {
                call.disconnect()
            } catch (_: Exception) {
            }
        } else {
            requestedOutgoingAccount = null
        }
    }

    fun registerInCallService(service: LineaInCallService) {
        this.inCallService = service
    }

    fun unregisterInCallService() {
        this.inCallService = null
    }

    fun onCallAdded(call: Call) {
        val number = call.details?.handle?.schemeSpecificPart ?: ""
        val isIncoming = call.state == Call.STATE_RINGING
        if (!isIncoming) verifySelectedPhoneAccount(call)

        call.registerCallback(object : Call.Callback() {
            override fun onStateChanged(c: Call, state: Int) {
                if (_currentCall.value?.call == c) {
                    updateCallState(c)
                } else if (_secondaryCall.value?.call == c) {
                    if (state == Call.STATE_DISCONNECTED) {
                        _secondaryCall.value = null
                        refreshOngoingCallNotification()
                    } else {
                        val sec = _secondaryCall.value
                        if (sec != null) {
                            val newSecState = when (state) {
                                Call.STATE_DIALING, Call.STATE_CONNECTING -> LineaCallState.DIALING
                                Call.STATE_RINGING -> LineaCallState.RINGING
                                Call.STATE_ACTIVE -> LineaCallState.ACTIVE
                                Call.STATE_HOLDING -> LineaCallState.HOLDING
                                Call.STATE_DISCONNECTED -> LineaCallState.DISCONNECTED
                                else -> LineaCallState.IDLE
                            }
                            _secondaryCall.value = sec.copy(state = newSecState)
                            refreshOngoingCallNotification()
                        }
                    }
                }
            }

            override fun onDetailsChanged(c: Call, details: Call.Details) {
                verifySelectedPhoneAccount(c)
                if (_currentCall.value?.call == c) {
                    updateCallState(c)
                }
            }
        })

        val current = _currentCall.value
        val isCurrentDisconnectedOrNull = current == null || current.state == LineaCallState.DISCONNECTED
        val isDuplicateOutgoing = !isIncoming && current != null && current.state == LineaCallState.DIALING && current.call != call

        if (isCurrentDisconnectedOrNull || isDuplicateOutgoing) {
            if (isDuplicateOutgoing) {
                try {
                    current?.call?.disconnect()
                } catch (_: Exception) {}
            }

            userSelectedSpeaker = false
            _audioRoute.value = LineaAudioRoute.EARPIECE
            _isMuted.value = false
            inCallService?.setAudioRoute(CallAudioState.ROUTE_EARPIECE)
            try {
                audioManager.isSpeakerphoneOn = false
                audioManager.isMicrophoneMute = false
            } catch (_: Exception) {}

            val initialName = call.details?.callerDisplayName?.ifBlank { number } ?: number
            val initialCallState = when (call.state) {
                Call.STATE_DIALING, Call.STATE_CONNECTING -> LineaCallState.DIALING
                Call.STATE_RINGING -> LineaCallState.RINGING
                Call.STATE_ACTIVE -> LineaCallState.ACTIVE
                Call.STATE_HOLDING -> LineaCallState.HOLDING
                Call.STATE_DISCONNECTED -> LineaCallState.DISCONNECTED
                else -> LineaCallState.DIALING
            }

            // Immediately and synchronously register currentCall so UI and state never drop
            _currentCall.value = ActiveCallInfo(
                call = call,
                phoneNumber = number,
                displayName = initialName,
                photoUri = null,
                state = initialCallState,
                isIncoming = isIncoming,
                connectTimeMillis = call.details?.connectTimeMillis ?: 0L,
                durationSeconds = 0L,
                isHeld = call.state == Call.STATE_HOLDING,
                isMuted = _isMuted.value,
                audioRoute = _audioRoute.value
            )

            if (!isIncoming) {
                // 1. Immediately launch InCallActivity
                val intent = Intent(context, InCallActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                }
                context.startActivity(intent)

                // 2. Immediately show notification in notification bar
                refreshOngoingCallNotification()

                // 3. Query contact info asynchronously to update name/photo when resolved
                scope.launch {
                    val contactLookup = contactLookupHelper.lookupContact(number)
                    val resolvedName = contactLookup.displayName ?: call.details?.callerDisplayName ?: initialName
                    val resolvedPhoto = contactLookup.photoUri
                    updateCallState(call, initialName = resolvedName, initialPhoto = resolvedPhoto)
                    refreshOngoingCallNotification()
                }
            } else {
                scope.launch {
                    val contactLookup = contactLookupHelper.lookupContact(number)
                    val resolvedName = contactLookup.displayName ?: call.details?.callerDisplayName ?: initialName
                    val resolvedPhoto = contactLookup.photoUri

                    val decision = screeningEngine.screenCall(
                        phoneNumber = number,
                        isPrivate = number.isBlank(),
                        simSlot = 0
                    )

                    if (decision is ScreeningDecision.Block) {
                        screeningEngine.recordBlockedCall(decision, number)
                        try {
                            call.reject(Call.REJECT_REASON_DECLINED)
                        } catch (_: Exception) {
                            call.disconnect()
                        }
                        callLogRepository.logCall(
                            phoneNumber = number,
                            formattedNumber = number,
                            callerName = resolvedName,
                            photoUri = resolvedPhoto,
                            direction = CallDirectionType.BLOCKED,
                            timestamp = System.currentTimeMillis(),
                            durationSeconds = 0,
                            simSlot = 0
                        )
                        _currentCall.value = null
                        return@launch
                    }

                    // Call allowed -> update state with resolved contact details
                    updateCallState(call, initialName = resolvedName, initialPhoto = resolvedPhoto)

                    // Play ringtone and vibrate (respects ringerMode normal/vibrate/silent)
                    callRingtoneManager.startRinging(number, contactLookup.customRingtoneUri)
                    callGestureManager.startListening {
                        silenceRinger()
                    }

                    // Decide between Full Screen vs Heads-Up / Mini Call Float
                    val isDimEnabled = preferences.dontInterruptMe.first()
                    val canDrawOverlay = floatingOverlayManager.canDrawOverlay()
                    val useFloatingOverlay = isDimEnabled && canDrawOverlay

                    val uiMode = callUiModeDecider.decideUiMode()
                    if (uiMode == CallUiMode.MINI_FLOAT && useFloatingOverlay) {
                        val callInfo = _currentCall.value
                        if (callInfo != null) {
                            _incomingFloatingCall.value = callInfo
                            floatingOverlayManager.show(callInfo)
                        }
                        notificationManager.dismissIncomingCallHeadsUpNotification()
                    } else if (uiMode == CallUiMode.MINI_FLOAT) {
                        floatingOverlayManager.dismiss()
                        _incomingFloatingCall.value = null
                        notificationManager.showIncomingCallHeadsUpNotification(
                            callerName = resolvedName,
                            phoneNumber = number,
                            photoUri = resolvedPhoto
                        )
                    } else {
                        floatingOverlayManager.dismiss()
                        _incomingFloatingCall.value = null
                        notificationManager.showIncomingCallHeadsUpNotification(
                            callerName = resolvedName,
                            phoneNumber = number,
                            photoUri = resolvedPhoto
                        )
                        val intent = Intent(context, InCallActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        }
                        context.startActivity(intent)
                    }
                }
            }
        } else {
            // Secondary call (Call Waiting or Outgoing Add Call)
            val secState = when (call.state) {
                Call.STATE_DIALING, Call.STATE_CONNECTING -> LineaCallState.DIALING
                Call.STATE_RINGING -> LineaCallState.RINGING
                Call.STATE_ACTIVE -> LineaCallState.ACTIVE
                Call.STATE_HOLDING -> LineaCallState.HOLDING
                else -> LineaCallState.IDLE
            }
            val initialSecName = call.details?.callerDisplayName?.ifBlank { number } ?: number
            _secondaryCall.value = ActiveCallInfo(
                call = call,
                phoneNumber = number,
                displayName = initialSecName,
                photoUri = null,
                state = secState,
                isIncoming = isIncoming
            )
            refreshOngoingCallNotification()

            if (!isIncoming) {
                val intent = Intent(context, InCallActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                }
                context.startActivity(intent)
            }

            scope.launch {
                val contactLookup = contactLookupHelper.lookupContact(number)
                val resolvedName = contactLookup.displayName ?: call.details?.callerDisplayName ?: initialSecName
                _secondaryCall.value = _secondaryCall.value?.copy(
                    displayName = resolvedName,
                    photoUri = contactLookup.photoUri
                )
                refreshOngoingCallNotification()
            }
        }
    }

    fun onCallRemoved(call: Call) {
        callRingtoneManager.stopRinging()
        callGestureManager.stopListening()
        dismissFloatingCall()

        val active = _currentCall.value
        val secondary = _secondaryCall.value

        if (active?.call == call) {
            timerJob?.cancel()
            timerJob = null
            proximitySensorManager.release()

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

            audioRecorder.stopCapture()
            wasSpeakerForcedByRecording = false

            // If secondary call is waiting or active, promote it
            if (secondary != null) {
                _currentCall.value = secondary
                _secondaryCall.value = null
                refreshOngoingCallNotification()
                val intent = Intent(context, InCallActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                }
                context.startActivity(intent)
            } else {
                notificationManager.dismissOngoingCallNotification()
                _currentCall.value = active.copy(state = LineaCallState.DISCONNECTED)
                _currentCall.value = null

                userSelectedSpeaker = false
                _audioRoute.value = LineaAudioRoute.EARPIECE
                try {
                    audioManager.isSpeakerphoneOn = false
                    audioManager.isMicrophoneMute = false
                } catch (_: Exception) {}
            }
        } else if (secondary?.call == call) {
            _secondaryCall.value = null
            refreshOngoingCallNotification()
        }
    }

    fun onAudioStateChanged(audioState: CallAudioState) {
        _isMuted.value = audioState.isMuted
        val route = when {
            audioState.route == CallAudioState.ROUTE_BLUETOOTH -> LineaAudioRoute.BLUETOOTH
            audioState.route == CallAudioState.ROUTE_WIRED_HEADSET -> LineaAudioRoute.WIRED_HEADSET
            audioState.route == CallAudioState.ROUTE_SPEAKER -> {
                if (userSelectedSpeaker) {
                    LineaAudioRoute.SPEAKER
                } else {
                    inCallService?.setAudioRoute(CallAudioState.ROUTE_EARPIECE)
                    try {
                        audioManager.isSpeakerphoneOn = false
                    } catch (_: Exception) {}
                    LineaAudioRoute.EARPIECE
                }
            }
            else -> LineaAudioRoute.EARPIECE
        }
        _audioRoute.value = route
        _currentCall.value = _currentCall.value?.copy(isMuted = audioState.isMuted, audioRoute = route)
        _secondaryCall.value = _secondaryCall.value?.copy(isMuted = audioState.isMuted, audioRoute = route)

        val isSpeaker = route == LineaAudioRoute.SPEAKER
        val isCallActive = _currentCall.value?.state == LineaCallState.ACTIVE
        proximitySensorManager.onCallStateOrAudioChanged(isCallActive, isSpeaker)
        refreshOngoingCallNotification()
    }

    private fun updateCallState(
        call: Call,
        initialName: String? = null,
        initialPhoto: String? = null
    ) {
        val number = call.details?.handle?.schemeSpecificPart ?: ""
        val callerName = initialName ?: _currentCall.value?.displayName ?: call.details?.callerDisplayName
        val photoUri = initialPhoto ?: _currentCall.value?.photoUri
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

        val updatedInfo = ActiveCallInfo(
            call = call,
            phoneNumber = number,
            displayName = callerName,
            photoUri = photoUri,
            state = state,
            isIncoming = isIncoming,
            connectTimeMillis = connectTime,
            durationSeconds = duration,
            isHeld = call.state == Call.STATE_HOLDING,
            isMuted = _isMuted.value,
            audioRoute = _audioRoute.value
        )
        _currentCall.value = updatedInfo
        if (state == LineaCallState.RINGING && _incomingFloatingCall.value != null) {
            floatingOverlayManager.update(updatedInfo)
        }

        val isSpeaker = _audioRoute.value == LineaAudioRoute.SPEAKER
        proximitySensorManager.onCallStateOrAudioChanged(state == LineaCallState.ACTIVE, isSpeaker)

        if (state == LineaCallState.ACTIVE) {
            callRingtoneManager.stopRinging()
            dismissFloatingCall()
            scope.launch {
                audioRecorder.startCapture()
            }
        }

        if (previousCallState != LineaCallState.ACTIVE && state == LineaCallState.ACTIVE) {
            scope.launch {
                if (preferences.callVibrationEnabled.first()) {
                    vibrateFeedback(longArrayOf(0, 80))
                }
                val normalized = number.filter { it.isDigit() }
                val matchedNumber = contactDao.findNumberByNormalized(normalized)
                    ?: if (normalized.length > 7) contactDao.findNumberByNormalized(normalized.takeLast(7)) else null
                val contact = if (matchedNumber != null) contactDao.getContactById(matchedNumber.contactId) else null
                val isPrivate = contact?.isPrivate == true

                val shouldRecord = if (isPrivate) {
                    audioRecorder.shouldAutoRecordPrivateSafe()
                } else {
                    audioRecorder.shouldAutoRecord(isContact = matchedNumber != null)
                }

                if (shouldRecord) {
                    if (_audioRoute.value != LineaAudioRoute.SPEAKER) {
                        wasSpeakerForcedByRecording = true
                        setSpeakerphone(true)
                    }
                    audioRecorder.startRecording(
                        phoneNumber = number,
                        contactId = contact?.id,
                        isPrivateContact = isPrivate
                    )
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
                stateText = "In call",
                connectTimeMillis = connectTime,
                isMuted = _isMuted.value,
                isSpeakerOn = _audioRoute.value == LineaAudioRoute.SPEAKER,
                photoUri = photoUri
            )
        } else if (state == LineaCallState.DIALING) {
            notificationManager.showOngoingCallNotification(
                callerName = callerName,
                phoneNumber = number,
                stateText = "Dialing...",
                connectTimeMillis = 0L,
                isMuted = _isMuted.value,
                isSpeakerOn = _audioRoute.value == LineaAudioRoute.SPEAKER,
                photoUri = photoUri
            )
        } else if (state == LineaCallState.HOLDING) {
            notificationManager.showOngoingCallNotification(
                callerName = callerName,
                phoneNumber = number,
                stateText = "On hold",
                connectTimeMillis = 0L,
                isMuted = _isMuted.value,
                isSpeakerOn = _audioRoute.value == LineaAudioRoute.SPEAKER,
                photoUri = photoUri
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

    private var lastPlaceCallTimestamp: Long = 0L

    @SuppressLint("MissingPermission")
    fun placeCall(phoneNumber: String, simAccountHandle: android.telecom.PhoneAccountHandle? = null) {
        permissionCoordinator.runWhenOutgoingCallPermitted {
            placeCallWhenPermitted(phoneNumber, simAccountHandle)
        }
    }

    @SuppressLint("MissingPermission")
    private fun placeCallWhenPermitted(phoneNumber: String, simAccountHandle: android.telecom.PhoneAccountHandle?) {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastPlaceCallTimestamp < 1200L) {
            return
        }
        lastPlaceCallTimestamp = now

        val uri = Uri.fromParts("tel", phoneNumber, null)
        requestedOutgoingAccount = simAccountHandle
        val extras = Bundle().apply {
            if (simAccountHandle != null) {
                putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, simAccountHandle)
            }
        }
        telecomManager.placeCall(uri, extras)
    }

    fun answerCall() {
        callRingtoneManager.stopRinging()
        callGestureManager.stopListening()
        dismissFloatingCall()
        _currentCall.value?.call?.answer(0)
    }

    fun rejectCall(rejectWithMessage: Boolean = false, textMessage: String? = null) {
        callRingtoneManager.stopRinging()
        callGestureManager.stopListening()
        dismissFloatingCall()
        _currentCall.value?.call?.reject(rejectWithMessage, textMessage)
    }

    fun silenceRinger() {
        callRingtoneManager.silence()
        callGestureManager.stopListening()
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
        _currentCall.value = _currentCall.value?.copy(state = LineaCallState.HOLDING, isHeld = true)
        refreshOngoingCallNotification()
    }

    fun unholdCall() {
        _currentCall.value?.call?.unhold()
        _currentCall.value = _currentCall.value?.copy(state = LineaCallState.ACTIVE, isHeld = false)
        refreshOngoingCallNotification()
    }

    fun swapCalls() {
        val active = _currentCall.value
        val secondary = _secondaryCall.value
        if (active != null && secondary != null) {
            active.call.hold()
            secondary.call.unhold()
            _currentCall.value = secondary
            _secondaryCall.value = active
            refreshOngoingCallNotification()
        }
    }

    fun mergeConference() {
        val active = _currentCall.value
        val secondary = _secondaryCall.value
        if (active != null && secondary != null) {
            active.call.conference(secondary.call)
            refreshOngoingCallNotification()
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
        _isMuted.value = newMute
        inCallService?.setMuted(newMute)
        try {
            audioManager.isMicrophoneMute = newMute
        } catch (_: Exception) {}
        _currentCall.value = _currentCall.value?.copy(isMuted = newMute)
        _secondaryCall.value = _secondaryCall.value?.copy(isMuted = newMute)
        refreshOngoingCallNotification()
    }

    private var wasSpeakerForcedByRecording = false

    fun setSpeakerphone(enable: Boolean) {
        val newRoute = if (enable) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE
        val routeEnum = if (enable) LineaAudioRoute.SPEAKER else LineaAudioRoute.EARPIECE
        _audioRoute.value = routeEnum
        inCallService?.setAudioRoute(newRoute)
        try {
            audioManager.isSpeakerphoneOn = enable
        } catch (_: Exception) {}
        _currentCall.value = _currentCall.value?.copy(audioRoute = routeEnum)
        _secondaryCall.value = _secondaryCall.value?.copy(audioRoute = routeEnum)
        val isCallActive = _currentCall.value?.state == LineaCallState.ACTIVE
        proximitySensorManager.onCallStateOrAudioChanged(isCallActive, enable)
        refreshOngoingCallNotification()
    }

    fun toggleSpeaker() {
        val willBeSpeaker = _audioRoute.value != LineaAudioRoute.SPEAKER
        userSelectedSpeaker = willBeSpeaker
        if (!willBeSpeaker) {
            wasSpeakerForcedByRecording = false
        }
        setSpeakerphone(willBeSpeaker)
    }

    fun canMergeConference(): Boolean {
        val current = _currentCall.value ?: return false
        val secondary = _secondaryCall.value ?: return false
        val details = current.call.details
        val secDetails = secondary.call.details
        val caps = details?.callCapabilities ?: 0
        return details?.can(Call.Details.CAPABILITY_MERGE_CONFERENCE) == true ||
                secDetails?.can(Call.Details.CAPABILITY_MERGE_CONFERENCE) == true ||
                (caps and Call.Details.CAPABILITY_MERGE_CONFERENCE) != 0 ||
                (secondary.state == LineaCallState.ACTIVE || secondary.state == LineaCallState.HOLDING)
    }

    fun refreshOngoingCallNotification() {
        val current = _currentCall.value ?: return
        if (current.state == LineaCallState.ACTIVE || current.state == LineaCallState.DIALING || current.state == LineaCallState.HOLDING) {
            val stateText = when (current.state) {
                LineaCallState.ACTIVE -> "In call"
                LineaCallState.DIALING -> "Dialing..."
                LineaCallState.HOLDING -> "On hold"
                else -> "Active call"
            }
            val secondary = _secondaryCall.value
            val hasSecondary = secondary != null
            val secondaryName = secondary?.displayName?.ifBlank { secondary.phoneNumber } ?: secondary?.phoneNumber
            val canMerge = canMergeConference()

            notificationManager.showOngoingCallNotification(
                callerName = current.displayName,
                phoneNumber = current.phoneNumber,
                stateText = stateText,
                connectTimeMillis = if (current.state == LineaCallState.ACTIVE) current.connectTimeMillis else 0L,
                isMuted = _isMuted.value,
                isSpeakerOn = _audioRoute.value == LineaAudioRoute.SPEAKER,
                photoUri = current.photoUri,
                hasSecondaryCall = hasSecondary,
                secondaryCallerName = secondaryName,
                canMerge = canMerge,
                isHeld = current.state == LineaCallState.HOLDING || current.isHeld
            )
        }
    }

    fun setBluetoothAudio() {
        _audioRoute.value = LineaAudioRoute.BLUETOOTH
        inCallService?.setAudioRoute(CallAudioState.ROUTE_BLUETOOTH)
        _currentCall.value = _currentCall.value?.copy(audioRoute = LineaAudioRoute.BLUETOOTH)
        _secondaryCall.value = _secondaryCall.value?.copy(audioRoute = LineaAudioRoute.BLUETOOTH)
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
            if (wasSpeakerForcedByRecording && !userSelectedSpeaker) {
                wasSpeakerForcedByRecording = false
                setSpeakerphone(false)
            }
        } else {
            if (!com.ryanshelby.linea.telecom.recorder.LineaCallAudioService.isServiceEnabled(context)) {
                scope.launch(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Call recording requires Linea Call Audio Service. You must enable it from Linea Settings before calls.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return
            }
            val phone = _currentCall.value?.phoneNumber ?: return
            if (_audioRoute.value != LineaAudioRoute.SPEAKER) {
                wasSpeakerForcedByRecording = true
                setSpeakerphone(true)
            }
            scope.launch {
                val normalized = phone.filter { it.isDigit() }
                val matchedNumber = contactDao.findNumberByNormalized(normalized)
                    ?: if (normalized.length > 7) contactDao.findNumberByNormalized(normalized.takeLast(7)) else null
                val contact = if (matchedNumber != null) contactDao.getContactById(matchedNumber.contactId) else null
                val isPrivate = contact?.isPrivate == true

                val success = audioRecorder.startRecording(
                    phoneNumber = phone,
                    contactId = contact?.id,
                    isPrivateContact = isPrivate
                )
                if (success) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "Call recording started",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    fun onMicrophonePermissionGranted() {
        if (_currentCall.value?.state == LineaCallState.ACTIVE) {
            scope.launch {
                audioRecorder.startCapture()
            }
        }
    }
}
