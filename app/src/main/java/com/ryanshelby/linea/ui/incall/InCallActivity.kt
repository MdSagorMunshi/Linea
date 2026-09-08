package com.ryanshelby.linea.ui.incall

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.ryanshelby.linea.data.preferences.LineaPreferences
import com.ryanshelby.linea.telecom.CallManager
import com.ryanshelby.linea.telecom.LineaCallState
import com.ryanshelby.linea.ui.theme.LineaTheme
import com.ryanshelby.linea.ui.theme.LocalReduceAnimations
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import javax.inject.Inject

@AndroidEntryPoint
class InCallActivity : ComponentActivity() {

    @Inject
    lateinit var callManager: CallManager

    @Inject
    lateinit var lineaPreferences: LineaPreferences

    @Inject
    lateinit var callNoteDao: com.ryanshelby.linea.data.local.dao.CallNoteDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Lockscreen takeover and screen wake
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            val reduceAnimations by lineaPreferences.reduceAnimations.collectAsState(initial = false)
            val currentCall by callManager.currentCall.collectAsState()
            val secondaryCall by callManager.secondaryCall.collectAsState()
            val isRecording by callManager.isRecording.collectAsState()
            val recordingDuration by callManager.recordingDurationSeconds.collectAsState()
            val durationWarningActive by callManager.durationWarningActive.collectAsState()

            LaunchedEffect(currentCall?.state) {
                val state = currentCall?.state
                if (currentCall == null || state == LineaCallState.DISCONNECTED) {
                    delay(600)
                    finishAndRemoveTask()
                }
            }

            CompositionLocalProvider(LocalReduceAnimations provides reduceAnimations) {
                LineaTheme {
                    val call = currentCall
                    if (call != null) {
                        if (call.state == LineaCallState.RINGING && call.isIncoming) {
                            IncomingCallScreen(
                                callInfo = call,
                                onAnswer = { callManager.answerCall() },
                                onReject = { callManager.rejectCall() },
                                onSilence = { callManager.silenceRinger() },
                                onQuickSms = { msg -> callManager.rejectCall(rejectWithMessage = true, textMessage = msg) }
                            )
                        } else {
                            val notes by callNoteDao.getNotesForContact(null, call.phoneNumber).collectAsState(initial = emptyList())
                            val preCallNote = notes.firstOrNull { it.isPreCallNote }?.noteText

                            InCallScreen(
                                callInfo = call,
                                secondaryCall = secondaryCall,
                                isRecording = isRecording,
                                recordingDurationSeconds = recordingDuration,
                                durationWarningActive = durationWarningActive,
                                existingNotes = notes,
                                preCallNote = preCallNote,
                                onDisconnect = { callManager.disconnectCall() },
                                onToggleMute = { callManager.toggleMute() },
                                onToggleSpeaker = { callManager.toggleSpeaker() },
                                onToggleHold = {
                                    if (call.state == LineaCallState.HOLDING || call.isHeld) {
                                        callManager.unholdCall()
                                    } else {
                                        callManager.holdCall()
                                    }
                                },
                                onDtmfPress = { digit -> callManager.playDtmfTone(digit) },
                                onDtmfRelease = { callManager.stopDtmfTone() },
                                onAddCall = {
                                    // Allow user to return to dialer to initiate second call
                                    moveTaskToBack(true)
                                },
                                onToggleRecord = { callManager.toggleRecording() },
                                onSaveNote = { noteText ->
                                    this@InCallActivity.lifecycleScope.launch {
                                        callNoteDao.insertNote(
                                            com.ryanshelby.linea.data.local.entities.CallNoteEntity(
                                                phoneNumber = call.phoneNumber,
                                                noteText = noteText,
                                                timestamp = System.currentTimeMillis()
                                            )
                                        )
                                    }
                                },
                                onAnswerWaitingHold = { callManager.answerWaitingCallAndHoldActive() },
                                onAnswerWaitingEnd = { callManager.answerWaitingCallAndEndActive() },
                                onRejectWaiting = { callManager.rejectWaitingCall() },
                                onSwapCalls = { callManager.swapCalls() },
                                onMergeConference = { callManager.mergeConference() }
                            )
                        }
                    }
                }
            }
        }
    }
}
