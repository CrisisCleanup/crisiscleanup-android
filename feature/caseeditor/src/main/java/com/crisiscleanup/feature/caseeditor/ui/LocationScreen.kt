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
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
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
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.core.ui.touchDownConsumer
import com.crisiscleanup.feature.caseeditor.*
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
    val editor = viewModel.editor

    val editDifferentWorksite by editor.editIncidentWorksite.collectAsStateWithLifecycle()
    if (editDifferentWorksite.isDefined) {
        openExistingCase(editDifferentWorksite)
    } else {
        val translate = remember(viewModel) { { s: String -> viewModel.translate(s) } }
        EditCaseLocationView(
            viewModel,
            editor,
            onBackClick = onBackClick,
            translate = translate,
        )
    }
}

@Composable
private fun EditCaseLocationView(
    viewModel: EditCaseBaseViewModel,
    editor: CaseLocationDataEditor,
    onBackClick: () -> Unit = {},
    translate: (String) -> String = { s -> s },
) {
    EditCaseBackCancelView(
        viewModel,
        onBackClick,
        translate(ScreenTitleTranslateKey),
    ) {
        LocationView(viewModel, editor, translate)
    }

    // TODO Handle out of bounds properly

    val closePermissionDialog =
        remember(viewModel) { { editor.showExplainPermissionLocation.value = false } }
    val explainPermission by editor.showExplainPermissionLocation
    ExplainLocationPermissionDialog(
        showDialog = explainPermission,
        closeDialog = closePermissionDialog,
    )
}

