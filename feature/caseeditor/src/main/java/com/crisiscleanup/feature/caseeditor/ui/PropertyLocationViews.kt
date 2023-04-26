package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.component.OutlinedSingleLineTextField
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.feature.caseeditor.CaseLocationDataEditor
import com.crisiscleanup.feature.caseeditor.EditCaseBaseViewModel
import com.crisiscleanup.feature.caseeditor.ExistingWorksiteIdentifier
import com.crisiscleanup.feature.caseeditor.R
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
internal fun PropertyLocationView(
    viewModel: EditCaseBaseViewModel,
    editor: CaseLocationDataEditor,
    isEditable: Boolean = false,
    openExistingCase: (ExistingWorksiteIdentifier) -> Unit = {},
    onMoveLocationOnMap: () -> Unit = {},
    openAddressSearch: () -> Unit = {},
) {
    val editDifferentWorksite by editor.editIncidentWorksite.collectAsStateWithLifecycle()
    if (editDifferentWorksite.isDefined) {
        openExistingCase(editDifferentWorksite)
    } else {
        editor.setMoveLocationOnMap(false)

        val locationText = viewModel.translate("formLabels.location")
        WithHelpDialog(
            viewModel,
            helpTitle = locationText,
            helpText = viewModel.translate("caseForm.location_instructions"),
            hasHtml = true,
        ) { showHelp ->
            HelpRow(
                locationText,
                viewModel.helpHint,
                // TODO Common dimensions
                modifier = Modifier.padding(top = 16.dp),
                showHelp = showHelp,
            )
        }

        OutlinedSingleLineTextField(
            modifier = Modifier
                .fillMaxWidth()
                .listItemPadding()
                .clickable(onClick = openAddressSearch),
            labelResId = 0,
            label = viewModel.translate("caseView.full_address"),
            value = "",
            onValueChange = {},
            enabled = false,
            isError = false,
            hasFocus = false,
            readOnly = true,
        )

        val (_, mapModifier) = getLayoutParameters(false)
        val cameraPositionState = rememberCameraPositionState()
        Box(mapModifier) {
            LocationMapView(
                viewModel,
                editor,
                zoomControls = true,
                disablePanning = true,
                cameraPositionState = cameraPositionState,
            )
        }

        LocationMapActionBar(
            viewModel,
            editor,
            isEditable,
            onMoveLocationOnMap,
        )

        LocationFormView(viewModel, editor, isEditable)

        // TODO Handle out of bounds properly

        val closePermissionDialog =
            remember(viewModel) { { editor.showExplainPermissionLocation.value = false } }
        val explainPermission by editor.showExplainPermissionLocation
        ExplainLocationPermissionDialog(
            showDialog = explainPermission,
            closeDialog = closePermissionDialog,
        )
    }
}

@Composable
private fun LocationMapActionBar(
    viewModel: EditCaseBaseViewModel,
    editor: CaseLocationDataEditor,
    isEditable: Boolean = false,
    moveLocationOnMap: () -> Unit = {},
) {
    val useMyLocation = remember(viewModel) { { editor.useMyLocation() } }

    Row(modifier = Modifier.listItemPadding()) {
        IconButton(
            modifier = Modifier.weight(1f),
            iconResId = R.drawable.ic_select_on_map,
            label = viewModel.translate("caseForm.select_on_map"),
            onClick = moveLocationOnMap,
            enabled = isEditable,
        )
        IconButton(
            modifier = Modifier.weight(1f),
            iconResId = R.drawable.ic_use_my_location,
            label = viewModel.translate("caseForm.use_my_location"),
            onClick = useMyLocation,
            enabled = isEditable,
        )
    }
}
