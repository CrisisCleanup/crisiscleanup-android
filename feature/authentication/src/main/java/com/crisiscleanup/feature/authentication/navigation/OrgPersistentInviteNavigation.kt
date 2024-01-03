package com.crisiscleanup.feature.authentication.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.crisiscleanup.core.appnav.RouteConstant.orgPersistentInviteRoute
import com.crisiscleanup.feature.authentication.ui.OrgPersistentInviteRoute

fun NavController.navigateToOrgPersistentInvite() {
    navigate(orgPersistentInviteRoute)
}

fun NavGraphBuilder.orgPersistentInviteScreen(
    onBack: () -> Unit,
    closeInvite: () -> Unit,
) {
    composable(
        route = orgPersistentInviteRoute,
    ) {
        OrgPersistentInviteRoute(
            onBack = onBack,
            closeInvite = closeInvite,
        )
    }
}
