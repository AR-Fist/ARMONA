package com.arfist.armona.screen.ar

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLUtils
import android.util.Size
import timber.log.Timber
import java.lang.Exception
import java.nio.*

class LiveCameraView(val screenRatio: Float) {

    var cameraViewBitmap: Bitmap = Bitmap.createBitmap(intArrayOf(0,0,0), 1,1, Bitmap.Config.ARGB_8888)
    set(value) {
        if (field.width != value.width || field.height != value.height)
            fitTextureCoord(Size(value.width, value.height))
        field = value;
        reloadTexture();
    }

    private val vertexShaderCode =
        """
            attribute vec2 vPosition;
            attribute vec2 texCoord;
            
            varying vec2 v_TexCoord;
            
            void main() {
                gl_Position = vec4(vPosition, 0., 1.);
                v_TexCoord = texCoord;
            }
        """

    private val fragmentShaderCode =
        """
            precision mediump float;
            uniform vec4 vColor;
            uniform sampler2D u_Texture;
            
            varying vec2 v_TexCoord;
            
            void main() {
                gl_FragColor = vec4(texture2D(u_Texture, v_TexCoord).rgb, 0.75);
            }
        """

    private val COORDS_PER_VERTEX = 2


    private val coords = floatArrayOf(
        1f, 1f,
        -1f, -1f,
        1f, -1f,
        -1f, 1f,
    )



    private val indices = shortArrayOf(
        0,1,2,
        0,1,3
    )

    private val vPMatrix = FloatArray(16)

    private val color = floatArrayOf(0.63671875f, 0.76953125f, 0.22265625f, 1.0f)

    private val vertexBuffer: FloatBuffer =
        // (number of coordinate values * 4 bytes per float)
        ByteBuffer.allocateDirect(coords.size * 4).run {
            // use the device hardware's native byte order
            order(ByteOrder.nativeOrder())

            // create a floating point buffer from the ByteBuffer
            asFloatBuffer().apply {
                // add the coordinates to the FloatBuffer
                put(coords)
                // set the buffer to read the first coordinate
                position(0)
            }
        }

    private val textureBuffer: FloatBuffer =
        // (number of coordinate values * 4 bytes per float)
        ByteBuffer.allocateDirect(32).run {
            // use the device hardware's native byte order
            order(ByteOrder.nativeOrder())

            // create a floating point buffer from the ByteBuffer
            asFloatBuffer()
        }

    private  val indicesBuffer: ShortBuffer =
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
        // Texture
        3
    )

    private val texture = IntBuffer.allocate(1)

    private val vertexStride: Int = COORDS_PER_VERTEX * 4 // 4 bytes per vertex
    private val textureStride: Int = vertexStride // 4 bytes per vertex

    private val mProgram: Int
    private val vPositionGLLocation: Int

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

    init {
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

        Timber.i("Created LiveCamView program with ID = $mProgram")
        GLES20.glUseProgram(mProgram)
        GLES20.glGenBuffers(3, vertexAndElementBuffer);
        Timber.i("Created LiveCamView's buffer = {${vertexAndElementBuffer[0]}, ${vertexAndElementBuffer[1]}, ${vertexAndElementBuffer[2]}}")

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
                COORDS_PER_VERTEX,
                GLES20.GL_FLOAT,
                false,
                vertexStride,
                0
            )
        }

        loadTexture()

        GLES20.glGetUniformLocation(mProgram, "u_Texture").run {
            GLES20.glUniform1i(this, 0)
        }
    }

    private fun loadTexture(){
        GLES20.glGenTextures(1, texture);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, cameraViewBitmap, 0)
        GLES20.glFlush()
    }

    private fun reloadTexture(){
        GLES20.glUseProgram(mProgram)
        GLES20.glDeleteTextures(1, texture)
        GLES20.glGenTextures(1, texture);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        try {
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, cameraViewBitmap, 0)
        }catch (e: Exception){
            Timber.e(e);
        }
        GLES20.glFlush()
        Timber.d("Bitmap is updated, ${cameraViewBitmap.width}:${cameraViewBitmap.height}, ${cameraViewBitmap.getPixel(0,0)} error: ${GLES20.glGetError()}")
    }

    private fun fitTextureCoord(targetSize: Size){
        val compoundFactor: Float = targetSize.height.toFloat() / (targetSize.width * screenRatio)
        Timber.i("compoundFactor: $compoundFactor")
        val textureCoords = floatArrayOf(
            1f - compoundFactor, 0f,
//            0f, 0f,
            1f, 1f,
            1f, 0f,
            1f - compoundFactor, 1f,
//            0f, 1f,
        )

        with(textureBuffer){
            put(textureCoords)
            position(0)
        }

        GLES20.glGetAttribLocation(mProgram, "texCoord").run {

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexAndElementBuffer[2]);
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, textureBuffer.capacity() * 4, textureBuffer, GLES20.GL_STATIC_DRAW)

            // Enable a handle to the triangle vertices
            GLES20.glEnableVertexAttribArray(this)

            // Prepare the triangle coordinate data
            GLES20.glVertexAttribPointer(
                this,
                COORDS_PER_VERTEX,
                GLES20.GL_FLOAT,
                false,
                textureStride,
                0
            )
        }
    }

    fun draw() {
        // Add program to OpenGL ES environment
        GLES20.glUseProgram(mProgram)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, vertexAndElementBuffer[0]);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexAndElementBuffer[1]);

        GLES20.glVertexAttribPointer(
            vPositionGLLocation,
            COORDS_PER_VERTEX,
            GLES20.GL_FLOAT,
            false,
            vertexStride,
            0
        )

        // Draw the triangle
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.size, GLES20.GL_UNSIGNED_SHORT, 0)
    }
}