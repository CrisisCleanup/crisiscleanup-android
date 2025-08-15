package com.crisiscleanup.feature.authentication.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.crisiscleanup.core.appnav.RouteConstant.REQUEST_ACCESS_ROUTE
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

fun NavController.navigateToRequestAccess(code: String) {
    val route = "$REQUEST_ACCESS_ROUTE?$INVITE_CODE_ARG=$code"
    navigate(route)
}

fun NavGraphBuilder.requestAccessScreen(
    onBack: () -> Unit,
    closeRequestAccess: () -> Unit,
    openAuth: () -> Unit,
    openForgotPassword: () -> Unit,
) {
    composable(
        route = "$REQUEST_ACCESS_ROUTE?$INVITE_CODE_ARG={$INVITE_CODE_ARG}",
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
