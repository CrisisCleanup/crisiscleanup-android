package com.crisiscleanup.feature.dashboard.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

const val dashboardRoute = "dashboard_route"

fun NavController.navigateToDashboard(navOptions: NavOptions? = null) {
    this.navigate(dashboardRoute, navOptions)
}

fun NavGraphBuilder.casesScreen() {
    composable(route = dashboardRoute) {
//        DashboardRoute()
    }
}
