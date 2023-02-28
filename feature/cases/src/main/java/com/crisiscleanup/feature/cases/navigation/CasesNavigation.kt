package com.crisiscleanup.feature.cases.navigation

import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.crisiscleanup.core.appnav.RouteConstant.casesGraphRoutePattern
import com.crisiscleanup.core.appnav.RouteConstant.casesRoute
import com.crisiscleanup.feature.cases.ui.CasesAction
import com.crisiscleanup.feature.cases.ui.CasesRoute

fun NavController.navigateToCases(navOptions: NavOptions? = null) {
    this.navigate(casesGraphRoutePattern, navOptions)
}

fun NavGraphBuilder.casesGraph(
    nestedGraphs: NavGraphBuilder.() -> Unit,
    onCasesAction: (CasesAction) -> Unit = { },
    createCase: (Long) -> Unit = {},
    editCase: (Long, Long) -> Boolean = { _, _ -> false },
) {
    navigation(
        route = casesGraphRoutePattern,
        startDestination = casesRoute,
    ) {
        composable(route = casesRoute) {
            val rememberOnCasesAction = remember(onCasesAction) {
                { casesAction: CasesAction ->
                    when (casesAction) {
                        CasesAction.Filters -> {
                            // TODO Navigate to filters screen
                        }

                        else -> onCasesAction(casesAction)
                    }
                }
            }
            CasesRoute(
                onCasesAction = rememberOnCasesAction,
                createNewCase = createCase,
                openCase = editCase,
            )
        }

        nestedGraphs()
    }
}