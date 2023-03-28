package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.feature.caseeditor.EditCaseDetailsViewModel

private const val ScreenTitleTranslateKey = "property_info"

@Composable
internal fun DetailsSummaryView(
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
