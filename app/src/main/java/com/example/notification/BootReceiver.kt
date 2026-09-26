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
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            PushNotificationService.start(context)
            NotificationSyncWorker.schedulePeriodicSync(context)
            NotificationSyncWorker.enqueueImmediateSync(context)
        }
    }
}
