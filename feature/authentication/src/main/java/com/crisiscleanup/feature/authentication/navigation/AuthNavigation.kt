package com.crisiscleanup.feature.authentication.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.crisiscleanup.core.appnav.RouteConstant.authGraphRoutePattern
import com.crisiscleanup.core.appnav.RouteConstant.authRoute
import com.crisiscleanup.feature.authentication.ui.AuthRoute
import com.crisiscleanup.feature.authentication.ui.RootAuthRoute

fun NavGraphBuilder.authGraph(
    nestedGraphs: NavGraphBuilder.() -> Unit,
    openLoginWithEmail: () -> Unit = {},
) {
    navigation(
        route = authGraphRoutePattern,
        startDestination = authRoute,
    ) {
        composable(route = authRoute) {
            RootAuthRoute(
                openLoginWithEmail = openLoginWithEmail,
            )
        }

        nestedGraphs()
    }
}
