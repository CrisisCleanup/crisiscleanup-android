package com.crisiscleanup.feature.cases.ui

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.component.CrisisCleanupAlertDialog
import com.crisiscleanup.core.designsystem.component.CrisisCleanupButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupFab
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.core.designsystem.component.ExplainLocationPermissionDialog
import com.crisiscleanup.core.designsystem.component.actionEdgeSpace
import com.crisiscleanup.core.designsystem.component.actionInnerSpace
import com.crisiscleanup.core.designsystem.component.actionRoundCornerShape
import com.crisiscleanup.core.designsystem.component.actionSize
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.CrisisCleanupTheme
import com.crisiscleanup.core.designsystem.theme.disabledAlpha
import com.crisiscleanup.core.designsystem.theme.incidentDisasterContainerColor
import com.crisiscleanup.core.designsystem.theme.incidentDisasterContentColor
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.designsystem.theme.navigationContainerColor
import com.crisiscleanup.core.designsystem.theme.primaryOrangeColor
import com.crisiscleanup.core.domain.IncidentsData
import com.crisiscleanup.core.mapmarker.model.MapViewCameraBounds
import com.crisiscleanup.core.mapmarker.model.MapViewCameraBoundsDefault
import com.crisiscleanup.core.mapmarker.model.MapViewCameraZoom
import com.crisiscleanup.core.mapmarker.model.MapViewCameraZoomDefault
import com.crisiscleanup.core.mapmarker.ui.rememberMapProperties
import com.crisiscleanup.core.mapmarker.ui.rememberMapUiSettings
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.Incident
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksiteMapMark
import com.crisiscleanup.core.selectincident.SelectIncidentDialog
import com.crisiscleanup.core.ui.LocalAppLayout
import com.crisiscleanup.feature.cases.CasesViewModel
import com.crisiscleanup.feature.cases.DataProgressMetrics
import com.crisiscleanup.feature.cases.R
import com.crisiscleanup.feature.cases.model.WorksiteGoogleMapMark
import com.crisiscleanup.feature.cases.zeroDataProgress
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.TileProvider
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.TileOverlay
import com.google.maps.android.compose.TileOverlayState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberTileOverlayState
import com.crisiscleanup.core.commonassets.R as commonAssetsR
import com.crisiscleanup.core.mapmarker.R as mapMarkerR

