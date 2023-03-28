package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.feature.caseeditor.EditCaseHazardsViewModel

private const val ScreenTitleTranslateKey = "hazards_info"

@Composable
internal fun HazardsSummaryView(
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
internal fun EditCaseHazardsRoute(
    viewModel: EditCaseHazardsViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
) {
    EditCaseBackCancelView(
        viewModel,
        onBackClick,
        viewModel.translate(ScreenTitleTranslateKey)
    ) {
        HazardsView()
    }
}

@Composable
private fun HazardsView(
    viewModel: EditCaseHazardsViewModel = hiltViewModel(),
) {
    val inputData = viewModel.hazardsInputData
    FormDataView(
        viewModel,
        inputData,
    )
}