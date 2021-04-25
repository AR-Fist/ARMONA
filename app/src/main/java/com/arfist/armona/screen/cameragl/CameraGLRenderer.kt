package com.arfist.armona.screen.cameragl

import android.opengl.GLES20
import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

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

    private val background_vertices = floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)
    private val background_textures = floatArrayOf(0f, 1f, 1f, 1f, 0f, 0f, 1f, 0f)

    val model_matrix = FloatArray(16).apply {
        Matrix.setIdentityM(this, 0)
        Matrix.scaleM(this, 0, .5f, .5f, .5f)
        Matrix.rotateM(this, 0, 90f, 1f, 0f, 0f) }
    val view_matrix = FloatArray(16).apply {
        Matrix.setLookAtM(this, 0, 2f, 4f, 1.2f, 0f, 0f, 0f, 0f, 0f, 2f); }
    val projection_matrix = FloatArray(16).apply {
        Matrix.perspectiveM(this, 0, 30f, 1f / 1f, 0f, 10f) }

    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        glClearColor(0f, 1f, 1f, 1f)
        program = CameraGLProgram()
        program.useProgram()
    }

    override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {
        glViewport(0, 0, width, height)
        Matrix.perspectiveM(projection_matrix, 0, 30f, width.toFloat() / height, 0f, 10f)
    }

    override fun onDrawFrame(unused: GL10?) {
        Timber.i("CameraGLRenderer draw frame")
        val texture = 0

        glBindFramebuffer(GL_FRAMEBUFFER, 0)
        glEnable(GL_BLEND)
        glEnable(GL_CULL_FACE)
        glClear(GL_COLOR_BUFFER_BIT)

        with (program) {
            glEnableVertexAttribArray(a_position)
            glEnableVertexAttribArray(a_normal)
            glEnableVertexAttribArray(a_texture)

            glUniformMatrix4fv(u_model, 1, false, toBuffer(model_matrix))
            glUniformMatrix4fv(u_view, 1, false, toBuffer(view_matrix))
            glUniformMatrix4fv(u_projection, 1, false, toBuffer(projection_matrix))

            // DRAW CAMERA
            if (texture != 0) {
                glUniform1i(u_mode, 1)
                glVertexAttribPointer(a_texture, 2, GL_FLOAT, false, 0, toBuffer(background_textures))
                glVertexAttribPointer(a_position, 2, GL_FLOAT, false, 0, toBuffer(background_vertices))

                glActiveTexture(GL_TEXTURE0)
                glBindTexture(GL_TEXTURE_2D, texture)
                glUniform1i(u_texture_image, 0)

                glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)
                glClear(GL_DEPTH_BUFFER_BIT)
            }

            // DRAW MODEL
            if (viewModel.model != null) {
                glUniform1i(u_mode, 3) // mode
                for (mesh in viewModel.model!!.meshes) {
                    mesh.material.run {
                        glUniform3f(u_ka, Ka[0], Ka[1], Ka[2])
                        glUniform3f(u_kd, Kd[0], Kd[1], Kd[2])
                        glUniform3f(u_ks, Ks[0], Ks[1], Ks[2])
                        glUniform1f(u_ns, Ns)
                    }
                    mesh.polygon.run {
                        glVertexAttribPointer(a_position, 3, GL_FLOAT, false, 0, bufferPosition)
                        glVertexAttribPointer(a_texture, 2, GL_FLOAT, false, 0, bufferTexture)
                        glVertexAttribPointer(a_normal, 3, GL_FLOAT, false, 0, bufferNormal)
                        glDrawArrays(GL_TRIANGLES, 0, this.numFaces())
                    }

                }
            }

            glDisableVertexAttribArray(program.a_position)
            glDisableVertexAttribArray(program.a_normal)
            glDisableVertexAttribArray(program.a_texture)
        }
    }
}