package com.crisiscleanup.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.crisiscleanup.core.appnav.RouteConstant.AUTH_GRAPH_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.AUTH_ROUTE
import com.crisiscleanup.feature.authentication.navigation.authGraph
import com.crisiscleanup.feature.authentication.navigation.emailLoginLinkScreen
import com.crisiscleanup.feature.authentication.navigation.forgotPasswordScreen
import com.crisiscleanup.feature.authentication.navigation.loginWithEmailScreen
import com.crisiscleanup.feature.authentication.navigation.loginWithPhoneScreen
import com.crisiscleanup.feature.authentication.navigation.magicLinkLoginScreen
import com.crisiscleanup.feature.authentication.navigation.navigateToEmailLoginLink
import com.crisiscleanup.feature.authentication.navigation.navigateToForgotPassword
import com.crisiscleanup.feature.authentication.navigation.navigateToLoginWithEmail
import com.crisiscleanup.feature.authentication.navigation.navigateToLoginWithPhone
import com.crisiscleanup.feature.authentication.navigation.navigateToVolunteerOrg
import com.crisiscleanup.feature.authentication.navigation.navigateToVolunteerPasteInviteLink
import com.crisiscleanup.feature.authentication.navigation.navigateToVolunteerRequestAccess
import com.crisiscleanup.feature.authentication.navigation.navigateToVolunteerScanQrCode
import com.crisiscleanup.feature.authentication.navigation.orgPersistentInviteScreen
import com.crisiscleanup.feature.authentication.navigation.requestAccessScreen
import com.crisiscleanup.feature.authentication.navigation.resetPasswordScreen
import com.crisiscleanup.feature.authentication.navigation.volunteerOrgScreen

private fun NavController.popToAuth() {
    popBackStack(AUTH_ROUTE, false, saveState = false)
}

@Composable
fun CrisisCleanupAuthNavHost(
    navController: NavHostController,
    enableBackHandler: Boolean,
    closeAuthentication: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    startDestination: String = AUTH_GRAPH_ROUTE,
) {
    val navToAuth = navController::popToAuth
    val navToLoginWithEmail =
        remember(navController) {
            {
                navController.popToAuth()
                navController.navigateToLoginWithEmail()
            }
        }
    val navToLoginWithPhone = navController::navigateToLoginWithPhone
    val navToVolunteerOrg = navController::navigateToVolunteerOrg
    val navToForgotPassword = navController::navigateToForgotPassword
    val navToEmailMagicLink = navController::navigateToEmailLoginLink
    val navToLoginWithPhoneClearStack = remember(navController) {
        {
            navController.popToAuth()
            navController.navigateToLoginWithPhone()
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        authGraph(
            nestedGraphs = {
                loginWithEmailScreen(
                    onBack = onBack,
                    onAuthenticated = navToAuth,
                    closeAuthentication = closeAuthentication,
                    openForgotPassword = navToForgotPassword,
                    openEmailMagicLink = navToEmailMagicLink,
                    openPhoneLogin = navToLoginWithPhoneClearStack,
                    nestedGraphs = {
                        forgotPasswordScreen(
                            onBack = onBack,
                        )
                        emailLoginLinkScreen(
                            onBack = onBack,
                        )
                    },
                )
                loginWithPhoneScreen(
                    onBack = onBack,
                    onAuthenticated = navToAuth,
                    closeAuthentication = closeAuthentication,
                )
                volunteerOrgScreen(
                    navController = navController,
                    nestedGraphs = {
                        navigateToVolunteerPasteInviteLink(navController, onBack)
                        navigateToVolunteerRequestAccess(
                            onBack,
                            closeRequestAccess = navToLoginWithEmail,
                        )
                        navigateToVolunteerScanQrCode(onBack)
                    },
                    onBack = onBack,
                )
                resetPasswordScreen(
                    isAuthenticated = false,
                    onBack = onBack,
                    closeResetPassword = navToLoginWithEmail,
                )
                magicLinkLoginScreen(
                    onBack = onBack,
                    onAuthenticated = navToAuth,
                    closeAuthentication = closeAuthentication,
                )
                requestAccessScreen(
                    onBack = onBack,
                    closeRequestAccess = navToLoginWithEmail,
                )
                orgPersistentInviteScreen(
                    onBack = onBack,
                    closeInvite = navToLoginWithEmail,
                )
            },
            enableBackHandler = enableBackHandler,
            openLoginWithEmail = navToLoginWithEmail,
            openLoginWithPhone = navToLoginWithPhone,
            openVolunteerOrg = navToVolunteerOrg,
            closeAuthentication = closeAuthentication,
        )
    }
}
