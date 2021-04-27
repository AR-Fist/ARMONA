package com.arfist.armona.screen.ar

import android.graphics.Bitmap
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

import android.opengl.GLES20
import android.opengl.GLSurfaceView

class GLRenderer(screenRatio: Float) : GLSurfaceView.Renderer {

    private val screenRatio = screenRatio
    private lateinit var liveViewProgram: LiveCameraView;

    public fun updateCameraViewBitmap(viewBitmap: Bitmap){
        liveViewProgram.cameraViewBitmap = viewBitmap
    }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        liveViewProgram = LiveCameraView(screenRatio);

    }

    override fun onDrawFrame(gl: GL10) {
        // Redraw background color
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT or GL10.GL_DEPTH_BUFFER_BIT)

//        liveViewProgram.draw()
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }
}