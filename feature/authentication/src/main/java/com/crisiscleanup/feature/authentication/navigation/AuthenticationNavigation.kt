package com.crisiscleanup.feature.authentication.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.crisiscleanup.feature.authentication.AuthenticateRoute

const val authenticateRoute = "authenticate_route"

fun NavController.navigateToLogin(navOptions: NavOptions? = null) {
    this.navigate(authenticateRoute, navOptions)
}

fun NavGraphBuilder.authenticateScreen() {
    composable(route = authenticateRoute) {
        AuthenticateRoute()
    }
}