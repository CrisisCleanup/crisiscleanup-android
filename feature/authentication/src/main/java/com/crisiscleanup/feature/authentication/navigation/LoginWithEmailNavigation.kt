package com.crisiscleanup.feature.authentication.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.crisiscleanup.core.appnav.RouteConstant.loginWithEmailRoute
import com.crisiscleanup.feature.authentication.ui.LoginWithEmailRoute

fun NavController.navigateToLoginWithEmail() {
    this.navigate(loginWithEmailRoute)
}

fun NavGraphBuilder.loginWithEmailScreen(
    nestedGraphs: NavGraphBuilder.() -> Unit,
    onBack: () -> Unit,
    openForgotPassword: () -> Unit,
    openEmailMagicLink: () -> Unit,
) {
    composable(route = loginWithEmailRoute) {
        LoginWithEmailRoute(
            onBack = onBack,
            openForgotPassword = openForgotPassword,
            openEmailMagicLink = openEmailMagicLink,
        )
    }
    nestedGraphs()
}
