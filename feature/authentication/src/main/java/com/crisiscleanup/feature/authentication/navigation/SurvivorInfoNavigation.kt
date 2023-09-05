package com.crisiscleanup.feature.authentication.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.crisiscleanup.core.appnav.RouteConstant.survivorInfoRoute
import com.crisiscleanup.feature.authentication.ui.SurvivorInfoRoute

fun NavController.navigateToSurvivorInfo() {
    this.navigate(survivorInfoRoute)
}

fun NavGraphBuilder.survivorInfoScreen(
    enableBackHandler: Boolean,
    onBack: () -> Unit,
    closeAuthentication: () -> Unit,
    nestedGraphs: NavGraphBuilder.() -> Unit,
) {
    composable(route = survivorInfoRoute) {
        SurvivorInfoRoute(
            enableBackHandler = enableBackHandler,
            onBack = onBack,
            closeAuthentication = closeAuthentication,
        )
    }
    nestedGraphs()
}
