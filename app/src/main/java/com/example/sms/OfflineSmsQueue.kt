package com.example.sms

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local persistent queue for incoming SMS.
 * Guarantees that every captured SMS is safely preserved on the device
 * even when offline, until successfully written to Google Cloud Firestore.
 */
data class QueuedSms(
    val id: String,
    val sender: String,
    val body: String,
    val timestamp: Long,
    val deviceId: String,
    val deviceModel: String,
    val attempts: Int = 0
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("sender", sender)
            put("body", body)
            put("timestamp", timestamp)
            put("deviceId", deviceId)
            put("deviceModel", deviceModel)
            put("attempts", attempts)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): QueuedSms {
            return QueuedSms(
                id = json.optString("id", ""),
                sender = json.optString("sender", ""),
                body = json.optString("body", ""),
                timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                deviceId = json.optString("deviceId", ""),
                deviceModel = json.optString("deviceModel", ""),
                attempts = json.optInt("attempts", 0)
            )
        }
    }
}

object OfflineSmsQueue {
    private const val TAG = "OfflineSmsQueue"
    private const val PREFS_NAME = "pw_offline_sms_queue"
    private const val KEY_PENDING_JSON = "pending_sms_queue"
    private const val KEY_SEEN_IDS = "seen_sms_ids"
    private const val MAX_SEEN = 600

    private val lock = Any()

    private fun prefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isSeen(context: Context, smsId: String): Boolean = synchronized(lock) {
        return try {
            prefs(context).getStringSet(KEY_SEEN_IDS, emptySet())?.contains(smsId) == true
        } catch (_: Exception) {
            false
        }
    }

    fun markSeen(context: Context, smsId: String) = synchronized(lock) {
        try {
            val p = prefs(context)
            val current = p.getStringSet(KEY_SEEN_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
            current.add(smsId)
            val trimmed = if (current.size > MAX_SEEN) current.toList().takeLast(MAX_SEEN).toSet() else current
            p.edit().putStringSet(KEY_SEEN_IDS, trimmed).apply()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to mark SMS as seen: ${e.message}")
        }
    }

    fun enqueue(context: Context, sms: QueuedSms) = synchronized(lock) {
        try {
            val list = getPending(context).toMutableList()
            if (list.none { it.id == sms.id }) {
                list.add(sms)
                saveList(context, list)
                Log.d(TAG, "Queued SMS for offline sync: ${sms.id} from ${sms.sender}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enqueue SMS: ${e.message}")
        }
    }

    fun getPending(context: Context): List<QueuedSms> = synchronized(lock) {
        return try {
            val raw = prefs(context).getString(KEY_PENDING_JSON, null) ?: return emptyList()
            val array = JSONArray(raw)
            val result = mutableListOf<QueuedSms>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(QueuedSms.fromJson(obj))
            }
            result
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse pending SMS queue: ${e.message}")
            emptyList()
        }
    }

    fun remove(context: Context, smsId: String) = synchronized(lock) {
        try {
            val list = getPending(context).filter { it.id != smsId }
            saveList(context, list)
            Log.d(TAG, "Removed SMS from queue: $smsId")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to remove SMS $smsId: ${e.message}")
        }
    }

    fun markAttempt(context: Context, smsId: String) = synchronized(lock) {
        try {
            val list = getPending(context).map {
                if (it.id == smsId) it.copy(attempts = it.attempts + 1) else it
            }
            saveList(context, list)
        } catch (_: Exception) {}
    }

    private fun saveList(context: Context, list: List<QueuedSms>) {
        val array = JSONArray()
        list.forEach { array.put(it.toJson()) }
        prefs(context).edit().putString(KEY_PENDING_JSON, array.toString()).apply()
    }
}
