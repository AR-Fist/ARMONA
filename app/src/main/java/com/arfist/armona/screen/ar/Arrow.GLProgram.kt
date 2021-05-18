package com.arfist.armona.screen.ar

import android.opengl.GLES20.*
import android.opengl.GLException
import android.opengl.Matrix
import com.arfist.armona.utils.ModelLoader
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer
import kotlin.math.sqrt
import kotlin.properties.Delegates

private val vertexShaderCode = """
    #version 100
    precision mediump float;
    precision mediump int;
    
    uniform mat4 model;
    uniform mat4 view;
    uniform mat4 projection;
    
    attribute vec4 position;
    attribute vec3 normal;
    
    varying vec3 v_normal;
    
    void main() {
        gl_Position = projection * view * model * position;
        v_normal = normal;
    }
""".trimIndent()

private val fragmentShaderCode = """
    #version 100
    precision mediump float;
    precision mediump int;
   
    varying vec3 v_normal;
    
    uniform vec3  ka;
    uniform vec3  kd;
    uniform vec3  ks;
    uniform float ns;
    
    vec3 lightDir = normalize(vec3(3.0, 6.0, -2.0));
    vec3 lightColor = vec3(1.0, 1.0, 1.0);
    
    void main() {
        float ambientStrength = 0.3;
        vec3 ambient = ka * ambientStrength * lightColor;
        
        float diff = max(dot(v_normal, lightDir), 0.0);
        float diffuseStrength = 1.0;
        vec3 diffuse = diff * kd * lightColor * diffuseStrength;
        
        vec3 result = (ambient + diffuse);
        gl_FragColor = vec4(result, 1.0);
    }
""".trimIndent()

