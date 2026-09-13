package com.ryanshelby.linea.telecom

import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LineaInCallService : InCallService() {

    @Inject
    lateinit var callManager: CallManager

    override fun onCreate() {
        super.onCreate()
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        callManager.registerInCallService(this)
        callManager.onCallAdded(call)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        callManager.onCallRemoved(call)
    }

    override fun onSilenceRinger() {
        super.onSilenceRinger()
        // Do NOT forward to callManager.silenceRinger() here.
        // Linea manages its own ringtone via CallRingtoneManager. The system
        // calls onSilenceRinger() during call setup which would prematurely
        // silence the ringer before it even starts playing. User-initiated
        // silencing (volume keys, power button, gesture) is handled separately
        // by the hardware key receiver and gesture manager.
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        super.onCallAudioStateChanged(audioState)
        callManager.onAudioStateChanged(audioState)
    }

    override fun onDestroy() {
        callManager.unregisterInCallService()
        super.onDestroy()
    }
}