@Composable
internal fun CasesRoute(
    viewModel: CasesViewModel = hiltViewModel(),
    onCasesAction: (CasesAction) -> Unit = { },
    createNewCase: (Long) -> Unit = {},
    viewCase: (Long, Long) -> Boolean = { _, _ -> false },
    openAddFlag: () -> Unit = {},
    openTransferWorkType: () -> Unit = {},
) {
    val openAddFlagCounter by viewModel.openWorksiteAddFlagCounter.collectAsStateWithLifecycle()
    LaunchedEffect(openAddFlagCounter) {
        if (viewModel.takeOpenWorksiteAddFlag()) {
            openAddFlag()
        }
    }

    val isPendingTransfer by viewModel.transferWorkTypeProvider.isPendingTransfer
    if (isPendingTransfer) {
        openTransferWorkType()
    }

    val incidentsData by viewModel.incidentsData.collectAsStateWithLifecycle()
    val isIncidentLoading by viewModel.isIncidentLoading.collectAsState(true)
    val isLoadingData by viewModel.isLoadingData.collectAsState(true)
    if (incidentsData is IncidentsData.Incidents) {
        val isTableView by viewModel.isTableView.collectAsStateWithLifecycle()
        BackHandler(enabled = isTableView) {
            viewModel.setContentViewType(false)
        }

        val isLayerView by viewModel.isLayerView

        val disasterResId by viewModel.disasterIconResId.collectAsStateWithLifecycle()
        var showChangeIncident by rememberSaveable { mutableStateOf(false) }
        val onIncidentSelect = remember(viewModel) { { showChangeIncident = true } }

        val rememberOnCasesAction = remember(onCasesAction, viewModel) {
            { action: CasesAction ->
                when (action) {
                    CasesAction.CreateNew -> {
                        val incidentId = viewModel.incidentId
                        if (incidentId != EmptyIncident.id) {
                            createNewCase(incidentId)
                        }
                    }

                    CasesAction.MapView -> viewModel.setContentViewType(false)
                    CasesAction.TableView -> viewModel.setContentViewType(true)
                    CasesAction.Layers -> viewModel.toggleLayersView()
                    CasesAction.ZoomToInteractive -> viewModel.zoomToInteractive()
                    CasesAction.ZoomToIncident -> viewModel.zoomToIncidentBounds()
                    CasesAction.ZoomIn -> viewModel.zoomIn()
                    CasesAction.ZoomOut -> viewModel.zoomOut()
                    else -> onCasesAction(action)
                }
            }
        }
        val filtersCount by viewModel.filtersCount.collectAsStateWithLifecycle(0)
        val isMapBusy by viewModel.isMapBusy.collectAsStateWithLifecycle(false)
        val isTableDataTransient by viewModel.isLoadingTableViewData.collectAsStateWithLifecycle()
        val casesCountMapText by viewModel.casesCountMapText.collectAsStateWithLifecycle()
        val worksitesOnMap by viewModel.worksitesMapMarkers.collectAsStateWithLifecycle()
        val mapCameraBounds by viewModel.incidentLocationBounds.collectAsStateWithLifecycle()
        val mapCameraZoom by viewModel.mapCameraZoom.collectAsStateWithLifecycle()
        val tileOverlayState = rememberTileOverlayState()
        val tileChangeValue by viewModel.overviewTileDataChange
        val clearTileLayer = remember(viewModel) { { viewModel.clearTileLayer } }
        val onMapCameraChange = remember(viewModel) {
            { position: CameraPosition, projection: Projection?, activeChange: Boolean ->
                viewModel.onMapCameraChange(position, projection, activeChange)
            }
        }
        val dataProgressMetrics by viewModel.dataProgress.collectAsStateWithLifecycle()
        val onMapMarkerSelect = remember(viewModel) {
            { mark: WorksiteMapMark -> viewCase(viewModel.incidentId, mark.id) }
        }
        val onTableItemSelect = remember(viewModel) {
            { worksite: Worksite ->
                viewCase(viewModel.incidentId, worksite.id)
                Unit
            }
        }
        val editedWorksiteLocation = viewModel.editedWorksiteLocation
        val isMyLocationEnabled = viewModel.isMyLocationEnabled
        val hasIncidents = (incidentsData as IncidentsData.Incidents).incidents.isNotEmpty()

        val onSyncDataDelta = remember(viewModel) {
            {
                viewModel.syncWorksitesDelta(false)
            }
        }
        val onSyncDataFull = remember(viewModel) {
            {
                viewModel.syncWorksitesDelta(true)
            }
        }
        CasesScreen(
            dataProgress = dataProgressMetrics,
            disasterResId = disasterResId,
            onSelectIncident = onIncidentSelect,
            onCasesAction = rememberOnCasesAction,
            centerOnMyLocation = viewModel::useMyLocation,
            isTableView = isTableView,
            isLayerView = isLayerView,
            filtersCount = filtersCount,
            isLoadingData = isLoadingData,
            isMapBusy = isMapBusy,
            isTableDataTransient = isTableDataTransient || isPendingTransfer,
            casesCountMapText = casesCountMapText,
            worksitesOnMap = worksitesOnMap,
            mapCameraBounds = mapCameraBounds,
            mapCameraZoom = mapCameraZoom,
            tileChangeValue = tileChangeValue,
            tileOverlayState = tileOverlayState,
            clearTileLayer = clearTileLayer,
            casesDotTileProvider = viewModel::overviewMapTileProvider,
            onMapLoadStart = viewModel::onMapLoadStart,
            onMapLoaded = viewModel::onMapLoaded,
            onMapCameraChange = onMapCameraChange,
            onMarkerSelect = onMapMarkerSelect,
            editedWorksiteLocation = editedWorksiteLocation,
            isMyLocationEnabled = isMyLocationEnabled,
            onTableItemSelect = onTableItemSelect,
            onSyncDataDelta = onSyncDataDelta,
            onSyncDataFull = onSyncDataFull,
            hasIncidents = hasIncidents,
        )

        if (showChangeIncident) {
            val closeDialog = remember(viewModel) { { showChangeIncident = false } }
            val selectedIncidentId by viewModel.incidentSelector.incidentId.collectAsStateWithLifecycle()
            val setSelected = remember(viewModel) {
                { incident: Incident ->
                    viewModel.loadSelectIncidents.selectIncident(incident)
                }
            }
            SelectIncidentDialog(
                rememberKey = viewModel,
                onBackClick = closeDialog,
                incidentsData = incidentsData,
                selectedIncidentId = selectedIncidentId,
                onSelectIncident = setSelected,
                onRefreshIncidents = viewModel::refreshIncidentsAsync,
            )
        }

        val closePermissionDialog =
            remember(viewModel) { { viewModel.showExplainPermissionLocation = false } }
        val explainPermission = viewModel.showExplainPermissionLocation
        ExplainLocationPermissionDialog(
            showDialog = explainPermission,
            closeDialog = closePermissionDialog,
        )
    } else {
        val isLoading = incidentsData is IncidentsData.Loading || isIncidentLoading
        val reloadIncidents = remember(viewModel) { { viewModel.refreshIncidentsData() } }
        NoCasesScreen(
            isLoading = isLoading,
            onRetryLoad = reloadIncidents,
        )
    }

    NonProductionDialog()
}

