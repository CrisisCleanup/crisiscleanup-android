package com.crisiscleanup.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.crisiscleanup.core.appnav.RouteConstant.ACCOUNT_RESET_PASSWORD_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.ACCOUNT_TRANSFER_ORG_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.CASES_FILTER_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.CASES_GRAPH_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.CASES_SEARCH_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.CASE_ADD_FLAG_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.CASE_EDITOR_MAP_MOVE_LOCATION_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.CASE_EDITOR_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.CASE_EDITOR_SEARCH_ADDRESS_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.CASE_HISTORY_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.CASE_SHARE_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.INCIDENT_WORKSITES_CACHE_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.INVITE_TEAMMATE_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.LISTS_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.REQUEST_REDEPLOY_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.TEAM_CASES_SEARCH_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.TEAM_EDITOR_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.TEAM_SCAN_QR_CODE_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.USER_FEEDBACK_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.VIEW_CASE_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.VIEW_CASE_ROUTE_RESTRICTED
import com.crisiscleanup.core.appnav.RouteConstant.VIEW_CASE_TRANSFER_WORK_TYPES_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.VIEW_IMAGE_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.VIEW_LIST_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.VIEW_TEAM_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.WORKSITE_IMAGES_ROUTE
import com.crisiscleanup.core.appnav.navigateToViewCase
import com.crisiscleanup.core.commoncase.ui.CasesAction
import com.crisiscleanup.core.data.model.ExistingWorksiteIdentifier
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.feature.authentication.navigation.requestAccessScreen
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
import com.crisiscleanup.feature.caseeditor.navigation.rerouteToViewCase
import com.crisiscleanup.feature.caseeditor.navigation.restrictedViewCaseScreen
import com.crisiscleanup.feature.cases.navigation.casesFilterScreen
import com.crisiscleanup.feature.cases.navigation.casesGraph
import com.crisiscleanup.feature.cases.navigation.casesSearchScreen
import com.crisiscleanup.feature.cases.navigation.navigateToCasesFilter
import com.crisiscleanup.feature.cases.navigation.navigateToCasesSearch
import com.crisiscleanup.feature.cases.navigation.teamCasesSearchScreen
import com.crisiscleanup.feature.crisiscleanuplists.navigation.listsScreen
import com.crisiscleanup.feature.crisiscleanuplists.navigation.navigateToLists
import com.crisiscleanup.feature.crisiscleanuplists.navigation.navigateToViewList
import com.crisiscleanup.feature.crisiscleanuplists.navigation.viewListScreen
import com.crisiscleanup.feature.dashboard.navigation.dashboardScreen
import com.crisiscleanup.feature.incidentcache.navigation.incidentWorksitesCache
import com.crisiscleanup.feature.incidentcache.navigation.navigateToIncidentWorksitesCache
import com.crisiscleanup.feature.mediamanage.navigation.viewSingleImageScreen
import com.crisiscleanup.feature.mediamanage.navigation.viewWorksiteImagesScreen
import com.crisiscleanup.feature.menu.navigation.menuScreen
import com.crisiscleanup.feature.organizationmanage.navigation.inviteTeammateScreen
import com.crisiscleanup.feature.organizationmanage.navigation.navigateToInviteTeammate
import com.crisiscleanup.feature.organizationmanage.navigation.navigateToRequestRedeploy
import com.crisiscleanup.feature.organizationmanage.navigation.requestRedeployScreen
import com.crisiscleanup.feature.qrcode.navigation.navigateToTeamQrCode
import com.crisiscleanup.feature.qrcode.navigation.teamScanQrCode
import com.crisiscleanup.feature.syncinsights.navigation.navigateToSyncInsights
import com.crisiscleanup.feature.syncinsights.navigation.syncInsightsScreen
import com.crisiscleanup.feature.team.model.TeamEditorStep
import com.crisiscleanup.feature.team.navigation.navigateToAssignCaseTeam
import com.crisiscleanup.feature.team.navigation.navigateToTeamEditor
import com.crisiscleanup.feature.team.navigation.navigateToViewTeam
import com.crisiscleanup.feature.team.navigation.setCaseSearchResult
import com.crisiscleanup.feature.team.navigation.teamEditorScreen
import com.crisiscleanup.feature.team.navigation.teamsScreen
import com.crisiscleanup.feature.team.navigation.viewTeamScreen
import com.crisiscleanup.feature.userfeedback.navigation.navigateToUserFeedback
import com.crisiscleanup.feature.userfeedback.navigation.userFeedbackScreen

private fun NavController.matchesRoute(route: String) = currentDestination?.route == route
private fun NavController.startsWithRoute(route: String) =
    currentDestination?.route?.startsWith(route) == true

