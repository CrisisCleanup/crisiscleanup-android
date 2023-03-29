package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.feature.caseeditor.EditCaseVolunteerReportViewModel
import com.crisiscleanup.feature.caseeditor.GroupSummaryFieldLookup
import com.crisiscleanup.feature.caseeditor.VolunteerReportFormGroupKey

private const val ScreenTitleTranslateKey = VolunteerReportFormGroupKey

@Composable
internal fun VolunteerReportSummaryView(
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
