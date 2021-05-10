package com.arfist.armona.screen.ar

import android.graphics.Bitmap
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import org.opencv.core.Point

class GLRenderer(screenRatio: Float) : GLSurfaceView.Renderer {

    private val screenRatio = screenRatio
    private lateinit var liveViewProgram: LiveCameraView;
    private lateinit var navView: NavigationView;

    fun updateCameraViewBitmap(viewBitmap: Bitmap){
        liveViewProgram.cameraViewBitmap = viewBitmap
    }

    fun updateRoadLine(vertices: Array<org.opencv.core.Point>){
        navView.roadVertexCoords = vertices.fold(ArrayList<Float>(), {
            floatArr, pt -> floatArr.add(pt.x.toFloat());floatArr.add(pt.y.toFloat());floatArr
        }).toFloatArray()
    }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        navView = NavigationView();
        liveViewProgram = LiveCameraView(screenRatio);
        gl.glLineWidth(2.0f)
    }

    override fun onDrawFrame(gl: GL10) {
        // Redraw background color
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT or GL10.GL_DEPTH_BUFFER_BIT)

        liveViewProgram.draw()
        navView.draw()
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }
}