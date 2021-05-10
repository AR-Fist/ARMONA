package com.arfist.armona.screen.ar

import android.opengl.GLES20
import timber.log.Timber
import java.nio.*

class NavigationView {

    private val vertexShaderCode =
        """
            attribute vec2 vPosition;
            
            void main() {
                gl_Position = vec4(vPosition, 0., 1.);
            }
        """

    private val fragmentShaderCode =
        """
            precision mediump float;
            
            void main() {
                gl_FragColor = vec4(0., 2., 0., 1.);
            }
        """

    var roadVertexCoords = floatArrayOf(
        0.5f, 1f,
        -0.5f, -1f,

        -0.5f, 1f,
        0.5f, -1f,
    )
    set(value) {
        if (value.size != field.size)
            throw RuntimeException("Try to set roadVertexCoords with difference size = ${value.size}")
        field = value
        vertexBuffer.run {
            put(roadVertexCoords)
            position(0)
        }
        GLES20.glUseProgram(mProgram)

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, vertexAndElementBuffer[0]);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexAndElementBuffer[1])
        GLES20.glVertexAttribPointer(
            vPositionGLLocation,
            2,
            GLES20.GL_FLOAT,
            false,
            8,
            0
        )

        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, vertexBuffer.capacity() * 4, vertexBuffer)
    }

    private val indices = shortArrayOf(
        0,1,
        2,3
    )

    private val vertexBuffer: FloatBuffer =
        // (number of coordinate values * 4 bytes per float)
        ByteBuffer.allocateDirect(roadVertexCoords.size * 4).run {
            // use the device hardware's native byte order
            order(ByteOrder.nativeOrder())

            // create a floating point buffer from the ByteBuffer
            asFloatBuffer().apply {
                // add the coordinates to the FloatBuffer
                put(roadVertexCoords)
                // set the buffer to read the first coordinate
                position(0)
            }
        }

    private val indicesBuffer: ShortBuffer =
        ByteBuffer.allocateDirect(indices.size * 2).run{
            order(ByteOrder.nativeOrder())

            // create a floating point buffer from the ByteBuffer
            asShortBuffer().apply {
                // add the coordinates to the FloatBuffer
                put(indices)
                // set the buffer to read the first coordinate
                position(0)
            }
        }


    private  val vertexAndElementBuffer = IntBuffer.allocate(
        // Index
        // Vertex
        2
    )

    private var mProgram: Int
    private val vPositionGLLocation: Int

    // region Duplicated code from LiveCameraView 1
    private fun loadShader(type: Int, shaderCode: String): Int {

        val shader = GLES20.glCreateShader(type).also { shader ->

            // add the source code to the shader and compile it
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }

        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0)
            throw RuntimeException(
                "Could not compile shader : ${GLES20.glGetShaderInfoLog(shader)} | $shaderCode"
            )

        return shader;
    }

    // endregion

    init {

        // region Duplicated code from LiveCameraView 2
        val vertexShader: Int = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader: Int = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        // create empty OpenGL ES Program
        mProgram = GLES20.glCreateProgram().also {

            // add the vertex shader to program
            GLES20.glAttachShader(it, vertexShader)

            // add the fragment shader to program
            GLES20.glAttachShader(it, fragmentShader)

            // creates OpenGL ES program executables
            GLES20.glLinkProgram(it)
        }

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(mProgram, GLES20.GL_LINK_STATUS, linkStatus, 0)

        if (linkStatus[0] != GLES20.GL_TRUE)
            throw RuntimeException("Could not link program: ${GLES20.glGetProgramInfoLog(mProgram)}")

        Timber.i("Created NavViewGL program with ID = $mProgram")
        GLES20.glUseProgram(mProgram)
        GLES20.glGenBuffers(2, vertexAndElementBuffer)
        Timber.i("Created NavViewGL's buffer = {${vertexAndElementBuffer[0]}, ${vertexAndElementBuffer[1]}}")

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, vertexAndElementBuffer[0]);
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer.capacity() * 2, indicesBuffer, GLES20.GL_STATIC_DRAW)

        vPositionGLLocation = GLES20.glGetAttribLocation(mProgram, "vPosition").also {

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexAndElementBuffer[1]);
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertexBuffer.capacity() * 4, vertexBuffer, GLES20.GL_STATIC_DRAW)

            // Enable a handle to the triangle vertices
            GLES20.glEnableVertexAttribArray(it)

            // Prepare the triangle coordinate data
            GLES20.glVertexAttribPointer(
                it,
                2,
                GLES20.GL_FLOAT,
                false,
                8,
                0
            )
        }

        // endregion
    }

     fun draw() {
        // Add program to OpenGL ES environment
        GLES20.glUseProgram(mProgram)

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, vertexAndElementBuffer[0])
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexAndElementBuffer[1])

         GLES20.glVertexAttribPointer(
             vPositionGLLocation,
             2,
             GLES20.GL_FLOAT,
             false,
             8,
             0
         )

        // Draw the triangle
        GLES20.glDrawElements(GLES20.GL_LINES, indices.size, GLES20.GL_UNSIGNED_SHORT, 0)
    }

}