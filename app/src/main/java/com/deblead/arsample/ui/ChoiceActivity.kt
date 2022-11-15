package com.deblead.arsample.ui

import android.app.Activity
import android.app.ActivityManager
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import com.xplora.arsample.R

class ChoiceActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choice)
        initListener()
    }

    private fun initListener() {
        findViewById<LinearLayout>(R.id.llModel).setOnClickListener {
            if(checkIsSupportedDeviceOrFinish(this)){
                startActivity(Intent(this, ArSampleActivity::class.java))
            }

        }
        findViewById<LinearLayout>(R.id.llChroma).setOnClickListener {
            if(checkIsSupportedDeviceOrFinish(this)){
                startActivity(Intent(this, ChromeVideoActivity::class.java))
            }
        }
    }


    /**
     * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
     * on this device.
     *
     *
     * Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
     *
     *
     * Finishes the activity if Sceneform can not run
     */
    private fun checkIsSupportedDeviceOrFinish(activity: Activity): Boolean {
        val openGlVersionString = (activity.getSystemService(ACTIVITY_SERVICE) as ActivityManager)
            .deviceConfigurationInfo
            .glEsVersion
        if (openGlVersionString.toDouble() < MIN_OPENGL_VERSION) {
            val TAG = "ArSample"
            Log.e(
                TAG,
                "Sceneform requires OpenGL ES 3.0 later"
            )
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                .show()
            activity.finish()
            return false
        }
        return true
    }
}