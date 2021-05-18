package com.arfist.armona.screen.ar

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import com.arfist.armona.R
import com.arfist.armona.databinding.ArFragmentBinding
import com.arfist.armona.shared.SharedViewModel
import timber.log.Timber
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import org.opencv.android.Utils
import org.opencv.imgproc.Imgproc
import java.util.concurrent.Executors
import android.os.Build
import androidx.annotation.RequiresApi
import org.opencv.android.OpenCVLoader
import org.opencv.core.*
import com.arfist.armona.utils.toBitmap
import android.util.Size
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.ceil

@SuppressLint("UnsafeExperimentalUsageError")
class ArFragment : Fragment() {

    private lateinit var viewModel: ArViewModel
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var binding: ArFragmentBinding
    private var executors = Array(2){ Executors.newSingleThreadExecutor() }
    private var roadDetectionSvc: RoadDetectionService? = null


    // Sensor: Axis convention https://developer.android.com/reference/android/hardware/SensorEvent#values
    private lateinit var sensorManager: SensorManager
    private val cameraProvider: ProcessCameraProvider by lazy {
        ProcessCameraProvider.getInstance(requireContext()).get()
    }

    private lateinit var container: ViewGroup
    private lateinit var glController: GLController

    companion object {
        private const val TAG = "CameraXBasic"
    }
    @RequiresApi(Build.VERSION_CODES.P)
    @androidx.camera.core.ExperimentalGetImage()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Timber.i("onCreateView")

        setupCam(container!!)
        this.container = container!!

        // init OpenCV
        if(OpenCVLoader.initDebug()) {
            Log.i("OpenCV", "Load successfully")
        }
        else {
            Log.e("OpenCV", "Load fail")
        }

        viewModel = ViewModelProvider(this).get(ArViewModel::class.java)
//        sharedViewModel.lastLocation.observe(viewLifecycleOwner, {
//            viewModel.calculateArrowRotation()
//        })

        viewModel.gyroscope.observe(viewLifecycleOwner, {
            viewModel.getOrientation(it.timestamp)
        })

        binding = DataBindingUtil.inflate(inflater, R.layout.ar_fragment, container, false)

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun setupCam(container: ViewGroup) {
        val roadDetectionUseCase = ImageAnalysis.Builder()
            .setTargetResolution(Size(resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels))
            .build()
            .also {
                it.setAnalyzer(
                    executors[0],
                    {imageProxy -> detectRoad(imageProxy, container)},
                )
            }

        val liveCamViewUseCase = ImageAnalysis.Builder()
            .setTargetResolution(Size(resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels))
            .build()
            .also {
                it.setAnalyzer(
                    executors[1],
                    {imageProxy -> liveCamView(imageProxy)
                    },
                )
            }

        // Select back camera as a default
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            // Unbind use cases before rebinding
            cameraProvider.unbindAll()

//            Timber.i(cameraProvider.)

            // Bind use cases to camera
            cameraProvider.bindToLifecycle(this, cameraSelector, roadDetectionUseCase, liveCamViewUseCase)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun liveCamView(imageproxy: ImageProxy){
        requireActivity().runOnUiThread {
            viewModel.cameraModel.setLiveViewBitmap(imageproxy.image!!.toBitmap())
            imageproxy.close()
        }
    }


    @RequiresApi(Build.VERSION_CODES.P)
    private fun detectRoad(imageproxy: ImageProxy, container: ViewGroup){

        // get image from image proxy
        val img = imageproxy.image!!.toBitmap()
        val _rgba = Mat()
        Utils.bitmapToMat(img, _rgba)
        val desiredSize = org.opencv.core.Size(ceil(_rgba.width() * 240.0 / _rgba.height()), 240.0)
        val rgba = Mat(desiredSize, _rgba.type())
        Imgproc.resize(_rgba, rgba, desiredSize)
        Timber.i("Processing those image with resolution W=${rgba.width()}, H=${rgba.height()}")

        if (roadDetectionSvc == null)
            roadDetectionSvc = RoadDetectionService(20f * desiredSize.height.toFloat() / 480f, 150f * desiredSize.width.toFloat() / 640f)
        var resultLines = roadDetectionSvc!!.detectRoadFromBitmap(rgba)

        requireActivity().runOnUiThread {

            viewModel.arrowModel.updateRoadLine(
                resultLines.fold(ArrayList<android.graphics.Point>())
                { arr, side ->
                    arrayOf(1, 0).fold(arr)
                    { a, i ->
                        a.add(android.graphics.Point(side[i * 2 + 1], side[i * 2])); a
                    }
                }.toTypedArray(),
                android.graphics.Point(desiredSize.height.toInt(), desiredSize.width.toInt())
            )
        }

        imageproxy.close()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.i("onViewCreated")

        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        viewModel.registerSensors()

        binding.arViewModel = viewModel
        binding.buttonArMap.setOnClickListener { mView: View ->
            mView.findNavController().navigate(ArFragmentDirections.actionArFragmentToMapFragment())
        }

        val glView = container.findViewById<GLView>(R.id.gl_view)
        glController = GLController(viewModel, viewLifecycleOwner, glView)

        Timer().scheduleAtFixedRate(object: TimerTask() {
            override fun run() {
                Timber.i("Mock up re-hint...")
                viewModel.setMeterText(ArViewModel.HintText(viewModel.meter.value!!.meter + 1))
            }
        },0L,1000L)
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
        cameraProvider.unbindAll()
        viewModel.unregisterSensors()
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