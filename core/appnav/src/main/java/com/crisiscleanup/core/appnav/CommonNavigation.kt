package com.crisiscleanup.core.appnav

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
