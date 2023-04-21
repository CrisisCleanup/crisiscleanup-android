package com.crisiscleanup.feature.cases.navigation

import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.dialog
import com.crisiscleanup.core.appnav.RouteConstant.selectIncidentRoute
import com.crisiscleanup.feature.cases.ui.SelectIncidentRoute

fun NavController.navigateToSelectIncident(navOptions: NavOptions? = null) {
    this.navigate(selectIncidentRoute, navOptions)
}

fun NavGraphBuilder.selectIncidentScreen(
    onBackClick: () -> Unit
) {
    // TODO Refactor related code. SelectIncidentRoute also wraps in Dialog (with properties)
    dialog(
        route = selectIncidentRoute,
        dialogProperties = DialogProperties()
    ) {
        SelectIncidentRoute(onBackClick)
    }
}