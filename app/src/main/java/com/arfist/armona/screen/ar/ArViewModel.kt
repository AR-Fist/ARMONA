package com.arfist.armona.screen.ar

import android.annotation.SuppressLint
import android.app.Application
import android.hardware.SensorManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.arfist.armona.Quaternion
import com.arfist.armona.cross
import com.arfist.armona.normalize
import com.arfist.armona.services.*
import com.arfist.armona.toDouble
import koma.create
import koma.extensions.get
import koma.extensions.set
import koma.matrix.Matrix
import koma.zeros
import timber.log.Timber
import kotlin.math.*

class ArViewModel(application: Application) : AndroidViewModel(application) {

    // Repository
    private val sensorsRepository = SensorsRepository.getInstance(application.applicationContext)
    private val locationRepository = LocationRepository.getInstance(application.applicationContext)

    // Base sensors
    val accelerometer = sensorsRepository.accelerometer
    val gyroscope = sensorsRepository.gyroscope
    val magnetometer = sensorsRepository.magnetometer

    // Software sensors
    val gravity = sensorsRepository.gravity
    val rotationVector = sensorsRepository.rotationVector
    //    val linearAccelerometer = sensorsRepository.linearAccelerometer

    // Base sensors but uncalibrated
//    val uncalibAccelerometer = sensorsRepository.uncalibratedAccelerometer
//    val uncalibGyroscope = sensorsRepository.uncalibratedGyroscope
//    val uncalibMagnetometer = sensorsRepository.uncalibratedMagnetometer

    val location = locationRepository.currentLocation
    // Filters
    private val complementaryFilter = ComplementaryFilter(0.98F)
    private val complementaryFilterGravity = ComplementaryFilter(0.98F)
//    private val kalmanFilter1D = KalmanFilter1D()
    private val extendedKalmanFilter = ExtendedKalmanFilter()
    private var lastTimestamp: Long = 0
    private var dt: Float = 0.0F
    private val nanosec2sec: Float = 1/1000000000F

    private val _complementaryAngle = MutableLiveData<FloatArray>()
    val complementaryAngle: LiveData<FloatArray>
        get() = _complementaryAngle

    private val _extendedKalman = MutableLiveData<FloatArray>()
    val extendedKalman: LiveData<FloatArray>
        get() = _extendedKalman

    private val _mGoogleOrientation = MutableLiveData<FloatArray>()
    val mGoogleOrientation: LiveData<FloatArray>
        get() = _mGoogleOrientation

    private val _arrowRotation = MutableLiveData<FloatArray>()
    val arrowRotation: LiveData<FloatArray>
        get() = _arrowRotation

    fun registerSensors() = sensorsRepository.registerSensors()

    fun unregisterSensors() = sensorsRepository.unregisterSensors()

    // Log all orientation related
    @SuppressLint("LogNotTimber")
    fun getOrientation(timestamp: Long) {
        dt = (timestamp - lastTimestamp)*nanosec2sec
        Log.i("Time", "$dt $timestamp $lastTimestamp")
        lastTimestamp = timestamp
        val androidRotationMatrix = FloatArray(9)
        Log.i(
            "CurrentLocation",
            "${location.value}, $timestamp"
        )
        try {
            // Android's
            SensorManager.getRotationMatrix(
                androidRotationMatrix,
                null,
                accelerometer.value!!.values,
                magnetometer.value!!.values
            )

            val orientationAngles = FloatArray(3)
            SensorManager.getOrientation(androidRotationMatrix, orientationAngles)
            _mGoogleOrientation.value = orientationAngles + floatArrayOf(timestamp.toFloat())
        } catch (e: Exception) {
            Timber.e(e)
        }
        try {
            // Implemented
            val myRotationMatrix = calculateRotationMatrix(accelerometer.value!!.values, magnetometer.value!!.values)
            val myRotationVector = calculateQuaternionFromRotationMatrix(myRotationMatrix)
            val myOrientationAngles = calculateMyOrientationAngle(myRotationMatrix)

            val myGravityRotationMatrix = calculateRotationMatrix(gravity.value!!.values, magnetometer.value!!.values)
            val myGravityRotationVector = calculateQuaternionFromRotationMatrix(myGravityRotationMatrix)
            val myGravityOrientationAngles = calculateMyOrientationAngle(myGravityRotationMatrix)

            gyroscope.value?.values?.let {
                val gyroscopeDouble = it.toDouble()
                complementaryFilter.filter(it, myRotationVector, dt)
                complementaryFilterGravity.filter(it, myGravityRotationVector, dt)

                extendedKalmanFilter.predict(gyroscopeDouble, dt)
                extendedKalmanFilter.update((accelerometer.value!!.values).toDouble(), (magnetometer.value!!.values).toDouble())
            }
            val xHat = extendedKalmanFilter.xHat[0..3, 0]
            _extendedKalman.value = Quaternion(xHat[0].toFloat(), xHat[1].toFloat(), xHat[2].toFloat(), xHat[3].toFloat()).toEuler() + floatArrayOf(timestamp.toFloat())

            _complementaryAngle.value = complementaryFilter.rotationQuaternion.toEuler() + floatArrayOf(timestamp.toFloat())
            calculateArrowRotation()

        } catch (e: Exception) {
            Timber.e(e)
        }
    }

//    fun gyroscopeRotationQuaternion(lastRotationVector: Quaternion): Quaternion? {
//        val gyroscopeValue = gyroscope.value?.values ?: return null
//        val magnitude = sqrt(gyroscopeValue[0]*gyroscopeValue[0] + gyroscopeValue[1]*gyroscopeValue[1] + gyroscopeValue[2]*gyroscopeValue[2])
//        Log.i("gyro_magnitude", "$magnitude, ${gyroscopeValue[0]}, ${gyroscopeValue[1]}, ${gyroscopeValue[2]}")
//        if(magnitude > epsilon) {
//            gyroscopeValue[0] /= magnitude
//            gyroscopeValue[1] /= magnitude
//            gyroscopeValue[2] /= magnitude
//        }
//
//        val theta = magnitude * dt
//        val sintheta = sin(theta/2)
//        val costheta = cos(theta/2)
//        Log.i("gyro_2", "${gyroscopeValue[0]}, ${gyroscopeValue[1]}, ${gyroscopeValue[2]}, $theta")
//        val quaternion = Quaternion(
//            sintheta*gyroscopeValue[0],
//            sintheta*gyroscopeValue[1],
//            sintheta*gyroscopeValue[2],
//            costheta)
//
//        return lastRotationVector * quaternion
//    }

