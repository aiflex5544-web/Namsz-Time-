package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

data class CompassState(
    val azimuthDegrees: Float = 0f,
    val isSensorAvailable: Boolean = true,
    val accuracy: Int = SensorManager.SENSOR_STATUS_ACCURACY_HIGH
)

class CompassManager(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationVectorSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometerSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magneticSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val _compassState = MutableStateFlow(CompassState())
    val compassState: StateFlow<CompassState> = _compassState.asStateFlow()

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private val lastAccelerometer = FloatArray(3)
    private val lastMagnetometer = FloatArray(3)
    private var lastAccelerometerSet = false
    private var lastMagnetometerSet = false

    private var smoothedAzimuth = 0f

    fun startListening() {
        val hasRotation = rotationVectorSensor != null
        val hasAccelMag = accelerometerSensor != null && magneticSensor != null

        if (!hasRotation && !hasAccelMag) {
            _compassState.value = _compassState.value.copy(isSensorAvailable = false)
            return
        }

        _compassState.value = _compassState.value.copy(isSensorAvailable = true)

        if (rotationVectorSensor != null) {
            sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            accelerometerSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
            magneticSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            var azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
            azimuth = (azimuth + 360f) % 360f
            updateSmoothedAzimuth(azimuth, event.accuracy)
        } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, lastAccelerometer, 0, event.values.size)
            lastAccelerometerSet = true
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.size)
            lastMagnetometerSet = true
        }

        if (lastAccelerometerSet && lastMagnetometerSet && rotationVectorSensor == null) {
            if (SensorManager.getRotationMatrix(rotationMatrix, null, lastAccelerometer, lastMagnetometer)) {
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                var azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                azimuth = (azimuth + 360f) % 360f
                updateSmoothedAzimuth(azimuth, event.accuracy)
            }
        }
    }

    private fun updateSmoothedAzimuth(rawAzimuth: Float, accuracy: Int) {
        // Handle 360-0 wrap-around smoothly
        var diff = rawAzimuth - smoothedAzimuth
        while (diff < -180f) diff += 360f
        while (diff > 180f) diff -= 360f

        // Exponential smoothing (alpha = 0.25f)
        smoothedAzimuth = (smoothedAzimuth + 0.25f * diff + 360f) % 360f

        _compassState.value = CompassState(
            azimuthDegrees = smoothedAzimuth,
            isSensorAvailable = true,
            accuracy = accuracy
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        _compassState.value = _compassState.value.copy(accuracy = accuracy)
    }
}
