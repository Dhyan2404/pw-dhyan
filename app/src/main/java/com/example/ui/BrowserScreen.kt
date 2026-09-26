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
import android.os.Message
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.security.PortalConfig
import com.example.security.PortalMaintenanceInfo
import com.example.security.SecurityManager
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceGlass
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

private fun isPlayerUrl(url: String?): Boolean {
    if (url.isNullOrBlank()) return false
    val lower = url.lowercase()
    return lower.contains("/player") ||
            lower.contains("player?") ||
            lower.contains("/watch") ||
            lower.contains("watch?")
}

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
    var currentPortal by remember { mutableStateOf(securityManager.getSelectedPortal()) }
    var currentUrl by remember { mutableStateOf(securityManager.getCurrentPortalUrl()) }
    var pageTitle by remember { mutableStateOf(currentPortal.displayName) }
    var loadingProgress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }
    var blockedUrlNotice by remember { mutableStateOf<String?>(null) }
    var scriptInjectionCount by remember { mutableIntStateOf(0) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var customVideoView by remember { mutableStateOf<View?>(null) }
    var swipeRefreshInstance by remember { mutableStateOf<SwipeRefreshLayout?>(null) }
    var showAdminDialog by remember { mutableStateOf(false) }
    var showAdminAuthDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showDownloadsDialog by remember { mutableStateOf(false) }
    var areControlsVisible by remember { mutableStateOf(true) }
    var isSettingsPillVisible by remember { mutableStateOf(false) }
    var isVideoPlaying by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var activeAnnouncement by remember { mutableStateOf<BroadcastAnnouncement?>(null) }
    var dismissedAnnouncementId by remember { mutableStateOf<String?>(null) }
    var maintenanceInfo by remember { mutableStateOf(AppMaintenanceInfo()) }
    var portalFailureOffer by remember { mutableStateOf(false) }
    var portalFailureMsg by remember { mutableStateOf("") }
    var portalConfig by remember { mutableStateOf(securityManager.getPortalConfig()) }
    var isDeviceOnline by remember { mutableStateOf(true) }

    // Live Network State Monitor for Offline Awareness
    DisposableEffect(context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        val callback = object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                isDeviceOnline = true
            }
            override fun onLost(network: android.net.Network) {
                isDeviceOnline = false
            }
        }
        val builder = android.net.NetworkRequest.Builder()
            .addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
        try {
            cm?.registerNetworkCallback(builder.build(), callback)
            val active = cm?.activeNetworkInfo
            isDeviceOnline = active != null && active.isConnected
        } catch (_: Exception) {}
        onDispose {
            try { cm?.unregisterNetworkCallback(callback) } catch (_: Exception) {}
        }
    }

    // Direct download helper saving files to user mobile's Downloads folder
    fun downloadFileToDownloads(url: String, suggestedName: String? = null) {
        try {
            var cleanFileName = suggestedName?.trim()
            if (cleanFileName.isNullOrBlank()) {
                cleanFileName = URLUtil.guessFileName(url, null, "application/pdf")
            }
            cleanFileName = cleanFileName.replace("[\\\\/:*?\"<>|]".toRegex(), "_")
            if (!cleanFileName.contains(".")) {
                cleanFileName += ".pdf"
            }

            val request = DownloadManager.Request(Uri.parse(url)).apply {
                val mimeType = if (cleanFileName.endsWith(".pdf", ignoreCase = true)) {
                    "application/pdf"
                } else {
                    URLUtil.guessFileName(url, null, null).let { fn ->
                        android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                            android.webkit.MimeTypeMap.getFileExtensionFromUrl(fn)
                        ) ?: "*/*"
                    }
                }
                setMimeType(mimeType)
                val cookies = android.webkit.CookieManager.getInstance().getCookie(url)
                if (cookies != null) {
                    addRequestHeader("Cookie", cookies)
                }
                webViewInstance?.settings?.userAgentString?.let {
                    addRequestHeader("User-Agent", it)
                }
                setTitle(cleanFileName)
                setDescription("Downloading to mobile Downloads folder...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, cleanFileName)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            dm?.enqueue(request)
            toastMessage = "Downloading: $cleanFileName\nSaving to Downloads folder"
        } catch (e: Exception) {
            android.util.Log.e("BrowserScreen", "Failed to enqueue download", e)
            try {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            } catch (_: Exception) {
                toastMessage = "Download failed: ${e.localizedMessage}"
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

    // Listen for live announcements, push notifications & maintenance mode from Admin
    DisposableEffect(securityManager) {
        val reg = securityManager.listenToAnnouncements { ann ->
            activeAnnouncement = ann
        }
        val mReg = securityManager.listenToMaintenance { info ->
            maintenanceInfo = info
        }
        val pReg = securityManager.listenToPushNotifications { notif ->
            showToast("🔔 ${notif.title}: ${notif.message}")
        }
        val portalListener: (SecurityManager.Portal, String) -> Unit = { newPortal, reason ->
            currentPortal = newPortal
            currentUrl = newPortal.url
            pageTitle = newPortal.displayName
            portalFailureOffer = false
            webViewInstance?.loadUrl(newPortal.url)
            showToast("Switched to ${newPortal.displayName}")
        }
        securityManager.addPortalChangeListener(portalListener)

        val cfgListener: (PortalConfig) -> Unit = { cfg ->
            portalConfig = cfg
        }
        securityManager.addPortalConfigListener(cfgListener)

        onDispose {
            reg?.remove()
            mReg?.remove()
            pReg?.remove()
            securityManager.removePortalChangeListener(portalListener)
            securityManager.removePortalConfigListener(cfgListener)
        }
    }

    // Real-time Session Expiry & Optimized Cloud Heartbeat watcher (Smooth & Battery-Efficient)
    LaunchedEffect(Unit) {
        var lastSentUrl: String? = null
        var lastSentTime = 0L
        while (true) {
            delay(5000)
            if (!securityManager.isSessionActive()) {
                onLockRequested()
            }
            val liveUrl = webViewInstance?.url
            val liveTitle = webViewInstance?.title
            val isPlayerActive = isPlayerUrl(liveUrl)
            val now = System.currentTimeMillis()
            // Send Firestore heartbeat only when URL changes or at least 60 seconds have elapsed
            if (liveUrl != lastSentUrl || (now - lastSentTime >= 60000L)) {
                lastSentUrl = liveUrl
                lastSentTime = now
                securityManager.sendHeartbeat(
                    currentUrl = liveUrl,
                    currentPageTitle = liveTitle,
                    currentLecture = if (!isPlayerActive) "" else null
                )
            }
        }
    }

    fun revealControls() {
        if (!isVideoPlaying) {
            areControlsVisible = true
            lastInteractionTime = System.currentTimeMillis()
        }
    }

    fun revealSettingsPill() {
        if (!isVideoPlaying) {
            areControlsVisible = true
            isSettingsPillVisible = true
            lastInteractionTime = System.currentTimeMillis()
        }
    }

    // Auto-hide floating navigation & settings after 3.5 seconds of inactivity
    LaunchedEffect(lastInteractionTime, areControlsVisible, isVideoPlaying) {
        if (areControlsVisible && !isVideoPlaying) {
            delay(3500)
            areControlsVisible = false
            isSettingsPillVisible = false
        }
    }

    // System Back Handler
    // Immersive Video Playback: stops the video, redirects to exact previous page, restores navigation UI
    BackHandler(enabled = true) {
        if (isVideoPlaying || customVideoView != null) {
            // 1. Stop video in WebView immediately
            webViewInstance?.evaluateJavascript(
                "(function() { var vids = document.querySelectorAll('video'); vids.forEach(function(v) { try { v.pause(); v.currentTime = 0; } catch(e){} }); })();",
                null
            )
            if (customVideoView != null) {
                customVideoView = null
            }
            isVideoPlaying = false
            areControlsVisible = true
            // 2. Redirect to exact previous page
            if (webViewInstance?.canGoBack() == true) {
                webViewInstance?.goBack()
            } else {
                webViewInstance?.loadUrl(securityManager.getCurrentPortalUrl())
            }
        } else {
            val curUrl = webViewInstance?.url ?: ""
            val homeUrl = securityManager.getCurrentPortalUrl()
            if (webViewInstance?.canGoBack() == true) {
                webViewInstance?.goBack()
            } else if (!curUrl.contains("#home-view") && !curUrl.contains("/study") && curUrl != homeUrl && curUrl.isNotEmpty()) {
                webViewInstance?.loadUrl(homeUrl)
            } else {
                (context as? android.app.Activity)?.moveTaskToBack(true)
            }
        }
    }

    fun handlePlayerOrientation(isFullscreenVideo: Boolean) {
        val activity = context as? Activity ?: return
        val window = activity.window ?: return
        val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        if (isFullscreenVideo) {
            // Fullscreen video: Landscape with hidden status bars
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            swipeRefreshInstance?.isEnabled = false
        } else {
            // Strictly locked to Portrait for normal browsing across both portals
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
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
            .then(
                if (customVideoView == null) {
                    Modifier
                        .statusBarsPadding()
                        .navigationBarsPadding()
                } else {
                    Modifier
                }
            )
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.any { it.pressed }) {
                            revealControls()
                        }
                    }
                }
            }
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

                    setOnTouchListener { _, event ->
                        if (event.action == MotionEvent.ACTION_DOWN) {
                            revealControls()
                        }
                        false
                    }

                    // Enable Hardware Acceleration & dark canvas to prevent white flash
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    setBackgroundColor(android.graphics.Color.BLACK)

                    // Enable full modern cookies for PWThor direct login & cross-domain batch data
                    try {
                        val cookieManager = android.webkit.CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)
                    } catch (_: Exception) {}

                    // Advanced WebSettings
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
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
                        setSupportMultipleWindows(true)
                        javaScriptCanOpenWindowsAutomatically = true
                    }

                    fun handlePopupNavigationOrDownload(url: String) {
                        val lower = url.lowercase()
                        if (lower.endsWith(".pdf") ||
                            lower.contains(".pdf?") ||
                            lower.contains("/pdf/") ||
                            lower.contains("static.pw.live") ||
                            lower.contains("attachment") ||
                            lower.contains("download") ||
                            lower.endsWith(".zip") ||
                            lower.endsWith(".docx")
                        ) {
                            downloadFileToDownloads(url)
                        } else if (securityManager.isUrlAllowed(url)) {
                            loadUrl(url)
                        } else {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
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
                                lowerUrl.endsWith(".doc") ||
                                lowerUrl.endsWith(".xlsx") ||
                                lowerUrl.endsWith(".pptx") ||
                                (lowerUrl.contains("static.pw.live") && (lowerUrl.contains("attachment") || lowerUrl.contains(".pdf") || lowerUrl.contains("/notes"))) ||
                                lowerUrl.contains("content-disposition=attachment")
                            ) {
                                downloadFileToDownloads(url)
                                return true // Screen never blocks!
                            }

                            if (securityManager.isUrlAllowed(url)) {
                                return false // Allow navigation within allowed portals
                            } else {
                                // Block external domain navigation!
                                showBlockedNotice(url)
                                return true // Cancel navigation
                            }
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            isLoading = true
                            currentUrl = url ?: securityManager.getCurrentPortalUrl()
                            if (!isPlayerUrl(currentUrl)) {
                                isVideoPlaying = false
                                areControlsVisible = true
                                isSettingsPillVisible = true
                                lastInteractionTime = System.currentTimeMillis()
                            }
                            // Inject early security pass-through (strictly isolated per portal)
                            view?.let { scriptManager.injectPreloadSecurity(it, currentUrl) }
                        }

                        override fun onPageCommitVisible(view: WebView?, url: String?) {
                            super.onPageCommitVisible(view, url)
                            view?.let { wv ->
                                scriptManager.injectForUrl(wv, wv.url ?: currentUrl)
                            }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                            swipeRefreshInstance?.isRefreshing = false
                            currentUrl = url ?: securityManager.getCurrentPortalUrl()
                            pageTitle = view?.title ?: currentPortal.displayName
                            if (!isPlayerUrl(currentUrl)) {
                                isVideoPlaying = false
                                areControlsVisible = true
                                isSettingsPillVisible = true
                                lastInteractionTime = System.currentTimeMillis()
                            }
                            if (currentPortal == SecurityManager.Portal.PWTHOR) {
                                portalFailureOffer = false
                            }
                            securityManager.sendHeartbeat(
                                currentUrl = currentUrl,
                                currentPageTitle = pageTitle
                            )
                            try {
                                android.webkit.CookieManager.getInstance().flush()
                            } catch (_: Exception) {}

                            // Inject scripts automatically on load/reload (strictly isolated per portal)
                            view?.let { wv ->
                                scriptManager.injectForUrl(wv, wv.url ?: currentUrl) { success ->
                                    if (success) {
                                        scriptInjectionCount = scriptManager.injectionCount
                                    }
                                }
                            }
                        }

                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                            super.onReceivedError(view, request, error)
                            if (request?.isForMainFrame == true) {
                                val reqUrl = request.url?.toString() ?: ""
                                if (currentPortal == SecurityManager.Portal.STUDYPARCHAM || reqUrl.contains("studyparcham")) {
                                    portalFailureOffer = true
                                    portalFailureMsg = "Server Sun ☀️ is not responding or unreachable."
                                }
                            }
                        }

                        override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                            super.onReceivedHttpError(view, request, errorResponse)
                            if (request?.isForMainFrame == true) {
                                val code = errorResponse?.statusCode ?: 200
                                if (code >= 500 && currentPortal == SecurityManager.Portal.STUDYPARCHAM) {
                                    portalFailureOffer = true
                                    portalFailureMsg = "Server Sun ☀️ returned server error $code."
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
                                    scriptManager.injectForUrl(wv, wv.url ?: currentUrl) {
                                        scriptInjectionCount = scriptManager.injectionCount
                                    }
                                }
                            }
                        }

                        override fun onCreateWindow(
                            view: WebView?,
                            isDialog: Boolean,
                            isUserGesture: Boolean,
                            resultMsg: Message?
                        ): Boolean {
                            val tempWebView = WebView(view!!.context)
                            tempWebView.webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val popupUrl = request?.url?.toString() ?: return false
                                    handlePopupNavigationOrDownload(popupUrl)
                                    return true
                                }
                                @Deprecated("Deprecated in Java")
                                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                    val popupUrl = url ?: return false
                                    handlePopupNavigationOrDownload(popupUrl)
                                    return true
                                }
                            }
                            val transport = resultMsg?.obj as? WebView.WebViewTransport
                            transport?.webView = tempWebView
                            resultMsg?.sendToTarget()
                            return true
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
                            isVideoPlaying = true
                            areControlsVisible = false
                            isSettingsPillVisible = false
                        }

                        override fun onHideCustomView() {
                            super.onHideCustomView()
                            handlePlayerOrientation(false)
                            customVideoView = null
                            isVideoPlaying = false
                            areControlsVisible = true
                        }

                        override fun onPermissionRequest(request: android.webkit.PermissionRequest?) {
                            try {
                                request?.grant(request.resources)
                            } catch (e: Exception) {
                                super.onPermissionRequest(request)
                            }
                        }

                        override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                            consoleMessage?.let {
                                android.util.Log.d("WebConsole", "[${it.messageLevel()}] ${it.message()} -- line ${it.lineNumber()} of ${it.sourceId()}")
                            }
                            return true
                        }
                    }

                    // Set DownloadListener for PDFs, notes, assignments directly to phone Downloads folder
                    setDownloadListener { downloadUrl, _, contentDisposition, mimetype, _ ->
                        val fileName = URLUtil.guessFileName(downloadUrl, contentDisposition, mimetype)
                        downloadFileToDownloads(downloadUrl, fileName)
                    }

                    // Bridge to detect SPA player route changes, manual rotation and direct file downloads
                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun downloadFile(url: String?, fileName: String?) {
                            (context as? Activity)?.runOnUiThread {
                                if (!url.isNullOrBlank()) {
                                    downloadFileToDownloads(url, fileName)
                                }
                            }
                        }

                        @JavascriptInterface
                        fun onVideoPlayStateChanged(isPlaying: Boolean) {
                            (context as? Activity)?.runOnUiThread {
                                isVideoPlaying = isPlaying
                                if (isPlaying) {
                                    areControlsVisible = false
                                    isSettingsPillVisible = false
                                }
                            }
                        }

                        @JavascriptInterface
                        fun onLecturePlayerDetected(isPlayer: Boolean) {
                            (context as? Activity)?.runOnUiThread {
                                isVideoPlaying = isPlayer
                                if (isPlayer) {
                                    areControlsVisible = false
                                    isSettingsPillVisible = false
                                }
                            }
                        }

                        @JavascriptInterface
                        fun toggleOrientation() {
                            (context as? Activity)?.runOnUiThread {
                                toggleOrientation()
                            }
                        }

                        @JavascriptInterface
                        fun logWatchProgress(
                            lectureTitle: String?,
                            subjectName: String?,
                            chapterName: String?,
                            currentTime: Double,
                            duration: Double,
                            progressPercent: Double
                        ) {
                            try {
                                securityManager.recordWatchProgress(
                                    lectureTitle = lectureTitle ?: "Lecture",
                                    subjectName = subjectName ?: "",
                                    chapterName = chapterName ?: "",
                                    currentTime = currentTime.toLong(),
                                    duration = duration.toLong(),
                                    progressPercent = progressPercent.toInt()
                                )
                            } catch (_: Exception) {}
                        }

                        @JavascriptInterface
                        fun updateCurrentActivity(url: String?, title: String?, lecture: String?) {
                            (context as? Activity)?.runOnUiThread {
                                securityManager.sendHeartbeat(
                                    currentUrl = url,
                                    currentPageTitle = title,
                                    currentLecture = if (lecture.isNullOrBlank()) "" else lecture
                                )
                            }
                        }
                    }, "AndroidPlayerBridge")

                    setOnTouchListener { _, event ->
                        if (event.action == MotionEvent.ACTION_DOWN) {
                            revealControls()
                        }
                        false
                    }

                    // Initial Load
                    loadUrl(securityManager.getCurrentPortalUrl())
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

                    setOnTouchListener { _, event ->
                        if (event.action == MotionEvent.ACTION_DOWN) {
                            revealControls()
                        }
                        false
                    }

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

        // Real-Time Offline Mode Detection Banner
        AnimatedVisibility(
            visible = !isDeviceOnline && customVideoView == null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = if (activeAnnouncement != null && activeAnnouncement?.id != dismissedAnnouncementId) 70.dp else 14.dp, start = 12.dp, end = 12.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceGlass),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonAlert.copy(alpha = 0.6f)),
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
                            .background(CrimsonAlert.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Offline Mode",
                            tint = CrimsonAlert,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "OFFLINE MODE ACTIVE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = CrimsonAlert,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp
                            )
                        )
                        Text(
                            text = "No internet connection. Push notifications & cloud sync will auto-deliver when back online.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextPrimary,
                                fontSize = 11.sp
                            )
                        )
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

        // Floating Quick Admin & Security Controls (Auto-fades after 3.5 seconds of inactivity)
        AnimatedVisibility(
            visible = areControlsVisible && !isVideoPlaying && customVideoView == null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 }),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 10.dp, end = 12.dp)
        ) {
            Box {
                // Expanded Pill
                AnimatedVisibility(
                    visible = isSettingsPillVisible,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = DarkSurface.copy(alpha = 0.94f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.45f)),
                        modifier = Modifier.clickable { revealControls() }
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

                            // Settings & Portal/Storage Configuration Button
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = GoldenAccent.copy(alpha = 0.18f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GoldenAccent.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .clickable {
                                        revealControls()
                                        showSettingsDialog = true
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

                            // Quick Downloads Folder Button
                            IconButton(
                                onClick = {
                                    revealControls()
                                    showDownloadsDialog = true
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = "My Downloads",
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            // Direct APK Download / Share button
                            IconButton(
                                onClick = {
                                    revealControls()
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
                                    revealControls()
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
                                onClick = {
                                    revealControls()
                                    onLockRequested()
                                },
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

                // Discreet Mini Trigger (Visible when pill is collapsed)
                AnimatedVisibility(
                    visible = !isSettingsPillVisible,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        shape = CircleShape,
                        color = DarkSurface.copy(alpha = 0.80f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { revealSettingsPill() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Show Settings",
                                tint = GoldenAccent.copy(alpha = 0.90f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Portal 1 Unreachable / Connection Fallback Banner
        AnimatedVisibility(
            visible = portalFailureOffer && currentPortal == SecurityManager.Portal.STUDYPARCHAM,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 56.dp, start = 12.dp, end = 12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = DarkSurface.copy(alpha = 0.95f),
                border = BorderStroke(1.5.dp, GoldenAccent),
                shadowElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = GoldenAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Server Sun ☀️ is not responding",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { portalFailureOffer = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = if (portalFailureMsg.isNotBlank()) portalFailureMsg else "Server Sun ☀️ is currently unreachable. You can switch to Server Moon 🌙 from settings or tap below to continue your studies without interruption.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                securityManager.setSelectedPortal(SecurityManager.Portal.PWTHOR)
                                currentPortal = SecurityManager.Portal.PWTHOR
                                currentUrl = SecurityManager.Portal.PWTHOR.url
                                portalFailureOffer = false
                                webViewInstance?.loadUrl(SecurityManager.Portal.PWTHOR.url)
                                showToast("Switched to Server Moon 🌙")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldenAccent),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Switch to Server Moon 🌙", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                portalFailureOffer = false
                                showSettingsDialog = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, GlassBorder)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Settings", color = CyberCyan, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Individual Portal Maintenance Mode - Full Screen Blocking Overlay
        if (portalConfig.isMaintenance(currentPortal) && !isPermanentUnlocked && !maintenanceInfo.isActive) {
            val altPortal = if (currentPortal == SecurityManager.Portal.STUDYPARCHAM) SecurityManager.Portal.PWTHOR else SecurityManager.Portal.STUDYPARCHAM
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground.copy(alpha = 0.98f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(GoldenAccent.copy(alpha = 0.2f))
                            .border(1.5.dp, GoldenAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Maintenance",
                            tint = GoldenAccent,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "${currentPortal.displayName.uppercase()} MAINTENANCE",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            color = TextPrimary
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = portalConfig.getMaintenanceMessage(currentPortal),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextMuted,
                            lineHeight = 22.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (!portalConfig.isMaintenance(altPortal) && !portalConfig.isBlocked(altPortal)) {
                        Button(
                            onClick = {
                                securityManager.setSelectedPortal(altPortal)
                                currentPortal = altPortal
                                currentUrl = altPortal.url
                                webViewInstance?.loadUrl(altPortal.url)
                                showToast("Switched to ${altPortal.displayName}")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text("Switch to ${altPortal.displayName}", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }

        // Floating Mini Navigation Bar (Auto-fades out after 3.5 seconds of inactivity)
        AnimatedVisibility(
            visible = areControlsVisible && !isVideoPlaying && customVideoView == null && !isPlayerUrl(currentUrl),
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = Color(0xFF070B16).copy(alpha = 0.94f),
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
                            revealControls()
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
                            revealControls()
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
                            revealControls()
                            webViewInstance?.loadUrl(securityManager.getCurrentPortalUrl())
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
                            revealControls()
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
                            text = "Admin Bypass",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = Color(0xFF030712),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }

        // App Settings Dialog (Portal Server Switcher, Downloads, Share, Admin)
        if (showSettingsDialog) {
            AppSettingsDialog(
                securityManager = securityManager,
                currentPortal = currentPortal,
                portalConfig = portalConfig,
                onPortalSelected = { selected ->
                    securityManager.setSelectedPortal(selected)
                    currentPortal = selected
                    webViewInstance?.loadUrl(selected.url)
                    currentUrl = selected.url
                    toastMessage = "Switched to ${selected.displayName}"
                    showSettingsDialog = false
                },
                onOpenDownloads = {
                    showSettingsDialog = false
                    showDownloadsDialog = true
                },
                onOpenAdmin = {
                    showSettingsDialog = false
                    showAdminAuthDialog = true
                },
                onDismiss = { showSettingsDialog = false }
            )
        }

        // Downloaded Files Dialog (Access stored PDFs, notes, and study files)
        if (showDownloadsDialog) {
            DownloadedFilesDialog(
                onDismiss = { showDownloadsDialog = false }
            )
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
