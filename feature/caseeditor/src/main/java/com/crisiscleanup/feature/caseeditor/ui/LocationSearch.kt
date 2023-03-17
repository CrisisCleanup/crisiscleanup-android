package com.crisiscleanup.feature.caseeditor.ui

import androidx.annotation.StringRes
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.model.data.LocationAddress
import com.crisiscleanup.feature.caseeditor.EditCaseLocationViewModel
import com.crisiscleanup.feature.caseeditor.LocationSearchResults
import com.crisiscleanup.feature.caseeditor.R
import com.crisiscleanup.feature.caseeditor.model.ExistingCaseLocation

@Composable
internal fun ColumnScope.SearchContents(
    query: String = "",
    viewModel: EditCaseLocationViewModel = hiltViewModel(),
) {
    val isBusySearching by viewModel.isLocationSearching.collectAsStateWithLifecycle(false)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
    ) {
        // TODO Refactor and use common UI
        androidx.compose.animation.AnimatedVisibility(
            modifier = Modifier.align(Alignment.TopCenter),
            visible = isBusySearching,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            CircularProgressIndicator(
                Modifier
                    .wrapContentSize()
                    .padding(96.dp)
                    .size(24.dp)
            )
        }

        val locationSearchResults by viewModel.searchResults.collectAsStateWithLifecycle()
        if (locationSearchResults.isEmpty) {
            if (!isBusySearching && locationSearchResults.query == query) {
                // TODO Use common styles
                val text = stringResource(R.string.no_location_results, locationSearchResults.query)
                Text(
                    text,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            val onCaseSelect = remember(viewModel) {
                { caseLocation: ExistingCaseLocation ->
                    viewModel.onExistingWorksiteSelected(caseLocation)
                }
            }
            val onAddressSelect = remember(viewModel) {
                { address: LocationAddress ->
                    viewModel.onGeocodeAddressSelected(address)
                }
            }
            ListSearchResults(
                locationSearchResults,
                onCaseSelect,
                onAddressSelect,
            )
        }
    }
}

// TODO Use common style
@Composable
private fun ListTitle(
    @StringRes textResId: Int = 0,
    text: String = "",
) {
    Text(
        if (textResId == 0) text else stringResource(textResId),
        modifier = Modifier.padding(16.dp),
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

private fun commaConcatNonEmpty(vararg ss: String) =
    ss.map(String::trim).filter(String::isNotEmpty).joinToString(", ")

@Composable
internal fun ListSearchResults(
    results: LocationSearchResults,
    onCaseSelect: (ExistingCaseLocation) -> Unit = {},
    onAddressSelect: (LocationAddress) -> Unit = {},
) {
    LazyColumn {
        if (results.worksites.isNotEmpty()) {
            listItemTitle(
                itemKey = "title-worksites",
                textResId = R.string.existing_worksites,
            )

            items(
                results.worksites,
                key = { it.networkWorksiteId },
                contentType = { "item-worksite" },
            ) { caseLocation ->
                Row(
                    Modifier
                        .clickable { onCaseSelect(caseLocation) }
                        .padding(16.dp)) {
                    with(caseLocation) {
                        if (icon != null && workType != null) {
                            Image(
                                bitmap = icon.asImageBitmap(),
                                contentDescription = workType.workTypeLiteral,
                            )
                        }
                        Column(
                            Modifier
                                .weight(1f)
                                .padding(start = 16.dp)
                        ) {
                            Text(commaConcatNonEmpty(name, caseNumber))
                            Text(commaConcatNonEmpty(address, city, state))
                        }
                    }
                }
            }
        }

        if (results.addresses.isNotEmpty()) {
            listItemTitle(
                itemKey = "title-addresses",
                textResId = R.string.geocoded_addresses,
            )

            items(
                results.addresses,
                key = { it.key },
                contentType = { "item-address" },
            ) { keyAddress ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onAddressSelect(keyAddress.address) }
                        .padding(16.dp)
                ) {
                    with(keyAddress.address) {
                        Column {
                            Text(address)
                            Text(commaConcatNonEmpty(city, state, country))
                        }
                    }
                }
            }
        }
    }
}