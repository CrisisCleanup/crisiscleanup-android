package com.crisiscleanup.feature.authentication.navigation

import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.crisiscleanup.core.appnav.RouteConstant
import com.crisiscleanup.core.appnav.RouteConstant.volunteerRequestAccessRoute
import com.crisiscleanup.feature.authentication.ui.RequestOrgAccessRoute
import com.crisiscleanup.feature.authentication.ui.VolunteerOrgRoute
import com.crisiscleanup.feature.authentication.ui.VolunteerPasteInviteLinkRoute

fun NavController.navigateToVolunteerOrg() {
    navigate(RouteConstant.volunteerOrgRoute)
}

fun NavController.navigateToVolunteerPasteInviteLink() {
    navigate(RouteConstant.volunteerPasteInviteLinkRoute)
}

fun NavController.navigateToVolunteerRequestAccess() {
    val route = "$volunteerRequestAccessRoute?$showEmailInputArg=true"
    navigate(route)
}

fun NavController.navigateToVolunteerScanQrCode() {
    navigate(RouteConstant.volunteerScanQrCodeRoute)
}

fun NavGraphBuilder.volunteerOrgScreen(
    navController: NavHostController,
    nestedGraphs: NavGraphBuilder.() -> Unit,
    onBack: () -> Unit,
) {
    composable(route = RouteConstant.volunteerOrgRoute) {
        val navToPasteOrgInviteLink =
            remember(navController) { { navController.navigateToVolunteerPasteInviteLink() } }
        val navToRequestOrgAccess =
            remember(navController) { { navController.navigateToVolunteerRequestAccess() } }
        val navToScanOrgQrCode =
            remember(navController) { { navController.navigateToVolunteerScanQrCode() } }

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
    composable(route = RouteConstant.volunteerPasteInviteLinkRoute) {
        val navigateToRequestAccess =
            remember(navController) { { code: String -> navController.navigateToRequestAccess(code) } }
        VolunteerPasteInviteLinkRoute(
            onBack = onBack,
            openOrgInvite = navigateToRequestAccess,
        )
    }
}

fun NavGraphBuilder.navigateToVolunteerRequestAccess(
    onBack: () -> Unit,
) {
    composable(
        route = "$volunteerRequestAccessRoute?$showEmailInputArg={$showEmailInputArg}",
        arguments = listOf(
            navArgument(showEmailInputArg) {
                type = NavType.BoolType
            },
        ),
    ) {
        RequestOrgAccessRoute(
            onBack = onBack,
        )
    }
}

fun NavGraphBuilder.navigateToVolunteerScanQrCode(
    onBack: () -> Unit,
) {
    composable(route = RouteConstant.volunteerScanQrCodeRoute) {
        VolunteerScanQrCodeRoute(
            onBack = onBack,
        )
    }
}
