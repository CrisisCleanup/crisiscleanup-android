package com.crisiscleanup

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.metrics.performance.JankStats
import com.crisiscleanup.MainActivityViewState.Loading
import com.crisiscleanup.core.common.NetworkMonitor
import com.crisiscleanup.core.common.PermissionManager
import com.crisiscleanup.core.common.PhoneNumberPicker
import com.crisiscleanup.core.common.VisualAlertManager
import com.crisiscleanup.core.common.event.TrimMemoryEventManager
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.sync.SyncPuller
import com.crisiscleanup.core.data.repository.AppMetricsRepository
import com.crisiscleanup.core.data.repository.EndOfLifeRepository
import com.crisiscleanup.core.data.repository.LanguageTranslationsRepository
import com.crisiscleanup.core.data.util.TimeZoneMonitor
import com.crisiscleanup.core.designsystem.theme.CrisisCleanupTheme
import com.crisiscleanup.core.ui.LocalTimeZone
import com.crisiscleanup.sync.initializers.scheduleSyncWorksites
import com.crisiscleanup.ui.CrisisCleanupApp
import com.crisiscleanup.ui.rememberCrisisCleanupAppState
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.MapsInitializer.Renderer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * Lazily inject [JankStats], which is used to track jank throughout the app.
     */
    @Inject
    internal lateinit var lazyStats: dagger.Lazy<JankStats>

    @Inject
    internal lateinit var networkMonitor: NetworkMonitor

    @Inject
    lateinit var timeZoneMonitor: TimeZoneMonitor

    @Inject
    internal lateinit var trimMemoryEventManager: TrimMemoryEventManager

    private val viewModel: MainActivityViewModel by viewModels()

    @Inject
    internal lateinit var syncPuller: SyncPuller

    @Inject
    @Logger(CrisisCleanupLoggers.Auth)
    internal lateinit var logger: AppLogger

    @Inject
    internal lateinit var permissionManager: PermissionManager

    @Inject
    internal lateinit var visualAlertManager: VisualAlertManager

    @Inject
    internal lateinit var intentProcessor: ExternalIntentProcessor

    @Inject
    internal lateinit var endOfLifeRepository: EndOfLifeRepository

    @Inject
    internal lateinit var appMetricsRepository: AppMetricsRepository

    @Inject
    internal lateinit var languageTranslationsRepository: LanguageTranslationsRepository

    @Inject
    lateinit var phoneNumberPicker: PhoneNumberPicker

    private val lifecycleObservers = mutableListOf<LifecycleObserver>()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        MapsInitializer.initialize(this, Renderer.LATEST) {}

        (permissionManager as? DefaultLifecycleObserver)?.let { lifecycleObservers.add(it) }
        (phoneNumberPicker as? DefaultLifecycleObserver)?.let { lifecycleObservers.add(it) }

        lifecycleObservers.forEach { lifecycle.addObserver(it) }

        var viewState: MainActivityViewState by mutableStateOf(Loading)
        var authState: AuthState by mutableStateOf(AuthState.Loading)

        // TODO Splash screen is blank on emulator 32
        // Keep the splash screen on-screen until the UI state is loaded. This condition is
        // evaluated each time the app needs to be redrawn so it should be fast to avoid blocking
        // the UI.
        splashScreen.setKeepOnScreenCondition {
            viewState is Loading || authState is AuthState.Loading
        }

        setContent {
            val darkTheme = isSystemInDarkTheme()

            val windowSizeClass = calculateWindowSizeClass(this)
            val appState = rememberCrisisCleanupAppState(
                networkMonitor = networkMonitor,
                windowSizeClass = windowSizeClass,
                timeZoneMonitor = timeZoneMonitor,
            )
            val currentTimeZone by appState.currentTimeZone.collectAsStateWithLifecycle()

            enableEdgeToEdge()

            CompositionLocalProvider(
                LocalTimeZone provides currentTimeZone,
            ) {
                CrisisCleanupTheme(
                    darkTheme = darkTheme,
                ) {
                    CrisisCleanupApp(appState)
                }
            }
        }

        // Update viewState
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.viewState.collect { viewState = it }
            }
        }

        // Update auth state
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.authState.collect { authState = it }
            }
        }

        intent?.let {
            if (!intentProcessor.processMainIntent(it)) {
                logUnprocessedExternalUri(it)
            }
        }

        if (savedInstanceState == null) {
            lifecycleScope.launch {
                appMetricsRepository.setAppOpen()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        val isConsumed = intentProcessor.processMainIntent(intent)
        if (!isConsumed) {
            logUnprocessedExternalUri(intent)
            super.onNewIntent(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        lazyStats.get().isTrackingEnabled = true
    }

    override fun onStart() {
        super.onStart()
        syncPuller.appPullIncidentData()
        visualAlertManager.setNonProductionAppAlert(true)
        viewModel.onAppFocus()

        endOfLifeRepository.saveEndOfLifeData()
        appMetricsRepository.saveAppSupportInfo()

        languageTranslationsRepository.setLanguageFromSystem()

        scheduleSyncWorksites(this)
    }

    override fun onPause() {
        super.onPause()
        lazyStats.get().isTrackingEnabled = false
    }

    override fun onDestroy() {
        super.onDestroy()

        lifecycleObservers.forEach { lifecycle.removeObserver(it) }
    }

    override fun onTrimMemory(level: Int) {
        trimMemoryEventManager.onTrimMemory(level)
        super.onTrimMemory(level)
    }

    private fun logUnprocessedExternalUri(intent: Intent?) {
        intent?.data?.let { dataUri ->
            // TODO Open to browser or WebView. Do no loop back here.
            logger.logDebug("App link not processed $dataUri")
        }
    }
}
