package com.crisiscleanup.feature.authentication.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.crisiscleanup.core.appnav.RouteConstant.ACCOUNT_TRANSFER_ORG_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.AUTH_REQUEST_ACCESS_ROUTE
import com.crisiscleanup.feature.authentication.ui.RequestOrgAccessRoute

internal const val INVITE_CODE_ARG = "inviteCode"
internal const val SHOW_EMAIL_INPUT_ARG = "showEmailInput"

internal class RequestOrgAccessArgs(
    val inviteCode: String?,
    val showEmailInput: Boolean? = false,
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        savedStateHandle.get<String>(INVITE_CODE_ARG),
        savedStateHandle[SHOW_EMAIL_INPUT_ARG],
    )
}

private fun getOrgAccessRoute(isAuthenticated: Boolean) = if (isAuthenticated) {
    ACCOUNT_TRANSFER_ORG_ROUTE
} else {
    AUTH_REQUEST_ACCESS_ROUTE
}

fun NavController.navigateToRequestAccess(code: String, isAuthenticated: Boolean) {
    val orgAccessRoute = getOrgAccessRoute(isAuthenticated)
    val route = "$orgAccessRoute?$INVITE_CODE_ARG=$code"
    navigate(route)
}

fun NavGraphBuilder.requestAccessScreen(
    isAuthenticated: Boolean,
    onBack: () -> Unit,
    closeRequestAccess: () -> Unit,
    openAuth: () -> Unit,
    openForgotPassword: () -> Unit,
) {
    val orgAccessRoute = getOrgAccessRoute(isAuthenticated)
    composable(
        route = "$orgAccessRoute?$INVITE_CODE_ARG={$INVITE_CODE_ARG}",
        arguments = listOf(
            navArgument(INVITE_CODE_ARG) {},
        ),
    ) {
        RequestOrgAccessRoute(
            onBack = onBack,
            closeRequestAccess = closeRequestAccess,
            openAuth = openAuth,
            openForgotPassword = openForgotPassword,
        )
    }
}
