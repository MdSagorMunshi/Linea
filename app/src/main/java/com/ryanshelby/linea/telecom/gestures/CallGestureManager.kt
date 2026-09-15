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

    // Proximity state
    @Volatile
    var isProximityNear: Boolean = false
        private set

    private var initialProximityWasNear = false
    private var farStartTime = 0L

    // Orientation state
    private var initialOrientationWasFaceDown = false
    private var wasActivelyHeldFaceUp = false
    private var faceUpStartTime = 0L

    private var listeningStartTime = 0L

    private var flipEnabled = false
    private var proximityEnabled = false

    companion object {
        private const val INITIAL_STABILIZATION_MS = 1200L
        private const val MIN_FAR_DURATION_FOR_WAVE_MS = 600L
        private const val MIN_FACE_UP_DURATION_MS = 400L
    }

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
        isProximityNear = false
        initialProximityWasNear = false
        initialOrientationWasFaceDown = false
        farStartTime = 0L
        wasActivelyHeldFaceUp = false
        faceUpStartTime = 0L
        listeningStartTime = android.os.SystemClock.elapsedRealtime()

        scope.launch {
            flipEnabled = preferences.flipToSilenceEnabled.first()
            proximityEnabled = preferences.proximityWaveToSilenceEnabled.first()

            android.util.Log.d("CallGestureManager", "startListening: flipEnabled=$flipEnabled, proximityEnabled=$proximityEnabled")

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
        android.util.Log.d("CallGestureManager", "stopListening")
        isListening = false
        hasSilenced = false
        isProximityNear = false
        initialProximityWasNear = false
        initialOrientationWasFaceDown = false
        farStartTime = 0L
        wasActivelyHeldFaceUp = false
        faceUpStartTime = 0L
        onSilenceCallback = null

        try {
            sensorManager?.unregisterListener(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || hasSilenced) return

        val now = android.os.SystemClock.elapsedRealtime()
        val elapsedSinceStart = now - listeningStartTime
        val sensorType = event.sensor.type

        // 1. Proximity Sensor (Wave to Silence & Pocket Detection)
        if (sensorType == Sensor.TYPE_PROXIMITY) {
            val distance = event.values[0]
            val maxRange = event.sensor.maximumRange
            val isNear = distance < maxRange && distance <= 4.5f
            isProximityNear = isNear

            // Capture initial pocket state during stabilization
            if (elapsedSinceStart < INITIAL_STABILIZATION_MS) {
                if (isNear) {
                    initialProximityWasNear = true
                }
                return
            }

            if (!proximityEnabled) return

            if (isNear) {
                // Trigger only if:
                // 1. Phone did NOT start in pocket/covered, OR was subsequently in open air long enough
                // 2. Proximity sensor was in open air (FAR) for at least MIN_FAR_DURATION_FOR_WAVE_MS before waving
                if (!initialProximityWasNear && farStartTime > 0L && (now - farStartTime >= MIN_FAR_DURATION_FOR_WAVE_MS)) {
                    triggerSilence("Proximity Wave to Silence")
                }
            } else {
                // FAR (open air)
                if (farStartTime == 0L) {
                    farStartTime = now
                }
                // If phone was in pocket and pulled out into open air for >= 1500ms, clear pocket restriction
                if (initialProximityWasNear && (now - farStartTime >= 1500L)) {
                    initialProximityWasNear = false
                }
            }
        }

        // 2. Gravity / Accelerometer (Flip to Silence)
        if (sensorType == Sensor.TYPE_GRAVITY || sensorType == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // Flat screen face-down condition (Z negative gravity, X and Y near 0)
            val isCurrentlyFaceDown = z < -7.0f && abs(x) < 4.5f && abs(y) < 4.5f

            if (elapsedSinceStart < INITIAL_STABILIZATION_MS) {
                if (isCurrentlyFaceDown) {
                    initialOrientationWasFaceDown = true
                }
                return
            }

            if (!flipEnabled) return

            // Actively viewed face-up condition:
            // Screen must be facing up (Z > 6.5f), not upside down in pocket (abs(y) < 5.0f),
            // and proximity must NOT be near (not in pocket or bag)
            val isFaceUp = z > 6.5f && abs(x) < 5.0f && abs(y) < 5.0f && !isProximityNear

            if (isFaceUp) {
                if (faceUpStartTime == 0L) {
                    faceUpStartTime = now
                } else if (now - faceUpStartTime >= MIN_FACE_UP_DURATION_MS) {
                    wasActivelyHeldFaceUp = true
                    initialOrientationWasFaceDown = false
                }
            } else if (!isCurrentlyFaceDown) {
                // Tilted away, reset continuous face-up counter
                faceUpStartTime = 0L
            }

            // Only flip to silence if:
            // 1. The user was confirmed actively viewing/holding the phone face-up
            // 2. The phone was NOT already face-down at call start
            // 3. The phone is now turned face-down on a flat surface
            if (isCurrentlyFaceDown && wasActivelyHeldFaceUp && !initialOrientationWasFaceDown) {
                triggerSilence("Flip to Silence")
            }
        }
    }

    private fun triggerSilence(reason: String) {
        if (hasSilenced) return
        hasSilenced = true
        android.util.Log.d("CallGestureManager", "triggerSilence: $reason")

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
