package com.example.notification

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R
import com.example.security.PushNotificationItem
import com.example.security.SecurityManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Silent Background Service to sync push notifications from PW Dhyan Cloud.
 * Runs silently in the background without any persistent or unremovable notifications.
 */
class PushNotificationService : Service() {
    companion object {
        private const val TAG = "PushNotifService"
        const val FOREGROUND_CHANNEL_ID = "pw_dhyan_sync_channel"
        const val FOREGROUND_NOTIF_ID = 9901
        private const val PREFS_NOTIF = "pw_push_service_prefs"
        private const val KEY_PROCESSED_IDS = "processed_notification_ids"

        @Volatile
        private var isRunning = false

        fun start(context: Context) {
            try {
                val intent = Intent(context, PushNotificationService::class.java)
                context.startService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to start PushNotificationService: ${e.message}")
            }
            // Ensure WorkManager periodic sync is registered for offline recovery
            NotificationSyncWorker.schedulePeriodicSync(context)
            checkPendingNotifications(context)
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, PushNotificationService::class.java)
                context.stopService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to stop PushNotificationService: ${e.message}")
            }
        }

        /**
         * Recovers and delivers any push notifications or alerts that were dispatched
         * while the user's device was offline.
         */
        fun checkPendingNotifications(context: Context) {
            val appContext = context.applicationContext
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val sm = SecurityManager(appContext)
                    val myDeviceId = sm.getDeviceId()
                    val myPasskey = sm.getCurrentUserSession().passkey
                    val notifHelper = NotificationHelper(appContext)

                    val sp = appContext.getSharedPreferences(PREFS_NOTIF, Context.MODE_PRIVATE)
                    val saved = sp.getStringSet(KEY_PROCESSED_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()

                    val fs = FirebaseFirestore.getInstance()
                    val cutoff = System.currentTimeMillis() - 48 * 60 * 60 * 1000L

                    // 1. Query push_notifications collection
                    val snapshot = fs.collection("push_notifications")
                        .whereGreaterThan("timestamp", cutoff)
                        .get()
                        .await()

                    var hasNew = false
                    snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { PushNotificationItem.fromFirestoreMap(it, doc.id) }
                    }.filter { it.isActive }.forEach { item ->
                        val isForMe = when (item.targetType) {
                            "DEVICE" -> item.targetValue.equals(myDeviceId, ignoreCase = true)
                            "PASSKEY" -> item.targetValue.equals(myPasskey, ignoreCase = true)
                            else -> true
                        }
                        if (isForMe && !saved.contains(item.id)) {
                            saved.add(item.id)
                            hasNew = true
                            launch(Dispatchers.Main) {
                                try {
                                    if (item.isBurst || item.burstCount > 1) {
                                        notifHelper.sendBurstNotification(item.title, item.message, item.burstCount)
                                    } else {
                                        notifHelper.sendCustomNotification(item.title, item.message)
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error displaying recovered notification: ${e.message}")
                                }
                            }
                        }
                    }

                    // 2. Also check user_sessions for direct pending alert
                    try {
                        val sessionDoc = fs.collection("user_sessions").document(myDeviceId).get().await()
                        if (sessionDoc.exists()) {
                            val alert = sessionDoc.get("pendingAlert") as? Map<*, *>
                            if (alert != null) {
                                val alertId = alert["id"] as? String ?: "alert_${alert["timestamp"]}"
                                val alertTime = (alert["timestamp"] as? Number)?.toLong() ?: 0L
                                if (alertTime > cutoff && !saved.contains(alertId)) {
                                    saved.add(alertId)
                                    hasNew = true
                                    val title = alert["title"] as? String ?: "🔥 PW DHYAN ALERT"
                                    val msg = alert["message"] as? String ?: ""
                                    val isBurst = alert["isBurst"] as? Boolean ?: false
                                    val burstCount = (alert["burstCount"] as? Number)?.toInt() ?: 1
                                    launch(Dispatchers.Main) {
                                        if (isBurst || burstCount > 1) {
                                            notifHelper.sendBurstNotification(title, msg, burstCount)
                                        } else {
                                            notifHelper.sendCustomNotification(title, msg)
                                        }
                                    }
                                }
                            }
                        }
                    } catch (_: Exception) {}

                    if (hasNew) {
                        val toSave = if (saved.size > 200) saved.toList().takeLast(200).toSet() else saved.toSet()
                        sp.edit().putStringSet(KEY_PROCESSED_IDS, toSave).apply()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Offline pending notification recovery note: ${e.message}")
                }
            }
        }
    }

    private var listenerRegistration: ListenerRegistration? = null
    private val processedIds = mutableSetOf<String>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true

        // Proactively clear and remove any legacy persistent foreground notifications
        try {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancel(FOREGROUND_NOTIF_ID)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm?.deleteNotificationChannel(FOREGROUND_CHANNEL_ID)
            }
        } catch (_: Exception) {}

        loadProcessedIds()
        attachCloudListener()
        Log.d(TAG, "PushNotificationService created and active (silent background sync)")
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
                        doc.data?.let { PushNotificationItem.fromFirestoreMap(it, doc.id) }
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
