package com.crisiscleanup.feature.cases.ui

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.commoncase.model.addressQuery
import com.crisiscleanup.core.commoncase.ui.IncidentDropdownSelect
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.component.CrisisCleanupAlertDialog
import com.crisiscleanup.core.designsystem.component.CrisisCleanupButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupFab
import com.crisiscleanup.core.designsystem.component.CrisisCleanupOutlinedButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.core.designsystem.component.ExplainLocationPermissionDialog
import com.crisiscleanup.core.designsystem.component.FormListSectionSeparator
import com.crisiscleanup.core.designsystem.component.actionEdgeSpace
import com.crisiscleanup.core.designsystem.component.actionInnerSpace
import com.crisiscleanup.core.designsystem.component.actionRoundCornerShape
import com.crisiscleanup.core.designsystem.component.actionSize
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.CrisisCleanupTheme
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.disabledAlpha
import com.crisiscleanup.core.designsystem.theme.incidentDisasterContainerColor
import com.crisiscleanup.core.designsystem.theme.incidentDisasterContentColor
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.designsystem.theme.listItemSpacedByHalf
import com.crisiscleanup.core.designsystem.theme.neutralIconColor
import com.crisiscleanup.core.designsystem.theme.optionItemHeight
import com.crisiscleanup.core.designsystem.theme.primaryOrangeColor
import com.crisiscleanup.core.domain.IncidentsData
import com.crisiscleanup.core.mapmarker.model.MapViewCameraBounds
import com.crisiscleanup.core.mapmarker.model.MapViewCameraBoundsDefault
import com.crisiscleanup.core.mapmarker.model.MapViewCameraZoom
import com.crisiscleanup.core.mapmarker.model.MapViewCameraZoomDefault
import com.crisiscleanup.core.mapmarker.ui.rememberMapProperties
import com.crisiscleanup.core.mapmarker.ui.rememberMapUiSettings
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksiteMapMark
import com.crisiscleanup.core.model.data.WorksiteSortBy
import com.crisiscleanup.core.ui.LocalAppLayout
import com.crisiscleanup.feature.cases.CasesViewModel
import com.crisiscleanup.feature.cases.R
import com.crisiscleanup.feature.cases.WorksiteDistance
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
import java.text.DecimalFormat
import com.crisiscleanup.core.commonassets.R as commonAssetsR
import com.crisiscleanup.core.mapmarker.R as mapMarkerR

