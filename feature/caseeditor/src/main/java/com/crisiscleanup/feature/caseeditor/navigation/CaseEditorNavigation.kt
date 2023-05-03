package com.crisiscleanup.feature.caseeditor.navigation

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.remember
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.*
import androidx.navigation.compose.composable
import com.crisiscleanup.core.appnav.RouteConstant.caseEditMapMoveLocationRoute
import com.crisiscleanup.core.appnav.RouteConstant.caseEditSearchAddressRoute
import com.crisiscleanup.core.appnav.RouteConstant.caseEditorRoute
import com.crisiscleanup.core.appnav.RouteConstant.viewCaseRoute
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.feature.caseeditor.ExistingWorksiteIdentifier
import com.crisiscleanup.feature.caseeditor.ui.CaseEditorRoute
import com.crisiscleanup.feature.caseeditor.ui.EditCaseAddressSearchRoute
import com.crisiscleanup.feature.caseeditor.ui.EditCaseMapMoveLocationRoute
import com.crisiscleanup.feature.caseeditor.ui.EditExistingCaseRoute

@VisibleForTesting
internal const val incidentIdArg = "incidentId"
internal const val worksiteIdArg = "worksiteId"

internal class CaseEditorArgs(val incidentId: Long, val worksiteId: Long?) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[incidentIdArg]),
        savedStateHandle.get<String>(worksiteIdArg)?.toLong(),
    )
}

internal class ExistingCaseArgs(val incidentId: Long, val worksiteId: Long) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[incidentIdArg]),
        checkNotNull(savedStateHandle[worksiteIdArg]),
    )
}

fun NavController.navigateToCaseEditor(incidentId: Long, worksiteId: Long? = null) {
    val routeParts = mutableListOf("$caseEditorRoute?$incidentIdArg=$incidentId")
    worksiteId?.let { routeParts.add("$worksiteIdArg=$worksiteId") }
    val route = routeParts.joinToString("&")
    this.navigate(route)
}


fun NavController.navigateToExistingCase(incidentId: Long, worksiteId: Long) {
    this.navigate("$viewCaseRoute?$incidentIdArg=$incidentId&$worksiteIdArg=$worksiteId")
}

fun NavGraphBuilder.caseEditorScreen(
    navController: NavHostController,
    onBackClick: () -> Unit,
) {
    composable(
        route = "$caseEditorRoute?$incidentIdArg={$incidentIdArg}&$worksiteIdArg={$worksiteIdArg}",
        arguments = listOf(
            navArgument(incidentIdArg) {
                type = NavType.LongType
                defaultValue = EmptyIncident.id
            },
            navArgument(worksiteIdArg) {
                nullable = true
            },
        ),
    ) {
        val navToEditCase = remember(navController) {
            { ids: ExistingWorksiteIdentifier -> navController.rerouteToCaseEdit(ids) }
        }
        val onEditSearchAddress =
            remember(navController) { { navController.navigateToCaseEditSearchAddress() } }
        val onEditMoveLocationOnMap =
            remember(navController) { { navController.navigateToCaseEditLocationMapMove() } }
        CaseEditorRoute(
            onBack = onBackClick,
            onOpenExistingCase = navToEditCase,
            onEditSearchAddress = onEditSearchAddress,
            onEditMoveLocationOnMap = onEditMoveLocationOnMap,
        )
    }
}

fun NavController.navigateToCaseEditSearchAddress() = this.navigate(caseEditSearchAddressRoute)
fun NavController.navigateToCaseEditLocationMapMove() = this.navigate(caseEditMapMoveLocationRoute)

fun NavGraphBuilder.existingCaseScreen(
    navController: NavHostController,
    onBackClick: () -> Unit,
) {
    composable(
        route = "$viewCaseRoute?$incidentIdArg={$incidentIdArg}&$worksiteIdArg={$worksiteIdArg}",
        arguments = listOf(
            navArgument(incidentIdArg) {
                type = NavType.LongType
                defaultValue = EmptyIncident.id
            },
            navArgument(worksiteIdArg) {
                type = NavType.LongType
                defaultValue = EmptyWorksite.id
            },
        ),
    ) {
        val navToEditCase = remember(navController) {
            { ids: ExistingWorksiteIdentifier ->
                navController.navigateToCaseEditor(
                    ids.incidentId,
                    ids.worksiteId,
                )
            }
        }
        EditExistingCaseRoute(
            onBack = onBackClick,
            onFullEdit = navToEditCase,
        )
    }
}

fun NavController.rerouteToCaseEdit(ids: ExistingWorksiteIdentifier) {
    popBackStack()
    currentBackStackEntry?.let {
        if (it.destination.route?.startsWith(caseEditorRoute) == true) {
            popBackStack()
        }
    }
    navigateToCaseEditor(ids.incidentId, ids.worksiteId)
}

fun NavGraphBuilder.caseEditSearchAddressScreen(
    navController: NavHostController,
    onBack: () -> Unit,
) {
    composable(caseEditSearchAddressRoute) {
        val navToEditCase = remember(navController) {
            { ids: ExistingWorksiteIdentifier -> navController.rerouteToCaseEdit(ids) }
        }
        EditCaseAddressSearchRoute(
            onBack = onBack,
            openExistingCase = navToEditCase,
        )
    }
}

fun NavGraphBuilder.caseEditMoveLocationOnMapScreen(
    onBackClick: () -> Unit
) {
    composable(caseEditMapMoveLocationRoute) {
        EditCaseMapMoveLocationRoute(onBack = onBackClick)
    }
}