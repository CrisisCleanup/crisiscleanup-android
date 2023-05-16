package com.crisiscleanup.feature.mediamanage.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.crisiscleanup.core.appnav.RouteConstant.viewImageRoute
import com.crisiscleanup.core.appnav.ViewImageArgs.Companion.imageIdArg
import com.crisiscleanup.core.appnav.ViewImageArgs.Companion.imageUrlArg
import com.crisiscleanup.core.appnav.ViewImageArgs.Companion.isNetworkImageArg
import com.crisiscleanup.feature.mediamanage.ui.ViewImageRoute

fun NavGraphBuilder.viewImageScreen(
    onBack: () -> Unit,
) {
    composable(
        route = "$viewImageRoute?$imageIdArg={$imageIdArg}&$imageUrlArg={$imageUrlArg}&$isNetworkImageArg={$isNetworkImageArg}",
        arguments = listOf(
            navArgument(imageIdArg) {
                type = NavType.LongType
            },
            navArgument(imageUrlArg) {
                type = NavType.StringType
            },
            navArgument(isNetworkImageArg) {
                type = NavType.BoolType
            },
        ),
    ) {
        ViewImageRoute(onBack = onBack)
    }
}
