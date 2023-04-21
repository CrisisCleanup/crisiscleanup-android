package com.crisiscleanup.feature.cases.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import com.crisiscleanup.core.commoncase.model.CaseSummaryResult
import com.crisiscleanup.core.commoncase.ui.listCaseResults
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.component.OutlinedClearableTextField
import com.crisiscleanup.core.designsystem.theme.fillWidthPadded
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.feature.cases.R
import com.crisiscleanup.feature.cases.model.CasesSearchViewModel

@Composable
internal fun CasesSearchRoute(
    onBackClick: () -> Unit = {},
    openFilter: () -> Unit = {},
    openCase: (Long, Long) -> Boolean = { _, _ -> false },
    viewModel: CasesSearchViewModel = hiltViewModel(),
) {
    val selectedWorksite by viewModel.selectedWorksite.collectAsState()
    if (selectedWorksite.second != EmptyWorksite.id) {
        openCase(selectedWorksite.first, selectedWorksite.second)
    } else {
        val isLoading by viewModel.isLoading.collectAsState()
        val isNotLoading = !isLoading

        val isSelectingResult by viewModel.isSelectingResult.collectAsState()

        BackHandler(isNotLoading) {
            if (viewModel.onBack()) {
                onBackClick()
            }
        }

        val onCaseSelect = if (isLoading) {
            {}
        } else remember(viewModel) {
            { result: CaseSummaryResult ->
                viewModel.onSelectWorksite(result.networkWorksiteId)
            }
        }

        val q by viewModel.searchQuery.collectAsState()
        val updateQuery =
            remember(viewModel) { { text: String -> viewModel.searchQuery.value = text } }

        val searchResults by viewModel.searchResults.collectAsState()

        val recentCases by viewModel.recentWorksites.collectAsState()

        Box(Modifier.fillMaxSize()) {
            Column {
                OutlinedClearableTextField(
                    modifier = fillWidthPadded,
                    labelResId = 0,
                    label = "",
                    value = q,
                    onValueChange = updateQuery,
                    keyboardType = KeyboardType.Password,
                    enabled = !isSelectingResult,
                    hasFocus = true,
                    isError = false,
                )

                val closeKeyboard = rememberCloseKeyboard(viewModel)
                val lazyListState = rememberLazyListState()
                LazyColumn(
                    Modifier
                        .fillMaxWidth()
                        .scrollFlingListener(closeKeyboard),
                    state = lazyListState,
                ) {
                    if (q.isNotEmpty()) {
                        listCaseResults(searchResults, onCaseSelect)
                    } else {
                        recentCases(recentCases, onCaseSelect)
                    }
                }
            }

            BusyIndicatorFloatingTopCenter(isLoading)
        }
    }
}

private fun LazyListScope.recentCases(
    cases: List<CaseSummaryResult>,
    onSelect: (CaseSummaryResult) -> Unit = {},
) {
    if (cases.isNotEmpty()) {
        item("section-title") {
            Text(
                text = stringResource(R.string.recently_viewed),
                modifier = Modifier.listItemPadding(),
            )
        }
    }

    listCaseResults(cases, onSelect)
}