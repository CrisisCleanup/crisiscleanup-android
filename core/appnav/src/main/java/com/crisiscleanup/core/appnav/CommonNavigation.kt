package com.crisiscleanup.core.appnav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.crisiscleanup.core.appnav.RouteConstant.VIEW_CASE_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.VIEW_CASE_ROUTE_RESTRICTED

const val INCIDENT_ID_ARG = "incidentId"
const val WORKSITE_ID_ARG = "worksiteId"

fun NavController.navigateToViewCase(
    incidentId: Long,
    worksiteId: Long,
    isRestricted: Boolean = false,
) {
    val path = if (isRestricted) VIEW_CASE_ROUTE_RESTRICTED else VIEW_CASE_ROUTE
    navigate("$path?$INCIDENT_ID_ARG=$incidentId&$WORKSITE_ID_ARG=$worksiteId")
}

fun List<String>.toRouteQuery() =
    joinToString("&") { "$it={$it}" }
        .let { if (it.isNotBlank()) "?$it" else "" }

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
