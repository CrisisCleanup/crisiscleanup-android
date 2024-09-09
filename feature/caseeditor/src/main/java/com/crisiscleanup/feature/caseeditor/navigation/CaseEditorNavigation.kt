package com.crisiscleanup.feature.caseeditor.navigation

import androidx.compose.runtime.remember
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.crisiscleanup.core.appnav.INCIDENT_ID_ARG
import com.crisiscleanup.core.appnav.RouteConstant.CASES_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.CASE_ADD_FLAG_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.CASE_EDITOR_MAP_MOVE_LOCATION_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.CASE_EDITOR_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.CASE_EDITOR_SEARCH_ADDRESS_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.CASE_HISTORY_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.CASE_SHARE_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.VIEW_CASE_ROUTE
import com.crisiscleanup.core.appnav.RouteConstant.VIEW_CASE_TRANSFER_WORK_TYPES_ROUTE
import com.crisiscleanup.core.appnav.WORKSITE_ID_ARG
import com.crisiscleanup.core.appnav.navigateToExistingCase
import com.crisiscleanup.core.appnav.navigateToWorksiteImages
import com.crisiscleanup.core.data.model.ExistingWorksiteIdentifier
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.feature.caseeditor.ui.CaseEditCaseHistoryRoute
import com.crisiscleanup.feature.caseeditor.ui.CaseEditShareCaseRoute
import com.crisiscleanup.feature.caseeditor.ui.CreateEditCaseRoute
import com.crisiscleanup.feature.caseeditor.ui.EditCaseAddressSearchRoute
import com.crisiscleanup.feature.caseeditor.ui.EditCaseMapMoveLocationRoute
import com.crisiscleanup.feature.caseeditor.ui.EditExistingCaseRoute
import com.crisiscleanup.feature.caseeditor.ui.TransferWorkTypesRoute
import com.crisiscleanup.feature.caseeditor.ui.addflag.CaseEditAddFlagRoute

internal const val IS_FROM_CASE_EDIT_ARG = "isFromCaseEdit"

internal class CaseEditorArgs(val incidentId: Long, val worksiteId: Long?) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[INCIDENT_ID_ARG]),
        savedStateHandle.get<String>(WORKSITE_ID_ARG)?.toLong(),
    )
}

internal class ExistingCaseArgs(val incidentId: Long, val worksiteId: Long) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[INCIDENT_ID_ARG]),
        checkNotNull(savedStateHandle[WORKSITE_ID_ARG]),
    )
}

internal class CaseAddFlagArgs(val isFromCaseEdit: Boolean) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle.get<Boolean>(IS_FROM_CASE_EDIT_ARG)),
    )
}

internal class TransferWorkTypeArgs(val isFromCaseEdit: Boolean) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle.get<Boolean>(IS_FROM_CASE_EDIT_ARG)),
    )
}

fun NavController.navigateToViewCase(incidentId: Long, worksiteId: Long) {
    val route = "$VIEW_CASE_ROUTE?$INCIDENT_ID_ARG=$incidentId&$WORKSITE_ID_ARG=$worksiteId"
    navigate(route)
}

fun NavController.navigateToCaseEditor(incidentId: Long, worksiteId: Long? = null) {
    val routeParts = mutableListOf("$CASE_EDITOR_ROUTE?$INCIDENT_ID_ARG=$incidentId")
    worksiteId?.let { routeParts.add("$WORKSITE_ID_ARG=$worksiteId") }
    val route = routeParts.joinToString("&")
    navigate(route)
}

fun NavGraphBuilder.caseEditorScreen(
    navController: NavHostController,
    onBack: () -> Unit,
) {
    composable(
        route = "$CASE_EDITOR_ROUTE?$INCIDENT_ID_ARG={$INCIDENT_ID_ARG}&$WORKSITE_ID_ARG={$WORKSITE_ID_ARG}",
        arguments = listOf(
            navArgument(INCIDENT_ID_ARG) {
                type = NavType.LongType
                defaultValue = EmptyIncident.id
            },
            navArgument(WORKSITE_ID_ARG) {
                nullable = true
            },
        ),
    ) {
        val navToNewCase = navController::rerouteToNewCase
        val navToChangedIncident = navController::rerouteToCaseChange
        val navToEditCase = navController::rerouteToCaseEdit
        val onEditSearchAddress = navController::navigateToCaseEditSearchAddress
        val onEditMoveLocationOnMap = navController::navigateToCaseEditLocationMapMove
        val navToWorksiteImages = navController::navigateToWorksiteImages
        CreateEditCaseRoute(
            onBack = onBack,
            changeNewIncidentCase = navToNewCase,
            changeExistingIncidentCase = navToChangedIncident,
            onOpenExistingCase = navToEditCase,
            onEditSearchAddress = onEditSearchAddress,
            onEditMoveLocationOnMap = onEditMoveLocationOnMap,
            openPhoto = navToWorksiteImages,
        )
    }
}

fun NavController.navigateToCaseEditSearchAddress() =
    navigate(CASE_EDITOR_SEARCH_ADDRESS_ROUTE)

fun NavController.navigateToCaseEditLocationMapMove() =
    navigate(CASE_EDITOR_MAP_MOVE_LOCATION_ROUTE)

fun NavController.navigateToCaseAddFlag(isFromCaseEdit: Boolean) {
    navigate("$CASE_ADD_FLAG_ROUTE?$IS_FROM_CASE_EDIT_ARG=$isFromCaseEdit")
}

fun NavController.navigateToCaseShare() = navigate(CASE_SHARE_ROUTE)
fun NavController.navigateToCaseHistory() = navigate(CASE_HISTORY_ROUTE)