@Composable
internal fun getLayoutParameters(isMoveLocationMode: Boolean): Pair<Boolean, Modifier> {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val minScreenDimension = screenWidth.coerceAtMost(screenHeight)
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
    viewModel: EditCaseBaseViewModel,
    editor: CaseLocationDataEditor,
    translate: (String) -> String = { s -> s },
) {
    val isMoveLocationMode by editor.isMoveLocationOnMapMode

    val cameraPositionState = rememberCameraPositionState("edit-location") {
        position = CameraPosition.fromLatLngZoom(
            DefaultCoordinates,
            editor.defaultMapZoom,
        )
    }

    val (_, mapModifier) = getLayoutParameters(isMoveLocationMode)

    if (isMoveLocationMode) {
        LocationMapContainerView(
            viewModel,
            editor,
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

        val locationQuery by editor.locationInputData.locationQuery.collectAsStateWithLifecycle()
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
            val updateLocation = remember(viewModel) { { s: String -> editor.onQueryChange(s) } }
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

            if (editor.takeClearSearchInputFocus) {
                LocalFocusManager.current.clearFocus(true)
            }

            val isShortQuery by editor.isShortQuery.collectAsStateWithLifecycle()
            if (showMapFormViews) {
                val onMapTouched = remember(viewModel) {
                    {
                        enableColumnScroll = false
                        closeKeyboard()
                    }
                }
                LocationMapContainerView(
                    viewModel,
                    editor,
                    mapModifier,
                    cameraPositionState,
                    onMapTouched = onMapTouched,
                )
                LocationFormView(editor, translate = translate)
            } else if (isShortQuery) {
                Text(
                    stringResource(R.string.location_query_hint),
                    modifier = Modifier.textMessagePadding(),
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                SearchContents(
                    viewModel,
                    editor,
                    query,
                )
            }
        }
    }
}

@Composable
internal fun MapButton(
    imageVector: ImageVector? = null,
    @DrawableRes iconResId: Int = 0,
    @StringRes contentDescriptionResId: Int = 0,
    contentDescription: String = "",
    onClick: () -> Unit = {},
) {
    CrisisCleanupIconButton(
        modifier = Modifier.size(mapButtonSize),
        imageVector = imageVector,
        iconResId = iconResId,
        contentDescriptionResId = contentDescriptionResId,
        contentDescription = contentDescription,
        onClick = onClick,
    )
}

@Composable
internal fun LocationMapActions(
    viewModel: EditCaseBaseViewModel,
    editor: CaseLocationDataEditor,
    isMoveLocationMode: Boolean,
    translate: (String) -> String = { s -> s },
) {
    val useMyLocation = remember(viewModel) { { editor.useMyLocation() } }
    val moveLocationOnMap = remember(viewModel) { { editor.toggleMoveLocationOnMap() } }
    val centerOnLocation = remember(viewModel) { { editor.centerOnLocation() } }

    ConstraintLayout(Modifier.fillMaxSize()) {
        val (actionBar) = createRefs()
        Row(
            Modifier.constrainAs(actionBar) {
                top.linkTo(parent.top, margin = mapButtonEdgeSpace)
                end.linkTo(parent.end, margin = mapButtonEdgeSpace)
            },
            horizontalArrangement = Arrangement.spacedBy(adjacentButtonSpace),
        ) {
            MapButton(
                iconResId = R.drawable.ic_move_location,
                contentDescription = translate("caseForm.select_on_map"),
                onClick = moveLocationOnMap,
            )
            if (!isMoveLocationMode) {
                // TODO Likely hint with translator
                MapButton(
                    imageVector = CrisisCleanupIcons.Location,
                    contentDescriptionResId = R.string.center_on_location,
                    onClick = centerOnLocation,
                )
                MapButton(
                    imageVector = CrisisCleanupIcons.MyLocation,
                    contentDescription = translate("caseForm.use_my_location"),
                    onClick = useMyLocation,
                )
            }
        }
    }
}

@Composable
internal fun BoxScope.LocationMapView(
    viewModel: EditCaseBaseViewModel,
    editor: CaseLocationDataEditor,
    modifier: Modifier = Modifier,
    zoomControls: Boolean = false,
    disablePanning: Boolean = false,
    cameraPositionState: CameraPositionState = rememberCameraPositionState(),
) {
    val onMapLoaded = remember(viewModel) { { editor.onMapLoaded() } }
    val onMapCameraChange = remember(viewModel) {
        { position: CameraPosition,
          projection: Projection?,
          isUserInteraction: Boolean ->
            editor.onMapCameraChange(position, projection, isUserInteraction)
        }
    }

    val mapCameraZoom by editor.mapCameraZoom.collectAsStateWithLifecycle()

    val uiSettings by rememberMapUiSettings(
        zoomControls = zoomControls,
        disablePanning = disablePanning,
    )

    val markerState = rememberMarkerState()
    val coordinates by editor.locationInputData.coordinates.collectAsStateWithLifecycle()
    markerState.position = coordinates

    val mapMarkerIcon by editor.mapMarkerIcon.collectAsStateWithLifecycle()

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

    val outOfBoundsMessage by editor.locationOutOfBoundsMessage.collectAsStateWithLifecycle()
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

@Composable
internal fun LocationMapContainerView(
    viewModel: EditCaseBaseViewModel,
    editor: CaseLocationDataEditor,
    modifier: Modifier = Modifier,
    cameraPositionState: CameraPositionState = rememberCameraPositionState(),
    isMoveLocationMode: Boolean = false,
    onMapTouched: () -> Unit = {},
) {
    Box(modifier.touchDownConsumer(onMapTouched)) {
        LocationMapView(
            viewModel,
            editor,
            cameraPositionState = cameraPositionState,
        )
        LocationMapActions(
            viewModel,
            editor,
            isMoveLocationMode,
        )
    }
}

@Composable
internal fun LocationFormView(
    editor: CaseLocationDataEditor,
    isEditable: Boolean = false,
    translate: (String) -> String = { s -> s },
) {
    val inputData = editor.locationInputData

    val closeKeyboard = rememberCloseKeyboard(inputData)

    val updateCrossStreet = remember(inputData) {
        { s: String ->
            inputData.crossStreetNearbyLandmark = s
        }
    }
    OutlinedClearableTextField(
        modifier = listItemModifier,
        labelResId = 0,
        label = translate("cross_street"),
        value = inputData.crossStreetNearbyLandmark,
        onValueChange = updateCrossStreet,
        keyboardType = KeyboardType.Text,
        isError = false,
        enabled = isEditable,
        imeAction = ImeAction.Next,
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
            LocationAddressFormView(
                editor,
                closeKeyboard,
                isEditable,
                translate,
            )
        } else {
            AddressSummaryInColumn(
                addressSummary,
                listItemModifier,
            )
        }
    }

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
        text = translate("caseForm.address_problems"),
        onToggle = toggleWrongLocation,
        onCheckChange = updateWrongLocation,
        enabled = isEditable,
    )
}