    private fun calculateRotationMatrix(gravity: FloatArray, magnetometer: FloatArray): Matrix<Double> {
        // TODO: in this case we will read acc as a gravity but it has to change later may use high-pass
        val rotationMatrix = zeros(3, 3)
        val up = gravity
        val magneto = magnetometer

        // Calculate H that orthogonal to both magneto that point to north and up(result from accelerometer as upward opposite to graviity)  vectors by cross product
        val east = magneto.cross(up)
        east.normalize()
        up.normalize()

        // Calculate N that direct to north but orthogonal to both H and A
        val north = up.cross(east)

        // Assign value to the matrix
        rotationMatrix[0, 0..2] = create(doubleArrayOf(east[0].toDouble(), east[1].toDouble(), east[2].toDouble()))
        rotationMatrix[1, 0..2] = create(doubleArrayOf(north[0].toDouble(), north[1].toDouble(), north[2].toDouble()))
        rotationMatrix[2, 0..2] = create(doubleArrayOf(up[0].toDouble(), up[1].toDouble(), up[2].toDouble()))
        return rotationMatrix
    }

    private fun calculateMyOrientationAngle(rotationMatrix: Matrix<Double>): DoubleArray {
        val angles = DoubleArray(3)
        // Tait bryan angle convention
        angles[0] = atan2(rotationMatrix[0, 1], rotationMatrix[1, 1])
        angles[1] = asin(-rotationMatrix[2, 1])
        angles[2] = atan2(-rotationMatrix[2, 0], rotationMatrix[2, 2])
        return angles
    }


    fun calculateQuaternionFromRotationMatrix(rotationMatrix: Matrix<Double>): Quaternion {
        val w = sqrt(1 + rotationMatrix[0, 0] + rotationMatrix[1, 1] + rotationMatrix[2, 2])/2
        return Quaternion(
            w.toFloat(),
            ((rotationMatrix[2, 1] - rotationMatrix[1, 2]) / (4*w)).toFloat(),
            ((rotationMatrix[0, 2] - rotationMatrix[2, 0]) / (4*w)).toFloat(),
            ((rotationMatrix[1, 0] - rotationMatrix[0, 1]) / (4*w)).toFloat()
        )
    }
    fun calculateArrowRotation() {
        /**
         * This method will be call every sensor update loop ie gyroscope, magnetometer,
         * accelerometer and GPS only in arviewmodel not in map viewmodel as mapviewmodel
         * not need this data
         *
         * For the arrow rotation
         * 1. the vector is heading north in the world frame(y axis)
         * 2. the vector will rotate around z axis as bearing degree
         * 3. apply the rotation of phone in order to change from world fram to local frame
         *
         * The (2) will calculated by location lat lng
         * The (3) will calculated by sensors with some filter
         */
        val bearing = locationRepository.getBearingToNextPosition()
        // Rotate arrow from north cw to east as bearing degree converted to quaternion then apply with the quaternion calculated
        val arrowRotationWorld = Quaternion(cos((360-bearing)/2), 0f, 0f, sin((360-bearing)/2))
        // This is the rotation of the arrow in the local as quaternion
        val arrowRotationLocal = arrowRotationWorld*complementaryFilter.rotationQuaternion
        // This return euler rotation as array of float with size 3 respect to yaw, pitch and roll
        _arrowRotation.value = arrowRotationLocal.toEuler() + floatArrayOf(lastTimestamp.toFloat())
    }
}