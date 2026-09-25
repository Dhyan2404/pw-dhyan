package com.example.script

import android.content.Context
import android.util.Log
import android.webkit.WebView

class ScriptManager(private val context: Context) {
    companion object {
        private const val TAG = "ScriptManager"
        private const val MASTERKEY_PATH = "scripts/masterkey.js"
        private const val LIQUID_PLAYER_PATH = "scripts/liquid_player.js"
        private const val PWTHOR_PORTAL_PATH = "scripts-2/portel.js"
        private const val PWTHOR_PORTAL_FALLBACK = "scripts/portel.js"
    }

    private var masterkeyCache: String? = null
    private var liquidPlayerCache: String? = null
    private var pwthorPortalCache: String? = null

    var lastInjectionTime: Long = 0
        private set
    var injectionCount: Int = 0
        private set

    /**
     * Loads the Admin Masterkey script from assets (for StudyParcham).
     */
    fun getMasterkeyScript(): String {
        return masterkeyCache ?: run {
            try {
                context.assets.open(MASTERKEY_PATH).bufferedReader().use { it.readText() }.also {
                    masterkeyCache = it
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read $MASTERKEY_PATH", e)
                ""
            }
        }
    }

    /**
     * Loads the Liquid Glass Player script from assets (for StudyParcham).
     */
    fun getLiquidPlayerScript(): String {
        return liquidPlayerCache ?: run {
            try {
                context.assets.open(LIQUID_PLAYER_PATH).bufferedReader().use { it.readText() }.also {
                    liquidPlayerCache = it
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read $LIQUID_PLAYER_PATH", e)
                ""
            }
        }
    }

    /**
     * Loads the PWThor Live portal script from assets.
     */
    fun getPWThorPortalScript(): String {
        return pwthorPortalCache ?: run {
            try {
                val stream = try {
                    context.assets.open(PWTHOR_PORTAL_PATH)
                } catch (_: Exception) {
                    context.assets.open(PWTHOR_PORTAL_FALLBACK)
                }
                stream.bufferedReader().use { it.readText() }.also {
                    pwthorPortalCache = it
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read PWThor portal script", e)
                ""
            }
        }
    }

    /**
     * Checks if current URL is a video player route.
     * Matches /player on StudyParcham and /watch on PWThor.
     * Non-player pages (home page, batch listings, subjects) return false.
     */
    fun isPlayerRoute(url: String?): Boolean {
        val u = url?.lowercase() ?: return false
        return u.contains("/player") ||
                u.contains("player?") ||
                u.contains("/watch") ||
                u.contains("watch?")
    }

    /**
     * Injects the appropriate script based on the target URL / domain.
     * - masterkey.js: Injected on StudyParcham pages for batch access.
     * - portel.js: Injected on PWThor pages.
     * - liquid_player.js: STRICTLY injected ONLY on player routes (/player on StudyParcham, /watch on PWThor).
     *   NEVER injected on home page or batch browsing pages!
     */
    fun injectForUrl(webView: WebView, url: String?, onResult: ((Boolean) -> Unit)? = null) {
        val currentUrl = (url ?: webView.url)?.lowercase() ?: ""
        val isPlayer = isPlayerRoute(currentUrl)

        if (currentUrl.contains("pwthor")) {
            // PWThor Portal: Strictly portel.js, plus liquid_player only on /watch
            val pwthorPortal = getPWThorPortalScript()

            webView.post {
                if (pwthorPortal.isNotBlank()) {
                    webView.evaluateJavascript(
                        """
                        (function() {
                            try {
                                $pwthorPortal
                            } catch(e) {
                                console.error('[PWThor Portal Injection Error]:', e);
                            }
                        })();
                        """.trimIndent(),
                        null
                    )
                }

                if (isPlayer) {
                    val liquidPlayer = getLiquidPlayerScript()
                    if (liquidPlayer.isNotBlank()) {
                        webView.evaluateJavascript(
                            """
                            (function() {
                                try {
                                    $liquidPlayer
                                } catch(e) {
                                    console.error('[PWThor LiquidPlayer Injection Error]:', e);
                                }
                            })();
                            """.trimIndent(),
                            null
                        )
                    }
                }

                lastInjectionTime = System.currentTimeMillis()
                injectionCount++
                Log.d(TAG, "PWThor scripts injected (isPlayer: $isPlayer, #$injectionCount)")
                onResult?.invoke(true)
            }
            return
        }

        // StudyParcham Portal (Default):
        val masterkey = getMasterkeyScript()

        webView.post {
            if (masterkey.isNotBlank()) {
                webView.evaluateJavascript(
                    """
                    (function() {
                        try {
                            $masterkey
                        } catch(e) {
                            console.error('[Masterkey Injection Error]:', e);
                        }
                    })();
                    """.trimIndent(),
                    null
                )
            }

            // LiquidPlayer ONLY injected on player pages (e.g. /player?...), NEVER on home page!
            if (isPlayer) {
                val liquidPlayer = getLiquidPlayerScript()
                if (liquidPlayer.isNotBlank()) {
                    webView.evaluateJavascript(
                        """
                        (function() {
                            try {
                                $liquidPlayer
                            } catch(e) {
                                console.error('[LiquidPlayer Injection Error]:', e);
                            }
                        })();
                        """.trimIndent()
                    ) { result ->
                        lastInjectionTime = System.currentTimeMillis()
                        injectionCount++
                        Log.d(TAG, "LiquidPlayer injected on player route (#$injectionCount), result: $result")
                        onResult?.invoke(true)
                    }
                } else {
                    onResult?.invoke(true)
                }
            } else {
                lastInjectionTime = System.currentTimeMillis()
                injectionCount++
                onResult?.invoke(true)
            }
        }
    }

    /**
     * Backward-compatible injectAll delegating to injectForUrl using the WebView's active URL.
     */
    fun injectAll(webView: WebView, onResult: ((Boolean) -> Unit)? = null) {
        injectForUrl(webView, webView.url, onResult)
    }

    /**
     * Injects early masterkey script for onPageStarted to ensure native window.open
     * and auth endpoints are intercepted before the DOM starts rendering.
     * Note: Skipped for pwthor.live so main script does not interfere with PWThor.
     */
    fun injectPreloadSecurity(webView: WebView, url: String? = null) {
        val currentUrl = (url ?: webView.url)?.lowercase() ?: ""
        if (currentUrl.contains("pwthor")) {
            // For PWThor: Do NOT inject StudyParcham masterkey! Inject PWThor portal script early
            val pwthorPortal = getPWThorPortalScript()
            if (pwthorPortal.isNotBlank()) {
                webView.post {
                    webView.evaluateJavascript(
                        """
                        (function() {
                            try {
                                $pwthorPortal
                            } catch(e) {
                                console.warn('[PWThor Preload Warning]:', e);
                            }
                        })();
                        """.trimIndent(),
                        null
                    )
                }
            }
            return
        }

        val masterkey = getMasterkeyScript()
        if (masterkey.isBlank()) return

        webView.post {
            webView.evaluateJavascript(
                """
                (function() {
                    try {
                        $masterkey
                    } catch(e) {
                        console.warn('[Preload Security Warning]:', e);
                    }
                })();
                """.trimIndent(),
                null
            )
        }
    }
}
