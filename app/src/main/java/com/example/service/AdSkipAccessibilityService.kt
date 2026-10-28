package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.AdSkipApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdSkipAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var audioManager: AudioManager? = null
    private var isMutedByAd = false
    private var previousVolume = -1
    private var lastClickTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        _isServiceActive.value = true
        Log.d(TAG, "AdSkipAccessibilityService connected and active")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val prefs = try { AdSkipApplication.instance.preferences } catch (e: Exception) { null } ?: return
        if (!prefs.isMasterEnabled.value) return

        val packageName = event.packageName?.toString() ?: ""
        
        // Check per-app filters
        if (!isAppProtectionEnabled(packageName, prefs)) {
            return
        }

        val rootNode = rootInActiveWindow ?: return

        try {
            // Check for Auto-Mute conditions on video ads
            if (prefs.isAutoMuteEnabled.value) {
                checkAndHandleAutoMute(rootNode)
            }

            // Find and click Skip Ad or Close buttons
            val skipDelay = if (prefs.isTurboModeEnabled.value) 0L else 300L
            if (skipDelay > 0) {
                handler.postDelayed({
                    findAndClickSkipButton(rootNode, packageName)
                }, skipDelay)
            } else {
                findAndClickSkipButton(rootNode, packageName)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing accessibility event", e)
        }
    }

    private fun isAppProtectionEnabled(packageName: String, prefs: com.example.data.AdSkipPreferences): Boolean {
        if (packageName.contains("youtube", ignoreCase = true)) {
            return prefs.protectYoutube.value
        }
        if (packageName.contains("chrome", ignoreCase = true) ||
            packageName.contains("browser", ignoreCase = true) ||
            packageName.contains("firefox", ignoreCase = true)) {
            return prefs.protectBrowsers.value
        }
        // General games / other apps
        return prefs.protectGames.value
    }

    private fun checkAndHandleAutoMute(rootNode: AccessibilityNodeInfo) {
        val am = audioManager ?: return
        val isAdPlaying = detectAdIndicators(rootNode)

        if (isAdPlaying && !isMutedByAd) {
            // Mute media audio
            try {
                previousVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                am.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                isMutedByAd = true
                Log.d(TAG, "Ad detected: Muted audio (previous volume: $previousVolume)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mute audio", e)
            }
        } else if (!isAdPlaying && isMutedByAd) {
            // Unmute media audio
            restoreAudioVolume()
        }
    }

    private fun restoreAudioVolume() {
        val am = audioManager ?: return
        if (isMutedByAd && previousVolume >= 0) {
            try {
                am.setStreamVolume(AudioManager.STREAM_MUSIC, previousVolume, 0)
                isMutedByAd = false
                Log.d(TAG, "Ad ended: Restored audio volume to $previousVolume")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore audio", e)
            }
        }
    }

    private fun detectAdIndicators(node: AccessibilityNodeInfo): Boolean {
        val text = node.text?.toString()?.lowercase() ?: ""
        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""

        val adSignatures = listOf(
            "ad 1 of", "ad 2 of", "video will play after ad",
            "ad · 0:", "ad · 00:", "ad • 0:", "विज्ञापन 1/", "विज्ञापन समाप्त होगा"
        )

        for (sig in adSignatures) {
            if (text.contains(sig) || contentDesc.contains(sig)) {
                return true
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (detectAdIndicators(child)) {
                return true
            }
        }
        return false
    }

    private fun findAndClickSkipButton(rootNode: AccessibilityNodeInfo, packageName: String): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastClickTime < 400L) {
            return false // Debounce rapid multi-clicks
        }

        // 1. Check known skip button view IDs
        for (viewId in KNOWN_SKIP_VIEW_IDS) {
            val nodes = rootNode.findAccessibilityNodeInfosByViewId(viewId)
            if (!nodes.isNullOrEmpty()) {
                for (node in nodes) {
                    if (performClick(node, packageName, "ViewId: $viewId")) {
                        lastClickTime = now
                        return true
                    }
                }
            }
        }

        // 2. Search by matching text keywords recursively
        val clicked = searchAndClickByKeywords(rootNode, packageName)
        if (clicked) {
            lastClickTime = now
        }
        return clicked
    }

    private fun searchAndClickByKeywords(node: AccessibilityNodeInfo, packageName: String): Boolean {
        val text = node.text?.toString()?.trim() ?: ""
        val contentDesc = node.contentDescription?.toString()?.trim() ?: ""

        if (matchesSkipPattern(text) || matchesSkipPattern(contentDesc)) {
            if (performClick(node, packageName, "Keyword: $text")) {
                return true
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (searchAndClickByKeywords(child, packageName)) {
                return true
            }
        }
        return false
    }

    private fun matchesSkipPattern(input: String): Boolean {
        if (input.isBlank()) return false
        val lower = input.lowercase()

        // Direct matching against known skip keywords
        for (keyword in SKIP_KEYWORDS) {
            if (lower == keyword || lower.startsWith(keyword) || lower.endsWith(keyword)) {
                return true
            }
        }

        // Regex for patterns like "Skip in 5", "Skip in 0:05", "Skip ad"
        if (lower.contains("skip ad") || lower.contains("skip ads") || lower.contains("skip in")) {
            return true
        }

        // Exact Hindi skip matches
        if (input.contains("विज्ञापन छोड़ें") || input.contains("स्किप करें") || input.contains("विज्ञापन छोड़िये")) {
            return true
        }

        return false
    }

    private fun performClick(node: AccessibilityNodeInfo, packageName: String, triggerReason: String): Boolean {
        var targetNode: AccessibilityNodeInfo? = node

        // If the node itself is not clickable, walk up to 3 parent levels to find clickable container
        var depth = 0
        while (targetNode != null && !targetNode.isClickable && depth < 3) {
            targetNode = targetNode.parent
            depth++
        }

        val nodeToClick = targetNode ?: node
        val success = nodeToClick.performAction(AccessibilityNodeInfo.ACTION_CLICK)

        if (success) {
            Log.d(TAG, "Successfully clicked skip button in $packageName ($triggerReason)")
            restoreAudioVolume()

            val appLabel = getAppLabel(packageName)
            try {
                AdSkipApplication.instance.preferences.recordSkippedAd(
                    appName = appLabel,
                    packageName = packageName,
                    estimatedTimeSavedSec = 15,
                    actionType = "AUTO_SKIPPED"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error recording skipped ad", e)
            }
            return true
        }
        return false
    }

    private fun getAppLabel(packageName: String): String {
        return try {
            val pm = applicationContext.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            when {
                packageName.contains("youtube", ignoreCase = true) -> "YouTube"
                packageName.contains("chrome", ignoreCase = true) -> "Chrome"
                packageName.contains("browser", ignoreCase = true) -> "Browser"
                packageName.contains("game", ignoreCase = true) -> "Game"
                else -> packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
            }
        }
    }

    override fun onInterrupt() {
        restoreAudioVolume()
        _isServiceActive.value = false
        Log.d(TAG, "AdSkipAccessibilityService interrupted")
    }

    override fun onDestroy() {
        restoreAudioVolume()
        _isServiceActive.value = false
        super.onDestroy()
    }

    companion object {
        private const val TAG = "AdSkipService"

        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

        private val KNOWN_SKIP_VIEW_IDS = listOf(
            "com.google.android.youtube:id/skip_ad_button",
            "com.google.android.youtube:id/sub_skip_ad_button",
            "com.google.android.youtube:id/modern_skip_ad_button",
            "com.google.android.youtube:id/ad_skip_button",
            "skip_ad_button",
            "ad_skip_button",
            "btn_skip",
            "btn_skip_ad",
            "btn_close",
            "close_button",
            "iv_close",
            "tt_video_ad_close_layout",
            "native_ad_view",
            "interstitial_close_button",
            "ad_close"
        )

        private val SKIP_KEYWORDS = listOf(
            "skip ad",
            "skip ads",
            "skip",
            "close",
            "close ad",
            "dismiss",
            "no thanks",
            "not now",
            "विज्ञापन छोड़ें",
            "स्किप करें",
            "विज्ञापन छोड़िये",
            "छोड़ें",
            "बंद करें"
        )
    }
}
