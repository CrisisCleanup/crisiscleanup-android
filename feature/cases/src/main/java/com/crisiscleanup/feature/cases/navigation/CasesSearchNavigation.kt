package com.crisiscleanup.feature.cases.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.crisiscleanup.core.appnav.RouteConstant
import com.crisiscleanup.feature.cases.ui.CasesSearchRoute

fun NavController.navigateToCasesSearch(navOptions: NavOptions? = null) {
    this.navigate(RouteConstant.casesSearchRoute, navOptions)
}

fun NavGraphBuilder.casesSearchScreen(
    onBackClick: () -> Unit,
    openCase: (Long, Long) -> Boolean = { _, _ -> false },
) {
    composable(route = RouteConstant.casesSearchRoute) {
        CasesSearchRoute(
            onBackClick,
            openCase = openCase,
        )
    }
}
