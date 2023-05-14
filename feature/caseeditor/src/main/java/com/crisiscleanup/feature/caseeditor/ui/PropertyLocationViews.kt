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
    openExistingCase: (ExistingWorksiteIdentifier) -> Unit = {},
    onMoveLocationOnMap: () -> Unit = {},
    openAddressSearch: () -> Unit = {},
    translate: (String) -> String = { s -> s },
) {
    val isEditable = LocalCaseEditor.current.isEditable

    val editDifferentWorksite by editor.editIncidentWorksite.collectAsStateWithLifecycle()
    if (editDifferentWorksite.isDefined) {
        openExistingCase(editDifferentWorksite)
    } else {
        editor.setMoveLocationOnMap(false)

        val locationText = translate("formLabels.location")
        WithHelpDialog(
            viewModel,
            helpTitle = locationText,
            helpText = translate("caseForm.location_instructions"),
            hasHtml = true,
            translate("actions.ok"),
        ) { showHelp ->
            HelpRow(
                locationText,
                viewModel.helpHint,
                // TODO Common dimensions
                modifier = Modifier.padding(top = 16.dp),
                showHelp = showHelp,
            )
        }

        val fullAddressLabel = translate("caseView.full_address")
        OutlinedSingleLineTextField(
            modifier = Modifier
                .fillMaxWidth()
                .listItemPadding()
                .clickable(
                    onClick = openAddressSearch,
                    enabled = isEditable,
                ),
            labelResId = 0,
            label = "$fullAddressLabel *",
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

        val useMyLocation = remember(viewModel) { { editor.useMyLocation() } }
        LocationMapActionBar(
            isEditable,
            moveLocationOnMap = onMoveLocationOnMap,
            useMyLocation = useMyLocation,
            translate = translate,
        )

        LocationFormView(editor, translate)

        // TODO Handle out of bounds properly

        val closePermissionDialog =
            remember(viewModel) { { editor.showExplainPermissionLocation.value = false } }
        val explainPermission by editor.showExplainPermissionLocation
        ExplainLocationPermissionDialog(
            showDialog = explainPermission,
            closeDialog = closePermissionDialog,
            closeText = translate("actions.close"),
        )
    }
}

@Composable
private fun LocationMapActionBar(
    isEditable: Boolean = false,
    moveLocationOnMap: () -> Unit = {},
    useMyLocation: () -> Unit = {},
    translate: (String) -> String = { s -> s },
) {
    Row(modifier = Modifier.listItemPadding()) {
        CrisisCleanupIconTextButton(
            modifier = Modifier.weight(1f),
            iconResId = R.drawable.ic_select_on_map,
            label = translate("caseForm.select_on_map"),
            onClick = moveLocationOnMap,
            enabled = isEditable,
        )
        CrisisCleanupIconTextButton(
            modifier = Modifier.weight(1f),
            iconResId = R.drawable.ic_use_my_location,
            label = translate("caseForm.use_my_location"),
            onClick = useMyLocation,
            enabled = isEditable,
        )
    }
}
