package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.feature.caseeditor.EditCaseVolunteerReportViewModel

private const val ScreenTitleTranslateKey = "claim_status_report_info"

@Composable
internal fun VolunteerReportSummaryView(
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
internal fun EditCaseVolunteerReportRoute(
    viewModel: EditCaseVolunteerReportViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
) {
    EditCaseBackCancelView(
        viewModel,
        onBackClick,
        viewModel.translate(ScreenTitleTranslateKey)
    ) {
        VolunteerReportView()
    }
}

@Composable
private fun VolunteerReportView(
    viewModel: EditCaseVolunteerReportViewModel = hiltViewModel(),
) {
    val inputData = viewModel.volunteerReportInputData
    FormDataView(
        viewModel,
        inputData,
    )
}
