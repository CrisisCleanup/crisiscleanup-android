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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.addresssearch.model.KeySearchAddress
import com.crisiscleanup.core.commoncase.model.CaseSummaryResult
import com.crisiscleanup.core.commoncase.ui.listCaseResults
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemOptionPadding
import com.crisiscleanup.core.designsystem.theme.textMessagePadding
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.feature.caseeditor.CaseLocationDataEditor
import com.crisiscleanup.feature.caseeditor.LocationSearchResults

@Composable
internal fun ColumnScope.SearchContents(
    editor: CaseLocationDataEditor,
    query: String = "",
    isEditable: Boolean = false,
) {
    val isBusySearching by editor.isLocationSearching.collectAsStateWithLifecycle(false)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
    ) {
        BusyIndicatorFloatingTopCenter(isBusySearching)

        val locationSearchResults by editor.searchResults.collectAsStateWithLifecycle()
        if (locationSearchResults.isEmpty) {
            if (!isBusySearching && locationSearchResults.query == query) {
                val translator = LocalAppTranslator.current
                val text = translator("worksiteSearchInput.no_location_results")
                    .replace("{q}", locationSearchResults.query)
                Text(
                    text,
                    modifier = Modifier.textMessagePadding(),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            val onCaseSelect = editor::onExistingWorksiteSelected
            val onAddress = editor::onGeocodeAddressSelected
            val closeKeyboard = rememberCloseKeyboard()
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
        style = LocalFontStyles.current.header3,
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
    onAddressSelect: (KeySearchAddress) -> Unit = {},
    closeKeyboard: () -> Unit = {},
    isEditable: Boolean = false,
) {
    val translator = LocalAppTranslator.current
    LazyColumn(modifier.scrollFlingListener(closeKeyboard)) {
        if (results.worksites.isNotEmpty()) {
            listItemTitle(
                itemKey = "title-worksites",
                text = translator("worksiteSearchInput.existing_cases"),
            )

            listCaseResults(
                isTeamCasesSearch = false,
                results.worksites,
                onCaseSelect,
                isEditable = isEditable,
            )
        }

        if (results.addresses.isNotEmpty()) {
            listItemTitle(
                itemKey = "title-addresses",
                text = translator("caseView.full_address"),
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
                            onClick = { onAddressSelect(keyAddress) },
                        )
                        .listItemOptionPadding(),
                ) {
                    with(keyAddress) {
                        Column {
                            Text(addressLine1)
                            Text(addressLine2)
                        }
                    }
                }
            }
        }
    }
}
