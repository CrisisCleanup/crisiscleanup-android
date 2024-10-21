package com.crisiscleanup.feature.cases.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.crisiscleanup.core.appnav.RouteConstant.CASES_SEARCH_ROUTE
import com.crisiscleanup.feature.cases.ui.CasesSearchRoute

fun NavController.navigateToCasesSearch(navOptions: NavOptions? = null) {
    this.navigate(CASES_SEARCH_ROUTE, navOptions)
}

fun NavGraphBuilder.casesSearchScreen(
    onBack: () -> Unit,
    openCase: (Long, Long) -> Boolean = { _, _ -> false },
) {
    composable(route = CASES_SEARCH_ROUTE) {
        CasesSearchRoute(
            onBack = onBack,
            openCase = openCase,
        )
    }
}
