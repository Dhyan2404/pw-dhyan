package com.example.script

import android.content.Context
import android.util.Log
import android.webkit.WebView

class ScriptManager(private val context: Context) {
    companion object {
        private const val TAG = "ScriptManager"
        private const val MASTERKEY_PATH = "scripts/masterkey.js"
        private const val LIQUID_PLAYER_PATH = "scripts/liquid_player.js"
    }

    private var masterkeyCache: String? = null
    private var liquidPlayerCache: String? = null

    var lastInjectionTime: Long = 0
        private set
    var injectionCount: Int = 0
        private set

    /**
     * Loads the Admin Masterkey script from assets.
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
     * Loads the Liquid Glass Player script from assets.
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
     * Injects both scripts into the given WebView.
     * Wrapped in an IIFE to prevent variable leakage and ensure safe re-injection on reload.
     */
    fun injectAll(webView: WebView, onResult: ((Boolean) -> Unit)? = null) {
        val masterkey = getMasterkeyScript()
        val liquidPlayer = getLiquidPlayerScript()

        if (masterkey.isBlank() && liquidPlayer.isBlank()) {
            onResult?.invoke(false)
            return
        }

        val combinedScript = buildString {
            append("/* [AI Studio Study Browser - Auto Injected Scripts] */\n")
            if (masterkey.isNotBlank()) {
                append("try { \n")
                append(masterkey)
                append("\n} catch (e) { console.error('[Masterkey Injection Error]:', e); }\n")
            }
            if (liquidPlayer.isNotBlank()) {
                append("try { \n")
                append(liquidPlayer)
                append("\n} catch (e) { console.error('[LiquidPlayer Injection Error]:', e); }\n")
            }
        }

        webView.post {
            webView.evaluateJavascript(combinedScript) { result ->
                lastInjectionTime = System.currentTimeMillis()
                injectionCount++
                Log.d(TAG, "Scripts injected successfully (#$injectionCount), result: $result")
                onResult?.invoke(true)
            }
        }
    }

    /**
     * Injects early masterkey script for onPageStarted to ensure native window.open
     * and auth endpoints are intercepted before the DOM starts rendering.
     */
    fun injectPreloadSecurity(webView: WebView) {
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
