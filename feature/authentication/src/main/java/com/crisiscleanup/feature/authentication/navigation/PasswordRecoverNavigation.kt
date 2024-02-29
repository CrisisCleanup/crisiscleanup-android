package com.crisiscleanup.feature.authentication.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.crisiscleanup.core.appnav.RouteConstant.ACCOUNT_RESET_PASSWORD_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.AUTH_RESET_PASSWORD_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.EMAIL_LOGIN_LINK_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.FORGOT_PASSWORD_ROUTE
import com.crisiscleanup.feature.authentication.ui.PasswordRecoverRoute
import com.crisiscleanup.feature.authentication.ui.ResetPasswordRoute

fun NavController.navigateToForgotPassword() {
    navigate(FORGOT_PASSWORD_ROUTE)
}

fun NavController.navigateToEmailLoginLink() {
    navigate(EMAIL_LOGIN_LINK_ROUTE)
}

fun NavController.navigateToPasswordReset(isAuthenticated: Boolean) {
    val resetPasswordRoute =
        if (isAuthenticated) ACCOUNT_RESET_PASSWORD_ROUTE else AUTH_RESET_PASSWORD_ROUTE
    navigate(resetPasswordRoute)
}

fun NavGraphBuilder.forgotPasswordScreen(
    onBack: () -> Unit,
) {
    composable(route = FORGOT_PASSWORD_ROUTE) {
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
    composable(route = EMAIL_LOGIN_LINK_ROUTE) {
        PasswordRecoverRoute(
            onBack,
            showMagicLink = true,
        )
    }
}

fun NavGraphBuilder.resetPasswordScreen(
    isAuthenticated: Boolean,
    onBack: () -> Unit,
    closeResetPassword: () -> Unit,
) {
    val resetPasswordRoute =
        if (isAuthenticated) ACCOUNT_RESET_PASSWORD_ROUTE else AUTH_RESET_PASSWORD_ROUTE
    composable(route = resetPasswordRoute) {
        ResetPasswordRoute(
            onBack = onBack,
            closeResetPassword = closeResetPassword,
        )
    }
}
