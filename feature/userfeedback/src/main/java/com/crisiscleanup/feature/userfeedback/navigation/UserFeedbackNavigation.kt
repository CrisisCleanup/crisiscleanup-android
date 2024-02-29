package com.crisiscleanup.feature.userfeedback.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.crisiscleanup.core.appnav.RouteConstant.USER_FEEDBACK_ROUTE
import com.crisiscleanup.feature.userfeedback.ui.UserFeedbackRoute

fun NavController.navigateToUserFeedback(navOptions: NavOptions? = null) {
    this.navigate(USER_FEEDBACK_ROUTE, navOptions)
}

fun NavGraphBuilder.userFeedbackScreen(
    onBack: () -> Unit = {},
) {
    composable(route = USER_FEEDBACK_ROUTE) {
        UserFeedbackRoute(onBack = onBack)
    }
}
