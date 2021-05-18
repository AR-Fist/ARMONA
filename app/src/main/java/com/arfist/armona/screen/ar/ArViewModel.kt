package com.arfist.armona.screen.ar

import android.annotation.SuppressLint
import android.app.Application
import android.graphics.Bitmap
import android.hardware.SensorManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.arfist.armona.*
import com.arfist.armona.services.*
import com.arfist.armona.utils.ModelLoader
import koma.create
import koma.extensions.get
import koma.extensions.set
import koma.matrix.Matrix
import koma.zeros
import timber.log.Timber
import kotlin.math.*

class ArViewModel(application: Application) : AndroidViewModel(application) {
    class ArrowModel {
        var arrowRotation = 0f // use if rotationMode = RotationMode.EULER
        var arrowModel: ModelLoader.MeshGroup? = null
        var arrowFovy = 30f
        var arrowQuaternion = floatArrayOf(0f, 1f, 0f, 0f) // use if rotationMode = RotationMode.QUATERNION

        private val _roadLine = MutableLiveData<Array<org.opencv.core.Point>>()
        val roadLine: LiveData<Array<org.opencv.core.Point>>
        get() = _roadLine

        fun updateRoadLine(vertices: Array<android.graphics.Point>, frame: android.graphics.Point){
            val middleX = frame.x / 2.0
            val middleY = frame.y / 2.0
            _roadLine.value = vertices.map { v -> org.opencv.core.Point( (middleX - v.x) / middleX, (middleY - v.y) / middleY) }.toTypedArray()
        }
    }
    val arrowModel = ArrowModel()

    class ScreenModel {
        var screenRatio = 0.0f
    }
    val screenModel = ScreenModel()

    class CameraModel {
        private val _liveViewBitmap = MutableLiveData<Bitmap>()
        val liveViewBitmap: LiveData<Bitmap>
            get() = _liveViewBitmap

        fun setLiveViewBitmap(bitmap: Bitmap) {
            _liveViewBitmap.value = bitmap
        }
    }
    val cameraModel = CameraModel()

    // Repository
    private val sensorsRepository = SensorsRepository.getInstance(application.applicationContext)
    private val locationRepository = LocationRepository.getInstance(application.applicationContext)

    // Base sensors
    val accelerometer = sensorsRepository.accelerometer
    val gyroscope = sensorsRepository.gyroscope
    val magnetometer = sensorsRepository.magnetometer

    // Software sensors
    val rotationVector = sensorsRepository.rotationVector

    // Filters
    private val complementaryFilter = ComplementaryFilterRotation(0.98F)
    private val complementaryFilterGravity = ComplementaryFilterRotation(0.98F)
    private val extendedKalmanFilter = ExtendedKalmanFilter()

    // Constant
    private var lastTimestamp: Long = 0
    private var dt: Float = 0.0F
    private val nanosec2sec: Float = 1/1000000000F

    private val _complementaryAngle = MutableLiveData<FloatArray>()
    val complementaryAngle: LiveData<FloatArray>
        get() = _complementaryAngle

    private val _extendedKalman = MutableLiveData<Quaternion>()
    val extendedKalman: LiveData<Quaternion>
        get() = _extendedKalman

    private val _extendedKalmanAngle = MutableLiveData<FloatArray>()
    val extendedKalmanAngle: LiveData<FloatArray>
        get() = _extendedKalmanAngle

    private val _mGoogleOrientation = MutableLiveData<FloatArray>()
    val mGoogleOrientation: LiveData<FloatArray>
        get() = _mGoogleOrientation

    private val _myOrientationAngle = MutableLiveData<FloatArray>()
    val myOrientationAngle: LiveData<FloatArray>
        get() = _myOrientationAngle

    // Arrow rotation (The output that we need)
    private val _arrowRotation = MutableLiveData<FloatArray>()
    val arrowRotation: LiveData<FloatArray>
        get() = _arrowRotation

    private val _arrowRotationSlerp = MutableLiveData<FloatArray>()
    val arrowRotationSlerp: LiveData<FloatArray>
        get() = _arrowRotationSlerp


    fun registerSensors() = sensorsRepository.registerSensors()

    fun unregisterSensors() = sensorsRepository.unregisterSensors()

    lateinit var myRotationMatrix: Matrix<Double>
    var myRotationVector = Quaternion(1.0f, 0f, 0f, 0f)

