package com.arfist.armona.screen.cameragl

import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.arfist.armona.BuildConfig
import com.arfist.armona.R
import timber.log.Timber
import java.util.concurrent.Executors


private const val SEEK_BAR_ROTATE_RANGE = 180

/**
 * PLEASE NOT EDIT THIS CLASS, THIS IS AN EXAMPLE ACTIVITY ONLY
 * this class will show how to interact with ViewModel and data will update in OpenGLRenderer
 **/
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

        val viewModel = ViewModelProvider(this).get(CameraGLViewModel::class.java)

        val rotationTextView = findViewById<TextView>(R.id.rotation_text_view)
        val rotationSeekBar = findViewById<SeekBar>(R.id.rotation_seek_bar)

        rotationSeekBar.max = SEEK_BAR_ROTATE_RANGE * 2;
        rotationSeekBar.progress = SEEK_BAR_ROTATE_RANGE
        rotationTextView.text = "ROTATE = 0"
        rotationSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val degree = progress - SEEK_BAR_ROTATE_RANGE
                rotationTextView.text = "ROTATE = $degree"
                viewModel.arrowRotation = degree.toFloat()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // CREATE CameraGLFragment
        val cameraglFragment = CameraGLFragment()

        // Dynamic Fragment, but use CameraGLFragment
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.cameragl_fragment_container, cameraglFragment)
            commit()
        }
    }
}