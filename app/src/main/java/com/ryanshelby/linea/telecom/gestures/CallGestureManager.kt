package com.ryanshelby.linea.telecom.gestures

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.ryanshelby.linea.data.preferences.LineaPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class CallGestureManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: LineaPreferences
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private var gravitySensor: Sensor? = null
    private var proximitySensor: Sensor? = null

    private var onSilenceCallback: (() -> Unit)? = null
    private var isListening = false
    private var hasSilenced = false
    private var wasFaceUpOrHeld = false

    private var flipEnabled = false
    private var proximityEnabled = false

    init {
        gravitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    }

    @Synchronized
    fun startListening(onSilence: () -> Unit) {
        if (isListening) return

        onSilenceCallback = onSilence
        hasSilenced = false
        wasFaceUpOrHeld = false

        scope.launch {
            flipEnabled = preferences.flipToSilenceEnabled.first()
            proximityEnabled = preferences.proximityWaveToSilenceEnabled.first()

            if (!flipEnabled && !proximityEnabled) {
                // Neither gesture enabled by user (default is off)
                return@launch
            }

            isListening = true

            if (flipEnabled && gravitySensor != null) {
                sensorManager?.registerListener(
                    this@CallGestureManager,
                    gravitySensor,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
            }

            if (proximityEnabled && proximitySensor != null) {
                sensorManager?.registerListener(
                    this@CallGestureManager,
                    proximitySensor,
                    SensorManager.SENSOR_DELAY_UI
                )
            }
        }
    }

    @Synchronized
    fun stopListening() {
        if (!isListening) return
        isListening = false
        hasSilenced = false
        wasFaceUpOrHeld = false
        onSilenceCallback = null

        try {
            sensorManager?.unregisterListener(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || hasSilenced) return

        val sensorType = event.sensor.type

        // 1. Flip to Silence (Gravity / Accelerometer)
        if (flipEnabled && (sensorType == Sensor.TYPE_GRAVITY || sensorType == Sensor.TYPE_ACCELEROMETER)) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // If phone was face-up or held vertically in hand
            if (z > 2.0f || abs(y) > 5.0f || abs(x) > 5.0f) {
                wasFaceUpOrHeld = true
            }

            // Detect transition to screen face-down on flat surface (z is negative gravity)
            val isFaceDown = z < -7.0f && abs(x) < 4.5f && abs(y) < 4.5f
            if (wasFaceDownOrFlipped(isFaceDown)) {
                triggerSilence("Flip to Silence")
            }
        }

        // 2. Proximity Wave to Silence
        if (proximityEnabled && sensorType == Sensor.TYPE_PROXIMITY) {
            val distance = event.values[0]
            val maxRange = event.sensor.maximumRange

            // Hand hovering or waving very close to proximity sensor (< 4.5cm or near)
            val isNear = distance < maxRange && distance <= 4.5f
            if (isNear) {
                triggerSilence("Proximity Wave to Silence")
            }
        }
    }

    private fun wasFaceDownOrFlipped(isFaceDown: Boolean): Boolean {
        return isFaceDown && wasFaceUpOrHeld
    }

    private fun triggerSilence(reason: String) {
        if (hasSilenced) return
        hasSilenced = true

        try {
            onSilenceCallback?.invoke()
            triggerHapticFeedback()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun triggerHapticFeedback() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(40)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