@Composable
internal fun CasesRoute(
    viewModel: CasesViewModel = hiltViewModel(),
    onCasesAction: (CasesAction) -> Unit = { },
    createNewCase: (Long) -> Unit = {},
    viewCase: (Long, Long) -> Boolean = { _, _ -> false },
    openAddFlag: () -> Unit = {},
) {
    val openAddFlagCounter by viewModel.openWorksiteAddFlagCounter.collectAsStateWithLifecycle()
    LaunchedEffect(openAddFlagCounter) {
        if (viewModel.takeOpenWorksiteAddFlag()) {
            openAddFlag()
        }
    }

    val incidentsData by viewModel.incidentsData.collectAsStateWithLifecycle(IncidentsData.Loading)
    val isIncidentLoading by viewModel.isIncidentLoading.collectAsState(true)
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
        val casesCount by viewModel.casesCount.collectAsStateWithLifecycle()
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
        val showDataProgress by viewModel.showDataProgress.collectAsStateWithLifecycle(false)
        val dataProgress by viewModel.dataProgress.collectAsStateWithLifecycle(0f)
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
        CasesScreen(
            showDataProgress = showDataProgress,
            dataProgress = dataProgress,
            disasterResId = disasterResId,
            onSelectIncident = onIncidentSelect,
            onCasesAction = rememberOnCasesAction,
            centerOnMyLocation = viewModel::useMyLocation,
            isTableView = isTableView,
            isLayerView = isLayerView,
            filtersCount = filtersCount,
            isIncidentLoading = isIncidentLoading,
            isMapBusy = isMapBusy,
            isTableDataTransient = isTableDataTransient,
            casesCount = casesCount,
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
        )

        if (showChangeIncident) {
            val closeDialog = remember(viewModel) { { showChangeIncident = false } }
            SelectIncidentDialog(closeDialog)
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
    if (showDialog && !viewModel.appEnv.isDebuggable) {
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
            text = t("phoneBeta.explanation")
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
    modifier: Modifier = Modifier,
    showDataProgress: Boolean = false,
    dataProgress: Float = 0f,
    onSelectIncident: () -> Unit = {},
    @DrawableRes disasterResId: Int = commonAssetsR.drawable.ic_disaster_other,
    onCasesAction: (CasesAction) -> Unit = {},
    centerOnMyLocation: () -> Unit = {},
    isTableView: Boolean = false,
    isLayerView: Boolean = false,
    filtersCount: Int = 0,
    isIncidentLoading: Boolean = false,
    isMapBusy: Boolean = false,
    isTableDataTransient: Boolean = false,
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
    isMyLocationEnabled: Boolean = false,
    onTableItemSelect: (Worksite) -> Unit = {},
) {
    Box(modifier.then(Modifier.fillMaxSize())) {
        if (isTableView) {
//            CasesTableView(
//                isIncidentLoading = isIncidentLoading,
//                isTableDataTransient = isTableDataTransient,
//                disasterResId = disasterResId,
//                openIncidentSelect = onSelectIncident,
//                onCasesAction = onCasesAction,
//                filtersCount = filtersCount,
//                casesCount = casesCount.second,
//                onTableItemSelect = onTableItemSelect,
//            )
            Text(
                "Table view under construction",
                Modifier.padding(48.dp),
            )
        } else {
            CasesMapView(
                mapCameraBounds,
                mapCameraZoom,
                isIncidentLoading || isMapBusy,
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
            modifier,
            onSelectIncident,
            disasterResId,
            onCasesAction,
            centerOnMyLocation,
            isTableView,
            casesCount,
            filtersCount,
            disableTableViewActions = isTableDataTransient,
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
private fun CasesOverlayElements(
    modifier: Modifier = Modifier,
    onSelectIncident: () -> Unit = {},
    @DrawableRes disasterResId: Int = commonAssetsR.drawable.ic_disaster_other,
    onCasesAction: (CasesAction) -> Unit = {},
    centerOnMyLocation: () -> Unit = {},
    isTableView: Boolean = false,
    casesCount: Pair<Int, Int> = Pair(0, 0),
    filtersCount: Int = 0,
    disableTableViewActions: Boolean = false,
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
                casesCount,
                Modifier.constrainAs(countTextRef) {
                    top.linkTo(parent.top, margin = actionEdgeSpace)
                    start.linkTo(disasterAction.end)
                    end.linkTo(actionBar.start)
                },
            )

            FloatingActionButton(
                modifier = modifier
                    .actionSize()
                    .constrainAs(myLocation) {
                        end.linkTo(toggleTableMap.end)
                        bottom.linkTo(newCaseFab.top, margin = actionEdgeSpace)
                    },
                onClick = centerOnMyLocation,
                shape = actionRoundCornerShape,
            ) {
                Icon(
                    painterResource(R.drawable.ic_my_location),
                    contentDescription = translator("~~My location"),
                )
            }
        }

        val enableLowerActions = !isTableView || !disableTableViewActions

        val onNewCase = remember(onCasesAction) { { onCasesAction(CasesAction.CreateNew) } }
        CrisisCleanupFab(
            modifier = modifier
                .actionSize()
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

@Composable
private fun CasesCountView(
    casesCount: Pair<Int, Int>,
    modifier: Modifier = Modifier,
) {
    val (visibleCount, totalCount) = casesCount
    val t = LocalAppTranslator.current
    if (totalCount > -1) {
        val countText = if (visibleCount == totalCount || visibleCount == 0) {
            if (visibleCount == 0) t("info.t_of_t_cases").replace("{visible_count}", "$totalCount")
            else if (totalCount == 1) t("info.1_of_1_case")
            else t("info.t_of_t_cases").replace("{visible_count}", "$totalCount")
        } else {
            t("info.v_of_t_cases")
                .replace("{visible_count}", "$visibleCount")
                .replace("{total_count}", "$totalCount")
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

@Composable
private fun BoxScope.CasesTableView(
    viewModel: CasesViewModel = hiltViewModel(),
    isIncidentLoading: Boolean = false,
    isTableDataTransient: Boolean = false,
    @DrawableRes disasterResId: Int = commonAssetsR.drawable.ic_disaster_other,
    openIncidentSelect: () -> Unit = {},
    onCasesAction: (CasesAction) -> Unit = {},
    filtersCount: Int = 0,
    casesCount: Int = 0,
    onTableItemSelect: (Worksite) -> Unit = {},
) {
    val translator = LocalAppTranslator.current

    val isTableBusy by viewModel.isTableBusy.collectAsStateWithLifecycle(false)

    val tableSort by viewModel.tableViewSort.collectAsStateWithLifecycle()
    val changeTableSort = remember(viewModel) {
        { sortBy: WorksiteSortBy -> viewModel.changeTableSort(sortBy) }
    }

    val selectedIncident by viewModel.selectedIncident.collectAsStateWithLifecycle()

    val isEditable = !isTableDataTransient

    val onOpenFlags = remember(viewModel) {
        { worksite: Worksite -> viewModel.onOpenCaseFlags(worksite) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        Row(
            listItemModifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IncidentDropdownSelect(
                onOpenIncidents = openIncidentSelect,
                disasterIconResId = disasterResId,
                title = selectedIncident.shortName,
                contentDescription = selectedIncident.shortName,
                isLoading = isIncidentLoading,
                enabled = isEditable
            )

            Spacer(Modifier.weight(1f))
            CasesActionFlatButton(
                CasesAction.Search,
                onCasesAction,
                isEditable,
            )
            FilterButtonBadge(filtersCount) {
                CasesActionFlatButton(
                    CasesAction.Filters,
                    onCasesAction,
                    isEditable,
                )
            }
        }

        Row(
            listItemModifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = listItemSpacedByHalf,
        ) {
            if (casesCount >= 0) {
                val caseCountText =
                    if (casesCount == 1) "$casesCount ${translator("casesVue.case")}"
                    else "$casesCount ${translator("casesVue.cases")}"
                Text(
                    caseCountText,
                    style = LocalFontStyles.current.header4,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            TableViewSortSelect(
                tableSort,
                isEditable = !(isIncidentLoading || isTableBusy || isTableDataTransient),
                onChange = changeTableSort
            )
        }

        val tableData by viewModel.tableData.collectAsStateWithLifecycle()

        val tableSortMessage by viewModel.tableSortResultsMessage.collectAsStateWithLifecycle()
        if (tableSortMessage.isNotBlank()) {
            Text(
                tableSortMessage,
                listItemModifier,
                style = LocalFontStyles.current.header3,
            )
        }

        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
        )
        {
            items(
                tableData,
                key = { it.worksite.id },
                contentType = { "table-item" },
            ) {
                TableViewItem(
                    it,
                    onViewCase = { onTableItemSelect(it.worksite) },
                    onOpenFlags = { onOpenFlags(it.worksite) },
                    isEditable = isEditable,
                )
                FormListSectionSeparator()
            }
        }
    }

    BusyIndicatorFloatingTopCenter(isTableBusy || isTableDataTransient)
}

private val sortByOptions = listOf(
    WorksiteSortBy.Nearest,
    WorksiteSortBy.CaseNumber,
    WorksiteSortBy.Name,
    WorksiteSortBy.City,
    WorksiteSortBy.CountyParish,
)

@Composable
private fun TableViewSortSelect(
    tableSort: WorksiteSortBy,
    isEditable: Boolean = false,
    onChange: (WorksiteSortBy) -> Unit = {},
) {
    val translator = LocalAppTranslator.current

    val sortText = translator(tableSort.translateKey)

    var showOptions by remember { mutableStateOf(false) }

    Box {
        // TODO: Dropdown where by distance asks for location permission
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.bodySmall
        ) {
            CrisisCleanupOutlinedButton(
                text = sortText,
                enabled = isEditable,
                onClick = { showOptions = true },
                fontWeight = FontWeight.W400,
            ) {
                Icon(
                    modifier = Modifier
                        .offset(x = 16.dp),
                    imageVector = CrisisCleanupIcons.ArrowDropDown,
                    contentDescription = null
                )
            }
        }

        val onSelect = { sortBy: WorksiteSortBy ->
            onChange(sortBy)
            showOptions = false
        }
        DropdownMenu(
            expanded = showOptions,
            onDismissRequest = { showOptions = false },
        ) {
            val selectedSort = if (tableSort == WorksiteSortBy.None) WorksiteSortBy.CaseNumber
            else tableSort
            for (option in sortByOptions) {
                key(option) {
                    DropdownMenuItem(
                        modifier = Modifier.optionItemHeight(),
                        text = {
                            val text = translator(option.translateKey)
                            Text(
                                text,
                                fontWeight = if (option == selectedSort) FontWeight.Bold else FontWeight.W400
                            )
                        },
                        onClick = { onSelect(option) },
                    )
                }
            }
        }
    }
}

private val oneDecimalFormat = DecimalFormat("#.#")

@Composable
private fun TableViewItem(
    worksiteDistance: WorksiteDistance,
    onViewCase: () -> Unit = {},
    onOpenFlags: () -> Unit = {},
    isEditable: Boolean = false,
) {
    val translator = LocalAppTranslator.current

    val (worksite, distance) = worksiteDistance
    val (fullAddress, locationQuery) = worksite.addressQuery

    Column(
        Modifier
            .clickable(
                onClick = onViewCase,
                enabled = isEditable,
            )
            // TODO Common dimensions
            .padding(16.dp),
        verticalArrangement = listItemSpacedBy,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = listItemSpacedBy,
        ) {
            Box(
                modifier = Modifier
                    .offset(x = (-8).dp)
                    // Similar to IconButton/IconButtonTokens.StateLayer*
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(
                        onClick = onOpenFlags,
                        enabled = isEditable,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                val tint = LocalContentColor.current
                Icon(
                    painterResource(R.drawable.ic_flag_filled_small),
                    contentDescription = translator("nav.flag"),
                    tint = if (isEditable) tint else tint.disabledAlpha(),
                )
            }
            Text(
                worksite.caseNumber,
                modifier = Modifier.offset(x = (-14).dp),
                style = LocalFontStyles.current.header3,
            )

            Spacer(modifier = Modifier.weight(1f))

            if (distance >= 0) {
                val distanceText = oneDecimalFormat.format(distance)
                Row {
                    Text(
                        distanceText,
                        modifier = Modifier.padding(end = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        translator("~~mi"),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

//        LineDivider()

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = listItemSpacedBy,
        ) {
            Icon(
                imageVector = CrisisCleanupIcons.Person,
                contentDescription = translator("nav.phone"),
                tint = neutralIconColor,
            )
            Text(worksite.name)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = listItemSpacedBy,
        ) {
            Icon(
                imageVector = CrisisCleanupIcons.Location,
                contentDescription = translator("profileOrg.address"),
                tint = neutralIconColor,
            )
            Text(fullAddress)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = listItemSpacedBy,
        ) {
            // TODO If single phone open to dialer
            //      If multiple dropdown and open to dialer on each
            //      If no numbers parsed and has text show dialog
            //      Parse each phone number by
            //      1. Replacing all non-numerics with blanks
            //      2. Replacing all consecutive spaces more than 1 space with with a new line
            //      3. Remove single spaces consolidating number sequences
            //      4. List remaining numbers of there are any 10 or 9 digit numbers or original numbers with newlines if no numbers recognized
            CrisisCleanupOutlinedButton(
                onClick = { /*TODO*/ },
                // TODO Enable if has any phone numbers
                enabled = isEditable && worksite.phone1.isNotBlank(),
            ) {
                Icon(
                    imageVector = CrisisCleanupIcons.Phone,
                    contentDescription = translator("nav.phone"),
                )
            }

            CrisisCleanupOutlinedButton(
                onClick = { /*TODO*/ },
                enabled = isEditable && locationQuery.isNotBlank(),
            ) {
                Icon(
                    imageVector = CrisisCleanupIcons.Directions,
                    contentDescription = translator("~~Directions"),
                )
            }

            // TODO Implement add to team when team management is in play

            Spacer(modifier = Modifier.weight(1f))

            // TODO Show busy and determine work type state then show correct action
            //      Enable if isEditable
            // Text("actions")
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