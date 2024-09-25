package com.crisiscleanup.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.crisiscleanup.core.appnav.RouteConstant.CASES_GRAPH_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.CASES_SEARCH_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.CASE_ADD_FLAG_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.CASE_EDITOR_MAP_MOVE_LOCATION_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.CASE_EDITOR_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.CASE_EDITOR_SEARCH_ADDRESS_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.CASE_HISTORY_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.CASE_SHARE_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.TEAM_EDITOR_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.TEAM_SCAN_QR_CODE_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.VIEW_CASE_TRANSFER_WORK_TYPES_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.VIEW_TEAM_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.WORKSITE_IMAGES_ROUTE
import com.crisiscleanup.core.appnav.navigateToExistingCase
import com.crisiscleanup.core.commoncase.ui.CasesAction
import com.crisiscleanup.core.data.model.ExistingWorksiteIdentifier
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.feature.authentication.navigation.resetPasswordScreen
import com.crisiscleanup.feature.caseeditor.navigation.caseAddFlagScreen
import com.crisiscleanup.feature.caseeditor.navigation.caseEditMoveLocationOnMapScreen
import com.crisiscleanup.feature.caseeditor.navigation.caseEditSearchAddressScreen
import com.crisiscleanup.feature.caseeditor.navigation.caseEditorScreen
import com.crisiscleanup.feature.caseeditor.navigation.caseHistoryScreen
import com.crisiscleanup.feature.caseeditor.navigation.caseShareScreen
import com.crisiscleanup.feature.caseeditor.navigation.existingCaseScreen
import com.crisiscleanup.feature.caseeditor.navigation.existingCaseTransferWorkTypesScreen
import com.crisiscleanup.feature.caseeditor.navigation.navigateToCaseAddFlag
import com.crisiscleanup.feature.caseeditor.navigation.navigateToCaseEditor
import com.crisiscleanup.feature.caseeditor.navigation.navigateToTransferWorkType
import com.crisiscleanup.feature.caseeditor.navigation.navigateToViewCase
import com.crisiscleanup.feature.caseeditor.navigation.rerouteToCaseChange
import com.crisiscleanup.feature.cases.navigation.casesFilterScreen
import com.crisiscleanup.feature.cases.navigation.casesGraph
import com.crisiscleanup.feature.cases.navigation.casesSearchScreen
import com.crisiscleanup.feature.cases.navigation.navigateToCasesFilter
import com.crisiscleanup.feature.cases.navigation.navigateToCasesSearch
import com.crisiscleanup.feature.crisiscleanuplists.navigation.listsScreen
import com.crisiscleanup.feature.crisiscleanuplists.navigation.navigateToLists
import com.crisiscleanup.feature.crisiscleanuplists.navigation.navigateToViewList
import com.crisiscleanup.feature.crisiscleanuplists.navigation.viewListScreen
import com.crisiscleanup.feature.dashboard.navigation.dashboardScreen
import com.crisiscleanup.feature.mediamanage.navigation.viewSingleImageScreen
import com.crisiscleanup.feature.mediamanage.navigation.viewWorksiteImagesScreen
import com.crisiscleanup.feature.menu.navigation.menuScreen
import com.crisiscleanup.feature.organizationmanage.navigation.inviteTeammateScreen
import com.crisiscleanup.feature.organizationmanage.navigation.navigateToInviteTeammate
import com.crisiscleanup.feature.organizationmanage.navigation.navigateToRequestRedeploy
import com.crisiscleanup.feature.organizationmanage.navigation.requestRedeployScreen
import com.crisiscleanup.feature.qrcode.navigation.navigateToTeamQrCode
import com.crisiscleanup.feature.qrcode.navigation.navigateToTeamScanQrCode
import com.crisiscleanup.feature.syncinsights.navigation.navigateToSyncInsights
import com.crisiscleanup.feature.syncinsights.navigation.syncInsightsScreen
import com.crisiscleanup.feature.team.model.TeamEditorStep
import com.crisiscleanup.feature.team.navigation.navigateToAssignCaseTeam
import com.crisiscleanup.feature.team.navigation.navigateToTeamEditor
import com.crisiscleanup.feature.team.navigation.navigateToViewTeam
import com.crisiscleanup.feature.team.navigation.teamEditorScreen
import com.crisiscleanup.feature.team.navigation.teamsScreen
import com.crisiscleanup.feature.team.navigation.viewTeamScreen
import com.crisiscleanup.feature.userfeedback.navigation.navigateToUserFeedback
import com.crisiscleanup.feature.userfeedback.navigation.userFeedbackScreen

