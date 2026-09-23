package com.example.security

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Represents an access key created by Admin.
 * Stored SOLELY in Google Cloud Firestore.
 */
data class AccessKey(
    val id: String = UUID.randomUUID().toString(),
    val code: String,
    val label: String = "Passkey",
    val createdAt: Long = System.currentTimeMillis(),
    val durationMillis: Long = 24 * 60 * 60 * 1000L,
    val isInfinite: Boolean = false,
    val isUsed: Boolean = false,
    val deviceId: String? = null,
    val deviceModel: String? = null,
    val usedAt: Long? = null
) {
    val expiresAt: Long get() = if (isInfinite) Long.MAX_VALUE else createdAt + durationMillis
    val isExpired: Boolean get() = if (isInfinite) false else System.currentTimeMillis() >= expiresAt
    val remainingMillis: Long get() = if (isInfinite) Long.MAX_VALUE else (expiresAt - System.currentTimeMillis()).coerceAtLeast(0L)

    fun getRemainingTimeFormatted(): String {
        if (isInfinite) return "Infinite / Admin"
        if (isExpired) return "Expired"
        val totalSecs = remainingMillis / 1000
        val hours = totalSecs / 3600
        val mins = (totalSecs % 3600) / 60
        return if (hours > 0) "${hours}h ${mins}m left" else "${mins}m left"
    }

    fun getCreatedTimeFormatted(): String {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        return sdf.format(Date(createdAt))
    }

    fun toFirestoreMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "id" to id,
            "code" to code,
            "label" to label,
            "createdAt" to createdAt,
            "durationMillis" to durationMillis,
            "isInfinite" to isInfinite,
            "isUsed" to isUsed
        )
        deviceId?.let { map["deviceId"] = it }
        deviceModel?.let { map["deviceModel"] = it }
        usedAt?.let { map["usedAt"] = it }
        return map
    }

    companion object {
        fun fromFirestoreMap(map: Map<String, Any?>): AccessKey {
            return AccessKey(
                id = (map["id"] as? String) ?: UUID.randomUUID().toString(),
                code = (map["code"] as? String) ?: "",
                label = (map["label"] as? String) ?: "Passkey",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                durationMillis = (map["durationMillis"] as? Number)?.toLong() ?: (24 * 60 * 60 * 1000L),
                isInfinite = (map["isInfinite"] as? Boolean) ?: false,
                isUsed = (map["isUsed"] as? Boolean) ?: false,
                deviceId = map["deviceId"] as? String,
                deviceModel = map["deviceModel"] as? String,
                usedAt = (map["usedAt"] as? Number)?.toLong()
            )
        }
    }
}

/**
 * Tracks a logged-in user device in Google Firestore.
 */
