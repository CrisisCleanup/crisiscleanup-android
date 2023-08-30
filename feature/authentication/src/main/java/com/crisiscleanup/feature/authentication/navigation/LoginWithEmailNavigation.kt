package com.crisiscleanup.feature.authentication.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.crisiscleanup.core.appnav.RouteConstant
import com.crisiscleanup.core.appnav.RouteConstant.loginWithEmailRoute
import com.crisiscleanup.feature.authentication.ui.AuthRoute

fun NavController.navigateToLoginWithEmail() {
    this.navigate(RouteConstant.loginWithEmailRoute)
}

fun NavGraphBuilder.loginWithEmailScreen(
    enableBackHandler: Boolean,
    onBack: () -> Unit,
    closeAuthentication: () -> Unit,
    openForgotPassword: () -> Unit,
    openEmailMagicLink: () -> Unit,
) {
    composable(route = loginWithEmailRoute) {
        AuthRoute(
            onBack = onBack,
            enableBackHandler = enableBackHandler,
            openForgotPassword = openForgotPassword,
            openEmailMagicLink = openEmailMagicLink,
            closeAuthentication = closeAuthentication,
        )
    }
}
