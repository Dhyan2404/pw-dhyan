package com.example.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.URLUtil
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
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.script.ScriptManager
import com.example.security.AppMaintenanceInfo
import com.example.security.BroadcastAnnouncement
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
    var activeAnnouncement by remember { mutableStateOf<BroadcastAnnouncement?>(null) }
    var dismissedAnnouncementId by remember { mutableStateOf<String?>(null) }
    var maintenanceInfo by remember { mutableStateOf(AppMaintenanceInfo()) }

    // Listen for live announcements & maintenance mode from Admin
    DisposableEffect(securityManager) {
        val reg = securityManager.listenToAnnouncements { ann ->
            activeAnnouncement = ann
        }
        val mReg = securityManager.listenToMaintenance { info ->
            maintenanceInfo = info
        }
        onDispose {
            reg?.remove()
            mReg?.remove()
        }
    }

    // Real-time Session Expiry & Admin Revocation watcher
    LaunchedEffect(Unit) {
        while (true) {
            delay(8000)
            if (!securityManager.isSessionActive()) {
                onLockRequested()
            }
        }
    }

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

    fun isPlayerUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val lower = url.lowercase()
        return lower.contains("/player") ||
                lower.contains("player?") ||
                lower.contains("videoid=") ||
                lower.contains("vurl=") ||
                lower.contains("lectureid=")
    }

    fun handlePlayerOrientation(isPlayer: Boolean) {
        val activity = context as? Activity ?: return
        val window = activity.window ?: return
        val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        if (isPlayer) {
            // Force horizontal landscape initially for widescreen lecture viewing
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            swipeRefreshInstance?.isEnabled = false
            // Release lock to FULL_SENSOR after 1.8s so user can auto-rotate to vertical whenever they want
            Handler(Looper.getMainLooper()).postDelayed({
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
            }, 1800)
        } else {
            // Non-lecture screens return to normal portrait / sensor mode
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            swipeRefreshInstance?.isEnabled = true
        }
    }

    fun toggleOrientation() {
        val activity = context as? Activity ?: return
        val current = activity.resources.configuration.orientation
        if (current == Configuration.ORIENTATION_LANDSCAPE) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        Handler(Looper.getMainLooper()).postDelayed({
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        }, 1800)
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

                    // Enable Hardware Acceleration & dark canvas to prevent white flash
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    setBackgroundColor(android.graphics.Color.BLACK)

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

                            // Handle direct PDF, notes, DPP or study document downloads
                            val lowerUrl = url.lowercase()
                            if (lowerUrl.endsWith(".pdf") ||
                                lowerUrl.contains(".pdf?") ||
                                lowerUrl.contains("/pdf/") ||
                                lowerUrl.endsWith(".zip") ||
                                lowerUrl.endsWith(".docx") ||
                                lowerUrl.contains("content-disposition=attachment")
                            ) {
                                try {
                                    val fileName = URLUtil.guessFileName(url, null, "application/pdf")
                                    val req = DownloadManager.Request(Uri.parse(url)).apply {
                                        setMimeType("application/pdf")
                                        val cookies = android.webkit.CookieManager.getInstance().getCookie(url)
                                        if (cookies != null) {
                                            addRequestHeader("Cookie", cookies)
                                        }
                                        addRequestHeader("User-Agent", settings.userAgentString)
                                        setTitle(fileName)
                                        setDescription("Downloading PW Notes / DPP...")
                                        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                        setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                                        setAllowedOverMetered(true)
                                        setAllowedOverRoaming(true)
                                    }
                                    (context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager)?.enqueue(req)
                                    toastMessage = "Downloading PDF in background: $fileName"
                                    return true // Screen never blocks!
                                } catch (e: Exception) {
                                    try {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                        return true
                                    } catch (_: Exception) {}
                                }
                            }

                            if (securityManager.isUrlAllowed(url)) {
                                handlePlayerOrientation(isPlayerUrl(url))
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
                            handlePlayerOrientation(isPlayerUrl(currentUrl))
                            // Inject early masterkey security pass-through
                            view?.let { scriptManager.injectPreloadSecurity(it) }
                        }

                        override fun onPageCommitVisible(view: WebView?, url: String?) {
                            super.onPageCommitVisible(view, url)
                            view?.let { wv ->
                                scriptManager.injectAll(wv)
                            }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                            swipeRefreshInstance?.isRefreshing = false
                            currentUrl = url ?: SecurityManager.HOME_URL
                            pageTitle = view?.title ?: "StudyParcham"
                            handlePlayerOrientation(isPlayerUrl(currentUrl))
                            try {
                                android.webkit.CookieManager.getInstance().flush()
                            } catch (_: Exception) {}

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
                            handlePlayerOrientation(true)
                            customVideoView = view
                        }

                        override fun onHideCustomView() {
                            super.onHideCustomView()
                            handlePlayerOrientation(false)
                            customVideoView = null
                        }

                        override fun onPermissionRequest(request: android.webkit.PermissionRequest?) {
                            try {
                                request?.grant(request.resources)
                            } catch (e: Exception) {
                                super.onPermissionRequest(request)
                            }
                        }
                    }

                    // Set DownloadListener for PDFs, notes, assignments
                    setDownloadListener { downloadUrl, userAgent, contentDisposition, mimetype, _ ->
                        try {
                            val fileName = URLUtil.guessFileName(downloadUrl, contentDisposition, mimetype)
                            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                                setMimeType(mimetype)
                                addRequestHeader("User-Agent", userAgent)
                                val cookies = android.webkit.CookieManager.getInstance().getCookie(downloadUrl)
                                if (cookies != null) {
                                    addRequestHeader("Cookie", cookies)
                                }
                                setTitle(fileName)
                                setDescription("Downloading study notes / PDF...")
                                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                                setAllowedOverMetered(true)
                                setAllowedOverRoaming(true)
                            }
                            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                            dm?.enqueue(request)
                            toastMessage = "Downloading: $fileName • Check notifications"
                        } catch (e: Exception) {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                toastMessage = "Download failed: ${e.localizedMessage}"
                            }
                        }
                    }

                    // Bridge to detect SPA player route changes and manual rotation
                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun onLecturePlayerDetected(isPlayer: Boolean) {
                            (context as? Activity)?.runOnUiThread {
                                handlePlayerOrientation(isPlayer)
                            }
                        }

                        @JavascriptInterface
                        fun toggleOrientation() {
                            (context as? Activity)?.runOnUiThread {
                                toggleOrientation()
                            }
                        }
                    }, "AndroidPlayerBridge")

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

        // Live Broadcast Announcement Banner (from Admin)
        AnimatedVisibility(
            visible = activeAnnouncement != null && activeAnnouncement?.id != dismissedAnnouncementId && customVideoView == null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 14.dp, start = 12.dp, end = 12.dp)
        ) {
            val announcement = activeAnnouncement
            if (announcement != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.96f)),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldenAccent.copy(alpha = 0.75f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(GoldenAccent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = "Announcement",
                                tint = GoldenAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "ANNOUNCEMENT • ${announcement.author}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = GoldenAccent,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = announcement.getFormattedTime(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextMuted,
                                        fontSize = 9.sp
                                    )
                                )
                            }
                            Text(
                                text = announcement.message,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            )
                        }

                        IconButton(
                            onClick = { dismissedAnnouncementId = announcement.id },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
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

                            // Direct APK Download / Share button
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("PW Dhyan Direct APK Link", SecurityManager.DIRECT_APK_DOWNLOAD_URL)
                                    clipboard.setPrimaryClip(clip)
                                    showToast("Direct APK Link copied! Share without ZIP.")
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Direct APK Link",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            // Manual Rotate Toggle (Horizontal <-> Vertical)
                            IconButton(
                                onClick = {
                                    toggleOrientation()
                                    showToast("Screen rotation toggled")
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Rotate Screen",
                                    tint = GoldenAccent,
                                    modifier = Modifier.size(14.dp)
                                )
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

        // Floating Mini Navigation Bar (Discreet frosted glass pill for notes, assignments, DPPs & batches)
        if (customVideoView == null && !isPlayerUrl(currentUrl)) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xFF070B16).copy(alpha = 0.92f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.35f)),
                    shadowElevation = 10.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (webViewInstance?.canGoBack() == true) {
                                    webViewInstance?.goBack()
                                }
                            },
                            enabled = webViewInstance?.canGoBack() == true,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = if (webViewInstance?.canGoBack() == true) CyberCyan else TextMuted.copy(alpha = 0.4f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                if (webViewInstance?.canGoForward() == true) {
                                    webViewInstance?.goForward()
                                }
                            },
                            enabled = webViewInstance?.canGoForward() == true,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Forward",
                                tint = if (webViewInstance?.canGoForward() == true) CyberCyan else TextMuted.copy(alpha = 0.4f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                webViewInstance?.loadUrl(SecurityManager.HOME_URL)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Home",
                                tint = GoldenAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                webViewInstance?.reload()
                                showToast("Reloading portal...")
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reload",
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Emergency Maintenance Mode Lockdown Overlay
        if (maintenanceInfo.isActive && !isPermanentUnlocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground.copy(alpha = 0.98f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(CrimsonAlert.copy(alpha = 0.2f))
                            .border(1.5.dp, CrimsonAlert, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Maintenance",
                            tint = CrimsonAlert,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "SYSTEM MAINTENANCE",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            color = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = maintenanceInfo.message,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextMuted,
                            lineHeight = 20.sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { showAdminAuthDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldenAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text(
                            text = "Admin Bypass (240411)",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = Color(0xFF030712),
                                fontWeight = FontWeight.Bold
                            )
                        )
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
