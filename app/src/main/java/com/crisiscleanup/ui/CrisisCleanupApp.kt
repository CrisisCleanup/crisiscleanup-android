package com.crisiscleanup.ui

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.crisiscleanup.R
import com.crisiscleanup.core.common.NavigationObserver
import com.crisiscleanup.core.common.NetworkMonitor
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
import com.crisiscleanup.feature.authentication.AuthenticateScreen
import com.crisiscleanup.feature.cases.navigation.navigateToSelectIncident
import com.crisiscleanup.navigation.CrisisCleanupNavHost
import com.crisiscleanup.navigation.TopLevelDestination
import com.crisiscleanup.feature.cases.R as casesR

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
            val appTranslator = remember(viewModel) {
                AppTranslator(translator = viewModel.translator)
            }
            val notConnectedMessage = remember(translationCount) {
                appTranslator.translator(
                    "info.not_connected_to_internet",
                    R.string.not_connected_to_internet,
                )
            }
            LaunchedEffect(isOffline) {
                if (isOffline) snackbarHostState.showSnackbar(
                    message = notConnectedMessage,
                    duration = SnackbarDuration.Indefinite,
                    withDismissAction = true,
                )
            }

            val authState by viewModel.authState.collectAsStateWithLifecycle()
            if (authState is AuthState.Loading) {
                // Splash screen should be showing
            } else {
                CompositionLocalProvider(LocalAppTranslator provides appTranslator) {
                    LoadedContent(snackbarHostState, appState, viewModel, authState)
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
) {
    val isAccountExpired by viewModel.isAccessTokenExpired

    val isNotAuthenticatedState = authState !is AuthState.Authenticated
    var openAuthentication by rememberSaveable { mutableStateOf(isNotAuthenticatedState) }
    if (openAuthentication || isNotAuthenticatedState) {
        val toggleAuthentication = remember(authState) {
            { open: Boolean -> openAuthentication = open }
        }
        AuthenticateContent(
            snackbarHostState,
            !isNotAuthenticatedState,
            toggleAuthentication,
            viewModel.isDebuggable,
        )
    } else {
        val accountData = (authState as AuthState.Authenticated).accountData
        val profilePictureUri by remember { mutableStateOf(accountData.profilePictureUri) }
        val appHeaderBar = viewModel.appHeaderUiState
        val appHeaderTitle by appHeaderBar.title.collectAsStateWithLifecycle()
        val isHeaderLoading by viewModel.showHeaderLoading.collectAsState(false)

        val openIncidentsSelect = remember(viewModel) {
            { appState.navController.navigateToSelectIncident() }
        }

        NavigableContent(
            snackbarHostState,
            appState,
            appHeaderTitle,
            isHeaderLoading,
            { openAuthentication = true },
            profilePictureUri,
            isAccountExpired,
            openIncidentsSelect,
        )

        if (isAccountExpired) {
            ExpiredTokenAlert(snackbarHostState) { openAuthentication = true }
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
    enableBackHandler: Boolean,
    toggleAuthentication: (Boolean) -> Unit,
    isDebuggable: Boolean = false,
) {
    Scaffold(
        modifier = Modifier.semantics {
            testTagsAsResourceId = true
        },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = WindowInsets.systemBars,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        content = { padding ->
            AuthenticateScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Horizontal
                        )
                    ),
                enableBackHandler = enableBackHandler,
                closeAuthentication = { toggleAuthentication(false) },
                isDebug = isDebuggable,
            )
        }
    )
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
                val titleResId = appState.currentTopLevelDestination?.titleTextId ?: 0
                val title = if (titleResId != 0) stringResource(titleResId) else headerTitle
                val onOpenIncidents = if (appState.isMenuRoute) openIncidentsSelect else null
                AppHeader(
                    modifier = Modifier.testTag("CrisisCleanupAppHeader"),
                    title = title,
                    isAppHeaderLoading = isHeaderLoading,
                    profilePictureUri = profilePictureUri,
                    isAccountExpired = isAccountExpired,
                    openAuthentication = openAuthentication,
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
                    modifier = Modifier.testTag("CrisisCleanupBottomBar")
                )
            }

            if (!showBottomBar) {
                val windowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Bottom)
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(windowInsets)
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
                    if (isFullscreen) WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                    else WindowInsets.safeDrawing
                )
        ) {
            if (showNavigation && appState.shouldShowNavRail) {
                CrisisCleanupNavRail(
                    destinations = appState.topLevelDestinations,
                    onNavigateToDestination = appState::navigateToTopLevelDestination,
                    currentDestination = appState.currentDestination,
                    modifier = Modifier
                        .testTag("CrisisCleanupNavRail")
                        .safeDrawingPadding()
                )
            }

            Column(Modifier.fillMaxSize()) {
                val snackbarAreaHeight =
                    if (!showNavigation && snackbarHostState.currentSnackbarData != null) 64.dp else 0.dp

                CompositionLocalProvider(
                    LocalAppLayout provides AppLayoutArea(snackbarHostState)
                ) {
                    CrisisCleanupNavHost(
                        navController = appState.navController,
                        onBack = appState::onBack,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(
                    Modifier
                        .height(snackbarAreaHeight)
                        .animateContentSize()
                )
            }
        }
    }
}

