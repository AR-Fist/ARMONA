package com.arfist.armona

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import kotlin.math.sqrt

fun Context.hasPermission(permission: String): Boolean {
    return ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

fun Location.getStringFormat(): String {
    return "${this.latitude},${this.longitude}"
}

fun FloatArray.cross(other: FloatArray): FloatArray? {
    if (this.size != 3 && other.size != 3) {
        return null
    }
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

fun FloatArray.show(): String{
    var out = ""
    for (v in this) {
        out += "$v. "
    }
    return out
}

class Quaternion(
    val x: Float,
    val y: Float,
    val z: Float,
    val w: Float) {

    fun add(other: Quaternion): Quaternion {
        return Quaternion(
            this.x + other.x,
            this.y + other.y,
            this.z + other.z,
            this.w + other.w
        )
    }

    fun multiply(other: Quaternion): Quaternion {
        return Quaternion(
            (this.w*other.x) + (this.x*other.w) + (this.y*other.z) - (this.z*other.y),
            (this.w*other.y) - (this.x*other.z) + (this.y*other.w) + (this.z*other.x),
            (this.w*other.z) + (this.x*other.y) - (this.y*other.x) + (this.z*other.w),
            (this.w*other.w) - (this.x*other.x) - (this.y*other.y) - (this.z*other.z))
    }

    fun multiply(scalar: Float): Quaternion {
        return Quaternion(
            this.x*scalar,
            this.y*scalar,
            this.z*scalar,
            this.w*scalar)
    }
}