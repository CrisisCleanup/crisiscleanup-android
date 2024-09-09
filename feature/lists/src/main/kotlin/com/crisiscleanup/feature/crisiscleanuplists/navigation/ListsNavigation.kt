package com.crisiscleanup.feature.crisiscleanuplists.navigation

import androidx.compose.runtime.remember
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.crisiscleanup.core.appnav.RouteConstant.LISTS_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.VIEW_LIST_ROUTE
import com.crisiscleanup.core.data.model.ExistingWorksiteIdentifier
import com.crisiscleanup.core.model.data.CrisisCleanupList
import com.crisiscleanup.core.model.data.EmptyList
import com.crisiscleanup.feature.crisiscleanuplists.ui.ListsRoute
import com.crisiscleanup.feature.crisiscleanuplists.ui.ViewListRoute

internal const val LIST_ID_ARG = "list_id"

internal class ViewListArgs(val listId: Long) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle.get<Long>(LIST_ID_ARG)),
    )
}

fun NavController.navigateToLists() {
    navigate(LISTS_ROUTE)
}

fun NavController.navigateToViewList(listId: Long) {
    navigate("${VIEW_LIST_ROUTE}?$LIST_ID_ARG=$listId")
}

fun NavGraphBuilder.listsScreen(
    navController: NavHostController,
    onBack: () -> Unit,
) {
    composable(route = LISTS_ROUTE) {
        val onOpenList = remember(navController) {
            { list: CrisisCleanupList ->
                navController.navigateToViewList(list.id)
            }
        }

        ListsRoute(
            onBack = onBack,
            onOpenList = onOpenList,
        )
    }
}

fun NavGraphBuilder.viewListScreen(
    onBack: () -> Unit,
    openList: (Long) -> Unit,
    openWorksite: (ExistingWorksiteIdentifier) -> Unit,
) {
    composable(
        route = "$VIEW_LIST_ROUTE?$LIST_ID_ARG={$LIST_ID_ARG}",
        arguments = listOf(
            navArgument(LIST_ID_ARG) {
                type = NavType.LongType
                defaultValue = EmptyList.id
            },
        ),
    ) {
        ViewListRoute(
            onBack = onBack,
            onOpenList = openList,
            onOpenWorksite = openWorksite,
        )
    }
}
