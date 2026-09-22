package com.example.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.R
import com.example.security.SecurityManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NotificationHelper(private val context: Context) {
    companion object {
        const val CHANNEL_ID = "pw_dhyan_notifications"
        const val CHANNEL_NAME = "PW DHYAN Reminders"
        const val NOTIFICATION_ID = 2404
        const val INTERVAL_MILLIS = 5 * 60 * 1000L // 5 minutes
    }

    private var job: Job? = null

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Periodic notifications for study portal users"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun sendDhyanNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("PW DHYAN")
            .setContentText("Built with ❤️ by Dhyan • Dedicated Study Browser")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID, builder.build())
        }
    }

    fun startPeriodicNotification(scope: CoroutineScope, securityManager: SecurityManager) {
        stopPeriodicNotification()
        job = scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(INTERVAL_MILLIS)
                // Only send to normal users, NEVER to admin!
                if (!securityManager.isPermanentUnlocked()) {
                    launch(Dispatchers.Main) {
                        sendDhyanNotification()
                    }
                }
            }
        }
    }

    fun stopPeriodicNotification() {
        job?.cancel()
        job = null
    }
}
