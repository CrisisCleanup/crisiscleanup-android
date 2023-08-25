package com.crisiscleanup.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.crisiscleanup.AuthState
import com.crisiscleanup.MainActivityViewModel
import com.crisiscleanup.core.common.NavigationObserver
import com.crisiscleanup.core.common.NetworkMonitor
import com.crisiscleanup.core.commoncase.ui.IncidentDropdownSelect
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupBackground
import com.crisiscleanup.core.designsystem.component.CrisisCleanupNavigationBar
import com.crisiscleanup.core.designsystem.component.CrisisCleanupNavigationBarItem
import com.crisiscleanup.core.designsystem.component.CrisisCleanupNavigationRail
import com.crisiscleanup.core.designsystem.component.CrisisCleanupNavigationRailItem
import com.crisiscleanup.core.designsystem.component.TopAppBarDefault
import com.crisiscleanup.core.designsystem.component.TruncatedAppBarText
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.icon.Icon.DrawableResourceIcon
import com.crisiscleanup.core.designsystem.icon.Icon.ImageVectorIcon
import com.crisiscleanup.core.ui.AppLayoutArea
import com.crisiscleanup.core.ui.LocalAppLayout
import com.crisiscleanup.core.ui.rememberIsKeyboardOpen
import com.crisiscleanup.feature.cases.ui.SelectIncidentDialog
import com.crisiscleanup.navigation.CrisisCleanupAuthNavHost
import com.crisiscleanup.navigation.CrisisCleanupNavHost
import com.crisiscleanup.navigation.TopLevelDestination
import com.crisiscleanup.feature.authentication.R as authenticationR

