package com.crisiscleanup.ui

import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import androidx.tracing.trace
import com.crisiscleanup.core.data.util.NetworkMonitor
import com.crisiscleanup.core.ui.TrackDisposableJank
import com.crisiscleanup.feature.cases.navigation.casesRoute
import com.crisiscleanup.feature.cases.navigation.navigateToCases
import com.crisiscleanup.feature.cases.navigation.selectIncidentRoute
import com.crisiscleanup.feature.dashboard.navigation.dashboardRoute
import com.crisiscleanup.feature.dashboard.navigation.navigateToDashboard
import com.crisiscleanup.feature.menu.navigation.menuRoute
import com.crisiscleanup.feature.menu.navigation.navigateToMenu
import com.crisiscleanup.feature.team.navigation.navigateToTeam
import com.crisiscleanup.feature.team.navigation.teamRoute
import com.crisiscleanup.navigation.TopLevelDestination
import com.crisiscleanup.navigation.TopLevelDestination.CASES
import com.crisiscleanup.navigation.TopLevelDestination.DASHBOARD
import com.crisiscleanup.navigation.TopLevelDestination.MENU
import com.crisiscleanup.navigation.TopLevelDestination.TEAM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Composable
fun rememberCrisisCleanupAppState(
    windowSizeClass: WindowSizeClass,
    networkMonitor: NetworkMonitor,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    navController: NavHostController = rememberNavController()
): CrisisCleanupAppState {
    NavigationTrackingSideEffect(navController)
    return remember(navController, coroutineScope, windowSizeClass, networkMonitor) {
        CrisisCleanupAppState(navController, coroutineScope, windowSizeClass, networkMonitor)
    }
}

@Stable
class CrisisCleanupAppState(
    val navController: NavHostController,
    val coroutineScope: CoroutineScope,
    val windowSizeClass: WindowSizeClass,
    networkMonitor: NetworkMonitor,
) {
    val currentDestination: NavDestination?
        @Composable get() = navController
            .currentBackStackEntryAsState().value?.destination

    val currentTopLevelDestination: TopLevelDestination?
        @Composable get() = when (currentDestination?.route) {
            casesRoute -> CASES
            dashboardRoute -> DASHBOARD
            teamRoute -> TEAM
            menuRoute -> MENU
            else -> null
        }

    /**
     * Routes (not top level) that should show app header.
     */
    private val showHeaderRoutes = setOf(
        selectIncidentRoute
    )
    val shouldShowHeader: Boolean
        @Composable
        get() = currentTopLevelDestination != null ||
                showHeaderRoutes.contains(currentDestination?.route)

    val shouldShowBottomBar: Boolean
        get() = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact ||
                windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact

    val shouldShowNavRail: Boolean
        get() = !shouldShowBottomBar

    val isOffline = networkMonitor.isOnline
        .map(Boolean::not)
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    /**
     * Map of top level destinations to be used in the TopBar, BottomBar and NavRail. The key is the
     * route.
     */
    val topLevelDestinations: List<TopLevelDestination> = TopLevelDestination.values().asList()

    /**
     * UI logic for navigating to a top level destination in the app. Top level destinations have
     * only one copy of the destination of the back stack, and save and restore state whenever you
     * navigate to and from it.
     *
     * @param topLevelDestination: The destination the app needs to navigate to.
     */
    fun navigateToTopLevelDestination(topLevelDestination: TopLevelDestination) {
        trace("Navigation: ${topLevelDestination.name}") {
            val topLevelNavOptions = navOptions {
                // Pop up to the start destination of the graph to
                // avoid building up a large stack of destinations
                // on the back stack as users select items
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                // Avoid multiple copies of the same destination when
                // reselecting the same item
                launchSingleTop = true
                // Restore state when reselecting a previously selected item
                restoreState = true
            }

            when (topLevelDestination) {
                CASES -> navController.navigateToCases(topLevelNavOptions)
                DASHBOARD -> navController.navigateToDashboard(topLevelNavOptions)
                TEAM -> navController.navigateToTeam(topLevelNavOptions)
                MENU -> navController.navigateToMenu(topLevelNavOptions)
            }
        }
    }

    fun onBackClick() {
        navController.popBackStack()
    }
}

/**
 * Stores information about navigation events to be used with JankStats
 */
@Composable
private fun NavigationTrackingSideEffect(navController: NavHostController) {
    TrackDisposableJank(navController) { metricsHolder ->
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            metricsHolder.state?.putState("Navigation", destination.route.toString())
        }

        navController.addOnDestinationChangedListener(listener)

        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }
}
