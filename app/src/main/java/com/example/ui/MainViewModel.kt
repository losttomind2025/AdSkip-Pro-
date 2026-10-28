package com.example.ui

import android.app.Application
import android.content.Context
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AdSkipApplication
import com.example.model.SkippedAdEvent
import com.example.service.AdSkipAccessibilityService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = (application as AdSkipApplication).preferences

    val totalAdsSkipped: StateFlow<Int> = prefs.totalAdsSkipped
    val totalTimeSavedSec: StateFlow<Long> = prefs.totalTimeSavedSec
    val todayAdsSkipped: StateFlow<Int> = prefs.todayAdsSkipped
    val isMasterEnabled: StateFlow<Boolean> = prefs.isMasterEnabled
    val isAutoMuteEnabled: StateFlow<Boolean> = prefs.isAutoMuteEnabled
    val isTurboModeEnabled: StateFlow<Boolean> = prefs.isTurboModeEnabled
    val selectedLanguage: StateFlow<String> = prefs.selectedLanguage
    val protectYoutube: StateFlow<Boolean> = prefs.protectYoutube
    val protectBrowsers: StateFlow<Boolean> = prefs.protectBrowsers
    val protectGames: StateFlow<Boolean> = prefs.protectGames
    val recentEvents: StateFlow<List<SkippedAdEvent>> = prefs.recentEvents
    val githubRepoUrl: StateFlow<String> = prefs.githubRepoUrl
    val totalNetworkAdsBlocked: StateFlow<Int> = prefs.totalNetworkAdsBlocked
    val recentBlockedDomains: StateFlow<List<String>> = prefs.recentBlockedDomains
    val isVpnPrefEnabled: StateFlow<Boolean> = prefs.isVpnEnabled
    val isVpnActive: StateFlow<Boolean> = com.example.service.LocalAdBlockVpnService.isVpnActive

    private val _isPermissionGranted = MutableStateFlow(false)
    val isPermissionGranted: StateFlow<Boolean> = _isPermissionGranted.asStateFlow()

    val isServiceLive: StateFlow<Boolean> = AdSkipAccessibilityService.isServiceActive

    // In-App Simulator State
    private val _simStage = MutableStateFlow("IDLE") // IDLE, VIDEO, AD_COUNTDOWN, AD_SKIPPABLE, AD_SKIPPED
    val simStage: StateFlow<String> = _simStage.asStateFlow()

    private val _simCountdown = MutableStateFlow(5)
    val simCountdown: StateFlow<Int> = _simCountdown.asStateFlow()

    private var simJob: Job? = null

    init {
        checkPermissionState()
    }

    fun checkPermissionState() {
        val context = getApplication<Application>()
        val serviceName = "${context.packageName}/${AdSkipAccessibilityService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        val isGranted = enabledServices.contains(context.packageName)
        _isPermissionGranted.value = isGranted
    }

    fun toggleMasterSwitch(enabled: Boolean) {
        prefs.setMasterEnabled(enabled)
    }

    fun toggleAutoMute(enabled: Boolean) {
        prefs.setAutoMuteEnabled(enabled)
    }

    fun toggleTurboMode(enabled: Boolean) {
        prefs.setTurboModeEnabled(enabled)
    }

    fun setLanguage(lang: String) {
        prefs.setSelectedLanguage(lang)
    }

    fun toggleProtectYoutube(enabled: Boolean) = prefs.setProtectYoutube(enabled)
    fun toggleProtectBrowsers(enabled: Boolean) = prefs.setProtectBrowsers(enabled)
    fun toggleProtectGames(enabled: Boolean) = prefs.setProtectGames(enabled)
    fun setGithubRepoUrl(url: String) = prefs.setGithubRepoUrl(url)
    fun toggleVpnPref(enabled: Boolean) = prefs.setVpnEnabled(enabled)

    fun clearHistory() = prefs.clearHistory()

    fun triggerManualTestSkip() {
        prefs.recordSkippedAd("Simulated Player", "com.example.adskipper", 15, "TEST_SIMULATOR")
        _simStage.value = "AD_SKIPPED"
        viewModelScope.launch {
            delay(2500)
            _simStage.value = "VIDEO"
        }
    }

    fun startSimulator() {
        simJob?.cancel()
        simJob = viewModelScope.launch {
            _simStage.value = "VIDEO"
            delay(1500)
            // Trigger Ad Countdown
            _simStage.value = "AD_COUNTDOWN"
            for (i in 3 downTo 1) {
                _simCountdown.value = i
                delay(1000)
            }
            // Ad is now skippable!
            _simStage.value = "AD_SKIPPABLE"
            
            // If the Accessibility service is on, it will click the button.
            // But if it's not on after 6s, user can test manually.
            delay(8000)
            if (_simStage.value == "AD_SKIPPABLE") {
                _simStage.value = "VIDEO"
            }
        }
    }

    fun resetSimulator() {
        simJob?.cancel()
        _simStage.value = "IDLE"
    }
}
