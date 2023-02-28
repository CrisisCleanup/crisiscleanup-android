package com.crisiscleanup.feature.team.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.crisiscleanup.core.appnav.RouteConstant.teamRoute
import com.crisiscleanup.feature.team.TeamRoute

fun NavController.navigateToTeam(navOptions: NavOptions? = null) {
    this.navigate(teamRoute, navOptions)
}

fun NavGraphBuilder.teamScreen() {
    composable(route = teamRoute) {
        TeamRoute()
    }
}
