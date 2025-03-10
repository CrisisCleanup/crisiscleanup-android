package com.crisiscleanup.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.AuthState
import com.crisiscleanup.MainActivityViewModel
import com.crisiscleanup.MainActivityViewState
import com.crisiscleanup.core.common.NetworkMonitor
import com.crisiscleanup.core.common.TutorialStep
import com.crisiscleanup.core.designsystem.LayoutProvider
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.LocalLayoutProvider
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.component.CrisisCleanupAlertDialog
import com.crisiscleanup.core.designsystem.component.CrisisCleanupBackground
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.core.designsystem.theme.LocalDimensions
import com.crisiscleanup.core.model.data.TutorialViewId
import com.crisiscleanup.core.ui.AppLayoutArea
import com.crisiscleanup.core.ui.LayoutSizePosition
import com.crisiscleanup.core.ui.LocalAppLayout
import com.crisiscleanup.core.ui.rememberIsKeyboardOpen
import com.crisiscleanup.core.ui.sizePosition
import com.crisiscleanup.feature.authentication.navigation.navigateToMagicLinkLogin
import com.crisiscleanup.feature.authentication.navigation.navigateToOrgPersistentInvite
import com.crisiscleanup.feature.authentication.navigation.navigateToPasswordReset
import com.crisiscleanup.feature.authentication.navigation.navigateToRequestAccess
import com.crisiscleanup.navigation.CrisisCleanupAuthNavHost
import com.crisiscleanup.navigation.CrisisCleanupNavHost
import com.crisiscleanup.feature.authentication.R as authenticationR

