package com.arfist.armona.screen.ar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.ImageDecoder
import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.res.getResourceIdOrThrow
import timber.log.Timber
import com.arfist.armona.R

@RequiresApi(Build.VERSION_CODES.P)
class GLView(context: Context, attrs: android.util.AttributeSet) : GLSurfaceView(context, attrs) {

    private lateinit var glRenderer: GLRenderer

    init {
        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2)

        setZOrderOnTop(true)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        holder.setFormat(PixelFormat.TRANSLUCENT)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        glRenderer = GLRenderer(width.toFloat() / height)
        setRenderer(glRenderer)
        renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
    }

    fun streamCameraView(bitmap: Bitmap) {
        queueEvent(
            Runnable{
                glRenderer.updateCameraViewBitmap(bitmap)
                requestRender()
            }
        )
    }

    fun updateRoadLine(vertices: Array<android.graphics.Point>, frame: android.graphics.Point){
        Timber.i("updateRoadLine: $vertices, $frame")
        queueEvent(
            Runnable{
                val middleX = frame.x / 2.0
                val middleY = frame.y / 2.0
                glRenderer.updateRoadLine(vertices.map { v -> org.opencv.core.Point( (middleX - v.x) / middleX, (middleY - v.y) / middleY) }.toTypedArray())
                requestRender()
            }
        )
    }
}