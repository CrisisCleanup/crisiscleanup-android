package com.crisiscleanup.feature.authentication.navigation

import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.crisiscleanup.core.appnav.RouteConstant
import com.crisiscleanup.core.appnav.RouteConstant.VOLUNTEER_REQUEST_ACCESS_ROUTE
import com.crisiscleanup.feature.authentication.ui.RequestOrgAccessRoute
import com.crisiscleanup.feature.authentication.ui.VolunteerOrgRoute
import com.crisiscleanup.feature.authentication.ui.VolunteerPasteInviteLinkRoute
import com.crisiscleanup.feature.authentication.ui.VolunteerScanQrCodeRoute

fun NavController.navigateToVolunteerOrg() {
    navigate(RouteConstant.VOLUNTEER_ORG_ROUTE)
}

fun NavController.navigateToVolunteerPasteInviteLink() {
    navigate(RouteConstant.VOLUNTEER_PASTE_INVITE_LINK_ROUTE)
}

fun NavController.navigateToVolunteerRequestAccess() {
    val route = "$VOLUNTEER_REQUEST_ACCESS_ROUTE?$SHOW_EMAIL_INPUT_ARG=true"
    navigate(route)
}

fun NavController.navigateToVolunteerScanQrCode() {
    navigate(RouteConstant.VOLUNTEER_SCAN_QR_CODE_ROUTE)
}

fun NavGraphBuilder.volunteerOrgScreen(
    navController: NavHostController,
    nestedGraphs: NavGraphBuilder.() -> Unit,
    onBack: () -> Unit,
) {
    composable(route = RouteConstant.VOLUNTEER_ORG_ROUTE) {
        val navToPasteOrgInviteLink = navController::navigateToVolunteerPasteInviteLink
        val navToRequestOrgAccess = navController::navigateToVolunteerRequestAccess
        val navToScanOrgQrCode = navController::navigateToVolunteerScanQrCode

        VolunteerOrgRoute(
            onBack = onBack,
            openPasteOrgInviteLink = navToPasteOrgInviteLink,
            openRequestOrgAccess = navToRequestOrgAccess,
            openScanOrgQrCode = navToScanOrgQrCode,
        )
    }

    nestedGraphs()
}

fun NavGraphBuilder.navigateToVolunteerPasteInviteLink(
    navController: NavHostController,
    onBack: () -> Unit,
) {
    composable(route = RouteConstant.VOLUNTEER_PASTE_INVITE_LINK_ROUTE) {
        val navigateToRequestAccess =
            remember(navController) {
                { code: String ->
                    navController.popBackStack()
                    navController.navigateToRequestAccess(code, false)
                }
            }
        VolunteerPasteInviteLinkRoute(
            onBack = onBack,
            openOrgInvite = navigateToRequestAccess,
        )
    }
}

fun NavGraphBuilder.navigateToVolunteerRequestAccess(
    onBack: () -> Unit,
    closeRequestAccess: () -> Unit,
) {
    composable(
        route = "$VOLUNTEER_REQUEST_ACCESS_ROUTE?$SHOW_EMAIL_INPUT_ARG={$SHOW_EMAIL_INPUT_ARG}",
        arguments = listOf(
            navArgument(SHOW_EMAIL_INPUT_ARG) {
                type = NavType.BoolType
            },
        ),
    ) {
        RequestOrgAccessRoute(
            onBack = onBack,
            closeRequestAccess = closeRequestAccess,
        )
    }
}

fun NavGraphBuilder.navigateToVolunteerScanQrCode(
    onBack: () -> Unit,
) {
    composable(route = RouteConstant.VOLUNTEER_SCAN_QR_CODE_ROUTE) {
        VolunteerScanQrCodeRoute(
            onBack = onBack,
        )
    }
}
