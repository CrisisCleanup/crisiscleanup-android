package com.crisiscleanup.feature.qrcode.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.crisiscleanup.core.appnav.RouteConstant.TEAM_SCAN_QR_CODE_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.VOLUNTEER_SCAN_QR_CODE_ROUTE
import com.crisiscleanup.feature.qrcode.ui.ScanQrCodeRoute

internal const val IS_JOIN_TEAM_ARG = "isJoinTeam"

internal class ScanQrCodeArgs(val isJoinTeam: Boolean) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        savedStateHandle.get<Boolean>(IS_JOIN_TEAM_ARG) ?: false,
    )
}

fun NavController.navigateToTeamQrCode() {
    navigate("$TEAM_SCAN_QR_CODE_ROUTE?$IS_JOIN_TEAM_ARG=true")
}

fun NavGraphBuilder.navigateToVolunteerScanQrCode(
    onBack: () -> Unit,
) {
    composable(
        route = VOLUNTEER_SCAN_QR_CODE_ROUTE,
        arguments = listOf(
            navArgument(IS_JOIN_TEAM_ARG) {
                type = NavType.BoolType
                defaultValue = false
            },
        ),
    ) {
        ScanQrCodeRoute(
            onBack = onBack,
        )
    }
}

fun NavGraphBuilder.navigateToTeamScanQrCode(
    onBack: () -> Unit,
) {
    composable(
        route = TEAM_SCAN_QR_CODE_ROUTE,
        arguments = listOf(
            navArgument(IS_JOIN_TEAM_ARG) {
                type = NavType.BoolType
                defaultValue = true
            },
        ),
    ) {
        ScanQrCodeRoute(
            onBack = onBack,
        )
    }
}
