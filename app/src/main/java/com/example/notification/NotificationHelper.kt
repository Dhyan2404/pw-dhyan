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

    fun sendCustomNotification(title: String, message: String, notificationId: Int = NOTIFICATION_ID + (0..999).random()) {
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
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, builder.build())
        }
    }

    fun sendBatchMotivationSpam(scope: CoroutineScope, count: Int = 10) {
        val quotes = listOf(
            "Padh lo beta mauka hai, baki sab dhokha hai! 📚",
            "IAS / IIT banoge ya reels scroll karoge? Utho aur padho! 🔥",
            "IIT Bombay CSE bula raha hai! Consistency is Selection! 🎯",
            "Dhyan Sir is watching your progress! Eyes on the goal! 👀",
            "Selection chahiye ya excuses? Lecture complete karo abhi! ⚡",
            "Notification band kar, book khol! Target pura karo! 📖",
            "Ek aur DPP solve karo, rank 1 tumhari hogi! 🏆",
            "Aaj ki mehnat, kal ka result! Revision chalu karo! ⏰",
            "Mummy Papa ka sapna pura karna hai ya nahi? Focus! 💯",
            "PW Dhyan Study Power Mode: Stay relentless! 🚀"
        )
        scope.launch(Dispatchers.Main) {
            for (i in 0 until count.coerceAtMost(quotes.size)) {
                sendCustomNotification(
                    title = "🔥 Study Alert #${i + 1}",
                    message = quotes[i],
                    notificationId = 3000 + i
                )
                delay(350)
            }
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
