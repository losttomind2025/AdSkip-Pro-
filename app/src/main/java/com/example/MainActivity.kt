package com.example

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.service.LocalAdBlockVpnService
import com.example.ui.MainViewModel
import com.example.ui.components.DnsShieldCard
import com.example.ui.components.GithubInstallCard
import com.example.ui.components.HeaderSection
import com.example.ui.components.InAppTestSimulatorCard
import com.example.ui.components.PermissionGuideDialog
import com.example.ui.components.PhoneInstallGuideDialog
import com.example.ui.components.QuickControlsCard
import com.example.ui.components.RecentActivityCard
import com.example.ui.components.ServiceStatusCard
import com.example.ui.components.StatMetricsSection
import com.example.ui.components.VpnAdBlockerCard
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ShieldDarkBg

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            viewModel.checkPermissionState()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                val vpnLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        startVpnServiceInternal()
                    }
                }

                AdSkipMainScreen(
                    viewModel = viewModel,
                    onToggleVpn = {
                        if (LocalAdBlockVpnService.isVpnActive.value) {
                            val intent = Intent(this@MainActivity, LocalAdBlockVpnService::class.java).apply {
                                action = LocalAdBlockVpnService.ACTION_DISCONNECT
                            }
                            startService(intent)
                        } else {
                            val prepareIntent = VpnService.prepare(this@MainActivity)
                            if (prepareIntent != null) {
                                vpnLauncher.launch(prepareIntent)
                            } else {
                                startVpnServiceInternal()
                            }
                        }
                    },
                    onOpenAccessibilitySettings = {
                        try {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            startActivity(intent)
                        } catch (e: Exception) {
                            val fallbackIntent = Intent(Settings.ACTION_SETTINGS)
                            startActivity(fallbackIntent)
                        }
                    },
                    onOpenDnsSettings = {
                        try {
                            val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
                            startActivity(intent)
                        } catch (e: Exception) {
                            val fallbackIntent = Intent(Settings.ACTION_SETTINGS)
                            startActivity(fallbackIntent)
                        }
                    },
                    onOpenUrl = { url ->
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            startActivity(intent)
                        } catch (e: Exception) {
                            // Ignored or handled gracefully
                        }
                    }
                )
            }
        }
    }

    private fun startVpnServiceInternal() {
        val intent = Intent(this, LocalAdBlockVpnService::class.java).apply {
            action = LocalAdBlockVpnService.ACTION_CONNECT
        }
        startService(intent)
    }
}

