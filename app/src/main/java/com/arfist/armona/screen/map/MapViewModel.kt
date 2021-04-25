package com.arfist.armona.screen.map

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.arfist.armona.getStringFormat
import com.arfist.armona.services.LocationRepository
import com.arfist.armona.services.LowestMetres
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import kotlinx.coroutines.launch
import timber.log.Timber

class MapViewModel(application: Application) :AndroidViewModel(application){

    private val locationRepository = LocationRepository.getInstance(application.applicationContext)

    val lastLocation = locationRepository.currentLocation

    val likelyPlaces = locationRepository.surroundPlaceNames

    private var _permissionGranted = MutableLiveData<Boolean>()
    val permissionGranted: LiveData<Boolean> get() = _permissionGranted

//    private var _direction = MutableLiveData<Direction>()
//    val direction: LiveData<Direction> get() = _direction
    val direction = locationRepository.direction

    private var _followLocation = MutableLiveData<Boolean>(false)
    val followLocation: LiveData<Boolean> get() = _followLocation

    fun onPermissionGranted() {
        Timber.i("onPermissionGranted")
        locationRepository.startLocationUpdates()
        locationRepository.getSurrounding()
        _permissionGranted.value = true
    }

    fun onPermissionDenied() {
        Timber.i("onPermissionDenied")
        locationRepository.stopLocationUpdates()
        _permissionGranted.value = false
    }

    fun getDirection(destination: String) {
        Timber.i("Get direction: ${lastLocation.value?.getStringFormat()}, ${destination}.")
        viewModelScope.launch {
            try {
//                _direction.value = lastLocation.value?.let {
//                    locationRepository.getDirection(
//                        it.getStringFormat(), destination)
//                }
                lastLocation.value?.let {
                    locationRepository.getDirection(
                        it.getStringFormat(), destination)
                }?.let { locationRepository.setDirection(it) }
                Timber.i("get direction success")
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    fun getOffsetDirection() = locationRepository.calculateOffsetDirectionLocation()
    fun getOffsetNorth() = locationRepository.calculateOffsetNorthLocation()
    fun getOffsetBearing(bearing: Double) = locationRepository.calculateOffsetBearing(bearing)
    fun getOffsetDegree(degree: Double) = locationRepository.calculateOffsetDegree(degree)
//    fun getDirection(destination: String) = locationRepository.getDirection(destination)

}