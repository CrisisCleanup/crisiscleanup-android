package com.crisiscleanup.feature.caseeditor.navigation

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.remember
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.*
import androidx.navigation.compose.composable
import com.crisiscleanup.core.appnav.RouteConstant.caseEditDetailsRoute
import com.crisiscleanup.core.appnav.RouteConstant.caseEditHazardsRoute
import com.crisiscleanup.core.appnav.RouteConstant.caseEditLocationRoute
import com.crisiscleanup.core.appnav.RouteConstant.caseEditNotesFlagsRoute
import com.crisiscleanup.core.appnav.RouteConstant.caseEditPropertyRoute
import com.crisiscleanup.core.appnav.RouteConstant.caseEditVolunteerReportRoute
import com.crisiscleanup.core.appnav.RouteConstant.caseEditWorkRoute
import com.crisiscleanup.core.appnav.RouteConstant.caseEditorRoute
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.feature.caseeditor.ExistingWorksiteIdentifier
import com.crisiscleanup.feature.caseeditor.ui.*

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
        val onEditNotesFlags =
            remember(navController) { { navController.navigateToCaseEditNotesFlags() } }
        val onEditDetails =
            remember(navController) { { navController.navigateToCaseEditDetails() } }
        val onEditWork =
            remember(navController) { { navController.navigateToCaseEditWork() } }
        val onEditHazards =
            remember(navController) { { navController.navigateToCaseEditHazards() } }
        val onEditVolunteerReport =
            remember(navController) { { navController.navigateToCaseEditVolunteerReport() } }
        CaseEditorRoute(
            onBackClick = onBackClick,
            onEditPropertyData = onEditPropertyData,
            onEditLocation = onEditLocation,
            onEditNotesFlags = onEditNotesFlags,
            onEditDetails = onEditDetails,
            onEditWork = onEditWork,
            onEditHazards = onEditHazards,
            onEditVolunteerReport = onEditVolunteerReport,
        )
    }
}

fun NavController.navigateToCaseEditProperty() = this.navigate(caseEditPropertyRoute)
fun NavController.navigateToCaseEditLocation() = this.navigate(caseEditLocationRoute)
fun NavController.navigateToCaseEditNotesFlags() = this.navigate(caseEditNotesFlagsRoute)
fun NavController.navigateToCaseEditDetails() = this.navigate(caseEditDetailsRoute)
fun NavController.navigateToCaseEditWork() = this.navigate(caseEditWorkRoute)
fun NavController.navigateToCaseEditHazards() = this.navigate(caseEditHazardsRoute)
fun NavController.navigateToCaseEditVolunteerReport() = this.navigate(caseEditVolunteerReportRoute)

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

fun NavGraphBuilder.caseEditNotesFlagsScreen(
    onBackClick: () -> Unit,
) {
    composable(caseEditNotesFlagsRoute) {
        EditCaseNotesFlagsRoute(onBackClick = onBackClick)
    }
}

fun NavGraphBuilder.caseEditDetailsScreen(
    onBackClick: () -> Unit,
) {
    composable(caseEditDetailsRoute) {
        EditCaseDetailsRoute(onBackClick = onBackClick)
    }
}

fun NavGraphBuilder.caseEditWorkScreen(
    onBackClick: () -> Unit,
) {
    composable(caseEditWorkRoute) {
        EditCaseWorkRoute(onBackClick = onBackClick)
    }
}

fun NavGraphBuilder.caseEditHazardsScreen(
    onBackClick: () -> Unit,
) {
    composable(caseEditHazardsRoute) {
        EditCaseHazardsRoute(onBackClick = onBackClick)
    }
}

fun NavGraphBuilder.caseEditVolunteerReportScreen(
    onBackClick: () -> Unit,
) {
    composable(caseEditVolunteerReportRoute) {
        EditCaseVolunteerReportRoute(onBackClick = onBackClick)
    }
}