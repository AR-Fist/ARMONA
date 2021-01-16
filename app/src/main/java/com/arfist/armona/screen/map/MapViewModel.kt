package com.arfist.armona.screen.map

import android.location.Location
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.LatLng
import timber.log.Timber

class MapViewModel :ViewModel() {

    private var _lastKnownLocation = MutableLiveData<LatLng>()
    val lastLocation: LiveData<LatLng> get() = _lastKnownLocation

    private var _permissionGranted = MutableLiveData<Boolean>()
    val permissionGranted: LiveData<Boolean> get() = _permissionGranted

    fun onPermissionGranted(mapRepository: MapRepository) {
        Timber.i("Permission Granted")

        _permissionGranted.value = true
        val location = mapRepository.getLocation()
        if (location != null) {
            _lastKnownLocation.value = LatLng(location.latitude, location.longitude)
        } else {
            _lastKnownLocation.value = null
        }
    }

    fun onPermissionDenied() {
        _permissionGranted.value = false
    }
}