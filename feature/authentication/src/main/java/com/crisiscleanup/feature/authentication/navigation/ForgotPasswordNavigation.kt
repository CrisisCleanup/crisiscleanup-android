package com.crisiscleanup.feature.authentication.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.crisiscleanup.core.appnav.RouteConstant
import com.crisiscleanup.feature.authentication.ui.ForgotPasswordRoute

fun NavController.navigateToForgotPassword(navOptions: NavOptions? = null) {
    this.navigate(RouteConstant.forgotPasswordRoute, navOptions)
}

fun NavGraphBuilder.forgotPasswordScreen(
    onBack: () -> Unit,
) {
    composable(route = RouteConstant.forgotPasswordRoute) {
        ForgotPasswordRoute(
            onBack,
        )
    }
}