@Composable
fun CrisisCleanupApp(
    windowSizeClass: WindowSizeClass,
    networkMonitor: NetworkMonitor,
    appState: CrisisCleanupAppState = rememberCrisisCleanupAppState(
        networkMonitor = networkMonitor,
        windowSizeClass = windowSizeClass,
    ),
    viewModel: MainActivityViewModel = hiltViewModel(),
) {
    CrisisCleanupBackground {
        Box {
            val snackbarHostState = remember { SnackbarHostState() }

            val isOffline by appState.isOffline.collectAsStateWithLifecycle()

            val t = viewModel.translator

            LaunchedEffect(isOffline) {
                val notConnectedMessage = t("info.no_internet")
                if (isOffline) {
                    snackbarHostState.showSnackbar(
                        message = notConnectedMessage,
                        duration = SnackbarDuration.Indefinite,
                        withDismissAction = true,
                    )
                }
            }

            val authState by viewModel.authState.collectAsStateWithLifecycle()
            if (authState is AuthState.Loading) {
                // Splash screen should be showing
            } else {
                val layoutBottomNav =
                    appState.shouldShowBottomBar || LocalDimensions.current.isPortrait
                val layoutProvider = LayoutProvider(
                    isBottomNav = layoutBottomNav,
                )
                CompositionLocalProvider(
                    LocalAppTranslator provides t,
                    LocalLayoutProvider provides layoutProvider,
                ) {
                    val minSupportedAppVersion = viewModel.supportedApp
                    if (minSupportedAppVersion?.isUnsupported == true) {
                        UnsupportedBuildView(minSupportedAppVersion)
                    } else {
                        // Render content even if translations are not fully downloaded in case internet connection is not available.
                        // Translations without fallbacks will show until translations are downloaded.
                        LoadedContent(
                            snackbarHostState,
                            appState,
                            viewModel,
                            authState,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.LoadedContent(
    snackbarHostState: SnackbarHostState,
    appState: CrisisCleanupAppState,
    viewModel: MainActivityViewModel,
    authState: AuthState,
) {
    val isAccountExpired = viewModel.isAccountExpired
    val hasAcceptedTerms = viewModel.hasAcceptedTerms

    val isNotAuthenticatedState = authState !is AuthState.Authenticated
    var openAuthentication by rememberSaveable { mutableStateOf(isNotAuthenticatedState) }

    val showPasswordReset by viewModel.showPasswordReset.collectAsStateWithLifecycle(false)

    if (openAuthentication ||
        isNotAuthenticatedState
    ) {
        val toggleAuthentication = remember(authState) {
            { open: Boolean -> openAuthentication = open }
        }
        AuthenticateContent(
            snackbarHostState,
            appState,
            !isNotAuthenticatedState,
            toggleAuthentication,
        )

        if (isNotAuthenticatedState) {
            val showMagicLinkLogin by viewModel.showMagicLinkLogin.collectAsStateWithLifecycle(false)
            val orgUserInviteCode by viewModel.orgUserInvites.collectAsStateWithLifecycle("")
            val orgPersistentInvite by viewModel.orgPersistentInvites.collectAsStateWithLifecycle()

            if (showPasswordReset) {
                appState.navController.navigateToPasswordReset(false)
            } else if (showMagicLinkLogin) {
                appState.navController.navigateToMagicLinkLogin()
            } else if (orgUserInviteCode.isNotBlank()) {
                appState.navController.navigateToRequestAccess(orgUserInviteCode)
            } else if (orgPersistentInvite.isValidInvite) {
                appState.navController.navigateToOrgPersistentInvite()
            }
        }
    } else if (!hasAcceptedTerms) {
        val isFetching by viewModel.isFetchingTermsAcceptance.collectAsStateWithLifecycle()
        if (!isFetching) {
            val isLoading by viewModel.isLoadingTermsAcceptance.collectAsStateWithLifecycle()
            val setAcceptingTerms = remember(viewModel) {
                { accept: Boolean ->
                    viewModel.isAcceptingTerms = accept
                }
            }
            AcceptTermsContent(
                snackbarHostState,
                viewModel.termsOfServiceUrl,
                viewModel.privacyPolicyUrl,
                isLoading,
                viewModel.isAcceptingTerms,
                setAcceptingTerms,
                onRejectTerms = viewModel::onRejectTerms,
                onAcceptTerms = viewModel::onAcceptTerms,
                errorMessage = viewModel.acceptTermsErrorMessage,
            )
        }
        BusyIndicatorFloatingTopCenter(isFetching)
    } else {
        val mainViewState by viewModel.viewState.collectAsStateWithLifecycle()
        val hideOnboarding =
            (mainViewState as? MainActivityViewState.Success)?.userData?.shouldHideOnboarding
                ?: true
        val isOnboarding = !hideOnboarding

        val menuTutorialStep by viewModel.menuTutorialStep.collectAsStateWithLifecycle()

        NavigableContent(
            snackbarHostState,
            appState,
            isOnboarding,
            menuTutorialStep,
            viewModel.tutorialViewTracker.viewSizePositionLookup,
            viewModel::onMenuTutorialNext,
        ) { openAuthentication = true }

        if (
            isAccountExpired &&
            !appState.hideLoginAlert
        ) {
            ExpiredAccountAlert(snackbarHostState) {
                openAuthentication = true
            }
        }

        if (showPasswordReset) {
            appState.navController.navigateToPasswordReset(true)
        }
    }

    if (viewModel.showInactiveOrganization) {
        val t = viewModel.translator
        CrisisCleanupAlertDialog(
            title = t("info.account_inactive"),
            text = t("info.account_inactive_no_organization"),
            confirmButton = {
                CrisisCleanupTextButton(
                    text = t("actions.ok"),
                    onClick = viewModel::acknowledgeInactiveOrganization,
                )
            },
        )
    }
}

@OptIn(
    ExperimentalComposeUiApi::class,
)
@Composable
private fun AuthenticateContent(
    snackbarHostState: SnackbarHostState,
    appState: CrisisCleanupAppState,
    enableBackHandler: Boolean,
    toggleAuthentication: (Boolean) -> Unit,
) {
    Scaffold(
        modifier = Modifier.semantics {
            testTagsAsResourceId = true
        },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = WindowInsets.systemBars,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        CrisisCleanupAuthNavHost(
            navController = appState.navController,
            enableBackHandler = enableBackHandler,
            closeAuthentication = { toggleAuthentication(false) },
            onBack = appState::onBack,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
                ),
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun AcceptTermsContent(
    snackbarHostState: SnackbarHostState,
    termsOfServiceUrl: String,
    privacyPolicyUrl: String,
    isLoading: Boolean,
    isAcceptingTerms: Boolean,
    setAcceptingTerms: (Boolean) -> Unit,
    onRejectTerms: () -> Unit = {},
    onAcceptTerms: () -> Unit = {},
    errorMessage: String = "",
) {
    Scaffold(
        modifier = Modifier.semantics {
            testTagsAsResourceId = true
        },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = WindowInsets.systemBars,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        AcceptTermsView(
            termsOfServiceUrl,
            privacyPolicyUrl,
            isLoading,
            isAcceptingTerms = isAcceptingTerms,
            setAcceptingTerms = setAcceptingTerms,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
                ),
            onRejectTerms = onRejectTerms,
            onAcceptTerms = onAcceptTerms,
            errorMessage = errorMessage,
        )
    }
}

@OptIn(
    ExperimentalComposeUiApi::class,
)
@Composable
private fun NavigableContent(
    snackbarHostState: SnackbarHostState,
    appState: CrisisCleanupAppState,
    isOnboarding: Boolean,
    menuTutorialStep: TutorialStep,
    tutorialViewLookup: SnapshotStateMap<TutorialViewId, LayoutSizePosition>,
    advanceMenuTutorial: () -> Unit,
    openAuthentication: () -> Unit,
) {
    val showNavigation = appState.isTopLevelRoute
    val layoutBottomNav = LocalLayoutProvider.current.isBottomNav
    val isFullscreen = appState.isFullscreenRoute

    val navBarSizePositionModifier = Modifier.onGloballyPositioned { coordinates ->
        tutorialViewLookup[TutorialViewId.AppNavBar] = coordinates.sizePosition
    }

    Scaffold(
        modifier = Modifier.semantics {
            testTagsAsResourceId = true
        },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            val showBottomBar = showNavigation && layoutBottomNav
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideIn { IntOffset.Zero },
                exit = slideOut { IntOffset.Zero },
            ) {
                AppNavigationBar(
                    appState,
                    navBarSizePositionModifier.testTag("AppNavigationBottomBar"),
                )
            }

            if (!showBottomBar) {
                val windowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Bottom)
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(windowInsets),
                )
            }
        },
    ) { padding ->
        Row(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .windowInsetsPadding(
                    if (isFullscreen) {
                        WindowInsets(0, 0, 0, 0)
                    } else {
                        WindowInsets.safeDrawing
                    },
                ),
        ) {
            if (showNavigation && !layoutBottomNav) {
                AppNavigationBar(
                    appState,
                    navBarSizePositionModifier
                        .safeDrawingPadding()
                        .testTag("AppNavigationSideRail"),
                    true,
                )
            }

            val isKeyboardOpen = rememberIsKeyboardOpen()
            Column {
                val snackbarAreaHeight =
                    if (!showNavigation &&
                        snackbarHostState.currentSnackbarData != null &&
                        !isKeyboardOpen
                    ) {
                        64.dp
                    } else {
                        0.dp
                    }

                CompositionLocalProvider(
                    LocalAppLayout provides AppLayoutArea(snackbarHostState),
                ) {
                    CrisisCleanupNavHost(
                        navController = appState.navController,
                        onBack = appState::onBack,
                        openAuthentication = openAuthentication,
                        modifier = Modifier.weight(1f),
                        startDestination = appState.lastTopLevelRoute(isOnboarding),
                    )
                }

                Spacer(
                    Modifier
                        .height(snackbarAreaHeight)
                        .animateContentSize(),
                )
            }
        }
    }

    if (menuTutorialStep != TutorialStep.End) {
        TutorialOverlay(
            tutorialStep = menuTutorialStep,
            onNextStep = advanceMenuTutorial,
            tutorialViewLookup = tutorialViewLookup,
        )
    }
}

@Composable
private fun ExpiredAccountAlert(
    snackbarHostState: SnackbarHostState,
    openAuthentication: () -> Unit,
) {
    val t = LocalAppTranslator.current
    val translationCount by t.translationCount.collectAsStateWithLifecycle()
    val message = t("info.log_in_for_updates")
    val loginText = t("actions.login", authenticationR.string.login)
    LaunchedEffect(translationCount) {
        val result = snackbarHostState.showSnackbar(
            message,
            actionLabel = loginText,
            withDismissAction = true,
            duration = SnackbarDuration.Indefinite,
        )
        when (result) {
            SnackbarResult.Dismissed -> {}
            SnackbarResult.ActionPerformed -> openAuthentication()
        }
    }
}
