package com.crisiscleanup.feature.incidentcache.ui

import android.view.MotionEvent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.AnimatedBusyIndicator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupIconButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupRadioButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.core.designsystem.component.ExplainLocationPermissionDialog
import com.crisiscleanup.core.designsystem.component.TopAppBarBackAction
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.disabledAlpha
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.designsystem.theme.listItemSpacedByHalf
import com.crisiscleanup.core.mapmarker.ui.rememberMapProperties
import com.crisiscleanup.core.mapmarker.ui.rememberMapUiSettings
import com.crisiscleanup.core.model.data.BoundedRegionParameters
import com.crisiscleanup.feature.incidentcache.BoundedRegionDataEditor
import com.crisiscleanup.feature.incidentcache.IncidentWorksitesCacheViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.launch

@Composable
fun IncidentWorksitesCacheRoute(
    onBack: () -> Unit,
) {
    IncidentWorksitesCacheScreen(
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun IncidentWorksitesCacheScreen(
    onBack: () -> Unit,
    viewModel: IncidentWorksitesCacheViewModel = hiltViewModel(),
) {
    val t = LocalAppTranslator.current

    val incident by viewModel.incident.collectAsStateWithLifecycle()
    val isSyncingIncident by viewModel.isSyncing.collectAsStateWithLifecycle()

    val isNotProduction = viewModel.isNotProduction

    val lastSynced by viewModel.lastSynced.collectAsStateWithLifecycle()

    val isUpdatingSyncParameters by viewModel.isUpdatingSyncMode.collectAsStateWithLifecycle()
    val isParametersEnabled = !isUpdatingSyncParameters
    val syncParameters by viewModel.syncingParameters.collectAsStateWithLifecycle()
    val isBoundedRegionSync = syncParameters.isRegionBounded
    val boundedRegionParameters by viewModel.editableRegionBoundedParameters.collectAsStateWithLifecycle()

    var isMapMoving by remember { mutableStateOf(false) }

    var contentSize by remember { mutableStateOf(IntSize.Zero) }

    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val scrollToBoundedSection = remember(scrollState) {
        {
            coroutineScope.launch {
                scrollState.animateScrollToItem(4)
            }
            Unit
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBarBackAction(
            title = incident.shortName,
            onAction = onBack,
        )

        LazyColumn(
            Modifier
                .fillMaxSize()
                .onGloballyPositioned {
                    contentSize = it.size
                },
            scrollState,
            userScrollEnabled = !isMapMoving,
        ) {
            item {
                val syncedText = lastSynced?.let {
                    t("~~Synced {sync_date}")
                        .replace("{sync_date}", it)
                } ?: t("~~Awaiting sync of {incident_name}")
                    .replace("{incident_name}", incident.shortName)
                Row(
                    listItemModifier,
                    horizontalArrangement = listItemSpacedBy,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(syncedText)

                    // TODO remove if buttons show loading state
                    AnimatedBusyIndicator(
                        isSyncingIncident,
                        padding = 0.dp,
                    )
                }
            }

            item(
                key = "sync-auto-download",
                contentType = "sync-choice-item",
            ) {
                CrisisCleanupRadioButton(
                    listItemModifier,
                    syncParameters.isAutoCache,
                    text = t("~~Auto download Cases"),
                    enabled = isParametersEnabled,
                    onSelect = viewModel::resumeCachingCases,
                ) {
                    // TODO Downloaded newest Cases action
                }
            }

            item(
                key = "sync-pause",
                contentType = "sync-choice-item",
            ) {
                CrisisCleanupRadioButton(
                    listItemModifier,
                    syncParameters.isPaused,
                    text = t("~~Pause downloading Cases"),
                    enabled = isParametersEnabled,
                    onSelect = viewModel::pauseCachingCases,
                ) {
                    t("~~Resume downloading Cases by selecting to auto download or download Cases in a region")
                }
            }

            item(
                key = "sync-bounded-region",
                contentType = "sync-choice-item",
            ) {
                CrisisCleanupRadioButton(
                    listItemModifier,
                    isBoundedRegionSync,
                    text = t("~~Downloading Cases within specified region"),
                    enabled = isParametersEnabled,
                    onSelect = {
                        viewModel.boundCachingCases()
                        scrollToBoundedSection()
                    },
                )
            }

            item {
                Column {
                    CompositionLocalProvider(
                        LocalContentColor provides if (isBoundedRegionSync) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.disabledAlpha()
                        },
                    ) {
                        BoundedRegionSection(
                            contentSize,
                            isUpdatingSyncParameters = isUpdatingSyncParameters,
                            isBoundedRegionSync = isBoundedRegionSync,
                            boundedRegionParameters,
                            editor = viewModel.boundedRegionDataEditor,
                            setMovingMap = {
                                isMapMoving = it
                            },
                            onMovableMap = scrollToBoundedSection,
                            setUseMyLocation = viewModel::setBoundedUseMyLocation,
                            updateRegionRadius = viewModel::setBoundedRegionRadius,
                        )
                    }
                }
            }

            if (isNotProduction) {
                item {
                    CrisisCleanupTextButton(
                        text = "Reset Incident Cases cache",
                        onClick = viewModel::resetCaching,
                    )
                }
            }
        }
    }

    val editor = viewModel.boundedRegionDataEditor
    val closePermissionDialog = remember(editor) {
        {
            editor.showExplainPermissionLocation.value = false
        }
    }
    val explainPermission by editor.showExplainPermissionLocation
    ExplainLocationPermissionDialog(
        showDialog = explainPermission,
        closeDialog = closePermissionDialog,
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun BoundedRegionSection(
    contentSize: IntSize,
    isUpdatingSyncParameters: Boolean,
    isBoundedRegionSync: Boolean,
    regionParameters: BoundedRegionParameters,
    editor: BoundedRegionDataEditor,
    setMovingMap: (Boolean) -> Unit,
    onMovableMap: () -> Unit,
    setUseMyLocation: (Boolean) -> Unit,
    updateRegionRadius: (Float) -> Unit,
) {
    val t = LocalAppTranslator.current

    var isMapMovable by remember { mutableStateOf(false) }
    val setMapMovable = { isMovable: Boolean ->
        isMapMovable = isMovable
    }

    val isRegionEditable = !isUpdatingSyncParameters &&
        isBoundedRegionSync

    val density = LocalDensity.current
    val mapContentHeightDp = remember(contentSize) {
        val minSize = with(contentSize) {
            width.coerceAtMost(height)
        }
        with(density) {
            minSize.toDp()
        }
    }
    val mapHeightAnimated = if (isBoundedRegionSync && isMapMovable) {
        mapContentHeightDp
    } else {
        mapContentHeightDp.div(2)
    }

    Row(
        Modifier
            .clickable(
                enabled = isRegionEditable,
                onClick = {
                    setMapMovable(!isMapMovable)
                },
            )
            .listItemPadding(),
        horizontalArrangement = listItemSpacedBy,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier.weight(1f),
            verticalArrangement = listItemSpacedByHalf,
        ) {
            Text(t("~~Change location on map"))
            Text(
                t("~~Toggle on to be able to change the map location"),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Switch(
            isMapMovable,
            setMapMovable,
            enabled = isRegionEditable,
        )
    }

    val isMapEditable = isRegionEditable && isMapMovable
    Box(
        Modifier.fillMaxWidth()
            .animateContentSize(
                finishedListener = { _, _ ->
                    if (isMapMovable) {
                        onMovableMap()
                    }
                },
            )
            .height(mapHeightAnimated),
    ) {
        MovableMapView(
            editor,
            isEditable = isMapEditable,
            Modifier
                .pointerInteropFilter(
                    onTouchEvent = {
                        when (it.action) {
                            MotionEvent.ACTION_DOWN -> {
                                setMovingMap(isMapMovable)
                                if (!isMapMovable) {
                                    // TODO Alert if this happens more than x times per n second
                                }
                                false
                            }

                            else -> true
                        }
                    },
                ),
            onReleaseMapTouch = { setMovingMap(false) },
        )

        if (!isMapEditable) {
            Box(
                Modifier
                    .background(Color.Black.disabledAlpha())
                    .fillMaxSize(),
            )
            if (isBoundedRegionSync && !isMapMovable) {
                CompositionLocalProvider(
                    LocalContentColor provides Color.White,
                ) {
                    CrisisCleanupIconButton(
                        // TODO Update button ripple size proportional to icon size
                        modifier = Modifier.align(Alignment.Center),
                        // TODO Common dimensions
                        iconModifier = Modifier.size(96.dp),
                        imageVector = CrisisCleanupIcons.PanMap,
                        contentDescription = t("~~Change location on map"),
                        onClick = { isMapMovable = true },
                    )
                }
            }
        }
    }

    Row(
        Modifier
            .clickable(
                enabled = isRegionEditable,
                onClick = {
                    setUseMyLocation(!regionParameters.isRegionMyLocation)
                },
            )
            .listItemPadding(),
        horizontalArrangement = listItemSpacedBy,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            t("~~Always use my location"),
            Modifier.weight(1f),
        )
        Switch(
            regionParameters.isRegionMyLocation,
            onCheckedChange = setUseMyLocation,
            enabled = isRegionEditable,
        )
    }

    Column(
        Modifier.listItemPadding(),
        verticalArrangement = listItemSpacedByHalf,
    ) {
        val fixedRadius = "%.1f".format(regionParameters.regionRadiusMiles)
        Text(
            t("~~Radius {magnitude} mi.")
                .replace("{magnitude}", fixedRadius),
        )
        Slider(
            regionParameters.regionRadiusMiles,
            onValueChange = updateRegionRadius,
            valueRange = 1.0f..120.0f,
            enabled = isRegionEditable,
        )
    }
}

@Composable
private fun MovableMapView(
    editor: BoundedRegionDataEditor,
    isEditable: Boolean,
    modifier: Modifier = Modifier,
    onReleaseMapTouch: () -> Unit = {},
) {
    val onMapLoaded = editor::onMapLoaded
    val onMapCameraChange = remember(editor) {
        {
                position: CameraPosition,
                projection: Projection?,
                isUserInteraction: Boolean,
            ->
            editor.onMapCameraChange(position, projection, isUserInteraction)
        }
    }

    val mapCameraZoom by editor.mapCameraZoom.collectAsStateWithLifecycle()

    val markerState = rememberMarkerState()
    val coordinates by editor.centerCoordinates.collectAsStateWithLifecycle()
    markerState.position = coordinates

    var uiSettings by rememberMapUiSettings()
    if (uiSettings.scrollGesturesEnabled != isEditable) {
        uiSettings = uiSettings.copy(
            scrollGesturesEnabled = isEditable,
            zoomGesturesEnabled = isEditable,
            zoomControlsEnabled = isEditable,
        )
    }
    val mapProperties by rememberMapProperties()
    val cameraPositionState = rememberCameraPositionState()
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            onReleaseMapTouch()
        }
    }
    // TODO Draw radius where data will be cached
    GoogleMap(
        modifier = modifier,
        uiSettings = uiSettings,
        properties = mapProperties,
        cameraPositionState = cameraPositionState,
        onMapLoaded = onMapLoaded,
    ) {
        Marker(
            markerState,
            icon = editor.mapMarkerIcon,
        )
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

    val movingCamera by remember {
        derivedStateOf {
            cameraPositionState.isMoving && cameraPositionState.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE
        }
    }
    onMapCameraChange(
        cameraPositionState.position,
        cameraPositionState.projection,
        movingCamera,
    )
}
