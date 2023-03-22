package com.crisiscleanup.feature.caseeditor.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.component.*
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.mapmarker.model.DefaultCoordinates
import com.crisiscleanup.core.mapmarker.ui.rememberMapProperties
import com.crisiscleanup.core.mapmarker.ui.rememberMapUiSettings
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.ui.MapOverlayMessage
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.core.ui.touchDownConsumer
import com.crisiscleanup.feature.caseeditor.EditCaseLocationViewModel
import com.crisiscleanup.feature.caseeditor.R
import com.crisiscleanup.feature.caseeditor.util.summarizeAddress
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.*

@Composable
private fun AddressSummaryInColumn(
    lines: Collection<String>,
) {
    lines.forEach {
        // TODO Use common styles
        Text(
            it,
            modifier = columnItemModifier,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

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

        worksite.run {
            val addressSummaryLines = summarizeAddress(address, postalCode, county, city, state)
            if (addressSummaryLines.isNotEmpty()) {
                // TODO Common style for padding
                Column(modifier.padding(8.dp)) {
                    AddressSummaryInColumn(addressSummaryLines)

                    if (worksite.crossStreetNearbyLandmark.isNotEmpty()) {
                        Text(
                            text = worksite.crossStreetNearbyLandmark,
                            modifier = modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
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
            // TODO This seems to be recomposing when map is moved.
            //      Is it possible to not recompose due to surrounding views?
            TopAppBarBackCancel(
                titleResId = R.string.location,
                onBack = onNavigateBack,
                onCancel = onNavigateCancel,
            )
            // TODO Remove the gap between the top bar above and location view below
            LocationView()
        }
    }

    val closePermissionDialog =
        remember(viewModel) { { viewModel.showExplainPermissionLocation.value = false } }
    val explainPermission by viewModel.showExplainPermissionLocation
    ExplainLocationPermissionDialog(
        showDialog = explainPermission,
        closeDialog = closePermissionDialog,
    )
}

@Composable
private fun getLayoutParameters(isMoveLocationMode: Boolean): Pair<Boolean, Modifier> {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val minScreenDimension = min(screenWidth, screenHeight)
    // TODO Revisit for all screen sizes. Adjust map size as necessary
    val isRowOriented = screenWidth > screenHeight.times(1.3f)

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

    return Pair(isRowOriented, mapModifier)
}

@Composable
internal fun ColumnScope.LocationView(
    viewModel: EditCaseLocationViewModel = hiltViewModel(),
) {
    val isMoveLocationMode by viewModel.isMoveLocationOnMapMode

    val cameraPositionState = rememberCameraPositionState("edit-location") {
        position = CameraPosition.fromLatLngZoom(
            DefaultCoordinates,
            viewModel.defaultMapZoom,
        )
    }

    val (isRowOriented, mapModifier) = getLayoutParameters(isMoveLocationMode)

    if (isMoveLocationMode) {
        LocationMapContainerView(
            mapModifier,
            cameraPositionState,
            true,
        )
    } else {
        var enableColumnScroll by remember { mutableStateOf(true) }
        LaunchedEffect(cameraPositionState.isMoving) {
            if (!cameraPositionState.isMoving) {
                enableColumnScroll = true
            }
        }

        val locationQuery by viewModel.locationInputData.locationQuery.collectAsStateWithLifecycle()
        val query = locationQuery.trim()
        val showMapFormViews = query.isEmpty()

        val scrollState = rememberScrollState()
        val closeKeyboard = rememberCloseKeyboard(viewModel)
        Column(
            Modifier
                .scrollFlingListener(closeKeyboard)
                .verticalScroll(
                    scrollState,
                    showMapFormViews && enableColumnScroll,
                )
                .weight(1f)
        ) {
            val updateLocation = remember(viewModel) { { s: String -> viewModel.onQueryChange(s) } }
            OutlinedClearableTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                labelResId = R.string.location_address_search,
                value = locationQuery,
                onValueChange = updateLocation,
                keyboardType = KeyboardType.Password,
                isError = false,
                enabled = true,
            )

            if (viewModel.takeClearSearchInputFocus) {
                LocalFocusManager.current.clearFocus(true)
            }

            val isShortQuery by viewModel.isShortQuery.collectAsStateWithLifecycle()
            if (showMapFormViews) {
                val onMapTouched =
                    remember(viewModel) {
                        {
                            enableColumnScroll = false
                            closeKeyboard()
                        }
                    }
                if (isRowOriented) {
                    Row {
                        LocationMapContainerView(
                            mapModifier,
                            cameraPositionState,
                            onMapTouched = onMapTouched,
                        )
                        Column {
                            LocationFormView()
                        }
                    }
                } else {
                    LocationMapContainerView(
                        mapModifier,
                        cameraPositionState,
                        onMapTouched = onMapTouched,
                    )
                    LocationFormView()
                }
            } else if (isShortQuery) {
                Text(
                    stringResource(R.string.location_query_hint),
                    // TODO Use common styles
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                SearchContents(query)
            }
        }
    }
}

@Composable
internal fun MapButton(
    imageVector: ImageVector? = null,
    @DrawableRes iconResId: Int = 0,
    @StringRes contentDescriptionResId: Int = 0,
    onClick: () -> Unit = {},
) {
    CrisisCleanupIconButton(
        modifier = Modifier.size(mapButtonSize),
        imageVector = imageVector,
        iconResId = iconResId,
        contentDescriptionResId = contentDescriptionResId,
        onClick = onClick,
    )
}

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
                top.linkTo(parent.top, margin = mapButtonEdgeSpace)
                end.linkTo(parent.end, margin = mapButtonEdgeSpace)
            },
            horizontalArrangement = Arrangement.spacedBy(mapButtonSpace),
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
internal fun BoxScope.LocationMapView(
    modifier: Modifier = Modifier,
    viewModel: EditCaseLocationViewModel = hiltViewModel(),
    cameraPositionState: CameraPositionState = rememberCameraPositionState(),
) {
    val onMapLoaded = remember(viewModel) { { viewModel.onMapLoaded() } }
    val onMapCameraChange = remember(viewModel) {
        { position: CameraPosition,
          projection: Projection?,
          isUserInteraction: Boolean ->
            viewModel.onMapCameraChange(position, projection, isUserInteraction)
        }
    }

    val mapCameraZoom by viewModel.mapCameraZoom.collectAsStateWithLifecycle()

    val uiSettings by rememberMapUiSettings()

    val markerState = rememberMarkerState()
    val coordinates by viewModel.locationInputData.coordinates.collectAsStateWithLifecycle()
    markerState.position = coordinates

    val mapMarkerIcon by viewModel.mapMarkerIcon.collectAsStateWithLifecycle()

    val mapProperties by rememberMapProperties()
    GoogleMap(
        modifier = modifier,
        uiSettings = uiSettings,
        properties = mapProperties,
        cameraPositionState = cameraPositionState,
        onMapLoaded = onMapLoaded,
    ) {
        Marker(
            markerState,
            icon = mapMarkerIcon,
        )
    }

    val outOfBoundsMessage by viewModel.locationOutOfBoundsMessage.collectAsStateWithLifecycle()
    MapOverlayMessage(outOfBoundsMessage)

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
    cameraPositionState: CameraPositionState = rememberCameraPositionState(),
    isMoveLocationMode: Boolean = false,
    onMapTouched: () -> Unit = {},
) {
    Box(modifier.touchDownConsumer(onMapTouched)) {
        LocationMapView(cameraPositionState = cameraPositionState)
        LocationMapActions(isMoveLocationMode)
    }
}

@Composable
internal fun LocationFormView(
    viewModel: EditCaseLocationViewModel = hiltViewModel(),
) {
    val locationInputData = viewModel.locationInputData

    val closeKeyboard = rememberCloseKeyboard(locationInputData)

    val updateCrossStreet =
        remember(locationInputData) {
            { s: String ->
                locationInputData.crossStreetNearbyLandmark = s
            }
        }
    OutlinedClearableTextField(
        modifier = columnItemModifier,
        labelResId = R.string.cross_street_nearby_landmark,
        value = locationInputData.crossStreetNearbyLandmark,
        onValueChange = updateCrossStreet,
        keyboardType = KeyboardType.Text,
        isError = false,
        enabled = true,
        imeAction = ImeAction.Next,
    )

    val updateWrongLocation = remember(locationInputData) {
        { b: Boolean ->
            locationInputData.hasWrongLocation = b
        }
    }
    val toggleWrongLocation = remember(locationInputData) {
        {
            updateWrongLocation(!locationInputData.hasWrongLocation)
        }
    }
    Row(
        Modifier
            .clickable(onClick = toggleWrongLocation)
            .then(columnItemModifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = locationInputData.hasWrongLocation,
            onCheckedChange = updateWrongLocation,
        )
        // TODO Translator from form fields data?
        Text(text = stringResource(R.string.wrong_location))
    }

    locationInputData.run {
        val showAddressForm by remember(
            streetAddressError,
            zipCodeError,
            cityError,
            countyError,
            stateError,
            hasWrongLocation,
        ) {
            derivedStateOf { hasAddressError || hasWrongLocation }
        }
        if (showAddressForm) {
            LocationAddressFormView(closeKeyboard = closeKeyboard)
        } else {
            AddressSummaryInColumn(addressSummary)
        }
    }
}

@Composable
internal fun LocationAddressFormView(
    viewModel: EditCaseLocationViewModel = hiltViewModel(),
    closeKeyboard: () -> Unit = {},
) {
    val locationInputData = viewModel.locationInputData

    val updateAddress =
        remember(locationInputData) { { s: String -> locationInputData.streetAddress = s } }
    val clearAddressError =
        remember(locationInputData) { { locationInputData.streetAddressError = "" } }
    val isAddressError = locationInputData.streetAddressError.isNotEmpty()
    val focusAddress = isAddressError
    ErrorText(locationInputData.streetAddressError)
    OutlinedClearableTextField(
        modifier = columnItemModifier,
        labelResId = R.string.street_address,
        value = locationInputData.streetAddress,
        onValueChange = updateAddress,
        keyboardType = KeyboardType.Password,
        isError = isAddressError,
        hasFocus = focusAddress,
        onNext = clearAddressError,
        enabled = true,
    )

    // TODO Move into view model to query for and present autofill.
    //      See ExposedDropdownMenu for presenting options.
    val updateZipCode =
        remember(locationInputData) { { s: String -> locationInputData.zipCode = s } }
    val clearZipCodeError =
        remember(locationInputData) { { locationInputData.zipCodeError = "" } }
    val isZipCodeError = locationInputData.zipCodeError.isNotEmpty()
    val focusZipCode = isZipCodeError
    ErrorText(locationInputData.zipCodeError)
    OutlinedClearableTextField(
        modifier = columnItemModifier,
        labelResId = R.string.zipcode,
        value = locationInputData.zipCode,
        onValueChange = updateZipCode,
        keyboardType = KeyboardType.Password,
        isError = isZipCodeError,
        hasFocus = focusZipCode,
        onNext = clearZipCodeError,
        enabled = true,
    )

    val updateCounty =
        remember(locationInputData) { { s: String -> locationInputData.county = s } }
    val clearCountyError =
        remember(locationInputData) { { locationInputData.countyError = "" } }
    val isCountyError = locationInputData.countyError.isNotEmpty()
    val focusCounty = isCountyError
    ErrorText(locationInputData.countyError)
    OutlinedClearableTextField(
        modifier = columnItemModifier,
        labelResId = R.string.county,
        value = locationInputData.county,
        onValueChange = updateCounty,
        keyboardType = KeyboardType.Password,
        isError = isCountyError,
        hasFocus = focusCounty,
        onNext = clearCountyError,
        enabled = true,
    )

    val updateCity =
        remember(locationInputData) { { s: String -> locationInputData.city = s } }
    val clearCityError =
        remember(locationInputData) { { locationInputData.cityError = "" } }
    val isCityError = locationInputData.cityError.isNotEmpty()
    val focusCity = isCityError
    ErrorText(locationInputData.cityError)
    OutlinedClearableTextField(
        modifier = columnItemModifier,
        labelResId = R.string.city,
        value = locationInputData.city,
        onValueChange = updateCity,
        keyboardType = KeyboardType.Password,
        isError = isCityError,
        hasFocus = focusCity,
        onNext = clearCityError,
        enabled = true,
    )

    val updateState =
        remember(locationInputData) { { s: String -> locationInputData.state = s } }
    val clearStateError =
        remember(locationInputData) { { locationInputData.stateError = "" } }
    val isStateError = locationInputData.stateError.isNotEmpty()
    val focusState = isStateError
    ErrorText(locationInputData.stateError)
    OutlinedClearableTextField(
        modifier = columnItemModifier,
        labelResId = R.string.state,
        value = locationInputData.state,
        onValueChange = updateState,
        keyboardType = KeyboardType.Password,
        isError = isStateError,
        hasFocus = focusState,
        imeAction = ImeAction.Done,
        onEnter = closeKeyboard,
        enabled = true,
    )
}
