package com.crisiscleanup.feature.team.navigation

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.crisiscleanup.core.appnav.RouteConstant.TEAM_EDITOR_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.TEAM_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.VIEW_TEAM_ROUTE
import com.crisiscleanup.core.model.data.EmptyCleanupTeam
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.feature.team.model.TeamEditorStep
import com.crisiscleanup.feature.team.ui.CreateEditTeamRoute
import com.crisiscleanup.feature.team.ui.TeamsRoute
import com.crisiscleanup.feature.team.ui.ViewTeamRoute

private const val INCIDENT_ID_ARG = "incidentId"
private const val TEAM_ID_ARG = "teamId"
private const val TEAM_EDITOR_STEP = "initialEditorStep"

internal class ViewTeamArgs(val incidentId: Long, val teamId: Long) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[INCIDENT_ID_ARG]),
        checkNotNull(savedStateHandle[TEAM_ID_ARG]),
    )
}

fun NavController.navigateToTeams(navOptions: NavOptions? = null) {
    navigate(TEAM_ROUTE, navOptions)
}

fun NavController.navigateToViewTeam(incidentId: Long, teamId: Long) {
    val route = "$VIEW_TEAM_ROUTE?$INCIDENT_ID_ARG=$incidentId&$TEAM_ID_ARG=$teamId"
    navigate(route)
}

fun NavController.navigateToTeamEditor(
    teamId: Long = EmptyCleanupTeam.id,
    editorStep: TeamEditorStep = TeamEditorStep.None,
) {
    val route = "$TEAM_EDITOR_ROUTE?$TEAM_ID_ARG=$teamId&$TEAM_EDITOR_STEP=${editorStep.literal}"
    navigate(route)
}

fun NavController.navigateToAssignCaseTeam(worksiteId: Long) {
    // TODO Determine if team parameter should be passed
    Log.w("team", "Assign Case $worksiteId to team not yet implemented")
}

fun NavGraphBuilder.teamsScreen(
    nestedGraphs: NavGraphBuilder.() -> Unit,
    openAuthentication: () -> Unit,
    openViewTeam: (Long, Long) -> Unit,
    openCreateTeam: () -> Unit,
) {
    composable(route = TEAM_ROUTE) {
        TeamsRoute(
            openAuthentication = openAuthentication,
            openViewTeam = openViewTeam,
            openCreateTeam = openCreateTeam,
        )
    }

    nestedGraphs()
}

fun NavGraphBuilder.viewTeamScreen(
    onBack: () -> Unit = {},
    viewCase: (Long, Long) -> Boolean = { _, _ -> false },
    openAddFlag: () -> Unit = {},
    openAssignCaseTeam: (Long) -> Unit = {},
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
        ViewTeamRoute(
            onBack = onBack,
            onViewCase = viewCase,
            onOpenFlags = openAddFlag,
            onAssignCaseTeam = openAssignCaseTeam,
        )
    }
}

fun NavGraphBuilder.teamEditorScreen(
    onBack: () -> Unit,
) {
    composable(
        route = "$TEAM_EDITOR_ROUTE?$TEAM_ID_ARG={$TEAM_ID_ARG}&$TEAM_EDITOR_STEP={$TEAM_EDITOR_STEP}",
        arguments = listOf(
            navArgument(TEAM_ID_ARG) {
                type = NavType.LongType
                defaultValue = EmptyCleanupTeam.id
            },
            navArgument(TEAM_EDITOR_STEP) {
                type = NavType.StringType
            },
        ),
    ) {
        CreateEditTeamRoute(
            onBack,
        )
    }
}