@Composable
private fun NonProductionDialog(
    viewModel: CasesViewModel = hiltViewModel(),
) {
    var showDialog by remember { mutableStateOf(false) }
    if (viewModel.visualAlertManager.takeNonProductionAppAlert()) {
        showDialog = true
    }
    if (showDialog &&
        viewModel.appEnv.isNotProduction &&
        !viewModel.appEnv.isDebuggable
    ) {
        val hideDialog = {
            viewModel.visualAlertManager.setNonProductionAppAlert(false)
            showDialog = false
        }
        val translator = LocalAppTranslator.current
        val translationCount by translator.translationCount.collectAsStateWithLifecycle()
        val t = remember(translationCount) { translator }
        CrisisCleanupAlertDialog(
            onDismissRequest = hideDialog,
            titleContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = listItemSpacedBy,
                ) {
                    Image(
                        imageVector = CrisisCleanupIcons.Warning,
                        contentDescription = "Beta app does not save information",
                        modifier = Modifier.size(96.dp),
                        colorFilter = ColorFilter.tint(Color.Red),
                    )
                    Text(
                        t("phoneBeta.title"),
                        color = Color.Red,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            confirmButton = {
                CrisisCleanupTextButton(
                    text = t("actions.ok"),
                    onClick = hideDialog,
                )
            },
            text = t("phoneBeta.explanation"),
        )
    }
}

@Composable
internal fun NoCasesScreen(
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onRetryLoad: () -> Unit = {},
) {
    Box(Modifier.fillMaxSize()) {
        if (isLoading) {
            BusyIndicatorFloatingTopCenter(true)
        } else {
            // TODO Use constant for width
            Column(
                modifier
                    .align(Alignment.Center)
                    .widthIn(max = 300.dp),
            ) {
                Text(text = LocalAppTranslator.current("info.incident_load_error"))
                // TODO Use constant for spacing
                Spacer(modifier = Modifier.height(16.dp))
                CrisisCleanupButton(
                    modifier = modifier.align(Alignment.End),
                    onClick = onRetryLoad,
                    text = LocalAppTranslator.current("actions.retry"),
                )
            }
        }
    }
}

@Composable
internal fun CasesScreen(
    dataProgress: DataProgressMetrics = zeroDataProgress,
    onSelectIncident: () -> Unit = {},
    @DrawableRes disasterResId: Int = commonAssetsR.drawable.ic_disaster_other,
    onCasesAction: (CasesAction) -> Unit = {},
    centerOnMyLocation: () -> Unit = {},
    isTableView: Boolean = false,
    isLayerView: Boolean = false,
    filtersCount: Int = 0,
    isLoadingData: Boolean = false,
    isMapBusy: Boolean = false,
    isTableDataTransient: Boolean = false,
    casesCountMapText: String = "",
    worksitesOnMap: List<WorksiteGoogleMapMark> = emptyList(),
    mapCameraBounds: MapViewCameraBounds = MapViewCameraBoundsDefault,
    mapCameraZoom: MapViewCameraZoom = MapViewCameraZoomDefault,
    tileChangeValue: Long = -1,
    clearTileLayer: () -> Boolean = { false },
    tileOverlayState: TileOverlayState = rememberTileOverlayState(),
    casesDotTileProvider: () -> TileProvider? = { null },
    onMapLoadStart: () -> Unit = {},
    onMapLoaded: () -> Unit = {},
    onMapCameraChange: (CameraPosition, Projection?, Boolean) -> Unit = { _, _, _ -> },
    onMarkerSelect: (WorksiteMapMark) -> Boolean = { false },
    editedWorksiteLocation: LatLng? = null,
    isMyLocationEnabled: Boolean = false,
    onTableItemSelect: (Worksite) -> Unit = {},
    onSyncDataDelta: () -> Unit = {},
    onSyncDataFull: () -> Unit = {},
    hasIncidents: Boolean = false,
) {
    Box {
        if (isTableView) {
            CasesTableView(
                isLoadingData = isLoadingData,
                isTableDataTransient = isTableDataTransient,
                disasterResId = disasterResId,
                openIncidentSelect = onSelectIncident,
                onCasesAction = onCasesAction,
                filtersCount = filtersCount,
                onTableItemSelect = onTableItemSelect,
                onSyncDataDelta = onSyncDataDelta,
                onSyncDataFull = onSyncDataFull,
                hasIncidents = hasIncidents,
            )
        } else {
            CasesMapView(
                mapCameraBounds,
                mapCameraZoom,
                isMapBusy,
                worksitesOnMap,
                tileChangeValue,
                clearTileLayer,
                tileOverlayState,
                casesDotTileProvider,
                onMapLoadStart,
                onMapLoaded,
                onMapCameraChange,
                onMarkerSelect,
                editedWorksiteLocation,
                isMyLocationEnabled,
            )
        }
        CasesOverlayElements(
            Modifier,
            onSelectIncident,
            disasterResId,
            onCasesAction,
            centerOnMyLocation,
            isTableView,
            isLoadingData,
            casesCountMapText,
            filtersCount,
            disableTableViewActions = isTableDataTransient,
            onSyncDataDelta = onSyncDataDelta,
            onSyncDataFull = onSyncDataFull,
            hasIncidents = hasIncidents,
        )

        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
            visible = dataProgress.showProgress,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            var progressColor = primaryOrangeColor
            if (dataProgress.isSecondaryData) {
                progressColor = progressColor.disabledAlpha()
            }
            LinearProgressIndicator(
                progress = { dataProgress.progress },
                color = progressColor,
            )
        }
    }
}

