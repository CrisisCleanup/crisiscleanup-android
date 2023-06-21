package com.crisiscleanup.feature.cases.ui

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.component.CrisisCleanupButton
import com.crisiscleanup.core.designsystem.component.actionEdgeSpace
import com.crisiscleanup.core.designsystem.component.actionInnerSpace
import com.crisiscleanup.core.designsystem.component.actionRoundCornerShape
import com.crisiscleanup.core.designsystem.component.actionSize
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.CrisisCleanupTheme
import com.crisiscleanup.core.designsystem.theme.incidentDisasterContainerColor
import com.crisiscleanup.core.designsystem.theme.incidentDisasterContentColor
import com.crisiscleanup.core.designsystem.theme.primaryOrangeColor
import com.crisiscleanup.core.domain.IncidentsData
import com.crisiscleanup.core.mapmarker.model.MapViewCameraBounds
import com.crisiscleanup.core.mapmarker.model.MapViewCameraBoundsDefault
import com.crisiscleanup.core.mapmarker.model.MapViewCameraZoom
import com.crisiscleanup.core.mapmarker.model.MapViewCameraZoomDefault
import com.crisiscleanup.core.mapmarker.ui.rememberMapProperties
import com.crisiscleanup.core.mapmarker.ui.rememberMapUiSettings
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.WorksiteMapMark
import com.crisiscleanup.core.ui.LocalAppLayout
import com.crisiscleanup.feature.cases.CasesViewModel
import com.crisiscleanup.feature.cases.R
import com.crisiscleanup.feature.cases.model.WorksiteGoogleMapMark
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
    onCasesAction: (CasesAction) -> Unit = { },
    casesViewModel: CasesViewModel = hiltViewModel(),
    createNewCase: (Long) -> Unit = {},
    viewCase: (Long, Long) -> Boolean = { _, _ -> false },
) {
    val incidentsData by casesViewModel.incidentsData.collectAsStateWithLifecycle(IncidentsData.Loading)
    val isIncidentLoading by casesViewModel.isIncidentLoading.collectAsState(true)
    if (incidentsData is IncidentsData.Incidents) {
        val isTableView by casesViewModel.isTableView.collectAsStateWithLifecycle()
        BackHandler(enabled = isTableView) {
            casesViewModel.setContentViewType(false)
        }

        val isLayerView by casesViewModel.isLayerView

        val disasterResId by casesViewModel.disasterIconResId.collectAsStateWithLifecycle()
        var showChangeIncident by rememberSaveable { mutableStateOf(false) }
        val onIncidentSelect = remember(casesViewModel) { { showChangeIncident = true } }

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
                    CasesAction.ZoomIn -> casesViewModel.zoomIn()
                    CasesAction.ZoomOut -> casesViewModel.zoomOut()
                    else -> onCasesAction(action)
                }
            }
        }
        val isMapBusy by casesViewModel.isMapBusy.collectAsStateWithLifecycle(false)
        val casesCount by casesViewModel.casesCount.collectAsStateWithLifecycle()
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
        val onMapLoadStart = remember(casesViewModel) { { casesViewModel.onMapLoadStart() } }
        val onMapLoaded = remember(casesViewModel) { { casesViewModel.onMapLoaded() } }
        val onMapCameraChange = remember(casesViewModel) {
            { position: CameraPosition, projection: Projection?, activeChange: Boolean ->
                casesViewModel.onMapCameraChange(position, projection, activeChange)
            }
        }
        val showDataProgress by casesViewModel.showDataProgress.collectAsStateWithLifecycle(false)
        val dataProgress by casesViewModel.dataProgress.collectAsStateWithLifecycle(0f)
        val onMapMarkerSelect = remember(casesViewModel) {
            { mark: WorksiteMapMark -> viewCase(casesViewModel.incidentId, mark.id) }
        }
        val editedWorksiteLocation = casesViewModel.editedWorksiteLocation
        CasesScreen(
            showDataProgress = showDataProgress,
            dataProgress = dataProgress,
            disasterResId = disasterResId,
            onSelectIncident = onIncidentSelect,
            onCasesAction = rememberOnCasesAction,
            isTableView = isTableView,
            isLayerView = isLayerView,
            isMapBusy = isIncidentLoading || isMapBusy,
            casesCount = casesCount,
            worksitesOnMap = worksitesOnMap,
            mapCameraBounds = mapCameraBounds,
            mapCameraZoom = mapCameraZoom,
            tileChangeValue = tileChangeValue,
            tileOverlayState = tileOverlayState,
            clearTileLayer = clearTileLayer,
            casesDotTileProvider = casesDotTileProvider,
            onMapLoadStart = onMapLoadStart,
            onMapLoaded = onMapLoaded,
            onMapCameraChange = onMapCameraChange,
            onMarkerSelect = onMapMarkerSelect,
            editedWorksiteLocation = editedWorksiteLocation,
        )

        if (showChangeIncident) {
            val closeDialog = remember(casesViewModel) { { showChangeIncident = false } }
            SelectIncidentDialog(closeDialog)
        }
    } else {
        val isLoading = incidentsData is IncidentsData.Loading || isIncidentLoading
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
            BusyIndicatorFloatingTopCenter(true)
        } else {
            // TODO Use constant for width
            Column(
                modifier
                    .align(Alignment.Center)
                    .widthIn(max = 300.dp)
            ) {
                Text(text = LocalAppTranslator.current.translator("info.incident_load_error"))
                // TODO Use constant for spacing
                Spacer(modifier = Modifier.height(16.dp))
                CrisisCleanupButton(
                    modifier = modifier.align(Alignment.End),
                    onClick = onRetryLoad,
                    text = LocalAppTranslator.current.translator("actions.retry"),
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
    onSelectIncident: () -> Unit = {},
    @DrawableRes disasterResId: Int = commonAssetsR.drawable.ic_disaster_other,
    onCasesAction: (CasesAction) -> Unit = {},
    isTableView: Boolean = false,
    isLayerView: Boolean = false,
    isMapBusy: Boolean = false,
    casesCount: Pair<Int, Int> = Pair(0, 0),
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
                onMapLoadStart,
                onMapLoaded,
                onMapCameraChange,
                onMarkerSelect,
                editedWorksiteLocation,
            )
        }
        CasesOverlayActions(
            modifier,
            onSelectIncident,
            disasterResId,
            onCasesAction,
            isTableView,
            casesCount,
        )

        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
            visible = showDataProgress,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            LinearProgressIndicator(
                progress = dataProgress,
                color = primaryOrangeColor,
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
internal fun CasesOverlayActions(
    modifier: Modifier = Modifier,
    onSelectIncident: () -> Unit = {},
    @DrawableRes disasterResId: Int = commonAssetsR.drawable.ic_disaster_other,
    onCasesAction: (CasesAction) -> Unit = {},
    isTableView: Boolean = false,
    casesCount: Pair<Int, Int> = Pair(0, 0),
) {
    val translator = LocalAppTranslator.current.translator
    ConstraintLayout(Modifier.fillMaxSize()) {
        val (
            disasterAction,
            zoomBar,
            actionBar,
            newCaseFab,
            toggleTableMap,
            countTextRef,
        ) = createRefs()

        FloatingActionButton(
            modifier = modifier
                .constrainAs(disasterAction) {
                    start.linkTo(parent.start, margin = actionEdgeSpace)
                    top.linkTo(parent.top, margin = actionEdgeSpace)
                },
            onClick = onSelectIncident,
            shape = CircleShape,
            containerColor = incidentDisasterContainerColor,
            contentColor = incidentDisasterContentColor,
        ) {
            Icon(
                painter = painterResource(disasterResId),
                contentDescription = translator("nav.change_incident"),
            )
        }

        if (!isTableView) {
            CasesZoomBar(
                modifier.constrainAs(zoomBar) {
                    top.linkTo(disasterAction.bottom, margin = actionInnerSpace)
                    start.linkTo(disasterAction.start)
                    end.linkTo(disasterAction.end)
                },
                onCasesAction,
            )
        }

        CasesActionBar(
            modifier.constrainAs(actionBar) {
                top.linkTo(parent.top, margin = actionEdgeSpace)
                end.linkTo(parent.end, margin = actionEdgeSpace)
            },
            onCasesAction,
        )

        CasesCountView(
            casesCount,
            Modifier.constrainAs(countTextRef) {
                top.linkTo(parent.top, margin = actionEdgeSpace)
                start.linkTo(disasterAction.end)
                end.linkTo(actionBar.start)
            },
        )

        val onNewCase = remember(onCasesAction) { { onCasesAction(CasesAction.CreateNew) } }
        FloatingActionButton(
            modifier = modifier
                .actionSize()
                .constrainAs(newCaseFab) {
                    end.linkTo(toggleTableMap.end)
                    bottom.linkTo(toggleTableMap.top, margin = actionEdgeSpace)
                },
            onClick = onNewCase,
            shape = actionRoundCornerShape,
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
        FloatingActionButton(
            modifier = modifier
                .actionSize()
                .constrainAs(toggleTableMap) {
                    end.linkTo(parent.end, margin = actionEdgeSpace)
                    bottom.linkTo(parent.bottom, margin = bottomPadding)
                },
            onClick = toggleMapTableView,
            shape = actionRoundCornerShape,
        ) {
            Icon(
                painter = painterResource(tableMapAction.iconResId),
                contentDescription = translator(tableMapAction.descriptionTranslateKey),
            )
        }
    }
}

@Composable
private fun CasesCountView(
    casesCount: Pair<Int, Int>,
    modifier: Modifier = Modifier,
) {
    val (visibleCount, totalCount) = casesCount
    if (totalCount > -1) {
        val countText = if (visibleCount in 1 until totalCount) {
            pluralStringResource(
                R.plurals.visible_total_case_count,
                visibleCount,
                visibleCount,
                totalCount,
            )
        } else {
            pluralStringResource(
                R.plurals.total_case_count,
                totalCount,
                totalCount,
            )
        }

        // Common dimensions and styles
        Surface(
            modifier,
            color = Color(0xFF393939),
            contentColor = Color.White,
            shadowElevation = 4.dp,
            shape = RoundedCornerShape(4.dp),
        ) {
            Text(
                countText,
                modifier = Modifier.padding(
                    horizontal = 15.dp,
                    vertical = 9.dp,
                ),
                color = Color.White,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Preview
@Composable
fun CasesOverlayActionsPreview() {
    CrisisCleanupTheme {
        CasesOverlayActions()
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