    private val _testAndroidRotationMatrix = MutableLiveData<FloatArray>()
    val testAndroidRotationMatrix: LiveData<FloatArray>
        get() = _testAndroidRotationMatrix

    // Calculate every solution for rotation
    fun getOrientation(timestamp: Long) {
//        Timber.i("GetOrientation")
        dt = (timestamp - lastTimestamp)*nanosec2sec
        lastTimestamp = timestamp
        val androidRotationMatrix = FloatArray(16 )
        try {
            // Android's
            SensorManager.getRotationMatrix(
                androidRotationMatrix,
                null,
                accelerometer.value!!.values,
                magnetometer.value!!.values
            )

            _testAndroidRotationMatrix.value = androidRotationMatrix
            val orientationAngles = FloatArray(3)
            SensorManager.getOrientation(androidRotationMatrix, orientationAngles)
            _mGoogleOrientation.value = orientationAngles + floatArrayOf(timestamp.toFloat())
        } catch (e: Exception) {
            Timber.e(e)
        }
        try {
            // Implemented
            // Orientation from only accelerometer and magnetometer
            myRotationMatrix = calculateRotationMatrix(accelerometer.value!!.values, magnetometer.value!!.values)
            myRotationVector = Quaternion.FromRotationMatrix(myRotationMatrix)
            val myOrientationAngles = calculateMyOrientationAngle(myRotationMatrix)

            _myOrientationAngle.value = myOrientationAngles + floatArrayOf(timestamp.toFloat())
            // Add gyroscope
            gyroscope.value?.values?.let {
                val gyroscopeDouble = it.toDouble()
                complementaryFilter.filter(it, myRotationVector, dt)

                extendedKalmanFilter.predict(gyroscopeDouble, dt)
                extendedKalmanFilter.update((accelerometer.value!!.values).toDouble(), (magnetometer.value!!.values).toDouble())
            }
            val xHat = extendedKalmanFilter.xHat[0..3, 0]
            _extendedKalman.value = Quaternion(xHat[0].toFloat(), xHat[1].toFloat(), xHat[2].toFloat(), xHat[3].toFloat())
            _extendedKalmanAngle.value = _extendedKalman.value!!.toEuler() + floatArrayOf(timestamp.toFloat())

            _complementaryAngle.value = complementaryFilter.rotationQuaternion.toEuler() + floatArrayOf(timestamp.toFloat())

            calculateArrowRotation()
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

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

    private fun calculateMyOrientationAngle(rotationMatrix: Matrix<Double>): FloatArray {
        val angles = FloatArray(3)
        // Tait bryan angle convention
        angles[0] = atan2(rotationMatrix[0, 1], rotationMatrix[1, 1]).toFloat()
        angles[1] = asin(-rotationMatrix[2, 1]).toFloat()
        angles[2] = atan2(-rotationMatrix[2, 0], rotationMatrix[2, 2]).toFloat()
        return angles
    }

    var arrowRotationLocal = Quaternion(1f, 0f, 0f, 0f)
    fun calculateArrowRotation() {
        /**
         * This method will be call every sensor update loop ie gyroscope, magnetometer,
         * accelerometer and GPS only in arviewmodel not in mapviewmodel as it not need this data
         *
         * For the arrow rotation
         * 1. the vector is heading north in the world frame(y axis)
         * 2. the vector will rotate around z axis as bearing degree
         * 3. apply the rotation of phone in order to change from world frame to local frame
         *
         * The (2) will calculated by location lat lng
         * The (3) will calculated by sensors with some filter
         */
        val bearing = locationRepository.getBearingToNextPosition()
        val degree = locationRepository.BearingToDegree(bearing)
        // Rotate arrow from north cw to east as bearing degree converted to quaternion then apply with the quaternion calculated
        val arrowRotationWorld = Quaternion.FromEuler(floatArrayOf(degree.DegToRad(), 0f, 0f))

//        val arrowRotationLocalNew = arrowRotationWorld*myRotationVector
//        val arrowRotationLocalNew = arrowRotationWorld*complementaryFilter.rotationQuaternion
        val arrowRotationLocalNew = arrowRotationWorld*extendedKalman.value!!
        arrowRotationLocal = arrowRotationLocal.slerp(arrowRotationLocalNew, 0.05f)
        _arrowRotationSlerp.value = arrowRotationLocal.toEuler() + floatArrayOf(lastTimestamp.toFloat())
        _arrowRotation.value = arrowRotationLocalNew.toEuler() + floatArrayOf(lastTimestamp.toFloat())
    }
}