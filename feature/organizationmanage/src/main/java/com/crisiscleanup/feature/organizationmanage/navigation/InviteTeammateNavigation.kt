package com.crisiscleanup.feature.organizationmanage.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.crisiscleanup.core.appnav.RouteConstant
import com.crisiscleanup.feature.organizationmanage.ui.InviteTeammateRoute

fun NavController.navigateToInviteTeammate(navOptions: NavOptions? = null) {
    this.navigate(RouteConstant.INVITE_TEAMMATE_ROUTE, navOptions)
}

fun NavGraphBuilder.inviteTeammateScreen(
    onBack: () -> Unit = {},
) {
    composable(route = RouteConstant.INVITE_TEAMMATE_ROUTE) {
        InviteTeammateRoute(onBack = onBack)
    }
}
