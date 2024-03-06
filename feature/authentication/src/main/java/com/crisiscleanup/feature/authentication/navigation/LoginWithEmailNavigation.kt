package com.crisiscleanup.feature.authentication.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.crisiscleanup.core.appnav.RouteConstant.LOGIN_WITH_EMAIL_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.MAGIC_LINK_ROUTE
import com.crisiscleanup.feature.authentication.ui.LoginWithEmailRoute
import com.crisiscleanup.feature.authentication.ui.MagicLinkLoginRoute

fun NavController.navigateToLoginWithEmail() {
    navigate(LOGIN_WITH_EMAIL_ROUTE)
}

fun NavController.navigateToMagicLinkLogin() {
    navigate(MAGIC_LINK_ROUTE)
}

fun NavGraphBuilder.loginWithEmailScreen(
    nestedGraphs: NavGraphBuilder.() -> Unit,
    onBack: () -> Unit,
    onAuthenticated: () -> Unit,
    closeAuthentication: () -> Unit,
    openForgotPassword: () -> Unit,
    openEmailMagicLink: () -> Unit,
) {
    composable(route = LOGIN_WITH_EMAIL_ROUTE) {
        LoginWithEmailRoute(
            onBack = onBack,
            onAuthenticated = onAuthenticated,
            closeAuthentication = closeAuthentication,
            openForgotPassword = openForgotPassword,
            openEmailMagicLink = openEmailMagicLink,
        )
    }
    nestedGraphs()
}

fun NavGraphBuilder.magicLinkLoginScreen(
    onBack: () -> Unit,
    onAuthenticated: () -> Unit,
    closeAuthentication: () -> Unit,
) {
    composable(route = MAGIC_LINK_ROUTE) {
        MagicLinkLoginRoute(
            onBack = onBack,
            onAuthenticated = onAuthenticated,
            closeAuthentication = closeAuthentication,
        )
    }
}
