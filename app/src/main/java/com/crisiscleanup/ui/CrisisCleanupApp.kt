package com.crisiscleanup.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.crisiscleanup.R
import com.crisiscleanup.core.data.util.NetworkMonitor
import com.crisiscleanup.designsystem.component.CrisisCleanupNavigationBar
import com.crisiscleanup.designsystem.component.CrisisCleanupNavigationBarItem
import com.crisiscleanup.designsystem.component.CrisisCleanupNavigationRail
import com.crisiscleanup.designsystem.component.CrisisCleanupNavigationRailItem
import com.crisiscleanup.designsystem.icon.Icon.DrawableResourceIcon
import com.crisiscleanup.designsystem.icon.Icon.ImageVectorIcon
import com.crisiscleanup.navigation.CrisisCleanupNavHost
import com.crisiscleanup.navigation.TopLevelDestination

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
    ExperimentalComposeUiApi::class,
    ExperimentalLifecycleComposeApi::class
)
@Composable
fun CrisisCleanupApp(
    windowSizeClass: WindowSizeClass,
    networkMonitor: NetworkMonitor,
    appState: CrisisCleanupAppState = rememberCrisisCleanupAppState(
        networkMonitor = networkMonitor,
        windowSizeClass = windowSizeClass
    ),
) {
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .matchParentSize()
                .fillMaxWidth()
        ) {
            Text(
                text = "Say something",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, start = 16.dp, end = 16.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = {
                    throw RuntimeException("Testing crashes")
                },
                modifier = Modifier.padding(48.dp),
            ) {
                Text(text = "Crash me")
            }
        }
    }

//    Box(Modifier.fillMaxSize()) {
//
//        val snackbarHostState = remember { SnackbarHostState() }
//
//        val isOffline by appState.isOffline.collectAsStateWithLifecycle()
//
//        // If user is not connected to the internet show a snack bar to inform them.
//        val notConnectedMessage = stringResource(R.string.not_connected)
//        LaunchedEffect(isOffline) {
//            if (isOffline) snackbarHostState.showSnackbar(
//                message = notConnectedMessage,
//                duration = SnackbarDuration.Indefinite
//            )
//        }
//
//        // TODO Implement/copy when needed
////        if (appState.shouldShowSettingsDialog) {
////            SettingsDialog(
////                onDismiss = { appState.setShowSettingsDialog(false) }
////            )
////        }
//
//        Scaffold(
//            modifier = Modifier.semantics {
//                testTagsAsResourceId = true
//            },
//            containerColor = Color.Transparent,
//            contentColor = MaterialTheme.colorScheme.onBackground,
//            contentWindowInsets = WindowInsets(0, 0, 0, 0),
//            snackbarHost = { SnackbarHost(snackbarHostState) },
//            bottomBar = {
//                if (appState.shouldShowBottomBar) {
//                    CrisisCleanupBottomBar(
//                        destinations = appState.topLevelDestinations,
//                        onNavigateToDestination = appState::navigateToTopLevelDestination,
//                        currentDestination = appState.currentDestination,
//                        modifier = Modifier.testTag("CrisisCleanupBottomBar")
//                    )
//                }
//            }
//        ) { padding ->
//            Row(
//                Modifier
//                    .fillMaxSize()
//                    .padding(padding)
//                    .consumedWindowInsets(padding)
//                    .windowInsetsPadding(
//                        WindowInsets.safeDrawing.only(
//                            WindowInsetsSides.Horizontal
//                        )
//                    )
//            ) {
//                if (appState.shouldShowNavRail) {
//                    CrisisCleanupNavRail(
//                        destinations = appState.topLevelDestinations,
//                        onNavigateToDestination = appState::navigateToTopLevelDestination,
//                        currentDestination = appState.currentDestination,
//                        modifier = Modifier
//                            .testTag("CrisisCleanupNavRail")
//                            .safeDrawingPadding()
//                    )
//                }
//
//                Column(Modifier.fillMaxSize()) {
//                    // Show the top app bar on top level destinations.
////                    val destination = appState.currentTopLevelDestination
////                    if (destination != null) {
////                        CrisisCleanupTopAppBar(
////                            titleRes = destination.titleTextId,
////                            actionIcon = CrisisCleanupIcons.Settings,
////                            actionIconContentDescription = stringResource(
////                                id = settingsR.string.top_app_bar_action_icon_description
////                            ),
////                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
////                                containerColor = Color.Transparent
////                            ),
////                            onActionClick = { appState.setShowSettingsDialog(true) }
////                        )
////                    }
//
//                    CrisisCleanupNavHost(
//                        navController = appState.navController,
//                        onBackClick = appState::onBackClick
//                    )
//                }
//
//                // TODO: We may want to add padding or spacer when the snackbar is shown so that
//                //  content doesn't display behind it.
//            }
//        }
//    }
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

private fun NavDestination?.isTopLevelDestinationInHierarchy(destination: TopLevelDestination) =
    this?.hierarchy?.any {
        it.route?.contains(destination.name, true) ?: false
    } ?: false
