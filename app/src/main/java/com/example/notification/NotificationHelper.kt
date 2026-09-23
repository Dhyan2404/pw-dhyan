package com.example.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
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

        /**
         * Checks whether notification permissions are fully granted and enabled in system settings.
         */
        fun hasPermission(context: Context): Boolean {
            val notificationManagerCompat = NotificationManagerCompat.from(context)
            if (!notificationManagerCompat.areNotificationsEnabled()) {
                return false
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) return false
            }
            return true
        }
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
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Important alerts & reminders for PW DHYAN study portal"
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun sendDhyanNotification() {
        val notificationManagerCompat = NotificationManagerCompat.from(context)
        if (!notificationManagerCompat.areNotificationsEnabled()) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("PW DHYAN")
            .setContentText("Built with ❤️ by Dhyan • Dedicated Study Browser")
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)

        with(notificationManagerCompat) {
            notify(NOTIFICATION_ID, builder.build())
        }
    }

    fun sendCustomNotification(title: String, message: String, notificationId: Int = NOTIFICATION_ID + (0..999).random()) {
        val notificationManagerCompat = NotificationManagerCompat.from(context)
        if (!notificationManagerCompat.areNotificationsEnabled()) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                android.widget.Toast.makeText(context, "Notifications are disabled in device settings for PW DHYAN!", android.widget.Toast.LENGTH_SHORT).show()
            }
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(context, "Notification permission not granted! Please allow notifications in App Settings.", android.widget.Toast.LENGTH_SHORT).show()
                }
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)

        with(notificationManagerCompat) {
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

    fun sendBurstNotification(
        title: String,
        message: String,
        count: Int = 5,
        delayMillis: Long = 300L
    ) {
        val notificationManagerCompat = NotificationManagerCompat.from(context)
        if (!notificationManagerCompat.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        CoroutineScope(Dispatchers.Main).launch {
            val baseId = 7000 + (0..999).random()
            val total = count.coerceIn(1, 20)
            for (i in 1..total) {
                sendCustomNotification(
                    title = if (total > 1) "⚡ [$i/$total] $title" else title,
                    message = message,
                    notificationId = baseId + i
                )
                if (delayMillis > 0 && i < total) {
                    delay(delayMillis)
                }
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
