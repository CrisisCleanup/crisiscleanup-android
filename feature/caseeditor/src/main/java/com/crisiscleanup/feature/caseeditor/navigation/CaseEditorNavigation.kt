package com.crisiscleanup.feature.caseeditor.navigation

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.remember
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.*
import androidx.navigation.compose.composable
import com.crisiscleanup.core.appnav.RouteConstant.caseEditLocationRoute
import com.crisiscleanup.core.appnav.RouteConstant.caseEditPropertyRoute
import com.crisiscleanup.core.appnav.RouteConstant.caseEditorRoute
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.feature.caseeditor.ExistingWorksiteIdentifier
import com.crisiscleanup.feature.caseeditor.ui.CaseEditorRoute
import com.crisiscleanup.feature.caseeditor.ui.EditCaseLocationRoute
import com.crisiscleanup.feature.caseeditor.ui.EditCasePropertyRoute

@VisibleForTesting
internal const val incidentIdArg = "incidentId"
internal const val worksiteIdArg = "worksiteId"

internal class CaseEditorArgs(val incidentId: Long, val worksiteId: Long?) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[incidentIdArg]),
        savedStateHandle.get<String>(worksiteIdArg)?.toLong(),
    )
}

fun NavController.navigateToCaseEditor(incidentId: Long, worksiteId: Long? = null) {
    val routeParts = mutableListOf("$caseEditorRoute?$incidentIdArg=$incidentId")
    worksiteId?.let { routeParts.add("$worksiteIdArg=$worksiteId") }
    val route = routeParts.joinToString("&")
    this.navigate(route)
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
        val onEditPropertyData =
            remember(navController) { { navController.navigateToCaseEditProperty() } }
        val onEditLocation =
            remember(navController) { { navController.navigateToCaseEditLocation() } }
        CaseEditorRoute(
            onBackClick = onBackClick,
            onEditPropertyData = onEditPropertyData,
            onEditLocation = onEditLocation,
        )
    }
}

fun NavController.navigateToCaseEditProperty() = this.navigate(caseEditPropertyRoute)
fun NavController.navigateToCaseEditLocation() = this.navigate(caseEditLocationRoute)

fun NavController.rerouteToCaseEdit(ids: ExistingWorksiteIdentifier) {
    popBackStack()
    currentBackStackEntry?.let {
        if (it.destination.route?.startsWith(caseEditorRoute) == true) {
            popBackStack()
        }
    }
    navigateToCaseEditor(ids.incidentId, ids.worksiteId)
}

fun NavGraphBuilder.caseEditPropertyScreen(
    navController: NavHostController,
    onBackClick: () -> Unit,
) {
    composable(caseEditPropertyRoute) {
        val navToEditCase = remember(navController) {
            { ids: ExistingWorksiteIdentifier ->
                navController.rerouteToCaseEdit(
                    ids
                )
            }
        }
        EditCasePropertyRoute(
            onBackClick = onBackClick,
            openExistingCase = navToEditCase,
        )
    }
}

fun NavGraphBuilder.caseEditLocationScreen(
    navController: NavHostController,
    onBackClick: () -> Unit,
) {
    composable(caseEditLocationRoute) {
        val navToEditCase = remember(navController) {
            { ids: ExistingWorksiteIdentifier ->
                navController.rerouteToCaseEdit(
                    ids
                )
            }
        }
        EditCaseLocationRoute(
            onBackClick = onBackClick,
            openExistingCase = navToEditCase,
        )
    }
}