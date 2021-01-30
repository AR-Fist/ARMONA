package com.arfist.armona

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import com.google.android.libraries.places.api.Places
import timber.log.Timber


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Logger
        Timber.plant(Timber.DebugTree())

        // Init for PlaceSDK
        Places.initialize(applicationContext, BuildConfig.mapsApiKey)

        val navController = this.findNavController(R.id.navHostFragment)
        NavigationUI.setupActionBarWithNavController(this, navController)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = this.findNavController(R.id.navHostFragment)
        return navController.navigateUp()
    }


//    fun createLocationRequest() {
//        val locationRequest = LocationRequest.create()?.apply {
//            interval = 10000
//            fastestInterval = 5000
//            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
//        }
//
//        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest!!)
//        val client: SettingsClient = LocationServices.getSettingsClient(this)
//        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())
//
//        task.addOnSuccessListener { locationSettingsResponse ->
//
//        }
//        task.addOnFailureListener { e ->
//            if (e is ResolvableApiException) {
//                try {
//                    e.startResolutionForResult(this@MainActivity, 0)
//                } catch (sendEx: IntentSender.SendIntentException) {
//
//                }
//            }
//        }
//    }

}