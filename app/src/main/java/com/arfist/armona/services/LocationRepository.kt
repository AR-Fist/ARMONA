package com.arfist.armona.services

import android.content.Context
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.arfist.armona.BuildConfig
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.maps.android.PolyUtil
import com.google.maps.android.SphericalUtil
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread
import kotlin.math.abs

const val LowestMetres: Double = 5.0

// This ref from while-in-use-location, LocationUpdateForeground, LocationUpdateBackGround
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

    private var isFetchDirectionNeeded = false

    data class ALocation(val latitude: Double, val longitude: Double){
        override fun toString(): String {
            return "$latitude,$longitude"
        }
    }
    var manualLocation =
            ALocation(13.739712414289736,100.53311438774314)

//    private var _currentLocation = MutableLiveData<ALocation>()
    private var _currentLocation = MutableLiveData(manualLocation)
    val currentLocation: LiveData<ALocation>
        get() = _currentLocation


    var destination: String = ""
    set(value) {
        field = value
        runBlocking {
            Timber.i("Fetching direction")
            setDirection(getDirection(currentLocation.value?.toString() ?: "$manualLocation", value))
            Timber.i("Direction fetching completed.")
        }
    }


    private var _direction = MutableLiveData<Direction>()
    val direction: LiveData<Direction>
        get() = _direction

    // Location service provider with fine+coarse
    // This is what get call to get the location lat lng
    private val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    // Location requester
    private val locationRequest = LocationRequest().apply {
//        interval = TimeUnit.SECONDS.toMillis(1)
//        fastestInterval = TimeUnit.SECONDS.toMillis(1)
//        maxWaitTime = TimeUnit.MINUTES.toMillis(2)
//        interval = TimeUnit.MILLISECONDS.toMillis(300)
//        fastestInterval = TimeUnit.MILLISECONDS.toMillis(100)
        interval = 0
        fastestInterval = 0
        maxWaitTime = 0
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    // Location callback
    // Callback when FusedLocationProvider get new location
    private val locationCallback = object: LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)

            if(locationResult.lastLocation != null) {
                // Save location
                _currentLocation.value = ALocation(locationResult.lastLocation.latitude, locationResult.lastLocation.longitude)
                Timber.i("Location callback: ${_currentLocation.value.toString()}")
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
        Timber.i("Setting direction")
        _direction.value = value
    }

    fun startLocationUpdates() {
        Timber.i("Start location update")
        try {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper())

            fusedLocationProviderClient.getCurrentLocation(LocationRequest.PRIORITY_LOW_POWER, null).addOnFailureListener {result ->
                if(result is ApiException) {
                    Timber.i("Setting up mock-up location update")
                    thread(start = true, isDaemon = true) {
                        Timer().scheduleAtFixedRate(object: TimerTask() {
                            override fun run() {
                                Timber.i("Mock up walking...")
                                manualLocation = ALocation(manualLocation.latitude + 0.0001, manualLocation.longitude + 0.0001)
                                _currentLocation.postValue(manualLocation)
                            }
                        },0L,1000L)
                    }
                }
            }
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
    var point_count = 0

    var stopLocation: JSONLatLng? = null
    fun getStop(route_count: Int, leg_count: Int, step_count: Int, point_count: Int): LatLng? {
        /**
         * Get next step in point in legs in Routes in direction
         */
        var next_stop: LatLng? = null
        if (direction.value != null) {
            val polyline = direction.value!!.routes?.get(route_count)?.legs?.get(leg_count)?.steps?.get(step_count)?.polyline?.points
            val points = PolyUtil.decode(polyline)
//            Log.i("TestPointCount", "${route_count}, ${leg_count}, ${step_count}, ${point_count}, ${points.size}")
            next_stop = points[point_count]
        }
        return next_stop

/*        if (step_count == -1) {
            var start_location: JSONLatLng? = null
            if (_direction.value != null) {
                start_location = _direction.value!!.routes?.get(route_count)?.legs?.get(leg_count)?.steps?.get(step_count+1)?.start_location
            }
            return start_location?.let { LatLng(it.lat!!, it.lng!!) }
        }
        var end_location: JSONLatLng? = null
        if (_direction.value != null) {
            end_location = _direction.value!!.routes?.get(route_count)?.legs?.get(leg_count)?.steps?.get(step_count)?.end_location
            }
        return end_location?.let { LatLng(it.lat!!, it.lng!!) }*/
        }

    fun getStop(position: IntArray): LatLng? {
        return getStop(position[0], position[1], position[2], position[3])
    }

    fun resetCounting() {
        /**
         * Reset every counting to start
         */
        route_count = 0
        leg_count = 0
        step_count = 0
        point_count = 0
    }

    fun getRouteLegStepPointSize(route_count: Int, leg_count: Int, step_count: Int): IntArray {
        if (direction.value == null ){
            return intArrayOf(-1, -1, -1, -1)
        }
        val route = direction.value!!.routes
        val route_size = route?.size
        val leg = route?.get(route_count)?.legs
        val leg_size = leg?.size
        val step = leg?.get(leg_count)?.steps
        val step_size = step?.size
        val point = PolyUtil.decode(step?.get(step_count)?.polyline?.points)
        val point_size = point.size
        if (step_size != null && leg_size != null && route_size != null) {
            return intArrayOf(route_size, leg_size, step_size, point_size)
        }
        return intArrayOf(-1, -1, -1, -1)
    }

    fun incrementCounting(route_count: Int, leg_count: Int, step_count: Int, point_count: Int): IntArray {
//        Log.i("Test", "Increment")
        val sizes = getRouteLegStepPointSize(route_count, leg_count, step_count)
        val result = intArrayOf(route_count, leg_count, step_count, point_count)

        // Can be change to recursive later
        result[3] += 1
        if (sizes[3] >= 0 && result[3] >= sizes[3]) {
            result[3] = 0
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
        }
        return result
    }

    fun incrementCounting(position: IntArray): IntArray {
        return incrementCounting(position[0], position[1], position[2], position[3])
    }

    fun decrementCounting(route_count: Int, leg_count: Int, step_count: Int, point_count: Int): IntArray {
//        Log.i("Test", "Decrement, ${route_count}, ${leg_count}, ${step_count}, ${point_count}")
        val sizes = getRouteLegStepPointSize(route_count, leg_count, step_count)
        val result = intArrayOf(route_count, leg_count, step_count, point_count)
        if (sizes[0] > 0 && sizes[1] > 0 && sizes[2] > 0 && sizes[3] > 0) {
            result[3] -= 1
            if (result[3] < 0) {
                result[2] -= 1
                if (result[2] < 0) {
                    result[1] -= 1
                    if (result[1] < 0) {
                        result[0] -= 1
                        if (result[0] < 0) {
                            result[0] = sizes[0]-1
                        }
                        val leg = direction.value!!.routes?.get(result[0])?.legs
                        val leg_size = leg?.size
                        if (leg_size != null) {
                            result[1] = leg_size-1
                        }
                    }
                    val step = direction.value!!.routes?.get(result[0])?.legs?.get(result[1])?.steps
                    val step_size = step?.size
                    if (step_size != null) {
                        result[2] = step_size-1
                    }
                }
                val point = PolyUtil.decode(direction.value!!.routes?.get(result[0])?.legs?.get(result[1])?.steps?.get(result[2])?.polyline?.points)
                val point_size = point.size
                result[3] = point_size-1
            }
            return result
        }
        return sizes
    }

    private fun distanceTo(destination: LatLng): Float {
        val result = FloatArray(3)
        android.location.Location.distanceBetween(_currentLocation.value!!.latitude, _currentLocation.value!!.longitude, destination.latitude, destination.longitude, result)
        return result[0]
    }

    fun distanceLeft(): Float? {
//        _stopCount.postValue(StopCount(route_count, leg_count, step_count, point_count))
        return getStop(_stopCount.value!!.countArray)?.let { distanceTo(it) }
    }
    var minimumDistance = 50.0f
    val inDistance = 10.0f
//    private fun getNextPosition(): LatLng? {
//        /**
//         * Determine next position
//         *
//         * Input:
//         *  - Current location: LatLng
//         *  - Last Position: LatLng
//         *  - Next Position: LatLng
//         *  - epsilon: Float = minimum value that have to request new direction
//         * Output:
//         *  - LatLng of the position that have to nanivate to
//         *
//         *  Algorithm
//         *   a = distance from current_location to heading_stop
//         *   b = distance from current_location to current_stop
//         *   c = distance from last_stop to heading_stop
//         *
//         *   if a < c and b < c then
//         *      next_position = heading_position
//         *   else if a > c and b < epsilon then
//         *      next_position = last_position
//         *      current_count = prev_count
//         *   else if b > c and a < epsilon then
//         *      next_position = next_position
//         *      current_count = heading_count
//         *   else if a > c and b > epsilon or b > c and a > epsilon then
//         *      request new direction
//         */
//        val currentStop = getStop(route_count, leg_count, step_count, point_count)
//        val nextCount = incrementCounting(route_count, leg_count, step_count, point_count)
//        val nextStop = getStop(nextCount[0], nextCount[1], nextCount[2], nextCount[3])
//        if (currentStop != null && nextStop != null) {
//            val distanceCurrent = distanceTo(currentStop)
//            val distanceNext = distanceTo(nextStop)
//            val temp = FloatArray(1)
//            android.location.Location.distanceBetween(currentStop.latitude, currentStop.longitude, nextStop.latitude, nextStop.longitude, temp)
//            val distanceBetween = temp[0]
//            Log.i("TestDistance", "${distanceBetween}, ${distanceNext < distanceBetween}, ${distanceCurrent<distanceBetween}, ${distanceNext<minimumDistance}, ${distanceCurrent<minimumDistance}")
//            if (distanceNext < distanceBetween && distanceCurrent < distanceBetween) {
//                return nextStop
//            } else if (distanceNext > distanceBetween && distanceCurrent < minimumDistance) {
//                val prevCount = decrementCounting(route_count, leg_count, step_count, point_count)
//                route_count = prevCount[0]
//                leg_count = prevCount[1]
//                step_count = prevCount[2]
//                point_count = prevCount[3]
//                return currentStop
//            } else if (distanceCurrent > distanceBetween && distanceNext < minimumDistance) {
//                val newCount = incrementCounting(nextCount[0], nextCount[1], nextCount[2], nextCount[3])
//                route_count = nextCount[0]
//                leg_count = nextCount[1]
//                step_count = nextCount[2]
//                point_count = nextCount[3]
//                return getStop(newCount[0], newCount[1], newCount[2], newCount[3])
//            } else if ((distanceNext > distanceBetween && distanceCurrent > minimumDistance) || (distanceCurrent > distanceBetween && distanceNext > minimumDistance)) {
//                // TODO: find new direction
//                resetCounting()
//                return null
//            }
//        }
//        return null
//    }

    fun getStopPoint(): LatLng? {
        var currentCount = intArrayOf(route_count, leg_count, step_count, point_count)
        var currentPoint = getStop(currentCount)
        var nextCount = incrementCounting(currentCount)
        var nextPoint = getStop(nextCount)
        val baseCount = currentCount.copyOf()
        while (!baseCount.contentEquals(nextCount)) {
            val result = FloatArray(1)
            android.location.Location.distanceBetween(
                currentPoint!!.latitude,
                currentPoint.longitude,
                nextPoint!!.latitude,
                nextPoint.longitude,
                result
            )
            val pointToPoint = result[0]
            val pointToHere = distanceTo(currentPoint)
            if (pointToHere <= pointToPoint) {
                val ratio = pointToHere / pointToPoint
                val midPoint = SphericalUtil.interpolate(currentPoint, nextPoint, ratio.toDouble())
                val offset = distanceTo(midPoint)
                if (offset <= minimumDistance || pointToHere < inDistance) {
                    route_count = currentCount[0]
                    leg_count = currentCount[1]
                    step_count = currentCount[2]
                    point_count = currentCount[3]
//                    _stopCount.postValue(StopCount(route_count, leg_count, step_count, point_count))
                    _stopCount.value = StopCount(nextCount)
                    return nextPoint
                } else {
//                    // TODO: find new direction
//                    val prevCount =
//                        decrementCounting(route_count, leg_count, step_count, point_count)
//                    route_count = prevCount[0]
//                    leg_count = prevCount[1]
//                    step_count = prevCount[2]
//                    point_count = prevCount[3]
//                    _stopCount.value = StopCount(currentCount)
//                    return currentPoint
                    runBlocking {
                        Timber.i("Fetching direction")
                        setDirection(getDirection(currentLocation.value?.toString() ?: "$manualLocation", destination))
                        Timber.i("Direction fetching completed.")
                    }
                    resetCounting()
                    return getStop(route_count, leg_count, step_count, point_count)
                }
            }
            currentCount = nextCount.copyOf()
            currentPoint = getStop(currentCount)
            nextCount = incrementCounting(nextCount)
            nextPoint = getStop(nextCount)
        }
        runBlocking {
            Timber.i("Fetching direction")
            setDirection(getDirection(currentLocation.value?.toString() ?: "$manualLocation", destination))
            Timber.i("Direction fetching completed.")
        }
        resetCounting()
        return getStop(route_count, leg_count, step_count, point_count)
    }


    fun getBearingToNextPosition(): Float {
        val result = FloatArray(3)
//        getNextPosition()?.let {
//            Location.distanceBetween(_currentLocation.value!!.latitude, _currentLocation.value!!.longitude,
//                it.latitude, it.longitude, result)
//        }
        getStopPoint()?.let {
            android.location.Location.distanceBetween(_currentLocation.value!!.latitude, _currentLocation.value!!.longitude, it.latitude, it.longitude, result)
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

    // Test purpose
    private val arrowLength = 50.0
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

    class StopCount(val countArray: IntArray) {
        override fun toString(): String {
            return "${countArray[0]}, ${countArray[1]}, ${countArray[2]}, ${countArray[3]}"
        }
    }
    private val _stopCount = MutableLiveData(StopCount(intArrayOf(route_count, leg_count, step_count, point_count)))
    val stopCount: LiveData<StopCount>
        get() = _stopCount

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
