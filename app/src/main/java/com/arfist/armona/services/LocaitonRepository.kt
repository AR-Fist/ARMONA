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
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.maps.android.SphericalUtil
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.abs

const val LowestMetres: Double = 5.0
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
        interval = TimeUnit.SECONDS.toMillis(1)
        fastestInterval = TimeUnit.SECONDS.toMillis(1)
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

    // The counting reset with get direction success
    var route_count = 0
    var leg_count = 0
    var step_count = 0
    var stopLocation: JSONLatLng? = null
    fun getStop(route_count: Int, leg_count: Int, step_count: Int): LatLng? {
        /**
         * Get next step in legs in Routes in direction
         */
        var end_location: JSONLatLng? = null
        if (_direction.value != null) {
            end_location = _direction.value!!.routes?.get(route_count)?.legs?.get(leg_count)?.steps?.get(step_count)?.end_location
            }
        return end_location?.let { LatLng(it.lat!!, it.lng!!) }
        }

    fun resetCounting() {
        /**
         * Reset every counting to start
         */
        route_count = 0
        leg_count = 0
        step_count = 0
    }

    fun getRouteLegStepSize(route_count: Int, leg_count: Int): IntArray {
        val route = _direction.value!!.routes
        val route_size = route?.size
        val leg = route?.get(route_count)?.legs
        val leg_size = leg?.size
        val step = leg?.get(leg_count)?.steps
        val step_size = step?.size
        if (step_size != null && leg_size != null && route_size != null) {
            return intArrayOf(route_size, leg_size, step_size)
        }
        return intArrayOf(-1, -1, -1)
    }

    fun incrementCounting(route_count: Int, leg_count: Int, step_count: Int): IntArray {
        val sizes = getRouteLegStepSize(route_count, leg_count)
        val result = intArrayOf(route_count, leg_count, step_count)
        result[2] += 1
        if (sizes[2] >= 0 && result[2] >= sizes[2]) {
            result[2] = 0
            result[1] += 1
            if (sizes[1] >= 0 && result[1] >= sizes[1]) {
                result[1] = 0
                result[0] += 1
                if (sizes[0] >= 0 && result[0] >= sizes[0]) {
                    result[0] = 0
                }
            }
        }
        return result
    }

    fun decrementCounting(route_count: Int, leg_count: Int, step_count: Int): IntArray {
        val sizes = getRouteLegStepSize(route_count, leg_count)
        val result = intArrayOf(route_count, leg_count, step_count)
        if (sizes[0] >= 0 && sizes[1] >= 0 && sizes[2] >= 0) {
            result[2] -= 1
            if (result[2] < 0) {
                result[1] -= 1
                if (result[1] < 0) {
                    result[0] -= 1
                    if (result[0] < 0) {
                        result[0] = sizes[0]-1
                    }
                    val leg = _direction.value!!.routes?.get(result[0])?.legs
                    val leg_size = leg?.size
                    if (leg_size != null) {
                        result[1] = leg_size-1
                    }
                }
                val step = _direction.value!!.routes?.get(route_count)?.legs?.get(leg_count)?.steps
                val step_size = step?.size
                if (step_size != null) {
                    result[2] = step_size-1
                }
            }
            return result
        }
        return sizes
    }

    fun distanceTo(destination: LatLng): Float {
        val current = LatLng(_currentLocation.value!!.latitude, _currentLocation.value!!.longitude)
        val result = FloatArray(3)
        Location.distanceBetween(_currentLocation.value!!.latitude, _currentLocation.value!!.longitude, destination.latitude, destination.longitude, result)
        return result[0]
    }

    val minimumDistance = 1000.0f
    fun getNextPosition(): LatLng? {
        /**
         * Determine next position
         *
         * Input:
         *  - Current location: LatLng
         *  - Last Position: LatLng
         *  - Next Position: LatLng
         *  - epsilon: Float = minimum value that have to request new direction
         * Output:
         *  - LatLng of the position that have to nanivate to
         *
         *  Algorithm
         *   a = distance from current_location to heading_stop
         *   b = distance from current_location to current_stop
         *   c = distance from last_stop to heading_stop
         *
         *   if a < c and b < c then
         *      next_position = heading_position
         *   else if a > c and b < epsilon then
         *      next_position = last_position
         *      current_count = prev_count
         *   else if b > c and a < epsilon then
         *      next_position = next_position
         *      current_count = heading_count
         *   else if a > c and b > epsilon or b > c and a > epsilon then
         *      request new direction
         */
        val currentStop = getStop(route_count, leg_count, step_count)
        val nextCount = incrementCounting(route_count, leg_count, step_count)
        val nextStop = getStop(nextCount[0], nextCount[1], nextCount[2])
        if (currentStop != null && nextStop != null) {
            val distanceCurrent = distanceTo(currentStop)
            val distanceNext = distanceTo(nextStop)
            val temp = FloatArray(1)
            Location.distanceBetween(currentStop.latitude, currentStop.longitude, nextStop.latitude, nextStop.longitude, temp)
            val distanceBetween = temp[0]
            if (distanceNext < distanceBetween && distanceCurrent < distanceBetween) {
                return nextStop
            } else if (distanceNext > distanceBetween && distanceCurrent < minimumDistance) {
                val prevCount = decrementCounting(route_count, leg_count, step_count)
                route_count = prevCount[0]
                leg_count = prevCount[1]
                step_count = prevCount[2]
                return currentStop
            } else if (distanceCurrent > distanceBetween && distanceNext < minimumDistance) {
                val newCount = incrementCounting(nextCount[0], nextCount[1], nextCount[2])
                route_count = nextCount[0]
                leg_count = nextCount[1]
                step_count = nextCount[2]
                return getStop(newCount[0], newCount[1], newCount[2])
            } else if ((distanceNext > distanceBetween && distanceCurrent > minimumDistance) || (distanceCurrent > distanceBetween && distanceNext > minimumDistance)) {
                Log.i("Location", "Out of range please find new direction")
                // TODO: find new direction
                resetCounting()
                return null
            }
        }
        return null
    }

    fun getBearingToNextPosition(): Float {
        val result = FloatArray(3)
        getNextPosition()?.let {
            Location.distanceBetween(_currentLocation.value!!.latitude, _currentLocation.value!!.longitude,
                it.latitude, it.longitude, result)
        }

        return result[1]
    }

    fun BearingToDegree(bearing: Float): Float {
        /**
         * Input: Bearing [-180, 180]
         * Output: Degree [-180, 180]
         */

        return if(-90 <= bearing && bearing < 0) {
            90 + abs(bearing)
        } else if(-180 <= bearing && bearing < -90) {
            -(bearing + 270)
        } else
            90 - bearing
    }

    fun DegreeToBearing(degree: Float): Float {
        /**
         * Input: Degree [-180, 180]
         * Output: Bearing [-180, 180]
         */

        return if (-90 <= degree && degree < 0) {
            90 + abs(degree)
        } else if(-180 <= degree && degree < -90) {
            -(degree + 270)
        } else {
            90 - degree
        }
    }

    fun BearingToDegree(bearing: Double): Double {
        /**
         * Input: Bearing [-180, 180]
         * Output: Degree [-180, 180]
         */

        return if(-90 <= bearing && bearing < 0) {
            90 + abs(bearing)
        } else if(-180 <= bearing && bearing < -90) {
            -(bearing + 270)
        } else
            90 - bearing
    }

    fun DegreeToBearing(degree: Double): Double {
        /**
         * Input: Degree [-180, 180]
         * Output: Bearing [-180, 180]
         */

        return if (-90 <= degree && degree < 0) {
            90 + abs(degree)
        } else if(-180 <= degree && degree < -90) {
            -(degree + 270)
        } else {
            90 - degree
        }
    }

    val arrowLength = 50.0
    fun calculateOffsetDirectionLocation(): LatLng {
        return SphericalUtil.computeOffset(LatLng(_currentLocation.value!!.latitude, _currentLocation.value!!.longitude), arrowLength, getBearingToNextPosition().toDouble())
    }

    fun calculateOffsetNorthLocation(): LatLng {
        return SphericalUtil.computeOffset(_currentLocation.value?.let { LatLng(it.latitude, it.longitude) }, arrowLength, 0.0)
    }

    fun calculateOffsetBearing(bearing: Double): LatLng {
        return SphericalUtil.computeOffset(_currentLocation.value?.let { LatLng(it.latitude, it.longitude) }, arrowLength, bearing)
    }

    fun calculateOffsetDegree(degree: Double): LatLng {
        return SphericalUtil.computeOffset(_currentLocation.value?.let { LatLng(it.latitude, it.longitude) }, arrowLength, DegreeToBearing(degree))
    }
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
