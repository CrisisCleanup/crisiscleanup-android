package com.crisiscleanup.feature.cases.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.crisiscleanup.core.appnav.RouteConstant
import com.crisiscleanup.feature.cases.ui.CasesFilterRoute

fun NavController.navigateToCasesFilter(navOptions: NavOptions? = null) {
    this.navigate(RouteConstant.casesFilterRoute, navOptions)
}

fun NavGraphBuilder.casesFilterScreen(
    onBack: () -> Unit,
) {
    composable(route = RouteConstant.casesFilterRoute) {
        CasesFilterRoute(
            onBack,
        )
    }
}
