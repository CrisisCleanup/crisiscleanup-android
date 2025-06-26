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
import com.crisiscleanup.core.appnav.RouteConstant.INCIDENT_WORKSITES_CACHE_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.VIEW_CASE_TRANSFER_WORK_TYPES_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.WORKSITE_IMAGES_ROUTE
import com.crisiscleanup.core.appnav.navigateToExistingCase
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
import com.crisiscleanup.feature.caseeditor.navigation.rerouteToViewCase
import com.crisiscleanup.feature.cases.navigation.casesFilterScreen
import com.crisiscleanup.feature.cases.navigation.casesGraph
import com.crisiscleanup.feature.cases.navigation.casesSearchScreen
import com.crisiscleanup.feature.cases.navigation.navigateToCasesFilter
import com.crisiscleanup.feature.cases.navigation.navigateToCasesSearch
import com.crisiscleanup.feature.cases.ui.CasesAction
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
import com.crisiscleanup.feature.syncinsights.navigation.navigateToSyncInsights
import com.crisiscleanup.feature.syncinsights.navigation.syncInsightsScreen
import com.crisiscleanup.feature.team.navigation.navigateToViewTeam
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
    val createNewCase = remember(navController) {
        { incidentId: Long -> navController.navigateToCaseEditor(incidentId) }
    }

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

    val replaceRouteViewCase = navController::rerouteToViewCase

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

    val searchCasesOnBack = rememberBackOnRoute(navController, onBack, CASES_SEARCH_ROUTE)

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

    val worksiteImagesOnBack =
        rememberBackStartingRoute(navController, onBack, WORKSITE_IMAGES_ROUTE)

    val incidentWorksitesCacheOnBack =
        rememberBackOnRoute(navController, onBack, INCIDENT_WORKSITES_CACHE_ROUTE)
    val openIncidentCache = navController::navigateToIncidentWorksitesCache

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        casesGraph(
            navController,
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
            createCase = createNewCase,
            viewCase = viewCase,
            caseAddFlag = navToCaseAddFlagNonEditing,
            caseTransferWorkType = navToTransferWorkTypeNonEditing,
        )
        dashboardScreen()
        teamsScreen(
            nestedGraphs = {
                viewTeamScreen(onBack)
            },
            openAuthentication = openAuthentication,
            openViewTeam = navController::navigateToViewTeam,
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
        viewSingleImageScreen(onBack)
        viewWorksiteImagesScreen(worksiteImagesOnBack)
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
