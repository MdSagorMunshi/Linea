package com.ryanshelby.linea.ui.incall

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import com.ryanshelby.linea.MainActivity
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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

    companion object {
        const val ACTION_ANSWER_CALL = "com.ryanshelby.linea.ACTION_ANSWER_CALL"
    }

    @Inject
    lateinit var callManager: CallManager

    @Inject
    lateinit var lineaPreferences: LineaPreferences

    @Inject
    lateinit var callNoteDao: com.ryanshelby.linea.data.local.dao.CallNoteDao


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        volumeControlStream = AudioManager.STREAM_RING
        handleIntent(intent)
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
            val isMuted by callManager.isMuted.collectAsState()
            val audioRoute by callManager.audioRoute.collectAsState()
            val durationWarningActive by callManager.durationWarningActive.collectAsState()
            val isRingerSilenced by callManager.isRingerSilenced.collectAsState()

            // System back gesture safely minimizes in-call task instead of terminating cellular call
            BackHandler {
                moveTaskToBack(true)
            }

            LaunchedEffect(currentCall?.state, secondaryCall?.state) {
                val current = currentCall
                val secondary = secondaryCall
                val noActiveCalls = (current == null || current.state == LineaCallState.DISCONNECTED) &&
                        (secondary == null || secondary.state == LineaCallState.DISCONNECTED)
                if (noActiveCalls) {
                    delay(800)
                    val stillNoCalls = (callManager.currentCall.value == null || callManager.currentCall.value?.state == LineaCallState.DISCONNECTED) &&
                            (callManager.secondaryCall.value == null || callManager.secondaryCall.value?.state == LineaCallState.DISCONNECTED)
                    if (stillNoCalls) {
                        finishAndRemoveTask()
                    }
                } else {
                    volumeControlStream = if (current?.state == LineaCallState.ACTIVE || current?.state == LineaCallState.HOLDING ||
                        secondary?.state == LineaCallState.ACTIVE || secondary?.state == LineaCallState.HOLDING) {
                        AudioManager.STREAM_VOICE_CALL
                    } else {
                        AudioManager.STREAM_RING
                    }
                }
            }


            val themePreference by lineaPreferences.themePreference.collectAsState(initial = "DARK")

            CompositionLocalProvider(LocalReduceAnimations provides reduceAnimations) {
                LineaTheme(theme = themePreference, reduceAnimations = reduceAnimations) {
                    val call = currentCall ?: secondaryCall
                    if (call != null) {
                        if (call.state == LineaCallState.RINGING && call.isIncoming) {
                            IncomingCallScreen(
                                callInfo = call,
                                onAnswer = { callManager.answerCall() },
                                onReject = { callManager.rejectCall() },
                                onSilence = { callManager.silenceRinger() },
                                onQuickSms = { msg -> callManager.rejectCall(rejectWithMessage = true, textMessage = msg) },
                                isSilenced = isRingerSilenced
                            )
                        } else {
                            val notes by callNoteDao.getNotesForContact(null, call.phoneNumber).collectAsState(initial = emptyList())
                            val preCallNote = notes.firstOrNull { it.isPreCallNote }?.noteText

                            InCallScreen(
                                callInfo = call,
                                secondaryCall = secondaryCall,
                                isMuted = isMuted,
                                audioRoute = audioRoute,
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
                                    val intent = Intent(this@InCallActivity, MainActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    }
                                    startActivity(intent)
                                },
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

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        if (intent?.action == ACTION_ANSWER_CALL) {
            callManager.answerCall()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (callManager.isRinging) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP,
                KeyEvent.KEYCODE_VOLUME_DOWN,
                KeyEvent.KEYCODE_VOLUME_MUTE,
                KeyEvent.KEYCODE_POWER -> {
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        callManager.silenceRinger()
                    }
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (callManager.isRinging) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP,
                KeyEvent.KEYCODE_VOLUME_DOWN,
                KeyEvent.KEYCODE_VOLUME_MUTE,
                KeyEvent.KEYCODE_POWER -> {
                    callManager.silenceRinger()
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}