@Composable
internal fun BoxScope.CasesMapView(
    mapCameraBounds: MapViewCameraBounds = MapViewCameraBoundsDefault,
    mapCameraZoom: MapViewCameraZoom = MapViewCameraZoomDefault,
    isMapBusy: Boolean = false,
    worksitesOnMap: List<WorksiteGoogleMapMark> = emptyList(),
    tileChangeValue: Long = -1,
    clearTileLayer: () -> Boolean = { false },
    tileOverlayState: TileOverlayState = rememberTileOverlayState(),
    casesDotTileProvider: () -> TileProvider? = { null },
    onMapLoadStart: () -> Unit = {},
    onMapLoaded: () -> Unit = {},
    onMapCameraChange: (CameraPosition, Projection?, Boolean) -> Unit = { _, _, _ -> },
    onMarkerSelect: (WorksiteMapMark) -> Boolean = { false },
    editedWorksiteLocation: LatLng? = null,
    isMyLocationEnabled: Boolean = false,
    onEditLocationZoom: Float = 12f,
) {
    // TODO Profile and optimize recompositions when map is changed (by user) if possible.

    LaunchedEffect(Unit) {
        onMapLoadStart()
    }

    val uiSettings by rememberMapUiSettings()
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            mapCameraBounds.bounds.center,
            mapCameraZoom.zoom,
        )
    }

    val mapProperties by rememberMapProperties(
        mapMarkerR.raw.map_style,
        isMyLocation = isMyLocationEnabled,
    )
    GoogleMap(
        modifier = Modifier
            .fillMaxSize()
            .testTag("casesMapViewContainer"),
        uiSettings = uiSettings,
        properties = mapProperties,
        onMapLoaded = onMapLoaded,
        cameraPositionState = cameraPositionState,
    ) {
        // TODO Is it possible to cache? If so test recomposition. If not document why not.
        worksitesOnMap.forEach { mapMark ->
            Marker(
                mapMark.markerState,
                icon = mapMark.mapIcon,
                anchor = mapMark.mapIconOffset,
                onClick = { onMarkerSelect(mapMark.source) },
            )
        }

        if (tileChangeValue >= 0) {
            casesDotTileProvider()?.let {
                if (clearTileLayer()) {
                    // TODO When inspection of state and overlay is possible remove the try/catch
                    //      This is fine for now as the exception is just an escape from the method.
                    try {
                        // TODO This is not clearing as expected on API 29 at times. Unrelated to try/catch.
                        tileOverlayState.clearTileCache()
                    } catch (e: IllegalStateException) {
                        if (e.message?.contains("is not used") == true) {
                            Log.d("tile-overlay", "Ignoring unattached tile overlay state")
                        } else {
                            throw e
                        }
                    }
                }
                TileOverlay(it, tileOverlayState)
            }
        }
    }

    BusyIndicatorFloatingTopCenter(isMapBusy)

    val currentLocalDensity = LocalDensity.current
    LaunchedEffect(mapCameraBounds) {
        if (mapCameraBounds.takeApply()) {
            // TODO Make padding dp a parameter
            val padding = with(currentLocalDensity) { 8.dp.toPx() }
            val update = CameraUpdateFactory.newLatLngBounds(
                mapCameraBounds.bounds,
                padding.toInt(),
            )
            if (mapCameraBounds.durationMs > 0) {
                cameraPositionState.animate(update, mapCameraBounds.durationMs)
            } else {
                cameraPositionState.move(update)
            }
        }
    }

    LaunchedEffect(mapCameraZoom) {
        if (mapCameraZoom.takeApply()) {
            val update = CameraUpdateFactory.newLatLngZoom(
                mapCameraZoom.center,
                mapCameraZoom.zoom,
            )
            if (mapCameraZoom.durationMs > 0) {
                cameraPositionState.animate(update, mapCameraZoom.durationMs)
            } else {
                cameraPositionState.move(update)
            }
        }
    }

    editedWorksiteLocation?.let {
        LaunchedEffect(Unit) {
            val update = CameraUpdateFactory.newLatLngZoom(it, onEditLocationZoom)
            cameraPositionState.move(update)
        }
    }

    val movingCamera = remember {
        derivedStateOf {
            cameraPositionState.isMoving && cameraPositionState.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE
        }
    }
    onMapCameraChange(
        cameraPositionState.position,
        cameraPositionState.projection,
        movingCamera.value,
    )
}

