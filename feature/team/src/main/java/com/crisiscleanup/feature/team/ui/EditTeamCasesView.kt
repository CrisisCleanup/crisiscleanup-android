package com.crisiscleanup.feature.team.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.commoncase.ui.CaseAddressInfoView
import com.crisiscleanup.core.commoncase.ui.CaseMapOverlayElements
import com.crisiscleanup.core.commoncase.ui.CasePhoneInfoView
import com.crisiscleanup.core.commoncase.ui.CasesAction
import com.crisiscleanup.core.commoncase.ui.CasesDownloadProgress
import com.crisiscleanup.core.commoncase.ui.CasesMapView
import com.crisiscleanup.core.commoncase.ui.CrisisCleanupFab
import com.crisiscleanup.core.commoncase.ui.MapLayersView
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupOutlinedButton
import com.crisiscleanup.core.designsystem.component.SmallBusyIndicator
import com.crisiscleanup.core.designsystem.component.actionHeight
import com.crisiscleanup.core.designsystem.component.actionSize
import com.crisiscleanup.core.designsystem.theme.LocalDimensions
import com.crisiscleanup.core.designsystem.theme.disabledAlpha
import com.crisiscleanup.core.designsystem.theme.edgePadding
import com.crisiscleanup.core.designsystem.theme.fillWidthPadded
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.designsystem.theme.listItemSpacedByHalf
import com.crisiscleanup.core.designsystem.theme.listItemTopPadding
import com.crisiscleanup.core.designsystem.theme.listItemVerticalPadding
import com.crisiscleanup.core.mapmarker.MapCaseIconProvider
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksiteMapMark
import com.crisiscleanup.core.ui.touchDownConsumer
import com.crisiscleanup.feature.team.EmptyTeamAssignableWorksite
import com.crisiscleanup.feature.team.TeamAssignableWorksite
import com.crisiscleanup.feature.team.TeamCaseMapManager
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.rememberTileOverlayState

