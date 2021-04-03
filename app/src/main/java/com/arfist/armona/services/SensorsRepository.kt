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

    private var _uncalibratedAccelerometer = MutableLiveData<SensorEvent>()
    val uncalibratedAccelerometer: LiveData<SensorEvent>
        get() = _uncalibratedAccelerometer

    private var _uncalibratedGyroscope = MutableLiveData<SensorEvent>()
    val uncalibratedGyroscope: LiveData<SensorEvent>
        get() = _uncalibratedGyroscope

    private var _uncalibratedMagnetoMeter = MutableLiveData<SensorEvent>()
    val uncalibratedMagnetometer: LiveData<SensorEvent>
        get() = _uncalibratedMagnetoMeter

    private var _gravity = MutableLiveData<SensorEvent>()
    val gravity = _gravity

    private var _linearAccelerometer = MutableLiveData<SensorEvent>()
    val linearAccelerometer: LiveData<SensorEvent>
        get() = _linearAccelerometer

    private var _rotationVector = MutableLiveData<SensorEvent>()
    val rotationVector: LiveData<SensorEvent>
        get() = _rotationVector

    private val sensorsList = arrayOf(
        Sensor.TYPE_ACCELEROMETER,
        Sensor.TYPE_ACCELEROMETER_UNCALIBRATED,
        Sensor.TYPE_LINEAR_ACCELERATION,
        Sensor.TYPE_GYROSCOPE,
        Sensor.TYPE_GYROSCOPE_UNCALIBRATED,
        Sensor.TYPE_MAGNETIC_FIELD,
        Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED,
        Sensor.TYPE_GRAVITY,
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
            when(event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER ->  _accelerometer.value = event
                Sensor.TYPE_ACCELEROMETER_UNCALIBRATED -> _uncalibratedAccelerometer.value = event
                Sensor.TYPE_LINEAR_ACCELERATION -> _linearAccelerometer.value = event
                Sensor.TYPE_GYROSCOPE -> _gyroscope.value = event
                Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> _uncalibratedGyroscope.value = event
                Sensor.TYPE_MAGNETIC_FIELD -> _magnetometer.value = event
                Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED -> _uncalibratedMagnetoMeter.value = event
                Sensor.TYPE_GRAVITY -> _gravity.value = event
                Sensor.TYPE_ROTATION_VECTOR -> _rotationVector.value = event
            }
//            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
//                System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
//                updateOrientationAngles(event.timestamp)
//            } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
//                System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
//                updateOrientationAngles(event.timestamp)
//            } else if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
//                System.arraycopy(event.values, 0, gyrometerReading, 0, gyrometerReading.size)
//                updateOrientationAngles(event.timestamp)
//            }
        }
    }
    // Compute the three orientation angles based on the most recent readings from
    // the device's accelerometer and magnetometer.
//    private fun updateOrientationAngles(timestamp: Long) {
//        myOrientationAccel(timestamp)
//        myOrientationWithComplementaryFilter(timestamp)
//        googleOrientation(timestamp)
//        myOrientationFMatrix(timestamp)
//        Log.i("GG rotation matrix", rotationMatrix.show())
//        Log.i("My rotation matrix", myRotationMatrix.show())
//    }



//    private fun googleOrientation(timestamp: Long) {
//        // Update rotation matrix, which is needed to update orientation angles.
//        SensorManager.getRotationMatrix(
//            rotationMatrix,
//            null,
//            accelerometerReading,
//            magnetometerReading
//        )
//
//        // "rotationMatrix" now has up-to-date information.
//        SensorManager.getOrientation(rotationMatrix, orientationAngles)
//        Log.i("GGOrient", "${orientationAngles[0]}, ${orientationAngles[1]}, ${orientationAngles[2]}, ${timestamp}")
//    }
//
//    private fun myOrientationFMatrix(timestamp: Long) {
//        getMyRotationMatrix(myRotationMatrix)
//        SensorManager.getOrientation(myRotationMatrix, myOriantaionAngles)
//        Log.i("MyOrient", "${myOriantaionAngles[0]}, ${myOriantaionAngles[1]}, ${myOriantaionAngles[2]}, ${timestamp}")
//    }

//    private fun myOrientationAccel(timestamp: Long) {
//        val pitch = atan2(accelerometerReading[1], accelerometerReading[2])*180/ PI
//        val roll = atan2(accelerometerReading[0], accelerometerReading[2])*180/ PI
//        val yaw = atan2(accelerometerReading[0], accelerometerReading[1])*180/ PI
//        Log.i("MyOrientAccel", "${pitch}, ${roll}, ${yaw}, ${timestamp}")
//    }

//    private var pitchCF = 0.0
//    private var rollCF = 0.0
//    private var yawCF = 0.0
//    private var pitchAcc = 0.0
//    private var rollAcc = 0.0
//    private var yawAcc = 0.0
//    private val gysen = 1
//    private val alpha = 0.98
//    private fun myOrientationWithComplementaryFilter(timestamp: Long) {
//        pitchAcc = atan2(accelerometerReading[1], accelerometerReading[2])*180/ PI
//        pitchCF += (gyrometerReading[0]/gysen) * dt/1000000 *180/ PI
//        pitchCF = pitchCF*alpha + pitchAcc*(1-alpha)
//        rollAcc = atan2(accelerometerReading[0], accelerometerReading[2])*180/ PI
//        rollCF += (gyrometerReading[1]/gysen) * dt/1000000 *180/ PI
//        rollCF = rollCF*alpha + rollAcc*(1-alpha)
//        yawAcc = atan2(accelerometerReading[0], accelerometerReading[1])*180/ PI
//        yawCF += (gyrometerReading[2]/gysen) * dt/1000000 *180/ PI
//        yawCF = yawCF*alpha+yawAcc*(1-alpha)
//        Log.i("MyOrientCF", "${pitchCF}, ${rollCF}, ${yawCF}, ${timestamp}")
//    }

    // Callback when sensor(s)'s accuracy change
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Timber.i("Accuracy changed")
    }

}