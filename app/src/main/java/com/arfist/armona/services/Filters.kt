package com.arfist.armona.services

import com.arfist.armona.Quaternion
import koma.*
import koma.extensions.get
import koma.extensions.set
import koma.matrix.Matrix
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class ComplementaryFilter(val alpha: Float) {
    var rotationQuaternion = Quaternion(1F, 0F, 0F, 0F)
    private val epsilon: Float = 1.0E-9F
    private val timeConstant = 0.5f

    fun filter(gyroscopeData: FloatArray, accelerometerMagnetometerRotation: Quaternion, deltaTime: Float ) {
//        val alpha =  timeConstant / (timeConstant+dt)
        val gyroscopeRotation = getGyroscopeRotation(gyroscopeData, deltaTime)
        val rotationVectorGyroscope = rotationQuaternion*gyroscopeRotation
        rotationQuaternion = ((accelerometerMagnetometerRotation * ((1-alpha).toFloat())) + (rotationVectorGyroscope * alpha.toFloat())).getNormalize()
    }

    private fun getGyroscopeRotation(gyroscopeData: FloatArray, deltaTime: Float): Quaternion {
        val magnitude =sqrt(gyroscopeData[0] * gyroscopeData[0] + gyroscopeData[1] * gyroscopeData[1] + gyroscopeData[2] * gyroscopeData[2])
        if(magnitude > epsilon) {
            gyroscopeData[0] /= magnitude
            gyroscopeData[1] /= magnitude
            gyroscopeData[2] /= magnitude
        }

        val theta = magnitude * deltaTime
        val sintheta = sin(theta / 2)
        val costheta = cos(theta / 2)
        return Quaternion(
            costheta,
            sintheta*gyroscopeData[0],
            sintheta*gyroscopeData[1],
            sintheta*gyroscopeData[2])
    }
}

class KalmanFilter1D(private val R: Float,
                   private val Q: Float,
                   private val A: Float = 1.0f,
                   private val B: Float = 0.0f,
                   private val C: Float = 1.0f) {

    /**
     * A is a State transition matrix
     * B is Control matrix
    */
    private var x: Float? = null
    private var cov: Float = 0.0f

    private fun square(x: Float) = x * x

    private fun predict(x: Float, u: Float): Float = (A * x) + (B * u)

    private fun uncertainty(): Float = (square(A) * cov) + R

    fun filter(signal: Float, u: Float = 0.0f): Float {
        val x: Float? = this.x

        if (x == null) {
            this.x = (1 / C) * signal
            cov = square(1 / C) * Q
        } else {
            val prediction: Float = predict(x, u)
            val uncertainty: Float = uncertainty()

            // kalman gain
            val k_gain: Float = uncertainty * C * (1 / ((square(C) * uncertainty) + Q))

            // correction
            this.x = prediction + k_gain * (signal - (C * prediction))
            cov = uncertainty - (k_gain * C * uncertainty)
        }

        return this.x!!
    }
}

class ExtendedKalmanFilter() {
    var xHat = create(doubleArrayOf(1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)).transpose()
    var xHatBar = zeros(7, 1)
    var xHatPrev = zeros(7, 1)
    var yHatBar = zeros(6, 1)
    var pBar = zeros(7, 7)
    var Sq = zeros(4, 3)
    var A = eye(7)
    var B = zeros(7, 3)
    var C = zeros(6, 7)
    var P = eye(7)
    var Q = eye(7)
    var R = eye(6)
    var K = zeros(7, 6)
    val magReference = mat[0, -1, 0].transpose()
    val accelReference = mat[0, 0, -1].transpose()

    fun predict(angular: DoubleArray, deltaTime: Float) {
        val deltaTime = deltaTime.toDouble()
        Sq = mat[-xHat[1], -xHat[2], -xHat[3] end
                -xHat[0], -xHat[3], xHat[2] end
                xHat[3], xHat[0], -xHat[1] end
                -xHat[2], xHat[1], xHat[0]]
        A = eye(7)
        A[0..2, 4..6] = (Sq* -deltaTime /2)
        B[0..3, 0..2] = (Sq* deltaTime /2)

        // State extrapolation
        xHatBar = A*xHat + B*create(angular).transpose()

        // Assign prior quat
        xHatBar[0..3, 0] = normalizeQuaternion(xHatBar[0..3, 0])
        xHatPrev = xHat

        yHatBar = predictAccelMag()

        //
        pBar = (A*P * A.transpose()) + Q

    }

    fun update(accelometerData: DoubleArray, magnetoMeterData: DoubleArray) {
        K = pBar*C.transpose()*(C*pBar*C.transpose() + R).inv()
        val m = create(doubleArrayOf(accelometerData[0], accelometerData[1], accelometerData[2], magnetoMeterData[0], magnetoMeterData[1], magnetoMeterData[2])).transpose()
        xHat = xHatBar + K*(m - yHatBar)
        xHat[0..3, 0] = normalizeQuaternion(xHat[0..3, 0])

        P = (eye(7) - (K*C))*pBar
    }

