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
        private const val FIRESTORE_COLLECTION_KEYS = "access_keys"
        private const val FIRESTORE_COLLECTION_SESSIONS = "user_sessions"
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
        }
    }

    fun isSessionActive(): Boolean {
        if (isPermanentUnlocked()) return true

        val deviceId = getDeviceId()
        val suspendedUntil = prefs.getLong("suspended_until_$deviceId", 0L)
        if (suspendedUntil > System.currentTimeMillis()) {
            return false // On temporary timeout!
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
        expired.forEach { key ->
            fs.collection(FIRESTORE_COLLECTION_KEYS).document(key.id).delete()
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
        // ENFORCE CLOUD REQUIREMENT:
        if (!isFirestoreConnected && firestore == null) {
            initFirestore()
            return UnlockResult.Invalid(
                "Cloud Connection Required: Connecting to Google Cloud... Please ensure you have internet access and that Firestore Database is created."
            )
        }

        val cleaned = enteredCode.trim().replace(":", "").replace(" ", "")
        val thisDeviceId = getDeviceId()
        val thisDeviceModel = getDeviceModelName()

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

        // 1. Master Admin permanent code (240411, or 2404)
        if (cleaned == MASTER_PERMANENT_CODE || cleaned == "2404") {
            setPermanentUnlocked(true)
            saveSession(MASTER_PERMANENT_CODE, "Administrator", Long.MAX_VALUE, true)
            recordUserSession(thisDeviceId, thisDeviceModel, "Admin Master Code (240411)", "Administrator")
            return UnlockResult.PermanentUnlocked
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

        // 3. Check Phone Time (Only allowed when connected to cloud)
        val validTimeCodes = getValidPhoneTimeCodes()
        if (validTimeCodes.contains(cleaned)) {
            currentActiveKey = null
            saveSession(cleaned, "Time Pass", 24 * 60 * 60 * 1000L, false)
            recordUserSession(thisDeviceId, thisDeviceModel, "Phone Time ($cleaned)", "Time Pass")
            return UnlockResult.SessionUnlocked
        }

        // SANITIZED ERROR: Does NOT expose 240411!
        return UnlockResult.Invalid(
            "Incorrect passkey. Please check your 6-digit key or enter the current phone time."
        )
    }

    private fun recordUserSession(deviceId: String, deviceModel: String, passkey: String, label: String) {
        val session = UserSession(
            deviceId = deviceId,
            deviceModel = deviceModel,
            androidVersion = "Android ${Build.VERSION.RELEASE}",
            passkey = passkey,
            label = label,
            loginTime = System.currentTimeMillis()
        )

        try {
            firestore?.collection(FIRESTORE_COLLECTION_SESSIONS)?.document(deviceId)
                ?.set(session.toFirestoreMap())
        } catch (e: Exception) {
            Log.w(TAG, "Failed to record session to Firestore: ${e.message}")
        }
    }

    fun listenToUserSessions(onSessionsUpdated: (List<UserSession>) -> Unit): ListenerRegistration? {
        val fs = firestore ?: return null
        return try {
            fs.collection(FIRESTORE_COLLECTION_SESSIONS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Session listen error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val sessions = snapshot.documents.mapNotNull { doc ->
                            doc.data?.let { UserSession.fromFirestoreMap(it) }
                        }
                        onSessionsUpdated(sessions.sortedByDescending { it.loginTime })
                    }
                }
        } catch (e: Exception) {
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

    fun getCurrentAccessRemainingFormatted(): String {
        if (isPermanentUnlocked()) return "🌟 Admin (Infinite)"
        val key = currentActiveKey
        if (key != null) {
            return if (key.isInfinite) "🌟 Admin (Infinite)" else "⏱️ ${key.getRemainingTimeFormatted()}"
        }
        return "⏱️ Session Active"
    }

    fun getValidPhoneTimeCodes(): Set<String> {
        val codes = mutableSetOf<String>()
        val cal = Calendar.getInstance()

        for (offset in listOf(0, -1, 1)) {
            val c = cal.clone() as Calendar
            c.add(Calendar.MINUTE, offset)
            val date = c.time

            val format24 = SimpleDateFormat("HHmm", Locale.getDefault()).format(date)
            val format24NoZero = SimpleDateFormat("kmm", Locale.getDefault()).format(date)
            val formatH = SimpleDateFormat("Hmm", Locale.getDefault()).format(date)
            val format12 = SimpleDateFormat("hhmm", Locale.getDefault()).format(date)
            val format12NoZero = SimpleDateFormat("hmm", Locale.getDefault()).format(date)

            codes.add(format24)
            codes.add(format24NoZero)
            codes.add(formatH)
            codes.add(format12)
            codes.add(format12NoZero)
        }

        return codes
    }

    fun getCurrentTimeString(): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }

    fun isUrlAllowed(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val lower = url.lowercase().trim()

        if (lower == "about:blank" || lower.startsWith("data:") || lower.startsWith("blob:")) {
            return true
        }

        return lower.startsWith("https://pw.studyparcham.in") ||
                lower.startsWith("http://pw.studyparcham.in") ||
                lower.startsWith("pw.studyparcham.in")
    }

    private fun showToast(msg: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }
}
