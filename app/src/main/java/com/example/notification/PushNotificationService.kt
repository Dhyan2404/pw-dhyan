package com.example.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.security.PushNotificationItem
import com.example.security.SecurityManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * 24/7 Background & Foreground Service to sync push notifications from PW Dhyan Cloud.
 * Runs continuously even when app is minimized or closed so user never misses study alerts or bursts.
 */
class PushNotificationService : Service() {
    companion object {
        private const val TAG = "PushNotifService"
        const val FOREGROUND_CHANNEL_ID = "pw_dhyan_sync_channel"
        const val FOREGROUND_CHANNEL_NAME = "PW DHYAN Study Sync"
        const val FOREGROUND_NOTIF_ID = 9901
        private const val PREFS_NOTIF = "pw_push_service_prefs"
        private const val KEY_PROCESSED_IDS = "processed_notification_ids"

        @Volatile
        private var isRunning = false

        fun start(context: Context) {
            val intent = Intent(context, PushNotificationService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to start PushNotificationService: ${e.message}")
            }
        }
    }

    private var listenerRegistration: ListenerRegistration? = null
    private val processedIds = mutableSetOf<String>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        loadProcessedIds()
        startForegroundNotification()
        attachCloudListener()
        Log.d(TAG, "PushNotificationService created and active")
    }

    private fun loadProcessedIds() {
        try {
            val sp = getSharedPreferences(PREFS_NOTIF, Context.MODE_PRIVATE)
            val saved = sp.getStringSet(KEY_PROCESSED_IDS, emptySet()) ?: emptySet()
            processedIds.addAll(saved)
        } catch (_: Exception) {}
    }

    private fun markIdProcessed(id: String) {
        processedIds.add(id)
        if (processedIds.size > 200) {
            val toRemove = processedIds.take(50)
            processedIds.removeAll(toRemove.toSet())
        }
        try {
            val sp = getSharedPreferences(PREFS_NOTIF, Context.MODE_PRIVATE)
            sp.edit().putStringSet(KEY_PROCESSED_IDS, processedIds.toSet()).apply()
        } catch (_: Exception) {}
    }

    private fun startForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                FOREGROUND_CHANNEL_ID,
                FOREGROUND_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps study alerts and notifications synced with PW Dhyan Cloud"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val notification: Notification = NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
            .setContentTitle("PW DHYAN Study Sync")
            .setContentText("Cloud alerts and study notifications active")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(FOREGROUND_NOTIF_ID, notification)
    }

    private fun attachCloudListener() {
        listenerRegistration?.remove()
        try {
            val sm = SecurityManager(applicationContext)
            val myDeviceId = sm.getDeviceId()
            val myPasskey = sm.getCurrentUserSession().passkey
            val notifHelper = NotificationHelper(applicationContext)

            val fs = FirebaseFirestore.getInstance()
            listenerRegistration = fs.collection("push_notifications")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val items = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { PushNotificationItem.fromFirestoreMap(it) }
                    }
                    items.filter { it.isActive }.forEach { item ->
                        val isForMe = when (item.targetType) {
                            "DEVICE" -> item.targetValue.equals(myDeviceId, ignoreCase = true)
                            "PASSKEY" -> item.targetValue.equals(myPasskey, ignoreCase = true)
                            else -> true // "ALL"
                        }
                        if (isForMe && !processedIds.contains(item.id)) {
                            // Check if recent (last 24 hours)
                            if (System.currentTimeMillis() - item.timestamp < 24 * 60 * 60 * 1000L) {
                                markIdProcessed(item.id)
                                try {
                                    if (item.isBurst || item.burstCount > 1) {
                                        notifHelper.sendBurstNotification(item.title, item.message, item.burstCount)
                                    } else {
                                        notifHelper.sendCustomNotification(item.title, item.message)
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error displaying push notification: ${e.message}")
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach cloud listener in service: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (listenerRegistration == null) {
            attachCloudListener()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        listenerRegistration?.remove()
        listenerRegistration = null
        Log.d(TAG, "PushNotificationService destroyed")
    }
}
