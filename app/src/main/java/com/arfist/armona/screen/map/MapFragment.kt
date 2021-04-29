package com.arfist.armona.screen.map

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import com.arfist.armona.MainActivity.Companion.PERMISSION_REQUEST_MAP
import com.arfist.armona.MainActivity.Companion.permissionList
import com.arfist.armona.Quaternion
import com.arfist.armona.R
import com.arfist.armona.RadToDeg
import com.arfist.armona.databinding.MapFragmentBinding
import com.arfist.armona.hasPermission
import com.arfist.armona.screen.ar.ArViewModel
import com.arfist.armona.services.Direction
import com.arfist.armona.services.LowestMetres
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.maps.android.PolyUtil
import timber.log.Timber
import kotlin.math.PI

class MapFragment : Fragment() {

    // Init
    private lateinit var binding: MapFragmentBinding
    private lateinit var googleMap: GoogleMap
    private val mapViewModel: MapViewModel by activityViewModels()
    private var isPermissionGranted = false
    private var direction: Direction? = null
    private var followLocation = false

    // Temp
    private val arViewModel: ArViewModel by activityViewModels()
    //

    companion object {
        private const val DEFAULT_ZOOM = 15
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Timber.i("onCreateView")

        binding = DataBindingUtil.inflate(inflater, R.layout.map_fragment, container, false)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync {
            Timber.i("Map ready")

            googleMap = it
            updateGoogleMapsUI()
        }

        return binding.root
    }

    // Test
    private fun getOrientation(timestamp: Long) = binding.arViewModel?.getOrientation(timestamp)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Timber.i("onViewCreated")
        super.onViewCreated(view, savedInstanceState)

        binding.buttonMapAr.setOnClickListener { viewGet: View ->
            viewGet.findNavController()
                .navigate(MapFragmentDirections.actionMapFragmentToArFragment())
        }

        binding.arViewModel = arViewModel
        arViewModel.registerSensors()

        binding.arViewModel!!.rotationVector.observe(viewLifecycleOwner, {
            getOrientation(it.timestamp)
            if (direction != null
                && ::polylineBlack.isInitialized
                && ::polylineGray.isInitialized
                && ::polylineRed.isInitialized
                && ::polylineGreen.isInitialized
                && ::polylineBlue.isInitialized
                && ::polylineCyan.isInitialized
                && ::polylineMagenta.isInitialized
                && ::polylineYellow.isInitialized
                && ::polylineWhite.isInitialized
                && ::polylinePurple.isInitialized
            ) {
                updatePointing()
            }
        })

