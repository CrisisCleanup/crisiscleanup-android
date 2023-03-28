package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.feature.caseeditor.EditCaseWorkViewModel

private const val ScreenTitleTranslateKey = "work_info"

@Composable
internal fun WorkSummaryView(
    worksite: Worksite,
    isEditable: Boolean,
    modifier: Modifier = Modifier,
    onEdit: () -> Unit = {},
    translate: (String) -> String = { s -> s },
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
