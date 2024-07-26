package com.crisiscleanup.feature.team.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.crisiscleanup.core.appnav.RouteConstant.TEAM_ROUTE
import com.crisiscleanup.feature.team.TeamsRoute

fun NavController.navigateToTeam(navOptions: NavOptions? = null) {
    this.navigate(TEAM_ROUTE, navOptions)
}

fun NavGraphBuilder.teamScreen() {
    composable(route = TEAM_ROUTE) {
        TeamsRoute()
    }
}
