package com.crisiscleanup.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.crisiscleanup.core.appnav.RouteConstant.casesGraphRoutePattern
import com.crisiscleanup.feature.caseeditor.navigation.*
import com.crisiscleanup.feature.cases.navigation.casesGraph
import com.crisiscleanup.feature.cases.navigation.selectIncidentScreen
import com.crisiscleanup.feature.cases.ui.CasesAction
import com.crisiscleanup.feature.dashboard.navigation.dashboardScreen
import com.crisiscleanup.feature.menu.navigation.menuScreen
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
    onCasesAction: (CasesAction) -> Unit = { },
) {
    val createNewCase = remember(navController) {
        { incidentId: Long -> navController.navigateToCaseEditor(incidentId) }
    }

    val openCase = remember(navController) {
        { incidentId: Long, worksiteId: Long ->
            navController.navigateToCaseEditor(incidentId, worksiteId)
            true
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
                caseEditorScreen(navController, onBackClick)
                caseEditPropertyScreen(navController, onBackClick)
                caseEditLocationScreen(navController, onBackClick)
                caseEditNotesFlagsScreen(onBackClick)
                caseEditDetailsScreen(onBackClick)
                caseEditWorkScreen(onBackClick)
                caseEditHazardsScreen(onBackClick)
                caseEditVolunteerReportScreen(onBackClick)
            },
            onCasesAction = onCasesAction,
            createCase = createNewCase,
            editCase = openCase,
        )
        dashboardScreen()
        teamScreen()
        menuScreen()
    }
}
