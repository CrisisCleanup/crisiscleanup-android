package com.crisiscleanup.feature.cases.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.commoncase.model.WorksiteGoogleMapMark
import com.crisiscleanup.core.commoncase.ui.CaseMapOverlayElements
import com.crisiscleanup.core.commoncase.ui.CasesAction
import com.crisiscleanup.core.commoncase.ui.CasesMapView
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.component.CrisisCleanupAlertDialog
import com.crisiscleanup.core.designsystem.component.CrisisCleanupButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.core.designsystem.component.ExplainLocationPermissionDialog
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.disabledAlpha
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.designsystem.theme.primaryOrangeColor
import com.crisiscleanup.core.domain.IncidentsData
import com.crisiscleanup.core.mapmarker.model.MapViewCameraBounds
import com.crisiscleanup.core.mapmarker.model.MapViewCameraBoundsDefault
import com.crisiscleanup.core.mapmarker.model.MapViewCameraZoom
import com.crisiscleanup.core.mapmarker.model.MapViewCameraZoomDefault
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.Incident
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksiteMapMark
import com.crisiscleanup.core.selectincident.SelectIncidentDialog
import com.crisiscleanup.feature.cases.CasesViewModel
import com.crisiscleanup.feature.cases.DataProgressMetrics
import com.crisiscleanup.feature.cases.zeroDataProgress
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.TileProvider
import com.google.maps.android.compose.TileOverlayState
import com.google.maps.android.compose.rememberTileOverlayState
import com.crisiscleanup.core.commonassets.R as commonAssetsR

@Composable
internal fun CasesRoute(
    viewModel: CasesViewModel = hiltViewModel(),
    onCasesAction: (CasesAction) -> Unit = { },
    createNewCase: (Long) -> Unit = {},
    viewCase: (Long, Long) -> Boolean = { _, _ -> false },
    openAddFlag: () -> Unit = {},
    openTransferWorkType: () -> Unit = {},
    onAssignCaseTeam: (Long) -> Unit = {},
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
            onAssignCaseTeam = onAssignCaseTeam,
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
                onRefreshIncidentsAsync = viewModel::refreshIncidentsAsync,
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
        NoIncidentsScreen(
            isLoading = isLoading,
            onRetryLoad = viewModel::refreshIncidentsData,
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
internal fun NoIncidentsScreen(
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
private fun CasesScreen(
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
    onAssignCaseTeam: (Long) -> Unit = {},
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
                onAssignCaseTeam = onAssignCaseTeam,
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
        CaseMapOverlayElements(
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
            showCasesMainActions = false,
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

@Preview
@Composable
fun NoCasesLoadingPreview() {
    NoIncidentsScreen(isLoading = true)
}

@Preview
@Composable
fun NoCasesRetryPreview() {
    NoIncidentsScreen()
}
