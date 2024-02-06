package com.crisiscleanup.core.appnav

import androidx.navigation.NavController
import com.crisiscleanup.core.appnav.RouteConstant.viewCaseRoute

const val incidentIdArg = "incidentId"
const val worksiteIdArg = "worksiteId"

fun NavController.navigateToExistingCase(incidentId: Long, worksiteId: Long) {
    // Must match composable route signature
    this.navigate("${viewCaseRoute}?$incidentIdArg=$incidentId&$worksiteIdArg=$worksiteId")
}
