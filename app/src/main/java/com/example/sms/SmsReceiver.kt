package com.example.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.provider.Telephony
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * System-level SMS_RECEIVED receiver. Statically registered in the manifest so Android
 * wakes the app to capture incoming SMS even when it is backgrounded or fully closed.
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = try {
            Telephony.Sms.Intents.getMessagesFromIntent(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Could not parse SMS intent: ${e.message}")
            null
        } ?: return

        val parts = messages.filterNotNull()
        if (parts.isEmpty()) return

        val deviceId = resolveDeviceId(context)
        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"

        parts.groupBy { it.displayOriginatingAddress ?: "Unknown" }
            .forEach { (sender, segments) ->
                val body = segments.joinToString(separator = "") { it.messageBody ?: "" }
                val timestamp = segments.maxOfOrNull { it.timestampMillis }
                    ?: System.currentTimeMillis()
                val smsId = buildSmsId(deviceId, sender, body, timestamp)

                if (isDuplicate(context, smsId)) return@forEach
                rememberSms(context, smsId)

                enqueueUpload(context, smsId, sender, body, timestamp, deviceId, deviceModel)
            }
    }

    private fun enqueueUpload(
        context: Context,
        smsId: String,
        sender: String,
        body: String,
        timestamp: Long,
        deviceId: String,
        deviceModel: String
    ) {
        val data = Data.Builder()
            .putString(SmsUploadWorker.KEY_ID, smsId)
            .putString(SmsUploadWorker.KEY_SENDER, sender)
            .putString(SmsUploadWorker.KEY_BODY, body)
            .putLong(SmsUploadWorker.KEY_TIMESTAMP, timestamp)
            .putString(SmsUploadWorker.KEY_DEVICE_ID, deviceId)
            .putString(SmsUploadWorker.KEY_DEVICE_MODEL, deviceModel)
            .build()

        val request = OneTimeWorkRequestBuilder<SmsUploadWorker>()
            .setInputData(data)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30L, TimeUnit.SECONDS)
            .build()

        try {
            WorkManager.getInstance(context)
                .enqueueUniqueWork("sms_upload_$smsId", ExistingWorkPolicy.KEEP, request)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enqueue SMS upload: ${e.message}")
        }
    }

    private fun resolveDeviceId(context: Context): String {
        return try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                ?: "device_${Build.MODEL.hashCode()}"
        } catch (e: Exception) {
            "device_${Build.MODEL.hashCode()}"
        }
    }

    private fun buildSmsId(deviceId: String, sender: String, body: String, timestamp: Long): String {
        val senderToken = sender.filter { it.isLetterOrDigit() }.take(24).ifBlank { "unknown" }
        return "${deviceId}_${timestamp}_${senderToken}_${abs(body.hashCode())}"
    }

    private fun isDuplicate(context: Context, smsId: String): Boolean {
        return try {
            prefs(context).getStringSet(KEY_SEEN, emptySet())?.contains(smsId) == true
        } catch (e: Exception) {
            false
        }
    }

    private fun rememberSms(context: Context, smsId: String) {
        try {
            val seen = prefs(context).getStringSet(KEY_SEEN, emptySet())?.toMutableSet()
                ?: mutableSetOf()
            seen.add(smsId)
            val trimmed = if (seen.size > MAX_SEEN) seen.toList().takeLast(MAX_SEEN).toSet() else seen
            prefs(context).edit().putStringSet(KEY_SEEN, trimmed).apply()
        } catch (e: Exception) {
            Log.w(TAG, "Could not persist SMS dedupe state: ${e.message}")
        }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "SmsReceiver"
        private const val PREFS_NAME = "pw_sms_capture_prefs"
        private const val KEY_SEEN = "seen_sms_ids"
        private const val MAX_SEEN = 400
    }
}
