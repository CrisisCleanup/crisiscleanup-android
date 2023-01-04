package com.crisiscleanup.feature.dashboard.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.crisiscleanup.feature.dashboard.DashboardRoute

const val dashboardRoute = "dashboard_route"

fun NavController.navigateToDashboard(navOptions: NavOptions? = null) {
    this.navigate(dashboardRoute, navOptions)
}

fun NavGraphBuilder.dashboardScreen() {
    composable(route = dashboardRoute) {
        DashboardRoute()
    }
}
