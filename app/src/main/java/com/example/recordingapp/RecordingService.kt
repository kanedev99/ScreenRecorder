package com.example.recordingapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import java.io.File

class RecordingService : Service() {

    private var mNotificationManager: NotificationManager? = null
    private var mRecordingNotificationBuilder: NotificationCompat.Builder? = null
    private var mAppPackage: String? = null
    private var mScreenCaptureIntent: Intent? = null
    private var mRecording = false
    private var mStopIntent: PendingIntent? = null

    private val NOTIFICATION_CHANNEL_ID = "MyAppRecordingChannel"
    private val NOTIFICATION_ID = 1234

    private var mMediaRecorder: MediaRecorder? = null
    private lateinit var mAppName: String
    private lateinit var mOutputFileUri: Uri

    companion object {
        private const val RECORDING_NOTIFICATION_CHANNEL_ID = "recording_channel"
        private const val CHANNEL_ID = "my_channel_id"
        const val ACTION_STOP_RECORDING = "stop_recording"
        const val EXTRA_APP_NAME = "app_name"
        const val EXTRA_PROJECTION = "projection"
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        mMediaRecorder = MediaRecorder()
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createRecordingNotificationChannel()
        mRecordingNotificationBuilder = NotificationCompat.Builder(this, RECORDING_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Screen Recording")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            mAppName = intent.getStringExtra("appName").toString()
            mAppPackage = intent.getStringExtra("appPackage")
            mScreenCaptureIntent = intent.getParcelableExtra("screenCaptureIntent")
        }

        if (mAppName != null && mAppPackage != null && mScreenCaptureIntent != null && !mRecording) {
            startRecordingInternal()
        }

        // Define the stop intent
        val stopIntent = Intent(this, RecordingService::class.java)
        stopIntent.action = ACTION_STOP_RECORDING
        mStopIntent = PendingIntent.getService(this, 0, stopIntent, 0)

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecordingInternal()
        mNotificationManager?.cancel(NOTIFICATION_ID)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startRecordingInternal() {
        mRecording = true

        val notificationText = "Recording $mAppName"
        mRecordingNotificationBuilder!!.setContentText(notificationText)

        val stopIntent = Intent(this, RecordingService::class.java)
        stopIntent.action = "stopRecording"
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, 0)

        mRecordingNotificationBuilder!!.addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)
        startForeground(NOTIFICATION_ID, mRecordingNotificationBuilder!!.build())

        mScreenCaptureIntent!!.putExtra(MediaStore.EXTRA_OUTPUT, getOutputFileUri())
        startForegroundService(mScreenCaptureIntent)
    }

    private fun stopRecordingInternal() {
        if (mRecording) {
            mRecording = false
            stopForeground(true)
            mMediaRecorder?.reset()
            mMediaRecorder?.release()
            stopService(mScreenCaptureIntent)
        }
    }

    private fun createRecordingNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                RECORDING_NOTIFICATION_CHANNEL_ID,
                "Screen Recording",
                NotificationManager.IMPORTANCE_LOW
            )
            mNotificationManager?.createNotificationChannel(channel)
        }
    }

    private fun getOutputFileUri(): Uri {
        val outputDirectory = File(Environment.getExternalStorageDirectory(), "ScreenRecordings")
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs()
        }
        val outputFile = File(outputDirectory, "${mAppName}_${System.currentTimeMillis()}.mp4")
        return FileProvider.getUriForFile(this, "$packageName.fileprovider", outputFile)
    }

    fun startRecording(appName: String) {
        mAppName = appName
        mOutputFileUri = getOutputFileUri()

        mMediaRecorder = MediaRecorder()

        val displayMetrics = resources.displayMetrics
        val displayWidth = displayMetrics.widthPixels
        val displayHeight = displayMetrics.heightPixels
        val displayDensity = displayMetrics.densityDpi

        mMediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
        mMediaRecorder!!.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mMediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        mMediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        mMediaRecorder!!.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        mMediaRecorder!!.setVideoSize(displayWidth, displayHeight)
        mMediaRecorder!!.setVideoFrameRate(30)
        mMediaRecorder!!.setVideoEncodingBitRate(1000000)
        mMediaRecorder!!.setOutputFile(mOutputFileUri.path)
        mMediaRecorder!!.prepare()

        mMediaRecorder!!.start()

        val notificationText = "Recording $appName"
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(appName)
            .setContentText(notificationText)
            .setSmallIcon(R.drawable.ic_notification)
            .addAction(R.drawable.ic_stop, "Stop", mStopIntent)
            .setOngoing(true)

        startForeground(NOTIFICATION_ID, notificationBuilder.build())
    }

    fun stopRecording() {
        mMediaRecorder?.stop()
        mMediaRecorder?.release()
        mMediaRecorder = null

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)

        stopForeground(true)

        Toast.makeText(this, "Recording saved: ${mOutputFileUri.path}", Toast.LENGTH_SHORT).show()
    }

}
