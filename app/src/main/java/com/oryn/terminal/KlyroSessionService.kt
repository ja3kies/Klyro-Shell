package com.oryn.terminal

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
            if (KlyroSessionRuntime.sessions.hasLiveSessions()) {
                handler.postDelayed(this, 60_000L)
            } else stopSelf()
        }
    }

    override fun onCreate() {
        super.onCreate()
        com.oryn.terminal.diagnostics.FreezeRecorder.serviceActive = true
        com.oryn.terminal.diagnostics.FreezeRecorder.event(com.oryn.terminal.diagnostics.FreezeEvents.Code.SERVICE_START)
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
        if (!KlyroSessionRuntime.sessions.hasLiveSessions()) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        val notification = sessionNotification(this, KlyroSessionRuntime.sessions.liveSessionCount)
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
        com.oryn.terminal.diagnostics.FreezeRecorder.serviceActive = false
        com.oryn.terminal.diagnostics.FreezeRecorder.event(com.oryn.terminal.diagnostics.FreezeEvents.Code.SERVICE_STOP)
        handler.removeCallbacks(check)
        // The service is a notification owner, not the PTY owner. Android may
        // recreate or stop this service while the process and sessions survive.
        // Only explicit session close owns PTY termination.
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL = "klyro_sessions"
        private const val NOTIFICATION_ID = 1001
        private fun sessionNotification(context: Context, sessionCount: Int): Notification {
            val openApp = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).apply {
                    this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            return NotificationCompat.Builder(context, CHANNEL)
                .setContentTitle("Klyro Shell")
                .setContentText("$sessionCount active terminal session" +
                    if (sessionCount == 1) "" else "s")
                .setContentIntent(openApp)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.ic_klyro_mark_monochrome)
                .setOngoing(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
        }

        /** Update an existing service notification; never restart an FGS from a background callback. */
        fun refreshSessionNotification(context: Context) {
            if (!KlyroSessionRuntime.sessions.hasLiveSessions()) {
                stopIfEmpty(context)
            } else if (com.oryn.terminal.diagnostics.FreezeRecorder.serviceActive) {
                try {
                    context.getSystemService(NotificationManager::class.java).notify(
                        NOTIFICATION_ID, sessionNotification(context, KlyroSessionRuntime.sessions.liveSessionCount))
                } catch (denied: SecurityException) {
                    android.util.Log.w("KlyroSessionService", "Cannot update session notification", denied)
                }
            }
        }

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
