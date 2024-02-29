package com.crisiscleanup.feature.mediamanage.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.crisiscleanup.core.appnav.RouteConstant.VIEW_IMAGE_ROUTE
import com.crisiscleanup.core.appnav.ViewImageArgs.Companion.ENCODED_TITLE_ARG
import com.crisiscleanup.core.appnav.ViewImageArgs.Companion.ENCODED_URI_ARG
import com.crisiscleanup.core.appnav.ViewImageArgs.Companion.IMAGE_ID_ARG
import com.crisiscleanup.core.appnav.ViewImageArgs.Companion.IS_NETWORK_IMAGE_ARG
import com.crisiscleanup.feature.mediamanage.ui.ViewImageRoute

fun NavGraphBuilder.viewSingleImageScreen(
    onBack: () -> Unit,
) {
    val queryString = listOf(
        "$IMAGE_ID_ARG={$IMAGE_ID_ARG}",
        "$ENCODED_URI_ARG={$ENCODED_URI_ARG}",
        "$IS_NETWORK_IMAGE_ARG={$IS_NETWORK_IMAGE_ARG}",
        "$ENCODED_TITLE_ARG={$ENCODED_TITLE_ARG}",
    ).joinToString("&")
    composable(
        route = "$VIEW_IMAGE_ROUTE?$queryString",
        arguments = listOf(
            navArgument(IMAGE_ID_ARG) {
                type = NavType.LongType
            },
            navArgument(ENCODED_URI_ARG) {
                type = NavType.StringType
            },
            navArgument(IS_NETWORK_IMAGE_ARG) {
                type = NavType.BoolType
            },
            navArgument(ENCODED_TITLE_ARG) {
                type = NavType.StringType
            },
        ),
    ) {
        ViewImageRoute(onBack = onBack)
    }
}
