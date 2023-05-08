package com.crisiscleanup.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.crisiscleanup.core.appnav.RouteConstant.casesGraphRoutePattern
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.feature.caseeditor.navigation.caseEditMoveLocationOnMapScreen
import com.crisiscleanup.feature.caseeditor.navigation.caseEditSearchAddressScreen
import com.crisiscleanup.feature.caseeditor.navigation.caseEditorScreen
import com.crisiscleanup.feature.caseeditor.navigation.existingCaseScreen
import com.crisiscleanup.feature.caseeditor.navigation.navigateToCaseEditor
import com.crisiscleanup.feature.caseeditor.navigation.navigateToExistingCase
import com.crisiscleanup.feature.cases.navigation.casesGraph
import com.crisiscleanup.feature.cases.navigation.casesSearchScreen
import com.crisiscleanup.feature.cases.navigation.navigateToCasesSearch
import com.crisiscleanup.feature.cases.navigation.selectIncidentScreen
import com.crisiscleanup.feature.cases.ui.CasesAction
import com.crisiscleanup.feature.dashboard.navigation.dashboardScreen
import com.crisiscleanup.feature.menu.navigation.menuScreen
import com.crisiscleanup.feature.syncinsights.navigation.navigateToSyncInsights
import com.crisiscleanup.feature.syncinsights.navigation.syncInsightsScreen
import com.crisiscleanup.feature.team.navigation.teamScreen

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
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    startDestination: String = casesGraphRoutePattern,
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

    val replaceRouteOpenCase = remember(navController) {
        { incidentId: Long, worksiteId: Long ->
            navController.popBackStack()
            viewCase(incidentId, worksiteId)
            true
        }
    }

    val openSyncLogs = remember(navController) {
        {
            navController.navigateToSyncInsights()
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        casesGraph(
            nestedGraphs = {
                selectIncidentScreen(onBackClick)
                casesSearchScreen(onBackClick, replaceRouteOpenCase)
                caseEditorScreen(navController, onBackClick)
                caseEditSearchAddressScreen(navController, onBackClick)
                caseEditMoveLocationOnMapScreen(onBackClick)
                existingCaseScreen(navController, onBackClick)
            },
            onCasesAction = onCasesAction,
            createCase = createNewCase,
            viewCase = viewCase,
        )
        dashboardScreen()
        teamScreen()
        menuScreen(
            openSyncLogs,
        )
        syncInsightsScreen(viewCase)
    }
}
