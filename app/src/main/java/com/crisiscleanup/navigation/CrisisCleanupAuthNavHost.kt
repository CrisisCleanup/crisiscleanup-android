package com.crisiscleanup.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.crisiscleanup.core.appnav.RouteConstant
import com.crisiscleanup.feature.authentication.navigation.authGraph
import com.crisiscleanup.feature.authentication.navigation.forgotPasswordScreen
import com.crisiscleanup.feature.authentication.navigation.navigateToForgotPassword

@Composable
fun CrisisCleanupAuthNavHost(
    navController: NavHostController,
    enableBackHandler: Boolean,
    closeAuthentication: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    startDestination: String = RouteConstant.authGraphRoutePattern,
) {
    val navToForgotPassword =
        remember(navController) { { navController.navigateToForgotPassword() } }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        authGraph(
            nestedGraphs = {
                forgotPasswordScreen(onBack)
            },
            enableBackHandler = enableBackHandler,
            closeAuthentication = closeAuthentication,
            openForgotPassword = navToForgotPassword,
        )
    }
}
