package com.example.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Ensures PushNotificationService resumes silently after device reboot or app update,
 * and clears any legacy persistent foreground notifications.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Immediately dismiss and delete any legacy ongoing notification
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancel(PushNotificationService.FOREGROUND_NOTIF_ID)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm?.deleteNotificationChannel(PushNotificationService.FOREGROUND_CHANNEL_ID)
            }
        } catch (_: Exception) {}

        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            PushNotificationService.start(context)
        }
    }
}
