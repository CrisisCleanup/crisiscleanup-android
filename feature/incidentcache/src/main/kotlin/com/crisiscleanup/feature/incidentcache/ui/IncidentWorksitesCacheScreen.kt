package com.crisiscleanup.feature.incidentcache.ui

import android.view.MotionEvent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.common.milesToMeters
import com.crisiscleanup.core.data.repository.IncidentCacheStage
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.AnimatedBusyIndicator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.core.designsystem.component.ExplainLocationPermissionDialog
import com.crisiscleanup.core.designsystem.component.TopAppBarBackAction
import com.crisiscleanup.core.designsystem.theme.disabledAlpha
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.designsystem.theme.listItemSpacedByHalf
import com.crisiscleanup.core.designsystem.theme.primaryOrangeColor
import com.crisiscleanup.core.mapmarker.ui.rememberMapProperties
import com.crisiscleanup.core.mapmarker.ui.rememberMapUiSettings
import com.crisiscleanup.core.model.data.BoundedRegionParameters
import com.crisiscleanup.feature.incidentcache.BoundedRegionDataEditor
import com.crisiscleanup.feature.incidentcache.IncidentWorksitesCacheViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.Circle
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IncidentWorksitesCacheScreen(
    onBack: () -> Unit,
    viewModel: IncidentWorksitesCacheViewModel = hiltViewModel(),
) {
    val t = LocalAppTranslator.current

    val incident by viewModel.incident.collectAsStateWithLifecycle()
    val isSyncingIncident by viewModel.isSyncing.collectAsStateWithLifecycle()

    val syncStage by viewModel.syncStage.collectAsStateWithLifecycle()

    val isNotProduction = viewModel.isNotProduction

    val lastSynced by viewModel.lastSynced.collectAsStateWithLifecycle()

    val editingParameters by viewModel.editingPreferences.collectAsStateWithLifecycle()

    var isMapMoving by remember { mutableStateOf(false) }

    var contentSize by remember { mutableStateOf(IntSize.Zero) }

    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val scrollToNearMeSection = remember(scrollState) {
        {
            coroutineScope.launch {
                scrollState.animateScrollToItem(4)
            }
            Unit
        }
    }
    val scrollToBoundedSection = remember(scrollState) {
        {
            coroutineScope.launch {
                scrollState.animateScrollToItem(5)
            }
            Unit
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBarBackAction(
            title = t("~~Sync strategy"),
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
            item(
                key = "last-synced-info",
                contentType = "text-item",
            ) {
                val syncedText = lastSynced?.let {
                    t("~~Completely synced {sync_date}")
                        .replace("{sync_date}", it)
                } ?: t("~~Awaiting complete sync of {incident_name}")
                    .replace("{incident_name}", incident.shortName)
                Text(
                    syncedText,
                    listItemModifier,
                )
            }

            item {
                Row(
                    listItemModifier,
                    horizontalArrangement = listItemSpacedBy,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val syncStageMessage = when (syncStage) {
                        IncidentCacheStage.Start -> t("~~Ready to sync")
                        IncidentCacheStage.Incidents -> t("~~Syncing Incidents")
                        IncidentCacheStage.WorksitesBounded -> t("~~Syncing Cases nearby/in area")
                        IncidentCacheStage.WorksitesPreload -> t("~~Syncing Cases nearby")
                        IncidentCacheStage.WorksitesCore -> t("~~Syncing Cases")
                        IncidentCacheStage.WorksitesAdditional -> t("~~Syncing additional Case data")
                        IncidentCacheStage.ActiveIncident -> t("~~Syncing active Incident")
                        IncidentCacheStage.ActiveIncidentOrganization -> t("~~Syncing organizations")
                        IncidentCacheStage.End -> t("~~Sync finished")
                    }
                    Text(syncStageMessage)

                    AnimatedBusyIndicator(
                        isSyncingIncident,
                        padding = 0.dp,
                    )
                }
            }

            item(
                key = "strategy-selection-info",
                contentType = "text-item",
            ) {
                Text(
                    "~~Cases are downloaded according to the selected strategy",
                    listItemModifier,
                )
            }

            synChoiceItem(
                itemKey = "sync-auto-download",
                editingParameters.isAutoCache,
                textKey = "~~Adaptive",
                subTextKey = "~~Changes the strategy and amount of Cases downloaded relative to network speed.",
                onSelect = viewModel::resumeCachingCases,
            )

            synChoiceItem(
                itemKey = "sync-pause",
                editingParameters.isPaused,
                textKey = "~~Paused",
                subTextKey = "~~Pause downloading Cases until network speed is sufficient to resume.",
                onSelect = viewModel::pauseCachingCases,
            )

            synChoiceItem(
                itemKey = "sync-near-me",
                editingParameters.isBoundedNearMe,
                textKey = "~~Near me",
                subTextKey = "~~Download Cases around my current location.",
                onSelect = {
                    viewModel.boundCachingCases(true, isUserAction = true)
                    scrollToNearMeSection()
                },
            )

            synChoiceItem(
                itemKey = "sync-bounded-region",
                editingParameters.isBoundedByCoordinates,
                textKey = "~~Specify area",
                subTextKey = "~~Drag the map to download Cases surrounding the pin.",
                onSelect = {
                    viewModel.boundCachingCases(false)
                    scrollToBoundedSection()
                },
            )

            item {
                Column {
                    CompositionLocalProvider(
                        LocalContentColor provides if (editingParameters.isRegionBounded) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.disabledAlpha()
                        },
                    ) {
                        BoundedRegionSection(
                            contentSize,
                            isBoundedNearMe = editingParameters.isBoundedNearMe,
                            isBoundedByCoordinates = editingParameters.isBoundedByCoordinates,
                            editingParameters.boundedRegionParameters,
                            viewModel.boundedRegionDataEditor,
                            setMovingMap = {
                                isMapMoving = it
                            },
                            onMovableMap = scrollToBoundedSection,
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

private fun LazyListScope.synChoiceItem(
    itemKey: String,
    isSelected: Boolean,
    textKey: String,
    subTextKey: String,
    enabled: Boolean = true,
    onSelect: () -> Unit = {},
) {
    item(
        key = itemKey,
        contentType = "sync-choice-item",
    ) {
        val t = LocalAppTranslator.current

        var radioButtonWidth by remember { mutableStateOf(48.dp) }
        val density = LocalDensity.current
        Column(
            Modifier
                .selectable(
                    selected = isSelected,
                    enabled = enabled,
                    onClick = onSelect,
                    role = Role.RadioButton,
                )
                .then(listItemModifier),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    isSelected,
                    onSelect,
                    Modifier.onGloballyPositioned {
                        radioButtonWidth = with(density) { it.size.width.toDp() }
                    },
                    enabled,
                )
                Text(t(textKey))
            }
            Text(
                t(subTextKey),
                Modifier
                    .padding(start = radioButtonWidth)
                    .animateContentSize(),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun BoundedRegionSection(
    contentSize: IntSize,
    isBoundedNearMe: Boolean,
    isBoundedByCoordinates: Boolean,
    regionParameters: BoundedRegionParameters,
    editor: BoundedRegionDataEditor,
    setMovingMap: (Boolean) -> Unit,
    onMovableMap: () -> Unit,
    updateRegionRadius: (Float) -> Unit,
) {
    val t = LocalAppTranslator.current

    val isBoundedRegion = isBoundedNearMe || isBoundedByCoordinates

    val isRegionEditable = isBoundedRegion

    val density = LocalDensity.current
    val mapContentHeightDp = remember(contentSize) {
        val minSize = with(contentSize) {
            width.coerceAtMost(height)
        }
        with(density) {
            minSize.toDp()
        }
    }
    val mapHeightAnimated = if (isBoundedRegion) {
        mapContentHeightDp
    } else {
        mapContentHeightDp.div(3)
    }

    Box(
        Modifier
            .animateContentSize(
                finishedListener = { _, _ ->
                    if (isBoundedRegion) {
                        onMovableMap()
                    }
                },
            )
            .fillMaxWidth()
            .height(mapHeightAnimated),
    ) {
        val mapWidth = if (contentSize.width > 0) {
            val scrollWidth = if (isBoundedRegion && isBoundedByCoordinates) 64.dp else 0.dp
            with(density) { contentSize.width.toDp() } - scrollWidth
        } else {
            0.dp
        }
        MovableMapView(
            editor,
            isEditable = isBoundedRegion,
            isMovable = isBoundedByCoordinates,
            Modifier
                .pointerInteropFilter(
                    onTouchEvent = {
                        when (it.action) {
                            MotionEvent.ACTION_DOWN -> {
                                setMovingMap(isBoundedByCoordinates)
                                false
                            }

                            else -> true
                        }
                    },
                )
                .align(Alignment.CenterEnd)
                .animateContentSize()
                .width(mapWidth),
            onReleaseMapTouch = { setMovingMap(false) },
            circleRadius = if (isBoundedRegion) regionParameters.regionRadiusMiles else 0.0,
        )

        if (!isBoundedRegion) {
            Box(
                Modifier
                    .background(Color.Black.disabledAlpha())
                    .fillMaxSize(),
            )
        }
    }

    Row(
        Modifier.listItemPadding(),
        horizontalArrangement = listItemSpacedByHalf,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val fixedRadius = "%.1f".format(regionParameters.regionRadiusMiles)
        Text(
            t("~~Radius {magnitude} mi.")
                .replace("{magnitude}", fixedRadius),
            Modifier.weight(1f),
        )
        Slider(
            regionParameters.regionRadiusMiles.toFloat(),
            updateRegionRadius,
            Modifier.weight(1f),
            isRegionEditable,
            1.0f..120.0f,
        )
    }
}

@Composable
private fun MovableMapView(
    editor: BoundedRegionDataEditor,
    isEditable: Boolean,
    isMovable: Boolean,
    modifier: Modifier = Modifier,
    onReleaseMapTouch: () -> Unit = {},
    circleRadius: Double = 0.0,
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
    LaunchedEffect(isEditable, isMovable) {
        uiSettings = uiSettings.copy(
            scrollGesturesEnabled = isEditable && isMovable,
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
        if (circleRadius > 0.0) {
            Circle(
                center = coordinates,
                fillColor = primaryOrangeColor.disabledAlpha(),
                radius = circleRadius.milesToMeters,
                strokeColor = primaryOrangeColor,
                strokeWidth = 10f,
            )
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
