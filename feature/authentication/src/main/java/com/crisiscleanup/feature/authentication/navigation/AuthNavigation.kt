package com.crisiscleanup.feature.authentication.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.crisiscleanup.core.appnav.RouteConstant.authGraphRoutePattern
import com.crisiscleanup.core.appnav.RouteConstant.authRoute
import com.crisiscleanup.feature.authentication.ui.AuthRoute

fun NavController.navigateToAuth(navOptions: NavOptions? = null) {
    this.navigate(authGraphRoutePattern, navOptions)
}

fun NavGraphBuilder.authGraph(
    nestedGraphs: NavGraphBuilder.() -> Unit,
    enableBackHandler: Boolean,
    closeAuthentication: () -> Unit,
    openForgotPassword: () -> Unit,
) {
    navigation(
        route = authGraphRoutePattern,
        startDestination = authRoute,
    ) {
        composable(route = authRoute) {
            AuthRoute(
                enableBackHandler = enableBackHandler,
                openForgotPassword = openForgotPassword,
                closeAuthentication = closeAuthentication,
            )
        }

        nestedGraphs()
    }
}
