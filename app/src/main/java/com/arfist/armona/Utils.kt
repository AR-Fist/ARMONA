package com.arfist.armona

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
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
        val magnitude: Float = this.w*this.w + this.x*this.x + this.y*this.y + this.z*this.z
        return Quaternion(
            this.w/magnitude,
            this.x/magnitude,
            this.y/magnitude,
            this.z/magnitude
        )
    }

    fun getRotationMatrix(): FloatArray{
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
        val rotationMatrix = getRotationMatrix()
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
    }

    fun oldToEuler(): FloatArray {
        // Euler angles as yaw, pitch, roll
        val euler = FloatArray(3)
        val srcp = 2*(this.w*this.y+this.x*this.z)
        val crcp = 1 - 2*(this.x*this.x+this.y*this.y)
        euler[2] = atan2(srcp, crcp)

        var sp = 2*(this.w*this.y-this.z*this.x)
        sp = if(abs(sp) > 1 ) { 1F } else { -1F }
        euler[1] = asin(sp)

        val sycp = 2*(this.w*this.z+this.x*this.y)
        val cycp = 1 - 2*(this.y*this.y+this.z*this.z)
        euler[0] = atan2(sycp, cycp)
        return euler
    }

    fun Inverse(): Quaternion {
        return Quaternion(
            this.w,
            -this.x,
            -this.y,
            -this.z
        )
    }
}
