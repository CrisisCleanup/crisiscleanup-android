package com.crisiscleanup.feature.cases

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.component.CrisisCleanupButton
import com.crisiscleanup.feature.cases.model.MapViewCameraBounds
import com.crisiscleanup.feature.cases.model.MapViewCameraBoundsDefault
import com.crisiscleanup.feature.cases.model.WorksiteGoogleMapMark
import com.crisiscleanup.feature.cases.ui.CasesAction
import com.crisiscleanup.feature.cases.ui.CasesActionBar
import com.crisiscleanup.feature.cases.ui.CasesZoomBar
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
internal fun CasesRoute(
    modifier: Modifier = Modifier,
    onCasesAction: (CasesAction) -> Unit = { },
    casesViewModel: CasesViewModel = hiltViewModel(),
) {
    val incidentsData by casesViewModel.incidentsData.collectAsStateWithLifecycle()
    if (incidentsData is IncidentsData.Incidents) {
        val isTableView by casesViewModel.isTableView.collectAsStateWithLifecycle()
        val isLayerView by casesViewModel.isLayerView

        val rememberOnCasesAction = remember(onCasesAction, casesViewModel) {
            { action: CasesAction ->
                when (action) {
                    CasesAction.MapView -> {
                        casesViewModel.setContentViewType(false)
                    }

                    CasesAction.TableView -> {
                        casesViewModel.setContentViewType(true)
                    }

                    CasesAction.Layers -> {
                        casesViewModel.toggleLayersView()
                    }

                    CasesAction.ZoomToInteractive -> {
                        // TODO
                    }

                    CasesAction.ZoomToIncident -> {
                        // TODO
                    }

                    else -> {
                        onCasesAction(action)
                    }
                }
            }
        }
        // TODO Delay evaluation only when necessary by remembering data
        val worksitesOnMap by casesViewModel.worksitesMapMarkers.collectAsStateWithLifecycle()
        val mapCameraBounds by casesViewModel.mapCameraBounds.collectAsStateWithLifecycle()
        val onMapCameraChange = remember(casesViewModel) {
            { projection: Projection?, activeChange: Boolean ->
                casesViewModel.onMapCameraChange(projection, activeChange)
            }
        }
        CasesScreen(
            modifier,
            onCasesAction = rememberOnCasesAction,
            isTableView = isTableView,
            isLayerView = isLayerView,
            worksitesOnMap = worksitesOnMap,
            mapCameraBounds = mapCameraBounds,
            onMapCameraChange = onMapCameraChange,
        )
    } else {
        val isLoading = incidentsData is IncidentsData.Loading
        NoCasesScreen(modifier, isLoading)
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
    onCasesAction: (CasesAction) -> Unit = {},
    isTableView: Boolean = false,
    isLayerView: Boolean = false,
    worksitesOnMap: List<WorksiteGoogleMapMark> = emptyList(),
    mapCameraBounds: MapViewCameraBounds = MapViewCameraBoundsDefault,
    onMapCameraChange: (Projection?, Boolean) -> Unit = { _, _ -> },
) {
    Box(modifier.then(Modifier.fillMaxSize())) {
        if (isTableView) {
            Text("Table view", Modifier.padding(48.dp))
        } else {
            CasesMapView(
                mapCameraBounds,
                worksitesOnMap,
                onMapCameraChange = onMapCameraChange,
            )
        }
        CasesOverlayActions(
            modifier,
            onCasesAction,
            isTableView,
        )
    }
}

@Composable
internal fun CasesMapView(
    mapCameraBounds: MapViewCameraBounds,
    worksitesOnMap: List<WorksiteGoogleMapMark> = emptyList(),
    onMapCameraChange: (Projection?, Boolean) -> Unit = { _, _ -> },
) {
    // TODO Profile and optimize recompositions when map is changed (by user) if possible.

    val uiSettings by remember {
        mutableStateOf(
            MapUiSettings(
                zoomControlsEnabled = false,
                tiltGesturesEnabled = false,
                rotationGesturesEnabled = false,
            )
        )
    }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            mapCameraBounds.bounds.center,
            // TODO Use parameter
            10f,
        )
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        uiSettings = uiSettings,
        cameraPositionState = cameraPositionState,
    ) {
        // TODO Is it possible to cache? If so test recomposition. If not document why not.
        worksitesOnMap.forEach {
            Marker(it.markerState)
        }
    }

    val currentLocalDensity = LocalDensity.current
    LaunchedEffect(mapCameraBounds) {
        if (mapCameraBounds.takeApply()) {
            // TODO Make padding dp a parameter
            val padding = with(currentLocalDensity) { 8.dp.toPx() }
            val update = CameraUpdateFactory.newLatLngBounds(
                mapCameraBounds.bounds, padding.toInt()
            )
            cameraPositionState.animate(update, 500)
        }
    }

    val movingCamera = remember {
        derivedStateOf {
            cameraPositionState.isMoving && cameraPositionState.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE
        }
    }
    onMapCameraChange(
        cameraPositionState.projection,
        movingCamera.value
    )
}

private val actionSpacing = 8.dp
private val fabSpacing = 16.dp

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
                    top.linkTo(parent.top, margin = actionSpacing)
                    start.linkTo(parent.start, margin = actionSpacing)
                },
                onCasesAction,
            )
        }

        CasesActionBar(
            modifier.constrainAs(actionBar) {
                top.linkTo(parent.top, margin = actionSpacing)
                end.linkTo(parent.end, margin = actionSpacing)
            },
            onCasesAction,
            isTableView,
        )

        val onNewCase = remember(onCasesAction) { { onCasesAction(CasesAction.CreateNew) } }
        FloatingActionButton(
            modifier = modifier.constrainAs(newCaseFab) {
                end.linkTo(parent.end, margin = fabSpacing)
                bottom.linkTo(parent.bottom, margin = fabSpacing)
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