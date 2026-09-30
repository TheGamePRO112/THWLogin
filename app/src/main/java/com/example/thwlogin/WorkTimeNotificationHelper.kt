package com.example.thwlogin

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object WorkTimeNotificationHelper {
    private const val CHANNEL_ID = "thw_work_time_channel_v2"
    private const val CHANNEL_NAME = "Dienstzeit Tracker"
    const val NOTIFICATION_ID = 1001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Zeigt den aktiven Dienstzeit-Timer an"
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showOrUpdateNotification(context: Context, startTimeMillis: Long) {
        if (!WorkTimeManager.isTracking(context) || startTimeMillis <= 0L) {
            cancelNotification(context)
            return
        }

        createNotificationChannel(context)

        // Main Activity Intent (Tap notification)
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Stop Action Intent (Tap "Dienst beenden" button)
        val stopIntent = Intent(context, WorkTimeNotificationReceiver::class.java).apply {
            action = WorkTimeNotificationReceiver.ACTION_STOP_WORK_TRACKING
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val logoBitmap = try {
            BitmapFactory.decodeResource(context.resources, R.drawable.thw_login_ico_png)
        } catch (_: Exception) {
            null
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.thw_login_ico_png)
            .apply {
                if (logoBitmap != null) {
                    setLargeIcon(logoBitmap)
                }
            }
            .setContentTitle("THW Login – Dienstzeit aktiv ⏱️")
            .setContentText("Dienst läuft seit...")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setUsesChronometer(true)
            .setWhen(startTimeMillis)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(mainPendingIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Dienst beenden",
                stopPendingIntent
            )

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {
            // Permission missing
        }
    }

    fun cancelNotification(context: Context) {
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancel(NOTIFICATION_ID)
        } catch (_: Exception) {}
    }
}
