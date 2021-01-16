package com.arfist.armona.screen.map

import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import timber.log.Timber

class MapRepository(context: Context) {
    private val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private var lastKnownLocation: Location? = null
    fun getLocation(): Location? {
        Timber.i("Get current location")

        try {
            val lastLocation = fusedLocationProviderClient.lastLocation
            lastLocation.addOnSuccessListener { location ->
                lastKnownLocation = location
            }
        } catch (e: SecurityException){
            Timber.e(e)
        }
        return lastKnownLocation
    }
}