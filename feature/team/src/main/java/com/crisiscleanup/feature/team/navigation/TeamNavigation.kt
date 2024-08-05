package com.crisiscleanup.feature.team.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.crisiscleanup.core.appnav.RouteConstant.TEAM_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.VIEW_TEAM_ROUTE
import com.crisiscleanup.feature.team.ui.TeamsRoute
import com.crisiscleanup.feature.team.ui.ViewTeamRoute

private const val TEAM_ID_ARG = "teamId"

internal class ViewTeamArgs(val teamId: Long) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle.get<Long>(TEAM_ID_ARG)),
    )
}

fun NavController.navigateToTeams(navOptions: NavOptions? = null) {
    this.navigate(TEAM_ROUTE, navOptions)
}

fun NavController.navigateToViewTeam(teamId: Long) {
    val route = "$VIEW_TEAM_ROUTE?$TEAM_ID_ARG=$teamId"
    this.navigate(route)
}

fun NavGraphBuilder.teamsScreen(
    nestedGraphs: NavGraphBuilder.() -> Unit,
    openAuthentication: () -> Unit,
    openViewTeam: (Long) -> Unit,
) {
    composable(route = TEAM_ROUTE) {
        TeamsRoute(
            openAuthentication = openAuthentication,
            openViewTeam = openViewTeam,
        )
    }

    nestedGraphs()
}

fun NavGraphBuilder.viewTeamScreen(
    onBack: () -> Unit = {},
) {
    composable(
        route = "$VIEW_TEAM_ROUTE?$TEAM_ID_ARG={$TEAM_ID_ARG}",
        arguments = listOf(
            navArgument(TEAM_ID_ARG) {
                type = NavType.LongType
            },
        ),
    ) {
        ViewTeamRoute(onBack = onBack)
    }
}