@Composable
private fun rememberBackOnRoute(
    navController: NavController,
    onBack: () -> Unit,
    route: String,
) = remember(onBack, navController) {
    {
        if (navController.matchesRoute(route)) {
            onBack()
        }
    }
}

@Composable
private fun rememberBackStartingRoute(
    navController: NavController,
    onBack: () -> Unit,
    route: String,
) = remember(onBack, navController) {
    {
        if (navController.startsWithRoute(route)) {
            onBack()
        }
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
                navController.navigateToViewCase(incidentId, worksiteId)
            }
            isValid
        }
    }
    val viewCaseUnit = remember(viewCase) {
        { incidentId: Long, worksiteId: Long ->
            viewCase(incidentId, worksiteId)
            Unit
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

    val replaceRouteViewCase = navController::rerouteToViewCase

    val openViewCase = remember(navController) {
        { ids: ExistingWorksiteIdentifier ->
            navController.navigateToViewCase(ids.incidentId, ids.worksiteId)
        }
    }
    val viewCaseOnBack = rememberBackStartingRoute(navController, onBack, VIEW_CASE_ROUTE)
    val viewCaseRestrictedOnBack =
        rememberBackStartingRoute(navController, onBack, VIEW_CASE_ROUTE_RESTRICTED)

    val openFilterCases = remember(navController) { { navController.navigateToCasesFilter(false) } }

    val openInviteTeammate = navController::navigateToInviteTeammate

    val openRequestRedeploy = navController::navigateToRequestRedeploy

    val openUserFeedback = navController::navigateToUserFeedback

    val openLists = navController::navigateToLists
    val openList = navController::navigateToViewList

    val navToCaseAddFlagNonEditing =
        remember(navController) { { navController.navigateToCaseAddFlag(false) } }

    val navToTransferWorkTypeNonEditing =
        remember(navController) { { navController.navigateToTransferWorkType(false) } }

    val searchCasesOnBack = rememberBackOnRoute(navController, onBack, CASES_SEARCH_ROUTE)

    val filterCasesOnBack = rememberBackOnRoute(navController, onBack, CASES_FILTER_ROUTE)

    val caseEditorOnBack = rememberBackStartingRoute(navController, onBack, CASE_EDITOR_ROUTE)

    val searchAddressOnBack =
        rememberBackOnRoute(navController, onBack, CASE_EDITOR_SEARCH_ADDRESS_ROUTE)
    val moveLocationOnBack =
        rememberBackOnRoute(navController, onBack, CASE_EDITOR_MAP_MOVE_LOCATION_ROUTE)

    val transferOnBack =
        rememberBackStartingRoute(navController, onBack, VIEW_CASE_TRANSFER_WORK_TYPES_ROUTE)

    val addFlagOnBack = rememberBackStartingRoute(navController, onBack, CASE_ADD_FLAG_ROUTE)

    val shareOnBack = rememberBackOnRoute(navController, onBack, CASE_SHARE_ROUTE)

    val historyOnBack = rememberBackOnRoute(navController, onBack, CASE_HISTORY_ROUTE)

    val viewSingleImageOnBack = rememberBackStartingRoute(navController, onBack, VIEW_IMAGE_ROUTE)
    val worksiteImagesOnBack =
        rememberBackStartingRoute(navController, onBack, WORKSITE_IMAGES_ROUTE)

    val viewTeamOnBack =
        rememberBackStartingRoute(navController, onBack, VIEW_TEAM_ROUTE)

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
    val navToTeamSearchCases = remember(navController) {
        {
            navController.navigateToCasesSearch(true)
        }
    }
    val navToTeamFilterCases = remember(navController) {
        {
            navController.navigateToCasesFilter(true)
        }
    }

    val teamEditorOnBack = rememberBackStartingRoute(navController, onBack, TEAM_EDITOR_ROUTE)
    val teamScanQrOnBack = rememberBackStartingRoute(navController, onBack, TEAM_SCAN_QR_CODE_ROUTE)
    val teamSearchCasesOnBack = rememberBackOnRoute(navController, onBack, TEAM_CASES_SEARCH_ROUTE)
    val navToViewCaseFromTeams = remember(navController) {
        { incidentId: Long, worksiteId: Long ->
            navController.navigateToViewCase(
                incidentId = incidentId,
                worksiteId = worksiteId,
                isRestricted = true,
            )
        }
    }
    val onAssignCaseToTeam = remember(navController) {
        { incidentId: Long, worksiteId: Long ->
            if (navController.currentBackStackEntry?.destination?.route == TEAM_CASES_SEARCH_ROUTE) {
                navController.previousBackStackEntry?.let { entry ->
                    if (entry.destination.route?.startsWith(TEAM_EDITOR_ROUTE) == true) {
                        entry.setCaseSearchResult(
                            incidentId = incidentId,
                            worksiteId = worksiteId,
                        )
                    }
                }
                navController.popBackStack()
            }
            Unit
        }
    }

    val incidentWorksitesCacheOnBack =
        rememberBackOnRoute(navController, onBack, INCIDENT_WORKSITES_CACHE_ROUTE)
    val openIncidentCache = navController::navigateToIncidentWorksitesCache

    val viewListsOnBack = rememberBackOnRoute(navController, onBack, LISTS_ROUTE)
    val viewListOnBack = rememberBackStartingRoute(navController, onBack, VIEW_LIST_ROUTE)
    val inviteTeammatesOnBack = rememberBackOnRoute(navController, onBack, INVITE_TEAMMATE_ROUTE)
    val requestRedeployOnBack = rememberBackOnRoute(navController, onBack, REQUEST_REDEPLOY_ROUTE)
    val userFeedbackOnBack = rememberBackOnRoute(navController, onBack, USER_FEEDBACK_ROUTE)

    val resetPasswordOnBack =
        rememberBackOnRoute(navController, onBack, ACCOUNT_RESET_PASSWORD_ROUTE)
    val requestAccessOnBack =
        rememberBackStartingRoute(navController, onBack, ACCOUNT_TRANSFER_ORG_ROUTE)

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        casesGraph(
            navController,
            nestedGraphs = {
                casesSearchScreen(searchCasesOnBack, viewCaseUnit)
                casesFilterScreen(filterCasesOnBack)
                caseEditorScreen(navController, caseEditorOnBack)
                caseEditSearchAddressScreen(navController, searchAddressOnBack)
                caseEditMoveLocationOnMapScreen(moveLocationOnBack)
                existingCaseScreen(navController, viewCaseOnBack)
                existingCaseTransferWorkTypesScreen(transferOnBack)
                caseAddFlagScreen(addFlagOnBack, replaceRouteViewCase)
                caseShareScreen(shareOnBack)
                caseHistoryScreen(historyOnBack)
                restrictedViewCaseScreen(navController, viewCaseRestrictedOnBack)
            },
            onCasesAction = onCasesAction,
            filterCases = openFilterCases,
            createCase = navController::navigateToCaseEditor,
            viewCase = viewCase,
            caseAddFlag = navToCaseAddFlagNonEditing,
            caseTransferWorkType = navToTransferWorkTypeNonEditing,
            openAssignCaseTeam = navToAssignCaseTeam,
        )
        // Used by teams as well
        casesFilterScreen(onBack)
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
                    navController,
                    teamEditorOnBack,
                    openCase = navToViewCaseFromTeams,
                    openSearchCases = navToTeamSearchCases,
                    openFilterCases = navToTeamFilterCases,
                )
                teamScanQrCode(teamScanQrOnBack)
                teamCasesSearchScreen(
                    teamSearchCasesOnBack,
                    openCase = navToViewCaseFromTeams,
                    onAssignToTeam = onAssignCaseToTeam,
                )
            },
            openAuthentication = openAuthentication,
            openViewTeam = navController::navigateToViewTeam,
            openCreateTeam = navController::navigateToTeamEditor,
            openJoinTeamByQrCode = navToJoinTeamByQrCode,
        )
        menuScreen(
            navController,
            openAuthentication = openAuthentication,
            openIncidentCache = openIncidentCache,
            openLists = openLists,
            openInviteTeammate = openInviteTeammate,
            openRequestRedeploy = openRequestRedeploy,
            openUserFeedback = openUserFeedback,
            openSyncLogs = navController::navigateToSyncInsights,
        )
        incidentWorksitesCache(incidentWorksitesCacheOnBack)
        viewSingleImageScreen(viewSingleImageOnBack)
        viewWorksiteImagesScreen(worksiteImagesOnBack)
        // Invite to org not teams feature
        inviteTeammateScreen(inviteTeammatesOnBack)
        requestRedeployScreen(requestRedeployOnBack)
        userFeedbackScreen(userFeedbackOnBack)
        listsScreen(navController, viewListsOnBack)
        viewListScreen(
            viewListOnBack,
            openList = openList,
            openWorksite = openViewCase,
        )
        syncInsightsScreen(viewCase)

        resetPasswordScreen(
            isAuthenticated = true,
            onBack = resetPasswordOnBack,
            closeResetPassword = resetPasswordOnBack,
        )

        requestAccessScreen(
            true,
            onBack = requestAccessOnBack,
            closeRequestAccess = requestAccessOnBack,
            openAuth = {},
            openForgotPassword = {},
        )
    }
}
