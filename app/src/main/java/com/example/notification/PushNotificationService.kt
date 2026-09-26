package com.example.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import com.example.security.PushNotificationItem
import com.example.security.SecurityManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Background Service to sync push notifications from PW Dhyan Cloud.
 * Dual-listens to both push_notifications and direct user_sessions/$myDeviceId alerts
 * for instant 0ms latency when online, and recovers missed offline notifications.
 */
class PushNotificationService : Service() {
    companion object {
        private const val TAG = "PushNotifService"
        const val FOREGROUND_CHANNEL_ID = "pw_dhyan_sync_channel"
        const val FOREGROUND_NOTIF_ID = 9901

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
                recoverMissedNotificationsInternal(appContext, useServerFirst = false)
            }
        }

        /**
         * Expedited server recovery: Forces a fetch directly from Cloud Firestore servers
         * (bypassing stale offline cache) to retrieve notifications dispatched while device was offline.
         */
        fun recoverMissedNotificationsFromServer(context: Context) {
            val appContext = context.applicationContext
            CoroutineScope(Dispatchers.IO).launch {
                recoverMissedNotificationsInternal(appContext, useServerFirst = true)
            }
        }

        private suspend fun recoverMissedNotificationsInternal(appContext: Context, useServerFirst: Boolean) {
            try {
                val sm = SecurityManager(appContext)
                val myDeviceId = sm.getDeviceId()
                val currentPasskey = sm.getCurrentUserSession().passkey
                val rawSessionCode = sm.getActiveSessionCode()
                val notifHelper = NotificationHelper(appContext)

                val fs = FirebaseFirestore.getInstance()
                val cutoff = System.currentTimeMillis() - 48 * 60 * 60 * 1000L

                // 1. Query push_notifications collection (Prefer SERVER source when recovering from offline)
                try {
                    val notifQuery = fs.collection("push_notifications")
                        .whereGreaterThan("timestamp", cutoff)

                    val snapshot = if (useServerFirst) {
                        try {
                            notifQuery.get(Source.SERVER).await()
                        } catch (_: Exception) {
                            notifQuery.get().await()
                        }
                    } else {
                        notifQuery.get().await()
                    }

                    val items = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { PushNotificationItem.fromFirestoreMap(it, doc.id) }
                    }

                    items.filter { it.isActive }.forEach { item ->
                        val isForMe = item.matchesTarget(myDeviceId, currentPasskey, rawSessionCode)
                        if (isForMe && !NotificationTracker.isProcessed(appContext, item.id)) {
                            NotificationTracker.markProcessed(appContext, item.id)
                            CoroutineScope(Dispatchers.Main).launch {
                                try {
                                    val isBroadcast = item.targetType.equals("ALL", ignoreCase = true) || item.targetValue.equals("ALL", ignoreCase = true)
                                    if (!isBroadcast && (item.isBurst || item.burstCount > 1)) {
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
                } catch (e: Exception) {
                    Log.w(TAG, "Error querying push_notifications for offline catchup: ${e.message}")
                }

                // 2. Also check user_sessions for direct pending alert
                try {
                    val sessionDocRef = fs.collection("user_sessions").document(myDeviceId)
                    val sessionDoc = if (useServerFirst) {
                        try {
                            sessionDocRef.get(Source.SERVER).await()
                        } catch (_: Exception) {
                            sessionDocRef.get().await()
                        }
                    } else {
                        sessionDocRef.get().await()
                    }

                    if (sessionDoc.exists()) {
                        val alert = sessionDoc.get("pendingAlert") as? Map<*, *>
                        if (alert != null) {
                            val alertId = (alert["id"] as? String)?.takeIf { it.isNotBlank() } ?: "alert_${alert["timestamp"]}"
                            val alertTime = (alert["timestamp"] as? Number)?.toLong() ?: 0L
                            if (alertTime > cutoff && !NotificationTracker.isProcessed(appContext, alertId)) {
                                NotificationTracker.markProcessed(appContext, alertId)
                                val title = (alert["title"] as? String)?.takeIf { it.isNotBlank() } ?: "🔥 PW DHYAN ALERT"
                                val msg = (alert["message"] as? String) ?: ""
                                val isBurst = (alert["isBurst"] as? Boolean) ?: false
                                val burstCount = (alert["burstCount"] as? Number)?.toInt() ?: 1

                                val isBroadcast = alert["targetType"] == "ALL" || alert["type"] == "APP_UPDATE"
                                CoroutineScope(Dispatchers.Main).launch {
                                    if (!isBroadcast && (isBurst || burstCount > 1)) {
                                        notifHelper.sendBurstNotification(title, msg, burstCount)
                                    } else {
                                        notifHelper.sendCustomNotification(title, msg)
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error checking user_sessions pendingAlert for offline catchup: ${e.message}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Offline pending notification recovery note: ${e.message}")
            }
        }
    }

    private var pushListenerRegistration: ListenerRegistration? = null
    private var sessionAlertRegistration: ListenerRegistration? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true

        // Proactively clear and remove any persistent foreground notifications
        try {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancel(FOREGROUND_NOTIF_ID)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm?.deleteNotificationChannel(FOREGROUND_CHANNEL_ID)
            }
        } catch (_: Exception) {}

        attachCloudListeners()
        Log.d(TAG, "PushNotificationService created and active (silent dual-channel sync)")
    }

    private fun attachCloudListeners() {
        pushListenerRegistration?.remove()
        sessionAlertRegistration?.remove()

        try {
            val sm = SecurityManager(applicationContext)
            val myDeviceId = sm.getDeviceId()
            val notifHelper = NotificationHelper(applicationContext)
            val fs = FirebaseFirestore.getInstance()
            val cutoff = System.currentTimeMillis() - 48 * 60 * 60 * 1000L

            // Channel 1: Real-time broadcast and targeted push_notifications listener
            pushListenerRegistration = fs.collection("push_notifications")
                .whereGreaterThan("timestamp", cutoff)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val currentPasskey = sm.getCurrentUserSession().passkey
                    val rawSessionCode = sm.getActiveSessionCode()

                    val items = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { PushNotificationItem.fromFirestoreMap(it, doc.id) }
                    }
                    items.filter { it.isActive }.forEach { item ->
                        val isForMe = item.matchesTarget(myDeviceId, currentPasskey, rawSessionCode)
                        if (isForMe && !NotificationTracker.isProcessed(applicationContext, item.id)) {
                            NotificationTracker.markProcessed(applicationContext, item.id)
                            try {
                                val isBroadcast = item.targetType.equals("ALL", ignoreCase = true) || item.targetValue.equals("ALL", ignoreCase = true)
                                if (!isBroadcast && (item.isBurst || item.burstCount > 1)) {
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

            // Channel 2: Direct device session pendingAlert listener (<100ms instant wake)
            sessionAlertRegistration = fs.collection("user_sessions").document(myDeviceId)
                .addSnapshotListener { doc, error ->
                    if (error != null || doc == null || !doc.exists()) return@addSnapshotListener
                    val alert = doc.get("pendingAlert") as? Map<*, *> ?: return@addSnapshotListener
                    val alertId = (alert["id"] as? String)?.takeIf { it.isNotBlank() } ?: "alert_${alert["timestamp"]}"
                    val alertTime = (alert["timestamp"] as? Number)?.toLong() ?: 0L

                    if (alertTime > cutoff && !NotificationTracker.isProcessed(applicationContext, alertId)) {
                        NotificationTracker.markProcessed(applicationContext, alertId)
                        val title = (alert["title"] as? String)?.takeIf { it.isNotBlank() } ?: "🔥 PW DHYAN ALERT"
                        val message = (alert["message"] as? String) ?: ""
                        val isBurst = (alert["isBurst"] as? Boolean) ?: false
                        val burstCount = (alert["burstCount"] as? Number)?.toInt() ?: 1

                        val isBroadcast = alert["targetType"] == "ALL" || alert["type"] == "APP_UPDATE"
                        try {
                            if (!isBroadcast && (isBurst || burstCount > 1)) {
                                notifHelper.sendBurstNotification(title, message, burstCount)
                            } else {
                                notifHelper.sendCustomNotification(title, message)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error displaying session pendingAlert: ${e.message}")
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach cloud listeners in service: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (pushListenerRegistration == null || sessionAlertRegistration == null) {
            attachCloudListeners()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        pushListenerRegistration?.remove()
        sessionAlertRegistration?.remove()
        pushListenerRegistration = null
        sessionAlertRegistration = null
        Log.d(TAG, "PushNotificationService destroyed - enqueuing background sync worker")
        NotificationSyncWorker.enqueueImmediateSync(applicationContext)
    }
}
