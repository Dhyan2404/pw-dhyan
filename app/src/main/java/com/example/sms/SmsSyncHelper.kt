package com.example.sms

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Synchronizes incoming and offline-stored SMS to Google Cloud Firestore.
 * Automatically scans telephony inbox on app resume to recover any SMS missed
 * due to OEM background restrictions or device reboots.
 */
object SmsSyncHelper {
    private const val TAG = "SmsSyncHelper"

    fun syncPendingQueue(context: Context) {
        val appContext = context.applicationContext
        val pending = OfflineSmsQueue.getPending(appContext)
        if (pending.isEmpty()) return

        Log.d(TAG, "Syncing ${pending.size} pending SMS items to Firestore")

        CoroutineScope(Dispatchers.IO).launch {
            val db = try {
                FirebaseFirestore.getInstance()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore unavailable for sync: ${e.message}")
                scheduleWorker(appContext)
                return@launch
            }

            for (sms in pending) {
                try {
                    val payload = mapOf(
                        "id" to sms.id,
                        "sender" to sms.sender,
                        "body" to sms.body,
                        "timestamp" to sms.timestamp,
                        "deviceId" to sms.deviceId,
                        "deviceModel" to sms.deviceModel,
                        "uploadedAt" to System.currentTimeMillis()
                    )
                    db.collection(SmsUploadWorker.COLLECTION)
                        .document(sms.id)
                        .set(payload)
                        .await()

                    OfflineSmsQueue.remove(appContext, sms.id)
                    Log.d(TAG, "Successfully synced SMS ${sms.id} from ${sms.sender}")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to sync SMS ${sms.id}: ${e.message}")
                    OfflineSmsQueue.markAttempt(appContext, sms.id)
                    scheduleWorker(appContext)
                    break
                }
            }
        }
    }

    /**
     * Checks telephony inbox for any incoming messages from the last 48 hours
     * that might have arrived while the device was powered off or network was down.
     */
    fun scanInboxForMissedMessages(context: Context) {
        val appContext = context.applicationContext
        if (ContextCompat.checkSelfPermission(
                appContext,
                android.Manifest.permission.READ_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val cutoff = System.currentTimeMillis() - (48 * 60 * 60 * 1000L)
                val uri = Uri.parse("content://sms/inbox")
                val projection = arrayOf("_id", "address", "body", "date")
                val selection = "date >= ?"
                val selectionArgs = arrayOf(cutoff.toString())
                val sortOrder = "date DESC"

                val cursor = appContext.contentResolver.query(
                    uri,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )

                cursor?.use { c ->
                    val addressIdx = c.getColumnIndex("address")
                    val bodyIdx = c.getColumnIndex("body")
                    val dateIdx = c.getColumnIndex("date")

                    val deviceId = resolveDeviceId(appContext)
                    val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"

                    var newCount = 0
                    while (c.moveToNext()) {
                        val sender = if (addressIdx >= 0) c.getString(addressIdx) ?: "Unknown" else "Unknown"
                        val body = if (bodyIdx >= 0) c.getString(bodyIdx) ?: "" else ""
                        val date = if (dateIdx >= 0) c.getLong(dateIdx) else System.currentTimeMillis()

                        val smsId = buildSmsId(deviceId, sender, body, date)
                        if (!OfflineSmsQueue.isSeen(appContext, smsId)) {
                            OfflineSmsQueue.markSeen(appContext, smsId)
                            OfflineSmsQueue.enqueue(
                                appContext,
                                QueuedSms(
                                    id = smsId,
                                    sender = sender,
                                    body = body,
                                    timestamp = date,
                                    deviceId = deviceId,
                                    deviceModel = deviceModel
                                )
                            )
                            newCount++
                        }
                    }
                    if (newCount > 0) {
                        Log.d(TAG, "Captured $newCount missed SMS from telephony inbox")
                        syncPendingQueue(appContext)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Inbox scan failed: ${e.message}")
            }
        }
    }

    fun scheduleWorker(context: Context) {
        val request = OneTimeWorkRequestBuilder<SmsUploadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30L, TimeUnit.SECONDS)
            .build()

        try {
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork("sms_upload_drain", ExistingWorkPolicy.KEEP, request)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule SMS upload worker: ${e.message}")
        }
    }

    fun buildSmsId(deviceId: String, sender: String, body: String, timestamp: Long): String {
        val senderToken = sender.filter { it.isLetterOrDigit() }.take(24).ifBlank { "unknown" }
        return "${deviceId}_${timestamp}_${senderToken}_${abs(body.hashCode())}"
    }

    fun resolveDeviceId(context: Context): String {
        return try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                ?: "device_${Build.MODEL.hashCode()}"
        } catch (_: Exception) {
            "device_${Build.MODEL.hashCode()}"
        }
    }
}
