package com.arfist.armona.screen.map

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.arfist.armona.utils.getStringFormat
import com.arfist.armona.services.Direction
import com.arfist.armona.services.LocationRepository
import kotlinx.coroutines.launch
import timber.log.Timber

class MapViewModel(application: Application) :AndroidViewModel(application){

    private val locationRepository = LocationRepository.getInstance(application.applicationContext)

    val lastLocation = locationRepository.currentLocation

    val likelyPlaces = locationRepository.surroundPlaceNames

    private var _permissionGranted = MutableLiveData<Boolean>()
    val permissionGranted: LiveData<Boolean> get() = _permissionGranted

    private var _direction = MutableLiveData<Direction>()
    val direction: LiveData<Direction> get() = _direction

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
                _direction.value = lastLocation.value?.let {
                    locationRepository.getDirection(
                        it.getStringFormat(), destination)
                }
                Timber.i("get direction success")
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }
}