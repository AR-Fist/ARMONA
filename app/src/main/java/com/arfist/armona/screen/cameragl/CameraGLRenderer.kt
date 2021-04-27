package com.arfist.armona.screen.cameragl

import android.opengl.*
import android.opengl.GLES20.*
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.properties.Delegates


private fun toBuffer(fa: FloatArray) = ByteBuffer
    .allocateDirect(fa.size * 4)
    .order(ByteOrder.nativeOrder())
    .asFloatBuffer()
    .apply {
        put(fa)
        position(0)
    }

class CameraGLRenderer(private val viewModel: CameraGLViewModel): GLSurfaceView.Renderer {

    lateinit var program: CameraGLProgram

    private val background_vertices = floatArrayOf(-0.7f, -0.7f, 1f, -1f, -1f, 1f, 1f, 1f)
    private val background_textures = floatArrayOf(0f, 1f, 1f, 1f, 0f, 0f, 1f, 0f)

    val model_matrix = FloatArray(16).apply {
        Matrix.setIdentityM(this, 0)
        Matrix.scaleM(this, 0, .5f, .5f, .5f)
        Matrix.rotateM(this, 0, 90f, 1f, 0f, 0f) }
    val view_matrix = FloatArray(16).apply {
        Matrix.setLookAtM(this, 0, 2f, 4f, 1.2f, 0f, 0f, 0f, 0f, 0f, 2f); }
    val projection_matrix = FloatArray(16).apply {
        Matrix.perspectiveM(this, 0, 30f, 1f / 1f, 0f, 10f) }

    private var external_texture by Delegates.notNull<Int>()

    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        glClearColor(0f, 1f, 1f, 1f)
        program = CameraGLProgram()
        program.useProgram()

        external_texture = IntArray(1).let {
            glGenTextures(1, it, 0)
            glBindTexture(GL_TEXTURE_2D, it[0])
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
            it[0]
        }
    }

    override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {
        glViewport(0, 0, width, height)
        Matrix.perspectiveM(projection_matrix, 0, 30f, width.toFloat() / height, 0f, 10f)
    }

    override fun onDrawFrame(unused: GL10?) {
        Timber.i("CameraGLRenderer draw frame")

        glBindFramebuffer(GL_FRAMEBUFFER, 0)
        glEnable(GL_BLEND)
        glEnable(GL_CULL_FACE)
        glClear(GL_COLOR_BUFFER_BIT)

        with(program) {
            glEnableVertexAttribArray(aPosition)
            glEnableVertexAttribArray(aNormal)
            glEnableVertexAttribArray(aTexture)

            glUniformMatrix4fv(uModel, 1, false, toBuffer(model_matrix))
            glUniformMatrix4fv(uView, 1, false, toBuffer(view_matrix))
            glUniformMatrix4fv(uProjection, 1, false, toBuffer(projection_matrix))

            // DRAW CAMERA
            if (viewModel.cameraBitmap != null) {
                glUniform1i(uMode, 1)

                glBindTexture(GL_TEXTURE_2D, external_texture)
                glActiveTexture(GL_TEXTURE0)
                viewModel.cameraBitmap?.let {
                    GLUtils.texImage2D(GL_TEXTURE_2D, 0, it, 0)
                }
                glUniform1i(uTextureImage, 0)

                glVertexAttribPointer(aTexture, 2, GL_FLOAT, false, 0, toBuffer(background_textures))
                glVertexAttribPointer(aPosition, 2, GL_FLOAT, false, 0, toBuffer(background_vertices))
                glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)
                glClear(GL_DEPTH_BUFFER_BIT)
            }

            // DRAW MODEL
            if (viewModel.model != null) {
                glUniform1i(uMode, 3) // mode
                for (mesh in viewModel.model!!.meshes) {
                    mesh.material.run {
                        glUniform3f(uKa, Ka[0], Ka[1], Ka[2])
                        glUniform3f(uKd, Kd[0], Kd[1], Kd[2])
                        glUniform3f(uKs, Ks[0], Ks[1], Ks[2])
                        glUniform1f(uNs, Ns)
                    }
                    mesh.polygon.run {
                        glVertexAttribPointer(aPosition, 3, GL_FLOAT, false, 0, bufferPosition)
                        glVertexAttribPointer(aTexture, 2, GL_FLOAT, false, 0, bufferTexture)
                        glVertexAttribPointer(aNormal, 3, GL_FLOAT, false, 0, bufferNormal)
                        glDrawArrays(GL_TRIANGLES, 0, this.numFaces())
                    }

                }
            }

            glDisableVertexAttribArray(program.aPosition)
            glDisableVertexAttribArray(program.aNormal)
            glDisableVertexAttribArray(program.aTexture)
        }
    }
}