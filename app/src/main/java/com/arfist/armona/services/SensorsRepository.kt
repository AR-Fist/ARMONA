package com.arfist.armona.services

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import timber.log.Timber

class SensorsRepository private constructor(context: Context): SensorEventListener{
    companion object {
        @Volatile private var INSTANCE: SensorsRepository? = null

        fun getInstance(context: Context): SensorsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE
                    ?: SensorsRepository(context).also { INSTANCE = it }
            }
        }

    }

    private var _accelerometer = MutableLiveData<SensorEvent>()
    val accelerometer: LiveData<SensorEvent>
            get() = _accelerometer

    private var _gyroscope = MutableLiveData<SensorEvent>()
    val gyroscope: LiveData<SensorEvent>
        get() = _gyroscope

    private var _magnetometer = MutableLiveData<SensorEvent>()
    val magnetometer: LiveData<SensorEvent>
        get() = _magnetometer

//    private var _uncalibratedAccelerometer = MutableLiveData<SensorEvent>()
//    val uncalibratedAccelerometer: LiveData<SensorEvent>
//        get() = _uncalibratedAccelerometer
//
//    private var _uncalibratedGyroscope = MutableLiveData<SensorEvent>()
//    val uncalibratedGyroscope: LiveData<SensorEvent>
//        get() = _uncalibratedGyroscope
//
//    private var _uncalibratedMagnetoMeter = MutableLiveData<SensorEvent>()
//    val uncalibratedMagnetometer: LiveData<SensorEvent>
//        get() = _uncalibratedMagnetoMeter
//
//    private var _gravity = MutableLiveData<SensorEvent>()
//    val gravity = _gravity
//
//    private var _linearAccelerometer = MutableLiveData<SensorEvent>()
//    val linearAccelerometer: LiveData<SensorEvent>
//        get() = _linearAccelerometer

    private var _rotationVector = MutableLiveData<SensorEvent>()
    val rotationVector: LiveData<SensorEvent>
        get() = _rotationVector

//    private val sensorsList = arrayOf(
//        Sensor.TYPE_ACCELEROMETER,
//        Sensor.TYPE_ACCELEROMETER_UNCALIBRATED,
//        Sensor.TYPE_LINEAR_ACCELERATION,
//        Sensor.TYPE_GYROSCOPE,
//        Sensor.TYPE_GYROSCOPE_UNCALIBRATED,
//        Sensor.TYPE_MAGNETIC_FIELD,
//        Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED,
//        Sensor.TYPE_GRAVITY,
//        Sensor.TYPE_ROTATION_VECTOR
//    )
private val sensorsList = arrayOf(
    Sensor.TYPE_ACCELEROMETER,
    Sensor.TYPE_GYROSCOPE,
    Sensor.TYPE_MAGNETIC_FIELD,
    Sensor.TYPE_ROTATION_VECTOR
)

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    fun registerSensors() {
        for (sensor in sensorsList) {
            sensorManager.registerListener(this, sensorManager.getDefaultSensor(sensor), SensorManager.SENSOR_DELAY_FASTEST)
        }
    }

    fun unregisterSensors() {
        sensorManager.unregisterListener(this)
    }

    // Callback when sensor(s) update
    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
//            _sensorsMap.value?.set(event.sensor.type, event)
//            when(event.sensor.type) {
//                Sensor.TYPE_ACCELEROMETER ->  _accelerometer.value = event
//                Sensor.TYPE_ACCELEROMETER_UNCALIBRATED -> _uncalibratedAccelerometer.value = event
//                Sensor.TYPE_LINEAR_ACCELERATION -> _linearAccelerometer.value = event
//                Sensor.TYPE_GYROSCOPE -> _gyroscope.value = event
//                Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> _uncalibratedGyroscope.value = event
//                Sensor.TYPE_MAGNETIC_FIELD -> _magnetometer.value = event
//                Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED -> _uncalibratedMagnetoMeter.value = event
//                Sensor.TYPE_GRAVITY -> _gravity.value = event
//                Sensor.TYPE_ROTATION_VECTOR -> _rotationVector.value = event
//            }
            when(event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER ->  _accelerometer.value = event
                Sensor.TYPE_GYROSCOPE -> _gyroscope.value = event
                Sensor.TYPE_MAGNETIC_FIELD -> _magnetometer.value = event
                Sensor.TYPE_ROTATION_VECTOR -> _rotationVector.value = event
            }
        }
    }

    // Callback when sensor(s)'s accuracy change
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Timber.i("Accuracy changed")
    }

}