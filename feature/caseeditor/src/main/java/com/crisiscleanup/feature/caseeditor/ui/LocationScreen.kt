package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupIconButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextCheckbox
import com.crisiscleanup.core.designsystem.component.OutlinedClearableTextField
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.listCheckboxAlignStartOffset
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.unfocusedBorderColor
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
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState

@Composable
private fun AddressSummaryInColumn(
    lines: Collection<String>,
    modifier: Modifier = Modifier,
    onClearAddress: () -> Unit = {},
    onEditAddress: () -> Unit = {},
) {
    if (lines.isNotEmpty()) {
        val strokeWidth: Float
        val cornerRadius: Float
        with(LocalDensity.current) {
            // TODO Common dimensions
            strokeWidth = 1.dp.toPx()
            cornerRadius = 4.dp.toPx()
        }
        val borderStroke = Stroke(width = strokeWidth)
        Surface(
            modifier = modifier
                .drawBehind {
                    drawRoundRect(
                        color = unfocusedBorderColor,
                        cornerRadius = CornerRadius(cornerRadius),
                        style = borderStroke,
                    )
                },
            color = Color.Transparent,
            shape = RoundedCornerShape(cornerRadius),
        ) {
            Box(Modifier.fillMaxWidth()) {
                Text(
                    lines.joinToString("\n"),
                    modifier = Modifier
                        .background(Color.Transparent)
                        .fillMaxWidth()
                        .padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
                val isEditable = LocalCaseEditor.current.isEditable
                Row(listItemModifier.align(Alignment.BottomEnd)) {
                    Spacer(Modifier.weight(1f))
                    CrisisCleanupIconButton(
                        imageVector = CrisisCleanupIcons.Delete,
                        onClick = onClearAddress,
                        enabled = isEditable,
                        modifier = Modifier.testTag("locationClearAddressBtn"),
                    )
                    CrisisCleanupIconButton(
                        imageVector = CrisisCleanupIcons.Edit,
                        onClick = onEditAddress,
                        enabled = isEditable,
                        modifier = Modifier.testTag("locationEditAddressBtn"),
                    )
                }
            }
        }
    }
}

@Composable
internal fun BoxScope.LocationMapView(
    viewModel: EditCaseBaseViewModel,
    editor: CaseLocationDataEditor,
    isSatelliteView: Boolean,
    modifier: Modifier = Modifier,
    zoomControls: Boolean = false,
    disablePanning: Boolean = false,
    cameraPositionState: CameraPositionState = rememberCameraPositionState(),
) {
    val onMapLoaded = remember(viewModel) { { editor.onMapLoaded() } }
    val onMapCameraChange = remember(viewModel) {
        {
                position: CameraPosition,
                projection: Projection?,
                isUserInteraction: Boolean,
            ->
            editor.onMapCameraChange(position, projection, isUserInteraction)
        }
    }

    val mapCameraZoom by editor.mapCameraZoom.collectAsStateWithLifecycle()

    val uiSettings by rememberMapUiSettings(
        zoomControls = zoomControls,
        disablePanning = disablePanning,
    )

    val coordinates by editor.locationInputData.coordinates.collectAsStateWithLifecycle()
    val markerState = rememberUpdatedMarkerState(coordinates)

    val mapMarkerIcon by editor.mapMarkerIcon.collectAsStateWithLifecycle()

    var mapProperties by rememberMapProperties()
    LaunchedEffect(isSatelliteView) {
        val mapType = if (isSatelliteView) MapType.SATELLITE else MapType.NORMAL
        mapProperties = mapProperties.copy(mapType = mapType)
    }
    GoogleMap(
        modifier = modifier.testTag("mapView"),
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

@Composable
internal fun LocationFormView(
    editor: CaseLocationDataEditor,
) {
    val translator = LocalAppTranslator.current
    val isEditable = LocalCaseEditor.current.isEditable

    val inputData = editor.locationInputData

    val closeKeyboard = rememberCloseKeyboard(inputData)

    inputData.run {
        val showAddressForm by remember(
            streetAddressError,
            zipCodeError,
            cityError,
            countyError,
            stateError,
            hasWrongLocation,
            isEditingAddress,
        ) {
            derivedStateOf { hasAddressError || hasWrongLocation || isEditingAddress }
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
                // TODO Profile method references does not recompose unnecessarily
                editor::clearAddress,
                editor::onEditAddress,
            )
        }
    }

    val updateCrossStreet = remember(inputData) {
        { s: String ->
            inputData.crossStreetNearbyLandmark = s
        }
    }
    OutlinedClearableTextField(
        modifier = listItemModifier.testTag("locationCrossStreetTextField"),
        labelResId = 0,
        label = translator("formLabels.cross_street"),
        value = inputData.crossStreetNearbyLandmark,
        onValueChange = updateCrossStreet,
        keyboardType = KeyboardType.Text,
        keyboardCapitalization = KeyboardCapitalization.Words,
        isError = false,
        enabled = isEditable,
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
        listItemModifier
            .listCheckboxAlignStartOffset()
            .testTag("locationAddressProblemsCheckbox"),
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
    val translator = LocalAppTranslator.current
    val inputData = editor.locationInputData

    val updateAddress = remember(inputData) { { s: String -> inputData.streetAddress = s } }
    val clearAddressError = remember(inputData) { { inputData.streetAddressError = "" } }
    val isAddressError = inputData.streetAddressError.isNotEmpty()
    val focusAddress = isAddressError
    val addressLabel = translator("formLabels.address")
    ErrorText(inputData.streetAddressError)
    OutlinedClearableTextField(
        modifier = listItemModifier.testTag("locationStreetAddressTextField"),
        labelResId = 0,
        label = "$addressLabel *",
        value = inputData.streetAddress,
        onValueChange = updateAddress,
        keyboardType = KeyboardType.Password,
        keyboardCapitalization = KeyboardCapitalization.Words,
        isError = isAddressError,
        hasFocus = focusAddress,
        onNext = clearAddressError,
        enabled = isEditable,
    )

    val updateCity = remember(inputData) { { s: String -> inputData.city = s } }
    val clearCityError = remember(inputData) { { inputData.cityError = "" } }
    val isCityError = inputData.cityError.isNotEmpty()
    val focusCity = isCityError
    val cityLabel = translator("formLabels.city")
    ErrorText(inputData.cityError)
    OutlinedClearableTextField(
        modifier = listItemModifier.testTag("locationCityTextField"),
        labelResId = 0,
        label = "$cityLabel *",
        value = inputData.city,
        onValueChange = updateCity,
        keyboardType = KeyboardType.Text,
        keyboardCapitalization = KeyboardCapitalization.Words,
        isError = isCityError,
        hasFocus = focusCity,
        onNext = clearCityError,
        enabled = isEditable,
    )

    val updateCounty = remember(inputData) { { s: String -> inputData.county = s } }
    val clearCountyError = remember(inputData) { { inputData.countyError = "" } }
    val isCountyError = inputData.countyError.isNotEmpty()
    val focusCounty = isCountyError
    val countyLabel = translator("formLabels.county")
    ErrorText(inputData.countyError)
    OutlinedClearableTextField(
        modifier = listItemModifier.testTag("locationCountyTextField"),
        labelResId = 0,
        label = "$countyLabel *",
        value = inputData.county,
        onValueChange = updateCounty,
        keyboardType = KeyboardType.Text,
        keyboardCapitalization = KeyboardCapitalization.Words,
        isError = isCountyError,
        hasFocus = focusCounty,
        onNext = clearCountyError,
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
        modifier = listItemModifier.testTag("locationStateTextField"),
        labelResId = 0,
        label = "$stateLabel *",
        value = inputData.state,
        onValueChange = updateState,
        keyboardType = KeyboardType.Text,
        keyboardCapitalization = KeyboardCapitalization.Words,
        isError = isStateError,
        hasFocus = focusState,
        onEnter = onStateEnd,
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
        modifier = listItemModifier.testTag("locationPostalCodeTextField"),
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
}
