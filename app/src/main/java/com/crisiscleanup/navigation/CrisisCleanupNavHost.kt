package com.crisiscleanup.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.crisiscleanup.feature.cases.navigation.casesGraph
import com.crisiscleanup.feature.cases.navigation.casesGraphRoutePattern
import com.crisiscleanup.feature.cases.ui.CasesAction
import com.crisiscleanup.feature.dashboard.navigation.dashboardScreen
import com.crisiscleanup.feature.menu.navigation.menuScreen
import com.crisiscleanup.feature.team.navigation.teamScreen

/**
 * Top-level navigation graph. Navigation is organized as explained at
 * https://d.android.com/jetpack/compose/nav-adaptive
 *
 * The navigation graph defined in this file defines the different top level routes. Navigation
 * within each route is handled using state and Back Handlers.
 */
@Composable
fun CrisisCleanupNavHost(
    navController: NavHostController,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    startDestination: String = casesGraphRoutePattern,
    onCasesAction: (CasesAction) -> Unit = { },
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        casesGraph(
            navController = navController,
            nestedGraphs = {
                // TODO Nested composables that can be navigated to
            },
            onCasesAction = onCasesAction,
        )
        dashboardScreen()
        teamScreen()
        menuScreen()
    }
}
