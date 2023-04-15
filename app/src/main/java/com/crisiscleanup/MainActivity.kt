package com.crisiscleanup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.*
import androidx.metrics.performance.JankStats
import com.crisiscleanup.MainActivityUiState.Loading
import com.crisiscleanup.MainActivityUiState.Success
import com.crisiscleanup.core.common.NavigationObserver
import com.crisiscleanup.core.common.NetworkMonitor
import com.crisiscleanup.core.common.PermissionManager
import com.crisiscleanup.core.common.event.*
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.designsystem.theme.CrisisCleanupTheme
import com.crisiscleanup.core.model.data.DarkThemeConfig
import com.crisiscleanup.core.testerfeedbackapi.FeedbackTriggerProvider
import com.crisiscleanup.core.testerfeedbackapi.di.FeedbackTriggerProviderKey
import com.crisiscleanup.core.testerfeedbackapi.di.FeedbackTriggerProviders
import com.crisiscleanup.ui.CrisisCleanupApp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.MapsInitializer.Renderer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity(),
    CredentialsRequestListener,
    SaveCredentialsListener {

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
    internal lateinit var authEventManager: AuthEventManager

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

    private var requestCredentialsListenerId = -1
    private var saveCredentialsListenerId = -1

    private val lifecycleObservers = mutableListOf<LifecycleObserver>()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        MapsInitializer.initialize(this, Renderer.LATEST) {}

        requestCredentialsListenerId = authEventManager.addCredentialsRequestListener(this)
        saveCredentialsListenerId = authEventManager.addSaveCredentialsListener(this)

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

        // Update the uiState
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.onEach { uiState = it }.collect()
            }
        }

        // Update auth state
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.authState.onEach { authState = it }.collect()
            }
        }

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
    }

    override fun onResume() {
        super.onResume()
        lazyStats.get().isTrackingEnabled = true
    }

    override fun onPause() {
        super.onPause()
        lazyStats.get().isTrackingEnabled = false
    }

    override fun onDestroy() {
        super.onDestroy()

        authEventManager.removeCredentialsRequestListener(requestCredentialsListenerId)
        authEventManager.removeSaveCredentialsListener(saveCredentialsListenerId)

        lifecycleObservers.forEach { lifecycle.removeObserver(it) }
    }

    override fun onTrimMemory(level: Int) {
        trimMemoryEventManager.onTrimMemory(level)
        super.onTrimMemory(level)
    }

    // PasswordRequestListener

    override fun onCredentialsRequest() {
        credentialSaveRetrieveManager.passkeySignIn(this, authEventManager)
    }

    // SaveCredentialsListener

    override fun onSaveCredentials(emailAddress: String, password: String) {
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
