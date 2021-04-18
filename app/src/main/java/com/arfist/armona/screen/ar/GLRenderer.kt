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
        liveViewProgram.textureBitmap = viewBitmap
    }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        // Set the background frame color
//         gl.glClearColor(0f, 0f, 0f, 0.8f)
//        gl.glShadeModel(GL10.GL_SMOOTH);
//        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);
//        gl.glHint(GL10.GL_LINE_SMOOTH_HINT, GL10.GL_NICEST);
//        gl.glHint(GL10.GL_POLYGON_SMOOTH_HINT, GL10.GL_NICEST);
//        gl.glEnable(GL10.GL_LINE_SMOOTH);
//        gl.glDisable(GL10.GL_DEPTH_TEST);
//        gl.glDisable(GL10.GL_CULL_FACE);
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