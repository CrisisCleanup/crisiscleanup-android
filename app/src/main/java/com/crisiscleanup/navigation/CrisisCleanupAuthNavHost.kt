package com.crisiscleanup.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.crisiscleanup.core.appnav.RouteConstant
import com.crisiscleanup.feature.authentication.navigation.authGraph
import com.crisiscleanup.feature.authentication.navigation.emailLoginLinkScreen
import com.crisiscleanup.feature.authentication.navigation.forgotPasswordScreen
import com.crisiscleanup.feature.authentication.navigation.loginWithEmailScreen
import com.crisiscleanup.feature.authentication.navigation.navigateToEmailLoginLink
import com.crisiscleanup.feature.authentication.navigation.navigateToForgotPassword
import com.crisiscleanup.feature.authentication.navigation.navigateToLoginWithEmail

@Composable
fun CrisisCleanupAuthNavHost(
    navController: NavHostController,
    enableBackHandler: Boolean,
    closeAuthentication: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    startDestination: String = RouteConstant.authGraphRoutePattern,
) {
    val navToLoginWithEmail =
        remember(navController) { { navController.navigateToLoginWithEmail() } }
    val navToForgotPassword =
        remember(navController) { { navController.navigateToForgotPassword() } }
    val navToEmailMagicLink =
        remember(navController) { { navController.navigateToEmailLoginLink() } }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        authGraph(
            nestedGraphs = {
                loginWithEmailScreen(
                    onBack = onBack,
                    closeAuthentication = closeAuthentication,
                    openForgotPassword = navToForgotPassword,
                    openEmailMagicLink = navToEmailMagicLink,
                    nestedGraphs = {
                        forgotPasswordScreen(
                            onBack = onBack,
                        )
                        emailLoginLinkScreen(
                            onBack = onBack,
                        )
                    },
                )
            },
            enableBackHandler = enableBackHandler,
            openLoginWithEmail = navToLoginWithEmail,
            closeAuthentication = closeAuthentication,
        )
    }
}
