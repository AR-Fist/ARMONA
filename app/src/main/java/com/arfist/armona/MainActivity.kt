package com.arfist.armona

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import org.opencv.android.OpenCVLoader

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity";

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if(OpenCVLoader.initDebug()) {
            Log.d(TAG,"Load OpenCV successful");
        }
        else {
            Log.d(TAG,"Load OpenCV fail");
        }
    }
}