package com.crisiscleanup.feature.menu.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.crisiscleanup.core.appnav.RouteConstant.menuRoute
import com.crisiscleanup.feature.menu.MenuRoute

fun NavController.navigateToMenu(navOptions: NavOptions? = null) {
    this.navigate(menuRoute, navOptions)
}

fun NavGraphBuilder.menuScreen(
    openUserFeedback: () -> Unit = {},
    openSyncLogs: () -> Unit = {},
) {
    composable(route = menuRoute) {
        MenuRoute(
            openUserFeedback = openUserFeedback,
            openSyncLogs = openSyncLogs,
        )
    }
}
