package com.crisiscleanup.core.appnav

import androidx.navigation.NavController
import com.crisiscleanup.core.appnav.RouteConstant.VIEW_CASE_ROUTE

const val INCIDENT_ID_ARG = "incidentId"
const val WORKSITE_ID_ARG = "worksiteId"

fun NavController.navigateToExistingCase(incidentId: Long, worksiteId: Long) {
    // Must match composable route signature
    navigate("${VIEW_CASE_ROUTE}?$INCIDENT_ID_ARG=$incidentId&$WORKSITE_ID_ARG=$worksiteId")
}
