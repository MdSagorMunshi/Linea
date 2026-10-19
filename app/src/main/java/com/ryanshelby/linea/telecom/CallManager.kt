package com.ryanshelby.linea.telecom

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.telecom.DisconnectCause
import android.os.Bundle
import android.os.SystemClock
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import android.widget.Toast
import com.ryanshelby.linea.data.local.dao.ContactDao
import com.ryanshelby.linea.data.local.entities.CallDirectionType
import com.ryanshelby.linea.data.repository.CallLogRepository
import com.ryanshelby.linea.notifications.CallNotificationManager
import com.ryanshelby.linea.ui.incall.InCallActivity
import com.ryanshelby.linea.telecom.screening.CallScreeningEngine
import com.ryanshelby.linea.telecom.screening.ScreeningDecision
import com.ryanshelby.linea.telecom.gestures.CallGestureManager
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
    val call: Call? = null,
    val phoneNumber: String,
    val displayName: String?,
    val photoUri: String? = null,
    val state: LineaCallState,
    val isIncoming: Boolean,
    val connectTimeMillis: Long = 0,
    val durationSeconds: Long = 0,
    val isMuted: Boolean = false,
    val isHeld: Boolean = false,
    val audioRoute: LineaAudioRoute = LineaAudioRoute.EARPIECE,
    val isSimulated: Boolean = false,
    val simSlot: Int? = null,
    val simLabel: String? = null,
    val phoneAccountHandle: android.telecom.PhoneAccountHandle? = null
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
    private val contactDao: ContactDao,
    private val contactLookupHelper: ContactLookupHelper,
    private val callRingtoneManager: CallRingtoneManager,
    private val callUiModeDecider: CallUiModeDecider,
    private val floatingOverlayManager: FloatingCallOverlayManager,
    private val callGestureManager: CallGestureManager,
    private val permissionCoordinator: PermissionCoordinator,
    private val simAccountResolver: SimAccountResolver
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

    private val _durationWarningActive = MutableStateFlow(false)
    val durationWarningActive: StateFlow<Boolean> = _durationWarningActive.asStateFlow()

    private val _incomingFloatingCall = MutableStateFlow<ActiveCallInfo?>(null)
    val incomingFloatingCall: StateFlow<ActiveCallInfo?> = _incomingFloatingCall.asStateFlow()

    private val _isRingerSilenced = MutableStateFlow(false)
    val isRingerSilenced: StateFlow<Boolean> = _isRingerSilenced.asStateFlow()

    private val _postCallSummary = MutableStateFlow<PostCallSummary?>(null)
    val postCallSummary: StateFlow<PostCallSummary?> = _postCallSummary.asStateFlow()

    private var isPostCallHudEnabled: Boolean = false

    init {
        scope.launch {
            preferences.postCallHudEnabled.collect { enabled ->
                isPostCallHudEnabled = enabled
                if (!enabled) {
                    _postCallSummary.value = null
                }
            }
        }
    }

    fun clearPostCallSummary() {
        _postCallSummary.value = null
    }

    // Timestamp when ringing started; volume-change events within a short window are
    // system-generated (audio stream setup) and must be ignored.
    private var ringerStartedTimestamp = 0L
    private val RINGER_GRACE_PERIOD_MS = 600L

    val isRinging: Boolean
        get() = (_currentCall.value?.state == LineaCallState.RINGING && _currentCall.value?.isIncoming == true) ||
                (_secondaryCall.value?.state == LineaCallState.RINGING && _secondaryCall.value?.isIncoming == true) ||
                (inCallService?.calls?.any { it.state == Call.STATE_RINGING } == true)

    val hasActiveOrPendingCall: Boolean
        get() = _currentCall.value != null ||
                _secondaryCall.value != null ||
                (inCallService?.calls?.isNotEmpty() == true)

    private var lastPowerPressTimestamp = 0L
    private val userRejectedCalls = java.util.Collections.synchronizedSet(java.util.HashSet<Call>())

    @Synchronized
    fun onPowerButtonPressed() {
        if (!hasActiveOrPendingCall) return

        val now = SystemClock.elapsedRealtime()
        val delta = now - lastPowerPressTimestamp

        // Debounce if multiple events (e.g. key event + broadcast) fire for the same physical press
        if (lastPowerPressTimestamp > 0L && delta < 120L) {
            android.util.Log.d("CallManager", "onPowerButtonPressed: debounced (delta=${delta}ms)")
            return
        }

        if (lastPowerPressTimestamp > 0L && delta <= 1000L) {
            // Double press detected within 120ms..1000ms: end current call across all states
            android.util.Log.d("CallManager", "onPowerButtonPressed: DOUBLE PRESS detected (delta=${delta}ms) → endCurrentCall()")
            lastPowerPressTimestamp = 0L
            endCurrentCall()
        } else {
            // First press of a potential double-press sequence
            android.util.Log.d("CallManager", "onPowerButtonPressed: FIRST press (isRinging=$isRinging, isSilenced=${_isRingerSilenced.value})")
            lastPowerPressTimestamp = now
            if (isRinging && !_isRingerSilenced.value) {
                silenceRinger()
            }
        }
    }

    fun endCurrentCall() {
        android.util.Log.d("CallManager", "endCurrentCall() called")
        _isRingerSilenced.value = false
        ringerStartedTimestamp = 0L
        callRingtoneManager.stopRinging()
        callGestureManager.stopListening()
        dismissFloatingCall()

        vibrateFeedback(longArrayOf(0, 100))

        // Handle simulated/escape calls (no real Telecom Call object)
        val current = _currentCall.value
        if (current?.isSimulated == true) {
            timerJob?.cancel()
            timerJob = null
            proximitySensorManager.release()
            recordPostCallSummary(current)
            _currentCall.value = current.copy(state = LineaCallState.DISCONNECTED)
            scope.launch {
                delay(300)
                _currentCall.value = null
            }
            return
        }

        val active = current?.call
        if (active != null) {
            try {
                if (isRinging) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        active.reject(Call.REJECT_REASON_DECLINED)
                    } else {
                        active.reject(false, null)
                    }
                } else {
                    active.disconnect()
                }
            } catch (e: Exception) {
                try {
                    active.disconnect()
                } catch (_: Exception) {}
            }
        }

        val secondary = _secondaryCall.value?.call
        if (secondary != null) {
            try {
                if (secondary.state == Call.STATE_RINGING) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        secondary.reject(Call.REJECT_REASON_DECLINED)
                    } else {
                        secondary.reject(false, null)
                    }
                } else {
                    secondary.disconnect()
                }
            } catch (e: Exception) {
                try {
                    secondary.disconnect()
                } catch (_: Exception) {}
            }
        }

        inCallService?.calls?.forEach { c ->
            try {
                if (c.state == Call.STATE_RINGING) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        c.reject(Call.REJECT_REASON_DECLINED)
                    } else {
                        c.reject(false, null)
                    }
                } else {
                    c.disconnect()
                }
            } catch (_: Exception) {}
        }
    }

    private var isHardwareKeyReceiverRegistered = false
    private val hardwareKeyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    // Power button pressed while screen is on during incoming ring
                    val elapsed = SystemClock.elapsedRealtime() - ringerStartedTimestamp
                    if (isRinging && !_isRingerSilenced.value && ringerStartedTimestamp > 0L && elapsed > 1200L) {
                        android.util.Log.d("CallManager", "ACTION_SCREEN_OFF: user pressed power during ring -> silence")
                        silenceRinger()
                    }
                }
                Intent.ACTION_SCREEN_ON -> {
                    // Do NOT treat screen turning on as power button press.
                    // The incoming call itself turns on the screen; silencing here was causing silent rings.
                    android.util.Log.d("CallManager", "ACTION_SCREEN_ON: screen turned on (incoming call active)")
                }
                "android.media.VOLUME_CHANGED_ACTION" -> {
                    // Only silence if user explicitly turned DOWN the ringer volume after the call has started ringing
                    val streamType = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1)
                    val newVol = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", -1)
                    val oldVol = intent.getIntExtra("android.media.EXTRA_PREV_VOLUME_STREAM_VALUE", -1)
                    val elapsed = SystemClock.elapsedRealtime() - ringerStartedTimestamp
                    if (streamType == AudioManager.STREAM_RING && newVol >= 0 && oldVol >= 0 && newVol < oldVol) {
                        if (isRinging && !_isRingerSilenced.value && ringerStartedTimestamp > 0L && elapsed > 1000L) {
                            android.util.Log.d("CallManager", "VOLUME_CHANGED_ACTION: user decreased ring volume -> silence")
                            silenceRinger()
                        }
                    }
                }
            }
        }
    }

    private fun registerHardwareKeyReceiver() {
        if (!isHardwareKeyReceiverRegistered) {
            try {
                val filter = IntentFilter().apply {
                    addAction(Intent.ACTION_SCREEN_OFF)
                    addAction(Intent.ACTION_SCREEN_ON)
                    addAction("android.media.VOLUME_CHANGED_ACTION")
                }
                ContextCompat.registerReceiver(
                    context,
                    hardwareKeyReceiver,
                    filter,
                    ContextCompat.RECEIVER_EXPORTED
                )
                isHardwareKeyReceiverRegistered = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun unregisterHardwareKeyReceiver() {
        if (isHardwareKeyReceiverRegistered) {
            try {
                context.unregisterReceiver(hardwareKeyReceiver)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isHardwareKeyReceiverRegistered = false
                lastPowerPressTimestamp = 0L
            }
        }
    }

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
        val expectedSlot = simAccountResolver.resolveSimSlot(expected)
        val actualSlot = simAccountResolver.resolveSimSlot(actual)
        val matches = (actual == expected) || (
            actual.componentName == expected.componentName &&
            actual.id == expected.id
        ) || (expectedSlot != null && actualSlot != null && expectedSlot == actualSlot)

        if (!matches) {
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
        if (!hasActiveOrPendingCall) {
            unregisterHardwareKeyReceiver()
        }
    }

    fun onCallAdded(call: Call) {
        android.util.Log.d("CallManager", "onCallAdded: state=${call.state}, number=${call.details?.handle?.schemeSpecificPart}")
        registerHardwareKeyReceiver()
        val number = call.details?.handle?.schemeSpecificPart ?: ""
        val isIncoming = call.state == Call.STATE_RINGING
        if (isIncoming) {
            _isRingerSilenced.value = false
            ringerStartedTimestamp = SystemClock.elapsedRealtime()
        }
        if (!isIncoming) verifySelectedPhoneAccount(call)

        call.registerCallback(object : Call.Callback() {
            override fun onStateChanged(c: Call, state: Int) {
                android.util.Log.d("CallManager", "onStateChanged: state=$state, call=${c.details?.handle?.schemeSpecificPart}")
                try {
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
                } catch (e: Exception) {
                    android.util.Log.e("CallManager", "onStateChanged exception", e)
                }
            }

            override fun onDetailsChanged(c: Call, details: Call.Details) {
                try {
                    verifySelectedPhoneAccount(c)
                    if (_currentCall.value?.call == c) {
                        updateCallState(c)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CallManager", "onDetailsChanged exception", e)
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

            // Resolve SIM slot & label via SimAccountResolver
            val activeAccountHandle = if (isIncoming) {
                call.details?.accountHandle
            } else {
                call.details?.accountHandle ?: requestedOutgoingAccount
            }
            val isEmergency = simAccountResolver.isEmergencyCall(call, number)
            val resolvedSlot = if (isEmergency) null else simAccountResolver.resolveSimSlot(activeAccountHandle)
            val simLabel = if (isEmergency) null else simAccountResolver.getSimLabel(resolvedSlot)

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
                audioRoute = _audioRoute.value,
                simSlot = resolvedSlot,
                simLabel = simLabel,
                phoneAccountHandle = activeAccountHandle
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
                // === INCOMING CALL: Start ring & UI IMMEDIATELY, before async work ===
                val simSlot = resolvedSlot ?: 0

                // 1. Start ringing IMMEDIATELY with system default (before contact lookup)
                _isRingerSilenced.value = false
                ringerStartedTimestamp = SystemClock.elapsedRealtime()
                registerHardwareKeyReceiver()
                callRingtoneManager.startRinging(number, null) // system default first
                callGestureManager.startListening {
                    silenceRinger()
                }

                // 2. Show notification and UI IMMEDIATELY
                notificationManager.showIncomingCallHeadsUpNotification(
                    callerName = initialName,
                    phoneNumber = number,
                    photoUri = null
                )
                val uiMode = callUiModeDecider.decideUiMode()
                if (uiMode == CallUiMode.FULL_SCREEN) {
                    val intent = Intent(context, InCallActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        android.util.Log.e("CallManager", "Failed to start InCallActivity", e)
                    }
                }

                // 3. Async: resolve contact, screen, update ringtone/UI if needed
                scope.launch {
                    try {
                        val contactLookup = contactLookupHelper.lookupContact(number)
                        val resolvedName = contactLookup.displayName ?: call.details?.callerDisplayName ?: initialName
                        val resolvedPhoto = contactLookup.photoUri

                        val decision = screeningEngine.screenCall(
                            phoneNumber = number,
                            isPrivate = number.isBlank(),
                            simSlot = simSlot
                        )

                        if (decision is ScreeningDecision.Block) {
                            android.util.Log.d("CallManager", "Screening blocked call from $number: ${decision.reason}")
                            // Stop ringing before rejecting
                            callRingtoneManager.stopRinging()
                            callGestureManager.stopListening()
                            dismissFloatingCall()
                            notificationManager.dismissIncomingCallHeadsUpNotification()

                            screeningEngine.recordBlockedCall(decision, number)
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    call.reject(Call.REJECT_REASON_DECLINED)
                                } else {
                                    call.reject(false, null)
                                }
                            } catch (_: Exception) {
                                try { call.disconnect() } catch (_: Exception) {}
                            }
                            callLogRepository.logCall(
                                phoneNumber = number,
                                formattedNumber = number,
                                callerName = resolvedName,
                                photoUri = resolvedPhoto,
                                direction = CallDirectionType.BLOCKED,
                                timestamp = System.currentTimeMillis(),
                                durationSeconds = 0,
                                simSlot = simSlot
                            )
                            _currentCall.value = null
                            return@launch
                        }

                        // Call allowed -> update state with resolved contact details
                        updateCallState(call, initialName = resolvedName, initialPhoto = resolvedPhoto)

                        // Switch to custom ringtone seamlessly if contact has one
                        if (!contactLookup.customRingtoneUri.isNullOrBlank()) {
                            callRingtoneManager.updateCustomRingtone(contactLookup.customRingtoneUri)
                        }

                        // Update notification with resolved contact info
                        notificationManager.showIncomingCallHeadsUpNotification(
                            callerName = resolvedName,
                            phoneNumber = number,
                            photoUri = resolvedPhoto
                        )

                        // Decide between Full Screen vs Heads-Up / Mini Call Float
                        val isDimEnabled = preferences.dontInterruptMe.first()
                        val canDrawOverlay = floatingOverlayManager.canDrawOverlay()
                        val useFloatingOverlay = isDimEnabled && canDrawOverlay

                        val updatedUiMode = callUiModeDecider.decideUiMode()
                        if (updatedUiMode == CallUiMode.MINI_FLOAT && useFloatingOverlay) {
                            val callInfo = _currentCall.value
                            if (callInfo != null) {
                                _incomingFloatingCall.value = callInfo
                                floatingOverlayManager.show(callInfo)
                            }
                            notificationManager.dismissIncomingCallHeadsUpNotification()
                        } else if (updatedUiMode == CallUiMode.MINI_FLOAT) {
                            floatingOverlayManager.dismiss()
                            _incomingFloatingCall.value = null
                            // Heads-up notification already shown above
                        } else {
                            floatingOverlayManager.dismiss()
                            _incomingFloatingCall.value = null
                            // Full-screen activity already launched above
                        }
                    } catch (e: Exception) {
                        // CRITICAL: Never silently swallow exceptions during incoming call setup.
                        // The call is already ringing and UI is shown from the immediate path above.
                        // Just log the error — the call remains functional.
                        android.util.Log.e("CallManager", "Exception during incoming call async setup", e)
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
            val secHandle = call.details?.accountHandle ?: requestedOutgoingAccount
            val isSecEmergency = simAccountResolver.isEmergencyCall(call, number)
            val secSlot = if (isSecEmergency) null else simAccountResolver.resolveSimSlot(secHandle)
            val secLabel = if (isSecEmergency) null else simAccountResolver.getSimLabel(secSlot)

            _secondaryCall.value = ActiveCallInfo(
                call = call,
                phoneNumber = number,
                displayName = initialSecName,
                photoUri = null,
                state = secState,
                isIncoming = isIncoming,
                simSlot = secSlot,
                simLabel = secLabel,
                phoneAccountHandle = secHandle
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
        android.util.Log.d("CallManager", "onCallRemoved: number=${call.details?.handle?.schemeSpecificPart}, disconnectCause=${call.details?.disconnectCause}")
        val active = _currentCall.value
        val secondary = _secondaryCall.value

        val remainingRinging = (active?.call != call && active?.state == LineaCallState.RINGING) ||
                (secondary?.call != call && secondary?.state == LineaCallState.RINGING)
        if (!remainingRinging) {
            _isRingerSilenced.value = false
            ringerStartedTimestamp = 0L
            callRingtoneManager.stopRinging()
            callGestureManager.stopListening()
            dismissFloatingCall()
        }

        if (active?.call == call) {
            timerJob?.cancel()
            timerJob = null
            proximitySensorManager.release()

            // Determine call direction with proper rejection detection.
            // Use the disconnect cause and user action to distinguish rejected-by-user from missed calls.
            val wasUserRejected = userRejectedCalls.remove(call)
            val disconnectCause = call.details?.disconnectCause
            val disconnectCode = disconnectCause?.code ?: DisconnectCause.UNKNOWN
            val detailsConnectTime = call.details?.connectTimeMillis ?: 0L
            val wasConnected = detailsConnectTime > 0L || active.connectTimeMillis > 0L

            android.util.Log.d("CallManager", "onCallRemoved: isIncoming=${active.isIncoming}, wasConnected=$wasConnected, wasUserRejected=$wasUserRejected, disconnectCode=$disconnectCode")

            val direction = if (active.isIncoming) {
                if (wasConnected) {
                    // Call was answered and connected
                    CallDirectionType.INCOMING
                } else if (wasUserRejected ||
                           disconnectCode == DisconnectCause.REJECTED ||
                           disconnectCode == DisconnectCause.LOCAL) {
                    // User explicitly declined/rejected the call, or local end terminated before connect
                    CallDirectionType.REJECTED
                } else {
                    // Caller hung up before answer, or call timed out — this is a missed call
                    CallDirectionType.MISSED
                }
            } else {
                CallDirectionType.OUTGOING
            }
            android.util.Log.d("CallManager", "onCallRemoved: logged direction=$direction")

            // Resolve SIM slot & handle accurately via SimAccountResolver
            val callHandle = call.details?.accountHandle ?: active.phoneAccountHandle ?: requestedOutgoingAccount
            val isEmergency = simAccountResolver.isEmergencyCall(call, active.phoneNumber)
            val resolvedSlot = if (isEmergency) null else (active.simSlot ?: simAccountResolver.resolveSimSlot(callHandle))
            val finalSimSlot = resolvedSlot ?: 0
            val finalSimDisplayName = if (resolvedSlot != null) (active.simLabel ?: simAccountResolver.getSimLabel(resolvedSlot)) else null

            val callStartTime = call.details?.creationTimeMillis.takeIf { it != null && it > 0L }
                ?: (if (active.connectTimeMillis > 0L) active.connectTimeMillis else (System.currentTimeMillis() - active.durationSeconds * 1000L))

            scope.launch {
                try {
                    callLogRepository.logCall(
                        phoneNumber = active.phoneNumber,
                        formattedNumber = active.phoneNumber,
                        callerName = active.displayName,
                        photoUri = active.photoUri,
                        direction = direction,
                        timestamp = callStartTime,
                        durationSeconds = active.durationSeconds,
                        simSlot = finalSimSlot,
                        simDisplayName = finalSimDisplayName,
                        phoneAccountHandle = callHandle
                    )

                    if (direction == CallDirectionType.MISSED) {
                        notificationManager.showMissedCallNotification(
                            callerName = active.displayName,
                            phoneNumber = active.phoneNumber,
                            simSlot = resolvedSlot ?: -1,
                            timestamp = callStartTime
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CallManager", "Error logging call or showing missed notification", e)
                }
            }

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
                recordPostCallSummary(active)
                notificationManager.dismissOngoingCallNotification()
                _currentCall.value = active.copy(state = LineaCallState.DISCONNECTED)
                _currentCall.value = null

                userSelectedSpeaker = false
                _audioRoute.value = LineaAudioRoute.EARPIECE
                try {
                    audioManager.isSpeakerphoneOn = false
                    audioManager.isMicrophoneMute = false
                } catch (_: Exception) {}

                if (!hasActiveOrPendingCall) {
                    unregisterHardwareKeyReceiver()
                }
            }
        } else if (secondary?.call == call) {
            _secondaryCall.value = null
            refreshOngoingCallNotification()
            if (!hasActiveOrPendingCall) {
                unregisterHardwareKeyReceiver()
            }
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

        val isCallActive = _currentCall.value?.state == LineaCallState.ACTIVE
        proximitySensorManager.onCallStateOrAudioChanged(isCallActive, route)
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
        // BUG FIX: Preserve the original isIncoming flag from onCallAdded().
        // Previously this was recalculated as `call.state == Call.STATE_RINGING`,
        // which becomes false when the call transitions to ACTIVE or DISCONNECTED,
        // causing rejected/missed incoming calls to be logged as OUTGOING.
        val isIncoming = _currentCall.value?.isIncoming ?: (call.state == Call.STATE_RINGING)

        val state = when (call.state) {
            Call.STATE_DIALING, Call.STATE_CONNECTING -> LineaCallState.DIALING
            Call.STATE_RINGING -> LineaCallState.RINGING
            Call.STATE_ACTIVE -> LineaCallState.ACTIVE
            Call.STATE_HOLDING -> LineaCallState.HOLDING
            Call.STATE_DISCONNECTED -> LineaCallState.DISCONNECTED
            else -> LineaCallState.IDLE
        }

        val previousInfo = _currentCall.value
        val connectTime = call.details?.connectTimeMillis ?: 0L
        val duration = if (connectTime > 0) (System.currentTimeMillis() - connectTime) / 1000 else 0L

        val handle = call.details?.accountHandle ?: previousInfo?.phoneAccountHandle ?: requestedOutgoingAccount
        val isEmergency = simAccountResolver.isEmergencyCall(call, number)
        val resolvedSlot = if (isEmergency) null else (previousInfo?.simSlot ?: simAccountResolver.resolveSimSlot(handle))
        val resolvedLabel = if (isEmergency) null else (previousInfo?.simLabel ?: simAccountResolver.getSimLabel(resolvedSlot))

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
            audioRoute = _audioRoute.value,
            simSlot = resolvedSlot,
            simLabel = resolvedLabel,
            phoneAccountHandle = handle
        )
        _currentCall.value = updatedInfo
        if (state == LineaCallState.RINGING && _incomingFloatingCall.value != null) {
            floatingOverlayManager.update(updatedInfo)
        }

        proximitySensorManager.onCallStateOrAudioChanged(state == LineaCallState.ACTIVE, _audioRoute.value)

        if (state == LineaCallState.ACTIVE) {
            _isRingerSilenced.value = false
            callRingtoneManager.stopRinging()
            dismissFloatingCall()
        }

        if (previousCallState != LineaCallState.ACTIVE && state == LineaCallState.ACTIVE) {
            scope.launch {
                if (preferences.callVibrationEnabled.first()) {
                    vibrateFeedback(longArrayOf(0, 80))
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
        registerHardwareKeyReceiver()
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

        val trimmed = phoneNumber.trim()
        val sanitizedNumber = android.telephony.PhoneNumberUtils.stripSeparators(
            android.telephony.PhoneNumberUtils.convertKeypadLettersToDigits(trimmed)
        ).ifBlank { trimmed }

        val uri = Uri.fromParts("tel", sanitizedNumber, null)
        val effectiveHandle = simAccountHandle ?: try {
            telecomManager.getDefaultOutgoingPhoneAccount(uri.scheme)
        } catch (_: Exception) { null }
        requestedOutgoingAccount = effectiveHandle
        val extras = Bundle().apply {
            if (effectiveHandle != null) {
                putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, effectiveHandle)
            }
        }
        telecomManager.placeCall(uri, extras)
    }

    fun triggerFakeCall(
        callerName: String,
        phoneNumber: String,
        photoUri: String? = null,
        vibrateOnly: Boolean = false
    ) {
        val fakeCall = ActiveCallInfo(
            call = null,
            phoneNumber = phoneNumber,
            displayName = callerName,
            photoUri = photoUri,
            state = LineaCallState.RINGING,
            isIncoming = true,
            isSimulated = true
        )
        _currentCall.value = fakeCall
        _isRingerSilenced.value = false
        ringerStartedTimestamp = SystemClock.elapsedRealtime()
        if (vibrateOnly) {
            vibrateFeedback(longArrayOf(0, 800, 400, 800, 400, 800))
        } else {
            callRingtoneManager.startRinging(phoneNumber)
        }
        registerHardwareKeyReceiver()

        val intent = Intent(context, InCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        context.startActivity(intent)
    }

    fun answerCall() {
        val current = _currentCall.value
        _isRingerSilenced.value = false
        ringerStartedTimestamp = 0L
        callRingtoneManager.stopRinging()
        callGestureManager.stopListening()
        dismissFloatingCall()

        if (current?.isSimulated == true) {
            val connectTime = System.currentTimeMillis()
            _currentCall.value = current.copy(
                state = LineaCallState.ACTIVE,
                connectTimeMillis = connectTime,
                durationSeconds = 0
            )
            startDurationTimer(connectTime)
            proximitySensorManager.onCallStateOrAudioChanged(true, _audioRoute.value)
            scope.launch {
                if (preferences.callVibrationEnabled.first()) {
                    vibrateFeedback(longArrayOf(0, 80))
                }
            }
            return
        }

        current?.call?.answer(0)
    }

    fun rejectCall(rejectWithMessage: Boolean = false, textMessage: String? = null) {
        val current = _currentCall.value
        current?.call?.let { userRejectedCalls.add(it) }
        _isRingerSilenced.value = false
        ringerStartedTimestamp = 0L
        callRingtoneManager.stopRinging()
        callGestureManager.stopListening()
        dismissFloatingCall()

        if (current?.isSimulated == true) {
            timerJob?.cancel()
            timerJob = null
            proximitySensorManager.release()
            recordPostCallSummary(current)
            _currentCall.value = current.copy(state = LineaCallState.DISCONNECTED)
            scope.launch {
                delay(300)
                _currentCall.value = null
            }
            return
        }

        try {
            if (rejectWithMessage) {
                current?.call?.reject(true, textMessage)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                current?.call?.reject(Call.REJECT_REASON_DECLINED)
            } else {
                current?.call?.reject(false, null)
            }
        } catch (e: Exception) {
            android.util.Log.e("CallManager", "Failed to reject call", e)
            try { current?.call?.disconnect() } catch (_: Exception) {}
        }
    }

    fun recordPostCallSummary(active: ActiveCallInfo) {
        if (!isPostCallHudEnabled) {
            _postCallSummary.value = null
            return
        }
        val callerId = com.ryanshelby.linea.telecom.screening.OfflineCallerIdEngine.identifyNumber(active.phoneNumber)
        _postCallSummary.value = PostCallSummary(
            phoneNumber = active.phoneNumber,
            callerName = active.displayName,
            photoUri = active.photoUri,
            durationSeconds = active.durationSeconds,
            isIncoming = active.isIncoming,
            wasConnected = active.durationSeconds > 0 || active.connectTimeMillis > 0,
            timestamp = System.currentTimeMillis(),
            callerIdResult = callerId
        )
    }

    fun silenceRinger() {
        if (_isRingerSilenced.value) return
        android.util.Log.d("CallManager", "silenceRinger() called")
        _isRingerSilenced.value = true
        callRingtoneManager.silence()
        callGestureManager.stopListening()
        try {
            telecomManager.silenceRinger()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun silenceRingerFromTelecom() {
        val elapsed = if (ringerStartedTimestamp > 0L) SystemClock.elapsedRealtime() - ringerStartedTimestamp else 0L
        android.util.Log.d("CallManager", "silenceRingerFromTelecom() called, elapsed=${elapsed}ms, isRinging=$isRinging, isSilenced=${_isRingerSilenced.value}")
        // Telecom can fire onSilenceRinger during initial connection/handoff setup.
        // Only silence if the call has already been actively ringing for at least 1000ms.
        if (isRinging && !_isRingerSilenced.value && elapsed >= 1000L) {
            silenceRinger()
        } else {
            android.util.Log.w("CallManager", "Ignored premature onSilenceRinger from Telecom (elapsed=${elapsed}ms < 1000ms)")
        }
    }

    fun disconnectCall() {
        val current = _currentCall.value
        if (current?.isSimulated == true) {
            timerJob?.cancel()
            timerJob = null
            proximitySensorManager.release()
            recordPostCallSummary(current)
            _currentCall.value = current.copy(state = LineaCallState.DISCONNECTED)
            scope.launch {
                delay(300)
                _currentCall.value = null
            }
            return
        }

        current?.call?.disconnect()
    }

    fun holdCall() {
        val current = _currentCall.value
        if (current?.isSimulated == true) {
            _currentCall.value = current.copy(state = LineaCallState.HOLDING, isHeld = true)
            return
        }
        current?.call?.hold()
        _currentCall.value = current?.copy(state = LineaCallState.HOLDING, isHeld = true)
        refreshOngoingCallNotification()
    }

    fun unholdCall() {
        val current = _currentCall.value
        if (current?.isSimulated == true) {
            _currentCall.value = current.copy(state = LineaCallState.ACTIVE, isHeld = false)
            return
        }
        current?.call?.unhold()
        _currentCall.value = current?.copy(state = LineaCallState.ACTIVE, isHeld = false)
        refreshOngoingCallNotification()
    }

    fun swapCalls() {
        val active = _currentCall.value
        val secondary = _secondaryCall.value
        if (active != null && secondary != null) {
            active.call?.hold()
            secondary.call?.unhold()
            _currentCall.value = secondary
            _secondaryCall.value = active
            refreshOngoingCallNotification()
        }
    }

    fun mergeConference() {
        val active = _currentCall.value
        val secondary = _secondaryCall.value
        if (active?.call != null && secondary?.call != null) {
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
        proximitySensorManager.onCallStateOrAudioChanged(isCallActive, routeEnum)
        refreshOngoingCallNotification()
    }

    fun toggleSpeaker() {
        val willBeSpeaker = _audioRoute.value != LineaAudioRoute.SPEAKER
        userSelectedSpeaker = willBeSpeaker
        setSpeakerphone(willBeSpeaker)
    }

    fun canMergeConference(): Boolean {
        val current = _currentCall.value ?: return false
        val secondary = _secondaryCall.value ?: return false
        val details = current.call?.details
        val secDetails = secondary.call?.details
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
        secondary.call?.answer(0)
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
        secondary.call?.answer(0)
        _currentCall.value = secondary.copy(state = LineaCallState.ACTIVE)
        _secondaryCall.value = null
    }

    fun rejectWaitingCall() {
        val secondary = _secondaryCall.value ?: return
        try {
            secondary.call?.reject(Call.REJECT_REASON_DECLINED)
        } catch (_: Exception) {
            secondary.call?.disconnect()
        }
        _secondaryCall.value = null
    }

}
