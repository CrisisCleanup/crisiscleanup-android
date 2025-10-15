package com.crisiscleanup.core.appnav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.crisiscleanup.core.appnav.RouteConstant.VIEW_CASE_ROUTE

const val INCIDENT_ID_ARG = "incidentId"
const val WORKSITE_ID_ARG = "worksiteId"

fun NavController.navigateToExistingCase(incidentId: Long, worksiteId: Long) {
    // Must match composable route signature
    this.navigate("${VIEW_CASE_ROUTE}?$INCIDENT_ID_ARG=$incidentId&$WORKSITE_ID_ARG=$worksiteId")
}

// https://medium.com/@mahbooberezaee68/retrieving-viewmodels-within-navigation-graphs-jetpack-compose-4eb5a1293d25
@Composable
inline fun <reified T : ViewModel> NavBackStackEntry.sharedViewModel(
    navController: NavController,
    navGraphRoute: String,
): T {
    val parentEntry = remember(this) {
        navController.getBackStackEntry(navGraphRoute)
    }
    return hiltViewModel(parentEntry)
}
