package com.arfist.armona.screen.ar

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import com.arfist.armona.Quaternion
import com.arfist.armona.R
import com.arfist.armona.databinding.ArFragmentBinding
import com.arfist.armona.screen.map.MapViewModel
import timber.log.Timber

class ArFragment : Fragment() {

    private lateinit var viewModel: ArViewModel
    private val mapViewModel: MapViewModel by activityViewModels()
    private lateinit var binding: ArFragmentBinding

    private lateinit var sensorManager: SensorManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Timber.i("onCreateView")

        binding = DataBindingUtil.inflate(inflater, R.layout.ar_fragment, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.i("onViewCreated")

        viewModel = ViewModelProvider(this).get(ArViewModel::class.java)
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        viewModel.registerSensors()

        binding.arViewModel = viewModel
        observeSensors()
        binding.buttonArMap.setOnClickListener { mView: View ->
            mView.findNavController().navigate(ArFragmentDirections.actionArFragmentToMapFragment())
        }
    }


    @SuppressLint("LogNotTimber", "SetTextI18n")
    private fun observeSensors() {

        binding.arViewModel!!.accelerometer.observe(viewLifecycleOwner, {
            showOutputAndLog("CalibratedAccelerometer", R.id.acc, it.values, it)
        })

        binding.arViewModel!!.gyroscope.observe(viewLifecycleOwner, {
            showOutputAndLog("CalibratedGyroscope", R.id.gyro, it.values, it)
            getOrientation(it.timestamp)
        })

        binding.arViewModel!!.magnetometer.observe(viewLifecycleOwner, {
            showOutputAndLog("CalibratedMagnetometer", R.id.magf, it.values, it)
        })

        binding.arViewModel!!.gravity.observe(viewLifecycleOwner, {
            showOutputAndLog("Gravity", R.id.gra, it.values, it)
        })
        binding.arViewModel!!.rotationVector.observe(viewLifecycleOwner, {
            showOutputAndLog("RotationVector", R.id.rotvec, it.values, it)

            val rotvecang = quaternionToEuler(Quaternion(it.values[0], it.values[1], it.values[2], it.values[3]))
            Log.i(
                "RotVecAngle",
                "${rotvecang?.get(0)}, ${rotvecang?.get(1)}, ${rotvecang?.get(2)}, ${it.timestamp}"
            )
        })

        binding.arViewModel!!.complementaryAngle.observe(viewLifecycleOwner, {
            view?.findViewById<TextView>(R.id.cf_ang)?.text = "${it[0]}, ${it[0]},${it[0]}"
        })
//        binding.arViewModel!!.uncalibAccelerometer.observe(viewLifecycleOwner, {
//            showOutputAndLog("UncalibratedAccelerometer", R.id.acc_un, it.values, it.timestamp)
//        })
//                binding.arViewModel!!.linearAccelerometer.observe(viewLifecycleOwner, {
//            showOutputAndLog("LinearAccelerometer", R.id.linacc, it.values, it.timestamp)
//        })
//                binding.arViewModel!!.uncalibGyrometer.observe(viewLifecycleOwner, {
//            showOutputAndLog("UncalibratedGyroscope", R.id.gyro_un, it.values, it.timestamp)
//        })
//                binding.arViewModel!!.uncalibMagnetometer.observe(viewLifecycleOwner, {
//            showOutputAndLog("UncalibratedMagnetometer", R.id.magfun, it.values, it.timestamp)
//        })
    }


    @SuppressLint("LogNotTimber")
    private fun showOutputAndLog(tag: String, textID: Int, values: FloatArray, event: SensorEvent) {
        var valString = ""
        for(v in values) {
            valString += "$v, "
        }
        Log.i(tag, "$valString${event.timestamp}")
        requireView().findViewById<TextView>(textID).text = valString
    }
    private fun getOrientation(timestamp: Long) = binding.arViewModel?.getOrientation(timestamp)
    private fun quaternionToEuler(quaternion: Quaternion) = binding.arViewModel?.quaternionToEuler(quaternion)

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