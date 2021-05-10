package com.arfist.armona.utils

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.location.Location
import android.media.Image
import androidx.core.app.ActivityCompat
import java.io.ByteArrayOutputStream

fun Context.hasPermission(permission: String): Boolean {
    return ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

fun Location.getStringFormat(): String {
    return "${this.latitude},${this.longitude}"
}

// util method for converting Image to Bitmap
fun Image.toBitmap(): Bitmap {
    val yBuffer = planes[0].buffer // Y
    val vuBuffer = planes[2].buffer // VU

    val ySize = yBuffer.remaining()
    val vuSize = vuBuffer.remaining()

    val nv21 = ByteArray(ySize + vuSize)

    yBuffer.get(nv21, 0, ySize)
    vuBuffer.get(nv21, ySize, vuSize)

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 50, out)
    val imageBytes = out.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}

// Not work somehow
//fun Location.getLatLngFormat(): LatLng {
//    return LatLng(this.latitude, this.longitude)
//}