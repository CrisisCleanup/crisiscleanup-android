package com.crisiscleanup.feature.userfeedback.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.crisiscleanup.core.appnav.RouteConstant
import com.crisiscleanup.feature.userfeedback.ui.UserFeedbackRoute

fun NavController.navigateToUserFeedback(navOptions: NavOptions? = null) {
    this.navigate(RouteConstant.userFeedbackRoute, navOptions)
}

fun NavGraphBuilder.userFeedbackScreen(
    onBack: () -> Unit = {},
) {
    composable(route = RouteConstant.userFeedbackRoute) {
        UserFeedbackRoute(onBack = onBack)
    }
}
