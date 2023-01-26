package com.crisiscleanup.feature.cases.navigation

import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.crisiscleanup.feature.cases.CasesRoute
import com.crisiscleanup.feature.cases.ui.CasesAction

const val casesGraphRoutePattern = "cases_graph"

// This cannot be used as the navHost startDestination
const val casesRoute = "cases_route"

fun NavController.navigateToCases(navOptions: NavOptions? = null) {
    this.navigate(casesGraphRoutePattern, navOptions)
}

fun NavGraphBuilder.casesGraph(
    navController: NavHostController,
    nestedGraphs: NavGraphBuilder.() -> Unit,
    onCasesAction: (CasesAction) -> Unit = { },
) {
    navigation(
        route = casesGraphRoutePattern,
        startDestination = casesRoute,
    ) {
        composable(route = casesRoute) {
            val rememberOnCasesAction = remember(onCasesAction) {
                { casesAction: CasesAction ->
                    when (casesAction) {
                        CasesAction.CreateNew -> {
                            // TODO Navigate to new screen
                        }

                        CasesAction.Filters -> {
                            // TODO Navigate to filters screen
                        }

                        else -> {
                            onCasesAction(casesAction)
                        }
                    }
                }
            }

            CasesRoute(
                onCasesAction = rememberOnCasesAction,
            )
        }

        nestedGraphs()
    }
}