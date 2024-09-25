package com.crisiscleanup.core.commoncase.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.crisiscleanup.core.commonassets.R
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupFab
import com.crisiscleanup.core.designsystem.component.actionEdgeSpace
import com.crisiscleanup.core.designsystem.component.actionInnerSpace
import com.crisiscleanup.core.designsystem.component.actionRoundCornerShape
import com.crisiscleanup.core.designsystem.component.actionSize
import com.crisiscleanup.core.designsystem.component.actionSmallWidth
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.CrisisCleanupTheme
import com.crisiscleanup.core.designsystem.theme.incidentDisasterContainerColor
import com.crisiscleanup.core.designsystem.theme.incidentDisasterContentColor
import com.crisiscleanup.core.ui.LocalAppLayout

@Composable
fun CaseMapOverlayElements(
    modifier: Modifier = Modifier,
    onSelectIncident: () -> Unit = {},
    @DrawableRes disasterResId: Int = R.drawable.ic_disaster_other,
    onCasesAction: (CasesAction) -> Unit = {},
    centerOnMyLocation: () -> Unit = {},
    isTableView: Boolean = false,
    isLoadingData: Boolean = false,
    casesCountText: String = "",
    filtersCount: Int = 0,
    disableTableViewActions: Boolean = false,
    onSyncDataDelta: () -> Unit = {},
    onSyncDataFull: () -> Unit = {},
    hasIncidents: Boolean = false,
    showCasesMainActions: Boolean = true,
) {
    val translator = LocalAppTranslator.current

    val isMapView = !isTableView

    ConstraintLayout(Modifier.fillMaxSize()) {
        val (
            disasterAction,
            zoomBar,
            actionBar,
            newCaseFab,
            toggleTableMap,
            myLocation,
            countTextRef,
        ) = createRefs()

        if (isMapView) {
            if (showCasesMainActions) {
                CrisisCleanupFab(
                    modifier = modifier
                        .testTag("workIncidentSelectorFab")
                        .constrainAs(disasterAction) {
                            start.linkTo(parent.start, margin = actionEdgeSpace)
                            top.linkTo(parent.top, margin = actionEdgeSpace)
                        },
                    onClick = onSelectIncident,
                    shape = CircleShape,
                    containerColor = incidentDisasterContainerColor,
                    contentColor = incidentDisasterContentColor,
                    enabled = hasIncidents,
                ) {
                    Icon(
                        painter = painterResource(disasterResId),
                        contentDescription = translator("nav.change_incident"),
                    )
                }
            } else {
                Box(
                    Modifier.actionSmallWidth()
                        .testTag("workIncidentSelectorPlaceholder")
                        .constrainAs(disasterAction) {
                            start.linkTo(parent.start, margin = actionEdgeSpace)
                            top.linkTo(parent.top, margin = 0.dp)
                        },
                )
            }

            CaseMapZoomBar(
                modifier.constrainAs(zoomBar) {
                    top.linkTo(disasterAction.bottom, margin = actionInnerSpace)
                    start.linkTo(disasterAction.start)
                    end.linkTo(disasterAction.end)
                },
                onCasesAction,
            )

            CaseMapActionBar(
                modifier.constrainAs(actionBar) {
                    top.linkTo(parent.top, margin = actionEdgeSpace)
                    end.linkTo(parent.end, margin = actionEdgeSpace)
                },
                onCasesAction,
                filtersCount,
            )

            CaseMapCountView(
                casesCountText,
                isLoadingData,
                Modifier.constrainAs(countTextRef) {
                    top.linkTo(parent.top, margin = actionEdgeSpace)
                    start.linkTo(disasterAction.end)
                    end.linkTo(actionBar.start)
                },
                onSyncDataDelta = onSyncDataDelta,
                onSyncDataFull = onSyncDataFull,
            )

            CrisisCleanupFab(
                modifier = modifier
                    .actionSize()
                    .testTag("workMyLocationFab")
                    .constrainAs(myLocation) {
                        end.linkTo(newCaseFab.end)
                        bottom.linkTo(newCaseFab.top, margin = actionEdgeSpace)
                    },
                onClick = centerOnMyLocation,
                shape = actionRoundCornerShape,
                enabled = true,
            ) {
                Icon(
                    painterResource(com.crisiscleanup.core.commoncase.R.drawable.ic_my_location),
                    contentDescription = translator("actions.my_location"),
                )
            }
        }

        val appLayout = LocalAppLayout.current
        val additionalBottomPadding by remember(appLayout.isBottomSnackbarVisible) {
            derivedStateOf { appLayout.bottomSnackbarPadding }
        }
        val bottomPadding = actionInnerSpace.plus(additionalBottomPadding)

        if (showCasesMainActions) {
            val enableLowerActions = !isTableView || !disableTableViewActions

            val onNewCase = remember(onCasesAction) { { onCasesAction(CasesAction.CreateNew) } }
            CrisisCleanupFab(
                modifier = modifier
                    .actionSize()
                    .testTag("workNewCaseFab")
                    .constrainAs(newCaseFab) {
                        end.linkTo(toggleTableMap.end)
                        bottom.linkTo(toggleTableMap.top, margin = actionEdgeSpace)
                    },
                onClick = onNewCase,
                shape = actionRoundCornerShape,
                enabled = enableLowerActions,
            ) {
                Icon(
                    imageVector = CrisisCleanupIcons.Add,
                    contentDescription = translator("nav.new_case"),
                )
            }
            val tableMapAction = if (isTableView) CasesAction.MapView else CasesAction.TableView
            val toggleMapTableView = remember(tableMapAction) { { onCasesAction(tableMapAction) } }
            CrisisCleanupFab(
                modifier = modifier
                    .actionSize()
                    .testTag("workToggleTableMapViewFab")
                    .constrainAs(toggleTableMap) {
                        end.linkTo(parent.end, margin = actionEdgeSpace)
                        bottom.linkTo(parent.bottom, margin = bottomPadding)
                    },
                onClick = toggleMapTableView,
                shape = actionRoundCornerShape,
                enabled = enableLowerActions,
            ) {
                Icon(
                    painter = painterResource(tableMapAction.iconResId),
                    contentDescription = translator(tableMapAction.descriptionTranslateKey),
                )
            }
        } else {
            Box(
                Modifier
                    .size(0.dp)
                    .testTag("workNewCasePlaceholder")
                    .constrainAs(newCaseFab) {
                        end.linkTo(parent.end, margin = actionEdgeSpace)
                        bottom.linkTo(parent.bottom, margin = 0.dp)
                    },
            )
        }
    }
}

@Preview
@Composable
fun CaseMapOverlayActionsPreview() {
    CrisisCleanupTheme {
        CaseMapOverlayElements()
    }
}
