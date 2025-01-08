package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.data.model.ExistingWorksiteIdentifier
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.ExplainLocationPermissionDialog
import com.crisiscleanup.core.designsystem.component.HelpRow
import com.crisiscleanup.core.designsystem.component.OutlinedSingleLineTextField
import com.crisiscleanup.core.designsystem.component.WithHelpDialog
import com.crisiscleanup.core.designsystem.theme.CrisisCleanupTheme
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.listItemHorizontalPadding
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.feature.caseeditor.CaseLocationDataEditor
import com.crisiscleanup.feature.caseeditor.EditCaseBaseViewModel
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
    val translator = LocalAppTranslator.current
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
        ) { showHelp ->
            HelpRow(
                locationText,
                viewModel.helpHint,
                modifier = Modifier
                    .testTag("propertyLocationHelpRow")
                    .listItemHorizontalPadding()
                    // TODO Common dimensions
                    .padding(top = 16.dp),
                showHelp = showHelp,
            )
        }

        if (isOnline && editor.isSearchSuggested) {
            val fullAddressLabel = translator("caseView.full_address")
            OutlinedSingleLineTextField(
                modifier = Modifier
                    .testTag("propertyLocationFullAddressTextField")
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
                readOnly = true,
            )
        }

        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
        val mapHeight = screenHeight.times(0.5f).coerceAtMost(240.dp)
        val mapModifier = Modifier.sizeIn(maxHeight = mapHeight)
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
        )
    }
}

@Composable
private fun LocationMapActionBar(
    isEditable: Boolean = false,
    moveLocationOnMap: () -> Unit = {},
    useMyLocation: () -> Unit = {},
) {
    val translator = LocalAppTranslator.current
    Row {
        // TODO Common dimensions
        val modifier = Modifier
            .padding(8.dp)
            .weight(1f)
        CompositionLocalProvider(
            LocalTextStyle provides LocalFontStyles.current.header4,
        ) {
            CrisisCleanupIconTextButton(
                modifier = modifier.testTag("propertyLocationSelectOnMapBtn"),
                iconResId = R.drawable.ic_select_on_map,
                label = translator("caseForm.select_on_map"),
                onClick = moveLocationOnMap,
                enabled = isEditable,
            )
            CrisisCleanupIconTextButton(
                modifier = modifier.testTag("propertyLocationUseMyLocationBtn"),
                iconResId = R.drawable.ic_use_my_location,
                label = translator("caseForm.use_my_location"),
                onClick = useMyLocation,
                enabled = isEditable,
            )
        }
    }
}

@Preview
@Composable
private fun LocationMapActionBarPreview() {
    CrisisCleanupTheme {
        LocationMapActionBar(isEditable = true)
    }
}
