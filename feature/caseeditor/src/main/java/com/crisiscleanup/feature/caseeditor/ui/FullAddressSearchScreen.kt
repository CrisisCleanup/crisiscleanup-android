package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.data.model.ExistingWorksiteIdentifier
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.LinkifyHtmlText
import com.crisiscleanup.core.designsystem.component.OutlinedClearableTextField
import com.crisiscleanup.core.designsystem.component.TopAppBarBackAction
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.textMessagePadding
import com.crisiscleanup.feature.caseeditor.CaseLocationDataEditor
import com.crisiscleanup.feature.caseeditor.EditCaseBaseViewModel
import com.crisiscleanup.feature.caseeditor.EditCaseLocationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditCaseAddressSearchRoute(
    viewModel: EditCaseLocationViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    openExistingCase: (ids: ExistingWorksiteIdentifier) -> Unit = { _ -> },
) {
    val editor = viewModel.editor
    val editDifferentWorksite by editor.editIncidentWorksite.collectAsStateWithLifecycle()
    val isLocationCommitted by editor.isLocationCommitted.collectAsStateWithLifecycle()
    val isAddressCommitted by editor.isAddressCommitted.collectAsStateWithLifecycle()
    if (editDifferentWorksite.isDefined) {
        openExistingCase(editDifferentWorksite)
    } else if (isLocationCommitted || isAddressCommitted) {
        onBack()
    } else {
        val isCheckingOutOfBounds by editor.isCheckingOutOfBounds.collectAsStateWithLifecycle()
        val isEditable = !isCheckingOutOfBounds

        Column {
            TopAppBarBackAction(
                title = LocalAppTranslator.current("formLabels.location"),
                onAction = onBack,
            )
            val locationQuery by editor.locationInputData.locationQuery.collectAsStateWithLifecycle()
            FullAddressSearchInput(viewModel, editor, locationQuery, true, isEditable)
            AddressSearchResults(viewModel, editor, locationQuery, isEditable)
        }

        LocationOutOfBoundsDialog(editor)
    }
}

@Composable
internal fun FullAddressSearchInput(
    viewModel: EditCaseBaseViewModel,
    editor: CaseLocationDataEditor,
    locationQuery: String,
    hasFocus: Boolean = false,
    isEditable: Boolean = false,
) {
    val updateQuery = remember(viewModel) { { s: String -> editor.onQueryChange(s) } }
    val fullAddressLabel = LocalAppTranslator.current("caseView.full_address")
    OutlinedClearableTextField(
        modifier = Modifier
            .fillMaxWidth()
            .listItemPadding(),
        labelResId = 0,
        label = "$fullAddressLabel *",
        value = locationQuery,
        onValueChange = updateQuery,
        keyboardType = KeyboardType.Password,
        keyboardCapitalization = KeyboardCapitalization.Words,
        isError = false,
        hasFocus = hasFocus,
        enabled = isEditable,
    )

    if (editor.takeClearSearchInputFocus) {
        LocalFocusManager.current.clearFocus(true)
    }
}

@Composable
internal fun ColumnScope.AddressSearchResults(
    viewModel: EditCaseBaseViewModel,
    editor: CaseLocationDataEditor,
    locationQuery: String,
    isEditable: Boolean = false,
) {
    val isShortQuery by editor.isShortQuery.collectAsStateWithLifecycle()
    if (isShortQuery) {
        val instructions = LocalAppTranslator.current("caseForm.location_instructions")
        LinkifyHtmlText(
            modifier = Modifier.textMessagePadding(),
            text = instructions,
        )
    } else {
        val query = locationQuery.trim()
        SearchContents(
            viewModel,
            editor,
            query,
            isEditable,
        )
    }
}
