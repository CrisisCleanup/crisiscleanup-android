package com.crisiscleanup.feature.syncinsights.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.crisiscleanup.core.appnav.RouteConstant
import com.crisiscleanup.feature.syncinsights.ui.SyncInsightsRoute

fun NavController.navigateToSyncInsights(navOptions: NavOptions? = null) {
    this.navigate(RouteConstant.syncInsightsRoute, navOptions)
}

fun NavGraphBuilder.syncInsightsScreen(
    openCase: (Long, Long) -> Boolean = { _, _ -> false },
) {
    composable(route = RouteConstant.syncInsightsRoute) {
        SyncInsightsRoute(openCase = openCase)
    }
}