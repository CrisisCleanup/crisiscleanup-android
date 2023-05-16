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
import com.crisiscleanup.core.appnav.RouteConstant.caseEditMapMoveLocationRoute
import com.crisiscleanup.core.appnav.RouteConstant.caseEditSearchAddressRoute
import com.crisiscleanup.core.appnav.RouteConstant.caseEditorRoute
import com.crisiscleanup.core.appnav.RouteConstant.viewCaseRoute
import com.crisiscleanup.core.appnav.RouteConstant.viewCaseTransferWorkTypesRoute
import com.crisiscleanup.core.appnav.navigateToViewImage
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.feature.caseeditor.ExistingWorksiteIdentifier
import com.crisiscleanup.feature.caseeditor.ui.CaseEditorRoute
import com.crisiscleanup.feature.caseeditor.ui.EditCaseAddressSearchRoute
import com.crisiscleanup.feature.caseeditor.ui.EditCaseMapMoveLocationRoute
import com.crisiscleanup.feature.caseeditor.ui.EditExistingCaseRoute
import com.crisiscleanup.feature.caseeditor.ui.TransferWorkTypesRoute

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
        val navToTransferWorkType = remember(navController) {
            {
                navController.navigateToExistingCaseTransferWorkType()
            }
        }
        val navToViewImage = remember(navController) {
            { imageId: Long, imageUrl: String, isNetworkImage: Boolean ->
                navController.navigateToViewImage(imageId, imageUrl, isNetworkImage)
            }
        }
        EditExistingCaseRoute(
            onBack = onBackClick,
            onFullEdit = navToEditCase,
            openTransferWorkType = navToTransferWorkType,
            openPhoto = navToViewImage,
        )
    }
}

fun NavController.navigateToExistingCaseTransferWorkType() =
    this.navigate(viewCaseTransferWorkTypesRoute)

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

fun NavGraphBuilder.existingCaseTransferWorkTypesScreen(
    onBack: () -> Unit = {},
) {
    composable(route = viewCaseTransferWorkTypesRoute) {
        TransferWorkTypesRoute(onBack = onBack)
    }
}
