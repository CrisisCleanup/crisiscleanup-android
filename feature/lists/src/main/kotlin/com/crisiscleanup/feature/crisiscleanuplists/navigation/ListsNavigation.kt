package com.crisiscleanup.feature.crisiscleanuplists.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.crisiscleanup.core.appnav.RouteConstant
import com.crisiscleanup.feature.crisiscleanuplists.ui.ListsRoute

fun NavController.navigateToLists(navOptions: NavOptions? = null) {
    this.navigate(RouteConstant.LISTS_ROUTE, navOptions)
}

fun NavGraphBuilder.listsScreen(
    onBack: () -> Unit,
) {
    composable(route = RouteConstant.LISTS_ROUTE) {
        ListsRoute(
            onBack = onBack,
        )
    }
}
