package com.arfist.armona.screen.ar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import com.arfist.armona.services.LocationRepository

class ArViewModel(application: Application) : AndroidViewModel(application) {
    // TODO: Implement the ViewModel
    // Repository
    private val locationRepository = LocationRepository.getInstance(application.applicationContext)

    fun calculateArrowRotation() {
        /**
         * This method will be call every sensor update loop ie gyroscope, magnetometer,
         * accelerometer and GPS only in arviewmodel not in map viewmodel as mapviewmodel
         * not need this data
         *
         * For the arrow rotation
         * 1. init arrow as 0 0 0 point to north tangent to surface (world frame)
         * 2. rotate by initial bearing from the north (world frame)
         * 3. apply rotation matrix to convert world frame to local frame
         * 4. send the converted rotation to GL
         */
        val bearing = locationRepository.getBearingToNextPosition()
    }
}