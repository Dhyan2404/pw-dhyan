package com.example.notification

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.security.PushNotificationItem
import com.example.security.SecurityManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * AndroidX WorkManager Worker that reliably synchronizes push notifications
 * for users who were offline when notifications were dispatched.
 *
 * Runs automatically under OS constraint [NetworkType.CONNECTED],
 * ensuring notifications pop up the exact moment connectivity returns,
 * even when the app is backgrounded or closed.
 */
class NotificationSyncWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "NotificationSyncWorker started checking for missed notifications...")
            val sm = SecurityManager(appContext)
            val myDeviceId = sm.getDeviceId()
            val myPasskey = sm.getCurrentUserSession().passkey
            val notifHelper = NotificationHelper(appContext)

            val sp = appContext.getSharedPreferences(PREFS_NOTIF, Context.MODE_PRIVATE)
            val saved = sp.getStringSet(KEY_PROCESSED_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()

            val fs = FirebaseFirestore.getInstance()
            // Check notifications dispatched in the last 48 hours
            val cutoff = System.currentTimeMillis() - 48 * 60 * 60 * 1000L

            var hasNew = false

            // 1. Check global/targeted push_notifications collection
            try {
                val snapshot = fs.collection("push_notifications")
                    .whereGreaterThan("timestamp", cutoff)
                    .get()
                    .await()

                val items = snapshot.documents.mapNotNull { doc ->
                    doc.data?.let { data ->
                        // Pass doc.id as canonical fallback so map without "id" never generates random UUIDs
                        PushNotificationItem.fromFirestoreMap(data, doc.id)
                    }
                }

                items.filter { it.isActive }.forEach { item ->
                    val isForMe = when (item.targetType) {
                        "DEVICE" -> item.targetValue.equals(myDeviceId, ignoreCase = true)
                        "PASSKEY" -> item.targetValue.equals(myPasskey, ignoreCase = true)
                        else -> true // "ALL" broadcast
                    }

                    if (isForMe && !saved.contains(item.id)) {
                        saved.add(item.id)
                        hasNew = true
                        withContext(Dispatchers.Main) {
                            try {
                                if (item.isBurst || item.burstCount > 1) {
                                    notifHelper.sendBurstNotification(item.title, item.message, item.burstCount)
                                } else {
                                    notifHelper.sendCustomNotification(item.title, item.message)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to display recovered notification: ${e.message}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed querying push_notifications collection: ${e.message}")
            }

            // 2. Check direct device pendingAlert in user_sessions/$myDeviceId
            try {
                val sessionDoc = fs.collection("user_sessions").document(myDeviceId).get().await()
                if (sessionDoc.exists()) {
                    val alert = sessionDoc.get("pendingAlert") as? Map<*, *>
                    if (alert != null) {
                        val alertId = (alert["id"] as? String)?.takeIf { it.isNotBlank() } ?: "alert_${alert["timestamp"]}"
                        val alertTime = (alert["timestamp"] as? Number)?.toLong() ?: 0L
                        if (alertTime > cutoff && !saved.contains(alertId)) {
                            saved.add(alertId)
                            hasNew = true
                            val title = (alert["title"] as? String) ?: "🔥 PW DHYAN ALERT"
                            val msg = (alert["message"] as? String) ?: ""
                            val isBurst = (alert["isBurst"] as? Boolean) ?: false
                            val burstCount = (alert["burstCount"] as? Number)?.toInt() ?: 1

                            withContext(Dispatchers.Main) {
                                if (isBurst || burstCount > 1) {
                                    notifHelper.sendBurstNotification(title, msg, burstCount)
                                } else {
                                    notifHelper.sendCustomNotification(title, msg)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed checking user_sessions pendingAlert: ${e.message}")
            }

            // 3. Persist processed IDs cache (limited to latest 200 items to avoid unbounded growth)
            if (hasNew) {
                val toSave = if (saved.size > 200) saved.toList().takeLast(200).toSet() else saved.toSet()
                sp.edit().putStringSet(KEY_PROCESSED_IDS, toSave).apply()
                Log.d(TAG, "Successfully processed and delivered offline pending notifications.")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "NotificationSyncWorker error: ${e.message}", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val TAG = "NotifSyncWorker"
        const val PREFS_NOTIF = "pw_push_service_prefs"
        const val KEY_PROCESSED_IDS = "processed_notification_ids"
        private const val UNIQUE_PERIODIC_WORK_NAME = "pw_dhyan_periodic_notif_sync"
        private const val UNIQUE_ONETIME_WORK_NAME = "pw_dhyan_onetime_notif_sync"

        /**
         * Enqueues a periodic background sync worker that runs every 15 minutes
         * whenever the device has network connectivity.
         */
        fun schedulePeriodicSync(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val request = PeriodicWorkRequestBuilder<NotificationSyncWorker>(
                    15, TimeUnit.MINUTES,
                    5, TimeUnit.MINUTES // Flex window
                )
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                    UNIQUE_PERIODIC_WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
                Log.d(TAG, "Scheduled periodic notification sync worker (every 15 min on network)")
            } catch (e: Exception) {
                Log.w(TAG, "Failed scheduling periodic sync: ${e.message}")
            }
        }

        /**
         * Enqueues an immediate one-time sync worker with network constraint.
         * Executes immediately when network is available.
         */
        fun enqueueImmediateSync(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val request = OneTimeWorkRequestBuilder<NotificationSyncWorker>()
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                    UNIQUE_ONETIME_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    request
                )
                Log.d(TAG, "Enqueued immediate notification sync worker on network available")
            } catch (e: Exception) {
                Log.w(TAG, "Failed enqueuing immediate sync: ${e.message}")
            }
        }
    }
}