    private fun normalizeQuaternion(matrix: Matrix<Double>): Matrix<Double> {
        val magnitude = matrix[0]*matrix[0] + matrix[1]*matrix[1] + matrix[2]*matrix[2] + matrix[3]*matrix[3]
        return matrix/magnitude
    }

    private fun predictAccelMag(): Matrix<Double> {
        val rotMat = getRotMat(xHatBar).transpose()
        val hprime_a  = jacobianMatrix(magReference)
        val accelBar = rotMat*accelReference

        val hprime_m = jacobianMatrix(magReference)
        val magBar = rotMat*magReference

        C[0..2, 0..3] = hprime_a
        C[3..5, 0..3] = hprime_m

        val out = zeros(6, 1)
        out[0..2, 0] = accelBar
        out[3..5, 0] = magBar
        return out
    }

    private fun jacobianMatrix(ref: Matrix<Double>): Matrix<Double> {
        return mat[xHatPrev[0]*ref[0] + xHatPrev[3]*ref[1] - xHatPrev[2]*ref[2], xHatPrev[1]*ref[0] + xHatPrev[2]*ref[1] + xHatPrev[3]*ref[2], -xHatPrev[2]*ref[0] + xHatPrev[1]*ref[1] - xHatPrev[0]*ref[2], -xHatPrev[3]*ref[0] + xHatPrev[0]*ref[1] + xHatPrev[1]*ref[2] end
                -xHatPrev[3]*ref[0] + xHatPrev[0]*ref[1] + xHatPrev[1]*ref[2], xHatPrev[2]*ref[0] - xHatPrev[1]*ref[1] + xHatPrev[0]*ref[2], xHatPrev[1]*ref[0] + xHatPrev[2]*ref[1] + xHatPrev[3]*ref[2], -xHatPrev[0]*ref[0] - xHatPrev[3]*ref[1] + xHatPrev[2]*ref[2] end
                xHatPrev[2]*ref[0] - xHatPrev[1]*ref[1] + xHatPrev[0]*ref[2], xHatPrev[3]*ref[0] - xHatPrev[0]*ref[1] - xHatPrev[1]*ref[2], xHatPrev[0]*ref[0] + xHatPrev[3]*ref[1] - xHatPrev[2]*ref[2], xHatPrev[1]*ref[0] + xHatPrev[2]*ref[1] + xHatPrev[3]*ref[2]] *2.0
    }

    private fun getRotMat(q: Matrix<Double>): Matrix<Double> {
        return mat[q[0]*q[0] + q[1]*q[1] - q[2]*q[2] - q[3]*q[3], 2*(q[1]*q[2] - q[0]*q[3]), 2*(q[1]*q[3] + q[0]*q[2]) end
                2*(q[1]*q[2] + q[0]*q[3]), q[0]*q[0] - q[1]*q[1] + q[2]*q[2] - q[3]*q[3], 2*(q[2]*q[3] - q[0]*q[1]) end
                2*(q[1]*q[3] - q[0]*q[2]), 2*(q[2]*q[3] + q[0]*q[1]), q[0]*q[0] - q[1]*q[1] - q[2]*q[2] + q[3]*q[3]]
    }
}

class MadKalmanFilter(
    val stateDimension: Int,
    val measureDimension: Int,
    val controlDimension: Int
) {
    // State-transition
    var Fk = zeros(stateDimension, stateDimension)
    // Observation
    var Hk = zeros(measureDimension, measureDimension)
    // Control
    var Bk = zeros(stateDimension, controlDimension)
    // Observation noise covariance
    var Rk = zeros(measureDimension, measureDimension)
    // Process noise covariance
    var Qk = zeros(stateDimension, stateDimension)

    // Control vector = gyro
    var Uk = zeros(controlDimension, 1)
    // Measurement = accel+magneto
    var Zk = zeros(measureDimension, 1)

    // Updated current state
    var Xk_k = zeros(stateDimension, 1)
    // Predicted state estimate
    var Xk_km1 = zeros(stateDimension, 1)
    // Estimate covariance
    var Pk_k = zeros(stateDimension, stateDimension)
    // Predicted estimate covariance
    var Pk_km1 = zeros(stateDimension, stateDimension)

    // Kalman gain
    var Kk = zeros(stateDimension, measureDimension)

    fun predict() {
        Xk_km1 = Fk*Xk_k + Bk*Uk
        Pk_km1 = Fk*Pk_k*Fk.transpose() + Qk
    }
    fun update() {
        Kk = Pk_km1*Hk.transpose()*(Rk + Hk*Pk_km1*Hk.transpose()).inv()
        Xk_k = Xk_km1 + Kk*(Zk - Hk*Xk_km1)
        Pk_k = (eye(stateDimension, measureDimension) - Kk*Hk)*Pk_km1
    }
}

class OrientationKalmanFilter() {

}
