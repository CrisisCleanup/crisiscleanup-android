package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.feature.caseeditor.EditCaseWorkViewModel
import com.crisiscleanup.feature.caseeditor.GroupSummaryFieldLookup
import com.crisiscleanup.feature.caseeditor.WorkFormGroupKey

private const val ScreenTitleTranslateKey = WorkFormGroupKey

@Composable
internal fun WorkSummaryView(
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
        worksite.run {
            // TODO
        }
    }
}

@Composable
internal fun EditCaseWorkRoute(
    viewModel: EditCaseWorkViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
) {
    EditCaseBackCancelView(
        viewModel,
        onBackClick,
        viewModel.translate(ScreenTitleTranslateKey)
    ) {
        WorkView()
    }
}

@Composable
private fun WorkView(
    viewModel: EditCaseWorkViewModel = hiltViewModel(),
) {
    val inputData = viewModel.workInputData
    FormDataView(
        viewModel,
        inputData,
    )
}
