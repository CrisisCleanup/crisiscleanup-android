package com.crisiscleanup.feature.menu.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.crisiscleanup.core.appnav.RouteConstant.MENU_ROUTE
import com.crisiscleanup.feature.menu.MenuRoute

fun NavController.navigateToMenu(navOptions: NavOptions? = null) {
    this.navigate(MENU_ROUTE, navOptions)
}

fun NavGraphBuilder.menuScreen(
    openAuthentication: () -> Unit = {},
    openInviteTeammate: () -> Unit = {},
    openUserFeedback: () -> Unit = {},
    openSyncLogs: () -> Unit = {},
) {
    composable(route = MENU_ROUTE) {
        MenuRoute(
            openAuthentication = openAuthentication,
            openInviteTeammate = openInviteTeammate,
            openUserFeedback = openUserFeedback,
            openSyncLogs = openSyncLogs,
        )
    }
}
