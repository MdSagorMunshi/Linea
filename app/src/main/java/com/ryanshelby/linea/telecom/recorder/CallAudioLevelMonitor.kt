package com.ryanshelby.linea.telecom.recorder

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thread-safe audio level monitor that maintains normalized amplitude (0f..1f)
 * and rolling history for driving the live ECG waveform visualizer.
 *
 * Fed directly by [CallAudioRecorder]'s unified audio capture pipeline to guarantee
 * zero hardware microphone contention or duplicate AudioRecord allocations.
 */
@Singleton
class CallAudioLevelMonitor @Inject constructor() {

    companion object {
        const val HISTORY_SIZE = 32

        // RMS threshold separating silence from acoustic speech
        private const val NOISE_FLOOR_RMS = 45.0
        // Scaled so human voice / loudspeaker acoustic feedback produces energetic 0.15f..1.0f values
        private const val SPEECH_MAX_RMS = 1500.0
    }

    /** Instantaneous normalized audio level (0f..1f). */
    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    /** Rolling history of the last [HISTORY_SIZE] amplitude samples. */
    private val _audioLevelHistory = MutableStateFlow(FloatArray(HISTORY_SIZE))
    val audioLevelHistory: StateFlow<FloatArray> = _audioLevelHistory.asStateFlow()

    private val historyBuffer = FloatArray(HISTORY_SIZE)

    /**
     * Called by the unified capture engine when an audio chunk is processed.
     * Computes normalized amplitude and updates the rolling history.
     */
    @Synchronized
    fun onAudioSampleRms(rms: Double) {
        val normalized = if (rms <= NOISE_FLOOR_RMS) {
            0f
        } else {
            ((rms - NOISE_FLOOR_RMS) / (SPEECH_MAX_RMS - NOISE_FLOOR_RMS)).toFloat().coerceIn(0f, 1f)
        }

        _audioLevel.value = normalized
        System.arraycopy(historyBuffer, 1, historyBuffer, 0, HISTORY_SIZE - 1)
        historyBuffer[HISTORY_SIZE - 1] = normalized
        _audioLevelHistory.value = historyBuffer.copyOf()
    }

    /**
     * Resets the visualizer to flat zero (straight line) when calls disconnect.
     */
    @Synchronized
    fun reset() {
        historyBuffer.fill(0f)
        _audioLevel.value = 0f
        _audioLevelHistory.value = FloatArray(HISTORY_SIZE)
    }

    /** Backward-compatibility no-ops managed by unified engine. */
    fun startMonitoring() {
        // Lifecycle managed by CallManager -> CallAudioRecorder.startCapture()
    }

    fun stopMonitoring() {
        reset()
    }
}
