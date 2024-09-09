package com.crisiscleanup.feature.userfeedback.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.crisiscleanup.core.appnav.RouteConstant.USER_FEEDBACK_ROUTE
import com.crisiscleanup.feature.userfeedback.ui.UserFeedbackRoute

fun NavController.navigateToUserFeedback() {
    navigate(USER_FEEDBACK_ROUTE)
}

fun NavGraphBuilder.userFeedbackScreen(
    onBack: () -> Unit = {},
) {
    composable(route = USER_FEEDBACK_ROUTE) {
        UserFeedbackRoute(onBack = onBack)
    }
}