internal fun NavController.backOnStartingRoute(route: String) {
    if (currentDestination?.route?.startsWith(route) == true) {
        popBackStack()
    }
}

internal fun NavController.backOnRoute(route: String) {
    if (currentDestination?.route == route) {
        popBackStack()
    }
}

/**
 * Top-level navigation graph. Navigation is organized as explained at
 * https://d.android.com/jetpack/compose/nav-adaptive
 *
 * The navigation graph defined in this file defines the different top level routes. Navigation
 * within each route is handled using state and Back Handlers.
 */
@Composable
fun CrisisCleanupNavHost(
    navController: NavHostController,
    onBack: () -> Unit,
    openAuthentication: () -> Unit,
    modifier: Modifier = Modifier,
    startDestination: String = CASES_GRAPH_ROUTE,
) {
    val viewCase = remember(navController) {
        { incidentId: Long, worksiteId: Long ->
            val isValid = incidentId != EmptyIncident.id && worksiteId != EmptyWorksite.id
            if (isValid) {
                navController.navigateToExistingCase(incidentId, worksiteId)
            }
            isValid
        }
    }

    val onCasesAction = remember(navController) {
        { casesAction: CasesAction ->
            when (casesAction) {
                CasesAction.Search -> navController.navigateToCasesSearch()
                else -> Log.w("cases-action", "New cases action $casesAction requires handling")
            }
            Unit
        }
    }

    val replaceRouteViewCase = navController::rerouteToCaseChange

    val openViewCase = remember(navController) {
        { ids: ExistingWorksiteIdentifier ->
            navController.navigateToViewCase(ids.incidentId, ids.worksiteId)
        }
    }

    val openFilterCases = navController::navigateToCasesFilter

    val openInviteTeammate = navController::navigateToInviteTeammate

    val openRequestRedeploy = navController::navigateToRequestRedeploy

    val openUserFeedback = navController::navigateToUserFeedback

    val openLists = navController::navigateToLists
    val openList = navController::navigateToViewList

    val navToCaseAddFlagNonEditing =
        remember(navController) { { navController.navigateToCaseAddFlag(false) } }

    val navToTransferWorkTypeNonEditing =
        remember(navController) { { navController.navigateToTransferWorkType(false) } }

    val searchCasesOnBack =
        remember(navController) { { navController.backOnRoute(CASES_SEARCH_ROUTE) } }

    val caseEditorOnBack =
        remember(navController) { { navController.backOnStartingRoute(CASE_EDITOR_ROUTE) } }

    val searchAddressOnBack =
        remember(navController) { { navController.backOnRoute(CASE_EDITOR_SEARCH_ADDRESS_ROUTE) } }
    val moveLocationOnBack =
        remember(navController) { { navController.backOnRoute(CASE_EDITOR_MAP_MOVE_LOCATION_ROUTE) } }

    val transferOnBack = remember(navController) {
        {
            navController.backOnStartingRoute(VIEW_CASE_TRANSFER_WORK_TYPES_ROUTE)
        }
    }

    val addFlagOnBack =
        remember(navController) { { navController.backOnStartingRoute(CASE_ADD_FLAG_ROUTE) } }

    val shareOnBack = remember(navController) { { navController.backOnRoute(CASE_SHARE_ROUTE) } }

    val historyOnBack =
        remember(navController) { { navController.backOnRoute(CASE_HISTORY_ROUTE) } }

    val worksiteImagesOnBack =
        remember(navController) { { navController.backOnStartingRoute(WORKSITE_IMAGES_ROUTE) } }

    val viewTeamOnBack =
        remember(navController) { { navController.backOnStartingRoute(VIEW_TEAM_ROUTE) } }

    val navToAssignCaseTeam = navController::navigateToAssignCaseTeam

    val navToEditTeamMembers = remember(navController) {
        { incidentId: Long, teamId: Long ->
            navController.navigateToTeamEditor(incidentId, teamId, TeamEditorStep.Members)
        }
    }
    val navToEditTeamCases = remember(navController) {
        { incidentId: Long, teamId: Long ->
            navController.navigateToTeamEditor(incidentId, teamId, TeamEditorStep.Cases)
        }
    }
    val navToEditTeamEquipment = remember(navController) {
        { incidentId: Long, teamId: Long ->
            navController.navigateToTeamEditor(incidentId, teamId, TeamEditorStep.Equipment)
        }
    }
    val navToJoinTeamByQrCode = navController::navigateToTeamQrCode

    val teamEditorOnBack =
        remember(navController) { { navController.backOnStartingRoute(TEAM_EDITOR_ROUTE) } }
    val teamScanQrOnBack =
        remember(navController) { { navController.backOnStartingRoute(TEAM_SCAN_QR_CODE_ROUTE) } }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        casesGraph(
            nestedGraphs = {
                casesSearchScreen(searchCasesOnBack, viewCase)
                casesFilterScreen(onBack)
                caseEditorScreen(navController, caseEditorOnBack)
                caseEditSearchAddressScreen(navController, searchAddressOnBack)
                caseEditMoveLocationOnMapScreen(moveLocationOnBack)
                existingCaseScreen(navController, onBack)
                existingCaseTransferWorkTypesScreen(transferOnBack)
                caseAddFlagScreen(addFlagOnBack, replaceRouteViewCase)
                caseShareScreen(shareOnBack)
                caseHistoryScreen(historyOnBack)
            },
            onCasesAction = onCasesAction,
            filterCases = openFilterCases,
            createCase = navController::navigateToCaseEditor,
            viewCase = viewCase,
            caseAddFlag = navToCaseAddFlagNonEditing,
            caseTransferWorkType = navToTransferWorkTypeNonEditing,
            openAssignCaseTeam = navToAssignCaseTeam,
        )
        dashboardScreen()
        teamsScreen(
            nestedGraphs = {
                viewTeamScreen(
                    viewTeamOnBack,
                    editTeamMembers = navToEditTeamMembers,
                    editCases = navToEditTeamCases,
                    editEquipment = navToEditTeamEquipment,
                    viewCase = viewCase,
                    openAddFlag = navToCaseAddFlagNonEditing,
                    openAssignCaseTeam = navToAssignCaseTeam,
                )
                teamEditorScreen(
                    teamEditorOnBack,
                )
                navigateToTeamScanQrCode(teamScanQrOnBack)
            },
            openAuthentication = openAuthentication,
            openViewTeam = navController::navigateToViewTeam,
            openCreateTeam = navController::navigateToTeamEditor,
            openJoinTeamByQrCode = navToJoinTeamByQrCode,
        )
        menuScreen(
            openAuthentication = openAuthentication,
            openLists = openLists,
            openInviteTeammate = openInviteTeammate,
            openRequestRedeploy = openRequestRedeploy,
            openUserFeedback = openUserFeedback,
            openSyncLogs = navController::navigateToSyncInsights,
        )
        viewSingleImageScreen(onBack)
        viewWorksiteImagesScreen(worksiteImagesOnBack)
        // Invite to org not teams feature
        inviteTeammateScreen(onBack)
        requestRedeployScreen(onBack)
        userFeedbackScreen(onBack)
        listsScreen(navController, onBack)
        viewListScreen(
            onBack,
            openList = openList,
            openWorksite = openViewCase,
        )
        syncInsightsScreen(viewCase)

        resetPasswordScreen(
            isAuthenticated = true,
            onBack = onBack,
            closeResetPassword = onBack,
        )
    }
}
