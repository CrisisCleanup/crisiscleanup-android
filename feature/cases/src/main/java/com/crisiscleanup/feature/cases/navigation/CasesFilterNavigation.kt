package com.crisiscleanup.feature.cases.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.crisiscleanup.core.appnav.RouteConstant.CASES_FILTER_ROUTE
import com.crisiscleanup.feature.cases.ui.CasesFilterRoute

private const val USE_TEAM_FILTERS_ARG = "useTeamFilters"

internal class CasesFilterArgs(val useTeamFilters: Boolean) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle.get<Boolean>(USE_TEAM_FILTERS_ARG)),
    )
}

fun NavController.navigateToCasesFilter(useTeamFilters: Boolean = false) {
    navigate("$CASES_FILTER_ROUTE?$USE_TEAM_FILTERS_ARG=$useTeamFilters")
}

fun NavGraphBuilder.casesFilterScreen(
    onBack: () -> Unit,
) {
    composable(
        route = "$CASES_FILTER_ROUTE?$USE_TEAM_FILTERS_ARG={$USE_TEAM_FILTERS_ARG}",
        arguments = listOf(
            navArgument(USE_TEAM_FILTERS_ARG) {
                type = NavType.BoolType
                defaultValue = false
            },
        ),
    ) {
        CasesFilterRoute(
            onBack,
        )
    }
}
