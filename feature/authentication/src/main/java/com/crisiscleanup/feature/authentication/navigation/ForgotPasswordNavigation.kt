package com.crisiscleanup.feature.authentication.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.crisiscleanup.core.appnav.RouteConstant.emailLoginLinkRoute
import com.crisiscleanup.core.appnav.RouteConstant.forgotPasswordRoute
import com.crisiscleanup.feature.authentication.ui.ForgotPasswordRoute

fun NavController.navigateToForgotPassword() {
    this.navigate(forgotPasswordRoute)
}

fun NavController.navigateToEmailLoginLink() {
    this.navigate(emailLoginLinkRoute)
}

fun NavGraphBuilder.forgotPasswordScreen(
    onBack: () -> Unit,
) {
    composable(route = forgotPasswordRoute) {
        ForgotPasswordRoute(
            onBack,
        )
    }
}

fun NavGraphBuilder.emailLoginLinkScreen(
    onBack: () -> Unit,
) {
    composable(route = emailLoginLinkRoute) {
        ForgotPasswordRoute(
            onBack,
            isMagicLinkOnly = true,
        )
    }
}
