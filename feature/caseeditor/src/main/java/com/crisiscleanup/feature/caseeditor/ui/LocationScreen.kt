package com.crisiscleanup.feature.caseeditor.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import com.crisiscleanup.core.designsystem.theme.listCheckboxAlignStartOffset
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.textMessagePadding
import com.crisiscleanup.core.mapmarker.model.DefaultCoordinates
import com.crisiscleanup.core.mapmarker.ui.rememberMapProperties
import com.crisiscleanup.core.mapmarker.ui.rememberMapUiSettings
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.ui.MapOverlayMessage
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.core.ui.touchDownConsumer
import com.crisiscleanup.feature.caseeditor.EditCaseLocationViewModel
import com.crisiscleanup.feature.caseeditor.ExistingWorksiteIdentifier
import com.crisiscleanup.feature.caseeditor.R
import com.crisiscleanup.feature.caseeditor.util.summarizeAddress
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.*

private const val ScreenTitleTranslateKey = "formLabels.location"

@Composable
private fun AddressSummaryInColumn(
    lines: Collection<String>,
    modifier: Modifier = Modifier,
) {
    lines.forEach {
        // TODO Use common styles
        Text(
            it,
            modifier = modifier,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
internal fun LocationSummaryView(
    worksite: Worksite,
    isEditable: Boolean,
    modifier: Modifier = Modifier,
    onEdit: () -> Unit = {},
    translate: (String) -> String = { s -> s },
) {
    EditCaseSummaryHeader(
        0,
        isEditable,
        onEdit,
        modifier,
        header = translate(ScreenTitleTranslateKey),
    ) {
        worksite.run {
            val addressSummaryLines = summarizeAddress(address, postalCode, county, city, state)
            if (addressSummaryLines.isNotEmpty()) {
                AddressSummaryInColumn(addressSummaryLines)
            }

            if (worksite.crossStreetNearbyLandmark.isNotEmpty()) {
                Text(
                    text = worksite.crossStreetNearbyLandmark,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
internal fun EditCaseLocationRoute(
    viewModel: EditCaseLocationViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
    openExistingCase: (ids: ExistingWorksiteIdentifier) -> Unit = { _ -> },
) {
    val editDifferentWorksite by viewModel.editIncidentWorksite.collectAsStateWithLifecycle()
    if (editDifferentWorksite.isDefined) {
        openExistingCase(editDifferentWorksite)
    } else {
        EditCaseLocationView(onBackClick = onBackClick)
    }
}

@Composable
private fun EditCaseLocationView(
    viewModel: EditCaseLocationViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
) {
    EditCaseBackCancelView(
        viewModel,
        onBackClick,
        viewModel.translate(ScreenTitleTranslateKey)
    ) {
        LocationView()
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
                    .listItemPadding(),
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
                val onMapTouched = remember(viewModel) {
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
                    modifier = Modifier.textMessagePadding(),
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
    val inputData = viewModel.locationInputData

    val closeKeyboard = rememberCloseKeyboard(inputData)

    val updateCrossStreet = remember(inputData) {
        { s: String ->
            inputData.crossStreetNearbyLandmark = s
        }
    }
    OutlinedClearableTextField(
        modifier = listItemModifier,
        labelResId = 0,
        label = viewModel.translate("cross_street"),
        value = inputData.crossStreetNearbyLandmark,
        onValueChange = updateCrossStreet,
        keyboardType = KeyboardType.Text,
        isError = false,
        enabled = true,
        imeAction = ImeAction.Next,
    )

    val updateWrongLocation = remember(inputData) {
        { b: Boolean ->
            inputData.hasWrongLocation = b
        }
    }
    val toggleWrongLocation = remember(inputData) {
        {
            updateWrongLocation(!inputData.hasWrongLocation)
        }
    }
    CrisisCleanupTextCheckbox(
        listItemModifier.listCheckboxAlignStartOffset(),
        inputData.hasWrongLocation,
        // TODO Translator from form fields data?
        R.string.wrong_location,
        onToggle = toggleWrongLocation,
        onCheckChange = updateWrongLocation,
    )

    inputData.run {
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
            AddressSummaryInColumn(
                addressSummary,
                listItemModifier,
            )
        }
    }
}

@Composable
internal fun LocationAddressFormView(
    viewModel: EditCaseLocationViewModel = hiltViewModel(),
    closeKeyboard: () -> Unit = {},
) {
    val inputData = viewModel.locationInputData

    val updateAddress = remember(inputData) { { s: String -> inputData.streetAddress = s } }
    val clearAddressError = remember(inputData) { { inputData.streetAddressError = "" } }
    val isAddressError = inputData.streetAddressError.isNotEmpty()
    val focusAddress = isAddressError
    ErrorText(inputData.streetAddressError)
    OutlinedClearableTextField(
        modifier = listItemModifier,
        labelResId = 0,
        label = viewModel.translate("formLabels.address"),
        value = inputData.streetAddress,
        onValueChange = updateAddress,
        keyboardType = KeyboardType.Password,
        isError = isAddressError,
        hasFocus = focusAddress,
        onNext = clearAddressError,
        enabled = true,
    )

    // TODO Move into view model to query for and present menu options.
    val updateZipCode = remember(inputData) { { s: String -> inputData.zipCode = s } }
    val clearZipCodeError = remember(inputData) { { inputData.zipCodeError = "" } }
    val isZipCodeError = inputData.zipCodeError.isNotEmpty()
    val focusZipCode = isZipCodeError
    ErrorText(inputData.zipCodeError)
    OutlinedClearableTextField(
        modifier = listItemModifier,
        labelResId = 0,
        label = viewModel.translate("formLabels.postal_code"),
        value = inputData.zipCode,
        onValueChange = updateZipCode,
        keyboardType = KeyboardType.Password,
        isError = isZipCodeError,
        hasFocus = focusZipCode,
        onNext = clearZipCodeError,
        enabled = true,
    )

    val updateCounty = remember(inputData) { { s: String -> inputData.county = s } }
    val clearCountyError = remember(inputData) { { inputData.countyError = "" } }
    val isCountyError = inputData.countyError.isNotEmpty()
    val focusCounty = isCountyError
    ErrorText(inputData.countyError)
    OutlinedClearableTextField(
        modifier = listItemModifier,
        labelResId = 0,
        label = viewModel.translate("formLabels.county"),
        value = inputData.county,
        onValueChange = updateCounty,
        keyboardType = KeyboardType.Password,
        isError = isCountyError,
        hasFocus = focusCounty,
        onNext = clearCountyError,
        enabled = true,
    )

    val updateCity = remember(inputData) { { s: String -> inputData.city = s } }
    val clearCityError = remember(inputData) { { inputData.cityError = "" } }
    val isCityError = inputData.cityError.isNotEmpty()
    val focusCity = isCityError
    ErrorText(inputData.cityError)
    OutlinedClearableTextField(
        modifier = listItemModifier,
        labelResId = 0,
        label = viewModel.translate("formLabels.city"),
        value = inputData.city,
        onValueChange = updateCity,
        keyboardType = KeyboardType.Password,
        isError = isCityError,
        hasFocus = focusCity,
        onNext = clearCityError,
        enabled = true,
    )

    val updateState = remember(inputData) { { s: String -> inputData.state = s } }
    val onStateEnd = remember(inputData) {
        {
            inputData.stateError = ""
            closeKeyboard()
        }
    }
    val isStateError = inputData.stateError.isNotEmpty()
    val focusState = isStateError
    ErrorText(inputData.stateError)
    OutlinedClearableTextField(
        modifier = listItemModifier,
        labelResId = 0,
        label = viewModel.translate("formLabels.state"),
        value = inputData.state,
        onValueChange = updateState,
        keyboardType = KeyboardType.Password,
        isError = isStateError,
        hasFocus = focusState,
        imeAction = ImeAction.Done,
        onEnter = onStateEnd,
        enabled = true,
    )
}
