package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.component.OutlinedClearableTextField
import com.crisiscleanup.core.designsystem.component.TopAppBarCancel
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.textMessagePadding
import com.crisiscleanup.feature.caseeditor.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditCaseAddressSearchRoute(
    viewModel: EditCaseLocationViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    openExistingCase: (ids: ExistingWorksiteIdentifier) -> Unit = { _ -> },
) {
    val editor = viewModel.editor
    val editDifferentWorksite by editor.editIncidentWorksite.collectAsStateWithLifecycle()
    if (editDifferentWorksite.isDefined) {
        openExistingCase(editDifferentWorksite)
    } else {
        Column {
            TopAppBarCancel(
                title = stringResource(R.string.location_address_search),
                onCancel = onBack,
            )
            AddressSearchView(
                viewModel,
                editor,
                onBack,
            )
        }
    }
}

@Composable
private fun ColumnScope.AddressSearchView(
    viewModel: EditCaseBaseViewModel,
    editor: CaseLocationDataEditor,
    onBack: () -> Unit = {},
) {
    val locationQuery by editor.locationInputData.locationQuery.collectAsStateWithLifecycle()
    val query = locationQuery.trim()
    val updateQuery = remember(viewModel) { { s: String -> editor.onQueryChange(s) } }
    OutlinedClearableTextField(
        modifier = Modifier
            .fillMaxWidth()
            .listItemPadding(),
        labelResId = R.string.location_address_search,
        value = locationQuery,
        onValueChange = updateQuery,
        keyboardType = KeyboardType.Password,
        isError = false,
        hasFocus = true,
        enabled = true,
    )

    if (editor.takeClearSearchInputFocus) {
        LocalFocusManager.current.clearFocus(true)
    }

    val isShortQuery by editor.isShortQuery.collectAsStateWithLifecycle()
    if (isShortQuery) {
        Text(
            stringResource(R.string.location_query_hint),
            modifier = Modifier.textMessagePadding(),
            style = MaterialTheme.typography.bodyLarge,
        )
    } else {
        val onAddressSelect = remember(viewModel) {
            {
                editor.commitChanges()
                onBack()
            }
        }
        SearchContents(
            viewModel,
            editor,
            query,
            onAddressSelect,
        )
    }
}
