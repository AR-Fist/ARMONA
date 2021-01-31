package com.arfist.armona

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat

fun Context.hasPermission(permission: String): Boolean {
    return ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

fun Location.getStringFormat(): String {
    return "${this.latitude},${this.longitude}"
}

// Not work somehow
//fun Location.getLatLngFormat(): LatLng {
//    return LatLng(this.latitude, this.longitude)
//}