data class UserSession(
    val deviceId: String,
    val deviceModel: String,
    val androidVersion: String,
    val passkey: String,
    val label: String,
    val loginTime: Long = System.currentTimeMillis(),
    val lastActiveTime: Long = System.currentTimeMillis(),
    val suspendedUntil: Long = 0L,
    val isRevoked: Boolean = false
) {
    fun getFormattedLoginTime(): String {
        return SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(loginTime))
    }

    fun getActiveDurationFormatted(): String {
        val totalSecs = ((System.currentTimeMillis() - loginTime) / 1000).coerceAtLeast(0)
        val hours = totalSecs / 3600
        val mins = (totalSecs % 3600) / 60
        val secs = totalSecs % 60
        return when {
            hours > 0 -> "${hours}h ${mins}m online"
            mins > 0 -> "${mins}m ${secs}s online"
            else -> "${secs}s online"
        }
    }

    fun isSuspended(): Boolean = System.currentTimeMillis() < suspendedUntil

    fun getRemainingSuspensionFormatted(): String {
        val remSecs = ((suspendedUntil - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
        val mins = remSecs / 60
        val secs = remSecs % 60
        return if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
    }

    fun toFirestoreMap(): Map<String, Any> {
        return mapOf(
            "deviceId" to deviceId,
            "deviceModel" to deviceModel,
            "androidVersion" to androidVersion,
            "passkey" to passkey,
            "label" to label,
            "loginTime" to loginTime,
            "lastActiveTime" to System.currentTimeMillis(),
            "suspendedUntil" to suspendedUntil,
            "isRevoked" to isRevoked
        )
    }

    companion object {
        fun fromFirestoreMap(map: Map<String, Any?>): UserSession {
            return UserSession(
                deviceId = (map["deviceId"] as? String) ?: "unknown_device",
                deviceModel = (map["deviceModel"] as? String) ?: "Android Device",
                androidVersion = (map["androidVersion"] as? String) ?: "Android",
                passkey = (map["passkey"] as? String) ?: "",
                label = (map["label"] as? String) ?: "Student",
                loginTime = (map["loginTime"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                lastActiveTime = (map["lastActiveTime"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                suspendedUntil = (map["suspendedUntil"] as? Number)?.toLong() ?: 0L,
                isRevoked = (map["isRevoked"] as? Boolean) ?: false
            )
        }
    }
}

/**
 * Global broadcast announcement sent by Admin to all active student devices.
 */
data class BroadcastAnnouncement(
    val id: String = UUID.randomUUID().toString(),
    val message: String,
    val author: String = "Admin Dhyan",
    val timestamp: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
) {
    fun getFormattedTime(): String {
        return SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(timestamp))
    }

    fun toFirestoreMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "message" to message,
            "author" to author,
            "timestamp" to timestamp,
            "isActive" to isActive
        )
    }

    companion object {
        fun fromFirestoreMap(map: Map<String, Any?>): BroadcastAnnouncement {
            return BroadcastAnnouncement(
                id = (map["id"] as? String) ?: UUID.randomUUID().toString(),
                message = (map["message"] as? String) ?: "",
                author = (map["author"] as? String) ?: "Admin",
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                isActive = (map["isActive"] as? Boolean) ?: true
            )
        }
    }
}

/**
 * Maintenance & Emergency Lockdown state synced in Firestore.
 */
data class AppMaintenanceInfo(
    val isActive: Boolean = false,
    val message: String = "Server maintenance is underway. Please check back shortly!"
)

sealed class UnlockResult {
    data object PermanentUnlocked : UnlockResult()
    data class KeyUnlocked(val key: AccessKey) : UnlockResult()
    data object SessionUnlocked : UnlockResult()
    data class Invalid(val reason: String) : UnlockResult()
}

class SecurityManager(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("study_browser_security", Context.MODE_PRIVATE)

    private var firestore: FirebaseFirestore? = null
    private var firestoreRegistration: ListenerRegistration? = null
    private var isFirestoreConnected = false

    // CLOUD-ONLY IN-MEMORY CACHE (Never stored locally)
    private var cloudKeysList = mutableListOf<AccessKey>()
    private val updateListeners = mutableListOf<(List<AccessKey>) -> Unit>()
    private var currentActiveKey: AccessKey? = null
    private var lastBroadcastAnnouncement: BroadcastAnnouncement? = null
    private val broadcastListeners = mutableListOf<(BroadcastAnnouncement?) -> Unit>()

    companion object {
        const val MASTER_PERMANENT_CODE = "240411"
        private const val KEY_PERMANENT_UNLOCKED = "is_permanent_unlocked"
        private const val KEY_REVOKED_DEVICES = "revoked_devices_set"
        private const val KEY_SESSION_ACTIVE = "session_active"
        private const val KEY_SESSION_EXPIRY = "session_expiry"
        private const val KEY_SESSION_CODE = "session_code"
        private const val KEY_SESSION_LABEL = "session_label"
        private const val KEY_SESSION_IS_INFINITE = "session_is_infinite"
        const val HOME_URL = "https://pw.studyparcham.in/#home-view"
        const val ALLOWED_DOMAIN = "pw.studyparcham.in"
        const val DIRECT_APK_DOWNLOAD_URL = "https://github.com/Dhyan2404/pw-dhyan/releases/latest/download/PW-DHYAN.apk"
        private const val FIRESTORE_COLLECTION_KEYS = "access_keys"
        private const val FIRESTORE_COLLECTION_SESSIONS = "user_sessions"
        private const val FIRESTORE_COLLECTION_ANNOUNCEMENTS = "announcements"
        private const val FIRESTORE_COLLECTION_SYSTEM = "system_config"
        private const val ANNOUNCEMENT_DOC_ID = "latest_announcement"
        private const val MAINTENANCE_DOC_ID = "maintenance_mode"
        private const val TAG = "FirestoreSecurity"
    }

    init {
        initFirestore()
        restoreSessionFromPrefs()
    }

    private fun restoreSessionFromPrefs() {
        if (isSessionActive()) {
            val code = prefs.getString(KEY_SESSION_CODE, "") ?: ""
            val label = prefs.getString(KEY_SESSION_LABEL, "Passkey") ?: "Passkey"
            val expiry = prefs.getLong(KEY_SESSION_EXPIRY, 0L)
            val isInf = prefs.getBoolean(KEY_SESSION_IS_INFINITE, false)
            val rem = if (isInf) Long.MAX_VALUE else (expiry - System.currentTimeMillis()).coerceAtLeast(0L)
            currentActiveKey = AccessKey(
                code = code,
                label = label,
                durationMillis = rem,
                isInfinite = isInf,
                isUsed = true
            )
            recordCurrentDeviceSession()
        }
    }

    fun isSessionActive(): Boolean {
        if (isPermanentUnlocked()) return true

        val deviceId = getDeviceId()
        val suspendedUntil = prefs.getLong("suspended_until_$deviceId", 0L)
        if (suspendedUntil > 0L) {
            if (suspendedUntil > System.currentTimeMillis()) {
                return false // On temporary timeout!
            } else {
                prefs.edit().remove("suspended_until_$deviceId").apply()
            }
        }

        val isSessionStored = prefs.getBoolean(KEY_SESSION_ACTIVE, false)
        val expiry = prefs.getLong(KEY_SESSION_EXPIRY, 0L)
        val isInf = prefs.getBoolean(KEY_SESSION_IS_INFINITE, false)

        if (isSessionStored && (isInf || System.currentTimeMillis() < expiry)) {
            val revokedDevices = prefs.getStringSet(KEY_REVOKED_DEVICES, emptySet()) ?: emptySet()
            if (revokedDevices.contains(deviceId)) {
                clearSession()
                return false
            }
            return true
        }

        if (isSessionStored && !isInf && System.currentTimeMillis() >= expiry) {
            clearSession()
        }
        return false
    }

    fun saveSession(code: String, label: String, durationMillis: Long, isInfinite: Boolean) {
        val expiry = if (isInfinite) Long.MAX_VALUE else System.currentTimeMillis() + durationMillis
        prefs.edit()
            .putBoolean(KEY_SESSION_ACTIVE, true)
            .putLong(KEY_SESSION_EXPIRY, expiry)
            .putString(KEY_SESSION_CODE, code)
            .putString(KEY_SESSION_LABEL, label)
            .putBoolean(KEY_SESSION_IS_INFINITE, isInfinite)
            .apply()

        currentActiveKey = AccessKey(
            code = code,
            label = label,
            durationMillis = durationMillis,
            isInfinite = isInfinite,
            isUsed = true,
            deviceId = getDeviceId(),
            deviceModel = getDeviceModelName(),
            usedAt = System.currentTimeMillis()
        )
    }

    fun clearSession() {
        prefs.edit()
            .putBoolean(KEY_SESSION_ACTIVE, false)
            .remove(KEY_SESSION_EXPIRY)
            .remove(KEY_SESSION_CODE)
            .remove(KEY_SESSION_LABEL)
            .remove(KEY_SESSION_IS_INFINITE)
            .apply()
        currentActiveKey = null
    }

    private fun initFirestore() {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            firestore = FirebaseFirestore.getInstance()
            listenToFirestore()
            Log.d(TAG, "Google Cloud Firestore initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase Firestore", e)
            firestore = null
            isFirestoreConnected = false
        }
    }

    private fun listenToFirestore() {
        val fs = firestore ?: return
        try {
            firestoreRegistration = fs.collection(FIRESTORE_COLLECTION_KEYS)
                .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Firestore listen error: ${error.message}", error)
                        isFirestoreConnected = false
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        isFirestoreConnected = true
                        val remoteKeys = snapshot.documents.mapNotNull { doc ->
                            doc.data?.let { AccessKey.fromFirestoreMap(it) }
                        }
                        cloudKeysList = remoteKeys.toMutableList()
                        notifyListeners(cloudKeysList.sortedByDescending { it.createdAt })
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error attaching Firestore listener", e)
            isFirestoreConnected = false
        }
    }

    fun isCloudSyncActive(): Boolean = isFirestoreConnected

    fun addKeysUpdateListener(listener: (List<AccessKey>) -> Unit) {
        updateListeners.add(listener)
        listener(getAllKeys())
    }

    fun removeKeysUpdateListener(listener: (List<AccessKey>) -> Unit) {
        updateListeners.remove(listener)
    }

    private fun notifyListeners(keys: List<AccessKey>) {
        updateListeners.forEach { it(keys) }
    }

    fun isPermanentUnlocked(): Boolean {
        return prefs.getBoolean(KEY_PERMANENT_UNLOCKED, false)
    }

    fun setPermanentUnlocked(unlocked: Boolean) {
        prefs.edit().putBoolean(KEY_PERMANENT_UNLOCKED, unlocked).apply()
    }

    fun revokeAdminAccess() {
        setPermanentUnlocked(false)
        clearSession()
    }

    fun verifyAdminPassword(password: String): Boolean {
        val clean = password.trim()
        return clean == MASTER_PERMANENT_CODE || clean == "2404"
    }

    fun getDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "device_${Build.MODEL.hashCode()}"
    }

    fun getDeviceModelName(): String {
        return "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
    }

    /**
     * Retrieves all saved access keys ONLY from live cloud cache.
     */
    fun getAllKeys(): List<AccessKey> {
        return cloudKeysList.sortedByDescending { it.createdAt }
    }

    /**
     * Admin creates a new key (24-Hour or Infinite).
     * Written EXCLUSIVELY to Google Firestore.
     */
    fun createKey(
        customCode: String? = null,
        label: String = "",
        isInfinite: Boolean = false,
        durationMillis: Long = 24 * 60 * 60 * 1000L
    ): AccessKey {
        val allExisting = getAllKeys()
        val existingCodes = allExisting.map { it.code }.toSet()

        val finalCode = if (!customCode.isNullOrBlank() && customCode.trim().length in 4..8) {
            customCode.trim()
        } else {
            var candidate: String
            do {
                candidate = (100000..999999).random().toString()
            } while (candidate == MASTER_PERMANENT_CODE || candidate == "2404" || existingCodes.contains(candidate))
            candidate
        }

        val defaultLabel = if (isInfinite) "Admin Infinite Key" else "Passkey #${allExisting.size + 1}"
        val finalLabel = if (label.isNotBlank()) label.trim() else defaultLabel

        val newKey = AccessKey(
            code = finalCode,
            label = finalLabel,
            createdAt = System.currentTimeMillis(),
            durationMillis = durationMillis,
            isInfinite = isInfinite,
            isUsed = false
        )

        // Write directly to Google Cloud Firestore
        val fs = firestore
        if (fs == null) {
            showToast("Cloud Connection Error: Cannot connect to Firestore. Check internet and Firebase Console.")
            return newKey
        }

        fs.collection(FIRESTORE_COLLECTION_KEYS).document(newKey.id)
            .set(newKey.toFirestoreMap())
            .addOnSuccessListener {
                Log.d(TAG, "Key ${newKey.code} successfully saved in Cloud Firestore!")
                showToast("Key ${newKey.code} saved to Google Cloud!")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to write key to Cloud Firestore", e)
                showToast("Cloud Error: ${e.localizedMessage}. Check Firestore Database rules.")
            }

        return newKey
    }

    /**
     * Rapidly generates a batch of passkeys and saves them simultaneously to Firestore.
     */
    fun createBatchKeys(
        count: Int,
        durationMillis: Long = 24 * 60 * 60 * 1000L,
        isInfinite: Boolean = false,
        labelPrefix: String = "Student Batch"
    ): List<AccessKey> {
        val allExisting = getAllKeys()
        val existingCodes = allExisting.map { it.code }.toMutableSet()
        val generatedList = mutableListOf<AccessKey>()

        val now = System.currentTimeMillis()
        for (i in 1..count) {
            var candidate: String
            do {
                candidate = (100000..999999).random().toString()
            } while (candidate == MASTER_PERMANENT_CODE || candidate == "2404" || existingCodes.contains(candidate))
            existingCodes.add(candidate)

            val key = AccessKey(
                code = candidate,
                label = "$labelPrefix #$i",
                createdAt = now,
                durationMillis = durationMillis,
                isInfinite = isInfinite,
                isUsed = false
            )
            generatedList.add(key)
        }

        val fs = firestore
        if (fs != null) {
            generatedList.chunked(400).forEach { chunk ->
                val batch = fs.batch()
                chunk.forEach { key ->
                    val docRef = fs.collection(FIRESTORE_COLLECTION_KEYS).document(key.id)
                    batch.set(docRef, key.toFirestoreMap())
                }
                batch.commit()
                    .addOnSuccessListener {
                        Log.d(TAG, "Batch of ${chunk.size} keys saved to Cloud Firestore")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Batch key creation error: ${e.message}")
                        showToast("Cloud Error: ${e.localizedMessage}")
                    }
            }
            showToast("Batch of ${generatedList.size} keys saved to Google Cloud!")
        }

        return generatedList
    }

    fun removeKey(keyId: String): Boolean {
        val fs = firestore ?: return false
        fs.collection(FIRESTORE_COLLECTION_KEYS).document(keyId).delete()
            .addOnSuccessListener {
                Log.d(TAG, "Key $keyId deleted from Firestore")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to delete key from Firestore", e)
            }
        return true
    }

    fun clearExpiredKeys(): Int {
        val fs = firestore ?: return 0
        val expired = cloudKeysList.filter { it.isExpired }
        if (expired.isEmpty()) return 0
        try {
            val batch = fs.batch()
            expired.take(450).forEach { key ->
                batch.delete(fs.collection(FIRESTORE_COLLECTION_KEYS).document(key.id))
            }
            batch.commit()
        } catch (e: Exception) {
            Log.e(TAG, "Batch delete failed: ${e.message}")
        }
        return expired.size
    }

    fun refreshFromCloud(onComplete: ((Boolean) -> Unit)? = null) {
        val fs = firestore
        if (fs == null) {
            initFirestore()
            onComplete?.invoke(false)
            return
        }

        fs.collection(FIRESTORE_COLLECTION_KEYS).get()
            .addOnSuccessListener { snapshot ->
                isFirestoreConnected = true
                val keys = snapshot.documents.mapNotNull { doc ->
                    doc.data?.let { AccessKey.fromFirestoreMap(it) }
                }
                cloudKeysList = keys.toMutableList()
                notifyListeners(cloudKeysList.sortedByDescending { it.createdAt })
                onComplete?.invoke(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Firestore refresh error", e)
                isFirestoreConnected = false
                onComplete?.invoke(false)
            }
    }

    /**
     * Verifies the entered code against Google Cloud Firestore.
     * IF APP IS NOT CONNECTED TO CLOUD, IT WILL NOT RUN!
     */
    fun verifyCode(enteredCode: String): UnlockResult {
        val cleaned = enteredCode.trim().replace(":", "").replace(" ", "")
        val thisDeviceId = getDeviceId()
        val thisDeviceModel = getDeviceModelName()

        // 1. Master Admin permanent code (240411, or 2404) - ALWAYS VERIFIED REGARDLESS OF NETWORK
        if (cleaned == MASTER_PERMANENT_CODE || cleaned == "2404") {
            setPermanentUnlocked(true)
            saveSession(MASTER_PERMANENT_CODE, "Administrator", Long.MAX_VALUE, true)
            recordUserSession(thisDeviceId, thisDeviceModel, "Admin Master Code (240411)", "Administrator")
            return UnlockResult.PermanentUnlocked
        }

        // ENFORCE CLOUD REQUIREMENT FOR STUDENT PASSKEYS:
        if (!isFirestoreConnected && firestore == null) {
            initFirestore()
            return UnlockResult.Invalid(
                "Cloud Connection Required: Connecting to Google Cloud... Please ensure you have internet access and that Firestore Database is created."
            )
        }

        // Check if device was revoked by Admin
        val revokedDevices = prefs.getStringSet(KEY_REVOKED_DEVICES, emptySet()) ?: emptySet()
        if (revokedDevices.contains(thisDeviceId)) {
            return UnlockResult.Invalid("Access Revoked: Your device has been restricted by Admin.")
        }

        // Check if device is in temporary timeout (e.g. 5-min cooldown)
        val suspendedUntil = prefs.getLong("suspended_until_$thisDeviceId", 0L)
        if (suspendedUntil > System.currentTimeMillis()) {
            val remMins = ((suspendedUntil - System.currentTimeMillis()) / 60000) + 1
            return UnlockResult.Invalid("Session Cooldown: Admin placed your device on a $remMins min timeout.")
        }

        // 2. Check 24-Hour and Infinite Keys from Cloud Firestore
        val allKeys = getAllKeys()
        val matchedKey = allKeys.firstOrNull { it.code == cleaned }
        if (matchedKey != null) {
            if (matchedKey.isExpired) {
                return UnlockResult.Invalid("Passkey Expired: This access key has concluded.")
            }

            // 1-Key 1-Time Device Binding:
            if (matchedKey.isUsed && matchedKey.deviceId != null && matchedKey.deviceId != thisDeviceId) {
                return UnlockResult.Invalid("Key Already Used: This passkey was claimed on another device (${matchedKey.deviceModel ?: "Other Device"}). 1 key is valid on 1 device only.")
            }

            // If not used yet, bind it to this device ID in Cloud Firestore
            val boundKey = if (!matchedKey.isUsed) {
                val updated = matchedKey.copy(
                    isUsed = true,
                    deviceId = thisDeviceId,
                    deviceModel = thisDeviceModel,
                    usedAt = System.currentTimeMillis()
                )

                // Sync directly to Firestore
                firestore?.collection(FIRESTORE_COLLECTION_KEYS)?.document(updated.id)
                    ?.set(updated.toFirestoreMap())

                updated
            } else {
                matchedKey
            }

            currentActiveKey = boundKey
            recordUserSession(thisDeviceId, thisDeviceModel, boundKey.code, boundKey.label)

            if (boundKey.isInfinite) {
                setPermanentUnlocked(true)
            }
            saveSession(boundKey.code, boundKey.label, boundKey.durationMillis, boundKey.isInfinite)
            return UnlockResult.KeyUnlocked(boundKey)
        }

        // SANITIZED ERROR: Only Admin passkeys and master code accepted
        return UnlockResult.Invalid(
            "Invalid passkey. Access requires an Admin-issued passkey or 240411."
        )
    }

    fun recordCurrentDeviceSession() {
        val thisDeviceId = getDeviceId()
        val thisDeviceModel = getDeviceModelName()
        val passkey = prefs.getString(KEY_SESSION_CODE, if (isPermanentUnlocked()) "Admin (240411)" else "Active Session") ?: "Active Session"
        val label = prefs.getString(KEY_SESSION_LABEL, if (isPermanentUnlocked()) "Administrator" else "Student") ?: "Student"
        recordUserSession(thisDeviceId, thisDeviceModel, passkey, label)
    }

    fun getCurrentUserSession(): UserSession {
        val thisDeviceId = getDeviceId()
        val thisDeviceModel = getDeviceModelName()
        val passkey = prefs.getString(KEY_SESSION_CODE, if (isPermanentUnlocked()) "Admin (240411)" else "Active Session") ?: "Active Session"
        val label = prefs.getString(KEY_SESSION_LABEL, if (isPermanentUnlocked()) "Administrator" else "Student") ?: "Student"
        return UserSession(
            deviceId = thisDeviceId,
            deviceModel = thisDeviceModel,
            androidVersion = "Android ${Build.VERSION.RELEASE}",
            passkey = passkey,
            label = label,
            loginTime = prefs.getLong("session_login_time", System.currentTimeMillis()),
            lastActiveTime = System.currentTimeMillis()
        )
    }

    private fun recordUserSession(deviceId: String, deviceModel: String, passkey: String, label: String) {
        val now = System.currentTimeMillis()
        if (!prefs.contains("session_login_time")) {
            prefs.edit().putLong("session_login_time", now).apply()
        }
        val loginTime = prefs.getLong("session_login_time", now)
        val session = UserSession(
            deviceId = deviceId,
            deviceModel = deviceModel,
            androidVersion = "Android ${Build.VERSION.RELEASE}",
            passkey = passkey,
            label = label,
            loginTime = loginTime,
            lastActiveTime = now
        )

        try {
            firestore?.collection(FIRESTORE_COLLECTION_SESSIONS)?.document(deviceId)
                ?.set(session.toFirestoreMap())
        } catch (e: Exception) {
            Log.w(TAG, "Failed to record session to Firestore: ${e.message}")
        }
    }

    fun listenToUserSessions(onSessionsUpdated: (List<UserSession>) -> Unit): ListenerRegistration? {
        val curSession = getCurrentUserSession()
        // Provide immediate local device state to prevent empty screen
        onSessionsUpdated(listOf(curSession))

        val fs = firestore ?: return null
        return try {
            fs.collection(FIRESTORE_COLLECTION_SESSIONS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Session listen error: ${error.message}")
                        onSessionsUpdated(listOf(curSession))
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val sessions = snapshot.documents.mapNotNull { doc ->
                            doc.data?.let { UserSession.fromFirestoreMap(it) }
                        }.toMutableList()

                        // Ensure current device is always in the list even if Firestore write had network delay
                        if (sessions.none { it.deviceId == curSession.deviceId }) {
                            sessions.add(curSession)
                        }
                        onSessionsUpdated(sessions.sortedByDescending { it.loginTime })
                    }
                }
        } catch (e: Exception) {
            onSessionsUpdated(listOf(curSession))
            null
        }
    }

    fun revokeDevice(deviceId: String, onComplete: ((Boolean) -> Unit)? = null) {
        val currentSet = prefs.getStringSet(KEY_REVOKED_DEVICES, emptySet())?.toMutableSet() ?: mutableSetOf()
        currentSet.add(deviceId)
        prefs.edit().putStringSet(KEY_REVOKED_DEVICES, currentSet).apply()

        firestore?.collection(FIRESTORE_COLLECTION_SESSIONS)?.document(deviceId)
            ?.update("isRevoked", true)
            ?.addOnCompleteListener { onComplete?.invoke(true) }
    }

    fun suspendDevice(deviceId: String, durationMinutes: Int = 5, onComplete: ((Boolean) -> Unit)? = null) {
        val timeoutUntil = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        prefs.edit().putLong("suspended_until_$deviceId", timeoutUntil).apply()
        firestore?.collection(FIRESTORE_COLLECTION_SESSIONS)?.document(deviceId)
            ?.update("suspendedUntil", timeoutUntil)
            ?.addOnCompleteListener { onComplete?.invoke(true) }
    }

    fun unsuspendDevice(deviceId: String, onComplete: ((Boolean) -> Unit)? = null) {
        prefs.edit().remove("suspended_until_$deviceId").apply()
        firestore?.collection(FIRESTORE_COLLECTION_SESSIONS)?.document(deviceId)
            ?.update("suspendedUntil", 0L)
            ?.addOnCompleteListener { onComplete?.invoke(true) }
    }

    fun publishAnnouncement(message: String, author: String = "Admin Dhyan", onComplete: ((Boolean) -> Unit)? = null) {
        val announcement = BroadcastAnnouncement(
            message = message.trim(),
            author = author,
            timestamp = System.currentTimeMillis(),
            isActive = true
        )
        // Store locally so it displays immediately on this device regardless of network latency
        lastBroadcastAnnouncement = announcement
        broadcastListeners.forEach { it.invoke(announcement) }

        val fs = firestore
        if (fs == null) {
            showToast("Broadcast active locally (Cloud offline)")
            onComplete?.invoke(true)
            return
        }
        fs.collection(FIRESTORE_COLLECTION_ANNOUNCEMENTS).document(ANNOUNCEMENT_DOC_ID)
            .set(announcement.toFirestoreMap())
            .addOnSuccessListener {
                showToast("Announcement Broadcast Live to all students!")
                onComplete?.invoke(true)
            }
            .addOnFailureListener { e ->
                showToast("Cloud sync failed, active locally: ${e.message}")
                onComplete?.invoke(true)
            }
    }

    fun clearAnnouncement(onComplete: ((Boolean) -> Unit)? = null) {
        lastBroadcastAnnouncement = null
        broadcastListeners.forEach { it.invoke(null) }
        val fs = firestore ?: run {
            onComplete?.invoke(true)
            return
        }
        fs.collection(FIRESTORE_COLLECTION_ANNOUNCEMENTS).document(ANNOUNCEMENT_DOC_ID)
            .update("isActive", false)
            .addOnCompleteListener { onComplete?.invoke(true) }
    }

    fun listenToAnnouncements(onUpdate: (BroadcastAnnouncement?) -> Unit): ListenerRegistration? {
        broadcastListeners.add(onUpdate)
        if (lastBroadcastAnnouncement != null && lastBroadcastAnnouncement?.isActive == true) {
            onUpdate(lastBroadcastAnnouncement)
        }
        val fs = firestore ?: return null
        return try {
            fs.collection(FIRESTORE_COLLECTION_ANNOUNCEMENTS).document(ANNOUNCEMENT_DOC_ID)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Announcement listen error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val ann = snapshot.data?.let { BroadcastAnnouncement.fromFirestoreMap(it) }
                        if (ann != null && ann.isActive && ann.message.isNotBlank()) {
                            val isNewAnnouncement = lastBroadcastAnnouncement?.id != ann.id || lastBroadcastAnnouncement?.message != ann.message
                            lastBroadcastAnnouncement = ann
                            onUpdate(ann)
                            if (isNewAnnouncement && !isPermanentUnlocked()) {
                                try {
                                    com.example.notification.NotificationHelper(context).sendCustomNotification("📢 PW DHYAN ANNOUNCEMENT", ann.message)
                                } catch (_: Exception) {}
                            }
                        } else {
                            lastBroadcastAnnouncement = null
                            onUpdate(null)
                        }
                    } else {
                        onUpdate(lastBroadcastAnnouncement)
                    }
                }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Toggles system-wide maintenance mode / emergency lockdown in Firestore.
     */
    fun setMaintenanceMode(isActive: Boolean, message: String = "", onComplete: ((Boolean) -> Unit)? = null) {
        val fs = firestore
        if (fs == null) {
            showToast("Firestore not connected!")
            onComplete?.invoke(false)
            return
        }
        val finalMsg = if (message.isNotBlank()) message.trim() else "Server maintenance is underway. Please check back shortly!"
        val data = mapOf(
            "isActive" to isActive,
            "message" to finalMsg,
            "updatedAt" to System.currentTimeMillis()
        )
        fs.collection(FIRESTORE_COLLECTION_SYSTEM).document(MAINTENANCE_DOC_ID)
            .set(data)
            .addOnSuccessListener {
                showToast(if (isActive) "Maintenance Mode ACTIVATED" else "Maintenance Mode DEACTIVATED")
                onComplete?.invoke(true)
            }
            .addOnFailureListener { e ->
                showToast("Failed to update maintenance: ${e.message}")
                onComplete?.invoke(false)
            }
    }

    /**
     * Listens in real time for Maintenance Mode state changes.
     */
    fun listenToMaintenance(onUpdate: (AppMaintenanceInfo) -> Unit): ListenerRegistration? {
        val fs = firestore ?: return null
        return try {
            fs.collection(FIRESTORE_COLLECTION_SYSTEM).document(MAINTENANCE_DOC_ID)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null && snapshot.exists()) {
                        val isActive = snapshot.getBoolean("isActive") ?: false
                        val msg = snapshot.getString("message") ?: "Server maintenance is underway. Please check back shortly!"
                        onUpdate(AppMaintenanceInfo(isActive = isActive, message = msg))
                    } else {
                        onUpdate(AppMaintenanceInfo(isActive = false))
                    }
                }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Formats all currently active (unused & unexpired) passkeys for 1-tap sharing.
     */
    fun getAllActiveKeysFormatted(): String {
        val activeKeys = getAllKeys().filter { !it.isExpired && !it.isUsed }
        if (activeKeys.isEmpty()) return "No active unused keys available. Tap '+ 1 Key' or 'Batch 10' to create new keys."
        val lines = activeKeys.mapIndexed { i, k -> "${i + 1}. ${k.code} (${if (k.isInfinite) "Permanent Admin" else "24-Hour Passkey"})" }.joinToString("\n")
        return "🔑 PW DHYAN ACTIVE PASSKEYS (${activeKeys.size} Available):\n$lines\n\nEnter any 6-digit code above on the lock screen for instant study access."
    }

    fun getCurrentAccessRemainingFormatted(): String {
        if (isPermanentUnlocked()) return "🌟 Admin (Infinite)"
        val key = currentActiveKey
        if (key != null) {
            return if (key.isInfinite) "🌟 Admin (Infinite)" else "⏱️ ${key.getRemainingTimeFormatted()}"
        }
        return "⏱️ Session Active"
    }

    fun isUrlAllowed(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val lower = url.lowercase().trim()

        if (lower == "about:blank" || lower.startsWith("data:") || lower.startsWith("blob:")) {
            return true
        }

        // Main study portal
        if (lower.startsWith("https://pw.studyparcham.in") ||
            lower.startsWith("http://pw.studyparcham.in") ||
            lower.startsWith("pw.studyparcham.in") ||
            lower.contains("studyparcham.in")
        ) {
            return true
        }

        // Educational content CDN, notes, DPP and Google Docs viewers
        if (lower.contains("cloudfront.net") ||
            lower.contains("pw.live") ||
            lower.contains("physicswallah") ||
            lower.contains("penpencil") ||
            lower.contains("amazonaws.com") ||
            lower.contains("drive.google.com") ||
            lower.contains("docs.google.com") ||
            lower.contains("googleusercontent.com") ||
            lower.contains("akamaized.net") ||
            lower.contains("fastly.net") ||
            lower.contains("jwplayer.com")
        ) {
            return true
        }

        return false
    }

    private fun showToast(msg: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }
}
