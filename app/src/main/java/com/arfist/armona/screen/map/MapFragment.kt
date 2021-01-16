package com.arfist.armona.screen.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import com.arfist.armona.R
import com.arfist.armona.databinding.MapFragmentBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import timber.log.Timber

class MapFragment : Fragment() {

    private lateinit var binding: MapFragmentBinding
    private lateinit var mapViewModel: MapViewModel
    private lateinit var mapRepository: MapRepository
    private lateinit var googleMap: GoogleMap

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Timber.i("onCreateView")

        binding = DataBindingUtil.inflate(inflater, R.layout.map_fragment, container, false)
        mapViewModel = ViewModelProvider(this).get(MapViewModel::class.java)
        mapRepository = MapRepository(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Timber.i("onViewCreated")
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync {
            Timber.i("Map ready")

            googleMap = it
            getPermission()
        }

        binding.buttonMapAr.setOnClickListener { viewGet: View ->
            viewGet.findNavController().navigate(MapFragmentDirections.actionMapFragmentToArFragment())
        }

        mapViewModel.lastLocation.observe(viewLifecycleOwner, Observer { location ->
            if (location == null) {
                Toast.makeText(requireContext(), "Can not retrieve current location", Toast.LENGTH_LONG).show()
            } else {
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, DEFAULT_ZOOM.toFloat()))
            }
        })

        mapViewModel.permissionGranted.observe(viewLifecycleOwner, {
            updateGoogleUI(it)
        })
    }

    companion object {
        private const val PERMISSION_REQUEST_MAP = 1
        private const val DEFAULT_ZOOM = 15
    }

    /**
     * Check if all permission is granted if not request it
     */
    private val permissionList = arrayListOf<String>(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)

    private fun getPermission() {
        Timber.i("Grant permission")

        val permissionRequest: MutableList<String> = ArrayList()
        for (permission in permissionList){
            if ( ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_DENIED) {
                permissionRequest.add(permission)
            }
        }
        if (permissionRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(requireActivity(),
                permissionRequest.toTypedArray(), PERMISSION_REQUEST_MAP)
        } else {
            mapViewModel.onPermissionGranted(mapRepository)
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
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }){
                    mapViewModel.onPermissionGranted(mapRepository)
                } else {
                    mapViewModel.onPermissionDenied()
                }
            }
        }
    }

    private fun updateGoogleUI(permissionGranted: Boolean) {
        try {
            if (permissionGranted) {
                googleMap.isMyLocationEnabled = true
                googleMap.uiSettings?.isMyLocationButtonEnabled = true
            } else {
                googleMap.isMyLocationEnabled = false
                googleMap.uiSettings?.isMyLocationButtonEnabled = false
                getPermission()
            }
        } catch (e: SecurityException) {
            Timber.e(e)
        }
    }
}