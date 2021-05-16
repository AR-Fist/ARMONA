package com.arfist.armona.shared

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.arfist.armona.services.LocationRepository
import timber.log.Timber

class SharedViewModel(application: Application) :AndroidViewModel(application){

    private val locationRepository = LocationRepository.getInstance(application.applicationContext)

    val lastLocation = locationRepository.currentLocation

    val likelyPlaces = locationRepository.surroundPlaceNames

    private var _permissionGranted = MutableLiveData<Boolean>()
    val permissionGranted: LiveData<Boolean> get() = _permissionGranted

    val direction = locationRepository.direction

    private var _followLocation = MutableLiveData<Boolean>(false)
    val followLocation: LiveData<Boolean> get() = _followLocation

    // Test param
    fun onMinumumDistanceChange(text: String) {
        locationRepository.minimumDistance = text.toFloat()
    }

    fun onPermissionGranted() {
        Timber.i("onPermissionGranted")
        locationRepository.startLocationUpdates()
//        locationRepository.getSurrounding()
        _permissionGranted.value = true
    }

    fun onPermissionDenied() {
        Timber.i("onPermissionDenied")
        locationRepository.stopLocationUpdates()
        _permissionGranted.value = false
    }

    fun setDestination(destination: String){
        locationRepository.destination = destination
    }

    fun getOffsetDirection() = locationRepository.calculateOffsetDirectionLocation()
    fun getOffsetNorth() = locationRepository.calculateOffsetNorthLocation()
    fun getOffsetBearing(bearing: Double) = locationRepository.calculateOffsetBearing(bearing)
    fun getOffsetDegree(degree: Double) = locationRepository.calculateOffsetDegree(degree)
    fun getBearingToNextPosition() = locationRepository.getBearingToNextPosition()
}