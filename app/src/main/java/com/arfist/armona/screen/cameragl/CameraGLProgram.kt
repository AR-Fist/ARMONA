package com.arfist.armona.screen.cameragl

import android.opengl.GLES20.*
import android.opengl.GLException
import timber.log.Timber
import kotlin.properties.Delegates

private val vertexShaderCode = """
    #version 100
    precision mediump float;
    precision mediump int;
    
    uniform int mode;
    
    uniform mat4 model;
    uniform mat4 view;
    uniform mat4 projection;
    
    attribute vec4 position;
    attribute vec2 texture;
    attribute vec3 normal;
    
    varying vec2 v_texture;
    varying vec3 v_normal;
    
    void main() {
      if (mode == 1) {
        gl_Position = position;
        v_texture = texture;
        
      } else if (mode == 2 || mode == 3) {
        gl_Position = projection * view * model * position;
        v_texture = texture;
        v_normal = normal;
      }
    }
""".trimIndent()

private val fragmentShaderCode = """
    #version 100
    precision mediump float;
    precision mediump int;
    
    uniform int mode;
    uniform sampler2D texture_image;
    
    varying vec2 v_texture;
    varying vec3 v_normal;
    
    uniform vec3  ka;
    uniform vec3  kd;
    uniform vec3  ks;
    uniform float ns;
    
    vec3 lightDir = normalize(vec3(3.0, 2.0, -60.0));
    vec3 lightColor = vec3(1.0, 1.0, 1.0);
    vec3 objectColor = vec3(0.8, 0.2, 0.6);
    
    void main() {
      if (mode == 1) {
        gl_FragColor = texture2D(texture_image, v_texture);
        
      } else if (mode == 2) {
        gl_FragColor = vec4(1, 0, 0, 1);
        
      } else if (mode == 3) {
        float ambientStrength = 0.3;
        vec3 ambient = ka * ambientStrength * lightColor;
        
        float diff = max(dot(v_normal, lightDir), 0.0);
        float diffuseStrength = 1.0;
        vec3 diffuse = diff * kd * lightColor * diffuseStrength;
        
        vec3 result = (ambient + diffuse) * objectColor;
        gl_FragColor = vec4(result, 1.0);
      }
    }
""".trimIndent()


/**
 * sekeleton ARMONA custom vertex shader and fragment shader
 * but DON'T write any draw workflow here
 **/
class CameraGLProgram {

    private var program by Delegates.notNull<Int>()

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
    }

    // mode switching
    val uMode = uniform("mode")
    // mvp
    val uModel = uniform("model")
    val uView = uniform("view")
    val uProjection = uniform("projection")
    // obj
    val aPosition = attrib("position")
    val aNormal = attrib("normal")
    val aTexture = attrib("texture")
    val uTextureImage = uniform("texture_image")
    // mtl
    val uKa = uniform("ka")
    val uKd = uniform("kd")
    val uKs = uniform("ks")
    val uNs = uniform("ns")

    fun useProgram() = glUseProgram(program)
    private fun uniform(name: String) = glGetUniformLocation(program, name)
    private fun attrib(name: String) = glGetAttribLocation(program, name)
}