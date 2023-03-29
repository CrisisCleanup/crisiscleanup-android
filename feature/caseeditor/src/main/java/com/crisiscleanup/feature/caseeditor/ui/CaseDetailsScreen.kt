package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.feature.caseeditor.DetailsFormGroupKey
import com.crisiscleanup.feature.caseeditor.EditCaseDetailsViewModel

private const val ScreenTitleTranslateKey = DetailsFormGroupKey

@Composable
internal fun DetailsSummaryView(
    worksite: Worksite,
    isEditable: Boolean,
    modifier: Modifier = Modifier,
    onEdit: () -> Unit = {},
    translate: (String) -> String = { s -> s },
    fieldMap: Map<String, String>?,
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
            fieldMap,
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
        viewModel.translate(ScreenTitleTranslateKey)
    ) {
        DetailsView()
    }
}

@Composable
private fun DetailsView(
    viewModel: EditCaseDetailsViewModel = hiltViewModel(),
) {
    val inputData = viewModel.detailsInputData
    FormDataView(
        viewModel,
        inputData,
    )
}
