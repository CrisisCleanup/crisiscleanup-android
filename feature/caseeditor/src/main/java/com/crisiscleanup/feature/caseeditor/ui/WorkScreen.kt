package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.crisiscleanup.core.designsystem.theme.listItemBottomPadding
import com.crisiscleanup.core.designsystem.theme.listItemNestedPadding
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
    workTypeGroups: Collection<String> = emptyList(),
    groupChildren: Map<String, Collection<String>> = emptyMap(),
    summaryFieldLookup: GroupSummaryFieldLookup? = null,
) {
    EditCaseSummaryHeader(
        0,
        isEditable,
        onEdit,
        modifier,
        header = translate(ScreenTitleTranslateKey),
    ) {
        summaryFieldLookup?.let { lookup ->
            val workTypeDataModifier = modifier.listItemNestedPadding()
            var isFirstWorkType = true
            for (workTypeKey in workTypeGroups) {
                if (!isFirstWorkType) {
                    Spacer(modifier = Modifier.listItemBottomPadding())
                }
                Text(
                    text = lookup.fieldMap[workTypeKey] ?: workTypeKey,
                    modifier = modifier,
                )
                for (key in groupChildren[workTypeKey] ?: emptyList()) {
                    val fieldName = lookup.fieldMap[key]
                    val formValue = worksite.formData?.get(key)
                    if (fieldName != null && formValue != null) {
                        FormFieldSummary(
                            key,
                            fieldName,
                            formValue,
                            lookup,
                            workTypeDataModifier,
                        )
                    }
                }
                isFirstWorkType = false
            }
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
        FormDataView(viewModel, viewModel.editor.inputData)
    }
}