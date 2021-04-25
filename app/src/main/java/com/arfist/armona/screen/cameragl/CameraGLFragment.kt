package com.arfist.armona.screen.cameragl

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.util.concurrent.Executors
import android.util.Size
import android.widget.Toast
import androidx.camera.core.*
import timber.log.Timber
import java.util.concurrent.ExecutorService

class CameraGLFragment: Fragment() {

    private var cameraExecutor: ExecutorService? = null
    val REQUEST_CODE_PERMISSIONS = 10

    @SuppressLint("UnsafeOptInUsageError")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Create GLview

        val glSurfaceView = GLSurfaceView(this.activity)
        val renderer = CameraGLRenderer()

        glSurfaceView.preserveEGLContextOnPause = true
        glSurfaceView.setEGLContextClientVersion(2)
        glSurfaceView.setRenderer(renderer)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY

        // listen camera
        cameraExecutor = Executors.newSingleThreadExecutor()
        requestPermissions(
            REQUIRED_PERMISSIONS,
            REQUEST_CODE_PERMISSIONS
        )

        return glSurfaceView
    }

    fun context() = super.requireActivity()

    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(context(), it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        val instance = ProcessCameraProvider.getInstance(context())
        instance.addListener(Runnable {
            val camera = instance.get()

            val preview = Preview.Builder().build() // usecase
            val imageCapture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() // usecase

            val imageAnalyzer = ImageAnalysis.Builder() // usecase
                .apply {
//                    setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
//                    this.setImageReaderProxyProvider()
                    val WIDTH = 640
                    val HEIGHT = 640
                    setTargetResolution(Size(WIDTH, HEIGHT))
                }
                .build()
            imageAnalyzer.setAnalyzer(cameraExecutor!!, object : ImageAnalysis.Analyzer {
                override fun analyze(image: ImageProxy) {
                    val rotation = image.imageInfo.rotationDegrees
                    //                        val rotationMatrix = Matrix().apply { postRotate(rotation.toFloat()) }
                    Timber.i("image is stream w=${image.width} h=${image.height} rotation=$rotation ${image.format} planes=${image.planes}")
                    //                        val bitmapRaw = image.toBitmap()
                    //                        val bitmapRotated = Bitmap.createBitmap(bitmapRaw, 0, 0, bitmapRaw.width, bitmapRaw.height, rotationMatrix, true)
                    //                            setImage(bitmapRotated)
                    //                            gLSurfaceView.requestRender()
                    //                        }
                    image.close()
                    return
                }
            }) // end image Analyzer
            val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()
            // please read more about image analyser usecase
            camera.bindToLifecycle(this, cameraSelector, imageAnalyzer /*, review, imageCapture*/)
            Timber.i("Finish Start Camera");
        }, ContextCompat.getMainExecutor(context()))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                Timber.i("camera permission ok")
                startCamera()
            } else {
                Timber.e("camera permission disable")
                Toast.makeText(
                    context(),
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
//                finish()
            }
        }
    }
}