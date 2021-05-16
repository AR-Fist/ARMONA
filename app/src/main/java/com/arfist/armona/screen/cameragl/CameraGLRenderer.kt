package com.arfist.armona.screen.cameragl

import android.opengl.*
import android.opengl.GLES20.*
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.sqrt
import kotlin.properties.Delegates


private fun toBuffer(fa: FloatArray) = ByteBuffer
    .allocateDirect(fa.size * 4)
    .order(ByteOrder.nativeOrder())
    .asFloatBuffer()
    .apply {
        put(fa)
        position(0)
    }

fun quanternionToRotationMatrix(quanternion: FloatArray): FloatArray {
    // https://stackoverflow.com/questions/1556260/convert-quaternion-rotation-to-rotation-matrix
    var qx = quanternion[0]
    var qy = quanternion[1]
    var qz = quanternion[2]
    var qw = quanternion[3]
    var n = 1.0f / sqrt(qx*qx + qy*qy + qz*qz + qw*qw);
    qx *= n;
    qy *= n;
    qz *= n;
    qw *= n;
    // TODO might need transpose ?
    return floatArrayOf(
        1.0f - 2.0f*qy*qy - 2.0f*qz*qz, 2.0f*qx*qy - 2.0f*qz*qw, 2.0f*qx*qz + 2.0f*qy*qw, 0.0f,
        2.0f*qx*qy + 2.0f*qz*qw, 1.0f - 2.0f*qx*qx - 2.0f*qz*qz, 2.0f*qy*qz - 2.0f*qx*qw, 0.0f,
        2.0f*qx*qz - 2.0f*qy*qw, 2.0f*qy*qz + 2.0f*qx*qw, 1.0f - 2.0f*qx*qx - 2.0f*qy*qy, 0.0f,
        0.0f, 0.0f, 0.0f, 1.0f);
}

/** reactive update matrix every frame **/
private fun calculateModelMatrixFromDegree(matrix: FloatArray, degree: Float) {
    matrix.apply {
        Matrix.setIdentityM(this, 0) // think in reverse order
        // last transform
        Matrix.translateM(this, 0, 0f, 0.6f, 0f) // change arrow position
        Matrix.rotateM(this, 0, -90f + degree, 0f, 0f, 1f)
        Matrix.translateM(this, 0, -0.1f, 0f, 0f) // change rotate origin
        Matrix.rotateM(this, 0, 90f, 1f, 0f, 0f)
        Matrix.scaleM(this, 0, .4f, .4f, .4f)
        // first transform
    }
}
private fun calculateModelMatrixFromQuanternion(matrix: FloatArray, quanternion: FloatArray) {
    matrix.apply {
        Matrix.setIdentityM(this, 0) // think in reverse order
        // last transform
        Matrix.translateM(this, 0, 0f, 0.6f, 0f) // change arrow position

        // TODO check if login work
        Matrix.multiplyMM(this, 0, this.copyOf(), 0, quanternionToRotationMatrix(quanternion), 0);

        Matrix.translateM(this, 0, -0.1f, 0f, 0f) // change rotate origin
        Matrix.rotateM(this, 0, 90f, 1f, 0f, 0f)
        Matrix.scaleM(this, 0, .4f, .4f, .4f)
        // first transform
    }
}

private fun calculateProjectionMatrix(matrix: FloatArray, fovy: Float) {
    Matrix.perspectiveM(matrix, 0, fovy, 1f / 1f, 0f, 10f)
}

/** real renderer **/
class CameraGLRenderer(private val viewModel: CameraGLViewModel): GLSurfaceView.Renderer {

    lateinit var program: CameraGLProgram

    private val background_vertices = floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)
    private val background_textures = floatArrayOf(0f, 1f, 1f, 1f, 0f, 0f, 1f, 0f)

    val model_matrix = FloatArray(16).apply { calculateModelMatrixFromDegree(this, 0f) }

    val view_matrix = FloatArray(16).apply {
        Matrix.setLookAtM(this, 0, 0f, 2.3f, 1.6f, 0f, 0f, 0f, 0f, 0f, 2f); }
    val projection_matrix = FloatArray(16).apply { calculateProjectionMatrix(this, viewModel.arrowFovy) }

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
        Matrix.perspectiveM(projection_matrix, 0, viewModel.arrowFovy, width.toFloat() / height, 0f, 10f)
    }

    override fun onDrawFrame(unused: GL10?) {
        Timber.i("CameraGLRenderer draw frame")
        // update model
        when (viewModel.rotationMode) {
            CameraGLViewModel.RotationMode.EULER -> { calculateModelMatrixFromDegree(model_matrix, viewModel.arrowRotation) }
            CameraGLViewModel.RotationMode.QUATERNION -> { calculateModelMatrixFromQuanternion(model_matrix, viewModel.arrowQuaternion) }
        }
        calculateProjectionMatrix(projection_matrix, viewModel.arrowFovy)

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
            if (viewModel.arrowModel != null) {
                glUniform1i(uMode, 3) // mode
                for (mesh in viewModel.arrowModel!!.meshes) {
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