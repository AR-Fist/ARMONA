package com.arfist.armona.screen.map

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import com.arfist.armona.R
import com.arfist.armona.databinding.MapFragmentBinding
import com.arfist.armona.hasPermission
import com.arfist.armona.services.Direction
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.maps.android.PolyUtil
import timber.log.Timber

class MapFragment : Fragment() {

    private lateinit var binding: MapFragmentBinding

    private lateinit var googleMap: GoogleMap

    private val mapViewModel: MapViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Timber.i("onCreateView")

        binding = DataBindingUtil.inflate(inflater, R.layout.map_fragment, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Timber.i("onViewCreated")
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync {
            Timber.i("Map ready")
            // TODO There is a problem with lifecycle
            googleMap = it
            getPermission()
        }

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
            } else {
                googleMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(location.latitude, location.longitude),
                        DEFAULT_ZOOM.toFloat()
                    )
                )
            }
        })

        mapViewModel.permissionGranted.observe(viewLifecycleOwner, {
            updateGoogleUI(it)
        })

        mapViewModel.direction.observe(viewLifecycleOwner, {
            drawPolyline(it)
        })

    }

    companion object {
        private const val PERMISSION_REQUEST_MAP = 1
        private const val DEFAULT_ZOOM = 15
    }

    /**
     * Check if all permission is granted if not request it
     */
    private val permissionList = arrayListOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

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

    /**
     *  requestPermission callback
     */
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

    private fun updateGoogleUI(permissionGranted: Boolean) {
        Timber.i("updateGoogleUI")
        try {
            if (permissionGranted) {
                googleMap.isMyLocationEnabled = true
                googleMap.uiSettings?.isMyLocationButtonEnabled = true
                mapViewModel.direction.value?.let { direction -> drawPolyline(direction) }
            } else {
                googleMap.isMyLocationEnabled = false
                googleMap.uiSettings?.isMyLocationButtonEnabled = false
                getPermission()
            }
        } catch (e: SecurityException) {
            Timber.e(e)
        }
    }

    private fun drawPolyline(direction: Direction) {
        Timber.i("Draw polyline")
        val paths: MutableList<List<LatLng>> = ArrayList()
        for (step in direction.routes?.get(0)?.legs?.get(0)?.steps!!) {
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
}