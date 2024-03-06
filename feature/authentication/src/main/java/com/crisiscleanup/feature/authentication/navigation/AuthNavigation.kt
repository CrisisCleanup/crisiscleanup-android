package com.crisiscleanup.feature.authentication.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.crisiscleanup.core.appnav.RouteConstant.AUTH_GRAPH_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.AUTH_ROUTE
import com.crisiscleanup.feature.authentication.ui.RootAuthRoute

fun NavGraphBuilder.authGraph(
    nestedGraphs: NavGraphBuilder.() -> Unit,
    enableBackHandler: Boolean = false,
    openLoginWithEmail: () -> Unit = {},
    openLoginWithPhone: () -> Unit = {},
    openVolunteerOrg: () -> Unit = {},
    closeAuthentication: () -> Unit = {},
) {
    navigation(
        route = AUTH_GRAPH_ROUTE,
        startDestination = AUTH_ROUTE,
    ) {
        composable(route = AUTH_ROUTE) {
            RootAuthRoute(
                enableBackHandler = enableBackHandler,
                openLoginWithEmail = openLoginWithEmail,
                openLoginWithPhone = openLoginWithPhone,
                openVolunteerOrg = openVolunteerOrg,
                closeAuthentication = closeAuthentication,
            )
        }

        nestedGraphs()
    }
}
