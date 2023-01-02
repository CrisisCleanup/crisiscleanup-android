package com.crisiscleanup.feature.cases.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

const val casesRoute = "cases_route"

fun NavController.navigateToCases(navOptions: NavOptions? = null) {
    this.navigate(casesRoute, navOptions)
}

fun NavGraphBuilder.casesScreen() {
    composable(route = casesRoute) {
//        CasesRoute()
    }
}
