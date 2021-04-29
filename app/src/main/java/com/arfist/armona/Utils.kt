package com.arfist.armona

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.app.ActivityCompat
import koma.extensions.get
import koma.matrix.Matrix
import kotlin.math.*

fun Context.hasPermission(permission: String): Boolean {
    return ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

fun Location.getStringFormat(): String {
    return "${this.latitude},${this.longitude}"
}

fun FloatArray.cross(other: FloatArray): FloatArray {
    val res = FloatArray(3)
    res[0] = this[1]*other[2] - this[2]*other[1]
    res[1] = this[2]*other[0] - this[0]*other[2]
    res[2] = this[0]*other[1] - this[1]*other[0]
    return res
}

fun FloatArray.normalize() : Boolean {
    if(this.size != 3) return false
    val inv = 1/ sqrt(this[0]*this[0]+this[1]*this[1]+this[2]*this[2])
    this[0] = this[0]*inv
    this[1] = this[1]*inv
    this[2] = this[2]*inv
    return true
}
fun FloatArray.toDouble() : DoubleArray {
    val result = DoubleArray(this.size)
    for (i in this.indices) {
        result[i] = this[i].toDouble()
    }
    return result
}

fun Float.DegToRad(): Float {
    return (this*PI/180).toFloat()
}

fun Float.RadToDeg(): Float {
    return (this*180/ PI).toFloat()
}

fun Double.DegToRad(): Double {
    return this*PI/180
}

fun Double.RadToDeg(): Double {
    return this*180/PI
}

class Quaternion(
    val w: Float,
    val x: Float,
    val y: Float,
    val z: Float
    ) {
    private val EPSILON = 1e-4
    operator fun plus(other: Quaternion): Quaternion {
        return Quaternion(
            this.w + other.w,
            this.x + other.x,
            this.y + other.y,
            this.z + other.z
        )
    }

    operator fun minus(other: Quaternion): Quaternion {
        return this+(other*-1f)
    }

    operator fun times(scalar: Float): Quaternion{
        return Quaternion(
            this.w*scalar,
            this.x*scalar,
            this.y*scalar,
            this.z*scalar
        )
    }
    operator fun times(other: Quaternion): Quaternion {
        return Quaternion(
            this.w*other.w - this.x*other.x - this.y*other.y - this.z*other.z,
            this.x*other.w + this.w*other.x - this.z*other.y + this.y*other.z,
            this.y*other.w + this.z*other.x + this.w*other.y - this.x*other.z,
            this.z*other.w - this.y*other.x + this.x*other.y + this.w*other.z
        )
    }

    fun getNormalize(): Quaternion {
//        val magnitude: Float = this.w*this.w + this.x*this.x + this.y*this.y + this.z*this.z
        val magnitude = length()
        return Quaternion(
            this.w/magnitude,
            this.x/magnitude,
            this.y/magnitude,
            this.z/magnitude
        )
    }

    fun inverse(): Quaternion {
        return Quaternion(
            this.w,
            -this.x,
            -this.y,
            -this.z
        )
    }

    fun dot(other: Quaternion): Float {
        return this.w*other.w + this.x*other.x + this.y*other.y + this.z*other.z
    }

    fun lengthSquare() = dot(this)

    fun length() = sqrt(lengthSquare())

    fun slerp(other: Quaternion, t: Float): Quaternion {
        val magnitude = sqrt(lengthSquare() * other.lengthSquare())
        if (magnitude <= 0f) {
            return this
        }

        val product = abs(dot(other) / magnitude)

        return if (product < (1f - EPSILON)) {
            // Take care of long angle case see http://en.wikipedia.org/wiki/Slerp
            val theta = acos(product)
            val d = sin(theta)

            val sign = if (product < 0) -1f else 1f
            val s0 = sin((1f - t) * theta) / d
            val s1 = sin(sign * t * theta) / d
            Log.i("Slerp", "Hello")
            this*s0 + other*s1
        } else this
    }

    companion object {
        fun FromRotationMatrix(rotationMatrix: Matrix<Double>): Quaternion {
            // From wiki
            val trace = rotationMatrix[0, 0] + rotationMatrix[1, 1] + rotationMatrix[2, 2]
            if (trace > 0) {
                val w = sqrt(1 + trace) / 2
                return Quaternion(
                    w.toFloat(),
                    ((rotationMatrix[2, 1] - rotationMatrix[1, 2]) / (4 * w)).toFloat(),
                    ((rotationMatrix[0, 2] - rotationMatrix[2, 0]) / (4 * w)).toFloat(),
                    ((rotationMatrix[1, 0] - rotationMatrix[0, 1]) / (4 * w)).toFloat()
                )
            } else if (rotationMatrix[0, 0] > rotationMatrix[1, 1] && rotationMatrix[0, 0] > rotationMatrix[2, 2]) {
                val w =
                    sqrt(1 + rotationMatrix[0, 0] - rotationMatrix[1, 1] - rotationMatrix[2, 2]) / 2
                return Quaternion(
                    ((rotationMatrix[2, 1] - rotationMatrix[1, 2]) / (4 * w)).toFloat(),
                    w.toFloat(),
                    ((rotationMatrix[0, 1] + rotationMatrix[1, 0]) / (4 * w)).toFloat(),
                    ((rotationMatrix[0, 2] + rotationMatrix[2, 0]) / (4 * w)).toFloat()
                )
            } else if (rotationMatrix[1, 1] > rotationMatrix[2, 2]) {
                val w =
                    sqrt(1 - rotationMatrix[0, 0] + rotationMatrix[1, 1] - rotationMatrix[2, 2]) / 2
                return Quaternion(
                    ((rotationMatrix[0, 2] - rotationMatrix[2, 0]) / (4 * w)).toFloat(),
                    ((rotationMatrix[0, 1] + rotationMatrix[1, 0]) / (4 * w)).toFloat(),
                    w.toFloat(),
                    ((rotationMatrix[1, 2] + rotationMatrix[2, 1]) / (4 * w)).toFloat(),
                )
            } else {
                val w =
                    sqrt(1 - rotationMatrix[0, 0] - rotationMatrix[1, 1] + rotationMatrix[2, 2]) / 2
                return Quaternion(
                    ((rotationMatrix[1, 0] - rotationMatrix[0, 1]) / (4 * w)).toFloat(),
                    ((rotationMatrix[0, 2] + rotationMatrix[2, 0]) / (4 * w)).toFloat(),
                    ((rotationMatrix[1, 2] + rotationMatrix[2, 1]) / (4 * w)).toFloat(),
                    w.toFloat()
                )
            }
        }

        fun FromRotationMatrix2(rotationMatrix: Matrix<Double>): Quaternion {
            // From cloud
            val trace = rotationMatrix[0, 0] + rotationMatrix[1, 1] + rotationMatrix[2, 2]
            if (trace > 0) {
                val w = sqrt(1 + trace) / 2
                return Quaternion(
                    w.toFloat(),
                    ((rotationMatrix[1, 2] - rotationMatrix[2, 1]) / (4 * w)).toFloat(),
                    ((rotationMatrix[2, 0] - rotationMatrix[0, 2]) / (4 * w)).toFloat(),
                    ((rotationMatrix[0, 1] - rotationMatrix[1, 0]) / (4 * w)).toFloat()
                )
            } else if (rotationMatrix[0, 0] > rotationMatrix[1, 1] && rotationMatrix[0, 0] > rotationMatrix[2, 2]) {
                val w =
                    sqrt(1 + rotationMatrix[0, 0] - rotationMatrix[1, 1] - rotationMatrix[2, 2]) / 2
                return Quaternion(
                    ((rotationMatrix[1, 2] - rotationMatrix[2, 1]) / (4 * w)).toFloat(),
                    w.toFloat(),
                    ((rotationMatrix[1, 0] + rotationMatrix[0, 1]) / (4 * w)).toFloat(),
                    ((rotationMatrix[2, 0] + rotationMatrix[0, 2]) / (4 * w)).toFloat()
                )
            } else if (rotationMatrix[1, 1] > rotationMatrix[2, 2]) {
                val w =
                    sqrt(1 - rotationMatrix[0, 0] + rotationMatrix[1, 1] - rotationMatrix[2, 2]) / 2
                return Quaternion(
                    ((rotationMatrix[2, 0] - rotationMatrix[0, 2]) / (4 * w)).toFloat(),
                    ((rotationMatrix[1, 0] + rotationMatrix[0, 1]) / (4 * w)).toFloat(),
                    w.toFloat(),
                    ((rotationMatrix[2, 1] + rotationMatrix[1, 2]) / (4 * w)).toFloat(),
                )
            } else {
                val w =
                    sqrt(1 - rotationMatrix[0, 0] - rotationMatrix[1, 1] + rotationMatrix[2, 2]) / 2
                return Quaternion(
                    ((rotationMatrix[0, 1] - rotationMatrix[1, 0]) / (4 * w)).toFloat(),
                    ((rotationMatrix[2, 0] + rotationMatrix[0, 2]) / (4 * w)).toFloat(),
                    ((rotationMatrix[2, 1] + rotationMatrix[1, 2]) / (4 * w)).toFloat(),
                    w.toFloat()
                )
            }
        }

        fun FromRotationMatrix3(rotationMatrix: Matrix<Double>): Quaternion {
            var t = 0.0
            var quat = Quaternion(1f, 0f, 0f, 0f)
            if (rotationMatrix[2, 2] < 0) {
                if (rotationMatrix[0, 0] > rotationMatrix[1, 1]) {
                    t = 1 + rotationMatrix[0, 0] - rotationMatrix[1, 1] - rotationMatrix[2, 2]
                    quat =  Quaternion(
                        (rotationMatrix[1, 2] - rotationMatrix[2, 1]).toFloat(),
                        t.toFloat(),
                        (rotationMatrix[0, 1] + rotationMatrix[1, 0]).toFloat(),
                        (rotationMatrix[2, 0] + rotationMatrix[0, 2]).toFloat())
                } else {
                    t = 1 - rotationMatrix[0, 0] + rotationMatrix[1, 1] - rotationMatrix[2, 2]
                    quat = Quaternion(
                        (rotationMatrix[2, 0] - rotationMatrix[0, 2]).toFloat(),
                        (rotationMatrix[0, 1] + rotationMatrix[1, 0]).toFloat(),
                        t.toFloat(),
                        (rotationMatrix[1, 2] + rotationMatrix[2, 1]).toFloat()
                    )
                }
            } else {
                if (rotationMatrix[0, 0] < -rotationMatrix[1, 1]) {
                    t = 1 - rotationMatrix[0, 0] - rotationMatrix[1, 1] + rotationMatrix[2, 2]
                    quat = Quaternion(
                        (rotationMatrix[0, 1] - rotationMatrix[1, 0]).toFloat(),
                        (rotationMatrix[2, 0] + rotationMatrix[0, 2]).toFloat(),
                        (rotationMatrix[1, 2] + rotationMatrix[2, 1]).toFloat(),
                        t.toFloat(),
                    )
                } else {
                    t = 1 + rotationMatrix[0, 0] + rotationMatrix[1, 1] + rotationMatrix[2, 2]
                    quat = Quaternion(
                        t.toFloat(),
                        (rotationMatrix[1, 2] - rotationMatrix[2, 1]).toFloat(),
                        (rotationMatrix[2, 0] - rotationMatrix[0, 2]).toFloat(),
                        (rotationMatrix[0, 1] - rotationMatrix[1,0]).toFloat()
                    )
                }
            }
            return quat * (0.5 / sqrt(t)).toFloat()
        }

        fun FromEuler(euler: FloatArray): Quaternion {
            // yaw pitch roll
            // from wiki: Conversion between quaternion and euler
            val yaw = euler[0] * 0.5
            val pitch = euler[1] * 0.5
            val roll = euler[2] * 0.5
            val cosYaw = cos(yaw)
            val sinYaw = sin(yaw)
            val cosPitch = cos(pitch)
            val sinPitch = sin(pitch)
            val cosRoll = cos(roll)
            val sinRoll = sin(roll)

            return Quaternion(
                (cosRoll * cosPitch * cosYaw + sinRoll * sinPitch * sinYaw).toFloat(),
                (sinRoll * cosPitch * cosYaw - cosRoll * sinPitch * sinYaw).toFloat(),
                (cosRoll * sinPitch * cosYaw + sinRoll * cosPitch * sinYaw).toFloat(),
                (cosRoll * cosPitch * sinYaw - sinRoll * sinPitch * cosYaw).toFloat()
            )
        }
    }

    fun toRotationMatrix(): FloatArray {
        val rotationMatrix = FloatArray(9)
        rotationMatrix[0] = this.w*this.w + this.x*this.x - this.y*this.y - this.z*this.z
        rotationMatrix[1] = 2 * (this.x*this.y - this.w*this.z)
        rotationMatrix[2] = 2 * (this.x*this.z + this.w*this.y)
        rotationMatrix[3] = 2 * (this.x*this.y + this.w*this.z)
        rotationMatrix[4] = this.w*this.w - this.x*this.x + this.y*this.y - this.z*this.z
        rotationMatrix[5] = 2 * (this.y*this.z - this.w*this.x)
        rotationMatrix[6] = 2 * (this.x*this.z - this.w*this.y)
        rotationMatrix[7] = 2 * (this.y*this.z + this.w*this.x)
        rotationMatrix[8] = this.w*this.w - this.x*this.x - this.y*this.y + this.z*this.z
        return rotationMatrix
    }

    fun toEuler(): FloatArray {
        // Euler angles as yaw, pitch, roll
        // from wiki: Conversion between quaternion and euler
        val euler = FloatArray(3)
        val srcp = 2*(this.w*this.x+this.y*this.z)
        val crcp = 1 - 2*(this.x*this.x+this.y*this.y)
        euler[2] = atan2(srcp, crcp)

        val sp = 2*(this.w*this.y-this.z*this.x)

        euler[1] = if(abs(sp) >= 1 ) (PI/2 * sp.sign).toFloat() else asin(sp)

        val sycp = 2*(this.w*this.z+this.x*this.y)
        val cycp = 1 - 2*(this.y*this.y+this.z*this.z)
        euler[0] = atan2(sycp, cycp)
        return euler
    }

    fun toEuler3(): FloatArray {
        // Euler angles as yaw, pitch, roll
        // from wiki: Conversion between quaternion and euler
        val euler = FloatArray(3)
        val srcp = 2*(this.w*this.x+this.y*this.z)
        val crcp = 1 - 2*(this.x*this.x+this.y*this.y)
        euler[2] = atan2(srcp, crcp)

        val sp = 2*(this.w*this.y-this.z*this.x)

        euler[1] = if(abs(sp) >= 1 ) (PI/2 * sp.sign).toFloat() else asin(sp)

        val sycp = 2*(this.w*this.z+this.x*this.y)
        val cycp = 1 - 2*(this.y*this.y+this.z*this.z)
        euler[0] = atan2(sycp, cycp)
        return euler
    }

    fun toEuler2(): FloatArray {
        val rotationMatrix = toRotationMatrix()
        val test = -rotationMatrix[6]
        var yaw = 0f
        var pitch = 0f
        var roll = 0f
        if (test > 1-EPSILON) {
            yaw = 0f
            pitch = PI.toFloat()/2
            roll = atan2(rotationMatrix[1], rotationMatrix[2])
        } else if (test < -(1-EPSILON)) {
            yaw = 0f
            pitch = -PI.toFloat()/2
            roll = atan2(-rotationMatrix[1], -rotationMatrix[2])
        } else {
            yaw = atan2(rotationMatrix[3], rotationMatrix[0])
            pitch = asin(-rotationMatrix[6])
            roll = atan2(rotationMatrix[7], rotationMatrix[8])
        }
        return  floatArrayOf(yaw, pitch, roll)
//        return floatArrayOf(yaw, roll, pitch)
    }
}
