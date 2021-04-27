package com.arfist.armona.screen.cameragl

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.*
import android.media.Image
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.util.concurrent.Executors
import android.view.Surface
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.core.AspectRatio.RATIO_4_3
import androidx.camera.core.SurfaceRequest.Result.RESULT_REQUEST_CANCELLED
import androidx.camera.core.SurfaceRequest.Result.RESULT_SURFACE_USED_SUCCESSFULLY
import androidx.fragment.app.viewModels
import timber.log.Timber
import java.io.ByteArrayOutputStream

private fun ImageProxy.toBitmap(): Bitmap {
    val yBuffer = planes[0].buffer // Y
    val vuBuffer = planes[2].buffer // VU

    val ySize = yBuffer.remaining()
    val vuSize = vuBuffer.remaining()

    val nv21 = ByteArray(ySize + vuSize)

    yBuffer.get(nv21, 0, ySize)
    vuBuffer.get(nv21, ySize, vuSize)

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 50, out)
    val imageBytes = out.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}

class CameraGLFragment: Fragment() {

    private var cameraExecutor = Executors.newSingleThreadExecutor()
    val REQUEST_CODE_PERMISSIONS = 10

    private val viewModel: CameraGLViewModel by viewModels()
    private lateinit var renderer: CameraGLRenderer

    @SuppressLint("UnsafeOptInUsageError", "Assert")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        renderer = CameraGLRenderer(viewModel)

        // load model
        viewModel.model = ModelLoader(requireActivity().assets, "model").loadOBJ("arrowk2.obj")

        val glSurfaceView = GLSurfaceView(this.activity)
        glSurfaceView.preserveEGLContextOnPause = true
        glSurfaceView.setEGLContextClientVersion(2)
        glSurfaceView.setRenderer(renderer)
//        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY


        // listen camera
        requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)

        return glSurfaceView
    }

    fun context() = super.requireActivity()

    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(context(), it) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("Recycle", "RestrictedApi")
    private fun startCamera() {
        val instance = ProcessCameraProvider.getInstance(context())
        instance.addListener({
            val camera_provider = instance.get()
            val camera_selector = CameraSelector
                .Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()

            val preview = Preview
                .Builder()
//                .setCameraSelector(camera_selector)
                .setTargetAspectRatio(RATIO_4_3)
                .build()

            val surface = Surface(viewModel.surfaceTexture!!)

            preview.setSurfaceProvider { req: SurfaceRequest ->
                val cam = req.camera
                req.provideSurface(surface, cameraExecutor) {
                    // In all cases (even errors), we can clean up the state. As an
                    // optimization, we could also optionally check for REQUEST_CANCELLED
                    // since we may be able to reuse the surface on subsequent surface requests.
                    Timber.d("surface preview status ${it.resultCode}")
                    when (it.resultCode) {
                        RESULT_SURFACE_USED_SUCCESSFULLY -> {
                            Timber.d("surface preview success")
                        }
                        RESULT_REQUEST_CANCELLED -> {
                            Timber.d("surface preview cancel")
                        }
                        else -> {
                            Timber.e("surface preview error on code ${it.resultCode}")
                        }
                    }


                }
            }

            val image_capture = ImageCapture
                .Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build() // usecase

            val image_analyzer = ImageAnalysis
                .Builder() // usecase
                .build()

            image_analyzer.setAnalyzer(cameraExecutor!!, ImageAnalysis.Analyzer { image ->
                val rotation = image.imageInfo.rotationDegrees
                Timber.i("image is stream w=${image.width} h=${image.height} rotation=$rotation ${image.format} planes=${image.planes}")
                val rotationMatrix = Matrix().apply { postRotate(rotation.toFloat()) }
                val bitmapRaw = image.toBitmap()
                val bitmapRotated = Bitmap.createBitmap(bitmapRaw, 0, 0, bitmapRaw.width, bitmapRaw.height, rotationMatrix, true)
//                bitmapRaw.recycle()
//                viewModel.cameraBitmap?.recycle()
                viewModel.cameraBitmap = bitmapRotated
                image.close()
                return@Analyzer
            }) // end image Analyzer

            camera_provider.bindToLifecycle(this, camera_selector, image_analyzer, preview, image_capture)
            Timber.i("Finish Start Camera")
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