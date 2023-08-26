package com.crisiscleanup.feature.authentication.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.crisiscleanup.core.appnav.RouteConstant.authGraphRoutePattern
import com.crisiscleanup.core.appnav.RouteConstant.authRoute
import com.crisiscleanup.feature.authentication.ui.AuthRoute

fun NavGraphBuilder.authGraph(
    nestedGraphs: NavGraphBuilder.() -> Unit,
    enableBackHandler: Boolean,
    closeAuthentication: () -> Unit,
    openForgotPassword: () -> Unit,
    openEmailMagicLink: () -> Unit,
) {
    navigation(
        route = authGraphRoutePattern,
        startDestination = authRoute,
    ) {
        composable(route = authRoute) {
            AuthRoute(
                enableBackHandler = enableBackHandler,
                openForgotPassword = openForgotPassword,
                openEmailMagicLink = openEmailMagicLink,
                closeAuthentication = closeAuthentication,
            )
        }

        nestedGraphs()
    }
}
