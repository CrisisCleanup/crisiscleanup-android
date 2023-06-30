package com.crisiscleanup.feature.cases.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.designsystem.AppTranslator
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.HelpRow
import com.crisiscleanup.core.designsystem.component.TopAppBarBackAction
import com.crisiscleanup.core.designsystem.component.WithHelpDialog
import com.crisiscleanup.core.model.data.CasesFilter
import com.crisiscleanup.core.model.data.CasesFilterMaxDaysAgo
import com.crisiscleanup.core.model.data.CasesFilterMinDaysAgo
import com.crisiscleanup.feature.cases.CasesFilterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CasesFilterRoute(
    onBack: () -> Unit = {},
    viewModel: CasesFilterViewModel = hiltViewModel(),
) {
    val translator = viewModel.translator
    val appTranslator = remember(viewModel) {
        AppTranslator(translator = translator)
    }

    val updateFilters =
        remember(viewModel) { { filters: CasesFilter -> viewModel.changeFilters(filters) } }

    val filters by viewModel.casesFilter.collectAsStateWithLifecycle()
    CompositionLocalProvider(
        LocalAppTranslator provides appTranslator,
    ) {
        Column(Modifier.fillMaxSize()) {
            TopAppBarBackAction(
                title = translator("worksiteFilters.filters"),
                onAction = onBack,
            )

            Text("Filters under construction")

//            LazyColumn(Modifier.weight(1f)) {
//                sviSlider(translator, filters, updateFilters)
//                daysUpdatedSlider(translator, filters, updateFilters)
//            }
        }
    }
}

internal fun LazyListScope.rangeSlider(
    minValueLabel: String,
    maxValueLabel: String,
    labelTranslateKey: String = "",
    value: Float = 1f,
    onUpdate: (Float) -> Unit = {},
    helpTranslateKey: String = "",
    isHelpHtml: Boolean = false,
) {
    item {
        val translator = LocalAppTranslator.current.translator
        val label = translator(labelTranslateKey)
        Column(Modifier.fillMaxWidth()) {
            if (helpTranslateKey.isEmpty()) {
                Text(label)
            } else {
                WithHelpDialog(
                    translator,
                    helpTitle = label,
                    helpText = translator(helpTranslateKey),
                    hasHtml = isHelpHtml,
                ) { showHelp ->
                    HelpRow(
                        text = label,
                        iconContentDescription = label,
                        showHelp = showHelp,
                    )
                }
            }
            // TODO Slider based on value
            Text("Slider")
            Row(Modifier.fillMaxWidth()) {
                Text(minValueLabel)
                Spacer(Modifier.weight(1f))
                Text(maxValueLabel)
            }
        }
    }
}

private fun LazyListScope.sviSlider(
    translator: KeyResourceTranslator,
    filters: CasesFilter,
    onUpdateFilter: (CasesFilter) -> Unit = {}
) {
    rangeSlider(
        translator("svi.most_vulnerable"),
        translator("svi.everyone"),
        "~~Vulnerability",
        filters.svi,
        { f: Float -> onUpdateFilter(filters.copy(svi = f)) },
        "svi.svi_more_info_link",
        true,
    )
}

private fun LazyListScope.daysUpdatedSlider(
    translator: KeyResourceTranslator,
    filters: CasesFilter,
    onUpdateFilter: (CasesFilter) -> Unit = {}
) {
    rangeSlider(
        translator("{days} days ago").replace("{days}", CasesFilterMinDaysAgo.toString()),
        translator("{days} days ago").replace("{days}", CasesFilterMaxDaysAgo.toString()),
        "worksiteFilters.updated",
        filters.daysAgoNormalized,
        { f: Float -> onUpdateFilter(filters.expandDaysAgo(f)) },
    )
}
