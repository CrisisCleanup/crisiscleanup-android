package com.crisiscleanup.feature.caseeditor.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupElevatedIconButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextCheckbox
import com.crisiscleanup.core.designsystem.component.OutlinedClearableTextField
import com.crisiscleanup.core.designsystem.component.mapButtonSize
import com.crisiscleanup.core.designsystem.theme.listCheckboxAlignStartOffset
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.mapmarker.ui.rememberMapProperties
import com.crisiscleanup.core.mapmarker.ui.rememberMapUiSettings
import com.crisiscleanup.core.ui.MapOverlayMessage
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.feature.caseeditor.CaseLocationDataEditor
import com.crisiscleanup.feature.caseeditor.EditCaseBaseViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

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
private fun MapButton(
    imageVector: ImageVector? = null,
    @DrawableRes iconResId: Int = 0,
    @StringRes contentDescriptionResId: Int = 0,
    contentDescription: String = "",
    onClick: () -> Unit = {},
) {
    CrisisCleanupElevatedIconButton(
        modifier = Modifier.size(mapButtonSize),
        imageVector = imageVector,
        iconResId = iconResId,
        contentDescriptionResId = contentDescriptionResId,
        contentDescription = contentDescription,
        onClick = onClick,
    )
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
internal fun LocationFormView(
    editor: CaseLocationDataEditor,
) {
    val translator = LocalAppTranslator.current.translator
    val isEditable = LocalCaseEditor.current.isEditable

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
        label = translator("cross_street"),
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
        text = translator("caseForm.address_problems"),
        onToggle = toggleWrongLocation,
        onCheckChange = updateWrongLocation,
        enabled = isEditable,
    )
}

@Composable
private fun LocationAddressFormView(
    editor: CaseLocationDataEditor,
    closeKeyboard: () -> Unit = {},
    isEditable: Boolean = false,
) {
    val translator = LocalAppTranslator.current.translator
    val inputData = editor.locationInputData

    val updateAddress = remember(inputData) { { s: String -> inputData.streetAddress = s } }
    val clearAddressError = remember(inputData) { { inputData.streetAddressError = "" } }
    val isAddressError = inputData.streetAddressError.isNotEmpty()
    val focusAddress = isAddressError
    val addressLabel = translator("formLabels.address")
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
    val postalCodeLabel = translator("formLabels.postal_code")
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
    val countyLabel = translator("formLabels.county")
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
    val cityLabel = translator("formLabels.city")
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
    val stateLabel = translator("formLabels.state")
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
