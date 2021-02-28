package com.arfist.armona.services

import android.content.Context
import android.location.Location
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.arfist.armona.BuildConfig
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import timber.log.Timber
import java.util.concurrent.TimeUnit

// This will be ref from while-in-use-location, LocationUpdateForeground, LocationUpdateBackGround
class LocationRepository private constructor(context: Context){

    // These object only created once at the runtime
    companion object {

        // Only one LocationRepository would exist
        @Volatile private var INSTANCE: LocationRepository? = null

        fun getInstance(context: Context): LocationRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocationRepository(context).also { INSTANCE = it }
            }
        }
        //---
        const val BASE_URL = "https://maps.googleapis.com"
        const val DIRECTION_API = "/maps/api/directions/json"
        //---
    }

    private var _currentLocation = MutableLiveData<Location>()
    val currentLocation: LiveData<Location>
        get() = _currentLocation

    // Location service provider with fine+coarse
    // This is what get call to get the location lat lng
    private val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    // Location requester
    private val locationRequest = LocationRequest().apply {
        interval = TimeUnit.SECONDS.toMillis(6)
        fastestInterval = TimeUnit.SECONDS.toMillis(3)
        maxWaitTime = TimeUnit.MINUTES.toMillis(2)
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    // Location callback
    // Callback when FusedLocationProvider get new location
    private val locationCallback = object: LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)

            if(locationResult.lastLocation != null) {
                Timber.i("Location callback")
                // Save location
                _currentLocation.value = locationResult.lastLocation
            } else {
                Timber.d("Location missing in callback")
            }
        }
    }

    // JSON parser
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // REST connector
    private val retrofit = Retrofit.Builder()
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .baseUrl(BASE_URL)
        .build()

    // REST service declaration
    private val retrofitService: MapApi by lazy {
        retrofit.create(MapApi::class.java)
    }

    // Client for surrounding places
    private val placesClient = Places.createClient(context)

    //
    private var _surroundPlaceNames = MutableLiveData<MutableList<String>>()
    val surroundPlaceNames: LiveData<MutableList<String>>
        get() = _surroundPlaceNames

    fun startLocationUpdates() {
        Timber.i("Start location update")
        try {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper())
        } catch (e: SecurityException){
            Timber.e(e)
        }
    }

    fun stopLocationUpdates() {
        Timber.i("Stop location update")
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    suspend fun getDirection(origin: String, destination: String): Direction {
        Timber.i("Get direction: ${origin}, ${destination}.")
        return retrofitService.getDirection(origin, destination)
    }

    fun getSurrounding() {
        Timber.i("Get surroundings")
        try {
            val placeFields = listOf(Place.Field.NAME)
            val placesRequest = FindCurrentPlaceRequest.newInstance(placeFields)
            val placeResult = placesClient.findCurrentPlace(placesRequest)

            placeResult.addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    val likelyPlaces = task.result

                    _surroundPlaceNames.value = ArrayList()
                    for (placeLikelihood in likelyPlaces.placeLikelihoods) {
                        _surroundPlaceNames.value?.add(placeLikelihood.place.name!!)
                    }
                }
            }
        } catch (e: SecurityException) {
            Timber.e(e)
        }
    }
}

interface MapApi {
    @GET(LocationRepository.DIRECTION_API)
    suspend fun getDirection(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("mode") mode: String = "walk",
        @Query("key") key: String = BuildConfig.mapsApiKey
    ): Direction
}