        mapViewModel.lastLocation.observe(viewLifecycleOwner, { location ->
            if (location == null) {
                Toast.makeText(
                    requireContext(),
                    "Can not retrieve current location",
                    Toast.LENGTH_LONG
                ).show()
            }
            else if(followLocation && ::googleMap.isInitialized){
                googleMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(location.latitude, location.longitude),
                        DEFAULT_ZOOM.toFloat()
                    ))
            }
        })

        mapViewModel.permissionGranted.observe(viewLifecycleOwner, {
            if(it && ::googleMap.isInitialized) {
                isPermissionGranted = true
                updateGoogleMapsUI()
            } else if(!it) {
                getPermission()
            }
        })

        mapViewModel.direction.observe(viewLifecycleOwner, {
//            if(::googleMap.isInitialized) {
//                direction = it
//                drawPolyline()
//            }
            direction = it
        })

        mapViewModel.followLocation.observe(viewLifecycleOwner, {
            followLocation = it
        })

    }

    // Check which permission is not granted, then ask for it
    private fun getPermission() {
        Timber.i("Grant permission")

        val permissionRequest: MutableList<String> = ArrayList()
        for (permission in permissionList) {
            if (!requireContext().hasPermission(permission)) {
                permissionRequest.add(permission)
            }
        }
        if (permissionRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                permissionRequest.toTypedArray(), PERMISSION_REQUEST_MAP
            )
        } else {
            mapViewModel.onPermissionGranted()
        }
    }

    // Callback when permission requested
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_MAP -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    mapViewModel.onPermissionGranted()
                } else {
                    mapViewModel.onPermissionDenied()
                }
            }
        }
    }

    // Update google map
    private fun updateGoogleMapsUI() {
        Timber.i("updateGoogleUI")
        try {
            if (isPermissionGranted) {
                googleMap.isMyLocationEnabled = true
                googleMap.uiSettings?.isMyLocationButtonEnabled = true
                drawPolyline()
            } else {
                googleMap.isMyLocationEnabled = false
                googleMap.uiSettings?.isMyLocationButtonEnabled = false
                getPermission()
            }
        } catch (e: SecurityException) {
            Timber.e(e)
            getPermission()
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    val arrowLength = 50.0
    // Draw direction on map
    private fun drawPolyline() {
        Timber.i("Draw polyline")
        val paths: MutableList<List<LatLng>> = ArrayList()
        val endLocations: MutableList<LatLng> = ArrayList()
        for (step in direction?.routes?.get(0)?.legs?.get(0)?.steps!!) {
            paths.add(PolyUtil.decode(step.polyline?.points))
            step.end_location?.let { endLocations.add(LatLng(step.end_location.lat!!, step.end_location.lng!!)) }
        }

        for (path in paths) {
            googleMap.addPolyline(PolylineOptions()
                .clickable(false)
                .addAll(path)
                .color(Color.GREEN)
            )
        }

        for (point in endLocations) {
            googleMap.addCircle(
                CircleOptions()
                    .center(point)
                    .radius(LowestMetres)
                    .strokeColor(Color.BLUE)
                    .strokeWidth(2.0f)
            )
        }

        // Test purpose
        testPointing()
    }

    // Test purpose
    lateinit var polylineBlack: Polyline
    lateinit var polylineGray: Polyline
    lateinit var polylineRed: Polyline
    lateinit var polylineGreen: Polyline
    lateinit var polylineBlue: Polyline
    lateinit var polylineCyan: Polyline
    lateinit var polylineMagenta: Polyline
    lateinit var polylineYellow: Polyline
    lateinit var polylineWhite: Polyline
    lateinit var polylinePurple: Polyline
    private fun testPointing() {
        val latlngDirection = mapViewModel.getOffsetDirection()
        val north = mapViewModel.getOffsetNorth()
        val currentLatLng = LatLng(mapViewModel.lastLocation.value!!.latitude, mapViewModel.lastLocation.value!!.longitude)
        polylineBlack = googleMap.addPolyline(PolylineOptions()
            .addAll(arrayListOf(currentLatLng, latlngDirection))
            .clickable(false)
            .color(Color.BLACK))
        polylineGray = googleMap.addPolyline(PolylineOptions()
            .addAll(arrayListOf(currentLatLng, north))
            .clickable(false)
            .color(Color.GRAY))
        polylineRed = googleMap.addPolyline(PolylineOptions()
            .addAll(arrayListOf(currentLatLng, north))
            .clickable(false)
            .color(Color.RED))
        polylineGreen = googleMap.addPolyline(PolylineOptions()
            .addAll(arrayListOf(currentLatLng, north))
            .clickable(false)
            .color(Color.GREEN))
        polylineBlue = googleMap.addPolyline(PolylineOptions()
            .addAll(arrayListOf(currentLatLng, north))
            .clickable(false)
            .color(Color.BLUE))
        polylineCyan = googleMap.addPolyline(PolylineOptions()
            .addAll(arrayListOf(currentLatLng, north))
            .clickable(false)
            .color(Color.CYAN))
        polylineMagenta = googleMap.addPolyline(PolylineOptions()
            .addAll(arrayListOf(currentLatLng, north))
            .clickable(false)
            .color(Color.MAGENTA))
        polylineYellow = googleMap.addPolyline(PolylineOptions()
            .addAll(arrayListOf(currentLatLng, north))
            .clickable(false)
            .color(Color.YELLOW))
        polylineWhite = googleMap.addPolyline(PolylineOptions()
            .addAll(arrayListOf(currentLatLng, north))
            .clickable(false)
            .color(Color.WHITE))
        polylinePurple = googleMap.addPolyline(PolylineOptions()
            .addAll(arrayListOf(currentLatLng, north))
            .clickable(false)
            .color(Color.argb(255, 153, 0, 255)))
    }

    private fun updatePointing() {
        val currentLatLng = LatLng(mapViewModel.lastLocation.value!!.latitude, mapViewModel.lastLocation.value!!.longitude)
        // Bearing
        polylineBlack.points = arrayListOf(currentLatLng, mapViewModel.getOffsetDirection())

        val facing = arViewModel.mGoogleOrientation.value!![0]*180/ PI
        polylineGray.points = arrayListOf(currentLatLng, mapViewModel.getOffsetBearing(facing))

        val rotvec = arViewModel.rotationVector.value!!.values
        val facing2 = Quaternion(rotvec[3], rotvec[0], rotvec[1], rotvec[2]).toEuler()[0]*180/ PI
        polylineRed.points = arrayListOf(currentLatLng, mapViewModel.getOffsetDegree(facing2))

        val facing3 = arViewModel.complementaryAngle.value!![0]*180/ PI
        polylineGreen.points = arrayListOf(currentLatLng, mapViewModel.getOffsetDegree(facing3))

        val facing4 = arViewModel.extendedKalman.value!![0]*180/ PI
        polylineBlue.points = arrayListOf(currentLatLng, mapViewModel.getOffsetDegree(facing4))

        val degree = arViewModel.testAngle.value!![0].RadToDeg()
        polylinePurple.points = arrayListOf(currentLatLng, mapViewModel.getOffsetDegree(degree.toDouble()))
    }
    //
    override fun onStart() {
        Timber.i("onStart")
        super.onStart()
    }

    override fun onResume() {
        Timber.i("onResume")
        super.onResume()
        updateGoogleMapsUI()
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