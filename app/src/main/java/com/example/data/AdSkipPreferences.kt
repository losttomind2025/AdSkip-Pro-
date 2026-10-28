package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.SkippedAdEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AdSkipPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("ad_skip_prefs", Context.MODE_PRIVATE)

    private val _totalAdsSkipped = MutableStateFlow(prefs.getInt(KEY_TOTAL_SKIPPED, 14))
    val totalAdsSkipped: StateFlow<Int> = _totalAdsSkipped.asStateFlow()

    private val _totalTimeSavedSec = MutableStateFlow(prefs.getLong(KEY_TIME_SAVED_SEC, 210L))
    val totalTimeSavedSec: StateFlow<Long> = _totalTimeSavedSec.asStateFlow()

    private val _todayAdsSkipped = MutableStateFlow(prefs.getInt(KEY_TODAY_SKIPPED, 3))
    val todayAdsSkipped: StateFlow<Int> = _todayAdsSkipped.asStateFlow()

    private val _isMasterEnabled = MutableStateFlow(prefs.getBoolean(KEY_MASTER_ENABLED, true))
    val isMasterEnabled: StateFlow<Boolean> = _isMasterEnabled.asStateFlow()

    private val _isAutoMuteEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUTO_MUTE, true))
    val isAutoMuteEnabled: StateFlow<Boolean> = _isAutoMuteEnabled.asStateFlow()

    private val _isTurboModeEnabled = MutableStateFlow(prefs.getBoolean(KEY_TURBO_MODE, true))
    val isTurboModeEnabled: StateFlow<Boolean> = _isTurboModeEnabled.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(prefs.getString(KEY_LANGUAGE, "hi") ?: "hi")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private val _protectYoutube = MutableStateFlow(prefs.getBoolean(KEY_PROTECT_YT, true))
    val protectYoutube: StateFlow<Boolean> = _protectYoutube.asStateFlow()

    private val _protectBrowsers = MutableStateFlow(prefs.getBoolean(KEY_PROTECT_BROWSER, true))
    val protectBrowsers: StateFlow<Boolean> = _protectBrowsers.asStateFlow()

    private val _protectGames = MutableStateFlow(prefs.getBoolean(KEY_PROTECT_GAMES, true))
    val protectGames: StateFlow<Boolean> = _protectGames.asStateFlow()

    private val _recentEvents = MutableStateFlow<List<SkippedAdEvent>>(loadInitialEvents())
    val recentEvents: StateFlow<List<SkippedAdEvent>> = _recentEvents.asStateFlow()

    private val _githubRepoUrl = MutableStateFlow(
        prefs.getString(KEY_GITHUB_REPO_URL, "https://github.com/ahtishammustafa/adskip-pro")
            ?: "https://github.com/ahtishammustafa/adskip-pro"
    )
    val githubRepoUrl: StateFlow<String> = _githubRepoUrl.asStateFlow()

    private val _totalNetworkAdsBlocked = MutableStateFlow(prefs.getInt(KEY_NETWORK_ADS_BLOCKED, 38))
    val totalNetworkAdsBlocked: StateFlow<Int> = _totalNetworkAdsBlocked.asStateFlow()

    private val _isVpnEnabled = MutableStateFlow(prefs.getBoolean(KEY_VPN_ENABLED, true))
    val isVpnEnabled: StateFlow<Boolean> = _isVpnEnabled.asStateFlow()

    private val _recentBlockedDomains = MutableStateFlow(
        listOf(
            "pagead2.googlesyndication.com",
            "googleads.g.doubleclick.net",
            "adservice.google.com",
            "api.unityads.unity3d.com",
            "ads.applovin.com"
        )
    )
    val recentBlockedDomains: StateFlow<List<String>> = _recentBlockedDomains.asStateFlow()

    init {
        checkAndResetDailyCount()
    }

    private fun checkAndResetDailyCount() {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val savedDate = prefs.getString(KEY_LAST_DATE, "")
        if (savedDate != todayStr) {
            prefs.edit()
                .putString(KEY_LAST_DATE, todayStr)
                .putInt(KEY_TODAY_SKIPPED, 0)
                .apply()
            _todayAdsSkipped.value = 0
        }
    }

    private fun loadInitialEvents(): List<SkippedAdEvent> {
        val now = System.currentTimeMillis()
        return listOf(
            SkippedAdEvent(
                id = UUID.randomUUID().toString(),
                appName = "YouTube",
                packageName = "com.google.android.youtube",
                timestamp = now - 180000,
                timeSavedSec = 15,
                actionType = "AUTO_SKIPPED"
            ),
            SkippedAdEvent(
                id = UUID.randomUUID().toString(),
                appName = "Chrome Browser",
                packageName = "com.android.chrome",
                timestamp = now - 920000,
                timeSavedSec = 10,
                actionType = "POPUP_CLOSED"
            ),
            SkippedAdEvent(
                id = UUID.randomUUID().toString(),
                appName = "Subway Rush 3D",
                packageName = "com.game.runner",
                timestamp = now - 2400000,
                timeSavedSec = 30,
                actionType = "INTERSTITIAL_SKIPPED"
            )
        )
    }

    fun recordSkippedAd(appName: String, packageName: String, estimatedTimeSavedSec: Int = 15, actionType: String = "AUTO_SKIPPED") {
        checkAndResetDailyCount()
        val newTotal = _totalAdsSkipped.value + 1
        val newTime = _totalTimeSavedSec.value + estimatedTimeSavedSec
        val newToday = _todayAdsSkipped.value + 1

        prefs.edit()
            .putInt(KEY_TOTAL_SKIPPED, newTotal)
            .putLong(KEY_TIME_SAVED_SEC, newTime)
            .putInt(KEY_TODAY_SKIPPED, newToday)
            .apply()

        _totalAdsSkipped.value = newTotal
        _totalTimeSavedSec.value = newTime
        _todayAdsSkipped.value = newToday

        val event = SkippedAdEvent(
            id = UUID.randomUUID().toString(),
            appName = appName,
            packageName = packageName,
            timestamp = System.currentTimeMillis(),
            timeSavedSec = estimatedTimeSavedSec,
            actionType = actionType
        )
        val updatedList = (listOf(event) + _recentEvents.value).take(30)
        _recentEvents.value = updatedList
    }

    fun setMasterEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MASTER_ENABLED, enabled).apply()
        _isMasterEnabled.value = enabled
    }

    fun setAutoMuteEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_MUTE, enabled).apply()
        _isAutoMuteEnabled.value = enabled
    }

    fun setTurboModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TURBO_MODE, enabled).apply()
        _isTurboModeEnabled.value = enabled
    }

    fun setSelectedLanguage(lang: String) {
        prefs.edit().putString(KEY_LANGUAGE, lang).apply()
        _selectedLanguage.value = lang
    }

    fun setProtectYoutube(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PROTECT_YT, enabled).apply()
        _protectYoutube.value = enabled
    }

    fun setProtectBrowsers(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PROTECT_BROWSER, enabled).apply()
        _protectBrowsers.value = enabled
    }

    fun setProtectGames(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PROTECT_GAMES, enabled).apply()
        _protectGames.value = enabled
    }

    fun clearHistory() {
        _recentEvents.value = emptyList()
    }

    fun setGithubRepoUrl(url: String) {
        prefs.edit().putString(KEY_GITHUB_REPO_URL, url).apply()
        _githubRepoUrl.value = url
    }

    fun setVpnEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VPN_ENABLED, enabled).apply()
        _isVpnEnabled.value = enabled
    }

    fun recordNetworkAdBlocked(domain: String) {
        val newCount = _totalNetworkAdsBlocked.value + 1
        prefs.edit().putInt(KEY_NETWORK_ADS_BLOCKED, newCount).apply()
        _totalNetworkAdsBlocked.value = newCount
        val updated = (listOf(domain) + _recentBlockedDomains.value.filter { it != domain }).take(10)
        _recentBlockedDomains.value = updated
    }

    companion object {
        private const val KEY_TOTAL_SKIPPED = "key_total_skipped"
        private const val KEY_TIME_SAVED_SEC = "key_time_saved_sec"
        private const val KEY_TODAY_SKIPPED = "key_today_skipped"
        private const val KEY_LAST_DATE = "key_last_date"
        private const val KEY_MASTER_ENABLED = "key_master_enabled"
        private const val KEY_AUTO_MUTE = "key_auto_mute"
        private const val KEY_TURBO_MODE = "key_turbo_mode"
        private const val KEY_LANGUAGE = "key_language"
        private const val KEY_PROTECT_YT = "key_protect_yt"
        private const val KEY_PROTECT_BROWSER = "key_protect_browser"
        private const val KEY_PROTECT_GAMES = "key_protect_games"
        private const val KEY_GITHUB_REPO_URL = "key_github_repo_url"
        private const val KEY_VPN_ENABLED = "key_vpn_enabled"
        private const val KEY_NETWORK_ADS_BLOCKED = "key_network_ads_blocked"
    }
}
