package com.crisiscleanup.feature.authentication.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.crisiscleanup.core.appnav.RouteConstant.ORG_PERSISTENT_INVITE_ROUTE
import com.crisiscleanup.feature.authentication.ui.OrgPersistentInviteRoute

fun NavController.navigateToOrgPersistentInvite() {
    navigate(ORG_PERSISTENT_INVITE_ROUTE)
}

fun NavGraphBuilder.orgPersistentInviteScreen(
    onBack: () -> Unit,
    closeInvite: () -> Unit,
) {
    composable(
        route = ORG_PERSISTENT_INVITE_ROUTE,
    ) {
        OrgPersistentInviteRoute(
            onBack = onBack,
            closeInvite = closeInvite,
        )
    }
}
