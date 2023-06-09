package com.crisiscleanup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.metrics.performance.JankStats
import com.crisiscleanup.MainActivityUiState.Loading
import com.crisiscleanup.MainActivityUiState.Success
import com.crisiscleanup.core.common.NavigationObserver
import com.crisiscleanup.core.common.NetworkMonitor
import com.crisiscleanup.core.common.PermissionManager
import com.crisiscleanup.core.common.event.AuthEventBus
import com.crisiscleanup.core.common.event.TrimMemoryEventManager
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.sync.SyncPuller
import com.crisiscleanup.core.designsystem.theme.CrisisCleanupTheme
import com.crisiscleanup.core.designsystem.theme.navigationContainerColor
import com.crisiscleanup.core.model.data.DarkThemeConfig
import com.crisiscleanup.core.testerfeedbackapi.FeedbackTriggerProvider
import com.crisiscleanup.core.testerfeedbackapi.di.FeedbackTriggerProviderKey
import com.crisiscleanup.core.testerfeedbackapi.di.FeedbackTriggerProviders
import com.crisiscleanup.ui.CrisisCleanupApp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
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
    internal lateinit var navigationObserver: NavigationObserver

    @Inject
    internal lateinit var trimMemoryEventManager: TrimMemoryEventManager

    private val viewModel: MainActivityViewModel by viewModels()

    @Inject
    internal lateinit var authEventBus: AuthEventBus

    @Inject
    internal lateinit var syncPuller: SyncPuller

    @Inject
    @Logger(CrisisCleanupLoggers.Auth)
    internal lateinit var logger: AppLogger

    @Inject
    internal lateinit var credentialManager: CredentialManager

    @Inject
    internal lateinit var permissionManager: PermissionManager

    @Inject
    @FeedbackTriggerProviderKey(FeedbackTriggerProviders.Default)
    internal lateinit var feedbackTriggerProvider: FeedbackTriggerProvider

    private lateinit var credentialSaveRetrieveManager: CredentialSaveRetrieveManager

    private val lifecycleObservers = mutableListOf<LifecycleObserver>()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        MapsInitializer.initialize(this, Renderer.LATEST) {}

        (permissionManager as? DefaultLifecycleObserver)?.let { lifecycleObservers.add(it) }

        lifecycleObservers.addAll(feedbackTriggerProvider.triggers.mapNotNull { it as? LifecycleObserver })

        lifecycleObservers.forEach { lifecycle.addObserver(it) }

        credentialSaveRetrieveManager = CredentialSaveRetrieveManager(
            lifecycleScope,
            credentialManager,
            logger,
        )

        var uiState: MainActivityUiState by mutableStateOf(Loading)
        var authState: AuthState by mutableStateOf(AuthState.Loading)

        // TODO Splash screen is blank on emulator 32
        // Keep the splash screen on-screen until the UI state is loaded. This condition is
        // evaluated each time the app needs to be redrawn so it should be fast to avoid blocking
        // the UI.
        splashScreen.setKeepOnScreenCondition {
            uiState is Loading || authState is AuthState.Loading
        }

        // Turn off the decor fitting system windows, which allows us to handle insets,
        // including IME animations
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val systemUiController = rememberSystemUiController()
            val darkTheme = shouldUseDarkTheme(uiState)

            // Update the dark content of the system bars to match the theme
            DisposableEffect(systemUiController, darkTheme) {
                systemUiController.systemBarsDarkContentEnabled = !darkTheme
                systemUiController.setSystemBarsColor(navigationContainerColor)
                onDispose {}
            }

            CrisisCleanupTheme(
                darkTheme = darkTheme,
            ) {
                CrisisCleanupApp(
                    windowSizeClass = calculateWindowSizeClass(this),
                    networkMonitor = networkMonitor,
                    navigationObserver = navigationObserver,
                )
            }
        }

        // Update the uiState
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState = it }
            }
        }

        // Update auth state
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.authState.collect { authState = it }
            }
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                authEventBus.credentialRequests
                    .collect { onCredentialsRequest() }
            }
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                authEventBus.saveCredentialRequests
                    .collect { onSaveCredentials(it.first, it.second) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lazyStats.get().isTrackingEnabled = true
    }

    override fun onStart() {
        super.onStart()
        syncPuller.appPullIncidentWorksitesDelta()
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

    private fun onCredentialsRequest() {
        credentialSaveRetrieveManager.passkeySignIn(this, authEventBus)
    }

    private fun onSaveCredentials(emailAddress: String, password: String) {
        credentialSaveRetrieveManager.saveAccountPassword(this, emailAddress, password)
    }
}

/**
 * Returns `true` if dark theme should be used, as a function of the [uiState] and the
 * current system context.
 */
@Composable
private fun shouldUseDarkTheme(
    uiState: MainActivityUiState,
): Boolean = when (uiState) {
    Loading -> isSystemInDarkTheme()
    is Success -> when (uiState.userData.darkThemeConfig) {
        DarkThemeConfig.FOLLOW_SYSTEM -> isSystemInDarkTheme()
        DarkThemeConfig.LIGHT -> false
        DarkThemeConfig.DARK -> true
    }
}
