package com.example.recordingapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_CODE_SCREEN_CAPTURE = 1000
    }

    private var mProjectionManager: MediaProjectionManager? = null
    private var mButtonStart: Button? = null
    private var mApps: MutableList<ApplicationInfo>? = null
    private var mAdapter: AppListAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        mButtonStart = findViewById(R.id.start_button)
        mButtonStart!!.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            } else {
                startRecording()
            }
        }

        mApps = loadApps()
        mAdapter = AppListAdapter(this, mApps!!)
        val listViewApps = findViewById<ListView>(R.id.app_list)
        listViewApps.adapter = mAdapter
    }

    private fun startRecording() {
        val selectedApp = mApps!![mAdapter!!.selectedPosition]

        val intent = Intent(this, RecordingService::class.java)
        intent.putExtra(RecordingService.EXTRA_APP_NAME, selectedApp.loadLabel(packageManager).toString())
        startService(intent)

        mButtonStart!!.isEnabled = false
        Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE && resultCode == RESULT_OK && data != null) {
            val projection = mProjectionManager!!.getMediaProjection(resultCode, data)
            if (projection != null) {
                val intent = Intent(this, RecordingService::class.java)
                intent.putExtra(RecordingService.EXTRA_PROJECTION, projection as IntArray)
                startService(intent)

                mButtonStart!!.isEnabled = false
                Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startRecording()
        }
    }

    private fun loadApps(): MutableList<ApplicationInfo> {
        val apps: MutableList<ApplicationInfo> = mutableListOf()

        val pm: PackageManager = packageManager
        val packs = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        for (pack in packs) {
            if (!isSystemPackage(pack) ) {
                Log.d("Kane", "APP NAME IS: ${pack.loadLabel(packageManager)} ")
                apps.add(pack)
            }
        }

        return apps
    }

    private fun isSystemPackage(packageInfo: ApplicationInfo): Boolean {
        return (packageInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
    }


    fun enableStartButton(value: Boolean) {
        mButtonStart?.isEnabled = value
    }

}