package com.crisiscleanup.feature.authentication.navigation

import androidx.compose.runtime.remember
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.crisiscleanup.core.appnav.RouteConstant.authRoute
import com.crisiscleanup.core.appnav.RouteConstant.requestAccessRoute
import com.crisiscleanup.feature.authentication.ui.RequestOrgAccessRoute

internal const val inviteCodeArg = "inviteCode"

internal class RequestOrgAccessArgs(val inviteCode: String) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle.get<String>(inviteCodeArg)),
    )
}

fun NavController.navigateToRequestAccess(code: String) {
    val route = "$requestAccessRoute?$inviteCodeArg=$code"
    navigate(route)
}

fun NavGraphBuilder.requestAccessScreen(
    navController: NavController,
) {
    composable(
        route = "$requestAccessRoute?$inviteCodeArg={$inviteCodeArg}",
        arguments = listOf(
            navArgument(inviteCodeArg) {},
        ),
    ) {
        val popNavigation = remember(navController) {
            {
                navController.popBackStack(authRoute, false)
                Unit
            }
        }
        RequestOrgAccessRoute(
            onBack = popNavigation,
        )
    }
}
