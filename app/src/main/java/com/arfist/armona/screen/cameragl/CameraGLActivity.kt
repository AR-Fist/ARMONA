package com.arfist.armona.screen.cameragl

import android.opengl.GLSurfaceView
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.arfist.armona.BuildConfig
import com.arfist.armona.R
import timber.log.Timber
import java.util.concurrent.Executors

class CameraGLActivity: AppCompatActivity() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var surfaceView: GLSurfaceView
    private lateinit var renderer: CameraGLRenderer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        setContentView(R.layout.activity_cameragl)

        // CREATE CameraGLFragment
        val cameraglFragment = CameraGLFragment()

        // Dynamic Fragment, but use CameraGLFragment
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.cameragl_fragment_container, cameraglFragment)
            commit()
        }
    }
}