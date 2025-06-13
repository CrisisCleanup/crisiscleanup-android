package com.crisiscleanup.feature.menu.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.crisiscleanup.core.appnav.RouteConstant.MENU_ROUTE
import com.crisiscleanup.core.appnav.sharedViewModel
import com.crisiscleanup.feature.menu.MenuViewModel
import com.crisiscleanup.feature.menu.ui.MenuRoute

fun NavController.navigateToMenu(navOptions: NavOptions? = null) {
    this.navigate(MENU_ROUTE, navOptions)
}

fun NavGraphBuilder.menuScreen(
    navController: NavController,
    openAuthentication: () -> Unit = {},
    openInviteTeammate: () -> Unit = {},
    openRequestRedeploy: () -> Unit = {},
    openUserFeedback: () -> Unit = {},
    openLists: () -> Unit = {},
    openIncidentCache: () -> Unit = {},
    openSyncLogs: () -> Unit = {},
) {
    composable(route = MENU_ROUTE) { backStackEntry ->
        val viewModel = backStackEntry.sharedViewModel<MenuViewModel>(navController, MENU_ROUTE)
        MenuRoute(
            viewModel,
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
