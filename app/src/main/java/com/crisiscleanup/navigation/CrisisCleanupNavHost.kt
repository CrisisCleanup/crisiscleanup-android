package com.crisiscleanup.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.crisiscleanup.core.appnav.RouteConstant.CASES_GRAPH_ROUTE
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
import com.crisiscleanup.feature.caseeditor.navigation.rerouteToCaseChange
import com.crisiscleanup.feature.cases.navigation.casesFilterScreen
import com.crisiscleanup.feature.cases.navigation.casesGraph
import com.crisiscleanup.feature.cases.navigation.casesSearchScreen
import com.crisiscleanup.feature.cases.navigation.navigateToCasesFilter
import com.crisiscleanup.feature.cases.navigation.navigateToCasesSearch
import com.crisiscleanup.feature.cases.ui.CasesAction
import com.crisiscleanup.feature.dashboard.navigation.dashboardScreen
import com.crisiscleanup.feature.mediamanage.navigation.viewSingleImageScreen
import com.crisiscleanup.feature.mediamanage.navigation.viewWorksiteImagesScreen
import com.crisiscleanup.feature.menu.navigation.menuScreen
import com.crisiscleanup.feature.organizationmanage.navigation.inviteTeammateScreen
import com.crisiscleanup.feature.organizationmanage.navigation.navigateToInviteTeammate
import com.crisiscleanup.feature.organizationmanage.navigation.navigateToRequestRedeploy
import com.crisiscleanup.feature.organizationmanage.navigation.requestRedeployScreen
import com.crisiscleanup.feature.syncinsights.navigation.navigateToSyncInsights
import com.crisiscleanup.feature.syncinsights.navigation.syncInsightsScreen
import com.crisiscleanup.feature.team.navigation.teamScreen
import com.crisiscleanup.feature.userfeedback.navigation.navigateToUserFeedback
import com.crisiscleanup.feature.userfeedback.navigation.userFeedbackScreen

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

    val replaceRouteViewCase = remember(navController) {
        { ids: ExistingWorksiteIdentifier -> navController.rerouteToCaseChange(ids) }
    }

    val openFilterCases = remember(navController) { { navController.navigateToCasesFilter() } }

    val openInviteTeammate =
        remember(navController) { { navController.navigateToInviteTeammate() } }

    val openRequestRedeploy =
        remember(navController) { { navController.navigateToRequestRedeploy() } }

    val openUserFeedback = remember(navController) { { navController.navigateToUserFeedback() } }

    val openSyncLogs = remember(navController) { { navController.navigateToSyncInsights() } }

    val navToCaseAddFlagNonEditing =
        remember(navController) { { navController.navigateToCaseAddFlag(false) } }

    val navToTransferWorkTypeNonEditing =
        remember(navController) { { navController.navigateToTransferWorkType(false) } }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        casesGraph(
            nestedGraphs = {
                casesSearchScreen(onBack, viewCase)
                casesFilterScreen(onBack)
                caseEditorScreen(navController, onBack)
                caseEditSearchAddressScreen(navController, onBack)
                caseEditMoveLocationOnMapScreen(onBack)
                existingCaseScreen(navController, onBack)
                existingCaseTransferWorkTypesScreen(onBack)
                caseAddFlagScreen(onBack, replaceRouteViewCase)
                caseShareScreen(onBack)
                caseHistoryScreen(onBack)
            },
            onCasesAction = onCasesAction,
            filterCases = openFilterCases,
            createCase = createNewCase,
            viewCase = viewCase,
            caseAddFlag = navToCaseAddFlagNonEditing,
            caseTransferWorkType = navToTransferWorkTypeNonEditing,
        )
        dashboardScreen()
        teamScreen()
        menuScreen(
            openAuthentication = openAuthentication,
            openInviteTeammate = openInviteTeammate,
            openRequestRedeploy = openRequestRedeploy,
            openUserFeedback = openUserFeedback,
            openSyncLogs = openSyncLogs,
        )
        viewSingleImageScreen(onBack)
        viewWorksiteImagesScreen(onBack)
        inviteTeammateScreen(onBack)
        requestRedeployScreen(onBack)
        userFeedbackScreen(onBack)
        syncInsightsScreen(viewCase)

        resetPasswordScreen(
            isAuthenticated = true,
            onBack = onBack,
            closeResetPassword = onBack,
        )
    }
}
