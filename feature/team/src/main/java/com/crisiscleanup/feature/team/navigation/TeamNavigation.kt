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
private const val TEAM_EDITOR_STEP = "teamEditorStep"

internal class ViewTeamArgs(val incidentId: Long, val teamId: Long) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[INCIDENT_ID_ARG]),
        checkNotNull(savedStateHandle[TEAM_ID_ARG]),
    )
}

internal class TeamEditorArgs(
    val incidentId: Long,
    val teamId: Long,
    val editorStep: String,
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[INCIDENT_ID_ARG]),
        checkNotNull(savedStateHandle[TEAM_ID_ARG]),
        checkNotNull(savedStateHandle[TEAM_EDITOR_STEP]),
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
    incidentId: Long = EmptyIncident.id,
    teamId: Long = EmptyCleanupTeam.id,
    editorStep: TeamEditorStep = TeamEditorStep.Info,
) {
    val args = listOf(
        "$INCIDENT_ID_ARG=$incidentId",
        "$TEAM_ID_ARG=$teamId",
        "$TEAM_EDITOR_STEP=${editorStep.literal}",
    ).joinToString(",")
    val route = "$TEAM_EDITOR_ROUTE?$args"
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
    openCreateTeam: (Long) -> Unit,
    openJoinTeamByQrCode: () -> Unit,
) {
    composable(route = TEAM_ROUTE) {
        TeamsRoute(
            openAuthentication = openAuthentication,
            openViewTeam = openViewTeam,
            openCreateTeam = openCreateTeam,
            openJoinTeamByQrCode = openJoinTeamByQrCode,
        )
    }

    nestedGraphs()
}

fun NavGraphBuilder.viewTeamScreen(
    onBack: () -> Unit = {},
    editTeamMembers: (Long, Long) -> Unit = { _, _ -> },
    editCases: (Long, Long) -> Unit = { _, _ -> },
    editEquipment: (Long, Long) -> Unit = { _, _ -> },
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
            onEditTeamMembers = editTeamMembers,
            onEditCases = editCases,
            onEditEquipment = editEquipment,
            onViewCase = viewCase,
            onOpenFlags = openAddFlag,
            onAssignCaseTeam = openAssignCaseTeam,
        )
    }
}

fun NavGraphBuilder.teamEditorScreen(
    onBack: () -> Unit,
    openFilterCases: () -> Unit = {},
) {
    val args = listOf(
        "$INCIDENT_ID_ARG={$INCIDENT_ID_ARG}",
        "$TEAM_ID_ARG={$TEAM_ID_ARG}",
        "$TEAM_EDITOR_STEP={$TEAM_EDITOR_STEP}",
    ).joinToString(",")
    composable(
        route = "$TEAM_EDITOR_ROUTE?$args",
        arguments = listOf(
            navArgument(INCIDENT_ID_ARG) {
                type = NavType.LongType
                defaultValue = EmptyIncident.id
            },
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
            onFilterCases = openFilterCases,
        )
    }
}