@Composable
internal fun LocationAddressFormView(
    editor: CaseLocationDataEditor,
    closeKeyboard: () -> Unit = {},
    isEditable: Boolean = false,
    translate: (String) -> String = { s -> s },
) {
    val inputData = editor.locationInputData

    val updateAddress = remember(inputData) { { s: String -> inputData.streetAddress = s } }
    val clearAddressError = remember(inputData) { { inputData.streetAddressError = "" } }
    val isAddressError = inputData.streetAddressError.isNotEmpty()
    val focusAddress = isAddressError
    val addressLabel = translate("formLabels.address")
    ErrorText(inputData.streetAddressError)
    OutlinedClearableTextField(
        modifier = listItemModifier,
        labelResId = 0,
        label = "$addressLabel *",
        value = inputData.streetAddress,
        onValueChange = updateAddress,
        keyboardType = KeyboardType.Password,
        isError = isAddressError,
        hasFocus = focusAddress,
        onNext = clearAddressError,
        enabled = isEditable,
    )

    // TODO Move into view model to query for and present menu options.
    val updateZipCode = remember(inputData) { { s: String -> inputData.zipCode = s } }
    val clearZipCodeError = remember(inputData) { { inputData.zipCodeError = "" } }
    val isZipCodeError = inputData.zipCodeError.isNotEmpty()
    val focusZipCode = isZipCodeError
    val postalCodeLabel = translate("formLabels.postal_code")
    ErrorText(inputData.zipCodeError)
    OutlinedClearableTextField(
        modifier = listItemModifier,
        labelResId = 0,
        label = "$postalCodeLabel *",
        value = inputData.zipCode,
        onValueChange = updateZipCode,
        keyboardType = KeyboardType.Password,
        isError = isZipCodeError,
        hasFocus = focusZipCode,
        onNext = clearZipCodeError,
        enabled = isEditable,
    )

    val updateCounty = remember(inputData) { { s: String -> inputData.county = s } }
    val clearCountyError = remember(inputData) { { inputData.countyError = "" } }
    val isCountyError = inputData.countyError.isNotEmpty()
    val focusCounty = isCountyError
    val countyLabel = translate("formLabels.county")
    ErrorText(inputData.countyError)
    OutlinedClearableTextField(
        modifier = listItemModifier,
        labelResId = 0,
        label = "$countyLabel *",
        value = inputData.county,
        onValueChange = updateCounty,
        keyboardType = KeyboardType.Password,
        isError = isCountyError,
        hasFocus = focusCounty,
        onNext = clearCountyError,
        enabled = isEditable,
    )

    val updateCity = remember(inputData) { { s: String -> inputData.city = s } }
    val clearCityError = remember(inputData) { { inputData.cityError = "" } }
    val isCityError = inputData.cityError.isNotEmpty()
    val focusCity = isCityError
    val cityLabel = translate("formLabels.city")
    ErrorText(inputData.cityError)
    OutlinedClearableTextField(
        modifier = listItemModifier,
        labelResId = 0,
        label = "$cityLabel *",
        value = inputData.city,
        onValueChange = updateCity,
        keyboardType = KeyboardType.Password,
        isError = isCityError,
        hasFocus = focusCity,
        onNext = clearCityError,
        enabled = isEditable,
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
    val stateLabel = translate("formLabels.state")
    ErrorText(inputData.stateError)
    OutlinedClearableTextField(
        modifier = listItemModifier,
        labelResId = 0,
        label = "$stateLabel *",
        value = inputData.state,
        onValueChange = updateState,
        keyboardType = KeyboardType.Password,
        isError = isStateError,
        hasFocus = focusState,
        imeAction = ImeAction.Done,
        onEnter = onStateEnd,
        enabled = isEditable,
    )
}
