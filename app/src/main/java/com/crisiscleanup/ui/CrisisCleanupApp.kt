package com.crisiscleanup.ui

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.crisiscleanup.AuthState
import com.crisiscleanup.CrisisCleanupApplication
import com.crisiscleanup.MainActivityViewModel
import com.crisiscleanup.R
import com.crisiscleanup.core.appheader.AppHeaderState
import com.crisiscleanup.core.common.NavigationObserver
import com.crisiscleanup.core.data.util.NetworkMonitor
import com.crisiscleanup.core.designsystem.component.*
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.icon.Icon.DrawableResourceIcon
import com.crisiscleanup.core.designsystem.icon.Icon.ImageVectorIcon
import com.crisiscleanup.feature.authentication.AuthenticateScreen
import com.crisiscleanup.feature.cases.navigation.navigateToSelectIncident
import com.crisiscleanup.feature.cases.ui.CasesAction
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

            // TODO Revisit and change the visual to be minimal since operation should be offline capable
            // If user is not connected to the internet show a snack bar to inform them.
            val notConnectedMessage = stringResource(R.string.not_connected)
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
                LoadedContent(snackbarHostState, appState, viewModel, authState)
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
    var isExpiredTokenReminderShown by rememberSaveable { mutableStateOf(false) }

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
        )
    } else {
        val accountData = (authState as AuthState.Authenticated).accountData
        val profilePictureUri by remember { mutableStateOf(accountData.profilePictureUri) }
        val appHeaderBar = viewModel.appHeaderUiState
        val appHeaderState by appHeaderBar.appHeaderState.collectAsStateWithLifecycle()
        val appHeaderTitle by appHeaderBar.title.collectAsStateWithLifecycle()
        val isHeaderLoading by viewModel.showHeaderLoading.collectAsState(
            false
        )

        val appSearchQuery =
            remember(viewModel) { { viewModel.searchManager.searchQuery.value } }
        val updateAppSearchQuery = remember(viewModel) {
            { q: String -> viewModel.searchManager.updateSearchQuery(q) }
        }

        val onCasesAction = remember(viewModel) {
            { casesAction: CasesAction ->
                when (casesAction) {
                    CasesAction.Search -> {
                        val isOnSearch = appHeaderState == AppHeaderState.SearchInTitle
                        val toggleState = if (isOnSearch) AppHeaderState.TopLevel
                        else AppHeaderState.SearchInTitle
                        viewModel.appHeaderUiState.setState(toggleState)
                    }

                    else -> {
                        // TODO Report unhandled cases action
                    }
                }
            }
        }

        val openIncidentsSelect = remember(appHeaderState) {
            { appState.navController.navigateToSelectIncident() }
        }

        NavigableContent(
            snackbarHostState,
            appState,
            appHeaderState,
            appHeaderTitle,
            isHeaderLoading,
            { openAuthentication = true },
            profilePictureUri,
            isAccountExpired,
            openIncidentsSelect,
            onCasesAction,
            appSearchQuery,
            updateAppSearchQuery,
        )

        if (isAccountExpired) {
            if (!isExpiredTokenReminderShown) {
                ExpiredTokenAlert(snackbarHostState) {
                    isExpiredTokenReminderShown = true
                }
            }
        } else {
            isExpiredTokenReminderShown = false
        }
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class,
    ExperimentalLayoutApi::class,
)
@Composable
private fun AuthenticateContent(
    snackbarHostState: SnackbarHostState,
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
        content = { padding ->
            AuthenticateScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumedWindowInsets(padding)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Horizontal
                        )
                    ),
                enableBackHandler = enableBackHandler,
                closeAuthentication = { toggleAuthentication(false) },
                isDebug = CrisisCleanupApplication.isDebuggable,
            )
        }
    )
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
    ExperimentalComposeUiApi::class,
)
@Composable
private fun NavigableContent(
    snackbarHostState: SnackbarHostState,
    appState: CrisisCleanupAppState,
    appHeaderState: AppHeaderState,
    headerTitle: String = "",
    isHeaderLoading: Boolean,
    openAuthentication: () -> Unit,
    profilePictureUri: String,
    isAccountExpired: Boolean,
    openIncidentsSelect: () -> Unit,
    onCasesAction: (CasesAction) -> Unit = { },
    searchQuery: () -> String = { "" },
    onQueryChange: (String) -> Unit = {},
) {
    val showNavBar = !appState.isFullscreenRoute
    // TODO Fix resize jitter when going from nested to top level
    val notDefaultTopBar = appHeaderState == AppHeaderState.None || appState.hasCustomTopBar
    Scaffold(
        modifier = Modifier.semantics {
            testTagsAsResourceId = true
        },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // TODO Profile recompose and optimize if necessary
            val showTopBar = !notDefaultTopBar
            if (showTopBar) {
                val titleResId = appState.currentTopLevelDestination?.titleTextId ?: 0
                val title = if (titleResId != 0) stringResource(titleResId) else headerTitle
                val onOpenIncidents = if (appState.isCasesRoute) openIncidentsSelect else null
                // TODO Back when search is showing should dismiss search
                AppHeader(
                    modifier = Modifier.testTag("CrisisCleanupAppHeader"),
                    title = title,
                    appHeaderState = appHeaderState,
                    isAppHeaderLoading = isHeaderLoading,
                    profilePictureUri = profilePictureUri,
                    isAccountExpired = isAccountExpired,
                    openAuthentication = openAuthentication,
                    onOpenIncidents = onOpenIncidents,
                    searchQuery = searchQuery,
                    onQueryChange = onQueryChange,
                )
            }
        },
        bottomBar = {
            if (showNavBar && appState.shouldShowBottomBar) {
                CrisisCleanupBottomBar(
                    destinations = appState.topLevelDestinations,
                    onNavigateToDestination = appState::navigateToTopLevelDestination,
                    currentDestination = appState.currentDestination,
                    modifier = Modifier.testTag("CrisisCleanupBottomBar")
                )
            }
        },
    ) { padding ->
        Row(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumedWindowInsets(padding)
                .windowInsetsPadding(
                    if (notDefaultTopBar) WindowInsets.safeDrawing
                    else WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                )
        ) {
            if (showNavBar && appState.shouldShowNavRail) {
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
                CrisisCleanupNavHost(
                    navController = appState.navController,
                    onBackClick = appState::onBackClick,
                    onCasesAction = onCasesAction,
                )
            }

            // TODO: We may want to add padding or spacer when the snackbar is shown so that
            //  content doesn't display behind it.
        }
    }
}

