package com.cintra.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.cintra.app.MainActivity
import com.cintra.app.R

/**
 * Lightweight Android Foreground Service (`dataSync`) that keeps Cintra AI's
 * SSE response stream & file analysis alive when the user leaves the app or locks the screen.
 * Stops automatically as soon as the response finishes or is cancelled.
 */
class CintraBackgroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val statusText = intent?.getStringExtra(EXTRA_STATUS) ?: "جارٍ إكمال الرد في الخلفية..."
        ensureChannel()
        val notification = buildNotification(statusText)

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIF_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
                )
            } else {
                startForeground(NOTIF_ID, notification)
            }
        }
        return START_NOT_STICKY
    }

    private fun ensureChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Cintra Background AI",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "إبقاء توليد ردود Cintra AI نشطاً عند الخروج من التطبيق"
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Cintra AI")
            .setContentText(statusText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(pi)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "cintra_bg_generation"
        private const val NOTIF_ID = 4041
        private const val ACTION_START = "com.cintra.app.action.BG_START"
        private const val ACTION_STOP = "com.cintra.app.action.BG_STOP"
        private const val EXTRA_STATUS = "extra_status"

        fun start(context: Context, statusText: String = "جارٍ إكمال الرد في الخلفية...") {
            runCatching {
                val intent = Intent(context, CintraBackgroundService::class.java).apply {
                    action = ACTION_START
                    putExtra(EXTRA_STATUS, statusText)
                }
                ContextCompat.startForegroundService(context, intent)
            }
        }

        fun stop(context: Context) {
            runCatching {
                context.stopService(Intent(context, CintraBackgroundService::class.java))
            }
        }
    }
}
