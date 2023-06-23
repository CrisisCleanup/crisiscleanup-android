package com.crisiscleanup.feature.cases.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.listItemVerticalPadding
import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.feature.cases.CasesSearchViewModel
import com.crisiscleanup.feature.cases.R

@Composable
internal fun CasesSearchRoute(
    onBackClick: () -> Unit = {},
    openFilter: () -> Unit = {},
    openCase: (Long, Long) -> Boolean = { _, _ -> false },
    viewModel: CasesSearchViewModel = hiltViewModel(),
) {
    val selectedWorksite by viewModel.selectedWorksite.collectAsStateWithLifecycle()
    if (selectedWorksite.second != EmptyWorksite.id) {
        openCase(selectedWorksite.first, selectedWorksite.second)
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
        val translator = LocalAppTranslator.current.translator

        Box(Modifier.fillMaxSize()) {
            Column {
                Row(
                    // TODO Common dimensions and color
                    Modifier
                        .listItemVerticalPadding()
                        .padding(horizontal = 8.dp)
                        .drawBehind {
                            drawRoundRect(
                                color = Color.Gray,
                                style = Stroke(width = 1.dp.toPx()),
                                cornerRadius = CornerRadius(4.dp.toPx()),
                            )
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CrisisCleanupIconButton(
                        imageVector = CrisisCleanupIcons.ArrowBack,
                        onClick = onBackClick,
                        contentDescription = translator("actions.back"),
                    )
                    ClearableTextField(
                        modifier = Modifier.weight(1f),
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
                    CrisisCleanupIconButton(
                        iconResId = R.drawable.ic_dials,
                        onClick = openFilter,
                        contentDescription = translator("casesVue.filters"),
                    )
                }

                ListCases(
                    q.isEmpty(),
                    closeKeyboard = closeKeyboard,
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
            recentCases(recentCases, onCaseSelect)
        } else {
            with(searchResults) {
                if (options.isNotEmpty()) {
                    listCaseResults(options, onCaseSelect, isEditable = true)
                } else {
                    item {
                        val translator = LocalAppTranslator.current.translator
                        val message =
                            if (isShortQ) translator("info.search_query_is_short")
                            else translator("info.no_search_results").replace("{query}", q)
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
) {
    if (cases.isNotEmpty()) {
        item("section-title") {
            Text(
                text = LocalAppTranslator.current.translator("casesVue.recently_viewed"),
                modifier = Modifier.listItemPadding(),
            )
        }
    }

    listCaseResults(cases, onSelect, isEditable = true)
}