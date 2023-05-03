package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemNestedPadding
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksiteFormValue
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.feature.caseeditor.EditCaseBaseViewModel
import com.crisiscleanup.feature.caseeditor.GroupSummaryFieldLookup
import com.crisiscleanup.feature.caseeditor.model.FieldDynamicValue
import com.crisiscleanup.feature.caseeditor.model.FormFieldsInputData
import org.apache.commons.text.StringEscapeUtils

@Composable
internal fun FormFieldSummary(
    key: String,
    fieldName: String,
    formValue: WorksiteFormValue,
    lookup: GroupSummaryFieldLookup,
    modifier: Modifier = Modifier,
) {
    if (formValue.hasValue) {
        val text = if (formValue.isBooleanTrue) {
            fieldName
        } else {
            val value = lookup.optionTranslations[formValue.valueString] ?: formValue.valueString
            "$fieldName: $value"
        }

        key(key) {
            Text(
                text,
                modifier = modifier,
            )
        }
    }
}

@Composable
internal fun FormDataSummary(
    worksite: Worksite,
    translations: GroupSummaryFieldLookup?,
    modifier: Modifier = Modifier,
    excludeFields: Set<String>? = null,
) {
    translations?.let { lookup ->
        for ((key, fieldName) in lookup.fieldMap) {
            if (excludeFields?.contains(key) == true) {
                continue
            }

            worksite.formData?.get(key)?.let {
                FormFieldSummary(
                    key,
                    fieldName,
                    it,
                    lookup,
                    modifier,
                )
            }
        }
    }
}

@Composable
private fun FormItems(
    viewModel: EditCaseBaseViewModel,
    inputData: FormFieldsInputData,
    isEditable: Boolean = true,
    showHelp: (FieldDynamicValue) -> Unit = {},
) {
    val breakGlassHint = viewModel.breakGlassHint
    val helpHint = viewModel.helpHint

    val translate = remember(viewModel) { { s: String -> viewModel.translate(s) } }

    val groupExpandState = remember { inputData.groupExpandState }

    for (field in inputData.mutableFormFieldData) {
        var state by remember { field }

        if (state.nestLevel > 0) {
            val isParentExpanded = groupExpandState[state.field.parentKey] ?: false
            if (!isParentExpanded) {
                continue
            }
        }

        key(state.key) {
            var label = state.field.getFieldLabel(translate)
            if (state.field.isRequired) {
                label = "$label *"
            }
            val fieldShowHelp = remember(viewModel) { { showHelp(state) } }
            val modifier =
                if (state.nestLevel > 0) listItemModifier.listItemNestedPadding(state.nestLevel * 2)
                else listItemModifier
            DynamicFormListItem(
                state,
                label,
                groupExpandState,
                modifier,
                breakGlassHint,
                helpHint,
                fieldShowHelp,
                isEditable,
                translate,
            ) { value: FieldDynamicValue ->
                state = state.copy(
                    dynamicValue = value.dynamicValue,
                    breakGlass = value.breakGlass,
                    workTypeStatus = value.workTypeStatus,
                )
            }
        }
    }
}

@Composable
internal fun FormDataView(
    viewModel: EditCaseBaseViewModel,
    inputData: FormFieldsInputData,
    isEditable: Boolean = true,
) {
    HelpContent(viewModel) { showHelp ->
        val closeKeyboard = rememberCloseKeyboard(viewModel)
        val scrollState = rememberScrollState()
        Column(
            Modifier
                .scrollFlingListener(closeKeyboard)
                .verticalScroll(scrollState)
                .fillMaxSize()
        ) {
            FormItems(viewModel, inputData, isEditable, showHelp)
        }
    }
}

@Composable
internal fun FormDataItems(
    viewModel: EditCaseBaseViewModel,
    inputData: FormFieldsInputData,
    isEditable: Boolean = true,
) {
    HelpContent(viewModel) {
        FormItems(viewModel, inputData, isEditable, it)
    }
}

@Composable
private fun HelpContent(
    viewModel: EditCaseBaseViewModel,
    content: @Composable ((FieldDynamicValue) -> Unit) -> Unit,
) {
    var helpTitle by remember { mutableStateOf("") }
    var helpText by remember { mutableStateOf("") }
    val showHelp = remember(viewModel) {
        { data: FieldDynamicValue ->
            val text = data.field.help
            if (text.isNotBlank()) {
                helpTitle = data.field.label
                helpText = StringEscapeUtils.unescapeHtml4(text).toString()
            }
        }
    }

    content(showHelp)

    if (helpText.isNotBlank()) {
        HelpDialog(
            title = helpTitle,
            text = helpText,
            onClose = { helpText = "" },
        )
    }
}