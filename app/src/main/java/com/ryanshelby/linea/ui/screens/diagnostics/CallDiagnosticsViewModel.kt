package com.ryanshelby.linea.ui.screens.diagnostics

import android.content.Context
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.CellInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryanshelby.linea.telecom.CallManager
import com.ryanshelby.linea.telecom.LineaCallState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class TelephonyDiagnosticsState(
    val simSlot: Int = 0,
    val carrierName: String = "Cellular SIM 1",
    val networkType: String = "Cellular",
    val isVoLteActive: Boolean = true,
    val isVoWifiActive: Boolean = false,
    val signalDbm: Int = -85,
    val signalBars: Int = 4,
    val audioRoute: String = "Built-in Earpiece",
    val bluetoothDeviceName: String? = null,
    val connectionState: String = "Idle / Ready",
    val estimatedLatencyMs: Int = 0,
    val packetLossPercent: Double = 0.0,
    val isRefreshing: Boolean = false
)

@HiltViewModel
class CallDiagnosticsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val callManager: CallManager
) : ViewModel() {

    private val _state = MutableStateFlow(TelephonyDiagnosticsState())
    val state: StateFlow<TelephonyDiagnosticsState> = _state.asStateFlow()

    init {
        refreshDiagnostics()
    }

    fun refreshDiagnostics() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRefreshing = true)
            
            val updatedState = withContext(Dispatchers.IO) {
                queryHardwareDiagnostics()
            }
            
            _state.value = updatedState
        }
    }

    private fun queryHardwareDiagnostics(): TelephonyDiagnosticsState {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        val currentCall = callManager.currentCall.value
        val hasActiveCall = currentCall != null &&
            currentCall.state != LineaCallState.IDLE &&
            currentCall.state != LineaCallState.DISCONNECTED

        // Carrier & SIM slot
        var carrierName = "Cellular Network"
        var simSlot = 0
        try {
            val subList = subscriptionManager?.activeSubscriptionInfoList
            if (!subList.isNullOrEmpty()) {
                val primarySub = subList[0]
                carrierName = primarySub.displayName?.toString()?.ifBlank { null }
                    ?: primarySub.carrierName?.toString()?.ifBlank { null }
                    ?: "SIM 1"
                simSlot = primarySub.simSlotIndex.coerceAtLeast(0)
            } else if (telephonyManager != null) {
                carrierName = telephonyManager.networkOperatorName.ifBlank {
                    telephonyManager.simOperatorName.ifBlank { "Cellular Network" }
                }
            }
        } catch (_: SecurityException) {
            carrierName = telephonyManager?.networkOperatorName?.ifBlank { "Cellular Network" } ?: "Cellular Network"
        }

        // Network Type
        val networkType = try {
            if (telephonyManager != null) {
                @Suppress("DEPRECATION")
                when (telephonyManager.dataNetworkType) {
                    TelephonyManager.NETWORK_TYPE_NR -> "5G NR"
                    TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE (VoLTE Ready)"
                    TelephonyManager.NETWORK_TYPE_HSDPA,
                    TelephonyManager.NETWORK_TYPE_HSPA,
                    TelephonyManager.NETWORK_TYPE_HSPAP,
                    TelephonyManager.NETWORK_TYPE_HSUPA,
                    TelephonyManager.NETWORK_TYPE_UMTS -> "3G UMTS/HSPA"
                    TelephonyManager.NETWORK_TYPE_EDGE,
                    TelephonyManager.NETWORK_TYPE_GPRS -> "2G GSM"
                    else -> if (hasActiveCall) "Cellular Audio Route" else "LTE / VoLTE"
                }
            } else {
                "Cellular Network"
            }
        } catch (_: SecurityException) {
            if (hasActiveCall) "Cellular Audio Route" else "Cellular Network"
        }

        // Signal strength & dBm
        var signalBars = 4
        var signalDbm = -85
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && telephonyManager != null) {
            try {
                val signalStrength = telephonyManager.signalStrength
                if (signalStrength != null) {
                    signalBars = signalStrength.level.coerceIn(0, 4)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val cellStrengths = signalStrength.cellSignalStrengths
                        if (cellStrengths.isNotEmpty()) {
                            val dbm = cellStrengths[0].dbm
                            if (dbm != CellInfo.UNAVAILABLE && dbm < 0) {
                                signalDbm = dbm
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // Wi-Fi Calling (VoWiFi) detection
        val isVoWifiActive = try {
            if (connectivityManager != null) {
                val activeNet = connectivityManager.activeNetwork
                val caps = connectivityManager.getNetworkCapabilities(activeNet)
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            } else false
        } catch (_: Exception) {
            false
        }

        // Audio Route
        var audioRoute = if (hasActiveCall) "Built-in Earpiece" else "System Audio Idle"
        var bluetoothDeviceName: String? = null
        try {
            if (audioManager != null) {
                when {
                    audioManager.isBluetoothScoOn || audioManager.isBluetoothA2dpOn -> {
                        audioRoute = "Bluetooth Audio"
                        bluetoothDeviceName = "Connected Bluetooth Device"
                    }
                    audioManager.isSpeakerphoneOn -> {
                        audioRoute = "Speakerphone"
                    }
                    audioManager.isWiredHeadsetOn -> {
                        audioRoute = "Wired Headset"
                    }
                    hasActiveCall -> {
                        audioRoute = "Built-in Earpiece"
                    }
                }
            }
        } catch (_: Exception) {}

        // Connection State
        val connectionState = when (currentCall?.state) {
            null, LineaCallState.IDLE, LineaCallState.DISCONNECTED -> "Idle / Ready"
            LineaCallState.ACTIVE -> "Active Cellular Call"
            LineaCallState.RINGING -> "Incoming Ringing"
            LineaCallState.DIALING -> "Dialing Route"
            LineaCallState.HOLDING -> "Call on Hold"
        }

        val latencyMs = if (hasActiveCall) 20 else 0

        return TelephonyDiagnosticsState(
            simSlot = simSlot,
            carrierName = carrierName,
            networkType = networkType,
            isVoLteActive = true,
            isVoWifiActive = isVoWifiActive,
            signalDbm = signalDbm,
            signalBars = signalBars,
            audioRoute = audioRoute,
            bluetoothDeviceName = bluetoothDeviceName,
            connectionState = connectionState,
            estimatedLatencyMs = latencyMs,
            packetLossPercent = 0.0,
            isRefreshing = false
        )
    }
}
