package com.crisiscleanup.feature.menu.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.crisiscleanup.core.appnav.RouteConstant.MENU_ROUTE
import com.crisiscleanup.feature.menu.ui.MenuRoute

fun NavController.navigateToMenu(navOptions: NavOptions? = null) {
    navigate(MENU_ROUTE, navOptions)
}

fun NavGraphBuilder.menuScreen(
    openAuthentication: () -> Unit = {},
    openInviteTeammate: () -> Unit = {},
    openRequestRedeploy: () -> Unit = {},
    openUserFeedback: () -> Unit = {},
    openLists: () -> Unit = {},
    openIncidentCache: () -> Unit = {},
    openSyncLogs: () -> Unit = {},
) {
    composable(route = MENU_ROUTE) {
        MenuRoute(
            openAuthentication = openAuthentication,
            openInviteTeammate = openInviteTeammate,
            openRequestRedeploy = openRequestRedeploy,
            openUserFeedback = openUserFeedback,
            openLists = openLists,
            openIncidentCache = openIncidentCache,
            openSyncLogs = openSyncLogs,
        )
    }
}
