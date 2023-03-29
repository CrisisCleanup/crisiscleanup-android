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
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.feature.caseeditor.EditCaseBaseViewModel
import com.crisiscleanup.feature.caseeditor.model.FieldDynamicValue
import com.crisiscleanup.feature.caseeditor.model.FormFieldsInputData
import org.apache.commons.text.StringEscapeUtils

@Composable
internal fun FormDataSummary(
    worksite: Worksite,
    fieldMap: Map<String, String>?,
    modifier: Modifier = Modifier,
) {
    fieldMap?.let {
        for ((key, fieldName) in fieldMap) {
            worksite.formData?.get(key)?.let {
                var text = ""
                if (it.isBoolean) {
                    if (it.valueBoolean) {
                        text = fieldName
                    }
                } else if (it.valueString.isNotBlank()) {
                    text = "$fieldName: ${it.valueString}"
                }

                if (text.isNotBlank()) {
                    Text(
                        text,
                        modifier = modifier,
                    )
                }
            }
        }
    }
}

@Composable
internal fun FormDataView(
    viewModel: EditCaseBaseViewModel,
    inputData: FormFieldsInputData,
) {
    val breakGlassHint = viewModel.breakGlassHint
    val helpHint = viewModel.helpHint

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

    val closeKeyboard = rememberCloseKeyboard(viewModel)
    val scrollState = rememberScrollState()
    val groupExpandState = remember { inputData.groupExpandState }
    Column(
        Modifier
            .scrollFlingListener(closeKeyboard)
            .verticalScroll(scrollState)
            .fillMaxSize()
    ) {
        val translate = remember(viewModel) { { s: String -> viewModel.translate(s) } }

        for (field in inputData.mutableFormFieldData) {
            var state by remember { field }

            if (state.nestLevel > 0) {
                val isParentExpanded = groupExpandState[state.field.parentKey] ?: false
                if (!isParentExpanded) {
                    continue
                }
            }

            // TODO Is it possible to isolate recomposition only to each changed item?
            //      key(){} is recomposing the entire list when only a single element changes.
            //      Try a simplified example first.
            key(state.key) {
                val label = state.field.getFieldLabel(translate)
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
                ) { value: FieldDynamicValue ->
                    state = state.copy(
                        dynamicValue = value.dynamicValue,
                        breakGlass = value.breakGlass,
                    )
                }
            }
        }
    }

    if (helpText.isNotBlank()) {
        HelpDialog(
            title = helpTitle,
            text = helpText,
            onClose = { helpText = "" },
        )
    }
}