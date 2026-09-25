package com.example.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Single source of truth for the mandatory SMS cloud-forwarding permission gate.
 * Checks both RECEIVE_SMS (for real-time capture) and READ_SMS (for offline missed SMS recovery).
 */
object SmsAccess {
    val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS
    )

    fun hasPermission(context: Context): Boolean {
        val hasReceive = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED

        return hasReceive
    }

    fun hasFullPermission(context: Context): Boolean {
        val hasReceive = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED

        val hasRead = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        return hasReceive && hasRead
    }

    fun hasReceivePermissionOnly(context: Context): Boolean {
        return hasPermission(context)
    }
}
