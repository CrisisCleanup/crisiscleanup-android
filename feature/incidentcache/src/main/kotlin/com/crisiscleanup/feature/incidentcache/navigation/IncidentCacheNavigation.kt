package com.crisiscleanup.feature.incidentcache.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.crisiscleanup.core.appnav.RouteConstant.INCIDENT_WORKSITES_CACHE_ROUTE
import com.crisiscleanup.feature.incidentcache.ui.IncidentWorksitesCacheRoute

fun NavController.navigateToIncidentWorksitesCache() {
    navigate(INCIDENT_WORKSITES_CACHE_ROUTE)
}

fun NavGraphBuilder.incidentWorksitesCache(
    onBack: () -> Unit = {},
) {
    composable(INCIDENT_WORKSITES_CACHE_ROUTE) {
        IncidentWorksitesCacheRoute(onBack)
    }
}
