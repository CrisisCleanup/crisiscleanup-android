package com.crisiscleanup.feature.caseeditor.navigation

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.remember
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.crisiscleanup.core.appnav.RouteConstant.caseAddFlagRoute
import com.crisiscleanup.core.appnav.RouteConstant.caseEditMapMoveLocationRoute
import com.crisiscleanup.core.appnav.RouteConstant.caseEditSearchAddressRoute
import com.crisiscleanup.core.appnav.RouteConstant.caseEditorRoute
import com.crisiscleanup.core.appnav.RouteConstant.caseHistoryRoute
import com.crisiscleanup.core.appnav.RouteConstant.caseShareRoute
import com.crisiscleanup.core.appnav.RouteConstant.viewCaseRoute
import com.crisiscleanup.core.appnav.RouteConstant.viewCaseTransferWorkTypesRoute
import com.crisiscleanup.core.appnav.ViewImageArgs
import com.crisiscleanup.core.appnav.navigateToViewImage
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.feature.caseeditor.ExistingWorksiteIdentifier
import com.crisiscleanup.feature.caseeditor.ui.CaseEditCaseHistoryRoute
import com.crisiscleanup.feature.caseeditor.ui.CaseEditShareCaseRoute
import com.crisiscleanup.feature.caseeditor.ui.CaseEditorRoute
import com.crisiscleanup.feature.caseeditor.ui.EditCaseAddressSearchRoute
import com.crisiscleanup.feature.caseeditor.ui.EditCaseMapMoveLocationRoute
import com.crisiscleanup.feature.caseeditor.ui.EditExistingCaseRoute
import com.crisiscleanup.feature.caseeditor.ui.TransferWorkTypesRoute
import com.crisiscleanup.feature.caseeditor.ui.addflag.CaseEditAddFlagRoute

@VisibleForTesting
internal const val incidentIdArg = "incidentId"
internal const val worksiteIdArg = "worksiteId"
internal const val isFromCaseEditArg = "isFromCaseEdit"

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

internal class CaseAddFlagArgs(val isFromCaseEdit: Boolean) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle.get<Boolean>(isFromCaseEditArg)),
    )
}

internal class TransferWorkTypeArgs(val isFromCaseEdit: Boolean) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle.get<Boolean>(isFromCaseEditArg)),
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
        val navToNewCase = remember(navController) {
            { incidentId: Long -> navController.rerouteToNewCase(incidentId) }
        }
        val navToChangedIncident = remember(navController) {
            { ids: ExistingWorksiteIdentifier -> navController.rerouteToCaseChange(ids) }
        }
        val navToEditCase = remember(navController) {
            { ids: ExistingWorksiteIdentifier -> navController.rerouteToCaseEdit(ids) }
        }
        val onEditSearchAddress =
            remember(navController) { { navController.navigateToCaseEditSearchAddress() } }
        val onEditMoveLocationOnMap =
            remember(navController) { { navController.navigateToCaseEditLocationMapMove() } }
        CaseEditorRoute(
            onBack = onBackClick,
            changeNewIncidentCase = navToNewCase,
            changeExistingIncidentCase = navToChangedIncident,
            onOpenExistingCase = navToEditCase,
            onEditSearchAddress = onEditSearchAddress,
            onEditMoveLocationOnMap = onEditMoveLocationOnMap,
        )
    }
}

fun NavController.navigateToCaseEditSearchAddress() = this.navigate(caseEditSearchAddressRoute)
fun NavController.navigateToCaseEditLocationMapMove() = this.navigate(caseEditMapMoveLocationRoute)

fun NavController.navigateToCaseAddFlag(isFromCaseEdit: Boolean) {
    this.navigate("$caseAddFlagRoute?$isFromCaseEditArg=$isFromCaseEdit")
}

fun NavController.navigateToCaseShare() = this.navigate(caseShareRoute)
fun NavController.navigateToCaseHistory() = this.navigate(caseHistoryRoute)

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
        val navToTransferWorkType = remember(navController) {
            {
                navController.navigateToTransferWorkType(true)
            }
        }
        val navToViewImage = remember(navController) {
            { args: ViewImageArgs -> navController.navigateToViewImage(args) }
        }
        val navToCaseAddFlag =
            remember(navController) { { navController.navigateToCaseAddFlag(true) } }
        val navToCaseShare = remember(navController) { { navController.navigateToCaseShare() } }
        val navToCaseHistory = remember(navController) { { navController.navigateToCaseHistory() } }
        EditExistingCaseRoute(
            onBack = onBackClick,
            onFullEdit = navToEditCase,
            openTransferWorkType = navToTransferWorkType,
            openPhoto = navToViewImage,
            openAddFlag = navToCaseAddFlag,
            openShareCase = navToCaseShare,
            openCaseHistory = navToCaseHistory,
        )
    }
}

fun NavController.navigateToTransferWorkType(isFromCaseEdit: Boolean) =
    this.navigate("$viewCaseTransferWorkTypesRoute?$isFromCaseEditArg=$isFromCaseEdit")

internal fun NavController.popRouteStartingWith(route: String) {
    popBackStack()
    while (currentBackStackEntry?.destination?.route?.startsWith(route) == true) {
        popBackStack()
    }
}

fun NavController.rerouteToNewCase(incidentId: Long) {
    popRouteStartingWith(caseEditorRoute)
    navigateToCaseEditor(incidentId)
}

fun NavController.rerouteToCaseEdit(ids: ExistingWorksiteIdentifier) {
    popRouteStartingWith(caseEditorRoute)
    navigateToCaseEditor(ids.incidentId, ids.worksiteId)
}

fun NavController.rerouteToCaseChange(ids: ExistingWorksiteIdentifier) {
    popBackStack()
    while (currentBackStackEntry?.destination?.route?.let {
            it.startsWith(caseEditorRoute) ||
                    it.startsWith(viewCaseRoute)
        } == true) {
        popBackStack()
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

fun NavGraphBuilder.existingCaseTransferWorkTypesScreen(
    onBack: () -> Unit = {},
) {
    composable(
        route = "$viewCaseTransferWorkTypesRoute?$isFromCaseEditArg={$isFromCaseEditArg}",
        arguments = listOf(
            navArgument(isFromCaseEditArg) {
                type = NavType.BoolType
                defaultValue = false
            },
        ),
    ) {
        TransferWorkTypesRoute(onBack = onBack)
    }
}

fun NavGraphBuilder.caseAddFlagScreen(
    onBack: () -> Unit = {},
    rerouteIncidentChange: (ExistingWorksiteIdentifier) -> Unit = {},
) {
    composable(
        route = "$caseAddFlagRoute?$isFromCaseEditArg={$isFromCaseEditArg}",
        arguments = listOf(
            navArgument(isFromCaseEditArg) {
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
    composable(route = caseShareRoute) {
        CaseEditShareCaseRoute(
            onBack = onBack,
        )
    }
}

fun NavGraphBuilder.caseHistoryScreen(
    onBack: () -> Unit = {},
) {
    composable(route = caseHistoryRoute) {
        CaseEditCaseHistoryRoute(
            onBack = onBack,
        )
    }
}