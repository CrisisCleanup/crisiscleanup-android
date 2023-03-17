package com.crisiscleanup.feature.caseeditor.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.component.CrisisCleanupIconButton
import com.crisiscleanup.core.designsystem.component.OutlinedClearableTextField
import com.crisiscleanup.core.designsystem.component.TopAppBarBackCancel
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.mapmarker.model.MapViewCameraBounds
import com.crisiscleanup.core.mapmarker.model.MapViewCameraBoundsDefault
import com.crisiscleanup.core.mapmarker.ui.rememberMapProperties
import com.crisiscleanup.core.mapmarker.ui.rememberMapUiSettings
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.feature.caseeditor.EditCaseLocationViewModel
import com.crisiscleanup.feature.caseeditor.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.*

@Composable
internal fun LocationSummaryView(
    worksite: Worksite,
    modifier: Modifier = Modifier,
    onEdit: () -> Unit = {},
) {
    Column(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.location),
            style = MaterialTheme.typography.headlineSmall,
        )

//        if (worksite..isNotEmpty()) {
//            Column(modifier.padding(8.dp)) {
//                Text(
//                    text = worksite.,
//                    modifier = modifier.fillMaxWidth(),
//                    style = MaterialTheme.typography.bodyLarge,
//                )
//            }
//        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditCaseLocationRoute(
    viewModel: EditCaseLocationViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
) {
    BackHandler {
        if (viewModel.onSystemBack()) {
            onBackClick()
        }
    }

    val navigateBack by remember { viewModel.navigateBack }
    if (navigateBack) {
        onBackClick()
    } else {
        val onNavigateBack = remember(viewModel) {
            {
                if (viewModel.onNavigateBack()) {
                    onBackClick()
                }
            }
        }
        val onNavigateCancel = remember(viewModel) {
            {
                if (viewModel.onNavigateCancel()) {
                    onBackClick()
                }
            }
        }
        Column {
            TopAppBarBackCancel(
                titleResId = R.string.location,
                onBack = onNavigateBack,
                onCancel = onNavigateCancel,
            )
            // TODO There is gap between the top bar and the location views
            LocationView()
        }
    }

    val closeDialog =
        remember(viewModel) { { viewModel.showExplainPermissionLocation.value = false } }
    val explainPermission by viewModel.showExplainPermissionLocation
    ExplainLocationPermissionDialog(
        showDialog = explainPermission,
        closeDialog = closeDialog,
    )
}

@Composable
internal fun ColumnScope.LocationView(
    viewModel: EditCaseLocationViewModel = hiltViewModel(),
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val minScreenDimension = min(screenWidth, screenHeight)
    // TODO Revisit for all screen sizes. Adjust map size as necessary
    val isRowOriented = screenWidth > screenHeight.times(1.3f)

    val isMoveLocationMode by viewModel.isMoveLocationOnMapMode

    val mapWidth: Dp
    val mapHeight: Dp
    if (isMoveLocationMode) {
        mapWidth = screenWidth
        mapHeight = screenHeight
    } else {
        mapWidth = minScreenDimension
        mapHeight = minScreenDimension
    }
    val mapModifier = Modifier.sizeIn(maxWidth = mapWidth, maxHeight = mapHeight)

    val locationInputData = viewModel.locationInputData

    val updateLocation = remember(viewModel) { { s: String -> viewModel.onQueryChange(s) } }
    if (isMoveLocationMode) {
        LocationMapContainerView(mapModifier, true)
    } else {
        val locationQuery by locationInputData.locationQuery.collectAsStateWithLifecycle()
        OutlinedClearableTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            labelResId = R.string.location_address_search,
            value = locationQuery,
            onValueChange = updateLocation,
            keyboardType = KeyboardType.Text,
            isError = false,
            enabled = true,
        )
        if (isRowOriented) {
            Row {
                LocationMapContainerView(mapModifier)
                Column {
                    LocationFormView()
                }
            }
        } else {
            LocationMapContainerView(mapModifier)
            LocationFormView()
        }
    }
}

