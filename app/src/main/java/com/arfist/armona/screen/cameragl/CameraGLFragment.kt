package com.arfist.armona.screen.cameragl

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.util.concurrent.Executors
import android.widget.Toast
import androidx.camera.core.*
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import timber.log.Timber
import java.io.ByteArrayOutputStream
import com.arfist.armona.screen.cameragl.REQUEST_CODE_PERMISSIONS as REQUEST_CODE_PERMISSIONS1

private fun toBitmap(img: ImageProxy): Bitmap {
    // https://stackoverflow.com/questions/56772967/converting-imageproxy-to-bitmap
    val yBuffer = img.planes[0].buffer // Y
    val vuBuffer = img.planes[2].buffer // VU
    val ySize = yBuffer.remaining()
    val vuSize = vuBuffer.remaining()
    val nv21 = ByteArray(ySize + vuSize)
    yBuffer.get(nv21, 0, ySize)
    vuBuffer.get(nv21, ySize, vuSize)
    val yuvImage = YuvImage(nv21, ImageFormat.NV21, img.width, img.height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 50, out)
    val imageBytes = out.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}

private const val REQUEST_CODE_PERMISSIONS = 10
private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

/**
 * CORE Fragment of AR View
 * because it is stand alone fragment, it's need to manage camera here in fragment, not in activity;
 **/
class CameraGLFragment: Fragment() {

    private var cameraExecutor = Executors.newSingleThreadExecutor()
    private val parentActivity: FragmentActivity
        get() = this.requireActivity()

    private lateinit var viewModel: CameraGLViewModel
    private lateinit var renderer: CameraGLRenderer

    /**
     * Create GL View with
     * - load OBJ model
     * - stream camera bitmap to viewModel
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // GET VIEWMODEL STORE from root activity
        viewModel = ViewModelProvider(parentActivity).get(CameraGLViewModel::class.java)

        // CREATE GL RENDERER
        renderer = CameraGLRenderer(viewModel)

        // LOAD ARROW MODEL
        viewModel.arrowModel = ModelLoader(parentActivity.assets, "model").loadOBJ("arrowk2.obj")

        /**
         * REQUEST CAMERA,
         * after success open camera will fire event in {@link onRequestPermissionsResult} then callback {@link startCamera}
         **/
        requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS1)

        // USE GL VIEW BIND TO MY GL RENDERER
        return GLSurfaceView(this.activity).apply {
            preserveEGLContextOnPause = true
            setEGLContextClientVersion(2)
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }

    /** Stream Camera Bitmap to viewModel (use camerax analyzer) */
    private fun startCamera() {
        val cameraSelector = CameraSelector
            .Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
        val imageAnalyzer = ImageAnalysis
            .Builder()
            .build()
            .apply {
                setAnalyzer(cameraExecutor!!, ImageAnalysis.Analyzer { image ->
                    val rotation = image.imageInfo.rotationDegrees
                    val rotationMatrix = Matrix().apply { postRotate(rotation.toFloat()) }
                    Timber.i("image is stream w=${image.width} h=${image.height} rotation=$rotation ${image.format} planes=${image.planes}")
                    val bitmapRaw = toBitmap(image)
                    val bitmapRotated = Bitmap.createBitmap(bitmapRaw, 0, 0, bitmapRaw.width, bitmapRaw.height, rotationMatrix, true)
                    bitmapRaw.recycle()
                    image.close()
                    // SEND CAMERA BITMAP TO VIEW MODEL
                    viewModel.cameraBitmap = bitmapRotated
                    return@Analyzer
                })
            }
        val currentProcessCameraProvider = ProcessCameraProvider.getInstance(parentActivity)
        currentProcessCameraProvider.addListener({
            currentProcessCameraProvider
                .get()
                .bindToLifecycle(this, cameraSelector, imageAnalyzer)
                .also { Timber.i("Attached CameraProvider to Fragment") }
        }, ContextCompat.getMainExecutor(parentActivity))

    }

    /** just boring permission callback */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val allPermissionsGranted = REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(parentActivity, it) == PackageManager.PERMISSION_GRANTED
        }
        when (requestCode) {
            REQUEST_CODE_PERMISSIONS1 -> when {
                allPermissionsGranted -> startCamera()
                else -> {
                    Timber.e("camera permission disable")
                    Toast.makeText(parentActivity, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}