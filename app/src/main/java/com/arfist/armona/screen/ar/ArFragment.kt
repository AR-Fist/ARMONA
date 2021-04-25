package com.arfist.armona.screen.ar

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
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
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.Viewport
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import timber.log.Timber

class ArFragment : Fragment() {

    private lateinit var viewModel: ArViewModel
    private val mapViewModel: MapViewModel by activityViewModels()
    private lateinit var binding: ArFragmentBinding

    private lateinit var sensorManager: SensorManager

    private val graphs = ArrayList<GraphView>()
    private val series = ArrayList<LineGraphSeries<DataPoint>>()

    private val graphSize = 5
    private val dataSize = 3
    private val seriesSize = graphSize*dataSize

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

        initGraphSeries()
        configGraph()
        configSeries()

        binding.buttonArMap.setOnClickListener { mView: View ->
            mView.findNavController().navigate(ArFragmentDirections.actionArFragmentToMapFragment())
        }
    }

    @SuppressLint("LogNotTimber")
    private fun observeSensors() {

        binding.arViewModel!!.mGoogleOrientation.observe(viewLifecycleOwner, {
            series.appendData(it[3].toDouble(), it, 0)
        })

        binding.arViewModel!!.myOrientationAngle.observe(viewLifecycleOwner, {
            series.appendData(it[3].toDouble(), it, 1)
        })

        binding.arViewModel!!.complementaryAngle.observe(viewLifecycleOwner, {
            series.appendData(it[3].toDouble(), it, 2)
        })

        binding.arViewModel!!.extendedKalman.observe(viewLifecycleOwner, {
            series.appendData(it[3].toDouble(), it, 3)
        })

        binding.arViewModel!!.rotationVector.observe(viewLifecycleOwner, {
            getOrientation(it.timestamp)
            val rotvecang = Quaternion(it.values[3], it.values[0], it.values[1], it.values[2]).toEuler()
            series.appendData(it.timestamp.toDouble(), rotvecang, 4)
        })
    }

    private fun getOrientation(timestamp: Long) = binding.arViewModel?.getOrientation(timestamp)

    private fun ArrayList<LineGraphSeries<DataPoint>>.appendData(timestamp: Double, values: FloatArray, count: Int) {
        val startIndex = count*3
        for (i in 0 until dataSize) {
            this[startIndex+i].appendData(DataPoint(timestamp, values[i].toDouble()), true, 100)
        }
    }

    private fun initGraphSeries() {
        graphs.add(requireView().findViewById(R.id.graphRotVec))
        graphs.add(requireView().findViewById(R.id.graphGGOrient))
        graphs.add(requireView().findViewById(R.id.graphComplementary))
        graphs.add(requireView().findViewById(R.id.graphMyOrient))
        graphs.add(requireView().findViewById(R.id.graphKalman))
        graphs.add(requireView().findViewById(R.id.graphArrow))

        for (i in 0 until seriesSize) {
            series.add(LineGraphSeries<DataPoint>())
        }
    }
    private fun configGraph() {
        for (i in 0 until graphSize) {
            configViewPort(graphs[i].viewport)
            add3Series(graphs[i], i)
            configNormalGraph(graphs[i])
        }
    }

    private fun add3Series(graph: GraphView, num: Int) {
        for (i in 0 until dataSize) {
            graph.addSeries(series[i+(num*3)])
        }
    }

    private fun configSeries() {
        for (i in 0 until graphSize) {
            series[0+(3*i)].title = "Yaw"
            series[0+(3*i)].color = Color.RED
            series[1+(3*i)].title = "Pitch"
            series[1+(3*i)].color = Color.GREEN
            series[2+(3*i)].title = "Roll"
            series[2+(3*i)].color = Color.BLUE
        }
    }

    private fun configNormalGraph(graph: GraphView) {
        graph.legendRenderer.isVisible = true
    }
    private fun configViewPort(viewPort: Viewport) {
        viewPort.isScalable = true
        viewPort.isXAxisBoundsManual = true
        viewPort.setMinX(0.0)
        viewPort.setMaxX(1000000000.0)
        viewPort.isYAxisBoundsManual = true
        viewPort.setMinY(-5.0)
        viewPort.setMaxY(5.0)
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