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
import com.example.script.ScriptManager
import com.example.security.SecurityManager
import com.example.ui.BrowserScreen
import com.example.ui.LockScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private lateinit var securityManager: SecurityManager
    private lateinit var scriptManager: ScriptManager
    private lateinit var notificationHelper: NotificationHelper

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // Permission result handled
        }

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

        // Start 5-minute background reminder notifications for normal users
        notificationHelper.startPeriodicNotification(lifecycleScope, securityManager)

        val initialUnlocked = securityManager.isSessionActive()
        val alreadyPermanentlyUnlocked = securityManager.isPermanentUnlocked()

        android.widget.Toast.makeText(this, "made by dhyan ❤️", android.widget.Toast.LENGTH_LONG).show()

        setContent {
            MyApplicationTheme {
                var isUnlocked by remember { mutableStateOf(initialUnlocked) }
                var isPermanentUnlocked by remember { mutableStateOf(alreadyPermanentlyUnlocked) }

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                    color = DarkBackground
                ) {
                    if (isUnlocked) {
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

    override fun onDestroy() {
        super.onDestroy()
        notificationHelper.stopPeriodicNotification()
    }
}
