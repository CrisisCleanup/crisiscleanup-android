package com.crisiscleanup.feature.mediamanage.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.crisiscleanup.core.appnav.RouteConstant.viewImageRoute
import com.crisiscleanup.core.appnav.ViewImageArgs.Companion.encodedTitleArg
import com.crisiscleanup.core.appnav.ViewImageArgs.Companion.encodedUriArg
import com.crisiscleanup.core.appnav.ViewImageArgs.Companion.imageIdArg
import com.crisiscleanup.core.appnav.ViewImageArgs.Companion.isNetworkImageArg
import com.crisiscleanup.feature.mediamanage.ui.ViewImageRoute

fun NavGraphBuilder.viewSingleImageScreen(
    onBack: () -> Unit,
) {
    val queryString = listOf(
        "$imageIdArg={$imageIdArg}",
        "$encodedUriArg={$encodedUriArg}",
        "$isNetworkImageArg={$isNetworkImageArg}",
        "$encodedTitleArg={$encodedTitleArg}",
    ).joinToString("&")
    composable(
        route = "$viewImageRoute?$queryString",
        arguments = listOf(
            navArgument(imageIdArg) {
                type = NavType.LongType
            },
            navArgument(encodedUriArg) {
                type = NavType.StringType
            },
            navArgument(isNetworkImageArg) {
                type = NavType.BoolType
            },
            navArgument(encodedTitleArg) {
                type = NavType.StringType
            },
        ),
    ) {
        ViewImageRoute(onBack = onBack)
    }
}
