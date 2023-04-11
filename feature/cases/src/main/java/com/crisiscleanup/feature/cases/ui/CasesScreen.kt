package com.crisiscleanup.feature.cases.ui

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.component.CrisisCleanupButton
import com.crisiscleanup.core.designsystem.component.fabEdgeSpace
import com.crisiscleanup.core.designsystem.component.mapButtonEdgeSpace
import com.crisiscleanup.core.domain.IncidentsData
import com.crisiscleanup.core.mapmarker.model.MapViewCameraBounds
import com.crisiscleanup.core.mapmarker.model.MapViewCameraBoundsDefault
import com.crisiscleanup.core.mapmarker.model.MapViewCameraZoom
import com.crisiscleanup.core.mapmarker.model.MapViewCameraZoomDefault
import com.crisiscleanup.core.mapmarker.ui.rememberMapProperties
import com.crisiscleanup.core.mapmarker.ui.rememberMapUiSettings
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.WorksiteMapMark
import com.crisiscleanup.core.ui.MapOverlayMessage
import com.crisiscleanup.feature.cases.CasesViewModel
import com.crisiscleanup.feature.cases.R
import com.crisiscleanup.feature.cases.model.WorksiteGoogleMapMark
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.TileProvider
import com.google.maps.android.compose.*
import com.crisiscleanup.core.mapmarker.R as mapMarkerR

@Composable
internal fun CasesRoute(
    onCasesAction: (CasesAction) -> Unit = { },
    casesViewModel: CasesViewModel = hiltViewModel(),
    createNewCase: (Long) -> Unit = {},
    openCase: (Long, Long) -> Boolean = { _, _ -> false },
) {
    val incidentsData by casesViewModel.incidentsData.collectAsStateWithLifecycle(IncidentsData.Loading)
    if (incidentsData is IncidentsData.Incidents) {
        val isTableView by casesViewModel.isTableView.collectAsStateWithLifecycle()
        BackHandler(enabled = isTableView) {
            casesViewModel.setContentViewType(false)
        }

        val isLayerView by casesViewModel.isLayerView

        val rememberOnCasesAction = remember(onCasesAction, casesViewModel) {
            { action: CasesAction ->
                when (action) {
                    CasesAction.CreateNew -> {
                        val incidentId = casesViewModel.incidentId
                        if (incidentId != EmptyIncident.id) {
                            createNewCase(incidentId)
                        }
                    }
                    CasesAction.MapView -> casesViewModel.setContentViewType(false)
                    CasesAction.TableView -> casesViewModel.setContentViewType(true)
                    CasesAction.Layers -> casesViewModel.toggleLayersView()
                    CasesAction.ZoomToInteractive -> casesViewModel.zoomToInteractive()
                    CasesAction.ZoomToIncident -> casesViewModel.zoomToIncidentBounds()
                    else -> onCasesAction(action)
                }
            }
        }
        val isMapBusy by casesViewModel.isMapBusy.collectAsStateWithLifecycle(false)
        // TODO Delay evaluation only when necessary by remembering data
        val worksitesOnMap by casesViewModel.worksitesMapMarkers.collectAsStateWithLifecycle()
        val mapCameraBounds by casesViewModel.incidentLocationBounds.collectAsStateWithLifecycle()
        val mapCameraZoom by casesViewModel.mapCameraZoom.collectAsStateWithLifecycle()
        val tileOverlayState = rememberTileOverlayState()
        val tileChangeValue by casesViewModel.overviewTileDataChange
        val clearTileLayer = remember(casesViewModel) {
            { casesViewModel.clearTileLayer }
        }
        val casesDotTileProvider = remember(casesViewModel) {
            { casesViewModel.overviewMapTileProvider() }
        }
        val onMapLoaded = remember(casesViewModel) {
            { casesViewModel.onMapLoaded() }
        }
        val onMapCameraChange = remember(casesViewModel) {
            { position: CameraPosition, projection: Projection?, activeChange: Boolean ->
                casesViewModel.onMapCameraChange(position, projection, activeChange)
            }
        }
        val showDataProgress by casesViewModel.showDataProgress.collectAsStateWithLifecycle(false)
        val dataProgress by casesViewModel.dataProgress.collectAsStateWithLifecycle(0f)
        val hiddenMarkersMessage =
            remember(casesViewModel) { { casesViewModel.hiddenMarkersMessage } }
        val onMapMarkerSelect = remember(casesViewModel) {
            { mark: WorksiteMapMark -> openCase(casesViewModel.incidentId, mark.id) }
        }
        val editedWorksiteLocation = casesViewModel.editedWorksiteLocation
        CasesScreen(
            showDataProgress = showDataProgress,
            dataProgress = dataProgress,
            onCasesAction = rememberOnCasesAction,
            isTableView = isTableView,
            isLayerView = isLayerView,
            isMapBusy = isMapBusy,
            worksitesOnMap = worksitesOnMap,
            mapCameraBounds = mapCameraBounds,
            mapCameraZoom = mapCameraZoom,
            tileChangeValue = tileChangeValue,
            tileOverlayState = tileOverlayState,
            clearTileLayer = clearTileLayer,
            casesDotTileProvider = casesDotTileProvider,
            onMapLoaded = onMapLoaded,
            onMapCameraChange = onMapCameraChange,
            hiddenMarkersMessage = hiddenMarkersMessage,
            onMarkerSelect = onMapMarkerSelect,
            editedWorksiteLocation = editedWorksiteLocation,
        )
    } else {
        val isSyncingIncidents by casesViewModel.isSyncingIncidents.collectAsState(true)
        val isLoading = incidentsData is IncidentsData.Loading || isSyncingIncidents
        val reloadIncidents = remember(casesViewModel) { { casesViewModel.refreshIncidentsData() } }
        NoCasesScreen(
            isLoading = isLoading,
            onRetryLoad = reloadIncidents,
        )
    }
}

