package com.crisiscleanup.feature.cases.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.crisiscleanup.core.designsystem.component.roundedOutline
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.listItemVerticalPadding
import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.feature.cases.CasesSearchViewModel

@Composable
internal fun CasesSearchRoute(
    onBackClick: () -> Unit = {},
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

        val isSelectingResult by viewModel.isSelectingResult.collectAsStateWithLifecycle()

        BackHandler(isNotLoading) {
            if (viewModel.onBack()) {
                onBackClick()
            }
        }

        val q by viewModel.searchQuery.collectAsStateWithLifecycle()
        val updateQuery =
            remember(viewModel) { { text: String -> viewModel.searchQuery.value = text } }

        val closeKeyboard = rememberCloseKeyboard(viewModel)
        val translator = LocalAppTranslator.current

        Box {
            Column {
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
                        contentDescription = translator("actions.back"),
                        modifier = Modifier.testTag("workIncidentSearchGoBackBtn"),
                    )
                    ClearableTextField(
                        modifier = Modifier.testTag("workIncidentSearchTextField").weight(1f),
                        labelResId = 0,
                        label = "",
                        placeholder = translator("actions.search"),
                        value = q,
                        onValueChange = updateQuery,
                        keyboardType = KeyboardType.Password,
                        enabled = !isSelectingResult,
                        imeAction = ImeAction.Done,
                        onEnter = closeKeyboard,
                        isError = false,
                    )
                }

                ListCases(
                    q.isEmpty(),
                    closeKeyboard = closeKeyboard,
                    isEditable = !isSelectingResult,
                )
            }

            BusyIndicatorFloatingTopCenter(isLoading)
        }
    }
}

@Composable
private fun ListCases(
    showRecents: Boolean,
    viewModel: CasesSearchViewModel = hiltViewModel(),
    closeKeyboard: () -> Unit = {},
    isEditable: Boolean = false,
) {
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()

    val onCaseSelect = remember(viewModel) {
        { result: CaseSummaryResult -> viewModel.onSelectWorksite(result) }
    }
    val recentCases by viewModel.recentWorksites.collectAsStateWithLifecycle()

    val lazyListState = rememberLazyListState()
    LazyColumn(
        Modifier
            .fillMaxWidth()
            .scrollFlingListener(closeKeyboard),
        state = lazyListState,
    ) {
        if (showRecents) {
            recentCases(recentCases, onCaseSelect, isEditable)
        } else {
            with(searchResults) {
                if (options.isNotEmpty()) {
                    listCaseResults(options, onCaseSelect, isEditable = isEditable)
                } else {
                    item {
                        val translator = LocalAppTranslator.current
                        val message =
                            if (isShortQ) {
                                translator("info.search_query_is_short")
                            } else {
                                translator("info.no_search_results").replace("{search_string}", q)
                            }
                        Text(message, listItemModifier)
                    }
                }
            }
        }
    }
}

private fun LazyListScope.recentCases(
    cases: List<CaseSummaryResult>,
    onSelect: (CaseSummaryResult) -> Unit = {},
    isEditable: Boolean = false,
) {
    if (cases.isNotEmpty()) {
        item("section-title") {
            Text(
                text = LocalAppTranslator.current("casesVue.recently_viewed"),
                modifier = Modifier.testTag("workIncidentSearchRecentHeaderText").listItemPadding(),
            )
        }
    }

    listCaseResults(cases, onSelect, isEditable = isEditable)
}
