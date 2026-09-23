package com.example.ui

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.script.ScriptManager
import com.example.security.SecurityManager
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GoldenAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    securityManager: SecurityManager,
    scriptManager: ScriptManager,
    isPermanentUnlocked: Boolean,
    onLockRequested: () -> Unit
) {
    val context = LocalContext.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf(SecurityManager.HOME_URL) }
    var pageTitle by remember { mutableStateOf("StudyParcham") }
    var loadingProgress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }
    var blockedUrlNotice by remember { mutableStateOf<String?>(null) }
    var scriptInjectionCount by remember { mutableIntStateOf(0) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var customVideoView by remember { mutableStateOf<View?>(null) }
    var swipeRefreshInstance by remember { mutableStateOf<SwipeRefreshLayout?>(null) }
    var showAdminDialog by remember { mutableStateOf(false) }
    var showAdminAuthDialog by remember { mutableStateOf(false) }
    var isSettingsPillVisible by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Auto-hide settings pill after 5 seconds of inactivity on main page
    LaunchedEffect(lastInteractionTime) {
        delay(5000)
        isSettingsPillVisible = false
    }

    fun revealSettingsPill() {
        isSettingsPillVisible = true
        lastInteractionTime = System.currentTimeMillis()
    }

    // System Back Handler
    // User request: "simple mobile back goes vid page to normal main page"
    BackHandler(enabled = true) {
        if (customVideoView != null) {
            // Exit fullscreen video
            customVideoView = null
        } else {
            val curUrl = webViewInstance?.url ?: ""
            if (webViewInstance?.canGoBack() == true) {
                webViewInstance?.goBack()
            } else if (!curUrl.contains("#home-view") && curUrl != SecurityManager.HOME_URL && curUrl.isNotEmpty()) {
                webViewInstance?.loadUrl(SecurityManager.HOME_URL)
            } else {
                (context as? android.app.Activity)?.moveTaskToBack(true)
            }
        }
    }

    // Auto-dismiss notices after 4 seconds
    fun showToast(msg: String) {
        toastMessage = msg
        CoroutineScope(Dispatchers.Main).launch {
            delay(3500)
            if (toastMessage == msg) toastMessage = null
        }
    }

    fun showBlockedNotice(url: String) {
        blockedUrlNotice = "External navigation blocked: Restricted to secure workspace"
        CoroutineScope(Dispatchers.Main).launch {
            delay(3500)
            blockedUrlNotice = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Main WebView Layer with Native Pull to Refresh
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .testTag("restricted_webview"),
            factory = { ctx ->
                val webView = WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    // Enable Hardware Acceleration
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)

                    // Advanced WebSettings
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = false
                        allowContentAccess = false
                        mediaPlaybackRequiresUserGesture = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        builtInZoomControls = true
                        displayZoomControls = false
                        cacheMode = WebSettings.LOAD_DEFAULT
                        userAgentString = "$userAgentString StudyBrowser/1.0"
                    }

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val url = request?.url?.toString() ?: return false
                            if (securityManager.isUrlAllowed(url)) {
                                return false // Allow navigation within pw.studyparcham.in
                            } else {
                                // Block external domain navigation!
                                showBlockedNotice(url)
                                return true // Cancel navigation
                            }
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            isLoading = true
                            currentUrl = url ?: SecurityManager.HOME_URL
                            // Inject early masterkey security pass-through
                            view?.let { scriptManager.injectPreloadSecurity(it) }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                            swipeRefreshInstance?.isRefreshing = false
                            currentUrl = url ?: SecurityManager.HOME_URL
                            pageTitle = view?.title ?: "StudyParcham"

                            // Inject both scripts automatically on load/reload
                            view?.let { wv ->
                                scriptManager.injectAll(wv) { success ->
                                    if (success) {
                                        scriptInjectionCount = scriptManager.injectionCount
                                    }
                                }
                            }
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            loadingProgress = newProgress / 100f
                            if (newProgress >= 100) {
                                swipeRefreshInstance?.isRefreshing = false
                            }
                            if (newProgress >= 50 && scriptInjectionCount == 0) {
                                view?.let { wv ->
                                    scriptManager.injectAll(wv) {
                                        scriptInjectionCount = scriptManager.injectionCount
                                    }
                                }
                            }
                        }

                        override fun onReceivedTitle(view: WebView?, title: String?) {
                            super.onReceivedTitle(view, title)
                            if (!title.isNullOrBlank()) {
                                pageTitle = title
                            }
                        }

                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                            super.onShowCustomView(view, callback)
                            // Switch to horizontal landscape mode for full widescreen video viewing
                            (context as? android.app.Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            customVideoView = view
                        }

                        override fun onHideCustomView() {
                            super.onHideCustomView()
                            // Return back to portrait mode when exiting fullscreen video
                            (context as? android.app.Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            customVideoView = null
                        }
                    }

                    // Initial Load
                    loadUrl(SecurityManager.HOME_URL)
                    webViewInstance = this
                }

                object : SwipeRefreshLayout(ctx) {
                    override fun canChildScrollUp(): Boolean {
                        // Only trigger pull to refresh when user is at the top of the webpage
                        return webView.scrollY > 0 || webView.canScrollVertically(-1)
                    }
                }.apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setColorSchemeColors(
                        android.graphics.Color.parseColor("#38BDF8"), // CyberCyan
                        android.graphics.Color.parseColor("#10B981")  // EmeraldSuccess
                    )
                    setProgressBackgroundColorSchemeColor(android.graphics.Color.parseColor("#0F172A")) // DarkSurface

                    addView(webView)

                    setOnRefreshListener {
                        webView.reload()
                        showToast("Refreshing page & scripts...")
                    }

                    swipeRefreshInstance = this
                }
            },
            update = { srl ->
                swipeRefreshInstance = srl
            }
        )

        // Custom HTML5 Fullscreen Video Overlay
        if (customVideoView != null) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                factory = {
                    FrameLayout(it).apply {
                        addView(customVideoView)
                    }
                }
            )
        }

        // Top Loading Progress Indicator
        if (isLoading && loadingProgress < 1.0f) {
            LinearProgressIndicator(
                progress = { loadingProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.TopCenter),
                color = CyberCyan,
                trackColor = Color.Transparent
            )
        }

        // Blocked Navigation Notice Banner
        AnimatedVisibility(
            visible = blockedUrlNotice != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonAlert),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = CrimsonAlert,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = blockedUrlNotice.orEmpty(),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Toast Message Pill
        AnimatedVisibility(
            visible = toastMessage != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DarkSurface.copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = toastMessage.orEmpty(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }

        // Floating Quick Admin & Security Controls (Auto-fades after 5 seconds, hidden in video player)
        if (customVideoView == null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 12.dp)
            ) {
                // Expanded Pill
                AnimatedVisibility(
                    visible = isSettingsPillVisible,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 })
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = DarkSurface.copy(alpha = 0.92f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.45f)),
                        modifier = Modifier.clickable { revealSettingsPill() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Remaining Access Time Chip
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF070B16),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GoldenAccent.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = securityManager.getCurrentAccessRemainingFormatted(),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = GoldenAccent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                            }

                            // Settings & Admin Management Button
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = GoldenAccent.copy(alpha = 0.18f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GoldenAccent.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .clickable {
                                        revealSettingsPill()
                                        showAdminAuthDialog = true
                                    }
                                    .testTag("browser_settings_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = GoldenAccent,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Settings",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = GoldenAccent,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }

                            // Lock Screen return button
                            IconButton(
                                onClick = onLockRequested,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Lock",
                                    tint = TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Discreet Mini Trigger (Visible when pill fades away)
                AnimatedVisibility(
                    visible = !isSettingsPillVisible,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        shape = CircleShape,
                        color = DarkSurface.copy(alpha = 0.70f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { revealSettingsPill() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Show Settings",
                                tint = GoldenAccent.copy(alpha = 0.85f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Admin Verification Dialog (Requires 240411)
        if (showAdminAuthDialog) {
            AdminVerificationDialog(
                securityManager = securityManager,
                onDismiss = { showAdminAuthDialog = false },
                onSuccess = {
                    showAdminAuthDialog = false
                    showAdminDialog = true
                }
            )
        }

        // Admin Key Dialog
        if (showAdminDialog) {
            AdminKeyDialog(
                securityManager = securityManager,
                onDismiss = { showAdminDialog = false },
                onLaunchBrowser = { showAdminDialog = false },
                onRevokeAdmin = {
                    showAdminDialog = false
                    onLockRequested()
                }
            )
        }

        DisposableEffect(Unit) {
            onDispose {
                (context as? android.app.Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                webViewInstance?.destroy()
                webViewInstance = null
                swipeRefreshInstance = null
            }
        }
    }
}
