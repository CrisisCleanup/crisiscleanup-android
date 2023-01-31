package com.crisiscleanup.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.crisiscleanup.AuthState
import com.crisiscleanup.CrisisCleanupApplication
import com.crisiscleanup.MainActivityViewModel
import com.crisiscleanup.R
import com.crisiscleanup.core.appheader.AppHeaderState
import com.crisiscleanup.core.data.util.NetworkMonitor
import com.crisiscleanup.core.designsystem.component.*
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.icon.Icon.DrawableResourceIcon
import com.crisiscleanup.core.designsystem.icon.Icon.ImageVectorIcon
import com.crisiscleanup.feature.authentication.AuthenticateScreen
import com.crisiscleanup.feature.cases.CasesViewModel
import com.crisiscleanup.feature.cases.navigation.navigateToSelectIncident
import com.crisiscleanup.feature.cases.ui.CasesAction
import com.crisiscleanup.navigation.CrisisCleanupNavHost
import com.crisiscleanup.navigation.TopLevelDestination
import kotlinx.datetime.Clock

@OptIn(
    ExperimentalLifecycleComposeApi::class,
)
@Composable
fun CrisisCleanupApp(
    windowSizeClass: WindowSizeClass,
    networkMonitor: NetworkMonitor,
    appState: CrisisCleanupAppState = rememberCrisisCleanupAppState(
        networkMonitor = networkMonitor,
        windowSizeClass = windowSizeClass
    ),
    mainActivityViewModel: MainActivityViewModel = hiltViewModel(),
    casesViewModel: CasesViewModel = hiltViewModel(),
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

            val authState by mainActivityViewModel.authState.collectAsStateWithLifecycle()
            var openAuthentication by rememberSaveable { mutableStateOf(false) }
            if (authState is AuthState.Loading) {
                // Splash screen should be showing
            } else if (openAuthentication || authState !is AuthState.Authenticated) {
                AuthenticateContent(
                    snackbarHostState,
                    toggleAuthentication = { b -> openAuthentication = b },
                )
            } else {
                val accountData = (authState as AuthState.Authenticated).accountData
                val profilePictureUri by remember { mutableStateOf(accountData.profilePictureUri) }
                val isAccountExpired by remember {
                    derivedStateOf { accountData.tokenExpiry < Clock.System.now() }
                }
                val appHeaderBar = mainActivityViewModel.appHeaderUiState
                val appHeaderState by appHeaderBar.appHeaderState.collectAsStateWithLifecycle()
                val appHeaderTitle by appHeaderBar.title.collectAsStateWithLifecycle()

                val onCasesAction = remember(mainActivityViewModel) {
                    { casesAction: CasesAction ->
                        when (casesAction) {
                            CasesAction.Search -> {
                                val isOnSearch = appHeaderState == AppHeaderState.SearchCases
                                val toggleState = if (isOnSearch) AppHeaderState.Default
                                else AppHeaderState.SearchCases
                                mainActivityViewModel.appHeaderUiState.setState(toggleState)
                            }

                            else -> {}
                        }
                    }
                }
                val casesSearchQuery =
                    remember(casesViewModel) { { casesViewModel.casesSearchQuery.value } }
                val updateCasesSearchQuery = remember(casesViewModel) {
                    { q: String -> casesViewModel.updateCasesSearchQuery(q) }
                }

                val openIncidentsSelect = remember(appHeaderState) {
                    {
                        if (appHeaderState == AppHeaderState.Default) {
                            appState.navController.navigateToSelectIncident()
                        }
                    }
                }

                NavigableContent(
                    snackbarHostState,
                    appState,
                    appHeaderState,
                    appHeaderTitle,
                    { openAuthentication = true },
                    profilePictureUri,
                    isAccountExpired,
                    openIncidentsSelect,
                    onCasesAction,
                    casesSearchQuery,
                    updateCasesSearchQuery,
                )
            }
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
    openAuthentication: () -> Unit,
    profilePictureUri: String,
    isAccountExpired: Boolean,
    onHeaderNavClick: () -> Unit,
    onCasesAction: (CasesAction) -> Unit = { },
    searchQuery: () -> String = { "" },
    onQueryChange: (String) -> Unit = {},
) {
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
            val showTopBar = appHeaderState != AppHeaderState.None && !appState.shouldHideHeader
            if (showTopBar) {
                val titleResId = appState.currentTopLevelDestination?.titleTextId ?: 0
                val title = if (titleResId != 0) stringResource(titleResId) else headerTitle
                AppHeader(
                    modifier = Modifier.testTag("CrisisCleanupAppHeader"),
                    title = title,
                    appHeaderState = appHeaderState,
                    profilePictureUri = profilePictureUri,
                    isAccountExpired = isAccountExpired,
                    openAuthentication = openAuthentication,
                    onNavClick = onHeaderNavClick,
                    searchQuery = searchQuery,
                    onQueryChange = onQueryChange,
                )
            }
        },
        bottomBar = {
            if (appState.shouldShowBottomBar) {
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
                    if (appHeaderState == AppHeaderState.None) WindowInsets.safeDrawing
                    else WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                )
        ) {
            if (appState.shouldShowNavRail) {
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
                    // TODO Back when search or table view is showing should go back?
                    onBackClick = appState::onBackClick,
                    onCasesAction = onCasesAction,
                )
            }

            // TODO: We may want to add padding or spacer when the snackbar is shown so that
            //  content doesn't display behind it.
        }
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
    appHeaderState: AppHeaderState = AppHeaderState.Default,
    profilePictureUri: String = "",
    isAccountExpired: Boolean = false,
    openAuthentication: () -> Unit = {},
    onNavClick: () -> Unit = {},
    searchQuery: () -> String = { "" },
    onQueryChange: (String) -> Unit = {},
) {
    when (appHeaderState) {
        AppHeaderState.Default -> {
            TopAppBarDefault(
                modifier = modifier,
                titleResId = titleRes,
                title = title,
                profilePictureUri = profilePictureUri,
                actionIcon = CrisisCleanupIcons.Account,
                actionResId = R.string.account,
                isActionAttention = isAccountExpired,
                onActionClick = openAuthentication,
                onNavigationClick = onNavClick,
            )
        }

        AppHeaderState.SearchCases -> {
            val keyboardController = LocalSoftwareKeyboardController.current

            TopAppBarSearch(
                modifier = modifier,
                q = searchQuery,
                onQueryChange = onQueryChange,
                onSearch = { keyboardController?.hide() },
            )
        }

        AppHeaderState.TitleActions -> {
            CrisisCleanupTopAppBar(
                titleResId = titleRes,
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