fun quanternionToRotationMatrix(quanternion: FloatArray): FloatArray {
    // https://stackoverflow.com/questions/1556260/convert-quaternion-rotation-to-rotation-matrix
    var qx = quanternion[1]
    var qy = quanternion[2]
    var qz = quanternion[3]
    var qw = quanternion[0]
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

private fun toBuffer(fa: FloatArray) = ByteBuffer
    .allocateDirect(fa.size * 4)
    .order(ByteOrder.nativeOrder())
    .asFloatBuffer()
    .apply {
        put(fa)
        position(0)
    }

/** reactive update matrix every frame **/
private  fun initModelMatrix(matrix: FloatArray) {
    matrix.apply {
        Matrix.setIdentityM(this, 0)

        // Below is the default transform of the arrow to get arrow pointing left face up
        Matrix.translateM(this, 0, 0f, 0.6f, 0f) // Translate arrow to the bottom of the screen
        Matrix.rotateM(this, 0, 90f, 0f, 1f, 0f) // Rotate the default arrow to be face up
        Matrix.rotateM(this, 0, 90f, 1f, 0f, 0f) // Rotate the default arrow to be face up
        Matrix.scaleM(this, 0, .4f, .4f, .4f) // Scale the arrow to be the proper size
    }
}

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

private fun calculateModelMatrixFromQuaternion(matrix: FloatArray, quanternion: FloatArray) {
    Timber.i("mmQuat called")
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

private fun calculateModelMatrixFromRotationMatrix(matrix: FloatArray, rotationMatrix: FloatArray) {
    matrix.apply {
        Matrix.setIdentityM(this, 0)
        Matrix.translateM(this, 0, 0f, 0.6f, 0f)
        Matrix.multiplyMM(this, 0, this.copyOf(), 0, rotationMatrix, 0);
        Matrix.translateM(this, 0, -0.1f, 0f, 0f) // change rotate origin
        Matrix.rotateM(this, 0, 90f, 1f, 0f, 0f)
        Matrix.scaleM(this, 0, .4f, .4f, .4f)
    }
}

private fun calculateProjectionMatrix(matrix: FloatArray, fovy: Float) {
    Matrix.perspectiveM(matrix, 0, fovy, 1f / 1f, 0f, 10f)
}

/**
 * SHADER PROGRAM WRAPPER (no brainer class)
 * just compile custom vertex shader and fragment shader
 * and expose uniform/attribute as variable
 **/
class ArrowGLProgram(arrowModel: ModelLoader.MeshGroup) {

    private var program by Delegates.notNull<Int>()
    var fovy = 30f
    set(value) {
        calculateProjectionMatrix(projection_matrix,value)
        field = value
    }

    var rotation = 0f
    set(value) {
        calculateModelMatrixFromDegree(model_matrix, value)
        field = value
    }

    var quaternion = floatArrayOf(0f, 1f, 0f, 0f)
    set(value) {
        calculateModelMatrixFromQuaternion(model_matrix, value)
        field = value
    }

    var rotationMatrix = FloatArray(16)
    set(value) {
        calculateModelMatrixFromRotationMatrix(model_matrix, rotationMatrix)
        field = value
    }

    // The apply function call once when created
    val model_matrix = FloatArray(16).apply {
        initModelMatrix(this)
    }

    val view_matrix = FloatArray(16).apply {
        Matrix.setLookAtM(this, 0, 0f, 2.3f, 1.6f, 0f, 0f, 0f, 0f, 0f, 2f); }
    val projection_matrix = FloatArray(16).apply {
        calculateProjectionMatrix(
            this,
            fovy
        )
    }

    private val arrowModel = arrowModel
    private val vertexBuffer = IntBuffer.allocate(
        // aPosition
        // aNormal
        2
    )

    // obj
    val aPosition: Int by lazy {
        attrib("position")
    }
    val aNormal: Int by lazy {
        attrib("normal")
    }

    init {
        val result = IntArray(1)

        // CREATE VERTEX SHADER
        val vertexShader = glCreateShader(GL_VERTEX_SHADER)
        glShaderSource(vertexShader, vertexShaderCode)
        glCompileShader(vertexShader)
        glGetShaderiv(vertexShader, GL_COMPILE_STATUS, result, 0)
        if (result[0] == GL_FALSE) {
            Timber.e("Could not compile vertex shader: ${glGetShaderInfoLog(vertexShader)}")
            glDeleteShader(vertexShader)
            throw GLException(result[0], "Could not compile vertex shader")
        }

        // CREATE FRAGMENT SHADER
        val fragmentShader = glCreateShader(GL_FRAGMENT_SHADER)
        glShaderSource(fragmentShader, fragmentShaderCode)
        glCompileShader(fragmentShader)
        glGetShaderiv(fragmentShader, GL_COMPILE_STATUS, result, 0)
        if (result[0] == GL_FALSE) {
            Timber.e("Could not compile fragment shader: ${glGetShaderInfoLog(fragmentShader)}")
            glDeleteShader(vertexShader)
            glDeleteShader(fragmentShader)
            throw GLException(result[0], "Could not compile fragment shader")
        }

        // CREATE LINK PROGRAM
        program = glCreateProgram()
        glAttachShader(program, vertexShader)
        glAttachShader(program, fragmentShader)
        glLinkProgram(program)
        glGetProgramiv(program, GL_LINK_STATUS, result, 0)
        if (result[0] == GL_FALSE) {
            val message = "Link program fail ${glGetProgramInfoLog(program)}"
            Timber.e(message)
            throw GLException(result[0], message)
        }
        glValidateProgram(program)
        glGetProgramiv(program, GL_VALIDATE_STATUS, result, 0)
        if (result[0] == GL_FALSE) {
            val message =
                if (!glIsProgram(program)) "Program handle deprecated!" else "Program do not validated!"
            throw GLException(result[0], message)
        }
        if (!glIsProgram(program)) {
            throw GLException(GL_FALSE, "program is not gl program")
        }
        Timber.i("Created ArrowGLProgram with ID = $program")

        useProgram()

        glGenBuffers(2, vertexBuffer);
        glEnableVertexAttribArray(aPosition)
        glEnableVertexAttribArray(aNormal)
        Timber.i("Done initialize, ${vertexBuffer[0]}, ${vertexBuffer[1]} ${glGetError()}")
    }

    private fun loadPolygonToBuffer(polygon: ModelLoader.Polygon) {
        with(polygon){
            glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer[0])
            glBufferData(GL_ARRAY_BUFFER, bufferPosition.capacity() * 4, bufferPosition, GL_STATIC_DRAW)
            glVertexAttribPointer(aPosition, 3, GL_FLOAT, false, 0, 0)

            glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer[1])
            glBufferData(GL_ARRAY_BUFFER, bufferNormal.capacity() * 4, bufferNormal, GL_STATIC_DRAW)
            glVertexAttribPointer(aNormal, 3, GL_FLOAT, false, 0, 0)
        }
    }

    /** Let's expose uniform/attribute */
    // mvp
    val uModel = uniform("model")
    val uView = uniform("view")
    val uProjection = uniform("projection")
    // mtl
    val uKa = uniform("ka")
    val uKd = uniform("kd")
    val uKs = uniform("ks")
    val uNs = uniform("ns")

    fun draw(){
//        Timber.i("Arrow is drawing: meshes has length of ${arrowModel.meshes.size}");
        glBindFramebuffer(GL_FRAMEBUFFER, 0)
        glEnable(GL_BLEND)
        glEnable(GL_CULL_FACE)

        useProgram()

        glUniformMatrix4fv(uModel, 1, false,
            toBuffer(model_matrix)
        )
        glUniformMatrix4fv(uView, 1, false, toBuffer(view_matrix))
        glUniformMatrix4fv(uProjection, 1, false,
            toBuffer(projection_matrix)
        )

        for (mesh in arrowModel.meshes) {
            mesh.material.run {
                glUniform3f(uKa, Ka[0], Ka[1], Ka[2])
                glUniform3f(uKd, Kd[0], Kd[1], Kd[2])
                glUniform3f(uKs, Ks[0], Ks[1], Ks[2])
                glUniform1f(uNs, Ns)
            }
            mesh.polygon.run {
                loadPolygonToBuffer(this)
                glDrawArrays(GL_TRIANGLES, 0, numFaces())
            }
        }

        glDisable(GL_BLEND)
        glDisable(GL_CULL_FACE)
    }

    fun useProgram() = glUseProgram(program)
    private fun uniform(name: String) = glGetUniformLocation(program, name)
    private fun attrib(name: String) = glGetAttribLocation(program, name)
}