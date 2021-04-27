package com.arfist.armona.screen.cameragl

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel

class CameraGLViewModel: ViewModel() {
    var arrowRotation = 0f
    var arrowModel: ModelLoader.MeshGroup? = null
    var arrowFovy = 30f
    var cameraBitmap: Bitmap? = null
}