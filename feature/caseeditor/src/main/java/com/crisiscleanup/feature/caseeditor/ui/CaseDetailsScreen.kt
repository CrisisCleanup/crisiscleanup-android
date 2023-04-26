package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.feature.caseeditor.DetailsFormGroupKey
import com.crisiscleanup.feature.caseeditor.EditCaseDetailsViewModel
import com.crisiscleanup.feature.caseeditor.GroupSummaryFieldLookup
import com.crisiscleanup.feature.caseeditor.excludeDetailsFormFields

private const val ScreenTitleTranslateKey = DetailsFormGroupKey

@Composable
internal fun DetailsSummaryView(
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
            excludeFields = excludeDetailsFormFields,
        )
    }
}

@Composable
internal fun EditCaseDetailsRoute(
    viewModel: EditCaseDetailsViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
) {
    EditCaseBackCancelView(
        viewModel,
        onBackClick,
        viewModel.translate(ScreenTitleTranslateKey),
    ) {
        FormDataView(viewModel, viewModel.editor.detailsInputData, true)
    }
}
