package com.crisiscleanup.feature.caseeditor.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.common.combineTrimText
import com.crisiscleanup.core.commoncase.model.CaseSummaryResult
import com.crisiscleanup.core.commoncase.ui.listCaseResults
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemOptionPadding
import com.crisiscleanup.core.designsystem.theme.textMessagePadding
import com.crisiscleanup.core.model.data.LocationAddress
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.feature.caseeditor.CaseLocationDataEditor
import com.crisiscleanup.feature.caseeditor.EditCaseBaseViewModel
import com.crisiscleanup.feature.caseeditor.LocationSearchResults
import com.crisiscleanup.feature.caseeditor.R

@Composable
internal fun ColumnScope.SearchContents(
    viewModel: EditCaseBaseViewModel,
    editor: CaseLocationDataEditor,
    query: String = "",
    onAddressSelect: () -> Unit = {},
    isEditable: Boolean = false,
) {
    val isBusySearching by editor.isLocationSearching.collectAsStateWithLifecycle(false)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
    ) {
        BusyIndicatorFloatingTopCenter(isBusySearching)

        val locationSearchResults by editor.searchResults.collectAsStateWithLifecycle()
        if (locationSearchResults.isEmpty) {
            if (!isBusySearching && locationSearchResults.query == query) {
                val translator = LocalAppTranslator.current.translator
                var text = translator("info.cases_search_no_results")
                if (text == "info.cases_search_no_results") {
                    text = stringResource(R.string.no_location_results, locationSearchResults.query)
                }
                Text(
                    text,
                    modifier = Modifier.textMessagePadding(),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            val onCaseSelect = remember(viewModel) {
                { caseLocation: CaseSummaryResult ->
                    editor.onExistingWorksiteSelected(caseLocation)
                }
            }
            val onAddress = remember(viewModel) {
                { address: LocationAddress ->
                    if (editor.onGeocodeAddressSelected(address)) {
                        onAddressSelect()
                    }
                }
            }
            val closeKeyboard = rememberCloseKeyboard(viewModel)
            ListSearchResults(
                locationSearchResults,
                onCaseSelect = onCaseSelect,
                onAddressSelect = onAddress,
                closeKeyboard = closeKeyboard,
                isEditable = isEditable,
            )
        }
    }
}

@Composable
private fun ListTitle(
    @StringRes textResId: Int = 0,
    text: String = "",
) {
    Text(
        if (textResId == 0) text else stringResource(textResId),
        modifier = listItemModifier,
        style = MaterialTheme.typography.headlineSmall,
    )
}

private fun LazyListScope.listItemTitle(
    itemKey: String,
    contentType: String = "item-title",
    @StringRes textResId: Int = 0,
    text: String = "",
) {
    item(
        key = itemKey,
        contentType = contentType,
    ) {
        ListTitle(textResId, text)
    }
}

@Composable
private fun ListSearchResults(
    results: LocationSearchResults,
    modifier: Modifier = Modifier,
    onCaseSelect: (CaseSummaryResult) -> Unit = {},
    onAddressSelect: (LocationAddress) -> Unit = {},
    closeKeyboard: () -> Unit = {},
    isEditable: Boolean = false,
) {
    val translator = LocalAppTranslator.current.translator
    LazyColumn(modifier.scrollFlingListener(closeKeyboard)) {
        if (results.worksites.isNotEmpty()) {
            listItemTitle(
                itemKey = "title-worksites",
                text = translator("worksiteSearchInput.existing_cases"),
            )

            listCaseResults(
                results.worksites,
                onCaseSelect,
                isEditable = isEditable,
            )
        }

        if (results.addresses.isNotEmpty()) {
            listItemTitle(
                itemKey = "title-addresses",
                text = translator("info.addresses", R.string.geocoded_addresses),
            )

            items(
                results.addresses,
                key = { it.key },
                contentType = { "item-address" },
            ) { keyAddress ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(
                            enabled = isEditable,
                            onClick = { onAddressSelect(keyAddress.address) },
                        )
                        .listItemOptionPadding()
                ) {
                    with(keyAddress.address) {
                        Column {
                            Text(address)
                            Text(listOf(city, state, country).combineTrimText())
                        }
                    }
                }
            }
        }
    }
}