@Composable
internal fun NoCasesScreen(
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onRetryLoad: () -> Unit = {},
) {
    Box(modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        } else {
            // TODO Use constant for width
            Column(
                modifier
                    .align(Alignment.Center)
                    .widthIn(max = 300.dp)
            ) {
                Text(stringResource(R.string.issues_loading_incidents))
                // TODO Use constant for spacing
                Spacer(modifier = Modifier.height(16.dp))
                CrisisCleanupButton(
                    modifier = modifier.align(Alignment.End),
                    onClick = onRetryLoad,
                    textResId = R.string.retry_load_incidents,
                )
            }
        }
    }
}

@Composable
internal fun CasesScreen(
    modifier: Modifier = Modifier,
    showDataProgress: Boolean = false,
    dataProgress: Float = 0f,
    onCasesAction: (CasesAction) -> Unit = {},
    isTableView: Boolean = false,
    isLayerView: Boolean = false,
    isMapBusy: Boolean = false,
    worksitesOnMap: List<WorksiteGoogleMapMark> = emptyList(),
    mapCameraBounds: MapViewCameraBounds = MapViewCameraBoundsDefault,
    mapCameraZoom: MapViewCameraZoom = MapViewCameraZoomDefault,
    tileChangeValue: Long = -1,
    clearTileLayer: () -> Boolean = { false },
    tileOverlayState: TileOverlayState = rememberTileOverlayState(),
    casesDotTileProvider: () -> TileProvider? = { null },
    onMapLoaded: () -> Unit = {},
    onMapCameraChange: (CameraPosition, Projection?, Boolean) -> Unit = { _, _, _ -> },
    hiddenMarkersMessage: () -> String = { "" },
    onMarkerSelect: (WorksiteMapMark) -> Boolean = { false },
    editedWorksiteLocation: LatLng? = null,
) {
    Box(modifier.then(Modifier.fillMaxSize())) {
        if (isTableView) {
            Text("Table view", Modifier.padding(48.dp))
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
                onMapLoaded,
                onMapCameraChange,
                hiddenMarkersMessage,
                onMarkerSelect,
                editedWorksiteLocation,
            )
        }
        CasesOverlayActions(
            modifier,
            onCasesAction,
            isTableView,
        )

        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
            visible = showDataProgress,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            LinearProgressIndicator(progress = dataProgress)
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
    onMapLoaded: () -> Unit = {},
    onMapCameraChange: (CameraPosition, Projection?, Boolean) -> Unit = { _, _, _ -> },
    hiddenMarkersMessage: () -> String = { "" },
    onMarkerSelect: (WorksiteMapMark) -> Boolean = { false },
    editedWorksiteLocation: LatLng? = null,
) {
    // TODO Profile and optimize recompositions when map is changed (by user) if possible.

    val uiSettings by rememberMapUiSettings()
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            mapCameraBounds.bounds.center,
            mapCameraZoom.zoom,
        )
    }

    val mapProperties by rememberMapProperties(mapMarkerR.raw.map_style)
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
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

    val message = hiddenMarkersMessage()
    MapOverlayMessage(message)

    val currentLocalDensity = LocalDensity.current
    LaunchedEffect(mapCameraBounds) {
        if (mapCameraBounds.takeApply()) {
            // TODO Make padding dp a parameter
            val padding = with(currentLocalDensity) { 8.dp.toPx() }
            val update = CameraUpdateFactory.newLatLngBounds(
                mapCameraBounds.bounds, padding.toInt()
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
                mapCameraZoom.center, mapCameraZoom.zoom
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
            // TODO Use zoom parameter/constant
            val update = CameraUpdateFactory.newLatLngZoom(it, 12f)
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
internal fun CasesOverlayActions(
    modifier: Modifier = Modifier,
    onCasesAction: (CasesAction) -> Unit = {},
    isTableView: Boolean = false,
) {
    ConstraintLayout(Modifier.fillMaxSize()) {
        val (zoomBar, actionBar, newCaseFab) = createRefs()

        if (!isTableView) {
            CasesZoomBar(
                modifier.constrainAs(zoomBar) {
                    top.linkTo(parent.top, margin = mapButtonEdgeSpace)
                    start.linkTo(parent.start, margin = mapButtonEdgeSpace)
                },
                onCasesAction,
            )
        }

        CasesActionBar(
            modifier.constrainAs(actionBar) {
                top.linkTo(parent.top, margin = mapButtonEdgeSpace)
                end.linkTo(parent.end, margin = mapButtonEdgeSpace)
            },
            onCasesAction,
            isTableView,
        )

        val onNewCase = remember(onCasesAction) { { onCasesAction(CasesAction.CreateNew) } }
        FloatingActionButton(
            modifier = modifier.constrainAs(newCaseFab) {
                end.linkTo(parent.end, margin = fabEdgeSpace)
                bottom.linkTo(parent.bottom, margin = fabEdgeSpace)
            },
            onClick = onNewCase,
            shape = CircleShape,
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.create_case),
            )
        }
    }
}

@Preview
@Composable
fun CasesOverlayActionsPreview() {
    CasesOverlayActions()
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