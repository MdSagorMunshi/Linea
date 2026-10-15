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
        android.util.Log.i("LineaInCallService", "onCallAdded: state=${call.state}, handle=${call.details?.handle}")
        try {
            callManager.registerInCallService(this)
            callManager.onCallAdded(call)
        } catch (e: Exception) {
            android.util.Log.e("LineaInCallService", "Error in onCallAdded", e)
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        android.util.Log.i("LineaInCallService", "onCallRemoved: state=${call.state}, disconnectCause=${call.details?.disconnectCause}")
        try {
            callManager.onCallRemoved(call)
        } catch (e: Exception) {
            android.util.Log.e("LineaInCallService", "Error in onCallRemoved", e)
        }
    }

    override fun onSilenceRinger() {
        super.onSilenceRinger()
        android.util.Log.d("LineaInCallService", "onSilenceRinger received from Telecom")
        // Only forward to silenceRinger if the call has already been ringing for > 1000ms
        // (Telecom can fire onSilenceRinger during initial connection/handoff).
        if (callManager.isRinging) {
            callManager.silenceRinger()
        }
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        super.onCallAudioStateChanged(audioState)
        android.util.Log.d("LineaInCallService", "onCallAudioStateChanged: route=${audioState.route}, isMuted=${audioState.isMuted}")
        try {
            callManager.onAudioStateChanged(audioState)
        } catch (e: Exception) {
            android.util.Log.e("LineaInCallService", "Error in onCallAudioStateChanged", e)
        }
    }

    override fun onDestroy() {
        callManager.unregisterInCallService()
        super.onDestroy()
    }
}