@Composable
internal fun EditTeamCasesView(
    isLoading: Boolean,
    assignedCases: List<Worksite>,
    isLoadingSelectedMapCase: Boolean,
    isListView: Boolean,
    isLayerView: Boolean,
    isAssigningCase: Boolean,
    mapManager: TeamCaseMapManager,
    iconProvider: MapCaseIconProvider,
    modifier: Modifier = Modifier,
    onMapCaseSelect: (WorksiteMapMark) -> Unit = { },
    onPropagateTouchScroll: (Boolean) -> Unit = {},
    onSearchCases: () -> Unit = {},
    onFilterCases: () -> Unit = {},
    toggleMapListView: () -> Unit = {},
    onViewCase: (Worksite) -> Unit = {},
    onUnassignCase: (Worksite) -> Unit = {},
) {
    val mapModifier = remember(onPropagateTouchScroll) {
        Modifier.touchDownConsumer { onPropagateTouchScroll(false) }
    }

    val onCasesAction = remember(mapManager, onFilterCases, toggleMapListView) {
        { action: CasesAction ->
            when (action) {
                CasesAction.Layers -> mapManager.toggleLayersView()
                CasesAction.ZoomToInteractive -> mapManager.zoomToInteractive()
                CasesAction.ZoomToIncident -> mapManager.zoomToIncidentBounds()
                CasesAction.ZoomIn -> mapManager.zoomIn()
                CasesAction.ZoomOut -> mapManager.zoomOut()
                CasesAction.Search -> onSearchCases()
                CasesAction.Filters -> onFilterCases()
                CasesAction.ListView -> toggleMapListView()
                else -> mapManager.onCasesAction(action)
            }
        }
    }

    val filtersCount by mapManager.filtersCount.collectAsStateWithLifecycle(0)
    val isMapBusy by mapManager.isMapBusy.collectAsStateWithLifecycle(false)
    val casesCountMapText by mapManager.casesCountMapText.collectAsStateWithLifecycle()
    val worksitesOnMap by mapManager.worksitesMapMarkers.collectAsStateWithLifecycle()
    val mapCameraBounds by mapManager.incidentLocationBounds.collectAsStateWithLifecycle()
    val mapCameraZoom by mapManager.mapCameraZoom.collectAsStateWithLifecycle()
    val tileOverlayState = rememberTileOverlayState()
    val tileChangeValue by mapManager.overviewTileDataChange
    val clearTileLayer = remember(mapManager) { { mapManager.clearTileLayer } }
    val onMapCameraChange = remember(mapManager, onPropagateTouchScroll) {
        { position: CameraPosition, projection: Projection?, isActiveMove: Boolean ->
            if (!isActiveMove) {
                onPropagateTouchScroll(true)
            }
            mapManager.onMapCameraChange(position, projection, isActiveMove)
        }
    }
    val dataProgress by mapManager.dataProgress.collectAsStateWithLifecycle()
    val isLoadingData by mapManager.isLoadingData.collectAsStateWithLifecycle(true)
    val onMapMarkerSelect = remember(mapManager) {
        { mark: WorksiteMapMark ->
            onMapCaseSelect(mark)
            true
        }
    }
    val isMyLocationEnabled by mapManager.isMyLocationEnabled.collectAsStateWithLifecycle()

    val onSyncDataDelta = remember(mapManager) {
        {
            mapManager.syncWorksitesDelta(false)
        }
    }
    val onSyncDataFull = remember(mapManager) {
        {
            mapManager.syncWorksitesDelta(true)
        }
    }

    if (isListView) {
        Box(modifier) {
            AssignedCasesView(
                isLoadingCases = isLoading,
                assignedCases,
                iconProvider,
                Modifier.fillMaxSize(),
                onSearchCases = onSearchCases,
                onViewCase = onViewCase,
                onUnassignCase = onUnassignCase,
            )

            CrisisCleanupFab(
                CasesAction.MapView,
                enabled = true,
                Modifier.align(Alignment.BottomEnd)
                    .edgePadding()
                    .actionSize(),
                onClick = toggleMapListView,
            )
        }
    } else {
        Box(modifier) {
            var isSatelliteMapType by remember { mutableStateOf(false) }

            CasesMapView(
                modifier = mapModifier,
                mapCameraBounds,
                mapCameraZoom,
                isMapBusy,
                worksitesOnMap,
                tileChangeValue,
                clearTileLayer,
                tileOverlayState,
                mapManager::overviewMapTileProvider,
                onMapLoadStart = mapManager::onMapLoadStart,
                onMapLoaded = mapManager::onMapLoaded,
                onMapCameraChange,
                onMapMarkerSelect,
                null,
                isMyLocationEnabled = isMyLocationEnabled,
                isSatelliteMapType = isSatelliteMapType,
            )

            MapLayersView(
                isLayerView,
                onDismiss = {
                    onCasesAction(CasesAction.Layers)
                },
                isSatelliteMapType = isSatelliteMapType,
                onToggleSatelliteType = { isSatellite: Boolean ->
                    isSatelliteMapType = isSatellite
                },
            )

            // TODO Use MapOverlayMessage
            Row(
                Modifier
                    .align(Alignment.BottomStart)
                    .offset(y = (-24).dp)
                    .padding(LocalDimensions.current.edgePadding)
                    // TODO Common dimensions
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.disabledAlpha())
                    .padding(8.dp),
                horizontalArrangement = listItemSpacedByHalf,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    LocalAppTranslator.current("~~Select Cases to assign to team"),
                )

                AnimatedVisibility(
                    isLoadingSelectedMapCase || isAssigningCase,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    SmallBusyIndicator(padding = 0.dp)
                }
            }

            CaseMapOverlayElements(
                Modifier,
                onCasesAction = onCasesAction,
                centerOnMyLocation = mapManager::grantAccessToDeviceLocation,
                isLoadingData = isLoadingData,
                casesCountText = casesCountMapText,
                filtersCount = filtersCount,
                disableTableViewActions = true,
                onSyncDataDelta = onSyncDataDelta,
                onSyncDataFull = onSyncDataFull,
                showCasesMainActions = false,
                assignedCaseCount = assignedCases.size,
            )

            CasesDownloadProgress(dataProgress)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditTeamMapCaseOverview(
    isAssigningCase: Boolean,
    selectedWorksite: TeamAssignableWorksite,
    iconProvider: MapCaseIconProvider,
    onViewDetails: () -> Unit = {},
    onAssignCase: () -> Unit = {},
    onClearSelection: () -> Unit = {},
    onUnassignCase: () -> Unit = {},
) {
    val t = LocalAppTranslator.current

    if (selectedWorksite != EmptyTeamAssignableWorksite) {
        ModalBottomSheet(
            onDismissRequest = onClearSelection,
            tonalElevation = 0.dp,
        ) {
            Column(verticalArrangement = listItemSpacedByHalf) {
                Row(
                    listItemModifier,
                    horizontalArrangement = listItemSpacedBy,
                    verticalAlignment = Alignment.Top,
                ) {
                    selectedWorksite.worksite.keyWorkType?.let { keyWorkType ->
                        iconProvider.getIconBitmap(
                            keyWorkType.statusClaim,
                            keyWorkType.workType,
                            hasMultipleWorkTypes = selectedWorksite.worksite.workTypes.size > 1,
                        )?.let { bitmap ->
                            // TODO Review if this produces the intended description
                            val workTypeLiteral =
                                t(selectedWorksite.worksite.keyWorkType?.workTypeLiteral ?: "")
                            Image(
                                bitmap.asImageBitmap(),
                                contentDescription = workTypeLiteral,
                            )
                        }
                    }

                    with(selectedWorksite.worksite) {
                        Column(verticalArrangement = listItemSpacedByHalf) {
                            Text(
                                "$name, $caseNumber",
                                Modifier.listItemTopPadding(),
                                fontWeight = FontWeight.Bold,
                            )

                            CaseAddressInfoView(
                                this@with,
                                false,
                                Modifier.listItemVerticalPadding(),
                            )

                            CasePhoneInfoView(
                                this@with,
                                false,
                                Modifier.listItemVerticalPadding(),
                            )
                        }
                    }
                }

                Row(
                    fillWidthPadded,
                    horizontalArrangement = listItemSpacedBy,
                ) {
                    CrisisCleanupOutlinedButton(
                        modifier = Modifier
                            .actionHeight()
                            .weight(1f),
                        text = t("~~View Details"),
                        onClick = onViewDetails,
                        enabled = !isAssigningCase,
                    )

                    if (selectedWorksite.isAssigned) {
                        CrisisCleanupButton(
                            modifier = Modifier.weight(1f),
                            text = t("~~Unassign Case"),
                            onClick = onUnassignCase,
                        )
                    } else {
                        BusyButton(
                            modifier = Modifier.weight(1f),
                            enabled = !isAssigningCase && selectedWorksite.isAssignable,
                            text = t("~~Assign Case"),
                            indicateBusy = isAssigningCase,
                            onClick = onAssignCase,
                        )
                    }
                }
            }
        }
    }
}
