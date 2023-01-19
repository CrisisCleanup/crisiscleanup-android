package com.crisiscleanup.feature.cases.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.crisiscleanup.feature.cases.CasesRoute

const val casesGraphRoutePattern = "cases_graph"
// This cannot be used as the navHost startDestination
const val casesRoute = "cases_route"

fun NavController.navigateToCases(navOptions: NavOptions? = null) {
    this.navigate(casesGraphRoutePattern, navOptions)
}

fun NavGraphBuilder.casesGraph(
    nestedGraphs: NavGraphBuilder.() -> Unit
) {
    navigation(
        route = casesGraphRoutePattern,
        startDestination = casesRoute,
    ) {
        composable(route = casesRoute) {
            CasesRoute()
        }

        nestedGraphs()
    }
}