package com.crisiscleanup.feature.cases.navigation

import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.crisiscleanup.core.appnav.RouteConstant.CASES_GRAPH_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.CASES_ROUTE
import com.crisiscleanup.feature.cases.ui.CasesAction
import com.crisiscleanup.feature.cases.ui.CasesRoute

fun NavController.navigateToCases(navOptions: NavOptions? = null) {
    this.navigate(CASES_GRAPH_ROUTE, navOptions)
}

fun NavGraphBuilder.casesGraph(
    nestedGraphs: NavGraphBuilder.() -> Unit,
    onCasesAction: (CasesAction) -> Unit = { },
    filterCases: () -> Unit = {},
    createCase: (Long) -> Unit = {},
    viewCase: (Long, Long) -> Boolean = { _, _ -> false },
    caseAddFlag: () -> Unit = {},
    caseTransferWorkType: () -> Unit = {},
    openAssignCaseTeam: (Long) -> Unit = {},
) {
    navigation(
        route = CASES_GRAPH_ROUTE,
        startDestination = CASES_ROUTE,
    ) {
        composable(route = CASES_ROUTE) {
            val rememberOnCasesAction = remember(onCasesAction) {
                { casesAction: CasesAction ->
                    when (casesAction) {
                        CasesAction.Filters -> {
                            filterCases()
                        }

                        else -> onCasesAction(casesAction)
                    }
                }
            }
            CasesRoute(
                onCasesAction = rememberOnCasesAction,
                createNewCase = createCase,
                viewCase = viewCase,
                openAddFlag = caseAddFlag,
                openTransferWorkType = caseTransferWorkType,
                onAssignCaseTeam = openAssignCaseTeam,
            )
        }

        nestedGraphs()
    }
}
