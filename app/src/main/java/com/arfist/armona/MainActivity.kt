package com.arfist.armona

import androidx.appcompat.app.AppCompatActivity
import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.fragment.app.activityViewModels
import org.opencv.android.OpenCVLoader
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import com.arfist.armona.shared.SharedViewModel
import com.arfist.armona.utils.hasPermission
import com.google.android.libraries.places.api.Places
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private val sharedViewModel: SharedViewModel by viewModels()

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

        getPermission()
    }

    // Check which permission is not granted, then ask for it
    private fun getPermission() {
        Timber.i("Grant permission")

        val permissionRequest: MutableList<String> = ArrayList()
        for (permission in permissionList) {
            if (!applicationContext.hasPermission(permission)) {
                permissionRequest.add(permission)
            }
        }
        if (permissionRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionRequest.toTypedArray(), PERMISSION_REQUEST_MAP
            )
        } else {
            sharedViewModel.onPermissionGranted()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = this.findNavController(R.id.navHostFragment)
        return navController.navigateUp()
    }

    companion object{
        val permissionList = arrayListOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA
        )
        const val PERMISSION_REQUEST_MAP = 1
    }
}