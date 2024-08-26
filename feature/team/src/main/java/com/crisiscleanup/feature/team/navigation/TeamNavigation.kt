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
import com.crisiscleanup.core.model.data.EmptyCleanupTeam
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.feature.team.ui.TeamsRoute
import com.crisiscleanup.feature.team.ui.ViewTeamRoute

private const val INCIDENT_ID_ARG = "incidentId"
private const val TEAM_ID_ARG = "teamId"

internal class ViewTeamArgs(val incidentId: Long, val teamId: Long) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[INCIDENT_ID_ARG]),
        checkNotNull(savedStateHandle[TEAM_ID_ARG]),
    )
}

fun NavController.navigateToTeams(navOptions: NavOptions? = null) {
    this.navigate(TEAM_ROUTE, navOptions)
}

fun NavController.navigateToViewTeam(incidentId: Long, teamId: Long) {
    val route = "$VIEW_TEAM_ROUTE?$INCIDENT_ID_ARG=$incidentId&$TEAM_ID_ARG=$teamId"
    this.navigate(route)
}

fun NavGraphBuilder.teamsScreen(
    nestedGraphs: NavGraphBuilder.() -> Unit,
    openAuthentication: () -> Unit,
    openViewTeam: (Long, Long) -> Unit,
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
        route = "$VIEW_TEAM_ROUTE?$INCIDENT_ID_ARG={$INCIDENT_ID_ARG}&$TEAM_ID_ARG={$TEAM_ID_ARG}",
        arguments = listOf(
            navArgument(INCIDENT_ID_ARG) {
                type = NavType.LongType
                defaultValue = EmptyIncident.id
            },
            navArgument(TEAM_ID_ARG) {
                type = NavType.LongType
                defaultValue = EmptyCleanupTeam.id
            },
        ),
    ) {
        ViewTeamRoute(onBack = onBack)
    }
}
