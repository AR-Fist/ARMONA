package com.arfist.armona.screen.ar

import android.graphics.Bitmap
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

import android.opengl.GLES20
import android.opengl.GLSurfaceView

class GLRenderer(viewModel: ArViewModel) : GLSurfaceView.Renderer {


    lateinit var liveViewProgram: LiveCameraView
    private set;

    lateinit var navView: Navigation
    private set;

    lateinit var arrowProgram: ArrowGLProgram
    private set;

    private val arrowModel = viewModel.arrowModel.arrowModel
    private val screenRatio = viewModel.screenModel.screenRatio

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {

        arrowProgram = ArrowGLProgram(arrowModel!!)
        navView = Navigation();
        liveViewProgram = LiveCameraView(screenRatio);


        gl.glLineWidth(2.0f)
    }

    override fun onDrawFrame(gl: GL10) {
        // Redraw background color
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT or GL10.GL_DEPTH_BUFFER_BIT)

        liveViewProgram.draw()
        arrowProgram.draw()
        navView.draw()
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }
}