@Composable
private fun ExpiredTokenAlert(
    snackbarHostState: SnackbarHostState,
    onSnackbarShown: () -> Unit,
) {
    val message = stringResource(R.string.expired_token_message)
    LaunchedEffect(Unit) {
        snackbarHostState.showSnackbar(
            message,
            withDismissAction = true,
            duration = SnackbarDuration.Indefinite,
        )
        onSnackbarShown()
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class
)
@Composable
private fun AppHeader(
    modifier: Modifier = Modifier,
    @StringRes titleRes: Int = 0,
    title: String = "",
    appHeaderState: AppHeaderState = AppHeaderState.TopLevel,
    isAppHeaderLoading: Boolean = false,
    profilePictureUri: String = "",
    isAccountExpired: Boolean = false,
    openAuthentication: () -> Unit = {},
    onOpenIncidents: (() -> Unit)? = null,
    searchQuery: () -> String = { "" },
    onQueryChange: (String) -> Unit = {},
) {
    when (appHeaderState) {
        AppHeaderState.TopLevel -> {
            TopAppBarDefault(
                modifier = modifier,
                titleResId = titleRes,
                title = title,
                profilePictureUri = profilePictureUri,
                actionIcon = CrisisCleanupIcons.Account,
                actionResId = R.string.account,
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

        AppHeaderState.SearchInTitle -> {
            val keyboardController = LocalSoftwareKeyboardController.current

            TopAppBarSearch(
                modifier = modifier,
                q = searchQuery,
                onQueryChange = onQueryChange,
                onSearch = { keyboardController?.hide() },
            )
        }

        AppHeaderState.BackTitleAction -> {
            CrisisCleanupTopAppBar(
                titleResId = titleRes,
                title = title,
            )
        }

        else -> {}
    }
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

private fun NavDestination?.isTopLevelDestinationInHierarchy(destination: TopLevelDestination) =
    this?.hierarchy?.any {
        it.route?.contains(destination.name, true) ?: false
    } ?: false
