package com.crisiscleanup.feature.organizationmanage.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.crisiscleanup.core.appnav.RouteConstant.REQUEST_REDEPLOY_ROUTE
import com.crisiscleanup.feature.organizationmanage.ui.RequestRedeployRoute

fun NavController.navigateToRequestRedeploy(navOptions: NavOptions? = null) {
    this.navigate(REQUEST_REDEPLOY_ROUTE, navOptions)
}

fun NavGraphBuilder.requestRedeployScreen(
    onBack: () -> Unit = {},
) {
    composable(route = REQUEST_REDEPLOY_ROUTE) {
        RequestRedeployRoute(onBack = onBack)
    }
}