// TODO Use common styles
private val buttonSizeModifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)

@Composable
internal fun MapButton(
    imageVector: ImageVector? = null,
    @DrawableRes iconResId: Int = 0,
    @StringRes contentDescriptionResId: Int = 0,
    onClick: () -> Unit = {},
) {
    CrisisCleanupIconButton(
        modifier = buttonSizeModifier,
        imageVector = imageVector,
        iconResId = iconResId,
        contentDescriptionResId = contentDescriptionResId,
        onClick = onClick,
    )
}

// TODO Move into common
private val actionSpacing = 8.dp

@Composable
internal fun LocationMapActions(
    isMoveLocationMode: Boolean,
    viewModel: EditCaseLocationViewModel = hiltViewModel(),
) {
    val useMyLocation = remember(viewModel) { { viewModel.useMyLocation() } }
    val moveLocationOnMap = remember(viewModel) { { viewModel.toggleMoveLocationOnMap() } }
    val centerOnLocation = remember(viewModel) { { viewModel.centerOnLocation() } }

    ConstraintLayout(Modifier.fillMaxSize()) {
        val (actionBar) = createRefs()
        Row(
            Modifier.constrainAs(actionBar) {
                top.linkTo(parent.top, margin = actionSpacing)
                end.linkTo(parent.end, margin = actionSpacing)
            },
            // TODO Move value into common
            horizontalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            // TODO Likely hint with translator
            MapButton(
                iconResId = R.drawable.ic_move_location,
                contentDescriptionResId = R.string.select_on_map,
                onClick = moveLocationOnMap,
            )
            if (!isMoveLocationMode) {
                MapButton(
                    imageVector = CrisisCleanupIcons.Location,
                    contentDescriptionResId = R.string.center_on_location,
                    onClick = centerOnLocation,
                )
                MapButton(
                    imageVector = CrisisCleanupIcons.MyLocation,
                    contentDescriptionResId = R.string.use_my_location,
                    onClick = useMyLocation,
                )
            }
        }
    }
}

@Composable
internal fun LocationMapView(
    modifier: Modifier = Modifier,
    viewModel: EditCaseLocationViewModel = hiltViewModel(),
    mapCameraBounds: MapViewCameraBounds = MapViewCameraBoundsDefault,
) {
    val onMapCameraChange = remember(viewModel) {
        { position: CameraPosition,
          projection: Projection?,
          isUserInteraction: Boolean ->
            viewModel.onMapCameraChange(position, projection, isUserInteraction)
        }
    }

    val mapCameraZoom by viewModel.mapCameraZoom.collectAsStateWithLifecycle()

    val uiSettings by rememberMapUiSettings(myLocation = true)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            mapCameraBounds.bounds.center,
            mapCameraZoom.zoom,
        )
    }

    val markerState = rememberMarkerState()
    val coordinates by viewModel.locationInputData.coordinates.collectAsStateWithLifecycle()
    markerState.position = coordinates

    val mapMarkerIcon by viewModel.mapMarkerIcon

    val mapProperties by rememberMapProperties()
    GoogleMap(
        modifier = modifier,
        uiSettings = uiSettings,
        properties = mapProperties,
        cameraPositionState = cameraPositionState,
    ) {
        Marker(
            markerState,
            icon = mapMarkerIcon,
        )
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
internal fun LocationMapContainerView(
    modifier: Modifier = Modifier,
    isMoveLocationMode: Boolean = false,
) {
    Box(modifier = modifier) {
        LocationMapView()
        LocationMapActions(isMoveLocationMode)
    }
}

@Composable
internal fun ColumnScope.LocationFormView(
    modifier: Modifier = Modifier,
    viewModel: EditCaseLocationViewModel = hiltViewModel(),
    isMoveLocationMode: Boolean = false,
) {
    Text("Location form input")
}
