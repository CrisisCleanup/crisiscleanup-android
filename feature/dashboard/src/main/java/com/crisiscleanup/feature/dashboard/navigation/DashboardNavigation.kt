package com.crisiscleanup.feature.dashboard.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.crisiscleanup.core.appnav.RouteConstant.DASHBOARD_ROUTE
import com.crisiscleanup.feature.dashboard.DashboardRoute

fun NavController.navigateToDashboard(navOptions: NavOptions? = null) {
    this.navigate(DASHBOARD_ROUTE, navOptions)
}

fun NavGraphBuilder.dashboardScreen() {
    composable(route = DASHBOARD_ROUTE) {
        DashboardRoute()
    }
}
