package com.oryn.terminal

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/** Maintains PTYs independently of the UI task; never restarts after process death. */
class KlyroSessionService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val check = object : Runnable {
        override fun run() {
            KlyroSessionRuntime.expireIfIdle()
            if (KlyroSessionRuntime.sessions.hasLiveSessions()) {
                handler.postDelayed(this, 60_000L)
            } else stopSelf()
        }
    }

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Active terminal sessions",
                NotificationManager.IMPORTANCE_LOW).apply {
                description = "Keeps interactive sessions running in the background"
                enableVibration(false)
                setSound(null, null)
                setShowBadge(false)
            })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        KlyroSessionRuntime.expireIfIdle()
        if (!KlyroSessionRuntime.sessions.hasLiveSessions()) {
            stopSelf()
            return START_NOT_STICKY
        }
        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setContentTitle("Klyro Shell")
            .setContentText("Terminal sessions active")
            .setSmallIcon(R.drawable.ic_klyro_mark_monochrome)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        handler.removeCallbacks(check)
        handler.postDelayed(check, 60_000L)
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        handler.removeCallbacks(check)
        // The service is a notification owner, not the PTY owner. Android may
        // recreate or stop this service while the process and sessions survive.
        // Explicit session close and the idle policy own PTY termination.
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL = "klyro_sessions"
        private const val NOTIFICATION_ID = 1001
        fun ensureRunning(context: Context) {
            if (!KlyroSessionRuntime.sessions.hasLiveSessions()) return
            ContextCompat.startForegroundService(context.applicationContext,
                Intent(context, KlyroSessionService::class.java))
        }
        fun stopIfEmpty(context: Context) {
            if (!KlyroSessionRuntime.sessions.hasLiveSessions()) {
                context.applicationContext.stopService(
                    Intent(context, KlyroSessionService::class.java))
            }
        }
    }
}
