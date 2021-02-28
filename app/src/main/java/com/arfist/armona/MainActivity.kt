package com.arfist.armona

import androidx.appcompat.app.AppCompatActivity
import android.Manifest
import android.os.Bundle
import android.util.Log
import org.opencv.android.OpenCVLoader
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

        // Init OpenCV
        if(OpenCVLoader.initDebug()) {
            Timber.i("Load OpenCV successful");
        }
        else {
            Timber.d("Load OpenCV fail");
        }


        // Init for PlaceSDK
        Places.initialize(applicationContext, BuildConfig.mapsApiKey)

        val navController = this.findNavController(R.id.navHostFragment)
        NavigationUI.setupActionBarWithNavController(this, navController)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = this.findNavController(R.id.navHostFragment)
        return navController.navigateUp()
    }

    companion object{
        val permissionList = arrayListOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        const val PERMISSION_REQUEST_MAP = 1
    }
}