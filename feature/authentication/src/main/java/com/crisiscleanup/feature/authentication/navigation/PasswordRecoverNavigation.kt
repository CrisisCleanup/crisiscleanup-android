package com.crisiscleanup.feature.authentication.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.crisiscleanup.core.appnav.RouteConstant.emailLoginLinkRoute
import com.crisiscleanup.core.appnav.RouteConstant.forgotPasswordRoute
import com.crisiscleanup.core.appnav.RouteConstant.resetPasswordRoute
import com.crisiscleanup.feature.authentication.ui.PasswordRecoverRoute
import com.crisiscleanup.feature.authentication.ui.ResetPasswordRoute

fun NavController.navigateToForgotPassword() {
    navigate(forgotPasswordRoute)
}

fun NavController.navigateToEmailLoginLink() {
    navigate(emailLoginLinkRoute)
}

fun NavController.navigateToPasswordReset() {
    navigate(resetPasswordRoute)
}

fun NavGraphBuilder.forgotPasswordScreen(
    onBack: () -> Unit,
) {
    composable(route = forgotPasswordRoute) {
        PasswordRecoverRoute(
            onBack,
            showForgotPassword = true,
            showMagicLink = true,
        )
    }
}

fun NavGraphBuilder.emailLoginLinkScreen(
    onBack: () -> Unit,
) {
    composable(route = emailLoginLinkRoute) {
        PasswordRecoverRoute(
            onBack,
            showMagicLink = true,
        )
    }
}

fun NavGraphBuilder.resetPasswordScreen(
    onBack: () -> Unit,
    closeResetPassword: () -> Unit,
) {
    composable(route = resetPasswordRoute) {
        ResetPasswordRoute(
            onBack = onBack,
            closeResetPassword = closeResetPassword,
        )
    }
}