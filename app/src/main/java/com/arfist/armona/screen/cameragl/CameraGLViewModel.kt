package com.arfist.armona.screen.cameragl

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel

class CameraGLViewModel: ViewModel() {
    var arrowRotation = 0f // use if rotationMode = RotationMode.EULER
    var arrowModel: ModelLoader.MeshGroup? = null
    var arrowFovy = 30f
    var cameraBitmap: Bitmap? = null
    var arrowQuaternion = floatArrayOf(0f, 1f, 0f, 0f) // use if rotationMode = RotationMode.QUATERNION
    var rotationMode: RotationMode = RotationMode.EULER // please set mode here

    enum class RotationMode {
        EULER,      // use arrowRotation (default=EULER)
        QUATERNION // use arrowQuaternion
    }
}