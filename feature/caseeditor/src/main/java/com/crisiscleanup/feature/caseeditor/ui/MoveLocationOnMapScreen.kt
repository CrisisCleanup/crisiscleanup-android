package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.data.model.ExistingWorksiteIdentifier
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyButton
import com.crisiscleanup.core.designsystem.component.LIST_DETAIL_DETAIL_WEIGHT
import com.crisiscleanup.core.designsystem.component.LIST_DETAIL_LIST_WEIGHT
import com.crisiscleanup.core.designsystem.component.TopAppBarBackAction
import com.crisiscleanup.core.designsystem.component.cancelButtonColors
import com.crisiscleanup.core.designsystem.component.listDetailDetailMaxWidth
import com.crisiscleanup.core.designsystem.theme.LocalDimensions
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.listItemHeight
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.mapmarker.ui.rememberMapProperties
import com.crisiscleanup.core.mapmarker.ui.rememberMapUiSettings
import com.crisiscleanup.core.ui.MapOverlayMessage
import com.crisiscleanup.feature.caseeditor.CaseLocationDataEditor
import com.crisiscleanup.feature.caseeditor.EditCaseBaseViewModel
import com.crisiscleanup.feature.caseeditor.EditCaseLocationViewModel
import com.crisiscleanup.feature.caseeditor.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

@Composable
internal fun EditCaseMapMoveLocationRoute(
    onBack: () -> Unit = {},
    openExistingCase: (ids: ExistingWorksiteIdentifier) -> Unit = { _ -> },
    viewModel: EditCaseLocationViewModel = hiltViewModel(),
) {
    val editor = viewModel.editor
    val editDifferentWorksite by editor.editIncidentWorksite.collectAsStateWithLifecycle()
    val isLocationCommitted by editor.isLocationCommitted.collectAsStateWithLifecycle()
    if (editDifferentWorksite.isDefined) {
        openExistingCase(editDifferentWorksite)
    } else if (isLocationCommitted) {
        onBack()
    } else {
        editor.setMoveLocationOnMap(true)

        val isCheckingOutOfBounds by editor.isCheckingOutOfBounds.collectAsStateWithLifecycle()
        val isEditable = !isCheckingOutOfBounds

        val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()

        val isListDetailLayout = LocalDimensions.current.isListDetailWidth

        val t = LocalAppTranslator.current
        val title = t("caseForm.select_on_map")

        val locationQuery by editor.locationInputData.locationQuery.collectAsStateWithLifecycle()

        val onUseMyLocation = remember(viewModel) { { editor.useMyLocation() } }

        if (isListDetailLayout) {
            ListDetailLayout(
                title,
                locationQuery,
                viewModel,
                editor,
                isOnline,
                onBack,
                isEditable,
                onUseMyLocation,
            )
        } else {
            PortraitLayout(
                title,
                locationQuery,
                viewModel,
                editor,
                isOnline,
                onBack,
                isEditable,
                onUseMyLocation,
            )
        }

        LocationOutOfBoundsDialog(editor)
    }
}

