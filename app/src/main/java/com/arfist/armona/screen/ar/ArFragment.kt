package com.arfist.armona.screen.ar

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.Rect
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import com.arfist.armona.R
import com.arfist.armona.databinding.ArFragmentBinding
import com.arfist.armona.screen.map.MapViewModel
import timber.log.Timber
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import org.opencv.android.Utils
import org.opencv.imgproc.Imgproc
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.PI
import android.media.Image
import org.opencv.android.OpenCVLoader
import org.opencv.core.*
import org.opencv.core.Point
import java.io.ByteArrayOutputStream
import kotlin.math.atan2


class ArFragment : Fragment() {
    private lateinit var viewModel: ArViewModel
    private val mapViewModel: MapViewModel by activityViewModels()
    private lateinit var binding: ArFragmentBinding
    private lateinit var cameraExecutor: ExecutorService


    companion object {
        private const val TAG = "CameraXBasic"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    private fun allRequiredPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Timber.i("onCreateView")

        if (this.allRequiredPermissionsGranted()){
            setupCam(container)
        }else{
            ActivityCompat.requestPermissions(requireActivity(), REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // init OpenCV
        if(OpenCVLoader.initDebug()) {
            Log.i("OpenCV", "Load successfully")
        }
        else {
            Log.e("OpenCV", "Load fail")
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        binding = DataBindingUtil.inflate(inflater, R.layout.ar_fragment, container, false)
        return binding.root
    }

    private fun setupCam(container: ViewGroup?) {
        val cameraProvider = ProcessCameraProvider.getInstance(requireContext())

        cameraProvider.addListener({
            val cameraProvider  = cameraProvider.get()
            val imageAnalysis = ImageAnalysis.Builder().build()
            imageAnalysis.setAnalyzer(
                cameraExecutor,
                {imageproxy -> roadDetector(imageproxy, container!!)},
            )
            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis)

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        },
            ContextCompat.getMainExecutor(context))
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    private fun roadDetector(imageproxy: ImageProxy, container: ViewGroup){

        val rotationDegrees = imageproxy.imageInfo.rotationDegrees

        // get image from image proxy
        val img = imageproxy.image?.toBitmap()
        val rgba = Mat()
        Utils.bitmapToMat(img, rgba)

        if (img != null) {
            Log.d("Image", img.height.toString() + ", " + img.width.toString())
        }

        var result_lines = img?.let { detectRoadFromBitmap(rgba) }

        // plot left
        var left = result_lines?.get(0)
        Log.d("Left line (x1,y1,x2,y2)",left.toString())
        var pt1 = left?.get(0)?.let { Point(it.toDouble(), left[1].toDouble()) }
        var pt2 = left?.get(2)?.let { Point(it.toDouble(), left[3].toDouble()) }
        //Drawing lines on an image
        Imgproc.line(rgba, pt1, pt2, Scalar(255.0, 0.0, 0.0), 2)

        // plot right
        var right = result_lines?.get(1)
        Log.d("Right line (x1,y1,x2,y2)",right.toString())
        pt1 = right?.get(0)?.let { Point(it.toDouble(), right[1].toDouble()) }
        pt2 = right?.get(2)?.let { Point(it.toDouble(), right[3].toDouble()) }
        //Drawing lines on an image
        Imgproc.line(rgba, pt1, pt2, Scalar(255.0, 0.0, 0.0), 2)


//        val resultBitmap = img?.copy(Bitmap.Config.RGB_565, true)
        Utils.matToBitmap(rgba, img)


        // UI thread for update ImageView
        requireActivity().runOnUiThread {
            var imgview = container.findViewById<ImageView>(R.id.image_view)
            imgview.setImageBitmap(img)
        }
        imageproxy.close()

    }

    // util method for converting Image to Bitmap
    private fun Image.toBitmap(): Bitmap {
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.i("onViewCreated")

        viewModel = ViewModelProvider(this).get(ArViewModel::class.java)

        binding.arViewModel = viewModel
        binding.buttonArMap.setOnClickListener { mView: View ->
            mView.findNavController().navigate(ArFragmentDirections.actionArFragmentToMapFragment())
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        Timber.i("onActivityCreated")
        super.onActivityCreated(savedInstanceState)

    }

    override fun onStart() {
        Timber.i("onStart")
        super.onStart()
    }

    override fun onResume() {
        Timber.i("onResume")
        super.onResume()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Timber.i("onSave")
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        Timber.i("onPause")
        super.onPause()
    }

    override fun onStop() {
        Timber.i("onStop")
        super.onStop()
    }

    override fun onDestroyView() {
        Timber.i("onDestroyView")
        super.onDestroyView()
    }

    override fun onDestroy() {
        Timber.i("onDestroy")
        super.onDestroy()
    }

    override fun onDetach() {
        Timber.i("onDetach")
        super.onDetach()
    }

    override fun onAttach(context: Context) {
        Timber.i("onAttach")
        super.onAttach(context)
    }
}