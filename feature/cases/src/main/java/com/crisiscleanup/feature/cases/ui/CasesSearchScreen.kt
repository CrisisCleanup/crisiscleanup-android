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
import com.crisiscleanup.core.data.model.ExistingWorksiteIdentifierNone
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.component.ClearableTextField
import com.crisiscleanup.core.designsystem.component.CrisisCleanupIconButton
import com.crisiscleanup.core.designsystem.component.HelpDialog
import com.crisiscleanup.core.designsystem.component.listDetailDetailMaxWidth
import com.crisiscleanup.core.designsystem.component.roundedOutline
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.LocalDimensions
import com.crisiscleanup.core.designsystem.theme.fillWidthPadded
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.listItemVerticalPadding
import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.feature.cases.CasesSearchResults
import com.crisiscleanup.feature.cases.CasesSearchViewModel

@Composable
internal fun CasesSearchRoute(
    isTeamCasesSearch: Boolean = false,
    onBack: () -> Unit = {},
    openCase: (Long, Long) -> Unit = { _, _ -> },
    onAssignToTeam: (Long, Long) -> Unit = { _, _ -> },
    viewModel: CasesSearchViewModel = hiltViewModel(),
) {
    val selectedWorksite by viewModel.selectedWorksite.collectAsStateWithLifecycle()
    val assigningWorksite by viewModel.assigningWorksite.collectAsStateWithLifecycle()
    if (selectedWorksite != ExistingWorksiteIdentifierNone) {
        openCase(selectedWorksite.incidentId, selectedWorksite.worksiteId)
        viewModel.clearSelection()
    } else if (assigningWorksite != ExistingWorksiteIdentifierNone) {
        onAssignToTeam(assigningWorksite.incidentId, assigningWorksite.worksiteId)
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

        val onCaseSelect = viewModel::onSelectWorksite
        val onCaseAssign = viewModel::onAssignToTeam
        val recentCases by viewModel.recentWorksites.collectAsStateWithLifecycle()
        val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()

        val focusOnSearchInput = viewModel.focusOnSearchInput

        val closeKeyboard = rememberCloseKeyboard(viewModel)
        val isEditable = !isSelectingResult

        val onClearUnassignableWorksite = remember(viewModel) {
            {
                viewModel.unassignableWorksite = EmptyWorksite
            }
        }
        if (isListDetailLayout) {
            Row {
                SearchCasesView(
                    onBack,
                    q,
                    updateQuery,
                    isTeamCasesSearch = isTeamCasesSearch,
                    isEditable = isEditable,
                    hasFocus = focusOnSearchInput,
                    closeKeyboard,
                    onCaseSelect = onCaseSelect,
                    onCaseAssign = onCaseAssign,
                    emptyList(),
                    searchResults,
                    isLoading = isLoading,
                    isSearching = isSearching,
                    isRecentsVisible = false,
                    unassignableWorksite = viewModel.unassignableWorksite,
                    onClearUnassignableWorksite = onClearUnassignableWorksite,
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
                        isTeamCasesSearch,
                        recentCases,
                        onSelect = onCaseSelect,
                        onAssignToTeam = onCaseAssign,
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
                isTeamCasesSearch = isTeamCasesSearch,
                isEditable = isEditable,
                hasFocus = focusOnSearchInput,
                closeKeyboard,
                onCaseSelect = onCaseSelect,
                onCaseAssign = onCaseAssign,
                recentCases,
                searchResults,
                isLoading = isLoading,
                isSearching = isSearching,
                isRecentsVisible = recentCases.isNotEmpty() && searchResults.q.isBlank(),
                unassignableWorksite = viewModel.unassignableWorksite,
                onClearUnassignableWorksite = onClearUnassignableWorksite,
            )
        }
    }
}

@Composable
private fun SearchCasesView(
    onBack: () -> Unit,
    q: String,
    updateQuery: (String) -> Unit,
    isTeamCasesSearch: Boolean,
    isEditable: Boolean,
    hasFocus: Boolean,
    closeKeyboard: () -> Unit,
    onCaseSelect: (CaseSummaryResult) -> Unit,
    onCaseAssign: (CaseSummaryResult) -> Unit,
    recentCases: List<CaseSummaryResult>,
    searchResults: CasesSearchResults,
    isLoading: Boolean,
    isSearching: Boolean,
    isRecentsVisible: Boolean,
    unassignableWorksite: Worksite,
    onClearUnassignableWorksite: () -> Unit,
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
                isTeamCasesSearch = isTeamCasesSearch,
                showRecents = isRecentsVisible,
                onCaseSelect = onCaseSelect,
                onCaseAssign = onCaseAssign,
                recentCases,
                searchResults,
                closeKeyboard = closeKeyboard,
                isEditable = isEditable,
                isSearching = isSearching,
            )
        }

        BusyIndicatorFloatingTopCenter(isLoading)

        UnassignableCaseDialog(unassignableWorksite, onClearUnassignableWorksite)
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
    isTeamCasesSearch: Boolean,
    showRecents: Boolean,
    onCaseSelect: (CaseSummaryResult) -> Unit,
    onCaseAssign: (CaseSummaryResult) -> Unit,
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
                isTeamCasesSearch,
                recentCases,
                onCaseSelect,
                onCaseAssign,
                isEditable = isEditable,
                alwaysShowTitle = false,
                Modifier.listItemPadding(),
            )
        } else {
            with(searchResults) {
                if (options.isNotEmpty()) {
                    listCaseResults(
                        isTeamCasesSearch,
                        options,
                        onCaseSelect,
                        onCaseAssign,
                        isEditable = isEditable,
                    )
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
    isTeamCasesSearch: Boolean,
    cases: List<CaseSummaryResult>,
    onSelect: (CaseSummaryResult) -> Unit = {},
    onAssignToTeam: (CaseSummaryResult) -> Unit = {},
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

    listCaseResults(
        isTeamCasesSearch = isTeamCasesSearch,
        cases,
        onCaseSelect = onSelect,
        onCaseAssign = onAssignToTeam,
        isEditable = isEditable,
    )
}

@Composable
private fun UnassignableCaseDialog(
    worksite: Worksite,
    onClose: () -> Unit,
) {
    if (worksite != EmptyWorksite) {
        val t = LocalAppTranslator.current
        val title = t("~~Unable to assign")
        val message =
            t("~~{case_number} cannot be reassigned as your organization has not claimed or cannot claim any work from this Case.")
                .replace("{case_number}", worksite.caseNumber)
        HelpDialog(
            title = title,
            text = message,
            onClose = onClose,
        )
    }
}
