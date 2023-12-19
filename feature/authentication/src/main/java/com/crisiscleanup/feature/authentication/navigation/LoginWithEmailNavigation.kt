package com.crisiscleanup.feature.authentication.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.crisiscleanup.core.appnav.RouteConstant
import com.crisiscleanup.core.appnav.RouteConstant.loginWithEmailRoute
import com.crisiscleanup.feature.authentication.ui.LoginWithEmailRoute
import com.crisiscleanup.feature.authentication.ui.MagicLinkLoginRoute

fun NavController.navigateToLoginWithEmail() {
    navigate(loginWithEmailRoute)
}

fun NavController.navigateToMagicLinkLogin() {
    navigate(RouteConstant.magicLinkLoginRoute)
}

fun NavGraphBuilder.loginWithEmailScreen(
    nestedGraphs: NavGraphBuilder.() -> Unit,
    onBack: () -> Unit,
    closeAuthentication: () -> Unit,
    openForgotPassword: () -> Unit,
    openEmailMagicLink: () -> Unit,
) {
    composable(route = loginWithEmailRoute) {
        LoginWithEmailRoute(
            onBack = onBack,
            closeAuthentication = closeAuthentication,
            openForgotPassword = openForgotPassword,
            openEmailMagicLink = openEmailMagicLink,
        )
    }
    nestedGraphs()
}

fun NavGraphBuilder.magicLinkLoginScreen(
    onBack: () -> Unit,
    closeAuthentication: () -> Unit,
) {
    composable(route = RouteConstant.magicLinkLoginRoute) {
        MagicLinkLoginRoute(
            onBack = onBack,
            closeAuthentication = closeAuthentication,
        )
    }
}
