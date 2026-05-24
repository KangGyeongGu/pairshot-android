package com.pairshot.core.infra.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.OrientationEventListener
import android.view.Surface
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@ViewModelScoped
class SensorSessionImpl
@Inject
constructor(
    @ApplicationContext private val context: Context,
) : SensorSession {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val _roll = MutableStateFlow(0f)
    override val roll: StateFlow<Float> = _roll.asStateFlow()

    private val _deviceOrientation = MutableStateFlow(0)
    override val deviceOrientation: StateFlow<Int> = _deviceOrientation.asStateFlow()

    private var smoothedRoll = 0f
    private val remappedMatrix = FloatArray(ROTATION_MATRIX_SIZE)

    private var isRegistered = false
    private var wasActiveBeforeStop = false

    private var lifecycleObserver: LifecycleEventObserver? = null
    private var observedLifecycle: Lifecycle? = null

    private val orientationListener =
        object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                val rotation =
                    when (orientation) {
                        in ORIENTATION_ROTATION_270_MIN..ORIENTATION_ROTATION_270_MAX -> Surface.ROTATION_270
                        in ORIENTATION_ROTATION_180_MIN..ORIENTATION_ROTATION_180_MAX -> Surface.ROTATION_180
                        in ORIENTATION_ROTATION_90_MIN..ORIENTATION_ROTATION_90_MAX -> Surface.ROTATION_90
                        else -> Surface.ROTATION_0
                    }
                _deviceOrientation.value = rotation
            }
        }

    private val sensorListener =
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val rotationMatrix = FloatArray(ROTATION_MATRIX_SIZE)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.remapCoordinateSystem(
                    rotationMatrix,
                    SensorManager.AXIS_X,
                    SensorManager.AXIS_Z,
                    remappedMatrix,
                )
                val orientation = FloatArray(ORIENTATION_VECTOR_SIZE)
                SensorManager.getOrientation(remappedMatrix, orientation)
                val rawRoll = Math.toDegrees(orientation[2].toDouble()).toFloat()

                smoothedRoll = smoothedRoll + SMOOTHING_FACTOR * (rawRoll - smoothedRoll)

                if (kotlin.math.abs(smoothedRoll - _roll.value) >= UPDATE_THRESHOLD) {
                    _roll.value = smoothedRoll
                }
            }

            override fun onAccuracyChanged(
                sensor: Sensor?,
                accuracy: Int,
            ) {}
        }

    companion object {
        private const val SMOOTHING_FACTOR = 0.04f
        private const val UPDATE_THRESHOLD = 0.3f
        private const val ROTATION_MATRIX_SIZE = 9
        private const val ORIENTATION_VECTOR_SIZE = 3
        private const val ORIENTATION_ROTATION_270_MIN = 45
        private const val ORIENTATION_ROTATION_270_MAX = 134
        private const val ORIENTATION_ROTATION_180_MIN = 135
        private const val ORIENTATION_ROTATION_180_MAX = 224
        private const val ORIENTATION_ROTATION_90_MIN = 225
        private const val ORIENTATION_ROTATION_90_MAX = 314
    }

    override fun bind(owner: LifecycleOwner) {
        val lifecycle = owner.lifecycle
        if (observedLifecycle === lifecycle) {
            registerSensorListener()
            startOrientationListener()
            return
        }

        lifecycleObserver?.let { observedLifecycle?.removeObserver(it) }

        val observer =
            LifecycleEventObserver { _: LifecycleOwner, event: Lifecycle.Event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> {
                        wasActiveBeforeStop = isRegistered
                        if (isRegistered) {
                            sensorManager.unregisterListener(sensorListener)
                            isRegistered = false
                        }
                    }

                    Lifecycle.Event.ON_START -> {
                        if (wasActiveBeforeStop) {
                            registerSensorListener()
                            wasActiveBeforeStop = false
                        }
                    }

                    else -> {
                        Unit
                    }
                }
            }

        lifecycleObserver = observer
        observedLifecycle = lifecycle
        lifecycle.addObserver(observer)

        registerSensorListener()
        startOrientationListener()
    }

    private fun registerSensorListener() {
        if (isRegistered) return
        rotationSensor?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_UI)
            isRegistered = true
        }
    }

    private fun startOrientationListener() {
        if (orientationListener.canDetectOrientation()) {
            orientationListener.enable()
        }
    }

    override fun release() {
        isRegistered = false
        wasActiveBeforeStop = false
        sensorManager.unregisterListener(sensorListener)
        orientationListener.disable()

        lifecycleObserver?.let { observedLifecycle?.removeObserver(it) }
        lifecycleObserver = null
        observedLifecycle = null
    }
}
