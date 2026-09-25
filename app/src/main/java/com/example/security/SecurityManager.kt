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
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.SetOptions
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
        fun fromFirestoreMap(map: Map<String, Any?>, docId: String? = null): AccessKey {
            val keyId = docId?.takeIf { it.isNotBlank() } ?: (map["id"] as? String) ?: UUID.randomUUID().toString()
            return AccessKey(
                id = keyId,
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
 * Tracks a logged-in user device in Google Firestore with complete state synchronization.
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
    val isRevoked: Boolean = false,
    val currentLecture: String? = null,
    val currentSubject: String? = null,
    val currentChapter: String? = null,
    val currentProgressPercent: Int = 0,
    val totalWatchTimeSeconds: Long = 0L,
    val isPermanentAdmin: Boolean = false,
    val appVersion: String = "2.0.0",
    val isOnline: Boolean = true,
    val lastHeartbeat: Long = System.currentTimeMillis(),
    val currentUrl: String? = null,
    val currentPageTitle: String? = null,
    val currentPortal: String = "studyparcham",
    val assignedPortal: String? = null,
    val blockedPortal: String? = null
) {
    val isCurrentlyOnline: Boolean get() = isOnline && (System.currentTimeMillis() - maxOf(lastHeartbeat, lastActiveTime) < 35000L)

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
        val map = mutableMapOf<String, Any>(
            "deviceId" to deviceId,
            "deviceModel" to deviceModel,
            "androidVersion" to androidVersion,
            "passkey" to passkey,
            "label" to label,
            "loginTime" to loginTime,
            "lastActiveTime" to lastActiveTime,
            "suspendedUntil" to suspendedUntil,
            "isRevoked" to isRevoked,
            "currentProgressPercent" to currentProgressPercent,
            "totalWatchTimeSeconds" to totalWatchTimeSeconds,
            "isPermanentAdmin" to isPermanentAdmin,
            "appVersion" to appVersion,
            "isOnline" to isOnline,
            "lastHeartbeat" to lastHeartbeat,
            "currentPortal" to currentPortal
        )
        currentLecture?.let { map["currentLecture"] = it }
        currentSubject?.let { map["currentSubject"] = it }
        currentChapter?.let { map["currentChapter"] = it }
        currentUrl?.let { map["currentUrl"] = it }
        currentPageTitle?.let { map["currentPageTitle"] = it }
        assignedPortal?.let { map["assignedPortal"] = it }
        blockedPortal?.let { map["blockedPortal"] = it }
        return map
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
                isRevoked = (map["isRevoked"] as? Boolean) ?: false,
                currentLecture = map["currentLecture"] as? String,
                currentSubject = map["currentSubject"] as? String,
                currentChapter = map["currentChapter"] as? String,
                currentProgressPercent = (map["currentProgressPercent"] as? Number)?.toInt() ?: 0,
                totalWatchTimeSeconds = (map["totalWatchTimeSeconds"] as? Number)?.toLong() ?: 0L,
                isPermanentAdmin = (map["isPermanentAdmin"] as? Boolean) ?: false,
                appVersion = (map["appVersion"] as? String) ?: "2.0.0",
                isOnline = (map["isOnline"] as? Boolean) ?: true,
                lastHeartbeat = (map["lastHeartbeat"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                currentUrl = map["currentUrl"] as? String,
                currentPageTitle = map["currentPageTitle"] as? String,
                currentPortal = (map["currentPortal"] as? String) ?: "studyparcham",
                assignedPortal = map["assignedPortal"] as? String,
                blockedPortal = map["blockedPortal"] as? String
            )
        }
    }
}

/**
 * Tracks real-time lecture watch activity.
 */
data class WatchLog(
    val id: String = UUID.randomUUID().toString(),
    val deviceId: String,
    val deviceModel: String,
    val passkey: String,
    val lectureTitle: String,
    val subjectName: String = "",
    val chapterName: String = "",
    val currentTime: Long = 0L,
    val duration: Long = 0L,
    val progressPercent: Int = 0,
    val isCompleted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun getFormattedTime(): String {
        return SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(timestamp))
    }

    fun toFirestoreMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "deviceId" to deviceId,
            "deviceModel" to deviceModel,
            "passkey" to passkey,
            "lectureTitle" to lectureTitle,
            "subjectName" to subjectName,
            "chapterName" to chapterName,
            "currentTime" to currentTime,
            "duration" to duration,
            "progressPercent" to progressPercent,
            "isCompleted" to isCompleted,
            "timestamp" to timestamp
        )
    }

    companion object {
        fun fromFirestoreMap(map: Map<String, Any?>): WatchLog {
            return WatchLog(
                id = (map["id"] as? String) ?: UUID.randomUUID().toString(),
                deviceId = (map["deviceId"] as? String) ?: "",
                deviceModel = (map["deviceModel"] as? String) ?: "Android",
                passkey = (map["passkey"] as? String) ?: "",
                lectureTitle = (map["lectureTitle"] as? String) ?: "Lecture",
                subjectName = (map["subjectName"] as? String) ?: "",
                chapterName = (map["chapterName"] as? String) ?: "",
                currentTime = (map["currentTime"] as? Number)?.toLong() ?: 0L,
                duration = (map["duration"] as? Number)?.toLong() ?: 0L,
                progressPercent = (map["progressPercent"] as? Number)?.toInt() ?: 0,
                isCompleted = (map["isCompleted"] as? Boolean) ?: false,
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}

/**
 * Tracks login history & authentication audit log.
 */
data class LoginLog(
    val id: String = UUID.randomUUID().toString(),
    val deviceId: String,
    val deviceModel: String,
    val androidVersion: String,
    val passkey: String,
    val label: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "SUCCESS"
) {
    fun getFormattedTime(): String {
        return SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(timestamp))
    }

    fun toFirestoreMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "deviceId" to deviceId,
            "deviceModel" to deviceModel,
            "androidVersion" to androidVersion,
            "passkey" to passkey,
            "label" to label,
            "timestamp" to timestamp,
            "status" to status
        )
    }

    companion object {
        fun fromFirestoreMap(map: Map<String, Any?>): LoginLog {
            return LoginLog(
                id = (map["id"] as? String) ?: UUID.randomUUID().toString(),
                deviceId = (map["deviceId"] as? String) ?: "",
                deviceModel = (map["deviceModel"] as? String) ?: "Android",
                androidVersion = (map["androidVersion"] as? String) ?: "Android",
                passkey = (map["passkey"] as? String) ?: "",
                label = (map["label"] as? String) ?: "Student",
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                status = (map["status"] as? String) ?: "SUCCESS"
            )
        }
    }
}