@Composable
fun AdSkipMainScreen(
    viewModel: MainViewModel,
    onToggleVpn: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenDnsSettings: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    val isPermissionGranted by viewModel.isPermissionGranted.collectAsStateWithLifecycle()
    val isServiceLive by viewModel.isServiceLive.collectAsStateWithLifecycle()
    val isVpnActive by viewModel.isVpnActive.collectAsStateWithLifecycle()
    val totalNetworkBlocked by viewModel.totalNetworkAdsBlocked.collectAsStateWithLifecycle()
    val recentBlockedDomains by viewModel.recentBlockedDomains.collectAsStateWithLifecycle()

    val isMasterEnabled by viewModel.isMasterEnabled.collectAsStateWithLifecycle()
    val isAutoMuteEnabled by viewModel.isAutoMuteEnabled.collectAsStateWithLifecycle()
    val isTurboModeEnabled by viewModel.isTurboModeEnabled.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val protectYoutube by viewModel.protectYoutube.collectAsStateWithLifecycle()
    val protectBrowsers by viewModel.protectBrowsers.collectAsStateWithLifecycle()
    val protectGames by viewModel.protectGames.collectAsStateWithLifecycle()
    val totalSkipped by viewModel.totalAdsSkipped.collectAsStateWithLifecycle()
    val timeSavedSec by viewModel.totalTimeSavedSec.collectAsStateWithLifecycle()
    val todaySkipped by viewModel.todayAdsSkipped.collectAsStateWithLifecycle()
    val recentEvents by viewModel.recentEvents.collectAsStateWithLifecycle()
    val githubRepoUrl by viewModel.githubRepoUrl.collectAsStateWithLifecycle()
    val simStage by viewModel.simStage.collectAsStateWithLifecycle()
    val simCountdown by viewModel.simCountdown.collectAsStateWithLifecycle()

    var showGuideDialog by remember { mutableStateOf(false) }
    var showInstallGuideDialog by remember { mutableStateOf(false) }

    val isActuallyActive = isPermissionGranted || isServiceLive

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(ShieldDarkBg),
        containerColor = ShieldDarkBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .safeDrawingPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    HeaderSection(
                        selectedLanguage = selectedLanguage,
                        onLanguageChange = { viewModel.setLanguage(it) },
                        onOpenGuide = { showGuideDialog = true }
                    )
                }

                // 1. PRIMARY ZERO-ADS VPN BLOCKER CARD (Blocks all browser & app ads from loading)
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        VpnAdBlockerCard(
                            isVpnActive = isVpnActive,
                            totalNetworkBlocked = totalNetworkBlocked,
                            recentBlockedDomains = recentBlockedDomains,
                            selectedLanguage = selectedLanguage,
                            onToggleVpn = onToggleVpn
                        )
                    }
                }

                // 2. ACCESSIBILITY AUTO-SKIPPER STATUS (For instant video ad skipping)
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        ServiceStatusCard(
                            isServiceActive = isActuallyActive,
                            isMasterEnabled = isMasterEnabled,
                            selectedLanguage = selectedLanguage,
                            onOpenAccessibilitySettings = {
                                showGuideDialog = true
                            }
                        )
                    }
                }

                // 3. STATS METRICS
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        StatMetricsSection(
                            totalSkipped = totalSkipped + totalNetworkBlocked,
                            timeSavedSec = timeSavedSec + (totalNetworkBlocked * 15L),
                            todaySkipped = todaySkipped,
                            selectedLanguage = selectedLanguage
                        )
                    }
                }

                // 4. QUICK CONTROLS
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        QuickControlsCard(
                            isMasterEnabled = isMasterEnabled,
                            isAutoMuteEnabled = isAutoMuteEnabled,
                            isTurboModeEnabled = isTurboModeEnabled,
                            protectYoutube = protectYoutube,
                            protectBrowsers = protectBrowsers,
                            protectGames = protectGames,
                            selectedLanguage = selectedLanguage,
                            onToggleMaster = { viewModel.toggleMasterSwitch(it) },
                            onToggleAutoMute = { viewModel.toggleAutoMute(it) },
                            onToggleTurboMode = { viewModel.toggleTurboMode(it) },
                            onToggleProtectYoutube = { viewModel.toggleProtectYoutube(it) },
                            onToggleProtectBrowsers = { viewModel.toggleProtectBrowsers(it) },
                            onToggleProtectGames = { viewModel.toggleProtectGames(it) }
                        )
                    }
                }

                // 5. GITHUB INSTALL & APK DOWNLOAD
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        GithubInstallCard(
                            githubRepoUrl = githubRepoUrl,
                            selectedLanguage = selectedLanguage,
                            onOpenGithubReleases = { url -> onOpenUrl(url) },
                            onShowInstallGuide = { showInstallGuideDialog = true }
                        )
                    }
                }

                // 6. IN-APP TEST SIMULATOR
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        InAppTestSimulatorCard(
                            simStage = simStage,
                            simCountdown = simCountdown,
                            isServiceActive = isActuallyActive,
                            selectedLanguage = selectedLanguage,
                            onStartSimulation = { viewModel.startSimulator() },
                            onSkipClicked = { viewModel.triggerManualTestSkip() }
                        )
                    }
                }

                // 7. PRIVATE DNS SHIELD
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        DnsShieldCard(
                            selectedLanguage = selectedLanguage,
                            onOpenDnsSettings = onOpenDnsSettings
                        )
                    }
                }

                // 8. RECENT ACTIVITY LOG
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        RecentActivityCard(
                            events = recentEvents,
                            selectedLanguage = selectedLanguage,
                            onClearHistory = { viewModel.clearHistory() }
                        )
                    }
                }
            }

            if (showGuideDialog) {
                PermissionGuideDialog(
                    selectedLanguage = selectedLanguage,
                    onDismiss = { showGuideDialog = false },
                    onOpenSettings = onOpenAccessibilitySettings
                )
            }

            if (showInstallGuideDialog) {
                PhoneInstallGuideDialog(
                    selectedLanguage = selectedLanguage,
                    onDismiss = { showInstallGuideDialog = false }
                )
            }
        }
    }
}
