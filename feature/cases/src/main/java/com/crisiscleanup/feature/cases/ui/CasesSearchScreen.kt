package com.crisiscleanup.feature.cases.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.commoncase.model.CaseSummaryResult
import com.crisiscleanup.core.commoncase.ui.listCaseResults
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.component.ClearableTextField
import com.crisiscleanup.core.designsystem.component.CrisisCleanupIconButton
import com.crisiscleanup.core.designsystem.component.listDetailDetailMaxWidth
import com.crisiscleanup.core.designsystem.component.roundedOutline
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.LocalDimensions
import com.crisiscleanup.core.designsystem.theme.fillWidthPadded
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.listItemVerticalPadding
import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.feature.cases.CasesSearchResults
import com.crisiscleanup.feature.cases.CasesSearchViewModel

@Composable
internal fun CasesSearchRoute(
    onBack: () -> Unit = {},
    openCase: (Long, Long) -> Boolean = { _, _ -> false },
    viewModel: CasesSearchViewModel = hiltViewModel(),
) {
    val selectedWorksite by viewModel.selectedWorksite.collectAsStateWithLifecycle()
    if (selectedWorksite.second != EmptyWorksite.id) {
        openCase(selectedWorksite.first, selectedWorksite.second)
        viewModel.clearSelection()
    } else {
        val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
        val isNotLoading = !isLoading

        val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()

        val isSelectingResult by viewModel.isSelectingResult.collectAsStateWithLifecycle()

        BackHandler(isNotLoading) {
            if (viewModel.onBack()) {
                onBack()
            }
        }

        val isListDetailLayout = LocalDimensions.current.isListDetailWidth

        val q by viewModel.searchQuery.collectAsStateWithLifecycle()
        val updateQuery =
            remember(viewModel) { { text: String -> viewModel.searchQuery.value = text } }

        val onCaseSelect = remember(viewModel) {
            { result: CaseSummaryResult -> viewModel.onSelectWorksite(result) }
        }
        val recentCases by viewModel.recentWorksites.collectAsStateWithLifecycle()
        val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()

        val focusOnSearchInput = viewModel.focusOnSearchInput

        val closeKeyboard = rememberCloseKeyboard(viewModel)
        val isEditable = !isSelectingResult
        if (isListDetailLayout) {
            Row {
                SearchCasesView(
                    onBack,
                    q,
                    updateQuery,
                    isEditable,
                    hasFocus = focusOnSearchInput,
                    closeKeyboard,
                    onCaseSelect,
                    emptyList(),
                    searchResults,
                    isLoading = isLoading,
                    isSearching = isSearching,
                    isRecentsVisible = false,
                    Modifier
                        .weight(0.5f)
                        .sizeIn(maxWidth = listDetailDetailMaxWidth),
                )

                LazyColumn(
                    Modifier
                        .weight(0.5f)
                        .scrollFlingListener(closeKeyboard),
                    state = rememberLazyListState(),
                ) {
                    recentCases(
                        recentCases,
                        onCaseSelect,
                        isEditable = isEditable,
                        alwaysShowTitle = true,
                        fillWidthPadded,
                    )
                }
            }
        } else {
            SearchCasesView(
                onBack,
                q,
                updateQuery,
                isEditable,
                hasFocus = focusOnSearchInput,
                closeKeyboard,
                onCaseSelect,
                recentCases,
                searchResults,
                isLoading = isLoading,
                isSearching = isSearching,
                isRecentsVisible = recentCases.isNotEmpty() && searchResults.q.isBlank(),
            )
        }
    }
}

@Composable
private fun SearchCasesView(
    onBack: () -> Unit,
    q: String,
    updateQuery: (String) -> Unit,
    isEditable: Boolean,
    hasFocus: Boolean,
    closeKeyboard: () -> Unit,
    onCaseSelect: (CaseSummaryResult) -> Unit,
    recentCases: List<CaseSummaryResult>,
    searchResults: CasesSearchResults,
    isLoading: Boolean,
    isSearching: Boolean,
    isRecentsVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        Column {
            SearchBar(
                onBack,
                q,
                updateQuery,
                isEditable,
                hasFocus = hasFocus,
                closeKeyboard,
            )

            ListCases(
                isRecentsVisible,
                onCaseSelect,
                recentCases,
                searchResults,
                closeKeyboard = closeKeyboard,
                isEditable = isEditable,
                isSearching = isSearching,
            )
        }

        BusyIndicatorFloatingTopCenter(isLoading)
    }
}

@Composable
private fun SearchBar(
    onBackClick: () -> Unit,
    q: String,
    updateQuery: (String) -> Unit,
    isEditable: Boolean,
    hasFocus: Boolean,
    closeKeyboard: () -> Unit,
) {
    val t = LocalAppTranslator.current

    Row(
        // TODO Common dimensions and color
        Modifier
            .listItemVerticalPadding()
            .padding(horizontal = 8.dp)
            .roundedOutline(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CrisisCleanupIconButton(
            imageVector = CrisisCleanupIcons.ArrowBack,
            onClick = onBackClick,
            contentDescription = t("actions.back"),
            modifier = Modifier.testTag("workIncidentSearchGoBackBtn"),
        )
        ClearableTextField(
            modifier = Modifier
                .testTag("workIncidentSearchTextField")
                .weight(1f),
            labelResId = 0,
            label = "",
            placeholder = t("actions.search"),
            value = q,
            onValueChange = updateQuery,
            keyboardType = KeyboardType.Password,
            enabled = isEditable,
            imeAction = ImeAction.Done,
            onEnter = closeKeyboard,
            hasFocus = hasFocus,
            isError = false,
        )
    }
}

@Composable
private fun ListCases(
    showRecents: Boolean,
    onCaseSelect: (CaseSummaryResult) -> Unit,
    recentCases: List<CaseSummaryResult>,
    searchResults: CasesSearchResults,
    closeKeyboard: () -> Unit = {},
    isEditable: Boolean = false,
    isSearching: Boolean = false,
) {
    val lazyListState = rememberLazyListState()
    LazyColumn(
        Modifier
            .fillMaxWidth()
            .scrollFlingListener(closeKeyboard),
        state = lazyListState,
    ) {
        if (showRecents) {
            recentCases(
                recentCases,
                onCaseSelect,
                isEditable = isEditable,
                alwaysShowTitle = false,
                Modifier.listItemPadding(),
            )
        } else {
            with(searchResults) {
                if (options.isNotEmpty()) {
                    listCaseResults(options, onCaseSelect, isEditable = isEditable)
                } else if (q.isNotBlank()) {
                    item {
                        if (!isSearching) {
                            val t = LocalAppTranslator.current
                            val message =
                                if (isShortQ) {
                                    t("info.search_query_is_short")
                                } else {
                                    t("info.no_search_results").replace("{search_string}", q)
                                }
                            Text(message, listItemModifier)
                        }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.recentCases(
    cases: List<CaseSummaryResult>,
    onSelect: (CaseSummaryResult) -> Unit = {},
    isEditable: Boolean,
    alwaysShowTitle: Boolean,
    modifier: Modifier = Modifier,
) {
    if (alwaysShowTitle || cases.isNotEmpty()) {
        item("section-title") {
            Text(
                text = LocalAppTranslator.current("casesVue.recently_viewed"),
                modifier = modifier
                    .testTag("workIncidentSearchRecentHeaderText"),
            )
        }
    }

    listCaseResults(cases, onSelect, isEditable = isEditable)
}
