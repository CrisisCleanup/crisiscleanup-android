package com.crisiscleanup.feature.cases.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.crisiscleanup.core.appnav.RouteConstant.CASES_SEARCH_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.TEAM_CASES_SEARCH_ROUTE
import com.crisiscleanup.feature.cases.ui.CasesSearchRoute

fun NavController.navigateToCasesSearch(isTeamCasesSearch: Boolean = false) {
    val route = if (isTeamCasesSearch) {
        TEAM_CASES_SEARCH_ROUTE
    } else {
        CASES_SEARCH_ROUTE
    }
    navigate(route)
}

fun NavGraphBuilder.casesSearchScreen(
    onBack: () -> Unit,
    openCase: (Long, Long) -> Unit = { _, _ -> },
) {
    composable(CASES_SEARCH_ROUTE) {
        CasesSearchRoute(
            onBack = onBack,
            openCase = openCase,
        )
    }
}

fun NavGraphBuilder.teamCasesSearchScreen(
    onBack: () -> Unit,
    openCase: (Long, Long) -> Unit = { _, _ -> },
) {
    composable(TEAM_CASES_SEARCH_ROUTE) {
        CasesSearchRoute(
            true,
            onBack = onBack,
            openCase = openCase,
        )
    }
}
