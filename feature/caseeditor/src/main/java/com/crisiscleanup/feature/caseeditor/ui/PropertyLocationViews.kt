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
import com.crisiscleanup.core.designsystem.LocalAppTranslator
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
    isOnline: Boolean,
    openExistingCase: (ExistingWorksiteIdentifier) -> Unit = {},
    onMoveLocationOnMap: () -> Unit = {},
    openAddressSearch: () -> Unit = {},
) {
    val translator = LocalAppTranslator.current.translator
    val isEditable = LocalCaseEditor.current.isEditable

    val editDifferentWorksite by editor.editIncidentWorksite.collectAsStateWithLifecycle()
    if (editDifferentWorksite.isDefined) {
        openExistingCase(editDifferentWorksite)
    } else {
        editor.setMoveLocationOnMap(false)

        val locationText = translator("formLabels.location")
        WithHelpDialog(
            viewModel,
            helpTitle = locationText,
            helpText = translator("caseForm.location_instructions"),
            hasHtml = true,
            translator("actions.ok"),
        ) { showHelp ->
            HelpRow(
                locationText,
                viewModel.helpHint,
                // TODO Common dimensions
                modifier = Modifier.padding(top = 16.dp),
                showHelp = showHelp,
            )
        }

        if (isOnline) {
            val fullAddressLabel = translator("caseView.full_address")
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
        }

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
        )

        val closePermissionDialog =
            remember(viewModel) { { editor.showExplainPermissionLocation.value = false } }
        val explainPermission by editor.showExplainPermissionLocation
        ExplainLocationPermissionDialog(
            showDialog = explainPermission,
            closeDialog = closePermissionDialog,
            closeText = translator("actions.close"),
        )
    }
}

@Composable
private fun LocationMapActionBar(
    isEditable: Boolean = false,
    moveLocationOnMap: () -> Unit = {},
    useMyLocation: () -> Unit = {},
) {
    val translator = LocalAppTranslator.current.translator
    Row(modifier = Modifier.listItemPadding()) {
        CrisisCleanupIconTextButton(
            modifier = Modifier.weight(1f),
            iconResId = R.drawable.ic_select_on_map,
            label = translator("caseForm.select_on_map"),
            onClick = moveLocationOnMap,
            enabled = isEditable,
        )
        CrisisCleanupIconTextButton(
            modifier = Modifier.weight(1f),
            iconResId = R.drawable.ic_use_my_location,
            label = translator("caseForm.use_my_location"),
            onClick = useMyLocation,
            enabled = isEditable,
        )
    }
}
