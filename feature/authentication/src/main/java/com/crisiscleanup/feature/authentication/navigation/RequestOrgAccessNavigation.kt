package com.crisiscleanup.feature.authentication.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.crisiscleanup.core.appnav.RouteConstant.requestAccessRoute
import com.crisiscleanup.feature.authentication.ui.RequestOrgAccessRoute

internal const val inviteCodeArg = "inviteCode"
internal const val showEmailInputArg = "showEmailInput"

internal class RequestOrgAccessArgs(
    val inviteCode: String?,
    val showEmailInput: Boolean? = false,
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        savedStateHandle.get<String>(inviteCodeArg),
        savedStateHandle[showEmailInputArg],
    )
}

fun NavController.navigateToRequestAccess(code: String) {
    val route = "$requestAccessRoute?$inviteCodeArg=$code"
    navigate(route)
}

fun NavGraphBuilder.requestAccessScreen(
    onBack: () -> Unit,
) {
    composable(
        route = "$requestAccessRoute?$inviteCodeArg={$inviteCodeArg}",
        arguments = listOf(
            navArgument(inviteCodeArg) {},
        ),
    ) {
        RequestOrgAccessRoute(
            onBack = onBack,
        )
    }
}