@Composable
private fun CasesOverlayElements(
    modifier: Modifier = Modifier,
    onSelectIncident: () -> Unit = {},
    @DrawableRes disasterResId: Int = commonAssetsR.drawable.ic_disaster_other,
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

            CasesZoomBar(
                modifier.constrainAs(zoomBar) {
                    top.linkTo(disasterAction.bottom, margin = actionInnerSpace)
                    start.linkTo(disasterAction.start)
                    end.linkTo(disasterAction.end)
                },
                onCasesAction,
            )

            CasesActionBar(
                modifier.constrainAs(actionBar) {
                    top.linkTo(parent.top, margin = actionEdgeSpace)
                    end.linkTo(parent.end, margin = actionEdgeSpace)
                },
                onCasesAction,
                filtersCount,
            )

            CasesCountView(
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
                        end.linkTo(toggleTableMap.end)
                        bottom.linkTo(newCaseFab.top, margin = actionEdgeSpace)
                    },
                onClick = centerOnMyLocation,
                shape = actionRoundCornerShape,
                enabled = true,
            ) {
                Icon(
                    painterResource(R.drawable.ic_my_location),
                    contentDescription = translator("actions.my_location"),
                )
            }
        }

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
        val appLayout = LocalAppLayout.current
        val additionalBottomPadding by remember(appLayout.isBottomSnackbarVisible) {
            derivedStateOf { appLayout.bottomSnackbarPadding }
        }
        val tableMapAction = if (isTableView) CasesAction.MapView else CasesAction.TableView
        val toggleMapTableView = remember(tableMapAction) { { onCasesAction(tableMapAction) } }
        val bottomPadding = actionInnerSpace.plus(additionalBottomPadding)
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
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CasesCountView(
    countText: String,
    isLoadingData: Boolean,
    modifier: Modifier = Modifier,
    onSyncDataDelta: () -> Unit = {},
    onSyncDataFull: () -> Unit = {},
) {
    // TODO Common dimensions of elements
    Surface(
        modifier,
        color = navigationContainerColor,
        contentColor = Color.White,
        shadowElevation = 4.dp,
        shape = RoundedCornerShape(4.dp),
    ) {
        Row(
            Modifier
                .combinedClickable(
                    enabled = !isLoadingData,
                    onClick = onSyncDataDelta,
                    onLongClick = onSyncDataFull,
                )
                .padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = listItemSpacedBy,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedVisibility(
                visible = countText.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Text(countText)
            }

            AnimatedVisibility(
                visible = isLoadingData,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                CircularProgressIndicator(
                    Modifier
                        .testTag("workIncidentsLoadingIndicator")
                        .wrapContentSize()
                        .size(24.dp),
                    color = Color.White,
                )
            }
        }
    }
}

@Preview
@Composable
fun CasesOverlayActionsPreview() {
    CrisisCleanupTheme {
        CasesOverlayElements()
    }
}

@Preview
@Composable
fun NoCasesLoadingPreview() {
    NoCasesScreen(isLoading = true)
}

@Preview
@Composable
fun NoCasesRetryPreview() {
    NoCasesScreen()
}
