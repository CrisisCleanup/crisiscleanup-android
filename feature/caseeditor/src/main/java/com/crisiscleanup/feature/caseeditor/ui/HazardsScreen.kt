package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.feature.caseeditor.EditCaseHazardsViewModel
import com.crisiscleanup.feature.caseeditor.GroupSummaryFieldLookup
import com.crisiscleanup.feature.caseeditor.HazardsFormGroupKey

private const val ScreenTitleTranslateKey = HazardsFormGroupKey

@Composable
internal fun HazardsSummaryView(
    worksite: Worksite,
    isEditable: Boolean,
    modifier: Modifier = Modifier,
    onEdit: () -> Unit = {},
    translate: (String) -> String = { s -> s },
    summaryFieldLookup: GroupSummaryFieldLookup? = null,
) {
    EditCaseSummaryHeader(
        0,
        isEditable,
        onEdit,
        modifier,
        header = translate(ScreenTitleTranslateKey),
    ) {
        FormDataSummary(
            worksite,
            summaryFieldLookup,
        )
    }
}

@Composable
internal fun EditCaseHazardsRoute(
    viewModel: EditCaseHazardsViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
) {
    EditCaseBackCancelView(
        viewModel,
        onBackClick,
        viewModel.translate(ScreenTitleTranslateKey)
    ) {
        FormDataView(viewModel, viewModel.editor.inputData)
    }
}
