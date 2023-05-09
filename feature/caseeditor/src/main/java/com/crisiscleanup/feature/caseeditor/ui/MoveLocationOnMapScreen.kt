package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.component.BusyButton
import com.crisiscleanup.core.designsystem.component.TopAppBarSingleAction
import com.crisiscleanup.core.designsystem.component.cancelButtonColors
import com.crisiscleanup.core.designsystem.theme.*
import com.crisiscleanup.core.mapmarker.ui.rememberMapProperties
import com.crisiscleanup.core.mapmarker.ui.rememberMapUiSettings
import com.crisiscleanup.core.ui.MapOverlayMessage
import com.crisiscleanup.feature.caseeditor.*
import com.crisiscleanup.feature.caseeditor.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.*

@Composable
internal fun EditCaseMapMoveLocationRoute(
    viewModel: EditCaseLocationViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    openExistingCase: (ids: ExistingWorksiteIdentifier) -> Unit = { _ -> },
) {
    val editor = viewModel.editor
    val editDifferentWorksite by editor.editIncidentWorksite.collectAsStateWithLifecycle()
    if (editDifferentWorksite.isDefined) {
        openExistingCase(editDifferentWorksite)
    } else {
        editor.setMoveLocationOnMap(true)
        EditCaseMapMoveLocationScreen(viewModel, editor, onBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditCaseMapMoveLocationScreen(
    viewModel: EditCaseBaseViewModel,
    editor: CaseLocationDataEditor,
    onBack: () -> Unit = {},
) {

    Column {
        TopAppBarSingleAction(
            title = viewModel.translate("caseForm.select_on_map"),
            onAction = onBack,
        )

        val locationQuery by editor.locationInputData.locationQuery.collectAsStateWithLifecycle()
        FullAddressSearchInput(viewModel, editor, locationQuery)

        if (locationQuery.isBlank()) {
            Box(Modifier.weight(1f)) {
                MoveMapUnderLocation(viewModel, editor)
            }

            val useMyLocation = remember(viewModel) { { editor.useMyLocation() } }
            IconButton(
                modifier = Modifier
                    .listItemHeight()
                    .fillMaxWidth(),
                iconResId = R.drawable.ic_use_my_location,
                label = viewModel.translate("caseForm.use_my_location"),
                onClick = useMyLocation,
                enabled = true,
            )

            SaveActionBar(viewModel, editor, onBack)
        } else {
            editor.isMapLoaded = false
            AddressSearchResults(viewModel, editor, locationQuery)
        }
    }
}

@Composable
private fun BoxScope.MoveMapUnderLocation(
    viewModel: EditCaseBaseViewModel,
    editor: CaseLocationDataEditor,
    modifier: Modifier = Modifier,
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

    val markerState = rememberMarkerState()
    val coordinates by editor.locationInputData.coordinates.collectAsStateWithLifecycle()
    markerState.position = coordinates

    val mapMarkerIcon by editor.mapMarkerIcon.collectAsStateWithLifecycle()

    val uiSettings by rememberMapUiSettings()
    val mapProperties by rememberMapProperties()
    val cameraPositionState = rememberCameraPositionState()
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
private fun SaveActionBar(
    viewModel: EditCaseBaseViewModel,
    editor: CaseLocationDataEditor,
    onBack: () -> Unit = {},
) {
    val onSave = remember(viewModel) {
        {
            editor.commitChanges()
            onBack()
        }
    }
    Row(
        modifier = Modifier
            // TODO Common dimensions
            .padding(16.dp),
        horizontalArrangement = listItemSpacedBy,
    ) {
        BusyButton(
            Modifier.weight(1f),
            text = viewModel.translate("actions.cancel"),
            enabled = true,
            onClick = onBack,
            colors = cancelButtonColors(),
        )
        BusyButton(
            Modifier.weight(1f),
            text = viewModel.translate("actions.save"),
            enabled = true,
            onClick = onSave,
        )
    }
}