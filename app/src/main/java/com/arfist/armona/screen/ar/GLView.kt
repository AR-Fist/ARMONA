package com.arfist.armona.screen.ar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProviders
import timber.log.Timber
import com.arfist.armona.utils.ModelLoader

@RequiresApi(Build.VERSION_CODES.P)
class GLView(context: Context, attrs: android.util.AttributeSet) : GLSurfaceView(context, attrs) {

    private lateinit var glRenderer: GLRenderer
    val activity: FragmentActivity by lazy {
        try {
            context as FragmentActivity
        } catch (exception: ClassCastException) {
            throw ClassCastException("Please ensure that the provided Context is a valid FragmentActivity")
        }
    }
    private var arViewModel = ViewModelProviders.of(activity).get(ArViewModel::class.java)

    init {
        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2)

        setZOrderOnTop(true)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        holder.setFormat(PixelFormat.TRANSLUCENT)
        Timber.i("CameraGLViewModel is $arViewModel")

        arViewModel.arrowModel.arrowModel = ModelLoader(context.assets, "model").loadOBJ("arrowk2.obj")
    }


    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        arViewModel.screenModel.screenRatio = width.toFloat() / height
        glRenderer = GLRenderer(arViewModel)
        setRenderer(glRenderer)
        preserveEGLContextOnPause = true
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    fun streamCameraView(bitmap: Bitmap) {
        queueEvent {
            glRenderer.updateCameraViewBitmap(bitmap)
            requestRender()
        }
    }

    fun updateRoadLine(vertices: Array<android.graphics.Point>, frame: android.graphics.Point){
        Timber.i("updateRoadLine: $vertices, $frame")
        queueEvent{
            val middleX = frame.x / 2.0
            val middleY = frame.y / 2.0
            glRenderer.updateRoadLine(vertices.map { v -> org.opencv.core.Point( (middleX - v.x) / middleX, (middleY - v.y) / middleY) }.toTypedArray())
            requestRender()
        }
    }
}