/**
 * Targeted & Global Push Notifications.
 */
data class PushNotificationItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val targetType: String = "ALL", // "ALL", "DEVICE", "PASSKEY"
    val targetValue: String = "ALL",
    val author: String = "Admin Dhyan",
    val timestamp: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val isBurst: Boolean = false,
    val burstCount: Int = 1
) {
    fun getFormattedTime(): String {
        return SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(timestamp))
    }

    fun toFirestoreMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "title" to title,
            "message" to message,
            "targetType" to targetType,
            "targetValue" to targetValue,
            "author" to author,
            "timestamp" to timestamp,
            "isActive" to isActive,
            "isBurst" to isBurst,
            "burstCount" to burstCount
        )
    }

    companion object {
        fun fromFirestoreMap(map: Map<String, Any?>): PushNotificationItem {
            return PushNotificationItem(
                id = (map["id"] as? String) ?: UUID.randomUUID().toString(),
                title = (map["title"] as? String) ?: "Notification",
                message = (map["message"] as? String) ?: "",
                targetType = (map["targetType"] as? String) ?: "ALL",
                targetValue = (map["targetValue"] as? String) ?: "ALL",
                author = (map["author"] as? String) ?: "Admin",
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                isActive = (map["isActive"] as? Boolean) ?: true,
                isBurst = (map["isBurst"] as? Boolean) ?: false,
                burstCount = (map["burstCount"] as? Number)?.toInt() ?: 1
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

/**
 * Individual Portal Maintenance state synced in Firestore under system_config/portal_config.
 */
data class PortalMaintenanceInfo(
    val isActive: Boolean = false,
    val message: String = "This portal is currently under maintenance. Please switch to the other portal!"
)

/**
 * Central Portal Configuration synced in real time across Android, Web, and Admin.
 */
data class PortalConfig(
    val defaultPortalId: String = "studyparcham",
    val studyparchamMaintenance: PortalMaintenanceInfo = PortalMaintenanceInfo(message = "Server Sun ☀️ is undergoing maintenance. Please switch to Server Moon 🌙!"),
    val pwthorMaintenance: PortalMaintenanceInfo = PortalMaintenanceInfo(message = "Server Moon 🌙 is undergoing maintenance. Please switch to Server Sun ☀️!"),
    val blockedPortals: List<String> = emptyList()
) {
    val defaultPortal: SecurityManager.Portal get() = SecurityManager.Portal.values().find { it.id == defaultPortalId } ?: SecurityManager.Portal.STUDYPARCHAM
    fun isMaintenance(portal: SecurityManager.Portal): Boolean = when(portal) {
        SecurityManager.Portal.STUDYPARCHAM -> studyparchamMaintenance.isActive
        SecurityManager.Portal.PWTHOR -> pwthorMaintenance.isActive
    }
    fun getMaintenanceMessage(portal: SecurityManager.Portal): String = when(portal) {
        SecurityManager.Portal.STUDYPARCHAM -> studyparchamMaintenance.message
        SecurityManager.Portal.PWTHOR -> pwthorMaintenance.message
    }
    fun isBlocked(portal: SecurityManager.Portal): Boolean = blockedPortals.contains(portal.id)
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
    private var lastBroadcastAnnouncement: BroadcastAnnouncement? = null
    private val broadcastListeners = mutableListOf<(BroadcastAnnouncement?) -> Unit>()
    private val sessionStateListeners = mutableListOf<(Boolean) -> Unit>()
    private val processedNotificationIds = mutableSetOf<String>()

    // Portal state and listeners
    private var cachedPortalConfig = PortalConfig()
    private val portalConfigListeners = mutableListOf<(PortalConfig) -> Unit>()
    private val portalChangeListeners = mutableListOf<(portal: Portal, reason: String) -> Unit>()

    companion object {
        const val MASTER_PERMANENT_CODE = "240411"
        private const val KEY_PERMANENT_UNLOCKED = "is_permanent_unlocked"
        private const val KEY_REVOKED_DEVICES = "revoked_devices_set"
        private const val KEY_SESSION_ACTIVE = "session_active"
        private const val KEY_SESSION_EXPIRY = "session_expiry"
        private const val KEY_SESSION_CODE = "session_code"
        private const val KEY_SESSION_LABEL = "session_label"
        private const val KEY_SESSION_IS_INFINITE = "session_is_infinite"
        private const val KEY_SELECTED_PORTAL = "selected_portal_id"
        const val HOME_URL = "https://pw.studyparcham.in/#home-view"
        const val ALLOWED_DOMAIN = "pw.studyparcham.in"
        const val DIRECT_APK_DOWNLOAD_URL = "https://github.com/Dhyan2404/pw-dhyan/releases/latest/download/PW-DHYAN.apk"
        private const val FIRESTORE_COLLECTION_KEYS = "access_keys"
        private const val FIRESTORE_COLLECTION_SESSIONS = "user_sessions"
        private const val FIRESTORE_COLLECTION_ANNOUNCEMENTS = "announcements"
        private const val FIRESTORE_COLLECTION_SYSTEM = "system_config"
        private const val FIRESTORE_COLLECTION_WATCH_LOGS = "watch_logs"
        private const val FIRESTORE_COLLECTION_LOGIN_LOGS = "login_logs"
        private const val FIRESTORE_COLLECTION_NOTIFICATIONS = "push_notifications"
        private const val ANNOUNCEMENT_DOC_ID = "latest_announcement"
        private const val MAINTENANCE_DOC_ID = "maintenance_mode"
        private const val PORTAL_CONFIG_DOC_ID = "portal_config"
        private const val TAG = "FirestoreSecurity"
    }

    enum class Portal(val id: String, val displayName: String, val url: String, val domain: String) {
        STUDYPARCHAM("studyparcham", "Server Sun ☀️", "https://pw.studyparcham.in/#home-view", "pw.studyparcham.in"),
        PWTHOR("pwthor", "Server Moon 🌙", "https://pwthor.live/study", "pwthor.live")
    }

    fun addPortalChangeListener(listener: (portal: Portal, reason: String) -> Unit) {
        portalChangeListeners.add(listener)
    }

    fun removePortalChangeListener(listener: (portal: Portal, reason: String) -> Unit) {
        portalChangeListeners.remove(listener)
    }

    fun notifyPortalChanged(portal: Portal, reason: String) {
        Handler(Looper.getMainLooper()).post {
            portalChangeListeners.forEach { it.invoke(portal, reason) }
        }
    }

    fun addPortalConfigListener(listener: (PortalConfig) -> Unit) {
        portalConfigListeners.add(listener)
        listener(cachedPortalConfig)
    }

    fun removePortalConfigListener(listener: (PortalConfig) -> Unit) {
        portalConfigListeners.remove(listener)
    }

    fun getPortalConfig(): PortalConfig = cachedPortalConfig

    fun hasUserExplicitlyChosenPortal(): Boolean = prefs.contains(KEY_SELECTED_PORTAL)

    fun getSelectedPortal(): Portal {
        if (!hasUserExplicitlyChosenPortal()) {
            return cachedPortalConfig.defaultPortal
        }
        val id = prefs.getString(KEY_SELECTED_PORTAL, cachedPortalConfig.defaultPortalId)
        return Portal.values().find { it.id == id } ?: cachedPortalConfig.defaultPortal
    }

    fun setSelectedPortal(portal: Portal) {
        prefs.edit().putString(KEY_SELECTED_PORTAL, portal.id).apply()
        try {
            val myDeviceId = getDeviceId()
            firestore?.collection(FIRESTORE_COLLECTION_SESSIONS)?.document(myDeviceId)?.update("currentPortal", portal.id)
        } catch (_: Exception) {}
    }

    fun getCurrentPortalUrl(): String {
        return getSelectedPortal().url
    }

    init {
        initFirestore()
        restoreSessionFromPrefs()
        ensureDeviceRegistered()
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
            val fs = FirebaseFirestore.getInstance()
            try {
                val settings = FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build()
                fs.firestoreSettings = settings
            } catch (se: Exception) {
                Log.w(TAG, "Offline settings config note: ${se.message}")
            }
            firestore = fs
            listenToFirestore()
            syncCurrentSessionToCloud()
            Log.d(TAG, "Google Cloud Firestore initialized with offline cache")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase Firestore", e)
            firestore = null
            isFirestoreConnected = false
        }
    }

    private var cloudAdminPin: String? = null

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
                            doc.data?.let { AccessKey.fromFirestoreMap(it, doc.id) }
                        }
                        cloudKeysList = remoteKeys.toMutableList()
                        notifyListeners(cloudKeysList.sortedByDescending { it.createdAt })
                        // Sync current session state when cloud connection is verified
                        syncCurrentSessionToCloud()
                    }
                }

            // Sync Cloud Admin PIN
            fs.collection(FIRESTORE_COLLECTION_SYSTEM).document("admin_auth")
                .addSnapshotListener { doc, _ ->
                    if (doc != null && doc.exists()) {
                        cloudAdminPin = doc.getString("pin")
                    }
                }

            // Real-time listener for Portal Configuration (Default portal, Maintenance per portal, Blocked portals)
            fs.collection(FIRESTORE_COLLECTION_SYSTEM).document(PORTAL_CONFIG_DOC_ID)
                .addSnapshotListener { doc, err ->
                    if (err != null || doc == null || !doc.exists()) return@addSnapshotListener
                    try {
                        val defPortalId = doc.getString("defaultPortal") ?: "studyparcham"
                        val spMap = doc.get("studyparchamMaintenance") as? Map<String, Any?>
                        val pwMap = doc.get("pwthorMaintenance") as? Map<String, Any?>
                        val blockedList = (doc.get("blockedPortals") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

                        val spMaint = PortalMaintenanceInfo(
                            isActive = spMap?.get("isActive") as? Boolean ?: false,
                            message = (spMap?.get("message") as? String) ?: "StudyParcham is undergoing maintenance. Please switch to PWThor Live!"
                        )
                        val pwMaint = PortalMaintenanceInfo(
                            isActive = pwMap?.get("isActive") as? Boolean ?: false,
                            message = (pwMap?.get("message") as? String) ?: "PWThor Live is undergoing maintenance. Please switch to StudyParcham!"
                        )

                        cachedPortalConfig = PortalConfig(
                            defaultPortalId = defPortalId,
                            studyparchamMaintenance = spMaint,
                            pwthorMaintenance = pwMaint,
                            blockedPortals = blockedList
                        )

                        Handler(Looper.getMainLooper()).post {
                            portalConfigListeners.forEach { it.invoke(cachedPortalConfig) }
                        }

                        // First time user: if user hasn't manually chosen a portal yet, automatically apply the admin's default portal
                        if (!hasUserExplicitlyChosenPortal()) {
                            val def = cachedPortalConfig.defaultPortal
                            setSelectedPortal(def)
                            notifyPortalChanged(def, "Default Portal Initialized")
                        }

                        // If user's currently selected portal is globally blocked, switch to the other portal!
                        val cur = getSelectedPortal()
                        if (cachedPortalConfig.isBlocked(cur)) {
                            val alt = if (cur == Portal.STUDYPARCHAM) Portal.PWTHOR else Portal.STUDYPARCHAM
                            if (!cachedPortalConfig.isBlocked(alt)) {
                                setSelectedPortal(alt)
                                notifyPortalChanged(alt, "${cur.displayName} is blocked by Admin. Switched to ${alt.displayName}.")
                                showToast("${cur.displayName} is disabled by Admin. Switched to ${alt.displayName}.")
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing portal config: ${e.message}")
                    }
                }

            // Real-time listener for this specific device's session document
            // Enables Admin to grant custom hours or extend access remotely without user login!
            val myDeviceId = getDeviceId()
            fs.collection(FIRESTORE_COLLECTION_SESSIONS).document(myDeviceId)
                .addSnapshotListener { doc, err ->
                    if (err != null || doc == null || !doc.exists()) return@addSnapshotListener
                    val data = doc.data ?: return@addSnapshotListener

                    val isRevoked = data["isRevoked"] as? Boolean ?: false
                    if (isRevoked) {
                        clearSession()
                        notifySessionStateChanged(false)
                        return@addSnapshotListener
                    }

                    val suspendedUntil = (data["suspendedUntil"] as? Number)?.toLong() ?: 0L
                    if (suspendedUntil > System.currentTimeMillis()) {
                        prefs.edit().putLong("suspended_until_$myDeviceId", suspendedUntil).apply()
                        notifySessionStateChanged(false)
                        return@addSnapshotListener
                    }

                    val sessionExpiry = (data["sessionExpiry"] as? Number)?.toLong() ?: 0L
                    val isInf = data["isPermanentAdmin"] as? Boolean ?: false
                    val passkey = data["passkey"] as? String ?: "Admin Granted"
                    val label = data["label"] as? String ?: "Student"

                    if (isInf || sessionExpiry > System.currentTimeMillis()) {
                        val currentExpiry = prefs.getLong(KEY_SESSION_EXPIRY, 0L)
                        if (!isSessionActive() || sessionExpiry > currentExpiry || isInf) {
                            val remMillis = if (isInf) Long.MAX_VALUE else (sessionExpiry - System.currentTimeMillis()).coerceAtLeast(60000L)
                            saveSession(passkey, label, remMillis, isInf)
                            if (isInf) setPermanentUnlocked(true)
                            notifySessionStateChanged(true)
                        }
                    }

                    // Admin Remote Portal Assignment for this user
                    val remoteAssignedPortalId = data["assignedPortal"] as? String
                    if (!remoteAssignedPortalId.isNullOrBlank()) {
                        val assignedPortal = Portal.values().find { it.id == remoteAssignedPortalId }
                        if (assignedPortal != null && assignedPortal != getSelectedPortal()) {
                            setSelectedPortal(assignedPortal)
                            notifyPortalChanged(assignedPortal, "Admin changed your portal to ${assignedPortal.displayName}")
                            showToast("Admin switched your portal to ${assignedPortal.displayName}")
                        }
                    }

                    // Admin Remote Block for specific portal for this user
                    val userBlockedPortalId = data["blockedPortal"] as? String
                    if (!userBlockedPortalId.isNullOrBlank()) {
                        val cur = getSelectedPortal()
                        if (cur.id == userBlockedPortalId) {
                            val alt = if (cur == Portal.STUDYPARCHAM) Portal.PWTHOR else Portal.STUDYPARCHAM
                            setSelectedPortal(alt)
                            notifyPortalChanged(alt, "${cur.displayName} has been blocked for this device. Switched to ${alt.displayName}.")
                            showToast("${cur.displayName} is blocked by Admin. Switched to ${alt.displayName}.")
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error attaching Firestore listener", e)
            isFirestoreConnected = false
        }
    }

    fun ensureDeviceRegistered() {
        val fs = firestore ?: return
        val myDeviceId = getDeviceId()
        val myDeviceModel = getDeviceModelName()
        val now = System.currentTimeMillis()
        val isInf = isPermanentUnlocked()
        val isAct = isSessionActive()

        val base = mutableMapOf<String, Any>(
            "deviceId" to myDeviceId,
            "deviceModel" to myDeviceModel,
            "androidVersion" to "Android ${Build.VERSION.RELEASE}",
            "lastActiveTime" to now,
            "lastHeartbeat" to now,
            "isOnline" to true,
            "appVersion" to "2.0.0",
            "currentPortal" to getSelectedPortal().id
        )
        if (isAct) {
            base["passkey"] = prefs.getString(KEY_SESSION_CODE, if (isInf) "Master Admin" else "Active Session") ?: "Active Session"
            base["label"] = prefs.getString(KEY_SESSION_LABEL, if (isInf) "Administrator" else "Student") ?: "Student"
            base["sessionExpiry"] = prefs.getLong(KEY_SESSION_EXPIRY, now + 24 * 3600 * 1000L)
            base["isPermanentAdmin"] = isInf
        } else {
            base["passkey"] = "🔒 Locked / Awaiting Access"
            base["label"] = "Student"
        }
        try {
            fs.collection(FIRESTORE_COLLECTION_SESSIONS).document(myDeviceId)
                .set(base, SetOptions.merge())
        } catch (_: Exception) {}
    }

    fun isCloudSyncActive(): Boolean = isFirestoreConnected

    fun addSessionStateListener(listener: (Boolean) -> Unit) {
        sessionStateListeners.add(listener)
    }

    fun removeSessionStateListener(listener: (Boolean) -> Unit) {
        sessionStateListeners.remove(listener)
    }

    private fun notifySessionStateChanged(isUnlocked: Boolean) {
        sessionStateListeners.forEach { it.invoke(isUnlocked) }
    }

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
        val cloudPin = cloudAdminPin
        return clean == MASTER_PERMANENT_CODE || (!cloudPin.isNullOrBlank() && clean == cloudPin)
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
            } while (candidate == MASTER_PERMANENT_CODE || existingCodes.contains(candidate))
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
            } while (candidate == MASTER_PERMANENT_CODE || existingCodes.contains(candidate))
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

        // 1. Master Admin permanent code (240411) - ALWAYS VERIFIED REGARDLESS OF NETWORK
        if (cleaned == MASTER_PERMANENT_CODE) {
            setPermanentUnlocked(true)
            saveSession(MASTER_PERMANENT_CODE, "Administrator", Long.MAX_VALUE, true)
            recordUserSession(thisDeviceId, thisDeviceModel, "Master Admin", "Administrator")
            recordLoginLog(MASTER_PERMANENT_CODE, "Administrator", "ADMIN_UNLOCK")
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
            recordLoginLog(cleaned, "Revoked Device Attempt", "BLOCKED_REVOKED")
            return UnlockResult.Invalid("Access Revoked: Your device has been restricted by Admin.")
        }

        // Check if device is in temporary timeout (e.g. 5-min cooldown)
        val suspendedUntil = prefs.getLong("suspended_until_$thisDeviceId", 0L)
        if (suspendedUntil > System.currentTimeMillis()) {
            val remMins = ((suspendedUntil - System.currentTimeMillis()) / 60000) + 1
            recordLoginLog(cleaned, "Timeout Device Attempt", "BLOCKED_TIMEOUT")
            return UnlockResult.Invalid("Session Cooldown: Admin placed your device on a $remMins min timeout.")
        }

        // 2. Check 24-Hour and Infinite Keys from Cloud Firestore
        val allKeys = getAllKeys()
        val matchedKey = allKeys.firstOrNull { it.code == cleaned }
        if (matchedKey != null) {
            if (matchedKey.isExpired) {
                recordLoginLog(cleaned, matchedKey.label, "FAILED_EXPIRED")
                return UnlockResult.Invalid("Passkey Expired: This access key has concluded.")
            }

            // 1-Key 1-Time Device Binding:
            if (matchedKey.isUsed && matchedKey.deviceId != null && matchedKey.deviceId != thisDeviceId) {
                recordLoginLog(cleaned, matchedKey.label, "FAILED_DEVICE_MISMATCH")
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
            recordLoginLog(boundKey.code, boundKey.label, "SUCCESS")

            if (boundKey.isInfinite) {
                setPermanentUnlocked(true)
            }
            saveSession(boundKey.code, boundKey.label, boundKey.durationMillis, boundKey.isInfinite)
            return UnlockResult.KeyUnlocked(boundKey)
        }

        // SANITIZED ERROR: Only valid passkeys accepted (never leak master admin code)
        recordLoginLog(cleaned, "Unknown Key", "INVALID_CODE")
        return UnlockResult.Invalid(
            "Invalid passkey. Please check your 6-digit access code and try again."
        )
    }

    fun recordCurrentDeviceSession() {
        val thisDeviceId = getDeviceId()
        val thisDeviceModel = getDeviceModelName()
        val passkey = prefs.getString(KEY_SESSION_CODE, if (isPermanentUnlocked()) "Master Admin" else "Active Session") ?: "Active Session"
        val label = prefs.getString(KEY_SESSION_LABEL, if (isPermanentUnlocked()) "Administrator" else "Student") ?: "Student"
        recordUserSession(thisDeviceId, thisDeviceModel, passkey, label)
    }

    fun getCurrentUserSession(): UserSession {
        val thisDeviceId = getDeviceId()
        val thisDeviceModel = getDeviceModelName()
        val isInf = isPermanentUnlocked()
        val passkey = prefs.getString(KEY_SESSION_CODE, if (isInf) "Master Admin" else "Active Session") ?: "Active Session"
        val label = prefs.getString(KEY_SESSION_LABEL, if (isInf) "Administrator" else "Student") ?: "Student"
        val now = System.currentTimeMillis()
        return UserSession(
            deviceId = thisDeviceId,
            deviceModel = thisDeviceModel,
            androidVersion = "Android ${Build.VERSION.RELEASE}",
            passkey = passkey,
            label = label,
            loginTime = prefs.getLong("session_login_time", now),
            lastActiveTime = now,
            lastHeartbeat = now,
            isOnline = true,
            isPermanentAdmin = isInf,
            appVersion = "2.0.0",
            currentPortal = getSelectedPortal().id
        )
    }

    fun recordUserSession(deviceId: String, deviceModel: String, passkey: String, label: String) {
        val now = System.currentTimeMillis()
        if (!prefs.contains("session_login_time")) {
            prefs.edit().putLong("session_login_time", now).apply()
        }
        val loginTime = prefs.getLong("session_login_time", now)
        val isInf = isPermanentUnlocked()
        val session = UserSession(
            deviceId = deviceId,
            deviceModel = deviceModel,
            androidVersion = "Android ${Build.VERSION.RELEASE}",
            passkey = passkey,
            label = label,
            loginTime = loginTime,
            lastActiveTime = now,
            lastHeartbeat = now,
            isOnline = true,
            isPermanentAdmin = isInf,
            appVersion = "2.0.0",
            currentPortal = getSelectedPortal().id
        )

        try {
            firestore?.collection(FIRESTORE_COLLECTION_SESSIONS)?.document(deviceId)
                ?.set(session.toFirestoreMap(), SetOptions.merge())
                ?.addOnSuccessListener {
                    Log.d(TAG, "User session successfully saved in Cloud Firestore for $deviceId")
                }
                ?.addOnFailureListener { e ->
                    Log.w(TAG, "Failed to record session to Firestore: ${e.message}")
                }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to record session to Firestore: ${e.message}")
        }
    }

    fun syncCurrentSessionToCloud() {
        val curSession = getCurrentUserSession()
        val fs = firestore ?: return
        try {
            fs.collection(FIRESTORE_COLLECTION_SESSIONS).document(curSession.deviceId)
                .set(curSession.toFirestoreMap(), SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "Session synced to Cloud Firestore: ${curSession.deviceId}")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Cloud session sync failed: ${e.message}")
                }
        } catch (e: Exception) {
            Log.w(TAG, "syncCurrentSessionToCloud exception: ${e.message}")
        }
    }

    fun sendHeartbeat(
        currentUrl: String? = null,
        currentPageTitle: String? = null,
        currentLecture: String? = null
    ) {
        val fs = firestore ?: return
        val thisDeviceId = getDeviceId()
        val now = System.currentTimeMillis()
        val update = mutableMapOf<String, Any>(
            "lastActiveTime" to now,
            "lastHeartbeat" to now,
            "isOnline" to true,
            "currentPortal" to getSelectedPortal().id
        )
        if (!currentUrl.isNullOrBlank()) update["currentUrl"] = currentUrl
        if (!currentPageTitle.isNullOrBlank()) update["currentPageTitle"] = currentPageTitle
        if (currentLecture != null) {
            if (currentLecture.isBlank()) {
                update["currentLecture"] = ""
                update["currentProgressPercent"] = 0
            } else {
                update["currentLecture"] = currentLecture
            }
        }

        try {
            fs.collection(FIRESTORE_COLLECTION_SESSIONS).document(thisDeviceId)
                .set(update, SetOptions.merge())
                .addOnFailureListener { e ->
                    Log.w(TAG, "Heartbeat failed: ${e.message}")
                }
        } catch (_: Exception) {}
    }

    fun setDeviceOffline() {
        val fs = firestore ?: return
        val thisDeviceId = getDeviceId()
        try {
            fs.collection(FIRESTORE_COLLECTION_SESSIONS).document(thisDeviceId)
                .set(
                    mapOf(
                        "isOnline" to false,
                        "lastActiveTime" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                )
        } catch (_: Exception) {}
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
            ?.set(mapOf("isRevoked" to true, "isOnline" to false), SetOptions.merge())
            ?.addOnCompleteListener { onComplete?.invoke(true) }
    }

    fun suspendDevice(deviceId: String, durationMinutes: Int = 5, onComplete: ((Boolean) -> Unit)? = null) {
        val timeoutUntil = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        prefs.edit().putLong("suspended_until_$deviceId", timeoutUntil).apply()
        firestore?.collection(FIRESTORE_COLLECTION_SESSIONS)?.document(deviceId)
            ?.set(mapOf("suspendedUntil" to timeoutUntil), SetOptions.merge())
            ?.addOnCompleteListener { onComplete?.invoke(true) }
    }

    fun unsuspendDevice(deviceId: String, onComplete: ((Boolean) -> Unit)? = null) {
        prefs.edit().remove("suspended_until_$deviceId").apply()
        firestore?.collection(FIRESTORE_COLLECTION_SESSIONS)?.document(deviceId)
            ?.set(mapOf("suspendedUntil" to 0L), SetOptions.merge())
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
     * Admin remotely changes the portal assignment of any specific user device.
     */
    fun assignUserPortal(targetDeviceId: String, portal: Portal, onComplete: ((Boolean) -> Unit)? = null) {
        val fs = firestore ?: run {
            showToast("Firestore offline")
            onComplete?.invoke(false)
            return
        }
        fs.collection(FIRESTORE_COLLECTION_SESSIONS).document(targetDeviceId)
            .update("assignedPortal", portal.id)
            .addOnSuccessListener {
                showToast("User portal assigned to ${portal.displayName}")
                onComplete?.invoke(true)
            }
            .addOnFailureListener { e ->
                showToast("Failed to assign portal: ${e.message}")
                onComplete?.invoke(false)
            }
    }

    /**
     * Admin blocks or unblocks a specific portal for a specific user device.
     */
    fun blockUserPortal(targetDeviceId: String, portalId: String?, onComplete: ((Boolean) -> Unit)? = null) {
        val fs = firestore ?: run {
            showToast("Firestore offline")
            onComplete?.invoke(false)
            return
        }
        val updateMap = mutableMapOf<String, Any?>("blockedPortal" to portalId)
        fs.collection(FIRESTORE_COLLECTION_SESSIONS).document(targetDeviceId)
            .set(updateMap, SetOptions.merge())
            .addOnSuccessListener {
                showToast(if (portalId == null) "Portal unblocked for device" else "Portal $portalId blocked for device")
                onComplete?.invoke(true)
            }
            .addOnFailureListener { e ->
                showToast("Error: ${e.message}")
                onComplete?.invoke(false)
            }
    }

    /**
     * Admin sets a specific portal in or out of maintenance independently.
     */
    fun setPortalMaintenance(portal: Portal, isActive: Boolean, message: String, onComplete: ((Boolean) -> Unit)? = null) {
        val fs = firestore ?: run {
            showToast("Firestore offline")
            onComplete?.invoke(false)
            return
        }
        val fieldName = if (portal == Portal.STUDYPARCHAM) "studyparchamMaintenance" else "pwthorMaintenance"
        val payload = mapOf(
            fieldName to mapOf(
                "isActive" to isActive,
                "message" to message.ifBlank { "${portal.displayName} is currently undergoing maintenance." },
                "updatedAt" to System.currentTimeMillis()
            )
        )
        fs.collection(FIRESTORE_COLLECTION_SYSTEM).document(PORTAL_CONFIG_DOC_ID)
            .set(payload, SetOptions.merge())
            .addOnSuccessListener {
                showToast("${portal.displayName} Maintenance ${if (isActive) "ACTIVATED" else "DEACTIVATED"}")
                onComplete?.invoke(true)
            }
            .addOnFailureListener { e ->
                showToast("Failed: ${e.message}")
                onComplete?.invoke(false)
            }
    }

    /**
     * Admin configures the global default portal for new/unassigned users.
     */
    fun setGlobalDefaultPortal(portal: Portal, onComplete: ((Boolean) -> Unit)? = null) {
        val fs = firestore ?: run {
            showToast("Firestore offline")
            onComplete?.invoke(false)
            return
        }
        fs.collection(FIRESTORE_COLLECTION_SYSTEM).document(PORTAL_CONFIG_DOC_ID)
            .set(mapOf("defaultPortal" to portal.id), SetOptions.merge())
            .addOnSuccessListener {
                showToast("Default portal set to ${portal.displayName}")
                onComplete?.invoke(true)
            }
            .addOnFailureListener { e ->
                showToast("Failed: ${e.message}")
                onComplete?.invoke(false)
            }
    }

    /**
     * Admin globally disables/blocks a portal for all students.
     */
    fun setGlobalBlockedPortal(portal: Portal, isBlocked: Boolean, onComplete: ((Boolean) -> Unit)? = null) {
        val fs = firestore ?: run {
            showToast("Firestore offline")
            onComplete?.invoke(false)
            return
        }
        val currentBlocked = cachedPortalConfig.blockedPortals.toMutableSet()
        if (isBlocked) currentBlocked.add(portal.id) else currentBlocked.remove(portal.id)
        fs.collection(FIRESTORE_COLLECTION_SYSTEM).document(PORTAL_CONFIG_DOC_ID)
            .set(mapOf("blockedPortals" to currentBlocked.toList()), SetOptions.merge())
            .addOnSuccessListener {
                showToast("${portal.displayName} ${if (isBlocked) "BLOCKED" else "UNBLOCKED"} globally")
                onComplete?.invoke(true)
            }
            .addOnFailureListener { e ->
                showToast("Failed: ${e.message}")
                onComplete?.invoke(false)
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

    // Cloud Watch Logging Engine
    fun recordWatchProgress(
        lectureTitle: String,
        subjectName: String = "",
        chapterName: String = "",
        currentTime: Long = 0L,
        duration: Long = 0L,
        progressPercent: Int = 0
    ) {
        val thisDeviceId = getDeviceId()
        val thisDeviceModel = getDeviceModelName()
        val passkey = prefs.getString(KEY_SESSION_CODE, if (isPermanentUnlocked()) "Master Admin" else "Student") ?: "Student"
        val now = System.currentTimeMillis()
        val isCompleted = progressPercent >= 90

        // 1. Update live user session doc in Firestore with SetOptions.merge() so it NEVER fails if doc was missing!
        val sessionUpdate = mutableMapOf<String, Any>(
            "deviceId" to thisDeviceId,
            "deviceModel" to thisDeviceModel,
            "androidVersion" to "Android ${Build.VERSION.RELEASE}",
            "lastActiveTime" to now,
            "lastHeartbeat" to now,
            "isOnline" to true,
            "currentProgressPercent" to progressPercent
        )
        if (lectureTitle.isNotBlank()) {
            sessionUpdate["currentLecture"] = lectureTitle
            sessionUpdate["currentPageTitle"] = "Watching: $lectureTitle"
        }
        if (subjectName.isNotBlank()) sessionUpdate["currentSubject"] = subjectName
        if (chapterName.isNotBlank()) sessionUpdate["currentChapter"] = chapterName

        val fs = firestore
        if (fs != null) {
            fs.collection(FIRESTORE_COLLECTION_SESSIONS).document(thisDeviceId)
                .set(sessionUpdate, SetOptions.merge())
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to update session progress: ${e.message}")
                }

            // 2. Save detailed record in watch_logs collection
            val safeKey = "${thisDeviceId}_${lectureTitle.hashCode()}"
            val watchLog = WatchLog(
                id = safeKey,
                deviceId = thisDeviceId,
                deviceModel = thisDeviceModel,
                passkey = passkey,
                lectureTitle = lectureTitle,
                subjectName = subjectName,
                chapterName = chapterName,
                currentTime = currentTime,
                duration = duration,
                progressPercent = progressPercent,
                isCompleted = isCompleted,
                timestamp = now
            )
            fs.collection(FIRESTORE_COLLECTION_WATCH_LOGS).document(safeKey)
                .set(watchLog.toFirestoreMap(), SetOptions.merge())
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to write watch log: ${e.message}")
                }
        }
    }

    // Cloud Login Audit Logging
    fun recordLoginLog(passkey: String, label: String, status: String = "SUCCESS") {
        val thisDeviceId = getDeviceId()
        val thisDeviceModel = getDeviceModelName()
        val log = LoginLog(
            deviceId = thisDeviceId,
            deviceModel = thisDeviceModel,
            androidVersion = "Android ${Build.VERSION.RELEASE}",
            passkey = passkey,
            label = label,
            timestamp = System.currentTimeMillis(),
            status = status
        )
        try {
            firestore?.collection(FIRESTORE_COLLECTION_LOGIN_LOGS)?.document(log.id)
                ?.set(log.toFirestoreMap(), SetOptions.merge())
                ?.addOnFailureListener { e ->
                    Log.w(TAG, "Failed to record login log: ${e.message}")
                }
        } catch (e: Exception) {
            Log.w(TAG, "Login log exception: ${e.message}")
        }
    }

    // Cloud Push Notification Sender (Target All, Device ID, or Passkey)
    fun sendPushNotification(
        title: String,
        message: String,
        targetType: String = "ALL",
        targetValue: String = "ALL",
        author: String = "Admin Dhyan",
        isBurst: Boolean = false,
        burstCount: Int = 1,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        val item = PushNotificationItem(
            title = title.trim(),
            message = message.trim(),
            targetType = targetType,
            targetValue = targetValue.trim(),
            author = author,
            timestamp = System.currentTimeMillis(),
            isActive = true,
            isBurst = isBurst,
            burstCount = burstCount
        )
        val fs = firestore
        if (fs == null) {
            showToast("Cloud Firestore offline")
            onComplete?.invoke(false)
            return
        }
        fs.collection(FIRESTORE_COLLECTION_NOTIFICATIONS).document(item.id)
            .set(item.toFirestoreMap())
            .addOnSuccessListener {
                showToast("Notification sent to $targetType ($targetValue)!")
                onComplete?.invoke(true)
            }
            .addOnFailureListener { e ->
                showToast("Failed to send notification: ${e.message}")
                onComplete?.invoke(false)
            }
    }

    // Real-Time Push Notification Listener for Devices
    fun listenToPushNotifications(onNotification: (PushNotificationItem) -> Unit): ListenerRegistration? {
        val fs = firestore ?: return null
        val myDeviceId = getDeviceId()
        val myPasskey = prefs.getString(KEY_SESSION_CODE, "") ?: ""

        return try {
            fs.collection(FIRESTORE_COLLECTION_NOTIFICATIONS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val items = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { PushNotificationItem.fromFirestoreMap(it) }
                    }
                    items.filter { it.isActive }.forEach { item ->
                        val isForMe = when (item.targetType) {
                            "DEVICE" -> item.targetValue.equals(myDeviceId, ignoreCase = true)
                            "PASSKEY" -> item.targetValue.equals(myPasskey, ignoreCase = true)
                            else -> true // "ALL"
                        }
                        if (isForMe && !processedNotificationIds.contains(item.id)) {
                            // Check if notification is recent (created within last 24 hours)
                            if (System.currentTimeMillis() - item.timestamp < 24 * 60 * 60 * 1000L) {
                                processedNotificationIds.add(item.id)
                                onNotification(item)
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            null
        }
    }

    // Listen to real-time Watch Logs for Admin
    fun listenToWatchLogs(onLogsUpdated: (List<WatchLog>) -> Unit): ListenerRegistration? {
        val fs = firestore ?: return null
        return try {
            fs.collection(FIRESTORE_COLLECTION_WATCH_LOGS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val logs = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { WatchLog.fromFirestoreMap(it) }
                    }
                    onLogsUpdated(logs.sortedByDescending { it.timestamp })
                }
        } catch (e: Exception) {
            null
        }
    }

    // Listen to real-time Login Logs for Admin
    fun listenToLoginLogs(onLogsUpdated: (List<LoginLog>) -> Unit): ListenerRegistration? {
        val fs = firestore ?: return null
        return try {
            fs.collection(FIRESTORE_COLLECTION_LOGIN_LOGS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val logs = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { LoginLog.fromFirestoreMap(it) }
                    }
                    onLogsUpdated(logs.sortedByDescending { it.timestamp })
                }
        } catch (e: Exception) {
            null
        }
    }

    fun isUrlAllowed(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val lower = url.lowercase().trim()

        if (lower == "about:blank" || lower.startsWith("data:") || lower.startsWith("blob:")) {
            return true
        }

        // Main study portals (Server Sun: StudyParcham & Server Moon: PWThor Live Suite)
        if (lower.startsWith("https://pw.studyparcham.in") ||
            lower.startsWith("http://pw.studyparcham.in") ||
            lower.startsWith("pw.studyparcham.in") ||
            lower.contains("studyparcham.in") ||
            lower.contains("pwthor") ||
            lower.contains("pwthor.live") ||
            lower.contains("pwthor.site") ||
            lower.contains("allinoneregenuine.in")
        ) {
            return true
        }

        // Educational content CDN, notes, DPP, and video stream servers
        if (lower.contains("cloudfront.net") ||
            lower.contains("cloudflareinsights.com") ||
            lower.contains("pw.live") ||
            lower.contains("physicswallah") ||
            lower.contains("penpencil") ||
            lower.contains("amazonaws.com") ||
            lower.contains("drive.google.com") ||
            lower.contains("docs.google.com") ||
            lower.contains("googleusercontent.com") ||
            lower.contains("akamaized.net") ||
            lower.contains("fastly.net") ||
            lower.contains("jwplayer.com") ||
            lower.contains("cdnjs.cloudflare.com") ||
            lower.contains("cloudflare.com") ||
            lower.contains("mux.dev")
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