@Composable
fun CrisisCleanupApp(
    windowSizeClass: WindowSizeClass,
    networkMonitor: NetworkMonitor,
    navigationObserver: NavigationObserver,
    appState: CrisisCleanupAppState = rememberCrisisCleanupAppState(
        networkMonitor = networkMonitor,
        windowSizeClass = windowSizeClass,
        navigationObserver = navigationObserver,
    ),
    viewModel: MainActivityViewModel = hiltViewModel(),
) {
    CrisisCleanupBackground {
        Box(Modifier.fillMaxSize()) {
            val snackbarHostState = remember { SnackbarHostState() }

            val isOffline by appState.isOffline.collectAsStateWithLifecycle()

            val translationCount by viewModel.translationCount.collectAsStateWithLifecycle()
            val translator = viewModel.translator

            LaunchedEffect(isOffline) {
                val notConnectedMessage = translator("info.no_internet")
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
                CompositionLocalProvider(LocalAppTranslator provides translator) {
                    val endOfLife = viewModel.buildEndOfLife
                    if (endOfLife?.isEndOfLife == true) {
                        EndOfLifeView(endOfLife)
                    } else {
                        // Render content even if translations are not fully downloaded in case internet connection is not available.
                        // Translations without fallbacks will show until translations are downloaded.
                        LoadedContent(
                            snackbarHostState,
                            appState,
                            viewModel,
                            authState,
                            translationCount,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadedContent(
    snackbarHostState: SnackbarHostState,
    appState: CrisisCleanupAppState,
    viewModel: MainActivityViewModel,
    authState: AuthState,
    translationCount: Int = 0,
) {
    val isAccountExpired by viewModel.isAccountExpired

    val isNotAuthenticatedState = authState !is AuthState.Authenticated
    var openAuthentication by rememberSaveable { mutableStateOf(isNotAuthenticatedState) }
    if (openAuthentication || isNotAuthenticatedState) {
        val toggleAuthentication = remember(authState) {
            { open: Boolean -> openAuthentication = open }
        }
        AuthenticateContent(
            snackbarHostState,
            appState,
            !isNotAuthenticatedState,
            toggleAuthentication,
        )
    } else {
        val accountData = (authState as AuthState.Authenticated).accountData
        val profilePictureUri = accountData.profilePictureUri
        val appHeaderBar = viewModel.appHeaderUiState
        val appHeaderTitle by appHeaderBar.title.collectAsStateWithLifecycle()
        val isHeaderLoading by viewModel.showHeaderLoading.collectAsState(false)

        var showIncidentPicker by remember { mutableStateOf(false) }
        val openIncidentsSelect = remember(viewModel) {
            { showIncidentPicker = true }
        }

        val disasterIconResId by viewModel.disasterIconResId.collectAsStateWithLifecycle()

        NavigableContent(
            snackbarHostState,
            appState,
            appHeaderTitle,
            isHeaderLoading,
            { openAuthentication = true },
            profilePictureUri,
            isAccountExpired,
            disasterIconResId,
            openIncidentsSelect,
        )

        if (
            isAccountExpired &&
            !appState.hideLoginAlert
        ) {
            ExpiredAccountAlert(snackbarHostState, translationCount) {
                openAuthentication = true
            }
        }

        if (showIncidentPicker) {
            val closeDialog = { showIncidentPicker = false }
            SelectIncidentDialog(closeDialog)
        }
    }
}

@OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalLayoutApi::class,
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
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal,
                    ),
                ),
        )
    }
}

@OptIn(
    ExperimentalLayoutApi::class,
    ExperimentalComposeUiApi::class,
)
@Composable
private fun NavigableContent(
    snackbarHostState: SnackbarHostState,
    appState: CrisisCleanupAppState,
    headerTitle: String = "",
    isHeaderLoading: Boolean,
    openAuthentication: () -> Unit,
    profilePictureUri: String,
    isAccountExpired: Boolean,
    @DrawableRes disasterIconResId: Int,
    openIncidentsSelect: () -> Unit,
) {
    val showNavigation = appState.isTopLevelRoute
    val showAppBar = appState.isMenuRoute
    val isFullscreen = appState.isFullscreenRoute
    Scaffold(
        modifier = Modifier.semantics {
            testTagsAsResourceId = true
        },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AnimatedVisibility(
                visible = showAppBar,
                enter = slideIn { IntOffset.Zero },
                exit = slideOut { IntOffset.Zero },
            ) {
                val title = headerTitle.ifBlank {
                    appState.currentTopLevelDestination?.let { destination ->
                        LocalAppTranslator.current(destination.titleTranslateKey)
                    } ?: ""
                }
                val onOpenIncidents = if (appState.isMenuRoute) openIncidentsSelect else null
                AppHeader(
                    modifier = Modifier,
                    title = title,
                    isAppHeaderLoading = isHeaderLoading,
                    profilePictureUri = profilePictureUri,
                    isAccountExpired = isAccountExpired,
                    openAuthentication = openAuthentication,
                    disasterIconResId = disasterIconResId,
                    onOpenIncidents = onOpenIncidents,
                )
            }
        },
        bottomBar = {
            val showBottomBar = showNavigation && appState.shouldShowBottomBar
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideIn { IntOffset.Zero },
                exit = slideOut { IntOffset.Zero },
            ) {
                CrisisCleanupBottomBar(
                    destinations = appState.topLevelDestinations,
                    onNavigateToDestination = appState::navigateToTopLevelDestination,
                    currentDestination = appState.currentDestination,
                    modifier = Modifier.testTag("CrisisCleanupBottomBar"),
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
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                    } else {
                        WindowInsets.safeDrawing
                    },
                ),
        ) {
            if (showNavigation && appState.shouldShowNavRail) {
                CrisisCleanupNavRail(
                    destinations = appState.topLevelDestinations,
                    onNavigateToDestination = appState::navigateToTopLevelDestination,
                    currentDestination = appState.currentDestination,
                    modifier = Modifier
                        .testTag("CrisisCleanupNavRail")
                        .safeDrawingPadding(),
                )
            }

            val isKeyboardOpen = rememberIsKeyboardOpen()
            Column(Modifier.fillMaxSize()) {
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
                        modifier = Modifier.weight(1f),
                        startDestination = appState.lastTopLevelRoute(),
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
}

@Composable
private fun ExpiredAccountAlert(
    snackbarHostState: SnackbarHostState,
    translationCount: Int,
    openAuthentication: () -> Unit,
) {
    val translator = LocalAppTranslator.current
    val message = translator("info.log_in_for_updates")
    val loginText = translator("actions.login", authenticationR.string.login)
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

@OptIn(
    ExperimentalMaterial3Api::class,
)
@Composable
private fun AppHeader(
    modifier: Modifier = Modifier,
    title: String = "",
    isAppHeaderLoading: Boolean = false,
    profilePictureUri: String = "",
    isAccountExpired: Boolean = false,
    openAuthentication: () -> Unit = {},
    @DrawableRes disasterIconResId: Int = 0,
    onOpenIncidents: (() -> Unit)? = null,
) {
    val t = LocalAppTranslator.current
    val actionText = t("actions.account")
    TopAppBarDefault(
        modifier = modifier,
        title = title,
        profilePictureUri = profilePictureUri,
        actionIcon = CrisisCleanupIcons.Account,
        actionText = actionText,
        isActionAttention = isAccountExpired,
        onActionClick = openAuthentication,
        onNavigationClick = null,
        titleContent = @Composable {
            // TODO Match height of visible part of app bar (not the entire app bar)
            if (onOpenIncidents == null) {
                TruncatedAppBarText(title = title)
            } else {
                IncidentDropdownSelect(
                    modifier = Modifier.testTag("appIncidentSelector"),
                    onOpenIncidents,
                    disasterIconResId,
                    title = title,
                    contentDescription = t("nav.change_incident"),
                    isLoading = isAppHeaderLoading,
                )
            }
        },
    )
}

@Composable
private fun TopLevelDestination.Icon(isSelected: Boolean, description: String) {
    val icon = if (isSelected) {
        selectedIcon
    } else {
        unselectedIcon
    }
    when (icon) {
        is ImageVectorIcon -> Icon(
            imageVector = icon.imageVector,
            contentDescription = description,
        )

        is DrawableResourceIcon -> Icon(
            painter = painterResource(id = icon.id),
            contentDescription = description,
        )
    }
}

@Composable
private fun CrisisCleanupNavRail(
    destinations: List<TopLevelDestination>,
    onNavigateToDestination: (TopLevelDestination) -> Unit,
    currentDestination: NavDestination?,
    modifier: Modifier = Modifier,
) {
    val translator = LocalAppTranslator.current
    CrisisCleanupNavigationRail(modifier = modifier) {
        destinations.forEach { destination ->
            val title = translator(destination.titleTranslateKey)
            val selected = currentDestination.isTopLevelDestinationInHierarchy(destination)
            CrisisCleanupNavigationRailItem(
                selected = selected,
                onClick = { onNavigateToDestination(destination) },
                icon = { destination.Icon(selected, title) },
                label = {
                    Text(
                        title,
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
            )
        }
    }
}

@Composable
private fun CrisisCleanupBottomBar(
    destinations: List<TopLevelDestination>,
    onNavigateToDestination: (TopLevelDestination) -> Unit,
    currentDestination: NavDestination?,
    modifier: Modifier = Modifier,
) {
    val translator = LocalAppTranslator.current
    CrisisCleanupNavigationBar(modifier = modifier) {
        destinations.forEach { destination ->
            val title = translator(destination.titleTranslateKey)
            val selected = currentDestination.isTopLevelDestinationInHierarchy(destination)
            CrisisCleanupNavigationBarItem(
                selected = selected,
                onClick = { onNavigateToDestination(destination) },
                icon = { destination.Icon(selected, title) },
                modifier = Modifier.testTag("navItem_$title"),
                label = {
                    Text(
                        title,
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
            )
        }
    }
}

private fun NavDestination?.isTopLevelDestinationInHierarchy(destination: TopLevelDestination) =
    this?.hierarchy?.any {
        it.route?.contains(destination.name, true) ?: false
    } ?: false