@Composable
private fun ExpiredTokenAlert(
    snackbarHostState: SnackbarHostState,
    openAuthentication: () -> Unit,
) {
    val translator = LocalAppTranslator.current.translator
    val message = translator("account.login_reminder", R.string.login_reminder)
    val loginText = translator("actions.login", R.string.login)
    LaunchedEffect(Unit) {
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
    @StringRes titleRes: Int = 0,
    title: String = "",
    isAppHeaderLoading: Boolean = false,
    profilePictureUri: String = "",
    isAccountExpired: Boolean = false,
    openAuthentication: () -> Unit = {},
    onOpenIncidents: (() -> Unit)? = null,
) {
    val actionText = LocalAppTranslator.current.translator("actions.account", R.string.account)
    TopAppBarDefault(
        modifier = modifier,
        titleResId = titleRes,
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
                Row(
                    modifier = modifier.clickable(onClick = onOpenIncidents),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TruncatedAppBarText(title = title)
                    Icon(
                        imageVector = CrisisCleanupIcons.ArrowDropDown,
                        contentDescription = stringResource(casesR.string.change_incident),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    AnimatedVisibility(
                        visible = isAppHeaderLoading,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        CircularProgressIndicator(
                            modifier
                                .size(48.dp)
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun CrisisCleanupNavRail(
    destinations: List<TopLevelDestination>,
    onNavigateToDestination: (TopLevelDestination) -> Unit,
    currentDestination: NavDestination?,
    modifier: Modifier = Modifier,
) {
    CrisisCleanupNavigationRail(modifier = modifier) {
        destinations.forEach { destination ->
            val selected = currentDestination.isTopLevelDestinationInHierarchy(destination)
            CrisisCleanupNavigationRailItem(
                selected = selected,
                onClick = { onNavigateToDestination(destination) },
                icon = {
                    val icon = if (selected) {
                        destination.selectedIcon
                    } else {
                        destination.unselectedIcon
                    }
                    when (icon) {
                        is ImageVectorIcon -> Icon(
                            imageVector = icon.imageVector,
                            contentDescription = null
                        )

                        is DrawableResourceIcon -> Icon(
                            painter = painterResource(id = icon.id),
                            contentDescription = null
                        )
                    }
                },
                label = { Text(stringResource(destination.iconTextId)) }
            )
        }
    }
}

@Composable
private fun CrisisCleanupBottomBar(
    destinations: List<TopLevelDestination>,
    onNavigateToDestination: (TopLevelDestination) -> Unit,
    currentDestination: NavDestination?,
    modifier: Modifier = Modifier
) {
    CrisisCleanupNavigationBar(
        modifier = modifier
    ) {
        destinations.forEach { destination ->
            val selected = currentDestination.isTopLevelDestinationInHierarchy(destination)
            CrisisCleanupNavigationBarItem(
                selected = selected,
                onClick = { onNavigateToDestination(destination) },
                icon = {
                    val icon = if (selected) destination.selectedIcon
                    else destination.unselectedIcon
                    val description = stringResource(destination.iconTextId)
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
                },
                label = { Text(stringResource(destination.iconTextId)) }
            )
        }
    }
}

private fun NavDestination?.isTopLevelDestinationInHierarchy(destination: TopLevelDestination) =
    this?.hierarchy?.any {
        it.route?.contains(destination.name, true) ?: false
    } ?: false
