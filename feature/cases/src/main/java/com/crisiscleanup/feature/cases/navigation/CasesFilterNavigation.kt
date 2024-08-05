package com.crisiscleanup.feature.cases.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.crisiscleanup.core.appnav.RouteConstant
import com.crisiscleanup.feature.cases.ui.CasesFilterRoute

fun NavController.navigateToCasesFilter() {
    this.navigate(RouteConstant.CASES_FILTER_ROUTE)
}

fun NavGraphBuilder.casesFilterScreen(
    onBack: () -> Unit,
) {
    composable(route = RouteConstant.CASES_FILTER_ROUTE) {
        CasesFilterRoute(
            onBack,
        )
    }
}
