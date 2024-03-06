package com.crisiscleanup.feature.organizationmanage.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.crisiscleanup.core.appnav.RouteConstant.INVITE_TEAMMATE_ROUTE
import com.crisiscleanup.feature.organizationmanage.ui.InviteTeammateRoute

fun NavController.navigateToInviteTeammate(navOptions: NavOptions? = null) {
    this.navigate(INVITE_TEAMMATE_ROUTE, navOptions)
}

fun NavGraphBuilder.inviteTeammateScreen(
    onBack: () -> Unit = {},
) {
    composable(route = INVITE_TEAMMATE_ROUTE) {
        InviteTeammateRoute(onBack = onBack)
    }
}
