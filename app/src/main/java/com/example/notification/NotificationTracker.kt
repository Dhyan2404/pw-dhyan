package com.example.notification

import android.content.Context
import android.util.Log

/**
 * Thread-safe global notification tracking cache.
 * Ensures notifications are processed and displayed exactly once across
 * SecurityManager, PushNotificationService, MainActivity foreground listeners,
 * and NotificationSyncWorker.
 */
object NotificationTracker {
    private const val TAG = "NotificationTracker"
    private const val PREFS_NOTIF = "pw_push_service_prefs"
    private const val KEY_PROCESSED_IDS = "processed_notification_ids"
    private const val MAX_SAVED_IDS = 300

    private val memoryCache = mutableSetOf<String>()
    @Volatile
    private var isInitialized = false

    @Synchronized
    fun init(context: Context) {
        if (isInitialized) return
        try {
            val sp = context.applicationContext.getSharedPreferences(PREFS_NOTIF, Context.MODE_PRIVATE)
            val saved = sp.getStringSet(KEY_PROCESSED_IDS, emptySet()) ?: emptySet()
            memoryCache.addAll(saved)
            isInitialized = true
        } catch (e: Exception) {
            Log.w(TAG, "Failed initializing NotificationTracker: ${e.message}")
        }
    }

    @Synchronized
    fun isProcessed(context: Context, id: String): Boolean {
        if (id.isBlank()) return false
        init(context)
        return memoryCache.contains(id)
    }

    @Synchronized
    fun markProcessed(context: Context, id: String) {
        if (id.isBlank()) return
        init(context)
        memoryCache.add(id)
        if (memoryCache.size > MAX_SAVED_IDS) {
            val toRemove = memoryCache.take(50).toSet()
            memoryCache.removeAll(toRemove)
        }
        try {
            val sp = context.applicationContext.getSharedPreferences(PREFS_NOTIF, Context.MODE_PRIVATE)
            sp.edit().putStringSet(KEY_PROCESSED_IDS, memoryCache.toSet()).apply()
        } catch (e: Exception) {
            Log.w(TAG, "Failed persisting processed notification ID: ${e.message}")
        }
    }
}