fun NavGraphBuilder.existingCaseScreen(
    navController: NavHostController,
    onBack: () -> Unit,
) {
    composable(
        route = "$VIEW_CASE_ROUTE?$INCIDENT_ID_ARG={$INCIDENT_ID_ARG}&$WORKSITE_ID_ARG={$WORKSITE_ID_ARG}",
        arguments = listOf(
            navArgument(INCIDENT_ID_ARG) {
                type = NavType.LongType
                defaultValue = EmptyIncident.id
            },
            navArgument(WORKSITE_ID_ARG) {
                type = NavType.LongType
                defaultValue = EmptyWorksite.id
            },
        ),
    ) {
        val navBackToCases = navController::popToWork
        val navToEditCase = remember(navController) {
            { ids: ExistingWorksiteIdentifier ->
                navController.navigateToCaseEditor(
                    ids.incidentId,
                    ids.worksiteId,
                )
            }
        }
        val navToTransferWorkType = remember(navController) {
            {
                navController.navigateToTransferWorkType(true)
            }
        }
        val navToWorksiteImages = navController::navigateToWorksiteImages
        val navToCaseAddFlag =
            remember(navController) { { navController.navigateToCaseAddFlag(true) } }
        val navToCaseShare = navController::navigateToCaseShare
        val navToCaseHistory = navController::navigateToCaseHistory
        EditExistingCaseRoute(
            onBack = onBack,
            onBackToCases = navBackToCases,
            onFullEdit = navToEditCase,
            openTransferWorkType = navToTransferWorkType,
            openPhoto = navToWorksiteImages,
            openAddFlag = navToCaseAddFlag,
            openShareCase = navToCaseShare,
            openCaseHistory = navToCaseHistory,
        )
    }
}

fun NavController.navigateToTransferWorkType(isFromCaseEdit: Boolean) =
    navigate("$VIEW_CASE_TRANSFER_WORK_TYPES_ROUTE?$IS_FROM_CASE_EDIT_ARG=$isFromCaseEdit")

internal fun NavController.popRouteStartingWith(route: String) {
    popBackStack()
    while (currentBackStackEntry?.destination?.route?.startsWith(route) == true) {
        popBackStack()
    }
}

private fun NavController.popToWork() {
    popBackStack(CASES_ROUTE, false, saveState = false)
}

fun NavController.rerouteToNewCase(incidentId: Long) {
    popRouteStartingWith(CASE_EDITOR_ROUTE)
    navigateToCaseEditor(incidentId)
}

fun NavController.rerouteToCaseEdit(ids: ExistingWorksiteIdentifier) {
    popToWork()
    navigateToExistingCase(ids.incidentId, ids.worksiteId)
    navigateToCaseEditor(ids.incidentId, ids.worksiteId)
}

fun NavController.rerouteToCaseChange(ids: ExistingWorksiteIdentifier) {
    popBackStack()
    while (currentBackStackEntry?.destination?.route?.let {
            it.startsWith(CASE_EDITOR_ROUTE) ||
                it.startsWith(VIEW_CASE_ROUTE)
        } == true
    ) {
        popBackStack()
    }

    navigateToCaseEditor(ids.incidentId, ids.worksiteId)
}

fun NavGraphBuilder.caseEditSearchAddressScreen(
    navController: NavHostController,
    onBack: () -> Unit,
) {
    composable(CASE_EDITOR_SEARCH_ADDRESS_ROUTE) {
        EditCaseAddressSearchRoute(
            onBack = onBack,
            openExistingCase = navController::rerouteToCaseEdit,
        )
    }
}

fun NavGraphBuilder.caseEditMoveLocationOnMapScreen(
    onBack: () -> Unit,
) {
    composable(CASE_EDITOR_MAP_MOVE_LOCATION_ROUTE) {
        EditCaseMapMoveLocationRoute(onBack)
    }
}

fun NavGraphBuilder.existingCaseTransferWorkTypesScreen(
    onBack: () -> Unit = {},
) {
    composable(
        route = "$VIEW_CASE_TRANSFER_WORK_TYPES_ROUTE?$IS_FROM_CASE_EDIT_ARG={$IS_FROM_CASE_EDIT_ARG}",
        arguments = listOf(
            navArgument(IS_FROM_CASE_EDIT_ARG) {
                type = NavType.BoolType
                defaultValue = false
            },
        ),
    ) {
        TransferWorkTypesRoute(onBack)
    }
}

fun NavGraphBuilder.caseAddFlagScreen(
    onBack: () -> Unit = {},
    rerouteIncidentChange: (ExistingWorksiteIdentifier) -> Unit = {},
) {
    composable(
        route = "$CASE_ADD_FLAG_ROUTE?$IS_FROM_CASE_EDIT_ARG={$IS_FROM_CASE_EDIT_ARG}",
        arguments = listOf(
            navArgument(IS_FROM_CASE_EDIT_ARG) {
                type = NavType.BoolType
                defaultValue = false
            },
        ),
    ) {
        CaseEditAddFlagRoute(
            onBack = onBack,
            rerouteIncidentChange = rerouteIncidentChange,
        )
    }
}

fun NavGraphBuilder.caseShareScreen(
    onBack: () -> Unit = {},
) {
    composable(route = CASE_SHARE_ROUTE) {
        CaseEditShareCaseRoute(onBack)
    }
}

fun NavGraphBuilder.caseHistoryScreen(
    onBack: () -> Unit = {},
) {
    composable(route = CASE_HISTORY_ROUTE) {
        CaseEditCaseHistoryRoute(onBack)
    }
}
