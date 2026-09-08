package com.ryanshelby.linea.ui.screens.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryanshelby.linea.telecom.CallManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TelephonyDiagnosticsState(
    val simSlot: Int = 0,
    val carrierName: String = "Cellular SIM 1",
    val networkType: String = "5G NR / LTE-A",
    val isVoLteActive: Boolean = true,
    val isVoWifiActive: Boolean = false,
    val signalDbm: Int = -84,
    val signalBars: Int = 4,
    val audioRoute: String = "Built-in Earpiece",
    val bluetoothDeviceName: String? = null,
    val connectionState: String = "Active & Stable",
    val estimatedLatencyMs: Int = 22,
    val packetLossPercent: Double = 0.0,
    val isRefreshing: Boolean = false
)

@HiltViewModel
class CallDiagnosticsViewModel @Inject constructor(
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
            delay(400) // Simulating telephony hardware query
            val hasActiveCall = callManager.currentCall.value != null
            _state.value = _state.value.copy(
                simSlot = 0,
                carrierName = "Default SIM 1",
                networkType = if (hasActiveCall) "VoLTE (AMR-WB 23.85 kbps)" else "5G NR (Sub-6 GHz)",
                isVoLteActive = true,
                isVoWifiActive = false,
                signalDbm = -82,
                signalBars = 4,
                audioRoute = if (hasActiveCall) "Earpiece (Direct)" else "System Audio Idle",
                bluetoothDeviceName = null,
                connectionState = if (hasActiveCall) "Active Cellular Route" else "Idle / Ready",
                estimatedLatencyMs = 21,
                packetLossPercent = 0.0,
                isRefreshing = false
            )
        }
    }
}
