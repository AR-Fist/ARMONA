package com.arfist.armona.screen.ar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture


class ArFragment : Fragment() {
    private lateinit var viewModel: ArViewModel
    private val mapViewModel: MapViewModel by activityViewModels()
    private lateinit var binding: ArFragmentBinding

    companion object {
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
        binding = DataBindingUtil.inflate(inflater, R.layout.ar_fragment, container, false)
        return binding.root
    }

    private fun setupCam(container: ViewGroup?) {
        val cameraProvider = requireContext().let { ProcessCameraProvider.getInstance(it) }
        cameraProvider.addListener({
            bindPreview(cameraProvider.get(), container)
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindPreview(cameraProvider : ProcessCameraProvider, container: ViewGroup?) {
        val preview : Preview = Preview.Builder()
            .build()

        val cameraSelector : CameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        val previewView: PreviewView = container!!.findViewById<PreviewView>(R.id.previewView)
        previewView.preferredImplementationMode = PreviewView.ImplementationMode.SURFACE_VIEW
        preview.setSurfaceProvider(previewView.createSurfaceProvider())

        cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview)
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