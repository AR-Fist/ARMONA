package com.arfist.armona.screen.map

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
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
import com.arfist.armona.R
import com.arfist.armona.databinding.MapFragmentBinding
import com.arfist.armona.hasPermission
import com.arfist.armona.services.Direction
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.PolyUtil
import timber.log.Timber

class MapFragment : Fragment() {

    private lateinit var binding: MapFragmentBinding

    private lateinit var googleMap: GoogleMap

    private val mapViewModel: MapViewModel by activityViewModels()

    private var isPermissionGranted = false

    private var direction: Direction? = null

    private var followLocation = false

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Timber.i("onViewCreated")
        super.onViewCreated(view, savedInstanceState)

        binding.buttonMapAr.setOnClickListener { viewGet: View ->
            viewGet.findNavController()
                .navigate(MapFragmentDirections.actionMapFragmentToArFragment())
        }

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
            if(::googleMap.isInitialized) {
                direction = it
                drawPolyline()
            }
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

    // Draw direction on map
    private fun drawPolyline() {
        Timber.i("Draw polyline")
        val paths: MutableList<List<LatLng>> = ArrayList()
        for (step in direction?.routes?.get(0)?.legs?.get(0)?.steps!!) {
            paths.add(PolyUtil.decode(step.polyline?.points))
        }

        for (path in paths) {
            googleMap.addPolyline(PolylineOptions()
                .clickable(false)
                .addAll(path)
                .color(Color.GREEN)
            )
        }
    }

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