@Composable
private fun UseMyLocationAction(
    isEditable: Boolean,
    onUseMyLocation: () -> Unit,
) {
    val useMyLocationText = LocalAppTranslator.current("caseForm.use_my_location")
    CompositionLocalProvider(
        LocalTextStyle provides LocalFontStyles.current.header4,
    ) {
        CrisisCleanupIconTextButton(
            modifier = Modifier
                .listItemHeight()
                .fillMaxWidth(),
            iconResId = R.drawable.ic_use_my_location,
            label = useMyLocationText,
            onClick = onUseMyLocation,
            enabled = isEditable,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PortraitLayout(
    title: String,
    locationQuery: String,
    viewModel: EditCaseBaseViewModel,
    editor: CaseLocationDataEditor,
    isOnline: Boolean,
    onBack: () -> Unit = {},
    isEditable: Boolean = false,
    onUseMyLocation: () -> Unit = {},
) {
    Column {
        TopAppBarBackAction(
            title = title,
            onAction = onBack,
        )

        if (isOnline) {
            FullAddressSearchInput(
                viewModel,
                editor,
                locationQuery,
                isEditable = isEditable,
            )
        }

        if (locationQuery.isBlank()) {
            Box(Modifier.weight(1f)) {
                MoveMapUnderLocation(viewModel, editor, isEditable)
            }

            UseMyLocationAction(
                isEditable = isEditable,
                onUseMyLocation = onUseMyLocation,
            )

            SaveActionBar(viewModel, editor, onBack, isEditable, horizontalLayout = true)
        } else {
            editor.isMapLoaded = false
            AddressSearchResults(viewModel, editor, locationQuery, isEditable = isEditable)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListDetailLayout(
    title: String,
    locationQuery: String,
    viewModel: EditCaseBaseViewModel,
    editor: CaseLocationDataEditor,
    isOnline: Boolean,
    onBack: () -> Unit = {},
    isEditable: Boolean = false,
    onUseMyLocation: () -> Unit = {},
) {
    Row {
        Column(Modifier.weight(LIST_DETAIL_LIST_WEIGHT)) {
            TopAppBarBackAction(
                title = title,
                onAction = onBack,
            )

            if (isOnline) {
                FullAddressSearchInput(
                    viewModel,
                    editor,
                    locationQuery,
                    isEditable = isEditable,
                )
            }

            Spacer(Modifier.weight(1f))

            val isEditableListDetail = isEditable && locationQuery.isBlank()
            UseMyLocationAction(
                isEditable = isEditableListDetail,
                onUseMyLocation = onUseMyLocation,
            )

            SaveActionBar(viewModel, editor, onBack, isEditableListDetail)
        }
        Column(
            Modifier
                .weight(LIST_DETAIL_DETAIL_WEIGHT)
                .sizeIn(maxWidth = listDetailDetailMaxWidth),
        ) {
            if (locationQuery.isBlank()) {
                Box(Modifier.weight(1f)) {
                    MoveMapUnderLocation(viewModel, editor, isEditable)
                }
            } else {
                editor.isMapLoaded = false
                AddressSearchResults(viewModel, editor, locationQuery, isEditable = isEditable)
            }
        }
    }
}

@Composable
private fun BoxScope.MoveMapUnderLocation(
    viewModel: EditCaseBaseViewModel,
    editor: CaseLocationDataEditor,
    isEditable: Boolean,
    modifier: Modifier = Modifier,
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

    val markerState = rememberMarkerState()
    val coordinates by editor.locationInputData.coordinates.collectAsStateWithLifecycle()
    markerState.position = coordinates

    val mapMarkerIcon by editor.mapMarkerIcon.collectAsStateWithLifecycle()

    var uiSettings by rememberMapUiSettings()
    if (uiSettings.scrollGesturesEnabled != isEditable) {
        uiSettings = uiSettings.copy(
            rotationGesturesEnabled = isEditable,
            scrollGesturesEnabled = isEditable,
            tiltGesturesEnabled = isEditable,
            zoomGesturesEnabled = isEditable,
        )
    }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SaveActionBar(
    viewModel: EditCaseBaseViewModel,
    editor: CaseLocationDataEditor,
    onBack: () -> Unit = {},
    isEditable: Boolean = false,
    horizontalLayout: Boolean = false,
) {
    val translator = LocalAppTranslator.current
    val onSave = remember(viewModel) {
        {
            if (editor.onSaveMoveLocationCoordinates()) {
                onBack()
            }
        }
    }
    val rowMaxItemCount = if (horizontalLayout) Int.MAX_VALUE else 1
    FlowRow(
        modifier = Modifier
            // TODO Common dimensions
            .padding(16.dp),
        horizontalArrangement = listItemSpacedBy,
        verticalArrangement = listItemSpacedBy,
        maxItemsInEachRow = rowMaxItemCount,
    ) {
        BusyButton(
            Modifier.weight(1f),
            text = translator("actions.cancel"),
            enabled = isEditable,
            onClick = onBack,
            colors = cancelButtonColors(),
        )
        BusyButton(
            Modifier.weight(1f),
            text = translator("actions.save"),
            enabled = isEditable,
            onClick = onSave,
        )
    }
}
