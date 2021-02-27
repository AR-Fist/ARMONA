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
import com.arfist.armona.services.LocationRepository
import com.arfist.armona.services.SensorsRepository
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
//    val uncalibGyrometer = sensorsRepository.uncalibratedGyroscope
//    val uncalibMagnetometer = sensorsRepository.uncalibratedMagnetometer

    private var lastTimestamp: Long = 0
    private var dt: Float = 0.0F
    private val timeConstant = 0.5
    private val epsilon: Float = 1.0E-9F
    private val nanosec2sec: Float = 1/1000000000F

    private val _complementaryAngle = MutableLiveData<FloatArray>()
    val complementaryAngle: LiveData<FloatArray>
        get() = _complementaryAngle

    fun registerSensors() = sensorsRepository.registerSensors()

    fun unregisterSensors() = sensorsRepository.unregisterSensors()

    // Log all orientation related
    @SuppressLint("LogNotTimber")
    fun getOrientation(timestamp: Long) {
        dt = (timestamp - lastTimestamp)*nanosec2sec
        Log.i("Time", "$dt $timestamp $lastTimestamp")
        lastTimestamp = timestamp
        val rotationMatrix = FloatArray(9)
        try {
            // Android's
            SensorManager.getRotationMatrix(
                rotationMatrix,
                null,
                accelerometer.value!!.values,
                magnetometer.value!!.values
            )

            val orientationAngles = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            Log.i(
                "GoogleOrientationAngles",
                "${orientationAngles[0]}, ${orientationAngles[1]}, ${orientationAngles[2]}, $timestamp"
            )

            // Implemented
            val myRotationMatrix = calculateRotationMatrix(9)
            val myRotationVector = myRotationMatrix?.let { calculateQuaternionFromRotationMatrix(it) }

            Log.i(
                "MyRotationQuaternion",
                "${myRotationVector?.x}, ${myRotationVector?.y}, ${myRotationVector?.z}, ${myRotationVector?.w}, $timestamp"
            )

            val myRotationAngle = myRotationVector?.let { quaternionToEuler(it) }
            Log.i(
                "MyRotationAngle",
                "${myRotationAngle?.get(0)}, ${myRotationAngle?.get(1)}, ${myRotationAngle?.get(2)}, $timestamp"
            )

            val myOrientationAngles = myRotationMatrix?.let { calculateMyOrientationAngle(it) }
            Log.i(
                "MyOrientationAngles",
                "${myOrientationAngles?.get(0)}, ${myOrientationAngles?.get(1)}, ${myOrientationAngles?.get(2)}, $timestamp"
            )

            val complementaryQuaternion = complementaryFilter(myRotationVector)
            Log.i(
                "MyComplementaryQuaternion",
                "${complementaryQuaternion?.x}, ${complementaryQuaternion?.y}, ${complementaryQuaternion?.z}, ${complementaryQuaternion?.w}, $timestamp"
            )

            _complementaryAngle.value = complementaryQuaternion?.let { quaternionToEuler(it) }
            Log.i(
                "MyComplementaryAngle",
                "${complementaryAngle.value!![0]}, ${complementaryAngle.value!![1]}, ${complementaryAngle.value!![2]}, $timestamp"
            )

        } catch (e: Exception) {
            Timber.e(e)
        }
    }
    private var lastRotationQuaternion = Quaternion(0F, 0F, 0F, 0F)
    private fun complementaryFilter(rotationVectorAccelerationMagnetic: Quaternion?): Quaternion? {
//        val alpha =  timeConstant / (timeConstant+dt)
        val alpha = 0.98F
        Log.i("alpha", "${alpha}, ${dt}, ${timeConstant}, ${nanosec2sec}")
        if (rotationVectorAccelerationMagnetic == null) return null
        val rotationVectorGyroscope = gyroscopeRotationQuaternion(lastRotationQuaternion) ?: return null
        Log.i("GyroscopeOnly", "${rotationVectorGyroscope.x}, ${rotationVectorGyroscope.y}, ${rotationVectorGyroscope.z}, ${rotationVectorGyroscope.w}, $lastTimestamp")

        val rotationVecGyroAngle = quaternionToEuler(rotationVectorGyroscope)
        Log.i("GyroscopeAngle", "${rotationVecGyroAngle[0]}, ${rotationVecGyroAngle[1]}, ${rotationVecGyroAngle[2]}, $lastTimestamp")
        val scaledVectorGyroscope = rotationVectorGyroscope.multiply(alpha.toFloat())
        val scaledVectorAccMag = rotationVectorAccelerationMagnetic.multiply((1-alpha).toFloat())
        val result = scaledVectorAccMag.add(scaledVectorGyroscope)
        lastRotationQuaternion = Quaternion(result.x, result.y, result.z, result.w)
        return result
    }
    private fun kalmanFilter() {

    }

    fun quaternionToEuler(quaternion: Quaternion): FloatArray {
        // Euler angles as roll, pitch, yaw
        val euler = FloatArray(3)
        val srcp = 2*(quaternion.w*quaternion.y+quaternion.x*quaternion.z)
        val crcp = 1 - 2*(quaternion.x*quaternion.x+quaternion.y*quaternion.y)
        euler[0] = atan2(srcp, crcp)

        var sp = 2*(quaternion.w*quaternion.y-quaternion.z*quaternion.x)
        sp = if(abs(sp) > 1 ) { 1F } else { -1F }
        euler[1] = asin(sp)

        val sycp = 2*(quaternion.w*quaternion.z+quaternion.x*quaternion.y)
        val cycp = 1 - 2*(quaternion.y*quaternion.y+quaternion.z*quaternion.z)
        euler[2] = atan2(sycp, cycp)
        return euler
    }

    fun gyroscopeRotationQuaternion(lastRotationVector: Quaternion): Quaternion? {
        val gyroscopeValue = gyroscope.value?.values ?: return null
        val magnitude = sqrt(gyroscopeValue[0]*gyroscopeValue[0] + gyroscopeValue[1]*gyroscopeValue[1] + gyroscopeValue[2]*gyroscopeValue[2])
        Log.i("gyro_magnitude", "$magnitude, ${gyroscopeValue[0]}, ${gyroscopeValue[1]}, ${gyroscopeValue[2]}")
        if(magnitude > epsilon) {
            gyroscopeValue[0] /= magnitude
            gyroscopeValue[1] /= magnitude
            gyroscopeValue[2] /= magnitude
        }

        val theta = magnitude * dt
        val sintheta = sin(theta/2)
        val costheta = cos(theta/2)
        Log.i("gyro_2", "${gyroscopeValue[0]}, ${gyroscopeValue[1]}, ${gyroscopeValue[2]}, $theta")
        val quaternion = Quaternion(
            sintheta*gyroscopeValue[0],
            sintheta*gyroscopeValue[1],
            sintheta*gyroscopeValue[2],
            costheta)

        return lastRotationVector.multiply(quaternion)
    }

    private fun calculateRotationMatrix(matrixSize: Int): FloatArray? {
        // TODO: in this case we will read acc as a gravity but it has to change later may use high-pass
        if(matrixSize != 9 && matrixSize != 16) return null

        val rotationMatrix = FloatArray(matrixSize)
        val up = accelerometer.value!!.values
        val magneto = magnetometer.value!!.values

        // Calculate H that orthogonal to both magneto that point to north and up(result from accelerometer as upward opposite to graviity)  vectors by cross product
        val east = magneto.cross(up) ?: return null
        east.normalize()
        up.normalize()

        // Calculate N that direct to north but orthogonal to both H and A
        val north = up.cross(east) ?: return null

        // Assign value to the matrix
        if (matrixSize == 9) {
            rotationMatrix[0] = east[0]
            rotationMatrix[1] = east[1]
            rotationMatrix[2] = east[2]
            rotationMatrix[3] = north[0]
            rotationMatrix[4] = north[1]
            rotationMatrix[5] = north[2]
            rotationMatrix[6] = up[0]
            rotationMatrix[7] = up[1]
            rotationMatrix[8] = up[2]
        } else if (matrixSize == 16) {
            rotationMatrix[0] = east[0]
            rotationMatrix[1] = east[1]
            rotationMatrix[2] = east[2]
            rotationMatrix[3] = 0F
            rotationMatrix[4] = north[0]
            rotationMatrix[5] = north[1]
            rotationMatrix[6] = north[2]
            rotationMatrix[7] = 0F
            rotationMatrix[8] = up[0]
            rotationMatrix[9] = up[1]
            rotationMatrix[10] = up[2]
            rotationMatrix[11] = 0F
            rotationMatrix[12] = 0F
            rotationMatrix[13] = 0F
            rotationMatrix[14] = 0F
            rotationMatrix[15] = 1F
        }
        return rotationMatrix
    }

    private fun calculateMyOrientationAngle(rotationMatrix: FloatArray): FloatArray {
        val angles = FloatArray(3)
        // Tait bryan angle convention
        angles[0] = atan2(rotationMatrix[1], rotationMatrix[4])
        angles[1] = asin(-rotationMatrix[7])
        angles[2] = atan2(-rotationMatrix[6], rotationMatrix[8])
        return angles
    }


    fun calculateQuaternionFromRotationMatrix(rotationMatrix: FloatArray): Quaternion? {
        if (rotationMatrix.size != 9) return null
        val w = sqrt(1 + rotationMatrix[0] + rotationMatrix[4] + rotationMatrix[8])/2
        return Quaternion(
            (rotationMatrix[7] - rotationMatrix[5]) / (4*w),
            (rotationMatrix[2] - rotationMatrix[6]) / (4*w),
            (rotationMatrix[3] - rotationMatrix[1]) / (4*w),
            w
        )
    }
}