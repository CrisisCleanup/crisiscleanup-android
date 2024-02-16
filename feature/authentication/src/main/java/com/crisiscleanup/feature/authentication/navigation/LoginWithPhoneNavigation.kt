package com.crisiscleanup.feature.authentication.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.crisiscleanup.core.appnav.RouteConstant
import com.crisiscleanup.feature.authentication.ui.LoginWithPhoneRoute

fun NavController.navigateToLoginWithPhone() {
    navigate(RouteConstant.loginWithPhoneRoute)
}

fun NavGraphBuilder.loginWithPhoneScreen(
    onBack: () -> Unit,
    onAuthenticated: () -> Unit,
    closeAuthentication: () -> Unit,
) {
    composable(route = RouteConstant.loginWithPhoneRoute) {
        LoginWithPhoneRoute(
            onBack = onBack,
            onAuthenticated = onAuthenticated,
            closeAuthentication = closeAuthentication,
        )
    }
}
