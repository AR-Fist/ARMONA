package com.arfist.armona.screen.map

import android.app.Application
import androidx.lifecycle.*
import com.arfist.armona.services.Direction
import com.arfist.armona.services.LocationRepository
import kotlinx.coroutines.launch
import timber.log.Timber
import java.lang.Exception

class MapViewModel(application: Application) :AndroidViewModel(application){

    private val locationRepository = LocationRepository.getInstance(application.applicationContext)

    val lastLocation = locationRepository.currentLocation

    val likelyPlaces = locationRepository.likelyPlaceNames

    private var _permissionGranted = MutableLiveData<Boolean>()
    val permissionGranted: LiveData<Boolean> get() = _permissionGranted

    private var _direction = MutableLiveData<Direction>()
    val direction: LiveData<Direction> get() = _direction

    fun onPermissionGranted() {
        Timber.i("onPermissionGranted")
        locationRepository.startLocationUpdates()
        locationRepository.getSurrounding()
    }

    fun onPermissionDenied() {
        Timber.i("onPermissionDenied")
        locationRepository.stopLocationUpdates()
        _permissionGranted.value = false
    }

    fun getDirection(destination: String) {
        Timber.i("Get direction")
        viewModelScope.launch {
            try {
                _direction.value = locationRepository.getDirection(destination = destination)
                Timber.i("get direction success")
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }
}