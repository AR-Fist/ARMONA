package com.arfist.armona.services

import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.arfist.armona.BuildConfig
import com.arfist.armona.getStringFormat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.launch
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

    private var _direction = MutableLiveData<Direction>()
    val direction: LiveData<Direction>
        get() = _direction

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

    fun setDirection(value: Direction) {
        _direction.value = value
    }

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
      //    fun getDirection(origin: String, destination: String): Direction {
        /**
         * Get direction from origin to destination via walking only
         */
        Timber.i("Get direction: ${origin}, ${destination}.")
        return retrofitService.getDirection(origin, destination)
    }

    fun getSurrounding() {
        /**
         * Get surrounding place in some range
         */
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

    // The counting reset with get direction success
    var route_count = 0
    var leg_count = 0
    var step_count = 0
    var stopLocation: JSONLatLng? = null
    fun getNextStop(): JSONLatLng? {
        /**
         * Get next step in legs in Routes in direction
         */
        var end_location: JSONLatLng? = null
        if (_direction.value != null) {
            end_location = _direction.value!!.routes?.get(route_count)?.legs?.get(leg_count)?.steps?.get(step_count)?.end_location
            }
        return end_location
        }

    fun resetCounting() {
        /**
         * Reset every counting to start
         */
        route_count = 0
        leg_count = 0
        step_count = 0
    }

    fun incrementCounting() {
        /**
         * Increment the counting and check if it exceed or not then reset counting and may invoke end navigation
         */

        val route = _direction.value!!.routes
        val leg = route?.get(route_count)?.legs
        val step = leg?.get(leg_count)?.steps
        step_count += 1
        if (step != null) {
            if (step_count >= step.size) {
                leg_count += 1
                step_count = 0
                if (leg_count >= leg.size) {
                    route_count += 1
                    leg_count = 0
                    if (route_count >= route.size){
                        resetCounting()
                        // May have to invoke end of navigation here
                    }
                }
            }
        }
    }

    val lowestMetres = 10
    fun getBearingToNextPosition(): Float {
        /**
         * 1. Check if distance between current location and next stop is lower than an epsilon
         * 2. if yes then increment and re-calculate the next stop
         * 3. get bearing to use as angle from north clock wise (as the range not really far the initial bearing is enough
         * 4. use the bearing to tell which direction to point
         *
         */
        if (stopLocation == null) {
            stopLocation = getNextStop()
        }

        val result = FloatArray(3)
        stopLocation?.let {
            Location.distanceBetween(_currentLocation.value!!.latitude, _currentLocation.value!!.longitude,
                it.lat!!, it.lng!!, result)
        }

        if (result[0] < lowestMetres) {
            incrementCounting()
            getNextStop()?.let {
                Location.distanceBetween(_currentLocation.value!!.latitude, _currentLocation.value!!.longitude,
                    it.lat!!, it.lng!!, result)
            }
        }
        return result[1]
    }
//    suspend fun getDirection(destination: String) {
////    fun getDirection(destination: String) {
//        Timber.i("Get direction: ${_currentLocation.value?.getStringFormat()}, ${destination}.")
//        try {
//            _direction.value = _currentLocation.value?.let {
//                getDirection(it.getStringFormat(), destination)
//            }
//            Timber.i("get direction success")
//        } catch (e: Exception) {
//            Timber.e(e)
//        }
//    }
}

interface MapApi {
    /**
     * This interface is HTTPS REST api to get direction data from google
     */

    @GET(LocationRepository.DIRECTION_API)
    suspend fun getDirection(
//    fun getDirection(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("mode") mode: String = "walking",
        @Query("key") key: String = BuildConfig.mapsApiKey
    ): Direction
}
